package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GemstoneDirectBreakTrackerTest {
    private static GemstoneDirectBreakTracker.Position position(
            int x,
            int y,
            int z) {
        return new GemstoneDirectBreakTracker.Position(x, y, z);
    }

    @Test
    void acceptsMatchingRecentRemoval() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        assertTrue(
                tracker.recordTarget(
                        100L,
                        GemstoneType.RUBY,
                        10_000L));

        assertEquals(
                GemstoneType.RUBY,
                tracker.acceptRemoval(
                        100L,
                        GemstoneType.RUBY,
                        true,
                        10_500L));
    }

    @Test
    void rejectsDifferentPosition() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        tracker.recordTarget(
                100L,
                GemstoneType.RUBY,
                10_000L);

        assertNull(
                tracker.acceptRemoval(
                        101L,
                        GemstoneType.RUBY,
                        true,
                        10_500L));
    }

    @Test
    void rejectsDifferentGemstone() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        tracker.recordTarget(
                100L,
                GemstoneType.RUBY,
                10_000L);

        assertNull(
                tracker.acceptRemoval(
                        100L,
                        GemstoneType.TOPAZ,
                        true,
                        10_500L));
    }

    @Test
    void rejectsExpiredTarget() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        tracker.recordTarget(
                100L,
                GemstoneType.JADE,
                10_000L);

        assertNull(
                tracker.acceptRemoval(
                        100L,
                        GemstoneType.JADE,
                        true,
                        12_501L));
    }

    @Test
    void rejectsNonAirTransition() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        tracker.recordTarget(
                100L,
                GemstoneType.SAPPHIRE,
                10_000L);

        assertNull(
                tracker.acceptRemoval(
                        100L,
                        GemstoneType.SAPPHIRE,
                        false,
                        10_500L));
    }

    @Test
    void acceptedTargetCannotBeReused() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        tracker.recordTarget(
                100L,
                GemstoneType.RUBY,
                10_000L);

        assertEquals(
                GemstoneType.RUBY,
                tracker.acceptRemoval(
                        100L,
                        GemstoneType.RUBY,
                        true,
                        10_500L));

        assertNull(
                tracker.acceptRemoval(
                        100L,
                        GemstoneType.RUBY,
                        true,
                        10_600L));
    }

    @Test
    void clearExpiredRemovesOldTarget() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        tracker.recordTarget(
                200L,
                GemstoneType.AMETHYST,
                20_000L);

        tracker.clearExpired(
                22_501L);

        assertNull(
                tracker.acceptRemoval(
                        200L,
                        GemstoneType.AMETHYST,
                        true,
                        22_502L));
    }

    @Test
    void nullGemstoneClearsTarget() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L);

        tracker.recordTarget(
                300L,
                GemstoneType.TOPAZ,
                30_000L);

        assertFalse(
                tracker.recordTarget(
                        300L,
                        null,
                        30_100L));

        assertNull(
                tracker.acceptRemoval(
                        300L,
                        GemstoneType.TOPAZ,
                        true,
                        30_200L));
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GemstoneDirectBreakTracker(
                        0L));

        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker();

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.recordTarget(
                        1L,
                        GemstoneType.RUBY,
                        -1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> tracker.acceptRemoval(
                        1L,
                        GemstoneType.RUBY,
                        true,
                        -1L));
    }

    @Test
    void acceptsAdjacentSameGemstoneAsSpreadWithoutConsumingTarget() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L,
                        500L);

        tracker.recordTarget(
                position(100, 64, 100),
                GemstoneType.RUBY,
                10_000L);

        GemstoneDirectBreakTracker.Acceptance spread =
                tracker.acceptRemoval(
                        position(101, 64, 100),
                        GemstoneType.RUBY,
                        true,
                        10_100L);

        assertEquals(
                GemstoneType.RUBY,
                spread.gemstone());
        assertEquals(
                GemstoneDirectBreakTracker.Evidence.GEMSTONE_SPREAD,
                spread.evidence());

        GemstoneDirectBreakTracker.Acceptance target =
                tracker.acceptRemoval(
                        position(100, 64, 100),
                        GemstoneType.RUBY,
                        true,
                        10_120L);

        assertEquals(
                GemstoneType.RUBY,
                target.gemstone());
        assertEquals(
                GemstoneDirectBreakTracker.Evidence.DIRECT_TARGET,
                target.evidence());
    }

    @Test
    void spreadCanBreakAnAdjacentDifferentGemstone() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L,
                        500L);

        tracker.recordTarget(
                position(10, 20, 30),
                GemstoneType.RUBY,
                20_000L);

        GemstoneDirectBreakTracker.Acceptance spread =
                tracker.acceptRemoval(
                        position(11, 21, 30),
                        GemstoneType.TOPAZ,
                        true,
                        20_100L);

        assertEquals(
                GemstoneType.TOPAZ,
                spread.gemstone());
        assertEquals(
                GemstoneDirectBreakTracker.Evidence.GEMSTONE_SPREAD,
                spread.evidence());
    }

    @Test
    void rejectsNonAdjacentOrLateSpreadCandidates() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L,
                        500L);

        tracker.recordTarget(
                position(0, 64, 0),
                GemstoneType.JADE,
                30_000L);

        assertNull(
                tracker.acceptRemoval(
                        position(2, 64, 0),
                        GemstoneType.JADE,
                        true,
                        30_100L));

        assertNull(
                tracker.acceptRemoval(
                        position(1, 64, 0),
                        GemstoneType.JADE,
                        true,
                        30_501L));
    }

    @Test
    void acceptsEachSpreadPositionOnlyOnce() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L,
                        500L);

        tracker.recordTarget(
                position(0, 64, 0),
                GemstoneType.AMBER,
                40_000L);

        assertEquals(
                GemstoneDirectBreakTracker.Evidence.GEMSTONE_SPREAD,
                tracker.acceptRemoval(
                                position(1, 64, 0),
                                GemstoneType.AMBER,
                                true,
                                40_100L)
                        .evidence());

        assertNull(
                tracker.acceptRemoval(
                        position(1, 64, 0),
                        GemstoneType.AMBER,
                        true,
                        40_120L));
    }

    @Test
    void laterMiningActionCanReuseARegeneratedPosition() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L,
                        500L);

        GemstoneDirectBreakTracker.Position target =
                position(5, 70, 5);

        tracker.recordTarget(
                target,
                GemstoneType.SAPPHIRE,
                50_000L);

        assertEquals(
                GemstoneDirectBreakTracker.Evidence.DIRECT_TARGET,
                tracker.acceptRemoval(
                                target,
                                GemstoneType.SAPPHIRE,
                                true,
                                50_100L)
                        .evidence());

        tracker.recordTarget(
                target,
                GemstoneType.SAPPHIRE,
                53_000L);

        assertEquals(
                GemstoneDirectBreakTracker.Evidence.DIRECT_TARGET,
                tracker.acceptRemoval(
                                target,
                                GemstoneType.SAPPHIRE,
                                true,
                                53_100L)
                        .evidence());
    }

    @Test
    void currentHypixelLimitAcceptsAtMostOneSpreadBlockPerAction() {
        GemstoneDirectBreakTracker tracker =
                new GemstoneDirectBreakTracker(
                        2_500L,
                        500L);

        tracker.recordTarget(
                position(0, 64, 0),
                GemstoneType.PERIDOT,
                60_000L);

        assertEquals(
                GemstoneDirectBreakTracker.Evidence.GEMSTONE_SPREAD,
                tracker.acceptRemoval(
                                position(1, 64, 0),
                                GemstoneType.PERIDOT,
                                true,
                                60_100L)
                        .evidence());

        assertNull(
                tracker.acceptRemoval(
                        position(-1, 64, 0),
                        GemstoneType.PERIDOT,
                        true,
                        60_120L));
    }
}
