package fi.rotclient;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a short Hypixel coin-pickup chat line. Bazaar/NPC sale lines are
 * rejected so they cannot become MOB currency.
 */
final class MobLootCoinParser {
    private static final Pattern FORMATTING = Pattern.compile("\u00A7.");
    private static final Pattern COINS = Pattern.compile(
            "\\+\\s*([\\d,]+)\\s+coins?\\b",
            Pattern.CASE_INSENSITIVE);

    private MobLootCoinParser() {
    }

    static Optional<Long> parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }
        String normalized = FORMATTING.matcher(rawLine)
                .replaceAll("")
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll(" +", " ");
        String lower = normalized.toLowerCase();
        if (lower.contains("[bazaar]")
                || lower.contains(" sold ")
                || lower.contains(" for ")
                || lower.contains("npc")) {
            return Optional.empty();
        }
        Matcher matcher = COINS.matcher(normalized);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            long quantity = Long.parseLong(matcher.group(1).replace(",", ""));
            if (quantity <= 0L) {
                return Optional.empty();
            }
            return Optional.of(quantity);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
