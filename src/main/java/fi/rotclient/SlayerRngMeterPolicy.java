package fi.rotclient;

/** Pure guard for the optional, local RNG Meter empty-selection warning. */
public final class SlayerRngMeterPolicy {
    private SlayerRngMeterPolicy() {
    }

    public static boolean shouldWarnEmpty(boolean enabled, boolean rngMeterUpdate, boolean selectedDropKnown) {
        return enabled && rngMeterUpdate && !selectedDropKnown;
    }

    /**
     * The server's standard Stored XP status line is safe to hide only after a
     * known selection has been observed locally.  An unknown selection must
     * remain visible so the player can diagnose the missing mapping.
     */
    public static boolean shouldHideChat(boolean enabled, boolean rngMeterUpdate, boolean selectedDropKnown) {
        return enabled && rngMeterUpdate && selectedDropKnown;
    }
}
