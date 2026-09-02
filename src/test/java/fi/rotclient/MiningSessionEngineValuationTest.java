package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MiningSessionEngineValuationTest {
    @Test
    void snapshotValuationDoesNotMutateLedgerEntries() {
        Fixture fixture = fixture(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("10"))));
        start(fixture, 10L);
        appendGold(fixture, 3L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(1, snapshot.valuation().resolvedEntryCount());
        assertTrue(snapshot.entries().stream().allMatch(entry ->
                entry.priceResolution().status()
                        == MiningSessionPriceResolution.PriceStatus.UNRESOLVED));
        assertEquals(3L, snapshot.quantity(
                MiningSessionCategory.TARGET_MINED,
                goldResource()));
    }

    @Test
    void quantityTotalsUnchangedWithoutPrices() {
        Fixture fixture = fixture(now -> MiningSessionPriceBook.unavailable());
        start(fixture, 10L);
        appendGold(fixture, 5L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(0, snapshot.valuation().resolvedEntryCount());
        assertEquals(5L, snapshot.totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void valuationFailureDoesNotPreventSnapshot() {
        Fixture fixture = fixture(now -> {
            throw new IllegalStateException("boom");
        });
        start(fixture, 10L);
        appendGold(fixture, 2L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(1, snapshot.entryCount());
        assertEquals(0, snapshot.valuation().resolvedEntryCount());
        assertTrue(fixture.sink.contains("SHADOW_VALUATION_FAILED"));
    }

    @Test
    void finalStopSnapshotFreezesValuation() {
        Fixture fixture = fixture(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2"))));
        start(fixture, 10L);
        appendGold(fixture, 4L, 4L, 11L, "gold-1");

        fixture.engine.onDiagnosticStop(20L);
        MiningSessionSnapshot retained = snapshot(fixture, 99L);
        assertEquals(0, new BigDecimal("8").compareTo(
                retained.valuation().resolvedItemValue()));
    }

    @Test
    void parityOutputRemainsUnchangedByValuation() {
        Fixture fixture = fixture(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2"))));
        start(fixture, 10L);
        appendGold(fixture, 4L, 4L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 12L);
        assertEquals(
                MiningSessionParity.Status.MATCH,
                snapshot.targetParityStatus());
        assertEquals(0L, snapshot.parityMismatchCount());
    }

    @Test
    void invalidTimestampProducesUnavailableValuationAndDiagnostic() {
        Fixture fixture = fixture(MiningSessionBazaarPriceProvider.INSTANCE);
        MiningSessionBazaarPriceCache.publish(
                goldMarketPrices(10.0, 100.0),
                10L);
        start(fixture, 10L);
        appendGold(fixture, 3L, 11L, "gold-1");

        MiningSessionSnapshot snapshot = snapshot(fixture, 9L);

        assertEquals(3L, snapshot.totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
        assertEquals(0, snapshot.valuation().resolvedEntryCount());
        assertFalse(snapshot.valuation().hasResolvedValue());
        assertTrue(fixture.sink.contains(
                "SHADOW_PRICE_BOOK_UNAVAILABLE",
                "INVALID_TIMESTAMP"));
    }

    @Test
    void retainedSnapshotIgnoresLaterCachePublication() {
        Fixture fixture = fixture(MiningSessionBazaarPriceProvider.INSTANCE);
        MiningSessionBazaarPriceCache.publish(
                goldMarketPrices(2.0, 20.0),
                10L);
        start(fixture, 10L);
        appendGold(fixture, 4L, 4L, 11L, "gold-1");

        fixture.engine.onDiagnosticStop(20L);
        MiningSessionBazaarPriceCache.publish(
                goldMarketPrices(99.0, 990.0),
                90L);

        MiningSessionSnapshot retained = snapshot(fixture, 100L);
        assertEquals(0, new BigDecimal("8").compareTo(
                retained.valuation().resolvedItemValue()));
        assertEquals(10L, retained.valuation()
                .priceBookObservedAtMillis()
                .orElse(-1L));
    }

    @Test
    void unchangedValuationDiagnosticsAreRateLimitedBetweenSnapshots() {
        Fixture fixture = fixture(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2"))));
        start(fixture, 10L);
        appendGold(fixture, 4L, 11L, "gold-1");

        snapshot(fixture, 12L);
        snapshot(fixture, 13L);
        snapshot(fixture, 4_999L);
        assertEquals(1, fixture.sink.count("SHADOW_PRICE_BOOK_CAPTURED"));
        assertEquals(1, fixture.sink.count("SHADOW_VALUATION_COMPUTED"));

        snapshot(fixture, 5_012L);
        assertEquals(2, fixture.sink.count("SHADOW_PRICE_BOOK_CAPTURED"));
        assertEquals(2, fixture.sink.count("SHADOW_VALUATION_COMPUTED"));
    }

    private static BazaarPriceService.MarketPrices goldMarketPrices(
            double rawPrice,
            double enchantedPrice) {
        return new BazaarPriceService.MarketPrices(Map.of(
                TrackedMaterial.GOLD,
                new BazaarPriceService.MaterialPrices(
                        new BazaarPriceService.ProductPrice(
                                java.util.List.of(
                                        new BazaarPriceService.OrderLevel(
                                                100,
                                                rawPrice)),
                                rawPrice),
                        new BazaarPriceService.ProductPrice(
                                java.util.List.of(
                                        new BazaarPriceService.OrderLevel(
                                                100,
                                                enchantedPrice)),
                                enchantedPrice))));
    }

    private static Fixture fixture(MiningSessionPriceProvider provider) {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        return new Fixture(
                catalog,
                sink,
                new MiningSessionEngine(
                        catalog,
                        new MiningSessionLedger(),
                        sink,
                        0L,
                        0L,
                        0L,
                        provider));
    }

    private static MiningSessionPriceProvider fixedBook(
            long observedAtMillis,
            Map<String, BigDecimal> prices) {
        MiningSessionPriceBook book =
                MiningSessionPriceBook.available(observedAtMillis, prices);
        return now -> book;
    }

    private static void start(Fixture fixture, long timestamp) {
        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                goldBaseline(timestamp),
                timestamp);
    }

    private static void appendGold(
            Fixture fixture,
            long quantity,
            long timestamp,
            String eventId) {
        appendGold(fixture, quantity, quantity, timestamp, eventId);
    }

    private static void appendGold(
            Fixture fixture,
            long quantity,
            long liveCumulative,
            long timestamp,
            String eventId) {
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                quantity,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                liveCumulative,
                timestamp,
                eventId,
                "inventory");
    }

    private static MiningSessionSnapshot snapshot(
            Fixture fixture,
            long timestamp) {
        return fixture.engine.snapshot(timestamp).orElseThrow();
    }

    private static MiningSessionResource goldResource() {
        return MiningSessionResource.material(
                TrackedMaterial.GOLD.rawBazaarId(),
                TrackedMaterial.GOLD.rawItemName(),
                TrackedMaterial.GOLD);
    }

    private static MiningSessionParity.LiveBaseline goldBaseline(
            long timestamp) {
        return MiningSessionParity.LiveBaseline.material(
                TrackerSelection.GOLD,
                Map.of(TrackedMaterial.GOLD, 0L),
                Map.of(TrackedMaterial.GOLD, 0L),
                timestamp);
    }

    private record Fixture(
            MiningResourceCatalog catalog,
            CaptureSink sink,
            MiningSessionEngine engine) {
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        private final List<String> events = new java.util.ArrayList<>();

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void record(String marker, String details) {
            events.add(marker + ":" + details);
        }

        private boolean contains(String marker) {
            return events.stream().anyMatch(value ->
                    value.startsWith(marker + ":"));
        }

        private boolean contains(String marker, String text) {
            return events.stream().anyMatch(value ->
                    value.startsWith(marker + ":")
                            && value.contains(text));
        }

        private long count(String marker) {
            return events.stream().filter(value ->
                    value.startsWith(marker + ":")).count();
        }
    }
}
