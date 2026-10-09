package az.hacktrain.migration;
import org.flywaydb.core.Flyway;
import java.util.Map;
/** One-shot process: elevated migration credentials never enter the API container. */
public final class DatabaseMigration {
    private DatabaseMigration() {}
    public static void main(String[] args) { migrate(System.getenv()); }
    static int migrate(Map<String,String> env) {
        return Flyway.configure().dataSource(required(env,"DB_URL"),required(env,"DB_USER"),required(env,"DB_PASSWORD"))
            .locations("classpath:db/migration").load().migrate().migrationsExecuted;
    }
    static String required(Map<String,String> env,String key) {
        var value=env.get(key);if(value==null||value.isBlank())throw new IllegalArgumentException(key+" is required");return value;
    }
}
