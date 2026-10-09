package az.hacktrain.course;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
@Entity @Table(name="course_event")
class CourseEvent {
    @Id UUID id;
     UUID organizationId;
     UUID actorId;
     UUID courseId;
     String eventType;
     Instant createdAt;
    protected CourseEvent() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getActorId() { return actorId; }
    public UUID getCourseId() { return courseId; }
    public String getEventType() { return eventType; }
    public Instant getCreatedAt() { return createdAt; }
}
