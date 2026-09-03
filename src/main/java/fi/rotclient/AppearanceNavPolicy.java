package fi.rotclient;

import java.util.List;

/**
 * Appearance customizer sidebar without a dedicated Mining HUD page.
 * Tracker overlay settings live on the Mining Tracker HUD drawer.
 */
public final class AppearanceNavPolicy {
    private AppearanceNavPolicy() {
    }

    public static List<String> sidebarLabels() {
        return List.of(
                "Overview",
                "Dashboard",
                "Colors",
                "Background",
                "Charts",
                "Reset & Defaults");
    }

    public static boolean includesMiningHud() {
        return false;
    }
}
