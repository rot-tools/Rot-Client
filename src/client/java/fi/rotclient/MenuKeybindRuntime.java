package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Applies wardrobe / pets / loadout keybinds to Hypixel chest GUIs.
 */
public final class MenuKeybindRuntime {
    private static long lastWardrobeClickMs;
    private static int autoCloseTicks = -1;

    private MenuKeybindRuntime() {
    }

    public static boolean handleKeyPressed(AbstractContainerScreen<?> screen, int glfwKey) {
        return handleContainerInput(screen, glfwKey);
    }

    public static boolean handleMousePressed(AbstractContainerScreen<?> screen, int button) {
        return handleContainerInput(screen, button);
    }

    public static boolean shouldCancelContainerRender(AbstractContainerScreen<?> screen) {
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
            return;
        }
        if (autoCloseTicks == 0) {
            autoCloseTicks = -1;
            client.player.closeContainer();
            return;
        }
        autoCloseTicks--;
    }

    private static boolean handleContainerInput(AbstractContainerScreen<?> screen, int code) {
        if (screen == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        String title = titleOf(screen);
        OptionalInt slot = OptionalInt.empty();
        if (qol.wardrobeKeybindsEnabled
                && MenuKeybindPolicy.parseWardrobeTitle(title) != null) {
            boolean consumed = handleWardrobeInput(screen, code, qol);
            if (consumed) {
                return true;
            }
        } else if (qol.petKeybindsEnabled
                && MenuKeybindPolicy.parsePetsTitle(title) != null) {
            int equipped = findEquippedPetSlot(screen);
            OptionalInt preview = MenuKeybindPolicy.resolvePetsSlot(
                    title,
                    code,
                    qol.petNextKey,
                    qol.petPreviousKey,
                    qol.petUnequipKey,
                    equipped,
                    false,
                    qol.petDisableUnequip,
                    qol.petCloseIfAlreadyEquipped);
            boolean targetEquipped = preview.isPresent()
                    && MenuKeybindPolicy.loreMeansPetEquipped(
                            loreLines(stackIn(screen, preview.getAsInt())));
            slot = MenuKeybindPolicy.resolvePetsSlot(
                    title,
                    code,
                    qol.petNextKey,
                    qol.petPreviousKey,
                    qol.petUnequipKey,
                    equipped,
                    targetEquipped,
                    qol.petDisableUnequip,
                    qol.petCloseIfAlreadyEquipped);
        } else if (qol.loadoutKeybindsEnabled
                && MenuKeybindPolicy.parseLoadoutTitle(title) != null) {
            slot = MenuKeybindPolicy.resolveLoadoutSlot(
                    title,
                    code,
                    qol.loadoutNextKey,
                    qol.loadoutPreviousKey);
        }
        if (slot.isEmpty()) {
            return false;
        }
        return clickSlot(screen, slot.getAsInt());
    }

    private static boolean handleWardrobeInput(
            AbstractContainerScreen<?> screen,
            int code,
            QolUtilityConfig qol) {
        Minecraft client = Minecraft.getInstance();
        boolean overrideHeld = client != null
                && client.getWindow() != null
                && QolKeybindNames.isBoundDown(
                        client.getWindow().handle(), qol.wardrobeOverrideKey);
        boolean inventoryOrEscape = code == GLFW.GLFW_KEY_ESCAPE
                || code == boundKeyValue(client == null ? null : client.options.keyInventory);
            boolean wardrobeAction = isWardrobeAction(code, qol, client);
        if (WardrobeKeybindPolicy.shouldCancelOtherInput(
                true,
                qol.wardrobeCancelAll,
                overrideHeld,
                inventoryOrEscape,
                wardrobeAction)) {
            return true;
        }
        if (!wardrobeAction) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (!WardrobeKeybindPolicy.pingReady(now, lastWardrobeClickMs, qol.wardrobePingMs)) {
            return qol.wardrobeCancelAll;
        }
        String title = titleOf(screen);
        MenuKeybindPolicy.PageTitle page = MenuKeybindPolicy.parseWardrobeTitle(title);
        if (page == null) {
            return false;
        }
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
            int equipped = findEquippedWardrobeSlot(screen);
            if (equipped >= 0) {
                slot = OptionalInt.of(equipped);
            }
        } else if (qol.wardrobeSwap && WardrobeKeybindPolicy.matchesInput(code, qol.wardrobeSwapKey)) {
            slot = resolveSwapSlot(screen, qol);
        } else {
            slot = resolveWardrobeSetSlot(screen, code, qol, client);
        }
        if (slot.isEmpty()) {
            return qol.wardrobeCancelAll && wardrobeAction;
        }
        if (!clickSlot(screen, slot.getAsInt())) {
            return false;
        }
        lastWardrobeClickMs = now;
        if (qol.wardrobeAutoClose && !pageTurn) {
            autoCloseTicks = 1;
        }
        return true;
    }

    private static boolean isWardrobeAction(
            int code,
            QolUtilityConfig qol,
            Minecraft client) {
        if (WardrobeKeybindPolicy.isPageOrUnequipAction(
                code,
                qol.wardrobeNextKey,
                qol.wardrobePreviousKey,
                qol.wardrobeUnequipKey)) {
            return true;
        }
        if (qol.wardrobeSwap && WardrobeKeybindPolicy.matchesInput(code, qol.wardrobeSwapKey)) {
            return true;
        }
        String style = WardrobeKeybindPolicy.effectiveStyle(
                qol.wardrobeKeybindStyle, qol.wardrobeUseHotbar);
        if (QolSkyblockExtras.STYLE_CUSTOM.equals(style)) {
            return WardrobeKeybindPolicy.customSlotForKey(code, customSlotBinds(qol)).isPresent();
        }
        if (QolSkyblockExtras.STYLE_HOTBAR.equals(style)) {
            return WardrobeKeybindPolicy.hotbarSlotForKey(code, hotbarKeyValues(client)).isPresent();
        }
        return MenuKeybindPolicy.numberRowIndex(code) >= 0;
    }

    private static OptionalInt resolveWardrobeSetSlot(
            AbstractContainerScreen<?> screen,
            int code,
            QolUtilityConfig qol,
            Minecraft client) {
        OptionalInt index;
        String style = WardrobeKeybindPolicy.effectiveStyle(
                qol.wardrobeKeybindStyle, qol.wardrobeUseHotbar);
        if (QolSkyblockExtras.STYLE_CUSTOM.equals(style)) {
            index = WardrobeKeybindPolicy.customSlotForKey(code, customSlotBinds(qol));
        } else if (QolSkyblockExtras.STYLE_HOTBAR.equals(style)) {
            index = WardrobeKeybindPolicy.hotbarSlotForKey(code, hotbarKeyValues(client));
        } else {
            int number = MenuKeybindPolicy.numberRowIndex(code);
            index = number < 0
                    ? OptionalInt.empty()
                    : OptionalInt.of(MenuKeybindPolicy.WARDROBE_SLOT_BASE + number);
        }
        if (index.isEmpty()) {
            return OptionalInt.empty();
        }
        int slotIndex = index.getAsInt();
        ItemStack stack = stackIn(screen, slotIndex);
        if (stack.isEmpty()) {
            return OptionalInt.empty();
        }
        boolean equipped = WardrobeKeybindPolicy.isEquipped(
                isLimeDye(stack),
                stack.getHoverName().getString());
        if (qol.wardrobeDisableUnequip && equipped) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(slotIndex);
    }

    private static OptionalInt resolveSwapSlot(
            AbstractContainerScreen<?> screen,
            QolUtilityConfig qol) {
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
                isLimeDye(firstStack),
                firstStack.getHoverName().getString());
        return OptionalInt.of(WardrobeKeybindPolicy.swapClickSlot(firstEquipped, first, second));
    }

    private static int[] hotbarKeyValues(Minecraft client) {
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

    private static String titleOf(AbstractContainerScreen<?> screen) {
        return screen.getTitle() == null ? "" : screen.getTitle().getString();
    }

    private static int findEquippedWardrobeSlot(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (WardrobeKeybindPolicy.isEquipped(
                    isLimeDye(stack),
                    stack.getHoverName().getString())) {
                return slot.index;
            }
        }
        return -1;
    }

    private static int findEquippedPetSlot(AbstractContainerScreen<?> screen) {
        List<Slot> slots = screen.getMenu().slots;
        int end = Math.min(43, slots.size());
        for (int i = 10; i < end; i++) {
            if (MenuKeybindPolicy.loreMeansPetEquipped(loreLines(slots.get(i).getItem()))) {
                return i;
            }
        }
        return -1;
    }

    private static ItemStack stackIn(AbstractContainerScreen<?> screen, int index) {
        List<Slot> slots = screen.getMenu().slots;
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        return slots.get(index).getItem();
    }

    private static boolean isLimeDye(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && "lime_dye".equals(id.getPath());
    }

    private static List<String> loreLines(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        List<String> lines = new ArrayList<>();
        for (Component line : lore.lines()) {
            lines.add(line.getString());
        }
        return lines;
    }

    private static boolean clickSlot(AbstractContainerScreen<?> screen, int slotIndex) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.gameMode == null) {
            return false;
        }
        if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
            return false;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slotIndex,
                0,
                ContainerInput.PICKUP,
                player);
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (qol.wardrobeKeybindsEnabled
                && qol.wardrobeSound
                && WardrobeKeybindPolicy.isWardrobeTitle(screen.getTitle().getString())) {
            player.playSound(net.minecraft.sounds.SoundEvents.HORSE_ARMOR.value(), 0.69F, 1.0F);
        }
        return true;
    }

    private static String[] customSlotBinds(QolUtilityConfig qol) {
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
}
