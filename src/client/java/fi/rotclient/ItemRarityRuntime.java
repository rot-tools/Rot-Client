package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Item-rarity slot tint from the last SkyBlock rarity lore line.
 */
public final class ItemRarityRuntime {
    private ItemRarityRuntime() {
    }

    /*
     * renderHotbar() re-parsed every hotbar item's lore into a rarity on
     * every single rendered frame, even though the 9 hotbar slots almost
     * never change between frames. These remember the last stack instance
     * seen in each slot and its resolved rarity, so an unchanged slot skips
     * loreLines()/parseRarity() entirely. Reference equality is enough here:
     * a slot's ItemStack instance changes whenever its contents actually
     * change (item swap, pickup, container sync), and this is purely
     * cosmetic tinting, so the rare case of an in-place mutation the cache
     * misses for one frame is harmless.
     */
    private static final ItemStack[] HOTBAR_RARITY_STACK = new ItemStack[9];
    private static final ItemRarityPolicy.Rarity[] HOTBAR_RARITY_CACHE =
            new ItemRarityPolicy.Rarity[9];

    public static void afterContainerContents(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.itemRarityEnabled || screen == null || graphics == null) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || slot.getItem().isEmpty()) {
                continue;
            }
            paint(graphics, leftPos + slot.x, topPos + slot.y, slot.getItem(), qol, false, true);
        }
    }

    public static void paintSlotBackground(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemStack stack) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.itemRarityEnabled || graphics == null || stack == null || stack.isEmpty()) {
            return;
        }
        paint(graphics, x, y, stack, qol, true, false);
    }

    public static void paintHotbarBackground(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemStack stack) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.itemRarityEnabled || !qol.itemRarityHotbar
                || graphics == null || stack == null || stack.isEmpty()) {
            return;
        }
        paint(graphics, x, y, stack, qol, true, false);
    }

    static void renderHotbar(
            GuiGraphicsExtractor graphics,
            int screenWidth,
            int screenHeight) {

        renderHotbar(
                graphics,
                screenWidth,
                screenHeight,
                false);
    }

    static void renderHotbar(
            GuiGraphicsExtractor graphics,
            int screenWidth,
            int screenHeight,
            boolean force) {

        QolUtilityConfig qol = RotClientClient.qolConfigPublic();

        if (graphics == null
                || (!force
                && (!qol.itemRarityEnabled
                || !qol.itemRarityHotbar))) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        Inventory inventory = client.player.getInventory();
        int left = (screenWidth - 182) / 2;
        int y = screenHeight - 22;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                HOTBAR_RARITY_STACK[i] = null;
                continue;
            }
            paint(
                    graphics,
                    left + 3 + i * 20,
                    y + 3,
                    resolveHotbarRarity(i, stack),
                    qol,
                    false,
                    true);
        }
    }

    private static ItemRarityPolicy.Rarity resolveHotbarRarity(int slot, ItemStack stack) {
        if (HOTBAR_RARITY_STACK[slot] == stack) {
            return HOTBAR_RARITY_CACHE[slot];
        }
        ItemRarityPolicy.Rarity rarity = ItemRarityPolicy.parseRarity(lore(stack));
        HOTBAR_RARITY_STACK[slot] = stack;
        HOTBAR_RARITY_CACHE[slot] = rarity;
        return rarity;
    }

    private static void paint(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemStack stack,
            QolUtilityConfig qol,
            boolean fill,
            boolean outline) {
        paint(graphics, x, y, ItemRarityPolicy.parseRarity(lore(stack)), qol, fill, outline);
    }

    private static void paint(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemRarityPolicy.Rarity rarity,
            QolUtilityConfig qol,
            boolean fill,
            boolean outline) {
        if (rarity == null) {
            return;
        }
        int color = ItemRarityPolicy.colorFor(
                rarity,
                qol.itemRarityCommon,
                qol.itemRarityUncommon,
                qol.itemRarityRare,
                qol.itemRarityEpic,
                qol.itemRarityLegendary,
                qol.itemRarityMythic,
                qol.itemRarityDivine,
                qol.itemRaritySpecial);
        if (fill && ItemRarityPolicy.drawFill(qol.itemRarityStyle)) {
            graphics.fill(
                    x,
                    y,
                    x + 16,
                    y + 16,
                    ItemRarityPolicy.withAlpha(color, qol.itemRarityFillAlpha));
        }
        if (outline && ItemRarityPolicy.drawOutline(qol.itemRarityStyle)) {
            int edge = ItemRarityPolicy.withAlpha(color, qol.itemRarityOutlineAlpha);
            graphics.fill(x, y, x + 16, y + 1, edge);
            graphics.fill(x, y + 15, x + 16, y + 16, edge);
            graphics.fill(x, y, x + 1, y + 16, edge);
            graphics.fill(x + 15, y, x + 16, y + 16, edge);
        }
    }

    static List<String> lore(ItemStack stack) {
        return InventoryChromeRuntime.loreLines(stack);
    }
}
