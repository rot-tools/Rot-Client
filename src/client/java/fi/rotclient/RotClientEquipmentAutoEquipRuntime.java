package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;

/**
 * Hidden-menu Equipment Set activation for Rot Client loadouts.
 *
 * Opens /equipment without rendering the Equipment Wardrobe,
 * selects the configured Equipment Set, then closes the hidden container.
 */
public final class RotClientEquipmentAutoEquipRuntime {
    private static final long TIMEOUT_MS = 5_000L;
    private static final int CLICK_DELAY_TICKS = 1;
    private static final int HIDDEN_CLOSE_GRACE_TICKS = 4;

    private static boolean active;
    private static boolean inMenu;
    private static boolean awaitingSelectionConfirmation;

    private static int targetSetNumber;
    private static int containerId = -1;
    private static int waitTicks;
    private static int pendingCloseTicks = -1;

    private static long startedAtMs;

    private RotClientEquipmentAutoEquipRuntime() {
    }

    static boolean busy() {
        return active
                || pendingCloseTicks >= 0;
    }

    static boolean begin(
            int equipmentSetNumber) {

        if (equipmentSetNumber <= 0) {
            return true;
        }

        if (equipmentSetNumber > 9
                || busy()) {

            return false;
        }

        Minecraft client =
                Minecraft.getInstance();

        LocalPlayer player =
                client == null
                        ? null
                        : client.player;

        if (player == null
                || player.connection == null) {

            return false;
        }

        active = true;
        inMenu = false;
        awaitingSelectionConfirmation = false;

        targetSetNumber =
                equipmentSetNumber;

        containerId = -1;
        waitTicks = 0;
        pendingCloseTicks = -1;

        startedAtMs =
                System.currentTimeMillis();

        player.connection.sendCommand(
                InventoryOverlayPolicy.OPEN_WARDROBE_COMMAND);

        return true;
    }

