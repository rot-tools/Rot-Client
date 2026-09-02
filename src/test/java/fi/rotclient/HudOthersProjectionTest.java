package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regression matrix for the HUD OTHERS projection: target-agnostic non-target
 * world/gameplay item units, preserving underlying source taxonomy.
 */
final class HudOthersProjectionTest {
    @Test
    void matrixA_pureGoldTarget_nonTargetMiningMobChestGoToOthers() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                target("GOLD_INGOT", 1_000L, 100.0),
                target("ENCHANTED_GOLD", 10L, 50.0),
                otherMined("HARD_STONE", 500L, 5.0),
                otherMined("DIAMOND", 20L, 40.0),
                otherMined("MITHRIL", 100L, 10.0),
                otherMined("TITANIUM", 5L, 25.0),
                mob("MOB_DROP_A", 2L, 8.0),
                chest("CHEST_REWARD_B", 3L, 12.0));

        MiningHudOtherSummary summary = project(items);
        // 500+20+100+5+2+3 = 630; gold target excluded.
        assertEquals(630L, summary.totalQuantity());
        assertEquals(100.0, summary.resolvedGrossValue(), 0.0001);
        assertFalse(summary.totalQuantity() >= 1_000L);
        assertEquals(625L, summary.slice(SessionSourceType.MINING).quantity());
        assertEquals(2L, summary.slice(SessionSourceType.MOB).quantity());
        assertEquals(3L, summary.slice(SessionSourceType.CHEST).quantity());
    }

    @Test
    void matrixB_mithrilTarget_goldBecomesOthers() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                target("MITHRIL", 900L, 90.0),
                otherMined("GOLD_INGOT", 40L, 4.0),
                otherMined("HARD_STONE", 200L, 2.0),
                otherMined("DIAMOND", 10L, 20.0),
                mob("MOB_DROP", 1L, 1.0));

        MiningHudOtherSummary summary = project(items);
        assertEquals(251L, summary.totalQuantity());
        assertEquals(27.0, summary.resolvedGrossValue(), 0.0001);
        assertEquals(250L, summary.slice(SessionSourceType.MINING).quantity());
        assertEquals(1L, summary.slice(SessionSourceType.MOB).quantity());
    }

    @Test
    void matrixC_titaniumTarget_mithrilIsOthersUnlessTarget() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                target("TITANIUM", 50L, 250.0),
                otherMined("MITHRIL", 300L, 30.0),
                otherMined("GOLD_INGOT", 10L, 1.0),
                mob("MOB_DROP", 4L, 2.0));

        MiningHudOtherSummary summary = project(items);
        assertEquals(314L, summary.totalQuantity());
        assertEquals(33.0, summary.resolvedGrossValue(), 0.0001);
    }

    @Test
    void matrixD_targetChangeDoesNotReclassifyHistory() {
        // Segment 1 (GOLD target): Mithril +100 stored as OTHER.
        // Segment 2 (MITHRIL target): Mithril +200 stored as TARGET.
        // History must keep 100 in OTHERS and 200 out of OTHERS.
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                otherMined("MITHRIL", 100L, 10.0),
                target("MITHRIL", 200L, 20.0));

        assertEquals(100L, RotClientCurrentSessionMath.sumHudOthersQuantity(items));
        assertEquals(100L, RotClientCurrentSessionMath.sumOtherMinedQuantity(items));
        MiningHudOtherSummary summary = project(items);
        assertEquals(100L, summary.totalQuantity());
        assertEquals(10.0, summary.resolvedGrossValue(), 0.0001);
    }

    @Test
    void matrixE_targetAutoPauseDoesNotBlockOthersProjection() {
        // Projection is a read-model over session rows — auto-pause of the
        // live target tracker is orthogonal and cannot filter these out.
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                otherMined("HARD_STONE", 50L, 1.0),
                mob("MOB_DROP", 3L, 6.0));
        MiningHudOtherSummary summary = project(items);
        assertEquals(53L, summary.totalQuantity());
        assertEquals(7.0, summary.resolvedGrossValue(), 0.0001);
    }

    @Test
    void matrixF_unknownStableMinedItemWithMiningSourceIsOthers() {
        RotClientCurrentSessionConfig.SessionItemRecord unknown = new
                RotClientCurrentSessionConfig.SessionItemRecord(
                "UNKNOWN_STABLE_ORE",
                "Unknown Stable Ore",
                40L,
                SessionSourceType.MINING.name(),
                MiningClassification.OTHER.name(),
                SkyBlockArea.DWARVEN_MINES.id(),
                false,
                "UNSUPPORTED",
                0.0);
        MiningHudOtherSummary summary = project(List.of(unknown));
        assertEquals(40L, summary.totalQuantity());
        assertEquals(0.0, summary.resolvedGrossValue(), 0.0001);
        assertTrue(summary.hasUnresolved());
        assertEquals(40L, summary.unresolvedQuantity());
        assertEquals("0+", summary.valueDisplayCompact("0"));
    }

    @Test
    void matrixG_unattributedInventoryGainIsNotAutomaticallyOthers() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                item("BAZAARISH_NOISE", 99L, SessionSourceType.UNATTRIBUTED,
                        null, "UNAVAILABLE", 0.0));
        MiningHudOtherSummary summary = project(items);
        assertEquals(0L, summary.totalQuantity());
        assertTrue(summary.isEmpty());
        assertFalse(RotClientCurrentSessionMath.isEligibleHudOthers(items.get(0)));
    }

    @Test
    void matrixH_bazaarPurchaseStyleUnattributedExcluded() {
        // Purchases are never confidently world-loot; they stay UNATTRIBUTED
        // (or never credited). Either way they must not enter HUD OTHERS.
        assertEquals(
                0L,
                RotClientCurrentSessionMath.sumHudOthersQuantity(List.of(
                        item("ENCHANTED_GOLD", 64L,
                                SessionSourceType.UNATTRIBUTED,
                                null, "UNAVAILABLE", 0.0))));
    }

    @Test
    void matrixI_storageWithdrawalStyleUnattributedExcluded() {
        assertEquals(
                0L,
                RotClientCurrentSessionMath.sumHudOthersQuantity(List.of(
                        item("HARD_STONE", 10_000L,
                                SessionSourceType.UNATTRIBUTED,
                                null, "UNAVAILABLE", 0.0))));
    }

    @Test
    void matrixJ_craftingStyleUnattributedExcluded() {
        assertEquals(
                0L,
                RotClientCurrentSessionMath.sumHudOthersQuantity(List.of(
                        item("ENCHANTED_MITHRIL", 1L,
                                SessionSourceType.UNATTRIBUTED,
                                null, "UNAVAILABLE", 0.0))));
    }

    @Test
    void matrixK_sameMobDropMergedOnceInSessionLedger() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.creditItem(
                "MOB_DROP_A",
                "Mob Drop A",
                2L,
                SessionSourceType.MOB,
                null,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                8.0,
                100L);
        // Second observation channel for the same item+source merges quantity.
        session.creditItem(
                "MOB_DROP_A",
                "Mob Drop A",
                2L,
                SessionSourceType.MOB,
                null,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                8.0,
                101L);

        RotClientCurrentSessionConfig snap = session.snapshotConfig();
        long mobRows = snap.items.stream()
                .filter(i -> i.source() == SessionSourceType.MOB)
                .count();
        assertEquals(1L, mobRows);
        assertEquals(4L, session.otherHudSummary().totalQuantity());
    }

    @Test
    void matrixL_miningGainMergesOncePerItemAndClassification() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.creditItem(
                "HARD_STONE",
                "Hard Stone",
                100L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                1.0,
                100L);
        session.creditItem(
                "HARD_STONE",
                "Hard Stone",
                50L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                0.5,
                110L);

        long otherMinedRows = session.snapshotConfig().items.stream()
                .filter(RotClientCurrentSessionMath::isOtherMined)
                .count();
        assertEquals(1L, otherMinedRows);
        assertEquals(150L, session.otherHudSummary().totalQuantity());
    }

    @Test
    void currencyNeverCountsTowardHudOthersQuantity() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                otherMined("HARD_STONE", 10L, 1.0),
                item("GEMSTONE_POWDER", 291L, SessionSourceType.CURRENCY,
                        null, "UNSUPPORTED", 0.0),
                item("COINS", 1_000_000L, SessionSourceType.CURRENCY,
                        null, "UNSUPPORTED", 0.0));
        assertEquals(10L, RotClientCurrentSessionMath.sumHudOthersQuantity(items));
    }

    @Test
    void nativeMobCoinsNeverCountTowardHudOthersQuantity() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                otherMined("HARD_STONE", 10L, 1.0),
                item("COINS", 1_000_000L, SessionSourceType.MOB, null,
                        "RESOLVED_BAZAAR", 1_000_000.0),
                mob("ROTTEN_FLESH", 4L, 8.0));
        assertEquals(14L, RotClientCurrentSessionMath.sumHudOthersQuantity(items));
    }

    @Test
    void targetMiningNeverDoubleCountsIntoOthers() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                target("GOLD_INGOT", 40_500L, 4_050.0),
                otherMined("MITHRIL", 840L, 84.0));
        MiningHudOtherSummary summary = project(items);
        assertEquals(840L, summary.totalQuantity());
        assertEquals(84.0, summary.resolvedGrossValue(), 0.0001);
    }

    @Test
    void hudRowLabelIsOthersNotOtherMined() {
        assertEquals("Others", MiningHudOtherSummary.hudRowLabel());
    }

    @Test
    void analyticsOtherMinedBreakdownPreservedSeparately() {
        List<RotClientCurrentSessionConfig.SessionItemRecord> items = List.of(
                otherMined("HARD_STONE", 500L, 5.0),
                mob("MOB_DROP", 2L, 8.0),
                chest("CHEST_ITEM", 3L, 12.0));
        assertEquals(
                500L,
                RotClientCurrentSessionMath.sumOtherMinedQuantity(items));
        assertEquals(
                505L,
                RotClientCurrentSessionMath.sumHudOthersQuantity(items));
        MiningHudOtherSummary summary = project(items);
        assertEquals(5.0, summary.slice(SessionSourceType.MINING).resolvedGrossValue(),
                0.0001);
        assertEquals(8.0, summary.slice(SessionSourceType.MOB).resolvedGrossValue(),
                0.0001);
        assertEquals(12.0, summary.slice(SessionSourceType.CHEST).resolvedGrossValue(),
                0.0001);
    }

    private static MiningHudOtherSummary project(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.items = new ArrayList<>(items);
        return RotClientCurrentSessionMath.toHudOtherSummary(config);
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord target(
            String id, long qty, double value) {
        return item(id, qty, SessionSourceType.MINING,
                MiningClassification.TARGET, "RESOLVED_BAZAAR", value);
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord otherMined(
            String id, long qty, double value) {
        return item(id, qty, SessionSourceType.MINING,
                MiningClassification.OTHER, "RESOLVED_BAZAAR", value);
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord mob(
            String id, long qty, double value) {
        return item(id, qty, SessionSourceType.MOB, null, "RESOLVED_BAZAAR", value);
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord chest(
            String id, long qty, double value) {
        return item(id, qty, SessionSourceType.CHEST, null, "RESOLVED_BAZAAR", value);
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord item(
            String id,
            long qty,
            SessionSourceType source,
            MiningClassification miningClass,
            String priceStatus,
            double value) {
        return new RotClientCurrentSessionConfig.SessionItemRecord(
                id,
                id,
                qty,
                source.name(),
                miningClass == null ? null : miningClass.name(),
                SkyBlockArea.DWARVEN_MINES.id(),
                true,
                priceStatus,
                value);
    }
}
