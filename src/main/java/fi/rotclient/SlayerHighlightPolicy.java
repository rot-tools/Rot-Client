package fi.rotclient;

/** Pure visibility rules shared by Slayer highlight boxes and target lines. */
public final class SlayerHighlightPolicy {
    public record Options(
            boolean onlyMine,
            boolean bosses,
            boolean minibosses,
            boolean demons,
            boolean targetLines,
            double maxTargetLineDistance) {
    }

    private SlayerHighlightPolicy() {
    }

    public static boolean shouldHighlight(
            SlayerPolicy.EntityRole role,
            boolean owned,
            Options options) {
        if (role == null || options == null || (options.onlyMine() && !owned)) {
            return false;
        }
        return switch (role) {
            case BOSS -> options.bosses();
            case MINIBOSS -> options.minibosses();
            case DEMON -> options.demons();
            default -> false;
        };
    }

    public static boolean shouldDrawTargetLine(
            SlayerPolicy.EntityRole role,
            boolean owned,
            double distance,
            Options options) {
        return options != null
                && options.targetLines()
                && shouldHighlight(role, owned, options)
                && Double.isFinite(distance)
                && distance >= 0.0D
                && distance <= clampTargetLineDistance(options.maxTargetLineDistance());
    }

    public static double clampTargetLineDistance(double value) {
        return Math.max(4.0D, Math.min(64.0D, value));
    }
}
