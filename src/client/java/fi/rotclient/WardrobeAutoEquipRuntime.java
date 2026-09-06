package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.OptionalInt;

/**
 * Hidden-menu wardrobe auto-equip: {@code /wd} without opening
 * the chest GUI, then click slots 36-44 once the contents packet fills them.
 */
public final class WardrobeAutoEquipRuntime {
    private static boolean swapping;
    private static boolean inMenu;
    private static int slotIndex = -1;
    private static int containerId = -1;
    private static int waitTicks;
    private static long startedAtMs;
    private static int pendingCloseTicks = -1;

    private WardrobeAutoEquipRuntime() {
    }

    static boolean swapping() {
        return swapping;
    }

    static String hudText(boolean editorOpen) {
        if (editorOpen) {
            return WardrobeKeybindPolicy.equipHudText(2);
        }
        if (!swapping || slotIndex < 0) {
            return "";
        }
        return WardrobeKeybindPolicy.equipHudText(
                WardrobeKeybindPolicy.slotNumberFromIndex(slotIndex));
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol =
                RotClientClient.qolConfigPublic();

        /*
         * Disable/cancel all queued wardrobe work before processing a pending
         * close. This matters when a profile switch disables the module while
         * an equip operation is still in progress.
         */
        if (!hiddenEquipEnabled(qol)) {
            reset();
            return;
        }

        tickPendingClose(client);

        if (!swapping) {
            return;
        }

        if (stationaryOnly(qol)
                && !isPlayerStationary(
                client,
                -1)) {

            cancelActiveSwap(
                    client);

            return;
        }

        long now =
                System.currentTimeMillis();

        if (WardrobeKeybindPolicy.autoEquipTimedOut(
                startedAtMs,
                now)) {

            reset();
            return;
        }

        if (!inMenu
                || client == null
                || client.player == null
                || client.gameMode == null) {

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
                || menu.containerId != containerId
                || slotIndex < 0) {

            return;
        }

        if (slotIndex >= menu.slots.size()) {
            return;
        }

        Slot slot =
                menu.slots.get(
                        slotIndex);

        ItemStack stack =
                slot.getItem();

        boolean ready =
                WardrobeKeybindPolicy.isSlotReady(
                        stack.isEmpty(),
                        isLoadingPane(stack),
                        WardrobeKeybindPolicy.isEmptyMarker(
                                isItemPath(
                                        stack,
                                        "gray_dye"),
                                stack.getHoverName()
                                        .getString()));

        if (!ready) {
            return;
        }

        boolean equipped =
                WardrobeKeybindPolicy.isEquipped(
                        isItemPath(
                                stack,
                                "lime_dye"),
                        stack.getHoverName()
                                .getString());

        if (WardrobeKeybindPolicy.shouldClickAutoEquip(
                true,
                equipped)) {

            client.gameMode.handleContainerInput(
                    containerId,
                    slotIndex,
                    0,
                    ContainerInput.PICKUP,
                    player);
        }

        pendingCloseTicks =
                WardrobeKeybindPolicy.delayWithVariance(
                        closeDelay(qol),
                        delayVariance(qol),
                        Math.random());

        resetSwapState();
    }

