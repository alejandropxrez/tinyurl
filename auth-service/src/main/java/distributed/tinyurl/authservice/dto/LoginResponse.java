package distributed.tinyurl.authservice.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) { }
