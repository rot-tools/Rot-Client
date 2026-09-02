package fi.rotclient;

import java.util.List;

/**
 * Chooses an edge position for the pause-menu shortcut without moving widgets
 * contributed by Minecraft or other mods.
 */
public final class PauseMenuButtonLayout {
    public static final int BUTTON_WIDTH = 98;
    public static final int BUTTON_HEIGHT = 20;
    private static final int MARGIN = 4;
    private static final int GAP = 4;

    private PauseMenuButtonLayout() {
    }

    public static Position choose(int screenWidth, int screenHeight, List<Bounds> occupied) {
        int horizontalMargin = screenWidth >= BUTTON_WIDTH + MARGIN * 2 ? MARGIN : 0;
        int verticalMargin = screenHeight >= BUTTON_HEIGHT + MARGIN * 2 ? MARGIN : 0;
        int right = Math.max(horizontalMargin, screenWidth - BUTTON_WIDTH - horizontalMargin);
        int bottom = Math.max(verticalMargin, screenHeight - BUTTON_HEIGHT - verticalMargin);

        List<Position> corners = List.of(
                new Position(right, verticalMargin),
                new Position(horizontalMargin, verticalMargin),
                new Position(right, bottom),
                new Position(horizontalMargin, bottom)
        );
        for (Position candidate : corners) {
            if (isFree(candidate, screenWidth, screenHeight, occupied)) return candidate;
        }

        for (int y = verticalMargin; y <= bottom; y += BUTTON_HEIGHT + GAP) {
            Position rightEdge = new Position(right, y);
            if (isFree(rightEdge, screenWidth, screenHeight, occupied)) return rightEdge;

            Position leftEdge = new Position(horizontalMargin, y);
            if (isFree(leftEdge, screenWidth, screenHeight, occupied)) return leftEdge;
        }

        for (int y = verticalMargin; y <= bottom; y += GAP) {
            for (int x = right; x >= horizontalMargin; x -= GAP) {
                Position candidate = new Position(x, y);
                if (isFree(candidate, screenWidth, screenHeight, occupied)) return candidate;
            }
        }

        // A completely widget-filled screen has no non-overlapping solution.
        // Keep a best-effort edge shortcut without moving another mod's controls.
        return corners.getFirst();
    }

    private static boolean isFree(Position candidate, int screenWidth, int screenHeight,
                                  List<Bounds> occupied) {
        Bounds button = new Bounds(candidate.x(), candidate.y(), BUTTON_WIDTH, BUTTON_HEIGHT);
        if (button.x() < 0 || button.y() < 0
                || button.right() > screenWidth || button.bottom() > screenHeight) {
            return false;
        }
        for (Bounds other : occupied) {
            if (button.intersectsWithGap(other, GAP)) return false;
        }
        return true;
    }

    public record Position(int x, int y) {
    }

    public record Bounds(int x, int y, int width, int height) {
        int right() {
            return x + Math.max(0, width);
        }

        int bottom() {
            return y + Math.max(0, height);
        }

        boolean intersectsWithGap(Bounds other, int gap) {
            return x < other.right() + gap
                    && right() + gap > other.x()
                    && y < other.bottom() + gap
                    && bottom() + gap > other.y();
        }
    }
}
