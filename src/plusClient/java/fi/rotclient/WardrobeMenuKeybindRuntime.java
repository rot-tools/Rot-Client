package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.OptionalInt;

/** Plus-only Wardrobe GUI keybind clicks and hidden-GUI presentation. */
final class WardrobeMenuKeybindRuntime {
    private static long lastClickAt;
    private static int autoCloseTicks = -1;

    private WardrobeMenuKeybindRuntime() {
    }

    static boolean shouldCancelRender(AbstractContainerScreen<?> screen) {
        if (screen == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.wardrobeKeybindsEnabled
                && qol.wardrobeCancelRender
                && MenuKeybindPolicy.parseWardrobeTitle(titleOf(screen)) != null;
    }

    static void tick(Minecraft client) {
        if (autoCloseTicks < 0) {
            return;
        }
        if (client == null || client.player == null) {
            autoCloseTicks = -1;
        } else if (autoCloseTicks == 0) {
            autoCloseTicks = -1;
            client.player.closeContainer();
        } else {
            autoCloseTicks--;
        }
    }

    static boolean handleInput(AbstractContainerScreen<?> screen, int code) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (screen == null || !qol.wardrobeKeybindsEnabled
                || MenuKeybindPolicy.parseWardrobeTitle(titleOf(screen)) == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        boolean overrideHeld = client != null
                && client.getWindow() != null
                && QolInputRuntime.isBoundDown(
                        client.getWindow().handle(), qol.wardrobeOverrideKey);
        boolean inventoryOrEscape = code == GLFW.GLFW_KEY_ESCAPE
                || code == boundKeyValue(client == null ? null : client.options.keyInventory);
        boolean action = isAction(code, qol, client);
        if (WardrobeKeybindPolicy.shouldCancelOtherInput(
                true, qol.wardrobeCancelAll, overrideHeld, inventoryOrEscape, action)) {
            return true;
        }
        if (!action) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (!WardrobeKeybindPolicy.pingReady(now, lastClickAt, qol.wardrobePingMs)) {
            return qol.wardrobeCancelAll;
        }
        MenuKeybindPolicy.PageTitle page =
                MenuKeybindPolicy.parseWardrobeTitle(titleOf(screen));
        OptionalInt slot = OptionalInt.empty();
        boolean pageTurn = false;
        if (WardrobeKeybindPolicy.matchesInput(code, qol.wardrobePreviousKey)) {
            if (page.current() > 1) {
                slot = OptionalInt.of(MenuKeybindPolicy.WARDROBE_PREVIOUS_SLOT);
                pageTurn = true;
            }
        } else if (WardrobeKeybindPolicy.matchesInput(code, qol.wardrobeNextKey)) {
            if (page.current() < page.total()) {
                slot = OptionalInt.of(MenuKeybindPolicy.WARDROBE_NEXT_SLOT);
                pageTurn = true;
            }
        } else if (WardrobeKeybindPolicy.matchesInput(code, qol.wardrobeUnequipKey)) {
            int equipped = findEquippedSlot(screen);
            if (equipped >= 0) {
                slot = OptionalInt.of(equipped);
            }
        } else if (qol.wardrobeSwap
                && WardrobeKeybindPolicy.matchesInput(code, qol.wardrobeSwapKey)) {
            slot = resolveSwapSlot(screen, qol);
        } else {
            slot = resolveSetSlot(screen, code, qol, client);
        }
        if (slot.isEmpty()) {
            return qol.wardrobeCancelAll && action;
        }
        if (!clickSlot(screen, slot.getAsInt(), qol)) {
            return false;
        }
        lastClickAt = now;
        if (qol.wardrobeAutoClose && !pageTurn) {
            autoCloseTicks = 1;
        }
        return true;
    }

    private static boolean isAction(int code, QolUtilityConfig qol, Minecraft client) {
        if (WardrobeKeybindPolicy.isPageOrUnequipAction(
                code, qol.wardrobeNextKey, qol.wardrobePreviousKey, qol.wardrobeUnequipKey)) {
            return true;
        }
        if (qol.wardrobeSwap
                && WardrobeKeybindPolicy.matchesInput(code, qol.wardrobeSwapKey)) {
            return true;
        }
        String style = WardrobeKeybindPolicy.effectiveStyle(
                qol.wardrobeKeybindStyle, qol.wardrobeUseHotbar);
        if (QolSkyblockExtras.STYLE_CUSTOM.equals(style)) {
            return WardrobeKeybindPolicy.customSlotForKey(code, customBinds(qol)).isPresent();
        }
        if (QolSkyblockExtras.STYLE_HOTBAR.equals(style)) {
            return WardrobeKeybindPolicy.hotbarSlotForKey(code, hotbarKeys(client)).isPresent();
        }
        return MenuKeybindPolicy.numberRowIndex(code) >= 0;
    }

    private static OptionalInt resolveSetSlot(
            AbstractContainerScreen<?> screen,
            int code,
            QolUtilityConfig qol,
            Minecraft client) {
        String style = WardrobeKeybindPolicy.effectiveStyle(
                qol.wardrobeKeybindStyle, qol.wardrobeUseHotbar);
        OptionalInt index;
        if (QolSkyblockExtras.STYLE_CUSTOM.equals(style)) {
            index = WardrobeKeybindPolicy.customSlotForKey(code, customBinds(qol));
        } else if (QolSkyblockExtras.STYLE_HOTBAR.equals(style)) {
            index = WardrobeKeybindPolicy.hotbarSlotForKey(code, hotbarKeys(client));
        } else {
            int number = MenuKeybindPolicy.numberRowIndex(code);
            index = number < 0 ? OptionalInt.empty()
                    : OptionalInt.of(MenuKeybindPolicy.WARDROBE_SLOT_BASE + number);
        }
        if (index.isEmpty()) {
            return OptionalInt.empty();
        }
        ItemStack stack = stackIn(screen, index.getAsInt());
        if (stack.isEmpty()) {
            return OptionalInt.empty();
        }
        boolean equipped = WardrobeKeybindPolicy.isEquipped(
                isLimeDye(stack), stack.getHoverName().getString());
        return qol.wardrobeDisableUnequip && equipped ? OptionalInt.empty() : index;
    }

    private static OptionalInt resolveSwapSlot(
            AbstractContainerScreen<?> screen, QolUtilityConfig qol) {
        if (qol.wardrobeSwapSlotA == qol.wardrobeSwapSlotB) {
            return OptionalInt.empty();
        }
        int first = WardrobeKeybindPolicy.wardrobeSlotIndex(qol.wardrobeSwapSlotA);
        int second = WardrobeKeybindPolicy.wardrobeSlotIndex(qol.wardrobeSwapSlotB);
        ItemStack firstStack = stackIn(screen, first);
        ItemStack secondStack = stackIn(screen, second);
        if (firstStack.isEmpty() || secondStack.isEmpty()) {
            return OptionalInt.empty();
        }
        boolean firstEquipped = WardrobeKeybindPolicy.isEquipped(
                isLimeDye(firstStack), firstStack.getHoverName().getString());
        return OptionalInt.of(WardrobeKeybindPolicy.swapClickSlot(
                firstEquipped, first, second));
    }

    private static int findEquippedSlot(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (WardrobeKeybindPolicy.isEquipped(
                    isLimeDye(stack), stack.getHoverName().getString())) {
                return slot.index;
            }
        }
        return -1;
    }

