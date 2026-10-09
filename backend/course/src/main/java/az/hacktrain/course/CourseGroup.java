package az.hacktrain.course;
import jakarta.persistence.*;
import java.util.*;
import java.time.Instant;
@Entity @Table(name="course_group")
class CourseGroup {
    @Id UUID id;
     UUID organizationId;
     UUID courseId;
     UUID groupId;
    protected CourseGroup() {}
    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getCourseId() { return courseId; }
    public UUID getGroupId() { return groupId; }
}
