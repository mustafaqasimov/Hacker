package az.hacktrain.migration;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
@Testcontainers
class DatabaseMigrationIT {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @Test void migrationsAreRepeatableWithoutReapplyingVersions() {
        var env=Map.of("DB_URL",postgres.getJdbcUrl(),"DB_USER",postgres.getUsername(),"DB_PASSWORD",postgres.getPassword());
        assertEquals(3,DatabaseMigration.migrate(env));assertEquals(0,DatabaseMigration.migrate(env));
    }
}
