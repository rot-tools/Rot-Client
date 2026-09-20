package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Turns "stack these HUDs in this corner" into absolute GUI-scaled positions
 * for one particular screen size.
 *
 * Example profiles describe their layout as anchored stacks instead of fixed
 * coordinates, so the same preset lands sensibly on a 640x360 window and on a
 * 1920x1080 one. It is resolved once, when the example is added; after that
 * the positions are ordinary profile settings the user can drag around in the
 * HUD editor.
 *
 * Rules:
 * - Elements in a stack are laid out top to bottom with {@link #GAP} between
 *   them, in the order listed.
 * - A stack is pinned to its anchor: the top edge for TOP_*, the bottom edge
 *   for BOTTOM_*, vertically centred for MIDDLE_*, and likewise for the left,
 *   right and centre columns.
 * - Several stacks on the same left or right anchor sit side by side, each
 *   placed after the previous one, so two columns never overlap.
 * - Every result is clamped onto the screen.
 *
 * Pure geometry with no Minecraft types so it can be tested.
 */
final class PresetHudLayout {
    /** Distance kept from the screen edges. */
    static final int MARGIN = 12;

    /** Vertical space between elements of one stack. */
    static final int GAP = 4;

    /** Horizontal space between side-by-side stacks. */
    static final int COLUMN_GAP = 8;

    private PresetHudLayout() {
    }

    enum Anchor {
        TOP_LEFT,
        TOP_CENTER,
        TOP_RIGHT,
        MIDDLE_LEFT,
        MIDDLE_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_CENTER,
        BOTTOM_RIGHT;

        boolean isLeft() {
            return this == TOP_LEFT || this == MIDDLE_LEFT || this == BOTTOM_LEFT;
        }

        boolean isRight() {
            return this == TOP_RIGHT || this == MIDDLE_RIGHT || this == BOTTOM_RIGHT;
        }

        boolean isTop() {
            return this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT;
        }

        boolean isBottom() {
            return this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT;
        }

        static Anchor parse(String raw) {
            if (raw == null) {
                return null;
            }
            String wanted = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            for (Anchor anchor : values()) {
                if (anchor.name().equals(wanted)) {
                    return anchor;
                }
            }
            return null;
        }
    }

    record Size(int width, int height) {
    }

    record Stack(Anchor anchor, List<String> elements) {
    }

    record Placement(String id, int x, int y, int width, int height) {
        boolean overlaps(Placement other) {
            return x < other.x + other.width
                    && other.x < x + width
                    && y < other.y + other.height
                    && other.y < y + height;
        }
    }

    /**
     * Places every element of every stack, clamped onto the screen. Elements
     * whose size is unknown (null) are skipped rather than guessed.
     */
    static List<Placement> resolve(
            List<Stack> stacks,
            int screenWidth,
            int screenHeight,
            Function<String, Size> sizes) {

        return place(stacks, screenWidth, screenHeight, sizes, true);
    }

    /**
     * Whether the layout fits this screen: everything on screen without any
     * clamping, and no two HUDs overlapping. When it does not,
     * {@link #resolve} still returns positions on screen, but some HUDs will
     * overlap, so callers can tell the player.
     */
    static boolean fits(
            List<Stack> stacks,
            int screenWidth,
            int screenHeight,
            Function<String, Size> sizes) {

        List<Placement> placed = place(stacks, screenWidth, screenHeight, sizes, false);

        for (int i = 0; i < placed.size(); i++) {
            Placement p = placed.get(i);

            if (p.x() < 0 || p.y() < 0
                    || p.x() + p.width() > screenWidth
                    || p.y() + p.height() > screenHeight) {
                return false;
            }

            // Columns are laid out from opposite edges, so on a narrow
            // screen a left column and a right column can meet.
            for (int j = i + 1; j < placed.size(); j++) {
                if (p.overlaps(placed.get(j))) {
                    return false;
                }
            }
        }

        return true;
    }

    private static List<Placement> place(
            List<Stack> stacks,
            int screenWidth,
            int screenHeight,
            Function<String, Size> sizes,
            boolean clamp) {

        List<Placement> placements = new ArrayList<>();
        int[] consumed = new int[Anchor.values().length];
        java.util.Arrays.fill(consumed, MARGIN);

        for (Stack stack : stacks) {
            if (stack == null || stack.anchor() == null) {
                continue;
            }

            List<String> ids = new ArrayList<>();
            List<Size> known = new ArrayList<>();
            int stackWidth = 0;
            int stackHeight = 0;

            for (String id : stack.elements()) {
                Size size = sizes.apply(id);

                if (size == null) {
                    continue;
                }

                ids.add(id);
                known.add(size);
                stackWidth = Math.max(stackWidth, size.width());
                stackHeight += size.height();
            }

            if (ids.isEmpty()) {
                continue;
            }

            stackHeight += GAP * (ids.size() - 1);

            Anchor anchor = stack.anchor();
            int column = anchor.ordinal();

            int stackLeft;

            if (anchor.isLeft()) {
                stackLeft = consumed[column];
                consumed[column] += stackWidth + COLUMN_GAP;
            } else if (anchor.isRight()) {
                stackLeft = screenWidth - consumed[column] - stackWidth;
                consumed[column] += stackWidth + COLUMN_GAP;
            } else {
                stackLeft = (screenWidth - stackWidth) / 2;
            }

            int y;

            if (anchor.isTop()) {
                y = MARGIN;
            } else if (anchor.isBottom()) {
                y = screenHeight - MARGIN - stackHeight;
            } else {
                y = (screenHeight - stackHeight) / 2;
            }

            for (int i = 0; i < ids.size(); i++) {
                Size size = known.get(i);

                int x;

                if (anchor.isLeft()) {
                    x = stackLeft;
                } else if (anchor.isRight()) {
                    x = stackLeft + (stackWidth - size.width());
                } else {
                    x = stackLeft + (stackWidth - size.width()) / 2;
                }

                placements.add(new Placement(
                        ids.get(i),
                        clamp ? clamp(x, screenWidth - size.width()) : x,
                        clamp ? clamp(y, screenHeight - size.height()) : y,
                        size.width(),
                        size.height()));

                y += size.height() + GAP;
            }
        }

        return placements;
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, Math.max(0, max)));
    }
}
