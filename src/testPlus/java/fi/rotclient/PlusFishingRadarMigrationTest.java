package fi.rotclient;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PlusFishingRadarMigrationTest {
    @Test
    void oldRadarSettingsStayOpaqueInLiteAndEditableInPlus() {
        JsonObject legacy = TrackerStore.toJson(new TrackerConfig());
        JsonObject extras = legacy.getAsJsonObject("qolUtilities").getAsJsonObject("extras");
        extras.addProperty("fishingHotspotsRadar", true);
        extras.addProperty("fishingHotspotsTracer", false);

        QolFlavorExtension previous = QolFlavorSupport.extension();
        try {
            QolFlavorSupport.install(QolFlavorExtension.NONE);
            JsonObject liteSaved = TrackerStore.toJson(TrackerStore.fromJson(legacy));
            assertTrue(liteSaved.toString().contains("fishingHotspotsRadar"));
            assertTrue(liteSaved.toString().contains("fishingHotspotsTracer"));

            QolFlavorSupport.install(new RotClientPlusExtension());
            QolUtilityConfig plus = TrackerStore.fromJson(liteSaved).qolUtilities;
            assertTrue(plus.readBoolean("qol.fishing_hotspots.radar"));
            assertFalse(plus.readBoolean("qol.fishing_hotspots.tracer"));
            plus.writeBoolean("qol.fishing_hotspots.tracer", true);
            assertTrue(plus.readBoolean("qol.fishing_hotspots.tracer"));
            plus.resetModuleToDefaults("qol.fishing_hotspots");
            assertFalse(plus.readBoolean("qol.fishing_hotspots.radar"));
            assertTrue(plus.readBoolean("qol.fishing_hotspots.tracer"));
        } finally {
            QolFlavorSupport.install(previous);
        }
    }
}
