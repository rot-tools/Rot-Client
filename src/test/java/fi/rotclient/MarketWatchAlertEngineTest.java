package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchAlertEngineTest {
    @Test
    void auctionListingOnlyAlertsOnce() {
        MarketWatchAuctionWatch watch =
                auctionWatch();

        watch.cooldownSeconds = 0L;

        MarketWatchAuctionSnapshot snapshot =
                auctionSnapshot("auction-1");

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        assertEquals(
                1,
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot,
                        1000L)
                        .size());

        assertTrue(
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot,
                        2000L)
                        .isEmpty());
    }

    @Test
    void auctionCooldownLimitsDifferentListings() {
        MarketWatchAuctionWatch watch =
                auctionWatch();

        watch.cooldownSeconds = 60L;

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        assertEquals(
                1,
                engine.evaluateAuctions(
                        List.of(watch),
                        auctionSnapshot("auction-1"),
                        100_000L)
                        .size());

        assertTrue(
                engine.evaluateAuctions(
                        List.of(watch),
                        auctionSnapshot("auction-2"),
                        120_000L)
                        .isEmpty());

        assertEquals(
                1,
                engine.evaluateAuctions(
                        List.of(watch),
                        auctionSnapshot("auction-2"),
                        160_000L)
                        .size());
    }

    @Test
    void sameBazaarSnapshotDoesNotAlertTwice() {
        MarketWatchBazaarWatch watch =
                bazaarWatch();

        watch.cooldownSeconds = 0L;

        MarketWatchBazaarSnapshot snapshot =
                bazaarSnapshot(500L);

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        assertEquals(
                1,
                engine.evaluateBazaar(
                        List.of(watch),
                        snapshot,
                        1000L)
                        .size());

        assertTrue(
                engine.evaluateBazaar(
                        List.of(watch),
                        snapshot,
                        2000L)
                        .isEmpty());
    }

    @Test
    void bazaarCooldownAppliesAcrossNewSnapshots() {
        MarketWatchBazaarWatch watch =
                bazaarWatch();

        watch.cooldownSeconds = 60L;

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        assertEquals(
                1,
                engine.evaluateBazaar(
                        List.of(watch),
                        bazaarSnapshot(500L),
                        100_000L)
                        .size());

        assertTrue(
                engine.evaluateBazaar(
                        List.of(watch),
                        bazaarSnapshot(501L),
                        120_000L)
                        .isEmpty());

        assertEquals(
                1,
                engine.evaluateBazaar(
                        List.of(watch),
                        bazaarSnapshot(502L),
                        160_000L)
                        .size());
    }

    @Test
    void clearAllowsFreshEvaluationAgain() {
        MarketWatchAuctionWatch watch =
                auctionWatch();

        watch.cooldownSeconds = 0L;

        MarketWatchAuctionSnapshot snapshot =
                auctionSnapshot("auction-1");

        MarketWatchAlertEngine engine =
                new MarketWatchAlertEngine();

        assertEquals(
                1,
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot,
                        1000L)
                        .size());

        engine.clear();

        assertEquals(
                1,
                engine.evaluateAuctions(
                        List.of(watch),
                        snapshot,
                        2000L)
                        .size());
    }

    private static MarketWatchAuctionWatch auctionWatch() {
        MarketWatchAuctionWatch watch =
                new MarketWatchAuctionWatch();

        watch.itemName = "Shadow Fury";
        watch.tier = "LEGENDARY";
        watch.binOnly = true;
        watch.maxPriceCoins = 40_000_000L;

        return watch;
    }

    private static MarketWatchBazaarWatch bazaarWatch() {
        MarketWatchBazaarWatch watch =
                new MarketWatchBazaarWatch();

        watch.productId =
                "ENCHANTED_DIAMOND";

        watch.maxInstantBuyPrice =
                1_500.0D;

        return watch;
    }

    private static MarketWatchAuctionSnapshot auctionSnapshot(
            String uuid) {

        MarketWatchAuction auction =
                new MarketWatchAuction(
                        uuid,
                        "Shadow Fury",
                        "weapon",
                        "LEGENDARY",
                        1000L,
                        2000L,
                        35_000_000L,
                        0L,
                        true,
                        "item-data");

        return new MarketWatchAuctionSnapshot(
                100L,
                1,
                List.of(auction));
    }

    private static MarketWatchBazaarSnapshot bazaarSnapshot(
            long lastUpdated) {

        MarketWatchBazaarProduct product =
                new MarketWatchBazaarProduct(
                        "ENCHANTED_DIAMOND",
                        1_400.0D,
                        1_300.0D,
                        1000L,
                        1000L,
                        100_000L,
                        100_000L,
                        List.of(),
                        List.of());

        return new MarketWatchBazaarSnapshot(
                lastUpdated,
                Map.of(
                        "ENCHANTED_DIAMOND",
                        product));
    }
}