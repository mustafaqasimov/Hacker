package az.hacktrain.auth;
import java.util.UUID;
/** Identity capability exposed to other modules without persistence details. */
public interface IdentityDirectory {
    record Identity(UUID id,String email) {}
    Identity requireActive(UUID id);
    Identity lockActive(UUID id);
}
