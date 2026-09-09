package fi.rotclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void roundTripsConfiguration() {
        Path path =
                tempDir.resolve(
                        "market-watch.json");

        MarketWatchConfig config =
                MarketWatchConfig.defaults();

        config.enabled = true;

        MarketWatchAuctionWatch auction =
                new MarketWatchAuctionWatch();

        auction.itemId = "ASPECT_OF_THE_END";
        auction.maxPriceCoins = 1_000_000L;

        MarketWatchBazaarWatch bazaar =
                new MarketWatchBazaarWatch();

        bazaar.productId = "ENCHANTED_DIAMOND";
        bazaar.maxInstantBuyPrice = 1500.0D;

        config.auctionWatches.add(auction);
        config.bazaarWatches.add(bazaar);

        assertTrue(
                MarketWatchStore.save(
                        path,
                        config));

        assertTrue(Files.exists(path));

        MarketWatchConfig loaded =
                MarketWatchStore.load(path);

        assertTrue(loaded.enabled);
        assertEquals(
                1,
                loaded.auctionWatches.size());
        assertEquals(
                1,
                loaded.bazaarWatches.size());

        assertEquals(
                "ASPECT_OF_THE_END",
                loaded.auctionWatches.getFirst().itemId);

        assertEquals(
                "ENCHANTED_DIAMOND",
                loaded.bazaarWatches.getFirst().productId);
    }

    @Test
    void refusesToOverwriteFutureSchema() {
        MarketWatchConfig config =
                MarketWatchConfig.defaults();

        config.schemaVersion =
                MarketWatchConfig.SCHEMA_VERSION + 1;

        assertFalse(
                MarketWatchStore.save(
                        tempDir.resolve("future.json"),
                        config));
    }

    @Test
    void malformedFileFallsBackToDefaults()
            throws Exception {

        Path path =
                tempDir.resolve(
                        "broken.json");

        Files.writeString(
                path,
                "{not-json");

        MarketWatchConfig loaded =
                MarketWatchStore.load(path);

        assertNotNull(loaded);
        assertEquals(
                MarketWatchConfig.SCHEMA_VERSION,
                loaded.schemaVersion);
        assertTrue(loaded.auctionWatches.isEmpty());
        assertTrue(loaded.bazaarWatches.isEmpty());
    }
}