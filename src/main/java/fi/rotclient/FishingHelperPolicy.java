package fi.rotclient;

import java.util.regex.Pattern;

/**
 * Fishing helper: pull on a nearby {@code !!!} hologram, then recast.
 */
public final class FishingHelperPolicy {
    private static final Pattern HOOK_TIMER = Pattern.compile("^\\d{1,2}(?:\\.\\d)?$");
    public static final float HOOK_TIMER_MAX_SECONDS = 20.0F;
    public static final int DEFAULT_PULL_DELAY = 1;
    public static final int DEFAULT_RECAST_DELAY = 1;
    public static final int MAX_DELAY = 40;
    public static final float BITE_RANGE = 3.0F;
    public static final int RECAST_CHECK_PERIOD_TICKS = 300;

    private FishingHelperPolicy() {
    }

    public static int clampDelay(int ticks) {
        return Math.max(0, Math.min(MAX_DELAY, ticks));
    }

    public static boolean isBiteHologram(String strippedName) {
        if (strippedName == null) {
            return false;
        }
        String text = ChatTextPolicy.stripFormatting(strippedName).trim();
        return text.equals("!!!");
    }

    public static boolean isHookTimerHologram(String strippedName) {
        if (isBiteHologram(strippedName)) {
            return true;
        }
        return parseHookSeconds(strippedName) != null;
    }

    public static Float parseHookSeconds(String strippedName) {
        if (strippedName == null) {
            return null;
        }
        String text = ChatTextPolicy.stripFormatting(strippedName).trim();
        if (text.equals("!!!")) {
            return 0.0F;
        }
        if (!HOOK_TIMER.matcher(text).matches()) {
            return null;
        }
        try {
            float seconds = Float.parseFloat(text);
            if (seconds < 0.0F || seconds > HOOK_TIMER_MAX_SECONDS) {
                return null;
            }
            return seconds;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean shouldShowBiteTitle(
            boolean moduleEnabled,
            boolean biteTitle,
            boolean holdingRod,
            boolean hasHook,
            boolean biteNearby) {
        return moduleEnabled && biteTitle && holdingRod && hasHook && biteNearby;
    }

    public static boolean shouldHideHookNametag(boolean moduleEnabled, boolean hideNametag, String name) {
        return moduleEnabled && hideNametag && isHookTimerHologram(name);
    }

    public static boolean shouldPull(
            boolean moduleEnabled,
            boolean autoPull,
            boolean holdingRod,
            boolean hasHook,
            boolean biteNearby) {
        return moduleEnabled && autoPull && holdingRod && hasHook && biteNearby;
    }

    public static int pullDelayTicks(int delay, int variance, double random01) {
        return delayTicks(delay, variance, random01);
    }

    public static int recastDelayTicks(int delay, int variance, double random01) {
        return 2 + delayTicks(delay, variance, random01);
    }

    public static boolean shouldRecastCheck(
            boolean moduleEnabled,
            boolean recast,
            boolean recastCheck,
            boolean holdingRod,
            boolean hookAlive) {
        return moduleEnabled && recast && recastCheck && holdingRod && !hookAlive;
    }

    static int delayTicks(int delay, int variance, double random01) {
        int base = clampDelay(delay);
        int spread = Math.max(0, clampDelay(variance));
        if (spread <= 0) {
            return base;
        }
        double roll = Math.max(0.0D, Math.min(1.0D, random01));
        return base + (int) Math.round(spread * roll);
    }
}
