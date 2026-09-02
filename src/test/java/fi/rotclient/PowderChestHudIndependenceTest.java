package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PowderChestHudIndependenceTest {
    @Test
    void powderHudHasIndependentPersistedPose() {
        TrackerConfig config = new TrackerConfig();
        config.x = 21.0F;
        config.y = 31.0F;
        config.scale = 1.4F;
        config.powderChestHudX = 401.0F;
        config.powderChestHudY = 51.0F;
        config.powderChestHudScale = 0.8F;

        JsonObject json = TrackerStore.toJson(config);
        TrackerConfig loaded = TrackerStore.fromJson(json);

        assertEquals(21.0F, loaded.x);
        assertEquals(401.0F, loaded.powderChestHudX);
        assertEquals(51.0F, loaded.powderChestHudY);
        assertEquals(0.8F, loaded.powderChestHudScale);
        assertNotEquals(loaded.x, loaded.powderChestHudX);
    }

    @Test
    void powderHudIsRegisteredAndEditableAsItsOwnElement()
            throws Exception {
        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        String editor = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientScreen.java"),
                StandardCharsets.UTF_8);

        assertTrue(client.contains("rotclient\", \"powder_chest_tracker"));
        assertTrue(client.contains("POWDER_CHEST_HUD.render"));
        assertTrue(editor.contains("POWDER_CHEST_HUD"));
        assertTrue(editor.contains("powderHud.beginDrag"));
    }
}
