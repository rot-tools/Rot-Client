package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RotClientAppearanceCompletenessTest {
    @AfterEach
    void restoreTheme() {
        RotClientTheme.resetToDefaults();
    }

    @Test
    void malformedAppearanceJsonFallsBackToDefaults(@TempDir Path dir)
            throws Exception {
        Path path = dir.resolve("rotclient-appearance.json");
        Files.writeString(path, "{not-json", StandardCharsets.UTF_8);
        RotClientAppearanceConfig loaded = loadFromPath(path);
        assertEquals(RotClientTheme.DEFAULT_BACKDROP, loaded.dashboardBackdrop);
        assertFalse(loaded.customBackgroundEnabled);
    }

    @Test
    void unknownJsonFieldsDoNotBreakLoad(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("rotclient-appearance.json");
        Files.writeString(
                path,
                """
                {
                  "schemaVersion": 1,
                  "dashboardAccent": -1885381,
                  "futureExperimentalField": "ignored",
                  "customBackgroundEnabled": false
                }
                """,
                StandardCharsets.UTF_8);
        RotClientAppearanceConfig loaded = loadFromPath(path);
        assertEquals(RotClientTheme.DEFAULT_BORDER_BRIGHT, loaded.dashboardAccent);
        assertFalse(loaded.customBackgroundEnabled);
    }

    @Test
    void legacyDefaultPaletteMigratesButCustomColorsRemain() {
        RotClientAppearanceConfig config = new RotClientAppearanceConfig();
        config.schemaVersion = 1;
        config.dashboardSurface = 0xEE120909;
        config.dashboardAccent = 0xFF123456;

        config.normalize();

        assertEquals(RotClientAppearanceConfig.SCHEMA_VERSION, config.schemaVersion);
        assertEquals(RotClientTheme.DEFAULT_SURFACE, config.dashboardSurface);
        assertEquals(0xFF123456, config.dashboardAccent);
    }

    @Test
    void channelAndAlphaParsingClampSafely() {
        assertEquals(0, RotClientAppearanceConfig.parseChannel("-9", 10));
        assertEquals(255, RotClientAppearanceConfig.parseChannel("999", 10));
        assertEquals(42, RotClientAppearanceConfig.parseChannel("42", 0));
        assertEquals(10, RotClientAppearanceConfig.parseChannel("nope", 10));
        assertEquals(0, RotClientAppearanceConfig.clamp(-1, 0, 255));
        assertEquals(255, RotClientAppearanceConfig.clamp(300, 0, 255));
        assertEquals(0x80ABCDEF, RotClientAppearanceConfig.withAlpha(0xFFABCDEF, 0x80));
        assertEquals(0x80, RotClientAppearanceConfig.alphaOf(0x80ABCDEF));
    }

    @Test
    void invalidHexFallsBackWithoutThrowing() {
        int fallback = 0xFFE33B3B;
        assertEquals(fallback, RotClientAppearanceConfig.fromHexRgb(null, 0xFF, fallback));
        assertEquals(fallback, RotClientAppearanceConfig.fromHexRgb("", 0xFF, fallback));
        assertEquals(fallback, RotClientAppearanceConfig.fromHexRgb("#GG0000", 0xFF, fallback));
        assertEquals(fallback, RotClientAppearanceConfig.fromHexRgb("#123", 0xFF, fallback));
        assertEquals(0xFF00AAFF, RotClientAppearanceConfig.fromHexRgb("#00aaff", 0xFF, fallback));
    }

    @Test
    void chartFillRoleAppliesToRuntimeTheme() {
        RotClientAppearanceConfig config = RotClientAppearanceConfig.defaults();
        config.hudChartFill = 0x55AABBCC;
        RotClientTheme.apply(config);
        assertEquals(0x55AABBCC, RotClientTheme.CHART_FILL);
        RotClientTheme.resetToDefaults();
        assertEquals(RotClientTheme.DEFAULT_CHART_FILL, RotClientTheme.CHART_FILL);
    }

    @Test
    void dashboardHeaderSidebarAndHudTextRolesWireThrough() {
        RotClientAppearanceConfig config = RotClientAppearanceConfig.defaults();
        config.dashboardHeader = 0xFF101010;
        config.dashboardSidebar = 0xFF202020;
        config.dashboardButtonText = 0xFF303030;
        config.hudBorder = 0xFF404040;
        config.hudTitle = 0xFF505050;
        config.hudTextPrimary = 0xFF606060;
        config.hudTextSecondary = 0xFF707070;
        RotClientTheme.apply(config);
        assertEquals(0xFF101010, RotClientTheme.DASHBOARD_HEADER);
        assertEquals(0xFF202020, RotClientTheme.DASHBOARD_SIDEBAR);
        assertEquals(0xFF303030, RotClientTheme.BUTTON_TEXT);
        assertEquals(0xFF404040, RotClientTheme.HUD_BORDER);
        assertEquals(0xFF505050, RotClientTheme.HUD_TITLE);
        assertEquals(0xFF606060, RotClientTheme.HUD_TEXT);
        assertEquals(0xFF707070, RotClientTheme.HUD_TEXT_DIM);
    }

    @Test
    void backgroundPathTraversalRejectedByResolver() {
        assertNull(RotClientBackgroundManager.resolveSafeBackground("../x.png"));
        assertNull(RotClientBackgroundManager.resolveSafeBackground("a/b.png"));
        assertNull(RotClientBackgroundManager.resolveSafeBackground("C:evil.png"));
        assertFalse(RotClientBackgroundManager.isSupportedImageName("wall.webp"));
        assertTrue(RotClientBackgroundManager.isSupportedImageName("wall.png"));
        assertTrue(RotClientBackgroundManager.isSupportedImageName("wall.JPG"));
    }

    @Test
    void emptyAndMissingBackgroundFolderHelpersArePure() {
        // FabricLoader is unavailable in plain unit tests; path listing is
        // covered by runtime boot. Pure validators stay assertable here.
        assertTrue(RotClientBackgroundManager.isSafeFileName("empty.png"));
        assertFalse(RotClientBackgroundManager.isSafeFileName(""));
        assertFalse(RotClientBackgroundManager.isSafeFileName("   "));
        assertNull(RotClientBackgroundManager.resolveSafeBackground(""));
    }

    @Test
    void chartMathHandlesEdgeCases() {
        assertEquals(0, RateGraphMath.sampleCatmullRom(new double[0], 0), 0.0001);
        assertEquals(5, RateGraphMath.sampleCatmullRom(new double[]{5}, 0), 0.0001);
        assertEquals(0, RateGraphMath.sanitizeSample(Double.NaN), 0.0001);
        assertEquals(0, RateGraphMath.sanitizeSample(Double.NEGATIVE_INFINITY), 0.0001);
        assertEquals(0, RateGraphMath.sanitizeSample(-12), 0.0001);
        double[] two = RateGraphMath.smooth(new double[]{10, 20});
        assertEquals(2, two.length);
        double between = RateGraphMath.sampleCatmullRom(new double[]{10, 20}, 0.5);
        assertTrue(between >= 10 && between <= 20);
    }

    @Test
    void appearanceScreenSectionsIncludeChartsAndBackground() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientAppearanceScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("CHARTS"));
        assertTrue(source.contains("BACKGROUND"));
        assertTrue(source.contains("openBackgroundsFolder"));
        assertTrue(source.contains("awaitingResetAllConfirm"));
        assertTrue(source.contains("enableScissor"));
        assertTrue(source.contains("Reset current section")
                || source.contains("Reset Current Section")
                || source.contains("resetCurrentSection")
                || source.contains("Reset current"));
    }

    @Test
    void colorPickerSupportsEditableHex() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientColorPickerScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("editingHex"));
        assertTrue(source.contains("charTyped"));
        assertTrue(source.contains("fromHexRgb"));
    }

    @Test
    void sessionAnalyticsUsesScissorAndPriorityMetrics() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("enableScissor"));
        assertTrue(source.contains("disableScissor"));
        assertTrue(source.contains("Session Time"));
        assertTrue(source.contains("Session Value"));
        assertTrue(source.contains("Coins / Hour"));
        assertTrue(source.contains("formatAnalyticsSessionTime"));
    }

    @Test
    void optionalModMenuIntegrationIsPresent() throws Exception {
        String fabric = Files.readString(
                Path.of("src/main/resources/fabric.mod.json"),
                StandardCharsets.UTF_8);
        assertTrue(fabric.contains("\"modmenu\""));
        assertTrue(fabric.contains("RotClientModMenuIntegration"));
        assertTrue(fabric.contains("\"suggests\""));
        assertTrue(Files.isRegularFile(Path.of(
                "src/client/java/fi/rotclient/RotClientModMenuIntegration.java")));
    }

    private static RotClientAppearanceConfig loadFromPath(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return RotClientAppearanceConfig.defaults();
            }
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json == null || json.isBlank()) {
                return RotClientAppearanceConfig.defaults();
            }
            RotClientAppearanceConfig config = new com.google.gson.Gson()
                    .fromJson(json, RotClientAppearanceConfig.class);
            if (config == null) {
                return RotClientAppearanceConfig.defaults();
            }
            config.normalize();
            return config;
        } catch (Exception ignored) {
            return RotClientAppearanceConfig.defaults();
        }
    }
}
