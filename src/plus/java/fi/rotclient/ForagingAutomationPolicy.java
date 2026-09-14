package fi.rotclient;

/** Pure Plus-only decisions for automatic beacon, chop, and axe-toss actions. */
public final class ForagingAutomationPolicy {
    public record AutoBeaconClick(int slot, boolean rightClick) {
    }

    private ForagingAutomationPolicy() {
    }

    public static AutoBeaconClick nextBeaconClick(
            boolean moduleEnabled,
            boolean cheatClick,
            String title,
            ForagingPolicy.BeaconHint hint) {
        if (hint == null || !ForagingPolicy.isBeaconTuneTitle(title)) {
            return null;
        }
        int remaining = firstPendingClicks(hint);
        if (!(moduleEnabled && cheatClick && remaining != 0)) {
            return null;
        }
        if (hint.colorClicks() != 0) {
            return new AutoBeaconClick(
                    ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_COLOR_SLOT),
                    hint.colorClicks() > 0);
        }
        if (hint.speedClicks() != 0) {
            return new AutoBeaconClick(
                    ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_SPEED_SLOT),
                    hint.speedClicks() > 0);
        }
        return new AutoBeaconClick(
                ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_PITCH_SLOT),
                hint.pitchClicks() > 0);
    }

    public static ForagingPolicy.BeaconHint applyPress(
            ForagingPolicy.BeaconHint hint, int slot, boolean rightClick, String title) {
        if (hint == null) {
            return null;
        }
        int colorSlot = ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_COLOR_SLOT);
        int speedSlot = ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_SPEED_SLOT);
        int pitchSlot = ForagingPolicy.beaconSlot(title, ForagingPolicy.BEACON_PITCH_SLOT);
        int color = hint.colorClicks();
        int speed = hint.speedClicks();
        int pitch = hint.pitchClicks();
        if (slot == colorSlot) {
            color = remainingAfterDirectedClick(
                    color, ForagingPolicy.COLOR_CYCLE_LENGTH, rightClick);
        } else if (slot == speedSlot) {
            speed = remainingAfterDirectedClick(
                    speed, ForagingPolicy.SPEED_CYCLE_LENGTH, rightClick);
        } else if (slot == pitchSlot) {
            pitch = remainingAfterDirectedClick(
                    pitch, ForagingPolicy.PITCH_CYCLE_LENGTH, rightClick);
        }
        return new ForagingPolicy.BeaconHint(color, speed, pitch);
    }

    public static int remainingAfterDirectedClick(
            int remaining, int cycleLength, boolean rightClick) {
        int delta = rightClick ? 1 : -1;
        int next = remaining - delta;
        return ForagingPolicy.shortestCycleClicks(
                0, Math.floorMod(next, cycleLength), cycleLength);
    }

    public static boolean shouldAutoChop(
            boolean moduleEnabled,
            boolean cheatEnabled,
            ForagingPolicy.Island island,
            boolean holdingAxe,
            boolean lookingAtLog) {
        return moduleEnabled
                && cheatEnabled
                && ForagingPolicy.chopTrees(island)
                && holdingAxe
                && lookingAtLog;
    }

    public static boolean shouldAxeToss(
            boolean moduleEnabled,
            boolean cheatEnabled,
            boolean throwableAxe,
            int clusterSize,
            int minCluster,
            boolean cooldownReady) {
        return moduleEnabled
                && cheatEnabled
                && throwableAxe
                && cooldownReady
                && clusterSize >= Math.max(1, minCluster);
    }

    private static int firstPendingClicks(ForagingPolicy.BeaconHint hint) {
        if (hint.colorClicks() != 0) {
            return hint.colorClicks();
        }
        if (hint.speedClicks() != 0) {
            return hint.speedClicks();
        }
        return hint.pitchClicks();
    }
}
