package fi.rotclient;

/**
 * Local Fox / map-art selection only. Callers decide whether a painting,
 * map, or item-display is a wall-art candidate; this never touches packets.
 */
public final class MapArtOverridePolicy {
    public static final float LARGE_CULL_AREA = 8.0F;
    public static final float THIN_SCALE = 0.35F;
    public static final float LARGE_FACE_AREA = 2.0F;
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
        return isLargeFlatScale(scaleX, scaleY, scaleZ)
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

    public static boolean isFilledRectangle(int tileCount, int width, int height) {
        return width >= 1 && height >= 1 && width * height == tileCount && tileCount >= 2;
    }
}
