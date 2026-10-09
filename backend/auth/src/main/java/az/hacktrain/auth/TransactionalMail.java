package az.hacktrain.auth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.UUID;
/** Shared durable email delivery boundary until the notification module is introduced. */
@Service
public class TransactionalMail implements MailOutbox {
    private final MailRepository repository;
    private final MailCipher cipher;
    private final Clock clock;
    public TransactionalMail(MailRepository repository,MailCipher cipher,Clock clock) { this.repository=repository; this.cipher=cipher; this.clock=clock; }
    @Transactional
    public void enqueue(String recipient,String subject,String body) {
        var message=new MailMessage(); message.id=UUID.randomUUID(); message.recipient=recipient;
        message.subject=subject; message.encryptedBody=cipher.encrypt(body); message.createdAt=clock.instant();
        message.nextAttemptAt=clock.instant(); repository.save(message);
    }
}
