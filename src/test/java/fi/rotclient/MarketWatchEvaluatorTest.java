package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchEvaluatorTest {
    @Test
    void matchesAuctionBelowConfiguredPrice() {
        MarketWatchAuctionWatch watch =
                new MarketWatchAuctionWatch();

        watch.itemName = "Shadow Fury";
        watch.tier = "LEGENDARY";
        watch.maxPriceCoins = 40_000_000L;

        MarketWatchAuctionSnapshot snapshot =
                new MarketWatchAuctionSnapshot(
                        100L,
                        2,
                        List.of(
                                auction(
                                        "cheap",
                                        "Shadow Fury",
                                        "LEGENDARY",
                                        35_000_000L,
                                        true),
                                auction(
                                        "expensive",
                                        "Shadow Fury",
                                        "LEGENDARY",
                                        50_000_000L,
                                        true)));

        List<MarketWatchAuctionMatch> matches =
                MarketWatchEvaluator.evaluateAuctions(
                        List.of(watch),
                        snapshot);

        assertEquals(1, matches.size());
        assertEquals(
                "cheap",
                matches.getFirst().auctionUuid());
        assertEquals(
                35_000_000L,
                matches.getFirst().priceCoins());
    }

    @Test
    void auctionRespectsTierAndBinOnly() {
        MarketWatchAuctionWatch watch =
                new MarketWatchAuctionWatch();

        watch.itemName = "Test Sword";
        watch.tier = "LEGENDARY";
        watch.maxPriceCoins = 10_000_000L;
        watch.binOnly = true;

        MarketWatchAuctionSnapshot snapshot =
                new MarketWatchAuctionSnapshot(
                        100L,
                        2,
                        List.of(
                                auction(
                                        "wrong-tier",
                                        "Test Sword",
                                        "EPIC",
                                        1_000_000L,
                                        true),
                                auction(
                                        "not-bin",
                                        "Test Sword",
                                        "LEGENDARY",
                                        1_000_000L,
                                        false)));

        assertTrue(
                MarketWatchEvaluator
                        .evaluateAuctions(
                                List.of(watch),
                                snapshot)
                        .isEmpty());
    }

    @Test
    void auctionDoesNotIgnoreUnimplementedValuationConditions() {
        MarketWatchAuctionWatch watch =
                new MarketWatchAuctionWatch();

        watch.itemName = "Shadow Fury";
        watch.maxPriceCoins = 40_000_000L;
        watch.minDiscountPercent = 10.0D;

        MarketWatchAuctionSnapshot snapshot =
                new MarketWatchAuctionSnapshot(
                        100L,
                        1,
                        List.of(
                                auction(
                                        "cheap",
                                        "Shadow Fury",
                                        "LEGENDARY",
                                        30_000_000L,
                                        true)));

        assertTrue(
                MarketWatchEvaluator
                        .evaluateAuctions(
                                List.of(watch),
                                snapshot)
                        .isEmpty());
    }

    @Test
    void bazaarMatchesAllConfiguredThresholds() {
        MarketWatchBazaarWatch watch =
                new MarketWatchBazaarWatch();

        watch.productId =
                "ENCHANTED_DIAMOND";

        watch.maxInstantBuyPrice =
                1_500.0D;

        watch.minInstantSellPrice =
                1_300.0D;

        watch.minSpreadCoins =
                100.0D;

        watch.minSpreadPercent =
                5.0D;

        watch.minWeeklyVolume =
                50_000L;

        MarketWatchBazaarSnapshot snapshot =
                bazaarSnapshot(
                        "ENCHANTED_DIAMOND",
                        1_500.0D,
                        1_350.0D,
                        80_000L,
                        70_000L);

        List<MarketWatchBazaarMatch> matches =
                MarketWatchEvaluator.evaluateBazaar(
                        List.of(watch),
                        snapshot);

        assertEquals(1, matches.size());

        MarketWatchBazaarMatch match =
                matches.getFirst();

        assertEquals(
                "ENCHANTED_DIAMOND",
                match.productId());

        assertEquals(
                150.0D,
                match.spreadCoins());

        assertEquals(
                10.0D,
                match.spreadPercent(),
                0.0001D);

        assertEquals(
                70_000L,
                match.weeklyVolume());
    }

    @Test
    void bazaarRejectsWhenOneConfiguredConditionFails() {
        MarketWatchBazaarWatch watch =
                new MarketWatchBazaarWatch();

        watch.productId =
                "ENCHANTED_DIAMOND";

        watch.maxInstantBuyPrice =
                1_400.0D;

        watch.minSpreadCoins =
                100.0D;

        MarketWatchBazaarSnapshot snapshot =
                bazaarSnapshot(
                        "ENCHANTED_DIAMOND",
                        1_500.0D,
                        1_300.0D,
                        100_000L,
                        100_000L);

        assertTrue(
                MarketWatchEvaluator
                        .evaluateBazaar(
                                List.of(watch),
                                snapshot)
                        .isEmpty());
    }

    @Test
    void bazaarWatchWithoutThresholdDoesNotSpamMatches() {
        MarketWatchBazaarWatch watch =
                new MarketWatchBazaarWatch();

        watch.productId =
                "ENCHANTED_DIAMOND";

        MarketWatchBazaarSnapshot snapshot =
                bazaarSnapshot(
                        "ENCHANTED_DIAMOND",
                        1_500.0D,
                        1_300.0D,
                        100_000L,
                        100_000L);

        assertTrue(
                MarketWatchEvaluator
                        .evaluateBazaar(
                                List.of(watch),
                                snapshot)
                        .isEmpty());
    }

    @Test
    void disabledWatchesNeverMatch() {
        MarketWatchAuctionWatch auctionWatch =
                new MarketWatchAuctionWatch();

        auctionWatch.enabled = false;
        auctionWatch.itemName = "Test";
        auctionWatch.maxPriceCoins = 10_000L;

        MarketWatchBazaarWatch bazaarWatch =
                new MarketWatchBazaarWatch();

        bazaarWatch.enabled = false;
        bazaarWatch.productId = "TEST";
        bazaarWatch.maxInstantBuyPrice = 10.0D;

        assertTrue(
                MarketWatchEvaluator
                        .evaluateAuctions(
                                List.of(auctionWatch),
                                new MarketWatchAuctionSnapshot(
                                        1L,
                                        1,
                                        List.of(
                                                auction(
                                                        "a",
                                                        "Test",
                                                        "RARE",
                                                        1L,
                                                        true))))
                        .isEmpty());

        assertTrue(
                MarketWatchEvaluator
                        .evaluateBazaar(
                                List.of(bazaarWatch),
                                bazaarSnapshot(
                                        "TEST",
                                        5.0D,
                                        4.0D,
                                        100L,
                                        100L))
                        .isEmpty());
    }

    private static MarketWatchAuction auction(
            String uuid,
            String itemName,
            String tier,
            long price,
            boolean bin) {

        return new MarketWatchAuction(
                uuid,
                itemName,
                "weapon",
                tier,
                1000L,
                2000L,
                price,
                0L,
                bin,
                "item-data");
    }

    private static MarketWatchBazaarSnapshot bazaarSnapshot(
            String productId,
            double buyPrice,
            double sellPrice,
            long buyWeek,
            long sellWeek) {

        MarketWatchBazaarProduct product =
                new MarketWatchBazaarProduct(
                        productId,
                        buyPrice,
                        sellPrice,
                        1000L,
                        1000L,
                        buyWeek,
                        sellWeek,
                        List.of(),
                        List.of());

        return new MarketWatchBazaarSnapshot(
                100L,
                Map.of(
                        productId,
                        product));
    }
}