package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AutoClickerSettingsMigrationTest {
    @Test
    void oldRootFieldsRemainUsableAfterLoadingAndSavingInPlus() {
        JsonObject oldConfig = TrackerStore.toJson(new TrackerConfig());
        JsonObject qol = oldConfig.getAsJsonObject("qolUtilities");
        qol.addProperty("autoClickerEnabled", true);
        qol.addProperty("autoClickerCpsHudEnabled", true);
        qol.addProperty("autoClickerCps", 9.5F);
        qol.add("autoClickerLeftWhitelist", JsonParser.parseString("[\"Terminator\"]"));
        qol.addProperty("autoClickerHudX", 74.0F);
        qol.addProperty("autoClickerHudY", 124.0F);

        TrackerConfig config = TrackerStore.fromJson(oldConfig);
        QolUtilityConfig loaded = config.qolUtilities;
        AutoClickerSettings settings = AutoClickerSettings.from(loaded);
        assertTrue(settings.enabled());
        assertTrue(settings.cpsHudEnabled());
        assertEquals(9.5F, settings.cps());
        assertEquals("Terminator", settings.leftWhitelist().getFirst());
        assertEquals(74.0F, loaded.pose("auto_clicker")[0]);

        loaded.setPose("auto_clicker", 88.0F, 140.0F);
        AutoClickerSettings.leftWhitelist(loaded, java.util.List.of("Juju"));
        JsonObject saved = TrackerStore.toJson(config);
        assertEquals("Juju", saved.getAsJsonObject("qolUtilities")
                .getAsJsonObject("extensionFields")
                .getAsJsonArray("autoClickerLeftWhitelist").get(0).getAsString());
        assertEquals(88.0F, loaded.pose("auto_clicker")[0]);
        assertEquals("Juju", AutoClickerSettings.from(loaded).leftWhitelist().getFirst());
        AutoClickerSettings.reset(loaded);
        assertFalse(AutoClickerSettings.from(loaded).enabled());
        assertEquals(5.0F, AutoClickerSettings.from(loaded).cps());
    }
}
