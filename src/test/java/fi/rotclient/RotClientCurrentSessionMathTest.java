package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RotClientCurrentSessionMathTest {
    @Test
    void otherMinedQuantityIsNarrowerThanHudOthers() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items =
                List.of(
                        item("GOLD_INGOT", 1000L, SessionSourceType.MINING,
                                MiningClassification.OTHER, 10.0),
                        item("MITHRIL_ORE", 400L, SessionSourceType.MINING,
                                MiningClassification.OTHER, 20.0),
                        item("HARD_STONE", 25L, SessionSourceType.MINING,
                                MiningClassification.OTHER, 5.0),
                        item("DIAMOND", 999L, SessionSourceType.MINING,
                                MiningClassification.TARGET, 50.0),
                        item("CHEST_ITEM", 3L, SessionSourceType.CHEST,
                                null, 1.0),
                        item("MOB_DROP", 2L, SessionSourceType.MOB,
                                null, 4.0),
                        item("COINS", 500L, SessionSourceType.CURRENCY,
                                null, 0.0),
                        item("UNKNOWN_NOISE", 7L, SessionSourceType.UNATTRIBUTED,
                                null, 0.0));
        // Analytics OTHER_MINED stays mining-only.
        assertEquals(1425L, RotClientCurrentSessionMath.sumOtherMinedQuantity(items));
        assertEquals(35.0,
                RotClientCurrentSessionMath.sumOtherMinedResolvedValue(items),
                0.0001);
        // HUD OTHERS adds chest + mob item units, still excludes TARGET /
        // CURRENCY / UNATTRIBUTED.
        assertEquals(1430L, RotClientCurrentSessionMath.sumHudOthersQuantity(items));
        assertEquals(40.0,
                RotClientCurrentSessionMath.sumHudOthersResolvedValue(items),
                0.0001);
        MiningHudOtherSummary summary =
                RotClientCurrentSessionMath.toHudOtherSummary(configWith(items));
        assertTrue(summary.analyticsActive());
        assertEquals(1430L, summary.totalQuantity());
        assertEquals(40.0, summary.resolvedGrossValue(), 0.0001);
        assertEquals(1425L, summary.slice(SessionSourceType.MINING).quantity());
        assertEquals(3L, summary.slice(SessionSourceType.CHEST).quantity());
        assertEquals(2L, summary.slice(SessionSourceType.MOB).quantity());
        assertEquals(0L, summary.slice(SessionSourceType.CURRENCY).quantity());
        assertEquals(0L, summary.slice(SessionSourceType.UNATTRIBUTED).quantity());
    }

    @Test
    void nativeCoinsStayOutOfHudOthersAndProjectToAnalytics() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items =
                List.of(
                        item("ROTTEN_FLESH", 4L, SessionSourceType.MOB,
                                null, 8.0),
                        item("COINS", 12L, SessionSourceType.MOB,
                                null, 12.0));
        assertEquals(4L, RotClientCurrentSessionMath.sumHudOthersQuantity(items));
        assertEquals(2, RotClientCurrentSessionMath.countMobRows(items));
        assertEquals(20.0, RotClientCurrentSessionMath.sumMobResolvedValue(items),
                0.0001);
        assertEquals(12L,
                RotClientCurrentSessionMath.quantitiesForNativeCoins(items)
                        .get("COINS").quantity());

        RotClientCurrentSessionConfig config = configWith(items);
        MiningSessionAnalyticsViewModel model =
                new MiningSessionAnalyticsProjector().project(
                        java.util.Optional.empty(), config);
        assertEquals(12L, model.currencyQuantities().get("COINS").quantity());
        assertEquals(12.0 + 8.0, model.resolvedItemValue().doubleValue(), 0.0001);
        assertEquals(2, model.entryCount());
        assertEquals(1, model.currencyEntryCount());
    }

    @Test
    void countOtherUnresolvedIgnoresResolvedRows() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items =
                new ArrayList<>();
        items.add(item("A", 1L, SessionSourceType.MINING,
                MiningClassification.OTHER, 1.0));
        items.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                "B",
                "B",
                2L,
                SessionSourceType.MINING.name(),
                MiningClassification.OTHER.name(),
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(),
                false,
                "UNAVAILABLE",
                0.0));
        assertEquals(1, RotClientCurrentSessionMath.countOtherUnresolved(items));
    }

    @Test
    void closeSegmentAndOpenKeepsHistory() {
        List<RotClientCurrentSessionConfig.TargetSegment> segments =
                new ArrayList<>();
        segments.add(new RotClientCurrentSessionConfig.TargetSegment(
                "GOLD", 1000L, 0L));
        List<RotClientCurrentSessionConfig.TargetSegment> next =
                RotClientCurrentSessionMath.closeSegmentAndOpen(
                        segments, "DIAMOND", 2000L);
        assertEquals(2, next.size());
        assertEquals("GOLD", next.get(0).targetId());
        assertEquals(2000L, next.get(0).endedAtMillis());
        assertEquals("DIAMOND", next.get(1).targetId());
        assertTrue(next.get(1).isOpen());
    }

    @Test
    void instantTargetAndAreaTransitionsDoNotCreatePhantomSegments() {
        List<RotClientCurrentSessionConfig.TargetSegment> targets =
                RotClientCurrentSessionMath.closeSegmentAndOpen(
                        List.of(new RotClientCurrentSessionConfig.TargetSegment(
                                "GOLD", 2_000L, 0L)),
                        "DIAMOND",
                        2_000L);
        assertEquals(1, targets.size());
        assertEquals("DIAMOND", targets.getFirst().targetId());
        assertTrue(targets.getFirst().isOpen());

        List<RotClientCurrentSessionConfig.AreaSegment> areas =
                RotClientCurrentSessionMath.closeAreaSegmentAndOpen(
                        List.of(new RotClientCurrentSessionConfig.AreaSegment(
                                SkyBlockArea.DWARVEN_MINES.id(),
                                2_000L,
                                0L)),
                        SkyBlockArea.CRYSTAL_HOLLOWS.id(),
                        2_000L);
        assertEquals(1, areas.size());
        assertEquals(
                SkyBlockArea.CRYSTAL_HOLLOWS.id(),
                areas.getFirst().areaId());
        assertTrue(areas.getFirst().isOpen());
    }

    @Test
    void archiveIncrementsDisplayNumber() {
        RotClientCurrentSessionConfig current =
                RotClientCurrentSessionConfig.defaults();
        current.displayNumber = 3;
        current.nextDisplayNumber = 4;
        RotClientCurrentSessionConfig next =
                RotClientCurrentSessionMath.archiveAndStartNext(current, 5000L);
        assertEquals(4, next.displayNumber);
        assertEquals(5, next.nextDisplayNumber);
        assertTrue(next.items.isEmpty());
        assertEquals(RotClientCurrentSessionConfig.STATE_ACTIVE, next.state);
        assertFalse(next.sessionId.equals(current.sessionId));
    }

    @Test
    void activeDurationExcludesClosedPauses() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.startedAtMillis = 0L;
        config.totalPausedMillis = 3_000L;
        config.state = RotClientCurrentSessionConfig.STATE_ACTIVE;

        assertEquals(
                7_000L,
                RotClientCurrentSessionMath.activeDurationMillis(config, 10_000L));
    }

    @Test
    void activeDurationExcludesInProgressPause() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.startedAtMillis = 0L;
        config.totalPausedMillis = 1_000L;
        config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
        config.pausedAtMillis = 8_000L;

        // 10s elapsed - 1s already-closed pause - 2s of the current
        // in-progress pause (8s -> 10s) = 7s active.
        assertEquals(
                7_000L,
                RotClientCurrentSessionMath.activeDurationMillis(config, 10_000L));
    }

    @Test
    void activeDurationNeverGoesNegative() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.startedAtMillis = 5_000L;
        config.totalPausedMillis = 999_999L;
        config.state = RotClientCurrentSessionConfig.STATE_ACTIVE;

        assertEquals(
                0L,
                RotClientCurrentSessionMath.activeDurationMillis(config, 10_000L));
        assertEquals(
                0L,
                RotClientCurrentSessionMath.activeDurationMillis(null, 10_000L));
    }

    @Test
    void quantitiesForSourceKeepsMobSeparateFromOtherMined() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                item("HARD_STONE", 50L, SessionSourceType.MINING,
                        MiningClassification.OTHER, 1.0),
                item("MOB_DROP", 3L, SessionSourceType.MOB, null, 2.0),
                item("CHEST_ITEM", 2L, SessionSourceType.CHEST, null, 3.0));

        assertEquals(
                1,
                RotClientCurrentSessionMath.quantitiesForOtherMined(items).size());
        assertEquals(
                50L,
                RotClientCurrentSessionMath.quantitiesForOtherMined(items)
                        .get("HARD_STONE").quantity());
        assertEquals(
                3L,
                RotClientCurrentSessionMath.quantitiesForSource(
                        items, SessionSourceType.MOB).get("MOB_DROP").quantity());
        assertEquals(
                2L,
                RotClientCurrentSessionMath.quantitiesForSource(
                        items, SessionSourceType.CHEST)
                        .get("CHEST_ITEM").quantity());
    }

    @Test
    void ensureSession1IfMissingFillsIdentity() {
        RotClientCurrentSessionConfig config =
                new RotClientCurrentSessionConfig();
        config.sessionId = "";
        config.displayNumber = 0;
        RotClientCurrentSessionConfig fixed =
                RotClientCurrentSessionMath.ensureSession1IfMissing(config);
        assertFalse(fixed.sessionId.isBlank());
        assertEquals(1, fixed.displayNumber);
    }

    @Test
    void crashRecoveryClosesActivitySegmentsAtLastHeartbeat() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        long started = config.startedAtMillis;
        config.currentTargetId = "GOLD";
        config.currentAreaId = SkyBlockArea.DWARVEN_MINES.id();
        config.targetSegments = new ArrayList<>(List.of(
                new RotClientCurrentSessionConfig.TargetSegment(
                        "GOLD", started, 0L)));
        config.areaSegments = new ArrayList<>(List.of(
                new RotClientCurrentSessionConfig.AreaSegment(
                        SkyBlockArea.DWARVEN_MINES.id(), started, 0L)));
        config.lastHeartbeatMillis = started + 5_000L;
        config.cleanShutdown = false;

        RotClientCurrentSessionConfig recovered =
                RotClientCurrentSessionMath.recoverUncleanShutdown(config);

        assertEquals(RotClientCurrentSessionConfig.STATE_PAUSED,
                recovered.state);
        assertEquals(started + 5_000L, recovered.pausedAtMillis);
        assertEquals(started + 5_000L,
                recovered.targetSegments.getFirst().endedAtMillis());
        assertEquals(started + 5_000L,
                recovered.areaSegments.getFirst().endedAtMillis());
        assertFalse(recovered.targetSegments.getFirst().isOpen());
        assertFalse(recovered.areaSegments.getFirst().isOpen());
    }

    @Test
    void legacyCleanPauseCannotKeepAnOpenActivitySegment() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        long started = config.startedAtMillis;
        config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
        config.pausedAtMillis = started + 2_000L;
        config.currentTargetId = "GOLD";
        config.targetSegments = new ArrayList<>(List.of(
                new RotClientCurrentSessionConfig.TargetSegment(
                        "GOLD", started, 0L)));
        config.cleanShutdown = true;

        RotClientCurrentSessionConfig recovered =
                RotClientCurrentSessionMath.recoverUncleanShutdown(config);

        assertEquals(started + 2_000L,
                recovered.targetSegments.getFirst().endedAtMillis());
        assertFalse(recovered.targetSegments.getFirst().isOpen());
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord item(
            String id,
            long qty,
            SessionSourceType source,
            MiningClassification miningClass,
            double value) {
        return new RotClientCurrentSessionConfig.SessionItemRecord(
                id,
                id,
                qty,
                source.name(),
                miningClass == null ? null : miningClass.name(),
                SkyBlockArea.DWARVEN_MINES.id(),
                true,
                "RESOLVED_BAZAAR",
                value);
    }

    private static RotClientCurrentSessionConfig configWith(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.items = new ArrayList<>(items);
        return config;
    }
}
