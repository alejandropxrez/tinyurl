package distributed.tinyurl.urlservice.cache;

import java.util.Optional;

public interface UrlRedirectCache {
    Optional<CachedRedirect> findByShortCode(String shortCode);

    void save(String shortCode, CachedRedirect redirect);
}
