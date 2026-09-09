package fi.rotclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void createsPersistsAndReloadsWatches() {
        Path path =
                tempDir.resolve("market-watch.json");

        MarketWatchManager manager =
                new MarketWatchManager(path);

        manager.loadFromDisk();

        assertTrue(manager.setEnabled(true));

        MarketWatchAuctionWatch auction =
                manager.createAuctionWatch(
                        "aspect_of_the_end",
                        "Aspect of the End");

        MarketWatchBazaarWatch bazaar =
                manager.createBazaarWatch(
                        "enchanted_diamond");

        assertNotNull(auction);
        assertNotNull(bazaar);

        MarketWatchManager reloaded =
                new MarketWatchManager(path);

        reloaded.loadFromDisk();

        assertTrue(reloaded.enabled());
        assertEquals(
                1,
                reloaded.auctionWatches().size());
        assertEquals(
                1,
                reloaded.bazaarWatches().size());

        assertEquals(
                "ASPECT_OF_THE_END",
                reloaded.auctionWatches()
                        .getFirst()
                        .itemId);

        assertEquals(
                "ENCHANTED_DIAMOND",
                reloaded.bazaarWatches()
                        .getFirst()
                        .productId);
    }

    @Test
    void returnedWatchesAreDetachedCopies() {
        Path path =
                tempDir.resolve("copies.json");

        MarketWatchManager manager =
                new MarketWatchManager(path);

        manager.loadFromDisk();

        MarketWatchAuctionWatch created =
                manager.createAuctionWatch(
                        "TEST_ITEM",
                        "Test Item");

        assertNotNull(created);

        created.maxPriceCoins = 123L;

        MarketWatchAuctionWatch stored =
                manager.findAuctionWatch(
                        created.id);

        assertNotNull(stored);
        assertEquals(
                0L,
                stored.maxPriceCoins);
    }

    @Test
    void updatesAndDeletesAuctionWatch() {
        Path path =
                tempDir.resolve("auction.json");

        MarketWatchManager manager =
                new MarketWatchManager(path);

        manager.loadFromDisk();

        MarketWatchAuctionWatch watch =
                manager.createAuctionWatch(
                        "TEST_ITEM",
                        "Test Item");

        assertNotNull(watch);

        watch.maxPriceCoins = 5_000_000L;
        watch.minDiscountPercent = 20.0D;

        assertTrue(
                manager.updateAuctionWatch(
                        watch));

        MarketWatchAuctionWatch updated =
                manager.findAuctionWatch(
                        watch.id);

        assertNotNull(updated);
        assertEquals(
                5_000_000L,
                updated.maxPriceCoins);
        assertEquals(
                20.0D,
                updated.minDiscountPercent);

        assertTrue(
                manager.deleteWatch(
                        watch.id));

        assertTrue(
                manager.auctionWatches()
                        .isEmpty());
    }

    @Test
    void updatesAndDeletesBazaarWatch() {
        Path path =
                tempDir.resolve("bazaar.json");

        MarketWatchManager manager =
                new MarketWatchManager(path);

        manager.loadFromDisk();

        MarketWatchBazaarWatch watch =
                manager.createBazaarWatch(
                        "ENCHANTED_DIAMOND");

        assertNotNull(watch);

        watch.maxInstantBuyPrice = 1500.0D;
        watch.minSpreadPercent = 5.0D;

        assertTrue(
                manager.updateBazaarWatch(
                        watch));

        MarketWatchBazaarWatch updated =
                manager.findBazaarWatch(
                        watch.id);

        assertNotNull(updated);
        assertEquals(
                1500.0D,
                updated.maxInstantBuyPrice);
        assertEquals(
                5.0D,
                updated.minSpreadPercent);

        assertTrue(
                manager.deleteWatch(
                        watch.id));

        assertTrue(
                manager.bazaarWatches()
                        .isEmpty());
    }

    @Test
    void rejectsBlankWatchTargets() {
        MarketWatchManager manager =
                new MarketWatchManager(
                        tempDir.resolve("invalid.json"));

        manager.loadFromDisk();

        assertNull(
                manager.createAuctionWatch(
                        " ",
                        " "));

        assertNull(
                manager.createBazaarWatch(
                        " "));

        assertTrue(
                manager.auctionWatches()
                        .isEmpty());

        assertTrue(
                manager.bazaarWatches()
                        .isEmpty());
    }
}