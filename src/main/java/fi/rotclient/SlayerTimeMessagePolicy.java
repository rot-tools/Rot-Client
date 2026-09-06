package fi.rotclient;

/** When a Slayer kill may print time-to-kill / personal-best chat. */
public final class SlayerTimeMessagePolicy {
    public static final long MIN_KILL_DURATION_MILLIS = 400L;
    public static final long ANNOUNCE_COOLDOWN_MILLIS = 1_500L;

    private SlayerTimeMessagePolicy() {
    }

    public static boolean shouldAnnounce(
            boolean featureEnabled,
            boolean lineEnabled,
            boolean bossKilled,
            boolean owned,
            SlayerPolicy.EntityRole role,
            long durationMillis,
            long nowMillis,
            long lastAnnounceAtMillis) {
        if (!featureEnabled || !lineEnabled || !bossKilled || !owned) {
            return false;
        }
        if (role != SlayerPolicy.EntityRole.BOSS) {
            return false;
        }
        if (durationMillis < MIN_KILL_DURATION_MILLIS) {
            return false;
        }
        return nowMillis - lastAnnounceAtMillis >= ANNOUNCE_COOLDOWN_MILLIS;
    }

    public static boolean shouldAnnounce(
            boolean featureEnabled,
            boolean lineEnabled,
            boolean bossKilled,
            boolean owned,
            SlayerPolicy.EntityRole role,
            long durationMillis,
            long nowMillis,
            long lastAnnounceAtMillis,
            SlayerPolicy.EntityDescriptor descriptor) {
        if (SlayerFightPolicy.isTarantulaTierFivePhaseOne(descriptor)) {
            return false;
        }
        return shouldAnnounce(
                featureEnabled,
                lineEnabled,
                bossKilled,
                owned,
                role,
                durationMillis,
                nowMillis,
                lastAnnounceAtMillis);
    }
}