    public static boolean onKeyPress(KeyEvent event, int action) {
        if (action != GLFW.GLFW_PRESS || event == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!hiddenEquipEnabled(qol) || client == null) {
            return false;
        }
        int key = event.key();
        if (swapping && !moveEquip(qol) && isMovementBound(client, key)) {
            return true;
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (screen != null || swapping) {
            return false;
        }
        OptionalInt target = resolveSlotKey(client, qol, key);
        if (target.isEmpty()) {
            return false;
        }
        LocalPlayer player = client.player;
        if (player == null || player.connection == null) {
            return false;
        }
        if (!WardrobeKeybindPolicy.canStartHiddenEquip(
                stationaryOnly(qol),
                isAnyMovementKeyDown(client) || isMovementBound(client, key),
                horizontalSpeedSquared(player))) {
            return false;
        }
        slotIndex = target.getAsInt();
        swapping = true;
        inMenu = false;
        containerId = -1;
        waitTicks = 0;
        startedAtMs = System.currentTimeMillis();
        player.connection.sendCommand(WardrobeKeybindPolicy.OPEN_COMMAND);
        return true;
    }

    public static boolean consumeOpenScreen(ClientboundOpenScreenPacket packet) {
        if (!swapping || packet == null) {
            return false;
        }
        String title = packet.getTitle() == null ? "" : packet.getTitle().getString();
        if (!WardrobeKeybindPolicy.containsArmorSets(title)) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || packet.getType() == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        player.containerMenu = packet.getType().create(
                packet.getContainerId(),
                player.getInventory());
        containerId = packet.getContainerId();
        waitTicks = WardrobeKeybindPolicy.delayWithVariance(
                clickDelay(qol),
                delayVariance(qol),
                Math.random());
        inMenu = true;
        startedAtMs = System.currentTimeMillis();
        return true;
    }

    public static void onContainerClosed() {
        reset();
    }

    static void onScreenOpened(Screen screen) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (resetOpen(qol)
                && swapping
                && screen instanceof AbstractContainerScreen<?>) {
            reset();
        }
    }

    private static void tickPendingClose(Minecraft client) {
        if (pendingCloseTicks < 0) {
            return;
        }
        if (client == null || client.player == null) {
            pendingCloseTicks = -1;
            return;
        }
        if (pendingCloseTicks == 0) {
            pendingCloseTicks = -1;
            client.player.closeContainer();
            return;
        }
        pendingCloseTicks--;
    }

    private static boolean hiddenEquipEnabled(QolUtilityConfig qol) {
        return qol.extras().cheaterWardrobeEnabled
                || (qol.wardrobeKeybindsEnabled && qol.wardrobeAutoEquip);
    }

    private static boolean moveEquip(QolUtilityConfig qol) {
        return qol.extras().cheaterWardrobeEnabled
                ? !qol.extras().cheaterWardrobeStationaryOnly
                : qol.wardrobeMoveEquip;
    }

    private static boolean stationaryOnly(QolUtilityConfig qol) {
        return qol.extras().cheaterWardrobeEnabled
                ? qol.extras().cheaterWardrobeStationaryOnly
                : !qol.wardrobeMoveEquip;
    }

    private static boolean resetOpen(QolUtilityConfig qol) {
        return qol.extras().cheaterWardrobeEnabled
                ? qol.extras().cheaterWardrobeResetOpen
                : qol.wardrobeResetOpen;
    }

    private static int clickDelay(QolUtilityConfig qol) {
        return qol.extras().cheaterWardrobeEnabled
                ? qol.extras().cheaterWardrobeClickDelay
                : qol.wardrobeClickDelay;
    }

    private static int closeDelay(QolUtilityConfig qol) {
        return qol.extras().cheaterWardrobeEnabled
                ? qol.extras().cheaterWardrobeCloseDelay
                : qol.wardrobeCloseDelay;
    }

    private static int delayVariance(QolUtilityConfig qol) {
        return qol.extras().cheaterWardrobeEnabled
                ? qol.extras().cheaterWardrobeDelayVariance
                : qol.wardrobeDelayVariance;
    }

    private static OptionalInt resolveSlotKey(
            Minecraft client,
            QolUtilityConfig qol,
            int key) {
        if (qol.extras().cheaterWardrobeEnabled) {
            OptionalInt direct = WardrobeKeybindPolicy.customSlotForKey(
                    key,
                    cheaterWardrobeSlotBinds(qol));
            if (direct.isPresent()) {
                return direct;
            }
        }
        if (qol.wardrobeKeybindsEnabled && qol.wardrobeAutoEquip) {
            return WardrobeKeybindPolicy.resolveConfiguredSlotKey(
                    key,
                    qol.wardrobeKeybindStyle,
                    qol.wardrobeUseHotbar,
                    wardrobeCustomSlotBinds(qol),
                    hotbarKeyValues(client));
        }
        return OptionalInt.empty();
    }

