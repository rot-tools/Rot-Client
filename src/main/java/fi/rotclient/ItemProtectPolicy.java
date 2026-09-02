package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Blocks accidental drops and salvage of starred / high-rarity SkyBlock
 * items plus an optional extra name list. Minecraft I/O stays in the
 * client runtime. Sneak bypasses the lock.
 */
public final class ItemProtectPolicy {
    private static final String[] HIGH_RARITY = {
            "legendary", "mythic", "divine", "special", "very special", "ultimate"
    };

    private ItemProtectPolicy() {
    }

    public static List<String> parseExtraNames(String raw) {
        List<String> names = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return names;
        }
        for (String line : raw.split("\\R")) {
            String name = ChatCommandsPolicy.stripFormatting(line).toLowerCase(Locale.ROOT);
            if (!name.isEmpty() && !name.startsWith("#")) {
                names.add(name);
            }
        }
        return names;
    }

    public static boolean isStarred(String name, List<String> lore) {
        if (containsStar(name)) {
            return true;
        }
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            if (containsStar(line) || ChatCommandsPolicy.stripFormatting(line)
                    .toLowerCase(Locale.ROOT)
                    .contains("dungeon item")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHighRarity(List<String> lore) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            String text = ChatCommandsPolicy.stripFormatting(line).toLowerCase(Locale.ROOT);
            for (String rarity : HIGH_RARITY) {
                if (text.contains(rarity)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean matchesExtra(String name, List<String> extraNames) {
        if (name == null || extraNames == null || extraNames.isEmpty()) {
            return false;
        }
        String cleaned = ChatCommandsPolicy.stripFormatting(name).toLowerCase(Locale.ROOT);
        for (String extra : extraNames) {
            if (!extra.isEmpty() && cleaned.contains(extra)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isProtected(
            boolean starred,
            boolean highRarity,
            String name,
            List<String> lore,
            List<String> extraNames) {
        if (starred && isStarred(name, lore)) {
            return true;
        }
        if (highRarity && isHighRarity(lore)) {
            return true;
        }
        return matchesExtra(name, extraNames);
    }

    public static boolean shouldBlockDrop(
            boolean moduleEnabled,
            boolean protectDrops,
            boolean sneaking,
            boolean starred,
            boolean highRarity,
            String name,
            List<String> lore,
            List<String> extraNames) {
        if (!moduleEnabled || !protectDrops || sneaking) {
            return false;
        }
        return isProtected(starred, highRarity, name, lore, extraNames);
    }

    public static boolean shouldBlockSalvage(
            boolean moduleEnabled,
            boolean protectSalvage,
            boolean sneaking,
            String screenTitle,
            boolean starred,
            boolean highRarity,
            String name,
            List<String> lore,
            List<String> extraNames) {
        if (!moduleEnabled || !protectSalvage || sneaking) {
            return false;
        }
        if (!isSalvageScreen(screenTitle)) {
            return false;
        }
        return isProtected(starred, highRarity, name, lore, extraNames);
    }

    public static boolean isSalvageScreen(String title) {
        String text = ChatCommandsPolicy.stripFormatting(title).toLowerCase(Locale.ROOT);
        return text.contains("salvage")
                || text.contains("the hex")
                || text.contains("attribute fusion")
                || text.contains("experimentation");
    }

    public static boolean isThrowClick(String clickType) {
        String type = clickType == null ? "" : clickType.toUpperCase(Locale.ROOT);
        return type.contains("THROW") || type.contains("DROP") || type.contains("CLONE");
    }

    private static boolean containsStar(String raw) {
        if (raw == null) {
            return false;
        }
        return raw.contains("✪") || raw.contains("➊") || raw.contains("➋")
                || raw.contains("➌") || raw.contains("➍") || raw.contains("➎")
                || raw.contains("⚚") || raw.contains("⭐");
    }
}
