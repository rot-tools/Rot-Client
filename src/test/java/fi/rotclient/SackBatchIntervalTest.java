package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

final class SackBatchIntervalTest {
    @Test
    void parsesLast24Seconds() {
        OptionalLong covered = SackBatchInterval.parseCoveredMillis(
                "[Sacks] +1,141 items. (Last 24s.)");
        assertTrue(covered.isPresent());
        assertEquals(24_000L, covered.getAsLong());
    }

    @Test
    void parsesLast11Seconds() {
        assertEquals(
                11_000L,
                SackBatchInterval.parseCoveredMillis(
                        "[Sacks] +80 items. (Last 11s.)").orElseThrow());
    }

    @Test
    void absentHintIsEmpty() {
        assertTrue(SackBatchInterval.parseCoveredMillis(
                "[Sacks] +10 items.").isEmpty());
    }

    @Test
    void clampsAbsurdDurations() {
        assertEquals(
                SackBatchInterval.MAX_COVERED_MILLIS,
                SackBatchInterval.parseCoveredMillis(
                        "[Sacks] +1 items. (Last 999s.)").orElseThrow());
    }

    @Test
    void effectiveWindowPrefersCoveredBatch() {
        assertEquals(24_000L, SackBatchInterval.effectiveWindowMillis(24_000L));
        assertEquals(
                SackBatchInterval.DEFAULT_COVERED_MILLIS,
                SackBatchInterval.effectiveWindowMillis(0L));
        assertFalse(SackBatchInterval.effectiveWindowMillis(5_000L) < 5_000L);
    }
}
