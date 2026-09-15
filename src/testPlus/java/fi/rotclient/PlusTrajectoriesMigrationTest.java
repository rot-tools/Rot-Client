package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusTrajectoriesMigrationTest {
    @Test
    void oldTrajectoryValuesRemainUsableAfterSchemaMove() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject qol = legacy.getAsJsonObject("qolUtilities");
            qol.addProperty("trajectoriesEnabled", true);
            qol.addProperty("trajectoriesBows", false);
            qol.addProperty("trajectoriesRange", 47);
            qol.addProperty("trajectoriesColor", 0xFF123456);
            TrackerConfig restored = TrackerStore.fromJson(legacy);
            assertTrue(restored.qolUtilities.isModuleEnabled("qol.trajectories"));
            assertFalse(restored.qolUtilities.readBoolean("qol.trajectories.bows"));
            assertEquals(47.0D, restored.qolUtilities.readNumber("qol.trajectories.range"));
            assertEquals(0xFF123456, restored.qolUtilities.readColor("qol.trajectories.color"));
            restored.qolUtilities.writeNumber("qol.trajectories.range", 999.0D);
            restored.qolUtilities.writeColor("qol.trajectories.color", 0xFF654321);
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(restored));
            assertEquals(TrajectoryPredictor.MAX_RANGE,
                    TrajectoriesSettings.from(saved.qolUtilities).range());
            assertEquals(0xFF654321, TrajectoriesSettings.from(saved.qolUtilities).color());
            assertFalse(TrajectoriesSettings.from(saved.qolUtilities).bows());
            saved.qolUtilities.resetModuleToDefaults("qol.trajectories");
            assertFalse(TrajectoriesSettings.from(saved.qolUtilities).enabled());
            assertTrue(TrajectoriesSettings.from(saved.qolUtilities).bows());
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
