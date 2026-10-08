package fi.rotclient;

import java.math.*;
import java.util.List;

/** Bazaar spread means buy-order -> sell-offer. It never authorizes an instant buy. */
final class MarketTradeBazaarPolicy {
    record Proposal(String id, double buy, double sell, long quantity, long reserved) {}
    static Proposal propose(MarketWatchBazaarProduct p, MarketTradeSettings s, long purse, long spent, double previous) {
        if (p == null || p.productId().isBlank() || p.buyOrders().size() < 3 || p.sellOrders().size() < 3
                || Math.min(p.buyMovingWeek(), p.sellMovingWeek()) < 10000) return null;
        // API buy_summary (buyOrders here) is the instant-buy / sell-offer book; sell_summary is bids.
        double ask = p.buyOrders().stream().mapToDouble(MarketWatchBazaarProduct.OrderLevel::pricePerUnit).min().orElse(0);
        double bid = p.sellOrders().stream().mapToDouble(MarketWatchBazaarProduct.OrderLevel::pricePerUnit).max().orElse(0);
        if (!Double.isFinite(ask) || !Double.isFinite(bid) || ask <= bid || bid <= 0
                || !Double.isFinite(p.quickBuyPrice()) || !Double.isFinite(p.quickSellPrice())
                || Math.abs(p.quickBuyPrice() - ask) / ask > .10 || Math.abs(p.quickSellPrice() - bid) / bid > .10
                || (ask - bid) / bid > .50 || previous > 0 && Math.abs(ask - previous) * 100 / previous > s.maxReferenceChangePercent) return null;
        if (p.buyOrders().stream().anyMatch(o -> o.pricePerUnit() <= 0 || !Double.isFinite(o.pricePerUnit()) || o.amount() <= 0)
                || p.sellOrders().stream().anyMatch(o -> o.pricePerUnit() <= 0 || !Double.isFinite(o.pricePerUnit()) || o.amount() <= 0)) return null;
        double buy = BigDecimal.valueOf(bid).add(new BigDecimal("0.1")).setScale(1,RoundingMode.CEILING).doubleValue();
        double sell = BigDecimal.valueOf(ask).subtract(new BigDecimal("0.1")).setScale(1,RoundingMode.DOWN).doubleValue();
        long cap = s.allowance(purse, spent);
        long depth = Math.min(p.buyOrders().getFirst().amount(), p.sellOrders().getFirst().amount()) / 10;
        long quantity = Math.min(s.maxBazaarQuantity, Math.min(depth, (long) Math.floor(cap / buy)));
        if (quantity < 1 || !MarketTradeRiskPolicy.profitable(buy * quantity, sell * quantity,
                sell * quantity * Math.max(.05, s.bazaarTaxPercent / 100), s)) return null;
        long reserved = MarketTradeRiskPolicy.ceiling(buy * quantity);
        return reserved > 0 && reserved <= cap ? new Proposal(p.productId(),buy,sell,quantity,reserved) : null;
    }
    static boolean amount(List<String> lore, long quantity) {
        String expected = String.format(java.util.Locale.ROOT,"%,d",quantity);
        return lore.stream().map(CommissionDisplayPolicy::normalizeLine)
                .anyMatch(row -> row.equals("Order amount: " + expected + "x") || row.equals("Offer amount: " + expected + "x"));
    }
    static boolean owner(List<String> lore, String username) {
        return lore.stream().map(CommissionDisplayPolicy::normalizeLine).anyMatch(row ->
                row.matches("^By: (?:\\[[^]]+] )?" + java.util.regex.Pattern.quote(username) + "$"));
    }
    private MarketTradeBazaarPolicy() {}
}
