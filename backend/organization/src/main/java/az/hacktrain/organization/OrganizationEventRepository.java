package az.hacktrain.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
interface OrganizationEventRepository extends JpaRepository<OrganizationEvent,UUID> {}
