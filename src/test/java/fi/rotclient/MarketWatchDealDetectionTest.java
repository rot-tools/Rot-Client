package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarketWatchDealDetectionTest {
    @Test
    void existingLowest50kAlertsBut75kNeverDoesWhile50kExists() {
        MarketWatchAuctionWatch watch =
                dynamicWatch(
                        20.0D);

        watch.cooldownSeconds =
                60L;

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        List<MarketWatchAuction> auctions =
                marketAround110k();

        List<MarketWatchAuctionMatch> first =
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                1L,
                                auctions),
                        1_000L);

        assertEquals(
                1,
                first.size());

        assertEquals(
                "existing-50k",
                first.getFirst()
                        .auctionUuid());

        assertEquals(
                50_000L,
                first.getFirst()
                        .priceCoins());

        /*
         * Same market again:
         *
         * 50k is already seen and 75k must NOT become a fallback candidate.
         * Only the current lowest BIN is evaluated.
         */
        assertTrue(
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                2L,
                                auctions),
                        2_000L)
                        .isEmpty());
    }


    @Test
    void new26kLowestAlertsImmediatelyThroughCooldown() {
        MarketWatchAuctionWatch watch =
                dynamicWatch(
                        20.0D);

        watch.cooldownSeconds =
                60L;

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        List<MarketWatchAuctionMatch> first =
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                10L,
                                marketAround110k()),
                        100_000L);

        assertEquals(
                1,
                first.size());

        assertEquals(
                "existing-50k",
                first.getFirst()
                        .auctionUuid());

        List<MarketWatchAuction> improved =
                new ArrayList<>();

        improved.add(
                auction(
                        "friend-26k",
                        26_000L));

        improved.addAll(
                marketAround110k());

        /*
         * Only ten seconds later: normal cooldown is still active.
         *
         * Since 26k is a new UUID and strictly cheaper than the previously
         * alerted 50k opportunity, V4 must allow the better deal through.
         */
        List<MarketWatchAuctionMatch> second =
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                11L,
                                improved),
                        110_000L);

        assertEquals(
                1,
                second.size());

        assertEquals(
                "friend-26k",
                second.getFirst()
                        .auctionUuid());

        assertEquals(
                26_000L,
                second.getFirst()
                        .priceCoins());
    }


    @Test
    void nineteenPointFiveMillionDoesNotTriggerAtTwentyPercent() {
        MarketWatchAuctionWatch watch =
                dynamicWatch(
                        20.0D);

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        List<MarketWatchAuction> auctions =
                new ArrayList<>();

        auctions.add(
                auction(
                        "19-5m",
                        19_500_000L));

        for (int i = 0;
                i < 12;
                i++) {

            auctions.add(
                    auction(
                            "20m-" + i,
                            20_000_000L));
        }

        assertTrue(
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                20L,
                                auctions),
                        200_000L)
                        .isEmpty());
    }


    @Test
    void sixteenMillionTriggersAtTwentyPercentAgainstTwentyMillionMarket() {
        MarketWatchAuctionWatch watch =
                dynamicWatch(
                        20.0D);

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        List<MarketWatchAuction> auctions =
                new ArrayList<>();

        auctions.add(
                auction(
                        "16m",
                        16_000_000L));

        for (int i = 0;
                i < 12;
                i++) {

            auctions.add(
                    auction(
                            "20m-" + i,
                            20_000_000L));
        }

        List<MarketWatchAuctionMatch> matches =
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                21L,
                                auctions),
                        210_000L);

        assertEquals(
                1,
                matches.size());

        assertEquals(
                "16m",
                matches.getFirst()
                        .auctionUuid());
    }


    @Test
    void sameDynamicAuctionUuidNeverAlertsTwice() {
        MarketWatchAuctionWatch watch =
                dynamicWatch(
                        20.0D);

        watch.cooldownSeconds =
                0L;

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        List<MarketWatchAuction> auctions =
                marketAround110k();

        assertEquals(
                1,
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                30L,
                                auctions),
                        300_000L)
                        .size());

        assertTrue(
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                31L,
                                auctions),
                        301_000L)
                        .isEmpty());
    }


    @Test
    void fixedAuctionWatchStillWorks() {
        MarketWatchAuctionWatch watch =
                new MarketWatchAuctionWatch();

        watch.itemName =
                "Shadow Fury";

        watch.tier =
                "LEGENDARY";

        watch.maxPriceCoins =
                90_000_000L;

        watch.minDiscountPercent =
                0.0D;

        watch.cooldownSeconds =
                0L;

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        List<MarketWatchAuctionMatch> matches =
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot(
                                40L,
                                List.of(
                                        auction(
                                                "fixed",
                                                85_000_000L))),
                        400_000L);

        assertEquals(
                1,
                matches.size());
    }


    @Test
    void bazaarDynamicBehaviorRemainsUnchanged() {
        MarketWatchBazaarWatch watch =
                new MarketWatchBazaarWatch();

        watch.productId =
                "ENCHANTED_DIAMOND";

        watch.maxInstantBuyPrice =
                0.0D;

        watch.buyDealPercent =
                10.0D;

        watch.cooldownSeconds =
                0L;

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        for (int i = 1;
                i <= 5;
                i++) {

            assertTrue(
                    engine.evaluateBazaar(
                            List.of(watch),
                            bazaar(
                                    i,
                                    100.0D),
                            500_000L + i)
                            .isEmpty());
        }

        List<MarketWatchBazaarMatch> matches =
                engine.evaluateBazaar(
                        List.of(watch),
                        bazaar(
                                6L,
                                89.0D),
                        510_000L);

        assertEquals(
                1,
                matches.size());
    }


    private static List<MarketWatchAuction> marketAround110k() {
        return List.of(
                auction(
                        "existing-50k",
                        50_000L),
                auction(
                        "irrelevant-75k",
                        75_000L),
                auction(
                        "market-105k",
                        105_000L),
                auction(
                        "market-110k",
                        110_000L),
                auction(
                        "market-115k",
                        115_000L),
                auction(
                        "market-120k",
                        120_000L),
                auction(
                        "market-125k",
                        125_000L));
    }


    private static MarketWatchAuctionWatch dynamicWatch(
            double percent) {

        MarketWatchAuctionWatch watch =
                new MarketWatchAuctionWatch();

        watch.itemName =
                "Shadow Fury";

        watch.tier =
                "LEGENDARY";

        watch.binOnly =
                true;

        watch.maxPriceCoins =
                0L;

        watch.minDiscountPercent =
                percent;

        watch.cooldownSeconds =
                60L;

        return watch;
    }


    private static MarketWatchAuctionSnapshot snapshot(
            long lastUpdated,
            List<MarketWatchAuction> auctions) {

        return new MarketWatchAuctionSnapshot(
                lastUpdated,
                auctions.size(),
                auctions);
    }


    private static MarketWatchAuction auction(
            String uuid,
            long price) {

        return new MarketWatchAuction(
                uuid,
                "Shadow Fury",
                "weapon",
                "LEGENDARY",
                100L,
                10_000_000L,
                price,
                0L,
                true,
                "data");
    }


    private static MarketWatchBazaarSnapshot bazaar(
            long updated,
            double buyPrice) {

        MarketWatchBazaarProduct product =
                new MarketWatchBazaarProduct(
                        "ENCHANTED_DIAMOND",
                        buyPrice,
                        Math.max(
                                1.0D,
                                buyPrice - 5.0D),
                        1_000L,
                        1_000L,
                        100_000L,
                        100_000L,
                        List.of(),
                        List.of());

        return new MarketWatchBazaarSnapshot(
                updated,
                Map.of(
                        "ENCHANTED_DIAMOND",
                        product));
    }
}