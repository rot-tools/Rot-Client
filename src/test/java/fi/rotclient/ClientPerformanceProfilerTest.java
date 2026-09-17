package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ClientPerformanceProfilerTest {
    @BeforeEach
    void reset() {
        ClientPerformanceProfiler.resetForTests();
        ClientBoundaryGuard.resetFailureCountForTests();
    }

    @Test
    void disabledProfilerDoesNotCollectSamples() {
        assertFalse(ClientPerformanceProfiler.isEnabled());
        assertEquals(0L, ClientPerformanceProfiler.beginSample());

        ClientPerformanceProfiler.recordSample(
                "DUNGEON_RUNTIME",
                5_000_000L);

        assertTrue(
                ClientPerformanceProfiler
                        .snapshot()
                        .entries()
                        .isEmpty());
    }

    @Test
    void enabledProfilerAggregatesBoundarySamples() {
        ClientPerformanceProfiler.enable();

        ClientPerformanceProfiler.recordSample(
                "DUNGEON_RUNTIME",
                2_000_000L);
        ClientPerformanceProfiler.recordSample(
                "DUNGEON_RUNTIME",
                4_000_000L);

        ClientPerformanceProfiler.Snapshot snapshot =
                ClientPerformanceProfiler.snapshot();

        assertTrue(snapshot.enabled());
        assertEquals(1, snapshot.entries().size());

        ClientPerformanceProfiler.Entry entry =
                snapshot.entries().getFirst();

        assertEquals("DUNGEON_RUNTIME", entry.boundary());
        assertEquals(2L, entry.calls());
        assertEquals(6_000_000L, entry.totalNanos());
        assertEquals(4_000_000L, entry.maxNanos());
        assertEquals(3.0D, entry.averageMillis(), 0.0001D);
        assertEquals(4.0D, entry.maxMillis(), 0.0001D);
        assertEquals(6.0D, entry.totalMillis(), 0.0001D);
    }

    @Test
    void snapshotIsSortedByTotalTimeDescending() {
        ClientPerformanceProfiler.enable();

        ClientPerformanceProfiler.recordSample(
                "FAST",
                1_000_000L);
        ClientPerformanceProfiler.recordSample(
                "SLOW",
                8_000_000L);
        ClientPerformanceProfiler.recordSample(
                "MEDIUM",
                3_000_000L);

        var entries =
                ClientPerformanceProfiler
                        .snapshot()
                        .entries();

        assertEquals("SLOW", entries.get(0).boundary());
        assertEquals("MEDIUM", entries.get(1).boundary());
        assertEquals("FAST", entries.get(2).boundary());
    }

    @Test
    void resetClearsSamplesWithoutChangingEnabledState() {
        ClientPerformanceProfiler.enable();
        ClientPerformanceProfiler.recordSample(
                "AREA_DETECT",
                1_000_000L);

        ClientPerformanceProfiler.reset();

        ClientPerformanceProfiler.Snapshot snapshot =
                ClientPerformanceProfiler.snapshot();

        assertTrue(snapshot.enabled());
        assertTrue(snapshot.entries().isEmpty());
    }

    @Test
    void disablePreservesCollectedSnapshot() {
        ClientPerformanceProfiler.enable();
        ClientPerformanceProfiler.recordSample(
                "SLAYER_RUNTIME",
                2_000_000L);

        ClientPerformanceProfiler.disable();

        ClientPerformanceProfiler.Snapshot snapshot =
                ClientPerformanceProfiler.snapshot();

        assertFalse(snapshot.enabled());
        assertEquals(1, snapshot.entries().size());
    }

    @Test
    void clientBoundaryGuardFeedsProfilerWhenEnabled() {
        ClientPerformanceProfiler.enable();

        ClientBoundaryGuard.run(
                "UNIT_TEST_BOUNDARY",
                () -> {
                    int ignored = 1 + 1;
                });

        ClientPerformanceProfiler.Snapshot snapshot =
                ClientPerformanceProfiler.snapshot();

        assertEquals(1, snapshot.entries().size());
        assertEquals(
                "UNIT_TEST_BOUNDARY",
                snapshot.entries().getFirst().boundary());
        assertEquals(
                1L,
                snapshot.entries().getFirst().calls());
    }

    @Test
    void containedFailureStillProducesOneTimingSample() {
        ClientPerformanceProfiler.enable();

        ClientBoundaryGuard.run(
                "FAILING_BOUNDARY",
                () -> {
                    throw new IllegalStateException("simulated");
                });

        ClientPerformanceProfiler.Snapshot snapshot =
                ClientPerformanceProfiler.snapshot();

        assertEquals(1L, ClientBoundaryGuard.failureCount());
        assertEquals(1, snapshot.entries().size());
        assertEquals(
                "FAILING_BOUNDARY",
                snapshot.entries().getFirst().boundary());
        assertEquals(
                1L,
                snapshot.entries().getFirst().calls());
    }
}