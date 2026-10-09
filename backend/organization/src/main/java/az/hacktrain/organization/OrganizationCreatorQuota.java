package az.hacktrain.organization;
import jakarta.persistence.*;
import java.util.UUID;
@Entity
@Table(name="organization_creator_quota")
class OrganizationCreatorQuota {
    @Id UUID userId;
    int createdCount;
    protected OrganizationCreatorQuota() {}
}
