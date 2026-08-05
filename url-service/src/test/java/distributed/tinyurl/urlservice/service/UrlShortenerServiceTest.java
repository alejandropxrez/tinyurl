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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-08-04T12:00:00Z");
    private static final String BASE_URL = "http://localhost:8081";

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private UrlRedirectCache urlRedirectCache;

    @Mock
    private ClickEventPublisher clickEventPublisher;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new UrlShortenerService(urlRepository, shortCodeGenerator, urlRedirectCache, clickEventPublisher, fixedClock, BASE_URL);
    }

    @Test
    void shortenSavesUrlAndReturnsFullShortUrl() {
        when(shortCodeGenerator.nextCode()).thenReturn("abc123X");
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url url = invocation.getArgument(0);
            url.setId(1L);
            url.setCreatedAt(FIXED_NOW);
            return url;
        });

        CreateUrlRequest request = new CreateUrlRequest("https://www.anthropic.com", null);
        CreateUrlResponse response = service.shorten(request, 1L);

        assertThat(response.shortCode()).isEqualTo("abc123X");
        assertThat(response.shortUrl()).isEqualTo(BASE_URL + "/abc123X");
        assertThat(response.originalUrl()).isEqualTo("https://www.anthropic.com");
        assertThat(response.createdAt()).isEqualTo(FIXED_NOW);

        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void resolveReturnsOriginalUrlWhenNotExpired() {
        Url url = Url.builder()
                .shortCode("abc123X")
                .originalUrl("https://www.anthropic.com")
                .expiresAt(FIXED_NOW.plusSeconds(3600)) // expira en el futuro
                .build();

        when(urlRedirectCache.findByShortCode("abc123X")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("abc123X")).thenReturn(Optional.of(url));

        String resolved = service.resolve("abc123X");

        assertThat(resolved).isEqualTo("https://www.anthropic.com");
        verify(urlRedirectCache).save("abc123X", new CachedRedirect("https://www.anthropic.com", FIXED_NOW.plusSeconds(3600)));
        verify(clickEventPublisher).publish(new ClickRecordedEvent("abc123X", FIXED_NOW));
    }

    @Test
    void resolveWorksWhenExpiresAtIsNull() {
        Url url = Url.builder()
                .shortCode("abc123X")
                .originalUrl("https://www.anthropic.com")
                .expiresAt(null)
                .build();

        when(urlRedirectCache.findByShortCode("abc123X")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("abc123X")).thenReturn(Optional.of(url));

        String resolved = service.resolve("abc123X");

        assertThat(resolved).isEqualTo("https://www.anthropic.com");
        verify(urlRedirectCache).save("abc123X", new CachedRedirect("https://www.anthropic.com", null));
        verify(clickEventPublisher).publish(new ClickRecordedEvent("abc123X", FIXED_NOW));
    }

    @Test
    void resolveUsesCacheWhenShortCodeIsCached() {
        CachedRedirect cachedRedirect = new CachedRedirect(
                "https://www.anthropic.com",
                FIXED_NOW.plusSeconds(3600)
        );

        when(urlRedirectCache.findByShortCode("abc123X")).thenReturn(Optional.of(cachedRedirect));

        String resolved = service.resolve("abc123X");

        assertThat(resolved).isEqualTo("https://www.anthropic.com");
        verify(urlRepository, never()).findByShortCode(anyString());
        verify(urlRedirectCache, never()).save(anyString(), any(CachedRedirect.class));
        verify(clickEventPublisher).publish(new ClickRecordedEvent("abc123X", FIXED_NOW));
    }

    @Test
    void resolveThrowsWhenCachedUrlExpired() {
        CachedRedirect cachedRedirect = new CachedRedirect(
                "https://www.anthropic.com",
                FIXED_NOW.minusSeconds(3600)
        );

        when(urlRedirectCache.findByShortCode("abc123X")).thenReturn(Optional.of(cachedRedirect));

        assertThatThrownBy(() -> service.resolve("abc123X"))
                .isInstanceOf(UrlExpiredException.class)
                .hasMessageContaining("abc123X");

        verify(urlRepository, never()).findByShortCode(anyString());
        verify(clickEventPublisher, never()).publish(any(ClickRecordedEvent.class));
    }

    @Test
    void resolveThrowsWhenUrlExpired() {
        Url url = Url.builder()
                .shortCode("abc123X")
                .originalUrl("https://www.anthropic.com")
                .expiresAt(FIXED_NOW.minusSeconds(3600)) // expired
                .build();

        when(urlRedirectCache.findByShortCode("abc123X")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("abc123X")).thenReturn(Optional.of(url));

        assertThatThrownBy(() -> service.resolve("abc123X"))
                .isInstanceOf(UrlExpiredException.class)
                .hasMessageContaining("abc123X");
        verify(clickEventPublisher, never()).publish(any(ClickRecordedEvent.class));
    }

    @Test
    void resolveThrowsWhenShortCodeDoesNotExist() {
        when(urlRedirectCache.findByShortCode("noexiste")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("noexiste"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("noexiste");
        verify(clickEventPublisher, never()).publish(any(ClickRecordedEvent.class));
    }

    @Test
    void getStatsReturnsCurrentClickCount() {
        Url url = Url.builder()
                .shortCode("abc123X")
                .originalUrl("https://www.anthropic.com")
                .userId(1L)
                .clickCount(42L)
                .createdAt(FIXED_NOW)
                .build();

        when(urlRepository.findByShortCodeAndUserId("abc123X", 1L)).thenReturn(Optional.of(url));

        UrlStatsResponse stats = service.getStats("abc123X", 1L);

        assertThat(stats.clickCount()).isEqualTo(42L);
        assertThat(stats.shortCode()).isEqualTo("abc123X");
    }

    @Test
    void getStatsThrowsWhenShortCodeDoesNotExist() {
        when(urlRepository.findByShortCodeAndUserId("noexiste", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStats("noexiste", 1L))
                .isInstanceOf(UrlNotFoundException.class);
    }
}
