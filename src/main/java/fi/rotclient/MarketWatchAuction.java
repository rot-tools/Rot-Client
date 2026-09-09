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
        String itemBytes) {

    MarketWatchAuction {
        uuid = normalize(uuid);
        itemName = normalize(itemName);
        category = normalize(category);
        tier = normalize(tier);
        itemBytes = normalize(itemBytes);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}