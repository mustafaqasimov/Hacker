package az.hacktrain.auth;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Component
class AuthRetention {
    private final EntityManager em;
    private final Clock clock;
    AuthRetention(EntityManager em,Clock clock) { this.em=em; this.clock=clock; }
    @Scheduled(cron="0 15 3 * * *",zone="UTC")
    @Transactional
    public void purgeExpiredCredentials() {
        var now=clock.instant();
        // Keep used refresh hashes until family expiry so replay detection remains effective.
        em.createQuery("delete from RefreshSession s where s.expiresAt<:cutoff").setParameter("cutoff",now.minus(Duration.ofDays(1))).executeUpdate();
    }
}
