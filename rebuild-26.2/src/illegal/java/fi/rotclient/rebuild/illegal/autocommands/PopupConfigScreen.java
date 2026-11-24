// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

public final class PopupConfigScreen extends Screen {
    private static final int PANEL_HEIGHT = 222;
    private static final int GOLD = 0xFFFFCC65;
    private static final int MUTED = 0xFFADB9CB;
    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final int[] TEXT_COLORS = {0x63F594, 0xFFFFFF, 0xFFD166, 0xFF6060, 0x62C8FF, 0xCE93FF, 0xFF94CD};
    private static final int[] BACKGROUND_COLORS = {0x101914, 0x0D1423, 0x251A39, 0x362021, 0x393021, 0x000000, 0xFFFFFF};
    private final PopupConfig config;
    private final Screen parent;
    private boolean editingLayout;
    private int left;
    private int top;
    private int panelWidth;
    private int column;
    private String error = "";

    public PopupConfigScreen(PopupConfig config, Screen parent) {
        super(Component.literal("Popup style"));
        this.config = config;
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(370, width - 16);
        left = (width - panelWidth) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        int x = left + 12;
        int usable = panelWidth - 24;
        column = (usable - 12) / 2;

        int tabWidth = Math.min(106, (usable - 12) / 3);
        addRenderableWidget(Button.builder(Component.literal("Appearance"), button -> switchTab(false))
                .bounds(x, top + 27, tabWidth, 20).build()).active = editingLayout;
        addRenderableWidget(Button.builder(Component.literal("Size & position"), button -> switchTab(true))
                .bounds(x + tabWidth + 6, top + 27, tabWidth, 20).build()).active = !editingLayout;
        addRenderableWidget(Button.builder(popupToggleLabel(), button -> {
            config.enabled = !config.enabled;
            button.setMessage(popupToggleLabel());
        }).bounds(x + 2 * (tabWidth + 6), top + 27, usable - 2 * (tabWidth + 6), 20).build());

        if (editingLayout) addLayoutControls(x, usable);
        else addAppearanceControls(x);

        addRenderableWidget(Button.builder(Component.literal("Preview / move"), button ->
                        minecraft.gui.setScreen(new PopupPositionScreen(this, config)))
                .bounds(x, top + 191, column, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(x + column + 12, top + 191, usable - column - 12, 20).build());
    }

    private void switchTab(boolean layout) {
        editingLayout = layout;
        rebuildWidgets();
    }

    private void addAppearanceControls(int x) {
        int backgroundX = x + column + 12;
        addColorControls(x, "Text color", config.textColor, TEXT_COLORS, value -> config.textColor = value);
        addColorControls(backgroundX, "Background color", config.backgroundColor, BACKGROUND_COLORS,
                value -> config.backgroundColor = value);
        addRenderableWidget(new Slider(x, top + 94, column, config.textOpacity, value -> config.textOpacity = value,
                value -> "Text opacity: " + Math.round(value * 100) + "%"));
        addRenderableWidget(new Slider(backgroundX, top + 94, column, config.backgroundOpacity,
                value -> config.backgroundOpacity = value,
                value -> "Background opacity: " + Math.round(value * 100) + "%"));
        addRenderableWidget(Button.builder(backgroundToggleLabel(), button -> {
            config.backgroundEnabled = !config.backgroundEnabled;
            button.setMessage(backgroundToggleLabel());
        }).bounds(backgroundX, top + 118, column, 20).build());
    }

    private void addColorControls(int x, String name, int initialColor, int[] presets, IntConsumer applyColor) {
        int inputWidth = Math.min(94, column - 51);
        EditBox input = new EditBox(font, x, top + 70, inputWidth, 20, Component.literal(name + " #RRGGBB"));
        input.setMaxLength(7);
        input.setValue(hex(initialColor));
        input.setResponder(value -> {
            boolean valid = HEX.matcher(value).matches();
            input.setTextColor(valid ? 0xFFFFFFFF : 0xFFFF7070);
            if (valid) applyColor.accept(readHex(value));
        });
        addRenderableWidget(input);
        addRenderableWidget(Button.builder(Component.literal("Preset"), button -> {
            String value = input.getValue();
            int current = HEX.matcher(value).matches() ? readHex(value) : -1;
            int next = 0;
            for (int i = 0; i < presets.length; i++) {
                if (presets[i] == current) {
                    next = (i + 1) % presets.length;
                    break;
                }
            }
            input.setValue(hex(presets[next]));
        }).bounds(x + inputWidth + 5, top + 70, column - inputWidth - 5, 20).build());
    }

    private void addLayoutControls(int x, int usable) {
        addRenderableWidget(new Slider(x, top + 57, usable, (config.scale - 0.5) / 2.5,
                value -> config.scale = 0.5 + value * 2.5,
                value -> String.format(Locale.ROOT, "Size: %.2f x", 0.5 + value * 2.5)));
        Slider duration = addRenderableWidget(new Slider(x + 24, top + 81, usable - 48, (config.durationSeconds - 0.1) / 4.9,
                value -> config.durationSeconds = seconds(value),
                value -> String.format(Locale.ROOT, "Duration: %.1f s", seconds(value)), 49));
        addRenderableWidget(Button.builder(Component.literal("-"), button -> duration.stepBy(-1))
                .bounds(x, top + 81, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> duration.stepBy(1))
                .bounds(x + usable - 20, top + 81, 20, 20).build());
        addRenderableWidget(new Slider(x, top + 105, column, config.horizontalPosition,
                value -> config.horizontalPosition = value,
                value -> "Position X: " + Math.round(value * 100) + "%"));
        addRenderableWidget(new Slider(x + column + 12, top + 105, usable - column - 12, config.verticalPosition,
                value -> config.verticalPosition = value,
                value -> "Position Y: " + Math.round(value * 100) + "%"));
    }

    private static String hex(int color) { return String.format(Locale.ROOT, "#%06X", color); }

    private static int readHex(String value) { return Integer.parseInt(value.startsWith("#") ? value.substring(1) : value, 16); }

    private Component popupToggleLabel() { return Component.literal(config.enabled ? "Popup: ON" : "Popup: OFF"); }

    private Component backgroundToggleLabel() {
        return Component.literal(config.backgroundEnabled ? "Background: ON" : "Background: OFF");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.fill(left, top, left + panelWidth, top + PANEL_HEIGHT, 0xF2192234);
        graphics.fill(left, top, left + panelWidth, top + 2, GOLD);
        graphics.centeredText(font, title, width / 2, top + 12, 0xFFFFFFFF);
        if (editingLayout) {
            graphics.centeredText(font, "Use Preview / move to drag the popup in game.", width / 2, top + 156, MUTED);
        } else {
            drawColorLabel(graphics, left + 12, "Text color", config.textColor);
            drawColorLabel(graphics, left + column + 24, "Background color", config.backgroundColor);
            graphics.fill(left + 12, top + 142, left + panelWidth - 12, top + 184, 0xFF26354C);
            PopupRenderer.drawSample(graphics, config, AutoCommandsClient.SUCCESS_PREVIEW, width / 2f, top + 163, panelWidth - 32);
        }
        if (!error.isEmpty()) graphics.centeredText(font, error, width / 2, top + 213, 0xFFFF8590);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawColorLabel(GuiGraphicsExtractor graphics, int x, String label, int color) {
        graphics.text(font, label, x, top + 56, MUTED);
        int swatchX = x + column - 12;
        graphics.fill(swatchX - 1, top + 53, swatchX + 12, top + 66, 0xFF586780);
        graphics.fill(swatchX, top + 54, swatchX + 11, top + 65, 0xFF000000 | color);
    }

    @Override
    public void onClose() {
        if (config.save()) minecraft.gui.setScreen(parent);
        else error = "Could not save settings. Please try again.";
    }

    @Override public void removed() { config.save(); }

    @Override public boolean isPauseScreen() { return false; }

    private static double seconds(double value) { return (1 + Math.round(Math.clamp(value, 0, 1) * 49)) / 10.0; }

    private static final class Slider extends AbstractSliderButton {
        private final DoubleConsumer setter;
        private final DoubleFunction<String> label;
        private final int steps;

        Slider(int x, int y, int width, double value, DoubleConsumer setter, DoubleFunction<String> label) {
            this(x, y, width, value, setter, label, 0);
        }

        Slider(int x, int y, int width, double value, DoubleConsumer setter, DoubleFunction<String> label, int steps) {
            super(x, y, width, 20, Component.empty(), Math.clamp(value, 0, 1));
            this.setter = setter;
            this.label = label;
            this.steps = steps;
            updateMessage();
        }

        @Override protected void setValue(double value) {
            super.setValue(steps > 0 ? Math.round(value * steps) / (double) steps : value);
        }

        void stepBy(int direction) { setValue(value + direction / (double) steps); }

        @Override public boolean keyPressed(KeyEvent event) {
            if (steps > 0 && (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT)) {
                stepBy(event.key() == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
                return true;
            }
            return super.keyPressed(event);
        }

        @Override protected void updateMessage() { setMessage(Component.literal(label.apply(value))); }
        @Override protected void applyValue() { setter.accept(value); }
    }
}
