package az.hacktrain.organization;
import java.util.UUID;
/** Authorization boundary. Scope is transaction-local; caller must retain the transaction. */
public interface OrganizationAccess {
    record Access(UUID membershipId,String role) { public boolean staff() { return !role.equals("STUDENT"); } }
    Access authorize(UUID organization,UUID actor,boolean write);
    void requireActiveGroup(UUID organization,UUID actor,UUID group);
}
