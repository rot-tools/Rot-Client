package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PlusTermSimMigrationTest {
    @Test
    void legacySimulatorSettingsSurviveLiteRoundTripAndRemainEditableInPlus() {
        JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
        JsonObject extras = legacy.getAsJsonObject("qolUtilities").getAsJsonObject("extras");
        extras.addProperty("dungeonTermSimEnabled", true);
        extras.addProperty("dungeonTermSimKeybind", "G");
        extras.addProperty("dungeonTermSimPing", 150);
        extras.addProperty("dungeonTermSimShowPbs", false);
        extras.addProperty("dungeonTermSimPbs", "PANES=2100");
        extras.getAsJsonObject("athen").addProperty("termSimIp", "example.org");

        QolFlavorExtension previous = QolFlavorSupport.extension();
        try {
            QolFlavorSupport.install(QolFlavorExtension.NONE);
            JsonObject liteSaved = TrackerStore.toJson(TrackerStore.fromJson(legacy));
            assertTrue(liteSaved.toString().contains("dungeonTermSimPbs"));
            assertTrue(liteSaved.toString().contains("termSimIp"));

            QolFlavorSupport.install(new RotClientPlusExtension());
            QolUtilityConfig plus = TrackerStore.fromJson(liteSaved).qolUtilities;
            assertTrue(plus.isModuleEnabled("qol.dungeon_termsim"));
            assertEquals("G", plus.readKeybind("qol.dungeon_termsim.keybind"));
            assertEquals(150.0D, plus.readNumber("qol.dungeon_termsim.ping"));
            assertFalse(plus.readBoolean("qol.dungeon_termsim.show_pbs"));
            assertEquals("example.org", plus.readText("qol.dungeon_termsim.ip"));
            assertEquals("PANES=2100", TermSimSettings.from(plus).pbs());

            assertTrue(plus.writeText("qol.dungeon_termsim.ip", "another.org"));
            assertTrue(plus.writeNumber("qol.dungeon_termsim.ping", 200.0D));
            assertEquals("another.org", plus.readText("qol.dungeon_termsim.ip"));
            assertEquals(200.0D, plus.readNumber("qol.dungeon_termsim.ping"));
            plus.resetModuleToDefaults("qol.dungeon_termsim");
            assertFalse(plus.isModuleEnabled("qol.dungeon_termsim"));
            assertEquals(0.0D, plus.readNumber("qol.dungeon_termsim.ping"));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
