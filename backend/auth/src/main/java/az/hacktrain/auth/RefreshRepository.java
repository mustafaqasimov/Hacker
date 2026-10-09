package az.hacktrain.auth;
import org.springframework.data.jpa.repository.*;
import java.util.*;
interface RefreshRepository extends JpaRepository<RefreshSession, UUID> {
    Optional<RefreshSession> findByTokenHash(String hash);
    @Modifying
    @Query("update RefreshSession s set s.revoked=true where s.familyId=:family")
    int revokeFamily(UUID family);
    @Modifying
    @Query("update RefreshSession s set s.revoked=true where s.userId=:user")
    int revokeUser(UUID user);
}
