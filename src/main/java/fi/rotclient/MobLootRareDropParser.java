package fi.rotclient;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel rare-drop chat lines without treating them as an allow-list.
 */
final class MobLootRareDropParser {
    private static final Pattern FORMATTING = Pattern.compile("\u00A7.");
    private static final Pattern RARE_DROP = Pattern.compile(
            "^(?:CRAZY RARE|VERY RARE|INSANE|LEGENDARY|RARE) DROP!\\s+(.+?)$",
            Pattern.CASE_INSENSITIVE);

    private MobLootRareDropParser() {
    }

    static Optional<String> parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }
        String normalized = FORMATTING.matcher(rawLine)
                .replaceAll("")
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll(" +", " ");
        Matcher matcher = RARE_DROP.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String itemName = matcher.group(1).trim();
        int magicFind = itemName.toLowerCase(Locale.ROOT).lastIndexOf("magic find");
        if (magicFind > 0) {
            int open = itemName.lastIndexOf('(');
            if (open > 0 && open < magicFind) {
                itemName = itemName.substring(0, open).trim();
            }
        }
        if (itemName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(itemName);
    }
}
