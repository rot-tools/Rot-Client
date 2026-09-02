package fi.rotclient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses exact gemstone rewards from Hypixel Pristine chat messages.
 *
 * This class only parses data. It does not modify tracker state.
 */
final class PristineMessageParser {
    private static final Pattern LEGACY_FORMATTING =
            Pattern.compile(
                    "\u00A7.");

    private static final Pattern PRISTINE_MESSAGE =
            Pattern.compile(
                    "^\\s*PRISTINE!\\s+"
                            + "You\\s+found\\s+"
                            + ".*?\\bFlawed\\s+"
                            + "[^A-Za-z]*"
                            + "([A-Za-z]+)\\s+"
                            + "Gemstone\\s+"
                            + "x([\\d,]+)!\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private PristineMessageParser() {
    }

    static Reward parse(
            String message) {
        if (message == null
                || message.isBlank()) {
            return null;
        }

        String normalized =
                normalize(
                        message);

        Matcher matcher =
                PRISTINE_MESSAGE.matcher(
                        normalized);

        if (!matcher.matches()) {
            return null;
        }

        GemstoneType gemstone =
                findGemstone(
                        matcher.group(1));

        if (gemstone == null) {
            return null;
        }

        long amount;

        try {
            amount =
                    Long.parseLong(
                            matcher.group(2)
                                    .replace(",", ""));
        }
        catch (NumberFormatException ignored) {
            return null;
        }

        if (amount <= 0L) {
            return null;
        }

        return new Reward(
                gemstone,
                amount);
    }

    private static String normalize(
            String message) {
        String withoutFormatting =
                LEGACY_FORMATTING.matcher(
                        message)
                        .replaceAll("");

        StringBuilder result =
                new StringBuilder(
                        withoutFormatting.length());

        for (int offset = 0;
                offset < withoutFormatting.length();) {
            int codePoint =
                    withoutFormatting.codePointAt(
                            offset);

            int characterType =
                    Character.getType(
                            codePoint);

            if (characterType
                    == Character.PRIVATE_USE
                    || characterType
                    == Character.FORMAT) {
                result.append(' ');
            }
            else if (codePoint == 0x00A0) {
                result.append(' ');
            }
            else {
                result.appendCodePoint(
                        codePoint);
            }

            offset +=
                    Character.charCount(
                            codePoint);
        }

        return result.toString()
                .replaceAll(
                        "\\s+",
                        " ")
                .trim();
    }

    private static GemstoneType findGemstone(
            String name) {
        if (name == null
                || name.isBlank()) {
            return null;
        }

        for (GemstoneType gemstone :
                GemstoneType.values()) {
            if (gemstone.id()
                    .equalsIgnoreCase(name)
                    || gemstone.displayName()
                    .equalsIgnoreCase(name)) {
                return gemstone;
            }
        }

        return null;
    }

    record Reward(
            GemstoneType gemstone,
            long flawedAmount) {
        Reward {
            if (gemstone == null) {
                throw new IllegalArgumentException(
                        "Gemstone type cannot be null");
            }

            if (flawedAmount <= 0L) {
                throw new IllegalArgumentException(
                        "Flawed amount must be positive");
            }
        }
    }
}
