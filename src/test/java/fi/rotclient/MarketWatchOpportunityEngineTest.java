package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchOpportunityEngineTest {

    @Test
    void ranksClearlyUnderpricedAuctionHouseBin() {
        long now = 10_000L;

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        auctionSnapshot(
                                now,
                                1_000_000L,
                                2_000_000L,
                                2_100_000L,
                                2_200_000L,
                                2_300_000L),
                        null,
                        now,
                        25_000_000.0D);

        assertFalse(
                result.opportunities()
                        .isEmpty());

        MarketWatchOpportunity opportunity =
                result.opportunities()
                        .getFirst();

        assertEquals(
                MarketWatchOpportunity.Market.AUCTION_HOUSE,
                opportunity.market());

        assertEquals(
                "a0",
                opportunity.auctionUuid());

        assertTrue(
                opportunity.expectedProfitCoins()
                        > 100_000.0D);

        assertTrue(
                opportunity.roiPercent()
                        > 2.5D);

        assertTrue(
                opportunity.score()
                        < 100.0D);
    }

    @Test
    void expensiveAuctionIsExcludedByBudget() {
        long now = 10_000L;

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        auctionSnapshot(
                                now,
                                50_000_000L,
                                70_000_000L,
                                72_000_000L,
                                74_000_000L,
                                76_000_000L),
                        null,
                        now,
                        10_000_000.0D);

        assertTrue(
                result.opportunities()
                        .isEmpty());
    }

    @Test
    void sparseAuctionGroupIsRejected() {
        long now = 10_000L;

        MarketWatchAuctionSnapshot auctions =
                new MarketWatchAuctionSnapshot(
                        123L,
                        2,
                        List.of(
                                auction(
                                        "cheap",
                                        1_000_000L,
                                        now),
                                auction(
                                        "other",
                                        5_000_000L,
                                        now)));

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        auctions,
                        null,
                        now,
                        25_000_000.0D);

        assertTrue(
                result.opportunities()
                        .isEmpty());
    }

    @Test
    void expiredAuctionIsIgnored() {
        long now = 50_000L;

        MarketWatchAuction expired =
                new MarketWatchAuction(
                        "expired",
                        "Example Sword",
                        "weapon",
                        "LEGENDARY",
                        0L,
                        now - 1L,
                        1_000_000L,
                        0L,
                        true,
                        "",
                        seller());

        MarketWatchAuctionSnapshot auctions =
                new MarketWatchAuctionSnapshot(
                        123L,
                        5,
                        List.of(
                                expired,
                                auction(
                                        "two",
                                        2_000_000L,
                                        now),
                                auction(
                                        "three",
                                        2_100_000L,
                                        now),
                                auction(
                                        "four",
                                        2_200_000L,
                                        now),
                                auction(
                                        "five",
                                        2_300_000L,
                                        now)));

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        auctions,
                        null,
                        now,
                        25_000_000.0D);

        assertTrue(
                result.opportunities()
                        .isEmpty());
    }

    @Test
    void findsProfitableBazaarOrderFlip() {
        MarketWatchBazaarSnapshot bazaar =
                bazaarSnapshot(
                        120.0D,
                        100.0D);

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        null,
                        bazaar,
                        1_000L,
                        25_000_000.0D);

        assertEquals(
                1,
                result.opportunities()
                        .size());

        MarketWatchOpportunity opportunity =
                result.opportunities()
                        .getFirst();

        assertEquals(
                MarketWatchOpportunity.Market.BAZAAR,
                opportunity.market());

        assertTrue(
                opportunity.expectedProfitCoins()
                        > 5_000.0D);

        assertTrue(
                opportunity.score()
                        < 100.0D);

        assertTrue(
                opportunity.estimatedFeesCoins()
                        > 0.0D);

        /*
         * Several million units per week is healthy, but HIGH is now
         * reserved for exceptionally liquid Bazaar products.
         */
        assertEquals(
                MarketWatchOpportunity.Liquidity.MEDIUM,
                opportunity.liquidity());
    }

    @Test
    void bazaarPositionSizingRespectsSmallBudget() {
        MarketWatchBazaarSnapshot bazaar =
                bazaarSnapshot(
                        150.0D,
                        100.0D);

        double budget =
                100_000.0D;

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        null,
                        bazaar,
                        1_000L,
                        budget);

        assertFalse(
                result.opportunities()
                        .isEmpty());

        MarketWatchOpportunity opportunity =
                result.opportunities()
                        .getFirst();

        /*
         * Normal Bazaar position target is 25% of trading budget.
         */
        assertTrue(
                opportunity.capitalCoins()
                        <= budget * 0.25D
                        + opportunity.buyPricePerUnit());
    }

    @Test
    void tightBazaarSpreadDisappearsAfterCosts() {
        MarketWatchBazaarSnapshot resultSource =
                bazaarSnapshot(
                        101.0D,
                        100.0D);

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        null,
                        resultSource,
                        1_000L,
                        25_000_000.0D);

        assertTrue(
                result.opportunities()
                        .isEmpty());
    }

    @Test
    void noOpportunityCanReachPerfectHundred() {
        long now =
                10_000L;

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine.scan(
                        auctionSnapshot(
                                now,
                                100_000L,
                                10_000_000L,
                                11_000_000L,
                                12_000_000L,
                                13_000_000L,
                                14_000_000L,
                                15_000_000L,
                                16_000_000L,
                                17_000_000L,
                                18_000_000L,
                                19_000_000L,
                                20_000_000L),
                        bazaarSnapshot(
                                500.0D,
                                100.0D),
                        now,
                        25_000_000.0D);

        assertFalse(
                result.opportunities()
                        .isEmpty());

        assertTrue(
                result.opportunities()
                        .stream()
                        .allMatch(
                                opportunity ->
                                        opportunity.score()
                                                <= 99.9D));
    }

    @Test
    void stackableAuctionListingsUsePerItemPrice() {

        long now =
                10_000L;

        /*
         * Shiny-Orb-style regression:
         *
         * 1 @ 120k total = 120k each
         * 2 @ 250k total = 125k each
         * 2 @ 252k total = 126k each
         * 2 @ 254k total = 127k each
         *
         * This is NOT a giant discount.
         */
        List<MarketWatchAuction> auctions =
                List.of(
                        auction(
                                "single",
                                120_000L,
                                now),
                        auction(
                                "stack-a",
                                250_000L,
                                now),
                        auction(
                                "stack-b",
                                252_000L,
                                now),
                        auction(
                                "stack-c",
                                254_000L,
                                now));

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine
                        .scan(
                                new MarketWatchAuctionSnapshot(
                                        500L,
                                        auctions.size(),
                                        auctions),
                                null,
                                now,
                                5_000_000.0D,
                                auction ->
                                        auction.uuid()
                                                .equals("single")
                                                ? 1
                                                : 2);

        assertTrue(
                result.opportunities()
                        .isEmpty());
    }

    @Test
    void genuineStackDiscountPreservesWholeListingEconomics() {

        long now =
                10_000L;

        List<MarketWatchAuction> auctions =
                List.of(
                        auction(
                                "cheap",
                                1_000_000L,
                                now),
                        auction(
                                "normal-a",
                                3_000_000L,
                                now),
                        auction(
                                "normal-b",
                                3_100_000L,
                                now),
                        auction(
                                "normal-c",
                                3_200_000L,
                                now));

        MarketWatchOpportunitySnapshot result =
                MarketWatchOpportunityEngine
                        .scan(
                                new MarketWatchAuctionSnapshot(
                                        501L,
                                        auctions.size(),
                                        auctions),
                                null,
                                now,
                                10_000_000.0D,
                                ignored -> 2);

        assertFalse(
                result.opportunities()
                        .isEmpty());

        MarketWatchOpportunity opportunity =
                result.opportunities()
                        .getFirst();

        assertEquals(
                2L,
                opportunity.quantity());

        /*
         * Per-unit buy is 500k, but the listing really costs 1M.
         */
        assertEquals(
                500_000.0D,
                opportunity.buyPricePerUnit(),
                0.01D);

        assertEquals(
                1_000_000.0D,
                opportunity.capitalCoins(),
                0.01D);

        assertTrue(
                opportunity.grossReturnCoins()
                        > opportunity.capitalCoins());

        assertTrue(
                opportunity.expectedProfitCoins()
                        > 100_000.0D);
    }

    private static MarketWatchAuctionSnapshot auctionSnapshot(
            long now,
            long... prices) {

        java.util.ArrayList<MarketWatchAuction> auctions =
                new java.util.ArrayList<>();

        for (int i = 0;
                i < prices.length;
                i++) {

            auctions.add(
                    auction(
                            "a" + i,
                            prices[i],
                            now));
        }

        return new MarketWatchAuctionSnapshot(
                123L,
                auctions.size(),
                auctions);
    }

    private static MarketWatchBazaarSnapshot bazaarSnapshot(
            double sellOffer,
            double buyOrder) {

        MarketWatchBazaarProduct product =
                new MarketWatchBazaarProduct(
                        "ENCHANTED_DIAMOND",
                        sellOffer,
                        buyOrder,
                        2_000_000L,
                        2_000_000L,
                        4_000_000L,
                        3_000_000L,
                        List.of(
                                new MarketWatchBazaarProduct.OrderLevel(
                                        500_000L,
                                        sellOffer),
                                new MarketWatchBazaarProduct.OrderLevel(
                                        500_000L,
                                        sellOffer + 1.0D),
                                new MarketWatchBazaarProduct.OrderLevel(
                                        500_000L,
                                        sellOffer + 2.0D)),
                        List.of(
                                new MarketWatchBazaarProduct.OrderLevel(
                                        500_000L,
                                        buyOrder),
                                new MarketWatchBazaarProduct.OrderLevel(
                                        500_000L,
                                        Math.max(
                                                0.01D,
                                                buyOrder - 1.0D)),
                                new MarketWatchBazaarProduct.OrderLevel(
                                        500_000L,
                                        Math.max(
                                                0.01D,
                                                buyOrder - 2.0D))));

        return new MarketWatchBazaarSnapshot(
                456L,
                Map.of(
                        "ENCHANTED_DIAMOND",
                        product));
    }

    private static MarketWatchAuction auction(
            String uuid,
            long price,
            long now) {

        return new MarketWatchAuction(
                uuid,
                "Example Sword",
                "weapon",
                "LEGENDARY",
                0L,
                now + 60_000L,
                price,
                0L,
                true,
                "",
                seller());
    }

    private static String seller() {
        return "0123456789abcdef0123456789abcdef";
    }
}