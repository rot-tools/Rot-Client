package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * The Powder Chest HUD is meant to wear the same card as the Pet HUD: HUD
 * Layout background, rounded corners, soft shadow, thin border, no accent
 * strip. The drawing itself needs Minecraft, so this pins what can be pinned:
 * the style it reads, and that it draws with the very same card constants.
 */
final class PowderChestHudCardTest {

    private static final Path POWDER = Path.of(
            "src/client/java/fi/rotclient/PowderChestHud.java");
    private static final Path OVERLAY = Path.of(
            "src/client/java/fi/rotclient/QolOverlayHud.java");

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Set<String> cardConstants(String source) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = Pattern.compile("HudCardStyle\\.([A-Z_]+)").matcher(source);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    @Test
    void itReadsTheSameBackgroundAsThePetHud() {
        QolSkyblockExtras extras = new QolUtilityConfig().extras();

        assertEquals(
                extras.resolvedHudStyle("pet").backgroundColor,
                extras.resolvedHudStyle("powder_chest").backgroundColor);
    }

    @Test
    void changingTheHudLayoutBackgroundRestylesBothTogether() {
        QolSkyblockExtras extras = new QolUtilityConfig().extras();

        extras.hudLayoutBackgroundColor = 0x80123456;

        assertEquals(0x80123456, extras.resolvedHudStyle("pet").backgroundColor);
        assertEquals(0x80123456, extras.resolvedHudStyle("powder_chest").backgroundColor);
    }

    @Test
    void itDrawsWithExactlyThePetHudCardConstants() throws Exception {
        String overlay = read(OVERLAY);
        int start = overlay.indexOf("private void fillHudPanel(");
        int end = overlay.indexOf("String selectedId()", start);
        assertTrue(start > 0 && end > start, "could not find the Pet HUD panel code");

        Set<String> pet = cardConstants(overlay.substring(start, end));
        Set<String> powder = cardConstants(read(POWDER));

        assertFalse(pet.isEmpty());
        assertEquals(pet, powder,
                "the Powder Chest card must use the same shadow, radius and "
                        + "border constants as the Pet HUD");
    }

    @Test
    void theOldDarkPanelAndAccentStripAreGone() throws Exception {
        String source = read(POWDER);

        assertTrue(source.contains("drawCard(graphics, 0, 0, WIDTH, height)"));
        assertTrue(source.contains("resolvedHudStyle(HUD_STYLE_ID)"));
        assertTrue(source.contains("HUD_STYLE_ID = \"powder_chest\""));

        assertFalse(source.contains("RotClientTheme.HUD_BACKGROUND"),
                "the fixed dark panel must not come back");
        assertFalse(source.contains("graphics.fill(0, 5, 3"),
                "the left accent strip is not part of the Pet HUD card");
    }

    @Test
    void theBackgroundToggleAndEditorOutlineStillWork() throws Exception {
        String source = read(POWDER);

        assertTrue(source.contains("config.powderChestHudShowBackground"));
        assertTrue(source.contains("if (editorOpen)"));
    }
}
