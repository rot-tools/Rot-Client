package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerRngMeterPolicyTest {
    @Test
    void warnsOnlyForAnObservedMeterUpdateWithoutASelectedDrop() {
        assertTrue(SlayerRngMeterPolicy.shouldWarnEmpty(true, true, false));
        assertFalse(SlayerRngMeterPolicy.shouldWarnEmpty(false, true, false));
        assertFalse(SlayerRngMeterPolicy.shouldWarnEmpty(true, false, false));
        assertFalse(SlayerRngMeterPolicy.shouldWarnEmpty(true, true, true));
    }

    @Test
    void hidesOnlyAConfirmedMeterUpdateWithAKnownSelection() {
        assertTrue(SlayerRngMeterPolicy.shouldHideChat(true, true, true));
        assertFalse(SlayerRngMeterPolicy.shouldHideChat(false, true, true));
        assertFalse(SlayerRngMeterPolicy.shouldHideChat(true, false, true));
        assertFalse(SlayerRngMeterPolicy.shouldHideChat(true, true, false));
    }

    @Test
    void readsSelectedDropFromRngMeterInventoryAndChat() {
        assertTrue(SlayerRngMeterPolicy.chatSelection(
                "§aYou set your Voidgloom Seraph RNG Meter to drop §rJudgement Core§a!").isPresent());
        assertEquals("Judgement Core", SlayerRngMeterPolicy.chatSelection(
                "You set your Voidgloom Seraph RNG Meter to drop Judgement Core!").orElseThrow());
        assertEquals(12_345L, SlayerRngMeterPolicy.chatStoredXp(
                "   RNG Meter - 12,345 Stored XP").orElseThrow());
        assertTrue(SlayerRngMeterPolicy.isRngMeterInventory("Voidgloom Seraph RNG Meter"));
        assertFalse(SlayerRngMeterPolicy.isRngMeterInventory("Catacombs RNG Meter"));
        assertTrue(SlayerRngMeterPolicy.loreMeansSelected(
                List.of("§7Progress: §e12,345§7/§e885,562", "§a§lSELECTED")));
        SlayerRngMeterPolicy.Selection selected = SlayerRngMeterPolicy.fromRngMeterInventory(
                "Voidgloom Seraph RNG Meter",
                List.of(new SlayerRngMeterPolicy.SlotView(
                        11,
                        "Judgement Core",
                        List.of("Progress: 12,345/885,562", "SELECTED")))).orElseThrow();
        assertEquals("Judgement Core", selected.itemName());
        assertEquals(12_345L, selected.storedXp());
        assertEquals("Smite VI", SlayerRngMeterPolicy.displayName("Enchanted Book (Smite VI)"));
    }
}
