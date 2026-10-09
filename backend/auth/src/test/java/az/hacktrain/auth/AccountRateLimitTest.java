package az.hacktrain.auth;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class AccountRateLimitTest {
    @SuppressWarnings("unchecked") @Test void denialAndOutageAreDistinct() {
        var proxy=(LettuceBasedProxyManager<String>)mock(LettuceBasedProxyManager.class,RETURNS_DEEP_STUBS);
        var limiter=new AccountRateLimit(proxy,new TokenSecrets());
        when(proxy.getProxy(anyString(),any()).tryConsume(1)).thenReturn(false);
        assertThatThrownBy(()->limiter.consume("a@example.com",true)).isInstanceOfSatisfying(AuthFailure.class,e->assertThat(e.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        when(proxy.getProxy(anyString(),any())).thenThrow(new IllegalStateException());
        assertThatThrownBy(()->limiter.consume("a@example.com",false)).isInstanceOfSatisfying(AuthFailure.class,e->assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
