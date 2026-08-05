package distributed.tinyurl.urlservice.controller;

import distributed.tinyurl.urlservice.dto.CreateUrlRequest;
import distributed.tinyurl.urlservice.dto.CreateUrlResponse;
import distributed.tinyurl.urlservice.dto.UrlSummaryResponse;
import distributed.tinyurl.urlservice.dto.UrlStatsResponse;
import distributed.tinyurl.urlservice.exception.RateLimitExceededException;
import distributed.tinyurl.urlservice.ratelimit.CreateUrlRateLimiter;
import distributed.tinyurl.urlservice.security.JwtPrincipal;
import distributed.tinyurl.urlservice.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        if (!createUrlRateLimiter.isAllowed(servletRequest.getRemoteAddr())) {
            throw new RateLimitExceededException();
        }

        CreateUrlResponse response = urlShortenerService.shorten(request, principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UrlSummaryResponse>> listMyUrls(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return ResponseEntity.ok(urlShortenerService.listByOwner(principal.userId()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<UrlStatsResponse> getStats(
            @PathVariable String code,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return ResponseEntity.ok(urlShortenerService.getStats(code, principal.userId()));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(
            @PathVariable String code,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        urlShortenerService.delete(code, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
