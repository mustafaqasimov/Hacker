package az.hacktrain.ai;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.UUID;
@Component
class AiQuota {
    private final LettuceBasedProxyManager<String> proxy; private final BucketConfiguration user,organization;
    AiQuota(LettuceBasedProxyManager<String> proxy,@Value("${hacktrain.ai.student-daily-limit:30}") int studentLimit,@Value("${hacktrain.ai.organization-daily-limit:1000}") int orgLimit) {
        if(studentLimit<1||orgLimit<1) throw new IllegalArgumentException("AI limits must be positive");this.proxy=proxy;
        user=BucketConfiguration.builder().addLimit(b->b.capacity(studentLimit).refillIntervally(studentLimit,Duration.ofDays(1))).addLimit(b->b.capacity(5).refillGreedy(5,Duration.ofMinutes(1))).build();
        organization=BucketConfiguration.builder().addLimit(b->b.capacity(orgLimit).refillIntervally(orgLimit,Duration.ofDays(1))).build();
    }
    void consume(UUID org,UUID actor) {
        boolean allowed;
        try { allowed=proxy.getProxy("ai:student:"+actor,()->user).tryConsume(1)&&proxy.getProxy("ai:org:"+org,()->organization).tryConsume(1); }
        catch(RuntimeException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"AI limit service unavailable"); }
        if(!allowed) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"AI request limit exceeded");
    }
}
