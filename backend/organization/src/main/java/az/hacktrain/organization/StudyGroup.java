package az.hacktrain.organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="study_group")
class StudyGroup {
    @Id UUID id;
     UUID organizationId;
     String name;
     boolean archived;
    @Version long version;
     Instant createdAt;
    protected StudyGroup() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public boolean getArchived() { return archived; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
