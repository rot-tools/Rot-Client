package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FullbrightNightWiringTest {
    @Test
    void catalogRenamesTheLightingCardAndAddsExclusiveSettings() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.fullbright");
        assertNotNull(module);
        assertEquals("Fullbright and Night", module.name());
        assertEquals(QolUtilityCatalog.Group.RENDER, module.group());
        assertTrue(module.runtimeReady());
        assertEquals("qol.fullbright", QolUtilityCatalog.findById(
                FullbrightNightPolicy.ALWAYS_NIGHT).id());
        boolean sawFullbright = false;
        boolean sawNight = false;
        boolean sawForce = false;
        for (QolUtilityCatalog.SettingDef setting : module.settings()) {
            if (FullbrightNightPolicy.USE_FULLBRIGHT.equals(setting.id())) {
                sawFullbright = setting.type() == QolUtilityCatalog.SettingType.TOGGLE;
            }
            if (FullbrightNightPolicy.ALWAYS_NIGHT.equals(setting.id())) {
                sawNight = setting.type() == QolUtilityCatalog.SettingType.TOGGLE;
            }
            if (FullbrightNightPolicy.FORCE_BOTH.equals(setting.id())) {
                sawForce = setting.type() == QolUtilityCatalog.SettingType.SQUARE;
            }
        }
        assertTrue(sawFullbright);
        assertTrue(sawNight);
        assertTrue(sawForce);
    }

    @Test
    void mixinsStayVisualAndDoNotGateOnSkyBlock() throws Exception {
        String sky = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/SkyRendererExtractMixin.java"),
                StandardCharsets.UTF_8);
        String light = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LightmapRenderStateExtractorMixin.java"),
                StandardCharsets.UTF_8);
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/FullbrightNightRuntime.java"),
                StandardCharsets.UTF_8);
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("SkyRendererExtractMixin"));
        assertTrue(sky.contains("extractRenderState"));
        assertTrue(sky.contains("FullbrightNightRuntime.applyNightSky"));
        assertTrue(light.contains("isFullbrightEnabled"));
        assertTrue(light.contains("applyNightLightmap"));
        assertTrue(runtime.contains("onAlwaysNightChanged"));
        assertTrue(runtime.contains("decideNightApply"));
        assertFalse(sky.contains("SkyBlockAreaDetector"));
        assertFalse(runtime.contains("SkyBlockAreaDetector"));
        assertFalse(runtime.contains("/time"));
    }
}
