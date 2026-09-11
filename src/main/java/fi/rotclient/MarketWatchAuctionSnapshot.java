package fi.rotclient;

import java.util.List;

record MarketWatchAuctionSnapshot(
        long lastUpdated,
        int totalAuctions,
        List<MarketWatchAuction> auctions) {

    MarketWatchAuctionSnapshot {
        auctions = List.copyOf(
                auctions == null
                        ? List.of()
                        : auctions);
    }
}