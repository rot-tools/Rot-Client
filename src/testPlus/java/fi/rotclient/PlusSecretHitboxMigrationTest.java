package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlusSecretHitboxMigrationTest {
    @Test
    void plusRestoresMissingDefaultsButKeepsExplicitOffChoices() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject qol = legacy.getAsJsonObject("qolUtilities");
            qol.remove("secretHitboxesLever");
            qol.remove("secretHitboxesButton");
            qol.remove("secretHitboxesSkull");
            qol.addProperty("secretHitboxesChests", false);

            TrackerConfig restored = TrackerStore.fromJson(legacy);
            assertTrue(restored.qolUtilities.secretHitboxesLever);
            assertTrue(restored.qolUtilities.secretHitboxesButton);
            assertTrue(restored.qolUtilities.secretHitboxesSkull);
            assertFalse(restored.qolUtilities.secretHitboxesChests);
            assertFalse(qol.has("secretHitboxesLever"));
            assertFalse(qol.has("secretHitboxesButton"));
            assertFalse(qol.has("secretHitboxesSkull"));

            TrackerConfig explicitOff = new TrackerConfig();
            explicitOff.qolUtilities.secretHitboxesLever = false;
            explicitOff.qolUtilities.secretHitboxesButton = false;
            explicitOff.qolUtilities.secretHitboxesSkull = false;
            TrackerConfig kept = TrackerStore.fromJson(TrackerStore.toJson(explicitOff));
            assertFalse(kept.qolUtilities.secretHitboxesLever);
            assertFalse(kept.qolUtilities.secretHitboxesButton);
            assertFalse(kept.qolUtilities.secretHitboxesSkull);
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