    private static boolean clickSlot(
            AbstractContainerScreen<?> screen, int slot, QolUtilityConfig qol) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.gameMode == null
                || slot < 0 || slot >= screen.getMenu().slots.size()) {
            return false;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId, slot, 0, ContainerInput.PICKUP, player);
        if (qol.wardrobeSound) {
            player.playSound(net.minecraft.sounds.SoundEvents.HORSE_ARMOR.value(), 0.69F, 1.0F);
        }
        return true;
    }

    private static ItemStack stackIn(AbstractContainerScreen<?> screen, int index) {
        List<Slot> slots = screen.getMenu().slots;
        return index < 0 || index >= slots.size() ? ItemStack.EMPTY : slots.get(index).getItem();
    }

    private static boolean isLimeDye(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && "lime_dye".equals(id.getPath());
    }

    private static int[] hotbarKeys(Minecraft client) {
        int[] keys = new int[9];
        if (client == null || client.options == null || client.options.keyHotbarSlots == null) {
            return keys;
        }
        KeyMapping[] slots = client.options.keyHotbarSlots;
        for (int i = 0; i < Math.min(9, slots.length); i++) {
            keys[i] = boundKeyValue(slots[i]);
        }
        return keys;
    }

    private static int boundKeyValue(KeyMapping mapping) {
        if (mapping == null) {
            return -1;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping).rotclient$boundKey();
        return bound == null ? -1 : bound.getValue();
    }

    private static String[] customBinds(QolUtilityConfig qol) {
        return new String[] {
                qol.wardrobeCustom1, qol.wardrobeCustom2, qol.wardrobeCustom3,
                qol.wardrobeCustom4, qol.wardrobeCustom5, qol.wardrobeCustom6,
                qol.wardrobeCustom7, qol.wardrobeCustom8, qol.wardrobeCustom9
        };
    }

    private static String titleOf(AbstractContainerScreen<?> screen) {
        return screen.getTitle() == null ? "" : screen.getTitle().getString();
    }
}
