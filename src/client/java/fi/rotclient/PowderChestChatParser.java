package fi.rotclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses one normalized Powder Chest chat line without mutating state. */
final class PowderChestChatParser {
    static final String START_TEXT = "CHEST LOCKPICKED";
    /** Hypixel Powder Chest separator observed in runtime captures. */
    static final char SEPARATOR_CHAR = '\u25AC';
    /** Legacy separator retained for backward-compatible unit fixtures. */
    static final char LEGACY_SEPARATOR_CHAR = '\u2580';
    static final int MIN_SEPARATOR_LENGTH = 21;

    private static final Pattern LEGACY_FORMATTING =
            Pattern.compile("\u00A7.");
    private static final Pattern HOTM_EXPERIENCE = Pattern.compile(
            "^\\s*\\+[\\d,]+\\s+HOTM Experience\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REWARD_LINE_EXPLICIT = Pattern.compile(
            "^( {4})(.+?)\\s+[xX]([\\d,]+)\\s*$");
    private static final Pattern REWARD_LINE_IMPLICIT = Pattern.compile(
            "^( {4})(.+?)\\s*$");
    private static final Pattern EXPLICIT_QUANTITY_MARKER = Pattern.compile(
            "^( {4}).+\\s+[xX].*$");
    private static final Pattern EXPLICIT_QUANTITY_SUFFIX = Pattern.compile(
            "^( {4})(.+?)\\s+[xX](.*)$");

    private PowderChestChatParser() {
    }

    static ParsedLine parse(String rawLine) {
        if (rawLine == null) {
            return ParsedLine.ignored("", "null-line");
        }

        String normalizedLine = normalizeLine(rawLine);
        if (normalizedLine.isEmpty()) {
            return ParsedLine.ignored(normalizedLine, "blank-line");
        }

        String trimmed = normalizedLine.trim();
        if (isStartMarker(trimmed)) {
            return ParsedLine.startMarker(normalizedLine);
        }
        if (isEndMarker(trimmed)) {
            return ParsedLine.endMarker(normalizedLine);
        }
        if (HOTM_EXPERIENCE.matcher(trimmed).matches()) {
            return ParsedLine.ignored(normalizedLine, "hotm-experience");
        }

        Matcher rewardMatcher = REWARD_LINE_EXPLICIT.matcher(normalizedLine);
        String quantityText = null;
        if (rewardMatcher.matches()) {
            quantityText = rewardMatcher.group(3);
        } else if (looksLikeExplicitQuantityLine(normalizedLine)) {
            return parseMalformedExplicitQuantity(normalizedLine);
        } else {
            rewardMatcher = REWARD_LINE_IMPLICIT.matcher(normalizedLine);
            if (!rewardMatcher.matches()) {
                return ParsedLine.ignored(normalizedLine, "unrecognized-line");
            }
        }

        String displayName = stripStatPrefix(
                rewardMatcher.group(2).trim());
        if (displayName.isEmpty()) {
            return ParsedLine.ignored(normalizedLine, "empty-reward-name");
        }

        if (isCurrencyName(displayName) && quantityText == null) {
            return ParsedLine.ignored(
                    normalizedLine,
                    "missing-explicit-quantity");
        }

        long quantity;
        try {
            quantity = parseQuantity(quantityText);
        } catch (IllegalArgumentException exception) {
            return ParsedLine.ignored(
                    normalizedLine,
                    exception.getMessage());
        }

        if (isCurrencyName(displayName)) {
            return ParsedLine.currencyReward(
                    normalizedLine,
                    displayName,
                    quantity);
        }
        return ParsedLine.itemReward(
                normalizedLine,
                displayName,
                quantity);
    }

    private static boolean isStartMarker(String trimmed) {
        return START_TEXT.equalsIgnoreCase(trimmed);
    }

