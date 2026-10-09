package az.hacktrain.course;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
@Entity @Table(name="course_topic")
class CourseTopic {
    @Id UUID id;
     UUID organizationId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="course_id") Course course;
     String title;
     int position;
    @OneToMany(mappedBy="topic",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("position ASC") List<CourseTask> tasks = new ArrayList<>();
    protected CourseTopic() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public Course getCourse() { return course; }
    public String getTitle() { return title; }
    public int getPosition() { return position; }
    public List<CourseTask> getTasks() { return tasks; }
}
