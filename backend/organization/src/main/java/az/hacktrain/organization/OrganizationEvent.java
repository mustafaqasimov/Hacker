package az.hacktrain.organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="organization_event")
class OrganizationEvent {
    @Id UUID id;
     UUID organizationId;
     UUID actorId;
     UUID resourceId;
     UUID contextId;
     String eventType;
     Instant createdAt;
    protected OrganizationEvent() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getActorId() { return actorId; }
    public UUID getResourceId() { return resourceId; }
    public String getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }
}
