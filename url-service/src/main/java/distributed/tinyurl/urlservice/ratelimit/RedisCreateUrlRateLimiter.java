package distributed.tinyurl.urlservice.ratelimit;

import distributed.tinyurl.urlservice.cache.RedisKeyPrefix;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

import static distributed.tinyurl.urlservice.observability.MetricName.RATE_LIMIT_CHECKS;
import static distributed.tinyurl.urlservice.observability.MetricTag.OPERATION;
import static distributed.tinyurl.urlservice.observability.MetricTag.OUTCOME;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.ALLOWED;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.BLOCKED;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.CREATE_URL;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.ERROR_ALLOWED;
import static distributed.tinyurl.urlservice.resilience.ResilienceInstance.REDIS_CREATE_URL_RATE_LIMITER;

@Component
public class RedisCreateUrlRateLimiter implements CreateUrlRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final long limit;
    private final Duration window;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker circuitBreaker;

    public RedisCreateUrlRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limits.create-url.limit}") long limit,
            @Value("${app.rate-limits.create-url.window}") Duration window,
            MeterRegistry meterRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.limit = limit;
        this.window = window;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(REDIS_CREATE_URL_RATE_LIMITER.key());
    }

    @Override
    public boolean isAllowed(String clientId) {
        try {
            return circuitBreaker.executeSupplier(() -> isAllowedByRedis(clientId));
        } catch (RuntimeException ex) {
            return isAllowedFallback();
        }
    }

    private boolean isAllowedByRedis(String clientId) {
        Long requestCount = redisTemplate.opsForValue().increment(cacheKey(clientId));
        if (requestCount == null) {
            return true;
        }

        if (requestCount == 1) {
            redisTemplate.expire(cacheKey(clientId), window);
        }

        boolean allowed = requestCount <= limit;
        meterRegistry.counter(
                RATE_LIMIT_CHECKS.key(),
                OPERATION.key(), CREATE_URL.key(),
                OUTCOME.key(), allowed ? ALLOWED.key() : BLOCKED.key()
        ).increment();
        return allowed;
    }

    private boolean isAllowedFallback() {
        meterRegistry.counter(
                RATE_LIMIT_CHECKS.key(),
                OPERATION.key(), CREATE_URL.key(),
                OUTCOME.key(), ERROR_ALLOWED.key()
        ).increment();
        return true;
    }

    private String cacheKey(String clientId) {
        return RedisKeyPrefix.CREATE_URL_RATE_LIMIT.key(clientId);
    }
}
