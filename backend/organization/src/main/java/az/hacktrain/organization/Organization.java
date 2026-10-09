package az.hacktrain.organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="organization")
class Organization {
    @Id UUID id;
     String name;
     UUID createdBy;
     boolean archived;
    @Version long version;
     Instant createdAt;
    protected Organization() {}
    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getCreatedBy() { return createdBy; }
    public boolean getArchived() { return archived; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
