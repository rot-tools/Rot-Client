package fi.rotclient;

/**
 * Pure Hide Players render filter. Local player must never be hidden.
 *
 * Semantics:
 * - Hide All ON → hide all eligible remote players
 * - Hide All OFF → hide remotes within configured distance
 * - Only in Dungeons ON → apply only when dungeon state is confidently true;
 *   outside / unknown dungeon state fails open (render)
 */
public final class HidePlayersPolicy {
    public static final double MIN_DISTANCE = 1.0D;
    public static final double MAX_DISTANCE = 128.0D;
    public static final double DEFAULT_DISTANCE = 32.0D;

    private HidePlayersPolicy() {
    }

    public static double clampDistance(double distance) {
        if (!Double.isFinite(distance)) {
            return DEFAULT_DISTANCE;
        }
        if (distance < MIN_DISTANCE) {
            return MIN_DISTANCE;
        }
        if (distance > MAX_DISTANCE) {
            return MAX_DISTANCE;
        }
        return distance;
    }

    public static boolean shouldHideRemotePlayer(
            boolean moduleEnabled,
            boolean onlyInDungeons,
            boolean confidentlyInDungeon,
            boolean hideAll,
            double distanceBlocks,
            double distanceToRemoteBlocks,
            boolean isLocalPlayer) {
        if (!moduleEnabled || isLocalPlayer) {
            return false;
        }
        if (onlyInDungeons && !confidentlyInDungeon) {
            return false;
        }
        if (hideAll) {
            return true;
        }
        double safeDistance = clampDistance(distanceBlocks);
        return distanceToRemoteBlocks >= 0.0D
                && distanceToRemoteBlocks <= safeDistance;
    }
}
