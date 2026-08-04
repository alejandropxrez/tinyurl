package distributed.tinyurl.urlservice.dto;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record CreateUrlRequest(

        @NotBlank(message = "originalUrl cannot be empty")
        @URL(message = "originalUrl must be a valid URL")
        String originalUrl,

        Instant expiresAt
) { }