package fi.rotclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;

final class MarketWatchOpportunityEngine {

    private static final double DEFAULT_BUDGET =
            25_000_000.0D;

    private static final double MIN_AH_DISCOUNT_PERCENT =
            5.0D;

    private static final double MIN_AH_ROI_PERCENT =
            2.5D;

    private static final double MIN_AH_PROFIT_COINS =
            100_000.0D;

    private static final double AH_SAFETY_BUFFER_RATE =
            0.005D;

    private static final double BAZAAR_ENTRY_BUFFER_RATE =
            0.001D;

    private static final double BAZAAR_EXIT_BUFFER_RATE =
            0.001D;

    private static final double BAZAAR_TAX_RATE =
            0.0125D;

    /*
     * A Bazaar flip should not consume the player's entire purse.
     * V2 models at most 25% of the configured trading budget.
     */
    private static final double BAZAAR_BUDGET_FRACTION =
            0.25D;

    private static final double BAZAAR_MAX_POSITION =
            25_000_000.0D;

    private static final double MIN_BAZAAR_ROI_PERCENT =
            1.0D;

    private static final double MIN_BAZAAR_PROFIT_COINS =
            5_000.0D;

    private static final long MIN_BAZAAR_WEEKLY_VOLUME =
            1_000L;

    private static final int MAX_OPPORTUNITIES =
            250;

    private MarketWatchOpportunityEngine() {
    }

    static MarketWatchOpportunitySnapshot scan(
            MarketWatchAuctionSnapshot auctions,
            MarketWatchBazaarSnapshot bazaar,
            long nowMillis) {

        return scan(
                auctions,
                bazaar,
                nowMillis,
                DEFAULT_BUDGET);
    }

    static MarketWatchOpportunitySnapshot scan(
            MarketWatchAuctionSnapshot auctions,
            MarketWatchBazaarSnapshot bazaar,
            long nowMillis,
            double requestedBudgetCoins) {

        return scan(
                auctions,
                bazaar,
                nowMillis,
                requestedBudgetCoins,
                ignored -> 1);
    }

    static MarketWatchOpportunitySnapshot scan(
            MarketWatchAuctionSnapshot auctions,
            MarketWatchBazaarSnapshot bazaar,
            long nowMillis,
            double requestedBudgetCoins,
            ToIntFunction<MarketWatchAuction> auctionQuantityResolver) {

        long now =
                Math.max(
                        0L,
                        nowMillis);

        double budget =
                normalizeBudget(
                        requestedBudgetCoins);

        List<MarketWatchOpportunity> result =
                new ArrayList<>();

        int scannedAuctions =
                auctions == null
                        ? 0
                        : auctions.auctions().size();

        int scannedBazaar =
                bazaar == null
                        ? 0
                        : bazaar.products().size();

        scanAuctions(
                auctions,
                now,
                budget,
                auctionQuantityResolver,
                result);

        scanBazaar(
                bazaar,
                now,
                budget,
                result);

        result.sort(
                (left, right) -> {
                    int byScore =
                            Double.compare(
                                    right.score(),
                                    left.score());

                    if (byScore != 0) {
                        return byScore;
                    }

                    int byRoi =
                            Double.compare(
                                    right.roiPercent(),
                                    left.roiPercent());

                    if (byRoi != 0) {
                        return byRoi;
                    }

                    int byProfit =
                            Double.compare(
                                    right.expectedProfitCoins(),
                                    left.expectedProfitCoins());

                    if (byProfit != 0) {
                        return byProfit;
                    }

                    return left.itemName()
                            .compareToIgnoreCase(
                                    right.itemName());
                });

        if (result.size()
                > MAX_OPPORTUNITIES) {

            result =
                    new ArrayList<>(
                            result.subList(
                                    0,
                                    MAX_OPPORTUNITIES));
        }

        return new MarketWatchOpportunitySnapshot(
                auctions == null
                        ? -1L
                        : auctions.lastUpdated(),
                bazaar == null
                        ? -1L
                        : bazaar.lastUpdated(),
                now,
                scannedAuctions,
                scannedBazaar,
                budget,
                result);
    }

