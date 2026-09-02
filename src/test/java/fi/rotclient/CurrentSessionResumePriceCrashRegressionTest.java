package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runtime incident regression: Resume → client-tick valuation sync must not
 * crash when the Bazaar cache contains over-scale but otherwise legitimate
 * instant-sell quotes (Hypixel {@code quick_status} weighted averages).
 */
final class CurrentSessionResumePriceCrashRegressionTest {
    @BeforeEach
    @AfterEach
    void resetCache() {
        MiningSessionBazaarPriceCache.publish(null, -1L);
    }

    @Test
    void overScaleBazaarQuoteDuringSyncDoesNotThrowAndPreservesQuantities() {
        AtomicLong clock = new AtomicLong(1_000L);
        MiningSessionEngine engine = new MiningSessionEngine();
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.ensureActiveForTracker(TrackerSelection.GOLD, clock.get());

        session.creditItem(
                "DIAMOND",
                "Diamond",
                40L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                clock.get());
        assertEquals(40L, otherQty(session, "DIAMOND"));

        session.pause(clock.addAndGet(100L));
        assertTrue(session.isPaused());

        // Market snapshot with the incident-shaped high-scale average plus a
        // peer product that must remain usable.
        BazaarPriceService.MarketPrices market =
                new BazaarPriceService.MarketPrices(
                        Map.of(
                                TrackedMaterial.DIAMOND,
                                new BazaarPriceService.MaterialPrices(
                                        product(4.99260315136572d),
                                        product(500.0d)),
                                TrackedMaterial.GOLD,
                                new BazaarPriceService.MaterialPrices(
                                        product(12.5d),
                                        product(400.0d))),
                        Map.of());
        MiningSessionBazaarPriceCache.publish(market, clock.get());

        clock.addAndGet(100L);
        session.resume(clock.get());
        assertTrue(session.isActive());

        engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                MiningSessionParity.LiveBaseline.material(
                        TrackerSelection.GOLD,
                        Map.of(),
                        Map.of(),
                        clock.get()),
                clock.get());
        assertTrue(engine.isCollectionActive());

        // Force valuation heartbeat past the refresh interval.
        clock.addAndGet(RotClientCurrentSession.valuationRefreshIntervalMillis());
        assertDoesNotThrow(() -> {
            MiningSessionPriceBook book = engine.capturePriceBook(clock.get());
            assertTrue(book.isAvailable());
            assertTrue(book.unitPrice(TrackedMaterial.DIAMOND.rawBazaarId())
                    .isPresent());
            assertTrue(book.unitPrice(TrackedMaterial.GOLD.rawBazaarId())
                    .isPresent());
            session.refreshFromEngineMetadata(
                    engine.snapshot(clock.get()).orElse(null),
                    TrackerSelection.GOLD,
                    book,
                    clock.get());
        });

