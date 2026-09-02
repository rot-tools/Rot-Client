package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Viewport, search, and row-index math for dashboard overflow lists
 * (HUD piece menus, long enum pickers). Minecraft-free so tests can lock it.
 */
public final class OverflowListPolicy {
    public static final int SEARCH_THRESHOLD = 6;
    public static final int SEARCH_HEIGHT = 24;
    public static final int DEFAULT_MAX_VISIBLE_ROWS = 8;
    public static final int SCREEN_MARGIN = 12;

    private OverflowListPolicy() {
    }

    public static boolean needsSearch(int itemCount) {
        return itemCount >= SEARCH_THRESHOLD;
    }

    public static int maxVisibleRows(int itemCount, int maxVisible) {
        int cap = Math.max(3, maxVisible);
        return Math.min(Math.max(0, itemCount), cap);
    }

    public static int menuHeight(int itemCount, int rowHeight, boolean search) {
        int rows = Math.max(0, itemCount);
        int body = 8 + rows * Math.max(1, rowHeight);
        return search ? SEARCH_HEIGHT + body : body;
    }

    public static int clampedHeight(
            int preferredHeight,
            int screenHeight,
            int preferredY) {
        int screen = Math.max(64, screenHeight);
        int y = clampY(preferredY, Math.min(preferredHeight, screen - SCREEN_MARGIN * 2), screen);
        int available = screen - SCREEN_MARGIN - y;
        return Math.max(48, Math.min(preferredHeight, available));
    }

    public static int clampY(int preferredY, int height, int screenHeight) {
        int screen = Math.max(64, screenHeight);
        int h = Math.max(1, height);
        int maxY = Math.max(SCREEN_MARGIN, screen - SCREEN_MARGIN - h);
        if (preferredY < SCREEN_MARGIN) {
            return SCREEN_MARGIN;
        }
        return Math.min(preferredY, maxY);
    }

    public static List<Integer> matchingIndices(List<String> labels, String query) {
        if (labels == null || labels.isEmpty()) {
            return List.of();
        }
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            if (needle.isEmpty()
                    || (label != null && label.toLowerCase(Locale.ROOT).contains(needle))) {
                out.add(i);
            }
        }
        return List.copyOf(out);
    }

    public static int firstVisibleIndex(int scrollPixels, int rowHeight) {
        int row = Math.max(1, rowHeight);
        return Math.max(0, scrollPixels / row);
    }

    public static int visibleRowCount(int viewportHeight, int rowHeight) {
        int row = Math.max(1, rowHeight);
        int viewport = Math.max(0, viewportHeight);
        return Math.max(1, (viewport + row - 1) / row);
    }
}
