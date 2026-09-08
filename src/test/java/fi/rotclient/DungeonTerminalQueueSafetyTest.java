package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonTerminalQueueSafetyTest {
    @Test
    void mixinChecksTerminalBeforeMutatingTheQueue() throws Exception {
        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/"
                        + "AbstractContainerScreenInventoryOverlayMixin.java"));
        int condition = mixin.indexOf("DungeonPolicy.detectTerminal(");
        int enqueue = mixin.indexOf("DungeonRuntime.enqueueTerminalClick(");

        assertTrue(condition >= 0);
        assertTrue(enqueue > condition);
    }

    @Test
    void runtimeDropsQueuedClicksOutsideTerminalScreens() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/DungeonRuntime.java"));

        assertTrue(runtime.contains(
                "DungeonPolicy.detectTerminal(title) "
                        + "== DungeonPolicy.Terminal.NONE"));
        assertTrue(runtime.contains("termQueue.clear();"));
        assertTrue(runtime.contains("melodySkipQueue.clear();"));
        assertTrue(runtime.contains("if (!title.equals(lastTermTitle))"));
    }
}
