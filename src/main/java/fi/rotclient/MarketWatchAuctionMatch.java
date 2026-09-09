package fi.rotclient;

record MarketWatchAuctionMatch(
        String watchId,
        String auctionUuid,
        String itemName,
        String tier,
        long priceCoins,
        boolean bin) {

    MarketWatchAuctionMatch {
        watchId = clean(watchId);
        auctionUuid = clean(auctionUuid);
        itemName = clean(itemName);
        tier = clean(tier);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}