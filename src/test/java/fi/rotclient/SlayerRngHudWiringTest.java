package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerRngHudWiringTest {
    @Test
    void rngChanceIsWithheldUntilMagicFindWasActuallyObserved() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/SlayerRuntime.java"));

        assertTrue(source.contains("private static Integer lastMagicFind"));
        assertTrue(source.contains("lastMagicFind = null"));
        assertTrue(source.contains("Magic Find pending"));
        assertTrue(source.contains("if (lastMagicFind == null)"));
    }
}
