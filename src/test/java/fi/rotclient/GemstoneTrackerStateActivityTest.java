package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneTrackerStateActivityTest {
    private static final long PAUSE_WINDOW =
            60_000L;

    @Test
    void currentActiveTimeIncludesOpenBreakWindow() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.recordBlock(
                10_000L);

        assertTrue(
                state.isActive(
                        30_000L,
                        PAUSE_WINDOW));

        assertEquals(
                20_000L,
                state.currentSessionActiveMillis(
                        30_000L,
                        PAUSE_WINDOW));

        assertEquals(
                20_000L,
                state.currentTotalActiveMillis(
                        30_000L,
                        PAUSE_WINDOW));
    }

    @Test
    void consecutiveBlocksPersistElapsedActivity() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.recordBlock(
                10_000L);

        state.persistActiveTime(
                30_000L,
                PAUSE_WINDOW);

        state.recordBlock(
                30_000L);

        assertEquals(
                20_000L,
                state.sessionActiveMillis);

        assertEquals(
                20_000L,
                state.totalActiveMillis);

        assertEquals(
                30_000L,
                state.lastBreakEpochMillis);

        assertEquals(
                30_000L,
                state.currentSessionActiveMillis(
                        40_000L,
                        PAUSE_WINDOW));
    }

    @Test
    void expiredWindowIsCappedAndClosed() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.recordBlock(
                10_000L);

        state.persistActiveTime(
                80_001L,
                PAUSE_WINDOW);

        assertEquals(
                PAUSE_WINDOW,
                state.sessionActiveMillis);

        assertEquals(
                0L,
                state.lastBreakEpochMillis);

        assertFalse(
                state.isActive(
                        80_001L,
                        PAUSE_WINDOW));

        assertEquals(
                PAUSE_WINDOW,
                state.currentSessionActiveMillis(
                        90_000L,
                        PAUSE_WINDOW));
    }

    @Test
    void sessionResetClearsActivity() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.recordBlock(
                10_000L);

        state.persistActiveTime(
                20_000L,
                PAUSE_WINDOW);

        state.resetSession();

        assertEquals(
                0L,
                state.sessionActiveMillis);

        assertEquals(
                0L,
                state.sessionBlocks);

        assertEquals(
                0L,
                state.lastBreakEpochMillis);

        assertEquals(
                10_000L,
                state.totalActiveMillis);

        assertEquals(
                1L,
                state.totalBlocks);
    }

    @Test
    void rejectsInvalidActivityArguments() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        assertThrows(
                IllegalArgumentException.class,
                () -> state.currentSessionActiveMillis(
                        -1L,
                        PAUSE_WINDOW));

        assertThrows(
                IllegalArgumentException.class,
                () -> state.isActive(
                        1L,
                        0L));

        assertThrows(
                IllegalArgumentException.class,
                () -> state.currentTotalActiveMillis(
                        1L,
                        -1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> state.persistActiveTime(
                        -1L,
                        PAUSE_WINDOW));
    }

    @Test
    void normalizationRemovesNegativePersistedActivity() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.sessionActiveMillis = -1L;
        state.totalActiveMillis = -2L;
        state.lastBreakEpochMillis = -3L;

        state.normalize();

        assertEquals(0L, state.sessionActiveMillis);
        assertEquals(0L, state.totalActiveMillis);
        assertEquals(0L, state.lastBreakEpochMillis);
    }

    @Test
    void timestampsNearLongMaxRemainExact() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.sessionActiveMillis = Long.MAX_VALUE - 20L;
        state.totalActiveMillis = Long.MAX_VALUE - 20L;
        state.lastBreakEpochMillis = Long.MAX_VALUE - 10L;

        assertEquals(
                Long.MAX_VALUE - 10L,
                state.currentSessionActiveMillis(
                        Long.MAX_VALUE,
                        PAUSE_WINDOW));
        assertEquals(
                Long.MAX_VALUE - 10L,
                state.currentTotalActiveMillis(
                        Long.MAX_VALUE,
                        PAUSE_WINDOW));
    }

    @Test
    void activeTimeOverflowThrowsWithoutPartialMutation() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.sessionActiveMillis = 100L;
        state.totalActiveMillis = Long.MAX_VALUE - 5L;
        state.lastBreakEpochMillis = 10L;

        assertThrows(
                ArithmeticException.class,
                () -> state.persistActiveTime(
                        20L,
                        PAUSE_WINDOW));

        assertEquals(100L, state.sessionActiveMillis);
        assertEquals(Long.MAX_VALUE - 5L, state.totalActiveMillis);
        assertEquals(10L, state.lastBreakEpochMillis);
    }

    @Test
    void backwardClockDoesNotMoveOpenWindowOrPersistTime() {
        GemstoneTrackerState state =
                new GemstoneTrackerState();

        state.sessionActiveMillis = 50L;
        state.totalActiveMillis = 75L;
        state.lastBreakEpochMillis = 1_000L;

        assertEquals(
                50L,
                state.currentSessionActiveMillis(
                        900L,
                        PAUSE_WINDOW));
        assertFalse(
                state.isActive(
                        900L,
                        PAUSE_WINDOW));

        state.persistActiveTime(
                900L,
                PAUSE_WINDOW);

        assertEquals(50L, state.sessionActiveMillis);
        assertEquals(75L, state.totalActiveMillis);
        assertEquals(1_000L, state.lastBreakEpochMillis);
    }
}
