package az.hacktrain.auth;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    AuthController(AuthService service) { this.service=service; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.Accepted register(@Valid @RequestBody AuthDtos.Register dto) { service.register(dto); return new AuthDtos.Accepted("Qeydiyyat sorğusu qəbul edildi. Hesabınızla daxil ola bilərsiniz."); }
    @PostMapping("/login")
    public AuthDtos.Tokens login(@Valid @RequestBody AuthDtos.Login dto) { return service.login(dto); }
    @PostMapping("/refresh")
    public AuthDtos.Tokens refresh(@Valid @RequestBody AuthDtos.TokenRequest dto) { return service.refresh(dto.token()); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthDtos.TokenRequest dto) { service.logout(dto.token()); }
    @PostMapping("/logout-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal Jwt jwt) { service.logoutAll(UUID.fromString(jwt.getSubject())); }
    @PostMapping("/verify-email") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@Valid @RequestBody AuthDtos.TokenRequest dto) { service.verifyEmail(dto.token()); }
    @PostMapping("/resend-verification") @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.Accepted resend(@Valid @RequestBody AuthDtos.EmailRequest dto) { service.requestAction(dto.email(),"VERIFY_EMAIL"); return AuthDtos.Accepted.generic(); }
    @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
    public AuthDtos.Accepted forgot(@Valid @RequestBody AuthDtos.EmailRequest dto) { service.requestAction(dto.email(),"RESET_PASSWORD"); return AuthDtos.Accepted.generic(); }
    @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody AuthDtos.ResetPassword dto) { service.resetPassword(dto); }
    @PostMapping("/change-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AuthDtos.ChangePassword dto) { service.changePassword(UUID.fromString(jwt.getSubject()),dto); }
    @GetMapping("/me")
    public AuthDtos.Profile me(@AuthenticationPrincipal Jwt jwt) { return service.profile(UUID.fromString(jwt.getSubject())); }
}
