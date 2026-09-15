package fi.rotclient;

import net.minecraft.client.Minecraft;

/** Plus-only keybind for opening the local simulator hub. */
final class TermSimKeybindRuntime {
    private static boolean wasDown;

    static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null
                || client.gui == null || client.gui.screen() != null) {
            wasDown = false;
            return;
        }
        TermSimSettings settings = TermSimSettings.from(RotClientClient.qolConfigPublic());
        boolean down = QolKeybindNames.isBoundDown(client.getWindow().handle(), settings.keybind());
        if (down && !wasDown && settings.enabled()) TermSimRuntime.openHub();
        wasDown = down;
    }
}
