package az.hacktrain.auth;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmail(String email);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> lockById(UUID id);
    @Modifying
    @Query(value="insert into user_account(id,email,password_hash,email_verified,blocked,token_version,platform_role,created_at) values (:id,:email,:password,false,false,0,'STUDENT',now()) on conflict(email) do nothing", nativeQuery=true)
    int insertIfAbsent(UUID id, String email, String password);
}
