package distributed.tinyurl.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken cannot be empty")
        String refreshToken
) {
}
