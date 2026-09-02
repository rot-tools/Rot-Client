package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientFontsWiringTest {
    @Test
    void uiFontAssetsAreBundled() {
        Path json = Path.of("src/client/resources/assets/rotclient/font/ui.json");
        Path ttf = Path.of("src/client/resources/assets/rotclient/font/ui.ttf");
        assertTrue(Files.isRegularFile(json));
        assertTrue(Files.isRegularFile(ttf));
        assertTrue(Files.isRegularFile(Path.of(
                "src/client/resources/assets/rotclient/license/OFL.txt")));
    }

    @Test
    void dashboardAndHudDrawThroughSansSerifHelper() throws Exception {
        String draw = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientUiDraw.java"));
        assertTrue(draw.contains("RotClientFonts.component"));
        String fonts = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientFonts.java"));
        assertTrue(fonts.contains("rotclient"));
        assertTrue(fonts.contains("\"ui\""));
        String json = Files.readString(Path.of(
                "src/client/resources/assets/rotclient/font/ui.json"));
        assertTrue(json.contains("\"type\": \"ttf\""));
        assertTrue(json.contains("rotclient:ui.ttf"));
    }
}
