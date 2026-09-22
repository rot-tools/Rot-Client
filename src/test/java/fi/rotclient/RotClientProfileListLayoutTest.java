package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientProfileListLayoutTest {

    private static final Path SCREEN = Path.of(
            "src/client/java/fi/rotclient/MiningUiScreen.java");

    @Test
    void rowsSitOnAFixedPitchWhenNoMenuIsOpen() {
        assertEquals(0, RotClientProfileListLayout.rowTop(0, -1));
        assertEquals(48, RotClientProfileListLayout.rowTop(1, -1));
        assertEquals(240, RotClientProfileListLayout.rowTop(5, -1));
        assertEquals(6 * 48, RotClientProfileListLayout.contentHeight(6, -1));
    }

    @Test
    void anOpenMenuPushesOnlyTheRowsBelowIt() {
        int extra = RotClientProfileListLayout.PITCH_WITH_MENU
                - RotClientProfileListLayout.PITCH;

        assertEquals(0, RotClientProfileListLayout.rowTop(0, 2));
        assertEquals(48, RotClientProfileListLayout.rowTop(1, 2));
        assertEquals(96, RotClientProfileListLayout.rowTop(2, 2));
        assertEquals(144 + extra, RotClientProfileListLayout.rowTop(3, 2));
        assertEquals(5 * 48 + extra, RotClientProfileListLayout.contentHeight(5, 2));
    }

    @Test
    void anOutOfRangeOpenIndexIsIgnored() {
        assertEquals(RotClientProfileListLayout.contentHeight(4, -1),
                RotClientProfileListLayout.contentHeight(4, 9));
    }

    @Test
    void emptyListHasNoHeightAndNoRows() {
        assertEquals(0, RotClientProfileListLayout.contentHeight(0, -1));
        assertEquals(0, RotClientProfileListLayout.contentHeight(-2, -1));
        assertEquals(-1, RotClientProfileListLayout.rowAt(0, 0, -1));
    }

    @Test
    void hitTestingIsTheExactInverseOfDrawing() {
        for (int rows = 1; rows <= 9; rows++) {
            for (int open = -1; open < rows; open++) {
                int height = RotClientProfileListLayout.contentHeight(rows, open);

                // Every pixel belongs to exactly the row that draws it.
                for (int offset = 0; offset < height; offset++) {
                    int row = RotClientProfileListLayout.rowAt(offset, rows, open);
                    int top = RotClientProfileListLayout.rowTop(row, open);
                    int pitch = RotClientProfileListLayout.pitch(row == open);

                    assertTrue(offset >= top && offset < top + pitch,
                            rows + " rows, menu " + open + ", offset " + offset);
                }

                assertEquals(-1, RotClientProfileListLayout.rowAt(-1, rows, open));
                assertEquals(-1, RotClientProfileListLayout.rowAt(height, rows, open));
            }
        }
    }

    @Test
    void rowsNeverOverlapAndLeaveAGapUnderTheirCard() {
        for (int open = -1; open < 6; open++) {
            for (int i = 0; i < 5; i++) {
                int bottomOfCard = RotClientProfileListLayout.rowTop(i, open)
                        + RotClientProfileListLayout.CARD_HEIGHT;

                assertTrue(RotClientProfileListLayout.rowTop(i + 1, open) > bottomOfCard,
                        "row " + (i + 1) + " touches row " + i + " with menu " + open);
            }
        }
    }

    @Test
    void anOpenMenuFitsInsideItsRow() {
        // 40px card, then the 26px button row starting 4px below it.
        int menuBottom = RotClientProfileListLayout.CARD_HEIGHT + 4 + 26;

        assertTrue(menuBottom <= RotClientProfileListLayout.PITCH_WITH_MENU,
                "the menu buttons must lie within the open row's pitch");
    }

    @Test
    void revealLeavesAVisibleRowAlone() {
        assertEquals(50, RotClientProfileListLayout.scrollToReveal(2, -1, 50, 200));
    }

    @Test
    void revealScrollsDownJustEnoughForAnOpenMenu() {
        // Row 3 opened at the bottom of a 150px viewport that shows rows 0-2.
        int scroll = RotClientProfileListLayout.scrollToReveal(3, 3, 0, 150);
        int top = RotClientProfileListLayout.rowTop(3, 3);
        int bottom = top + RotClientProfileListLayout.PITCH_WITH_MENU;

        assertEquals(bottom - 150, scroll);
        assertTrue(top >= scroll && bottom <= scroll + 150);
    }

    @Test
    void revealScrollsUpToARowAboveTheViewport() {
        assertEquals(RotClientProfileListLayout.rowTop(1, -1),
                RotClientProfileListLayout.scrollToReveal(1, -1, 200, 100));
    }

    @Test
    void aRowTallerThanTheViewportIsAlignedToItsTop() {
        assertEquals(RotClientProfileListLayout.rowTop(2, 2),
                RotClientProfileListLayout.scrollToReveal(2, 2, 999, 60));
    }

    @Test
    void theSavedListIsClippedAndScrollableNotCappedAtSixRows() throws Exception {
        String source = Files.readString(SCREEN, StandardCharsets.UTF_8);

        int start = source.indexOf("private void drawProfilesPage(");
        int end = source.indexOf("private int autoSwitchButtonX(", start);
        String draw = source.substring(start, end);

        assertTrue(draw.contains("graphics.enableScissor("), "list must be clipped");
        assertTrue(draw.contains("graphics.disableScissor()"));
        assertTrue(draw.contains("profilesScroll.setBounds("));
        assertTrue(draw.contains("RotClientUiDraw.drawScrollbar("));
        assertTrue(draw.contains("profilesScroll.intersects("),
                "rows outside the viewport must be skipped");

        assertFalse(draw.contains("int maxShown"), "the six-row cap is gone");
        assertFalse(draw.contains("more profiles"), "nothing is hidden behind '+N more'");
    }

    @Test
    void wheelClickAndDragAllUseTheSameScrollState() throws Exception {
        String source = Files.readString(SCREEN, StandardCharsets.UTF_8);

        assertTrue(source.contains("profilesScroll.scrollBySteps("), "wheel");
        assertTrue(source.contains("handleProfilesScrollbarPress("), "press");
        assertTrue(source.contains("profilesScroll.dragThumbTo("), "drag");
        assertTrue(source.contains("profilesScroll.endThumbDrag()"), "release");
        assertTrue(source.contains("revealOpenProfileRow("),
                "an opened menu must be scrolled into view");
    }

    @Test
    void onlyTheVisiblePartOfTheListIsClickable() throws Exception {
        String source = Files.readString(SCREEN, StandardCharsets.UTF_8);

        int start = source.indexOf("private boolean handleProfilesClick(");
        int end = source.indexOf("private boolean handleLoadoutsClick(", start);
        String click = source.substring(start, end);

        assertTrue(click.contains("boolean overList"), click);
        assertTrue(click.contains("&& overList"), click);
        assertTrue(click.contains("profilesScroll.scrollPixels()"),
                "rows must be hit-tested at their scrolled position");
        String compact = click.chars()
                .filter(c -> !Character.isWhitespace(c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        assertFalse(compact.contains("Math.min(6,profiles.size())"),
                "the six-row cap must be gone from the click path too");
    }

    /**
     * The list starts 42 (page header) + 104 (active card) + 18 (label) below
     * the content top; see MiningUiScreen#profilesListClipTop.
     */
    private static final int LIST_TOP_BELOW_CONTENT_TOP = 42 + 104 + 18;

    @Test
    void theListStaysUsableInTheSmallestAllowedWindow() {
        int contentTop = RotClientDashboardLayout.chromeHeight() + 12;
        int viewport = RotClientDashboardLayout.MIN_HEIGHT
                - RotClientDashboardLayout.CONTENT_INSET
                - contentTop
                - LIST_TOP_BELOW_CONTENT_TOP;

        // Room for at least one row with its menu open, so nothing is
        // unreachable even though the list scrolls.
        assertTrue(viewport >= RotClientProfileListLayout.PITCH_WITH_MENU,
                "only " + viewport + "px of list in the smallest window");

        // And the old six-row list really did not fit there.
        assertTrue(RotClientProfileListLayout.contentHeight(6, -1) > viewport,
                "the fix is only needed if six rows overflow");
    }

    @Test
    void theExamplesAndAutoSwitchPagesScrollToo() throws Exception {
        String source = Files.readString(SCREEN, StandardCharsets.UTF_8);

        int start = source.indexOf("private void drawScrolledProfilesSubpage(");
        int end = source.indexOf("/** Bottom edge of the saved-profiles list", start);
        String draw = source.substring(start, end);

        assertTrue(draw.contains("graphics.enableScissor("), draw);
        assertTrue(draw.contains("profilesScroll.scrollPixels()"),
                "sub-pages are drawn at a top moved up by the scroll offset");
        assertTrue(draw.contains("drawAutoSwitchPage(") && draw.contains("drawExamplesPage("));

        int click = source.indexOf("private boolean handleProfileSubpageClick(");
        String clickBody = source.substring(click,
                source.indexOf("private void resetProfileSubpageMessages()", click));
        assertTrue(clickBody.contains("profilesScroll.scrollPixels()"),
                "clicks must use the same shifted top as drawing");
        assertTrue(clickBody.contains("mouseY < clipTop"),
                "clicks outside the clipped page must be ignored");

        int reset = source.indexOf("private void resetProfileSubpageMessages()");
        assertTrue(source.substring(reset, reset + 200).contains("profilesScroll.reset()"),
                "each page starts scrolled to the top");
    }

    @Test
    void neitherSubPageFitsTheSmallestWindowWithoutScrolling() {
        // Documents why they scroll: at the minimum window the content area
        // below the page top is 352px, and both pages are taller than that.
        int room = RotClientDashboardLayout.MIN_HEIGHT
                - RotClientDashboardLayout.CONTENT_INSET
                - (RotClientDashboardLayout.chromeHeight() + 12);

        int autoSwitchEnd = RotClientAutoSwitchLayout.footerY(
                42 + 62 + 12 + 18,
                11) + 12 + 9;
        int examplesEnd = RotClientExamplesLayout.footerY(0, 5) + 24 + 9;

        assertTrue(autoSwitchEnd > room, autoSwitchEnd + " vs " + room);
        assertTrue(examplesEnd > room, examplesEnd + " vs " + room);
    }

    @Test
    void theCompactedSourceCheckWouldCatchARegressionOfTheCap() {
        String capped = "int shown = Math.min(      6,     profiles.size());";
        String compact = capped.chars()
                .filter(c -> !Character.isWhitespace(c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        assertTrue(compact.contains("Math.min(6,profiles.size())"));
    }
}
