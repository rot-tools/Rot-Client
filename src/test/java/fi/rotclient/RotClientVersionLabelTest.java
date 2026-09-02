package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

final class RotClientVersionLabelTest {
    @Test
    void formatsDeterministicBrandAndShortLabels() {
        String version = "2.0.0+mc26.2";
        assertEquals(
                "Rot Client v2.0.0+mc26.2",
                RotClientVersionLabel.brandLabel(version));
        assertEquals(
                "v2.0.0+mc26.2",
                RotClientVersionLabel.shortLabel(version));
        assertEquals(
                "Rot Client v2.0.0+mc26.2 | GEMSTONE",
                RotClientVersionLabel.gemstoneHudFooter(version));
        assertEquals(
                RotClientVersionLabel.brandLabel(version),
                RotClientVersionLabel.brandLabel(version));
        assertTrue(RotClientVersionLabel.brandLabel(version)
                .contains("2.0.0+mc26.2"));
        assertTrue(RotClientVersionLabel.shortLabel(version)
                .contains("2.0.0+mc26.2"));
    }

    @Test
    void hudAndDashboardShareTheSameAuthoritativeVersion() {
        String version = "2.0.0+mc26.2";
        String brand = RotClientVersionLabel.brandLabel(version);
        String shortLabel = RotClientVersionLabel.shortLabel(version);
        String gemstone = RotClientVersionLabel.gemstoneHudFooter(version);

        assertTrue(brand.startsWith("Rot Client v"));
        assertEquals("v" + version, shortLabel);
        assertTrue(gemstone.startsWith(brand));
        assertTrue(brand.contains(version));
        assertTrue(shortLabel.contains(version));
        assertTrue(gemstone.contains(version));
    }

    @Test
    void missingOrBlankMetadataUsesSafeNonMisleadingFallback() {
        assertEquals(
                "Rot Client v" + RotClientVersionLabel.FALLBACK_VERSION,
                RotClientVersionLabel.brandLabel(null));
        assertEquals(
                "Rot Client v" + RotClientVersionLabel.FALLBACK_VERSION,
                RotClientVersionLabel.brandLabel("   "));
        assertEquals(
                "v" + RotClientVersionLabel.FALLBACK_VERSION,
                RotClientVersionLabel.shortLabel(""));
        assertFalse(RotClientVersionLabel.brandLabel(null)
                .toLowerCase(Locale.ROOT)
                .contains("v1.9"));
        assertFalse(RotClientVersionLabel.brandLabel(" \n\t ")
                .contains("1.9"));
    }

    @Test
    void productionSourcesContainNoStaleV19Literal() throws IOException {
        Path root = Path.of("src");
        assertTrue(Files.isDirectory(root), "src directory must exist");
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("test"))
                    .forEach(path -> assertNoStaleVersion(path));
        }
    }

    private static void assertNoStaleVersion(Path path) {
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("Unable to read " + path, exception);
        }
        assertFalse(
                text.contains("v1.9"),
                () -> "Stale v1.9 literal found in " + path);
    }
}
