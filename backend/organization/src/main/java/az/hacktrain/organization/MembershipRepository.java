package az.hacktrain.organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
interface MembershipRepository extends JpaRepository<Membership,UUID> {
    Optional<Membership> findByOrganizationIdAndUserId(UUID org,UUID user);
    Optional<Membership> findByOrganizationIdAndId(UUID org,UUID id);
    Page<Membership> findByUserIdAndStatus(UUID user,String status,Pageable page);
    Page<Membership> findByOrganizationId(UUID org,Pageable page);
    long countByOrganizationIdAndRoleAndStatus(UUID org,String role,String status);
}
