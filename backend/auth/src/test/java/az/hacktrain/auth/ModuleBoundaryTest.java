package az.hacktrain.auth;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ModuleBoundaryTest {
    @Test void identityBoundaryRejectsBlockedAccounts() {
        var repo=mock(UserRepository.class);var user=new UserAccount();user.id=UUID.randomUUID();user.email="a@example.com";
        user.blocked=true;
        when(repo.findById(user.id)).thenReturn(Optional.of(user));var access=new IdentityAccess(repo);
        assertThatThrownBy(()->access.requireActive(user.id)).isInstanceOf(AuthFailure.class);
        user.blocked=false;assertThat(access.requireActive(user.id).email()).isEqualTo(user.email);
    }
    @Test void identityLockRefreshesBeforeCheckingAccountStatus() {
        var repo=mock(UserRepository.class);var em=mock(jakarta.persistence.EntityManager.class);
        var user=new UserAccount();user.id=UUID.randomUUID();
        when(repo.lockById(user.id)).thenReturn(Optional.of(user));doAnswer(call->{user.blocked=true;return null;}).when(em).refresh(user);
        var access=new IdentityAccess(repo);org.springframework.test.util.ReflectionTestUtils.setField(access,"em",em);
        assertThatThrownBy(()->access.lockActive(user.id)).isInstanceOf(AuthFailure.class);
    }
}
