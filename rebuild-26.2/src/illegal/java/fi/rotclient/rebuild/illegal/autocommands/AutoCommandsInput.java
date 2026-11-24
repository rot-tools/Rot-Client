// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class AutoCommandsInput {
    private AutoCommandsInput() {}

    public static boolean onKeyPress(long window, int action, KeyEvent event) {
        if (event == null || (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT)
                || !IllegalEdition.autoCommandsEnabled()) return false;
        var config = AutoCommandsClient.config();
        var client = Minecraft.getInstance();
        if (config == null || client.getWindow().handle() != window || !client.isWindowActive()
                || !client.isSameThread() || client.gui.screen() != null || client.gui.overlay() != null
                || client.level == null || client.player == null || !client.player.isAlive()
                || client.getConnection() == null || (event.modifiers() & AutoCommandsClient.MODIFIERS) != 0) return false;
        String key = InputConstants.getKey(event).getName();
        for (CommandShortcut shortcut : config.shortcuts) {
            if (!key.equals(shortcut.key())) continue;
            if (action == GLFW.GLFW_PRESS) {
                client.getConnection().sendCommand(shortcut.command().substring(1));
                AutoCommandsClient.showShortcut(client, shortcut);
            }
            return true;
        }
        return false;
    }
}
