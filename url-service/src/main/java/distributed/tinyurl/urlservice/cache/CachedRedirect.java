package distributed.tinyurl.urlservice.cache;

import java.time.Instant;

public record CachedRedirect(
        String originalUrl,
        Instant expiresAt
) { }
