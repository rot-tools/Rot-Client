package fi.rotclient;

import java.util.List;

record MarketWatchAuctionPage(
        int page,
        int totalPages,
        int totalAuctions,
        long lastUpdated,
        List<MarketWatchAuction> auctions) {

    MarketWatchAuctionPage {
        auctions = List.copyOf(auctions == null ? List.of() : auctions);
    }
}