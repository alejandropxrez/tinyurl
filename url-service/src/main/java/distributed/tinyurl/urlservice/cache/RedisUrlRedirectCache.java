package distributed.tinyurl.urlservice.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import distributed.tinyurl.urlservice.observability.MetricTagValue;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static distributed.tinyurl.urlservice.observability.MetricName.REDIRECT_CACHE_EVICTIONS;
import static distributed.tinyurl.urlservice.observability.MetricName.REDIRECT_CACHE_REQUESTS;
import static distributed.tinyurl.urlservice.observability.MetricName.REDIRECT_CACHE_WRITES;
import static distributed.tinyurl.urlservice.observability.MetricTag.OUTCOME;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.ERROR;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.HIT;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.MISS;
import static distributed.tinyurl.urlservice.observability.MetricTagValue.SUCCESS;

@Component
public class RedisUrlRedirectCache implements UrlRedirectCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration ttl;
    private final MeterRegistry meterRegistry;

    public RedisUrlRedirectCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.cache.redirects.ttl}") Duration ttl,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.ttl = ttl;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<CachedRedirect> findByShortCode(String shortCode) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey(shortCode));
            if (json == null) {
                recordCacheRequest(MISS);
                return Optional.empty();
            }
            recordCacheRequest(HIT);
            return Optional.of(objectMapper.readValue(json, CachedRedirect.class));
        } catch (JsonProcessingException | DataAccessException ex) {
            recordCacheRequest(ERROR);
            return Optional.empty();
        }
    }

    @Override
    public void save(String shortCode, CachedRedirect redirect) {
        try {
            String json = objectMapper.writeValueAsString(redirect);
            redisTemplate.opsForValue().set(cacheKey(shortCode), json, effectiveTtl(redirect));
            meterRegistry.counter(REDIRECT_CACHE_WRITES.key(), OUTCOME.key(), SUCCESS.key()).increment();
        } catch (JsonProcessingException | DataAccessException ignored) {
            meterRegistry.counter(REDIRECT_CACHE_WRITES.key(), OUTCOME.key(), ERROR.key()).increment();
            // Redis is an optimization for redirects, not the source of truth.
        }
    }

    @Override
    public void delete(String shortCode) {
        try {
            redisTemplate.delete(cacheKey(shortCode));
            meterRegistry.counter(REDIRECT_CACHE_EVICTIONS.key(), OUTCOME.key(), SUCCESS.key()).increment();
        } catch (DataAccessException ignored) {
            meterRegistry.counter(REDIRECT_CACHE_EVICTIONS.key(), OUTCOME.key(), ERROR.key()).increment();
            // Postgres remains the source of truth if Redis is temporarily unavailable.
        }
    }

    private void recordCacheRequest(MetricTagValue outcome) {
        meterRegistry.counter(REDIRECT_CACHE_REQUESTS.key(), OUTCOME.key(), outcome.key()).increment();
    }

    private String cacheKey(String shortCode) {
        return RedisKeyPrefix.REDIRECT.key(shortCode);
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
