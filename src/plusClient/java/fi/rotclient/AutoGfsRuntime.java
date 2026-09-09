package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Minecraft bridge for {@link AutoGfsPolicy}. Sends the {@code /gfs} sack
 * names only when a matching SkyBlock id is already in the inventory.
 */
public final class AutoGfsRuntime {
    private static int lastTimerTick;

    private AutoGfsRuntime() {
    }

    public static void tick(Minecraft client) {
        if (!enabled()) {
            lastTimerTick = 0;
            return;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.autoGfsRefillOnTimer || client == null || client.player == null) {
            return;
        }
        int ticks = client.player.tickCount;
        if (!AutoGfsPolicy.timerReady(ticks, lastTimerTick, extras.autoGfsTimerIncrements)) {
            return;
        }
        lastTimerTick = ticks;
        refill(client);
    }

    public static void onChat(Component message) {
        if (message == null || !enabled()) {
            return;
        }
        QolSkyblockExtras extras = extras();
        String raw = message.getString();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (extras.autoGfsAutoGetDraft
                && player != null
                && AutoGfsPolicy.isLocalPuzzleFail(raw, player.getName().getString())) {
            send(AutoGfsPolicy.DRAFT_COMMAND);
        }
        if (extras.autoGfsRefillOnDungeonStart && AutoGfsPolicy.isDungeonStart(raw)) {
            refill(client);
        }
    }

    static void refill(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        if (client.gui != null && client.gui.screen() != null) {
            return;
        }
        QolSkyblockExtras extras = extras();
        if (!AutoGfsPolicy.locationAllowsRefill(
                extras.autoGfsInSkyblock,
                extras.autoGfsInKuudra,
                extras.autoGfsInDungeon,
                SkyBlockAreaDetector.isInSkyblock(),
                AutoGfsPolicy.isKuudraSidebar(SkyBlockSidebar.text()),
                SkyBlockDungeonDetector.confidentlyInDungeon())) {
            return;
        }
        Inventory inventory = client.player.getInventory();
        maybeFill(inventory, extras.autoGfsRefillLeap, AutoGfsPolicy.LEAP);
        maybeFill(inventory, extras.autoGfsRefillPearl, AutoGfsPolicy.PEARL);
        maybeFill(inventory, extras.autoGfsRefillJerry, AutoGfsPolicy.JERRY);
        maybeFill(inventory, extras.autoGfsRefillTnt, AutoGfsPolicy.TNT);
        maybeFill(inventory, extras.autoGfsRefillTwilight, AutoGfsPolicy.TWILIGHT);
    }

    private static void maybeFill(
            Inventory inventory,
            boolean enabled,
            AutoGfsPolicy.SackItem item) {
        if (!enabled || inventory == null || item == null) {
            return;
        }
        ItemStack found = firstMatching(inventory, item);
        Optional<String> command = AutoGfsPolicy.gfsCommand(
                found != null, found == null ? 0 : found.getCount(), item);
        command.ifPresent(AutoGfsRuntime::send);
    }

    private static ItemStack firstMatching(Inventory inventory, AutoGfsPolicy.SackItem item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (AutoGfsPolicy.idMatches(AutoClickerItemIdentity.skyBlockId(stack), item)) {
                return stack;
            }
        }
        return null;
    }

    private static void send(String command) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || player.connection == null || command == null || command.isBlank()) {
            return;
        }
        player.connection.sendCommand(command);
    }

    private static boolean enabled() {
        return extras().autoGfsEnabled;
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
