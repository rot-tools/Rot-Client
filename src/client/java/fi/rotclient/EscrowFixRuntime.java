package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Minecraft bridge for {@link EscrowFixPolicy}: send {@code ah} or {@code bz}
 * when Hypixel-identical escrow chat closes those menus.
 */
public final class EscrowFixRuntime {
    private EscrowFixRuntime() {
    }

    public static void onChat(Component message) {
        if (message == null || !enabled()) {
            return;
        }
        String command = EscrowFixPolicy.commandForMessage(message.getString());
        if (command == null || command.isBlank()) {
            return;
        }
        send(command);
    }

    private static boolean enabled() {
        return RotClientClient.qolConfigPublic().extras().escrowFixEnabled;
    }

    private static void send(String command) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || player.connection == null) {
            return;
        }
        player.connection.sendCommand(command);
    }
}
