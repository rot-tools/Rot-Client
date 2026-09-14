package fi.rotclient;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure Plus-only rules for automatic Slayer item use and swapping. */
public final class SlayerAutomationPolicy {
    public static final int MIN_DAGGER_DELAY_TICKS = 0;
    public static final int MAX_DAGGER_DELAY_TICKS = 10;
    public static final int MIN_DAGGER_VARIANCE_TICKS = 0;
    public static final int MAX_DAGGER_VARIANCE_TICKS = 10;
    public static final int SOULCRY_ABILITY_COOLDOWN_TICKS = 80;

    private static final Set<String> SOULCRY_KATANAS = Set.of(
            "VOIDEDGE_KATANA", "VORPAL_KATANA", "ATOMSPLIT_KATANA");
    private static final Pattern ABILITY_COOLDOWN_CHAT = Pattern.compile(
            "(?i)this ability is on cooldown(?: for)?\\s+([0-9]+(?:\\.[0-9]+)?)s");

    private SlayerAutomationPolicy() {
    }

    public static final class DaggerSwapState {
        private SlayerMechanicsPolicy.DaggerAttunement lastObserved;
        private SlayerMechanicsPolicy.DaggerAttunement pending;
        private int remainingTicks = -1;

        public boolean observe(String tag, int delayTicks, int sampledVarianceTicks) {
            Optional<SlayerMechanicsPolicy.DaggerAttunement> observed =
                    SlayerMechanicsPolicy.daggerAttunement(tag);
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

        public Optional<SlayerMechanicsPolicy.DaggerAttunement> ready() {
            return pending != null && remainingTicks <= 0 ? Optional.of(pending) : Optional.empty();
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

    public static final class SoulcryAbilityGate {
        private int remainingTicks;

        public void tick() {
            if (remainingTicks > 0) {
                remainingTicks--;
            }
        }

        public boolean ready(boolean itemOnCooldown) {
            return remainingTicks <= 0 && !itemOnCooldown;
        }

        public void markUsed() {
            remainingTicks = SOULCRY_ABILITY_COOLDOWN_TICKS;
        }

        public void observeRemaining(int ticks) {
            remainingTicks = Math.max(remainingTicks, Math.max(0, ticks));
        }

        public int remainingTicks() {
            return remainingTicks;
        }

        public void reset() {
            remainingTicks = 0;
        }
    }

    public static boolean supportsDagger(
            String itemId,
            SlayerMechanicsPolicy.DaggerAttunement attunement) {
        return attunement != null && attunement.supports(itemId);
    }

    public static boolean isSoulcryKatana(String itemId) {
        return SOULCRY_KATANAS.contains(normalizeId(itemId));
    }

    public static int soulcryManaCost(boolean ultimateWise) {
        return ultimateWise ? 100 : 200;
    }

    public static boolean hasSoulcryMana(double mana, double overflowMana, boolean ultimateWise) {
        return Math.max(0.0D, mana) + Math.max(0.0D, overflowMana)
                >= soulcryManaCost(ultimateWise);
    }

    public static int clampSoulcryDelay(int ticks) {
        return Math.max(0, Math.min(5, ticks));
    }

    public static int clampDelay(int ticks) {
        return Math.max(MIN_DAGGER_DELAY_TICKS, Math.min(MAX_DAGGER_DELAY_TICKS, ticks));
    }

    public static int clampVariance(int ticks) {
        return Math.max(MIN_DAGGER_VARIANCE_TICKS, Math.min(MAX_DAGGER_VARIANCE_TICKS, ticks));
    }

    public static OptionalInt abilityCooldownTicks(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return OptionalInt.empty();
        }
        Matcher matcher = ABILITY_COOLDOWN_CHAT.matcher(rawLine.replaceAll("§.", "").trim());
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        try {
            double seconds = Double.parseDouble(matcher.group(1));
            if (!(seconds > 0.0D) || !Double.isFinite(seconds)) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(Math.min(20 * 30, Math.max(1, (int) Math.ceil(seconds * 20.0D))));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
