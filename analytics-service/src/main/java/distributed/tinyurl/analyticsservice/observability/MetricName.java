package distributed.tinyurl.analyticsservice.observability;

public enum MetricName {
    CLICK_EVENTS_CONSUMED("tinyurl_click_events_consumed_total"),
    CLICK_COUNT_QUERIES("tinyurl_analytics_click_count_queries_total");

    private final String key;

    MetricName(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
