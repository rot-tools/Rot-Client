package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemRarityPolicyTest {
    @Test
    void readsTheRarityLineFromTheBottomOfTheLore() {
        assertEquals(
                ItemRarityPolicy.Rarity.EPIC,
                ItemRarityPolicy.parseRarity(List.of("§7Damage: §c+100", "§5§lEPIC SWORD")));
        assertEquals(
                ItemRarityPolicy.Rarity.MYTHIC,
                ItemRarityPolicy.parseRarity(List.of(
                        "§7Some ability",
                        "§d§l§ka§r §d§l§d§lMYTHIC ACCESSORY §r§d§l§ka")));
        assertEquals(
                ItemRarityPolicy.Rarity.SPECIAL,
                ItemRarityPolicy.parseRarity(List.of("§c§lVERY SPECIAL")));
        assertEquals(
                ItemRarityPolicy.Rarity.COMMON,
                ItemRarityPolicy.parseRarity(List.of("§f§lCOMMON")));
    }

    @Test
    void ordinaryWordsInMenuLoreAreNotARarity() {
        // A menu button's sentence must not tint the slot just because it says "rare" or "special".
        assertNull(ItemRarityPolicy.parseRarity(List.of(
                "§7View your rare drops and special rewards.",
                "§eClick to view!")));
        assertNull(ItemRarityPolicy.parseRarity(List.of("§7A common problem for epic players.")));
        assertNull(ItemRarityPolicy.parseRarity(List.of()));
        assertNull(ItemRarityPolicy.parseRarity(null));
    }

    @Test
    void uncommonIsNotMistakenForCommon() {
        assertEquals(
                ItemRarityPolicy.Rarity.UNCOMMON,
                ItemRarityPolicy.parseRarityLine("§a§lUNCOMMON"));
    }
}
