package distributed.tinyurl.urlservice.security;

import distributed.tinyurl.urlservice.config.JwtProperties;
import distributed.tinyurl.urlservice.config.RsaKeyUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import java.security.PublicKey;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidJwtException();
        }
    }

    private PublicKey publicKey() {
        return RsaKeyUtils.publicKeyFromBase64(jwtProperties.publicKey());
    }
}
