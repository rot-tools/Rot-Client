package fi.rotclient;

/** Pure visibility rules shared by Slayer highlight boxes and target lines. */
public final class SlayerHighlightPolicy {
    /** Yang Glyph, BOOM, egg sacs, pups, pillars and vampire markers stay inside the local fight. */
    public static final double LOCAL_FIGHT_MARKER_RANGE = 28.0D;
    /** Thrown Yang Glyphs often land farther from the boss than other fight markers. */
    public static final double YANG_GLYPH_PLAYER_RANGE = 48.0D;

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
        return shouldHighlight(role, null, owned, false, options);
    }

    /**
     * Every Slayer family uses the local-player fight. Minibosses often have no
     * owner hologram, so they may render while that family's quest is active.
     * Other players' bosses never render.
     */
    public static boolean shouldHighlight(
            SlayerPolicy.EntityRole role,
            SlayerPolicy.SlayerType type,
            boolean owned,
            boolean localQuestSameFamily,
            Options options) {
        if (role == null || options == null) {
            return false;
        }
        boolean mineOnly = options.onlyMine() || type != null;
        if (mineOnly) {
            if (role == SlayerPolicy.EntityRole.MINIBOSS) {
                if (!owned && !localQuestSameFamily) {
                    return false;
                }
            } else if (!owned) {
                return false;
            }
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
        return shouldDrawTargetLine(role, null, owned, false, distance, options);
    }

    public static boolean shouldDrawTargetLine(
            SlayerPolicy.EntityRole role,
            SlayerPolicy.SlayerType type,
            boolean owned,
            boolean localQuestSameFamily,
            double distance,
            Options options) {
        return options != null
                && options.targetLines()
                && shouldHighlight(role, type, owned, localQuestSameFamily, options)
                && Double.isFinite(distance)
                && distance >= 0.0D
                && distance <= clampTargetLineDistance(options.maxTargetLineDistance());
    }

    public static boolean shouldDrawLocalFightMarker(
            boolean hasOwnedBoss,
            double distanceToOwnedBoss) {
        return shouldDrawLocalFightMarker(hasOwnedBoss, distanceToOwnedBoss, false, Double.NaN);
    }

    /**
     * Local-quest markers near the player still count when the owned boss is
     * across the arena, which is how thrown Yang Glyphs land.
     */
    public static boolean shouldDrawLocalFightMarker(
            boolean hasOwnedBoss,
            double distanceToOwnedBoss,
            boolean localQuestActive,
            double distanceToPlayer) {
        if (hasOwnedBoss
                && Double.isFinite(distanceToOwnedBoss)
                && distanceToOwnedBoss >= 0.0D
                && distanceToOwnedBoss <= LOCAL_FIGHT_MARKER_RANGE) {
            return true;
        }
        return localQuestActive
                && Double.isFinite(distanceToPlayer)
                && distanceToPlayer >= 0.0D
                && distanceToPlayer <= LOCAL_FIGHT_MARKER_RANGE;
    }

    public static boolean shouldTrackYangGlyph(
            boolean hasOwnedBoss,
            double distanceToOwnedBoss,
            boolean localQuestActive,
            double distanceToPlayer) {
        if (shouldDrawLocalFightMarker(
                hasOwnedBoss, distanceToOwnedBoss, localQuestActive, distanceToPlayer)) {
            return true;
        }
        return localQuestActive
                && Double.isFinite(distanceToPlayer)
                && distanceToPlayer >= 0.0D
                && distanceToPlayer <= YANG_GLYPH_PLAYER_RANGE;
    }

    public static double clampTargetLineDistance(double value) {
        return Math.max(4.0D, Math.min(64.0D, value));
    }
}
