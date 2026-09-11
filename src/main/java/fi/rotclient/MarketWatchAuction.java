package fi.rotclient;

record MarketWatchAuction(
        String uuid,
        String itemName,
        String category,
        String tier,
        long startMillis,
        long endMillis,
        long startingBid,
        long highestBidAmount,
        boolean bin,
        String itemBytes,
        String auctioneerUuid) {

    MarketWatchAuction(
            String uuid,
            String itemName,
            String category,
            String tier,
            long startMillis,
            long endMillis,
            long startingBid,
            long highestBidAmount,
            boolean bin,
            String itemBytes) {

        this(
                uuid,
                itemName,
                category,
                tier,
                startMillis,
                endMillis,
                startingBid,
                highestBidAmount,
                bin,
                itemBytes,
                "");
    }

    MarketWatchAuction {
        uuid =
                normalize(
                        uuid);

        itemName =
                normalize(
                        itemName);

        category =
                normalize(
                        category);

        tier =
                normalize(
                        tier);

        itemBytes =
                normalize(
                        itemBytes);

        auctioneerUuid =
                normalize(
                        auctioneerUuid);
    }

    private static String normalize(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }
}