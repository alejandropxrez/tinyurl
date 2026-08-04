package distributed.tinyurl.analyticsservice.controller;

import distributed.tinyurl.analyticsservice.dto.UrlClickCountResponse;
import distributed.tinyurl.analyticsservice.service.AnalyticsQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/analytics/urls")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsController(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/{shortCode}/clicks")
    public ResponseEntity<UrlClickCountResponse> countClicks(@PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsQueryService.countClicks(shortCode));
    }
}
