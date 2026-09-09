package fi.rotclient;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchBazaarParserTest {
    @Test
    void parsesProductPricesVolumeAndOrderBooks() {
        MarketWatchBazaarSnapshot snapshot =
                MarketWatchBazaarParser.parse(
                        JsonParser.parseString("""
                                {
                                  "success": true,
                                  "lastUpdated": 555000,
                                  "products": {
                                    "ENCHANTED_DIAMOND": {
                                      "quick_status": {
                                        "buyPrice": 1500.5,
                                        "sellPrice": 1400.25,
                                        "buyVolume": 10000,
                                        "sellVolume": 8000,
                                        "buyMovingWeek": 70000,
                                        "sellMovingWeek": 60000
                                      },
                                      "buy_summary": [
                                        {
                                          "amount": 64,
                                          "pricePerUnit": 1501.0
                                        }
                                      ],
                                      "sell_summary": [
                                        {
                                          "amount": 128,
                                          "pricePerUnit": 1399.0
                                        }
                                      ]
                                    }
                                  }
                                }
                                """).getAsJsonObject());

        assertEquals(555000L, snapshot.lastUpdated());

        MarketWatchBazaarProduct product =
                snapshot.product("enchanted_diamond");

        assertNotNull(product);
        assertEquals("ENCHANTED_DIAMOND", product.productId());
        assertEquals(1500.5D, product.quickBuyPrice());
        assertEquals(1400.25D, product.quickSellPrice());
        assertEquals(10000L, product.buyVolume());
        assertEquals(8000L, product.sellVolume());
        assertEquals(70000L, product.buyMovingWeek());
        assertEquals(60000L, product.sellMovingWeek());

        assertEquals(1, product.buyOrders().size());
        assertEquals(64L, product.buyOrders().getFirst().amount());
        assertEquals(1501.0D,
                product.buyOrders().getFirst().pricePerUnit());

        assertEquals(1, product.sellOrders().size());
        assertEquals(128L, product.sellOrders().getFirst().amount());
        assertEquals(1399.0D,
                product.sellOrders().getFirst().pricePerUnit());
    }

    @Test
    void toleratesMissingProducts() {
        MarketWatchBazaarSnapshot snapshot =
                MarketWatchBazaarParser.parse(
                        JsonParser.parseString("""
                                {
                                  "success": true,
                                  "lastUpdated": 999
                                }
                                """).getAsJsonObject());

        assertEquals(999L, snapshot.lastUpdated());
        assertTrue(snapshot.products().isEmpty());
    }
}