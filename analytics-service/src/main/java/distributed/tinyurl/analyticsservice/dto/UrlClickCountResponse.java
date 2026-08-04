package distributed.tinyurl.analyticsservice.dto;

public record UrlClickCountResponse(
        String shortCode,
        long clicks
) { }
