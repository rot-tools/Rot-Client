package fi.rotclient;

/**
 * Vertical geometry of the Auto Switch rule grid: two columns of cells, so
 * cell {@code i} sits in row {@code i / 2}. Kept free of Minecraft types so
 * the arithmetic can be tested; the screen only draws where this says.
 */
final class RotClientAutoSwitchLayout {
    static final int CELL_HEIGHT = 28;
    static final int ROW_GAP = 6;

    /** Space between the last row of cells and the footer text. */
    static final int FOOTER_GAP = 8;

    private RotClientAutoSwitchLayout() {
    }

    static int rows(int cellCount) {
        return (Math.max(0, cellCount) + 1) / 2;
    }

    /** Top edge of a row (not of a cell index). */
    static int rowY(int gridTop, int row) {
        return gridTop + row * (CELL_HEIGHT + ROW_GAP);
    }

    /** Top edge of the cell with this index, counting cells left to right. */
    static int cellY(int gridTop, int index) {
        return rowY(gridTop, index / 2);
    }

    /** Bottom edge of the last row of cells. */
    static int gridBottom(int gridTop, int cellCount) {
        int rows = rows(cellCount);
        return rows == 0
                ? gridTop
                : rowY(gridTop, rows - 1) + CELL_HEIGHT;
    }

    /** Where the help text below the grid starts. */
    static int footerY(int gridTop, int cellCount) {
        return gridBottom(gridTop, cellCount) + FOOTER_GAP;
    }
}
