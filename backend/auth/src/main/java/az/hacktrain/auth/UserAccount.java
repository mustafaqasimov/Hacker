package az.hacktrain.auth;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="user_account")
class UserAccount {
    @Id UUID id;
     String email;
     String passwordHash;
     boolean emailVerified;
     boolean blocked;
     long tokenVersion;
     String platformRole;
     Instant createdAt;
    protected UserAccount() {}
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean getEmailVerified() { return emailVerified; }
    public boolean getBlocked() { return blocked; }
    public long getTokenVersion() { return tokenVersion; }
    public String getPlatformRole() { return platformRole; }
    public Instant getCreatedAt() { return createdAt; }
}
