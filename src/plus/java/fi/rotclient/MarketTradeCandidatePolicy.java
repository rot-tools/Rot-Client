package fi.rotclient;

import java.util.List;
import java.util.OptionalLong;

/** Immutable transaction proposal; ordinary Market Watch alerts never imply permission to buy. */
final class MarketTradeCandidatePolicy {
    record Target(String itemId, String name, String tier, String reforge, long maxPrice,
                  long minProfit, double minDiscount, double minRoi) {
        boolean matches(MarketWatchAuction a) {
            var watch = new MarketWatchAuctionWatch();
            watch.itemId = itemId; watch.itemName = name; watch.tier = tier; watch.reforge = reforge;
            return MarketWatchVariantPolicy.matches(watch, a);
        }
    }
    record Proposal(MarketWatchAuction auction, long buy, long sell, long reserved, String reason) {}
    static Proposal propose(MarketWatchAuction a, List<MarketWatchAuction> peers, List<Target> targets,
                            MarketTradeSettings s, long purse, long spent, String owner, long now, long previous) {
        if (a == null || !a.bin() || a.endMillis() <= now || !a.variant().known()
                || a.variant().attributesKey().isBlank() || a.variant().itemUuid().isBlank()
                || a.variant().quantity() != 1 || a.tier().isBlank()
                || uuid(a.auctioneerUuid()).equals(uuid(owner)) || !validUuid(a.uuid())
                || !validUuid(a.auctioneerUuid()) || !validUuid(a.variant().itemUuid())) return null;
        if (!s.rarity.isBlank() && !s.rarity.equals(a.tier())) return null;
        String modifier = s.reforge;
        if (!modifier.isBlank() && !(modifier.equals("none") ? a.variant().reforge().isBlank()
                : modifier.equals(a.variant().reforge()))) return null;
        var allowed = targets.stream().filter(t -> t.matches(a)).toList();
        if (allowed.isEmpty()) return null;
        long buy = MarketTradeRiskPolicy.ceiling(a.startingBid());
        if (buy < 1 || a.startingBid() != buy) return null;
        String key = MarketWatchVariantPolicy.comparableKey(a);
        var comparables = peers.stream().filter(p -> p.bin() && p.endMillis() > now
                && !p.uuid().equals(a.uuid()) && p.variant().quantity() == 1
                && !uuid(p.auctioneerUuid()).equals(uuid(a.auctioneerUuid()))
                && !uuid(p.auctioneerUuid()).equals(uuid(owner))
                && key.equals(MarketWatchVariantPolicy.comparableKey(p)))
                .map(p -> new MarketTradeRiskPolicy.Peer(MarketTradeRiskPolicy.ceiling(p.startingBid()), p.auctioneerUuid())).toList();
        var risk = MarketTradeRiskPolicy.assess(comparables, s, previous);
        if (!risk.eligible()) return null;
        long sell = risk.reference() - Math.min(risk.reference(), s.undercutCoins);
        // A conservative 5% resale-tax buffer plus the configured maximum listing fee.
        double fees = sell * .05 + s.maxListingFeeCoins;
        if (!MarketTradeRiskPolicy.profitable(buy, sell, fees, s)) return null;
        if (allowed.stream().noneMatch(t -> (t.maxPrice <= 0 || buy <= t.maxPrice)
                && sell - buy - fees >= t.minProfit && (sell - buy - fees) / buy * 100 >= t.minRoi
                && (sell - buy) / (double) sell * 100 >= t.minDiscount)) return null;
        long reserve;
        try { reserve = Math.addExact(buy, s.maxListingFeeCoins); } catch (ArithmeticException overflow) { return null; }
        return reserve <= s.allowance(purse, spent) ? new Proposal(a, buy, sell, reserve, risk.reason()) : null;
    }
    static OptionalLong purse(List<String> lines) {
        Long value = null;
        for (String line : lines) {
            var m = java.util.regex.Pattern.compile("^(?:Purse|Piggy): ([0-9,]+(?:\\.[0-9]+)?)$")
                    .matcher(CommissionDisplayPolicy.normalizeLine(line));
            if (!m.matches()) continue;
            try {
                long amount = new java.math.BigDecimal(m.group(1).replace(",", ""))
                        .setScale(0, java.math.RoundingMode.DOWN).longValueExact();
                if (amount < 0 || value != null && value != amount) return OptionalLong.empty();
                value = amount;
            } catch (RuntimeException invalid) { return OptionalLong.empty(); }
        }
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }
    static boolean fresh(long observed, long serverUpdated, long now, MarketTradeSettings s) {
        long limit = s.maxSnapshotAgeSeconds * 1000L;
        return observed > 0 && serverUpdated > 0 && now >= observed && now >= serverUpdated
                && now - observed <= limit && now - serverUpdated <= limit;
    }
    static String uuid(String raw) { return raw == null ? "" : raw.replace("-", "").toLowerCase(java.util.Locale.ROOT); }
    static boolean validUuid(String raw) { return uuid(raw).matches("[0-9a-f]{32}"); }
    private MarketTradeCandidatePolicy() {}
}
