package fi.rotclient;

import java.util.List;
import java.util.Locale;

/**
 * Dwarven Mines Mist ghosts (charged creepers). Body/overlay toggles stay
 * independent of the highlight boxes: Outline, Filled, or Both, each with
 * its own fill and outline color.
 */
public final class GhostsPolicy {
    public static final double MIST_CEILING_Y = 100.0D;
    public static final String DEFAULT_HIGHLIGHT = "Both";
    public static final List<String> HIGHLIGHT_STYLES = List.of("Outline", "Filled", "Both");
    /** Semi-transparent teal fill (RGB 0, 200, 200). */
    public static final int DEFAULT_FILL = 0x7F00C8C8;
    /** Solid teal outline. */
    public static final int DEFAULT_OUTLINE = 0xFF00C8C8;

    public record Decision(boolean suppressEntity, boolean forceVisible, boolean hidePoweredLayer) {
        public static final Decision NONE = new Decision(false, false, false);
    }

    public record BoxPaint(boolean fill, boolean stroke) {
        public static final BoxPaint NONE = new BoxPaint(false, false);

        public boolean draws() {
            return fill || stroke;
        }
    }

    private GhostsPolicy() {
    }

    public static boolean isMistCreeper(boolean creeper, boolean dwarvenMines, double y) {
        return creeper && dwarvenMines && Double.isFinite(y) && y < MIST_CEILING_Y;
    }

    /**
     * NoFrills keys off tab {@code Area: Dwarven Mines}. Sidebar often says
     * {@code The Mist} instead; either counts as ghost ground.
     */
    public static boolean boardLooksLikeGhostIsland(String tabAndBoard) {
        if (tabAndBoard == null || tabAndBoard.isBlank()) {
            return false;
        }
        String text = tabAndBoard.toLowerCase(Locale.ROOT);
        return text.contains("dwarven mines") || text.contains("the mist");
    }

    public static String normalizeHighlight(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_HIGHLIGHT;
        }
        String compact = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if ("both".equals(compact) || compact.contains("filled_outline") || compact.contains("outline_filled")) {
            return "Both";
        }
        if ("filled".equals(compact) || "fill".equals(compact)) {
            return "Filled";
        }
        if ("outline".equals(compact) || "stroke".equals(compact)) {
            return "Outline";
        }
        for (String style : HIGHLIGHT_STYLES) {
            if (style.equalsIgnoreCase(value.trim())) {
                return style;
            }
        }
        return DEFAULT_HIGHLIGHT;
    }

    public static BoxPaint boxPaint(String style) {
        return switch (normalizeHighlight(style)) {
            case "Filled" -> new BoxPaint(true, false);
            case "Outline" -> new BoxPaint(false, true);
            default -> new BoxPaint(true, true);
        };
    }

    public static float strokeWidth(BoxPaint paint) {
        if (paint == null || !paint.stroke()) {
            return 0.01F;
        }
        return 3.0F;
    }

    public static int strokeArgb(BoxPaint paint, int outlineArgb, int fillArgb) {
        if (paint == null || !paint.stroke()) {
            return fillArgb;
        }
        return outlineArgb;
    }

    public static int fillArgb(BoxPaint paint, int fillColor) {
        if (paint == null || !paint.fill()) {
            return 0x00000000;
        }
        return fillColor;
    }

    public static Decision decide(
            boolean enabled,
            boolean showGhosts,
            boolean showPowered,
            boolean creeper,
            boolean invisible) {
        return decide(enabled, showGhosts, showPowered, creeper, invisible, true, 0.0D);
    }

    public static Decision decide(
            boolean enabled,
            boolean showGhosts,
            boolean showPowered,
            boolean creeper,
            boolean invisible,
            boolean dwarvenMines,
            double y) {
        if (!enabled || !isMistCreeper(creeper, dwarvenMines, y)) {
            return Decision.NONE;
        }
        boolean hidePowered = !showPowered;
        if (invisible && !showGhosts && hidePowered) {
            return new Decision(true, false, true);
        }
        return new Decision(false, invisible && showGhosts, hidePowered);
    }
}
