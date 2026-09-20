package fi.rotclient;

/**
 * Packs right-aligned HUD table columns from the right edge inward and
 * reports how much room is left for the left-hand label column. Works on
 * measured text widths, so nothing depends on the font being a certain size.
 * Minecraft-free.
 */
final class HudColumnLayout {
    /**
     * @param rights  right edge of each column, same order as the input
     * @param nameMax width available to the label column (never negative)
     * @param gap     gap that was used between columns
     */
    record Result(int[] rights, int nameMax, int gap) {
    }

    private HudColumnLayout() {
    }

    /**
     * Uses {@code gap} between columns, falling back to {@code tightGap} when
     * the label column would get less than {@code minName}.
     *
     * @param widths     measured width of each column's widest text
     * @param rightEdge  x of the table's right edge
     * @param leftEdge   x where the label column starts
     */
    static Result packFromRight(int[] widths,
                                int rightEdge,
                                int leftEdge,
                                int gap,
                                int tightGap,
                                int minName) {
        Result roomy = pack(widths, rightEdge, leftEdge, gap);
        if (roomy.nameMax() >= minName || tightGap >= gap) {
            return roomy;
        }
        return pack(widths, rightEdge, leftEdge, tightGap);
    }

    private static Result pack(int[] widths,
                               int rightEdge,
                               int leftEdge,
                               int gap) {
        int[] rights = new int[widths.length];
        int edge = rightEdge;
        for (int k = widths.length - 1; k >= 0; k--) {
            rights[k] = edge;
            edge -= Math.max(0, widths[k]) + gap;
        }
        // edge is now the columns' left bound minus one gap.
        return new Result(rights, Math.max(0, edge - leftEdge), gap);
    }
}
