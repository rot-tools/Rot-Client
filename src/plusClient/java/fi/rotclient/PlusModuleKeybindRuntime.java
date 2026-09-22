package fi.rotclient;

import net.minecraft.client.Minecraft;

/** Plus-only edge-triggered toggles for Plus module keybinds. */
final class PlusModuleKeybindRuntime {
    private static boolean gfsWasDown;
    private static boolean sellWasDown;
    private static boolean ghostsWasDown;
    private static boolean freecamWasDown;
    private static boolean petWasDown;

    private PlusModuleKeybindRuntime() {
    }

    static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null
                || (client.gui != null && client.screen != null)) {
            reset();
            return;
        }
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = config.extras();
        long window = client.getWindow().handle();
        gfsWasDown = fire(window, extras.autoGfsKeybind, gfsWasDown,
                "qol.auto_gfs", "Auto GFS");
        sellWasDown = fire(window, extras.autoSellKeybind, sellWasDown,
                "qol.auto_sell", "Auto Sell");
        ghostsWasDown = fire(window, extras.ghostsKeybind, ghostsWasDown,
                "qol.ghosts", "Ghosts");
        freecamWasDown = fire(window, extras.freecamKeybind, freecamWasDown,
                "qol.freecam", "Free Camera");
        petWasDown = fire(window, config.petModuleKeybind, petWasDown,
                "qol.pet_keybinds", "Pet Keybinds");
    }

    private static boolean fire(
            long window, String keyName, boolean wasDown, String moduleId, String label) {
        boolean down = QolKeybindNames.isBoundDown(window, keyName);
        if (down && !wasDown) {
            QolUtilityConfig qol = RotClientClient.qolConfigPublic();
            boolean next = !qol.isModuleEnabled(moduleId);
            qol.setModuleEnabled(moduleId, next);
            RotClientClient.notifyQolModuleToggled(label, next);
        }
        return down;
    }

    private static void reset() {
        gfsWasDown = false;
        sellWasDown = false;
        ghostsWasDown = false;
        freecamWasDown = false;
        petWasDown = false;
    }
}
