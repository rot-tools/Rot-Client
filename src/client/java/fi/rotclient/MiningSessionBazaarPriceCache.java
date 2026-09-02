package fi.rotclient;

/** Atomic Bazaar cache snapshot shared by live HUD and shadow valuation. */
final class MiningSessionBazaarPriceCache {
    static final class CacheSnapshot {
        static final CacheSnapshot UNAVAILABLE =
                new CacheSnapshot(null, -1L);

        private final BazaarPriceService.MarketPrices marketPrices;
        private final long observedAtMillis;

        private CacheSnapshot(
                BazaarPriceService.MarketPrices marketPrices,
                long observedAtMillis) {
            this.marketPrices = marketPrices;
            this.observedAtMillis = observedAtMillis;
        }

        static CacheSnapshot available(
                BazaarPriceService.MarketPrices marketPrices,
                long observedAtMillis) {
            if (marketPrices == null || observedAtMillis < 0L) {
                return UNAVAILABLE;
            }
            return new CacheSnapshot(marketPrices, observedAtMillis);
        }

        boolean isAvailable() {
            return marketPrices != null && observedAtMillis >= 0L;
        }

        BazaarPriceService.MarketPrices marketPrices() {
            return marketPrices;
        }

        long observedAtMillis() {
            return observedAtMillis;
        }
    }

    private static volatile CacheSnapshot snapshot =
            CacheSnapshot.UNAVAILABLE;

    private MiningSessionBazaarPriceCache() {
    }

    static CacheSnapshot capture() {
        return snapshot;
    }

    static void publish(
            BazaarPriceService.MarketPrices prices,
            long observedAtMillis) {
        snapshot = CacheSnapshot.available(prices, observedAtMillis);
    }
}
