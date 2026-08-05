package distributed.tinyurl.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String privateKey, String publicKey, Duration expiration) {
}