    private static void scanAuctions(
            MarketWatchAuctionSnapshot snapshot,
            long nowMillis,
            double budget,
            ToIntFunction<MarketWatchAuction> quantityResolver,
            List<MarketWatchOpportunity> result) {

        if (snapshot == null
                || snapshot.auctions().isEmpty()) {

            return;
        }

        /*
         * starting_bid is the total price of the entire auction stack.
         *
         * Example:
         *
         *   1 x Shiny Orb for 120k = 120k each
         *   2 x Shiny Orb for 250k = 125k each
         *
         * Those prices are almost identical and must NOT look like
         * 120k versus 250k.
         */
        record AuctionListing(
                MarketWatchAuction auction,
                int quantity,
                double unitPrice) {
        }

        Map<String, List<AuctionListing>> groups =
                new HashMap<>();

        for (MarketWatchAuction auction
                : snapshot.auctions()) {

            if (!usableBin(
                    auction,
                    nowMillis)) {

                continue;
            }

            long totalPrice =
                    auctionPrice(
                            auction);

            if (totalPrice <= 0L) {
                continue;
            }

            int quantity =
                    safeAuctionQuantity(
                            quantityResolver,
                            auction);

            double unitPrice =
                    totalPrice
                            / (double) quantity;

            if (!Double.isFinite(
                    unitPrice)
                    || unitPrice <= 0.0D) {

                continue;
            }

            groups.computeIfAbsent(
                            comparableKey(
                                    auction),
                            ignored ->
                                    new ArrayList<>())
                    .add(
                            new AuctionListing(
                                    auction,
                                    quantity,
                                    unitPrice));
        }

        for (List<AuctionListing> group
                : groups.values()) {

            if (group == null
                    || group.size() < 4) {

                continue;
            }

            /*
             * Candidate selection and comparison are both per item.
             */
            group.sort(
                    (left, right) ->
                            Double.compare(
                                    left.unitPrice(),
                                    right.unitPrice()));

            AuctionListing candidateListing =
                    group.getFirst();

            MarketWatchAuction candidate =
                    candidateListing.auction();

            int candidateQuantity =
                    candidateListing.quantity();

            long purchaseTotal =
                    auctionPrice(
                            candidate);

            if (purchaseTotal <= 0L
                    || purchaseTotal > budget) {

                continue;
            }

            long purchaseUnit =
                    Math.max(
                            1L,
                            Math.round(
                                    candidateListing
                                            .unitPrice()));

            List<Long> comparisonUnitPrices =
                    new ArrayList<>(
                            group.size() - 1);

            for (int i = 1;
                    i < group.size();
                    i++) {

                long unit =
                        Math.max(
                                1L,
                                Math.round(
                                        group.get(i)
                                                .unitPrice()));

                comparisonUnitPrices.add(
                        unit);
            }

            MarketWatchDealPolicy.Analysis analysis =
                    MarketWatchDealPolicy
                            .analyzeAuction(
                                    purchaseUnit,
                                    comparisonUnitPrices,
                                    MIN_AH_DISCOUNT_PERCENT);

            if (!analysis.usable()
                    || !analysis.trigger()
                    || comparisonUnitPrices.isEmpty()) {

                continue;
            }

            long nextUnitPrice =
                    comparisonUnitPrices
                            .getFirst();

            double undercutPerUnit =
                    Math.max(
                            1.0D,
                            nextUnitPrice
                                    * 0.001D);

            double estimatedSalePerUnit =
                    Math.min(
                            analysis.referencePrice(),
                            nextUnitPrice
                                    - undercutPerUnit);

            if (!(estimatedSalePerUnit
                    > candidateListing.unitPrice())) {

                continue;
            }

            /*
             * Profit, fees and ROI are based on the complete stack the
             * player actually purchases and resells.
             */
            double estimatedSaleTotal =
                    estimatedSalePerUnit
                            * candidateQuantity;

            if (!(estimatedSaleTotal
                    > purchaseTotal)) {

                continue;
            }

            double listingFee =
                    estimatedSaleTotal
                            * auctionListingRate(
                                    estimatedSaleTotal);

            double claimTax =
                    auctionClaimTax(
                            estimatedSaleTotal);

            double estimatedFees =
                    listingFee
                            + claimTax;

            double safetyBuffer =
                    estimatedSaleTotal
                            * AH_SAFETY_BUFFER_RATE;

            double profit =
                    estimatedSaleTotal
                            - estimatedFees
                            - safetyBuffer
                            - purchaseTotal;

            if (!(profit
                    >= MIN_AH_PROFIT_COINS)) {

                continue;
            }

            double roi =
                    (profit
                            / purchaseTotal)
                            * 100.0D;

            if (!(roi
                    >= MIN_AH_ROI_PERCENT)) {

                continue;
            }

            MarketWatchOpportunity.Confidence confidence =
                    confidence(
                            analysis.confidence());

            MarketWatchOpportunity.Liquidity liquidity =
                    auctionLiquidity(
                            group.size());

            double score =
                    opportunityScore(
                            profit,
                            roi,
                            analysis.actualDiscountPercent(),
                            confidence,
                            liquidity);

            result.add(
                    new MarketWatchOpportunity(
                            MarketWatchOpportunity
                                    .Market.AUCTION_HOUSE,
                            "AH:"
                                    + candidate.uuid(),
                            "",
                            candidate.itemName(),
                            candidate.category(),
                            candidate.tier(),
                            candidate.uuid(),
                            candidate.auctioneerUuid(),

                            /*
                             * MarketWatchOpportunity stores per-unit
                             * prices plus quantity. capitalCoins() and
                             * grossReturnCoins() then produce stack totals.
                             */
                            candidateListing.unitPrice(),
                            estimatedSalePerUnit,
                            candidateQuantity,

                            profit,
                            roi,
                            score,
                            analysis.actualDiscountPercent(),
                            estimatedFees,
                            safetyBuffer,
                            confidence,
                            liquidity,
                            analysis.comparisonCount(),
                            0L,
                            nowMillis));
        }
    }

