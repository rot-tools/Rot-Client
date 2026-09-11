package fi.rotclient;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchAuctionPageParserTest {
    @Test
    void parsesBasicAuctionPage() {
        MarketWatchAuctionPage page =
                MarketWatchAuctionPageParser.parse(
                        JsonParser.parseString("""
                                {
                                  "success": true,
                                  "page": 2,
                                  "totalPages": 10,
                                  "totalAuctions": 1234,
                                  "lastUpdated": 987654321,
                                  "auctions": [
                                    {
                                      "uuid": "auction-1",
                                      "item_name": "Shadow Fury",
                                      "category": "weapon",
                                      "tier": "LEGENDARY",
                                      "start": 1000,
                                      "end": 2000,
                                      "starting_bid": 42500000,
                                      "highest_bid_amount": 0,
                                      "bin": true,
                                      "item_bytes": "encoded-data"
                                    }
                                  ]
                                }
                                """).getAsJsonObject());

        assertEquals(2, page.page());
        assertEquals(10, page.totalPages());
        assertEquals(1234, page.totalAuctions());
        assertEquals(987654321L, page.lastUpdated());
        assertEquals(1, page.auctions().size());

        MarketWatchAuction auction = page.auctions().getFirst();

        assertEquals("auction-1", auction.uuid());
        assertEquals("Shadow Fury", auction.itemName());
        assertEquals("weapon", auction.category());
        assertEquals("LEGENDARY", auction.tier());
        assertEquals(42_500_000L, auction.startingBid());
        assertTrue(auction.bin());
        assertEquals("encoded-data", auction.itemBytes());
    }

    @Test
    void toleratesMissingAuctionArray() {
        MarketWatchAuctionPage page =
                MarketWatchAuctionPageParser.parse(
                        JsonParser.parseString("""
                                {
                                  "page": 0,
                                  "totalPages": 1,
                                  "lastUpdated": 123
                                }
                                """).getAsJsonObject());

        assertTrue(page.auctions().isEmpty());
        assertEquals(123L, page.lastUpdated());
    }
}