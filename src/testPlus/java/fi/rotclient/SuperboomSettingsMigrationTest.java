package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SuperboomSettingsMigrationTest {
    @Test
    void legacyRootAndAthenValuesMigrateIntoPlusOpaqueFields() {
        JsonObject oldConfig = TrackerStore.toJson(new TrackerConfig());
        JsonObject extras = oldConfig.getAsJsonObject("qolUtilities").getAsJsonObject("extras");
        extras.addProperty("dungeonF7AutoSuperboom", true);
        extras.addProperty("dungeonF7SuperboomSwapBack", true);
        extras.addProperty("dungeonF7SuperboomDelay", 6);
        JsonObject athen = extras.getAsJsonObject("athen");
        athen.addProperty("superboomMinDelay", 2);
        athen.addProperty("superboomMaxDelay", 5);
        athen.addProperty("superboomSwapBackMin", 3);
        athen.addProperty("superboomSwapBackMax", 4);
        athen.addProperty("superboomSwapTo", "Custom slot");
        athen.addProperty("superboomCustomSlot", 8);
        athen.addProperty("superboomExtraBlocks", "minecraft:obsidian");

        TrackerConfig config = TrackerStore.fromJson(oldConfig);
        QolUtilityConfig qol = config.qolUtilities;
        SuperboomSettings settings = SuperboomSettings.from(qol);
        assertTrue(settings.enabled());
        assertTrue(settings.swapBack());
        assertEquals(6, settings.legacyDelay());
        assertEquals(2, settings.minDelay());
        assertEquals(5, settings.maxDelay());
        assertEquals(3, settings.swapBackMin());
        assertEquals(4, settings.swapBackMax());
        assertEquals("Custom slot", settings.swapTo());
        assertEquals(8, settings.customSlot());
        assertEquals("minecraft:obsidian", settings.extraBlocks());

        qol.writeBoolean("qol.dungeon_f7.auto_superboom", false);
        assertTrue(qol.writeNumber("qol.dungeon_f7.superboom_min_delay", 4));
        assertTrue(qol.writeEnum("qol.dungeon_f7.superboom_swap_to", "Original slot"));
        assertTrue(qol.writeText("qol.dungeon_f7.superboom_blocks", "minecraft:coal_block"));
        SuperboomSettings edited = SuperboomSettings.from(qol);
        assertFalse(edited.enabled());
        assertEquals(4, edited.minDelay());
        assertEquals("Original slot", edited.swapTo());
        assertEquals("minecraft:coal_block", edited.extraBlocks());

        JsonObject savedExtras = TrackerStore.toJson(config)
                .getAsJsonObject("qolUtilities").getAsJsonObject("extras");
        assertFalse(savedExtras.getAsJsonObject("extensionFields")
                .get("dungeonF7AutoSuperboom").getAsBoolean());
        assertEquals(4, savedExtras.getAsJsonObject("athen").getAsJsonObject("extensionFields")
                .get("superboomMinDelay").getAsInt());

        SuperboomSettings.reset(qol);
        assertFalse(SuperboomSettings.from(qol).enabled());
        assertEquals(1, SuperboomSettings.from(qol).minDelay());
        assertEquals("", SuperboomSettings.from(qol).extraBlocks());
    }
}