    private static int safeAuctionQuantity(
            ToIntFunction<MarketWatchAuction> resolver,
            MarketWatchAuction auction) {

        if (resolver == null
                || auction == null) {

            return 1;
        }

        try {
            return Math.max(
                    1,
                    Math.min(
                            64,
                            resolver.applyAsInt(
                                    auction)));

        } catch (RuntimeException ignored) {
            return 1;
        }
    }
    private static void scanBazaar(
            MarketWatchBazaarSnapshot snapshot,
            long nowMillis,
            double budget,
            List<MarketWatchOpportunity> result) {

        if (snapshot == null
                || snapshot.products().isEmpty()) {

            return;
        }

        double targetCapital =
                Math.min(
                        BAZAAR_MAX_POSITION,
                        budget
                                * BAZAAR_BUDGET_FRACTION);

        for (MarketWatchBazaarProduct product
                : snapshot.products().values()) {

            if (product == null
                    || product.productId().isBlank()) {

                continue;
            }

            double currentSellOffer =
                    product.quickBuyPrice();

            double currentBuyOrder =
                    product.quickSellPrice();

            if (!positiveFinite(
                    currentSellOffer)
                    || !positiveFinite(
                    currentBuyOrder)
                    || currentSellOffer
                    <= currentBuyOrder) {

                continue;
            }

            long weeklyVolume =
                    Math.min(
                            Math.max(
                                    0L,
                                    product.buyMovingWeek()),
                            Math.max(
                                    0L,
                                    product.sellMovingWeek()));

            if (weeklyVolume
                    < MIN_BAZAAR_WEEKLY_VOLUME) {

                continue;
            }

            double entry =
                    currentBuyOrder
                            * (1.0D
                            + BAZAAR_ENTRY_BUFFER_RATE);

            double exit =
                    currentSellOffer
                            * (1.0D
                            - BAZAAR_EXIT_BUFFER_RATE);

            if (entry > budget) {
                continue;
            }

            double netExitPerUnit =
                    exit
                            * (1.0D
                            - BAZAAR_TAX_RATE);

            double profitPerUnit =
                    netExitPerUnit
                            - entry;

            if (!(profitPerUnit > 0.0D)) {
                continue;
            }

            double roi =
                    (profitPerUnit
                            / entry)
                            * 100.0D;

            if (!(roi
                    >= MIN_BAZAAR_ROI_PERCENT)) {

                continue;
            }

            long budgetQuantity =
                    (long) Math.floor(
                            targetCapital
                                    / entry);

            if (budgetQuantity < 1L) {
                budgetQuantity = 1L;
            }

            long depth =
                    Math.min(
                            topDepth(
                                    product.buyOrders()),
                            topDepth(
                                    product.sellOrders()));

            long depthQuantity =
                    depth > 0L
                            ? Math.max(
                                    1L,
                                    depth / 10L)
                            : budgetQuantity;

            long quantity =
                    Math.max(
                            1L,
                            Math.min(
                                    budgetQuantity,
                                    depthQuantity));

            double capital =
                    entry
                            * quantity;

            if (capital > budget) {
                continue;
            }

            double expectedFees =
                    exit
                            * quantity
                            * BAZAAR_TAX_RATE;

            double entryBuffer =
                    currentBuyOrder
                            * BAZAAR_ENTRY_BUFFER_RATE
                            * quantity;

            double exitBuffer =
                    currentSellOffer
                            * BAZAAR_EXIT_BUFFER_RATE
                            * quantity;

            double modeledPriceBuffer =
                    entryBuffer
                            + exitBuffer;

            double expectedProfit =
                    profitPerUnit
                            * quantity;

            if (!(expectedProfit
                    >= MIN_BAZAAR_PROFIT_COINS)) {

                continue;
            }

            int orderLevels =
                    Math.min(
                            product.buyOrders().size(),
                            product.sellOrders().size());

            MarketWatchOpportunity.Confidence confidence =
                    bazaarConfidence(
                            weeklyVolume,
                            depth,
                            orderLevels);

            MarketWatchOpportunity.Liquidity liquidity =
                    bazaarLiquidity(
                            weeklyVolume,
                            depth,
                            orderLevels);

            double rawSpreadPercent =
                    ((currentSellOffer
                            - currentBuyOrder)
                            / currentSellOffer)
                            * 100.0D;

            double score =
                    opportunityScore(
                            expectedProfit,
                            roi,
                            rawSpreadPercent,
                            confidence,
                            liquidity);

            result.add(
                    new MarketWatchOpportunity(
                            MarketWatchOpportunity.Market.BAZAAR,
                            "BAZAAR:"
                                    + product.productId(),
                            product.productId(),
                            prettyProductId(
                                    product.productId()),
                            "BAZAAR",
                            "",
                            "",
                            "",
                            entry,
                            exit,
                            quantity,
                            expectedProfit,
                            roi,
                            score,
                            rawSpreadPercent,
                            expectedFees,
                            modeledPriceBuffer,
                            confidence,
                            liquidity,
                            orderLevels,
                            weeklyVolume,
                            nowMillis));
        }
    }

