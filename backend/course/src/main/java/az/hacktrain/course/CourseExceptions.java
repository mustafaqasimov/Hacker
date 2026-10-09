package az.hacktrain.course;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
@RestControllerAdvice @Order(0)
class CourseExceptions {
    @ExceptionHandler(CourseFailure.class)
    ResponseEntity<ProblemDetail> domain(CourseFailure failure) {
        var p=ProblemDetail.forStatusAndDetail(failure.status,failure.getMessage()); p.setTitle("course_error"); p.setType(URI.create("urn:hacktrain:problem:course_error"));
        return ResponseEntity.status(failure.status).cacheControl(CacheControl.noStore()).body(p);
    }
}
