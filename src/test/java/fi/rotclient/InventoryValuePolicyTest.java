package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class InventoryValuePolicyTest {
    @Test
    void survivalCraftingResultIsExcludedFromTheBagTotal() {
        assertFalse(InventoryValuePolicy.includeSurvivalSlot(
                InventoryOverlayPolicy.CRAFT_RESULT_X,
                InventoryOverlayPolicy.CRAFT_RESULT_Y));
        assertTrue(InventoryValuePolicy.includeSurvivalSlot(
                InventoryOverlayPolicy.ARMOR_COLUMN_X,
                InventoryOverlayPolicy.ARMOR_COLUMN_Y));
        assertTrue(InventoryValuePolicy.includeSurvivalSlot(
                InventoryOverlayPolicy.MAIN_INVENTORY_X,
                InventoryOverlayPolicy.MAIN_INVENTORY_Y));
        assertTrue(InventoryValuePolicy.includeSurvivalSlot(
                InventoryOverlayPolicy.MAIN_INVENTORY_X,
                InventoryOverlayPolicy.HOTBAR_Y));
        assertTrue(InventoryValuePolicy.includeSurvivalSlot(
                InventoryOverlayPolicy.CRAFT_INPUT_X,
                InventoryOverlayPolicy.CRAFT_INPUT_Y));
        assertTrue(InventoryValuePolicy.includeSurvivalSlot(
                InventoryOverlayPolicy.VANILLA_OFFHAND_SLOT_X,
                InventoryOverlayPolicy.VANILLA_OFFHAND_SLOT_Y));
    }

    @Test
    void petCandidatesUseNeuTierAndTypeKeys() {
        List<String> ids = InventoryValuePolicy.marketIdCandidates(
                "PET",
                "{\"type\":\"GOLDEN_DRAGON\",\"tier\":\"LEGENDARY\",\"exp\":1}",
                "[Lvl 200] Golden Dragon");
        assertEquals("GOLDEN_DRAGON;4", ids.get(0));
        assertTrue(ids.contains("PET_GOLDEN_DRAGON"));
        assertTrue(ids.contains("GOLDEN_DRAGON"));
        assertFalse(ids.contains("PET"));
    }

    @Test
    void ordinaryItemsDoNotInventPetKeysFromTheHoverName() {
        List<String> ids = InventoryValuePolicy.marketIdCandidates(
                "HYPERION", "", "Hyperion");
        assertEquals(List.of("HYPERION"), ids);
    }

    @Test
    void combinedAhBzPrefersBazaarSellThenBuyThenLowestBin() {
        Map<String, Double> bin = Map.of("HYPERION", 1_000_000_000.0D, "ENCHANTED_DIAMOND", 12.0D);
        Map<String, Double> buy = Map.of("ENCHANTED_DIAMOND", 25.0D);
        Map<String, Double> sell = Map.of("ENCHANTED_DIAMOND", 40.0D);
        assertEquals(40.0D, InventoryValuePolicy.unitValue("ENCHANTED_DIAMOND", bin, buy, sell));
        assertEquals(1_000_000_000.0D, InventoryValuePolicy.unitValue("HYPERION", bin, buy, sell));
        assertEquals("ENCHANTED_DIAMOND", InventoryValuePolicy.pricedMarketId(
                List.of("ENCHANTED_DIAMOND"), bin, buy, sell));
        assertEquals("", InventoryValuePolicy.pricedMarketId(
                List.of("UNKNOWN_ITEM"), bin, buy, sell));
    }

    @Test
    void totalAddsArmorEquipmentPetAndBagUsingLiveQuotes() {
        List<StorageOverlayPolicy.MarketLine> lines = List.of(
                new StorageOverlayPolicy.MarketLine("HYPERION", 1),
                new StorageOverlayPolicy.MarketLine("ENCHANTED_DIAMOND", 2),
                new StorageOverlayPolicy.MarketLine("GOLDEN_DRAGON;4", 1),
                new StorageOverlayPolicy.MarketLine("MOLTEN_BELT", 1));
        Map<String, Double> bin = Map.of(
                "HYPERION", 100.0D,
                "GOLDEN_DRAGON;4", 50.0D,
                "MOLTEN_BELT", 7.0D);
        Map<String, Double> sell = Map.of("ENCHANTED_DIAMOND", 3.0D);
        assertEquals(100.0D + 6.0D + 50.0D + 7.0D, InventoryValuePolicy.totalCoins(
                lines, bin, Map.of(), sell));
        assertEquals("163", InventoryValuePolicy.compactMark(163.0D));
        assertEquals("Total value: 163", InventoryValuePolicy.tooltip(163.0D));
        assertEquals("?", InventoryValuePolicy.compactMark(0.0D));
        assertEquals("No AH/BZ prices yet", InventoryValuePolicy.tooltip(0.0D));
    }

    @Test
    void liveQuoteMapsChangeThePrintedTotal() {
        List<StorageOverlayPolicy.MarketLine> bag = List.of(
                new StorageOverlayPolicy.MarketLine("HYPERION", 1));
        assertEquals(10.0D, InventoryValuePolicy.totalCoins(
                bag, Map.of("HYPERION", 10.0D), Map.of(), Map.of()));
        assertEquals(25.0D, InventoryValuePolicy.totalCoins(
                bag, Map.of("HYPERION", 25.0D), Map.of(), Map.of()));
    }
}
