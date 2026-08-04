package distributed.tinyurl.urlservice.ratelimit;

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

    public RedisCreateUrlRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limits.create-url.limit}") long limit,
            @Value("${app.rate-limits.create-url.window}") Duration window
    ) {
        this.redisTemplate = redisTemplate;
        this.limit = limit;
        this.window = window;
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

            return requestCount <= limit;
        } catch (DataAccessException ex) {
            return true;
        }
    }

    private String cacheKey(String clientId) {
        return KEY_PREFIX + clientId;
    }
}
