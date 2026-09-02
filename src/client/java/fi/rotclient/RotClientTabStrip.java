package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;

import java.util.List;

/**
 * Browser-style tab strip drawing and hit-testing for Rot Client.
 * Tabs sit at the top of the dashboard window, like Chrome.
 */
final class RotClientTabStrip {
    static final int HEIGHT = RotClientDashboardLayout.TAB_STRIP_HEIGHT;
    static final int MIN_TAB_WIDTH = 72;
    static final int MAX_TAB_WIDTH = 140;
    static final int NEW_TAB_WIDTH = 22;
    static final int WINDOW_BUTTON = 20;
    static final int WINDOW_CONTROLS = WINDOW_BUTTON * 2 + 6;
    static final int CLOSE_SIZE = 10;
    static final int PAD = 6;
    static final int DRAG_THRESHOLD = 4;

    private RotClientTabStrip() {
    }

    record Layout(
            int x,
            int y,
            int width,
            int tabWidth,
            int scrollPixels,
            int maxScroll,
            int plusX,
            int maximizeX,
            int closeX) {
    }

    static Layout compute(
            int panelX,
            int panelY,
            int panelWidth,
            int tabCount) {
        int x = panelX + 8;
        int y = panelY;
        int available = Math.max(
                MIN_TAB_WIDTH,
                panelWidth - 16 - NEW_TAB_WIDTH - 8 - WINDOW_CONTROLS);
        int count = Math.max(1, tabCount);
        int tabWidth = Math.min(MAX_TAB_WIDTH, Math.max(MIN_TAB_WIDTH, available / count));
        int content = count * tabWidth;
        int maxScroll = Math.max(0, content - available);
        int plusX = x + available + 4;
        int closeX = panelX + panelWidth - 8 - WINDOW_BUTTON;
        int maximizeX = closeX - WINDOW_BUTTON - 2;
        return new Layout(x, y, available, tabWidth, 0, maxScroll, plusX, maximizeX, closeX);
    }

