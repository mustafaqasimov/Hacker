package az.hacktrain.organization;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public final class OrganizationDtos {
    private OrganizationDtos() {}
    public enum Role { STUDENT, TEACHER, ORG_ADMIN }
    public enum Status { ACTIVE, INACTIVE }
    public record Create(@NotBlank @Size(max=120) String name) {}
    public record Rename(@NotBlank @Size(max=120) String name,@Min(0) long version) {}
    public record ChangeMember(@NotNull Role role,@NotNull Status status,@Min(0) long version) {}
    public record Invite(@NotBlank @Email @Size(max=254) String email,@NotNull Role role) {}
    public record AcceptInvite(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token) {}
    public record OrganizationView(UUID id,String name,boolean archived,long version,Instant createdAt) {}
    public record MemberView(UUID id,UUID organizationId,UUID userId,String role,String status,long version,Instant createdAt) {}
    public record GroupView(UUID id,String name,boolean archived,long version,Instant createdAt) {}
    public record InvitationView(UUID id,String email,String role,Instant expiresAt,Instant consumedAt,boolean revoked,Instant createdAt) {}
    public record PageResult<T>(List<T> items,int page,int size,long total) {}
}
