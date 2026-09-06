package fi.rotclient;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the stable Slayer combat-XP fraction from action bar, sidebar, or tab. */
public final class SlayerProgressPolicy {
    private static final String AMOUNT = "([\\d,.]+[kKmM]?)";
    private static final Pattern LABEL_THEN_FRACTION = Pattern.compile(
            "(?i)(?:combat\\s*xp|slayer\\s*xp)\\s*:?\\s*"
                    + AMOUNT + "\\s*/\\s*" + AMOUNT);
    private static final Pattern FRACTION_THEN_LABEL = Pattern.compile(
            "(?i)" + AMOUNT + "\\s*/\\s*" + AMOUNT
                    + "\\s*(?:combat\\s*xp|slayer\\s*xp)\\b");
    /**
     * SkyBlock's active-quest action bar has also appeared as a bare fraction
     * wrapped in parentheses. The runtime still requires an active verified
     * quest before it accepts this permissive form.
     */
    private static final Pattern PARENTHESIZED_FRACTION = Pattern.compile(
            "\\(\\s*" + AMOUNT + "\\s*/\\s*" + AMOUNT + "\\s*\\)");

    public record Progress(long earnedXp, long requiredXp) {
        public Progress {
            earnedXp = Math.max(0L, earnedXp);
            requiredXp = Math.max(1L, requiredXp);
        }

        public int percent() {
            if (earnedXp >= requiredXp) {
                return 100;
            }
            return (int) Math.max(0L, Math.min(100L,
                    Math.floor((double) earnedXp * 100.0D / (double) requiredXp)));
        }

        public long remainingXp() {
            return Math.max(0L, requiredXp - earnedXp);
        }
    }

    private SlayerProgressPolicy() {
    }

    /** Stateful, quest-scoped boss-spawn warning latch. */
    public static final class ThresholdState {
        private Progress previous;
        private long requiredXp = -1L;
        private boolean warned;
        private long lastWarnedEarnedXp = -1L;

        /**
         * Accepts a value only while a verified Slayer quest is active. The
         * threshold is deliberately strict: exactly 80% does not warn at an
         * 80% setting. An initial value already above the threshold is a real
         * warning candidate, matching the in-game action-bar behaviour.
         */
        public boolean observe(
                boolean activeQuest,
                Progress current,
                int thresholdPercent,
                boolean repeat) {
            if (!activeQuest || current == null) {
                if (!activeQuest) {
                    reset();
                }
                return false;
            }
            int threshold = clampThreshold(thresholdPercent);
            if (requiredXp != current.requiredXp()) {
                requiredXp = current.requiredXp();
                warned = false;
                lastWarnedEarnedXp = -1L;
            }
            previous = current;
            if (!strictlyAbove(current, threshold)) {
                warned = false;
                lastWarnedEarnedXp = -1L;
                return false;
            }
            if (!warned) {
                warned = true;
                lastWarnedEarnedXp = current.earnedXp();
                return true;
            }
            if (repeat && current.earnedXp() > lastWarnedEarnedXp) {
                lastWarnedEarnedXp = current.earnedXp();
                return true;
            }
            return false;
        }

        public void reset() {
            previous = null;
            requiredXp = -1L;
            warned = false;
            lastWarnedEarnedXp = -1L;
        }

        public Progress previous() {
            return previous;
        }
    }

    /**
     * Combat-skill totals such as {@code (19,896,487/0)} are not quest XP.
     * Sidebar quest fractions stay well below this cap.
     */
    public static final long MAX_QUEST_XP = 500_000L;
    private static final Pattern BARE_FRACTION = Pattern.compile(
            "^\\s*" + AMOUNT + "\\s*/\\s*" + AMOUNT + "\\s*$");

    public static Optional<Progress> parse(String raw) {
        return parse(raw, false);
    }

