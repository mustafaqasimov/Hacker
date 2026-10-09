package az.hacktrain.ai;
import az.hacktrain.course.CourseService;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@Component
class AiContext {
    private final CourseService courses;
    AiContext(CourseService courses) { this.courses=courses; }
    record LearningData(UUID taskId,List<String> learningObjectives,String studentExplanation,int allowedHintLevel,List<String> recentAttempts,List<String> approvedExplanations) {}
    LearningData load(UUID org,UUID actor,UUID course,UUID task,String explanation) {
        var visible=courses.get(org,actor,course);
        if(!visible.status().equals("PUBLISHED")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Publish the course before requesting AI help");
        var selected=visible.topics().stream().flatMap(t->t.tasks().stream()).filter(t->t.id().equals(task)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        // Explicit allowlist: do not serialize task entities or instructions into model context.
        return new LearningData(task,selected.objectives().stream().map(o->o.description()).toList(),explanation,1,List.of(),List.of());
    }
}