    private static String[] cheaterWardrobeSlotBinds(QolUtilityConfig qol) {
        QolSkyblockExtras extras = qol.extras();
        return new String[] {
            extras.cheaterWardrobeSlot1,
            extras.cheaterWardrobeSlot2,
            extras.cheaterWardrobeSlot3,
            extras.cheaterWardrobeSlot4,
            extras.cheaterWardrobeSlot5,
            extras.cheaterWardrobeSlot6,
            extras.cheaterWardrobeSlot7,
            extras.cheaterWardrobeSlot8,
            extras.cheaterWardrobeSlot9
        };
    }

    private static String[] wardrobeCustomSlotBinds(QolUtilityConfig qol) {
        return new String[] {
            qol.wardrobeCustom1,
            qol.wardrobeCustom2,
            qol.wardrobeCustom3,
            qol.wardrobeCustom4,
            qol.wardrobeCustom5,
            qol.wardrobeCustom6,
            qol.wardrobeCustom7,
            qol.wardrobeCustom8,
            qol.wardrobeCustom9
        };
    }

    private static int[] hotbarKeyValues(Minecraft client) {
        int[] keys = new int[9];
        if (client.options == null || client.options.keyHotbarSlots == null) {
            return keys;
        }
        KeyMapping[] slots = client.options.keyHotbarSlots;
        for (int i = 0; i < Math.min(9, slots.length); i++) {
            keys[i] = boundKeyValue(slots[i]);
        }
        return keys;
    }

    private static boolean isMovementBound(Minecraft client, int key) {
        if (client.options == null) {
            return false;
        }
        return boundKeyValue(client.options.keyUp) == key
                || boundKeyValue(client.options.keyDown) == key
                || boundKeyValue(client.options.keyLeft) == key
                || boundKeyValue(client.options.keyRight) == key
                || boundKeyValue(client.options.keyJump) == key
                || boundKeyValue(client.options.keyShift) == key;
    }

    private static boolean isAnyMovementKeyDown(Minecraft client) {
        if (client == null || client.options == null) {
            return false;
        }
        return client.options.keyUp.isDown()
                || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown()
                || client.options.keyRight.isDown()
                || client.options.keyJump.isDown()
                || client.options.keyShift.isDown();
    }

    private static boolean isPlayerStationary(Minecraft client, int triggeringKey) {
        LocalPlayer player = client == null ? null : client.player;
        if (player == null) {
            return false;
        }
        return WardrobeKeybindPolicy.canStartHiddenEquip(
                true,
                isAnyMovementKeyDown(client)
                        || (triggeringKey >= 0 && isMovementBound(client, triggeringKey)),
                horizontalSpeedSquared(player));
    }

    private static double horizontalSpeedSquared(LocalPlayer player) {
        if (player == null || player.getDeltaMovement() == null) {
            return Double.POSITIVE_INFINITY;
        }
        double x = player.getDeltaMovement().x;
        double z = player.getDeltaMovement().z;
        return x * x + z * z;
    }

    private static void cancelActiveSwap(Minecraft client) {
        if (inMenu && client != null && client.player != null) {
            client.player.closeContainer();
        }
        reset();
    }

    private static int boundKeyValue(KeyMapping mapping) {
        if (mapping == null) {
            return -1;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping).rotclient$boundKey();
        return bound == null ? -1 : bound.getValue();
    }

    private static boolean isLoadingPane(ItemStack stack) {
        return isItemPath(stack, "pink_stained_glass_pane")
                || isItemPath(stack, "magenta_stained_glass_pane");
    }

    private static boolean isItemPath(ItemStack stack, String path) {
        if (stack == null || stack.isEmpty() || path == null) {
            return false;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && path.equals(id.getPath());
    }

    private static void resetSwapState() {
        swapping = false;
        inMenu = false;
        slotIndex = -1;
        containerId = -1;
        waitTicks = 0;
        startedAtMs = 0L;
    }

    private static void reset() {
        resetSwapState();
        pendingCloseTicks = -1;
    }
}
