package az.hacktrain.organization;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
interface GroupStudentRepository extends JpaRepository<GroupStudent,UUID> {
    boolean existsByOrganizationIdAndGroupIdAndMembershipId(UUID org,UUID group,UUID membership);
    void deleteByOrganizationIdAndGroupIdAndMembershipId(UUID org,UUID group,UUID membership);
    void deleteByOrganizationIdAndMembershipId(UUID org,UUID membership);
    @Query("select m from Membership m where m.organizationId=:org and exists(select s from GroupStudent s where s.organizationId=:org and s.groupId=:group and s.membershipId=m.id)")
    Page<Membership> roster(UUID org,UUID group,Pageable page);
}
