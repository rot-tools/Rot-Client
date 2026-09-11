package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MarketWatchAuctionSnapshotAssembler {
    private MarketWatchAuctionSnapshotAssembler() {
    }

    static MarketWatchAuctionSnapshot assemble(
            List<MarketWatchAuctionPage> pages) {

        if (pages == null || pages.isEmpty()) {
            return null;
        }

        MarketWatchAuctionPage first = pages.getFirst();

        if (first == null
                || first.totalPages() <= 0
                || first.lastUpdated() < 0L) {
            return null;
        }

        int expectedPages = first.totalPages();

        if (pages.size() != expectedPages) {
            return null;
        }

        MarketWatchAuctionPage[] ordered =
                new MarketWatchAuctionPage[expectedPages];

        for (MarketWatchAuctionPage page : pages) {
            if (page == null
                    || page.lastUpdated() != first.lastUpdated()
                    || page.totalPages() != expectedPages
                    || page.totalAuctions() != first.totalAuctions()
                    || page.page() < 0
                    || page.page() >= expectedPages
                    || ordered[page.page()] != null) {
                return null;
            }

            ordered[page.page()] = page;
        }

        List<MarketWatchAuction> auctions =
                new ArrayList<>();

        Set<String> seenUuids =
                new HashSet<>();

        for (MarketWatchAuctionPage page : ordered) {
            if (page == null) {
                return null;
            }

            for (MarketWatchAuction auction : page.auctions()) {
                if (auction == null
                        || auction.uuid().isBlank()
                        || !seenUuids.add(auction.uuid())) {
                    continue;
                }

                auctions.add(auction);
            }
        }

        return new MarketWatchAuctionSnapshot(
                first.lastUpdated(),
                first.totalAuctions(),
                auctions);
    }
}