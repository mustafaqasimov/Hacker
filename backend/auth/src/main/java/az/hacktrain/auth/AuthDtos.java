package az.hacktrain.auth;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}
    public record Register(@NotBlank @Email @Size(max=254) String email,
                           @NotBlank @Size(min=12,max=72) String password) {}
    public record Login(@NotBlank @Email @Size(max=254) String email,
                        @NotBlank @Size(max=72) String password) {}
    public record EmailRequest(@NotBlank @Email @Size(max=254) String email) {}
    public record TokenRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) {}
    public record ResetPassword(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
                                @NotBlank @Size(min=12,max=72) String password) {}
    public record ChangePassword(@NotBlank @Size(max=72) String currentPassword,
                                 @NotBlank @Size(min=12,max=72) String newPassword) {}
    public record Tokens(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
    public record Profile(UUID id, String email, boolean emailVerified, String platformRole, Instant createdAt) {}
    public record Accepted(String message) {
        public static Accepted generic() { return new Accepted("Sorğu qəbul edildi. Uyğun hesab varsa, e-poçt göndəriləcək."); }
    }
}
