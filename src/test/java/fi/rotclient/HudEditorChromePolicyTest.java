package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudEditorChromePolicyTest {
    @Test
    void unplacedChromeDocksOnTheRightSoItDoesNotCoverTypicalHuds() {
        HudEditorChromePolicy.Rect title = HudEditorChromePolicy.resolve(
                HudEditorChromePolicy.Panel.TITLE,
                HudEditorChromePolicy.UNPLACED,
                HudEditorChromePolicy.UNPLACED,
                960,
                540);
        assertEquals(960 - HudEditorChromePolicy.TITLE_WIDTH - HudEditorChromePolicy.MARGIN, title.x());
        assertEquals(HudEditorChromePolicy.MARGIN, title.y());
        HudEditorChromePolicy.Rect help = HudEditorChromePolicy.defaultRect(
                HudEditorChromePolicy.Panel.HELP, 960, 540);
        assertTrue(help.y() > title.y());
        assertTrue(title.contains(title.x() + 4, title.y() + 4));
        assertFalse(title.contains(0, 0));
    }

    @Test
    void draggedChromeStaysOnScreenAndShiftRUsesUnplacedSentinel() {
        HudEditorChromePolicy.Rect moved = HudEditorChromePolicy.resolve(
                HudEditorChromePolicy.Panel.HELP,
                12,
                80,
                960,
                540);
        assertEquals(12, moved.x());
        assertEquals(80, moved.y());
        assertEquals(0, HudEditorChromePolicy.clampDragX(-40, 268, 960));
        assertEquals(960 - 268, HudEditorChromePolicy.clampDragX(9000, 268, 960));
        assertFalse(HudEditorChromePolicy.isPlaced(
                HudEditorChromePolicy.UNPLACED, HudEditorChromePolicy.UNPLACED));
        assertEquals(7, HudEditorChromePolicy.helpLines().size());
        assertTrue(HudEditorChromePolicy.helpLines().get(0).contains("Drag"));
        assertTrue(HudEditorChromePolicy.helpLines().stream().anyMatch(
                line -> line.contains("Ctrl+Z")));
        assertTrue(HudEditorChromePolicy.TITLE_HEIGHT > HudEditorChromePolicy.HELP_HEIGHT);
    }
}
