package az.hacktrain.course;
import az.hacktrain.organization.OrganizationAccess;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;
import static az.hacktrain.course.CourseDtos.*;
@Service @Transactional
public class CourseService {
    private final CourseRepository courses; private final CourseGroupRepository assignments; private final CourseEventRepository events;
    private final OrganizationAccess organizations; private final CourseMapper mapper; private final EntityManager em; private final Clock clock;
    CourseService(CourseRepository courses,CourseGroupRepository assignments,CourseEventRepository events,OrganizationAccess organizations,CourseMapper mapper,EntityManager em,Clock clock) {
        this.courses=courses;this.assignments=assignments;this.events=events;this.organizations=organizations;this.mapper=mapper;this.em=em;this.clock=clock;
    }
    public View create(UUID org,UUID actor,Create dto) {
        staff(org,actor); var c=new Course();c.id=UUID.randomUUID();c.organizationId=org;c.title=dto.title().strip();c.description=dto.description().strip();c.difficulty=dto.difficulty().name();c.status="DRAFT";c.createdBy=actor;c.createdAt=clock.instant();
        courses.saveAndFlush(c);event(c,actor,"COURSE_CREATED");return mapper.view(c);
    }
    @Transactional(readOnly=true)
    public PageResult<Summary> list(UUID org,UUID actor,int page,int size) {
        var access=organizations.authorize(org,actor,false);var result=courses.visible(org,access.membershipId(),access.staff(),page(page,size));
        return new PageResult<>(result.getContent().stream().map(mapper::summary).toList(),page,size,result.getTotalElements());
    }
    @Transactional(readOnly=true)
    public View get(UUID org,UUID actor,UUID id) {
        var access=organizations.authorize(org,actor,false);var c=find(org,id);
        if(!access.staff()&&(!c.status.equals("PUBLISHED")||!courses.enrolled(org,id,access.membershipId()))) throw CourseFailure.missing();
        return mapper.view(c);
    }
    public View update(UUID org,UUID actor,UUID id,Update dto) {
        staff(org,actor);var c=find(org,id);version(c,dto.version());draft(c);
        c.title=dto.title().strip();c.description=dto.description().strip();c.difficulty=dto.difficulty().name();
        // Remove old draft children before inserting replacements to avoid position constraint collisions.
        c.topics.clear();em.flush();
        for(var input:dto.topics()) {
            var topic=new CourseTopic();topic.id=UUID.randomUUID();topic.organizationId=org;topic.course=c;topic.title=input.title().strip();topic.position=c.topics.size();c.topics.add(topic);
            for(var taskInput:input.tasks()) {
                var task=new CourseTask();task.id=UUID.randomUUID();task.organizationId=org;task.topic=topic;task.title=taskInput.title().strip();task.instructions=taskInput.instructions().strip();task.difficulty=taskInput.difficulty().name();task.position=topic.tasks.size();topic.tasks.add(task);
                var skills=new HashSet<String>();
                for(var objective:taskInput.objectives()) {
                    if(!skills.add(objective.skillCode())) throw CourseFailure.conflict("Eyni tapşırıqda bacarıq kodu təkrarlana bilməz.");
                    var o=new LearningObjective();o.id=UUID.randomUUID();o.organizationId=org;o.task=task;o.skillCode=objective.skillCode();o.description=objective.description().strip();o.position=task.objectives.size();task.objectives.add(o);
                }
            }
        }
        // Collection-only edits must also advance the optimistic aggregate version.
        c.contentRevision++;em.flush();
        event(c,actor,"COURSE_UPDATED");return mapper.view(c);
    }
    public View publish(UUID org,UUID actor,UUID id,Version dto) {
        staff(org,actor);var c=find(org,id);version(c,dto.version());draft(c);validatePublication(c);
        c.status="PUBLISHED";em.flush();event(c,actor,"COURSE_PUBLISHED");return mapper.view(c);
    }
    public View archive(UUID org,UUID actor,UUID id,Version dto) {
        staff(org,actor);var c=find(org,id);version(c,dto.version());c.status="ARCHIVED";em.flush();event(c,actor,"COURSE_ARCHIVED");return mapper.view(c);
    }
    public void delete(UUID org,UUID actor,UUID id,long expectedVersion) {
        staff(org,actor);var c=find(org,id);version(c,expectedVersion);draft(c);assignments.deleteByOrganizationIdAndCourseId(org,id);courses.delete(c);event(c,actor,"COURSE_DELETED");
    }
    @Transactional(readOnly=true)
    public PageResult<Assignment> assignments(UUID org,UUID actor,UUID id,int page,int size) {
        if(!organizations.authorize(org,actor,false).staff()) throw CourseFailure.forbidden();find(org,id);
        var result=assignments.findByOrganizationIdAndCourseId(org,id,page(page,size));return new PageResult<>(result.getContent().stream().map(mapper::view).toList(),page,size,result.getTotalElements());
    }
    public void assign(UUID org,UUID actor,UUID id,UUID group,boolean add) {
        staff(org,actor);var c=find(org,id);organizations.requireActiveGroup(org,actor,group);
        if(add) {
            if(!c.status.equals("PUBLISHED")) throw CourseFailure.conflict("Yalnız yayımlanmış kurs qrupa təyin edilə bilər.");
            if(!assignments.existsByOrganizationIdAndCourseIdAndGroupId(org,id,group)) { var link=new CourseGroup();link.id=UUID.randomUUID();link.organizationId=org;link.courseId=id;link.groupId=group;assignments.save(link);event(c,actor,"COURSE_ASSIGNED"); }
        } else { assignments.deleteByOrganizationIdAndCourseIdAndGroupId(org,id,group);event(c,actor,"COURSE_UNASSIGNED"); }
    }
    static void validatePublication(Course c) {
        if(c.topics.isEmpty()||c.topics.stream().anyMatch(t->t.tasks.isEmpty()||t.tasks.stream().anyMatch(task->task.objectives.isEmpty()))) throw CourseFailure.conflict("Hər kursda mövzu, hər mövzuda tapşırıq, hər tapşırıqda öyrənmə məqsədi olmalıdır.");
    }
    private void staff(UUID org,UUID actor) { if(!organizations.authorize(org,actor,true).staff()) throw CourseFailure.forbidden(); }
    private Course find(UUID org,UUID id) { return courses.findByOrganizationIdAndId(org,id).orElseThrow(CourseFailure::missing); }
    private void draft(Course c) { if(!c.status.equals("DRAFT")) throw CourseFailure.conflict("Yalnız qaralama kurs redaktə və ya silinə bilər."); }
    private void version(Course c,long expected) { if(c.version!=expected) throw CourseFailure.conflict("Kurs dəyişib. Səhifəni yeniləyin."); }
    private Pageable page(int page,int size) { if(page<0||page>10000||size<1||size>100) throw new CourseFailure(org.springframework.http.HttpStatus.BAD_REQUEST,"Səhifə həddi etibarsızdır.");return PageRequest.of(page,size); }
    private void event(Course c,UUID actor,String type) { var e=new CourseEvent();e.id=UUID.randomUUID();e.organizationId=c.organizationId;e.actorId=actor;e.courseId=c.id;e.eventType=type;e.createdAt=clock.instant();events.save(e); }
}
