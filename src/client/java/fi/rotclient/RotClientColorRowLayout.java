package fi.rotclient;

/**
 * Deterministic right-aligned geometry for Appearance color-setting rows.
 */
final class RotClientColorRowLayout {
    static final int ROW_HEIGHT = 26;
    static final int ROW_STEP = 28;
    static final int PADDING = 8;
    static final int GAP = 8;
    static final int PICKER_SIZE = 18;
    static final int SWATCH_WIDTH = 28;
    static final int SWATCH_HEIGHT = 16;
    static final int HEX_WIDTH = 58;

    private RotClientColorRowLayout() {
    }

    record Columns(
            int labelLeft,
            int labelMaxWidth,
            int hexX,
            int swatchX,
            int pickerX,
            int controlTop,
            int rowHeight) {
    }

    static Columns compute(int contentLeft, int contentRight, int rowY) {
        int right = contentRight - PADDING;
        int pickerX = right - PICKER_SIZE;
        int swatchX = pickerX - GAP - SWATCH_WIDTH;
        int hexX = swatchX - GAP - HEX_WIDTH;
        int labelLeft = contentLeft + PADDING;
        int labelMaxWidth = Math.max(24, hexX - GAP - labelLeft);
        int controlTop = rowY + (ROW_HEIGHT - SWATCH_HEIGHT) / 2;
        return new Columns(
                labelLeft,
                labelMaxWidth,
                hexX,
                swatchX,
                pickerX,
                controlTop,
                ROW_HEIGHT);
    }

    static boolean pickerInsideBounds(Columns columns, int contentRight) {
        return columns.pickerX() >= 0
                && columns.pickerX() + PICKER_SIZE <= contentRight;
    }

    static boolean swatchInsideBounds(Columns columns, int contentRight) {
        return columns.swatchX() >= 0
                && columns.swatchX() + SWATCH_WIDTH <= contentRight;
    }

    static boolean hexBeforeSwatch(Columns columns) {
        return columns.hexX() + HEX_WIDTH <= columns.swatchX() - GAP + 1;
    }

    static boolean swatchBeforePicker(Columns columns) {
        return columns.swatchX() + SWATCH_WIDTH <= columns.pickerX() - GAP + 1;
    }
}
