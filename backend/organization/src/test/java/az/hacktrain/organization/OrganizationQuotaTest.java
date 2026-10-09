package az.hacktrain.organization;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class OrganizationQuotaTest {
    @SuppressWarnings("unchecked") @Test void invitationDenialAndRedisOutageFailClosed() {
        var proxy=(LettuceBasedProxyManager<String>)mock(LettuceBasedProxyManager.class,RETURNS_DEEP_STUBS);var quota=new OrganizationQuota(proxy);
        when(proxy.getProxy(anyString(),any()).tryConsume(1)).thenReturn(false);
        assertThatThrownBy(()->quota.invitation(UUID.randomUUID(),UUID.randomUUID())).isInstanceOfSatisfying(OrganizationFailure.class,e->assertThat(e.status.value()).isEqualTo(429));
        when(proxy.getProxy(anyString(),any())).thenThrow(new IllegalStateException());
        assertThatThrownBy(()->quota.invitation(UUID.randomUUID(),UUID.randomUUID())).isInstanceOfSatisfying(OrganizationFailure.class,e->assertThat(e.status.value()).isEqualTo(503));
    }
}
