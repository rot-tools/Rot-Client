package fi.rotclient;

import com.google.gson.JsonObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

final class PlusDungeonTerminalClickMigrationTest {
    @Test
    void oldAthenFieldsMigrateThroughPlusRoutes() {
        QolFlavorExtension previous = QolFlavorSupport.extension();
        QolFlavorSupport.install(new RotClientPlusExtension());
        try {
            JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
            JsonObject athen = legacy.getAsJsonObject("qolUtilities")
                    .getAsJsonObject("extras").getAsJsonObject("athen");
            athen.addProperty("termClickEnabled", true);
            athen.addProperty("termClickRadius", 9);
            athen.addProperty("termClickLeftColor", 0xFF123456);

            QolUtilityConfig config = TrackerStore.fromJson(legacy).qolUtilities;
            assertTrue(config.isModuleEnabled("qol.dungeon_term_click"));
            assertEquals(9.0D, config.readNumber("qol.dungeon_term_click.radius"));
            assertEquals(0xFF123456, config.readColor("qol.dungeon_term_click.left_color"));

            assertTrue(config.writeNumber("qol.dungeon_term_click.radius", 16.0D));
            assertTrue(config.writeColor("qol.dungeon_term_click.left_color", 0xFFABCDEF));
            assertEquals(16.0D, config.readNumber("qol.dungeon_term_click.radius"));
            assertEquals(0xFFABCDEF, config.readColor("qol.dungeon_term_click.left_color"));

            config.resetModuleToDefaults("qol.dungeon_term_click");
            assertFalse(config.isModuleEnabled("qol.dungeon_term_click"));
            assertEquals(4.0D, config.readNumber("qol.dungeon_term_click.radius"));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
