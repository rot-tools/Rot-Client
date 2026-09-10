package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MarketWatchAlertDetailTest {
    @Test
    void parserAndAlertKeepSellerAndExactPrice() {
        String seller =
                "069a79f444e94726a5befca90e38aaf5";

        JsonObject root =
                JsonParser
                        .parseString(
                                """
                                {
                                  "page": 0,
                                  "totalPages": 1,
                                  "totalAuctions": 1,
                                  "lastUpdated": 1234,
                                  "auctions": [
                                    {
                                      "uuid": "auction-1",
                                      "auctioneer": "069a79f444e94726a5befca90e38aaf5",
                                      "item_name": "Shadow Fury",
                                      "category": "weapon",
                                      "tier": "LEGENDARY",
                                      "start": 1000,
                                      "end": 2000,
                                      "starting_bid": 85000000,
                                      "highest_bid_amount": 0,
                                      "bin": true,
                                      "item_bytes": "item-data"
                                    }
                                  ]
                                }
                                """)
                        .getAsJsonObject();

        MarketWatchAuctionPage page =
                MarketWatchAuctionPageParser
                        .parse(
                                root);

        MarketWatchAuction auction =
                page.auctions()
                        .getFirst();

        assertEquals(
                seller,
                auction.auctioneerUuid());

        assertEquals(
                85_000_000L,
                auction.startingBid());

        MarketWatchAuctionMatch match =
                new MarketWatchAuctionMatch(
                        "watch-1",
                        auction.uuid(),
                        auction.itemName(),
                        auction.tier(),
                        auction.startingBid(),
                        true,
                        100_000_000L,
                        15.0D);

        MarketWatchLiveAlert alert =
                MarketWatchLiveAlert
                        .fromAuction(
                                match,
                                auction.auctioneerUuid(),
                                match.referencePriceCoins(),
                                5000L);

        assertEquals(
                "Shadow Fury",
                alert.displayName());

        assertEquals(
                85_000_000L,
                alert.auctionPriceCoins());

        assertEquals(
                seller,
                alert.sellerUuid());

        assertEquals(
                15_000_000.0D,
                alert.spreadCoins(),
                0.001D);

        assertEquals(
                15.0D,
                alert.spreadPercent(),
                0.001D);
    }

    @Test
    void oldAuctionConstructorStillWorks() {
        MarketWatchAuction auction =
                new MarketWatchAuction(
                        "auction-2",
                        "Aspect of the End",
                        "weapon",
                        "RARE",
                        1L,
                        2L,
                        1_500_000L,
                        0L,
                        true,
                        "item-data");

        assertEquals(
                "",
                auction.auctioneerUuid());

        assertEquals(
                1_500_000L,
                auction.startingBid());
    }
}