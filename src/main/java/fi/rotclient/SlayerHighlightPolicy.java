package fi.rotclient;

/** Pure visibility rules shared by Slayer highlight boxes and target lines. */
public final class SlayerHighlightPolicy {
    /** Yang Glyph, BOOM, egg sacs, pups, pillars and vampire markers stay inside the local fight. */
    public static final double LOCAL_FIGHT_MARKER_RANGE = 28.0D;
    /** Thrown Yang Glyphs often land farther from the boss than other fight markers. */
    public static final double YANG_GLYPH_PLAYER_RANGE = 48.0D;
    /** A newly thrown Yang Glyph stand still sits next to the owned Voidgloom. */
    public static final double YANG_GLYPH_THROW_ORIGIN_RANGE = 12.0D;

    public record Options(
            boolean onlyMine,
            boolean bosses,
            boolean minibosses,
            boolean demons,
            boolean targetLines,
            double maxTargetLineDistance) {
    }

    /**
     * One Yang Glyph candidate. Latch at the owned throw, then keep that stand
     * instead of grabbing a nearby player's beacon.
     */
    public record YangGlyphTrack(
            boolean alreadyTrackingThisStand,
            boolean glyphAlreadyClaimed,
            boolean ownedThrowActive,
            boolean hasOwnedBoss,
            double distanceToThrowOrigin,
            double distanceToNearestForeignBoss,
            double distanceToPlayer) {
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
     * across the arena. Yang Glyphs do not use this fallback; they latch at
     * the owned throw and then follow that stand.
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

    /**
     * Follow the local player's Yang Glyph only. A nearby player's beacon is
     * not "close enough" just because the local Voidgloom quest is active.
     */
    public static boolean shouldTrackYangGlyph(YangGlyphTrack probe) {
        if (probe == null) {
            return false;
        }
        if (probe.alreadyTrackingThisStand()) {
            return inRange(probe.distanceToPlayer(), YANG_GLYPH_PLAYER_RANGE);
        }
        if (probe.glyphAlreadyClaimed() || !probe.ownedThrowActive()) {
            return false;
        }
        if (!inRange(probe.distanceToPlayer(), YANG_GLYPH_PLAYER_RANGE)) {
            return false;
        }
        if (!probe.hasOwnedBoss()
                || !inRange(probe.distanceToThrowOrigin(), YANG_GLYPH_THROW_ORIGIN_RANGE)) {
            return false;
        }
        return closerToLocalBoss(
                probe.distanceToThrowOrigin(), probe.distanceToNearestForeignBoss());
    }

    public static boolean shouldAdoptSittingYangGlyph(
            boolean nearTrackedFlightPath,
            YangGlyphTrack probe) {
        return nearTrackedFlightPath || shouldTrackYangGlyph(probe);
    }

    public static boolean closerToLocalBoss(
            double distanceToOwnedBoss,
            double distanceToNearestForeignBoss) {
        if (!Double.isFinite(distanceToOwnedBoss) || distanceToOwnedBoss < 0.0D) {
            return false;
        }
        if (!Double.isFinite(distanceToNearestForeignBoss) || distanceToNearestForeignBoss < 0.0D) {
            return true;
        }
        return distanceToOwnedBoss < distanceToNearestForeignBoss;
    }

    public static double clampTargetLineDistance(double value) {
        return Math.max(4.0D, Math.min(64.0D, value));
    }

    private static boolean inRange(double distance, double maxDistance) {
        return Double.isFinite(distance) && distance >= 0.0D && distance <= maxDistance;
    }
}
