package az.hacktrain.auth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
/** Public module boundary; no persistence entity escapes auth. */
@Service
public class IdentityAccess implements IdentityDirectory {
    private final UserRepository users;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    IdentityAccess(UserRepository users) { this.users=users; }
    @Transactional(readOnly=true)
    public Identity requireActive(UUID id) { return safe(users.findById(id).orElseThrow(AuthFailure::invalid)); }
    @Transactional
    public Identity lockActive(UUID id) { var user=users.lockById(id).orElseThrow(AuthFailure::invalid); em.refresh(user); return safe(user); }
    private Identity safe(UserAccount user) {
        if(user.blocked) throw AuthFailure.invalid();
        return new Identity(user.id,user.email);
    }
}
