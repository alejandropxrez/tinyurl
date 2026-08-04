package distributed.tinyurl.urlservice.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Too many URL creation requests. Please try again later.");
    }
}
