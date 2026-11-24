package fi.rotclient.rebuild;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class DashboardScreen extends Screen {
    enum Page {
        OVERVIEW("Overview"), MODULES("Modules"), HUD("HUD Elements Editor");
        final String title;
        Page(String title) { this.title = title; }
    }

    private static final int NAV_ITEM_HEIGHT = 38;
    private static final int NAV_HEIGHT = NAV_ITEM_HEIGHT + 8;
    private static final int CARD_HEIGHT = 112;
    private static final int CARD_GAP = 14;
    private static final int SETTING_HEIGHT = 29;
    private final Screen parent;
    private final List<DashboardTab> tabs = new ArrayList<>();
    private final RotClientScrollState sidebarScroll = new RotClientScrollState();
    private final RotClientScrollState listScroll = new RotClientScrollState();
    private final List<Target> targets = new ArrayList<>();
    private List<Suggestion> suggestions = List.of();
    private int suggestionIndex;
    private DashboardTab tab;
    private EditBox search;
    private EditBox settingsSearch;
    private ModuleCard expanded;
    private ModuleCard.Keybind capture;
    private String settingQuery = "";
    private boolean visualsOpen = true;
    private boolean modulesOpen = true;
    private boolean maximized;
    private int tabScroll;
    private int x, y, w, h;
    private float scale;

    DashboardScreen(Screen parent) {
        super(Component.literal(RebuildClient.editionName()));
        this.parent = parent;
        newTab();
    }

    @Override
    protected void init() {
        RotClientTheme.apply(RebuildClient.settings());
        scale = RotClientDashboardLayout.uiScale(width, height);
        int logicalW = (int) (width / scale);
        int logicalH = (int) (height / scale);
        var panel = RotClientDashboardLayout.resolve(logicalW, logicalH);
        w = maximized ? logicalW - 8 : panel.width();
        h = maximized ? logicalH - 8 : panel.height();
        x = (logicalW - w) / 2;
        y = (logicalH - h) / 2;
        search = field("Search modules and HUD…", tab.search, left(), y + 34, right() - left(), text -> {
            tab.search = text;
            suggestionIndex = 0;
            listScroll.reset();
        });
        settingsSearch = expanded == null ? null : field("Search settings…", settingQuery,
                left(), top() + 34, right() - left(), text -> {
                    settingQuery = text;
                    listScroll.reset();
                });
    }

    private EditBox field(String hint, String value, int px, int py, int width,
                          java.util.function.Consumer<String> response) {
        EditBox field = new EditBox(font, px + 8, py + 3, Math.max(1, width - 16), 18, Component.literal(hint));
        field.setBordered(false);
        field.setTextColor(RotClientTheme.TEXT);
        field.setMaxLength(100);
        field.setHint(RotClientFonts.component(hint));
        field.setValue(value);
        field.setResponder(response);
        return addRenderableWidget(field);
    }

    private int left() { return RotClientDashboardLayout.contentLeft(x); }
    private int right() { return RotClientDashboardLayout.contentRight(x, w); }
    private int top() { return RotClientDashboardLayout.contentTop(y) + 12; }
    private int bottom() { return y + h - 12; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        RotClientUiClock.beginFrame(minecraft);
        int mx = Math.round(mouseX / scale), my = Math.round(mouseY / scale);
        targets.clear();
        g.pose().pushMatrix();
        g.pose().scale(scale, scale);
        try {
            RotClientUiDraw.beginHoverFrame(mx, my);
            drawShell(g, mx, my);
            drawSidebar(g, mx, my);
            switch (tab.page) {
                case OVERVIEW -> drawOverview(g, mx, my);
                case MODULES -> drawModules(g, mx, my);
                case HUD -> drawHudLanding(g, mx, my);
            }
            super.extractRenderState(g, mx, my, delta);
            drawSuggestions(g, mx, my);
            RotClientUiDraw.drawHoverFrame(g, font, x + w, y + h);
        } finally {
            g.pose().popMatrix();
        }
    }

    private void drawShell(GuiGraphicsExtractor g, int mx, int my) {
        RotClientUiDraw.drawShadowedPanel(g, x, y, w, h);
        var layout = RotClientTabStrip.compute(x, y, w, tabs.size());
        RotClientUiDraw.noteHoverTip(RotClientTabStrip.draw(g, font, layout, tabs, tab.id, tabScroll, mx, my, null, maximized));
        RotClientUiDraw.text(g, font, "ROT", x + 12, y + 35, RotClientTheme.HUD_ACCENT, false);
        RotClientUiDraw.text(g, font, " CLIENT", x + 12 + RotClientUiDraw.textWidth(font, "ROT"), y + 35, RotClientTheme.TEXT, false);
        RotClientUiDraw.text(g, font, "made by Rot Tools", x + 12, y + 46, RotClientTheme.TEXT_MUTED, false);
        RotClientUiDraw.drawBackButton(g, font, mx, my, left() - 27, y + 33);
        target(left() - 27, y + 33, 22, 22, this::goBack);
        drawSearchField(g, left(), y + 33, right() - left(), search);
        int side = RotClientDashboardLayout.SIDEBAR_WIDTH;
        int chrome = RotClientDashboardLayout.contentTop(y);
        g.fill(x + 1, chrome, x + side, y + h - 1, RotClientTheme.DASHBOARD_SIDEBAR);
        g.fill(x + side, chrome, x + side + 1, y + h - 1, RotClientTheme.DIVIDER);
        RotClientUiDraw.roundedFill(g, x + side + 1, chrome, x + w - 1, y + h - 1,
                RotClientUiDraw.withAlpha(RotClientTheme.SURFACE, 0x55), RotClientUiDraw.RADIUS_SM);
    }

    private static void drawSearchField(GuiGraphicsExtractor g, int x, int y, int w, EditBox box) {
        RotClientUiDraw.roundedFill(g, x, y, x + w, y + 22,
                box.isFocused() ? RotClientTheme.FIELD_ACTIVE : RotClientTheme.FIELD);
        RotClientUiDraw.roundedOutline(g, x, y, x + w, y + 22,
                box.isFocused() ? RotClientTheme.HUD_ACCENT : RotClientTheme.BORDER);
    }

    private void drawSuggestions(GuiGraphicsExtractor g, int mx, int my) {
        suggestions = search.isFocused() ? findSuggestions(tab.search) : List.of();
        if (suggestions.isEmpty()) return;
        int px = left(), py = y + 57, width = right() - px;
        RotClientUiDraw.drawElevatedCard(g, px, py, width, 8 + suggestions.size() * 20);
        for (int i = 0; i < suggestions.size(); i++) {
            Suggestion item = suggestions.get(i);
            int row = py + 4 + i * 20;
            if (i == suggestionIndex || RotClientUiDraw.inside(mx, my, px + 4, row, width - 8, 20)) {
                RotClientUiDraw.roundedFill(g, px + 4, row, px + width - 4, row + 20, RotClientTheme.HOVER_ROW);
            }
            RotClientUiDraw.text(g, font, item.layer != null ? "HUD" : "Module", px + 10, row + 5, RotClientTheme.TEXT_DIM, false);
            RotClientUiDraw.text(g, font, item.title, px + 65, row + 5, RotClientTheme.TEXT, false);
            if (item.layer != null) {
                RotClientUiDraw.drawPremiumButton(g, font, mx, my, px + width - 90, row + 1, 76, 18,
                        "Settings", false, true);
            }
        }
    }

    private static List<Suggestion> findSuggestions(String value) {
        String query = value.strip().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) return List.of();
        List<Suggestion> matches = new ArrayList<>();
        for (ModuleCard module : RebuildClient.modules()) {
            if (matchesWords(module.title() + " " + module.description(), query)) {
                matches.add(new Suggestion(module.title(), module, null));
            }
        }
        for (var layer : HudLayerSettings.LAYERS) {
            if (matchesWords("hide " + layer.title() + " hud settings position scale default", query)) {
                matches.add(new Suggestion(layer.title(), null, layer));
            }
        }
        return matches.stream()
                .sorted(Comparator.comparing(suggestion -> !suggestion.title.toLowerCase(Locale.ROOT).startsWith(query)))
                .limit(7).toList();
    }

    private static boolean matchesWords(String value, String query) {
        String candidate = value.toLowerCase(Locale.ROOT);
        return java.util.Arrays.stream(query.split("\\s+")).allMatch(candidate::contains);
    }

    private void chooseSuggestion(Suggestion item, boolean openSettings) {
        if (item.module != null) {
            navigate(Page.MODULES, null);
            expand(item.module);
        } else {
            tab.page = Page.HUD;
            tab.category = null;
            tab.search = item.layer.title();
            resetPage();
            if (openSettings) minecraft.gui.setScreen(new HudEditorScreen(this, item.layer));
        }
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mx, int my) {
        int start = RotClientDashboardLayout.contentTop(y);
        int viewport = y + h - 8 - start;
        int total = 12 + NAV_HEIGHT + 18 * 2 + (visualsOpen ? NAV_HEIGHT * 2 : 0)
                + (modulesOpen ? NAV_HEIGHT * (1 + Category.values().length) : 0);
        sidebarScroll.setBounds(total, viewport);
        sidebarScroll.advanceSeconds(RotClientUiClock.seconds());
        int px = x + 10;
        int py = start + 8 - sidebarScroll.scrollPixels();
        int navWidth = RotClientDashboardLayout.SIDEBAR_WIDTH - 20;
        g.enableScissor(x + 1, start, x + RotClientDashboardLayout.SIDEBAR_WIDTH - 1, y + h - 8);
        try {
            nav(g, mx, my, px, py, navWidth, "Overview", "Status at a glance", tab.page == Page.OVERVIEW,
                    () -> navigate(Page.OVERVIEW, null));
            py += NAV_HEIGHT;
            section(g, mx, my, px, py, navWidth, "Visuals", visualsOpen, () -> visualsOpen = !visualsOpen);
            py += 18;
            if (visualsOpen) {
                nav(g, mx, my, px, py, navWidth, "Appearance", "Theme & colors", false,
                        () -> minecraft.gui.setScreen(new AppearanceScreen(this)));
                py += NAV_HEIGHT;
                nav(g, mx, my, px, py, navWidth, "HUD Elements Editor", "Move & align overlays", tab.page == Page.HUD,
                        () -> navigate(Page.HUD, null));
                py += NAV_HEIGHT;
            }
            section(g, mx, my, px, py, navWidth, "Modules", modulesOpen, () -> modulesOpen = !modulesOpen);
            py += 18;
            if (modulesOpen) {
                nav(g, mx, my, px, py, navWidth, "All modules", "Browse modules", tab.page == Page.MODULES && tab.category == null,
                        () -> navigate(Page.MODULES, null));
                for (Category category : Category.values()) {
                    py += NAV_HEIGHT;
                    nav(g, mx, my, px, py, navWidth, category.title(), null,
                            tab.page == Page.MODULES && tab.category == category, () -> navigate(Page.MODULES, category));
                }
            }
        } finally {
            g.disableScissor();
        }
        if (sidebarScroll.canScroll()) {
            RotClientUiDraw.drawScrollbar(g, x + RotClientDashboardLayout.SIDEBAR_WIDTH - 7,
                    start, y + h - 8, total, sidebarScroll.scrollPixels());
        }
    }

    private void nav(GuiGraphicsExtractor g, int mx, int my, int px, int py, int width,
                     String label, String subtitle, boolean selected, Runnable click) {
        RotClientUiDraw.drawNavItem(g, font, mx, my, px, py, width, NAV_ITEM_HEIGHT, label, subtitle, selected, false);
        clippedTarget(px, py, width, NAV_ITEM_HEIGHT, RotClientDashboardLayout.contentTop(y), y + h - 8, click);
    }

    private void section(GuiGraphicsExtractor g, int mx, int my, int px, int py, int width,
                         String label, boolean open, Runnable click) {
        RotClientUiDraw.drawSectionLabel(g, font, label, px + 2, py + 3);
        RotClientUiDraw.drawChevron(g, px + width - 12, py + 6, open ? 1 : 0, RotClientTheme.TEXT_MUTED);
        clippedTarget(px, py, width, 18, RotClientDashboardLayout.contentTop(y), y + h - 8, click);
    }

    private void drawOverview(GuiGraphicsExtractor g, int mx, int my) {
        RotClientUiDraw.pageTitle(g, font, "Overview", left(), top());
        RotClientUiDraw.helpText(g, font, RebuildClient.editionName(), left(), top() + 18);
        action(g, mx, my, left(), top() + 44, 156, 28, "Browse modules", true, () -> navigate(Page.MODULES, null));
        action(g, mx, my, left() + 164, top() + 44, 172, 28, "HUD Elements Editor", false, () -> navigate(Page.HUD, null));
    }

    private void drawModules(GuiGraphicsExtractor g, int mx, int my) {
        RotClientUiDraw.pageTitle(g, font, tab.title(), left(), top());
        List<ModuleCard> modules = modules();
        RotClientUiDraw.helpText(g, font, modules.size() + " modules", left(), top() + 17);
        int listTop = top() + 34;
        if (settingsSearch != null && settingsSearch.visible) {
            drawSearchField(g, left(), listTop, right() - left(), settingsSearch);
            listTop += 29;
        }
        if (modules.isEmpty()) {
            RotClientUiDraw.helpText(g, font, tab.search.isBlank() ? "No modules in this category." : "No matching modules.",
                    left(), listTop + 8);
            return;
        }
        int gap = CARD_GAP;
        int columns = right() - left() >= 530 && expanded == null ? 2 : 1;
        int cardW = (right() - left() - 12 - (columns - 1) * gap) / columns;
        List<ModuleCard.Control> settings = expanded == null ? List.of() : filteredSettings();
        int total = ((modules.size() + columns - 1) / columns) * (CARD_HEIGHT + gap);
        if (modules.contains(expanded)) total += settings.size() * SETTING_HEIGHT;
        listScroll.setBounds(total, bottom() - listTop);
        listScroll.advanceSeconds(RotClientUiClock.seconds());
        g.enableScissor(left(), listTop, right(), bottom());
        try {
            int py = listTop - listScroll.scrollPixels();
            for (int index = 0; index < modules.size(); index++) {
                ModuleCard module = modules.get(index);
                int px = left() + index % columns * (cardW + gap);
                drawModule(g, mx, my, px, py, cardW, module, listTop);
                if (module == expanded) {
                    for (int i = 0; i < settings.size(); i++) {
                        drawSetting(g, mx, my, px + 10, py + CARD_HEIGHT + i * SETTING_HEIGHT,
                                cardW - 20, settings.get(i), listTop);
                    }
                    py += settings.size() * SETTING_HEIGHT;
                }
                if (index % columns == columns - 1) py += CARD_HEIGHT + gap;
            }
        } finally {
            g.disableScissor();
        }
        scrollbar(g, listTop, total);
    }

    private void drawModule(GuiGraphicsExtractor g, int mx, int my, int px, int py, int width,
                            ModuleCard module, int clipTop) {
        if (py + CARD_HEIGHT < clipTop || py >= bottom()) return;
        boolean selected = expanded == module;
        boolean hover = RotClientUiDraw.inside(mx, my, px, py, width, CARD_HEIGHT);
        ModuleCard.Toggle toggle = module.controls().isEmpty() || !(module.controls().getFirst() instanceof ModuleCard.Toggle first)
                ? null : first;
        boolean on = toggle != null && toggle.read().getAsBoolean();
        int accent = selected ? RotClientTheme.VIOLET : RotClientTheme.HUD_ACCENT;
        RotClientUiDraw.drawInteractiveSurface(g, px, py, width, CARD_HEIGHT, hover ? 1 : 0, selected,
                accent, RotClientUiDraw.RADIUS_SM);
        g.fill(px + 1, py + 10, px + 4, py + CARD_HEIGHT - 10, on || selected ? accent : RotClientTheme.DIVIDER);
        RotClientUiDraw.text(g, font, module.title(), px + 14, py + 12, RotClientTheme.TEXT, true);
        var lines = RotClientUiDraw.wrapTooltip(font, module.description(), width - 28);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            RotClientUiDraw.helpText(g, font, lines.get(i), px + 14, py + 31 + i * 13);
        }
        RotClientUiDraw.text(g, font, module.controls().size() + " settings", px + 14, py + 59, RotClientTheme.TEXT_MUTED, false);
        g.fill(px + 12, py + 76, px + width - 12, py + 77, RotClientTheme.DIVIDER);
        if (toggle != null) {
            RotClientUiDraw.drawAnimatedToggle(g, px + 14, py + 83, on,
                    RotClientUiDraw.inside(mx, my, px + 14, py + 83, 44, 20), "module:" + module.id(), accent);
            RotClientUiDraw.text(g, font, on ? "ON" : "OFF", px + 64, py + 86,
                    on ? accent : RotClientTheme.TEXT_MUTED, false);
            clippedTarget(px + 14, py + 83, 78, 20, clipTop, bottom(), () -> toggle.write().accept(!toggle.read().getAsBoolean()));
        }
        int bx = px + width - 102;
        RotClientUiDraw.drawPremiumButton(g, font, mx, my, bx, py + 82, 88, 24,
                selected ? "Close settings" : "Settings", selected, true);
        clippedTarget(bx, py + 82, 88, 24, clipTop, bottom(), () -> expand(module));
        clippedTarget(px + 5, py + 5, width - 10, 70, clipTop, bottom(), () -> expand(module));
    }

    private void drawSetting(GuiGraphicsExtractor g, int mx, int my, int px, int py, int w,
                             ModuleCard.Control control, int clipTop) {
        if (py + SETTING_HEIGHT < clipTop || py >= bottom()) return;
        RotClientUiDraw.roundedFill(g, px, py + 2, px + w, py + SETTING_HEIGHT - 2, RotClientTheme.SURFACE_ALT);
        RotClientUiDraw.text(g, font, control.label(), px + 9, py + 8, RotClientTheme.TEXT, false);
        switch (control) {
            case ModuleCard.Toggle toggle -> {
                boolean on = toggle.read().getAsBoolean();
                RotClientUiDraw.drawAnimatedToggle(g, px + w - 56, py + 4, on,
                        RotClientUiDraw.inside(mx, my, px, py, w, SETTING_HEIGHT), "setting:" + control.label(), RotClientTheme.HUD_ACCENT);
                clippedTarget(px, py, w, SETTING_HEIGHT, clipTop, bottom(), () -> toggle.write().accept(!toggle.read().getAsBoolean()));
            }
            case ModuleCard.Action action -> {
                RotClientUiDraw.drawPremiumButton(g, font, mx, my, px + w - 100, py + 3, 88, 22, "Open", false, true);
                clippedTarget(px, py, w, SETTING_HEIGHT, clipTop, bottom(), () -> action.open().accept(this));
            }
            case ModuleCard.Keybind keybind -> {
                int key = keybind.read().getAsInt();
                String label = capture == keybind ? "Press a key…" : key < 0 ? "Unbound"
                        : InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
                RotClientUiDraw.drawPremiumButton(g, font, mx, my, px + w - 100, py + 3, 88, 22, label, false, true);
                clippedTarget(px, py, w, SETTING_HEIGHT, clipTop, bottom(), () -> capture = keybind);
            }
        }
    }

    private void drawHudLanding(GuiGraphicsExtractor g, int mx, int my) {
        int l = left(), t = top();
        RotClientUiDraw.text(g, font, "← Back", l, t + 4,
                RotClientUiDraw.inside(mx, my, l, t, 72, 22) ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED, false);
        target(l, t, 72, 22, this::goBack);
        RotClientUiDraw.pageTitle(g, font, "HUD Elements Editor", l + 80, t + 1);
        RotClientUiDraw.helpText(g, font, "Turn overlays on or off, open a HUD's settings, or edit placement.", l + 80, t + 16);
        action(g, mx, my, l, t + 28, 168, 28, "Open layout editor", true,
                () -> minecraft.gui.setScreen(new HudEditorScreen(this)));
        String filter = tab.search.strip().toLowerCase(Locale.ROOT);
        var layers = HudLayerSettings.LAYERS.stream()
                .filter(layer -> matchesWords("hide " + layer.title() + " hud settings position scale default", filter))
                .toList();
        long hidden = layers.stream().filter(HudLayerSettings::hidden).count();
        RotClientUiDraw.helpText(g, font, hidden + " of " + layers.size() + " elements hidden", l + 180, t + 37);
        int clip = t + 56;
        int total = 12 + 18 + layers.size() * 54 + 8;
        int rowWidth = right() - l - RotClientUiDraw.SCROLLBAR_HIT_WIDTH - 4;
        listScroll.setBounds(total, bottom() - clip);
        listScroll.advanceSeconds(RotClientUiClock.seconds());
        g.enableScissor(l, clip, right(), bottom());
        try {
            int py = clip + 12 - listScroll.scrollPixels();
            RotClientUiDraw.text(g, font, "Vanilla / Hypixel  ·  " + layers.size(), l + 2, py + 2, RotClientTheme.TEXT_DIM, true);
            py += 18;
            for (var layer : layers) {
                boolean on = HudLayerSettings.hidden(layer);
                boolean hover = RotClientUiDraw.inside(mx, my, l, py, rowWidth, 48);
                RotClientUiDraw.drawInteractiveSurface(g, l, py, rowWidth, 48, hover ? 1 : 0, false,
                        on ? RotClientTheme.HUD_ACCENT : RotClientTheme.BORDER_BRIGHT, RotClientUiDraw.RADIUS_SM);
                if (on) g.fill(l + 1, py + 8, l + 4, py + 40, RotClientTheme.HUD_ACCENT);
                RotClientUiDraw.text(g, font, "Hide " + layer.title(), l + 12, py + 8,
                        on ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM, true);
                String description = HudLayerSettings.STATUS_EFFECTS.equals(layer.id())
                        ? "Hide effects on the HUD and in the inventory." : "Hide this overlay when the switch is on.";
                RotClientUiDraw.text(g, font, description, l + 12, py + 26, RotClientTheme.TEXT_MUTED, false);
                RotClientUiDraw.drawAnimatedToggle(g, l + rowWidth - 56, py + 16, on, hover,
                        "hud-layout-toggle:" + layer.id(), RotClientTheme.HUD_ACCENT);
                RotClientUiDraw.drawPremiumButton(g, font, mx, my, l + rowWidth - 152, py + 12, 88, 24,
                        "Settings", false, true);
                clippedTarget(l + rowWidth - 152, py + 12, 88, 24, clip, bottom(),
                        () -> minecraft.gui.setScreen(new HudEditorScreen(this, layer)));
                clippedTarget(l, py, rowWidth, 48, clip, bottom(), () -> HudLayerSettings.toggleHidden(layer));
                py += 54;
            }
        } finally {
            g.disableScissor();
        }
        scrollbar(g, clip, total);
    }

    private void scrollbar(GuiGraphicsExtractor g, int top, int total) {
        if (listScroll.canScroll()) {
            RotClientUiDraw.drawScrollbar(g, right() - RotClientUiDraw.SCROLLBAR_WIDTH - 2, top, bottom(),
                    total, listScroll.scrollPixels());
        }
    }

    private List<ModuleCard> modules() {
        String filter = tab.search.strip().toLowerCase(Locale.ROOT);
        return RebuildClient.modules().stream()
                .filter(card -> tab.category == null || card.category() == tab.category)
                .filter(card -> card.title().toLowerCase(Locale.ROOT).contains(filter)
                        || card.description().toLowerCase(Locale.ROOT).contains(filter))
                .toList();
    }

    private List<ModuleCard.Control> filteredSettings() {
        String filter = settingQuery.strip().toLowerCase(Locale.ROOT);
        return expanded.controls().stream().filter(control -> control.label().toLowerCase(Locale.ROOT).contains(filter)).toList();
    }

    private void expand(ModuleCard module) {
        expanded = expanded == module ? null : module;
        capture = null;
        settingQuery = "";
        int index = expanded == null ? 0 : modules().indexOf(expanded);
        rebuildWidgets();
        listScroll.setBounds(modules().size() * (CARD_HEIGHT + CARD_GAP) + (expanded == null ? 0 : expanded.controls().size() * SETTING_HEIGHT),
                bottom() - top() - 64);
        listScroll.setScrollPixels(Math.max(0, index) * (CARD_HEIGHT + CARD_GAP));
    }

    private void action(GuiGraphicsExtractor g, int mx, int my, int px, int py, int w, int h,
                        String title, boolean primary, Runnable click) {
        RotClientUiDraw.drawPremiumButton(g, font, mx, my, px, py, w, h, title, primary, true);
        target(px, py, w, h, click);
    }

    private void newTab() {
        tab = new DashboardTab(java.util.UUID.randomUUID().toString());
        tabs.add(tab);
    }

    private void navigate(Page page, Category category) {
        tab.page = page;
        tab.category = category;
        tab.search = "";
        resetPage();
    }

    private void resetPage() {
        expanded = null;
        capture = null;
        settingQuery = "";
        listScroll.reset();
        rebuildWidgets();
    }

    private void goBack() {
        if (expanded != null) expand(expanded);
        else if (tab.page != Page.OVERVIEW) navigate(Page.OVERVIEW, null);
        else onClose();
    }

    private void target(int x, int y, int w, int h, Runnable action) {
        if (h > 0) targets.add(new Target(x, y, w, h, action));
    }

    private void clippedTarget(int x, int y, int w, int h, int top, int bottom, Runnable action) {
        int clipped = Math.max(y, top);
        target(x, clipped, w, Math.min(y + h, bottom) - clipped, action);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) (event.x() / scale), my = (int) (event.y() / scale);
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        if (search.isFocused() && !suggestions.isEmpty()
                && RotClientUiDraw.inside(mx, my, left(), y + 57, right() - left(), 8 + suggestions.size() * 20)) {
            int row = (my - y - 61) / 20;
            if (my >= y + 61 && row < suggestions.size()) chooseSuggestion(suggestions.get(row), mx >= right() - 90);
            return true;
        }
        var layout = RotClientTabStrip.compute(x, y, w, tabs.size());
        if (RotClientTabStrip.hitWindowClose(layout, mx, my)) { onClose(); return true; }
        if (RotClientTabStrip.hitMaximize(layout, mx, my)) { maximized = !maximized; rebuildWidgets(); return true; }
        if (RotClientTabStrip.hitNewTab(layout, mx, my)) {
            if (tabs.size() < 12) {
                newTab();
                tabScroll = RotClientTabStrip.compute(x, y, w, tabs.size()).maxScroll();
                resetPage();
            }
            return true;
        }
        int index = RotClientTabStrip.hitTabIndex(layout, tabs.size(), tabScroll, mx, my);
        if (index >= 0) {
            if (RotClientTabStrip.hitClose(layout, index, tabScroll, mx, my)) {
                if (tabs.size() == 1) { onClose(); return true; }
                if (tabs.remove(index) == tab) tab = tabs.get(Math.min(index, tabs.size() - 1));
                tabScroll = Math.min(tabScroll, RotClientTabStrip.compute(x, y, w, tabs.size()).maxScroll());
            } else tab = tabs.get(index);
            resetPage();
            return true;
        }
        if (super.mouseClicked(logicalEvent(event), doubleClick)) return true;
        for (Target target : targets) {
            if (RotClientUiDraw.inside(mx, my, target.x, target.y, target.w, target.h)) {
                setFocused(null);
                target.action.run();
                return true;
            }
        }
        setFocused(null);
        return false;
    }

    private MouseButtonEvent logicalEvent(MouseButtonEvent event) {
        return new MouseButtonEvent(event.x() / scale, event.y() / scale, event.buttonInfo());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return super.mouseDragged(logicalEvent(event), dx / scale, dy / scale);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(logicalEvent(event));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        int mx = (int) (mouseX / scale), my = (int) (mouseY / scale);
        if (vertical == 0 || !RotClientUiDraw.inside(mx, my, x, y, w, h)) return false;
        if (my < y + RotClientTabStrip.HEIGHT) {
            var layout = RotClientTabStrip.compute(x, y, w, tabs.size());
            tabScroll = Math.clamp(tabScroll - (int) (vertical * 48), 0, layout.maxScroll());
        } else if (mx < left()) sidebarScroll.scrollBySteps(vertical, 36);
        else listScroll.scrollBySteps(vertical, 48);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capture != null) {
            if (event.key() != GLFW.GLFW_KEY_ESCAPE && event.key() != GLFW.GLFW_KEY_UNKNOWN) {
                capture.write().accept(event.key() == GLFW.GLFW_KEY_DELETE || event.key() == GLFW.GLFW_KEY_BACKSPACE ? -1 : event.key());
            }
            capture = null;
            return true;
        }
        if (search.isFocused() && !suggestions.isEmpty()) {
            if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
                suggestionIndex = Math.floorMod(suggestionIndex + (event.key() == GLFW.GLFW_KEY_UP ? -1 : 1), suggestions.size());
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
                chooseSuggestion(suggestions.get(Math.min(suggestionIndex, suggestions.size() - 1)), false);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                setFocused(null);
                return true;
            }
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && (expanded != null || tab.page != Page.OVERVIEW)) {
            goBack();
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean isEditing() {
        return capture != null || search != null && search.isFocused() || settingsSearch != null && settingsSearch.isFocused();
    }

    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }

    @Override
    public boolean isPauseScreen() { return false; }

    private record Target(int x, int y, int w, int h, Runnable action) {}
    private record Suggestion(String title, ModuleCard module, HudLayerSettings.Layer layer) {}
}
