package az.hacktrain.organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="org_membership")
class Membership {
    @Id UUID id;
     UUID organizationId;
     UUID userId;
     String role;
     String status;
    @Version long version;
     Instant createdAt;
    protected Membership() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getUserId() { return userId; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
