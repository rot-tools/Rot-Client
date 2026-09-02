package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.junit.jupiter.api.Test;

final class RotClientThemeAndIconTest {
    @Test
    void themePaletteMatchesCanonicalBrandTokens() {
        assertEquals(0xF208080A, RotClientTheme.DEFAULT_BACKDROP);
        assertEquals(0xF518141C, RotClientTheme.DEFAULT_SURFACE);
        assertEquals(0xF5221A28, RotClientTheme.DEFAULT_SURFACE_ALT);
        assertEquals(0xF50C0A10, RotClientTheme.DEFAULT_FIELD);
        assertEquals(0xF5322040, RotClientTheme.DEFAULT_FIELD_ACTIVE);
        assertEquals(0xFF4A3858, RotClientTheme.DEFAULT_BORDER);
        assertEquals(0xFFE11D48, RotClientTheme.DEFAULT_BORDER_BRIGHT);
        assertEquals(0xFFA855F7, RotClientTheme.DEFAULT_VIOLET);
        assertEquals(0xFFF8F5FF, RotClientTheme.DEFAULT_TEXT);
        assertEquals(0xFFE6DDF0, RotClientTheme.DEFAULT_TEXT_DIM);
        assertEquals(0xFFC8BFD8, RotClientTheme.DEFAULT_TEXT_MUTED);
        assertEquals(0xFFC4B5FD, RotClientTheme.DEFAULT_SUCCESS);
        assertEquals(0xFFF4C06A, RotClientTheme.DEFAULT_WARNING);
        assertEquals(0xFFFF4D6D, RotClientTheme.DEFAULT_ERROR);
        RotClientTheme.resetToDefaults();
        assertEquals(RotClientTheme.DEFAULT_BACKDROP, RotClientTheme.BACKDROP);
        assertEquals(RotClientTheme.DEFAULT_BORDER_BRIGHT, RotClientTheme.BORDER_BRIGHT);
    }

    @Test
    void defaultTextRolesRemainReadableOnPrimarySurface() {
        assertTrue(contrastRatio(
                RotClientTheme.DEFAULT_TEXT,
                RotClientTheme.DEFAULT_SURFACE) >= 7.0D);
        assertTrue(contrastRatio(
                RotClientTheme.DEFAULT_TEXT_DIM,
                RotClientTheme.DEFAULT_SURFACE) >= 4.5D);
        assertTrue(contrastRatio(
                RotClientTheme.DEFAULT_TEXT_MUTED,
                RotClientTheme.DEFAULT_SURFACE) >= 3.0D);
    }

    @Test
    void semanticStatusColorsDifferFromBrandAccent() {
        assertNotEquals(RotClientTheme.BORDER_BRIGHT, RotClientTheme.SUCCESS);
        assertNotEquals(RotClientTheme.BORDER_BRIGHT, RotClientTheme.WARNING);
        assertNotEquals(RotClientTheme.SUCCESS, RotClientTheme.WARNING);
        assertNotEquals(RotClientTheme.SUCCESS, RotClientTheme.ERROR);
    }

    @Test
    void productionUiSourcesUseCentralTheme() throws IOException {
        assertTrue(read("src/client/java/fi/rotclient/MiningUiScreen.java")
                .contains("RotClientTheme."));
        assertTrue(read("src/client/java/fi/rotclient/RotClientHud.java")
                .contains("RotClientTheme."));
        assertTrue(read("src/client/java/fi/rotclient/RotClientScreen.java")
                .contains("RotClientTheme."));
        assertTrue(read("src/client/java/fi/rotclient/RotClientHomeScreen.java")
                .contains("RotClientTheme."));
        assertTrue(read("src/client/java/fi/rotclient/RotClientUiDraw.java")
                .contains("RotClientTheme."));
        String ui = read("src/client/java/fi/rotclient/MiningUiScreen.java")
                + read("src/client/java/fi/rotclient/RotClientHud.java")
                + read("src/client/java/fi/rotclient/RotClientHomeScreen.java");
        assertFalse(ui.contains("0xFF9F68DC"));
        assertFalse(ui.contains("0xFFB57AF2"));
        assertFalse(ui.contains("0xFF55BFFF"));
        assertFalse(ui.toLowerCase(Locale.ROOT).contains("purple"));
        assertFalse(ui.contains("CYAN"));
    }

    @Test
    void fabricIconExistsAsValidPngUnderRotclient() throws Exception {
        Path icon = Path.of("src/main/resources/assets/rotclient/icon.png");
        assertTrue(Files.isRegularFile(icon));
        byte[] bytes = Files.readAllBytes(icon);
        assertTrue(bytes.length > 32);
        assertEquals((byte) 0x89, bytes[0]);
        assertEquals((byte) 0x50, bytes[1]);
        assertEquals((byte) 0x4E, bytes[2]);
        assertEquals((byte) 0x47, bytes[3]);

        String fabric = read("src/main/resources/fabric.mod.json");
        assertTrue(fabric.contains("assets/rotclient/icon.png"));
        assertFalse(Files.exists(Path.of(
                "src/main/resources/assets/miningtracker/icon.png")));

        // IHDR width/height at bytes 16-23 big-endian after 8-byte signature + 8-byte chunk header
        int width = ((bytes[16] & 0xFF) << 24)
                | ((bytes[17] & 0xFF) << 16)
                | ((bytes[18] & 0xFF) << 8)
                | (bytes[19] & 0xFF);
        int height = ((bytes[20] & 0xFF) << 24)
                | ((bytes[21] & 0xFF) << 16)
                | ((bytes[22] & 0xFF) << 8)
                | (bytes[23] & 0xFF);
        assertEquals(256, width);
        assertEquals(256, height);

        String sha = sha256(bytes);
        assertNotEquals(
                "2E73AE2451082FB6323B28B79882802728EED38A5662002A376FDF9C7CE0C71F",
                sha);
        assertNotEquals(
                "6D4E370FDCA65831F9738A0C6AC9306427A7F70FF1274D08037DAD5082AD0DA3",
                sha);
    }

    @Test
    void documentationCoversBrandAndPalette() throws IOException {
        String readme = read("README.md");
        assertTrue(readme.contains("Rot Client"));
        assertTrue(readme.contains("assets/rotclient/icon.png"));
        assertTrue(readme.contains("rot-tools/Rot-Client"));
        assertTrue(readme.contains("/rot"));
        assertTrue(readme.toLowerCase(Locale.ROOT).contains("migrat"));
        assertFalse(readme.contains("OgRudolf/MiningTracker"));

        String branding = read("docs/BRANDING.md");
        assertTrue(branding.contains("0xF208080A"));
        assertTrue(branding.contains("0xFFE11D48"));
        assertTrue(branding.contains("0xFFA855F7"));
        assertTrue(branding.contains("rotclient"));
    }

    private static double contrastRatio(int first, int second) {
        double lighter = Math.max(luminance(first), luminance(second));
        double darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05D) / (darker + 0.05D);
    }

    private static double luminance(int argb) {
        double r = linear((argb >>> 16) & 0xFF);
        double g = linear((argb >>> 8) & 0xFF);
        double b = linear(argb & 0xFF);
        return 0.2126D * r + 0.7152D * g + 0.0722D * b;
    }

    private static double linear(int channel) {
        double value = channel / 255.0D;
        return value <= 0.04045D
                ? value / 12.92D
                : Math.pow((value + 0.055D) / 1.055D, 2.4D);
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        return HexFormat.of().withUpperCase().formatHex(digest);
    }
}
