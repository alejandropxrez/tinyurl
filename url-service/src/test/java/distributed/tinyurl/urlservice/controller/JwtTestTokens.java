package distributed.tinyurl.urlservice.controller;

import io.jsonwebtoken.Jwts;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

final class JwtTestTokens {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private JwtTestTokens() {
    }

    static String publicKey() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
    }

    static String bearerToken() {
        Instant now = Instant.now();

        return "Bearer " + Jwts.builder()
                .subject("ada@example.com")
                .claim("userId", 1L)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(privateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private static PrivateKey privateKey() {
        return KEY_PAIR.getPrivate();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not generate test RSA keypair", ex);
        }
    }
}
