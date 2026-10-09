package az.hacktrain.auth;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class AuthRateLimitTest {
    @SuppressWarnings("unchecked")
    LettuceBasedProxyManager<String> proxy() { return mock(LettuceBasedProxyManager.class,RETURNS_DEEP_STUBS); }
    MockHttpServletRequest request() { var request=new MockHttpServletRequest("POST","/api/v1/auth/login"); request.setServletPath("/api/v1/auth/login"); return request; }
    @Test void redisFailureFailsClosed() throws Exception {
        var proxy=proxy(); when(proxy.getProxy(anyString(),any())).thenThrow(new IllegalStateException());
        var response=new MockHttpServletResponse(); var chain=mock(FilterChain.class);
        new AuthRateLimit(proxy,new TokenSecrets()).doFilter(request(),response,chain);
        assertThat(response.getStatus()).isEqualTo(503); verifyNoInteractions(chain);
    }
    @Test void deniedProbeReturns429() throws Exception {
        var proxy=proxy(); when(proxy.getProxy(anyString(),any()).tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.rejected(0,2_000_000_000L,3_000_000_000L));
        var response=new MockHttpServletResponse(); var chain=mock(FilterChain.class);
        new AuthRateLimit(proxy,new TokenSecrets()).doFilter(request(),response,chain);
        assertThat(response.getStatus()).isEqualTo(429); assertThat(response.getHeader("Retry-After")).isEqualTo("2"); verifyNoInteractions(chain);
    }
    @Test void oversizedJsonIsRejected() throws Exception {
        var proxy=proxy(); when(proxy.getProxy(anyString(),any()).tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(19,0));
        var req=request(); req.setContent(new byte[16385]); var response=new MockHttpServletResponse(); var chain=mock(FilterChain.class);
        new AuthRateLimit(proxy,new TokenSecrets()).doFilter(req,response,chain);
        assertThat(response.getStatus()).isEqualTo(413); verifyNoInteractions(chain);
    }
    @Test void permittedBodyIsPreserved() throws Exception {
        var proxy=proxy(); when(proxy.getProxy(anyString(),any()).tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(19,0));
        var req=request(); req.setContent("{\"email\":\"x@example.com\"}".getBytes());
        var response=new MockHttpServletResponse();
        new AuthRateLimit(proxy,new TokenSecrets()).doFilter(req,response,(r,s)->assertThat(new String(r.getInputStream().readAllBytes())).contains("x@example.com"));
    }
}
