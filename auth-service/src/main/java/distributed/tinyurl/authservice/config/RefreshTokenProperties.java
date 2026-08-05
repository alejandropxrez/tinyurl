package distributed.tinyurl.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(Duration expiration) {

    public RefreshTokenProperties {
        if (expiration == null) {
            expiration = Duration.ofDays(30);
        }
    }
}
