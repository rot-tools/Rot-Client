package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Read/write manager for dungeon-floor carries. */
final class DungeonCarryManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 720;
    private static final int PANEL_HEIGHT = 430;
    private static final int SIDEBAR_WIDTH = 90;
    private final Screen parent;
    private String filterFloor = "";
    private String selectedPlayer = "";
    private boolean historyMode;

    DungeonCarryManagerScreen(Screen parent) {
        super(Component.literal("Dungeon Carry Manager"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {
        int x = panelX();
        int y = panelY();
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);
        RotClientUiDraw.drawHeaderBar(
                graphics, font, x, y, PANEL_WIDTH, 38,
                "Dungeon Carry Manager",
                historyMode ? "Completed dungeon carries" : "Active dungeon carries");
        graphics.fill(x + 1, y + 39, x + SIDEBAR_WIDTH, y + PANEL_HEIGHT - 1,
                RotClientTheme.SURFACE_ALT);
        drawSidebar(graphics, x, y);
        drawCarries(graphics, x, y);
        drawButton(graphics, mouseX, mouseY, x + SIDEBAR_WIDTH + 12, y + PANEL_HEIGHT - 38, 150,
                "Delete", !selectedPlayer.isBlank());
        drawButton(graphics, mouseX, mouseY, x + SIDEBAR_WIDTH + 172, y + PANEL_HEIGHT - 38, 100,
                historyMode ? "Active" : "History", true);
        drawButton(graphics, mouseX, mouseY, x + PANEL_WIDTH - 132, y + PANEL_HEIGHT - 38, 116,
                "Done", true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawSidebar(GuiGraphicsExtractor graphics, int x, int y) {
        int rowY = y + 52;
        drawFloor(graphics, x, rowY, "All", "");
        rowY += 22;
        for (String floor : DungeonCarryPolicy.FLOORS) {
            drawFloor(graphics, x, rowY, floor, floor);
            rowY += 22;
        }
    }

    private void drawFloor(GuiGraphicsExtractor graphics, int x, int rowY, String label, String floor) {
        boolean selected = filterFloor.equalsIgnoreCase(floor);
        if (selected) {
            graphics.fill(x + 8, rowY - 3, x + SIDEBAR_WIDTH - 8, rowY + 14,
                    RotClientTheme.SELECTED_ROW);
            graphics.fill(x + 8, rowY - 3, x + 11, rowY + 14, RotClientTheme.HUD_ACCENT);
        }
        RotClientUiDraw.text(graphics, font, label, x + 16, rowY, selected
                ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED, true);
    }

    private void drawCarries(GuiGraphicsExtractor graphics, int x, int y) {
        int left = x + SIDEBAR_WIDTH + 12;
        int top = y + 52;
        int width = PANEL_WIDTH - SIDEBAR_WIDTH - 28;
        RotClientUiDraw.text(graphics, font, historyMode ? "Completed carries" : "Active carries",
                left, top, RotClientTheme.HUD_ACCENT, true);
        RotClientUiDraw.text(graphics, font,
                "Add with /rot dcarry add <player> <count> <floor>",
                left, top + 14, RotClientTheme.TEXT_MUTED, false);
        if (historyMode) {
            drawHistory(graphics, left, top, width);
            return;
        }
        List<DungeonCarryPolicy.TrackedCarry> carries = filtered();
        if (carries.isEmpty()) {
            RotClientUiDraw.text(graphics, font, "No active carries", left + 170, top + 130,
                    RotClientTheme.TEXT_MUTED, true);
            return;
        }
        int rowY = top + 42;
        for (DungeonCarryPolicy.TrackedCarry carry : carries) {
            boolean selected = carry.player.equalsIgnoreCase(selectedPlayer);
            graphics.fill(left, rowY, left + width, rowY + 38,
                    selected ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT);
            if (selected) {
                graphics.fill(left, rowY, left + 3, rowY + 38, RotClientTheme.HUD_ACCENT);
            }
            RotClientUiDraw.text(graphics, font, carry.player, left + 10, rowY + 7,
                    RotClientTheme.TEXT, true);
            RotClientUiDraw.text(graphics, font, carry.floor, left + 170, rowY + 7,
                    RotClientTheme.TEXT_MUTED, true);
            RotClientUiDraw.text(graphics, font, carry.completed + " / " + carry.total,
                    left + width - 70, rowY + 7,
                    carry.done() ? RotClientTheme.SUCCESS : RotClientTheme.HUD_ACCENT, true);
            rowY += 46;
        }
    }

    private void drawHistory(GuiGraphicsExtractor graphics, int left, int top, int width) {
        List<DungeonCarryPolicy.HistoryEntry> history = DungeonCarryRuntime.snapshot().history().stream()
                .filter(entry -> filterFloor.isBlank() || filterFloor.equalsIgnoreCase(entry.floor))
                .limit(7)
                .toList();
        if (history.isEmpty()) {
            RotClientUiDraw.text(graphics, font, "No completed carries yet", left + 170, top + 130,
                    RotClientTheme.TEXT_MUTED, true);
            return;
        }
        int rowY = top + 36;
        for (DungeonCarryPolicy.HistoryEntry entry : history) {
            graphics.fill(left, rowY, left + width, rowY + 38, RotClientTheme.SURFACE_ALT);
            RotClientUiDraw.text(graphics, font, entry.player, left + 10, rowY + 7, RotClientTheme.TEXT, true);
            RotClientUiDraw.text(graphics, font, entry.floor + " x" + entry.total,
                    left + 170, rowY + 7, RotClientTheme.TEXT_MUTED, true);
            rowY += 44;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mouseX = (int) Math.round(event.x());
        int mouseY = (int) Math.round(event.y());
        int x = panelX();
        int y = panelY();
        int rowY = y + 52;
        if (RotClientUiDraw.inside(mouseX, mouseY, x + 8, rowY - 3, SIDEBAR_WIDTH - 16, 18)) {
            filterFloor = "";
            selectedPlayer = "";
            return true;
        }
        rowY += 22;
        for (String floor : DungeonCarryPolicy.FLOORS) {
            if (RotClientUiDraw.inside(mouseX, mouseY, x + 8, rowY - 3, SIDEBAR_WIDTH - 16, 18)) {
                filterFloor = floor;
                selectedPlayer = "";
                return true;
            }
            rowY += 22;
        }
        int carryY = y + 94;
        int left = x + SIDEBAR_WIDTH + 12;
        int width = PANEL_WIDTH - SIDEBAR_WIDTH - 28;
        for (DungeonCarryPolicy.TrackedCarry carry : historyMode ? List.<DungeonCarryPolicy.TrackedCarry>of() : filtered()) {
            if (RotClientUiDraw.inside(mouseX, mouseY, left, carryY, width, 38)) {
                selectedPlayer = carry.player;
                return true;
            }
            carryY += 46;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, x + SIDEBAR_WIDTH + 172,
                y + PANEL_HEIGHT - 38, 100, RotClientUiDraw.BUTTON_HEIGHT)) {
            historyMode = !historyMode;
            selectedPlayer = "";
            return true;
        }
        if (!selectedPlayer.isBlank()
                && RotClientUiDraw.inside(mouseX, mouseY, x + SIDEBAR_WIDTH + 12,
                y + PANEL_HEIGHT - 38, 150, RotClientUiDraw.BUTTON_HEIGHT)) {
            DungeonCarryRuntime.remove(selectedPlayer);
            selectedPlayer = "";
            return true;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, x + PANEL_WIDTH - 132,
                y + PANEL_HEIGHT - 38, 116, RotClientUiDraw.BUTTON_HEIGHT)) {
            Minecraft.getInstance().gui.setScreen(parent);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<DungeonCarryPolicy.TrackedCarry> filtered() {
        return DungeonCarryRuntime.snapshot().active().stream()
                .filter(carry -> filterFloor.isBlank() || filterFloor.equalsIgnoreCase(carry.floor))
                .toList();
    }

    private void drawButton(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            String label,
            boolean enabled) {
        RotClientUiDraw.drawButton(graphics, font, mouseX, mouseY, x, y, width, label, false, enabled);
    }

    private int panelX() {
        return Math.max(8, (width - PANEL_WIDTH) / 2);
    }

    private int panelY() {
        return Math.max(8, (height - PANEL_HEIGHT) / 2);
    }
}
