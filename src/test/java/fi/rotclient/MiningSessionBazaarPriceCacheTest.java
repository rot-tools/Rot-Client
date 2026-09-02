package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class MiningSessionBazaarPriceCacheTest {
    @BeforeEach
    void resetCache() {
        MiningSessionBazaarPriceCache.publish(null, -1L);
    }

    @Test
    void initialCacheIsUnavailable() {
        MiningSessionBazaarPriceCache.CacheSnapshot snapshot =
                MiningSessionBazaarPriceCache.capture();

        assertFalse(snapshot.isAvailable());
        assertEquals(-1L, snapshot.observedAtMillis());
    }

    @Test
    void combinedMaterialAndGemstonePricesShareOneCacheSnapshot() {
        BazaarPriceService.MarketPrices prices =
                new BazaarPriceService.MarketPrices(
                        Map.of(
                                TrackedMaterial.GOLD,
                                new BazaarPriceService.MaterialPrices(
                                        product(10.0),
                                        product(100.0))),
                        Map.of("ROUGH_RUBY_GEM", product(1.5)));

        MiningSessionBazaarPriceCache.publish(prices, 5_000L);
        MiningSessionBazaarPriceCache.CacheSnapshot snapshot =
                MiningSessionBazaarPriceCache.capture();

        assertTrue(snapshot.isAvailable());
        assertEquals(5_000L, snapshot.observedAtMillis());
        assertNotNull(snapshot.marketPrices().forMaterial(TrackedMaterial.GOLD));
        assertEquals(
                1.5,
                snapshot.marketPrices()
                        .forGemstoneProduct("ROUGH_RUBY_GEM")
                        .instantSellPrice(),
                0.0001);
    }

    @Test
    void publishAssignsOneAtomicSnapshot() {
        BazaarPriceService.MarketPrices prices = goldPrices(10.0, 100.0);

        MiningSessionBazaarPriceCache.publish(prices, 5_000L);
        MiningSessionBazaarPriceCache.CacheSnapshot snapshot =
                MiningSessionBazaarPriceCache.capture();

        assertTrue(snapshot.isAvailable());
        assertSame(prices, snapshot.marketPrices());
        assertEquals(5_000L, snapshot.observedAtMillis());
    }

    @Test
    void laterPublicationDoesNotMutateEarlierCapturedPriceBook() {
        MiningSessionBazaarPriceCache.publish(goldPrices(10.0, 100.0), 1_000L);
        MiningSessionPriceBook firstBook =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(2_000L);

        MiningSessionBazaarPriceCache.publish(goldPrices(20.0, 200.0), 9_000L);
        MiningSessionPriceBook secondBook =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(10_000L);

        assertEquals(
                0,
                new BigDecimal("10.0").compareTo(
                        firstBook.unitPrice(TrackedMaterial.GOLD.rawBazaarId())
                                .orElseThrow()));
        assertEquals(1_000L, firstBook.observedAtMillis());
        assertEquals(
                0,
                new BigDecimal("20.0").compareTo(
                        secondBook.unitPrice(TrackedMaterial.GOLD.rawBazaarId())
                                .orElseThrow()));
        assertEquals(9_000L, secondBook.observedAtMillis());
        assertNotSame(firstBook, secondBook);
    }

    @Test
    void sequentialPublicationsAlwaysCaptureCompletePairs() {
        for (long observedAt = 1_000L; observedAt <= 3_000L; observedAt += 1_000L) {
            MiningSessionBazaarPriceCache.publish(
                    goldPrices(observedAt, observedAt * 10D),
                    observedAt);
            MiningSessionBazaarPriceCache.CacheSnapshot snapshot =
                    MiningSessionBazaarPriceCache.capture();

            assertTrue(snapshot.isAvailable());
            assertEquals(observedAt, snapshot.observedAtMillis());
            assertEquals(
                    observedAt,
                    snapshot.marketPrices()
                            .forMaterial(TrackedMaterial.GOLD)
                            .raw()
                            .instantSellPrice(),
                    0.0001);
        }
    }

    @Test
    void invalidPublicationMarksCacheUnavailable() {
        MiningSessionBazaarPriceCache.publish(goldPrices(10.0, 100.0), 1_000L);
        MiningSessionBazaarPriceCache.publish(null, -1L);

        MiningSessionBazaarPriceCache.CacheSnapshot snapshot =
                MiningSessionBazaarPriceCache.capture();

        assertFalse(snapshot.isAvailable());
        assertFalse(MiningSessionBazaarPriceProvider.INSTANCE
                .capture(2_000L)
                .isAvailable());
    }

    private static BazaarPriceService.MarketPrices goldPrices(
            double rawPrice,
            double enchantedPrice) {
        return new BazaarPriceService.MarketPrices(Map.of(
                TrackedMaterial.GOLD,
                new BazaarPriceService.MaterialPrices(
                        product(rawPrice),
                        product(enchantedPrice))));
    }

    private static BazaarPriceService.ProductPrice product(double price) {
        return new BazaarPriceService.ProductPrice(
                java.util.List.of(
                        new BazaarPriceService.OrderLevel(100, price)),
                price);
    }
}
