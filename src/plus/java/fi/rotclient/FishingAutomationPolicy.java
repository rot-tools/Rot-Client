package fi.rotclient;

/** Pure Plus-only decisions for automatic sea-creature attacks. */
public final class FishingAutomationPolicy {
    public static final int MAX_AUTO_DELAY = 40;

    private FishingAutomationPolicy() {
    }

    public static int clampAutoDelay(int ticks) {
        return Math.max(1, Math.min(MAX_AUTO_DELAY, ticks));
    }

    public static boolean shouldAutoAttack(
            boolean moduleEnabled,
            boolean autoAttack,
            boolean lookingAtTracked,
            boolean screenOpen) {
        return moduleEnabled && autoAttack && lookingAtTracked && !screenOpen;
    }
}
