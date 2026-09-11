package fi.rotclient;

import java.util.List;

record MarketWatchOpportunitySnapshot(
        long auctionLastUpdated,
        long bazaarLastUpdated,
        long generatedAtMillis,
        int scannedAuctions,
        int scannedBazaarProducts,
        double budgetCoins,
        List<MarketWatchOpportunity> opportunities) {

    MarketWatchOpportunitySnapshot {
        scannedAuctions = Math.max(
                0,
                scannedAuctions);

        scannedBazaarProducts = Math.max(
                0,
                scannedBazaarProducts);

        budgetCoins = Double.isFinite(budgetCoins)
                && budgetCoins > 0.0D
                ? budgetCoins
                : 0.0D;

        opportunities = List.copyOf(
                opportunities == null
                        ? List.of()
                        : opportunities);
    }

    static MarketWatchOpportunitySnapshot empty() {
        return new MarketWatchOpportunitySnapshot(
                -1L,
                -1L,
                -1L,
                0,
                0,
                0.0D,
                List.of());
    }

    boolean hasMarketData() {
        return scannedAuctions > 0
                || scannedBazaarProducts > 0;
    }

    int totalScanned() {
        return scannedAuctions
                + scannedBazaarProducts;
    }
}