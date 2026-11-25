package fi.rotclient.rebuild.illegal;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.rebuild.RotChrome;
import fi.rotclient.rebuild.illegal.autocommands.AutoCommandsClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class AutoClickerScreen extends Screen {
    private final Screen parent;
    private final IllegalSettings config;
    private final AutoClickerSettings settings;
    private enum Group {
        LEFT("Left clicker"), RIGHT("Right clicker"), CUSTOM("Customisation");

        final String title;
        Group(String title) { this.title = title; }
    }

    private record Label(String text, int y, int width) {}

    private Group expanded = Group.LEFT;
    private boolean advanced;
    private final List<Label> labels = new ArrayList<>();
    private AutoClickerSettings.Button capture;
    private String status = "";
    private String availability;
    private int x, y, panelWidth, panelHeight;

    AutoClickerScreen(Screen parent, IllegalSettings config) {
        super(Component.literal("Auto Clicker"));
        this.parent = parent;
        this.config = config;
        settings = config.autoClicker;
    }

    @Override
    protected void init() {
        labels.clear();
        availability = availabilityMessage();
        panelWidth = Math.min(420, width - 16);
        int rows = advanced ? 7 : expanded == Group.CUSTOM ? 8 : expanded == null ? 3 : 6;
        int step = height < 255 ? 21 : 24;
        panelHeight = 55 + rows * step;
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        button("Done", x + panelWidth - 76, y + panelHeight - 29, 64, true, this::onClose);
        int top = y + 25;
        if (advanced) {
            button("‹ Customisation", x + 12, top, 112, false, () -> showAdvanced(false));
            toggle("Terminator only", top + step, settings.terminatorOnly, value -> settings.terminatorOnly = value);
            inputRow("Extra test server", top + 2 * step, settings.testServer, 255,
                    value -> settings.testServer = value.strip());
            inputRow("Left whitelist", top + 3 * step, String.join(", ", settings.left.whitelist), 512,
                    value -> settings.left.whitelist = AutoClickerSettings.items(value));
            inputRow("Right whitelist", top + 4 * step, String.join(", ", settings.right.whitelist), 512,
                    value -> settings.right.whitelist = AutoClickerSettings.items(value));
            label("Separate item UUIDs, IDs or names with commas.", top + 5 * step + 4, panelWidth - 24);
            label("Hypixel and singleplayer work without an address.", top + 6 * step + 3, panelWidth - 24);
            return;
        }
        for (Group group : Group.values()) {
            boolean open = expanded == group;
            button(group.title + (open ? "  ▾" : "  ›"), x + 12, top, panelWidth - 24, open, () -> {
                expanded = open ? null : group;
                capture = null;
                status = "";
                rebuildWidgets();
            });
            top += step;
            if (!open) continue;
            if (group == Group.CUSTOM) {
                slider("CPS jitter", top, 0, AutoClickerSettings.MAX_JITTER, settings.jitter,
                        value -> settings.jitter = value);
                toggle("Allow breaking blocks", top + step, settings.allowBreaking, value -> settings.allowBreaking = value);
                toggle("Block Dungeon Breaker", top + 2 * step, settings.skipDungeonBreaker,
                        value -> settings.skipDungeonBreaker = value);
                toggle("Whitelist mode", top + 3 * step, settings.whitelistOnly, value -> settings.whitelistOnly = value);
                button("Server and item filters", x + 12, top + 4 * step, panelWidth - 24, false, () -> showAdvanced(true));
                top += 5 * step;
            } else {
                String side = group == Group.LEFT ? "Left" : "Right";
                var lane = group == Group.LEFT ? settings.left : settings.right;
                toggle("Enable " + side.toLowerCase(Locale.ROOT) + " clicker", top, lane.enabled,
                        value -> lane.enabled = value);
                bindingRow(side + " key", lane, top + step);
                slider(side + " CPS", top + 2 * step, AutoClickerSettings.MIN_CPS, AutoClickerSettings.MAX_CPS,
                        lane.cps, value -> lane.cps = value);
                top += 3 * step;
            }
        }
    }

    private void bindingRow(String title, AutoClickerSettings.Button lane, int row) {
        int right = x + panelWidth - 12;
        int bindWidth = Math.min(160, panelWidth / 2 - 30);
        int bindingX = right - bindWidth - 24;
        label(title, row + 6, bindingX - x - 18);
        var key = AutoClickerSettings.inputKey(lane.binding);
        String label = capture == lane ? "Press input…" : key == InputConstants.UNKNOWN
                ? "Not bound" : key.getDisplayName().getString();
        button(label, bindingX, row, bindWidth, capture == lane, () -> {
            capture = lane;
            status = "Press a key or mouse button. Esc cancels.";
            rebuildWidgets();
        });
        button("×", right - 20, row, 20, false,
                () -> setBinding(lane, AutoClickerSettings.UNBOUND));
    }

    private void toggle(String title, int row, boolean enabled, Consumer<Boolean> setter) {
        label(title, row + 6, panelWidth - 87);
        button(enabled ? "On" : "Off", x + panelWidth - 68, row, 56, enabled, () -> {
            setter.accept(!enabled);
            rebuildWidgets();
        });
    }

    private void slider(String title, int row, int min, int max, int value, IntConsumer setter) {
        int controlWidth = panelWidth / 2 - 12;
        label(title, row + 6, panelWidth - controlWidth - 33);
        addRenderableWidget(new RateSlider(x + panelWidth / 2, row, controlWidth, min, max, value, setter));
    }

    private void inputRow(String title, int row, String value, int maxLength, Consumer<String> setter) {
        label(title, row + 6, panelWidth / 2 - 18);
        EditBox box = addRenderableWidget(RotChrome.field(font, x + panelWidth / 2, row, panelWidth / 2 - 12, 20, title));
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setResponder(setter);
    }

    private void showAdvanced(boolean open) {
        advanced = open;
        capture = null;
        status = "";
        rebuildWidgets();
    }

    private void button(String label, int x, int y, int w, boolean primary, Runnable action) {
        addRenderableWidget(RotChrome.button(font, Component.literal(label), x, y, w, 20, primary, ignored -> action.run()));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capture == null) return super.keyPressed(event);
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            capture = null;
            status = "";
            rebuildWidgets();
        } else if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
            setBinding(capture, AutoClickerSettings.UNBOUND);
        } else if ((event.modifiers() & 15) == 0) {
            tryBind(key);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (capture == null) return super.mouseClicked(event, doubleClick);
        tryBind(AutoClickerSettings.mouseBinding(event.button()));
        return true;
    }

    private void tryBind(int binding) {
        var key = AutoClickerSettings.inputKey(binding);
        if (!AutoClicker.canBind(minecraft, key)
                || binding == (capture == settings.left ? settings.right : settings.left).binding
                || binding == config.clickGuiKey || binding == config.autoSprintKey
                || config.autoLoadout.slotForKey(binding) >= 0 || AutoCommandsClient.keyIsAssigned(key.getName())) {
            status = "Choose an available mouse button or an unused key.";
            return;
        }
        setBinding(capture, binding);
    }

    private void setBinding(AutoClickerSettings.Button lane, int binding) {
        lane.binding = binding;
        capture = null;
        status = "";
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        RotChrome.panel(g, x, y, panelWidth, panelHeight);
        g.fill(x + 12, y + 1, x + 77, y + 3, RotChrome.accent());
        text(g, "Auto Clicker", 12, 9, panelWidth - 24);
        for (Label label : labels) text(g, label.text(), 12, label.y() - y, label.width());
        text(g, status.isEmpty() && !advanced ? availability : status, 12, panelHeight - 23, panelWidth - 94);
        super.extractRenderState(g, mx, my, delta);
    }

    private String availabilityMessage() {
        if (!settings.enabled) return "Auto Clicker is off. Enable it in Combat.";
        if (minecraft.hasSingleplayerServer()) return "Ready in singleplayer.";
        var server = minecraft.getCurrentServer();
        if (server == null) return "Join Hypixel or a test server.";
        if (AutoClicker.isHypixelAddress(server.ip)) return "Ready on Hypixel.";
        if (!AutoClicker.serverAllowed(false, settings.testServer, server.ip)) return "Set this test server in Customisation.";
        return "Ready on the configured test server.";
    }

    private void label(String text, int row, int maxWidth) {
        labels.add(new Label(text, row, maxWidth));
    }

    private void text(GuiGraphicsExtractor g, String label, int dx, int dy, int maxWidth) {
        RotChrome.text(g, font, label, x + dx, y + dy, maxWidth, RotChrome.TEXT);
    }

    @Override public void removed() { config.save(); }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private final class RateSlider extends AbstractSliderButton {
        private final int minimum;
        private final int steps;
        private final IntConsumer setter;

        RateSlider(int x, int y, int width, int minimum, int maximum, int rate, IntConsumer setter) {
            super(x, y, width, 20, Component.empty(), (rate - minimum) / (double) (maximum - minimum));
            this.minimum = minimum;
            this.steps = maximum - minimum;
            this.setter = setter;
            updateMessage();
        }

        private int rate() { return minimum + (int) Math.round(value * steps); }

        @Override protected void setValue(double value) {
            super.setValue(Math.round(Math.clamp(value, 0, 1) * steps) / (double) steps);
        }

        @Override public boolean keyPressed(KeyEvent event) {
            if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT) {
                setValue(value + (event.key() == GLFW.GLFW_KEY_LEFT ? -1d : 1d) / steps);
                return true;
            }
            return super.keyPressed(event);
        }

        @Override protected void updateMessage() { setMessage(Component.literal(Integer.toString(rate()))); }
        @Override protected void applyValue() { setter.accept(rate()); }

        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            RotChrome.slider(g, font, getMessage().getString(), getX(), getY(), getWidth(), getHeight(), value, isHoveredOrFocused());
            handleCursor(g);
        }
    }
}