    /*
     * V2 score:
     *
     * ROI                42 points, smooth saturation
     * Market edge        25 points, smooth saturation
     * Confidence         15 points
     * Liquidity          13 points
     * Absolute profit     5 points, smooth saturation
     *
     * This prevents expensive items from winning simply because their
     * absolute coin profit is large, and prevents large groups of 100s.
     */
    private static double opportunityScore(
            double profit,
            double roiPercent,
            double edgePercent,
            MarketWatchOpportunity.Confidence confidence,
            MarketWatchOpportunity.Liquidity liquidity) {

        double roiComponent =
                42.0D
                        * saturation(
                                roiPercent,
                                12.0D);

        double edgeComponent =
                25.0D
                        * saturation(
                                edgePercent,
                                15.0D);

        double confidenceComponent =
                switch (confidence) {
                    case HIGH -> 15.0D;
                    case MEDIUM -> 10.0D;
                    case LOW -> 4.0D;
                };

        double liquidityComponent =
                switch (liquidity) {
                    case HIGH -> 13.0D;
                    case MEDIUM -> 8.0D;
                    case LOW -> 3.0D;
                };

        double profitComponent =
                5.0D
                        * saturation(
                                profit,
                                1_000_000.0D);

        double score =
                roiComponent
                        + edgeComponent
                        + confidenceComponent
                        + liquidityComponent
                        + profitComponent;

        return Math.max(
                0.0D,
                Math.min(
                        99.9D,
                        score));
    }

