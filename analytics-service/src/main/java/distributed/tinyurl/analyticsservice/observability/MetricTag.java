package distributed.tinyurl.analyticsservice.observability;

public enum MetricTag {
    OUTCOME("outcome");

    private final String key;

    MetricTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
