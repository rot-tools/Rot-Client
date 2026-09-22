package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientAutoSwitchLayoutTest {

    private static final int GRID_TOP = 100;

    /** The real cell count: one per rule row plus "Everywhere else". */
    private static int realCellCount() {
        int count = 1;
        for (AutoProfileContext context : AutoProfileContext.values()) {
            if (context.hasRuleRow()) {
                count++;
            }
        }
        return count;
    }

    @Test
    void footerSitsBelowEveryCellForAnyCellCount() {
        for (int cells = 1; cells <= 24; cells++) {
            int footer = RotClientAutoSwitchLayout.footerY(GRID_TOP, cells);

            for (int i = 0; i < cells; i++) {
                int bottom = RotClientAutoSwitchLayout.cellY(GRID_TOP, i)
                        + RotClientAutoSwitchLayout.CELL_HEIGHT;

                assertTrue(footer >= bottom + RotClientAutoSwitchLayout.FOOTER_GAP,
                        "footer " + footer + " overlaps cell " + i
                                + " (bottom " + bottom + ") of " + cells);
            }
        }
    }

    @Test
    void footerSitsBelowTheRealRuleGrid() {
        int cells = realCellCount();
        int lastCellBottom = RotClientAutoSwitchLayout.cellY(GRID_TOP, cells - 1)
                + RotClientAutoSwitchLayout.CELL_HEIGHT;

        assertEquals(lastCellBottom, RotClientAutoSwitchLayout.gridBottom(GRID_TOP, cells));
        assertTrue(RotClientAutoSwitchLayout.footerY(GRID_TOP, cells) > lastCellBottom);
    }

    @Test
    void cellsShareARowInPairsAndRowsNeverOverlap() {
        assertEquals(
                RotClientAutoSwitchLayout.cellY(GRID_TOP, 0),
                RotClientAutoSwitchLayout.cellY(GRID_TOP, 1));
        assertEquals(
                RotClientAutoSwitchLayout.cellY(GRID_TOP, 2),
                RotClientAutoSwitchLayout.cellY(GRID_TOP, 3));

        for (int row = 0; row < 10; row++) {
            int gap = RotClientAutoSwitchLayout.rowY(GRID_TOP, row + 1)
                    - (RotClientAutoSwitchLayout.rowY(GRID_TOP, row)
                    + RotClientAutoSwitchLayout.CELL_HEIGHT);

            assertEquals(RotClientAutoSwitchLayout.ROW_GAP, gap);
        }
    }

    @Test
    void rowCountRoundsUpAndToleratesEmpty() {
        assertEquals(0, RotClientAutoSwitchLayout.rows(0));
        assertEquals(0, RotClientAutoSwitchLayout.rows(-3));
        assertEquals(1, RotClientAutoSwitchLayout.rows(1));
        assertEquals(1, RotClientAutoSwitchLayout.rows(2));
        assertEquals(2, RotClientAutoSwitchLayout.rows(3));
        assertEquals(6, RotClientAutoSwitchLayout.rows(11));
        assertEquals(GRID_TOP, RotClientAutoSwitchLayout.gridBottom(GRID_TOP, 0));
    }
}
