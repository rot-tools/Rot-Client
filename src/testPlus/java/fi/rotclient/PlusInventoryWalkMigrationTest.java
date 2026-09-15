package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusInventoryWalkMigrationTest {
    @Test
    void oldSettingsSurviveConfigAndProfileRoundTrips() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            var module = QolPlusCatalog.extraModules().stream()
                    .filter(entry -> "qol.inventory_walk".equals(entry.id()))
                    .findFirst().orElseThrow();
            assertTrue(QolUtilityCatalog.hasCheatTag(module));

            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject qol = legacy.getAsJsonObject("qolUtilities");
            qol.addProperty("inventoryWalkEnabled", true);
            qol.addProperty("inventoryWalkPingMs", 320);
            TrackerConfig restored = TrackerStore.fromJson(legacy);
            assertTrue(restored.qolUtilities.isModuleEnabled("qol.inventory_walk"));
            assertEquals(320.0, restored.qolUtilities.readNumber("qol.inventory_walk.ping"));
            assertEquals(320, InventoryWalkSettings.from(restored.qolUtilities).pingMs());
            assertTrue(qol.has("inventoryWalkPingMs"));
            assertFalse(qol.getAsJsonObject("extensionFields").has("inventoryWalkPingMs"));

            restored.qolUtilities.writeNumber("qol.inventory_walk.ping", 999);
            assertEquals(500, InventoryWalkSettings.from(restored.qolUtilities).pingMs());
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(restored));
            assertTrue(InventoryWalkSettings.from(saved.qolUtilities).enabled());
            assertEquals(500, InventoryWalkSettings.from(saved.qolUtilities).pingMs());

            RotClientProfileSettings profile = RotClientProfileSettings.defaults();
            profile.qolUtilities = saved.qolUtilities;
            assertEquals(500, InventoryWalkSettings.from(profile.copy().qolUtilities).pingMs());

            saved.qolUtilities.resetModuleToDefaults("qol.inventory_walk");
            assertFalse(InventoryWalkSettings.from(saved.qolUtilities).enabled());
            assertEquals(InventoryWalkPolicy.DEFAULT_PING_MS,
                    InventoryWalkSettings.from(saved.qolUtilities).pingMs());
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
