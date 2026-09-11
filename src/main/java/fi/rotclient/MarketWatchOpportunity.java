package fi.rotclient;

record MarketWatchOpportunity(
        Market market,
        String id,
        String itemId,
        String itemName,
        String category,
        String tier,
        String auctionUuid,
        String sellerUuid,
        double buyPricePerUnit,
        double sellPricePerUnit,
        long quantity,
        double expectedProfitCoins,
        double roiPercent,
        double score,
        double marketEdgePercent,
        double estimatedFeesCoins,
        double riskBufferCoins,
        Confidence confidence,
        Liquidity liquidity,
        int comparisonCount,
        long weeklyVolume,
        long observedAtMillis) {

    enum Market {
        AUCTION_HOUSE,
        BAZAAR
    }

    enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    enum Liquidity {
        LOW,
        MEDIUM,
        HIGH
    }

    MarketWatchOpportunity {
        market = market == null
                ? Market.AUCTION_HOUSE
                : market;

        id = clean(id);
        itemId = clean(itemId);
        itemName = clean(itemName);
        category = clean(category);
        tier = clean(tier);
        auctionUuid = clean(auctionUuid);
        sellerUuid = clean(sellerUuid);

        buyPricePerUnit = positiveFinite(buyPricePerUnit);
        sellPricePerUnit = positiveFinite(sellPricePerUnit);

        quantity = Math.max(1L, quantity);

        expectedProfitCoins = finite(expectedProfitCoins);
        roiPercent = finite(roiPercent);

        score = Math.max(
                0.0D,
                Math.min(
                        99.9D,
                        finite(score)));

        marketEdgePercent = Math.max(
                0.0D,
                finite(marketEdgePercent));

        estimatedFeesCoins = Math.max(
                0.0D,
                finite(estimatedFeesCoins));

        riskBufferCoins = Math.max(
                0.0D,
                finite(riskBufferCoins));

        confidence = confidence == null
                ? Confidence.LOW
                : confidence;

        liquidity = liquidity == null
                ? Liquidity.LOW
                : liquidity;

        comparisonCount = Math.max(
                0,
                comparisonCount);

        weeklyVolume = Math.max(
                0L,
                weeklyVolume);

        observedAtMillis = Math.max(
                0L,
                observedAtMillis);
    }

    double capitalCoins() {
        return buyPricePerUnit * quantity;
    }

    double grossReturnCoins() {
        return sellPricePerUnit * quantity;
    }

    private static String clean(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    private static double positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0D
                ? value
                : 0.0D;
    }

    private static double finite(double value) {
        return Double.isFinite(value)
                ? value
                : 0.0D;
    }
}