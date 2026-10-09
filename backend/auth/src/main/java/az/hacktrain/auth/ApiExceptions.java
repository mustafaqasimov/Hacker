package az.hacktrain.auth;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import java.net.URI;
@RestControllerAdvice
public class ApiExceptions {
    @ExceptionHandler(AuthFailure.class)
    ResponseEntity<ProblemDetail> auth(AuthFailure e) { return problem(e.status(),e.code(),e.getMessage()); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,jakarta.validation.ConstraintViolationException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> validation(Exception e) { return problem(HttpStatus.BAD_REQUEST,"invalid_request","Sorğunun formatını və sahələrini yoxlayın."); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> forbidden(Exception e) { return problem(HttpStatus.FORBIDDEN,"forbidden","İcazə yoxdur."); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception e) {
        if(e instanceof org.springframework.web.ErrorResponse error) return problem(HttpStatus.valueOf(error.getStatusCode().value()),"invalid_request","Sorğu emal edilə bilmədi.");
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,"internal_error","Sorğu emal edilə bilmədi.");
    }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String message) {
        var problem=ProblemDetail.forStatusAndDetail(status,message); problem.setTitle(code); problem.setType(URI.create("urn:hacktrain:problem:"+code));
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(problem);
    }
}
