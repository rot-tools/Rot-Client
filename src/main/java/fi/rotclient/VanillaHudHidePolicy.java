package fi.rotclient;

/**
 * Local vanilla HUD layer suppression for Player Display hide settings.
 */
public final class VanillaHudHidePolicy {
    public enum Layer {
        HEALTH,
        FOOD,
        ARMOR,
        XP
    }

    private VanillaHudHidePolicy() {
    }

    public static boolean shouldHideLayer(
            boolean playerDisplayEnabled,
            boolean hideHealth,
            boolean hideFood,
            boolean hideArmor,
            boolean hideXp,
            Layer layer) {
        if (!playerDisplayEnabled || layer == null) {
            return false;
        }
        return switch (layer) {
            case HEALTH -> hideHealth;
            case FOOD -> hideFood;
            case ARMOR -> hideArmor;
            case XP -> hideXp;
        };
    }
}
