package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Hypixel SkyBlock-style action-bar stat fragments.
 * Returns only confidently matched values; never invents missing stats.
 */
public final class SkyBlockStatBarParser {
    private static final String PAIR =
            "([\\d,]+(?:\\.\\d+)?)(?:\\s*/\\s*([\\d,]+(?:\\.\\d+)?))?";
    private static final Pattern HEALTH = Pattern.compile(
            "(?:❤|♥)\\s*" + PAIR
                    + "|" + PAIR + "\\s*(?:❤|♥)"
                    + "|(?:Health|HP)[:\\s]+" + PAIR,
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFENSE = Pattern.compile(
            "(?:❈)\\s*([\\d,]+)"
                    + "|([\\d,]+)\\s*❈"
                    + "|(?:Defen[cs]e)[:\\s]+([\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MANA = Pattern.compile(
            "(?:✎)\\s*" + PAIR
                    + "|" + PAIR + "\\s*✎"
                    + "|(?:Mana)[:\\s]+" + PAIR,
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OVERFLOW = Pattern.compile(
            "(?:ʬ)\\s*([\\d,]+)"
                    + "|([\\d,]+)\\s*ʬ"
                    + "|(?:Overflow(?:\\s*Mana)?)[:\\s]+([\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEED = Pattern.compile(
            "(?:✦|⚡)\\s*([\\d,]+)"
                    + "|([\\d,]+)\\s*(?:✦|⚡)"
                    + "|(?:Speed)[:\\s]+([\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VITALITY = Pattern.compile(
            "(?:♨)\\s*([\\d,]+)"
                    + "|([\\d,]+)\\s*♨"
                    + "|(?:Vitality)[:\\s]+([\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN = Pattern.compile(
            "(\\d[\\d,]*(?:\\.\\d+)?)(?:\\s*/\\s*(\\d[\\d,]*(?:\\.\\d+)?))?");
    private static final List<Pattern> LOCATION_NAME_PATTERNS = locationNamePatterns();

    public record Stats(
            OptionalDouble health,
            OptionalDouble maxHealth,
            OptionalDouble defense,
            OptionalDouble mana,
            OptionalDouble maxMana,
            OptionalDouble overflowMana,
            OptionalDouble speed,
            OptionalDouble vitality) {
        public static Stats empty() {
            return new Stats(
                    OptionalDouble.empty(),
                    OptionalDouble.empty(),
                    OptionalDouble.empty(),
                    OptionalDouble.empty(),
                    OptionalDouble.empty(),
                    OptionalDouble.empty(),
                    OptionalDouble.empty(),
                    OptionalDouble.empty());
        }
    }

    private record Pair(OptionalDouble current, OptionalDouble max) {
        static Pair empty() {
            return new Pair(OptionalDouble.empty(), OptionalDouble.empty());
        }

        boolean present() {
            return current.isPresent();
        }
    }

    private record Token(boolean pair, OptionalDouble current, OptionalDouble max) {
    }

    private SkyBlockStatBarParser() {
    }

    public static Stats parse(String raw) {
        return parseInternal(raw, true);
    }

    /**
     * Tab list / scoreboard: labeled and icon fragments only. Bare {@code 8/23}
     * player counts must not overwrite SkyBlock health.
     */
    public static Stats parseHudSources(String raw) {
        return parseInternal(raw, false);
    }

    private static Stats parseInternal(String raw, boolean positionalBar) {
        if (raw == null || raw.isBlank()) {
            return Stats.empty();
        }
        String text = stripFormatting(raw);
        Pair health = firstPair(HEALTH.matcher(text));
        OptionalDouble defense = firstSingle(DEFENSE.matcher(text));
        Pair mana = firstPair(MANA.matcher(text));
        OptionalDouble overflow = firstSingle(OVERFLOW.matcher(text));
        OptionalDouble speed = firstSingle(SPEED.matcher(text));
        OptionalDouble vitality = firstSingle(VITALITY.matcher(text));
        if (positionalBar) {
            PositionalStats positional = positionalStats(text, health, mana);
            if (!health.present()) {
                health = positional.health;
            }
            if (defense.isEmpty()) {
                defense = positional.defense;
            }
            if (!mana.present()) {
                mana = positional.mana;
            }
            if (overflow.isEmpty()) {
                overflow = positional.overflow;
            }
            if (speed.isEmpty()) {
                speed = positional.speed;
            }
        }
        return new Stats(
                health.current,
                health.max,
                defense,
                mana.current,
                mana.max,
                overflow,
                speed,
                vitality);
    }

    /**
     * Classic SkyBlock action bar after private-use icons are stripped:
     * {@code health/max  defense  mana/max  [overflow]  [speed]}.
     */
    private static PositionalStats positionalStats(String text, Pair knownHealth, Pair knownMana) {
        List<Token> tokens = tokenize(text);
        List<Integer> pairIndexes = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).pair()) {
                pairIndexes.add(i);
            }
        }
        Pair health = Pair.empty();
        Pair mana = Pair.empty();
        OptionalDouble defense = OptionalDouble.empty();
        OptionalDouble overflow = OptionalDouble.empty();
        OptionalDouble speed = OptionalDouble.empty();
        int healthIndex = -1;
        int manaIndex = -1;
        for (int index : pairIndexes) {
            Token token = tokens.get(index);
            Pair pair = new Pair(token.current(), token.max());
            if (looksLikeVanillaHearts(pair) || samePair(pair, knownMana) || samePair(pair, knownHealth)) {
                continue;
            }
            if (!health.present()) {
                health = pair;
                healthIndex = index;
            } else if (!mana.present() && !samePair(pair, health)) {
                mana = pair;
                manaIndex = index;
            }
        }
        if (health.present() && mana.present() && healthIndex >= 0 && manaIndex > healthIndex) {
            for (int i = healthIndex + 1; i < manaIndex; i++) {
                Token token = tokens.get(i);
                if (!token.pair() && token.current().isPresent()) {
                    defense = token.current();
                    break;
                }
            }
        }
        if (mana.present() && manaIndex >= 0) {
            List<OptionalDouble> after = new ArrayList<>();
            for (int i = manaIndex + 1; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (!token.pair() && token.current().isPresent()) {
                    after.add(token.current());
                }
            }
            if (!after.isEmpty()) {
                overflow = after.get(0);
            }
            if (after.size() > 1) {
                speed = after.get(1);
            }
        }
        return new PositionalStats(health, mana, defense, overflow, speed);
    }

    private record PositionalStats(
            Pair health,
            Pair mana,
            OptionalDouble defense,
            OptionalDouble overflow,
            OptionalDouble speed) {
    }

    private static List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            OptionalDouble current = parseNumber(matcher.group(1));
            OptionalDouble max = parseNumber(matcher.group(2));
            if (current.isEmpty()) {
                continue;
            }
            tokens.add(new Token(max.isPresent(), current, max));
        }
        return tokens;
    }

    /**
     * Removes selected SkyBlock stat fragments from an action-bar string while
     * preserving unrelated text. Returns empty if the result would be blank.
     */
    public static Optional<String> filterActionBar(
            String raw,
            boolean hideHealth,
            boolean hideDefense,
            boolean hideMana,
            boolean hideOverflow,
            boolean hideSpeed,
            boolean hideVitality) {
        return filterActionBar(
                raw,
                hideHealth,
                hideDefense,
                hideMana,
                hideOverflow,
                hideSpeed,
                hideVitality,
                false);
    }

    public static Optional<String> filterActionBar(
            String raw,
            boolean hideHealth,
            boolean hideDefense,
            boolean hideMana,
            boolean hideOverflow,
            boolean hideSpeed,
            boolean hideVitality,
            boolean hideLocation) {
        if (raw == null) {
            return Optional.empty();
        }
        String text = stripFormatting(raw);
        if (hideLocation) {
            text = stripLocationNames(text);
        }
        if (hideHealth) {
            text = HEALTH.matcher(text).replaceAll("");
        }
        if (hideDefense) {
            text = DEFENSE.matcher(text).replaceAll("");
        }
        if (hideMana) {
            text = MANA.matcher(text).replaceAll("");
        }
        if (hideOverflow) {
            text = OVERFLOW.matcher(text).replaceAll("");
        }
        if (hideSpeed) {
            text = SPEED.matcher(text).replaceAll("");
        }
        if (hideVitality) {
            text = VITALITY.matcher(text).replaceAll("");
        }
        text = text.replaceAll("\\s{2,}", " ").trim();
        if (text.isBlank()) {
            return Optional.empty();
        }
        if ((hideHealth || hideDefense || hideMana || hideOverflow || hideSpeed || hideVitality)
                && text.matches("[\\d,./\\s]+")) {
            return Optional.empty();
        }
        return Optional.of(text);
    }

    /**
     * Hypixel writes the current SkyBlock zone into the action bar when the
     * player walks across a sub-area. Strip known area names so they do not
     * sit between the health/mana numbers.
     */
    public static String stripLocationNames(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String stripped = text;
        for (Pattern pattern : LOCATION_NAME_PATTERNS) {
            stripped = pattern.matcher(stripped).replaceAll(" ");
        }
        return stripped.replaceAll("\\s{2,}", " ").trim();
    }

    private static List<Pattern> locationNamePatterns() {
        List<String> names = new ArrayList<>();
        for (SkyBlockSubArea sub : SkyBlockSubArea.values()) {
            names.add(sub.displayName());
        }
        for (SkyBlockArea area : SkyBlockArea.values()) {
            if (area == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA) {
                continue;
            }
            names.add(area.displayName());
        }
        names.sort(Comparator.comparingInt(String::length).reversed());
        List<Pattern> patterns = new ArrayList<>(names.size());
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            patterns.add(Pattern.compile(
                    "(?<!\\p{L})" + Pattern.quote(name) + "(?!\\p{L})",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
        return List.copyOf(patterns);
    }

    public static String stripFormatting(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("§.", "")
                .replace('\u00A0', ' ')
                .replaceAll("[\\uE000-\\uF8FF]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean looksLikeVanillaHearts(Pair pair) {
        if (pair.current.isEmpty() || pair.max.isEmpty()) {
            return false;
        }
        double current = pair.current.getAsDouble();
        double max = pair.max.getAsDouble();
        return current <= 40.0D && max <= 40.0D && current <= max;
    }

    private static boolean samePair(Pair left, Pair right) {
        return sameNumber(left.current, right.current) && sameNumber(left.max, right.max);
    }

    private static boolean sameNumber(OptionalDouble left, OptionalDouble right) {
        return left.isPresent() && right.isPresent()
                && Double.compare(left.getAsDouble(), right.getAsDouble()) == 0;
    }

    private static Pair firstPair(Matcher matcher) {
        if (!matcher.find()) {
            return Pair.empty();
        }
        OptionalDouble current = OptionalDouble.empty();
        OptionalDouble max = OptionalDouble.empty();
        for (int i = 1; i <= matcher.groupCount(); i += 2) {
            OptionalDouble value = parseNumber(matcher.group(i));
            if (value.isPresent()) {
                current = value;
                if (i + 1 <= matcher.groupCount()) {
                    max = parseNumber(matcher.group(i + 1));
                }
                break;
            }
        }
        return new Pair(current, max);
    }

    private static OptionalDouble firstSingle(Matcher matcher) {
        if (!matcher.find()) {
            return OptionalDouble.empty();
        }
        for (int i = 1; i <= matcher.groupCount(); i++) {
            OptionalDouble value = parseNumber(matcher.group(i));
            if (value.isPresent()) {
                return value;
            }
        }
        return OptionalDouble.empty();
    }

    private static OptionalDouble parseNumber(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalDouble.empty();
        }
        try {
            double value = Double.parseDouble(raw.replace(",", "").trim());
            if (!Double.isFinite(value) || value < 0) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(value);
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    public static String formatStat(OptionalDouble value) {
        if (value.isEmpty()) {
            return "--";
        }
        double v = value.getAsDouble();
        if (Math.rint(v) == v) {
            return String.format(Locale.ROOT, "%,.0f", v);
        }
        return String.format(Locale.ROOT, "%,.1f", v);
    }
}
