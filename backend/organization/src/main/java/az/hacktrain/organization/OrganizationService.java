package az.hacktrain.organization;

import az.hacktrain.auth.IdentityDirectory;
import az.hacktrain.auth.MailOutbox;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import static az.hacktrain.organization.OrganizationDtos.*;

@Service
@Transactional
public class OrganizationService implements OrganizationAccess {
    private final OrganizationRepository organizations;
    private final MembershipRepository members;
    private final GroupRepository groups;
    private final GroupStudentRepository students;
    private final TeacherAssignmentRepository teachers;
    private final InvitationRepository invitations;
    private final OrganizationEventRepository events;
    private final OrganizationMapper mapper;
    private final TenantScope scope;
    private final IdentityDirectory identities;
    private final MailOutbox mail;
    private final InvitationTokens tokens;
    private final Clock clock;
    private final EntityManager em;
    private final String frontend;
    private final OrganizationQuota quota;
    OrganizationService(OrganizationRepository organizations,MembershipRepository members,GroupRepository groups,
            GroupStudentRepository students,TeacherAssignmentRepository teachers,InvitationRepository invitations,
            OrganizationEventRepository events,OrganizationMapper mapper,TenantScope scope,IdentityDirectory identities,
            MailOutbox mail,InvitationTokens tokens,Clock clock,EntityManager em,OrganizationQuota quota,
            @Value("${hacktrain.auth.frontend-url}") String frontend) {
        this.organizations=organizations; this.members=members; this.groups=groups; this.students=students;
        this.teachers=teachers; this.invitations=invitations; this.events=events; this.mapper=mapper;
        this.scope=scope; this.identities=identities; this.mail=mail; this.tokens=tokens; this.clock=clock; this.em=em;
        this.frontend=frontend; this.quota=quota;
    }
    public OrganizationView create(UUID actor,Create dto) {
        scope.actor(actor); identities.lockActive(actor);
        if(organizations.reserveCreation(actor)==0) throw OrganizationFailure.conflict("Təşkilat yaratma limiti keçilib.");
        var org=new Organization(); org.id=UUID.randomUUID(); org.name=dto.name().strip(); org.createdBy=actor; org.createdAt=clock.instant();
        scope.organization(org.id); organizations.saveAndFlush(org);
        var member=new Membership(); member.id=UUID.randomUUID(); member.organizationId=org.id; member.userId=actor;
        member.role="ORG_ADMIN"; member.status="ACTIVE"; member.createdAt=clock.instant(); members.saveAndFlush(member);
        event(org.id,actor,org.id,"ORGANIZATION_CREATED"); return mapper.view(org);
    }
    @Transactional(readOnly=true)
    public PageResult<OrganizationView> mine(UUID actor,int page,int size) {
        scope.actor(actor);
        var memberships=members.findByUserIdAndStatus(actor,"ACTIVE",page(page,size));
        return new PageResult<>(memberships.getContent().stream().map(m->mapper.view(organizations.findById(m.organizationId).orElseThrow(OrganizationFailure::missing))).toList(),page,size,memberships.getTotalElements());
    }
    @Transactional(readOnly=true)
    public OrganizationView get(UUID org,UUID actor) { return mapper.view(enter(org,actor,false,true).organization); }
    @Transactional(readOnly=true)
    public MemberView membership(UUID org,UUID actor) { return mapper.view(enter(org,actor,false,true).member); }
    public OrganizationView rename(UUID org,UUID actor,Rename dto) {
        var context=admin(org,actor,false); checkVersion(context.organization.version,dto.version());
        context.organization.name=dto.name().strip(); em.flush(); event(org,actor,org,"ORGANIZATION_RENAMED"); return mapper.view(context.organization);
    }
    public void archive(UUID org,UUID actor) {
        var context=admin(org,actor,true);
        if(!context.organization.archived) { context.organization.archived=true; event(org,actor,org,"ORGANIZATION_ARCHIVED"); }
    }
    @Transactional(readOnly=true)
    public PageResult<MemberView> members(UUID org,UUID actor,int page,int size) {
        var context=enter(org,actor,false,true); requireAdmin(context.member);
        return result(members.findByOrganizationId(org,page(page,size)),mapper::view);
    }
    public MemberView changeMember(UUID org,UUID actor,UUID memberId,ChangeMember dto) {
        admin(org,actor,false); var target=member(org,memberId); checkVersion(target.version,dto.version());
        protectLastAdmin(target,dto.role().name(),dto.status().name());
        target.role=dto.role().name(); target.status=dto.status().name();
        if(!target.status.equals("ACTIVE") || target.role.equals("STUDENT")) teachers.deleteByOrganizationIdAndMembershipId(org,target.id);
        if(!target.status.equals("ACTIVE") || !target.role.equals("STUDENT")) students.deleteByOrganizationIdAndMembershipId(org,target.id);
        em.flush(); event(org,actor,target.id,"MEMBERSHIP_CHANGED"); return mapper.view(target);
    }
    public void removeMember(UUID org,UUID actor,UUID memberId) {
        admin(org,actor,false); var target=member(org,memberId); protectLastAdmin(target,target.role,"INACTIVE");
        target.status="INACTIVE"; teachers.deleteByOrganizationIdAndMembershipId(org,target.id); students.deleteByOrganizationIdAndMembershipId(org,target.id);
        event(org,actor,target.id,"MEMBERSHIP_REMOVED");
    }
    public GroupView createGroup(UUID org,UUID actor,Create dto) {
        admin(org,actor,false); var group=new StudyGroup(); group.id=UUID.randomUUID(); group.organizationId=org;
        group.name=dto.name().strip(); group.createdAt=clock.instant(); groups.saveAndFlush(group);
        event(org,actor,group.id,"GROUP_CREATED"); return mapper.view(group);
    }
    @Transactional(readOnly=true)
    public PageResult<GroupView> groups(UUID org,UUID actor,int page,int size) {
        var c=enter(org,actor,false,true);
        return result(groups.accessible(org,c.member.id,c.member.role.equals("ORG_ADMIN"),page(page,size)),mapper::view);
    }
    @Transactional(readOnly=true)
    public GroupView group(UUID org,UUID actor,UUID groupId) {
        var c=enter(org,actor,false,true); var group=groupEntity(org,groupId); requireGroupAccess(c.member,groupId); return mapper.view(group);
    }
    public GroupView renameGroup(UUID org,UUID actor,UUID groupId,Rename dto) {
        admin(org,actor,false); var group=activeGroup(org,groupId); checkVersion(group.version,dto.version());
        group.name=dto.name().strip(); em.flush(); event(org,actor,group.id,"GROUP_RENAMED"); return mapper.view(group);
    }
    public void archiveGroup(UUID org,UUID actor,UUID groupId) {
        admin(org,actor,false); var group=groupEntity(org,groupId);
        if(!group.archived) { group.archived=true; event(org,actor,group.id,"GROUP_ARCHIVED"); }
    }
    public void assignStudent(UUID org,UUID actor,UUID groupId,UUID memberId,boolean add) {
        admin(org,actor,false); activeGroup(org,groupId); var target=member(org,memberId);
        if(add) {
            if(!target.role.equals("STUDENT") || !target.status.equals("ACTIVE")) throw OrganizationFailure.conflict("Yalnız aktiv tələbə qrupa əlavə edilə bilər.");
            if(!students.existsByOrganizationIdAndGroupIdAndMembershipId(org,groupId,memberId)) {
                var entry=new GroupStudent(); entry.id=UUID.randomUUID(); entry.organizationId=org; entry.groupId=groupId; entry.membershipId=memberId; students.save(entry);
                event(org,actor,memberId,"GROUP_STUDENT_ADDED",groupId);
            }
        } else { students.deleteByOrganizationIdAndGroupIdAndMembershipId(org,groupId,memberId); event(org,actor,memberId,"GROUP_STUDENT_REMOVED",groupId); }
    }
    public void assignTeacher(UUID org,UUID actor,UUID groupId,UUID memberId,boolean add) {
        admin(org,actor,false); activeGroup(org,groupId); var target=member(org,memberId);
        if(add) {
            if(!Set.of("TEACHER","ORG_ADMIN").contains(target.role) || !target.status.equals("ACTIVE")) throw OrganizationFailure.conflict("Yalnız aktiv müəllim və ya admin təyin edilə bilər.");
            if(!teachers.existsByOrganizationIdAndGroupIdAndMembershipId(org,groupId,memberId)) {
                var entry=new TeacherAssignment(); entry.id=UUID.randomUUID(); entry.organizationId=org; entry.groupId=groupId; entry.membershipId=memberId; teachers.save(entry);
                event(org,actor,memberId,"TEACHER_ASSIGNED",groupId);
            }
        } else { teachers.deleteByOrganizationIdAndGroupIdAndMembershipId(org,groupId,memberId); event(org,actor,memberId,"TEACHER_UNASSIGNED",groupId); }
    }
    @Transactional(readOnly=true)
    public PageResult<MemberView> roster(UUID org,UUID actor,UUID groupId,int page,int size,boolean teacherList) {
        var c=enter(org,actor,false,true); groupEntity(org,groupId);
        if(c.member.role.equals("STUDENT")) throw OrganizationFailure.forbidden();
        requireGroupAccess(c.member,groupId);
        return result(teacherList?teachers.teachers(org,groupId,page(page,size)):students.roster(org,groupId,page(page,size)),mapper::view);
    }
    public InvitationView invite(UUID org,UUID actor,Invite dto) {
        var c=admin(org,actor,false); quota.invitation(org,actor);
        String email=dto.email().strip().toLowerCase(Locale.ROOT);
        for(var prior:invitations.findByOrganizationIdAndEmailAndRevokedFalseAndConsumedAtIsNull(org,email)) prior.revoked=true;
        em.flush();
        var invitation=new Invitation(); String raw=tokens.create();
        invitation.id=UUID.randomUUID(); invitation.organizationId=org; invitation.email=email; invitation.role=dto.role().name();
        invitation.tokenHash=tokens.digest(raw); invitation.createdBy=actor; invitation.createdAt=clock.instant(); invitation.expiresAt=clock.instant().plus(Duration.ofDays(3));
        invitations.saveAndFlush(invitation);
        mail.enqueue(email,"HackTrain təşkilat dəvəti",c.organization.name+" təşkilatına dəvət:\n"+frontend+"/accept-invitation#token="+raw);
        event(org,actor,invitation.id,"INVITATION_CREATED"); return mapper.view(invitation);
    }
    @Transactional(readOnly=true)
    public PageResult<InvitationView> invitations(UUID org,UUID actor,int page,int size) {
        var c=enter(org,actor,false,true); requireAdmin(c.member);
        return result(invitations.findByOrganizationId(org,page(page,size)),mapper::view);
    }
    public void revokeInvitation(UUID org,UUID actor,UUID invitationId) {
        admin(org,actor,false); var invitation=invitations.findByOrganizationIdAndId(org,invitationId).orElseThrow(OrganizationFailure::missing);
        invitation.revoked=true; event(org,actor,invitation.id,"INVITATION_REVOKED");
    }
    public MemberView accept(UUID actor,String rawToken) {
        scope.actor(actor); var identity=identities.requireActive(actor); scope.invitation(tokens.digest(rawToken));
        var invitation=invitations.findByTokenHash(tokens.digest(rawToken)).orElseThrow(OrganizationFailure::missing);
        if(!identity.email().equals(invitation.email)) throw OrganizationFailure.forbidden();
        scope.organization(invitation.organizationId);
        var org=organizations.lock(invitation.organizationId).orElseThrow(OrganizationFailure::missing);
        em.refresh(invitation);
        if(org.archived || invitation.revoked || invitation.consumedAt!=null || !invitation.expiresAt.isAfter(clock.instant())) throw OrganizationFailure.missing();
        var existing=members.findByOrganizationIdAndUserId(org.id,actor);
        if(existing.isPresent() && existing.get().status.equals("ACTIVE")) throw OrganizationFailure.conflict("İstifadəçi artıq aktiv üzvdür; rol yalnız üzvlük API-si ilə dəyişir.");
        var target=existing.orElseGet(Membership::new);
        if(target.id==null) { target.id=UUID.randomUUID(); target.organizationId=org.id; target.userId=actor; target.createdAt=clock.instant(); }
        target.role=invitation.role; target.status="ACTIVE"; invitation.consumedAt=clock.instant(); members.saveAndFlush(target);
        event(org.id,actor,target.id,"INVITATION_ACCEPTED"); return mapper.view(target);
    }
    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public OrganizationAccess.Access authorize(UUID org,UUID actor,boolean write) {
        var c=enter(org,actor,write,!write);
        return new OrganizationAccess.Access(c.member.id,c.member.role);
    }
    @Override
    @Transactional(propagation=Propagation.MANDATORY)
    public void requireActiveGroup(UUID org,UUID actor,UUID group) {
        var c=enter(org,actor,true,false); activeGroup(org,group); requireGroupAccess(c.member,group);
    }
    private record Context(Organization organization,Membership member) {}
    private Context enter(UUID org,UUID actor,boolean lock,boolean allowArchived) {
        scope.actor(actor); var self=members.findByOrganizationIdAndUserId(org,actor).filter(m->m.status.equals("ACTIVE")).orElseThrow(OrganizationFailure::missing);
        scope.organization(org); var organization=(lock?organizations.lock(org):organizations.findById(org)).orElseThrow(OrganizationFailure::missing);
        if(lock) em.refresh(self);
        if(!self.status.equals("ACTIVE")) throw OrganizationFailure.missing();
        if(organization.archived&&!allowArchived) throw OrganizationFailure.conflict("Təşkilat arxivdədir.");
        return new Context(organization,self);
    }
    private Context admin(UUID org,UUID actor,boolean allowArchived) { var c=enter(org,actor,true,allowArchived); requireAdmin(c.member); return c; }
    private void requireAdmin(Membership member) { if(!member.role.equals("ORG_ADMIN")) throw OrganizationFailure.forbidden(); }
    private Membership member(UUID org,UUID id) { return members.findByOrganizationIdAndId(org,id).orElseThrow(OrganizationFailure::missing); }
    private StudyGroup groupEntity(UUID org,UUID id) { return groups.findByOrganizationIdAndId(org,id).orElseThrow(OrganizationFailure::missing); }
    private StudyGroup activeGroup(UUID org,UUID id) { var g=groupEntity(org,id); if(g.archived) throw OrganizationFailure.conflict("Qrup arxivdədir."); return g; }
    private void requireGroupAccess(Membership member,UUID group) {
        if(member.role.equals("ORG_ADMIN")) return;
        boolean allowed=member.role.equals("TEACHER")?teachers.existsByOrganizationIdAndGroupIdAndMembershipId(member.organizationId,group,member.id):students.existsByOrganizationIdAndGroupIdAndMembershipId(member.organizationId,group,member.id);
        if(!allowed) throw OrganizationFailure.missing();
    }
    private void protectLastAdmin(Membership member,String nextRole,String nextStatus) {
        if(member.role.equals("ORG_ADMIN")&&member.status.equals("ACTIVE")&&(!nextRole.equals("ORG_ADMIN")||!nextStatus.equals("ACTIVE"))
            && members.countByOrganizationIdAndRoleAndStatus(member.organizationId,"ORG_ADMIN","ACTIVE")<=1) throw OrganizationFailure.conflict("Son aktiv təşkilat admini silinə və ya səlahiyyətsizləşdirilə bilməz.");
    }
    private void checkVersion(long actual,long expected) { if(actual!=expected) throw OrganizationFailure.conflict("Məlumat dəyişib; yeniləyib təkrar cəhd edin."); }
    private Pageable page(int number,int size) {
        if(number<0||number>10000||size<1||size>100) throw new OrganizationFailure(org.springframework.http.HttpStatus.BAD_REQUEST,"invalid_page","Səhifə həddi etibarsızdır.");
        return PageRequest.of(number,size,Sort.by("createdAt").ascending().and(Sort.by("id")));
    }
    private <E,T> PageResult<T> result(Page<E> data,Function<E,T> convert) { return new PageResult<>(data.getContent().stream().map(convert).toList(),data.getNumber(),data.getSize(),data.getTotalElements()); }
    private void event(UUID org,UUID actor,UUID resource,String type) { event(org,actor,resource,type,null); }
    private void event(UUID org,UUID actor,UUID resource,String type,UUID context) {
        var event=new OrganizationEvent(); event.id=UUID.randomUUID(); event.organizationId=org; event.actorId=actor;
        event.resourceId=resource; event.contextId=context; event.eventType=type; event.createdAt=clock.instant(); events.save(event);
    }
}
