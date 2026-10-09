package az.hacktrain.course;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;
interface CourseRepository extends JpaRepository<Course,UUID> {
    Optional<Course> findByOrganizationIdAndId(UUID org,UUID id);
    // Read projection across organization membership; mutations remain behind OrganizationAccess.
    @Query(value="""
      select c.* from course c where c.organization_id=:org and (:staff=true or
        (c.status='PUBLISHED' and exists(select 1 from course_group cg join study_group g on g.id=cg.group_id and g.organization_id=cg.organization_id
          join group_student gs on gs.group_id=g.id and gs.organization_id=g.organization_id
          where cg.course_id=c.id and cg.organization_id=:org and not g.archived and gs.membership_id=:member)))
      order by c.created_at,c.id
      """,countQuery="""
      select count(*) from course c where c.organization_id=:org and (:staff=true or
        (c.status='PUBLISHED' and exists(select 1 from course_group cg join study_group g on g.id=cg.group_id and g.organization_id=cg.organization_id
          join group_student gs on gs.group_id=g.id and gs.organization_id=g.organization_id
          where cg.course_id=c.id and cg.organization_id=:org and not g.archived and gs.membership_id=:member)))
      """,nativeQuery=true)
    Page<Course> visible(UUID org,UUID member,boolean staff,Pageable page);
    @Query(value="""
      select exists(select 1 from course_group cg join study_group g on g.id=cg.group_id and g.organization_id=cg.organization_id
        join group_student gs on gs.group_id=g.id and gs.organization_id=g.organization_id
        where cg.organization_id=:org and cg.course_id=:course and gs.membership_id=:member and not g.archived)
      """,nativeQuery=true)
    boolean enrolled(UUID org,UUID course,UUID member);
}
