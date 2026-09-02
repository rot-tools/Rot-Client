package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Read/write manager for the one live Slayer carry list. */
final class SlayerCarryManagerScreen extends Screen {
    private static final int PANEL_WIDTH = 720;
    private static final int PANEL_HEIGHT = 430;
    private static final int SIDEBAR_WIDTH = 135;
    private final Screen parent;
    private SlayerPolicy.SlayerType filter;
    private String selectedPlayer = "";
    private boolean historyMode;

    SlayerCarryManagerScreen(Screen parent) {
        super(Component.literal("Slayer Carry Manager"));
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
                "Slayer Carry Manager", historyMode ? "Persistent local carry history" : "Canonical active carry list");
        graphics.fill(x + 1, y + 39, x + SIDEBAR_WIDTH, y + PANEL_HEIGHT - 1,
                RotClientTheme.SURFACE_ALT);
        drawSidebar(graphics, x, y);
        drawCarries(graphics, x, y);
        drawButton(graphics, mouseX, mouseY, x + SIDEBAR_WIDTH + 12, y + PANEL_HEIGHT - 38, 150,
                "Complete", !selectedPlayer.isBlank());
        drawButton(graphics, mouseX, mouseY, x + SIDEBAR_WIDTH + 172, y + PANEL_HEIGHT - 38, 150,
                "Delete", !selectedPlayer.isBlank());
        drawButton(graphics, mouseX, mouseY, x + SIDEBAR_WIDTH + 332, y + PANEL_HEIGHT - 38, 100,
                historyMode ? "Active" : "History", true);
        drawButton(graphics, mouseX, mouseY, x + PANEL_WIDTH - 132, y + PANEL_HEIGHT - 38, 116,
                "Done", true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawSidebar(GuiGraphicsExtractor graphics, int x, int y) {
        String[] labels = {"Global", "Revenant", "Tarantula", "Sven", "Voidgloom", "Inferno", "Bloodfiend"};
        int rowY = y + 52;
        for (int i = 0; i < labels.length; i++) {
            boolean selected = i == 0 ? filter == null : filter == typeAt(i);
            if (selected) {
                graphics.fill(x + 8, rowY - 3, x + SIDEBAR_WIDTH - 8, rowY + 14,
                        RotClientTheme.SELECTED_ROW);
                graphics.fill(x + 8, rowY - 3, x + 11, rowY + 14, RotClientTheme.HUD_ACCENT);
            }
            RotClientUiDraw.text(graphics, font, labels[i], x + 16, rowY, selected
                    ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED, true);
            rowY += 30;
        }
    }

    private void drawCarries(GuiGraphicsExtractor graphics, int x, int y) {
        int left = x + SIDEBAR_WIDTH + 12;
        int top = y + 52;
        int width = PANEL_WIDTH - SIDEBAR_WIDTH - 28;
        RotClientUiDraw.text(graphics, font, historyMode ? "Completed carries" : filter == null ? "All active carries" : filter.displayName(),
                left, top, RotClientTheme.HUD_ACCENT, true);
        if (historyMode) {
            drawHistory(graphics, left, top, width);
            return;
        }
        RotClientUiDraw.text(graphics, font,
                "Add with /rot slayer carry add <player> <count> <type> <tier>",
                left, top + 14, RotClientTheme.TEXT_MUTED, false);
        List<SlayerSessionEngine.Carry> carries = filteredCarries();
        if (carries.isEmpty()) {
            RotClientUiDraw.text(graphics, font, "No active carries", left + 170, top + 130,
                    RotClientTheme.TEXT_MUTED, true);
            return;
        }
        int rowY = top + 42;
        for (SlayerSessionEngine.Carry carry : carries) {
            boolean selected = carry.player().equalsIgnoreCase(selectedPlayer);
            graphics.fill(left, rowY, left + width, rowY + 38,
                    selected ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT);
            if (selected) {
                graphics.fill(left, rowY, left + 3, rowY + 38, RotClientTheme.HUD_ACCENT);
            }
            RotClientUiDraw.text(graphics, font, carry.player(), left + 10, rowY + 7,
                    RotClientTheme.TEXT, true);
            RotClientUiDraw.text(graphics, font,
                    carry.type().displayName() + tierSuffix(carry.tier()),
                    left + 170, rowY + 7, RotClientTheme.TEXT_MUTED, true);
            RotClientUiDraw.text(graphics, font, carry.completed() + " / " + carry.total(),
                    left + width - 70, rowY + 7,
                    carry.complete() ? RotClientTheme.SUCCESS : RotClientTheme.HUD_ACCENT, true);
            RotClientUiDraw.text(graphics, font,
                    carry.complete() ? "Complete" : "In progress",
                    left + 10, rowY + 21,
                    carry.complete() ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_MUTED, false);
            rowY += 46;
        }
    }

    private void drawHistory(GuiGraphicsExtractor graphics, int left, int top, int width) {
        List<SlayerCarryPolicy.HistoryEntry> history = RotClientClient.qolConfigPublic().extras()
                .slayerCarryHistory.stream()
                .filter(entry -> filter == null || entry.slayerType() == filter)
                .sorted(java.util.Comparator.comparingLong((SlayerCarryPolicy.HistoryEntry e) -> e.completedAtMillis).reversed())
                .limit(7).toList();
        if (history.isEmpty()) {
            RotClientUiDraw.text(graphics, font, "No completed carries yet", left + 170, top + 130,
                    RotClientTheme.TEXT_MUTED, true);
            return;
        }
        int rowY = top + 36;
        for (SlayerCarryPolicy.HistoryEntry entry : history) {
            graphics.fill(left, rowY, left + width, rowY + 38, RotClientTheme.SURFACE_ALT);
            SlayerPolicy.SlayerType type = entry.slayerType();
            RotClientUiDraw.text(graphics, font, entry.player, left + 10, rowY + 7, RotClientTheme.TEXT, true);
            RotClientUiDraw.text(graphics, font, (type == null ? "Unknown" : type.displayName()) + tierSuffix(entry.tier),
                    left + 170, rowY + 7, RotClientTheme.TEXT_MUTED, true);
            RotClientUiDraw.text(graphics, font, entry.amount + " bosses", left + width - 100, rowY + 7, RotClientTheme.SUCCESS, true);
            RotClientUiDraw.text(graphics, font, "Completed in " + SlayerRuntime.duration(entry.durationMillis),
                    left + 10, rowY + 21, RotClientTheme.TEXT_MUTED, false);
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
        for (int i = 0; i < 7; i++) {
            if (RotClientUiDraw.inside(mouseX, mouseY, x + 8, y + 49 + i * 30,
                    SIDEBAR_WIDTH - 16, 20)) {
                filter = i == 0 ? null : typeAt(i);
                selectedPlayer = "";
                return true;
            }
        }
        int rowY = y + 94;
        int left = x + SIDEBAR_WIDTH + 12;
        int width = PANEL_WIDTH - SIDEBAR_WIDTH - 28;
        for (SlayerSessionEngine.Carry carry : historyMode ? List.<SlayerSessionEngine.Carry>of() : filteredCarries()) {
            if (RotClientUiDraw.inside(mouseX, mouseY, left, rowY, width, 38)) {
                selectedPlayer = carry.player();
                return true;
            }
            rowY += 46;
        }
        if (RotClientUiDraw.inside(mouseX, mouseY, x + SIDEBAR_WIDTH + 332,
                y + PANEL_HEIGHT - 38, 100, RotClientUiDraw.BUTTON_HEIGHT)) {
            historyMode = !historyMode;
            selectedPlayer = "";
            return true;
        }
        if (!selectedPlayer.isBlank()
                && RotClientUiDraw.inside(mouseX, mouseY, x + SIDEBAR_WIDTH + 12,
                y + PANEL_HEIGHT - 38, 150, RotClientUiDraw.BUTTON_HEIGHT)) {
            SlayerRuntime.completeCarry(selectedPlayer);
            return true;
        }
        if (!selectedPlayer.isBlank()
                && RotClientUiDraw.inside(mouseX, mouseY, x + SIDEBAR_WIDTH + 172,
                y + PANEL_HEIGHT - 38, 150, RotClientUiDraw.BUTTON_HEIGHT)) {
            SlayerRuntime.engine().removeCarry(selectedPlayer);
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

    private List<SlayerSessionEngine.Carry> filteredCarries() {
        return SlayerRuntime.engine().carries().stream()
                .filter(carry -> filter == null || carry.type() == filter)
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
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY, x, y, width, label, false, enabled);
    }

    private int panelX() {
        return Math.max(8, (width - PANEL_WIDTH) / 2);
    }

    private int panelY() {
        return Math.max(8, (height - PANEL_HEIGHT) / 2);
    }

    private static SlayerPolicy.SlayerType typeAt(int index) {
        return switch (index) {
            case 1 -> SlayerPolicy.SlayerType.REVENANT;
            case 2 -> SlayerPolicy.SlayerType.TARANTULA;
            case 3 -> SlayerPolicy.SlayerType.SVEN;
            case 4 -> SlayerPolicy.SlayerType.VOIDGLOOM;
            case 5 -> SlayerPolicy.SlayerType.INFERNO;
            default -> SlayerPolicy.SlayerType.VAMPIRE;
        };
    }

    private static String tierSuffix(int tier) {
        return tier <= 0 ? " · Any tier" : " · Tier " + tier;
    }
}
