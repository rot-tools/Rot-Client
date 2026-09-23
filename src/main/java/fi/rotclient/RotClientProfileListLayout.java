package fi.rotclient;

/**
 * Row geometry of the saved-profiles list. Free of Minecraft types so the
 * arithmetic is tested; the screen only draws and hit-tests where this says.
 *
 * Each row is a 40px card on a 48px pitch. The row whose "..." menu is open
 * is 32px taller (an 80px pitch) to fit its RENAME / DUPLICATE / DELETE
 * buttons. At most one menu is open at a time; {@code openIndex} is its row,
 * or -1.
 *
 * All offsets are relative to the top of the list, before any scrolling.
 */
final class RotClientProfileListLayout {
    static final int CARD_HEIGHT = 40;
    static final int PITCH = 48;
    static final int PITCH_WITH_MENU = 80;

    private RotClientProfileListLayout() {
    }

    static int pitch(boolean menuOpen) {
        return menuOpen ? PITCH_WITH_MENU : PITCH;
    }

    /** Offset of a row's card from the top of the list. */
    static int rowTop(int index, int openIndex) {
        if (index <= 0) {
            return 0;
        }

        int top = index * PITCH;

        if (openIndex >= 0 && openIndex < index) {
            top += PITCH_WITH_MENU - PITCH;
        }

        return top;
    }

    /** Total height of all rows, including the gap after the last one. */
    static int contentHeight(int rowCount, int openIndex) {
        if (rowCount <= 0) {
            return 0;
        }

        int height = rowCount * PITCH;

        if (openIndex >= 0 && openIndex < rowCount) {
            height += PITCH_WITH_MENU - PITCH;
        }

        return height;
    }

    /**
     * The row whose pitch contains this offset (which includes the gap under
     * its card), or -1 when the offset is above, below or between no rows.
     */
    static int rowAt(int offset, int rowCount, int openIndex) {
        if (offset < 0 || offset >= contentHeight(rowCount, openIndex)) {
            return -1;
        }

        for (int i = 0; i < rowCount; i++) {
            int top = rowTop(i, openIndex);
            int pitch = pitch(i == openIndex);

            if (offset >= top && offset < top + pitch) {
                return i;
            }
        }

        return -1;
    }

    /**
     * The scroll offset that brings a whole row, including an open menu,
     * into a viewport, moving as little as possible. A row taller than the
     * viewport is aligned to the top.
     */
    static int scrollToReveal(
            int index,
            int openIndex,
            int currentScroll,
            int viewportHeight) {

        int top = rowTop(index, openIndex);
        int bottom = top + pitch(index == openIndex);

        if (bottom - top >= viewportHeight) {
            return top;
        }

        if (top < currentScroll) {
            return top;
        }

        if (bottom > currentScroll + viewportHeight) {
            return bottom - viewportHeight;
        }

        return currentScroll;
    }
}
