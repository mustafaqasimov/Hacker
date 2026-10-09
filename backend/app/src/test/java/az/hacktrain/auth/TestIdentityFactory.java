package az.hacktrain.auth;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
/** Test-only fixture uses the real registration and login flows. */
public class TestIdentityFactory {
    private final AuthService auth;
    private final JdbcTemplate jdbc;
    public TestIdentityFactory(AuthService auth,JdbcTemplate jdbc) { this.auth=auth; this.jdbc=jdbc; }
    public record Actor(UUID id,String email,String accessToken) {}
    public Actor create() {
        String email=UUID.randomUUID()+"@example.com"; String password="FixturePassword123!";
        auth.register(new AuthDtos.Register(email,password));
        var tokens=auth.login(new AuthDtos.Login(email,password));
        return new Actor(jdbc.queryForObject("select id from user_account where email=?",UUID.class,email),email,tokens.accessToken());
    }
}
