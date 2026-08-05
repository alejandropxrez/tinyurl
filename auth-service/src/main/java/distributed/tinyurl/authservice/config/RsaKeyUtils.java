package distributed.tinyurl.authservice.config;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyUtils {

    private RsaKeyUtils() {
    }

    public static PrivateKey privateKeyFromBase64(String base64Key) {
        try {
            byte[] encoded = Base64.getDecoder().decode(base64Key);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA private key", ex);
        }
    }

    public static PublicKey publicKeyFromBase64(String base64Key) {
        try {
            byte[] encoded = Base64.getDecoder().decode(base64Key);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA public key", ex);
        }
    }
}
