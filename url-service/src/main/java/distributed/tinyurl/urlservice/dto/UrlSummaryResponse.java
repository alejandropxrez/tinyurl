package distributed.tinyurl.urlservice.dto;

import java.time.Instant;

public record UrlSummaryResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        Instant createdAt,
        Instant expiresAt,
        Long clickCount
) { }
