package fi.rotclient.rebuild;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HudPlacementTest {
    @TempDir Path folder;

    @Test
    void existingVisibilitySurvivesAnUpgradeAndPlacementDefaultsToVanilla() throws Exception {
        Path config = folder.resolve("interface.json");
        Files.writeString(config, """
                {"hiddenVanillaLayers":["scoreboard"]}
                """);
        InterfaceSettings settings = InterfaceSettings.load(config);
        assertTrue(settings.hiddenVanillaLayers.contains("scoreboard"));
        assertFalse(settings.hiddenVanillaLayers.contains("hotbar"));
        assertEquals(HudPlacement.DEFAULT, settings.vanillaHudPlacements.getOrDefault("hotbar", HudPlacement.DEFAULT));

        var modified = HudPlacement.DEFAULT.move(28, -16).resize(0.25);
        settings.vanillaHudPlacements.put("hotbar", modified);
        settings.save();
        InterfaceSettings loaded = InterfaceSettings.load(config);
        assertEquals(modified, loaded.vanillaHudPlacements.get("hotbar"));
        assertEquals(settings.hiddenVanillaLayers, loaded.hiddenVanillaLayers);
    }

    @Test
    void zoomAndMovementPreserveEachOthersValues() {
        var shifted = HudPlacement.DEFAULT.move(32, -16);
        assertEquals(new HudPlacement(32, -16, 1.05), shifted.resize(0.05));
        assertEquals(new HudPlacement(34, -19, 1.05), shifted.resize(0.05).move(2, -3));
        assertEquals(2.0, shifted.resize(10).scale());
        assertEquals(0.5, shifted.resize(-10).scale());
    }
}
