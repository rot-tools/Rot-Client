package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Regression for Session Analytics starting without DiagnosticRecorder while
 * tracker/selection transitions occur before the first TARGET_MINED credit.
 */
final class MiningSessionAnalyticsLifecycleTest {
    @Test
    void startDisabledThenSelectGoldThenEnableCreditsTargetMined() {
        Fixture fixture = fixtureInactiveSink();
        long liveSessionTotal = 1_300L;

        // 1. Analytics starts with tracker OFF and no usable target activity.
        fixture.controller.start(false, TrackerSelection.DIAMOND);
        assertTrue(fixture.engine.isDiagnosticsActive());
        assertFalse(fixture.engine.isObservationEnabled());

        // 2. Target changes to Pure Gold while analytics remains active.
        fixture.clock.set(20L);
        fixture.engine.onSelectionChanged(
                TrackerSelection.GOLD,
                false,
                materialBaseline(TrackerSelection.GOLD, liveSessionTotal, 0L, 20L),
                20L);
        assertFalse(fixture.engine.isObservationEnabled());

        // 3. Tracker becomes enabled with a fresh live baseline (no backfill).
        fixture.clock.set(30L);
        fixture.engine.onTrackerEnabled(
                TrackerSelection.GOLD,
                materialBaseline(TrackerSelection.GOLD, liveSessionTotal, 0L, 30L),
                30L);
        assertTrue(fixture.engine.isObservationEnabled());
        assertEquals(0, snapshot(fixture, 31L).targetMinedEntryCount());

        // 4-6. A fresh Pure Gold gain after enable is mirrored.
        liveSessionTotal = 1_400L;
        MiningSessionEngine.EventDisposition disposition =
                fixture.engine.onAcceptedTargetMaterialQuantity(
                        TrackedMaterial.GOLD,
                        100L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        liveSessionTotal,
                        40L,
                        "gold-after-enable",
                        "inventory");
        assertSame(
                MiningSessionEngine.EventDisposition.APPENDED,
                disposition);

        MiningSessionSnapshot active = snapshot(fixture, 41L);
        assertEquals(1, active.targetMinedEntryCount());
        assertEquals(100L, active.quantity(
                MiningSessionCategory.TARGET_MINED,
                goldResource()));
        assertEquals(
                MiningSessionParity.Status.MATCH,
                active.targetParityStatus());
        assertEquals(0L, active.parityMismatchCount());

        MiningSessionAnalyticsViewModel model =
                fixture.controller.viewModel();
        assertEquals(1, model.targetEntryCount());
        assertEquals(100L, model.targetQuantities()
                .get("GOLD").quantity());
        String summary = fixture.controller.statusText();
        assertTrue(summary.contains("TARGET_MINED: 1"));
        assertTrue(summary.contains("GOLD: 100"));
        assertTrue(summary.contains("Target: Pure Gold")
                || summary.contains("Target: GOLD"));

        // Live persistent total was only advanced by the test harness input.
        assertEquals(1_400L, liveSessionTotal);
    }

