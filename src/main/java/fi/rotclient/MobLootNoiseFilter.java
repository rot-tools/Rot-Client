package fi.rotclient;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Rejects Hypixel overlay/UI strings that are not loot items. Identity
 * normalization would otherwise turn kill-combo and menu titles into MOB rows.
 */
final class MobLootNoiseFilter {
    private static final Pattern FORMATTING = Pattern.compile("\u00A7.");
    // Hypixel uses ★ in some overlays and ✯ on Magic Find stats.
    private static final Pattern MAGIC_FIND_CLAUSE = Pattern.compile(
            "(?i)\\s*(?:\\(\\s*)?\\+?\\s*[\\d,]+\\s*%?\\s*[★✯✧*]?\\s*Magic\\s+Find\\s*\\)?");
    private static final Pattern STAR_MAGIC_FIND = Pattern.compile(
            "(?i)\\s*[★✯✧*]\\s*Magic\\s+Find");

    private MobLootNoiseFilter() {
    }

    /**
     * Returns a cleaned loot item name, or empty when the text is overlay/UI
     * noise rather than an item.
     */
    static String lootItemName(String raw) {
        String stripped = stripStatSuffix(raw);
        if (!isLootName(stripped)) {
            return "";
        }
        return stripped;
    }

    static boolean isLootName(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return false;
        }
        String lower = itemName.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("magic find")
                || lower.contains("kill combo")
                || lower.contains("skyblock menu")
                || lower.contains("(click)")
                || lower.contains("recipe book")
                || lower.contains("quest log")) {
            return false;
        }
        if (lower.indexOf('%') >= 0) {
            return false;
        }
        return !isMobOverlayTitle(lower);
    }

    static String stripStatSuffix(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = FORMATTING.matcher(raw)
                .replaceAll("")
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll(" +", " ");
        String withoutFind = MAGIC_FIND_CLAUSE.matcher(normalized)
                .replaceAll("");
        withoutFind = STAR_MAGIC_FIND.matcher(withoutFind).replaceAll("");
        withoutFind = withoutFind.trim();
        int magicFind = withoutFind.toLowerCase(Locale.ROOT)
                .lastIndexOf("magic find");
        if (magicFind > 0) {
            int open = withoutFind.lastIndexOf('(', magicFind);
            int plus = withoutFind.lastIndexOf('+', magicFind);
            int cut = Math.max(open, plus);
            if (cut > 0) {
                withoutFind = withoutFind.substring(0, cut).trim();
            }
        }
        return withoutFind;
    }

    private static boolean isMobOverlayTitle(String lowerName) {
        return lowerName.endsWith(" ghoul")
                || lowerName.endsWith(" zombie")
                || lowerName.endsWith(" skeleton")
                || lowerName.endsWith(" creeper")
                || lowerName.equals("ghoul")
                || lowerName.equals("zombie");
    }
}
