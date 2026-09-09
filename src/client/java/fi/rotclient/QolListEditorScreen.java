package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Ordered-line editor for Custom Scoreboard appearance, events, and chunked stats. */
final class QolListEditorScreen extends Screen {
    private static final int WIDTH = 780;
    private static final int HEIGHT = 450;
    private static final int ROW_H = 26;
    private static final int VISIBLE = 11;

    private final Screen parent;
    private final String settingId;
    private final List<String> choices;
    private final List<String> drafts = new ArrayList<>();
    private int selected;
    private int listScroll;
    private int choiceScroll;
    private String typed = "";
    private boolean typing;

    QolListEditorScreen(Screen parent, String settingId) {
        super(Component.literal(CustomScoreboardPolicy.listSettingTitle(settingId)));
        this.parent = parent;
        this.settingId = settingId == null ? "" : settingId;
        this.choices = new ArrayList<>(CustomScoreboardPolicy.listChoices(this.settingId));
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        String current = qol == null ? "" : qol.readText(this.settingId);
        drafts.addAll(CustomScoreboardPolicy.splitListText(current));
        if (drafts.isEmpty()) {
            drafts.addAll(CustomScoreboardPolicy.splitListText(
                    CustomScoreboardPolicy.defaultListText(this.settingId)));
        }
        selected = drafts.isEmpty() ? -1 : 0;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = panelX();
        int y = panelY();
        String title = CustomScoreboardPolicy.listSettingTitle(settingId);
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, WIDTH, HEIGHT);
        RotClientUiDraw.drawHeaderBar(
                graphics, font, x, y, WIDTH, 40,
                title,
                "Click a row, then Up/Down/Remove. Click a name on the right to add it.");
        graphics.fill(x + 1, y + 41, x + 248, y + HEIGHT - 1, RotClientTheme.SURFACE_ALT);
        drawCurrent(graphics, x, y, mouseX, mouseY);
        drawChoices(graphics, x, y, mouseX, mouseY);
        drawField(graphics, x, y);
        drawButton(graphics, mouseX, mouseY, x + 14, y + HEIGHT - 36, 104, "Up", selected > 0);
        drawButton(graphics, mouseX, mouseY, x + 126, y + HEIGHT - 36, 104, "Down",
                selected >= 0 && selected < drafts.size() - 1);
        drawButton(graphics, mouseX, mouseY, x + 238, y + HEIGHT - 36, 104, "Remove", selected >= 0);
        drawButton(graphics, mouseX, mouseY, x + WIDTH - 350, y + HEIGHT - 36, 104, "Reset", true);
        drawButton(graphics, mouseX, mouseY, x + WIDTH - 238, y + HEIGHT - 36, 104, "Save", true);
        drawButton(graphics, mouseX, mouseY, x + WIDTH - 126, y + HEIGHT - 36, 104, "Cancel", true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawCurrent(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        RotClientUiDraw.text(graphics, font, "ON BOARD", x + 16, y + 50, RotClientTheme.VIOLET, true);
        if (drafts.isEmpty()) {
            RotClientUiDraw.text(
                    graphics, font, "Empty list",
                    x + 20, y + 180, RotClientTheme.TEXT_MUTED, true);
            return;
        }
        int start = Math.max(0, Math.min(listScroll, Math.max(0, drafts.size() - VISIBLE)));
        listScroll = start;
        int rowY = y + 68;
        for (int i = 0; i < VISIBLE && start + i < drafts.size(); i++) {
            int index = start + i;
            int fill = index == selected ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE;
            graphics.fill(x + 10, rowY, x + 238, rowY + ROW_H, fill);
            if (index == selected) {
                graphics.fill(x + 10, rowY, x + 13, rowY + ROW_H, RotClientTheme.HUD_ACCENT);
            }
            RotClientUiDraw.text(
                    graphics, font,
                    RotClientUiDraw.ellipsize(font, drafts.get(index), 210),
                    x + 20, rowY + 8,
                    RotClientTheme.TEXT, true);
            rowY += ROW_H + 4;
        }
    }

    private void drawChoices(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        int left = x + 270;
        RotClientUiDraw.text(graphics, font, "ADD", left, y + 50, RotClientTheme.VIOLET, true);
        int start = Math.max(0, Math.min(choiceScroll, Math.max(0, choices.size() - VISIBLE)));
        choiceScroll = start;
        int rowY = y + 68;
        int colW = 230;
        for (int i = 0; i < VISIBLE && start + i < choices.size(); i++) {
            int index = start + i;
            boolean hover = RotClientUiDraw.inside(mouseX, mouseY, left, rowY, colW, ROW_H);
            graphics.fill(
                    left, rowY, left + colW, rowY + ROW_H,
                    hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE);
            RotClientUiDraw.text(
                    graphics, font,
                    RotClientUiDraw.ellipsize(font, choices.get(index), 210),
                    left + 8, rowY + 8,
                    RotClientTheme.TEXT, false);
            rowY += ROW_H + 4;
        }
        int second = left + 246;
        int extraStart = start + VISIBLE;
        rowY = y + 68;
        for (int i = 0; i < VISIBLE && extraStart + i < choices.size(); i++) {
            int index = extraStart + i;
            boolean hover = RotClientUiDraw.inside(mouseX, mouseY, second, rowY, colW, ROW_H);
            graphics.fill(
                    second, rowY, second + colW, rowY + ROW_H,
                    hover ? RotClientTheme.HOVER_ROW : RotClientTheme.SURFACE);
            RotClientUiDraw.text(
                    graphics, font,
                    RotClientUiDraw.ellipsize(font, choices.get(index), 210),
                    second + 8, rowY + 8,
                    RotClientTheme.TEXT, false);
            rowY += ROW_H + 4;
        }
    }

    private void drawField(GuiGraphicsExtractor graphics, int x, int y) {
        int left = x + 270;
        int top = y + HEIGHT - 78;
        RotClientUiDraw.text(
                graphics, font, "Or type a name and press Enter",
                left, top - 12, RotClientTheme.TEXT_MUTED, false);
        graphics.fill(
                left, top, x + WIDTH - 16, top + 24,
                typing ? RotClientTheme.FIELD_ACTIVE : RotClientTheme.FIELD);
        String shown = typed.isEmpty() ? (typing ? "▌" : "Click here to type") : typed + (typing ? "▌" : "");
        RotClientUiDraw.text(
                graphics, font,
                RotClientUiDraw.ellipsize(font, shown, WIDTH - 300),
                left + 8, top + 7,
                typed.isEmpty() ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT,
                false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) Math.round(event.x());
        int my = (int) Math.round(event.y());
        int x = panelX();
        int y = panelY();
        int start = listScroll;
        int rowY = y + 68;
        for (int i = 0; i < VISIBLE && start + i < drafts.size(); i++) {
            if (RotClientUiDraw.inside(mx, my, x + 10, rowY, 228, ROW_H)) {
                selected = start + i;
                typing = false;
                return true;
            }
            rowY += ROW_H + 4;
        }
        int left = x + 270;
        int choiceStart = choiceScroll;
        rowY = y + 68;
        for (int i = 0; i < VISIBLE && choiceStart + i < choices.size(); i++) {
            if (RotClientUiDraw.inside(mx, my, left, rowY, 230, ROW_H)) {
                addLine(choices.get(choiceStart + i));
                return true;
            }
            rowY += ROW_H + 4;
        }
        int second = left + 246;
        int extraStart = choiceStart + VISIBLE;
        rowY = y + 68;
        for (int i = 0; i < VISIBLE && extraStart + i < choices.size(); i++) {
            if (RotClientUiDraw.inside(mx, my, second, rowY, 230, ROW_H)) {
                addLine(choices.get(extraStart + i));
                return true;
            }
            rowY += ROW_H + 4;
        }
        if (RotClientUiDraw.inside(mx, my, left, y + HEIGHT - 78, WIDTH - 286, 24)) {
            typing = true;
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + 14, y + HEIGHT - 36, 104, 24)) {
            moveSelected(-1);
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + 126, y + HEIGHT - 36, 104, 24)) {
            moveSelected(1);
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + 238, y + HEIGHT - 36, 104, 24) && selected >= 0) {
            drafts.remove(selected);
            selected = drafts.isEmpty() ? -1 : Math.min(selected, drafts.size() - 1);
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + WIDTH - 350, y + HEIGHT - 36, 104, 24)) {
            drafts.clear();
            drafts.addAll(CustomScoreboardPolicy.splitListText(
                    CustomScoreboardPolicy.defaultListText(settingId)));
            selected = drafts.isEmpty() ? -1 : 0;
            listScroll = 0;
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + WIDTH - 238, y + HEIGHT - 36, 104, 24)) {
            saveAndClose();
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + WIDTH - 126, y + HEIGHT - 36, 104, 24)) {
            closeToParent();
            return true;
        }
        typing = false;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int mx = (int) Math.round(mouseX);
        int my = (int) Math.round(mouseY);
        int x = panelX();
        int y = panelY();
        int delta = verticalAmount > 0 ? -1 : 1;
        if (RotClientUiDraw.inside(mx, my, x + 10, y + 60, 228, 320)) {
            listScroll = Math.max(0, listScroll + delta);
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + 270, y + 60, 490, 320)) {
            choiceScroll = Math.max(0, choiceScroll + delta);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!typing || !event.isAllowedChatCharacter()) {
            return super.charTyped(event);
        }
        typed += event.codepointAsString();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (typing) {
                typing = false;
                return true;
            }
            closeToParent();
            return true;
        }
        if (typing && event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!typed.isEmpty()) {
                typed = typed.substring(0, typed.offsetByCodePoints(typed.length(), -1));
            }
            return true;
        }
        if (typing && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            if (!typed.isBlank()) {
                addLine(typed.trim());
                typed = "";
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        drafts.add(line.trim());
        selected = drafts.size() - 1;
        listScroll = Math.max(0, drafts.size() - VISIBLE);
    }

    private void moveSelected(int delta) {
        if (selected < 0) {
            return;
        }
        int next = selected + delta;
        if (next < 0 || next >= drafts.size()) {
            return;
        }
        String line = drafts.remove(selected);
        drafts.add(next, line);
        selected = next;
    }

    private void saveAndClose() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (qol != null) {
            qol.writeText(settingId, CustomScoreboardPolicy.joinListText(drafts));
            TrackerStore.save(RotClientClient.trackerConfig());
        }
        closeToParent();
    }

    private void closeToParent() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private void drawButton(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            int x, int y, int width, String label, boolean enabled) {
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY, x, y, width, label, false, enabled);
    }

    private int panelX() {
        return Math.max(8, (this.width - WIDTH) / 2);
    }

    private int panelY() {
        return Math.max(8, (this.height - HEIGHT) / 2);
    }
}
