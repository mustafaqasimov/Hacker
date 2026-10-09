package az.hacktrain.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@Transactional(noRollbackFor=AuthFailure.class)
public class AuthService {
    @org.springframework.beans.factory.annotation.Value("${hacktrain.auth.email-confirmation-required:false}")
    private boolean confirmationRequired=true;
    private final UserRepository users;
    private final RefreshRepository refresh;
    private final ActionTokenRepository actions;
    private final MailRepository mail;
    private final AuthEventRepository events;
    private final PasswordEncoder passwords;
    private final TokenSecrets secrets;
    private final MailCipher cipher;
    private final AccessTokens access;
    private final AuthProperties props;
    private final Clock clock;
    private final UserMapper mapper;
    private final AccountRateLimit accountRateLimit;
    private final String dummyHash;

    AuthService(UserRepository users, RefreshRepository refresh, ActionTokenRepository actions,
                MailRepository mail, AuthEventRepository events, PasswordEncoder passwords,
                TokenSecrets secrets, MailCipher cipher, AccessTokens access, AuthProperties props, Clock clock, UserMapper mapper, AccountRateLimit accountRateLimit) {
        this.users=users; this.refresh=refresh; this.actions=actions; this.mail=mail; this.events=events;
        this.passwords=passwords; this.secrets=secrets; this.cipher=cipher; this.access=access;
        this.props=props; this.clock=clock; this.mapper=mapper; this.accountRateLimit=accountRateLimit;
        this.dummyHash=passwords.encode(secrets.generate());
    }
    public void register(AuthDtos.Register request) {
        validatePassword(request.password());
        String email=normalize(request.email());
        accountRateLimit.consume(email,true);
        UUID id=UUID.randomUUID();
        if (users.insertIfAbsent(id,email,passwords.encode(request.password())) == 0) return;
        var user=users.findById(id).orElseThrow();
        if(confirmationRequired) sendAction(user,"VERIFY_EMAIL",Duration.ofHours(24));
        event(id,"REGISTERED");
    }
    public AuthDtos.Tokens login(AuthDtos.Login request) {
        validatePasswordBytes(request.password());
        accountRateLimit.consume(normalize(request.email()),false);
        var candidate=users.findByEmail(normalize(request.email()));
        if (candidate.isEmpty()) { passwords.matches(request.password(),dummyHash); throw AuthFailure.invalid(); }
        var user=lock(candidate.get().id);
        if (!passwords.matches(request.password(),user.passwordHash) || user.blocked || (confirmationRequired && !user.emailVerified)) {
            event(user.id,"LOGIN_REJECTED"); throw AuthFailure.invalid();
        }
        event(user.id,"LOGIN_SUCCEEDED");
        return issue(user,UUID.randomUUID(),clock.instant().plus(props.refreshTtl()));
    }
    public AuthDtos.Tokens refresh(String token) {
        var session=refresh.findByTokenHash(secrets.hash(token)).orElseThrow(AuthFailure::invalid);
        var user=lock(session.userId);
        // Refresh the managed entity after waiting for the user lock: another request may have rotated it.
        entityManager.refresh(session);
        if (session.usedAt != null || session.revoked) {
            refresh.revokeFamily(session.familyId); event(user.id,"REFRESH_REPLAY"); throw AuthFailure.invalid();
        }
        if (!session.expiresAt.isAfter(clock.instant()) || user.blocked || (confirmationRequired && !user.emailVerified)) throw AuthFailure.invalid();
        session.usedAt=clock.instant();
        event(user.id,"TOKEN_ROTATED");
        return issue(user,session.familyId,session.expiresAt);
    }
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public void logout(String token) {
        refresh.findByTokenHash(secrets.hash(token)).ifPresent(session -> {
            lock(session.userId); refresh.revokeFamily(session.familyId); event(session.userId,"LOGOUT");
        });
    }
    public void logoutAll(UUID userId) { var user=lock(userId); revokeAll(user); event(user.id,"LOGOUT_ALL"); }
    public void requestAction(String email, String purpose) {
        if(purpose.equals("VERIFY_EMAIL") && !confirmationRequired) return;
        accountRateLimit.consume(normalize(email),true);
        users.findByEmail(normalize(email)).ifPresent(candidate -> {
            var user=lock(candidate.id);
            if (user.blocked || (purpose.equals("VERIFY_EMAIL") && user.emailVerified)) return;
            sendAction(user,purpose,purpose.equals("VERIFY_EMAIL")?Duration.ofHours(24):Duration.ofMinutes(20));
        });
    }
    public void verifyEmail(String token) {
        if(!confirmationRequired) throw new AuthFailure(HttpStatus.NOT_FOUND,"verification_disabled","E-poçt təsdiqi tələb olunmur.");
        var action=consume(token,"VERIFY_EMAIL");
        var user=lock(action.userId); user.emailVerified=true; event(user.id,"EMAIL_VERIFIED");
    }
    public void resetPassword(AuthDtos.ResetPassword request) {
        validatePassword(request.password());
        var action=consume(request.token(),"RESET_PASSWORD");
        var user=lock(action.userId); user.passwordHash=passwords.encode(request.password());
        revokeAll(user); event(user.id,"PASSWORD_RESET");
    }
    public void changePassword(UUID userId, AuthDtos.ChangePassword request) {
        validatePassword(request.newPassword()); validatePasswordBytes(request.currentPassword());
        var user=lock(userId);
        if (!passwords.matches(request.currentPassword(),user.passwordHash)) throw AuthFailure.invalid();
        user.passwordHash=passwords.encode(request.newPassword()); revokeAll(user); event(user.id,"PASSWORD_CHANGED");
    }
    @Transactional(readOnly=true)
    public AuthDtos.Profile profile(UUID userId) { return mapper.profile(users.findById(userId).orElseThrow(AuthFailure::invalid)); }
    private ActionToken consume(String raw, String purpose) {
        var token=actions.findByTokenHash(secrets.hash(raw)).orElseThrow(AuthFailure::invalid);
        var user=lock(token.userId); entityManager.refresh(token);
        if (!token.purpose.equals(purpose) || token.consumedAt != null || !token.expiresAt.isAfter(clock.instant()) || user.blocked) throw AuthFailure.invalid();
        token.consumedAt=clock.instant(); return token;
    }
    private void revokeAll(UserAccount user) { user.tokenVersion++; refresh.revokeUser(user.id); invalidateActions(user.id); }
    private void invalidateActions(UUID userId) {
        entityManager.createQuery("update ActionToken t set t.consumedAt=:now where t.userId=:user and t.consumedAt is null")
                .setParameter("now",clock.instant()).setParameter("user",userId).executeUpdate();
    }
    private AuthDtos.Tokens issue(UserAccount user, UUID family, Instant expires) {
        String raw=secrets.generate(); var session=new RefreshSession();
        session.id=UUID.randomUUID(); session.userId=user.id; session.familyId=family;
        session.tokenHash=secrets.hash(raw); session.expiresAt=expires; refresh.save(session);
        return new AuthDtos.Tokens(access.issue(user),raw,"Bearer",props.accessTtl().toSeconds());
    }
    private void sendAction(UserAccount user, String purpose, Duration ttl) {
        String raw=secrets.generate(); var token=new ActionToken();
        token.id=UUID.randomUUID(); token.userId=user.id; token.tokenHash=secrets.hash(raw);
        token.purpose=purpose; token.expiresAt=clock.instant().plus(ttl); actions.save(token);
        String page=purpose.equals("VERIFY_EMAIL")?"verify-email":"reset-password";
        var message=new MailMessage(); message.id=UUID.randomUUID(); message.recipient=user.email;
        message.subject=purpose.equals("VERIFY_EMAIL")?"HackTrain e-poçt təsdiqi":"HackTrain şifrə bərpası";
        // Fragment is not sent in HTTP access logs or Referer; client must POST token from fragment.
        message.encryptedBody=cipher.encrypt(props.frontendUrl()+"/"+page+"#token="+raw);
        message.createdAt=clock.instant(); message.nextAttemptAt=clock.instant(); mail.save(message);
    }
    private UserAccount lock(UUID id) {
        var user=users.lockById(id).orElseThrow(AuthFailure::invalid);
        entityManager.refresh(user); // Discard any state loaded before waiting for the row lock.
        return user;
    }
    private void event(UUID user, String type) {
        var event=new AuthEvent(); event.id=UUID.randomUUID(); event.userId=user; event.eventType=type;
        event.createdAt=clock.instant(); events.save(event);
    }
    private String normalize(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private void validatePassword(String value) {
        validatePasswordBytes(value);
        if (value.length()<12) throw new AuthFailure(HttpStatus.BAD_REQUEST,"password_policy","Parol ən azı 12 simvol olmalıdır.");
    }
    private void validatePasswordBytes(String value) {
        if (value.getBytes(StandardCharsets.UTF_8).length>72) throw new AuthFailure(HttpStatus.BAD_REQUEST,"password_policy","Parol UTF-8 ilə 72 baytdan uzun ola bilməz.");
    }
}
