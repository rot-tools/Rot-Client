package fi.rotclient;

import net.minecraft.client.Minecraft;

/**
 * Edge-triggered module toggle binds for keybind rows that are
 * otherwise only stored in extras.
 */
public final class QolModuleKeybindRuntime {
    private static boolean termSimWasDown;

    private static boolean boardWasDown;

    private QolModuleKeybindRuntime() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            reset();
            return;
        }
        if (client.gui != null && client.gui.screen() != null) {
            reset();
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        long window = client.getWindow().handle();
        QolClientFlavorSupport.hooks().plusModuleKeybindTick(client);
        termSimWasDown = fireOpen(
                window, extras.dungeonTermSimKeybind, termSimWasDown, extras.dungeonTermSimEnabled);
        boardWasDown = fire(
                window, extras.board().keybind, boardWasDown,
                CustomScoreboardPolicy.MODULE_ID, "Custom Scoreboard");
    }

    private static boolean fireOpen(long window, String keyName, boolean wasDown, boolean enabled) {
        boolean down = QolKeybindNames.isBoundDown(window, keyName);
        if (down && !wasDown && enabled) {
            TermSimRuntime.openHub();
        }
        return down;
    }

    private static boolean fire(
            long window,
            String keyName,
            boolean wasDown,
            String moduleId,
            String label) {
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
        termSimWasDown = false;
        boardWasDown = false;
    }
}
