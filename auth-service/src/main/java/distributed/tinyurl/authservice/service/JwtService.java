package distributed.tinyurl.authservice.service;

import distributed.tinyurl.authservice.config.JwtProperties;
import distributed.tinyurl.authservice.config.RsaKeyUtils;
import distributed.tinyurl.authservice.model.User;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String createAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.expiration());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(privateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public String extractSubject(String token) {
        return Jwts.parser()
                .verifyWith(publicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public long expiresInSeconds() {
        return jwtProperties.expiration().toSeconds();
    }

    private PrivateKey privateKey() {
        return RsaKeyUtils.privateKeyFromBase64(jwtProperties.privateKey());
    }

    private PublicKey publicKey() {
        return RsaKeyUtils.publicKeyFromBase64(jwtProperties.publicKey());
    }
}
