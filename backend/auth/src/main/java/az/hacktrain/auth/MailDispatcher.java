package az.hacktrain.auth;
import org.springframework.mail.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
@Component
class MailDispatcher {
    @org.springframework.beans.factory.annotation.Value("${hacktrain.auth.email-confirmation-required:false}")
    private boolean confirmationRequired=true;
    private final MailRepository messages;
    private final MailSender sender;
    private final MailCipher cipher;
    private final AuthProperties props;
    private final Clock clock;
    MailDispatcher(MailRepository messages, MailSender sender, MailCipher cipher, AuthProperties props, Clock clock) {
        this.messages=messages; this.sender=sender; this.cipher=cipher; this.props=props; this.clock=clock;
    }
    @Scheduled(fixedDelayString="${hacktrain.mail.poll-ms:5000}")
    @Transactional
    public void dispatch() {
        for (var message : messages.lockDue()) {
            if(!confirmationRequired && message.subject.equals("HackTrain e-poçt təsdiqi")) { message.attempts=10; continue; }
            message.attempts++;
            try {
                var email=new SimpleMailMessage(); email.setFrom(props.mailFrom()); email.setTo(message.recipient);
                email.setSubject(message.subject); email.setText(cipher.decrypt(message.encryptedBody)); sender.send(email);
                message.sentAt=clock.instant(); message.encryptedBody=null;
            } catch (RuntimeException e) {
                // Never log exception text: SMTP errors may contain recipients or reset links.
                message.nextAttemptAt=clock.instant().plusSeconds(Math.min(3600, 30L << Math.min(message.attempts,7)));
            }
        }
    }
}
