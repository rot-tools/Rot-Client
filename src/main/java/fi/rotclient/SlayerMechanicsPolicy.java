package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure rules for the smaller live Slayer mechanics. */
public final class SlayerMechanicsPolicy {
    public static final long COCOON_DURATION_MILLIS = 6_000L;
    public static final int MIN_DAGGER_DELAY_TICKS = 0;
    public static final int MAX_DAGGER_DELAY_TICKS = 10;
    public static final int MIN_DAGGER_VARIANCE_TICKS = 0;
    public static final int MAX_DAGGER_VARIANCE_TICKS = 10;
    public static final int VENGEANCE_DURATION_TICKS = 120;
    private static final Set<String> SOULCRY_KATANAS = Set.of(
            "VOIDEDGE_KATANA", "VORPAL_KATANA", "ATOMSPLIT_KATANA");
    private static final Pattern ATTUNEMENT_DISPLAY = Pattern.compile(
            "^(ASHEN|AURIC|SPIRIT|CRYSTAL)\\s+♨(\\d)\\s+\\d{2}:\\d{2}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VENGEANCE_DAMAGE = Pattern.compile(
            "^(\\d+(?:,\\d+)*)ﬗ$");

    public enum DaggerAttunement {
        ASHEN(0, Set.of(
                "HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER",
                "KINDLEBANE_DAGGER", "PYROCHAOS_DAGGER")),
        AURIC(1, Set.of(
                "HEARTFIRE_DAGGER", "BURSTFIRE_DAGGER", "FIREDUST_DAGGER",
                "KINDLEBANE_DAGGER", "PYROCHAOS_DAGGER")),
        SPIRIT(2, Set.of(
                "HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER",
                "MAWDREDGE_DAGGER", "DEATHRIPPER_DAGGER")),
        CRYSTAL(3, Set.of(
                "HEARTMAW_DAGGER", "BURSTMAW_DAGGER", "MAWDUST_DAGGER",
                "MAWDREDGE_DAGGER", "DEATHRIPPER_DAGGER"));

        private final int mode;
        private final Set<String> daggerIds;

        DaggerAttunement(int mode, Set<String> daggerIds) {
            this.mode = mode;
            this.daggerIds = Set.copyOf(daggerIds);
        }

        public int mode() {
            return mode;
        }

        boolean supports(String itemId) {
            return daggerIds.contains(normalizeId(itemId));
        }
    }

    public record LaserAnchor(
            double x,
            double y,
            double z,
            boolean owned,
            boolean carry) {
    }

    public record AttunementDisplay(
            DaggerAttunement attunement,
            int count,
            String formatted) {
    }

    public static final class CocoonTimer {
        private long expiresAtMillis;

        public boolean observe(String line, long nowMillis) {
            if (!SlayerPolicy.isCocooned(line)) {
                return false;
            }
            expiresAtMillis = Math.max(0L, nowMillis) + COCOON_DURATION_MILLIS;
            return true;
        }

        public long remainingMillis(long nowMillis) {
            long remaining = expiresAtMillis - Math.max(0L, nowMillis);
            if (remaining <= 0L) {
                expiresAtMillis = 0L;
                return 0L;
            }
            return remaining;
        }

        public boolean active(long nowMillis) {
            return remainingMillis(nowMillis) > 0L;
        }

        public void reset() {
            expiresAtMillis = 0L;
        }
    }

    public static final class DaggerSwapState {
        private DaggerAttunement lastObserved;
        private DaggerAttunement pending;
        private int remainingTicks = -1;

        /**
         * {@code sampledVarianceTicks} is supplied by the runtime so this state
         * stays deterministic in tests.
         */
        public boolean observe(String tag, int delayTicks, int sampledVarianceTicks) {
            Optional<DaggerAttunement> observed = daggerAttunement(tag);
            if (observed.isEmpty() || observed.get() == lastObserved) {
                return false;
            }
            lastObserved = observed.get();
            pending = observed.get();
            remainingTicks = clampDelay(delayTicks) + clampVariance(sampledVarianceTicks);
            return true;
        }

        public void tick() {
            if (pending != null && remainingTicks >= 0) {
                remainingTicks--;
            }
        }

        public Optional<DaggerAttunement> ready() {
            return pending != null && remainingTicks <= 0
                    ? Optional.of(pending)
                    : Optional.empty();
        }

        public void complete() {
            pending = null;
            remainingTicks = -1;
        }

        public void reset() {
            lastObserved = null;
            complete();
        }
    }

    public static final class SoulcryState {
        private int remainingTicks = -1;

        public boolean arm(int minDelayTicks, int maxDelayTicks, int sampledDelayTicks) {
            if (remainingTicks >= 0) {
                return false;
            }
            int min = clampSoulcryDelay(minDelayTicks);
            int max = Math.max(min, clampSoulcryDelay(maxDelayTicks));
            // Arm on the observation tick and start counting on the
            // following client tick. Keep that boundary explicit here.
            remainingTicks = Math.max(min, Math.min(max, sampledDelayTicks)) + 1;
            return true;
        }

        public void tick() {
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        }

        public boolean ready() {
            return remainingTicks == 0;
        }

        public void complete() {
            remainingTicks = -1;
        }

        public void reset() {
            complete();
        }
    }

    public static final class VengeanceTimer {
        private int remainingTicks;

        public void start() {
            remainingTicks = VENGEANCE_DURATION_TICKS;
        }

        public void tick() {
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        }

        public boolean active() {
            return remainingTicks > 0;
        }

        public int remainingTicks() {
            return remainingTicks;
        }