    static String draw(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            List<RotClientWorkspaceTab> tabs,
            String activeTabId,
            int stripScroll,
            int mouseX,
            int mouseY,
            Integer draggingIndex,
            boolean maximized) {
        String hoverTip = null;
        int y = layout.y();
        graphics.fill(
                layout.x() - 2,
                y,
                layout.closeX() + WINDOW_BUTTON + 4,
                y + HEIGHT,
                RotClientUiDraw.withAlpha(RotClientTheme.SURFACE_ALT, 0xAA));
        int scroll = Math.max(0, Math.min(stripScroll, layout.maxScroll()));
        int drawX = layout.x() - scroll;
        for (int i = 0; i < tabs.size(); i++) {
            RotClientWorkspaceTab tab = tabs.get(i);
            boolean active = tab.id.equals(activeTabId);
            boolean hover = RotClientUiDraw.inside(
                    mouseX, mouseY, drawX, y, layout.tabWidth(), HEIGHT);
            if (draggingIndex != null && draggingIndex == i) {
                hover = true;
            }
            int fill = active
                    ? RotClientTheme.SELECTED_ROW
                    : (hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE);
            RotClientUiDraw.roundedFill(
                    graphics,
                    drawX,
                    y + 2,
                    drawX + layout.tabWidth() - 2,
                    y + HEIGHT,
                    fill,
                    RotClientUiDraw.RADIUS_SM);
            if (active) {
                graphics.fill(
                        drawX + 2,
                        y + HEIGHT - 2,
                        drawX + layout.tabWidth() - 4,
                        y + HEIGHT,
                        RotClientTheme.HUD_ACCENT);
            }
            String title = RotClientUiDraw.ellipsizeAndHover(
                    font, tab.title(), layout.tabWidth() - 28, drawX + 8, y + 7, 12);
            if (hover && RotClientUiDraw.truncated(font, tab.title(), layout.tabWidth() - 28)) {
                hoverTip = tab.title();
            }
            RotClientUiDraw.text(graphics, font,
                    title,
                    drawX + 8,
                    y + 9,
                    active ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM,
                    active);
            int closeX = drawX + layout.tabWidth() - 16;
            int closeY = y + 8;
            boolean closeHover = RotClientUiDraw.inside(
                    mouseX, mouseY, closeX, closeY, CLOSE_SIZE, CLOSE_SIZE);
            RotClientUiDraw.text(graphics, font,
                    "×",
                    closeX,
                    closeY - 1,
                    closeHover ? RotClientTheme.WARNING : RotClientTheme.TEXT_MUTED,
                    false);
            drawX += layout.tabWidth();
        }
        boolean plusHover = RotClientUiDraw.inside(
                mouseX, mouseY, layout.plusX(), y + 2, NEW_TAB_WIDTH, HEIGHT - 2);
        RotClientUiDraw.roundedFill(
                graphics,
                layout.plusX(),
                y + 2,
                layout.plusX() + NEW_TAB_WIDTH,
                y + HEIGHT,
                plusHover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE_ALT,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.text(graphics, font,
                "+",
                layout.plusX() + 7,
                y + 8,
                RotClientTheme.TEXT,
                false);

        boolean maxHover = hitMaximize(layout, mouseX, mouseY);
        RotClientUiDraw.roundedFill(
                graphics,
                layout.maximizeX(),
                y + 3,
                layout.maximizeX() + WINDOW_BUTTON,
                y + HEIGHT - 1,
                maxHover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.text(graphics, font,
                maximized ? "❐" : "□",
                layout.maximizeX() + 6,
                y + 8,
                RotClientTheme.TEXT,
                false);

        boolean winCloseHover = hitWindowClose(layout, mouseX, mouseY);
        RotClientUiDraw.roundedFill(
                graphics,
                layout.closeX(),
                y + 3,
                layout.closeX() + WINDOW_BUTTON,
                y + HEIGHT - 1,
                winCloseHover ? RotClientTheme.WARNING : RotClientTheme.SURFACE,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.text(graphics, font,
                "×",
                layout.closeX() + 6,
                y + 7,
                winCloseHover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                false);
        return hoverTip;
    }

    static int hitTabIndex(
            Layout layout,
            int tabCount,
            int stripScroll,
            int mouseX,
            int mouseY) {
        if (!RotClientUiDraw.inside(
                mouseX, mouseY, layout.x(), layout.y(), layout.width(), HEIGHT)) {
            return -1;
        }
        int scroll = Math.max(0, Math.min(stripScroll, layout.maxScroll()));
        int local = mouseX - layout.x() + scroll;
        int index = local / Math.max(1, layout.tabWidth());
        if (index < 0 || index >= tabCount) {
            return -1;
        }
        return index;
    }

    static boolean hitClose(
            Layout layout,
            int tabIndex,
            int stripScroll,
            int mouseX,
            int mouseY) {
        int scroll = Math.max(0, Math.min(stripScroll, layout.maxScroll()));
        int drawX = layout.x() - scroll + tabIndex * layout.tabWidth();
        int closeX = drawX + layout.tabWidth() - 16;
        int closeY = layout.y() + 8;
        return RotClientUiDraw.inside(mouseX, mouseY, closeX, closeY, CLOSE_SIZE, CLOSE_SIZE);
    }

    static boolean hitNewTab(Layout layout, int mouseX, int mouseY) {
        return RotClientUiDraw.inside(
                mouseX, mouseY, layout.plusX(), layout.y() + 2, NEW_TAB_WIDTH, HEIGHT - 2);
    }

    static boolean hitMaximize(Layout layout, int mouseX, int mouseY) {
        return RotClientUiDraw.inside(
                mouseX, mouseY, layout.maximizeX(), layout.y() + 3, WINDOW_BUTTON, HEIGHT - 4);
    }

    static boolean hitWindowClose(Layout layout, int mouseX, int mouseY) {
        return RotClientUiDraw.inside(
                mouseX, mouseY, layout.closeX(), layout.y() + 3, WINDOW_BUTTON, HEIGHT - 4);
    }

    /**
     * Empty tab-strip / caption area — drag this like a Windows title bar.
     */
    static boolean hitCaptionDrag(
            Layout layout,
            int panelX,
            int panelWidth,
            int tabCount,
            int stripScroll,
            int mouseX,
            int mouseY) {
        if (!RotClientUiDraw.inside(
                mouseX, mouseY, panelX, layout.y(), panelWidth, HEIGHT)) {
            return false;
        }
        if (hitNewTab(layout, mouseX, mouseY)
                || hitMaximize(layout, mouseX, mouseY)
                || hitWindowClose(layout, mouseX, mouseY)) {
            return false;
        }
        return hitTabIndex(layout, tabCount, stripScroll, mouseX, mouseY) < 0;
    }

    static int dropIndex(
            Layout layout,
            int tabCount,
            int stripScroll,
            int mouseX) {
        int scroll = Math.max(0, Math.min(stripScroll, layout.maxScroll()));
        int local = mouseX - layout.x() + scroll;
        int index = (int) Math.floor(local / (double) Math.max(1, layout.tabWidth()));
        if (index < 0) {
            return 0;
        }
        if (index >= tabCount) {
            return Math.max(0, tabCount - 1);
        }
        return index;
    }
}
