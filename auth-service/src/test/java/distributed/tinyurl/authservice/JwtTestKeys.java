package distributed.tinyurl.authservice;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

final class JwtTestKeys {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private JwtTestKeys() {
    }

    static String privateKey() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPrivate().getEncoded());
    }

    static String publicKey() {
        return Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded());
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
