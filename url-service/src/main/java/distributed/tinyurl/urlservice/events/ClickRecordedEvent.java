package distributed.tinyurl.urlservice.events;

import java.time.Instant;

public record ClickRecordedEvent(
        String shortCode,
        Instant clickedAt
) { }
