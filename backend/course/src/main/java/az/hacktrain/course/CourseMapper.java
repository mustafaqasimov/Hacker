package az.hacktrain.course;
import org.mapstruct.Mapper;
import static az.hacktrain.course.CourseDtos.*;
@Mapper(componentModel="spring",unmappedTargetPolicy=org.mapstruct.ReportingPolicy.ERROR)
interface CourseMapper {
    Summary summary(Course entity);
    View view(Course entity);
    TopicView view(CourseTopic entity);
    TaskView view(CourseTask entity);
    ObjectiveView view(LearningObjective entity);
    Assignment view(CourseGroup entity);
}
