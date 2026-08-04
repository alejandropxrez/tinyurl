package distributed.tinyurl.urlservice.idgen;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mutable clock for tests: allows explicit control over the passage of time,
 * including advancing it from another thread while the main test is blocked
 * in a spin-wait.
 */
class ManualClock extends Clock {

    private final AtomicLong millis;

    ManualClock(long startMillis) {
        this.millis = new AtomicLong(startMillis);
    }

    void advance(long deltaMillis) {
        millis.addAndGet(deltaMillis);
    }

    @Override
    public long millis() {
        return millis.get();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(millis.get());
    }
}