    private static double saturation(
            double value,
            double scale) {

        if (!Double.isFinite(value)
                || value <= 0.0D
                || !Double.isFinite(scale)
                || scale <= 0.0D) {

            return 0.0D;
        }

        return 1.0D
                - Math.exp(
                        -value / scale);
    }

    private static boolean usableBin(
            MarketWatchAuction auction,
            long nowMillis) {

        return auction != null
                && auction.bin()
                && !auction.uuid().isBlank()
                && !auction.itemName().isBlank()
                && auction.startingBid() > 0L
                && (auction.endMillis() <= 0L
                || nowMillis <= 0L
                || auction.endMillis()
                > nowMillis);
    }

    private static long auctionPrice(
            MarketWatchAuction auction) {

        return auction == null
                || !auction.bin()
                ? 0L
                : Math.max(
                        0L,
                        auction.startingBid());
    }

    private static String comparableKey(
            MarketWatchAuction auction) {

        if (auction == null) {
            return "";
        }

        return normalizeComparable(
                auction.itemName())
                + '\u0000'
                + normalizeComparable(
                auction.tier())
                + '\u0000'
                + normalizeComparable(
                auction.category());
    }

    private static String normalizeComparable(
            String raw) {

        if (raw == null
                || raw.isBlank()) {

            return "";
        }

        String source =
                raw.trim();

        StringBuilder result =
                new StringBuilder(
                        source.length());

        boolean skipFormattingCode =
                false;

        boolean lastWasSpace =
                false;

        for (int i = 0;
                i < source.length();
                i++) {

            char c =
                    source.charAt(i);

            if (skipFormattingCode) {
                skipFormattingCode = false;
                continue;
            }

            if (c == '\u00a7') {
                skipFormattingCode = true;
                continue;
            }

            if (Character.isWhitespace(c)) {
                if (!lastWasSpace
                        && result.length() > 0) {

                    result.append(' ');
                }

                lastWasSpace = true;
                continue;
            }

            result.append(
                    Character.toLowerCase(
                            c));

            lastWasSpace = false;
        }

        return result.toString()
                .trim();
    }

    private static double auctionListingRate(
            double salePrice) {

        if (salePrice
                < 10_000_000.0D) {

            return 0.01D;
        }

        if (salePrice
                < 100_000_000.0D) {

            return 0.02D;
        }

        return 0.025D;
    }

    private static double auctionClaimTax(
            double salePrice) {

        if (!(salePrice
                > 1_000_000.0D)) {

            return 0.0D;
        }

        return Math.min(
                salePrice * 0.01D,
                Math.max(
                        0.0D,
                        salePrice
                                - 1_000_000.0D));
    }

    private static long topDepth(
            List<MarketWatchBazaarProduct.OrderLevel> levels) {

        if (levels == null
                || levels.isEmpty()) {

            return 0L;
        }

        long total =
                0L;

        int count =
                Math.min(
                        3,
                        levels.size());

        for (int i = 0;
                i < count;
                i++) {

            MarketWatchBazaarProduct.OrderLevel level =
                    levels.get(i);

            if (level == null
                    || level.amount() <= 0L) {

                continue;
            }

            if (Long.MAX_VALUE - total
                    < level.amount()) {

                return Long.MAX_VALUE;
            }

            total +=
                    level.amount();
        }

        return total;
    }

