package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SlayerHighlightWiringTest {
    @Test
    void targetLinesComeFromTheCanonicalSlayerEngineHighlightPass() throws IOException {
        String runtime = Files.readString(
                Path.of("src/client/java/fi/rotclient/SlayerRuntime.java"),
                StandardCharsets.UTF_8);

        int render = runtime.indexOf("public static void renderGizmos()");
        int scan = runtime.indexOf("private static void scanEntities", render);
        String section = runtime.substring(render, scan);

        assertTrue(section.contains("ENGINE.snapshot("));
        assertTrue(section.contains("SlayerHighlightPolicy.shouldHighlight"));
        assertTrue(section.contains("SlayerHighlightPolicy.shouldDrawTargetLine"));
        assertTrue(section.contains("Gizmos.line("));
        assertTrue(section.contains("YANG_GLYPH_BEAMS"));
    }

    @Test
    void yangGlyphTimerTracksThePlacedBeaconBlock() throws IOException {
        String runtime = Files.readString(
                Path.of("src/client/java/fi/rotclient/SlayerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("static void onBlockUpdate("));
        assertTrue(runtime.contains("pruneAndPaintSittingBeacons("));
        assertTrue(runtime.contains("nearPowerOrb("));
        assertTrue(runtime.contains("observeContainer("));
        assertTrue(runtime.contains("shouldTrackYangGlyph("));
        assertTrue(runtime.contains("adoptSittingBeacon("));
        assertTrue(runtime.contains("YANG_GLYPH_BEAM_HEIGHT"));
        assertTrue(!runtime.contains("SITTING_BEACONS.keySet().retainAll(liveSitting)"));
    }
}
