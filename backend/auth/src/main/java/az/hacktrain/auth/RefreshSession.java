package az.hacktrain.auth;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="refresh_session")
class RefreshSession {
    @Id UUID id;
     UUID userId;
     UUID familyId;
     String tokenHash;
     Instant expiresAt;
     Instant usedAt;
     boolean revoked;
    protected RefreshSession() {}
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getFamilyId() { return familyId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public boolean getRevoked() { return revoked; }
}
