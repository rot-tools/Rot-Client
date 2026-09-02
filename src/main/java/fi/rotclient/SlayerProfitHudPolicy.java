package fi.rotclient;

/** Pure visibility rules for the read-only Slayer Item Profit HUD. */
public final class SlayerProfitHudPolicy {
    private SlayerProfitHudPolicy() {
    }

    public static boolean shouldRender(
            boolean moduleEnabled,
            boolean profitHudEnabled,
            boolean hideOutsideInventory,
            boolean hudEditorOpen,
            boolean inventoryOpen) {
        if (!moduleEnabled || !profitHudEnabled) {
            return false;
        }
        return !hideOutsideInventory || hudEditorOpen || inventoryOpen;
    }
}