    private static boolean isEndMarker(String trimmed) {
        if (trimmed.length() < MIN_SEPARATOR_LENGTH) {
            return false;
        }
        for (int index = 0; index < trimmed.length(); index++) {
            if (!isSeparatorChar(trimmed.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSeparatorChar(char character) {
        return character == SEPARATOR_CHAR
                || character == LEGACY_SEPARATOR_CHAR;
    }

    private static boolean looksLikeExplicitQuantityLine(String normalizedLine) {
        return EXPLICIT_QUANTITY_MARKER.matcher(normalizedLine).matches();
    }

    private static ParsedLine parseMalformedExplicitQuantity(
            String normalizedLine) {
        Matcher suffixMatcher =
                EXPLICIT_QUANTITY_SUFFIX.matcher(normalizedLine);
        if (!suffixMatcher.matches()) {
            return ParsedLine.ignored(normalizedLine, "malformed-quantity");
        }

        String quantityText = suffixMatcher.group(3).trim();
        if (quantityText.isEmpty()) {
            return ParsedLine.ignored(normalizedLine, "malformed-quantity");
        }

        try {
            parseQuantity(quantityText);
        } catch (IllegalArgumentException exception) {
            return ParsedLine.ignored(
                    normalizedLine,
                    exception.getMessage());
        }
        return ParsedLine.ignored(normalizedLine, "malformed-quantity");
    }

    private static boolean isCurrencyName(String displayName) {
        return "Gemstone Powder".equalsIgnoreCase(displayName)
                || "Mithril Powder".equalsIgnoreCase(displayName);
    }

    private static long parseQuantity(String quantityText) {
        if (quantityText == null || quantityText.isBlank()) {
            return 1L;
        }

        String digits = quantityText.replace(",", "").trim();
        if (digits.isEmpty()) {
            return 1L;
        }

        long quantity;
        try {
            quantity = Long.parseLong(digits);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("malformed-quantity");
        }

        if (quantity <= 0L) {
            throw new IllegalArgumentException("non-positive-quantity");
        }
        if (quantity > Long.MAX_VALUE / 2L) {
            throw new IllegalArgumentException("overflowing-quantity");
        }
        return quantity;
    }

    static String normalizeLine(String rawLine) {
        String withoutFormatting = LEGACY_FORMATTING.matcher(rawLine)
                .replaceAll("");
        StringBuilder result = new StringBuilder(withoutFormatting.length());

        for (int offset = 0; offset < withoutFormatting.length();) {
            int codePoint = withoutFormatting.codePointAt(offset);
            int characterType = Character.getType(codePoint);
            if (characterType == Character.PRIVATE_USE
                    || characterType == Character.FORMAT) {
                result.append(' ');
            } else if (codePoint == 0x00A0) {
                result.append(' ');
            } else {
                result.appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
        }

        return preserveLeadingSpaces(
                result.toString().replace('\t', ' '));
    }

    private static String preserveLeadingSpaces(String value) {
        int leading = 0;
        while (leading < value.length() && value.charAt(leading) == ' ') {
            leading++;
        }
        String prefix = value.substring(0, leading);
        String remainder = value.substring(leading).replaceAll(" +", " ");
        return prefix + remainder;
    }

    private static String stripStatPrefix(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        int start = 0;
        while (start < value.length()) {
            int codePoint = value.codePointAt(start);
            if (Character.isLetterOrDigit(codePoint)) {
                break;
            }
            start += Character.charCount(codePoint);
        }
        return value.substring(start).trim();
    }

    sealed interface ParsedLine {
        String normalizedLine();

        static StartMarker startMarker(String normalizedLine) {
            return new StartMarker(normalizedLine);
        }

        static EndMarker endMarker(String normalizedLine) {
            return new EndMarker(normalizedLine);
        }

        static ItemReward itemReward(
                String normalizedLine,
                String displayName,
                long quantity) {
            return new ItemReward(normalizedLine, displayName, quantity);
        }

        static CurrencyReward currencyReward(
                String normalizedLine,
                String displayName,
                long quantity) {
            return new CurrencyReward(
                    normalizedLine,
                    displayName,
                    quantity);
        }

        static Ignored ignored(String normalizedLine, String reason) {
            return new Ignored(normalizedLine, reason);
        }

        record StartMarker(String normalizedLine) implements ParsedLine {
        }

        record EndMarker(String normalizedLine) implements ParsedLine {
        }

        record ItemReward(
                String normalizedLine,
                String displayName,
                long quantity) implements ParsedLine {
        }

        record CurrencyReward(
                String normalizedLine,
                String displayName,
                long quantity) implements ParsedLine {
        }

        record Ignored(String normalizedLine, String reason)
                implements ParsedLine {
        }
    }
}
