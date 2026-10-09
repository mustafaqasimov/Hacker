package az.hacktrain.organization;
import org.mapstruct.*;
@Mapper(componentModel="spring",unmappedTargetPolicy=ReportingPolicy.ERROR)
interface OrganizationMapper {
    OrganizationDtos.OrganizationView view(Organization entity);
    OrganizationDtos.MemberView view(Membership entity);
    OrganizationDtos.GroupView view(StudyGroup entity);
    OrganizationDtos.InvitationView view(Invitation entity);
}
