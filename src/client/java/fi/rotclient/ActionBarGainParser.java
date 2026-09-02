package fi.rotclient;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel-style action-bar / overlay quantity lines such as
 * {@code +128 Hard Stone} without requiring a {@code [Sacks]} chat envelope.
 */
final class ActionBarGainParser {
    private static final Pattern FORMATTING = Pattern.compile("\u00A7.");
    private static final Pattern PLUS_ITEM = Pattern.compile(
            "\\+\\s*([\\d,]+)\\s+([A-Za-z].+)",
            Pattern.CASE_INSENSITIVE);

    private ActionBarGainParser() {
    }

    record Gain(String itemName, long quantity, TrackedMaterial material) {
    }

    static Optional<Gain> parse(String plainMessage) {
        if (plainMessage == null || plainMessage.isBlank()) {
            return Optional.empty();
        }
        String trimmed = FORMATTING.matcher(plainMessage.trim())
                .replaceAll("")
                .replace('\u00A0', ' ')
                .trim();
        // Ignore chat envelopes already handled by sack parsers.
        if (trimmed.regionMatches(true, 0, "[Sacks]", 0, 7)) {
            return Optional.empty();
        }
        Matcher matcher = PLUS_ITEM.matcher(trimmed);
        if (!matcher.find()) {
            return Optional.empty();
        }
        long qty = parseLong(matcher.group(1));
        if (qty <= 0L) {
            return Optional.empty();
        }
        String itemName = MobLootNoiseFilter.lootItemName(matcher.group(2));
        if (itemName.isEmpty()) {
            return Optional.empty();
        }
        int paren = itemName.indexOf('(');
        if (paren > 0) {
            itemName = MobLootNoiseFilter.lootItemName(
                    itemName.substring(0, paren));
        }
        if (itemName.isEmpty()) {
            return Optional.empty();
        }
        TrackedMaterial matched = matchMaterial(itemName);
        return Optional.of(new Gain(itemName, qty, matched));
    }

    private static TrackedMaterial matchMaterial(String itemName) {
        for (TrackedMaterial material : TrackedMaterial.values()) {
            if (material.displayMultiplier(itemName) == 1L) {
                return material;
            }
        }
        return null;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    static String normalizePrefix(String plain) {
        if (plain == null || plain.isBlank()) {
            return "";
        }
        String trimmed = plain.trim();
        return trimmed.length() <= 48
                ? trimmed
                : trimmed.substring(0, 48).toLowerCase(Locale.ROOT);
    }
}
