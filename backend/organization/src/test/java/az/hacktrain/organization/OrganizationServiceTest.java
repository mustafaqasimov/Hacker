package az.hacktrain.organization;
import az.hacktrain.auth.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import java.time.Clock;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OrganizationServiceTest {
    OrganizationRepository orgs=mock(OrganizationRepository.class);
    MembershipRepository members=mock(MembershipRepository.class);
    GroupRepository groups=mock(GroupRepository.class);
    GroupStudentRepository students=mock(GroupStudentRepository.class);
    TeacherAssignmentRepository teachers=mock(TeacherAssignmentRepository.class);
    InvitationRepository invitations=mock(InvitationRepository.class);
    OrganizationEventRepository events=mock(OrganizationEventRepository.class);
    OrganizationMapper mapper=mock(OrganizationMapper.class);
    TenantScope scope=mock(TenantScope.class);
    IdentityAccess identities=mock(IdentityAccess.class);
    EntityManager em=mock(EntityManager.class);
    OrganizationQuota quota=mock(OrganizationQuota.class);
    OrganizationService service;
    UUID actor=UUID.randomUUID(),orgId=UUID.randomUUID();
    @BeforeEach void init() { service=new OrganizationService(orgs,members,groups,students,teachers,invitations,events,mapper,scope,identities,new InvitationTokens(),Clock.systemUTC(),em,quota); }
    @Test void unauthorizedTenantStopsBeforeLoadingTenantData() {
        when(members.findByOrganizationIdAndUserId(orgId,actor)).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.get(orgId,actor)).isInstanceOf(OrganizationFailure.class);
        verifyNoInteractions(orgs,groups,invitations);verify(scope,never()).organization(any());
    }
    @Test void roleRevocationWhileWaitingForOrganizationLockIsRechecked() {
        var member=new Membership();member.id=UUID.randomUUID();member.role="ORG_ADMIN";member.status="ACTIVE";
        var organization=new Organization();organization.id=orgId;
        when(members.findByOrganizationIdAndUserId(orgId,actor)).thenReturn(Optional.of(member));when(orgs.lock(orgId)).thenReturn(Optional.of(organization));
        doAnswer(call->{member.role="STUDENT";return null;}).when(em).refresh(member);
        assertThatThrownBy(()->service.createGroup(orgId,actor,new OrganizationDtos.Create("forbidden"))).isInstanceOf(OrganizationFailure.class);
        verifyNoInteractions(groups,events);
    }
    @Test void onlyActiveVerifiedIdentityCanCreateTenant() {
        when(identities.lockActive(actor)).thenThrow(new AuthFailure(org.springframework.http.HttpStatus.UNAUTHORIZED,"invalid","invalid"));
        assertThatThrownBy(()->service.create(actor,new OrganizationDtos.Create("blocked"))).isInstanceOf(AuthFailure.class);verifyNoInteractions(orgs,members,events);
    }
}
