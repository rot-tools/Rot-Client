package fi.rotclient;

/**
 * Ghosts: Dwarven Mines "ghosts" are invisible charged creepers. Show
 * Ghosts forces the body visible; Show Powered Layer keeps or hides the
 * vanilla charged overlay. Both off suppresses the whole entity.
 */
public final class GhostsPolicy {
    public record Decision(boolean suppressEntity, boolean forceVisible, boolean hidePoweredLayer) {
        public static final Decision NONE = new Decision(false, false, false);
    }

    private GhostsPolicy() {
    }

    public static Decision decide(
            boolean enabled,
            boolean showGhosts,
            boolean showPowered,
            boolean creeper,
            boolean invisible) {
        if (!enabled || !creeper) {
            return Decision.NONE;
        }
        boolean hidePowered = !showPowered;
        if (invisible && !showGhosts && hidePowered) {
            return new Decision(true, false, true);
        }
        return new Decision(false, invisible && showGhosts, hidePowered);
    }
}
