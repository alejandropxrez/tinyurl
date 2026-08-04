package distributed.tinyurl.urlservice.service;

import distributed.tinyurl.urlservice.dto.CreateUrlRequest;
import distributed.tinyurl.urlservice.dto.CreateUrlResponse;
import distributed.tinyurl.urlservice.dto.UrlStatsResponse;
import distributed.tinyurl.urlservice.cache.CachedRedirect;
import distributed.tinyurl.urlservice.cache.UrlRedirectCache;
import distributed.tinyurl.urlservice.events.ClickEventPublisher;
import distributed.tinyurl.urlservice.events.ClickRecordedEvent;
import distributed.tinyurl.urlservice.exception.UrlExpiredException;
import distributed.tinyurl.urlservice.exception.UrlNotFoundException;
import distributed.tinyurl.urlservice.idgen.ShortCodeGenerator;
import distributed.tinyurl.urlservice.model.Url;
import distributed.tinyurl.urlservice.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class UrlShortenerService {

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlRedirectCache urlRedirectCache;
    private final ClickEventPublisher clickEventPublisher;
    private final Clock clock;
    private final String baseUrl;

    public UrlShortenerService(
            UrlRepository urlRepository,
            ShortCodeGenerator shortCodeGenerator,
            UrlRedirectCache urlRedirectCache,
            ClickEventPublisher clickEventPublisher,
            Clock clock,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlRedirectCache = urlRedirectCache;
        this.clickEventPublisher = clickEventPublisher;
        this.clock = clock;
        this.baseUrl = baseUrl;
    }

    public CreateUrlResponse shorten(CreateUrlRequest request) {
        String shortCode = shortCodeGenerator.nextCode();

        Url url = Url.builder()
                .shortCode(shortCode)
                .originalUrl(request.originalUrl())
                .expiresAt(request.expiresAt())
                .clickCount(0L)
                .build();

        Url saved = urlRepository.save(url);

        return new CreateUrlResponse(
                saved.getShortCode(),
                baseUrl + "/" + saved.getShortCode(),
                saved.getOriginalUrl(),
                saved.getCreatedAt(),
                saved.getExpiresAt()
        );
    }

    public String resolve(String shortCode) {
        return urlRedirectCache.findByShortCode(shortCode)
                .map(cachedRedirect -> resolveFromCache(shortCode, cachedRedirect))
                .orElseGet(() -> resolveFromDatabase(shortCode));
    }

    private String resolveFromCache(String shortCode, CachedRedirect cachedRedirect) {
        if (cachedRedirect.expiresAt() != null && cachedRedirect.expiresAt().isBefore(Instant.now(clock))) {
            throw new UrlExpiredException(shortCode);
        }

        publishClick(shortCode);
        return cachedRedirect.originalUrl();
    }

    private String resolveFromDatabase(String shortCode) {
        Url url = findByShortCodeOrThrow(shortCode);

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new UrlExpiredException(shortCode);
        }

        urlRedirectCache.save(shortCode, new CachedRedirect(url.getOriginalUrl(), url.getExpiresAt()));
        publishClick(shortCode);

        return url.getOriginalUrl();
    }

    private void publishClick(String shortCode) {
        clickEventPublisher.publish(new ClickRecordedEvent(shortCode, Instant.now(clock)));
    }

    public UrlStatsResponse getStats(String shortCode) {
        Url url = findByShortCodeOrThrow(shortCode);

        return new UrlStatsResponse(
                url.getShortCode(),
                url.getOriginalUrl(),
                url.getClickCount(),
                url.getCreatedAt(),
                url.getExpiresAt()
        );
    }

    private Url findByShortCodeOrThrow(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }
}