        assertTrue(session.isActive());
        assertTrue(engine.isCollectionActive());
        assertEquals(40L, otherQty(session, "DIAMOND"));
    }

    @Test
    void providerSkipsInvalidProductWithoutDiscardingPeers() {
        BazaarPriceService.MarketPrices market =
                new BazaarPriceService.MarketPrices(
                        Map.of(
                                TrackedMaterial.GOLD,
                                new BazaarPriceService.MaterialPrices(
                                        product(12.5d),
                                        product(Double.NaN)),
                                TrackedMaterial.DIAMOND,
                                new BazaarPriceService.MaterialPrices(
                                        product(4.99260315136572d),
                                        product(-5.0d))),
                        Map.of(
                                "ROUGH_TOPAZ_GEM",
                                product(2.25d),
                                "FLAWED_RUBY_GEM",
                                product(Double.POSITIVE_INFINITY)));
        MiningSessionBazaarPriceCache.publish(market, 5_000L);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(6_000L);
        assertTrue(book.isAvailable());
        assertTrue(book.unitPrice(TrackedMaterial.GOLD.rawBazaarId()).isPresent());
        assertTrue(book.unitPrice(TrackedMaterial.DIAMOND.rawBazaarId()).isPresent());
        assertTrue(book.unitPrice("ROUGH_TOPAZ_GEM").isPresent());
        assertTrue(book.unitPrice(TrackedMaterial.GOLD.enchantedBazaarId())
                .isEmpty());
        assertTrue(book.unitPrice(TrackedMaterial.DIAMOND.enchantedBazaarId())
                .isEmpty());
        assertTrue(book.unitPrice("FLAWED_RUBY_GEM").isEmpty());
    }

    @Test
    void captureNeverThrowsOnAbsurdCachedValues() {
        BazaarPriceService.MarketPrices market =
                new BazaarPriceService.MarketPrices(
                        Map.of(
                                TrackedMaterial.GOLD,
                                new BazaarPriceService.MaterialPrices(
                                        product(Double.NaN),
                                        product(Double.NEGATIVE_INFINITY))),
                        Map.of());
        MiningSessionBazaarPriceCache.publish(market, 1_000L);
        assertDoesNotThrow(() -> {
            MiningSessionPriceBook book =
                    MiningSessionBazaarPriceProvider.INSTANCE.capture(2_000L);
            assertFalse(book.isAvailable());
        });
    }

    @Test
    void mithrilTitaniumPauseResumeAndNewGainRemainIntegrated() {
        AtomicLong clock = new AtomicLong(10_000L);
        AtomicReference<TrackerSelection> selection =
                new AtomicReference<>(TrackerSelection.GOLD);
        MiningSessionEngine engine = new MiningSessionEngine();
        RotClientCurrentSession session = new RotClientCurrentSession();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        engine.onDiagnosticStart(
                true,
                selection.get(),
                baseline(selection.get(), clock.get()),
                clock.get());
        session.ensureActiveForTracker(selection.get(), clock.get());
        SackItemGainPipeline.Context context = new SackItemGainPipeline.Context(
                engine,
                session,
                catalog,
                selection::get,
                () -> SkyBlockArea.DWARVEN_MINES,
                engine::isCollectionActive,
                clock::get);

        engine.onConfirmedMaterialBreak(
                TrackedMaterial.MITHRIL, 1, clock.addAndGet(10L));
        assertTrue(SackItemGainPipeline.submit(
                context,
                sack("Mithril", 7L),
                10_000L,
                "mithril-before-pause").creditedOthers());
        engine.onConfirmedMaterialBreak(
                TrackedMaterial.TITANIUM, 1, clock.addAndGet(10L));
        assertTrue(SackItemGainPipeline.submit(
                context,
                sack("Titanium", 8L),
                10_000L,
                "titanium-before-pause").creditedOthers());
        assertEquals(7L, otherQty(session, "MITHRIL"));
        assertEquals(8L, otherQty(session, "TITANIUM"));

        session.pause(clock.addAndGet(10L));
        engine.onDiagnosticStop(clock.get());
        assertTrue(session.isPaused());
        assertFalse(engine.isCollectionActive());
        assertFalse(SackItemGainPipeline.submit(
                context,
                sack("Mithril", 100L),
                10_000L,
                "paused-mithril").creditedOthers());
        assertEquals(7L, otherQty(session, "MITHRIL"));

        BazaarPriceService.MarketPrices market =
                new BazaarPriceService.MarketPrices(
                        Map.of(
                                TrackedMaterial.DIAMOND,
                                new BazaarPriceService.MaterialPrices(
                                        product(4.99260315136572d),
                                        product(500.0d)),
                                TrackedMaterial.GOLD,
                                new BazaarPriceService.MaterialPrices(
                                        product(12.5d),
                                        product(400.0d))),
                        Map.of());
        MiningSessionBazaarPriceCache.publish(market, clock.get());

        session.resume(clock.addAndGet(100L));
        engine.onDiagnosticStart(
                true,
                selection.get(),
                baseline(selection.get(), clock.get()),
                clock.get());
        assertTrue(session.isActive());
        assertTrue(engine.isCollectionActive());

        clock.addAndGet(RotClientCurrentSession.valuationRefreshIntervalMillis());
        assertDoesNotThrow(() -> session.refreshFromEngineMetadata(
                engine.snapshot(clock.get()).orElse(null),
                selection.get(),
                engine.capturePriceBook(clock.get()),
                clock.get()));
        assertEquals(7L, otherQty(session, "MITHRIL"));
        assertEquals(8L, otherQty(session, "TITANIUM"));
        assertUnresolved(session, "MITHRIL");
        assertUnresolved(session, "TITANIUM");

        engine.onConfirmedMaterialBreak(
                TrackedMaterial.MITHRIL, 1, clock.addAndGet(10L));
        assertTrue(SackItemGainPipeline.submit(
                context,
                sack("Mithril", 3L),
                10_000L,
                "mithril-after-resume").creditedOthers());
        assertEquals(10L, otherQty(session, "MITHRIL"));
        assertEquals(8L, otherQty(session, "TITANIUM"));

        engine.onConfirmedMaterialBreak(
                TrackedMaterial.GOLD, 1, clock.addAndGet(10L));
        assertFalse(SackItemGainPipeline.submit(
                context,
                sack("Gold Ingot", 20L),
                10_000L,
                "gold-target-after-resume").creditedOthers());
        assertEquals(0L, otherQty(session, "GOLD"));
    }

    private static long otherQty(RotClientCurrentSession session, String id) {
        return session.snapshotConfig().items.stream()
                .filter(i -> id.equals(i.itemId()))
                .filter(i -> i.source() == SessionSourceType.MINING)
                .filter(i -> i.miningClassification() == MiningClassification.OTHER)
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    private static void assertUnresolved(
            RotClientCurrentSession session,
            String id) {
        RotClientCurrentSessionConfig.SessionItemRecord row =
                session.snapshotConfig().items.stream()
                        .filter(i -> id.equals(i.itemId()))
                        .filter(i -> i.source() == SessionSourceType.MINING)
                        .filter(i -> i.miningClassification()
                                == MiningClassification.OTHER)
                        .findFirst()
                        .orElseThrow();
        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                row.price());
        assertEquals(0.0, row.resolvedGrossValue());
    }

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long now) {
        return MiningSessionParity.LiveBaseline.material(
                selection,
                Map.of(),
                Map.of(),
                now);
    }

    private static SackChangeParser.Change sack(String item, long quantity) {
        return new SackChangeParser.Change(
                quantity,
                item,
                List.of("Mining Sack"));
    }

    private static BazaarPriceService.ProductPrice product(double price) {
        return new BazaarPriceService.ProductPrice(
                java.util.List.of(new BazaarPriceService.OrderLevel(64, price)),
                price);
    }
}
