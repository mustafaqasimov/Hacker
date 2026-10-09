package az.hacktrain.organization;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static az.hacktrain.organization.OrganizationDtos.*;
@RestController
@RequestMapping("/api/v1")
public class OrganizationController {
    private final OrganizationService service;
    OrganizationController(OrganizationService service) { this.service=service; }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @PostMapping("/organizations") @ResponseStatus(HttpStatus.CREATED)
    public OrganizationView create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create dto) { return service.create(actor(jwt),dto); }
    @GetMapping("/organizations")
    public PageResult<OrganizationView> mine(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.mine(actor(jwt),page,size); }
    @GetMapping("/organizations/{org}")
    public OrganizationView get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org) { return service.get(org,actor(jwt)); }
    @GetMapping("/organizations/{org}/membership")
    public MemberView membership(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org) { return service.membership(org,actor(jwt)); }
    @PatchMapping("/organizations/{org}")
    public OrganizationView rename(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@Valid @RequestBody Rename dto) { return service.rename(org,actor(jwt),dto); }
    @PostMapping("/organizations/{org}/archive") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org) { service.archive(org,actor(jwt)); }
    @GetMapping("/organizations/{org}/members")
    public PageResult<MemberView> members(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.members(org,actor(jwt),page,size); }
    @PatchMapping("/organizations/{org}/members/{member}")
    public MemberView changeMember(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID member,@Valid @RequestBody ChangeMember dto) { return service.changeMember(org,actor(jwt),member,dto); }
    @DeleteMapping("/organizations/{org}/members/{member}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID member) { service.removeMember(org,actor(jwt),member); }
    @PostMapping("/organizations/{org}/groups") @ResponseStatus(HttpStatus.CREATED)
    public GroupView createGroup(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@Valid @RequestBody Create dto) { return service.createGroup(org,actor(jwt),dto); }
    @GetMapping("/organizations/{org}/groups")
    public PageResult<GroupView> groups(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.groups(org,actor(jwt),page,size); }
    @GetMapping("/organizations/{org}/groups/{group}")
    public GroupView group(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group) { return service.group(org,actor(jwt),group); }
    @PatchMapping("/organizations/{org}/groups/{group}")
    public GroupView renameGroup(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group,@Valid @RequestBody Rename dto) { return service.renameGroup(org,actor(jwt),group,dto); }
    @DeleteMapping("/organizations/{org}/groups/{group}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveGroup(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group) { service.archiveGroup(org,actor(jwt),group); }
    @PostMapping("/organizations/{org}/groups/{group}/students/{member}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignStudent(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group,@PathVariable UUID member) { service.assignStudent(org,actor(jwt),group,member,true); }
    @DeleteMapping("/organizations/{org}/groups/{group}/students/{member}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStudent(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group,@PathVariable UUID member) { service.assignStudent(org,actor(jwt),group,member,false); }
    @PutMapping("/organizations/{org}/groups/{group}/teachers/{member}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignTeacher(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group,@PathVariable UUID member) { service.assignTeacher(org,actor(jwt),group,member,true); }
    @DeleteMapping("/organizations/{org}/groups/{group}/teachers/{member}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTeacher(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group,@PathVariable UUID member) { service.assignTeacher(org,actor(jwt),group,member,false); }
    @GetMapping("/organizations/{org}/groups/{group}/students")
    public PageResult<MemberView> students(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.roster(org,actor(jwt),group,page,size,false); }
    @GetMapping("/organizations/{org}/groups/{group}/teachers")
    public PageResult<MemberView> teachers(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID group,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.roster(org,actor(jwt),group,page,size,true); }
    @PostMapping("/organizations/{org}/invitations") @ResponseStatus(HttpStatus.CREATED)
    public CreatedInvitation invite(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@Valid @RequestBody Invite dto) { return service.invite(org,actor(jwt),dto); }
    @GetMapping("/organizations/{org}/invitations")
    public PageResult<InvitationView> invitations(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.invitations(org,actor(jwt),page,size); }
    @DeleteMapping("/organizations/{org}/invitations/{invitation}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID org,@PathVariable UUID invitation) { service.revokeInvitation(org,actor(jwt),invitation); }
    @PostMapping("/invitations/accept")
    public MemberView accept(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AcceptInvite dto) { return service.accept(actor(jwt),dto.token()); }
}
