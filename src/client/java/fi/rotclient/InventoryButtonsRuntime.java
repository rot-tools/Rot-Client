package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Server-command inventory shortcuts rendered around every supported container. */
public final class InventoryButtonsRuntime {
    private static InventoryButtonsPolicy.Button dragging;
    private static int grabX;
    private static int grabY;

    private InventoryButtonsRuntime() {}

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                              int left, int top, int guiWidth, int guiHeight,
                              int mouseX, int mouseY) {
        QolSkyblockExtras extras = settings();
        if (!extras.inventoryButtonsEnabled || screen == null || graphics == null) return;
        if (extras.inventoryButtonsInventoryOnly && !isPlayerInventory(screen)) return;
        int[] anchor = buttonAnchor(screen, left, top, guiWidth, guiHeight);
        Minecraft client = Minecraft.getInstance();
        for (InventoryButtonsPolicy.Button button : extras.inventoryButtons) {
            if (!InventoryButtonsPolicy.valid(button)) continue;
            int x = InventoryButtonsPolicy.screenX(button, anchor[0], anchor[2]);
            int y = InventoryButtonsPolicy.screenY(button, anchor[1], anchor[3]);
            int size = InventoryButtonsPolicy.size(button);
            boolean hover = InventoryButtonsPolicy.hit(
                    button, anchor[0], anchor[1], anchor[2], anchor[3], mouseX, mouseY)
                    || button == dragging;
            graphics.fill(x, y, x + size, y + size, hover ? 0xE02A4B5F : 0xD0182A38);
            graphics.fill(x, y, x + size, y + 1, hover ? RotClientTheme.BORDER_BRIGHT : 0xFF31536A);
            graphics.fill(x, y + size - 1, x + size, y + size, 0xFF0A1118);
            ItemStack icon = icon(button.icon);
            graphics.item(icon, x + Math.max(1, (size - 16) / 2), y + Math.max(1, (size - 16) / 2));
            if (hover && extras.inventoryButtonsHoverTooltip && client != null) {
                String tip = dragging != null
                        ? "Release to save this spot"
                        : "/" + InventoryButtonsPolicy.normalizeCommand(button.command)
                        + "  ·  Shift-drag to move";
                RotClientUiDraw.text(graphics, client.font, tip, mouseX + 12, mouseY - 10, 0xFFFFFFFF, true);
            }
        }
    }

    public static boolean click(AbstractContainerScreen<?> screen, int left, int top,
                                int guiWidth, int guiHeight, int mouseX, int mouseY,
                                int mouseButton, boolean shiftDown) {
        QolSkyblockExtras extras = settings();
        if (!extras.inventoryButtonsEnabled || mouseButton != 0 || screen == null) return false;
        if (extras.inventoryButtonsInventoryOnly && !isPlayerInventory(screen)) return false;
        int[] anchor = buttonAnchor(screen, left, top, guiWidth, guiHeight);
        for (InventoryButtonsPolicy.Button button : extras.inventoryButtons) {
            if (!InventoryButtonsPolicy.valid(button)
                    || !InventoryButtonsPolicy.hit(
                    button, anchor[0], anchor[1], anchor[2], anchor[3], mouseX, mouseY)) continue;
            if (shiftDown) {
                int x = InventoryButtonsPolicy.screenX(button, anchor[0], anchor[2]);
                int y = InventoryButtonsPolicy.screenY(button, anchor[1], anchor[3]);
                dragging = button;
                grabX = mouseX - x;
                grabY = mouseY - y;
                return true;
            }
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.player.connection != null) {
                client.player.connection.sendCommand(InventoryButtonsPolicy.normalizeCommand(button.command));
                return true;
            }
        }
        return false;
    }

    public static boolean drag(AbstractContainerScreen<?> screen, int left, int top,
                               int guiWidth, int guiHeight, int mouseX, int mouseY, int mouseButton) {
        if (dragging == null || mouseButton != 0 || screen == null) {
            return false;
        }
        int[] anchor = buttonAnchor(screen, left, top, guiWidth, guiHeight);
        InventoryButtonsPolicy.moveTo(
                dragging,
                mouseX - grabX,
                mouseY - grabY,
                anchor[0],
                anchor[1],
                anchor[2],
                anchor[3]);
        return true;
    }

    public static boolean release(int mouseButton) {
        if (mouseButton != 0 || dragging == null) {
            return false;
        }
        dragging = null;
        TrackerStore.save(RotClientClient.trackerConfig());
        return true;
    }

    private static int[] buttonAnchor(AbstractContainerScreen<?> screen, int left, int top,
                                      int guiWidth, int guiHeight) {
        StorageOverlayPolicy.OverlayLayout layout = StorageOverlayRuntime.lastLayout();
        if (layout == null || !StorageOverlayRuntime.shouldReplaceVanilla(screen)) {
            return new int[] {left, top, guiWidth, guiHeight};
        }
        return new int[] {
                layout.panelX(),
                layout.panelY(),
                layout.panelWidth(),
                layout.panelHeight() + StorageOverlayPolicy.PLAYER_HEIGHT - StorageOverlayPolicy.PLAYER_Y_INSET};
    }

    private static ItemStack icon(String raw) {
        try {
            Identifier id = Identifier.tryParse(InventoryButtonsPolicy.normalizeIcon(raw));
            var item = id == null ? null : BuiltInRegistries.ITEM.getValue(id);
            return new ItemStack(item == null ? Items.COMMAND_BLOCK : item);
        } catch (RuntimeException ignored) {
            return new ItemStack(Items.COMMAND_BLOCK);
        } catch (LinkageError ignored) {
            return new ItemStack(Items.COMMAND_BLOCK);
        }
    }

    private static boolean isPlayerInventory(AbstractContainerScreen<?> screen) {
        String name = screen.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT);
        return name.contains("inventory");
    }

    private static QolSkyblockExtras settings() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
