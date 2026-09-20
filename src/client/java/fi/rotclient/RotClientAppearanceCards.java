package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;

/**
 * The Appearance page of the dashboard: dropdown cards for Dashboard, Colors, Background, Charts
 * and Reset, with the controls inside the cards instead of on separate screens.
 *
 * <p>Changes apply and are saved immediately. Every card can undo back to how things were when the
 * page was opened, or reset to the defaults, so a bad colour is one click from being fixed.
 * What is on screen and where is decided by {@link AppearanceCards#layout}; this class only
 * paints that layout and reacts to clicks on it.
 */
final class RotClientAppearanceCards {
    private static final long FILE_LIST_TTL_MS = 1_000L;
    private static final Map<String, Field> FIELDS = new HashMap<>();

    private final Screen host;
    private final TrackerConfig trackerConfig;
    private final RotClientAppearanceConfig working;
    private final RotClientAppearanceConfig baseline;
    private final RotClientAppearanceConfig defaults = RotClientAppearanceConfig.defaults();
    private final RotClientExpandState accordion = new RotClientExpandState();
    private final Set<String> confirming = new LinkedHashSet<>();
    private final IntSupplier accent;
    private final IntSupplier radius;
    private final BiFunction<String, Boolean, Float> hoverAmount;

    private AppearanceCards.Layout layout = new AppearanceCards.Layout(List.of(), 0);
    private List<String> files = List.of();
    private long filesReadAtMs;
    private String status = "";

    RotClientAppearanceCards(
            Screen host,
            TrackerConfig trackerConfig,
            IntSupplier accent,
            IntSupplier radius,
            BiFunction<String, Boolean, Float> hoverAmount) {
        this.host = host;
        this.trackerConfig = trackerConfig;
        this.accent = accent;
        this.radius = radius;
        this.hoverAmount = hoverAmount;
        this.working = RotClientTheme.activeConfig().copy();
        this.working.normalize();
        this.baseline = working.copy();
    }

    String status() {
        return status;
    }

    /** Opens one card, e.g. from a search result, without closing the others. */
    void expand(String cardId) {
        AppearanceCards.Card card = AppearanceCards.card(cardId);
        if (card != null && !RotClientClient.workspace().isAppearanceSectionExpanded(card.expandId())) {
            RotClientClient.workspace().toggleAppearanceSection(card.expandId());
        }
    }

    /** Advances the open/close animation and rebuilds the layout that this frame draws and hits. */
    int update() {
        accordion.syncTargets(
                RotClientClient.workspace().expandedAppearanceSections(),
                AppearanceCards.allExpandIds());
        accordion.advanceSeconds(RotClientUiClock.seconds());
        AppearanceCards.Card background = AppearanceCards.card("background");
        if (background != null && accordion.visuallyOpen(background.expandId())) {
            refreshFilesIfStale(false);
        }
        layout = AppearanceCards.layout(new AppearanceCards.Input(
                accordion::amount, files, confirming));
        return layout.height();
    }

