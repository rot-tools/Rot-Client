package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

final class MarketWatchEvaluator {
    private MarketWatchEvaluator() {
    }

    static List<MarketWatchAuctionMatch> evaluateAuctions(
            List<MarketWatchAuctionWatch> watches,
            MarketWatchAuctionSnapshot snapshot) {

        if (watches == null
                || watches.isEmpty()
                || snapshot == null
                || snapshot.auctions().isEmpty()) {
            return List.of();
        }

        List<MarketWatchAuctionMatch> matches =
                new ArrayList<>();

        for (MarketWatchAuctionWatch sourceWatch : watches) {
            if (sourceWatch == null) {
                continue;
            }

            MarketWatchAuctionWatch watch =
                    sourceWatch.copy();

            if (!watch.enabled) {
                continue;
            }

            /*
             * Item IDs are not yet extracted from item_bytes.
             * Until that normalization layer exists, AH matching uses the
             * exact visible item name.
             */
            if (watch.itemName.isBlank()) {
                continue;
            }

            /*
             * Discount/profit require a trustworthy market value.
             * Do not silently ignore those conditions.
             */
            if (watch.minDiscountPercent > 0.0D
                    || watch.minProfitCoins > 0L
                    || watch.minProfitPercent > 0.0D) {
                continue;
            }

            /*
             * Without a price threshold there is currently no actionable
             * AH condition to evaluate.
             */
            if (watch.maxPriceCoins <= 0L) {
                continue;
            }

            for (MarketWatchAuction auction
                    : snapshot.auctions()) {

                if (!matchesAuction(
                        watch,
                        auction)) {
                    continue;
                }

                long price =
                        currentAuctionPrice(
                                auction);

                if (price <= 0L
                        || price > watch.maxPriceCoins) {
                    continue;
                }

                matches.add(
                        new MarketWatchAuctionMatch(
                                watch.id,
                                auction.uuid(),
                                auction.itemName(),
                                auction.tier(),
                                price,
                                auction.bin(),
                                price,
                                0.0D));
            }
        }

        return List.copyOf(matches);
    }

    static List<MarketWatchBazaarMatch> evaluateBazaar(
            List<MarketWatchBazaarWatch> watches,
            MarketWatchBazaarSnapshot snapshot) {

        if (watches == null
                || watches.isEmpty()
                || snapshot == null
                || snapshot.products().isEmpty()) {
            return List.of();
        }

        List<MarketWatchBazaarMatch> matches =
                new ArrayList<>();

        for (MarketWatchBazaarWatch sourceWatch : watches) {
            if (sourceWatch == null) {
                continue;
            }

            MarketWatchBazaarWatch watch =
                    sourceWatch.copy();

            if (!watch.enabled
                    || watch.productId.isBlank()
                    || !hasBazaarThreshold(watch)) {
                continue;
            }

            MarketWatchBazaarProduct product =
                    snapshot.product(
                            watch.productId);

            if (product == null) {
                continue;
            }

            double buy =
                    product.quickBuyPrice();

            double sell =
                    product.quickSellPrice();

            boolean validBuy =
                    Double.isFinite(buy)
                            && buy > 0.0D;

            boolean validSell =
                    Double.isFinite(sell)
                            && sell > 0.0D;

            double spread =
                    validBuy && validSell
                            ? buy - sell
                            : 0.0D;

            double spreadPercent =
                    validBuy && validSell
                            ? (spread / buy) * 100.0D
                            : 0.0D;

            /*
             * For liquidity, use the weaker side of the market rather than
             * adding both sides together.
             */
            long weeklyVolume =
                    Math.min(
                            Math.max(
                                    0L,
                                    product.buyMovingWeek()),
                            Math.max(
                                    0L,
                                    product.sellMovingWeek()));

            if (watch.maxInstantBuyPrice > 0.0D
                    && (!validBuy
                    || buy > watch.maxInstantBuyPrice)) {
                continue;
            }

            if (watch.minInstantSellPrice > 0.0D
                    && (!validSell
                    || sell < watch.minInstantSellPrice)) {
                continue;
            }

            if (watch.minSpreadCoins > 0.0D
                    && (!validBuy
                    || !validSell
                    || spread < watch.minSpreadCoins)) {
                continue;
            }

            if (watch.minSpreadPercent > 0.0D
                    && (!validBuy
                    || !validSell
                    || spreadPercent
                    < watch.minSpreadPercent)) {
                continue;
            }

            if (watch.minWeeklyVolume > 0L
                    && weeklyVolume
                    < watch.minWeeklyVolume) {
                continue;
            }

            matches.add(
                    new MarketWatchBazaarMatch(
                            watch.id,
                            product.productId(),
                            buy,
                            sell,
                            spread,
                            spreadPercent,
                            weeklyVolume));
        }

        return List.copyOf(matches);
    }

    private static boolean matchesAuction(
            MarketWatchAuctionWatch watch,
            MarketWatchAuction auction) {

        if (auction == null
                || auction.uuid().isBlank()) {
            return false;
        }

        if (watch.binOnly
                && !auction.bin()) {
            return false;
        }

        if (!watch.itemName.equalsIgnoreCase(
                auction.itemName())) {
            return false;
        }

        return watch.tier.isBlank()
                || watch.tier.equalsIgnoreCase(
                        auction.tier());
    }

    private static long currentAuctionPrice(
            MarketWatchAuction auction) {

        if (auction.bin()) {
            return auction.startingBid();
        }

        if (auction.highestBidAmount() > 0L) {
            return auction.highestBidAmount();
        }

        return auction.startingBid();
    }

    private static boolean hasBazaarThreshold(
            MarketWatchBazaarWatch watch) {

        return watch.maxInstantBuyPrice > 0.0D
                || watch.minInstantSellPrice > 0.0D
                || watch.minSpreadCoins > 0.0D
                || watch.minSpreadPercent > 0.0D;
    }
}