package distributed.tinyurl.urlservice.idgen;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdGeneratorTest {

    private static final long START_MILLIS = 1_800_000_000_000L; // Arbitrary date after the custom EPOCH

    @Test
    void generatesDifferentIdsForSequentialCallsInSameMillisecond() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock);

        long id1 = generator.nextId();
        long id2 = generator.nextId();

        // Same timestamp, same nodeId -> only the sequence changes by +1
        assertEquals(id1 + 1, id2);
    }

    @Test
    void differentNodesProduceDifferentIdsAtSameTimestamp() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);
        SnowflakeIdGenerator generatorNode1 = new SnowflakeIdGenerator(1, clock);
        SnowflakeIdGenerator generatorNode2 = new SnowflakeIdGenerator(2, clock);

        long idFromNode1 = generatorNode1.nextId();
        long idFromNode2 = generatorNode2.nextId();

        assertNotEquals(idFromNode1, idFromNode2);
    }

    @Test
    void rejectsNodeIdBelowMinimum() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);

        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1, clock));
    }

    @Test
    void rejectsNodeIdAboveMaximum() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);

        // 10-bit node ID -> maximum valid value is 1023
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(1024, clock));
    }

    @Test
    void acceptsBoundaryNodeIds() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);

        assertDoesNotThrow(() -> new SnowflakeIdGenerator(0, clock));
        assertDoesNotThrow(() -> new SnowflakeIdGenerator(1023, clock));
    }

    @Test
    void configuredNodeIdTakesPrecedenceOverHostname() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(7L, 1, false, "url-service-0", clock);

        long id = generator.nextId();
        long extractedNodeId = (id >> 12) & 1023;

        assertEquals(7, extractedNodeId);
    }

    @Test
    void derivesNodeIdFromStatefulSetHostnameWhenNodeIdIsNotConfigured() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);

        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1L, 1, true, "url-service-2", clock);

        long id = generator.nextId();
        long extractedNodeId = (id >> 12) & 1023;

        assertEquals(3, extractedNodeId);
    }

    @Test
    void throwsWhenNodeIdIsNotConfiguredAndHostnameHasNoOrdinal() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(START_MILLIS), ZoneOffset.UTC);

        assertThrows(IllegalStateException.class, () -> new SnowflakeIdGenerator(1L, 1, true, "url-service-abcd", clock));
    }

    @Test
    void sequenceOverflowWaitsForNextMillisecond() throws Exception {
        ManualClock clock = new ManualClock(START_MILLIS);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock);

        // Exhaust the entire sequence available within the same millisecond
        // (12 bits -> 4096 values)
        long lastIdInSameMillis = generator.nextId();
        for (int i = 1; i < 4096; i++) {
            lastIdInSameMillis = generator.nextId();
        }

        // The next nextId() call must wait (spin-wait) because the sequence
        // wrapped back to 0 while the timestamp has not changed yet. We run it
        // in another thread and release the spin by advancing the manual clock.
        try (ExecutorService executor = Executors.newSingleThreadExecutor())  {
            Future<Long> future = executor.submit(generator::nextId);

            Thread.sleep(50); // Give the thread time to enter the spin-wait
            clock.advance(1);

            long idAfterOverflow = future.get(2, TimeUnit.SECONDS);

            assertTrue(idAfterOverflow > lastIdInSameMillis);
        }
    }

    @Test
    void toleratesSmallClockDriftWithinThreshold() throws Exception {
        ManualClock clock = new ManualClock(START_MILLIS);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock);

        long id1 = generator.nextId();

        clock.advance(-3); // Move the clock back by 3 ms, within the tolerated threshold (5 ms)

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<Long> future = executor.submit(generator::nextId);

            Thread.sleep(50);
            clock.advance(3); // The clock "catches up" to its original value

            long id2 = future.get(2, TimeUnit.SECONDS);

            assertTrue(id2 > id1);
        }
    }

    @Test
    void throwsWhenClockDriftExceedsThreshold() {
        ManualClock clock = new ManualClock(START_MILLIS);
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, clock);

        generator.nextId();
        clock.advance(-10); // Move the clock back by 10 ms, exceeding the 5 ms threshold

        assertThrows(IllegalStateException.class, generator::nextId);
    }

    @Test
    void generatesUniqueIdsUnderConcurrentLoad() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, Clock.systemUTC());

        int threadCount = 8;
        int idsPerThread = 5_000;
        Set<Long> generatedIds = ConcurrentHashMap.newKeySet();
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < idsPerThread; i++) {
                            generatedIds.add(generator.nextId());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(10, TimeUnit.SECONDS));
        }

        assertEquals(threadCount * idsPerThread, generatedIds.size());
    }
}
