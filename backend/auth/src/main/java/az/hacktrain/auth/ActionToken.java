package az.hacktrain.auth;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="action_token")
class ActionToken {
    @Id UUID id;
     UUID userId;
     String purpose;
     String tokenHash;
     Instant expiresAt;
     Instant consumedAt;
    protected ActionToken() {}
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPurpose() { return purpose; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
}
