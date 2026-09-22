package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HudColumnLayoutTest {
    private static final int RIGHT = 166;
    private static final int LEFT = 6;

    @Test
    void columnsPackFromTheRightWithTheGapBetweenThem() {
        HudColumnLayout.Result result = HudColumnLayout.packFromRight(
                new int[]{20, 30}, RIGHT, LEFT, 6, 3, 24);
        assertEquals(166, result.rights()[1]);
        // First column ends one gap left of where the second one begins.
        assertEquals(166 - 30 - 6, result.rights()[0]);
        assertEquals(6, result.gap());
        assertEquals(166 - 30 - 6 - 20 - 6 - LEFT, result.nameMax());
    }

    @Test
    void columnsNeverOverlapEachOther() {
        int[] widths = {22, 24, 20, 19, 20};
        HudColumnLayout.Result result = HudColumnLayout.packFromRight(
                widths, RIGHT, LEFT, 6, 3, 24);
        for (int k = 1; k < widths.length; k++) {
            int leftOfThis = result.rights()[k] - widths[k];
            assertTrue(leftOfThis >= result.rights()[k - 1] + result.gap(),
                    "column " + k + " overlaps column " + (k - 1));
        }
    }

    @Test
    void labelColumnNeverReachesTheFirstColumn() {
        int[] widths = {22, 24, 20};
        HudColumnLayout.Result result = HudColumnLayout.packFromRight(
                widths, RIGHT, LEFT, 6, 3, 24);
        int firstColumnLeft = result.rights()[0] - widths[0];
        assertTrue(LEFT + result.nameMax() < firstColumnLeft,
                "a label filling nameMax must still clear the first column");
    }

    @Test
    void tightGapKicksInOnlyWhenTheLabelWouldGetTooLittleRoom() {
        int[] wide = {26, 26, 26, 26, 26};
        HudColumnLayout.Result roomy = HudColumnLayout.packFromRight(
                new int[]{20, 20}, RIGHT, LEFT, 6, 3, 24);
        assertEquals(6, roomy.gap());

        HudColumnLayout.Result tight = HudColumnLayout.packFromRight(
                wide, RIGHT, LEFT, 6, 3, 24);
        assertEquals(3, tight.gap());
        assertTrue(tight.nameMax() > HudColumnLayout.packFromRight(
                wide, RIGHT, LEFT, 6, 6, 24).nameMax());
    }

    @Test
    void nameRoomIsNeverNegativeEvenWhenColumnsOverflow() {
        HudColumnLayout.Result result = HudColumnLayout.packFromRight(
                new int[]{80, 80, 80}, RIGHT, LEFT, 6, 3, 24);
        assertEquals(0, result.nameMax());
    }

    @Test
    void noColumnsLeavesEverythingToTheLabel() {
        HudColumnLayout.Result result = HudColumnLayout.packFromRight(
                new int[0], RIGHT, LEFT, 6, 3, 24);
        assertEquals(0, result.rights().length);
        assertEquals(RIGHT - LEFT, result.nameMax());
    }
}
