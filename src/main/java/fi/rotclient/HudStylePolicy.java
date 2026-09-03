package fi.rotclient;

/**
 * Minecraft-free HUD chrome: dim unfocused editor elements, clamp scale,
 * and blend per-element overrides with global HUD Layout defaults.
 */
public final class HudStylePolicy {
    public static final int DEFAULT_BG = 0xAA14141C;
    public static final int DEFAULT_TEXT = 0xFFF4F4F8;
    public static final float DEFAULT_SCALE = 1.0F;
    public static final float MIN_SCALE = 0.6F;
    public static final float MAX_SCALE = 2.5F;
    public static final int UNFOCUSED_ALPHA = 0x68;

    private HudStylePolicy() {
    }

    public static float clampScale(float scale) {
        if (!Float.isFinite(scale)) {
            return DEFAULT_SCALE;
        }
        if (scale < MIN_SCALE) {
            return MIN_SCALE;
        }
        if (scale > MAX_SCALE) {
            return MAX_SCALE;
        }
        return scale;
    }

    public static HudStyleState copy(HudStyleState source) {
        HudStyleState out = new HudStyleState();
        if (source == null) {
            return out;
        }
        out.showBackground = source.showBackground;
        out.showTitle = source.showTitle;
        out.backgroundColor = source.backgroundColor;
        out.textColor = source.textColor;
        out.scale = clampScale(source.scale);
        return out;
    }

    public static boolean titleVisible(HudStyleState style) {
        return style == null || style.showTitle == null || style.showTitle;
    }

    public static HudStyleState resolve(HudStyleState global, HudStyleState override) {
        HudStyleState base = copy(global);
        if (override == null) {
            return base;
        }
        base.showBackground = override.showBackground;
        if (override.showTitle != null) {
            base.showTitle = override.showTitle;
        }
        base.backgroundColor = override.backgroundColor;
        base.textColor = override.textColor;
        base.scale = clampScale(override.scale);
        return base;
    }

    public static int withAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    public static int dim(int argb, boolean focused, boolean dimUnfocused) {
        if (focused || !dimUnfocused) {
            return argb;
        }
        int srcAlpha = (argb >>> 24) & 0xFF;
        int next = Math.min(srcAlpha, UNFOCUSED_ALPHA);
        if (srcAlpha == 0) {
            next = UNFOCUSED_ALPHA;
        }
        return withAlpha(argb, next);
    }

    public static boolean isFocused(String elementId, String focusId) {
        if (focusId == null || focusId.isBlank()) {
            return true;
        }
        return focusId.equalsIgnoreCase(elementId == null ? "" : elementId.trim());
    }
}
