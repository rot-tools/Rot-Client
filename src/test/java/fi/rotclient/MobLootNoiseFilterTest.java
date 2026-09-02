package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MobLootNoiseFilterTest {
    @Test
    void acceptsRealGraveyardDrops() {
        assertTrue(MobLootNoiseFilter.isLootName("Rotten Flesh"));
        assertTrue(MobLootNoiseFilter.isLootName("Poisonous Potato"));
        assertTrue(MobLootNoiseFilter.isLootName("Carrot"));
        assertTrue(MobLootNoiseFilter.isLootName("Coins"));
    }

    @Test
    void stripKeepsRealDropsAttachedToMagicFindOverlay() {
        assertEquals(
                "Rotten Flesh",
                MobLootNoiseFilter.lootItemName(
                        "Rotten Flesh +3% ★ Magic Find"));
        assertEquals(
                "Rotten Flesh",
                MobLootNoiseFilter.lootItemName(
                        "Rotten Flesh (+3 ✯ Magic Find)"));
        assertEquals(
                "Poisonous Potato",
                MobLootNoiseFilter.lootItemName(
                        "Poisonous Potato (+42% Magic Find)"));
        assertTrue(MobLootNoiseFilter.lootItemName(
                "Kill Combo +3% ★ Magic Find").isEmpty());
        assertTrue(MobLootNoiseFilter.lootItemName(
                "Crypt Ghoul ★ Magic Find").isEmpty());
        assertTrue(MobLootNoiseFilter.lootItemName(
                "Crypt Ghoul ✯ Magic Find").isEmpty());
    }
}
