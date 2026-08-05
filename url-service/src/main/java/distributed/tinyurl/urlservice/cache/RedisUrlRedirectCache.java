package distributed.tinyurl.urlservice.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class RedisUrlRedirectCache implements UrlRedirectCache {

    private static final String KEY_PREFIX = "redirect:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration ttl;

    public RedisUrlRedirectCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.cache.redirects.ttl}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public Optional<CachedRedirect> findByShortCode(String shortCode) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(shortCode));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, CachedRedirect.class));
        } catch (JsonProcessingException | DataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void save(String shortCode, CachedRedirect redirect) {
        try {
            String json = objectMapper.writeValueAsString(redirect);
            redisTemplate.opsForValue().set(cacheKey(shortCode), json, effectiveTtl(redirect));
        } catch (JsonProcessingException | DataAccessException ignored) {
            // Redis is an optimization for redirects, not the source of truth.
        }
    }

    @Override
    public void delete(String shortCode) {
        try {
            redisTemplate.delete(cacheKey(shortCode));
        } catch (DataAccessException ignored) {
            // Postgres remains the source of truth if Redis is temporarily unavailable.
        }
    }

    private String cacheKey(String shortCode) {
        return KEY_PREFIX + shortCode;
    }

    private Duration effectiveTtl(CachedRedirect redirect) {
        Instant expiresAt = redirect.expiresAt();
        if (expiresAt == null) {
            return ttl;
        }

        Duration timeUntilUrlExpires = Duration.between(Instant.now(clock), expiresAt);
        if (timeUntilUrlExpires.compareTo(Duration.ZERO) <= 0) {
            return Duration.ofSeconds(1);
        }

        return timeUntilUrlExpires.compareTo(ttl) < 0 ? timeUntilUrlExpires : ttl;
    }
}
