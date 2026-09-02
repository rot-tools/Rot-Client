package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GemstoneTrackerStateTest {
    @Test
    void recordsGainIntoSessionAndTotalLedgers() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.recordGain(
                GemstoneTier.ROUGH,
                120L);

        state.recordGain(
                GemstoneTier.FLAWED,
                3L);

        assertEquals(
                120L,
                state.sessionLedger().quantity(
                        GemstoneTier.ROUGH));

        assertEquals(
                120L,
                state.totalLedger().quantity(
                        GemstoneTier.ROUGH));

        assertEquals(
                3L,
                state.sessionLedger().quantity(
                        GemstoneTier.FLAWED));

        assertEquals(
                3L,
                state.totalLedger().quantity(
                        GemstoneTier.FLAWED));
    }

    @Test
    void recordsBlocksAndActiveTime() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.recordBlock(5_000L);
        state.recordBlock(6_000L);
        state.addActiveMillis(1_500L);

        assertEquals(2L, state.sessionBlocks);
        assertEquals(2L, state.totalBlocks);

        assertEquals(
                1_500L,
                state.sessionActiveMillis);

        assertEquals(
                1_500L,
                state.totalActiveMillis);

        assertEquals(
                6_000L,
                state.lastBreakEpochMillis);
    }

    @Test
    void resetSessionPreservesLifetimeData() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.recordGain(
                GemstoneTier.FINE,
                2L);

        state.recordBlock(10_000L);
        state.addActiveMillis(2_000L);

        state.resetSession();

        assertEquals(
                0L,
                state.sessionLedger().quantity(
                        GemstoneTier.FINE));

        assertEquals(
                2L,
                state.totalLedger().quantity(
                        GemstoneTier.FINE));

        assertEquals(0L, state.sessionBlocks);
        assertEquals(1L, state.totalBlocks);

        assertEquals(
                0L,
                state.sessionActiveMillis);

        assertEquals(
                2_000L,
                state.totalActiveMillis);

        assertEquals(
                0L,
                state.lastBreakEpochMillis);
    }

    @Test
    void repairsNullLedgersAndNormalizesCounters() throws Exception {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        Field sessionLedgerField =
                GemstoneTrackerState.class.getDeclaredField("sessionLedger");
        sessionLedgerField.setAccessible(true);
        sessionLedgerField.set(state, null);

        Field totalLedgerField =
                GemstoneTrackerState.class.getDeclaredField("totalLedger");
        totalLedgerField.setAccessible(true);
        totalLedgerField.set(state, null);

        state.sessionBlocks = -3L;
        state.totalBlocks = -5L;
        state.sessionActiveMillis = -7L;
        state.totalActiveMillis = -11L;
        state.lastBreakEpochMillis = -13L;

        state.normalize();

        assertNotNull(state.sessionLedger());
        assertNotNull(state.totalLedger());
        assertEquals(0L, state.sessionBlocks);
        assertEquals(0L, state.totalBlocks);
        assertEquals(0L, state.sessionActiveMillis);
        assertEquals(0L, state.totalActiveMillis);
        assertEquals(0L, state.lastBreakEpochMillis);
    }

    @Test
    void preservesSeparateSessionAndLifetimeLedgers() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.sessionLedger().add(GemstoneTier.ROUGH, 4L);
        state.totalLedger().add(GemstoneTier.ROUGH, 7L);

        state.normalize();

        assertEquals(4L, state.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(7L, state.totalLedger().quantity(GemstoneTier.ROUGH));
    }

    @Test
    void rejectsInvalidValues() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        assertThrows(
                IllegalArgumentException.class,
                () -> state.recordGain(
                        null,
                        1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> state.recordGain(
                        GemstoneTier.ROUGH,
                        -1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> state.recordBlock(-1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> state.addActiveMillis(-1L));
    }
}
