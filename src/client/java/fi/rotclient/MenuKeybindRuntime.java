package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Applies the legit pets/loadout keybinds to their open SkyBlock menus. */
public final class MenuKeybindRuntime {
    private MenuKeybindRuntime() {
    }

    public static boolean handleKeyPressed(AbstractContainerScreen<?> screen, int keyCode) {
        return handleContainerInput(screen, keyCode, QolInputRuntime.formatGlfwKey(keyCode));
    }

    public static boolean handleMousePressed(AbstractContainerScreen<?> screen, int button) {
        return handleContainerInput(screen, button, QolInputRuntime.formatMouseButton(button));
    }

    public static boolean shouldCancelContainerRender(AbstractContainerScreen<?> screen) {
        return QolClientFlavorSupport.hooks().wardrobeMenuShouldCancelRender(screen);
    }

    static void tick(Minecraft client) {
        QolClientFlavorSupport.hooks().wardrobeMenuTick(client);
    }

    private static boolean handleContainerInput(
            AbstractContainerScreen<?> screen, int rawCode, String keyName) {
        if (screen == null) {
            return false;
        }
        if (QolClientFlavorSupport.hooks().wardrobeMenuHandleInput(screen, rawCode, keyName)) {
            return true;
        }
        if (!QolFlavorSupport.isPlus() || keyName == null || keyName.isBlank()) {
            // Pet and loadout keybinds click menu slots, so they remain Plus-only.
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        String title = titleOf(screen);
        OptionalInt slot = OptionalInt.empty();
        if (qol.petKeybindsEnabled && MenuKeybindPolicy.parsePetsTitle(title) != null) {
            int equipped = findEquippedPetSlot(screen);
            OptionalInt preview = MenuKeybindPolicy.resolvePetsSlot(
                    title,
                    keyName,
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
                    keyName,
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
                    title, keyName, qol.loadoutNextKey, qol.loadoutPreviousKey);
        }
        if (slot.isEmpty()) {
            return false;
        }
        int slotIndex = slot.getAsInt();
        if (MenuKeybindPolicy.parsePetsTitle(title) != null
                && slotIndex >= 0
                && slotIndex < screen.getMenu().slots.size()) {
            InventoryChromeRuntime.notePetMenuSelection(
                    screen, screen.getMenu().slots.get(slotIndex), 0);
        }
        return clickSlot(screen, slotIndex);
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
        if (player == null || client.gameMode == null
                || slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
            return false;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slotIndex,
                0,
                ContainerInput.PICKUP,
                player);
        return true;
    }

    private static String titleOf(AbstractContainerScreen<?> screen) {
        return screen.getTitle() == null ? "" : screen.getTitle().getString();
    }
}
