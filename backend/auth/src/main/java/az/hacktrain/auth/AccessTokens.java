package az.hacktrain.auth;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.List;
@Component
class AccessTokens {
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;
    AccessTokens(JwtEncoder encoder, AuthProperties properties, Clock clock) { this.encoder=encoder; this.properties=properties; this.clock=clock; }
    String issue(UserAccount user) {
        var now=clock.instant();
        var claims=JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(user.id.toString()).issuedAt(now).expiresAt(now.plus(properties.accessTtl()))
                .id(java.util.UUID.randomUUID().toString()).claim("ver",user.tokenVersion).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
}
