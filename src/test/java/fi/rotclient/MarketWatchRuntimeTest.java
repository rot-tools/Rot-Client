package fi.rotclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchRuntimeTest {
    @TempDir
    Path tempDir;

    @Test
    void disabledMarketWatchDoesNotEmitAlerts() {
        MarketWatchManager manager =
                new MarketWatchManager(
                        tempDir.resolve("disabled.json"));

        manager.loadFromDisk();

        MarketWatchAuctionWatch watch =
                manager.createAuctionWatch(
                        "SHADOW_FURY",
                        "Shadow Fury");

        assertNotNull(watch);

        watch.maxPriceCoins =
                40_000_000L;

        assertTrue(
                manager.updateAuctionWatch(
                        watch));

        MarketWatchRuntime runtime =
                new MarketWatchRuntime(
                        manager);

        runtime.startInternal();

        assertTrue(
                runtime.acceptAuctionSnapshot(
                        auctionSnapshot(),
                        1000L)
                        .isEmpty());

        assertTrue(
                runtime.drainAlerts()
                        .isEmpty());
    }

    @Test
    void liveAuctionSnapshotCreatesAlert() {
        Path path =
                tempDir.resolve("auction.json");

        MarketWatchManager manager =
                configuredAuctionManager(path);

        MarketWatchRuntime runtime =
                new MarketWatchRuntime(
                        manager);

        runtime.startInternal();

        List<MarketWatchLiveAlert> emitted =
                runtime.acceptAuctionSnapshot(
                        auctionSnapshot(),
                        1000L);

        assertEquals(1, emitted.size());

        MarketWatchLiveAlert alert =
                emitted.getFirst();

        assertEquals(
                MarketWatchLiveAlert.Market.AUCTION_HOUSE,
                alert.market());

        assertEquals(
                "auction-1",
                alert.targetId());

        assertEquals(
                35_000_000L,
                alert.auctionPriceCoins());

        assertEquals(
                1,
                runtime.drainAlerts().size());

        assertTrue(
                runtime.drainAlerts().isEmpty());
    }

    @Test
    void liveBazaarSnapshotCreatesAlert() {
        Path path =
                tempDir.resolve("bazaar.json");

        MarketWatchManager manager =
                new MarketWatchManager(path);

        manager.loadFromDisk();

        assertTrue(manager.setEnabled(true));

        MarketWatchBazaarWatch watch =
                manager.createBazaarWatch(
                        "ENCHANTED_DIAMOND");

        assertNotNull(watch);

        watch.maxInstantBuyPrice =
                1_500.0D;

        assertTrue(
                manager.updateBazaarWatch(
                        watch));

        MarketWatchRuntime runtime =
                new MarketWatchRuntime(
                        manager);

        runtime.startInternal();

        List<MarketWatchLiveAlert> emitted =
                runtime.acceptBazaarSnapshot(
                        bazaarSnapshot(500L),
                        1000L);

        assertEquals(1, emitted.size());

        MarketWatchLiveAlert alert =
                emitted.getFirst();

        assertEquals(
                MarketWatchLiveAlert.Market.BAZAAR,
                alert.market());

        assertEquals(
                "ENCHANTED_DIAMOND",
                alert.targetId());

        assertEquals(
                1_400.0D,
                alert.bazaarBuyPrice());

        assertEquals(
                1,
                runtime.drainAlerts().size());
    }

    @Test
    void duplicateLiveSnapshotDoesNotQueueTwice() {
        Path path =
                tempDir.resolve("duplicate.json");

        MarketWatchManager manager =
                configuredAuctionManager(path);

        MarketWatchRuntime runtime =
                new MarketWatchRuntime(
                        manager);

        runtime.startInternal();

        assertEquals(
                1,
                runtime.acceptAuctionSnapshot(
                        auctionSnapshot(),
                        1000L)
                        .size());

        assertTrue(
                runtime.acceptAuctionSnapshot(
                        auctionSnapshot(),
                        2000L)
                        .isEmpty());

        assertEquals(
                1,
                runtime.drainAlerts().size());
    }

        @Test
    void drainingPopupQueueDoesNotEraseAlertHistory() {
        Path path =
                tempDir.resolve(
                        "history.json");

        MarketWatchManager manager =
                configuredAuctionManager(
                        path);

        MarketWatchRuntime runtime =
                new MarketWatchRuntime(
                        manager);

        runtime.startInternal();

        assertEquals(
                1,
                runtime.acceptAuctionSnapshot(
                        auctionSnapshot(),
                        1000L)
                        .size());

        assertEquals(
                1,
                runtime.drainAlerts()
                        .size());

        assertEquals(
                1,
                runtime.historySnapshot()
                        .size());

        assertEquals(
                "Shadow Fury",
                runtime.historySnapshot()
                        .getFirst()
                        .displayName());

        runtime.clearAlertStateInternal();

        assertTrue(
                runtime.historySnapshot()
                        .isEmpty());

        assertTrue(
                runtime.drainAlerts()
                        .isEmpty());
    }
    private static MarketWatchManager configuredAuctionManager(
            Path path) {

        MarketWatchManager manager =
                new MarketWatchManager(path);

        manager.loadFromDisk();

        assertTrue(manager.setEnabled(true));

        MarketWatchAuctionWatch watch =
                manager.createAuctionWatch(
                        "SHADOW_FURY",
                        "Shadow Fury");

        assertNotNull(watch);

        watch.tier = "LEGENDARY";
        watch.maxPriceCoins =
                40_000_000L;
        watch.cooldownSeconds = 0L;

        assertTrue(
                manager.updateAuctionWatch(
                        watch));

        return manager;
    }

    private static MarketWatchAuctionSnapshot auctionSnapshot() {
        MarketWatchAuction auction =
                new MarketWatchAuction(
                        "auction-1",
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