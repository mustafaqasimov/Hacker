package az.hacktrain.organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="group_student")
class GroupStudent {
    @Id UUID id;
     UUID organizationId;
     UUID groupId;
     UUID membershipId;
    protected GroupStudent() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getGroupId() { return groupId; }
    public UUID getMembershipId() { return membershipId; }
}
