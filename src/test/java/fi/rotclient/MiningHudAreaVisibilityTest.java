package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningHudAreaVisibilityTest {
    @Test
    void showAreaDefaultIsOnAndResetRestoresIt() {
        TrackerConfig config = new TrackerConfig();
        assertTrue(config.showArea);
        config.showArea = false;
        config.resetHudVisibility();
        assertTrue(config.showArea);
    }

    @Test
    void areaRowParticipatesInTopCardReflow() {
        int without = HudLayoutMath.topCardHeight(
                true, true, true, true,
                true, true, true, true, true, true,
                false);
        int with = HudLayoutMath.topCardHeight(
                true, true, true, true,
                true, true, true, true, true, true,
                true);
        assertEquals(HudLayoutMath.AREA_ROW_HEIGHT, with - without);
        assertEquals(167, without);
        assertEquals(167 + HudLayoutMath.AREA_ROW_HEIGHT, with);
    }

    @Test
    void areaOffLeavesNoBlankSpacer() {
        int baseline = HudLayoutMath.topCardHeight(
                false, false, false, false,
                true, false, false, false, false, false,
                false);
        int stillOff = HudLayoutMath.topCardHeight(
                false, false, false, false,
                true, false, false, false, false, false,
                false);
        assertEquals(baseline, stillOff);

        int withArea = HudLayoutMath.topCardHeight(
                false, false, false, false,
                true, false, false, false, false, false,
                true);
        assertEquals(baseline + HudLayoutMath.AREA_ROW_HEIGHT, withArea);
    }

    @Test
    void gemstoneTopCardAlsoReflowsForArea() {
        int without = HudLayoutMath.gemstoneTopCardHeight(true, true, false);
        int with = HudLayoutMath.gemstoneTopCardHeight(true, true, true);
        assertEquals(HudLayoutMath.AREA_ROW_HEIGHT, with - without);
        assertEquals(
                without + HudLayoutMath.AREA_ROW_HEIGHT + 6 + 150,
                HudLayoutMath.gemstoneHudHeight(true, true, true));
    }

    @Test
    void miningUiScreenExposesAreaToggle() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("showArea"));
        assertTrue(source.contains("AREA / LOCATION"));
    }

    @Test
    void hudRendersAreaFromCanonicalLocationNotInlineParser() throws Exception {
        String hud = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientHud.java"),
                StandardCharsets.UTF_8);
        assertTrue(hud.contains("drawAreaRow"));
        assertTrue(hud.contains("SkyBlockAreaDetector.detectLocation()"));
        assertTrue(hud.contains("EDITOR_PREVIEW"));
        assertFalse(hud.contains("contains(\"forge\")"));
        assertFalse(hud.contains("Cliffside Veins"));
    }

    @Test
    void settingsIndexListsAreaVisibility() {
        assertTrue(RotClientSettingsIndex.isHudVisibilityToggle("showArea"));
        assertTrue(
                RotClientSettingsIndex.search("area").stream()
                        .anyMatch(e -> "showArea".equals(e.id())));
        assertTrue(
                RotClientSettingsIndex.search("location").stream()
                        .anyMatch(e -> "showArea".equals(e.id())));
        assertTrue(
                RotClientSettingsIndex.search("sub-area").stream()
                        .anyMatch(e -> "showArea".equals(e.id())));
    }

    @Test
    void configPersistenceRoundTripKeepsShowArea() {
        TrackerConfig source = new TrackerConfig();
        source.showArea = false;
        TrackerConfig loaded = TrackerStore.fromJson(TrackerStore.toJson(source));
        assertFalse(loaded.showArea);

        loaded.showArea = true;
        TrackerConfig again = TrackerStore.fromJson(TrackerStore.toJson(loaded));
        assertTrue(again.showArea);
    }
}
