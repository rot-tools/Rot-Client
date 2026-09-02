package fi.rotclient;

/**
 * Edge-triggered Click GUI open/close policy. Hold-repeat must not re-fire.
 */
public final class ClickGuiKeyPolicy {
    private ClickGuiKeyPolicy() {
    }

    public static boolean shouldToggleOpen(
            boolean moduleEnabled,
            boolean keyDown,
            boolean wasKeyDown,
            boolean textFieldConsuming) {
        if (!moduleEnabled || textFieldConsuming) {
            return false;
        }
        return keyDown && !wasKeyDown;
    }

    /** When UI is already open, edge press closes it; otherwise opens. */
    public static boolean shouldOpen(
            boolean moduleEnabled,
            boolean keyDown,
            boolean wasKeyDown,
            boolean textFieldConsuming,
            boolean uiAlreadyOpen) {
        return shouldToggleOpen(
                moduleEnabled, keyDown, wasKeyDown, textFieldConsuming)
                && !uiAlreadyOpen;
    }

    public static boolean shouldClose(
            boolean moduleEnabled,
            boolean keyDown,
            boolean wasKeyDown,
            boolean textFieldConsuming,
            boolean uiAlreadyOpen) {
        return shouldToggleOpen(
                moduleEnabled, keyDown, wasKeyDown, textFieldConsuming)
                && uiAlreadyOpen;
    }
}
