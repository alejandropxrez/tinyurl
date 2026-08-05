package distributed.tinyurl.authservice.observability;

public enum MetricName {
    AUTH_REGISTRATIONS("tinyurl_auth_registrations_total"),
    AUTH_LOGINS("tinyurl_auth_logins_total"),
    AUTH_REFRESHES("tinyurl_auth_refreshes_total"),
    AUTH_LOGOUTS("tinyurl_auth_logouts_total");

    private final String key;

    MetricName(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
