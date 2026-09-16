package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CheaterWardrobeSettingsMigrationTest {
    @Test
    void oldExtrasFieldsRemainUsableAfterLoadingAndSavingInPlus() {
        JsonObject oldConfig = TrackerStore.toJson(new TrackerConfig());
        JsonObject extras = oldConfig.getAsJsonObject("qolUtilities").getAsJsonObject("extras");
        extras.addProperty("cheaterWardrobeEnabled", true);
        extras.addProperty("cheaterWardrobeStationaryOnly", false);
        extras.addProperty("cheaterWardrobeResetOpen", false);
        extras.addProperty("cheaterWardrobeClickDelay", 4);
        extras.addProperty("cheaterWardrobeCloseDelay", 5);
        extras.addProperty("cheaterWardrobeDelayVariance", 2);
        extras.addProperty("cheaterWardrobeSlot1", "R");

        TrackerConfig config = TrackerStore.fromJson(oldConfig);
        QolUtilityConfig qol = config.qolUtilities;
        CheaterWardrobeSettings settings = CheaterWardrobeSettings.from(qol);
        assertTrue(settings.enabled());
        assertFalse(settings.stationaryOnly());
        assertFalse(settings.resetOpen());
        assertEquals(4, settings.clickDelay());
        assertEquals(5, settings.closeDelay());
        assertEquals(2, settings.delayVariance());
        assertEquals("R", settings.slotBinds()[0]);

        qol.writeBoolean("qol.cheater_wardrobe.stationary_only", true);
        assertTrue(qol.writeNumber("qol.cheater_wardrobe.click_delay", 7));
        assertTrue(qol.writeKeybind("qol.cheater_wardrobe.slot_1", "G"));

        JsonObject savedFields = TrackerStore.toJson(config)
                .getAsJsonObject("qolUtilities").getAsJsonObject("extras")
                .getAsJsonObject("extensionFields");
        assertTrue(savedFields.get("cheaterWardrobeStationaryOnly").getAsBoolean());
        assertEquals(7, savedFields.get("cheaterWardrobeClickDelay").getAsInt());
        assertEquals("G", savedFields.get("cheaterWardrobeSlot1").getAsString());

        CheaterWardrobeSettings.reset(qol);
        assertFalse(CheaterWardrobeSettings.from(qol).enabled());
        assertEquals(1, CheaterWardrobeSettings.from(qol).clickDelay());
        assertEquals("", CheaterWardrobeSettings.from(qol).slotBinds()[0]);
    }
}
