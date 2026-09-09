package fi.rotclient;

record MarketWatchLiveAlert(
        Market market,
        String watchId,
        String targetId,
        String displayName,
        long auctionPriceCoins,
        double bazaarBuyPrice,
        double bazaarSellPrice,
        double spreadCoins,
        double spreadPercent,
        long weeklyVolume,
        long observedAtMillis) {

    enum Market {
        AUCTION_HOUSE,
        BAZAAR
    }

    MarketWatchLiveAlert {
        market =
                market == null
                        ? Market.AUCTION_HOUSE
                        : market;

        watchId = clean(watchId);
        targetId = clean(targetId);
        displayName = clean(displayName);
    }

    static MarketWatchLiveAlert fromAuction(
            MarketWatchAuctionMatch match,
            long observedAtMillis) {

        return new MarketWatchLiveAlert(
                Market.AUCTION_HOUSE,
                match.watchId(),
                match.auctionUuid(),
                match.itemName(),
                match.priceCoins(),
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0L,
                observedAtMillis);
    }

    static MarketWatchLiveAlert fromBazaar(
            MarketWatchBazaarMatch match,
            long observedAtMillis) {

        return new MarketWatchLiveAlert(
                Market.BAZAAR,
                match.watchId(),
                match.productId(),
                match.productId(),
                0L,
                match.instantBuyPrice(),
                match.instantSellPrice(),
                match.spreadCoins(),
                match.spreadPercent(),
                match.weeklyVolume(),
                observedAtMillis);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}