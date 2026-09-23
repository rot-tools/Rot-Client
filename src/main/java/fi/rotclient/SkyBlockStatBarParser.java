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
    /*
     * Current SkyBlock vitality is a resource pair, for example 85/100,
     * rather than only a single standalone number.
     */
    private static final Pattern VITALITY_PAIR = Pattern.compile(
            PAIR + "\\s*♨"
                    + "|(?:Vitality)[:\\s]+" + PAIR,
            Pattern.CASE_INSENSITIVE);

    private static final Pattern VITALITY = Pattern.compile(
            "(?:♨)\\s*([\\d,]+)"
                    + "|([\\d,]+)\\s*♨"
                    + "|(?:Vitality)[:\\s]+([\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOKEN = Pattern.compile(
            "(\\d[\\d,]*(?:\\.\\d+)?)(?:\\s*/\\s*(\\d[\\d,]*(?:\\.\\d+)?))?");
    private static final Pattern SKILL_TICK = Pattern.compile(
            "(?i)\\+\\s*[\\d,.]+(?:[kmb])?\\s+"
                    + "(?:combat|mining|farming|foraging|enchanting|alchemy|fishing|"
                    + "taming|carpentry|runecrafting|social|hunting)"
                    + "(?:\\s*\\([^)]*\\))?");
    private static final Pattern SECTION_CODE = Pattern.compile("§.");
    private static final Pattern PRIVATE_USE_GLYPH = Pattern.compile("[\\uE000-\\uF8FF]");
    private static final Pattern WHITESPACE_RUN = Pattern.compile("\\s+");
    private static final Pattern WHITESPACE_2PLUS = Pattern.compile("\\s{2,}");
    private static final Pattern ONLY_NUMBER_PUNCTUATION = Pattern.compile("[\\d,./+\\s]+");
    private static final Pattern UNLABELED_PAIR = Pattern.compile(
            "\\b[\\d,]+(?:\\.\\d+)?\\s*/\\s*[\\d,]+(?:\\.\\d+)?\\b");
    /**
     * Hypixel HUD-font icon slots. With the SkyBlock font they are hearts /
     * defense / mana glyphs; after {@code Component.literal} they show as
     * {@code T1}..{@code T5} on top of the vanilla hearts.
     */
    private static final Pattern HUD_ICON_TOKEN = Pattern.compile("(?i)T[1-5]");
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

        String text =
                stripHudIconTokens(
                        stripFormatting(
                                raw));

        Pair health =
                firstPair(
                        HEALTH.matcher(
                                text));

        OptionalDouble defense =
                firstSingle(
                        DEFENSE.matcher(
                                text));

        Pair mana =
                firstPair(
                        MANA.matcher(
                                text));

        OptionalDouble overflow =
                firstSingle(
                        OVERFLOW.matcher(
                                text));

        OptionalDouble speed =
                firstSingle(
                        SPEED.matcher(
                                text));

        Pair vitalityPair =
                firstPair(
                        VITALITY_PAIR.matcher(
                                text));

        OptionalDouble vitality =
                vitalityPair.current();

        if (vitality.isEmpty()) {
            vitality =
                    firstSingle(
                            VITALITY.matcher(
                                    text));
        }

        if (positionalBar) {
            /*
             * Skill gain messages may contain another current/max pair.
             * Remove that fragment before interpreting positional resource
             * pairs so skill XP cannot become Mana or Vitality.
             */
            String positionalText =
                    SKILL_TICK
                            .matcher(text)
                            .replaceAll(" ");

            PositionalStats positional =
                    positionalStats(
                            positionalText,
                            health,
                            mana);

            if (!health.present()) {
                health =
                        positional.health();
            }

            if (defense.isEmpty()) {
                defense =
                        positional.defense();
            }

            if (!mana.present()) {
                mana =
                        positional.mana();
            }

            if (overflow.isEmpty()) {
                overflow =
                        positional.overflow();
            }

            if (speed.isEmpty()) {
                speed =
                        positional.speed();
            }

            if (vitality.isEmpty()) {
                vitality =
                        positional.vitality();
            }

            /*
             * Hypixel omits the overflow fragment when Overflow Mana is zero.
             * A valid Mana status with no overflow therefore means 0, not an
             * unavailable stat.
             */
            if (mana.present()
                    && overflow.isEmpty()) {

                overflow =
                        OptionalDouble.of(
                                0.0D);
            }
        }

        return new Stats(
                health.current(),
                health.max(),
                defense,
                mana.current(),
                mana.max(),
                overflow,
                speed,
                vitality);
    }

    /**
     * Classic SkyBlock action bar after custom font icons are stripped:
     *
     * health/max  defense  mana/max  [overflow]  [speed]  [vitality/max]
     *
     * Speed still has a client-side live fallback because it is not guaranteed
     * to be present in every action-bar form.
     */
    private static PositionalStats positionalStats(
            String text,
            Pair knownHealth,
            Pair knownMana) {

        List<Token> tokens =
                tokenize(
                        text);

        List<Integer> pairIndexes =
                new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).pair()) {
                pairIndexes.add(i);
            }
        }

        Pair health =
                knownHealth != null
                        && knownHealth.present()
                        ? knownHealth
                        : Pair.empty();

        Pair mana =
                knownMana != null
                        && knownMana.present()
                        ? knownMana
                        : Pair.empty();

        Pair vitality =
                Pair.empty();

        OptionalDouble defense =
                OptionalDouble.empty();

        OptionalDouble overflow =
                OptionalDouble.empty();

        OptionalDouble speed =
                OptionalDouble.empty();

        int healthIndex = -1;
        int manaIndex = -1;
        int vitalityIndex = -1;

        for (int index : pairIndexes) {
            Token token =
                    tokens.get(
                            index);

            Pair pair =
                    new Pair(
                            token.current(),
                            token.max());

            if (looksLikeVanillaHearts(
                    pair)) {

                continue;
            }

            if (knownHealth != null
                    && knownHealth.present()
                    && samePair(
                            pair,
                            knownHealth)) {

                healthIndex = index;
                continue;
            }

            if (knownMana != null
                    && knownMana.present()
                    && samePair(
                            pair,
                            knownMana)) {

                manaIndex = index;
                continue;
            }

            if (!health.present()) {
                health = pair;
                healthIndex = index;
                continue;
            }

            if (!mana.present()
                    && !samePair(
                            pair,
                            health)) {

                mana = pair;
                manaIndex = index;
                continue;
            }

            if (manaIndex >= 0
                    && index > manaIndex
                    && !samePair(
                            pair,
                            health)
                    && !samePair(
                            pair,
                            mana)) {

                vitality = pair;
                vitalityIndex = index;
                break;
            }
        }

        if (health.present()
                && mana.present()
                && healthIndex >= 0
                && manaIndex > healthIndex) {

            for (int i =
                    healthIndex + 1;
                    i < manaIndex;
                    i++) {

                Token token =
                        tokens.get(i);

                if (!token.pair()
                        && token.current()
                        .isPresent()) {

                    defense =
                            token.current();

                    break;
                }
            }
        }

        if (mana.present()
                && manaIndex >= 0) {

            int end =
                    vitalityIndex > manaIndex
                            ? vitalityIndex
                            : tokens.size();

            List<OptionalDouble> after =
                    new ArrayList<>();

            for (int i =
                    manaIndex + 1;
                    i < end;
                    i++) {

                Token token =
                        tokens.get(i);

                if (!token.pair()
                        && token.current()
                        .isPresent()) {

                    after.add(
                            token.current());
                }
            }

            if (!after.isEmpty()) {
                overflow =
                        after.get(0);
            }

            if (after.size() > 1) {
                speed =
                        after.get(1);
            }
        }

        return new PositionalStats(
                health,
                mana,
                defense,
                overflow,
                speed,
                vitality.current());
    }

    private record PositionalStats(
            Pair health,
            Pair mana,
            OptionalDouble defense,
            OptionalDouble overflow,
            OptionalDouble speed,
            OptionalDouble vitality) {
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
        if (hideHealth || hideDefense || hideMana || hideOverflow || hideSpeed || hideVitality) {
            text = stripHudIconTokens(text);
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
            text = VITALITY_PAIR.matcher(text).replaceAll("");
            text = VITALITY.matcher(text).replaceAll("");
        }
        if (hideHealth || hideDefense || hideMana || hideOverflow || hideSpeed || hideVitality) {
            text = SKILL_TICK.matcher(text).replaceAll("");
        }
        if (hideHealth || hideMana) {
            text = UNLABELED_PAIR.matcher(text).replaceAll("");
        }
        text = WHITESPACE_2PLUS.matcher(text).replaceAll(" ").trim();
        if (text.isBlank()) {
            return Optional.empty();
        }
        if ((hideHealth || hideDefense || hideMana || hideOverflow || hideSpeed || hideVitality)
                && ONLY_NUMBER_PUNCTUATION.matcher(text).matches()) {
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
        return WHITESPACE_2PLUS.matcher(stripped).replaceAll(" ").trim();
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

    static String stripHudIconTokens(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return HUD_ICON_TOKEN.matcher(text).replaceAll(" ");
    }

    public static String stripFormatting(String raw) {
        if (raw == null) {
            return "";
        }
        String text = SECTION_CODE.matcher(raw).replaceAll("").replace('\u00A0', ' ');
        text = PRIVATE_USE_GLYPH.matcher(text).replaceAll(" ");
        return WHITESPACE_RUN.matcher(text).replaceAll(" ").trim();
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
