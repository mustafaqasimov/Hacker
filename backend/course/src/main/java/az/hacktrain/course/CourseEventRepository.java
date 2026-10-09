package az.hacktrain.course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
interface CourseEventRepository extends JpaRepository<CourseEvent,UUID> {}
