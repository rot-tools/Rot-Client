package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class BlockOutlineWiringTest {
    private static final String MODULE = "qol.block_outline";

    @Test
    void catalogListsTheModuleWithModeColorAndRainbowControls() {
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(MODULE);
        assertNotNull(module);
        assertEquals("Block Outline", module.name());
        assertEquals(QolUtilityCatalog.Group.RENDER, module.group());
        assertTrue(module.toggleable());
        assertTrue(module.runtimeReady());
        assertFalse(module.wip());
        assertEquals(QolUtilityCatalog.SettingType.ENUM, type(module, "qol.block_outline.mode"));
        assertEquals(QolUtilityCatalog.SettingType.COLOR, type(module, "qol.block_outline.color"));
        assertEquals(QolUtilityCatalog.SettingType.NUMBER, type(module, "qol.block_outline.width"));
        assertEquals(QolUtilityCatalog.SettingType.NUMBER,
                type(module, "qol.block_outline.rainbow_speed"));
        assertEquals(QolUtilityCatalog.SettingType.NUMBER,
                type(module, "qol.block_outline.rainbow_spread"));
        QolUtilityCatalog.SettingDef mode = module.settings().stream()
                .filter(setting -> "qol.block_outline.mode".equals(setting.id()))
                .findFirst()
                .orElseThrow();
        assertEquals(
                java.util.List.of(BlockOutlinePolicy.MODE_SOLID, BlockOutlinePolicy.MODE_RAINBOW),
                mode.enumOptions());
    }

    @Test
    void moduleIsOffByDefaultAndNumberSpecsMatchThePolicyBounds() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertFalse(config.isModuleEnabled(MODULE));
        assertEquals(BlockOutlinePolicy.MODE_SOLID, config.readEnum("qol.block_outline.mode"));
        assertEquals(BlockOutlinePolicy.DEFAULT_COLOR, config.readColor("qol.block_outline.color"));
        assertSpec("qol.block_outline.width",
                BlockOutlinePolicy.MIN_WIDTH, BlockOutlinePolicy.MAX_WIDTH);
        assertSpec("qol.block_outline.rainbow_speed",
                BlockOutlinePolicy.MIN_SPEED, BlockOutlinePolicy.MAX_SPEED);
        assertSpec("qol.block_outline.rainbow_spread",
                BlockOutlinePolicy.MIN_SPREAD, BlockOutlinePolicy.MAX_SPREAD);
    }

    @Test
    void settingsReadWriteClampAndResetThroughTheConfig() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled(MODULE, true);
        assertTrue(config.isModuleEnabled(MODULE));
        assertTrue(config.writeEnum("qol.block_outline.mode", "Rainbow"));
        assertEquals("Rainbow", config.readEnum("qol.block_outline.mode"));
        assertTrue(config.writeEnum("qol.block_outline.mode", "garbage"));
        assertEquals(BlockOutlinePolicy.MODE_SOLID, config.readEnum("qol.block_outline.mode"));
        assertTrue(config.writeColor("qol.block_outline.color", 0x80FF00AA));
        assertEquals(0x80FF00AA, config.readColor("qol.block_outline.color"));
        assertTrue(config.writeNumber("qol.block_outline.width", 99.0D));
        assertEquals(BlockOutlinePolicy.MAX_WIDTH, config.readNumber("qol.block_outline.width"));
        assertTrue(config.writeNumber("qol.block_outline.rainbow_speed", 2.5D));
        assertEquals(2.5D, config.readNumber("qol.block_outline.rainbow_speed"));
        assertTrue(config.writeNumber("qol.block_outline.rainbow_spread", -4.0D));
        assertEquals(BlockOutlinePolicy.MIN_SPREAD,
                config.readNumber("qol.block_outline.rainbow_spread"));

        assertTrue(config.resetModuleToDefaults(MODULE));
        assertFalse(config.isModuleEnabled(MODULE));
        assertEquals(BlockOutlinePolicy.DEFAULT_COLOR, config.readColor("qol.block_outline.color"));
        assertEquals(BlockOutlinePolicy.DEFAULT_WIDTH, config.readNumber("qol.block_outline.width"));
        assertEquals(BlockOutlinePolicy.DEFAULT_SPEED,
                config.readNumber("qol.block_outline.rainbow_speed"));
        assertEquals(BlockOutlinePolicy.DEFAULT_SPREAD,
                config.readNumber("qol.block_outline.rainbow_spread"));
    }

    @Test
    void settingsSurviveASaveAndLoad() {
        TrackerConfig config = new TrackerConfig();
        config.qolUtilities.setModuleEnabled(MODULE, true);
        config.qolUtilities.writeEnum("qol.block_outline.mode", "Rainbow");
        config.qolUtilities.writeColor("qol.block_outline.color", 0xCC123456);
        config.qolUtilities.writeNumber("qol.block_outline.width", 2.5D);
        config.qolUtilities.writeNumber("qol.block_outline.rainbow_speed", 3.0D);
        config.qolUtilities.writeNumber("qol.block_outline.rainbow_spread", 0.5D);

        JsonObject saved = TrackerStore.toJson(config);
        QolUtilityConfig loaded = TrackerStore.fromJson(saved).qolUtilities;

        assertTrue(loaded.isModuleEnabled(MODULE));
        assertEquals("Rainbow", loaded.readEnum("qol.block_outline.mode"));
        assertEquals(0xCC123456, loaded.readColor("qol.block_outline.color"));
        assertEquals(2.5D, loaded.readNumber("qol.block_outline.width"));
        assertEquals(3.0D, loaded.readNumber("qol.block_outline.rainbow_speed"));
        assertEquals(0.5D, loaded.readNumber("qol.block_outline.rainbow_spread"));
    }

    @Test
    void mixinIsRegisteredAndOnlyReplacesTheEdgeLoop() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"), StandardCharsets.UTF_8);
        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ShapeOutlineFeatureRendererMixin.java"),
                StandardCharsets.UTF_8);
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/BlockOutlineRuntime.java"), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"ShapeOutlineFeatureRendererMixin\""));
        assertTrue(mixin.contains("@WrapOperation"));
        assertTrue(mixin.contains("forAllEdges"));
        assertTrue(mixin.contains("original.call(shape, vanillaEdges)"));
        assertTrue(mixin.contains("BlockOutlineRuntime.draw"));
        // Vanilla stays in charge when the module is off, and the high-contrast pass is untouched.
        assertTrue(runtime.contains("blockOutlineEnabled"));
        assertTrue(runtime.contains("RenderTypes.lines()"));
        assertFalse(mixin.contains("SkyBlockAreaDetector"));
        assertFalse(runtime.contains("SkyBlockAreaDetector"));
    }

    private static QolUtilityCatalog.SettingType type(
            QolUtilityCatalog.ModuleDef module, String settingId) {
        return module.settings().stream()
                .filter(setting -> settingId.equals(setting.id()))
                .findFirst()
                .orElseThrow()
                .type();
    }

    private static void assertSpec(String settingId, double min, double max) {
        QolNumberSettings.Spec spec = QolNumberSettings.spec(settingId);
        assertNotNull(spec, settingId);
        assertEquals(min, spec.min());
        assertEquals(max, spec.max());
    }
}
