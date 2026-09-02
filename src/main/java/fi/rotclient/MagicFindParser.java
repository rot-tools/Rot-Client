package fi.rotclient;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts a displayed Magic Find integer. Does not invent gear-derived
 * bonuses and does not treat missing text as zero.
 */
final class MagicFindParser {
    private static final Pattern FORMATTING = Pattern.compile("\u00A7.");
    private static final Pattern DROP_SUFFIX = Pattern.compile(
            "\\(\\s*\\+?\\s*([\\d,]+)\\s*%?\\s*Magic\\s+Find\\s*\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LABELED = Pattern.compile(
            "(?:✯\\s*)?Magic\\s+Find\\s*:?\\s*\\+?\\s*([\\d,]+)",
            Pattern.CASE_INSENSITIVE);

    private MagicFindParser() {
    }

    static OptionalInt parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalInt.empty();
        }
        String normalized = FORMATTING.matcher(raw)
                .replaceAll("")
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll(" +", " ");
        Matcher drop = DROP_SUFFIX.matcher(normalized);
        if (drop.find()) {
            return parseNumber(drop.group(1));
        }
        Matcher labeled = LABELED.matcher(normalized);
        if (labeled.find()) {
            return parseNumber(labeled.group(1));
        }
        return OptionalInt.empty();
    }

    private static OptionalInt parseNumber(String raw) {
        try {
            int value = Integer.parseInt(raw.replace(",", "").trim());
            if (value < 0) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(value);
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }
}
