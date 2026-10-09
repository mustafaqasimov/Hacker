package az.hacktrain.auth;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="auth_mail_outbox")
class MailMessage {
    @Id UUID id;
     String recipient;
     String subject;
     String encryptedBody;
     int attempts;
     Instant nextAttemptAt;
     Instant sentAt;
     Instant createdAt;
    protected MailMessage() {}
    public UUID getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getEncryptedBody() { return encryptedBody; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
