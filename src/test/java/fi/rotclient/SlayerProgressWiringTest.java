package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SlayerProgressWiringTest {
    @Test
    void centralGameOverlayIngressObservesProgressBeforeOptionalFiltering()
            throws IOException {
        String client = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        int gameCallback = client.indexOf("ClientReceiveMessageEvents.GAME.register");
        int overlayBranch = client.indexOf("if (overlay)", gameCallback);
        int progressObserver = client.indexOf("SlayerRuntime.onActionBar(message);", overlayBranch);
        int actionBarObserver = client.indexOf("observeActionBarMessage(message);", overlayBranch);

        assertTrue(gameCallback >= 0);
        assertTrue(overlayBranch > gameCallback);
        assertTrue(progressObserver > overlayBranch);
        assertTrue(actionBarObserver > progressObserver);
    }

    @Test
    void progressHudIsReadOnlyAndDoesNotCreateASessionSnapshot() throws IOException {
        String runtime = Files.readString(
                Path.of("src/client/java/fi/rotclient/SlayerRuntime.java"),
                StandardCharsets.UTF_8);
        int start = runtime.indexOf("static List<String> progressLines(boolean editorOpen)");
        int end = runtime.indexOf("static List<String> rngLines(boolean editorOpen)", start);

        assertTrue(start >= 0);
        assertTrue(end > start);
        String section = runtime.substring(start, end);
        assertFalse(section.contains("ENGINE.snapshot("));
        assertTrue(section.contains("latestProgress"));
    }
}
