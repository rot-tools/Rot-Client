package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

final class AutoProfileSwitchTrackerTest {

    private static final long STABLE = AutoProfileSwitchTracker.STABLE_MILLIS;
    private static final long OTHER_STABLE =
            AutoProfileSwitchTracker.OTHER_STABLE_MILLIS;

    @Test
    void reportsOnlyAfterTheContextHasBeenStable() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        assertNull(tracker.observe(AutoProfileContext.DUNGEONS, 1_000L));
        assertNull(tracker.observe(AutoProfileContext.DUNGEONS, 1_000L + STABLE - 1));
        assertEquals(AutoProfileContext.DUNGEONS,
                tracker.observe(AutoProfileContext.DUNGEONS, 1_000L + STABLE));
    }

    @Test
    void reportsEachStableContextExactlyOnce() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        tracker.observe(AutoProfileContext.KUUDRA, 0L);
        assertEquals(AutoProfileContext.KUUDRA,
                tracker.observe(AutoProfileContext.KUUDRA, STABLE));
        assertNull(tracker.observe(AutoProfileContext.KUUDRA, STABLE + 1_000L));
        assertNull(tracker.observe(AutoProfileContext.KUUDRA, STABLE * 10));
    }

    @Test
    void aFlickeringContextNeverReports() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();
        long now = 0L;

        for (int i = 0; i < 20; i++) {
            AutoProfileContext flicker = i % 2 == 0
                    ? AutoProfileContext.DWARVEN_MINES
                    : AutoProfileContext.OTHER;
            assertNull(tracker.observe(flicker, now));
            now += STABLE - 1;
        }
    }

    @Test
    void anEmptyObservationRestartsTheStabilityWindow() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        tracker.observe(AutoProfileContext.GARDEN, 0L);
        assertNull(tracker.observe(null, STABLE - 500L));
        assertNull(tracker.observe(AutoProfileContext.GARDEN, STABLE));
        assertEquals(AutoProfileContext.GARDEN,
                tracker.observe(AutoProfileContext.GARDEN, STABLE * 2));
    }

    @Test
    void returningToAnEarlierContextReportsItAgain() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        tracker.observe(AutoProfileContext.DUNGEONS, 0L);
        assertEquals(AutoProfileContext.DUNGEONS,
                tracker.observe(AutoProfileContext.DUNGEONS, STABLE));

        long otherStart = STABLE + 1L;
        tracker.observe(AutoProfileContext.OTHER, otherStart);
        assertEquals(AutoProfileContext.OTHER,
                tracker.observe(AutoProfileContext.OTHER, otherStart + OTHER_STABLE));

        long backStart = otherStart + OTHER_STABLE + 1L;
        tracker.observe(AutoProfileContext.DUNGEONS, backStart);
        assertEquals(AutoProfileContext.DUNGEONS,
                tracker.observe(AutoProfileContext.DUNGEONS, backStart + STABLE));
    }

    @Test
    void aRecognisedPlaceIsQuickButElsewhereIsSlow() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        // Two sidebar reads in a row are enough for a recognised place.
        assertNull(tracker.observe(AutoProfileContext.DWARVEN_MINES, 0L));
        assertEquals(AutoProfileContext.DWARVEN_MINES,
                tracker.observe(AutoProfileContext.DWARVEN_MINES, 1_000L));

        // "Nothing recognised" must hold much longer before it counts.
        tracker.reset();
        assertNull(tracker.observe(AutoProfileContext.OTHER, 0L));
        assertNull(tracker.observe(AutoProfileContext.OTHER, 1_000L));
        assertNull(tracker.observe(AutoProfileContext.OTHER, 2_000L));
        assertEquals(AutoProfileContext.OTHER,
                tracker.observe(AutoProfileContext.OTHER, 3_000L));
    }

    @Test
    void aHalfLoadedSidebarNeverReportsElsewhere() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        // New island: the first read has the title but no location row yet.
        assertNull(tracker.observe(AutoProfileContext.OTHER, 0L));
        assertNull(tracker.observe(AutoProfileContext.OTHER, 1_000L));
        // The real rows arrive; only the real place may be reported.
        assertNull(tracker.observe(AutoProfileContext.CRYSTAL_HOLLOWS, 2_000L));
        assertEquals(AutoProfileContext.CRYSTAL_HOLLOWS,
                tracker.observe(AutoProfileContext.CRYSTAL_HOLLOWS, 3_000L));
        assertEquals(AutoProfileContext.CRYSTAL_HOLLOWS, tracker.lastReported());
    }

    @Test
    void resetMakesTheSameContextApplyAgain() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        tracker.observe(AutoProfileContext.DUNGEONS, 0L);
        assertEquals(AutoProfileContext.DUNGEONS,
                tracker.observe(AutoProfileContext.DUNGEONS, STABLE));

        tracker.reset();
        assertNull(tracker.lastReported());

        tracker.observe(AutoProfileContext.DUNGEONS, STABLE + 10L);
        assertEquals(AutoProfileContext.DUNGEONS,
                tracker.observe(AutoProfileContext.DUNGEONS, STABLE * 2 + 10L));
    }

    @Test
    void clockRollbackRestartsInsteadOfReporting() {
        AutoProfileSwitchTracker tracker = new AutoProfileSwitchTracker();

        tracker.observe(AutoProfileContext.SLAYER, 100_000L);
        assertNull(tracker.observe(AutoProfileContext.SLAYER, 50_000L));
        assertNull(tracker.observe(AutoProfileContext.SLAYER, 50_000L + STABLE - 1));
        assertEquals(AutoProfileContext.SLAYER,
                tracker.observe(AutoProfileContext.SLAYER, 50_000L + STABLE));
    }
}
