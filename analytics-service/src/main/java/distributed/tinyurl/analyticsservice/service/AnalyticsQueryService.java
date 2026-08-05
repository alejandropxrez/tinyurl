package distributed.tinyurl.analyticsservice.service;

import distributed.tinyurl.analyticsservice.dto.UrlClickCountResponse;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import static distributed.tinyurl.analyticsservice.observability.MetricName.CLICK_COUNT_QUERIES;

@Service
public class AnalyticsQueryService {

    private final ClickEventRepository clickEventRepository;
    private final MeterRegistry meterRegistry;

    public AnalyticsQueryService(ClickEventRepository clickEventRepository, MeterRegistry meterRegistry) {
        this.clickEventRepository = clickEventRepository;
        this.meterRegistry = meterRegistry;
    }

    public UrlClickCountResponse countClicks(String shortCode) {
        long clicks = clickEventRepository.countByShortCode(shortCode);
        meterRegistry.counter(CLICK_COUNT_QUERIES.key()).increment();
        return new UrlClickCountResponse(shortCode, clicks);
    }
}
