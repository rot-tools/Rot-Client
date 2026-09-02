package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DiagnosticRecorderPathTest {
    @Test
    void genericDiagnosticsUseTheRotClientDiagnosticsDirectory() {
        Path configDir = Path.of("config-root");

        Path path = DiagnosticRecorder.outputPath(
                configDir,
                "20260814-225154");

        assertEquals(
                configDir.resolve("rotclient")
                        .resolve("diagnostics")
                        .resolve("rotclient-diagnostic-20260814-225154.log"),
                path);
    }
}
