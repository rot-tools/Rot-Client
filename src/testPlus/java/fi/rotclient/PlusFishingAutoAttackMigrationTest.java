package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PlusFishingAutoAttackMigrationTest {
    @Test
    void oldAttackSettingsStayOpaqueInLiteAndEditableInPlus() {
        JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
        JsonObject extras = legacy.getAsJsonObject("qolUtilities").getAsJsonObject("extras");
        extras.addProperty("fishingCreaturesAutoAttack", true);
        extras.addProperty("fishingCreaturesAutoDelay", 12);

        QolFlavorExtension previous = QolFlavorSupport.extension();
        try {
            QolFlavorSupport.install(QolFlavorExtension.NONE);
            JsonObject liteSaved = TrackerStore.toJson(TrackerStore.fromJson(legacy));
            assertTrue(liteSaved.toString().contains("fishingCreaturesAutoAttack"));
            assertTrue(liteSaved.toString().contains("fishingCreaturesAutoDelay"));

            QolFlavorSupport.install(new RotClientPlusExtension());
            QolUtilityConfig plus = TrackerStore.fromJson(liteSaved).qolUtilities;
            assertTrue(plus.readBoolean("qol.fishing_creatures.auto_attack"));
            assertEquals(12.0D, plus.readNumber("qol.fishing_creatures.auto_delay"));
            plus.writeBoolean("qol.fishing_creatures.auto_attack", false);
            assertTrue(plus.writeNumber("qol.fishing_creatures.auto_delay", 8.0D));
            assertFalse(plus.readBoolean("qol.fishing_creatures.auto_attack"));
            assertEquals(8.0D, plus.readNumber("qol.fishing_creatures.auto_delay"));
            plus.resetModuleToDefaults("qol.fishing_creatures");
            assertFalse(plus.readBoolean("qol.fishing_creatures.auto_attack"));
            assertEquals(4.0D, plus.readNumber("qol.fishing_creatures.auto_delay"));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