    private static MarketWatchOpportunity.Confidence confidence(
            MarketWatchDealPolicy.Confidence confidence) {

        if (confidence
                == MarketWatchDealPolicy.Confidence.HIGH) {

            return MarketWatchOpportunity
                    .Confidence.HIGH;
        }

        if (confidence
                == MarketWatchDealPolicy.Confidence.MEDIUM) {

            return MarketWatchOpportunity
                    .Confidence.MEDIUM;
        }

        return MarketWatchOpportunity
                .Confidence.LOW;
    }

    private static MarketWatchOpportunity.Liquidity auctionLiquidity(
            int listings) {

        /*
         * Active listings are only a rough liquidity signal.
         *
         * HIGH is intentionally difficult to reach: an AH market needs
         * substantial depth before we describe it as highly liquid.
         */
        if (listings >= 25) {
            return MarketWatchOpportunity
                    .Liquidity.HIGH;
        }

        if (listings >= 8) {
            return MarketWatchOpportunity
                    .Liquidity.MEDIUM;
        }

        return MarketWatchOpportunity
                .Liquidity.LOW;
    }

    private static MarketWatchOpportunity.Confidence bazaarConfidence(
            long weeklyVolume,
            long depth,
            int orderLevels) {

        if (weeklyVolume >= 1_000_000L
                && depth >= 10_000L
                && orderLevels >= 3) {

            return MarketWatchOpportunity
                    .Confidence.HIGH;
        }

        if (weeklyVolume >= 100_000L
                && depth >= 1_000L
                && orderLevels >= 2) {

            return MarketWatchOpportunity
                    .Confidence.MEDIUM;
        }

        return MarketWatchOpportunity
                .Confidence.LOW;
    }

    private static MarketWatchOpportunity.Liquidity bazaarLiquidity(
            long weeklyVolume,
            long topOrderDepth,
            int orderLevels) {

        /*
         * V4 liquidity:
         *
         * Weekly volume alone is not enough to call a product HIGH.
         * The current order book must also have meaningful depth and
         * multiple price levels.
         *
         * This deliberately makes HIGH a relatively rare label.
         */
        if (weeklyVolume >= 10_000_000L
                && topOrderDepth >= 50_000L
                && orderLevels >= 3) {

            return MarketWatchOpportunity
                    .Liquidity.HIGH;
        }

        if (weeklyVolume >= 500_000L
                && topOrderDepth >= 5_000L
                && orderLevels >= 2) {

            return MarketWatchOpportunity
                    .Liquidity.MEDIUM;
        }

        return MarketWatchOpportunity
                .Liquidity.LOW;
    }

    private static double normalizeBudget(
            double value) {

        if (!Double.isFinite(value)
                || value <= 0.0D) {

            return DEFAULT_BUDGET;
        }

        return Math.min(
                100_000_000_000.0D,
                value);
    }

    private static boolean positiveFinite(
            double value) {

        return Double.isFinite(value)
                && value > 0.0D;
    }

    private static String prettyProductId(
            String productId) {

        if (productId == null
                || productId.isBlank()) {

            return "Unknown Bazaar Item";
        }

        String source =
                productId
                        .replace(
                                '_',
                                ' ')
                        .trim()
                        .toLowerCase(
                                Locale.ROOT);

        StringBuilder result =
                new StringBuilder(
                        source.length());

        boolean capitalize =
                true;

        for (int i = 0;
                i < source.length();
                i++) {

            char c =
                    source.charAt(i);

            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalize = true;
                continue;
            }

            result.append(
                    capitalize
                            ? Character.toUpperCase(c)
                            : c);

            capitalize = false;
        }

        return result.toString();
    }
}