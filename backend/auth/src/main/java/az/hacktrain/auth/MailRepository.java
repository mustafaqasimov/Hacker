package az.hacktrain.auth;
import org.springframework.data.jpa.repository.*;
import java.util.*;
interface MailRepository extends JpaRepository<MailMessage, UUID> {
    @Query(value="select * from auth_mail_outbox where sent_at is null and next_attempt_at <= now() and attempts < 10 order by next_attempt_at for update skip locked limit 10", nativeQuery=true)
    List<MailMessage> lockDue();
}
