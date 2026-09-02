package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Sanitized deterministic runtime-derived Session Analytics regression fixture.
 */
final class MiningSessionAnalyticsRegressionTest {
    @Test
    void mixedSupportedUnsupportedCurrencyAndSackResearchRemainCorrect() {
        Fixture fixture = fixture(book(
                10_000L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.50"),
                        "ROUGH_TOPAZ_GEM",
                        new BigDecimal("2.00"),
                        "FLAWED_RUBY_GEM",
                        new BigDecimal("5.00"))));

        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                baseline(10_000L),
                10_000L);
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                4L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                4L,
                10_050L,
                "gold-supported-1",
                "test");
        playChest(fixture, 10_100L);

        // Sack research-only observation must not duplicate chest rewards.
        fixture.engine.observeSackChanges(
                List.of(new SackChangeParser.Change(
                        24L,
                        "Rough Topaz Gemstone",
                        List.of("Gemstone Sack"))),
                10_200L);

        MiningSessionSnapshot snapshot =
                fixture.engine.snapshot(10_300L).orElseThrow();
        MiningSessionAnalyticsViewModel model =
                new MiningSessionAnalyticsProjector().project(snapshot);
        String summary = new MiningSessionSummaryFormatter().format(model);

        assertEquals(1, model.targetEntryCount());
        assertEquals(4L, model.targetQuantities().get("GOLD").quantity());
        assertEquals(24L, model.chestLootQuantities()
                .get("ROUGH_TOPAZ_GEM").quantity());
        assertEquals(2L, model.chestLootQuantities()
                .get("FLAWED_RUBY_GEM").quantity());
        assertEquals(291L, model.currencyQuantities()
                .get("GEMSTONE_POWDER").quantity());
        assertTrue(model.unsupportedEntryCount() >= 1);
        assertEquals(1, model.excludedCurrencyEntryCount());
        assertTrue(model.resolvedValueAvailable());
        assertEquals(0, new BigDecimal("68.00").compareTo(
                model.resolvedItemValue()));
        assertEquals(0L, model.mismatchCount());
        assertTrue(snapshot.entries().stream().allMatch(entry ->
                entry.priceResolution().status()
                        == MiningSessionPriceResolution.PriceStatus.UNRESOLVED));

        String again = new MiningSessionSummaryFormatter().format(model);
        assertEquals(summary, again);
        assertFalse(summary.toLowerCase(Locale.ROOT).contains("profit"));
        assertFalse(summary.contains("uuid"));
        assertFalse(summary.contains("sessionId"));
        assertTrue(summary.contains("GOLD: 4"));
        assertTrue(summary.contains("ROUGH_TOPAZ_GEM: 24"));
        assertTrue(summary.contains("FLAWED_RUBY_GEM: 2"));
        assertTrue(summary.contains("GEMSTONE_POWDER: 291"));
    }

    @Test
    void clearAnalyticsSessionReturnsNotStartedWithoutLiveMutation() {
        Fixture fixture = fixture(book(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"))));
        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                baseline(10L),
                10L);
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                2L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                2L,
                11L,
                "gold-1",
                "test");
        fixture.engine.onDiagnosticStop(20L);
        fixture.engine.clearAnalyticsSession(30L);

        assertTrue(fixture.engine.snapshot(40L).isEmpty());
        assertFalse(fixture.engine.isDiagnosticsActive());
        assertFalse(fixture.engine.hasRetainedFinalSnapshot());
        MiningSessionAnalyticsViewModel model =
                new MiningSessionAnalyticsProjector()
                        .project(fixture.engine.snapshot(40L));
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.NOT_STARTED,
                model.sessionState());
    }

    private static void playChest(Fixture fixture, long timestamp) {
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                timestamp++);
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
    }

    private static Fixture fixture(MiningSessionPriceBook book) {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        return new Fixture(
                new MiningSessionEngine(
                        catalog,
                        new MiningSessionLedger(),
                        sink,
                        0L,
                        0L,
                        0L,
                        now -> book));
    }

    private static MiningSessionPriceBook book(
            long observedAtMillis,
            Map<String, BigDecimal> prices) {
        return MiningSessionPriceBook.available(observedAtMillis, prices);
    }

    private static MiningSessionParity.LiveBaseline baseline(long timestamp) {
        Map<TrackedMaterial, Long> quantities =
                new EnumMap<>(TrackedMaterial.class);
        Map<TrackedMaterial, Long> blocks =
                new EnumMap<>(TrackedMaterial.class);
        quantities.put(TrackedMaterial.GOLD, 0L);
        blocks.put(TrackedMaterial.GOLD, 0L);
        return MiningSessionParity.LiveBaseline.material(
                TrackerSelection.GOLD,
                quantities,
                blocks,
                timestamp);
    }

    private record Fixture(MiningSessionEngine engine) {
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
