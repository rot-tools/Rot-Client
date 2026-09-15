package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusMobHighlightMigrationTest {
    @Test
    void oldHighlightSettingsSurvivePlusWritesAndReset() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject qol = legacy.getAsJsonObject("qolUtilities");
            qol.addProperty("mobHighlightEnabled", true);
            qol.addProperty("mobHighlightRequireKey", true);
            qol.addProperty("mobHighlightAddKey", "H");
            qol.addProperty("mobHighlightDepth", false);
            qol.addProperty("mobHighlightColor", 0xFF123456);
            qol.add("mobHighlightNames", JsonParser.parseString("[\"Goblin\"]"));
            TrackerConfig restored = TrackerStore.fromJson(legacy);
            assertTrue(restored.qolUtilities.isModuleEnabled("qol.mob_highlight"));
            assertTrue(restored.qolUtilities.readBoolean("qol.mob_highlight.highlight_key"));
            assertFalse(restored.qolUtilities.readBoolean("qol.mob_highlight.depth"));
            assertEquals("H", restored.qolUtilities.readKeybind("qol.mob_highlight.add_key"));
            assertEquals(0xFF123456, restored.qolUtilities.readColor("qol.mob_highlight.color"));
            assertEquals("Goblin", MobHighlightSettings.from(restored.qolUtilities).names().getFirst());

            restored.qolUtilities.writeKeybind("qol.mob_highlight.add_key", "J");
            restored.qolUtilities.writeColor("qol.mob_highlight.color", 0xFFABCDEF);
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(restored));
            assertEquals("J", saved.qolUtilities.readKeybind("qol.mob_highlight.add_key"));
            assertEquals(0xFFABCDEF, saved.qolUtilities.readColor("qol.mob_highlight.color"));
            assertEquals("Goblin", MobHighlightSettings.from(saved.qolUtilities).names().getFirst());

            saved.qolUtilities.resetModuleToDefaults("qol.mob_highlight");
            assertFalse(MobHighlightSettings.from(saved.qolUtilities).enabled());
            assertTrue(MobHighlightSettings.from(saved.qolUtilities).depth());
            assertTrue(MobHighlightSettings.from(saved.qolUtilities).names().isEmpty());
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
