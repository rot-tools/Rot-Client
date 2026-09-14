package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudEditorChromePolicyTest {
    @Test
    void compactInspectorDocksOnTheRight() {
        HudEditorChromePolicy.Rect inspector =
                HudEditorChromePolicy.resolve(
                        HudEditorChromePolicy.Panel.TITLE,
                        HudEditorChromePolicy.UNPLACED,
                        HudEditorChromePolicy.UNPLACED,
                        960,
                        540);

        assertEquals(
                960
                        - HudEditorChromePolicy.TITLE_WIDTH
                        - HudEditorChromePolicy.MARGIN,
                inspector.x());

        assertEquals(
                HudEditorChromePolicy.MARGIN,
                inspector.y());

        assertEquals(
                HudEditorChromePolicy.TITLE_WIDTH,
                inspector.w());

        assertEquals(
                HudEditorChromePolicy.TITLE_HEIGHT,
                inspector.h());

        assertTrue(
                inspector.contains(
                        inspector.x() + 4,
                        inspector.y() + 4));

        assertFalse(
                inspector.contains(
                        0,
                        0));

        assertTrue(
                HudEditorChromePolicy.TITLE_HEIGHT <= 224);
    }

    @Test
    void compatibilityPanelsStillClampAndHintsStayShort() {
        HudEditorChromePolicy.Rect moved =
                HudEditorChromePolicy.resolve(
                        HudEditorChromePolicy.Panel.HELP,
                        12,
                        80,
                        960,
                        540);

        assertEquals(
                12,
                moved.x());

        assertEquals(
                80,
                moved.y());

        assertEquals(
                0,
                HudEditorChromePolicy.clampDragX(
                        -40,
                        252,
                        960));

        assertEquals(
                960 - 252,
                HudEditorChromePolicy.clampDragX(
                        9000,
                        252,
                        960));

        assertFalse(
                HudEditorChromePolicy.isPlaced(
                        HudEditorChromePolicy.UNPLACED,
                        HudEditorChromePolicy.UNPLACED));

        assertEquals(
                2,
                HudEditorChromePolicy.helpLines()
                        .size());

        assertTrue(
                HudEditorChromePolicy.helpLines()
                        .get(0)
                        .contains("Drag"));

        assertTrue(
                HudEditorChromePolicy.helpLines()
                        .stream()
                        .anyMatch(
                                line ->
                                        line.contains(
                                                "Ctrl+Z")));
    }
}