package distributed.tinyurl.urlservice.observability;

public enum MetricTagValue {
    ALLOWED("allowed"),
    BLOCKED("blocked"),
    CACHE("cache"),
    CREATE_URL("create_url"),
    DATABASE("database"),
    ERROR("error"),
    ERROR_ALLOWED("error_allowed"),
    EXPIRED("expired"),
    HIT("hit"),
    MISS("miss"),
    SUCCESS("success");

    private final String key;

    MetricTagValue(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
