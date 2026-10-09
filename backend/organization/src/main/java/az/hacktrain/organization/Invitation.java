package az.hacktrain.organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="org_invitation")
class Invitation {
    @Id UUID id;
     UUID organizationId;
     String email;
     String role;
     String tokenHash;
     UUID createdBy;
     Instant expiresAt;
     Instant consumedAt;
     boolean revoked;
     Instant createdAt;
    protected Invitation() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public boolean getRevoked() { return revoked; }
    public Instant getCreatedAt() { return createdAt; }
}
