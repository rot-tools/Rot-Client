package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Spirit Leap class-panel overlay: 2x2 class panels on Spirit Leap.
 */
public final class DungeonLeapOverlayRuntime {
    private static List<DungeonLeftoverPolicy.LeapCell> cells = List.of();

    private DungeonLeapOverlayRuntime() {
    }

    public static boolean active(AbstractContainerScreen<?> screen) {
        QolSkyblockExtras extras = extras();
        if (screen == null || !extras.dungeonLeapEnabled || !extras.dungeonLeapCustomGui) {
            return false;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        return DungeonPolicy.isLeapMenu(title);
    }

    public static void render(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY) {
        if (!active(screen) || graphics == null) {
            cells = List.of();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        cells = DungeonLeftoverPolicy.leapOverlay(width, height, players(screen));
        graphics.fill(0, 0, width, height, 0xCC0B1220);
        for (DungeonLeftoverPolicy.LeapCell cell : cells) {
            boolean hover = mouseX >= cell.x() && mouseX < cell.x() + cell.width()
                    && mouseY >= cell.y() && mouseY < cell.y() + cell.height();
            DungeonLeftoverPolicy.LeapEntry entry = cell.entry();
            int color = entry == null
                    ? 0x661E293B
                    : DungeonLeftoverPolicy.leapPanelColor(entry.dungeonClass());
            if (hover && entry != null && !entry.dead()) {
                color = color | 0x22000000;
            }
            if (entry != null && entry.dead()) {
                color = 0x80808080;
            }
            graphics.fill(cell.x(), cell.y(), cell.x() + cell.width(), cell.y() + cell.height(), color);
            graphics.fill(cell.x(), cell.y(), cell.x() + cell.width(), cell.y() + 2, 0xFFFFFFFF);
            if (entry == null) {
                RotClientUiDraw.text(graphics, client.font, "Empty",
                        cell.x() + 8, cell.y() + 28, 0xFF94A3B8, false);
                continue;
            }
            String clazz = entry.dungeonClass() == DungeonPolicy.DungeonClass.UNKNOWN
                    ? "?"
                    : entry.dungeonClass().name().charAt(0)
                    + entry.dungeonClass().name().substring(1).toLowerCase();
            RotClientUiDraw.text(graphics, client.font, clazz,
                    cell.x() + 8, cell.y() + 10, 0xFFFFFFFF, false);
            RotClientUiDraw.text(graphics, client.font, entry.name(),
                    cell.x() + 8, cell.y() + 28, entry.dead() ? 0xFF94A3B8 : 0xFFFFFFFF, true);
            if (entry.dead()) {
                RotClientUiDraw.text(graphics, client.font, "DEAD",
                        cell.x() + 8, cell.y() + 44, 0xFFE11D48, false);
            }
        }
    }

    public static boolean click(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!active(screen)) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameMode == null || client.player == null) {
            return false;
        }
        var hit = DungeonLeftoverPolicy.cellAt(cells, mouseX, mouseY);
        if (hit.isEmpty() || hit.get().entry() == null || hit.get().entry().dead()) {
            return true;
        }
        int slot = hit.get().entry().slot();
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slot,
                0,
                ContainerInput.PICKUP,
                client.player);
        return true;
    }

    public static boolean clickDigit(AbstractContainerScreen<?> screen, int digit) {
        if (!active(screen) && (screen == null || !DungeonPolicy.isLeapMenu(
                screen.getTitle() == null ? "" : screen.getTitle().getString()))) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameMode == null || client.player == null) {
            return false;
        }
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        List<DungeonLeftoverPolicy.LeapCell> layout =
                DungeonLeftoverPolicy.leapOverlay(width, height, players(screen));
        int index = digit - 1;
        if (index < 0 || index >= layout.size()) {
            return false;
        }
        DungeonLeftoverPolicy.LeapEntry entry = layout.get(index).entry();
        if (entry == null || entry.dead()) {
            return true;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                entry.slot(),
                0,
                ContainerInput.PICKUP,
                client.player);
        return true;
    }

    private static List<DungeonLeftoverPolicy.LeapEntry> players(AbstractContainerScreen<?> screen) {
        List<DungeonLeftoverPolicy.LeapEntry> out = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || slot.container instanceof net.minecraft.world.entity.player.Inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String name = stack.getHoverName().getString();
            if (name == null || name.isBlank()) {
                continue;
            }
            DungeonPolicy.DungeonClass dungeonClass =
                    DungeonPolicy.classFromLore(InventoryChromeRuntime.loreLines(stack));
            boolean dead = playerDead(name);
            out.add(new DungeonLeftoverPolicy.LeapEntry(slot.index, name, dungeonClass, dead));
        }
        return out;
    }

    private static boolean playerDead(String name) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || name == null || name.isBlank()) {
            return false;
        }
        for (Player player : client.level.players()) {
            if (player != null && name.equalsIgnoreCase(player.getGameProfile().name())
                    && player.isDeadOrDying()) {
                return true;
            }
        }
        return false;
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
