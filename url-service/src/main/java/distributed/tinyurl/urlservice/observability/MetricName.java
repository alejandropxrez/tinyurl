package distributed.tinyurl.urlservice.observability;

public enum MetricName {
    URL_CREATIONS("tinyurl.url.creations"),
    URL_DELETIONS("tinyurl.url.deletions"),
    URL_UPDATES("tinyurl.url.updates"),
    REDIRECTS("tinyurl_redirects_total"),
    REDIRECT_CACHE_REQUESTS("tinyurl_redirect_cache_requests_total"),
    REDIRECT_CACHE_WRITES("tinyurl_redirect_cache_writes_total"),
    REDIRECT_CACHE_EVICTIONS("tinyurl_redirect_cache_evictions_total"),
    CLICK_EVENTS_PUBLISHED("tinyurl_click_events_published_total"),
    RATE_LIMIT_CHECKS("tinyurl_rate_limit_checks_total");

    private final String key;

    MetricName(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
