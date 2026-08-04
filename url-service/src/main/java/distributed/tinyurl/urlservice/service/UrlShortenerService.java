package distributed.tinyurl.urlservice.service;

import distributed.tinyurl.urlservice.dto.CreateUrlRequest;
import distributed.tinyurl.urlservice.dto.CreateUrlResponse;
import distributed.tinyurl.urlservice.dto.UrlStatsResponse;
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
    private final ClickEventPublisher clickEventPublisher;
    private final Clock clock;
    private final String baseUrl;

    public UrlShortenerService(
            UrlRepository urlRepository,
            ShortCodeGenerator shortCodeGenerator,
            ClickEventPublisher clickEventPublisher,
            Clock clock,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
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
        Url url = findByShortCodeOrThrow(shortCode);

        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new UrlExpiredException(shortCode);
        }

        clickEventPublisher.publish(new ClickRecordedEvent(shortCode, Instant.now(clock)));

        return url.getOriginalUrl();
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
