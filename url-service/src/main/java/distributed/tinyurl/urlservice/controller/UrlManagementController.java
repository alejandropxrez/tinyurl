package distributed.tinyurl.urlservice.controller;

import distributed.tinyurl.urlservice.dto.CreateUrlRequest;
import distributed.tinyurl.urlservice.dto.CreateUrlResponse;
import distributed.tinyurl.urlservice.dto.UrlStatsResponse;
import distributed.tinyurl.urlservice.exception.RateLimitExceededException;
import distributed.tinyurl.urlservice.ratelimit.CreateUrlRateLimiter;
import distributed.tinyurl.urlservice.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${app.api.base-path}/urls")
public class UrlManagementController {

    private final UrlShortenerService urlShortenerService;
    private final CreateUrlRateLimiter createUrlRateLimiter;

    public UrlManagementController(
            UrlShortenerService urlShortenerService,
            CreateUrlRateLimiter createUrlRateLimiter
    ) {
        this.urlShortenerService = urlShortenerService;
        this.createUrlRateLimiter = createUrlRateLimiter;
    }

    @PostMapping
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest servletRequest
    ) {
        if (!createUrlRateLimiter.isAllowed(servletRequest.getRemoteAddr())) {
            throw new RateLimitExceededException();
        }

        CreateUrlResponse response = urlShortenerService.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<UrlStatsResponse> getStats(@PathVariable String code) {
        return ResponseEntity.ok(urlShortenerService.getStats(code));
    }
}
