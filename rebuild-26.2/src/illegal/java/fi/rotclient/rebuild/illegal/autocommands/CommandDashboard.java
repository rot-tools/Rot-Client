// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import fi.rotclient.rebuild.RotChrome;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class CommandDashboard extends Screen {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath("rotclient_rebuild", "icon.png");

    private final PopupConfig config;
    private final Screen parent;
    private final List<Button> rowButtons = new ArrayList<>();
    private List<WarpCatalog.Destination> matches = List.of();
    private String query = "";
    private boolean browsing;
    private int page;
    private int rows;
    private int left;
    private int top;
    private int panelWidth;
    private int bottom;
    private Button previous;
    private Button next;

    public CommandDashboard(PopupConfig config, Screen parent) {
        super(Component.literal("AutoCommands"));
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(408, width - 16);
        left = (width - panelWidth) / 2;
        rows = Math.clamp((height - 104) / 29, 1, 8);
        int panelHeight = rows * 29 + 96;
        top = (height - panelHeight) / 2;
        bottom = top + panelHeight;
        int x = left + 12;
        int usable = panelWidth - 24;
        int quarter = (usable - 12) / 4;

        if (browsing) {
            var search = RotChrome.field(font, x, top + 37, usable - 58, 20, "Search warps");
            search.setHint(Component.literal("Search destinations or commands"));
            search.setMaxLength(64);
            search.setValue(query);
            search.setResponder(value -> {
                query = value;
                page = 0;
                rebuildRows();
            });
            addRenderableWidget(search);
            addButton("Back", x + usable - 52, top + 37, 52, button -> showBindings());
        } else {
            addButton("Add command", x, top + 37, quarter + 10, button -> edit(-1, ""));
            addButton("Browse warps", x + quarter + 14, top + 37, quarter + 10, button -> {
                browsing = true;
                page = 0;
                rebuildWidgets();
            });
            addButton("Popup style", x + 2 * quarter + 28, top + 37, usable - 2 * quarter - 28,
                    button -> minecraft.gui.setScreen(new PopupConfigScreen(config, this)));
        }

        previous = addButton("<", x, bottom - 28, 24, button -> changePage(-1));
        next = addButton(">", x + 28, bottom - 28, 24, button -> changePage(1));
        addButton("Done", x + usable - 52, bottom - 28, 52, button -> onClose());
        rowButtons.clear();
        rebuildRows();
    }

    private Button addButton(String text, int x, int y, int buttonWidth, Consumer<Button> action) {
        return addRenderableWidget(RotChrome.button(font, Component.literal(text), x, y, buttonWidth, 20,
                "Done".equals(text) || "Add command".equals(text), action::accept));
    }

    private void rebuildRows() {
        rowButtons.forEach(this::removeWidget);
        rowButtons.clear();
        if (browsing) {
            String needle = query.strip().toLowerCase(Locale.ROOT);
            matches = WarpCatalog.DESTINATIONS.stream().filter(warp ->
                    warp.name().toLowerCase(Locale.ROOT).contains(needle) || warp.command().contains(needle)
                            || warp.aliases().stream().anyMatch(alias -> alias.contains(needle)
                            || ("/warp " + alias).contains(needle))).toList();
        }
        page = Math.clamp(page, 0, pageCount() - 1);
        previous.active = page > 0;
        next.active = page + 1 < pageCount();
        for (int row = 0; row < rows && page * rows + row < entryCount(); row++) {
            int index = page * rows + row;
            int x = left + panelWidth - 58;
            int y = top + 64 + row * 29;
            rowButtons.add(addButton(browsing ? "Select" : "Edit", x, y, 46, button -> {
                if (browsing) edit(-1, matches.get(index).command());
                else edit(index, "");
            }));
        }
    }

    void showBindings() {
        browsing = false;
        query = "";
        page = 0;
        rebuildWidgets();
    }

    private void edit(int index, String preset) {
        minecraft.gui.setScreen(new ShortcutEditor(this, config, index, preset));
    }

    private int entryCount() { return browsing ? matches.size() : config.shortcuts.size(); }

    private int pageCount() { return Math.max(1, (entryCount() + rows - 1) / rows); }

    private void changePage(int step) {
        int updated = Math.clamp(page + step, 0, pageCount() - 1);
        if (updated != page) {
            page = updated;
            rebuildRows();
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy == 0 || y < top + 60 || y >= bottom - 31) return false;
        changePage(dy > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        RotChrome.panel(graphics, left, top, panelWidth, bottom - top);
        graphics.fill(left + 12, top + 1, left + 78, top + 3, RotChrome.accent());
        graphics.blit(RenderPipelines.GUI_TEXTURED, ICON, left + 12, top + 8, 0, 0, 23, 23, 128, 128, 128, 128);
        RotChrome.text(graphics, font, title.getString(), left + 42, top + 9, panelWidth - 54, RotChrome.TEXT);
        RotChrome.text(graphics, font, browsing ? "Warp destinations" : "Command shortcuts",
                left + 42, top + 22, panelWidth - 54, RotChrome.MUTED);

        if (entryCount() == 0) {
            RotChrome.centered(graphics, font, browsing ? "No destinations found." : "Add a command to create a shortcut.",
                    width / 2, top + 71, RotChrome.MUTED);
        }
        for (int row = 0; row < rows && page * rows + row < entryCount(); row++) {
            int index = page * rows + row;
            int y = top + 61 + row * 29;
            int x = left + 17;
            RotChrome.row(graphics, left + 12, y, panelWidth - 24, 27, mouseY >= y && mouseY < y + 27
                    && mouseX >= left + 12 && mouseX < left + panelWidth - 12);
            if (browsing) {
                var warp = matches.get(index);
                RotChrome.text(graphics, font, warp.name(), x, y + 3, panelWidth - 83, RotChrome.TEXT);
                RotChrome.text(graphics, font, warp.command(), x, y + 14, panelWidth - 83, RotChrome.MUTED);
            } else {
                var shortcut = config.shortcuts.get(index);
                String key = ShortcutEditor.keyLabel(shortcut.key());
                int keyWidth = Math.min(76, font.width(key));
                int available = panelWidth - 95 - keyWidth;
                RotChrome.text(graphics, font, shortcut.notice().title(), x, y + 3, available, RotChrome.TEXT);
                RotChrome.text(graphics, font, shortcut.command(), x, y + 14, available, RotChrome.MUTED);
                RotChrome.text(graphics, font, key, left + panelWidth - 66 - keyWidth, y + 8, keyWidth,
                        shortcut.key().isEmpty() ? RotChrome.MUTED : RotChrome.accent());
            }
        }
        RotChrome.centered(graphics, font, (page + 1) + " / " + pageCount(), width / 2, bottom - 22, RotChrome.MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
