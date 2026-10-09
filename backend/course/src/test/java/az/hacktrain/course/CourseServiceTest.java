package az.hacktrain.course;
import az.hacktrain.organization.OrganizationAccess;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import java.time.Clock;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class CourseServiceTest {
    @Test void publicationRequiresTopicsTasksAndObjectives() {
        var c=new Course();assertThatThrownBy(()->CourseService.validatePublication(c)).isInstanceOf(CourseFailure.class);
        var topic=new CourseTopic();c.topics.add(topic);assertThatThrownBy(()->CourseService.validatePublication(c)).isInstanceOf(CourseFailure.class);
        var task=new CourseTask();topic.tasks.add(task);assertThatThrownBy(()->CourseService.validatePublication(c)).isInstanceOf(CourseFailure.class);
        task.objectives.add(new LearningObjective());assertThatCode(()->CourseService.validatePublication(c)).doesNotThrowAnyException();
    }
    @Test void studentWriteFailsBeforeAnyCourseRepositoryAccess() {
        var org=UUID.randomUUID();var actor=UUID.randomUUID();var access=mock(OrganizationAccess.class);var courses=mock(CourseRepository.class);
        when(access.authorize(org,actor,true)).thenReturn(new OrganizationAccess.Access(UUID.randomUUID(),"STUDENT"));
        var service=new CourseService(courses,mock(CourseGroupRepository.class),mock(CourseEventRepository.class),access,mock(CourseMapper.class),mock(EntityManager.class),Clock.systemUTC());
        assertThatThrownBy(()->service.create(org,actor,new CourseDtos.Create("Course","",CourseDtos.Difficulty.BEGINNER))).isInstanceOf(CourseFailure.class);
        verifyNoInteractions(courses);
    }
}
