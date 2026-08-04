package distributed.tinyurl.urlservice.exception;

public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String shortCode) {
        super("The URL for code" + shortCode + "has expired");
    }
}