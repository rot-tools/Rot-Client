package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class ItemProtectPolicyTest {
    @Test
    void starredAndHighRarityItemsAreProtected() {
        assertTrue(ItemProtectPolicy.isStarred("Hyperion ✪✪", List.of()));
        assertTrue(ItemProtectPolicy.isStarred("Midas Staff", List.of("§7Dungeon Item")));
        assertTrue(ItemProtectPolicy.isHighRarity(List.of("§5§lMYTHIC DUNGEON SWORD")));
        assertTrue(ItemProtectPolicy.matchesExtra("Aspect of the Dragons", List.of("aspect of the dragon")));
        assertTrue(ItemProtectPolicy.isProtected(
                true, true, "Hyperion ✪", List.of("§6Legendary"), List.of()));
    }

    @Test
    void sneakBypassesDropAndSalvageLocks() {
        List<String> lore = List.of("§6Legendary Sword");
        assertTrue(ItemProtectPolicy.shouldBlockDrop(
                true, true, false, true, true, "Hyperion ✪", lore, List.of()));
        assertFalse(ItemProtectPolicy.shouldBlockDrop(
                true, true, true, true, true, "Hyperion ✪", lore, List.of()));
        assertTrue(ItemProtectPolicy.shouldBlockSalvage(
                true, true, false, "Salvage Items", true, true, "Hyperion ✪", lore, List.of()));
        assertFalse(ItemProtectPolicy.shouldBlockSalvage(
                true, true, false, "Chest", true, true, "Hyperion ✪", lore, List.of()));
        assertTrue(ItemProtectPolicy.isSalvageScreen("The Hex"));
        assertTrue(ItemProtectPolicy.isThrowClick("THROW"));
        assertEquals(List.of("hyperion"), ItemProtectPolicy.parseExtraNames("Hyperion\n# comment"));
    }
}
