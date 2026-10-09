package az.hacktrain.auth;
import io.github.bucket4j.*;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
@Component
class AuthRateLimit extends OncePerRequestFilter {
    private final LettuceBasedProxyManager<String> proxy;
    private final TokenSecrets secrets;
    private final BucketConfiguration configuration=BucketConfiguration.builder()
        .addLimit(b -> b.capacity(20).refillGreedy(20,Duration.ofMinutes(1)))
        .addLimit(b -> b.capacity(100).refillGreedy(100,Duration.ofHours(1))).build();
    private final BucketConfiguration apiConfiguration=BucketConfiguration.builder()
        .addLimit(b -> b.capacity(300).refillGreedy(300,Duration.ofMinutes(1))).build();
    AuthRateLimit(LettuceBasedProxyManager<String> proxy, TokenSecrets secrets) { this.proxy=proxy; this.secrets=secrets; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/") || request.getMethod().equals("OPTIONS");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException,IOException {
        try {
            // Never trust client-supplied X-Forwarded-For. A trusted ingress must normalize remote address.
            boolean auth=request.getServletPath().startsWith("/api/v1/auth/");
            var probe=proxy.getProxy((auth?"auth:ip:":"api:ip:")+secrets.hash(request.getRemoteAddr()),()->auth?configuration:apiConfiguration).tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                response.setHeader("Retry-After",Long.toString(Math.max(1,(probe.getNanosToWaitForRefill()+999_999_999)/1_000_000_000)));
                ProblemWriter.write(response,429,"rate_limited","Sorğu limiti keçilib."); return;
            }
        } catch (RuntimeException e) { ProblemWriter.write(response,503,"rate_limit_unavailable","Xidmət müvəqqəti əlçatmazdır."); return; }
        if (request.getContentLengthLong()>16384) { ProblemWriter.write(response,413,"body_too_large","Sorğu həddən böyükdür."); return; }
        byte[] body=request.getInputStream().readNBytes(16385);
        if (body.length>16384) { ProblemWriter.write(response,413,"body_too_large","Sorğu həddən böyükdür."); return; }
        chain.doFilter(new BoundedRequest(request,body),response);
    }
}
