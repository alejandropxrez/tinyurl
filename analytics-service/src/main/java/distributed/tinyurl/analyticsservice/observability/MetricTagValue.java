package distributed.tinyurl.analyticsservice.observability;

public enum MetricTagValue {
    SUCCESS("success");

    private final String key;

    MetricTagValue(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
