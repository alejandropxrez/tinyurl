package distributed.tinyurl.analyticsservice.events;

import java.time.Instant;

public record ClickRecordedEvent(
        String shortCode,
        Instant clickedAt
) { }
