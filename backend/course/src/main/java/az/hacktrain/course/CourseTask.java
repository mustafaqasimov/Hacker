package az.hacktrain.course;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
@Entity @Table(name="course_task")
class CourseTask {
    @Id UUID id;
     UUID organizationId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="topic_id") CourseTopic topic;
     String title;
     String instructions;
     String difficulty;
     int position;
    @OneToMany(mappedBy="task",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("position ASC") List<LearningObjective> objectives = new ArrayList<>();
    protected CourseTask() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public CourseTopic getTopic() { return topic; }
    public String getTitle() { return title; }
    public String getInstructions() { return instructions; }
    public String getDifficulty() { return difficulty; }
    public int getPosition() { return position; }
    public List<LearningObjective> getObjectives() { return objectives; }
}
