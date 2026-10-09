package az.hacktrain.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
interface InvitationRepository extends JpaRepository<Invitation,UUID> {
    Optional<Invitation> findByTokenHash(String hash);
    Optional<Invitation> findByOrganizationIdAndId(UUID org,UUID id);
    List<Invitation> findByOrganizationIdAndEmailAndRevokedFalseAndConsumedAtIsNull(UUID org,String email);
    Page<Invitation> findByOrganizationId(UUID org,Pageable page);
}
