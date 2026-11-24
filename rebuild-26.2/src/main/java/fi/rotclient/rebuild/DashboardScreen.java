package fi.rotclient.rebuild;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class DashboardScreen extends Screen {
    private static final int TEXT = 0xFFF8F5FF;
    private static final int MUTED = 0xFFC8BFD8;
    private static final int ROW = 23;
    private final Screen parent;
    private Category category;
    private ModuleCard detail;
    private ModuleCard.Keybind capture;
    private EditBox search;
    private String query = "";
    private int left, top, bodyX, bodyWidth, right, bottom, navWidth, navOffset, contentOffset;
    private int pageRows;
    private List<ModuleCard> results = List.of();
    private List<ModuleCard.Control> controls = List.of();
    private final List<Button> contentButtons = new ArrayList<>();
    private boolean buildingContent;

    DashboardScreen(Screen parent) {
        super(Component.literal(RebuildClient.editionName()));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(670, width - 18);
        int panelHeight = Math.min(415, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        right = left + panelWidth;
        bottom = top + panelHeight;
        navWidth = Math.min(129, panelWidth / 3);
        bodyX = left + navWidth + 15;
        bodyWidth = right - bodyX - 12;
        pageRows = Math.max(1, (bottom - top - 114) / ROW);
        button("×", right - 27, top + 7, 19, 18, this::onClose);
        addSidebar();
        if (detail != null) button("‹ Modules", bodyX, top + 37, 69, 19, () -> select(null, false));
        int searchInset = detail == null ? 0 : 76;
        search = new EditBox(font, bodyX + searchInset, top + 37, bodyWidth - searchInset, 19,
                Component.literal(detail == null ? "Search modules" : "Search settings"));
        search.setMaxLength(100);
        search.setHint(Component.literal(detail == null ? "Search modules..." : "Search settings..."));
        search.setValue(query);
        search.setResponder(value -> {
            query = value;
            contentOffset = 0;
            rebuildContent();
        });
        addRenderableWidget(search);
        rebuildContent();
    }

    private void addSidebar() {
        int x = left + 8;
        int w = navWidth - 13;
        button("HUD Editor", x, top + 37, w, 19, () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        button("Appearance", x, top + 59, w, 19, () -> minecraft.gui.setScreen(new AppearanceScreen(this)));
        button("All modules", x, top + 86, w, 18, () -> chooseCategory(null));
        int visible = sidebarRows();
        Category[] categories = Category.values();
        navOffset = Math.clamp(navOffset, 0, Math.max(0, categories.length - visible));
        for (int index = 0; index < visible && index + navOffset < categories.length; index++) {
            Category item = categories[index + navOffset];
            String label = (category == item && detail == null ? "› " : "") + item.title();
            button(label, x, top + 107 + index * 18, w, 17, () -> chooseCategory(item));
        }
        if (visible < categories.length) {
            int half = (w - 3) / 2;
            button("▲", x, bottom - 21, half, 16, () -> scrollSidebar(-1)).active = navOffset > 0;
            button("▼", x + half + 3, bottom - 21, half, 16, () -> scrollSidebar(1)).active =
                    navOffset + visible < categories.length;
        }
    }

    private int sidebarRows() {
        return Math.max(0, (bottom - top - 130) / 18);
    }

    private void chooseCategory(Category category) {
        this.category = category;
        select(null, true);
    }

    private void select(ModuleCard module, boolean resetQuery) {
        if (resetQuery || detail != module) query = "";
        detail = module;
        capture = null;
        contentOffset = 0;
        rebuildWidgets();
    }

    private void scrollSidebar(int delta) {
        int next = Math.clamp(navOffset + delta, 0, Math.max(0, Category.values().length - sidebarRows()));
        if (next != navOffset) {
            navOffset = next;
            rebuildWidgets();
        }
    }

    private void rebuildContent() {
        contentButtons.forEach(this::removeWidget);
        contentButtons.clear();
        buildingContent = true;
        String filter = query.strip().toLowerCase(Locale.ROOT);
        int length;
        if (detail == null) {
            results = RebuildClient.modules().stream()
                    .filter(module -> category == null || module.category() == category)
                    .filter(module -> matches(module.title(), filter) || matches(module.description(), filter))
                    .toList();
            length = results.size();
        } else {
            controls = detail.controls().stream().filter(control -> matches(control.label(), filter)).toList();
            length = controls.size();
        }
        contentOffset = Math.clamp(contentOffset, 0, Math.max(0, length - pageRows));
        for (int i = 0; i < pageRows && contentOffset + i < length; i++) {
            int y = top + 83 + ROW * i;
            if (detail == null) {
                ModuleCard module = results.get(contentOffset + i);
                button(module.title(), bodyX, y, bodyWidth, 20, () -> select(module, true));
            } else {
                addControl(controls.get(contentOffset + i), y);
            }
        }
        if (length > pageRows) {
            button("▲", right - 54, bottom - 23, 19, 17, () -> scrollContent(-1)).active = contentOffset > 0;
            button("▼", right - 31, bottom - 23, 19, 17, () -> scrollContent(1)).active =
                    contentOffset + pageRows < length;
        }
        buildingContent = false;
    }

    private void addControl(ModuleCard.Control control, int y) {
        switch (control) {
            case ModuleCard.Toggle toggle -> button(toggle.label() + "   " + state(toggle), bodyX, y, bodyWidth, 20,
                    () -> {
                        toggle.write().accept(!toggle.read().getAsBoolean());
                        rebuildWidgets();
                    });
            case ModuleCard.Action action -> button(action.label(), bodyX, y, bodyWidth, 20,
                    () -> action.open().accept(this));
            case ModuleCard.Keybind keybind -> {
                int key = keybind.read().getAsInt();
                String name = key < 0 ? "Unbound" : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
                button(keybind.label() + "   " + (capture == keybind ? "Press key..." : name), bodyX, y, bodyWidth, 20,
                        () -> {
                            capture = keybind;
                            rebuildWidgets();
                        });
            }
        }
    }

    private static String state(ModuleCard.Toggle toggle) {
        return toggle.read().getAsBoolean() ? "ON" : "OFF";
    }

    private static boolean matches(String text, String filter) {
        return text.toLowerCase(Locale.ROOT).contains(filter);
    }

    private Button button(String text, int x, int y, int w, int h, Runnable click) {
        Button button = addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(text, Math.max(1, w - 9))),
                ignored -> click.run()).bounds(x, y, w, h).build());
        if (buildingContent) contentButtons.add(button);
        return button;
    }

    private void scrollContent(int delta) {
        int length = detail == null ? results.size() : controls.size();
        int next = Math.clamp(contentOffset + delta, 0, Math.max(0, length - pageRows));
        if (next != contentOffset) {
            contentOffset = next;
            rebuildContent();
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= left && x < right && y >= top + 58 && y < bottom && vertical != 0) {
            int direction = vertical > 0 ? -1 : 1;
            if (x < bodyX) scrollSidebar(direction);
            else scrollContent(direction);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capture != null) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE && event.key() != GLFW.GLFW_KEY_UNKNOWN) {
                capture.write().accept(event.key() == GLFW.GLFW_KEY_BACKSPACE || event.key() == GLFW.GLFW_KEY_DELETE
                        ? -1 : event.key());
            }
            capture = null;
            rebuildWidgets();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && detail != null) {
            select(null, true);
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean isEditing() {
        return capture != null || search != null && search.isFocused();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        InterfaceSettings theme = RebuildClient.settings();
        graphics.fill(left, top, right, bottom, 0xFF000000 | theme.panel);
        graphics.fill(left, top, right, top + 2, 0xFF000000 | theme.accent);
        graphics.fill(left + navWidth, top + 32, left + navWidth + 1, bottom - 6, 0xFF493751);
        graphics.text(font, getTitle(), left + 10, top + 12, TEXT);
        String heading = detail == null ? category == null ? "Modules" : category.title() : detail.title();
        graphics.text(font, font.plainSubstrByWidth(heading, bodyWidth), bodyX, top + 65, TEXT);
        if (detail == null ? results.isEmpty() : controls.isEmpty()) {
            graphics.text(font, query.isBlank() ? "No modules in this category yet." : "No matches.",
                    bodyX, top + 88, MUTED);
        } else if (detail != null && controls.size() <= pageRows) {
            graphics.text(font, font.plainSubstrByWidth(detail.description(), bodyWidth),
                    bodyX, bottom - 18, MUTED);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
