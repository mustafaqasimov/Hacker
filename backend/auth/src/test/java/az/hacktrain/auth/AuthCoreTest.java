package az.hacktrain.auth;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class AuthCoreTest {
    static AuthProperties props() {
        return new AuthProperties(Base64.getEncoder().encodeToString(new byte[32]),
            "test","api",Duration.ofMinutes(10),Duration.ofDays(30),List.of("http://localhost:5173"));
    }
    @Test void tokensAreRandomAndOnlyDigestIsDeterministic() {
        var secrets=new TokenSecrets(); var first=secrets.generate(); var second=secrets.generate();
        assertThat(first).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second);
        assertThat(secrets.hash(first)).hasSize(64).isEqualTo(secrets.hash(first)).isNotEqualTo(first);
    }
    @ParameterizedTest @ValueSource(strings={"", "abc", "c2hvcnQ="})
    void weakSigningKeysAreRejected(String key) {
        var p=props();
        assertThatThrownBy(()->new AuthProperties(key,p.issuer(),p.audience(),p.accessTtl(),p.refreshTtl(),p.corsOrigins())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void mapperDoesNotExposeSecrets() {
        var user=new UserAccount(); user.id=UUID.randomUUID(); user.email="student@example.com"; user.passwordHash="hidden";
        user.platformRole="STUDENT"; user.createdAt=Instant.now();
        var profile=Mappers.getMapper(UserMapper.class).profile(user);
        assertThat(profile.id()).isEqualTo(user.id); assertThat(profile.email()).isEqualTo(user.email);
        assertThat(profile.toString()).doesNotContain("hidden","passwordHash","tokenVersion");
    }
    @Test void jwtHasAudienceIssuerExpiryAndVersionButNoPii() {
        var p=props(); var config=new AuthSecurity(); var clock=Clock.fixed(Instant.now(),ZoneOffset.UTC);
        var user=new UserAccount(); user.id=UUID.randomUUID(); user.tokenVersion=4; user.email="secret@example.com";
        var issuer=new AccessTokens(config.jwtEncoder(p),p,clock);
        var jwt=config.jwtDecoder(p).decode(issuer.issue(user));
        assertThat(jwt.getSubject()).isEqualTo(user.id.toString());
        assertThat(jwt.getAudience()).containsExactly("api"); assertThat(jwt.getClaimAsString("ver")).isEqualTo("4");
        assertThat(jwt.getExpiresAt()).isEqualTo(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(600));
        assertThat(jwt.getClaims().toString()).doesNotContain(user.email);
    }
    @Test void expiredJwtRejected() {
        var p=props(); var config=new AuthSecurity(); var user=new UserAccount(); user.id=UUID.randomUUID();
        var issuer=new AccessTokens(config.jwtEncoder(p),p,Clock.offset(Clock.systemUTC(),Duration.ofHours(-1)));
        assertThatThrownBy(()->config.jwtDecoder(p).decode(issuer.issue(user))).isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
    }
}
