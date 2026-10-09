package az.hacktrain.auth;
import org.junit.jupiter.api.Test;
import org.springframework.mail.*;
import java.time.*;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class MailDispatcherTest {
    @Test void successfulDeliveryErasesSensitivePayload() {
        var repo=mock(MailRepository.class); var sender=mock(MailSender.class); var cipher=new MailCipher(AuthCoreTest.props());
        var m=new MailMessage(); m.recipient="a@example.com"; m.subject="Verify"; m.encryptedBody=cipher.encrypt("secret-link");
        when(repo.lockDue()).thenReturn(List.of(m)); new MailDispatcher(repo,sender,cipher,AuthCoreTest.props(),Clock.systemUTC()).dispatch();
        assertThat(m.sentAt).isNotNull(); assertThat(m.encryptedBody).isNull(); assertThat(m.attempts).isEqualTo(1); verify(sender).send(any(SimpleMailMessage.class));
    }
    @Test void smtpFailureRetainsEncryptedPayloadAndSchedulesRetry() {
        var repo=mock(MailRepository.class); var sender=mock(MailSender.class); var cipher=new MailCipher(AuthCoreTest.props());
        var m=new MailMessage(); m.recipient="a@example.com"; m.subject="Verify"; m.encryptedBody=cipher.encrypt("secret-link");
        when(repo.lockDue()).thenReturn(List.of(m)); doThrow(new MailSendException("unavailable")).when(sender).send(any(SimpleMailMessage.class));
        var now=Instant.now(); new MailDispatcher(repo,sender,cipher,AuthCoreTest.props(),Clock.fixed(now,ZoneOffset.UTC)).dispatch();
        assertThat(m.sentAt).isNull(); assertThat(m.encryptedBody).isNotNull(); assertThat(m.nextAttemptAt).isEqualTo(now.plusSeconds(60));
    }
}
