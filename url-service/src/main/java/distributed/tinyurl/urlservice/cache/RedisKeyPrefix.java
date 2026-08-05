package distributed.tinyurl.urlservice.cache;

public enum RedisKeyPrefix {
    REDIRECT("redirect:"),
    CREATE_URL_RATE_LIMIT("rate-limit:create-url:");

    private final String key;

    RedisKeyPrefix(String key) {
        this.key = key;
    }

    public String key(String suffix) {
        return key + suffix;
    }
}
