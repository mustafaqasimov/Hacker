package az.hacktrain.auth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users; @Mock RefreshRepository refresh; @Mock AuthEventRepository events; @Mock PasswordEncoder passwords;
    @Mock AccountRateLimit accountRateLimit; @Mock AccessTokens access; @Mock UserMapper mapper; @Mock jakarta.persistence.EntityManager em;
    AuthService service;
    TokenSecrets secrets=new TokenSecrets();
    Instant now=Instant.parse("2026-10-09T00:00:00Z");
    @BeforeEach void init() {
        when(passwords.encode(anyString())).thenReturn("bcrypt-hash");
        var p=AuthCoreTest.props(); service=new AuthService(users,refresh,events,passwords,secrets,access,p,Clock.fixed(now,ZoneOffset.UTC),mapper,accountRateLimit);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"entityManager",em);
    }
    UserAccount user() { var u=new UserAccount(); u.id=UUID.randomUUID(); u.email="a@example.com"; u.passwordHash="hash"; u.platformRole="STUDENT"; return u; }
    @Test void registrationNormalizesEmailWithoutSendingEmail() {
        when(users.insertIfAbsent(any(),eq("a@example.com"),anyString())).thenReturn(1);
        service.register(new AuthDtos.Register(" A@EXAMPLE.COM ","StrongPassword123"));
        verify(users).insertIfAbsent(any(),eq("a@example.com"),anyString());
        verify(events).save(argThat(e->e.eventType.equals("REGISTERED")));
    }
    @Test void duplicateRegistrationDoesNotOverwritePassword() {
        when(users.insertIfAbsent(any(),anyString(),anyString())).thenReturn(0);
        service.register(new AuthDtos.Register("a@example.com","StrongPassword123")); verifyNoInteractions(events);
    }
    @Test void unicodePasswordIsBoundedInBytes() {
        assertThatThrownBy(()->service.register(new AuthDtos.Register("a@example.com","ş".repeat(40)))).isInstanceOf(AuthFailure.class);
        verifyNoInteractions(users);
    }
    @Test void shortPasswordIsRejected() {
        assertThatThrownBy(()->service.register(new AuthDtos.Register("a@example.com","short"))).isInstanceOf(AuthFailure.class);
    }
    @Test void unknownUserStillRunsBcrypt() {
        when(users.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.login(new AuthDtos.Login("a@example.com","bad"))).isInstanceOf(AuthFailure.class);
        verify(passwords).matches(eq("bad"),anyString());
    }
    @Test void blockedUserCannotLogin() {
        var u=user(); u.blocked=true; when(users.findByEmail(anyString())).thenReturn(Optional.of(u)); when(users.lockById(u.id)).thenReturn(Optional.of(u));
        when(passwords.matches(anyString(),anyString())).thenReturn(true);
        assertThatThrownBy(()->service.login(new AuthDtos.Login(u.email,"StrongPassword123"))).isInstanceOf(AuthFailure.class);
        verifyNoInteractions(refresh,access);
    }
    @Test void successfulLoginHashesRefreshToken() {
        var u=user(); when(users.findByEmail(anyString())).thenReturn(Optional.of(u)); when(users.lockById(u.id)).thenReturn(Optional.of(u)); when(passwords.matches(anyString(),anyString())).thenReturn(true); when(access.issue(u)).thenReturn("jwt");
        var result=service.login(new AuthDtos.Login(u.email,"StrongPassword123"));
        var stored=ArgumentCaptor.forClass(RefreshSession.class); verify(refresh).save(stored.capture());
        assertThat(stored.getValue().tokenHash).isEqualTo(secrets.hash(result.refreshToken())); assertThat(result.accessToken()).isEqualTo("jwt");
    }
    RefreshSession session(UserAccount u) { var s=new RefreshSession(); s.userId=u.id; s.familyId=UUID.randomUUID(); s.expiresAt=now.plusSeconds(100); when(refresh.findByTokenHash(anyString())).thenReturn(Optional.of(s)); when(users.lockById(u.id)).thenReturn(Optional.of(u)); return s; }
    @Test void replayRevokesEntireFamilyAndWritesEvidence() {
        var u=user(); var s=session(u); s.usedAt=now.minusSeconds(10);
        assertThatThrownBy(()->service.refresh("token")).isInstanceOf(AuthFailure.class);
        verify(refresh).revokeFamily(s.familyId); verify(events).save(argThat(e->e.eventType.equals("REFRESH_REPLAY"))); verify(refresh,never()).save(any());
    }
    @Test void expiredRefreshCannotBeRotated() {
        var s=session(user()); s.expiresAt=now;
        assertThatThrownBy(()->service.refresh("token")).isInstanceOf(AuthFailure.class); verify(refresh,never()).save(any());
    }
    @Test void rotationKeepsAbsoluteFamilyExpiry() {
        var u=user(); var s=session(u); when(access.issue(u)).thenReturn("jwt"); service.refresh("token");
        verify(refresh).save(argThat(next->next.familyId.equals(s.familyId)&&next.expiresAt.equals(s.expiresAt)));
        assertThat(s.usedAt).isEqualTo(now); verify(em).refresh(s);
    }
    @Test void unknownLogoutIsIdempotent() { when(refresh.findByTokenHash(anyString())).thenReturn(Optional.empty()); service.logout("token"); verify(refresh,never()).revokeFamily(any()); }
}
