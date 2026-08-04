package distributed.tinyurl.urlservice.idgen;

public final class Base62Encoder {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();

    private Base62Encoder() {
    }

    public static String encode(long value) {
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        long n = value;

        while (n > 0) {
            int remainder = (int) (n % BASE);
            sb.append(ALPHABET.charAt(remainder));
            n /= BASE;
        }

        return sb.reverse().toString();
    }
}