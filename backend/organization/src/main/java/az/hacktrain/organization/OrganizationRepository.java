package az.hacktrain.organization;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
interface OrganizationRepository extends JpaRepository<Organization,UUID> {
    @Modifying
    @Query(value="insert into organization_creator_quota(user_id,created_count) values (:user,1) on conflict(user_id) do update set created_count=organization_creator_quota.created_count+1 where organization_creator_quota.created_count<3",nativeQuery=true)
    int reserveCreation(UUID user);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select o from Organization o where o.id=:id")
    Optional<Organization> lock(UUID id);
}
