package az.hacktrain.auth;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;
import java.util.List;
@ConfigurationProperties("hacktrain.auth")
public record AuthProperties(String jwtSecret, String mailEncryptionKey, String issuer, String audience,
        Duration accessTtl, Duration refreshTtl, String frontendUrl, String mailFrom, List<String> corsOrigins) {
    public AuthProperties {
        if (jwtSecret == null || java.util.Base64.getDecoder().decode(jwtSecret).length < 32) throw new IllegalArgumentException("JWT_SECRET must be base64 of at least 32 random bytes");
        if (mailEncryptionKey == null || java.util.Base64.getDecoder().decode(mailEncryptionKey).length != 32) throw new IllegalArgumentException("MAIL_ENCRYPTION_KEY must be base64 of 32 random bytes");
        if (issuer == null || audience == null || frontendUrl == null || mailFrom == null) throw new IllegalArgumentException("Auth configuration is incomplete");
        if (accessTtl == null || accessTtl.isNegative() || accessTtl.isZero() || accessTtl.compareTo(Duration.ofMinutes(15)) > 0) throw new IllegalArgumentException("Access TTL must be between 0 and 15 minutes");
        if (refreshTtl == null || refreshTtl.isNegative() || refreshTtl.isZero() || refreshTtl.compareTo(Duration.ofDays(30)) > 0) throw new IllegalArgumentException("Refresh TTL must be between 0 and 30 days");
        if (corsOrigins == null || corsOrigins.isEmpty() || corsOrigins.stream().anyMatch(x -> x.contains("*"))) throw new IllegalArgumentException("Explicit CORS origins are required");
    }
}
