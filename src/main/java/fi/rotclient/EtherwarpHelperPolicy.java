package fi.rotclient;

/**
 * Etherwarp helper: left-click uses the item when sneaking, or after a
 * brief auto-shift if that option is on.
 */
public final class EtherwarpHelperPolicy {
    public static final int MIN_SHIFT_TICKS = 2;
    public static final int MAX_SHIFT_TICKS = 4;

    private EtherwarpHelperPolicy() {
    }

    public static boolean isEtherwarpItem(boolean ethermerge, String skyBlockId) {
        if (ethermerge) {
            return true;
        }
        return skyBlockId != null && skyBlockId.equalsIgnoreCase("ETHERWARP_CONDUIT");
    }

    public static boolean shouldHandleLeftClick(
            boolean moduleEnabled,
            boolean leftClickWarp,
            boolean noScreen,
            boolean leftButton,
            boolean etherwarpItem) {
        return moduleEnabled
                && leftClickWarp
                && noScreen
                && leftButton
                && etherwarpItem;
    }

    public static boolean needsAutoShift(boolean alreadySneaking, boolean shiftAutomatically) {
        return !alreadySneaking && shiftAutomatically;
    }

    public static boolean canWarpNow(boolean alreadySneaking, boolean shiftAutomatically) {
        return alreadySneaking || shiftAutomatically;
    }

    /**
     * Auto-shift and left-click warp fire only when the look ray hits a
     * physical block with clear stand space. Aiming at air or a blocked pad
     * must not sneak or consume the click.
     */
    public static boolean shouldAssistLookTarget(boolean validWarpTarget) {
        return validWarpTarget;
    }

    public static boolean forceSneakInput(boolean holdingSneakForWarp) {
        return holdingSneakForWarp;
    }

    public static int shiftHoldTicks(double random01) {
        double roll = Math.max(0.0D, Math.min(1.0D, random01));
        return MIN_SHIFT_TICKS
                + (int) Math.round((MAX_SHIFT_TICKS - MIN_SHIFT_TICKS) * roll);
    }
}
