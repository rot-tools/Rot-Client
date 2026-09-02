package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class IotaKuudraProfitPolicyTest {
    @Test
    void resolveIdsMapEssenceShardsAndNamedDrops() {
        assertEquals("ESSENCE_CRIMSON", IotaKuudraProfitPolicy.resolveItemId("ESSENCE_CRIMSON", "x"));
        assertEquals("ESSENCE_CRIMSON", IotaKuudraProfitPolicy.resolveItemId("", "Crimson Essence x32"));
        assertEquals("KUUDRA_TEETH", IotaKuudraProfitPolicy.resolveItemId("", "Kuudra Teeth"));
        assertEquals("KISMET_FEATHER", IotaKuudraProfitPolicy.resolveItemId("", "Kismet Feather"));
        assertEquals("WHEEL_OF_FATE", IotaKuudraProfitPolicy.resolveItemId("", "Wheel of Fate"));
        assertEquals("SHARD_PRECURSOR", IotaKuudraProfitPolicy.resolveItemId("", "Precursor Shard"));
        assertEquals(32, IotaKuudraProfitPolicy.resolveQuantity("Crimson Essence x32", 1));
    }

    @Test
    void essenceBooksArmorAndKeysUseRotFormulas() {
        IotaKuudraProfitPolicy.Prices prices = id -> switch (id) {
            case "ESSENCE_CRIMSON" -> 100L;
            case "ENCHANTMENT_SHARPNESS_7" -> 5_000_000L;
            case "NETHER_STAR" -> 10_000L;
            case "ENCHANTED_MYCELIUM" -> 1_000L;
            case "KISMET_FEATHER" -> 750_000L;
            default -> 0L;
        };
        assertEquals(3_800L, IotaKuudraProfitPolicy.itemValue(
                "ESSENCE_CRIMSON", "", 32, 0, true, 20.0D, prices));
        assertEquals(5_000_000L, IotaKuudraProfitPolicy.itemValue(
                "ENCHANTED_BOOK", "ENCHANTMENT_SHARPNESS_7", 1, 0, true, 20.0D, prices));
        assertEquals(0L, IotaKuudraProfitPolicy.itemValue(
                "ENCHANTED_BOOK", "", 1, 0, true, 20.0D, prices));
        assertEquals(2, IotaKuudraProfitPolicy.countStars("Aurora Helmet" + ((char) 10026) + ((char) 10026)));
        assertEquals(142, IotaKuudraProfitPolicy.salvageEssence(2));
        assertTrue(IotaKuudraProfitPolicy.isKuudraArmor("AURORA_HELMET"));
        assertEquals(14_200L, IotaKuudraProfitPolicy.itemValue(
                "AURORA_HELMET", "", 1, 2, true, 20.0D, prices));
        assertEquals(IotaKuudraProfitPolicy.KeyTier.INFERNAL,
                IotaKuudraProfitPolicy.parseKeyTier("Requires Infernal Kuudra Key"));
        assertEquals(2_400_000L + 20_000L + 80_000L, IotaKuudraProfitPolicy.keyCost(
                IotaKuudraProfitPolicy.KeyTier.INFERNAL, true, true, prices));
        assertEquals(0L, IotaKuudraProfitPolicy.keyCost(
                IotaKuudraProfitPolicy.KeyTier.INFERNAL, false, true, prices));
        assertEquals(750_000L, prices.price(IotaKuudraProfitPolicy.KISMET_FEATHER));
    }

    @Test
    void quotePrefersSellOrderThenBinAndLoreFallback() {
        PriceTooltipsPolicy.Quote quote = new PriceTooltipsPolicy.Quote(9.0D, 4.0D, 3.0D, 0, 0);
        assertEquals(3L, IotaKuudraProfitPolicy.quotePrice(true, quote));
        assertEquals(4L, IotaKuudraProfitPolicy.quotePrice(false, quote));
        assertEquals(9L, IotaKuudraProfitPolicy.quotePrice(
                true, new PriceTooltipsPolicy.Quote(9.0D, 0, 0, 0, 0)));
        assertEquals(12L, IotaKuudraProfitPolicy.preferLive(12L, 99L));
        assertEquals(99L, IotaKuudraProfitPolicy.preferLive(0L, 99L));
        assertTrue(IotaKuudraProfitPolicy.sellOrderMode(null));
        assertFalse(IotaKuudraProfitPolicy.sellOrderMode("INSTA_SELL"));
        assertTrue(IotaKuudraProfitPolicy.buySlotLooksFree("Free Reward Chest"));
        assertEquals("ENCHANTMENT_SHARPNESS_7",
                IotaKuudraProfitPolicy.enchantedBookPriceId("sharpness", 7));
    }
}
