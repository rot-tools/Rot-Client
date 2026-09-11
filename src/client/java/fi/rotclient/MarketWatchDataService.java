package fi.rotclient;

final class MarketWatchDataService {
    record BazaarState(
            MarketWatchBazaarSnapshot snapshot,
            long observedAtMillis) {

        boolean available() {
            return snapshot != null
                    && observedAtMillis >= 0L
                    && !snapshot.products().isEmpty();
        }
    }

    record AuctionState(
            MarketWatchAuctionSnapshot snapshot,
            long observedAtMillis) {

        boolean available() {
            return snapshot != null
                    && observedAtMillis >= 0L
                    && !snapshot.auctions().isEmpty();
        }
    }

    private static volatile BazaarState bazaar =
            new BazaarState(
                    new MarketWatchBazaarSnapshot(
                            -1L,
                            java.util.Map.of()),
                    -1L);

    private static volatile AuctionState auctions =
            new AuctionState(
                    new MarketWatchAuctionSnapshot(
                            -1L,
                            0,
                            java.util.List.of()),
                    -1L);

    private MarketWatchDataService() {
    }

    static void publishBazaar(
            MarketWatchBazaarSnapshot snapshot,
            long observedAtMillis) {

        if (snapshot == null
                || observedAtMillis < 0L
                || snapshot.products().isEmpty()) {
            return;
        }

        bazaar = new BazaarState(
                snapshot,
                observedAtMillis);
    }

    static BazaarState currentBazaar() {
        return bazaar;
    }

    static void publishAuctions(
            MarketWatchAuctionSnapshot snapshot,
            long observedAtMillis) {

        if (snapshot == null
                || observedAtMillis < 0L) {
            return;
        }

        auctions = new AuctionState(
                snapshot,
                observedAtMillis);
    }

    static AuctionState currentAuctions() {
        return auctions;
    }
}