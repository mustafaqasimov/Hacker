package az.hacktrain.course;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static az.hacktrain.course.CourseDtos.*;
@RestController @RequestMapping("/api/v1/organizations/{org}/courses")
public class CourseController {
    private final CourseService service;
    CourseController(CourseService service) { this.service=service; }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @GetMapping public PageResult<Summary> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.list(org,actor(jwt),page,size); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public View create(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@Valid @RequestBody Create dto) { return service.create(org,actor(jwt),dto); }
    @GetMapping("/{id}") public View get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id) { return service.get(org,actor(jwt),id); }
    @PutMapping("/{id}") public View update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id,@Valid @RequestBody Update dto) { return service.update(org,actor(jwt),id,dto); }
    @PostMapping("/{id}/publish") public View publish(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id,@Valid @RequestBody Version dto) { return service.publish(org,actor(jwt),id,dto); }
    @PostMapping("/{id}/archive") public View archive(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id,@Valid @RequestBody Version dto) { return service.archive(org,actor(jwt),id,dto); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id,@RequestParam long version) { service.delete(org,actor(jwt),id,version); }
    @GetMapping("/{id}/groups") public PageResult<Assignment> assignments(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.assignments(org,actor(jwt),id,page,size); }
    @PutMapping("/{id}/groups/{group}") @ResponseStatus(HttpStatus.NO_CONTENT) public void assign(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id,@PathVariable UUID group) { service.assign(org,actor(jwt),id,group,true); }
    @DeleteMapping("/{id}/groups/{group}") @ResponseStatus(HttpStatus.NO_CONTENT) public void unassign(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID id,@PathVariable UUID group) { service.assign(org,actor(jwt),id,group,false); }
}