        public String display(boolean ticks) {
            if (!active()) {
                return "";
            }
            return ticks
                    ? Integer.toString(remainingTicks)
                    : String.format(Locale.ROOT, "%.1fs", remainingTicks / 20.0D);
        }

        public void reset() {
            remainingTicks = 0;
        }
    }

    private SlayerMechanicsPolicy() {
    }

    public static Optional<DaggerAttunement> daggerAttunement(String rawTag) {
        String tag = normalizeText(rawTag).toUpperCase(Locale.ROOT);
        for (DaggerAttunement value : DaggerAttunement.values()) {
            if (tag.contains(value.name() + " ♨")) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static boolean supportsDagger(String itemId, DaggerAttunement attunement) {
        return attunement != null && attunement.supports(itemId);
    }

    public static boolean isSoulcryKatana(String itemId) {
        return SOULCRY_KATANAS.contains(normalizeId(itemId));
    }

    public static int soulcryManaCost(boolean ultimateWise) {
        return ultimateWise ? 100 : 200;
    }

    public static boolean hasSoulcryMana(
            double mana,
            double overflowMana,
            boolean ultimateWise) {
        return Math.max(0.0D, mana) + Math.max(0.0D, overflowMana)
                >= soulcryManaCost(ultimateWise);
    }

    public static int clampSoulcryDelay(int ticks) {
        return Math.max(0, Math.min(5, ticks));
    }

    public static Optional<AttunementDisplay> attunementDisplay(
            String rawTag,
            boolean includeCount) {
        Matcher matcher = ATTUNEMENT_DISPLAY.matcher(normalizeText(rawTag));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        DaggerAttunement attunement = DaggerAttunement.valueOf(
                matcher.group(1).toUpperCase(Locale.ROOT));
        int count = Integer.parseInt(matcher.group(2));
        String color = switch (attunement) {
            case ASHEN -> "§l§8";
            case AURIC -> "§l§e";
            case SPIRIT -> "§l§f";
            case CRYSTAL -> "§l§b";
        };
        return Optional.of(new AttunementDisplay(
                attunement,
                count,
                color + attunement.name() + (includeCount ? " ♨" + count : "")));
    }

    public static boolean isVoidgloomNoise(String soundId) {
        String normalized = soundId == null
                ? ""
                : soundId.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        return normalized.equals("entity.enderman.stare")
                || normalized.equals("entity.enderman.scream");
    }

    /**
     * The Vengeance timer starts from a stripped nametag that
     * {@code contains("ASHEN ♨7")}. The attunement HUD regex also requires a
     * {@code mm:ss} suffix, so this stay a separate, looser match.
     */
    public static boolean isVengeanceStartTag(String rawTag) {
        return normalizeText(rawTag).toUpperCase(Locale.ROOT).contains("ASHEN ♨7");
    }

    public static Optional<Long> vengeanceDamage(String entityName) {
        Matcher matcher = VENGEANCE_DAMAGE.matcher(normalizeText(entityName));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            long damage = Long.parseLong(matcher.group(1).replace(",", ""));
            return damage >= 500_000L ? Optional.of(damage) : Optional.empty();
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static String abbreviateDamage(long damage) {
        double value = Math.max(0L, damage);
        String suffix = "";
        if (value >= 1_000_000_000.0D) {
            value /= 1_000_000_000.0D;
            suffix = "B";
        } else if (value >= 1_000_000.0D) {
            value /= 1_000_000.0D;
            suffix = "M";
        } else if (value >= 1_000.0D) {
            value /= 1_000.0D;
            suffix = "K";
        }
        if (suffix.isEmpty()) {
            return Long.toString(Math.max(0L, damage));
        }
        String formatted = String.format(Locale.ROOT, "%.2f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return formatted + suffix;
    }

    public static boolean shouldHideLaser(
            double guardianX,
            double guardianY,
            double guardianZ,
            List<LaserAnchor> anchors,
            boolean showForCarries) {
        if (anchors == null || anchors.isEmpty()) {
            return false;
        }
        for (LaserAnchor anchor : anchors) {
            if (anchor == null || anchor.owned() || (anchor.carry() && !showForCarries)) {
                continue;
            }
            if (Math.abs(guardianX - anchor.x()) <= 0.5D
                    && Math.abs(guardianZ - anchor.z()) <= 0.5D
                    && Math.abs(guardianY - anchor.y()) <= 5.0D) {
                return true;
            }
        }
        return false;
    }

    public static int clampDelay(int ticks) {
        return Math.max(MIN_DAGGER_DELAY_TICKS, Math.min(MAX_DAGGER_DELAY_TICKS, ticks));
    }

    public static int clampVariance(int ticks) {
        return Math.max(MIN_DAGGER_VARIANCE_TICKS, Math.min(MAX_DAGGER_VARIANCE_TICKS, ticks));
    }

    public static String formatAlertText(String configured) {
        String value = configured == null || configured.isBlank()
                ? "<red>Boss cocooned!"
                : configured.trim();
        return value
                .replace("<dark_red>", "§4")
                .replace("<red>", "§c")
                .replace("<gold>", "§6")
                .replace("<yellow>", "§e")
                .replace("<green>", "§a")
                .replace("<aqua>", "§b")
                .replace("<blue>", "§9")
                .replace("<light_purple>", "§d")
                .replace("<white>", "§f")
                .replace("<gray>", "§7")
                .replace("<dark_gray>", "§8")
                .replace("<reset>", "§r");
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("§[0-9A-FK-ORa-fk-or]", "").trim();
    }
}
