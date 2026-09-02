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

    static void renderHotbar(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.itemRarityEnabled || !qol.itemRarityHotbar || graphics == null) {
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
                continue;
            }
            paint(graphics, left + 3 + i * 20, y + 3, stack, qol, false, true);
        }
    }

    private static void paint(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            ItemStack stack,
            QolUtilityConfig qol,
            boolean fill,
            boolean outline) {
        ItemRarityPolicy.Rarity rarity = ItemRarityPolicy.parseRarity(lore(stack));
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
