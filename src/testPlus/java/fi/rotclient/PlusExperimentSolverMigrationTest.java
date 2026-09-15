package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusExperimentSolverMigrationTest {
    @Test
    void oldNestedSettingsSurvivePlusWritesAndReset() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject extras = legacy.getAsJsonObject("qolUtilities").getAsJsonObject("extras");
            extras.addProperty("experimentSolverEnabled", true);
            extras.addProperty("experimentChronomatron", false);
            extras.addProperty("experimentFirstColor", 0x80123456);
            TrackerConfig restored = TrackerStore.fromJson(legacy);
            assertTrue(restored.qolUtilities.isModuleEnabled("qol.experiment_solver"));
            assertFalse(restored.qolUtilities.readBoolean("qol.experiment_solver.chronomatron"));
            assertEquals(0x80123456,
                    restored.qolUtilities.readColor("qol.experiment_solver.first_color"));

            restored.qolUtilities.writeBoolean("qol.experiment_solver.chronomatron", true);
            restored.qolUtilities.writeColor("qol.experiment_solver.first_color", 0x80ABCDEF);
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(restored));
            assertTrue(saved.qolUtilities.readBoolean("qol.experiment_solver.chronomatron"));
            assertEquals(0x80ABCDEF,
                    saved.qolUtilities.readColor("qol.experiment_solver.first_color"));
            saved.qolUtilities.resetModuleToDefaults("qol.experiment_solver");
            assertFalse(saved.qolUtilities.isModuleEnabled("qol.experiment_solver"));
            assertTrue(saved.qolUtilities.readBoolean("qol.experiment_solver.chronomatron"));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
