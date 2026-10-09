package az.hacktrain.course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
interface CourseGroupRepository extends JpaRepository<CourseGroup,UUID> {
    boolean existsByOrganizationIdAndCourseIdAndGroupId(UUID org,UUID course,UUID group);
    void deleteByOrganizationIdAndCourseIdAndGroupId(UUID org,UUID course,UUID group);
    void deleteByOrganizationIdAndCourseId(UUID org,UUID course);
    Page<CourseGroup> findByOrganizationIdAndCourseId(UUID org,UUID course,Pageable page);
}
