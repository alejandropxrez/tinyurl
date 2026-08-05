package distributed.tinyurl.authservice.observability;

public enum AuthMetricOperation {
    REFRESH(MetricName.AUTH_REFRESHES),
    LOGOUT(MetricName.AUTH_LOGOUTS);

    private final MetricName metricName;

    AuthMetricOperation(MetricName metricName) {
        this.metricName = metricName;
    }

    public MetricName metricName() {
        return metricName;
    }
}