    @Test
    void disabledIntervalGainsAreNotCreditedAndNoHistoricalBackfill() {
        Fixture fixture = fixtureInactiveSink();
        fixture.controller.start(false, TrackerSelection.GOLD);
        fixture.engine.onSelectionChanged(
                TrackerSelection.GOLD,
                false,
                materialBaseline(TrackerSelection.GOLD, 500L, 0L, 20L),
                20L);

        MiningSessionEngine.EventDisposition whileDisabled =
                fixture.engine.onAcceptedTargetMaterialQuantity(
                        TrackedMaterial.GOLD,
                        50L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        550L,
                        25L,
                        "gold-while-disabled",
                        "inventory");
        assertSame(
                MiningSessionEngine.EventDisposition.INACTIVE,
                whileDisabled);
        assertEquals(0, snapshot(fixture, 26L).entryCount());

        fixture.engine.onTrackerEnabled(
                TrackerSelection.GOLD,
                materialBaseline(TrackerSelection.GOLD, 550L, 0L, 30L),
                30L);
        assertEquals(0, snapshot(fixture, 31L).targetMinedEntryCount());

        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                10L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                560L,
                40L,
                "gold-after-enable",
                "inventory");
        assertEquals(10L, snapshot(fixture, 41L).quantity(
                MiningSessionCategory.TARGET_MINED,
                goldResource()));
    }

    @Test
    void secondTargetTransitionDoesNotMixIdentitiesAndPreservesChest() {
        Fixture fixture = fixtureInactiveSink(book(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"),
                        TrackedMaterial.DIAMOND.rawBazaarId(),
                        new BigDecimal("3.00"),
                        "ROUGH_TOPAZ_GEM",
                        new BigDecimal("2.00"))));
        fixture.controller.start(true, TrackerSelection.GOLD);
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                4L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                4L,
                11L,
                "gold-1",
                "inventory");
        playChest(fixture, 20L);

        fixture.engine.onSelectionChanged(
                TrackerSelection.DIAMOND,
                true,
                materialBaseline(TrackerSelection.DIAMOND, 0L, 0L, 40L),
                40L);
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.DIAMOND,
                7L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                7L,
                50L,
                "diamond-1",
                "inventory");

        MiningSessionSnapshot snapshot = snapshot(fixture, 60L);
        assertEquals(4L, snapshot.quantity(
                MiningSessionCategory.TARGET_MINED,
                goldResource()));
        assertEquals(7L, snapshot.quantity(
                MiningSessionCategory.TARGET_MINED,
                diamondResource()));
        assertEquals(24L, snapshot.quantity(
                MiningSessionCategory.CHEST_LOOT,
                roughTopaz()));
        assertEquals(291L, snapshot.quantity(
                MiningSessionCategory.CURRENCY,
                gemstonePowder()));
        assertEquals(TrackerSelection.DIAMOND, snapshot.selectedTracker());
    }

    @Test
    void otherMinedWorksAfterTrackerEnableWithInactiveDiagnosticSink() {
        Fixture fixture = fixtureInactiveSink();
        fixture.controller.start(false, TrackerSelection.GOLD);
        fixture.engine.onTrackerEnabled(
                TrackerSelection.GOLD,
                materialBaseline(TrackerSelection.GOLD, 0L, 0L, 20L),
                20L);

        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE,
                1,
                30L);
        fixture.engine.observeSackChanges(
                List.of(new SackChangeParser.Change(
                        12L,
                        "Enchanted Hard Stone",
                        List.of("Enchanted Mining Sack"))),
                35L);

        assertEquals(12L, snapshot(fixture, 40L).quantity(
                MiningSessionCategory.OTHER_MINED,
                enchantedHardStone()));
    }

    @Test
    void stopRetainsCorrectedTargetQuantitiesAndResetClearsAnalyticsOnly() {
        Fixture fixture = fixtureInactiveSink(book(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"))));
        MaterialTrackerState live = new MaterialTrackerState();
        live.sessionActualRawEquivalent = 42L;
        live.sessionBlocks = 9L;

        fixture.controller.start(false, TrackerSelection.GOLD);
        fixture.engine.onTrackerEnabled(
                TrackerSelection.GOLD,
                materialBaseline(TrackerSelection.GOLD, 42L, 9L, 20L),
                20L);
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                5L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                47L,
                30L,
                "gold-1",
                "inventory");
        assertSame(
                MiningSessionAnalyticsController.StopResult.STOPPED,
                fixture.controller.stop());

        MiningSessionAnalyticsViewModel stopped =
                fixture.controller.viewModel();
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                stopped.sessionState());
        assertEquals(5L, stopped.targetQuantities()
                .get("GOLD").quantity());

        BigDecimal frozen = stopped.resolvedItemValue();
        fixture.prices.set(book(
                90L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("99.00"))));
        fixture.clock.set(100L);
        assertEquals(0, frozen.compareTo(
                fixture.controller.viewModel().resolvedItemValue()));

        assertSame(
                MiningSessionAnalyticsController.ResetResult.CLEARED,
                fixture.controller.reset());
        assertTrue(fixture.engine.snapshot(110L).isEmpty());
        assertEquals(42L, live.sessionActualRawEquivalent);
        assertEquals(9L, live.sessionBlocks);
    }

    @Test
    void duplicateStartRemainsSafeWithInactiveSink() {
        Fixture fixture = fixtureInactiveSink();
        assertSame(
                MiningSessionAnalyticsController.StartResult.STARTED,
                fixture.controller.start(false, TrackerSelection.GOLD));
        assertSame(
                MiningSessionAnalyticsController.StartResult.ALREADY_ACTIVE,
                fixture.controller.start(true, TrackerSelection.GOLD));
        assertTrue(fixture.engine.isDiagnosticsActive());
        assertFalse(fixture.engine.isObservationEnabled());
    }

    private static Fixture fixtureInactiveSink() {
        return fixtureInactiveSink(book(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"))));
    }

    private static Fixture fixtureInactiveSink(MiningSessionPriceBook book) {
        InactiveSink sink = new InactiveSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MutablePriceProvider prices = new MutablePriceProvider(now -> book);
        MiningSessionEngine engine = new MiningSessionEngine(
                catalog,
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                0L,
                prices);
        MutableClock clock = new MutableClock(10L);
        List<String> clipboard = new ArrayList<>();
        MiningSessionAnalyticsController controller =
                new MiningSessionAnalyticsController(
                        engine,
                        clipboard::add,
                        (selection, at) -> materialBaseline(selection, 0L, 0L, at),
                        clock);
        return new Fixture(engine, controller, clock, prices);
    }

    private static MiningSessionSnapshot snapshot(Fixture fixture, long at) {
        fixture.clock.set(at);
        return fixture.engine.snapshot(at).orElseThrow();
    }

    private static void playChest(Fixture fixture, long timestamp) {
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Rough Topaz Gemstone x24",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Gemstone Powder x291",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                String.valueOf(PowderChestChatParser.SEPARATOR_CHAR)
                        .repeat(27),
                timestamp);
    }

    private static MiningSessionPriceBook book(
            long observedAt,
            Map<String, BigDecimal> prices) {
        return MiningSessionPriceBook.available(observedAt, prices);
    }

    private static MiningSessionParity.LiveBaseline materialBaseline(
            TrackerSelection selection,
            long normalized,
            long blocks,
            long at) {
        Map<TrackedMaterial, Long> quantities =
                new EnumMap<>(TrackedMaterial.class);
        Map<TrackedMaterial, Long> blockQuantities =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : selection.materialTarget().materials()) {
            quantities.put(material, normalized);
            blockQuantities.put(material, blocks);
        }
        return MiningSessionParity.LiveBaseline.material(
                selection,
                quantities,
                blockQuantities,
                at);
    }

    private static MiningSessionResource goldResource() {
        return MiningSessionResource.material(
                TrackedMaterial.GOLD.rawBazaarId(),
                TrackedMaterial.GOLD.rawItemName(),
                TrackedMaterial.GOLD);
    }

    private static MiningSessionResource diamondResource() {
        return MiningSessionResource.material(
                TrackedMaterial.DIAMOND.rawBazaarId(),
                TrackedMaterial.DIAMOND.rawItemName(),
                TrackedMaterial.DIAMOND);
    }

    private static MiningSessionResource enchantedHardStone() {
        return MiningSessionResource.material(
                TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                TrackedMaterial.HARD_STONE.enchantedItemName(),
                TrackedMaterial.HARD_STONE);
    }

    private static MiningSessionResource roughTopaz() {
        return MiningSessionResource.gemstone(
                "ROUGH_TOPAZ_GEM",
                "Rough Topaz Gemstone",
                GemstoneType.TOPAZ,
                GemstoneTier.ROUGH);
    }

    private static MiningSessionResource gemstonePowder() {
        return MiningSessionResource.currency(
                "GEMSTONE_POWDER",
                "Gemstone Powder");
    }

    private record Fixture(
            MiningSessionEngine engine,
            MiningSessionAnalyticsController controller,
            MutableClock clock,
            MutablePriceProvider prices) {
    }

    private static final class MutableClock
            implements MiningSessionAnalyticsController.Clock {
        private long now;

        private MutableClock(long now) {
            this.now = now;
        }

        void set(long now) {
            this.now = now;
        }

        @Override
        public long nowMillis() {
            return now;
        }
    }

    private static final class MutablePriceProvider
            implements MiningSessionPriceProvider {
        private MiningSessionPriceProvider delegate;

        private MutablePriceProvider(MiningSessionPriceProvider delegate) {
            this.delegate = delegate;
        }

        void set(MiningSessionPriceBook book) {
            this.delegate = now -> book;
        }

        @Override
        public MiningSessionPriceBook capture(long snapshotAtMillis) {
            return delegate.capture(snapshotAtMillis);
        }
    }

    /** Mimics Session Analytics without DiagnosticRecorder file recording. */
    private static final class InactiveSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public void record(String marker, String details) {
        }
    }
}
