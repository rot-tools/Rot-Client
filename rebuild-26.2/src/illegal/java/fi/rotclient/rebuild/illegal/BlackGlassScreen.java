package fi.rotclient.rebuild.illegal;

import fi.rotclient.rebuild.RotChrome;
import java.util.EnumSet;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class BlackGlassScreen extends Screen {
    private static final BlackGlassSettings.Color[] COLORS = BlackGlassSettings.Color.values();
    private static final int ROW_HEIGHT = 21;
    private final Screen parent;
    private final IllegalSettings config;
    private final EnumSet<BlackGlassSettings.Color> selected = EnumSet.noneOf(BlackGlassSettings.Color.class);
    private boolean enabled;
    private int page;
    private int rows;
    private int left, top, panelWidth, panelHeight;
    private String status = "";

    BlackGlassScreen(Screen parent, IllegalSettings config) {
        super(Component.literal("MSC Black Glass"));
        this.parent = parent;
        this.config = config;
        enabled = config.blackGlass.enabled();
        selected.addAll(config.blackGlass.colors());
    }

    @Override protected void init() {
        panelWidth = Math.min(452, width - 16);
        rows = Math.clamp((height - 113) / ROW_HEIGHT, 1, 9);
        panelHeight = 102 + rows * ROW_HEIGHT;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        int right = left + panelWidth - 12;
        button(enabled ? "On" : "Off", right - 54, top + 26, 54, false, pressed -> {
            enabled = !enabled;
            pressed.setMessage(state(enabled));
        });
        int pageSize = 2 * rows;
        page = Math.min(page, (COLORS.length - 1) / pageSize);
        int columnWidth = (panelWidth - 30) / 2;
        for (int i = 0; i < pageSize && i + page * pageSize < COLORS.length; i++) {
            var color = COLORS[page * pageSize + i];
            int x = left + 12 + i / rows * (columnWidth + 6);
            int y = top + 52 + i % rows * ROW_HEIGHT;
            button(color.title + "  ·  " + state(selected.contains(color)).getString(), x, y,
                    columnWidth, false, pressed -> {
                        if (!selected.add(color)) selected.remove(color);
                        pressed.setMessage(Component.literal(color.title + "  ·  " + state(selected.contains(color)).getString()));
                    });
        }
        int footer = top + panelHeight - 29;
        if (pageSize < COLORS.length) {
            button("‹", left + 12, footer, 23, false, ignored -> changePage(-1)).active = page > 0;
            button("›", left + 39, footer, 23, false, ignored -> changePage(1)).active =
                    (page + 1) * pageSize < COLORS.length;
        }
        button("Cancel", right - 130, footer, 62, false, ignored -> onClose());
        button("Apply", right - 62, footer, 62, true, ignored -> apply());
    }

    private static Component state(boolean active) {
        return Component.literal(active ? "On" : "Off");
    }

    private Button button(String title, int x, int y, int width, boolean primary, Button.OnPress handler) {
        return addRenderableWidget(RotChrome.button(font, Component.literal(title), x, y, width, 19, primary, handler));
    }

    private void changePage(int step) {
        page += step;
        rebuildWidgets();
    }

    private void apply() {
        if (BlackGlassPack.apply(minecraft, config, new BlackGlassSettings(enabled, selected))) {
            onClose();
        } else {
            status = "Could not load MSC-Black-Glass-26.2.zip.";
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        RotChrome.panel(graphics, left, top, panelWidth, panelHeight);
        graphics.fill(left + 12, top + 1, left + 77, top + 3, RotChrome.accent());
        RotChrome.text(graphics, font, "MSC Black Glass", left + 12, top + 9, panelWidth - 24, RotChrome.TEXT);
        RotChrome.text(graphics, font, "Enable Black Glass", left + 12, top + 32, panelWidth - 90, RotChrome.MUTED);
        String hint = status.isEmpty() ? "Selected colors affect blocks and their matching panes." : status;
        RotChrome.text(graphics, font, hint, left + 12, top + panelHeight - 42, panelWidth - 24, RotChrome.MUTED);
        super.extractRenderState(graphics, mx, my, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
