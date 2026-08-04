package distributed.tinyurl.urlservice.exception;

public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String shortCode) {
        super("No URL was found for the code: " + shortCode);
    }
}