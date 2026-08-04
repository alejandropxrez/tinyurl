package distributed.tinyurl.urlservice.idgen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class SnowflakeIdGenerator {


    private static final long EPOCH = 1767225600000L;

    private static final long TIMESTAMP_BITS = 41L;
    private static final long NODE_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;

    static {
        assert TIMESTAMP_BITS + NODE_ID_BITS + SEQUENCE_BITS == 63
                : "The sum of bits must occupy exactly 63 bits (positive long).";
    }

    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;   // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4095

    // Tolerance for small backward clock adjustments (e.g., NTP adjustments).
    // Above this threshold, we assume something more serious and fail fast
    // instead of blocking ID generation indefinitely.
    private static final long MAX_CLOCK_DRIFT_MS = 5L;

    private static final long NODE_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + NODE_ID_BITS;

    private final long nodeId;
    private final Clock clock;

    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(@Value("${app.snowflake.node-id}") long nodeId, Clock clock) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException(
                    "node-id must be between 0 and " + MAX_NODE_ID + ", received: " + nodeId);
        }
        this.nodeId = nodeId;
        this.clock = clock;
    }

    public synchronized long nextId() {
        long currentTimestamp = currentTime();

        if (currentTimestamp < lastTimestamp) {
            long drift = lastTimestamp - currentTimestamp;
            if (drift > MAX_CLOCK_DRIFT_MS) {
                throw new IllegalStateException(
                        "System clock moved backwards by " + drift + "ms, exceeding the tolerated threshold ("
                                + MAX_CLOCK_DRIFT_MS + "ms); IDs cannot be generated safely");
            }

            // Small, tolerable drift: we wait for the clock to reach lastTimestamp.
            currentTimestamp = waitUntil(lastTimestamp);
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (nodeId << NODE_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long currentTimestamp) {
        while (currentTimestamp <= lastTimestamp) {
            Thread.onSpinWait();
            currentTimestamp = currentTime();
        }
        return currentTimestamp;
    }

    private long waitUntil(long targetTimestamp) {
        long now = currentTime();
        while (now < targetTimestamp) {
            Thread.onSpinWait();
            now = currentTime();
        }
        return now;
    }

    private long currentTime() {
        return clock.millis();
    }
}
