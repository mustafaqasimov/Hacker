package az.hacktrain.organization;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
interface GroupRepository extends JpaRepository<StudyGroup,UUID> {
    Optional<StudyGroup> findByOrganizationIdAndId(UUID org,UUID id);
    @Query("select g from StudyGroup g where g.organizationId=:org and (:admin=true or exists(select s from GroupStudent s where s.organizationId=:org and s.groupId=g.id and s.membershipId=:member) or exists(select t from TeacherAssignment t where t.organizationId=:org and t.groupId=g.id and t.membershipId=:member))")
    Page<StudyGroup> accessible(UUID org,UUID member,boolean admin,Pageable page);
}
