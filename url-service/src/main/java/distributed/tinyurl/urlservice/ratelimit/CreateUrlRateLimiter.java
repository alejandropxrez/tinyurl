package distributed.tinyurl.urlservice.ratelimit;

public interface CreateUrlRateLimiter {
    boolean isAllowed(String clientId);
}
