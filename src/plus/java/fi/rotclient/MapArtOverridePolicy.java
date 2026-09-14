package fi.rotclient;

/**
 * Local Fox / map-art selection only. Callers decide whether a painting,
 * map, or item-display is a wall-art candidate; this never touches packets.
 */
public final class MapArtOverridePolicy {
    public static final float LARGE_CULL_AREA = 8.0F;
    public static final float THIN_SCALE = 0.35F;
    public static final float LARGE_FACE_AREA = 2.0F;
    public static final float LARGE_MAX_SCALE = 3.0F;
    /**
     * Hypixel Hub spawn map wall from the owner's latest screenshot: inner Fox
     * tiles plus the vanilla empty-map right column and bottom row (14x7).
     * Nearby 13-16 x 6-8 grids still count as the same canvas.
     */
    public static final int HUB_MAP_COLUMNS = 14;
    public static final int HUB_MAP_ROWS = 7;
    public static final int HUB_MAP_MIN_COLUMNS = 8;
    public static final int HUB_MAP_MAX_COLUMNS = 16;
    public static final int HUB_MAP_MIN_ROWS = 4;
    public static final int HUB_MAP_MAX_ROWS = 10;
    public static final int HUB_MAP_TILES = HUB_MAP_COLUMNS * HUB_MAP_ROWS;
    /**
     * Chebyshev reach from a corner tile of the Hub wall to the opposite edge,
     * plus slack so glow frames, empty maps, and chunk-edge AABBs still join.
     */
    public static final int HUB_MAP_SEARCH_RADIUS = HUB_MAP_COLUMNS + 4;
    /** Plane Chebyshev reach used to jump a one-block hole in the frame grid. */
    public static final int HUB_MAP_NEIGHBOR_REACH = 2;
    /** Bundled Fox photo is square; do not stretch it to fill the wall. */
    public static final float FOX_IMAGE_ASPECT = 1.0F;
    /** Vanilla map quad is 128x128; item-frame pose origin after translate is a corner. */
    public static final float MAP_QUAD_CENTER = 64.0F;
    static final int PACKED_FULL_BRIGHT = 0xF000F0;

    private MapArtOverridePolicy() {
    }

    public static boolean shouldReplacePainting(boolean enabled, boolean hasHorizontalDirection) {
        return enabled && hasHorizontalDirection;
    }

    public static boolean shouldReplaceItemDisplay(
            boolean enabled,
            boolean mapOrPaintingItem,
            boolean fixedBillboard,
            float scaleX,
            float scaleY,
            float scaleZ,
            float boundingWidth,
            float boundingHeight) {
        if (!enabled) {
            return false;
        }
        if (mapOrPaintingItem) {
            return true;
        }
        if (!fixedBillboard) {
            return false;
        }
        float max = Math.max(Math.abs(scaleX), Math.max(Math.abs(scaleY), Math.abs(scaleZ)));
        return max >= LARGE_MAX_SCALE
                || isLargeFlatScale(scaleX, scaleY, scaleZ)
                || boundingWidth * boundingHeight >= LARGE_CULL_AREA;
    }

    public static boolean isLargeFlatScale(float scaleX, float scaleY, float scaleZ) {
        float ax = Math.abs(scaleX);
        float ay = Math.abs(scaleY);
        float az = Math.abs(scaleZ);
        float min = Math.min(ax, Math.min(ay, az));
        float max = Math.max(ax, Math.max(ay, az));
        float mid = ax + ay + az - min - max;
        return min <= THIN_SCALE && mid * max >= LARGE_FACE_AREA;
    }

    public static int packedLight(int preferred, int fallback) {
        if (preferred != 0) {
            return preferred;
        }
        if (fallback != 0) {
            return fallback;
        }
        return PACKED_FULL_BRIGHT;
    }

    /**
     * Z rotation {@code ItemFrameRenderer} applies before {@code MapRenderer.render}.
     * Hub map walls alternate frame rotation (typically 0 vs 2), which is 180 degrees
     * apart; callers must undo this before stretching one image across the grid.
     */
    public static float mapFrameZDegrees(int itemFrameRotation) {
        int rotation = Math.floorMod(itemFrameRotation, 8);
        return (rotation % 4) * 90.0F + 180.0F;
    }

    /**
     * Z rotation vanilla applies to a non-map item in an item frame (45° steps).
     */
    public static float itemFrameZDegrees(int itemFrameRotation) {
        return Math.floorMod(itemFrameRotation, 8) * 45.0F;
    }

    /**
     * Wall column increasing toward the viewer's right. {@code facing2d} is
     * vanilla horizontal 2D: 0 south, 1 west, 2 north, 3 east.
     */
    public static int wallColumn(int x, int z, int facing2d) {
        return switch (Math.floorMod(facing2d, 4)) {
            case 0 -> x;
            case 1 -> z;
            case 2 -> -x;
            default -> -z;
        };
    }

    public static boolean isHubSizedBounds(int width, int height) {
        return width >= HUB_MAP_MIN_COLUMNS
                && width <= HUB_MAP_MAX_COLUMNS
                && height >= HUB_MAP_MIN_ROWS
                && height <= HUB_MAP_MAX_ROWS;
    }

