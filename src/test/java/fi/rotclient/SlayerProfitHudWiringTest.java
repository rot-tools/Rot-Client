package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SlayerProfitHudWiringTest {
    @Test
    void profitHudReadsTheCanonicalEngineWithoutStartingASession() throws IOException {
        String runtime = Files.readString(
                Path.of("src/client/java/fi/rotclient/SlayerRuntime.java"),
                StandardCharsets.UTF_8);
        int start = runtime.indexOf("static List<String> profitLines(boolean editorOpen)");
        int end = runtime.indexOf("private static Map<String, BigDecimal> slayerUnitPrices()", start);

        assertTrue(start >= 0);
        assertTrue(end > start);
        String section = runtime.substring(start, end);
        assertTrue(section.contains("ENGINE.viewSnapshot()"));
        assertFalse(section.contains("ENGINE.snapshot("));
    }
}
