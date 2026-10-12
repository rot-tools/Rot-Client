package fi.rotclient;

import java.util.List;
import java.util.regex.Pattern;

final class MarketTradePolicy {
    static final long DELAY = 4_000;
    static final class Countdown {
        private long started, last;
        Countdown(long now) { started = last = now; }
        long remaining(long now) {
            if (now < last || now < 0) return -1;
            last = now;
            return Math.max(0, DELAY - (now - started));
        }
    }
    /** One action per acknowledged container revision, even when identical screens last many ticks. */
    static final class Gate {
        Object identity;
        int state;
        long sentAt = -1;
        boolean canAct(MarketTradeMenuPolicy.Menu menu, long now) {
            return menu != null && now >= 0 && (sentAt < 0 || now >= sentAt && now - sentAt >= 300
                    && (identity != menu.identity() || state != menu.stateId()));
        }
        void sent(MarketTradeMenuPolicy.Menu menu, long now) { identity = menu.identity(); state = menu.stateId(); sentAt = now; }
        void reset() { identity = null; sentAt = -1; }
    }
    static boolean purchaseReceipt(String message, MarketTradeJournal.Position p) {
        String text = CommissionDisplayPolicy.normalizeLine(message);
        var matcher = Pattern.compile("^\\[Auction\\] You purchased (.+) for ([0-9,]+) coins!$").matcher(text);
        try {
            return matcher.matches() && matcher.group(1).equals(p.name)
                    && MarketTradeMenuPolicy.equal(Double.parseDouble(matcher.group(2).replace(",", "")), p.cost());
        } catch (NumberFormatException malformed) { return false; }
    }
    static boolean bazaarSetupReceipt(String message, MarketTradeJournal.Position p, boolean sell) {
        String text = CommissionDisplayPolicy.normalizeLine(message);
        var matcher = Pattern.compile("^\\[Bazaar\\] " + (sell ? "Sell Offer" : "Buy Order")
                + " Setup! ([0-9,]+)x (.+) for ([0-9,.]+) coins\\.$").matcher(text);
        if (!matcher.matches()) return false;
        try {
            return Long.parseLong(matcher.group(1).replace(",", "")) == p.quantity && matcher.group(2).equals(p.name)
                    && MarketTradeMenuPolicy.equal(Double.parseDouble(matcher.group(3).replace(",", "")),
                    (sell ? p.sellUnit : p.buyUnit) * p.quantity);
        } catch (NumberFormatException malformed) { return false; }
    }
    static boolean bazaarOrder(MarketTradeMenuPolicy.Item item, MarketTradeJournal.Position p, boolean sell, boolean filled) {
        if (item == null || !item.name().equals((sell ? "SELL " : "BUY ") + p.name)) return false;
        boolean quantity = MarketTradeBazaarPolicy.amount(item.lore(), p.quantity);
        var price = MarketTradeMenuPolicy.coins(item.lore(), "Price per unit");
        var fills = item.lore().stream().map(CommissionDisplayPolicy::normalizeLine)
                .filter(row -> row.startsWith("Filled:")).toList();
        boolean full = !fills.isEmpty() && fills.stream().allMatch(row -> {
            var matcher = Pattern.compile("^Filled: ([0-9,]+)/([0-9,]+) \\(?100%\\)?!?$").matcher(row);
            try {
                return matcher.matches() && Long.parseLong(matcher.group(1).replace(",", "")) == p.quantity
                        && Long.parseLong(matcher.group(2).replace(",", "")) == p.quantity;
            } catch (NumberFormatException malformed) { return false; }
        });
        return quantity && price.isPresent() && MarketTradeMenuPolicy.equal(price.getAsDouble(), sell ? p.sellUnit : p.buyUnit)
                && (!filled || full);
    }
    static boolean newOwnedItem(List<MarketTradeMenuPolicy.Item> inventory, MarketTradeJournal.Position p) {
        return !p.itemUuid.isBlank() && inventory.stream().filter(item -> item.uuid().equals(p.itemUuid)).count() == 1
                && inventory.stream().filter(item ->
                MarketTradeMenuPolicy.variant(item, p.itemId, p.itemUuid, p.tier, p.reforge, (int) p.quantity)).count() == 1;
    }
    /** The receipt is saved before ownership can be credited; unrelated inventory changes never suffice. */
    static boolean purchaseConfirmed(List<MarketTradeMenuPolicy.Item> inventory, MarketTradeJournal.Position p) {
        if (p == null || !p.purchaseOnly || !p.buySent || !p.receiptObserved || p.manualReview || p.manuallyReviewed) return false;
        if ("AH".equals(p.market)) return p.baselineClear && newOwnedItem(inventory, p);
        if (!"BZ".equals(p.market) || !p.fillObserved || !p.claimSent || p.baselineQuantity < 0) return false;
        long quantity = inventory.stream().filter(item -> p.itemId.equals(item.id())).mapToLong(MarketTradeMenuPolicy.Item::count).sum();
        return quantity >= p.baselineQuantity && quantity - p.baselineQuantity == p.quantity;
    }
    private MarketTradePolicy() {}
}
