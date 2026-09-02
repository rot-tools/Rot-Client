package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MiningSessionHistoryPresentationTest {
    @TempDir
    Path tempDir;

    @Test
    void emptyOneAndTwentyRecordPresentation() {
        Fixture fixture = fixture();
        assertTrue(fixture.history.presentation().empty());

        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 2L, 11L, "gold-1");
        fixture.analytics.stop();
        fixture.history.saveCurrentStoppedSession();
        assertEquals(1, fixture.history.presentation().sessions().size());
        assertFalse(fixture.history.presentation().saveEnabled());

        for (int i = 0; i < 19; i++) {
            fixture.analytics.reset();
            fixture.clock.set(100L + i * 10L);
            fixture.analytics.start(true, TrackerSelection.GOLD);
            appendGold(fixture, i + 3L, fixture.clock.nowMillis() + 1L, "g-" + i);
            fixture.clock.set(fixture.clock.nowMillis() + 5L);
            fixture.analytics.stop();
            fixture.history.saveCurrentStoppedSession();
        }
        MiningSessionHistoryPresentation full =
                fixture.history.presentation();
        assertEquals(20, full.sessions().size());
        assertTrue(full.clearEnabled());
        assertFalse(full.detailOpen());
    }

    @Test
    void selectedRecordIsReadOnlyHistoricalWhileLiveAnalyticsRetained() {
        Fixture fixture = fixture();
        fixture.analytics.start(true, TrackerSelection.GOLD);
        appendGold(fixture, 4L, 11L, "gold-1");
        fixture.analytics.stop();
        BigDecimal stoppedValue =
                fixture.analytics.viewModel().resolvedItemValue();
        fixture.history.saveCurrentStoppedSession();
        fixture.history.open("1");

        MiningSessionHistoryPresentation detail =
                fixture.history.presentation();
        assertTrue(detail.detailOpen());
        assertEquals(
                0,
                stoppedValue.compareTo(
                        detail.selectedViewModel().resolvedItemValue()));

        fixture.analytics.start(true, TrackerSelection.DIAMOND);
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.ACTIVE,
                fixture.analytics.viewModel().sessionState());
        assertEquals(
                0,
                stoppedValue.compareTo(
                        fixture.history.presentation()
                                .selectedViewModel()
                                .resolvedItemValue()));
        assertEquals(
                "Pure Gold",
                fixture.history.presentation()
                        .selectedViewModel()
                        .selectedTargetDisplayName());
    }

    private Fixture fixture() {
        CaptureSink sink = new CaptureSink();
        MiningSessionEngine engine = new MiningSessionEngine(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                0L,
                now -> MiningSessionPriceBook.available(
                        10L,
                        Map.of(
                                TrackedMaterial.GOLD.rawBazaarId(),
                                new BigDecimal("2.00"),
                                TrackedMaterial.DIAMOND.rawBazaarId(),
                                new BigDecimal("3.00"))));
        List<String> clipboard = new ArrayList<>();
        MutableClock clock = new MutableClock(10L);
        MiningSessionAnalyticsController analytics =
                new MiningSessionAnalyticsController(
                        engine,
                        clipboard::add,
                        (selection, at) -> baseline(selection, at),
                        clock);
        MiningSessionHistoryController history =
                new MiningSessionHistoryController(
                        analytics,
                        engine,
                        clipboard::add,
                        clock,
                        tempDir.resolve("history.json"));
        return new Fixture(engine, analytics, history, clock);
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

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long at) {
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
            MiningSessionAnalyticsController analytics,
            MiningSessionHistoryController history,
            MutableClock clock) {
    }

    private static final class MutableClock
            implements MiningSessionAnalyticsController.Clock,
            MiningSessionHistoryController.Clock {
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
