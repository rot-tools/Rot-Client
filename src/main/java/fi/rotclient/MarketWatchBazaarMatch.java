package fi.rotclient;

record MarketWatchBazaarMatch(
        String watchId,
        String productId,
        double instantBuyPrice,
        double instantSellPrice,
        double spreadCoins,
        double spreadPercent,
        long weeklyVolume) {

    MarketWatchBazaarMatch {
        watchId = clean(watchId);
        productId = clean(productId);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}