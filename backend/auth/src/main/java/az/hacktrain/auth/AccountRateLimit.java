package az.hacktrain.auth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.Duration;
/** Supplements the IP limit against distributed password guessing and email flooding. */
@Component
class AccountRateLimit {
    private final LettuceBasedProxyManager<String> proxy;
    private final TokenSecrets secrets;
    private final BucketConfiguration login=BucketConfiguration.builder()
        .addLimit(b->b.capacity(10).refillGreedy(10,Duration.ofMinutes(1)))
        .addLimit(b->b.capacity(50).refillGreedy(50,Duration.ofHours(1))).build();
    private final BucketConfiguration email=BucketConfiguration.builder()
        .addLimit(b->b.capacity(5).refillGreedy(5,Duration.ofHours(1))).build();
    AccountRateLimit(LettuceBasedProxyManager<String> proxy,TokenSecrets secrets) { this.proxy=proxy; this.secrets=secrets; }
    void consume(String address,boolean mail) {
        boolean allowed;
        try { allowed=proxy.getProxy("auth:account:"+(mail?"mail:":"login:")+secrets.hash(address),()->mail?email:login).tryConsume(1); }
        catch(RuntimeException e) { throw new AuthFailure(HttpStatus.SERVICE_UNAVAILABLE,"rate_limit_unavailable","Xidmət müvəqqəti əlçatmazdır."); }
        if(!allowed) throw new AuthFailure(HttpStatus.TOO_MANY_REQUESTS,"rate_limited","Sorğu limiti keçilib.");
    }
}
