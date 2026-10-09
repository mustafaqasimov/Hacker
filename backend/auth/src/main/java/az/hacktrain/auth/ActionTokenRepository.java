package az.hacktrain.auth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface ActionTokenRepository extends JpaRepository<ActionToken, UUID> {
    Optional<ActionToken> findByTokenHash(String hash);
}
