package distributed.tinyurl.urlservice.ratelimit;

import distributed.tinyurl.urlservice.cache.RedisKeyPrefix;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
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

@Component
public class RedisCreateUrlRateLimiter implements CreateUrlRateLimiter {

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
                    RATE_LIMIT_CHECKS.key(),
                    OPERATION.key(), CREATE_URL.key(),
                    OUTCOME.key(), allowed ? ALLOWED.key() : BLOCKED.key()
            ).increment();
            return allowed;
        } catch (DataAccessException ex) {
            meterRegistry.counter(
                    RATE_LIMIT_CHECKS.key(),
                    OPERATION.key(), CREATE_URL.key(),
                    OUTCOME.key(), ERROR_ALLOWED.key()
            ).increment();
            return true;
        }
    }

    private String cacheKey(String clientId) {
        return RedisKeyPrefix.CREATE_URL_RATE_LIMIT.key(clientId);
    }
}
