package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RotClientAppearanceConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsMatchBrandThemeAndNormalizeSafely() {
        RotClientAppearanceConfig config = RotClientAppearanceConfig.defaults();
        config.normalize();
        assertEquals(RotClientTheme.DEFAULT_BACKDROP, config.dashboardBackdrop);
        assertEquals(RotClientTheme.DEFAULT_BORDER_BRIGHT, config.dashboardAccent);
        assertEquals(RotClientTheme.DEFAULT_HUD_BACKGROUND, config.hudBackground);
        assertFalse(config.customBackgroundEnabled);
        assertEquals("fill", config.customBackgroundFit);
    }

    @Test
    void applyUpdatesRuntimeThemeAndResetRestoresDefaults() {
        RotClientAppearanceConfig config = RotClientAppearanceConfig.defaults();
        config.dashboardBackdrop = 0xFF112233;
        config.dashboardAccent = 0xFFAABBCC;
        config.hudAccent = 0xFF445566;
        RotClientTheme.apply(config);
        assertEquals(0xFF112233, RotClientTheme.BACKDROP);
        assertEquals(0xFFAABBCC, RotClientTheme.BORDER_BRIGHT);
        assertEquals(0xFF445566, RotClientTheme.HUD_ACCENT);
        RotClientTheme.resetToDefaults();
        assertEquals(RotClientTheme.DEFAULT_BACKDROP, RotClientTheme.BACKDROP);
        assertEquals(RotClientTheme.DEFAULT_BORDER_BRIGHT, RotClientTheme.BORDER_BRIGHT);
    }

    @Test
    void rejectsUnsafeBackgroundPaths() {
        RotClientAppearanceConfig config = RotClientAppearanceConfig.defaults();
        config.customBackgroundEnabled = true;
        config.customBackgroundFile = "../secret.png";
        config.normalize();
        assertEquals("", config.customBackgroundFile);
        assertFalse(config.customBackgroundEnabled);
    }

    @Test
    void hexRoundTripAndColorMathAreDeterministic() {
        int color = 0xFFE33B3B;
        assertEquals("#E33B3B", RotClientAppearanceConfig.toHexRgb(color));
        assertEquals(
                0xFFE33B3B,
                RotClientAppearanceConfig.fromHexRgb("#E33B3B", 0xFF, 0));
        RotClientColorMath.Hsv hsv = RotClientColorMath.fromArgb(color);
        int rebuilt = RotClientColorMath.toArgb(hsv.h(), hsv.s(), hsv.v(), 0xFF);
        assertEquals((color >> 16) & 0xFF, (rebuilt >> 16) & 0xFF, 2);
        assertEquals((color >> 8) & 0xFF, (rebuilt >> 8) & 0xFF, 2);
        assertEquals(color & 0xFF, rebuilt & 0xFF, 2);
    }

    @Test
    void appearanceStoreRoundTripUsesAtomicWrite(@TempDir Path dir)
            throws Exception {
        Path path = dir.resolve("rotclient-appearance.json");
        RotClientAppearanceConfig config = RotClientAppearanceConfig.defaults();
        config.dashboardTextPrimary = 0xFFabcdef;
        config.customBackgroundOpacity = 200;
        String json = new com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(config);
        AtomicFileWriter.writeAtomically(path, json);
        String loaded = Files.readString(path, StandardCharsets.UTF_8);
        RotClientAppearanceConfig parsed = new com.google.gson.Gson()
                .fromJson(loaded, RotClientAppearanceConfig.class);
        parsed.normalize();
        assertEquals(0xFFABCDEF, parsed.dashboardTextPrimary);
        assertEquals(200, parsed.customBackgroundOpacity);
    }

    @Test
    void backgroundFileNameValidation() {
        assertTrue(RotClientBackgroundManager.isSupportedImageName("wall.png"));
        assertTrue(RotClientBackgroundManager.isSafeFileName("wall.png"));
        assertFalse(RotClientBackgroundManager.isSafeFileName("../x.png"));
        assertFalse(RotClientBackgroundManager.isSafeFileName("C:/x.png"));
    }

    @Test
    void scrollStateClampsAndReportsCapacity() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setViewportHeight(100);
        scroll.setContentHeight(250);
        assertTrue(scroll.canScroll());
        scroll.scrollBy(999);
        assertEquals(150, scroll.scrollPixels());
        scroll.scrollBy(-999);
        assertEquals(0, scroll.scrollPixels());
        assertTrue(scroll.intersects(40, 20, 0, 100));
        assertFalse(scroll.intersects(200, 20, 0, 100));
        scroll.setBounds(50, 100);
        assertEquals(0, scroll.scrollPixels());
        assertFalse(scroll.canScroll());
    }
}
