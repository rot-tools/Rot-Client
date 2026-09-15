package fi.rotclient;

import com.google.gson.JsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PlusDianaMigrationTest {
    @Test
    void oldNestedSettingsRemainEditableAndResettableInPlus() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject extras = legacy.getAsJsonObject("qolUtilities").getAsJsonObject("extras");
            extras.addProperty("dianaBurrowsEnabled", true);
            extras.addProperty("dianaBurrowsGuess", false);
            extras.addProperty("dianaBurrowsStartColor", 0xFF123456);
            extras.addProperty("dianaShareEnabled", true);
            extras.addProperty("dianaShareParty", true);

            QolUtilityConfig config = TrackerStore.fromJson(legacy).qolUtilities;
            assertTrue(config.isModuleEnabled("qol.diana_burrows"));
            assertFalse(config.readBoolean("qol.diana_burrows.guess"));
            assertEquals(0xFF123456, config.readColor("qol.diana_burrows.start_color"));
            assertTrue(config.isModuleEnabled("qol.diana_share"));
            assertTrue(config.readBoolean("qol.diana_share.party"));

            config.writeBoolean("qol.diana_burrows.guess", true);
            config.writeColor("qol.diana_burrows.start_color", 0xFFABCDEF);
            assertTrue(config.readBoolean("qol.diana_burrows.guess"));
            assertEquals(0xFFABCDEF, config.readColor("qol.diana_burrows.start_color"));

            config.resetModuleToDefaults("qol.diana_burrows");
            assertFalse(config.isModuleEnabled("qol.diana_burrows"));
            assertTrue(config.readBoolean("qol.diana_burrows.guess"));
            assertEquals(0xFF55FFFF, config.readColor("qol.diana_burrows.start_color"));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
