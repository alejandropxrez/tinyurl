package distributed.tinyurl.urlservice.security;

public record JwtPrincipal(Long userId, String email) {
}
