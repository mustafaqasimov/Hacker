package az.hacktrain.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
interface AuthEventRepository extends JpaRepository<AuthEvent, UUID> {}
