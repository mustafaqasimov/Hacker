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
    private final UserRepository users;
    private final RefreshRepository refresh;
    private final AuthEventRepository events;
    private final PasswordEncoder passwords;
    private final TokenSecrets secrets;
    private final AccessTokens access;
    private final AuthProperties props;
    private final Clock clock;
    private final UserMapper mapper;
    private final AccountRateLimit accountRateLimit;
    private final String dummyHash;

    AuthService(UserRepository users, RefreshRepository refresh, AuthEventRepository events,
                PasswordEncoder passwords, TokenSecrets secrets, AccessTokens access,
                AuthProperties props, Clock clock, UserMapper mapper, AccountRateLimit accountRateLimit) {
        this.users=users; this.refresh=refresh; this.events=events;
        this.passwords=passwords; this.secrets=secrets; this.access=access;
        this.props=props; this.clock=clock; this.mapper=mapper; this.accountRateLimit=accountRateLimit;
        this.dummyHash=passwords.encode(secrets.generate());
    }
    public void register(AuthDtos.Register request) {
        validatePassword(request.password());
        String email=normalize(request.email());
        accountRateLimit.consumeRegistration(email);
        UUID id=UUID.randomUUID();
        if (users.insertIfAbsent(id,email,passwords.encode(request.password())) == 0) return;
        event(id,"REGISTERED");
    }
    public AuthDtos.Tokens login(AuthDtos.Login request) {
        validatePasswordBytes(request.password());
        accountRateLimit.consumeLogin(normalize(request.email()));
        var candidate=users.findByEmail(normalize(request.email()));
        if (candidate.isEmpty()) { passwords.matches(request.password(),dummyHash); throw AuthFailure.invalid(); }
        var user=lock(candidate.get().id);
        if (!passwords.matches(request.password(),user.passwordHash) || user.blocked) {
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
        if (!session.expiresAt.isAfter(clock.instant()) || user.blocked) throw AuthFailure.invalid();
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
    public void changePassword(UUID userId, AuthDtos.ChangePassword request) {
        validatePassword(request.newPassword()); validatePasswordBytes(request.currentPassword());
        var user=lock(userId);
        if (!passwords.matches(request.currentPassword(),user.passwordHash)) throw AuthFailure.invalid();
        user.passwordHash=passwords.encode(request.newPassword()); revokeAll(user); event(user.id,"PASSWORD_CHANGED");
    }
    @Transactional(readOnly=true)
    public AuthDtos.Profile profile(UUID userId) { return mapper.profile(users.findById(userId).orElseThrow(AuthFailure::invalid)); }
    private void revokeAll(UserAccount user) { user.tokenVersion++; refresh.revokeUser(user.id); }
    private AuthDtos.Tokens issue(UserAccount user, UUID family, Instant expires) {
        String raw=secrets.generate(); var session=new RefreshSession();
        session.id=UUID.randomUUID(); session.userId=user.id; session.familyId=family;
        session.tokenHash=secrets.hash(raw); session.expiresAt=expires; refresh.save(session);
        return new AuthDtos.Tokens(access.issue(user),raw,"Bearer",props.accessTtl().toSeconds());
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
