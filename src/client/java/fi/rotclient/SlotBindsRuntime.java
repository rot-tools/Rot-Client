package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import java.util.Map;
import java.util.OptionalInt;

/**
 * Inventory slot-bind overlay, bind-key editing, and shift-swap. Rules live
 * in {@link SlotBindsPolicy}.
 */
public final class SlotBindsRuntime {
    private static Integer pendingSlot;

    private SlotBindsRuntime() {
    }

    static void clearPending() {
        pendingSlot = null;
    }

    static void tick(Minecraft client) {
        if (client == null
                || !(client.gui != null && client.gui.screen() instanceof InventoryScreen)) {
            clearPending();
        }
    }

    public static boolean handleKeyPressed(
            AbstractContainerScreen<?> screen,
            int glfwKey,
            Slot hovered) {
        if (!(screen instanceof InventoryScreen) || hovered == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.slotBindsEnabled
                || !InventoryOverlayPolicy.showSkyblockInventoryUi(SkyBlockAreaDetector.isInSkyblock())
                || qol.slotBindSetKey == null
                || qol.slotBindSetKey.isBlank()) {
            return false;
        }
        if (QolKeybindNames.resolveGlfwKey(qol.slotBindSetKey, "") != glfwKey) {
            return false;
        }
        Map<Integer, Integer> binds = qol.slotBindsForActiveProfile();
        SlotBindsPolicy.BindChange change = SlotBindsPolicy.onBindKey(
                hovered.index, pendingSlot, binds);
        return applyChange(qol, change);
    }

    public static boolean handleSlotClicked(
            AbstractContainerScreen<?> screen,
            Slot slot,
            ContainerInput input) {
        if (!(screen instanceof InventoryScreen) || slot == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.slotBindsEnabled
                || !InventoryOverlayPolicy.showSkyblockInventoryUi(SkyBlockAreaDetector.isInSkyblock())
                || input != ContainerInput.QUICK_MOVE) {
            return false;
        }
        OptionalInt partner = SlotBindsPolicy.boundPartner(
                qol.slotBindsForActiveProfile(), slot.index);
        if (partner.isEmpty()) {
            return false;
        }
        OptionalInt inventorySlot = SlotBindsPolicy.swapInventorySlot(slot.index, partner.getAsInt());
        OptionalInt hotbar = SlotBindsPolicy.swapHotbarButton(slot.index, partner.getAsInt());
        if (inventorySlot.isEmpty() || hotbar.isEmpty()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.gameMode == null) {
            return false;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                inventorySlot.getAsInt(),
                hotbar.getAsInt(),
                ContainerInput.SWAP,
                player);
        return true;
    }

    public static void render(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos,
            int mouseX,
            int mouseY,
            Slot hovered) {
        if (!(screen instanceof InventoryScreen) || graphics == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.slotBindsEnabled
                || !InventoryOverlayPolicy.showSkyblockInventoryUi(SkyBlockAreaDetector.isInSkyblock())) {
            return;
        }
        int hoveredIndex = hovered == null ? -1 : hovered.index;
        Map<Integer, Integer> binds = qol.slotBindsForActiveProfile();
        boolean hoveredHasBind = SlotBindsPolicy.boundPartner(binds, hoveredIndex).isPresent();
        if (!SlotBindsPolicy.shouldDrawLines(
                SlotBindsPolicy.parseLineDisplay(qol.slotBindLineDisplay),
                pendingSlot != null,
                hoveredHasBind,
                shiftHeld())) {
            return;
        }
        graphics.nextStratum();
        int color = qol.slotBindColor;
        float width = SlotBindsPolicy.clampLineWidth(qol.slotBindLineWidth);
        if (pendingSlot != null) {
            int[] from = slotCenter(screen, leftPos, topPos, pendingSlot);
            if (from != null) {
                drawLine(graphics, from[0], from[1], mouseX, mouseY, color, width);
            }
        }
        OptionalInt partner = SlotBindsPolicy.boundPartner(binds, hoveredIndex);
        if (partner.isPresent()) {
            int[] from = slotCenter(screen, leftPos, topPos, hoveredIndex);
            int[] to = slotCenter(screen, leftPos, topPos, partner.getAsInt());
            if (from != null && to != null) {
                drawLine(graphics, from[0], from[1], to[0], to[1], color, width);
            }
        }
    }

    private static boolean applyChange(QolUtilityConfig qol, SlotBindsPolicy.BindChange change) {
        LocalPlayer player = Minecraft.getInstance().player;
        switch (change.action()) {
            case START -> {
                pendingSlot = change.pendingSlot();
                return true;
            }
            case COMPLETE -> {
                qol.putSlotBindsForActiveProfile(
                        SlotBindsPolicy.apply(qol.slotBindsForActiveProfile(), change));
                pendingSlot = null;
                TrackerStore.save(RotClientClient.trackerConfig());
                tell(player, "Bound slot " + change.from() + " to " + change.to() + ".");
                return true;
            }
            case REMOVE -> {
                qol.putSlotBindsForActiveProfile(
                        SlotBindsPolicy.apply(qol.slotBindsForActiveProfile(), change));
                pendingSlot = null;
                TrackerStore.save(RotClientClient.trackerConfig());
                tell(player, "Removed slot bind.");
                return true;
            }
            case REJECT_SAME -> {
                pendingSlot = null;
                tell(player, "Cannot bind a slot to itself.");
                return true;
            }
            case REJECT_NO_HOTBAR -> {
                pendingSlot = null;
                tell(player, "Bind one hotbar slot to one inventory slot.");
                return true;
            }
            case IGNORE -> {
                return false;
            }
        }
        return false;
    }

    private static boolean shiftHeld() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        long window = client.getWindow().handle();
        return QolKeybindNames.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || QolKeybindNames.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static int[] slotCenter(
            AbstractContainerScreen<?> screen,
            int leftPos,
            int topPos,
            int index) {
        if (index < 0 || index >= screen.getMenu().slots.size()) {
            return null;
        }
        Slot slot = screen.getMenu().slots.get(index);
        return new int[] {leftPos + slot.x + 8, topPos + slot.y + 8};
    }

    private static void drawLine(
            GuiGraphicsExtractor graphics,
            int x0,
            int y0,
            int x1,
            int y1,
            int color,
            float width) {
        int thickness = Math.max(1, Math.round(width));
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + thickness, y + thickness, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    private static void tell(LocalPlayer player, String text) {
        if (player != null) {
            player.sendSystemMessage(Component.literal(text));
        }
    }
}
