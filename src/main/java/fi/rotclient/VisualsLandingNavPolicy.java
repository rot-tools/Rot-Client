package fi.rotclient;

/**
 * Visuals landings (Appearance, HUD Elements Editor) live in the sidebar, not
 * as Modules → HUD &amp; Display cards. Unhandled clicks dismiss the landing.
 */
public final class VisualsLandingNavPolicy {
    private VisualsLandingNavPolicy() {
    }

    public static boolean hiddenFromGroupPage(String moduleId) {
        return MiningTrackerCatalogPolicy.APPEARANCE.equals(moduleId)
                || MiningTrackerCatalogPolicy.HUD_LAYOUT.equals(moduleId);
    }

    public static boolean dismissOnUnhandledClick(
            boolean visualsLandingOpen,
            RotClientSidebarNav.HitTarget hit) {
        return visualsLandingOpen
                && (hit == null || hit == RotClientSidebarNav.HitTarget.NONE);
    }
}
