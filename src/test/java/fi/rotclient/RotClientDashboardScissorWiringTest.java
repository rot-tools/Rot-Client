package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientDashboardScissorWiringTest {
    @Test
    void nestedSidebarChildClipPopsInsteadOfPushingParentAgain() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        int start = source.indexOf("private void drawSidebarChildren");
        int end = source.indexOf("private void drawSidebarSectionHeader", start);
        assertTrue(start >= 0 && end > start);
        String method = source.substring(start, end);
        assertTrue(method.contains("disableScissor()"));
        assertFalse(method.contains("enableScissor(left, sidebarTop"));
    }
}
