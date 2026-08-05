package distributed.tinyurl.urlservice.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record UpdateUrlRequest(
        @Size(min = 1, message = "originalUrl cannot be empty")
        @URL(message = "originalUrl must be a valid URL")
        String originalUrl,

        Instant expiresAt
) {
}
