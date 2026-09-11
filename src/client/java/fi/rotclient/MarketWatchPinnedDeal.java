package fi.rotclient;

import java.util.Locale;

record MarketWatchPinnedDeal(
        String id,
        MarketWatchOpportunity.Market market,
        String itemId,
        String itemName,
        String category,
        double buyPricePerUnit,
        double sellPricePerUnit,
        long quantity,
        double expectedProfitCoins,
        double roiPercent,
        long observedAtMillis,
        long pinnedAtMillis) {

    MarketWatchPinnedDeal {

        id =
                id == null
                        ? ""
                        : id;

        market =
                market == null
                        ? MarketWatchOpportunity.Market.AUCTION_HOUSE
                        : market;

        itemId =
                itemId == null
                        ? ""
                        : itemId;

        itemName =
                itemName == null
                        || itemName.isBlank()
                        ? itemId
                        : itemName;

        category =
                category == null
                        ? ""
                        : category;

        quantity =
                Math.max(
                        1L,
                        quantity);

        buyPricePerUnit =
                finiteNonNegative(
                        buyPricePerUnit);

        sellPricePerUnit =
                finiteNonNegative(
                        sellPricePerUnit);

        expectedProfitCoins =
                Double.isFinite(
                        expectedProfitCoins)
                        ? expectedProfitCoins
                        : 0.0D;

        roiPercent =
                Double.isFinite(
                        roiPercent)
                        ? roiPercent
                        : 0.0D;

        observedAtMillis =
                Math.max(
                        0L,
                        observedAtMillis);

        pinnedAtMillis =
                Math.max(
                        0L,
                        pinnedAtMillis);
    }

    static MarketWatchPinnedDeal from(
            MarketWatchOpportunity opportunity) {

        long now =
                System.currentTimeMillis();

        return new MarketWatchPinnedDeal(
                opportunity.id(),
                opportunity.market(),
                opportunity.itemId(),
                opportunity.itemName(),
                opportunity.category(),
                opportunity.buyPricePerUnit(),
                opportunity.sellPricePerUnit(),
                opportunity.quantity(),
                opportunity.expectedProfitCoins(),
                opportunity.roiPercent(),
                opportunity.observedAtMillis(),
                now);
    }

    /*
     * One canonical display name for every representation of a frozen
     * opportunity. In particular, legacy/raw variants such as SAND:1
     * become simply "Sand" rather than "Sand 1" or "Sand:1".
     */
    String displayName() {

        String preferred =
                cleanPinnedName(
                        itemName);

        if (!preferred.isBlank()) {
            return preferred;
        }

        String fallback =
                cleanPinnedName(
                        itemId);

        return fallback.isBlank()
                ? "Unknown Item"
                : fallback;
    }

    private static String cleanPinnedName(
            String raw) {

        if (raw == null
                || raw.isBlank()) {

            return "";
        }

        String value =
                raw.trim();

        /*
         * Hypixel / Minecraft legacy style variant or damage suffix.
         * SAND:1 -> SAND
         *
         * Only strip a numeric trailing component, so ordinary names
         * containing punctuation are left untouched.
         */
        value =
                value.replaceFirst(
                        ":\\d+$",
                        "");

        if (value.regionMatches(
                true,
                0,
                "minecraft:",
                0,
                "minecraft:".length())) {

            value =
                    value.substring(
                            "minecraft:".length());
        }

        if (value.isBlank()) {
            return "";
        }

        boolean internalStyle =
                value.indexOf('_') >= 0
                        || value.indexOf('-') >= 0
                        || value.equals(
                        value.toUpperCase(
                                Locale.ROOT));

        if (!internalStyle) {
            return value;
        }

        String normalized =
                value.replace('_', ' ')
                        .replace('-', ' ')
                        .trim();

        String[] parts =
                normalized.split(
                        "\\s+");

        StringBuilder result =
                new StringBuilder();

        for (String part
                : parts) {

            if (part == null
                    || part.isBlank()) {

                continue;
            }

            String lower =
                    part.toLowerCase(
                            Locale.ROOT);

            if (result.length() > 0) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(
                            lower.charAt(0)));

            if (lower.length() > 1) {
                result.append(
                        lower.substring(1));
            }
        }

        return result.toString();
    }

    double capitalCoins() {
        return buyPricePerUnit
                * quantity;
    }

    double grossReturnCoins() {
        return sellPricePerUnit
                * quantity;
    }

    private static double finiteNonNegative(
            double value) {

        if (!Double.isFinite(value)) {
            return 0.0D;
        }

        return Math.max(
                0.0D,
                value);
    }
}