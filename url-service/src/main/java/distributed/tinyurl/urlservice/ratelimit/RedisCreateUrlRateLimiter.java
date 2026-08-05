package distributed.tinyurl.urlservice.ratelimit;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisCreateUrlRateLimiter implements CreateUrlRateLimiter {

    private static final String KEY_PREFIX = "rate-limit:create-url:";

    private final StringRedisTemplate redisTemplate;
    private final long limit;
    private final Duration window;
    private final MeterRegistry meterRegistry;

    public RedisCreateUrlRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limits.create-url.limit}") long limit,
            @Value("${app.rate-limits.create-url.window}") Duration window,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.limit = limit;
        this.window = window;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean isAllowed(String clientId) {
        try {
            Long requestCount = redisTemplate.opsForValue().increment(cacheKey(clientId));
            if (requestCount == null) {
                return true;
            }

            if (requestCount == 1) {
                redisTemplate.expire(cacheKey(clientId), window);
            }

            boolean allowed = requestCount <= limit;
            meterRegistry.counter(
                    "tinyurl_rate_limit_checks_total",
                    "operation", "create_url",
                    "outcome", allowed ? "allowed" : "blocked"
            ).increment();
            return allowed;
        } catch (DataAccessException ex) {
            meterRegistry.counter(
                    "tinyurl_rate_limit_checks_total",
                    "operation", "create_url",
                    "outcome", "error_allowed"
            ).increment();
            return true;
        }
    }

    private String cacheKey(String clientId) {
        return KEY_PREFIX + clientId;
    }
}
