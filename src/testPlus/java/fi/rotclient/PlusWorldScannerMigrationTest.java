package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusWorldScannerMigrationTest {
    @Test
    void oldScannerSettingsAndTargetDetailsSurviveWrites() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject qol = legacy.getAsJsonObject("qolUtilities");
            qol.addProperty("worldScannerEnabled", true);
            qol.addProperty("worldScannerEspRange", 64);
            JsonObject fairy = new JsonObject();
            fairy.addProperty("enabled", false);
            fairy.addProperty("colorArgb", 0xFF123456);
            fairy.addProperty("futureKey", "keep");
            JsonObject targets = new JsonObject();
            targets.add("fairy", fairy);
            qol.add("worldScannerTargets", targets);
            TrackerConfig restored = TrackerStore.fromJson(legacy);
            assertTrue(restored.qolUtilities.isModuleEnabled("qol.world_scanner"));
            assertEquals(64.0D, restored.qolUtilities.readNumber("qol.world_scanner.esp_range"));
            assertFalse(restored.qolUtilities.readBoolean("qol.world_scanner.target.fairy.enabled"));
            assertEquals(0xFF123456,
                    restored.qolUtilities.readColor("qol.world_scanner.target.fairy.color"));

            restored.qolUtilities.writeBoolean("qol.world_scanner.target.fairy.enabled", true);
            restored.qolUtilities.writeNumber("qol.world_scanner.target.fairy.opacity", 0.75D);
            restored.qolUtilities.writeEnum("qol.world_scanner.target.fairy.style", "Outline");
            assertTrue(restored.qolUtilities.readBoolean("qol.world_scanner.fairy"));
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(restored));
            assertEquals("keep", saved.qolUtilities.extensionFields
                    .getAsJsonObject("worldScannerTargets").getAsJsonObject("fairy")
                    .get("futureKey").getAsString());
            assertEquals("Outline", saved.qolUtilities.readEnum("qol.world_scanner.target.fairy.style"));
            assertEquals(0.75D, saved.qolUtilities.readNumber("qol.world_scanner.target.fairy.opacity"));

            saved.qolUtilities.resetModuleToDefaults("qol.world_scanner");
            assertFalse(WorldScannerSettings.from(saved.qolUtilities).enabled());
            assertEquals(WorldScannerPolicy.DEFAULT_ESP_RANGE,
                    WorldScannerSettings.from(saved.qolUtilities).espRange());
            assertTrue(WorldScannerSettings.from(saved.qolUtilities).target("fairy").enabled);
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
