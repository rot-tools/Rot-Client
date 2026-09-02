package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class MiningSessionBazaarPriceProviderTest {
    @BeforeEach
    void resetCache() {
        MiningSessionBazaarPriceCache.publish(null, -1L);
    }

    @Test
    void capturesGemstoneProductsFromCombinedMarketPrices() {
        BazaarPriceService.MarketPrices marketPrices =
                new BazaarPriceService.MarketPrices(
                        Map.of(
                                TrackedMaterial.GOLD,
                                new BazaarPriceService.MaterialPrices(
                                        product(12.5),
                                        product(500.0))),
                        Map.of(
                                "ROUGH_TOPAZ_GEM",
                                product(2.0),
                                "FLAWED_RUBY_GEM",
                                product(3.0)));
        MiningSessionBazaarPriceCache.publish(marketPrices, 5_000L);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(6_000L);

        assertTrue(book.isAvailable());
        assertEquals(
                0,
                new BigDecimal("2.0").compareTo(
                        book.unitPrice("ROUGH_TOPAZ_GEM").orElseThrow()));
        assertEquals(
                0,
                new BigDecimal("3.0").compareTo(
                        book.unitPrice("FLAWED_RUBY_GEM").orElseThrow()));
        assertTrue(book.unitPrice("FINE_RUBY_GEM").isEmpty());
    }

    @Test
    void capturesCurrentMarketPricesWithoutNetwork() {
        BazaarPriceService.MarketPrices marketPrices =
                new BazaarPriceService.MarketPrices(Map.of(
                        TrackedMaterial.GOLD,
                        new BazaarPriceService.MaterialPrices(
                                product(12.5),
                                product(500.0))));
        MiningSessionBazaarPriceCache.publish(marketPrices, 5_000L);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(6_000L);

        assertTrue(book.isAvailable());
        assertEquals(5_000L, book.observedAtMillis());
        assertEquals(
                0,
                new BigDecimal("12.5").compareTo(
                        book.unitPrice(TrackedMaterial.GOLD.rawBazaarId())
                                .orElseThrow()));
        assertEquals(
                0,
                new BigDecimal("500.0").compareTo(
                        book.unitPrice(TrackedMaterial.GOLD.enchantedBazaarId())
                                .orElseThrow()));
    }

    @Test
    void unavailableWhenCacheMissing() {
        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(1_000L);

        assertFalse(book.isAvailable());
        assertFalse(book.invalidTimestamp());
    }

    @Test
    void capturesValidRawProductWhenEnchantedPeerIsUnavailable() {
        BazaarPriceService.MarketPrices marketPrices =
                new BazaarPriceService.MarketPrices(Map.of(
                        TrackedMaterial.GOLD,
                        new BazaarPriceService.MaterialPrices(
                                product(12.5),
                                null)));
        MiningSessionBazaarPriceCache.publish(marketPrices, 5_000L);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(6_000L);

        assertTrue(book.isAvailable());
        assertTrue(book.unitPrice(TrackedMaterial.GOLD.rawBazaarId()).isPresent());
        assertTrue(book.unitPrice(TrackedMaterial.GOLD.enchantedBazaarId())
                .isEmpty());
    }

    @Test
    void negativeNowMillisFailsClosed() {
        MiningSessionBazaarPriceCache.publish(
                goldPrices(10.0, 100.0),
                1_000L);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(-1L);

        assertFalse(book.isAvailable());
        assertTrue(book.invalidTimestamp());
    }

    @Test
    void futureObservedTimestampFailsClosed() {
        MiningSessionBazaarPriceCache.publish(
                goldPrices(10.0, 100.0),
                5_000L);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(4_999L);

        assertFalse(book.isAvailable());
        assertTrue(book.invalidTimestamp());
    }

    @Test
    void farFutureObservedTimestampFailsClosed() {
        MiningSessionBazaarPriceCache.publish(
                goldPrices(10.0, 100.0),
                Long.MAX_VALUE);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(1_000L);

        assertFalse(book.isAvailable());
        assertTrue(book.invalidTimestamp());
    }

    @Test
    void negativeObservedTimestampFailsClosed() {
        MiningSessionBazaarPriceCache.publish(
                goldPrices(10.0, 100.0),
                -1L);

        MiningSessionPriceBook book =
                MiningSessionBazaarPriceProvider.INSTANCE.capture(1_000L);

        assertFalse(book.isAvailable());
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
