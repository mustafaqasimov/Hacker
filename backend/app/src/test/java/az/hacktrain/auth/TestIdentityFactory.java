package az.hacktrain.auth;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
/** Test-only fixture uses the real registration and verification flows. */
public class TestIdentityFactory {
    private final AuthService auth;
    private final JdbcTemplate jdbc;
    private final MailCipher cipher;
    public TestIdentityFactory(AuthService auth,JdbcTemplate jdbc,MailCipher cipher) { this.auth=auth; this.jdbc=jdbc; this.cipher=cipher; }
    public record Actor(UUID id,String email,String accessToken) {}
    public Actor create() {
        String email=UUID.randomUUID()+"@example.com"; String password="FixturePassword123!";
        auth.register(new AuthDtos.Register(email,password)); auth.verifyEmail(latestToken(email));
        var tokens=auth.login(new AuthDtos.Login(email,password));
        return new Actor(jdbc.queryForObject("select id from user_account where email=?",UUID.class,email),email,tokens.accessToken());
    }
    public String latestToken(String email) {
        String encrypted=jdbc.queryForObject("select encrypted_body from auth_mail_outbox where recipient=? order by created_at desc limit 1",String.class,email);
        return cipher.decrypt(encrypted).split("#token=")[1];
    }
}
