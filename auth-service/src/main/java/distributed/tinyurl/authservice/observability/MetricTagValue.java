package distributed.tinyurl.authservice.observability;

public enum MetricTagValue {
    FAILURE("failure"),
    SUCCESS("success");

    private final String key;

    MetricTagValue(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