    /** UV region for one tile in a contiguous width x height rectangle. */
    public static float[] tileUv(int column, int rowFromTop, int width, int height) {
        if (width < 1 || height < 1 || column < 0 || rowFromTop < 0
                || column >= width || rowFromTop >= height) {
            return new float[] {0.0F, 0.0F, 1.0F, 1.0F};
        }
        return new float[] {
                column / (float) width,
                rowFromTop / (float) height,
                (column + 1.0F) / width,
                (rowFromTop + 1.0F) / height
        };
    }

    /**
     * Centered contain rect of {@code imageAspect} inside a {@code width}x{@code height}
     * tile wall, in wall UV 0-1: {@code [x, y, w, h]}. Leftover area is letterbox.
     */
    public static float[] containRect(int width, int height, float imageAspect) {
        if (width < 1 || height < 1 || imageAspect <= 0.0F) {
            return new float[] {0.0F, 0.0F, 1.0F, 1.0F};
        }
        float wallAspect = width / (float) height;
        if (wallAspect > imageAspect) {
            float fittedWidth = imageAspect / wallAspect;
            return new float[] {(1.0F - fittedWidth) * 0.5F, 0.0F, fittedWidth, 1.0F};
        }
        float fittedHeight = wallAspect / imageAspect;
        return new float[] {0.0F, (1.0F - fittedHeight) * 0.5F, 1.0F, fittedHeight};
    }

    /**
     * Image UVs for one wall tile using contain (no stretch-to-fill). Values may
     * fall outside {@code [0, 1]}; those samples are black letterbox bars.
     */
    public static float[] tileUvContain(
            int column, int rowFromTop, int width, int height, float imageAspect) {
        float[] tile = tileUv(column, rowFromTop, width, height);
        if (tile[0] == 0.0F && tile[1] == 0.0F && tile[2] == 1.0F && tile[3] == 1.0F
                && (width < 1 || height < 1 || column < 0 || rowFromTop < 0
                || column >= width || rowFromTop >= height)) {
            return tile;
        }
        float[] rect = containRect(width, height, imageAspect);
        if (rect[2] <= 0.0F || rect[3] <= 0.0F) {
            return new float[] {0.0F, 0.0F, 1.0F, 1.0F};
        }
        return new float[] {
                (tile[0] - rect[0]) / rect[2],
                (tile[1] - rect[1]) / rect[3],
                (tile[2] - rect[0]) / rect[2],
                (tile[3] - rect[1]) / rect[3]
        };
    }

    public static boolean isLetterboxUv(float u0, float v0, float u1, float v1) {
        return u1 <= 0.0F || u0 >= 1.0F || v1 <= 0.0F || v0 >= 1.0F;
    }

    /**
     * Fox sub-quad inside a 0-1 tile, mapped from extended image UVs.
     * {@code [x0, y0, x1, y1, u0, v0, u1, v1]}, or {@code null} when the tile is
     * entirely black letterbox.
     */
    public static float[] containedTileImageQuad(float u0, float v0, float u1, float v1) {
        if (isLetterboxUv(u0, v0, u1, v1) || u1 == u0 || v1 == v0) {
            return null;
        }
        float clipU0 = Math.max(u0, 0.0F);
        float clipV0 = Math.max(v0, 0.0F);
        float clipU1 = Math.min(u1, 1.0F);
        float clipV1 = Math.min(v1, 1.0F);
        if (clipU0 >= clipU1 || clipV0 >= clipV1) {
            return null;
        }
        return new float[] {
                (clipU0 - u0) / (u1 - u0),
                (clipV0 - v0) / (v1 - v0),
                (clipU1 - u0) / (u1 - u0),
                (clipV1 - v0) / (v1 - v0),
                clipU0,
                clipV0,
                clipU1,
                clipV1
        };
    }

    public static boolean isFilledRectangle(int tileCount, int width, int height) {
        return width >= 1 && height >= 1 && width * height == tileCount && tileCount >= 2;
    }

    /**
     * Hub-sized walls still get one canvas when a few interior tiles are missing.
     * Callers paint only the frames they found.
     */
    public static boolean isMapPanelLayout(int tileCount, int width, int height) {
        if (width < 1 || height < 1 || tileCount < 2 || tileCount > width * height) {
            return false;
        }
        if (isFilledRectangle(tileCount, width, height)) {
            return true;
        }
        return isHubSizedBounds(width, height) && tileCount * 2 >= width * height;
    }

    public static boolean isHubMapGrid(int tileCount, int width, int height) {
        return width == HUB_MAP_COLUMNS
                && height == HUB_MAP_ROWS
                && tileCount == HUB_MAP_TILES;
    }

    /** Filled maps, empty maps, and map-id stacks all belong on the Hub wall. */
    public static boolean isMapWallItem(boolean filledMap, boolean emptyMap, boolean hasMapId) {
        return filledMap || emptyMap || hasMapId;
    }

    /**
     * Hub edge tiles may be custom / empty-looking items in frames. Any non-empty
     * framed item on the same facing still belongs on the wall canvas.
     */
    public static boolean isWallFrameItem(boolean mapWallItem, boolean hasItem) {
        return hasItem || mapWallItem;
    }
}
