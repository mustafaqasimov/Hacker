package az.hacktrain.auth;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import java.time.Duration;
@Configuration
class RateLimitConfig {
    @Bean(destroyMethod="shutdown") RedisClient rateLimitRedis(@Value("${hacktrain.rate-limit.redis-uri}") String uri) {
        var redisUri=RedisURI.create(uri); redisUri.setTimeout(Duration.ofSeconds(2)); return RedisClient.create(redisUri);
    }
    @Bean(destroyMethod="close") StatefulRedisConnection<String,byte[]> rateLimitConnection(RedisClient rateLimitRedis) {
        return rateLimitRedis.connect(RedisCodec.of(StringCodec.UTF8,ByteArrayCodec.INSTANCE));
    }
    @Bean LettuceBasedProxyManager<String> rateLimitProxy(StatefulRedisConnection<String,byte[]> connection) {
        return Bucket4jLettuce.casBasedBuilder(connection)
            .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1))).build();
    }
}
