package fi.rotclient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Conservative checks are evidence heuristics, never a claim to know an item's true value. */
final class MarketTradeRiskPolicy {
    record Peer(long price, String seller) {}
    record Assessment(boolean eligible, long reference, String reason) {}
    static Assessment assess(List<Peer> peers, MarketTradeSettings settings, long previousReference) {
        var valid = peers.stream().filter(p -> p.price > 0 && p.seller != null && !p.seller.isBlank())
                .sorted(java.util.Comparator.comparingLong(Peer::price)).toList();
        if (valid.size() < settings.minComparables) return new Assessment(false, 0, "Too few same-variant comparisons");
        if (valid.stream().map(p -> p.seller.toLowerCase(java.util.Locale.ROOT)).distinct().count() < settings.minSellers)
            return new Assessment(false, 0, "Comparable listings concentrated in too few sellers");
        var sellerCounts = valid.stream().collect(java.util.stream.Collectors.groupingBy(
                p -> p.seller.toLowerCase(java.util.Locale.ROOT), java.util.stream.Collectors.counting()));
        if (sellerCounts.values().stream().anyMatch(count -> count > valid.size() * .6))
            return new Assessment(false, 0, "One seller controls most comparable listings");
        long low = valid.get((valid.size() - 1) / 4).price, median = valid.get(valid.size() / 2).price;
        if (median / (double) low > settings.maxPeerSpreadRatio)
            return new Assessment(false, low, "Wide comparable prices: manipulation risk");
        // A future listing competes with current low BINs. An inflated median is not a resale price.
        long reference = valid.getFirst().price;
        if (previousReference > 0 && Math.abs(reference - (double) previousReference) * 100 / previousReference
                > settings.maxReferenceChangePercent)
            return new Assessment(false, reference, "Abrupt reference-price change");
        return new Assessment(true, reference, "Same-variant market checks passed");
    }
    static long ceiling(double value) {
        if (!Double.isFinite(value) || value <= 0 || value > 100_000_000_000L) return -1;
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.CEILING).longValueExact();
    }
    static boolean profitable(double buy, double sell, double fees, MarketTradeSettings settings) {
        if (!Double.isFinite(buy) || !Double.isFinite(sell) || !Double.isFinite(fees) || buy <= 0 || fees < 0) return false;
        double profit = sell - buy - fees;
        return profit >= settings.minProfitCoins && profit / buy * 100 >= settings.minRoiPercent;
    }
    private MarketTradeRiskPolicy() {}
}