    void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int right,
            int top,
            int viewTop,
            int viewBottom,
            int mouseX,
            int mouseY) {
        int bodyLeft = left + AppearanceCards.BODY_INSET;
        for (AppearanceCards.Item item : layout.items()) {
            int y = top + item.y();
            if (y + item.height() < viewTop || y > viewBottom) {
                continue;
            }
            boolean clipped = item.clipped();
            if (clipped) {
                graphics.enableScissor(
                        left,
                        top + Math.max(item.clipTop(), -100_000),
                        right,
                        top + Math.min(item.clipBottom(), 100_000));
            }
            try {
                drawItem(graphics, font, item, left, bodyLeft, right, y, mouseX, mouseY);
            } finally {
                if (clipped) {
                    graphics.disableScissor();
                }
            }
        }
    }

    private void drawItem(
            GuiGraphicsExtractor graphics,
            Font font,
            AppearanceCards.Item item,
            int left,
            int bodyLeft,
            int right,
            int y,
            int mouseX,
            int mouseY) {
        switch (item.kind()) {
            case CARD -> drawCardHeader(graphics, font, item, left, right, y, mouseX, mouseY);
            case GROUP -> drawGroupHeader(graphics, font, item, bodyLeft, right, y, mouseX, mouseY);
            case COLOR -> drawColorRow(graphics, font, item, bodyLeft, right, y, mouseX, mouseY);
            case HELP -> RotClientUiDraw.text(graphics, font, item.label(),
                    bodyLeft + 8, y, RotClientTheme.TEXT_MUTED, false);
            case TEXT -> drawText(graphics, font, item, bodyLeft, y);
            case TOGGLE -> drawBackgroundToggle(graphics, font, item, bodyLeft, right, y, mouseX, mouseY);
            case FILE -> {
                boolean selected = item.id().equals(working.customBackgroundFile);
                RotClientUiDraw.drawCard(graphics, bodyLeft, y, right - bodyLeft, item.height(), selected);
                RotClientUiDraw.text(graphics, font, item.label(), bodyLeft + 8, y + 7,
                        selected ? RotClientTheme.TEXT : RotClientTheme.TEXT_DIM, selected);
            }
            case BUTTONS -> {
                for (AppearanceCards.Button button : item.buttons()) {
                    RotClientUiDraw.drawButton(graphics, font, mouseX, mouseY,
                            bodyLeft + button.x(), y, button.width(), button.label(),
                            button.accent(), true);
                }
            }
        }
    }

    private void drawCardHeader(
            GuiGraphicsExtractor graphics,
            Font font,
            AppearanceCards.Item item,
            int left,
            int right,
            int y,
            int mouseX,
            int mouseY) {
        AppearanceCards.Card card = AppearanceCards.card(item.id());
        double open = card == null ? 0.0D : accordion.amount(card.expandId());
        int width = right - left;
        boolean hover = RotClientUiDraw.inside(mouseX, mouseY, left, y, width, item.height());
        RotClientUiDraw.drawInteractiveSurface(
                graphics, left, y, width, item.height(),
                hoverAmount.apply("appearance-card:" + item.id(), hover),
                open > 0.5D,
                accent.getAsInt(),
                radius.getAsInt());
        graphics.fill(left + 1, y + 10, left + 4, y + item.height() - 10, accent.getAsInt());
        RotClientUiDraw.text(graphics, font, item.label(), left + 16, y + 10,
                RotClientTheme.TEXT, true);
        RotClientUiDraw.helpText(graphics, font,
                RotClientUiDraw.ellipsizeAndHover(
                        font, item.detail(), Math.max(20, width - 120),
                        left + 16, y + 26, 12),
                left + 16, y + 27);
        RotClientUiDraw.drawChevron(graphics, right - 26, y + 18, open,
                hover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED);
        if (card != null && changed(card)) {
            String tag = "EDITED";
            RotClientUiDraw.text(graphics, font, tag,
                    right - 36 - font.width(tag), y + 20, RotClientTheme.WARNING, false);
        }
    }

    private void drawGroupHeader(
            GuiGraphicsExtractor graphics,
            Font font,
            AppearanceCards.Item item,
            int bodyLeft,
            int right,
            int y,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(mouseX, mouseY, bodyLeft, y, right - bodyLeft, item.height());
        RotClientUiDraw.drawCard(graphics, bodyLeft, y, right - bodyLeft, item.height(), hover);
        RotClientUiDraw.drawChevron(graphics, bodyLeft + 8, y + 5, accordion.amount(item.id()),
                hover ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED);
        RotClientUiDraw.glyph(graphics, font, item.label(), bodyLeft + 24, y + 7,
                RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font, item.detail(),
                right - 10 - font.width(item.detail()), y + 7, RotClientTheme.TEXT_MUTED, false);
    }

    private void drawColorRow(
            GuiGraphicsExtractor graphics,
            Font font,
            AppearanceCards.Item item,
            int bodyLeft,
            int right,
            int y,
            int mouseX,
            int mouseY) {
        int color = colorOf(item.id());
        RotClientColorRowLayout.Columns columns = RotClientColorRowLayout.compute(bodyLeft, right, y);
        boolean hover = RotClientUiDraw.inside(
                mouseX, mouseY, bodyLeft, y, right - bodyLeft, RotClientColorRowLayout.ROW_HEIGHT);
        RotClientUiDraw.drawCard(graphics, bodyLeft, y, right - bodyLeft,
                RotClientColorRowLayout.ROW_HEIGHT, hover);
        RotClientUiDraw.text(graphics, font,
                RotClientUiDraw.ellipsizeAndHover(
                        font, item.label(), columns.labelMaxWidth(), columns.labelLeft(), y + 6, 12),
                columns.labelLeft(), y + 8, RotClientTheme.TEXT_DIM, false);
        RotClientUiDraw.text(graphics, font, RotClientAppearanceConfig.toHexRgb(color),
                columns.hexX(), y + 8, RotClientTheme.TEXT_MUTED, false);
        RotClientUiDraw.drawColorSwatch(graphics, columns.swatchX(), columns.controlTop(),
                RotClientColorRowLayout.SWATCH_WIDTH, RotClientColorRowLayout.SWATCH_HEIGHT,
                color, hover);
        RotClientUiDraw.drawRainbowSwatch(graphics, columns.pickerX(), columns.controlTop(),
                RotClientColorRowLayout.PICKER_SIZE);
    }

    private void drawText(
            GuiGraphicsExtractor graphics,
            Font font,
            AppearanceCards.Item item,
            int bodyLeft,
            int y) {
        String text = item.label();
        int color = RotClientTheme.TEXT_MUTED;
        switch (item.id()) {
            case AppearanceCards.BG_HINT -> text = RotClientBackgroundManager.backgroundsPathHint();
            case AppearanceCards.BG_EMPTY -> color = RotClientTheme.WARNING;
            case AppearanceCards.BG_STATS -> {
                text = "Opacity " + working.customBackgroundOpacity
                        + "  ·  Dim " + working.customBackgroundDim
                        + "  ·  Fit " + working.customBackgroundFit;
                color = RotClientTheme.TEXT_DIM;
            }
            case "reset.blurb1" -> color = RotClientTheme.TEXT_DIM;
            default -> {
                if (item.id().endsWith(".warning")) {
                    color = RotClientTheme.WARNING;
                }
            }
        }
        RotClientUiDraw.text(graphics, font, text, bodyLeft + 8, y, color, false);
    }

    private void drawBackgroundToggle(
            GuiGraphicsExtractor graphics,
            Font font,
            AppearanceCards.Item item,
            int bodyLeft,
            int right,
            int y,
            int mouseX,
            int mouseY) {
        boolean hover = RotClientUiDraw.inside(mouseX, mouseY, bodyLeft, y, right - bodyLeft, item.height());
        RotClientUiDraw.drawCard(graphics, bodyLeft, y, right - bodyLeft, item.height(), hover);
        RotClientUiDraw.text(graphics, font, item.label(), bodyLeft + 10, y + 6, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font,
                working.customBackgroundEnabled ? "Enabled" : "Disabled",
                bodyLeft + 10, y + 18,
                working.customBackgroundEnabled ? RotClientTheme.SUCCESS : RotClientTheme.TEXT_MUTED,
                false);
    }

    /** Handles a left click; returns true when it landed on the cards and was used. */
    boolean click(int mouseX, int mouseY, int left, int right, int top, int viewTop, int viewBottom) {
        if (mouseX < left || mouseX >= right || mouseY < viewTop || mouseY >= viewBottom) {
            return false;
        }
        AppearanceCards.Item item = layout.itemAt(mouseY - top);
        if (item == null) {
            return true;
        }
        int bodyLeft = left + AppearanceCards.BODY_INSET;
        switch (item.kind()) {
            case CARD -> {
                confirming.clear();
                AppearanceCards.Card card = AppearanceCards.card(item.id());
                if (card != null) {
                    toggle(card.expandId());
                }
            }
            case GROUP -> toggle(item.id());
            case COLOR -> {
                if (mouseX >= bodyLeft) {
                    openPicker(item.id(), item.label());
                }
            }
            case TOGGLE -> {
                if (mouseX >= bodyLeft) {
                    working.customBackgroundEnabled = !working.customBackgroundEnabled;
                    commit("Background " + (working.customBackgroundEnabled ? "enabled" : "disabled"));
                }
            }
            case FILE -> {
                if (mouseX >= bodyLeft) {
                    working.customBackgroundFile = item.id();
                    working.customBackgroundEnabled = true;
                    RotClientBackgroundManager.invalidateCache();
                    commit("Background set to " + item.id());
                }
            }
            case BUTTONS -> {
                AppearanceCards.Button button = item.buttonAt(mouseX, bodyLeft);
                if (button != null) {
                    runAction(button.id(), item.id());
                }
            }
            default -> {
            }
        }
        return true;
    }

    private void runAction(String action, String rowId) {
        boolean keepConfirm = action.equals(AppearanceCards.RESET_ALL)
                || action.equals(AppearanceCards.RESET_LAYOUT)
                || action.equals(AppearanceCards.RESET_HUD);
        if (!keepConfirm) {
            confirming.clear();
        }
        switch (action) {
            case AppearanceCards.CARD_UNDO -> {
                AppearanceCards.Card card = AppearanceCards.card(rowId.substring("card.".length()));
                if (card != null) {
                    copyFields(card.fields(), baseline, working);
                    RotClientBackgroundManager.invalidateCache();
                    commit(card.title() + " changes undone");
                }
            }
            case AppearanceCards.CARD_RESET -> {
                AppearanceCards.Card card = AppearanceCards.card(rowId.substring("card.".length()));
                if (card != null) {
                    copyFields(card.fields(), defaults, working);
                    RotClientBackgroundManager.invalidateCache();
                    commit(card.title() + " reset to default");
                }
            }
            case AppearanceCards.BG_OPACITY_DOWN -> {
                working.customBackgroundOpacity =
                        RotClientAppearanceConfig.clamp(working.customBackgroundOpacity - 15, 0, 255);
                commit("");
            }
            case AppearanceCards.BG_OPACITY_UP -> {
                working.customBackgroundOpacity =
                        RotClientAppearanceConfig.clamp(working.customBackgroundOpacity + 15, 0, 255);
                commit("");
            }
            case AppearanceCards.BG_DIM_DOWN -> {
                working.customBackgroundDim =
                        RotClientAppearanceConfig.clamp(working.customBackgroundDim - 15, 0, 255);
                commit("");
            }
            case AppearanceCards.BG_DIM_UP -> {
                working.customBackgroundDim =
                        RotClientAppearanceConfig.clamp(working.customBackgroundDim + 15, 0, 255);
                commit("");
            }
            case AppearanceCards.BG_FIT_FILL -> {
                working.customBackgroundFit = "fill";
                commit("");
            }
            case AppearanceCards.BG_FIT_FIT -> {
                working.customBackgroundFit = "fit";
                commit("");
            }
            case AppearanceCards.BG_FIT_STRETCH -> {
                working.customBackgroundFit = "stretch";
                commit("");
            }
            case AppearanceCards.BG_OPEN_FOLDER ->
                    status = RotClientBackgroundManager.openBackgroundsFolder()
                            ? "Opened backgrounds folder"
                            : "Could not open folder";
            case AppearanceCards.BG_REFRESH -> {
                RotClientBackgroundManager.invalidateCache();
                refreshFilesIfStale(true);
                status = "Background list refreshed";
            }
            case AppearanceCards.RESET_UNDO_ALL -> {
                copyFields(AppearanceCards.allFields(), baseline, working);
                RotClientBackgroundManager.invalidateCache();
                commit("Everything undone");
            }
            case AppearanceCards.RESET_ALL -> {
                if (confirm(action, "Click again to confirm reset all")) {
                    copyFields(AppearanceCards.allFields(), defaults, working);
                    RotClientBackgroundManager.invalidateCache();
                    commit("All appearance settings reset");
                }
            }
            case AppearanceCards.RESET_LAYOUT -> {
                if (confirm(action, "Click again to confirm reset UI positions")) {
                    RotClientClient.resetLayoutPositionsFromUi(true, true);
                    status = "Client UI and Mining HUD positions reset";
                }
            }
            case AppearanceCards.RESET_HUD -> {
                if (confirm(action, "Click again to confirm reset HUD visibility")) {
                    trackerConfig.resetHudVisibility();
                    RotClientClient.save();
                    status = "HUD visibility restored to defaults";
                }
            }
            default -> {
            }
        }
    }

    /** First click arms the button, the second one performs it. */
    private boolean confirm(String id, String prompt) {
        if (confirming.remove(id)) {
            return true;
        }
        confirming.clear();
        confirming.add(id);
        status = prompt;
        return false;
    }

    private void toggle(String expandId) {
        RotClientClient.workspace().toggleAppearanceSection(expandId);
    }

    private void openPicker(String field, String label) {
        Minecraft.getInstance().gui.setScreen(
                new RotClientColorPickerScreen(
                        host,
                        label,
                        colorOf(field),
                        live -> {
                            // Preview only; the value is saved when the picker is confirmed.
                            setColor(field, live);
                            preview();
                        },
                        value -> {
                            setColor(field, value);
                            commit("Color updated");
                        }));
    }

    private void preview() {
        working.normalize();
        RotClientTheme.apply(working);
    }

    private void commit(String message) {
        preview();
        if (!RotClientAppearanceStore.save(working)) {
            status = "Save failed";
            return;
        }
        if (!message.isEmpty()) {
            status = message;
        }
    }

    private void refreshFilesIfStale(boolean force) {
        long now = System.currentTimeMillis();
        if (force || now - filesReadAtMs >= FILE_LIST_TTL_MS) {
            files = List.copyOf(RotClientBackgroundManager.listBackgroundFiles());
            filesReadAtMs = now;
        }
    }

    private boolean changed(AppearanceCards.Card card) {
        for (String name : card.fields()) {
            if (!Objects.equals(read(working, name), read(baseline, name))) {
                return true;
            }
        }
        return false;
    }

    private int colorOf(String field) {
        Object value = read(working, field);
        return value instanceof Integer color ? color : 0;
    }

    private void setColor(String field, int color) {
        write(working, field, color);
    }

    private static void copyFields(
            List<String> names,
            RotClientAppearanceConfig from,
            RotClientAppearanceConfig to) {
        for (String name : names) {
            write(to, name, read(from, name));
        }
    }

    private static Object read(RotClientAppearanceConfig config, String name) {
        try {
            return field(name).get(config);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Appearance field " + name, e);
        }
    }

    private static void write(RotClientAppearanceConfig config, String name, Object value) {
        try {
            field(name).set(config, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Appearance field " + name, e);
        }
    }

    private static Field field(String name) {
        return FIELDS.computeIfAbsent(name, key -> {
            try {
                Field field = RotClientAppearanceConfig.class.getDeclaredField(key);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Unknown appearance field " + key, e);
            }
        });
    }
}
