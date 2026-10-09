package az.hacktrain.organization;
import az.hacktrain.app.HackTrainApplication;
import az.hacktrain.auth.TestIdentityFactory;
import az.hacktrain.auth.TestIdentityFactory.Actor;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static az.hacktrain.organization.OrganizationDtos.*;

@Testcontainers
@SpringBootTest(classes=HackTrainApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test","dev"})
@Import(TestIdentityFactory.class)
class OrganizationFlowIT {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine").withInitScript("runtime-role.sql");
    @Container static GenericContainer<?> redis=new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl); r.add("spring.datasource.username",()->"hacktrain_test_runtime"); r.add("spring.datasource.password",()->"only-for-ephemeral-testcontainers");
        r.add("spring.flyway.url",postgres::getJdbcUrl); r.add("spring.flyway.user",postgres::getUsername); r.add("spring.flyway.password",postgres::getPassword);
        r.add("spring.data.redis.url",()->"redis://"+redis.getHost()+":"+redis.getMappedPort(6379));
        r.add("hacktrain.rate-limit.redis-uri",()->"redis://"+redis.getHost()+":"+redis.getMappedPort(6379));
        r.add("hacktrain.auth.jwt-secret",()->Base64.getEncoder().encodeToString(new byte[32]));
        r.add("hacktrain.auth.cors-origins",()->"http://localhost:8080"); r.add("management.server.port",()->"0");
    }
    @Autowired az.hacktrain.course.CourseService courses;
    @Autowired OrganizationService service; @Autowired TestIdentityFactory accounts;
    @Autowired JdbcTemplate jdbc; @Autowired PlatformTransactionManager manager; @Autowired TenantScope scope;
    @Autowired TestRestTemplate http; @Autowired org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @BeforeEach void resetLimits() { redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Object>)c->{c.serverCommands().flushDb();return null;}); }
    OrganizationView create(Actor owner) { return service.create(owner.id(),new Create("Academy "+UUID.randomUUID())); }
    MemberView join(OrganizationView org,Actor admin,Actor invited,Role role) {
        var invitation=service.invite(org.id(),admin.id(),new Invite(invited.email(),role));
        return service.accept(invited.id(),invitation.token());
    }
    MemberView ownerMembership(OrganizationView org,Actor owner) { return service.members(org.id(),owner.id(),0,100).items().stream().filter(m->m.userId().equals(owner.id())).findFirst().orElseThrow(); }
    ResponseEntity<String> request(Actor actor,HttpMethod method,String path,Object body) {
        var headers=new HttpHeaders(); headers.setBearerAuth(actor.accessToken()); headers.setContentType(MediaType.APPLICATION_JSON);
        return http.exchange("/api/v1"+path,method,new HttpEntity<>(body,headers),String.class);
    }
    az.hacktrain.course.CourseDtos.Update courseContent(long version) {
        return new az.hacktrain.course.CourseDtos.Update(version,"SQL injection","Evidence based training",az.hacktrain.course.CourseDtos.Difficulty.BEGINNER,
            List.of(new az.hacktrain.course.CourseDtos.TopicInput("SQLi",List.of(new az.hacktrain.course.CourseDtos.TaskInput("Choose entry point","Explain which parameter reaches a query",az.hacktrain.course.CourseDtos.Difficulty.BEGINNER,
                List.of(new az.hacktrain.course.CourseDtos.ObjectiveInput("SQLI_ENTRY","Identify the entry point")))))));
    }
    az.hacktrain.course.CourseDtos.View createCourse(OrganizationView org,Actor owner) {
        return courses.create(org.id(),owner.id(),new az.hacktrain.course.CourseDtos.Create("SQL injection","Evidence based training",az.hacktrain.course.CourseDtos.Difficulty.BEGINNER));
    }
    @Test void courseDraftPublishAssignmentAndStudentVisibility() {
        var owner=accounts.create();var student=accounts.create();var org=create(owner);var member=join(org,owner,student,Role.STUDENT);
        var group=service.createGroup(org.id(),owner.id(),new Create("Students"));service.assignStudent(org.id(),owner.id(),group.id(),member.id(),true);
        var draft=createCourse(org,owner);var path="/organizations/"+org.id()+"/courses/"+draft.id();
        assertThat(courses.list(org.id(),student.id(),0,20).total()).isZero();
        assertThat(request(student,HttpMethod.GET,path,null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(owner,HttpMethod.POST,path+"/publish",new az.hacktrain.course.CourseDtos.Version(draft.version())).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var updated=courses.update(org.id(),owner.id(),draft.id(),courseContent(draft.version()));
        assertThat(updated.topics().getFirst().tasks().getFirst().objectives().getFirst().skillCode()).isEqualTo("SQLI_ENTRY");
        assertThat(courses.get(org.id(),owner.id(),draft.id()).version()).isEqualTo(updated.version());
        var twice=courses.update(org.id(),owner.id(),draft.id(),courseContent(updated.version()));
        assertThat(twice.version()).isGreaterThan(updated.version());
        assertThat(courses.get(org.id(),owner.id(),draft.id()).version()).isEqualTo(twice.version());
        assertThat(request(owner,HttpMethod.PUT,path,courseContent(updated.version())).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var published=courses.publish(org.id(),owner.id(),draft.id(),new az.hacktrain.course.CourseDtos.Version(twice.version()));
        assertThat(courses.list(org.id(),student.id(),0,20).total()).isZero();
        assertThat(request(owner,HttpMethod.PUT,path+"/groups/"+group.id(),null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        courses.assign(org.id(),owner.id(),draft.id(),group.id(),true);
        assertThat(courses.assignments(org.id(),owner.id(),draft.id(),0,20).total()).isEqualTo(1);
        assertThat(courses.list(org.id(),student.id(),0,20).items()).extracting(az.hacktrain.course.CourseDtos.Summary::id).containsExactly(draft.id());
        assertThat(request(student,HttpMethod.GET,path,null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(student,HttpMethod.GET,path+"/groups",null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(request(owner,HttpMethod.PUT,path,courseContent(published.version())).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(request(owner,HttpMethod.DELETE,path+"?version="+published.version(),null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(request(owner,HttpMethod.DELETE,path+"/groups/"+group.id(),null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(student,HttpMethod.GET,path,null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        courses.assign(org.id(),owner.id(),draft.id(),group.id(),true);
        service.archiveGroup(org.id(),owner.id(),group.id());
        assertThat(courses.list(org.id(),student.id(),0,20).total()).isZero();
        assertThat(request(student,HttpMethod.GET,path,null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test void courseTenantIsolationStudentWriteProtectionAndDraftDeletion() {
        var owner=accounts.create();var outsider=accounts.create();var student=accounts.create();var org=create(owner);join(org,owner,student,Role.STUDENT);
        var draft=createCourse(org,owner);var path="/organizations/"+org.id()+"/courses";
        assertThat(request(outsider,HttpMethod.GET,path,null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(outsider,HttpMethod.GET,path+"/"+draft.id(),null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(student,HttpMethod.POST,path,new az.hacktrain.course.CourseDtos.Create("forbidden","",az.hacktrain.course.CourseDtos.Difficulty.BEGINNER)).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(request(owner,HttpMethod.GET,path+"?page=-1",null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(owner,HttpMethod.GET,path+"?size=101",null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(owner,HttpMethod.GET,path+"/"+UUID.randomUUID(),null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(owner,HttpMethod.DELETE,path+"/"+draft.id()+"?version=9",null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(request(owner,HttpMethod.DELETE,path+"/"+draft.id()+"?version=0",null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(courses.list(org.id(),owner.id(),0,20).total()).isZero();
        var tx=new TransactionTemplate(manager);
        tx.executeWithoutResult(t->{scope.actor(owner.id());scope.organization(org.id());assertThat(jdbc.queryForObject("select count(*) from course_event where course_id=?",Integer.class,draft.id())).isEqualTo(2);assertThat(jdbc.update("delete from course_event where course_id=?",draft.id())).isZero();});
        assertThat(jdbc.queryForObject("select count(*) from course",Integer.class)).isZero();
    }
    @Test void teacherCanEditCatalogButAssignOnlyOwnGroupsAndCannotCrossTenants() {
        var owner=accounts.create();var teacher=accounts.create();var outsider=accounts.create();var org=create(owner);var other=create(outsider);var member=join(org,owner,teacher,Role.TEACHER);
        var assigned=service.createGroup(org.id(),owner.id(),new Create("Assigned"));var hidden=service.createGroup(org.id(),owner.id(),new Create("Other"));var foreign=service.createGroup(other.id(),outsider.id(),new Create("Foreign"));
        service.assignTeacher(org.id(),owner.id(),assigned.id(),member.id(),true);
        var draft=createCourse(org,teacher);var updated=courses.update(org.id(),teacher.id(),draft.id(),courseContent(0));var path="/organizations/"+org.id()+"/courses/"+draft.id();
        assertThat(request(teacher,HttpMethod.PUT,path+"/groups/"+assigned.id(),null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        var published=courses.publish(org.id(),teacher.id(),draft.id(),new az.hacktrain.course.CourseDtos.Version(updated.version()));
        assertThat(request(teacher,HttpMethod.PUT,path+"/groups/"+hidden.id(),null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(owner,HttpMethod.PUT,path+"/groups/"+foreign.id(),null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(teacher,HttpMethod.PUT,path+"/groups/"+assigned.id(),null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(teacher,HttpMethod.POST,path+"/archive",new az.hacktrain.course.CourseDtos.Version(published.version())).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(teacher,HttpMethod.PUT,path+"/groups/"+assigned.id(),null).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        service.archive(org.id(),owner.id());
        assertThat(request(teacher,HttpMethod.POST,"/organizations/"+org.id()+"/courses",new az.hacktrain.course.CourseDtos.Create("blocked","",az.hacktrain.course.CourseDtos.Difficulty.BEGINNER)).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
    @Test void courseNestedInputValidationAndDuplicateObjectiveRollback() {
        var owner=accounts.create();var org=create(owner);var draft=createCourse(org,owner);var path="/organizations/"+org.id()+"/courses/"+draft.id();
        var duplicate=new az.hacktrain.course.CourseDtos.Update(0,"Title","",az.hacktrain.course.CourseDtos.Difficulty.BEGINNER,List.of(new az.hacktrain.course.CourseDtos.TopicInput("Topic",List.of(new az.hacktrain.course.CourseDtos.TaskInput("Task","Instruction",az.hacktrain.course.CourseDtos.Difficulty.BEGINNER,List.of(new az.hacktrain.course.CourseDtos.ObjectiveInput("SKILL","One"),new az.hacktrain.course.CourseDtos.ObjectiveInput("SKILL","Two")))))));
        assertThat(request(owner,HttpMethod.PUT,path,duplicate).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(courses.get(org.id(),owner.id(),draft.id()).topics()).isEmpty();
        var invalidTask=Map.of("title","Task","instructions","","difficulty","BEGINNER","objectives",List.of());
        var invalidBody=Map.of("version",0,"title","Title","description","","difficulty","BEGINNER","topics",List.of(Map.of("title","Topic","tasks",List.of(invalidTask))));
        assertThat(request(owner,HttpMethod.PUT,path,invalidBody).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(owner,HttpMethod.POST,"/organizations/"+org.id()+"/courses",Map.of("title"," ","description","","difficulty","BEGINNER")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(owner,HttpMethod.GET,path+"/groups",null).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test void ownMembershipIsAvailableToStudentButNotOtherTenant() {
        var owner=accounts.create(); var student=accounts.create(); var outsider=accounts.create(); var org=create(owner);
        var membership=join(org,owner,student,Role.STUDENT);
        var response=request(student,HttpMethod.GET,"/organizations/"+org.id()+"/membership",null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains(membership.id().toString(),"STUDENT",student.id().toString()).doesNotContain(owner.id().toString());
        assertThat(request(outsider,HttpMethod.GET,"/organizations/"+org.id()+"/membership",null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        service.removeMember(org.id(),owner.id(),membership.id());
        assertThat(request(student,HttpMethod.GET,"/organizations/"+org.id()+"/membership",null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
    @Test void creationMembershipAndOptimisticRename() {
        var owner=accounts.create(); var org=create(owner);
        assertThat(service.mine(owner.id(),0,20).items()).extracting(OrganizationView::id).containsExactly(org.id());
        assertThat(ownerMembership(org,owner).role()).isEqualTo("ORG_ADMIN");
        var renamed=service.rename(org.id(),owner.id(),new Rename("Renamed",org.version()));
        assertThat(renamed.name()).isEqualTo("Renamed"); assertThat(renamed.version()).isEqualTo(1);
        assertThatThrownBy(()->service.rename(org.id(),owner.id(),new Rename("Stale",0))).isInstanceOf(OrganizationFailure.class);
    }
    @Test void anotherTenantCannotReadOrMutateOrganizationsOrGroups() {
        var a=accounts.create(); var b=accounts.create(); var orgA=create(a); var orgB=create(b);
        var group=service.createGroup(orgA.id(),a.id(),new Create("A group"));
        assertThat(request(b,HttpMethod.GET,"/organizations/"+orgA.id(),null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(b,HttpMethod.PATCH,"/organizations/"+orgA.id(),new Rename("stolen",0)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(request(b,HttpMethod.GET,"/organizations/"+orgB.id()+"/groups/"+group.id(),null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(service.get(orgA.id(),a.id()).name()).isEqualTo(orgA.name());
    }
    @Test void rlsFiltersUnscopedQueriesAndDoesNotLeakThroughPool() {
        var a=accounts.create(); var b=accounts.create(); var orgA=create(a); var orgB=create(b);
        service.createGroup(orgA.id(),a.id(),new Create("Only A")); service.createGroup(orgB.id(),b.id(),new Create("Only B"));
        assertThat(jdbc.queryForObject("select rolsuper or rolbypassrls from pg_roles where rolname=current_user",Boolean.class)).isFalse();
        var tx=new TransactionTemplate(manager);
        tx.executeWithoutResult(s->{scope.actor(a.id());scope.organization(orgA.id());assertThat(jdbc.queryForList("select name from study_group",String.class)).containsExactly("Only A");});
        tx.executeWithoutResult(s->assertThat(jdbc.queryForObject("select count(*) from study_group",Integer.class)).isZero());
        assertThatThrownBy(()->tx.executeWithoutResult(s->{scope.actor(a.id());scope.organization(orgA.id());jdbc.update("insert into study_group(id,organization_id,name) values (?,?,?)",UUID.randomUUID(),orgB.id(),"forbidden");})).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void compositeForeignKeyRejectsCrossTenantMembershipEvenInsideSelectedScope() {
        var a=accounts.create(); var b=accounts.create(); var orgA=create(a); var orgB=create(b);
        var group=service.createGroup(orgA.id(),a.id(),new Create("A")); var memberB=ownerMembership(orgB,b);
        var tx=new TransactionTemplate(manager);
        assertThatThrownBy(()->tx.executeWithoutResult(s->{scope.actor(a.id());scope.organization(orgA.id());jdbc.update("insert into group_student(id,organization_id,group_id,membership_id) values (?,?,?,?)",UUID.randomUUID(),orgA.id(),group.id(),memberB.id());})).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void lastAdminCannotBeRemovedOrDemoted() {
        var owner=accounts.create();var org=create(owner);var membership=ownerMembership(org,owner);
        assertThatThrownBy(()->service.removeMember(org.id(),owner.id(),membership.id())).isInstanceOf(OrganizationFailure.class);
        assertThatThrownBy(()->service.changeMember(org.id(),owner.id(),membership.id(),new ChangeMember(Role.TEACHER,Status.ACTIVE,membership.version()))).isInstanceOf(OrganizationFailure.class);
        assertThat(ownerMembership(org,owner).role()).isEqualTo("ORG_ADMIN");
    }
    @Test void concurrentAdminRemovalPreservesOneAdministrator() throws Exception {
        var a=accounts.create();var b=accounts.create();var org=create(a);var memberA=ownerMembership(org,a);var memberB=join(org,a,b,Role.ORG_ADMIN);
        var latch=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var one=pool.submit(()->{latch.await();try{service.removeMember(org.id(),a.id(),memberB.id());return true;}catch(OrganizationFailure e){return false;}});
            var two=pool.submit(()->{latch.await();try{service.removeMember(org.id(),b.id(),memberA.id());return true;}catch(OrganizationFailure e){return false;}});
            latch.countDown(); var results=List.of(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS));assertThat(results).containsExactlyInAnyOrder(true,false);
        }
    }
    @Test void invitationsAreEmailBoundSingleUseAndReplaceOlderLink() {
        var admin=accounts.create();var student=accounts.create();var stranger=accounts.create();var org=create(admin);
        var old=service.invite(org.id(),admin.id(),new Invite(student.email(),Role.STUDENT)).token();
        var current=service.invite(org.id(),admin.id(),new Invite(student.email(),Role.STUDENT)).token();
        assertThatThrownBy(()->service.accept(student.id(),old)).isInstanceOf(OrganizationFailure.class);
        assertThatThrownBy(()->service.accept(stranger.id(),current)).isInstanceOf(OrganizationFailure.class);
        var member=service.accept(student.id(),current);assertThat(member.role()).isEqualTo("STUDENT");
        assertThatThrownBy(()->service.accept(student.id(),current)).isInstanceOf(OrganizationFailure.class);
        assertThat(service.invitations(org.id(),admin.id(),0,20).items().toString()).doesNotContain(old,current,"tokenHash");
    }
    @Test void expiredAndRevokedInvitationsCannotJoin() {
        var admin=accounts.create();var invitee=accounts.create();var org=create(admin);
        var first=service.invite(org.id(),admin.id(),new Invite(invitee.email(),Role.TEACHER));
        service.revokeInvitation(org.id(),admin.id(),first.invitation().id());assertThatThrownBy(()->service.accept(invitee.id(),first.token())).isInstanceOf(OrganizationFailure.class);
        var second=service.invite(org.id(),admin.id(),new Invite(invitee.email(),Role.TEACHER));
        new TransactionTemplate(manager).executeWithoutResult(s->{scope.actor(admin.id());scope.organization(org.id());jdbc.update("update org_invitation set expires_at=now()-interval '1 second' where id=?",second.invitation().id());});
        var expired=second.token();
        assertThatThrownBy(()->service.accept(invitee.id(),expired)).isInstanceOf(OrganizationFailure.class);
    }
    @Test void teacherSeesOnlyAssignedGroupsAndStudentsCannotManageRoles() {
        var admin=accounts.create();var teacher=accounts.create();var student=accounts.create();var org=create(admin);
        var tm=join(org,admin,teacher,Role.TEACHER);var sm=join(org,admin,student,Role.STUDENT);
        var one=service.createGroup(org.id(),admin.id(),new Create("Group one"));var two=service.createGroup(org.id(),admin.id(),new Create("Group two"));
        service.assignTeacher(org.id(),admin.id(),one.id(),tm.id(),true);service.assignStudent(org.id(),admin.id(),one.id(),sm.id(),true);
        assertThat(service.groups(org.id(),teacher.id(),0,20).items()).extracting(GroupView::id).containsExactly(one.id());
        assertThat(service.group(org.id(),student.id(),one.id()).id()).isEqualTo(one.id());
        assertThat(service.roster(org.id(),teacher.id(),one.id(),0,20,false).items()).extracting(MemberView::id).containsExactly(sm.id());
        assertThat(service.roster(org.id(),admin.id(),one.id(),0,20,true).items()).extracting(MemberView::id).containsExactly(tm.id());
        assertThatThrownBy(()->service.group(org.id(),teacher.id(),two.id())).isInstanceOf(OrganizationFailure.class);
        assertThatThrownBy(()->service.members(org.id(),teacher.id(),0,20)).isInstanceOf(OrganizationFailure.class);
        assertThatThrownBy(()->service.changeMember(org.id(),student.id(),sm.id(),new ChangeMember(Role.ORG_ADMIN,Status.ACTIVE,0))).isInstanceOf(OrganizationFailure.class);
        assertThatThrownBy(()->service.roster(org.id(),student.id(),one.id(),0,20,false)).isInstanceOf(OrganizationFailure.class);
        service.assignStudent(org.id(),admin.id(),one.id(),sm.id(),false);service.assignTeacher(org.id(),admin.id(),one.id(),tm.id(),false);
        assertThat(service.groups(org.id(),teacher.id(),0,20).items()).isEmpty();assertThat(service.groups(org.id(),student.id(),0,20).items()).isEmpty();
    }
    @Test void membershipDeactivationRevokesGroupAccessAndReinvitationWorks() {
        var admin=accounts.create();var teacher=accounts.create();var org=create(admin);var member=join(org,admin,teacher,Role.TEACHER);
        var group=service.createGroup(org.id(),admin.id(),new Create("Group"));service.assignTeacher(org.id(),admin.id(),group.id(),member.id(),true);
        var changed=service.changeMember(org.id(),admin.id(),member.id(),new ChangeMember(Role.TEACHER,Status.INACTIVE,member.version()));
        assertThat(changed.status()).isEqualTo("INACTIVE");assertThatThrownBy(()->service.get(org.id(),teacher.id())).isInstanceOf(OrganizationFailure.class);
        join(org,admin,teacher,Role.TEACHER);assertThat(service.groups(org.id(),teacher.id(),0,20).items()).isEmpty();
    }
    @Test void groupUniquenessArchiveAndVersionConflict() {
        var admin=accounts.create();var org=create(admin);var group=service.createGroup(org.id(),admin.id(),new Create("Named"));
        assertThatThrownBy(()->service.createGroup(org.id(),admin.id(),new Create("named"))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(service.renameGroup(org.id(),admin.id(),group.id(),new Rename("New name",0)).version()).isEqualTo(1);
        assertThatThrownBy(()->service.renameGroup(org.id(),admin.id(),group.id(),new Rename("Stale",0))).isInstanceOf(OrganizationFailure.class);
        service.archiveGroup(org.id(),admin.id(),group.id());service.archiveGroup(org.id(),admin.id(),group.id());
        assertThatThrownBy(()->service.renameGroup(org.id(),admin.id(),group.id(),new Rename("Archived",2))).isInstanceOf(OrganizationFailure.class);
        service.archive(org.id(),admin.id());service.archive(org.id(),admin.id());
        assertThat(service.get(org.id(),admin.id()).archived()).isTrue();assertThatThrownBy(()->service.createGroup(org.id(),admin.id(),new Create("No"))).isInstanceOf(OrganizationFailure.class);
    }
    @Test void invalidAssignmentsAndBoundsFailWithoutTenantLeakage() {
        var admin=accounts.create();var org=create(admin);var group=service.createGroup(org.id(),admin.id(),new Create("Group"));var am=ownerMembership(org,admin);
        assertThatThrownBy(()->service.assignStudent(org.id(),admin.id(),group.id(),am.id(),true)).isInstanceOf(OrganizationFailure.class);
        assertThatThrownBy(()->service.groups(org.id(),admin.id(),0,101)).isInstanceOf(OrganizationFailure.class);
        create(admin);create(admin);assertThatThrownBy(()->create(admin)).isInstanceOf(OrganizationFailure.class);
    }
    @Test void auditRowsAreAppendOnlyForRuntimeDatabaseRole() {
        var admin=accounts.create();var org=create(admin);
        new TransactionTemplate(manager).executeWithoutResult(s->{scope.actor(admin.id());scope.organization(org.id());
            assertThat(jdbc.queryForObject("select count(*) from organization_event",Integer.class)).isPositive();
            assertThat(jdbc.update("delete from organization_event")).isZero();
            assertThat(jdbc.update("update organization_event set event_type='changed'")).isZero();
        });
    }
    @Test void httpControllerEnforcesContractsAndUsesProblemDetails() {
        var owner=accounts.create();
        var created=request(owner,HttpMethod.POST,"/organizations",new Create("HTTP academy"));assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var org=create(owner);var path="/organizations/"+org.id();
        assertThat(request(owner,HttpMethod.GET,"/organizations",null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(owner,HttpMethod.GET,"/organizations/not-a-uuid",null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(owner,HttpMethod.GET,path+"/members",null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(owner,HttpMethod.POST,path+"/groups",new Create("HTTP group")).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var duplicate=request(owner,HttpMethod.POST,path+"/groups",new Create("HTTP group"));assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);assertThat(duplicate.getHeaders().getContentType().toString()).contains("application/problem+json");
        assertThat(request(owner,HttpMethod.GET,path+"/groups?size=101",null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(request(owner,HttpMethod.POST,path+"/invitations",Map.of("email","invalid","role","SUPER_ADMIN")).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    UUID responseId(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return UUID.fromString(new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getBody()).get("id").asText());
    }
    String invitationToken(ResponseEntity<String> response) throws Exception {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getBody()).get("token").asText();
    }
    @Test void completeTeacherAndStudentAssignmentThroughHttp() throws Exception {
        var admin=accounts.create();var teacher=accounts.create();var student=accounts.create();var org=create(admin);String base="/organizations/"+org.id();
        assertThat(request(admin,HttpMethod.GET,base,null).getStatusCode()).isEqualTo(HttpStatus.OK);
        var teacherInvitation=request(admin,HttpMethod.POST,base+"/invitations",new Invite(teacher.email(),Role.TEACHER));
        UUID teacherMember=responseId(request(teacher,HttpMethod.POST,"/invitations/accept",new AcceptInvite(invitationToken(teacherInvitation))));
        var studentInvitation=request(admin,HttpMethod.POST,base+"/invitations",new Invite(student.email(),Role.STUDENT));
        UUID studentMember=responseId(request(student,HttpMethod.POST,"/invitations/accept",new AcceptInvite(invitationToken(studentInvitation))));
        UUID group=responseId(request(admin,HttpMethod.POST,base+"/groups",new Create("HTTP assignments")));String gp=base+"/groups/"+group;
        assertThat(request(admin,HttpMethod.PUT,gp+"/teachers/"+teacherMember,null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(admin,HttpMethod.POST,gp+"/students/"+studentMember,null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(teacher,HttpMethod.GET,gp+"/students",null).getBody()).contains(studentMember.toString());
        assertThat(request(admin,HttpMethod.GET,gp+"/teachers",null).getBody()).contains(teacherMember.toString());
        assertThat(request(student,HttpMethod.GET,gp,null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(teacher,HttpMethod.GET,base+"/groups",null).getBody()).contains(group.toString());
        assertThat(request(admin,HttpMethod.PATCH,gp,new Rename("Renamed group",0)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(admin,HttpMethod.PATCH,base,new Rename("Renamed organization",0)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(admin,HttpMethod.GET,base+"/invitations",null).getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID unused=responseId(request(admin,HttpMethod.POST,base+"/invitations",new Invite("unused@example.com",Role.STUDENT)));
        assertThat(request(admin,HttpMethod.DELETE,base+"/invitations/"+unused,null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(admin,HttpMethod.DELETE,gp+"/students/"+studentMember,null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(admin,HttpMethod.DELETE,gp+"/teachers/"+teacherMember,null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(admin,HttpMethod.PATCH,base+"/members/"+teacherMember,new ChangeMember(Role.TEACHER,Status.INACTIVE,0)).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request(admin,HttpMethod.DELETE,base+"/members/"+studentMember,null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(admin,HttpMethod.DELETE,gp,null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(request(admin,HttpMethod.POST,base+"/archive",null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test void creationQuotaCannotBeBypassedByLeavingOwnedOrganization() {
        var creator=accounts.create();var replacement=accounts.create();var one=create(creator);var creatorMember=ownerMembership(one,creator);
        join(one,creator,replacement,Role.ORG_ADMIN);service.removeMember(one.id(),replacement.id(),creatorMember.id());
        create(creator);create(creator);
        assertThatThrownBy(()->create(creator)).isInstanceOf(OrganizationFailure.class);
    }
}
