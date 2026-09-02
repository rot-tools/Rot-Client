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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class MiningSessionAnalyticsControllerTest {
    @Test
    void startStopResetAndDuplicateLifecycle() {
        Fixture fixture = fixture();
        assertSame(
                MiningSessionAnalyticsController.StartResult.STARTED,
                fixture.controller.start(true, TrackerSelection.GOLD));
        assertTrue(fixture.engine.isDiagnosticsActive());
        assertSame(
                MiningSessionAnalyticsController.StartResult.ALREADY_ACTIVE,
                fixture.controller.start(true, TrackerSelection.GOLD));

        assertSame(
                MiningSessionAnalyticsController.StopResult.STOPPED,
                fixture.controller.stop());
        assertFalse(fixture.engine.isDiagnosticsActive());
        assertTrue(fixture.engine.hasRetainedFinalSnapshot());
        assertSame(
                MiningSessionAnalyticsController.StopResult.ALREADY_STOPPED,
                fixture.controller.stop());

        assertSame(
                MiningSessionAnalyticsController.ResetResult.CLEARED,
                fixture.controller.reset());
        assertFalse(fixture.engine.hasRetainedFinalSnapshot());
        assertFalse(fixture.engine.hasSessionIdentity());
        assertSame(
                MiningSessionAnalyticsController.ResetResult.NOTHING_TO_CLEAR,
                fixture.controller.reset());
    }

    @Test
    void statusAndCopyUseSharedFormatterWithoutNetwork() {
        Fixture fixture = fixture();
        fixture.controller.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 3L, 11L, "gold-1");

        String status = fixture.controller.statusText();
        assertTrue(status.contains("Rot Client Session Analytics"));
        assertTrue(status.contains("State: Active"));

        assertSame(
                MiningSessionAnalyticsController.CopyResult.COPIED,
                fixture.controller.copySummary());
        assertEquals(status, fixture.clipboard.get(0));
        assertEquals(0, fixture.networkCalls.get());
    }

    @Test
    void statusAndCopyCanProjectTheCanonicalCurrentSession() {
        Fixture fixture = fixture();
        fixture.controller.start(true, TrackerSelection.GOLD);
        RotClientCurrentSession current = new RotClientCurrentSession();
        current.creditItem(
                "HARD_STONE",
                "Hard Stone",
                64L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                11L);

        RotClientCurrentSessionConfig canonical = current.snapshotConfig();
        String status = fixture.controller.statusText(canonical);
        assertTrue(status.contains("HARD_STONE: 64"));
        assertFalse(fixture.controller.statusText().contains("HARD_STONE: 64"));

        assertSame(
                MiningSessionAnalyticsController.CopyResult.COPIED,
                fixture.controller.copySummary(canonical));
        assertEquals(status, fixture.clipboard.getFirst());
    }

    @Test
    void emptyCanonicalLifecycleRemainsViewableWhenEngineHasNoSnapshot() {
        Fixture fixture = fixture();
        RotClientCurrentSession current = new RotClientCurrentSession();

        RotClientCurrentSessionConfig active = current.snapshotConfig();
        assertTrue(fixture.controller.statusText(active)
                .contains("State: Active"));
        assertSame(
                MiningSessionAnalyticsController.CopyResult.COPIED,
                fixture.controller.copySummary(active));

        current.pause(active.startedAtMillis + 100L);
        RotClientCurrentSessionConfig paused = current.snapshotConfig();
        assertTrue(fixture.controller.statusText(paused)
                .contains("State: Stopped"));
        assertSame(
                MiningSessionAnalyticsController.CopyResult.COPIED,
                fixture.controller.copySummary(paused));
        assertEquals(2, fixture.clipboard.size());
    }

    @Test
    void sessionCommandsDoNotTouchLiveLedgerOrPersistenceMarkers() {
        Fixture fixture = fixture();
        MaterialTrackerState live = new MaterialTrackerState();
        live.sessionBlocks = 42;
        live.sessionActualRawEquivalent = 99;

        fixture.controller.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 2L, 11L, "gold-1");
        fixture.controller.stop();
        fixture.controller.reset();

        assertEquals(42, live.sessionBlocks);
        assertEquals(99, live.sessionActualRawEquivalent);
        assertEquals(0, fixture.networkCalls.get());
        assertTrue(fixture.engine.snapshot(100L).isEmpty());
    }

    @Test
    void buttonPresentationMatchesLifecycle() {
        assertFalse(MiningSessionAnalyticsPresentation.from(
                MiningSessionAnalyticsViewModel.notStarted()).resetEnabled());
        assertTrue(MiningSessionAnalyticsPresentation.from(
                MiningSessionAnalyticsViewModel.notStarted()).startEnabled());

        Fixture fixture = fixture();
        fixture.controller.start(true, TrackerSelection.GOLD);
        MiningSessionAnalyticsPresentation active =
                fixture.controller.presentation();
        assertFalse(active.startEnabled());
        assertTrue(active.stopEnabled());
        assertTrue(active.resetEnabled());
        assertTrue(active.copyEnabled());
        assertEquals(
                "Session active, no credited entries yet.",
                active.emptyStateMessage());

        fixture.controller.stop();
        MiningSessionAnalyticsPresentation stopped =
                fixture.controller.presentation();
        assertTrue(stopped.startEnabled());
        assertFalse(stopped.stopEnabled());
        assertEquals(
                "Final session contains no credited entries.",
                stopped.emptyStateMessage());
    }

    @Test
    void unsupportedAndCurrencyVisibleWithoutCurrencyValue() {
        Fixture fixture = fixture(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"),
                        "ROUGH_TOPAZ_GEM",
                        new BigDecimal("2.00"),
                        "FLAWED_RUBY_GEM",
                        new BigDecimal("5.00"))));
        fixture.controller.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 4L, 11L, "gold-1");
        playChest(fixture, 20L);

        MiningSessionAnalyticsViewModel model =
                fixture.controller.viewModel();
        assertEquals(1, model.targetEntryCount());
        assertTrue(model.chestLootEntryCount() >= 2);
        assertTrue(model.currencyEntryCount() >= 1);
        assertTrue(model.unsupportedEntryCount() >= 1);
        assertTrue(model.resolvedValueAvailable());
        assertFalse(model.currencyQuantities().isEmpty());
        String summary = fixture.controller.statusText();
        assertTrue(summary.contains("CURRENCY:"));
        assertFalse(summary.toLowerCase().contains("currency value"));
        assertTrue(summary.contains("Unsupported entries:"));
    }

    @Test
    void retainedStoppedSnapshotSurvivesLaterPriceBookChange() {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MutablePriceProvider prices = new MutablePriceProvider(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"))));
        MiningSessionEngine engine = new MiningSessionEngine(
                catalog,
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                0L,
                prices);
        List<String> clipboard = new ArrayList<>();
        MutableClock clock = new MutableClock(10L);
        MiningSessionAnalyticsController controller =
                new MiningSessionAnalyticsController(
                        engine,
                        clipboard::add,
                        (selection, at) -> baseline(selection, at),
                        clock);

        controller.start(true, TrackerSelection.GOLD);
        engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                4L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                4L,
                11L,
                "gold-1",
                "test");
        clock.set(20L);
        controller.stop();

        BigDecimal frozen = controller.viewModel().resolvedItemValue();
        prices.set(fixedBook(
                90L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("99.00"))));
        clock.set(100L);
        assertEquals(0, frozen.compareTo(controller.viewModel().resolvedItemValue()));
        assertEquals(0, new BigDecimal("8.00").compareTo(frozen));
    }

    private static Fixture fixture() {
        return fixture(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"))));
    }

    private static Fixture fixture(MiningSessionPriceProvider provider) {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MiningSessionEngine engine = new MiningSessionEngine(
                catalog,
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                0L,
                provider);
        List<String> clipboard = new ArrayList<>();
        AtomicInteger network = new AtomicInteger();
        MutableClock clock = new MutableClock(10L);
        MiningSessionAnalyticsController controller =
                new MiningSessionAnalyticsController(
                        engine,
                        clipboard::add,
                        (selection, at) -> baseline(selection, at),
                        () -> {
                            // Status/copy/viewModel must not count as network.
                            return clock.nowMillis();
                        });
        return new Fixture(
                engine,
                controller,
                clipboard,
                network,
                clock);
    }

    private static MiningSessionPriceProvider fixedBook(
            long observedAt,
            Map<String, BigDecimal> prices) {
        MiningSessionPriceBook book =
                MiningSessionPriceBook.available(observedAt, prices);
        return now -> book;
    }

    private static void appendGold(
            Fixture fixture,
            long quantity,
            long at,
            String eventId) {
        fixture.clock.set(at);
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                quantity,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                quantity,
                at,
                eventId,
                "test");
    }

    private static void playChest(Fixture fixture, long timestamp) {
        fixture.clock.set(timestamp);
        fixture.engine.observePowderChestChatLine("CHEST LOCKPICKED", timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Rough Topaz Gemstone x24",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Flawed Ruby Gemstone x2",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Gold Essence x2",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Gemstone Powder x291",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                String.valueOf(PowderChestChatParser.SEPARATOR_CHAR)
                        .repeat(27),
                timestamp);
        fixture.clock.set(timestamp);
    }

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long at) {
        if (selection.isGemstone()) {
            Map<GemstoneTier, Long> quantities =
                    new EnumMap<>(GemstoneTier.class);
            for (GemstoneTier tier : GemstoneTier.values()) {
                quantities.put(tier, 0L);
            }
            return MiningSessionParity.LiveBaseline.gemstone(
                    selection,
                    quantities,
                    0L,
                    0L,
                    at);
        }
        Map<TrackedMaterial, Long> quantities =
                new EnumMap<>(TrackedMaterial.class);
        Map<TrackedMaterial, Long> blocks =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : selection.materialTarget().materials()) {
            quantities.put(material, 0L);
            blocks.put(material, 0L);
        }
        return MiningSessionParity.LiveBaseline.material(
                selection,
                quantities,
                blocks,
                at);
    }

    private record Fixture(
            MiningSessionEngine engine,
            MiningSessionAnalyticsController controller,
            List<String> clipboard,
            AtomicInteger networkCalls,
            MutableClock clock) {
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

        void set(MiningSessionPriceProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public MiningSessionPriceBook capture(long snapshotAtMillis) {
            return delegate.capture(snapshotAtMillis);
        }
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void record(String marker, String details) {
        }
    }
}
