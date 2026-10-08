package fi.rotclient;

/** All trade settings are Plus-owned. A zero budget cannot place a transaction. */
final class MarketTradeSettings {
    int schemaVersion = 1;
    long sessionBudgetCoins;
    long perTradeCoins;
    double pursePercent = 10;
    long reserveCoins = 100_000;
    long minProfitCoins = 50_000;
    double minRoiPercent = 5;
    int minComparables = 5;
    int minSellers = 3;
    double maxPeerSpreadRatio = 1.5;
    double maxReferenceChangePercent = 20;
    int maxSnapshotAgeSeconds = 90;
    int maxOpenPositions = 3;
    int listingHours = 48;
    long undercutCoins = 1;
    long maxListingFeeCoins = 100_000;
    long maxBazaarQuantity = 64;
    boolean auctionEnabled = true;
    boolean bazaarEnabled;
    String rarity = "";
    String reforge = "";
    double bazaarTaxPercent = 1.25;
    java.util.Set<String> watchIds = new java.util.HashSet<>();
    void normalize() {
        sessionBudgetCoins = clamp(sessionBudgetCoins, 0, 100_000_000_000L);
        perTradeCoins = clamp(perTradeCoins, 0, sessionBudgetCoins);
        reserveCoins = clamp(reserveCoins, 0, 100_000_000_000L);
        pursePercent = finite(pursePercent, 0, 100);
        minRoiPercent = finite(minRoiPercent, 0, 1000);
        minProfitCoins = clamp(minProfitCoins, 0, 100_000_000_000L);
        minComparables = (int) clamp(minComparables, 3, 100);
        minSellers = (int) clamp(minSellers, 2, minComparables);
        maxPeerSpreadRatio = finite(maxPeerSpreadRatio, 1.01, 10);
        maxReferenceChangePercent = finite(maxReferenceChangePercent, 1, 100);
        maxSnapshotAgeSeconds = (int) clamp(maxSnapshotAgeSeconds, 5, 120);
        maxOpenPositions = (int) clamp(maxOpenPositions, 1, 20);
        listingHours = java.util.List.of(1, 6, 12, 24, 48).contains(listingHours) ? listingHours : 48;
        undercutCoins = clamp(undercutCoins, 0, 100_000_000L);
        maxListingFeeCoins = clamp(maxListingFeeCoins, 0, 100_000_000L);
        maxBazaarQuantity = clamp(maxBazaarQuantity, 1, 64);
        bazaarTaxPercent = finite(bazaarTaxPercent, 0, 20);
        rarity = rarity == null ? "" : rarity.trim().toUpperCase(java.util.Locale.ROOT);
        if (!MarketWatchVariantPolicy.TIERS.contains(rarity)) rarity = "";
        reforge = MarketWatchVariantPolicy.reforgeFilter(reforge);
        if (watchIds == null) watchIds = new java.util.HashSet<>();
    }
    long allowance(long purse, long reserved) {
        normalize();
        if (purse < 0 || reserved < 0 || reserved > sessionBudgetCoins) return 0;
        long available = Math.max(0, purse - Math.min(purse, reserveCoins));
        long percentCap = java.math.BigDecimal.valueOf(available).multiply(java.math.BigDecimal.valueOf(pursePercent))
                .divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN).longValueExact();
        return Math.min(Math.min(available, percentCap), Math.min(perTradeCoins, sessionBudgetCoins - reserved));
    }
    private static long clamp(long n, long lo, long hi) { return Math.max(lo, Math.min(hi, n)); }
    private static double finite(double n, double lo, double hi) { return Double.isFinite(n) ? Math.max(lo, Math.min(hi, n)) : lo; }
}