    public static boolean showsQuest(List<String> lines) {
        if (lines == null) {
            return false;
        }
        for (String line : lines) {
            String text = SlayerPolicy.normalize(line).toLowerCase(Locale.ROOT);
            if (text.contains("slayer quest")
                    || text.contains("combat xp")
                    || text.contains("slayer xp")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads Combat XP from a block of HUD lines. A nearby {@code Slayer Quest}
     * header unlocks the same bare {@code 2,403/3,000} form used on the sidebar.
     * Labeled Combat XP wins over a bare fraction on the same snapshot.
     */
    public static Optional<Progress> parseLines(List<String> lines, boolean questVisible) {
        if (lines == null || lines.isEmpty()) {
            return Optional.empty();
        }
        boolean visible = questVisible || showsQuest(lines);
        Optional<Progress> labeled = Optional.empty();
        Optional<Progress> any = Optional.empty();
        for (String line : lines) {
            Optional<Progress> parsed = parse(line, visible);
            if (parsed.isEmpty()) {
                continue;
            }
            any = parsed;
            String text = SlayerPolicy.normalize(line).toLowerCase(Locale.ROOT);
            if (text.contains("combat") || text.contains("slayer xp") || text.contains("slayer quest")) {
                labeled = parsed;
            }
        }
        return labeled.isPresent() ? labeled : any;
    }

    /** Keep the higher completion while the required XP stays the same quest. */
    public static Progress preferFresh(Progress current, Progress incoming) {
        if (incoming == null) {
            return current;
        }
        if (current == null || current.requiredXp() != incoming.requiredXp()) {
            return incoming;
        }
        return incoming.earnedXp() >= current.earnedXp() ? incoming : current;
    }

    /**
     * @param questVisible when true, a short sidebar {@code 2,403/3,000} line
     *                     is accepted. Action-bar health/mana pairs stay rejected.
     */
    public static Optional<Progress> parse(String raw, boolean questVisible) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = SlayerPolicy.normalize(raw);
        Matcher matcher = LABEL_THEN_FRACTION.matcher(normalized);
        boolean labeled = matcher.find();
        if (!labeled) {
            matcher = FRACTION_THEN_LABEL.matcher(normalized);
            labeled = matcher.find();
        }
        if (!labeled) {
            matcher = PARENTHESIZED_FRACTION.matcher(normalized);
            if (!matcher.find()) {
                return questVisible ? parseBareFraction(normalized) : Optional.empty();
            }
        }
        try {
            long earned = parseAmount(matcher.group(1));
            long required = parseAmount(matcher.group(2));
            return toProgress(earned, required);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    static Optional<Progress> parseBareFraction(String normalized) {
        Matcher matcher = BARE_FRACTION.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            long earned = parseAmount(matcher.group(1));
            long required = parseAmount(matcher.group(2));
            if (required < 100L || required % 50L != 0L) {
                return Optional.empty();
            }
            return toProgress(earned, required);
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<Progress> toProgress(long earned, long required) {
        if (required <= 0L || required > MAX_QUEST_XP) {
            return Optional.empty();
        }
        return Optional.of(new Progress(earned, required));
    }

    public static int clampThreshold(int thresholdPercent) {
        return Math.max(50, Math.min(90, thresholdPercent));
    }

    private static boolean strictlyAbove(Progress progress, int threshold) {
        return BigInteger.valueOf(progress.earnedXp())
                .multiply(BigInteger.valueOf(100L))
                .compareTo(BigInteger.valueOf(progress.requiredXp())
                        .multiply(BigInteger.valueOf(threshold))) > 0;
    }

    private static long parseAmount(String raw) {
        String value = raw == null ? "" : raw.trim().replace(",", "");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("empty amount");
        }
        char suffix = Character.toUpperCase(value.charAt(value.length() - 1));
        long multiplier = 1L;
        if (suffix == 'K' || suffix == 'M') {
            multiplier = suffix == 'K' ? 1_000L : 1_000_000L;
            value = value.substring(0, value.length() - 1);
        }
        double parsed = Double.parseDouble(value);
        if (!Double.isFinite(parsed) || parsed < 0.0D
                || parsed > Long.MAX_VALUE / (double) multiplier) {
            throw new IllegalArgumentException("invalid amount");
        }
        return Math.round(parsed * multiplier);
    }
}
