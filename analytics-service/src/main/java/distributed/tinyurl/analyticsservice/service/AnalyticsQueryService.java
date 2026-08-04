package distributed.tinyurl.analyticsservice.service;

import distributed.tinyurl.analyticsservice.dto.UrlClickCountResponse;
import distributed.tinyurl.analyticsservice.repository.ClickEventRepository;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsQueryService {

    private final ClickEventRepository clickEventRepository;

    public AnalyticsQueryService(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    public UrlClickCountResponse countClicks(String shortCode) {
        long clicks = clickEventRepository.countByShortCode(shortCode);
        return new UrlClickCountResponse(shortCode, clicks);
    }
}
