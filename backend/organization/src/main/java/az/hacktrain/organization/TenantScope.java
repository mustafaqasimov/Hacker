package az.hacktrain.organization;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;
@Component
class TenantScope {
    private final EntityManager em;
    TenantScope(EntityManager em) { this.em=em; }
    void actor(UUID actor) { set("app.actor_id",actor.toString()); set("app.organization_id",""); set("app.invitation_digest",""); }
    void organization(UUID org) { set("app.organization_id",org.toString()); }
    void invitation(String digest) { set("app.invitation_digest",digest); }
    private void set(String key,String value) {
        if(!TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Tenant scope requires a transaction");
        em.createNativeQuery("select set_config(:key,:value,true)").setParameter("key",key).setParameter("value",value).getSingleResult();
    }
}
