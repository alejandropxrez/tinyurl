package distributed.tinyurl.urlservice.resilience;

public enum ResilienceInstance {
    RABBIT_CLICK_EVENTS("rabbitClickEvents"),
    REDIS_REDIRECT_CACHE("redisRedirectCache"),
    REDIS_CREATE_URL_RATE_LIMITER("redisCreateUrlRateLimiter");

    private final String key;

    ResilienceInstance(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
