package fi.rotclient;

import java.util.Locale;

/**
 * Lite compatibility type for the old dungeon map projection. All puzzle
 * algorithms are compiled only in the Plus source set.
 */
public final class DungeonPuzzlePolicy {
    public record MapPreview(int width, int height, int[] argb, int playerX, int playerZ, String summary) {
        public MapPreview {
            argb = argb == null ? new int[0] : argb.clone();
            summary = summary == null ? "" : summary;
        }
    }

    private DungeonPuzzlePolicy() {
    }

    public static MapPreview previewDungeonMap(byte[] colors, int stride, int crop) {
        return new MapPreview(0, 0, new int[0], -1, -1, "");
    }

    public static boolean isChestBlock(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("chest") && !id.contains("ender");
    }
}
