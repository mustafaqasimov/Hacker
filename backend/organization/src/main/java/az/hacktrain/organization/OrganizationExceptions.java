package az.hacktrain.organization;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
@RestControllerAdvice
@Order(0)
class OrganizationExceptions {
    @ExceptionHandler(OrganizationFailure.class)
    ResponseEntity<ProblemDetail> domain(OrganizationFailure e) {
        var p=ProblemDetail.forStatusAndDetail(e.status,e.getMessage()); p.setTitle(e.code); p.setType(URI.create("urn:hacktrain:problem:"+e.code));
        return ResponseEntity.status(e.status).cacheControl(CacheControl.noStore()).body(p);
    }
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> conflict(Exception e) { return domain(OrganizationFailure.conflict("Məlumat dəyişib və ya belə qeyd artıq mövcuddur.")); }
}
