package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BazaarPriceServiceTest {
    @Test
    void quoteWalksBestSellOrdersBeforeUsingFallback() {
        JsonObject product = JsonParser.parseString("""
                {
                  "sell_summary": [
                    {"amount": 10, "pricePerUnit": 8.0},
                    {"amount": 5, "pricePerUnit": 10.0},
                    {"amount": 20, "pricePerUnit": 7.0}
                  ],
                  "quick_status": {"sellPrice": 6.5}
                }
                """).getAsJsonObject();

        BazaarPriceService.ProductPrice price =
                BazaarPriceService.readInstantSellProduct(product);

        assertNotNull(price);
        assertEquals(10.0, price.instantSellPrice(), 0.0001);
        assertEquals(50.0, price.quoteInstantSell(5), 0.0001);
        assertEquals(130.0, price.quoteInstantSell(15), 0.0001);
        assertEquals(270.0, price.quoteInstantSell(35), 0.0001);
        assertEquals(335.0, price.quoteInstantSell(45), 0.0001);
    }

    @Test
    void materialPricesRemainIsolated() {
        BazaarPriceService.ProductPrice goldRaw =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        1.2)),
                        1.1);

        BazaarPriceService.ProductPrice goldEnchanted =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        460)),
                        450);

        BazaarPriceService.ProductPrice diamondRaw =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        8.0)),
                        7.5);

        BazaarPriceService.ProductPrice diamondEnchanted =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        1_300)),
                        1_250);

        BazaarPriceService.ProductPrice mithrilRaw =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        8.0)),
                        7.5);

        BazaarPriceService.ProductPrice mithrilEnchanted =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        1_700)),
                        1_650);

        BazaarPriceService.ProductPrice titaniumRaw =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        50.0)),
                        45.0);

        BazaarPriceService.ProductPrice titaniumEnchanted =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        8_000)),
                        7_800);

        BazaarPriceService.ProductPrice tungstenRaw =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        100.0)),
                        95);

        BazaarPriceService.ProductPrice tungstenEnchanted =
                new BazaarPriceService.ProductPrice(
                        java.util.List.of(
                                new BazaarPriceService.OrderLevel(
                                        100,
                                        13_500)),
                        13_000);

        BazaarPriceService.MarketPrices prices =
                new BazaarPriceService.MarketPrices(
                        Map.of(
                                TrackedMaterial.GOLD,
                                new BazaarPriceService.MaterialPrices(
                                        goldRaw,
                                        goldEnchanted),

                                TrackedMaterial.DIAMOND,
                                new BazaarPriceService.MaterialPrices(
                                        diamondRaw,
                                        diamondEnchanted),

                                TrackedMaterial.MITHRIL,
                                new BazaarPriceService.MaterialPrices(
                                        mithrilRaw,
                                        mithrilEnchanted),

                                TrackedMaterial.TITANIUM,
                                new BazaarPriceService.MaterialPrices(
                                        titaniumRaw,
                                        titaniumEnchanted),

                                TrackedMaterial.TUNGSTEN,
                                new BazaarPriceService.MaterialPrices(
                                        tungstenRaw,
                                        tungstenEnchanted)));

        assertEquals(
                461.2,
                prices.forMaterial(TrackedMaterial.GOLD)
                        .quoteInstantSell(1, 1),
                0.0001);

        assertEquals(
                1_308.0,
                prices.forMaterial(TrackedMaterial.DIAMOND)
                        .quoteInstantSell(1, 1),
                0.0001);

        assertEquals(
                1_708.0,
                prices.forMaterial(TrackedMaterial.MITHRIL)
                        .quoteInstantSell(1, 1),
                0.0001);

        assertEquals(
                8_050.0,
                prices.forMaterial(TrackedMaterial.TITANIUM)
                        .quoteInstantSell(1, 1),
                0.0001);

        assertEquals(
                13_600.0,
                prices.forMaterial(TrackedMaterial.TUNGSTEN)
                        .quoteInstantSell(1, 1),
                0.0001);
    }

    @Test
    void depthFallbackCannotImproveAfterWorseVisibleOrders() {
        JsonObject product = JsonParser.parseString("""
                {
                  "sell_summary": [
                    {"amount": 5, "pricePerUnit": 10.0},
                    {"amount": 5, "pricePerUnit": 7.0}
                  ],
                  "quick_status": {"sellPrice": 9.0}
                }
                """).getAsJsonObject();

        BazaarPriceService.ProductPrice price =
                BazaarPriceService.readInstantSellProduct(product);

        assertNotNull(price);
        assertEquals(
                120.0,
                price.quoteInstantSell(15),
                0.0001);
    }

    @Test
    void instantSellUsesSellSummaryDespiteBuySummaryPresence() {
        JsonObject product = JsonParser.parseString("""
                {
                  "sell_summary": [
                    {"amount": 1, "pricePerUnit": 12.5}
                  ],
                  "buy_summary": [
                    {"amount": 1, "pricePerUnit": 20.0}
                  ],
                  "quick_status": {
                    "sellPrice": 12.0,
                    "buyPrice": 19.0
                  }
                }
                """).getAsJsonObject();

        BazaarPriceService.ProductPrice price =
                BazaarPriceService.readInstantSellProduct(product);

        assertNotNull(price);
        assertEquals(12.5, price.instantSellPrice(), 0.0001);
    }

    @Test
    void emptySellSummaryFallsBackToQuickStatusSellPrice() {
        JsonObject product = JsonParser.parseString("""
                {
                  "sell_summary": [],
                  "buy_summary": [
                    {"amount": 1, "pricePerUnit": 20.0}
                  ],
                  "quick_status": {
                    "sellPrice": 12.0,
                    "buyPrice": 19.0
                  }
                }
                """).getAsJsonObject();

        BazaarPriceService.ProductPrice price =
                BazaarPriceService.readInstantSellProduct(product);

        assertNotNull(price);
        assertEquals(12.0, price.instantSellPrice(), 0.0001);
    }

    @Test
    void buySummaryAndBuyPriceNeverDetermineInstantSellOutput() {
        JsonObject product = JsonParser.parseString("""
                {
                  "buy_summary": [
                    {"amount": 1, "pricePerUnit": 20.0}
                  ],
                  "quick_status": {
                    "buyPrice": 19.0
                  }
                }
                """).getAsJsonObject();

        assertNull(BazaarPriceService.readInstantSellProduct(product));
    }

    @Test
    void parsesRoughGemstoneProduct() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "ROUGH_RUBY_GEM": {
                    "sell_summary": [
                      {"amount": 10, "pricePerUnit": 3.5}
                    ],
                    "quick_status": {"sellPrice": 3.0}
                  }
                }
                """);

        BazaarPriceService.ProductPrice ruby =
                prices.forGemstoneProduct("ROUGH_RUBY_GEM");
        assertNotNull(ruby);
        assertEquals(3.5, ruby.instantSellPrice(), 0.0001);
    }

    @Test
    void parsesFlawedGemstoneProduct() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "FLAWED_TOPAZ_GEM": {
                    "sell_summary": [
                      {"amount": 5, "pricePerUnit": 42.0}
                    ],
                    "quick_status": {"sellPrice": 40.0}
                  }
                }
                """);

        assertEquals(
                42.0,
                prices.forGemstoneProduct("FLAWED_TOPAZ_GEM")
                        .instantSellPrice(),
                0.0001);
    }

    @Test
    void parsesMultipleGemstoneTypes() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "ROUGH_RUBY_GEM": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 1.0}],
                    "quick_status": {"sellPrice": 1.0}
                  },
                  "FLAWED_JADE_GEM": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 2.0}],
                    "quick_status": {"sellPrice": 2.0}
                  }
                }
                """);

        assertEquals(2, prices.byGemstoneProductId().size());
        assertEquals(
                1.0,
                prices.forGemstoneProduct("ROUGH_RUBY_GEM")
                        .instantSellPrice(),
                0.0001);
        assertEquals(
                2.0,
                prices.forGemstoneProduct("FLAWED_JADE_GEM")
                        .instantSellPrice(),
                0.0001);
    }

    @Test
    void fineFlawlessAndPerfectGemstonesAreIgnored() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "FINE_RUBY_GEM": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 99.0}],
                    "quick_status": {"sellPrice": 99.0}
                  },
                  "FLAWLESS_RUBY_GEM": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 88.0}],
                    "quick_status": {"sellPrice": 88.0}
                  },
                  "PERFECT_RUBY_GEM": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 77.0}],
                    "quick_status": {"sellPrice": 77.0}
                  }
                }
                """);

        assertTrue(prices.byGemstoneProductId().isEmpty());
    }

    @Test
    void unrelatedBazaarProductIsIgnored() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "WISHING_COMPASS": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 500.0}],
                    "quick_status": {"sellPrice": 500.0}
                  }
                }
                """);

        assertTrue(prices.byGemstoneProductId().isEmpty());
    }

    @Test
    void missingGemstoneProductIsTolerated() {
        BazaarPriceService.MarketPrices prices = parseProducts("{}");

        assertTrue(prices.byGemstoneProductId().isEmpty());
    }

    @Test
    void malformedGemstoneOrderLevelIsSkippedButFallbackSurvives() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "ROUGH_RUBY_GEM": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": "bad"}
                    ],
                    "quick_status": {"sellPrice": 3.0}
                  },
                  "FLAWED_RUBY_GEM": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": 4.0}
                    ],
                    "quick_status": {"sellPrice": 4.0}
                  }
                }
                """);

        // Malformed sell_summary levels are skipped; quick_status sellPrice remains.
        assertEquals(
                3.0,
                prices.forGemstoneProduct("ROUGH_RUBY_GEM")
                        .instantSellPrice(),
                0.0001);
        assertEquals(
                4.0,
                prices.forGemstoneProduct("FLAWED_RUBY_GEM")
                        .instantSellPrice(),
                0.0001);
    }

    @Test
    void malformedMaterialOrderDoesNotDiscardPeerMaterial() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "GOLD_INGOT": {
                    "sell_summary": [
                      "not-an-object",
                      {"amount": "bad", "pricePerUnit": 12.5}
                    ],
                    "quick_status": {"sellPrice": 12.5}
                  },
                  "ENCHANTED_GOLD": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": 400.0}
                    ],
                    "quick_status": {"sellPrice": 400.0}
                  },
                  "DIAMOND": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": 4.99260315136572}
                    ],
                    "quick_status": {"sellPrice": 4.99260315136572}
                  },
                  "ENCHANTED_DIAMOND": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": 500.0}
                    ],
                    "quick_status": {"sellPrice": 500.0}
                  }
                }
                """);

        assertEquals(
                12.5,
                prices.forMaterial(TrackedMaterial.GOLD).raw().instantSellPrice(),
                0.0001);
        assertEquals(
                4.99260315136572,
                prices.forMaterial(TrackedMaterial.DIAMOND)
                        .raw()
                        .instantSellPrice(),
                0.0001);
    }

    @Test
    void zeroAndNonPositiveGemstonePricesAreOmitted() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "ROUGH_RUBY_GEM": {
                    "sell_summary": [],
                    "quick_status": {"sellPrice": 0.0}
                  },
                  "FLAWED_RUBY_GEM": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": -1.0}
                    ],
                    "quick_status": {"sellPrice": 0.0}
                  },
                  "ROUGH_TOPAZ_GEM": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": 2.0}
                    ],
                    "quick_status": {"sellPrice": 2.0}
                  }
                }
                """);

        assertNull(prices.forGemstoneProduct("ROUGH_RUBY_GEM"));
        assertNull(prices.forGemstoneProduct("FLAWED_RUBY_GEM"));
        assertEquals(
                2.0,
                prices.forGemstoneProduct("ROUGH_TOPAZ_GEM")
                        .instantSellPrice(),
                0.0001);
    }

    @Test
    void malformedMaterialProductDoesNotDiscardValidPeerProduct() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "GOLD_INGOT": {
                    "sell_summary": [
                      {"amount": 1, "pricePerUnit": 12.5}
                    ],
                    "quick_status": {"sellPrice": 12.5}
                  },
                  "ENCHANTED_GOLD": {
                    "sell_summary": [
                      {"amount": "bad", "pricePerUnit": "bad"}
                    ],
                    "quick_status": {"sellPrice": "bad"}
                  }
                }
                """);

        BazaarPriceService.MaterialPrices gold =
                prices.forMaterial(TrackedMaterial.GOLD);
        assertNotNull(gold);
        assertNotNull(gold.raw());
        assertNull(gold.enchanted());
        assertEquals(12.5, gold.raw().instantSellPrice(), 0.0001);
        assertTrue(Double.isNaN(gold.quoteInstantSell(1L, 1L)));
        assertEquals(12.5, gold.quoteInstantSell(1L, 0L), 0.0001);
    }

    @Test
    void oneMalformedGemstoneDoesNotBlockValidProducts() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "GOLD_INGOT": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 1.2}],
                    "quick_status": {"sellPrice": 1.1}
                  },
                  "ENCHANTED_GOLD": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 460.0}],
                    "quick_status": {"sellPrice": 450.0}
                  },
                  "ROUGH_RUBY_GEM": null,
                  "FLAWED_TOPAZ_GEM": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 2.0}],
                    "quick_status": {"sellPrice": 2.0}
                  }
                }
                """);

        assertNotNull(prices.forMaterial(TrackedMaterial.GOLD));
        assertEquals(
                2.0,
                prices.forGemstoneProduct("FLAWED_TOPAZ_GEM")
                        .instantSellPrice(),
                0.0001);
    }

    @Test
    void gemstoneMapIsImmutable() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "ROUGH_RUBY_GEM": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 1.0}],
                    "quick_status": {"sellPrice": 1.0}
                  }
                }
                """);

        assertThrows(
                UnsupportedOperationException.class,
                () -> prices.byGemstoneProductId().put(
                        "ROUGH_RUBY_GEM",
                        prices.forGemstoneProduct("ROUGH_RUBY_GEM")));
    }

    @Test
    void materialMapIsImmutable() {
        BazaarPriceService.MarketPrices prices = parseProducts("""
                {
                  "GOLD_INGOT": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 1.2}],
                    "quick_status": {"sellPrice": 1.1}
                  },
                  "ENCHANTED_GOLD": {
                    "sell_summary": [{"amount": 1, "pricePerUnit": 460.0}],
                    "quick_status": {"sellPrice": 450.0}
                  }
                }
                """);

        assertThrows(
                UnsupportedOperationException.class,
                () -> prices.byMaterial().put(
                        TrackedMaterial.GOLD,
                        prices.forMaterial(TrackedMaterial.GOLD)));
    }

    private static BazaarPriceService.MarketPrices parseProducts(
            String json) {
        JsonObject products = JsonParser.parseString(json)
                .getAsJsonObject();
        return BazaarPriceService.parseMarketPrices(products);
    }
}
