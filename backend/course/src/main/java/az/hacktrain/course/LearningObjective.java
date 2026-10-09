package az.hacktrain.course;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
@Entity @Table(name="learning_objective")
class LearningObjective {
    @Id UUID id;
     UUID organizationId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="task_id") CourseTask task;
     String skillCode;
     String description;
     int position;
    protected LearningObjective() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public CourseTask getTask() { return task; }
    public String getSkillCode() { return skillCode; }
    public String getDescription() { return description; }
    public int getPosition() { return position; }
}
