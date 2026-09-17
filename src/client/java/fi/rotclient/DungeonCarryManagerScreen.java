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

        int x =
                panelX();

        int y =
                panelY();

        RotClientUiDraw.drawShadowedPanel(
                graphics,
                x,
                y,
                PANEL_WIDTH,
                PANEL_HEIGHT);

        RotClientUiDraw.drawHeaderBar(
                graphics,
                font,
                x,
                y,
                PANEL_WIDTH,
                38,
                "Dungeon Carries",
                historyMode
                        ? "Review completed carries by floor."
                        : "Track active players, floors and remaining runs.");

        /*
         * Floor filter panel.
         */
        RotClientUiDraw.drawElevatedCard(
                graphics,
                x + 8,
                y + 46,
                SIDEBAR_WIDTH - 16,
                PANEL_HEIGHT - 96);

        /*
         * Carry workspace.
         */
        RotClientUiDraw.drawElevatedCard(
                graphics,
                x + SIDEBAR_WIDTH + 6,
                y + 46,
                PANEL_WIDTH - SIDEBAR_WIDTH - 14,
                PANEL_HEIGHT - 96);

        drawSidebar(
                graphics,
                x,
                y,
                mouseX,
                mouseY);

        drawCarries(
                graphics,
                x,
                y,
                mouseX,
                mouseY);

        graphics.fill(
                x + 10,
                y + PANEL_HEIGHT - 49,
                x + PANEL_WIDTH - 10,
                y + PANEL_HEIGHT - 48,
                RotClientTheme.DIVIDER);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + SIDEBAR_WIDTH + 12,
                y + PANEL_HEIGHT - 38,
                150,
                RotClientUiDraw.BUTTON_HEIGHT,
                "Delete selected",
                false,
                !selectedPlayer.isBlank()
                        && !historyMode);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + SIDEBAR_WIDTH + 172,
                y + PANEL_HEIGHT - 38,
                100,
                RotClientUiDraw.BUTTON_HEIGHT,
                historyMode
                        ? "Active"
                        : "History",
                historyMode,
                true);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + PANEL_WIDTH - 132,
                y + PANEL_HEIGHT - 38,
                116,
                RotClientUiDraw.BUTTON_HEIGHT,
                "Done",
                true,
                true);

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta);
    }
    private void drawSidebar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                "FLOOR",
                x + 16,
                y + 48);

        int rowY =
                y + 52;

        drawFloor(
                graphics,
                x,
                rowY,
                "All",
                "",
                mouseX,
                mouseY);

        rowY += 22;

        for (String floor
                : DungeonCarryPolicy.FLOORS) {

            drawFloor(
                    graphics,
                    x,
                    rowY,
                    floor,
                    floor,
                    mouseX,
                    mouseY);

            rowY += 22;
        }
    }
    private void drawFloor(
            GuiGraphicsExtractor graphics,
            int x,
            int rowY,
            String label,
            String floor,
            int mouseX,
            int mouseY) {

        boolean selected =
                filterFloor.equalsIgnoreCase(
                        floor);

        boolean hover =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x + 8,
                        rowY - 3,
                        SIDEBAR_WIDTH - 16,
                        18);

        RotClientUiDraw.drawInteractiveSurface(
                graphics,
                x + 8,
                rowY - 3,
                SIDEBAR_WIDTH - 16,
                18,
                hover
                        ? 1.0F
                        : 0.0F,
                selected,
                RotClientTheme.HUD_ACCENT,
                RotClientUiDraw.RADIUS_SM);

        if (selected) {
            graphics.fill(
                    x + 9,
                    rowY,
                    x + 12,
                    rowY + 11,
                    RotClientTheme.HUD_ACCENT);
        }

        RotClientUiDraw.text(
                graphics,
                font,
                label,
                x + 16,
                rowY,
                selected
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_DIM,
                true);
    }
    private void drawCarries(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int mouseX,
            int mouseY) {

        int left =
                x + SIDEBAR_WIDTH + 12;

        int top =
                y + 52;

        int width =
                PANEL_WIDTH
                        - SIDEBAR_WIDTH
                        - 28;

        List<DungeonCarryPolicy.TrackedCarry> carries =
                filtered();

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                historyMode
                        ? "COMPLETED CARRIES"
                        : "ACTIVE CARRIES",
                left,
                top);

        if (!historyMode) {

            String count =
                    carries.size()
                            + (carries.size() == 1
                            ? " active"
                            : " active");

            RotClientUiDraw.text(
                    graphics,
                    font,
                    count,
                    left + width - font.width(count),
                    top,
                    carries.isEmpty()
                            ? RotClientTheme.TEXT_MUTED
                            : RotClientTheme.HUD_ACCENT,
                    true);
        }

        RotClientUiDraw.helpText(
                graphics,
                font,
                historyMode
                        ? "Completed runs are kept here for quick reference."
                        : "Add with /rot dcarry add <player> <count> <floor>",
                left,
                top + 14);

        if (historyMode) {

            drawHistory(
                    graphics,
                    left,
                    top,
                    width,
                    mouseX,
                    mouseY);

            return;
        }

        if (carries.isEmpty()) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    filterFloor.isBlank()
                            ? "No active carries"
                            : "No active " + filterFloor + " carries",
                    left + 180,
                    top + 130,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "New carries will appear here as soon as they are tracked.",
                    left + 112,
                    top + 149);

            return;
        }

        int rowY =
                top + 42;

        for (DungeonCarryPolicy.TrackedCarry carry
                : carries) {

            boolean selected =
                    carry.player.equalsIgnoreCase(
                            selectedPlayer);

            boolean hover =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            left,
                            rowY,
                            width,
                            38);

            RotClientUiDraw.drawInteractiveSurface(
                    graphics,
                    left,
                    rowY,
                    width,
                    38,
                    hover
                            ? 1.0F
                            : 0.0F,
                    selected,
                    RotClientTheme.HUD_ACCENT,
                    RotClientUiDraw.RADIUS_SM);

            if (selected) {
                graphics.fill(
                        left + 1,
                        rowY + 7,
                        left + 4,
                        rowY + 31,
                        RotClientTheme.HUD_ACCENT);
            }

            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            carry.player,
                            145),
                    left + 10,
                    rowY + 7,
                    RotClientTheme.TEXT,
                    true);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    carry.floor,
                    left + 170,
                    rowY + 7,
                    RotClientTheme.TEXT_DIM,
                    true);

            String progress =
                    carry.completed
                            + " / "
                            + carry.total;

            RotClientUiDraw.text(
                    graphics,
                    font,
                    progress,
                    left + width - 10 - font.width(progress),
                    rowY + 7,
                    carry.done()
                            ? RotClientTheme.SUCCESS
                            : RotClientTheme.HUD_ACCENT,
                    true);

            /*
             * Small run-progress indicator.
             */
            int progressLeft =
                    left + 10;

            int progressRight =
                    left + width - 10;

            int progressWidth =
                    Math.max(
                            1,
                            progressRight - progressLeft);

            graphics.fill(
                    progressLeft,
                    rowY + 30,
                    progressRight,
                    rowY + 32,
                    RotClientTheme.DIVIDER);

            double ratio =
                    carry.total <= 0
                            ? 0.0D
                            : Math.max(
                                    0.0D,
                                    Math.min(
                                            1.0D,
                                            carry.completed
                                                    / (double) carry.total));

            int filled =
                    (int) Math.round(
                            progressWidth * ratio);

            if (filled > 0) {
                graphics.fill(
                        progressLeft,
                        rowY + 30,
                        progressLeft + filled,
                        rowY + 32,
                        carry.done()
                                ? RotClientTheme.SUCCESS
                                : RotClientTheme.HUD_ACCENT);
            }

            rowY += 46;
        }
    }
    private void drawHistory(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int width,
            int mouseX,
            int mouseY) {

        List<DungeonCarryPolicy.HistoryEntry> history =
                DungeonCarryRuntime.snapshot()
                        .history()
                        .stream()
                        .filter(
                                entry ->
                                        filterFloor.isBlank()
                                                || filterFloor.equalsIgnoreCase(
                                                entry.floor))
                        .limit(7)
                        .toList();

        String count =
                history.size()
                        + (history.size() == 1
                        ? " shown"
                        : " shown");

        RotClientUiDraw.text(
                graphics,
                font,
                count,
                left + width - font.width(count),
                top,
                history.isEmpty()
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.HUD_ACCENT,
                true);

        if (history.isEmpty()) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    filterFloor.isBlank()
                            ? "No completed carries yet"
                            : "No completed " + filterFloor + " carries",
                    left + 160,
                    top + 130,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Finished carry sessions will appear here.",
                    left + 150,
                    top + 149);

            return;
        }

        int rowY =
                top + 36;

        for (DungeonCarryPolicy.HistoryEntry entry
                : history) {

            /*
             * History entries are informational rather than selectable.
             */
            RotClientUiDraw.drawElevatedCard(
                    graphics,
                    left,
                    rowY,
                    width,
                    38);

            graphics.fill(
                    left + 1,
                    rowY + 7,
                    left + 4,
                    rowY + 31,
                    RotClientTheme.SUCCESS);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            entry.player,
                            145),
                    left + 10,
                    rowY + 7,
                    RotClientTheme.TEXT,
                    true);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    entry.floor
                            + "  x"
                            + entry.total,
                    left + 170,
                    rowY + 7,
                    RotClientTheme.TEXT_DIM,
                    true);

            String done =
                    "COMPLETED";

            RotClientUiDraw.text(
                    graphics,
                    font,
                    done,
                    left + width - 10 - font.width(done),
                    rowY + 7,
                    RotClientTheme.SUCCESS,
                    true);

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
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
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
