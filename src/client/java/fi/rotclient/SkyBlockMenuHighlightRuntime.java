package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Pets-menu and Anvil slot fills.
 */
public final class SkyBlockMenuHighlightRuntime {
    private SkyBlockMenuHighlightRuntime() {
    }

    public static void afterContainerContents(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos) {
        if (screen == null || graphics == null || screen.getMenu() == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (extras.activePetHighlightEnabled && ActivePetHighlightPolicy.isPetsMenu(title)) {
            for (Slot slot : screen.getMenu().slots) {
                if (slot == null || slot.getItem().isEmpty()) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                if (ActivePetHighlightPolicy.isActivePet(
                        AutoClickerItemIdentity.skyBlockId(stack),
                        InventoryChromeRuntime.loreLines(stack))) {
                    outline(graphics, leftPos + slot.x, topPos + slot.y, extras.activePetHighlightColor);
                }
            }
        }
        if (extras.experimentSolverEnabled) {
            for (int index = 0; index < screen.getMenu().slots.size(); index++) {
                Slot slot = screen.getMenu().slots.get(index);
                if (slot == null) {
                    continue;
                }
                int color = ExperimentSolverRuntime.highlightColor(screen, index);
                if (color != 0) {
                    fill(graphics, leftPos + slot.x, topPos + slot.y, color);
                }
            }
        }
        if (extras.dungeonLeapEnabled || extras.dungeonTerminalsEnabled || extras.dungeonMenusEnabled) {
            for (int index = 0; index < screen.getMenu().slots.size(); index++) {
                Slot slot = screen.getMenu().slots.get(index);
                if (slot == null) {
                    continue;
                }
                int color = DungeonRuntime.highlightColor(screen, index);
                if (color != 0) {
                    fill(graphics, leftPos + slot.x, topPos + slot.y, color);
                }
            }
        }
        if (CommissionDisplayPolicy.isCommissionsMenu(title)
                && extras.miningHelpersEnabled
                && extras.miningHelpersCommissionGui) {
            for (Slot slot : screen.getMenu().slots) {
                if (slot == null || slot.getItem().isEmpty()) {
                    continue;
                }
                if (CommissionDisplayPolicy.loreCompleted(InventoryChromeRuntime.loreLines(slot.getItem()))) {
                    fill(graphics, leftPos + slot.x, topPos + slot.y, 0x8022C55E);
                }
            }
        }
        if (extras.miningHelpersEnabled && extras.miningHelpersFossilExcavator) {
            for (int index = 0; index < screen.getMenu().slots.size(); index++) {
                Slot slot = screen.getMenu().slots.get(index);
                if (slot == null) {
                    continue;
                }
                int color = MiningAssistRuntime.fossilHighlight(screen, index, slot.getItem());
                if (color != 0) {
                    fill(graphics, leftPos + slot.x, topPos + slot.y, color);
                }
            }
        }
        if (extras.stallMarketEnabled) {
            for (Slot slot : screen.getMenu().slots) {
                if (slot == null) {
                    continue;
                }
                Integer color = StallMarketRuntime.slotHighlightArgb(screen, slot);
                if (color != null) {
                    fill(graphics, leftPos + slot.x, topPos + slot.y, color);
                }
            }
        }
        if (extras.anvilHelperEnabled && AnvilHelperPolicy.isAnvilTitle(title)) {
            String target = targetEnchantId(screen);
            if (target.isBlank()) {
                return;
            }
            for (Slot slot : screen.getMenu().slots) {
                if (slot == null || slot.getItem().isEmpty()) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                boolean book = stack.is(Items.ENCHANTED_BOOK);
                if (AnvilHelperPolicy.highlightBook(target, SkyBlockItemData.marketId(stack), book)) {
                    fill(graphics, leftPos + slot.x, topPos + slot.y, extras.anvilHelperColor);
                }
            }
        }
    }

    private static String targetEnchantId(AbstractContainerScreen<?> screen) {
        boolean sawAnvil = false;
        String onlyId = "";
        int idCount = 0;
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || slot.getItem().isEmpty()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (AnvilHelperPolicy.isAnvilMarker(
                    stack.getHoverName().getString(),
                    stack.is(Items.BARRIER))) {
                sawAnvil = true;
            }
            String id = SkyBlockItemData.marketId(stack);
            if (!id.isBlank()) {
                idCount++;
                onlyId = id;
            }
        }
        if (!sawAnvil || idCount != 1) {
            return "";
        }
        return onlyId;
    }

    private static void outline(GuiGraphicsExtractor graphics, int x, int y, int color) {
        int edge = ItemRarityPolicy.withAlpha(color, 0.95F);
        graphics.fill(x, y, x + 16, y + 2, edge);
        graphics.fill(x, y + 14, x + 16, y + 16, edge);
        graphics.fill(x, y, x + 2, y + 16, edge);
        graphics.fill(x + 14, y, x + 16, y + 16, edge);
    }

    private static void fill(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + 16, y + 16, color);
    }
}
