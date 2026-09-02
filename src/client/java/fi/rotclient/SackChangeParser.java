package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel Sack hover changes without assigning them to any tracker.
 *
 * Both additions and removals are preserved as signed deltas. Item names and
 * the source Sack names remain available for later material-specific routing.
 */
final class SackChangeParser {
    private static final Pattern CHANGE_PATTERN =
            Pattern.compile(
                    "([+-])([\\d,]+)\\s+([^\\n(]+?)\\s*\\(([^)\\n]+)\\)",
                    Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE
                            | Pattern.UNICODE_CHARACTER_CLASS);

    private SackChangeParser() {
    }

    static List<Change> parse(String hoverText) {
        return parseWithDiagnostics(hoverText).changes();
    }

    /**
     * Parse hover text and also return privacy-safe token traces so diagnostics
     * can distinguish missing Hard Stone (never present) from parser misses.
     */
    static ParseResult parseWithDiagnostics(String hoverText) {
        if (hoverText == null || hoverText.isBlank()) {
            return new ParseResult(List.of(), List.of());
        }

        String normalized = normalizeVisibleText(hoverText);
        Matcher matcher = CHANGE_PATTERN.matcher(normalized);
        List<Change> changes = new ArrayList<>();
        List<TokenTrace> traces = new ArrayList<>();
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String gap = normalized.substring(lastEnd, matcher.start()).trim();
                if (!gap.isEmpty() && !isSackHeaderNoise(gap)) {
                    traces.add(TokenTrace.unparsed(gap));
                }
            }
            lastEnd = matcher.end();

            long amount = parseAmount(matcher.group(2));
            if (amount <= 0L) {
                traces.add(TokenTrace.unparsed(matcher.group(0)));
                continue;
            }

            long delta = "-".equals(matcher.group(1)) ? -amount : amount;
            String itemName = matcher.group(3).trim();
            List<String> sacks = parseSackNames(matcher.group(4));
            if (itemName.isEmpty() || sacks.isEmpty()) {
                traces.add(TokenTrace.unparsed(matcher.group(0)));
                continue;
            }

            changes.add(new Change(delta, itemName, sacks));
            traces.add(TokenTrace.parsed(itemName, delta));
        }

        if (lastEnd < normalized.length()) {
            String tail = normalized.substring(lastEnd).trim();
            if (!tail.isEmpty()
                    && !isSackHeaderNoise(tail)) {
                traces.add(TokenTrace.unparsed(tail));
            }
        }

        return new ParseResult(List.copyOf(changes), List.copyOf(traces));
    }

    private static boolean isSackHeaderNoise(String value) {
        String trimmed = value.trim();
        return trimmed.equalsIgnoreCase("Added items")
                || trimmed.equalsIgnoreCase("Removed items")
                || trimmed.regionMatches(true, 0, "Added items", 0, 11)
                || trimmed.regionMatches(true, 0, "Removed items", 0, 13);
    }

    static String normalizeVisibleText(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }

        StringBuilder result = new StringBuilder(value.length());
        int offset = 0;
        while (offset < value.length()) {
            int codePoint = value.codePointAt(offset);
            int type = Character.getType(codePoint);
            boolean removable =
                    type == Character.PRIVATE_USE
                            || type == Character.FORMAT;
            if (!removable) {
                if (codePoint != '\n'
                        && codePoint != '\r'
                        && Character.isSpaceChar(codePoint)) {
                    result.append(' ');
                } else {
                    result.appendCodePoint(codePoint);
                }
            }
            offset += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static List<String> parseSackNames(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] parts = value.split("\\s*,\\s*");
        List<String> sacks = new ArrayList<>(parts.length);
        for (String part : parts) {
            String sack = part.trim();
            if (!sack.isEmpty()) {
                sacks.add(sack);
            }
        }
        return List.copyOf(sacks);
    }

    private static long parseAmount(String value) {
        try {
            return Long.parseLong(value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    record ParseResult(List<Change> changes, List<TokenTrace> tokenTraces) {
        ParseResult {
            changes = changes == null ? List.of() : List.copyOf(changes);
            tokenTraces = tokenTraces == null
                    ? List.of()
                    : List.copyOf(tokenTraces);
        }
    }

    /**
     * Privacy-safe token diagnostic: item display name + qty only — never
     * full chat bodies or account identifiers.
     */
    record TokenTrace(boolean parsed, String itemLabel, long quantity) {
        static TokenTrace parsed(String itemName, long quantity) {
            return new TokenTrace(true, sanitize(itemName), quantity);
        }

        static TokenTrace unparsed(String fragment) {
            return new TokenTrace(false, sanitize(fragment), 0L);
        }

        String safeLabel() {
            return itemLabel == null ? "" : itemLabel;
        }

        private static String sanitize(String value) {
            if (value == null || value.isBlank()) {
                return "";
            }
            String cleaned = value
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .replace('\t', ' ')
                    .trim()
                    .replaceAll("\\s+", " ");
            return cleaned.length() <= 48
                    ? cleaned
                    : cleaned.substring(0, 48);
        }
    }

    record Change(
            long delta,
            String itemName,
            List<String> sacks) {
        Change {
            itemName = itemName == null ? "" : itemName.trim();
            sacks = sacks == null ? List.of() : List.copyOf(sacks);
        }

        boolean fromSack(String expectedName) {
            if (expectedName == null || expectedName.isBlank()) {
                return false;
            }
            for (String sack : sacks) {
                if (sack.equalsIgnoreCase(expectedName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