    static void tick(
            Minecraft client) {

        tickPendingClose(client);

        if (!active) {
            return;
        }

        if (client == null
                || client.player == null
                || client.gameMode == null) {

            reset();
            return;
        }

        if (System.currentTimeMillis()
                - startedAtMs > TIMEOUT_MS) {

            cancelAndClose(client);
            return;
        }

        if (!inMenu) {
            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        LocalPlayer player =
                client.player;

        AbstractContainerMenu menu =
                player.containerMenu;

        if (menu == null
                || menu.containerId != containerId) {

            return;
        }

        int targetSlot =
                35 + targetSetNumber;

        if (targetSlot < 36
                || targetSlot > 44
                || targetSlot >= menu.slots.size()) {

            cancelAndClose(client);
            return;
        }

        Slot slot =
                menu.slots.get(targetSlot);

        if (slot == null) {
            return;
        }

        ItemStack stack =
                slot.getItem();

        /*
         * Wait for Hypixel to populate the selector row.
         */
        if (stack == null
                || stack.isEmpty()) {

            return;
        }

        /*
         * Lime dye marks the currently selected Equipment Set.
         */
        if (isTargetSelected(
                targetSlot,
                stack)) {

            InventoryChromeRuntime
                    .noteEquipmentSet(
                            menu.slots,
                            targetSetNumber - 1);

            scheduleHiddenClose();
            return;
        }

        /*
         * We already sent the selector click. Wait for either the lime-dye
         * confirmation or Hypixel closing the menu.
         */
        if (awaitingSelectionConfirmation) {
            return;
        }

        client.gameMode
                .handleContainerInput(
                        containerId,
                        targetSlot,
                        0,
                        ContainerInput.PICKUP,
                        player);

        awaitingSelectionConfirmation = true;
        waitTicks = CLICK_DELAY_TICKS;

        /*
         * Give the server a fresh timeout window to acknowledge the click.
         */
        startedAtMs =
                System.currentTimeMillis();
    }

    public static boolean consumeOpenScreen(
            ClientboundOpenScreenPacket packet) {

        boolean pendingHiddenClose =
                pendingCloseTicks >= 0;

        if ((!active && !pendingHiddenClose)
                || packet == null) {

            return false;
        }

        String title =
                packet.getTitle() == null
                        ? ""
                        : packet.getTitle()
                        .getString();

        if (!InventoryOverlayPolicy
                .isEquipmentSetsMenu(title)) {

            return false;
        }

        Minecraft client =
                Minecraft.getInstance();

        LocalPlayer player =
                client == null
                        ? null
                        : client.player;

        if (player == null
                || packet.getType() == null) {

            return false;
        }

        /*
         * Recreate the server container locally while cancelling the real
         * screen open. Content packets can then populate it normally.
         */
        player.containerMenu =
                packet.getType()
                        .create(
                                packet.getContainerId(),
                                player.getInventory());

        containerId =
                packet.getContainerId();

        inMenu = true;

        if (active) {
            waitTicks =
                    CLICK_DELAY_TICKS;

            startedAtMs =
                    System.currentTimeMillis();
        }

        return true;
    }

    public static void onContainerClosed() {
        if (!busy()) {
            return;
        }

        /*
         * Ignore stale close packets while /equipment is still waiting for its
         * hidden screen to arrive.
         */
        if (active
                && !inMenu) {

            return;
        }

        /*
         * Hypixel may close the Equipment Wardrobe immediately after changing
         * sets. Treat that close as successful acknowledgement of our click,
         * while retaining a short hidden-open grace period.
         */
        if (active
                && awaitingSelectionConfirmation) {

            Minecraft client =
                    Minecraft.getInstance();

            LocalPlayer player =
                    client == null
                            ? null
                            : client.player;

            AbstractContainerMenu menu =
                    player == null
                            ? null
                            : player.containerMenu;

            if (menu != null
                    && menu.containerId == containerId) {

                InventoryChromeRuntime
                        .noteEquipmentSet(
                                menu.slots,
                                targetSetNumber - 1);
            }

            active = false;
            inMenu = false;
            awaitingSelectionConfirmation = false;

            containerId = -1;
            waitTicks = 0;

            pendingCloseTicks =
                    HIDDEN_CLOSE_GRACE_TICKS;

            return;
        }

        /*
         * Keep the grace period alive if a server close arrives while waiting
         * to suppress a possible follow-up Equipment Wardrobe refresh.
         */
        if (pendingCloseTicks >= 0) {
            inMenu = false;
            containerId = -1;
            return;
        }

        reset();
    }

    private static boolean isTargetSelected(
            int slotIndex,
            ItemStack stack) {

        if (stack == null
                || stack.isEmpty()) {

            return false;
        }

        String itemPath =
                BuiltInRegistries.ITEM
                        .getKey(stack.getItem())
                        .getPath();

        OptionalInt selectedColumn =
                InventoryOverlayPolicy
                        .equipmentSetsColumn(
                                slotIndex,
                                itemPath);

        return selectedColumn.isPresent()
                && selectedColumn.getAsInt()
                == targetSetNumber - 1;
    }

    private static void scheduleHiddenClose() {
        active = false;
        awaitingSelectionConfirmation = false;

        waitTicks = 0;

        pendingCloseTicks =
                HIDDEN_CLOSE_GRACE_TICKS;
    }

    private static void tickPendingClose(
            Minecraft client) {

        if (pendingCloseTicks < 0) {
            return;
        }

        if (client == null
                || client.player == null) {

            reset();
            return;
        }

        if (pendingCloseTicks == 0) {
            pendingCloseTicks = -1;

            client.player
                    .closeContainer();

            reset();
            return;
        }

        pendingCloseTicks--;
    }

    private static void cancelAndClose(
            Minecraft client) {

        if (client != null
                && client.player != null
                && inMenu) {

            client.player
                    .closeContainer();
        }

        reset();
    }

    private static void reset() {
        active = false;
        inMenu = false;
        awaitingSelectionConfirmation = false;

        targetSetNumber = 0;
        containerId = -1;
        waitTicks = 0;
        pendingCloseTicks = -1;

        startedAtMs = 0L;
    }
}