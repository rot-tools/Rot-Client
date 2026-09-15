package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusEtherwarpDepthMigrationTest {
    @Test
    void legacyDepthOffSurvivesInPlusAndResetRestoresOcclusion() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            legacy.getAsJsonObject("qolUtilities").addProperty("etherwarpDepth", false);
            TrackerConfig restored = TrackerStore.fromJson(legacy);
            assertFalse(restored.qolUtilities.readBoolean("qol.etherwarp.depth"));
            assertFalse(PlusOpaqueSettings.bool(restored.qolUtilities, "etherwarpDepth", true));
            TrackerConfig saved = TrackerStore.fromJson(TrackerStore.toJson(restored));
            assertFalse(saved.qolUtilities.readBoolean("qol.etherwarp.depth"));
            saved.qolUtilities.resetModuleToDefaults("qol.etherwarp");
            assertTrue(saved.qolUtilities.readBoolean("qol.etherwarp.depth"));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
