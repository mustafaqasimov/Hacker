package az.hacktrain.organization;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import java.time.Duration;
import java.util.UUID;
@Component
class OrganizationQuota {
    private final LettuceBasedProxyManager<String> proxy;
    private final BucketConfiguration config=BucketConfiguration.builder().addLimit(b->b.capacity(50).refillGreedy(50,Duration.ofHours(1))).build();
    OrganizationQuota(LettuceBasedProxyManager<String> proxy) { this.proxy=proxy; }
    void invitation(UUID org,UUID actor) {
        final boolean allowed;
        try { allowed=proxy.getProxy("org:invitations:"+org+":"+actor,()->config).tryConsume(1); }
        catch(RuntimeException e) { throw new OrganizationFailure(HttpStatus.SERVICE_UNAVAILABLE,"quota_unavailable","Limit xidməti əlçatmazdır."); }
        if(!allowed) throw new OrganizationFailure(HttpStatus.TOO_MANY_REQUESTS,"rate_limited","Dəvət limiti keçilib.");
    }
}
