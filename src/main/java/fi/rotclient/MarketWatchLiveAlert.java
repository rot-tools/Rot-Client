package fi.rotclient;

record MarketWatchLiveAlert(
        Market market,
        String watchId,
        String targetId,
        String displayName,
        long auctionPriceCoins,
        String sellerUuid,
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

        watchId =
                clean(
                        watchId);

        targetId =
                clean(
                        targetId);

        displayName =
                clean(
                        displayName);

        sellerUuid =
                clean(
                        sellerUuid);
    }

    static MarketWatchLiveAlert fromAuction(
            MarketWatchAuctionMatch match,
            long observedAtMillis) {

        return fromAuction(
                match,
                "",
                match.referencePriceCoins(),
                observedAtMillis);
    }

    static MarketWatchLiveAlert fromAuction(
            MarketWatchAuctionMatch match,
            String sellerUuid,
            long referencePriceCoins,
            long observedAtMillis) {

        double possibleMargin =
                Math.max(
                        0L,
                        referencePriceCoins
                                - match.priceCoins());

        double possibleMarginPercent =
                referencePriceCoins <= 0L
                        ? 0.0D
                        : (possibleMargin
                        / (double) referencePriceCoins)
                        * 100.0D;

        return new MarketWatchLiveAlert(
                Market.AUCTION_HOUSE,
                match.watchId(),
                match.auctionUuid(),
                match.itemName(),
                match.priceCoins(),
                sellerUuid,
                0.0D,
                0.0D,
                possibleMargin,
                possibleMarginPercent,
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
                "",
                match.instantBuyPrice(),
                match.instantSellPrice(),
                match.spreadCoins(),
                match.spreadPercent(),
                match.weeklyVolume(),
                observedAtMillis);
    }

    private static String clean(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }
}