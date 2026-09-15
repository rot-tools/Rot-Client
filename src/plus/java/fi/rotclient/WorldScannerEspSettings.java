package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * World Scanner per-target ESP: enable, highlight style, color, tracer,
 * nametag, chat coords. Keys match Rot Client structure names.
 */
public final class WorldScannerEspSettings {
    public static final String STYLE_OUTLINE = "Outline";
    public static final String STYLE_FILLED = "Filled";
    public static final String STYLE_BOTH = "Both";
    public static final List<String> STYLES =
            List.of(STYLE_OUTLINE, STYLE_FILLED, STYLE_BOTH);

    public static final class Target {
        public boolean enabled = true;
        public String highlightStyle = STYLE_BOTH;
        public int colorArgb;
        public boolean tracer;
        public boolean displayName = true;
        public float nameScale = 1.0F;
        public float backgroundOpacity = 0.5F;
        public boolean notify = true;
        public boolean chatCoords;
        public boolean showBlockCount;

        public Target() {
        }

        public Target(int colorArgb) {
            this.colorArgb = colorArgb;
        }

        public Target copy() {
            Target copy = new Target();
            copy.enabled = enabled;
            copy.highlightStyle = highlightStyle;
            copy.colorArgb = colorArgb;
            copy.tracer = tracer;
            copy.displayName = displayName;
            copy.nameScale = nameScale;
            copy.backgroundOpacity = backgroundOpacity;
            copy.notify = notify;
            copy.chatCoords = chatCoords;
            copy.showBlockCount = showBlockCount;
            return copy;
        }
    }

    public record Spec(String id, String label, int defaultColor) {
    }

    public static final List<Spec> TARGETS = List.of(
            new Spec("fairy", "Fairy Grotto", 0xFFFF55FF),
            new Spec("king", "Goblin King", 0xFFFFAA00),
            new Spec("queen", "Goblin Queen", 0xFFFFAA00),
            new Spec("divan", "Mines of Divan", 0xFF55FF55),
            new Spec("city", "Precursor City", 0xFF55FFFF),
            new Spec("temple", "Jungle Temple", 0xFFAA00AA),
            new Spec("bal", "Khazad-dûm", 0xFFFFAA00),
            new Spec("corleone", "Corleone", 0xFF55FF55),
            new Spec("key_guardian", "Key Guardian", 0xFFAA00AA),
            new Spec("xalx", "Xalx", 0xFF506E00),
            new Spec("pete", "Pete", 0xFF6E2A00),
            new Spec("odawa", "Odawa", 0xFFAAAAAA),
            new Spec("dragon", "Golden Dragon", 0xFFFFAA00),
            new Spec("worm", "Worm Fishing", 0xFFFF5555));

    private WorldScannerEspSettings() {
    }

    public static Map<String, Target> defaults() {
        LinkedHashMap<String, Target> map = new LinkedHashMap<>();
        for (Spec spec : TARGETS) {
            map.put(spec.id(), new Target(spec.defaultColor()));
        }
        return map;
    }

    public static String idForHit(String hitName) {
        if (hitName == null) {
            return "";
        }
        String name = hitName.trim().toLowerCase(Locale.ROOT);
        if (name.contains("fairy")) {
            return "fairy";
        }
        if (name.contains("divan")) {
            return "divan";
        }
        if (name.contains("corleone")) {
            return "corleone";
        }
        if (name.contains("key guardian")) {
            return "key_guardian";
        }
        if (name.contains("dragon")) {
            return "dragon";
        }
        if (name.contains("worm")) {
            return "worm";
        }
        if (name.contains("king")) {
            return "king";
        }
        if (name.contains("queen")) {
            return "queen";
        }
        if (name.contains("temple")) {
            return "temple";
        }
        if (name.contains("city") || name.contains("precursor")) {
            return "city";
        }
        if (name.contains("bal") || name.contains("khazad")) {
            return "bal";
        }
        return name.replace(' ', '_');
    }

    public static String labelForHit(String hitName) {
        String id = idForHit(hitName);
        for (Spec spec : TARGETS) {
            if (spec.id().equals(id)) {
                return spec.label();
            }
        }
        return hitName == null || hitName.isBlank() ? "Waypoint" : hitName;
    }

    public static String normalizeStyle(String raw) {
        if (raw == null) {
            return STYLE_BOTH;
        }
        if (raw.equalsIgnoreCase(STYLE_OUTLINE)) {
            return STYLE_OUTLINE;
        }
        if (raw.equalsIgnoreCase(STYLE_FILLED)) {
            return STYLE_FILLED;
        }
        return STYLE_BOTH;
    }

    public static boolean drawFill(String style) {
        String normalized = normalizeStyle(style);
        return normalized.equals(STYLE_FILLED) || normalized.equals(STYLE_BOTH);
    }

    public static boolean drawStroke(String style) {
        String normalized = normalizeStyle(style);
        return normalized.equals(STYLE_OUTLINE) || normalized.equals(STYLE_BOTH);
    }

    public static Target get(Map<String, Target> map, String id) {
        if (map == null || id == null) {
            return new Target();
        }
        Target target = map.get(id);
        return target == null ? new Target() : target;
    }

    public static boolean masterAllows(
            String id,
            boolean crystals,
            boolean mobSpots,
            boolean fairy,
            boolean dragon,
            boolean worm) {
        if (id == null) {
            return false;
        }
        return switch (id) {
            case "fairy" -> fairy;
            case "dragon" -> dragon;
            case "worm" -> worm;
            case "king", "queen", "divan", "city", "temple", "bal" -> crystals;
            case "corleone", "key_guardian", "xalx", "pete", "odawa" -> mobSpots;
            default -> true;
        };
    }

    public static String[] splitSetting(String settingId) {
        String prefix = "qol.world_scanner.target.";
        if (settingId == null || !settingId.startsWith(prefix)) {
            return null;
        }
        String rest = settingId.substring(prefix.length());
        int dot = rest.lastIndexOf('.');
        if (dot <= 0 || dot >= rest.length() - 1) {
            return null;
        }
        return new String[] {rest.substring(0, dot), rest.substring(dot + 1)};
    }

    public static int defaultColor(String id) {
        for (Spec spec : TARGETS) {
            if (spec.id().equals(id)) {
                return spec.defaultColor();
            }
        }
        return 0xFFFFFFFF;
    }
}
