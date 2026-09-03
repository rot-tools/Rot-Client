package fi.rotclient;

/**
 * World editor only previews overlays that are currently on. Right-click hide
 * must remove the HUD from the editor, not leave a ghost frame.
 */
public final class HudEditorPreviewPolicy {
    private HudEditorPreviewPolicy() {
    }

    public static boolean showMiningTracker(boolean trackerEnabled) {
        return trackerEnabled;
    }

    public static boolean showPowderChest(boolean powderChestHudEnabled) {
        return powderChestHudEnabled;
    }
}
