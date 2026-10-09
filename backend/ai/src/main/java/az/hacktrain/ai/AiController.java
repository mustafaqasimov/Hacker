package az.hacktrain.ai;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1")
public class AiController {
    private final AiService service;
    AiController(AiService service) { this.service=service; }
    @GetMapping("/ai/status") public AiDtos.Availability status() { return service.availability(); }
    @PostMapping("/organizations/{org}/courses/{course}/tasks/{task}/ai/hint")
    public AiDtos.Answer hint(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID course,@PathVariable UUID task,@Valid @RequestBody AiDtos.Ask request) {
        return service.hint(org,UUID.fromString(jwt.getSubject()),course,task,request);
    }
}
