package az.hacktrain.auth;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="auth_event")
class AuthEvent {
    @Id UUID id;
     UUID userId;
     String eventType;
     Instant createdAt;
    protected AuthEvent() {}
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }
}
