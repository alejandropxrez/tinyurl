package distributed.tinyurl.urlservice.observability;

public enum MetricTag {
    OPERATION("operation"),
    OUTCOME("outcome"),
    SOURCE("source");

    private final String key;

    MetricTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
