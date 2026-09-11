package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchConfigTest {
    @Test
    void normalizesAuctionAndBazaarWatches() {
        MarketWatchAuctionWatch auction =
                new MarketWatchAuctionWatch();

        auction.itemId = "  aspect_of_the_end ";
        auction.itemName = "  Aspect of the End  ";
        auction.tier = " legendary ";
        auction.maxPriceCoins = -10L;
        auction.minDiscountPercent = 150.0D;
        auction.minProfitPercent = -5.0D;
        auction.minSampleSize = 0;
        auction.cooldownSeconds = -1L;

        MarketWatchBazaarWatch bazaar =
                new MarketWatchBazaarWatch();

        bazaar.productId = " enchanted_diamond ";
        bazaar.maxInstantBuyPrice = -1.0D;
        bazaar.minSpreadPercent = Double.NaN;
        bazaar.minWeeklyVolume = -5L;

        MarketWatchConfig config =
                MarketWatchConfig.defaults();

        config.auctionWatches =
                new ArrayList<>(List.of(auction));

        config.bazaarWatches =
                new ArrayList<>(List.of(bazaar));

        config.normalize();

        assertEquals(
                "ASPECT_OF_THE_END",
                auction.itemId);

        assertEquals(
                "Aspect of the End",
                auction.itemName);

        assertEquals(
                "LEGENDARY",
                auction.tier);

        assertEquals(0L, auction.maxPriceCoins);
        assertEquals(100.0D, auction.minDiscountPercent);
        assertEquals(0.0D, auction.minProfitPercent);
        assertEquals(1, auction.minSampleSize);
        assertEquals(0L, auction.cooldownSeconds);

        assertEquals(
                "ENCHANTED_DIAMOND",
                bazaar.productId);

        assertEquals(0.0D, bazaar.maxInstantBuyPrice);
        assertEquals(0.0D, bazaar.minSpreadPercent);
        assertEquals(0L, bazaar.minWeeklyVolume);
    }

    @Test
    void duplicateIdsAreRepairedAcrossBothMarkets() {
        MarketWatchAuctionWatch auction =
                new MarketWatchAuctionWatch();

        MarketWatchBazaarWatch bazaar =
                new MarketWatchBazaarWatch();

        bazaar.id = auction.id;

        MarketWatchConfig config =
                MarketWatchConfig.defaults();

        config.auctionWatches.add(auction);
        config.bazaarWatches.add(bazaar);

        config.normalize();

        assertNotEquals(
                auction.id,
                bazaar.id);
    }

    @Test
    void nullWatchListsBecomeEmptyLists() {
        MarketWatchConfig config =
                MarketWatchConfig.defaults();

        config.auctionWatches = null;
        config.bazaarWatches = null;

        config.normalize();

        assertNotNull(config.auctionWatches);
        assertNotNull(config.bazaarWatches);
        assertTrue(config.auctionWatches.isEmpty());
        assertTrue(config.bazaarWatches.isEmpty());
    }
}