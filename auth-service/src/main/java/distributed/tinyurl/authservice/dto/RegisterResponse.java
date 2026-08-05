package distributed.tinyurl.authservice.dto;

import java.time.Instant;

public record RegisterResponse(
        Long id,
        String email,
        Instant createdAt
) { }
