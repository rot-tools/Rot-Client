package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Applies {@link ItemProtectPolicy} and foraging huntaxe lock to drops and
 * container clicks. Sneak bypasses the lock.
 */
public final class ItemProtectRuntime {
    private ItemProtectRuntime() {
    }

    public static boolean shouldBlockDrop(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        boolean sneak = client.player.isShiftKeyDown();
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        List<String> lore = InventoryChromeRuntime.loreLines(stack);
        String name = stack.getHoverName().getString();
        if (ItemProtectPolicy.shouldBlockDrop(
                qol.inventoryOverlayEnabled && qol.inventoryOverlayProtectDrops,
                true,
                sneak,
                true,
                true,
                name,
                lore,
                ItemProtectPolicy.parseExtraNames(qol.inventoryOverlayProtectList))) {
            return true;
        }
        return huntaxeLocked(qol, sneak, stack, name);
    }

    public static boolean shouldBlockSlotClick(
            AbstractContainerScreen<?> screen,
            Slot slot,
            ContainerInput input) {
        if (screen == null || slot == null || slot.getItem().isEmpty()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        boolean sneak = client.player.isShiftKeyDown();
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        ItemStack stack = slot.getItem();
        String name = stack.getHoverName().getString();
        List<String> lore = InventoryChromeRuntime.loreLines(stack);
        String title = screenTitle(screen);
        boolean throwClick = ItemProtectPolicy.isThrowClick(input == null ? "" : input.name());
        if (throwClick && ItemProtectPolicy.shouldBlockDrop(
                qol.inventoryOverlayEnabled && qol.inventoryOverlayProtectDrops,
                true,
                sneak,
                true,
                true,
                name,
                lore,
                ItemProtectPolicy.parseExtraNames(qol.inventoryOverlayProtectList))) {
            return true;
        }
        if (ItemProtectPolicy.shouldBlockSalvage(
                qol.inventoryOverlayEnabled && qol.inventoryOverlayProtectSalvage,
                true,
                sneak,
                title,
                true,
                true,
                name,
                lore,
                ItemProtectPolicy.parseExtraNames(qol.inventoryOverlayProtectList))) {
            return true;
        }
        return huntaxeLocked(qol, sneak, stack, name)
                && (throwClick || ItemProtectPolicy.isSalvageScreen(title));
    }

    private static boolean huntaxeLocked(
            QolUtilityConfig qol, boolean sneak, ItemStack stack, String name) {
        if (sneak || qol == null) {
            return false;
        }
        QolSkyblockExtras extras = qol.extras();
        if (!extras.foragingHelpersEnabled || !extras.foragingHelpersHuntaxeLock) {
            return false;
        }
        return ForagingPolicy.isHuntaxe(AutoClickerItemIdentity.skyBlockId(stack), name);
    }

    private static String screenTitle(Screen screen) {
        if (screen == null || screen.getTitle() == null) {
            return "";
        }
        return screen.getTitle().getString();
    }
}
