package az.hacktrain.organization;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment,UUID> {
    boolean existsByOrganizationIdAndGroupIdAndMembershipId(UUID org,UUID group,UUID membership);
    void deleteByOrganizationIdAndGroupIdAndMembershipId(UUID org,UUID group,UUID membership);
    void deleteByOrganizationIdAndMembershipId(UUID org,UUID membership);
    @Query("select m from Membership m where m.organizationId=:org and exists(select t from TeacherAssignment t where t.organizationId=:org and t.groupId=:group and t.membershipId=m.id)")
    Page<Membership> teachers(UUID org,UUID group,Pageable page);
}
