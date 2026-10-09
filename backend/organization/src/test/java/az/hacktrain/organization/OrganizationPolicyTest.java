package az.hacktrain.organization;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OrganizationPolicyTest {
    @Test void productionGuardRejectsPrivilegedDatabaseRole() {
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(true);
        assertThatThrownBy(()->new ProductionRlsGuard(jdbc).run(null)).isInstanceOf(IllegalStateException.class);
    }
    @Test void productionGuardAcceptsRestrictedRole() {
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject(anyString(),eq(Boolean.class))).thenReturn(false);new ProductionRlsGuard(jdbc).run(null);
    }
    @Test void invitationSecretHasEnoughEntropyAndOnlyDigestIsPersisted() {
        var codec=new InvitationTokens();String first=codec.create();String second=codec.create();
        assertThat(first).matches("[A-Za-z0-9_-]{43}").isNotEqualTo(second);assertThat(codec.digest(first)).hasSize(64).isNotEqualTo(first).isEqualTo(codec.digest(first));
    }
    @Test void tenantScopeCannotEscapeTransaction() {
        var em=mock(jakarta.persistence.EntityManager.class);
        assertThatThrownBy(()->new TenantScope(em).actor(java.util.UUID.randomUUID())).isInstanceOf(IllegalStateException.class);verifyNoInteractions(em);
    }
}
