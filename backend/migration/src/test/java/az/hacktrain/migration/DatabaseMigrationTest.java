package az.hacktrain.migration;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class DatabaseMigrationTest {
    @Test void missingCredentialsFailWithoutPrintingSecrets() {
        assertThrows(IllegalArgumentException.class,()->DatabaseMigration.required(Map.of(),"DB_PASSWORD"));
        assertThrows(IllegalArgumentException.class,()->DatabaseMigration.required(Map.of("DB_USER"," "),"DB_USER"));
        assertEquals("runtime",DatabaseMigration.required(Map.of("DB_USER","runtime"),"DB_USER"));
    }
}
