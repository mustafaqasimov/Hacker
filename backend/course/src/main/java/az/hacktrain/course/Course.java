package az.hacktrain.course;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
@Entity @Table(name="course")
class Course {
    @Id UUID id;
     UUID organizationId;
     String title;
     String description;
     String difficulty;
     String status;
    @Version long version;
    long contentRevision;
     UUID createdBy;
     Instant createdAt;
    @OneToMany(mappedBy="course",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("position ASC") List<CourseTopic> topics = new ArrayList<>();
    protected Course() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDifficulty() { return difficulty; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public List<CourseTopic> getTopics() { return topics; }
}
