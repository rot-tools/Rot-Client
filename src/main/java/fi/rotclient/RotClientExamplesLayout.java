package fi.rotclient;

/**
 * Vertical geometry of the Examples page: a list of equally tall rows with a
 * help footer underneath. Free of Minecraft types so the arithmetic is
 * tested; the screen only draws where this says.
 */
final class RotClientExamplesLayout {
    static final int ROW_HEIGHT = 46;
    static final int ROW_GAP = 6;

    /** First row's offset from the top of the page content. */
    static final int LIST_OFFSET = 68;

    /** Space between the last row and the footer text. */
    static final int FOOTER_GAP = 8;

    private RotClientExamplesLayout() {
    }

    /** Top edge of row {@code index}; index == count is the slot after the last. */
    static int rowY(int top, int index) {
        return top + LIST_OFFSET + index * (ROW_HEIGHT + ROW_GAP);
    }

    /** Bottom edge of the last row, or the list top when there are none. */
    static int listBottom(int top, int count) {
        return count <= 0
                ? rowY(top, 0)
                : rowY(top, count - 1) + ROW_HEIGHT;
    }

    static int footerY(int top, int count) {
        return listBottom(top, count) + FOOTER_GAP;
    }
}
