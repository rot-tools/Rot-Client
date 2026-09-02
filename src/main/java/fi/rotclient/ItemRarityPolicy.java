package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Item-rarity slot tint from the last SkyBlock rarity lore line. */
public final class ItemRarityPolicy {
    public static final String STYLE_FILLED_OUTLINE = "Filled outline";
    public static final String STYLE_OUTLINE = "Outline";
    public static final String STYLE_FILLED = "Filled";
    public static final String MODE_SLOTS = "Slots";
    public static final int DEFAULT_COMMON = 0xFFFFFFFF;
    public static final int DEFAULT_UNCOMMON = 0xFF55FF55;
    public static final int DEFAULT_RARE = 0xFF5555FF;
    public static final int DEFAULT_EPIC = 0xFFAA00AA;
    public static final int DEFAULT_LEGENDARY = 0xFFFFAA00;
    public static final int DEFAULT_MYTHIC = 0xFFFF55FF;
    public static final int DEFAULT_DIVINE = 0xFF55FFFF;
    public static final int DEFAULT_SPECIAL = 0xFFFF5555;
    public static final float DEFAULT_FILL_ALPHA = 0.22F;
    public static final float DEFAULT_OUTLINE_ALPHA = 0.85F;

    private static final Pattern RARITY = Pattern.compile(
            "\\b(COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC|DIVINE|SPECIAL|VERY SPECIAL)\\b",
            Pattern.CASE_INSENSITIVE);

    public enum Rarity {
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY,
        MYTHIC,
        DIVINE,
        SPECIAL
    }

    private ItemRarityPolicy() {
    }

    public static Rarity parseRarity(List<String> loreLines) {
        if (loreLines == null || loreLines.isEmpty()) {
            return null;
        }
        for (int i = loreLines.size() - 1; i >= 0; i--) {
            Rarity rarity = parseRarityLine(loreLines.get(i));
            if (rarity != null) {
                return rarity;
            }
        }
        return null;
    }

    public static Rarity parseRarityLine(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = RARITY.matcher(
                AutoConversationPolicy.stripFormatting(raw).toUpperCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }
        String token = matcher.group(1).replace(' ', '_');
        if ("VERY_SPECIAL".equals(token) || "SPECIAL".equals(token)) {
            return Rarity.SPECIAL;
        }
        try {
            return Rarity.valueOf(token);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static int colorFor(
            Rarity rarity,
            int common,
            int uncommon,
            int rare,
            int epic,
            int legendary,
            int mythic,
            int divine,
            int special) {
        if (rarity == null) {
            return 0;
        }
        return switch (rarity) {
            case COMMON -> common;
            case UNCOMMON -> uncommon;
            case RARE -> rare;
            case EPIC -> epic;
            case LEGENDARY -> legendary;
            case MYTHIC -> mythic;
            case DIVINE -> divine;
            case SPECIAL -> special;
        };
    }

    public static int withFillAlpha(int argb, float fill) {
        return withAlpha(argb, fill);
    }

    public static int withAlpha(int argb, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    public static String normalizeStyle(String raw) {
        if (raw == null) {
            return STYLE_FILLED_OUTLINE;
        }
        String value = raw.trim();
        if (value.equalsIgnoreCase(STYLE_OUTLINE) || value.equalsIgnoreCase("Outline")) {
            return STYLE_OUTLINE;
        }
        if (value.equalsIgnoreCase(STYLE_FILLED) || value.equalsIgnoreCase("Filled")) {
            return STYLE_FILLED;
        }
        return STYLE_FILLED_OUTLINE;
    }

    public static boolean drawFill(String style) {
        String normalized = normalizeStyle(style);
        return normalized.equals(STYLE_FILLED) || normalized.equals(STYLE_FILLED_OUTLINE);
    }

    public static boolean drawOutline(String style) {
        String normalized = normalizeStyle(style);
        return normalized.equals(STYLE_OUTLINE) || normalized.equals(STYLE_FILLED_OUTLINE);
    }
}
