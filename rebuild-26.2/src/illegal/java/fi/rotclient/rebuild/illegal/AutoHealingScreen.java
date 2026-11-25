package fi.rotclient.rebuild.illegal;

import fi.rotclient.rebuild.RotChrome;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class AutoHealingScreen extends Screen {
    private static final AutoHealingSettings.Item[] ITEMS = AutoHealingSettings.Item.values();
    private static final int PANEL_HEIGHT = 254;
    private final Screen parent;
    private final IllegalSettings config;
    private final AutoHealingSettings settings;
    private int x, y, panelWidth;

    AutoHealingScreen(Screen parent, IllegalSettings config) {
        super(Component.literal("Auto Healing"));
        this.parent = parent;
        this.config = config;
        settings = config.autoHealing;
    }

    @Override protected void init() {
        panelWidth = Math.min(400, width - 16);
        x = (width - panelWidth) / 2;
        y = (height - PANEL_HEIGHT) / 2;
        int selector = x + panelWidth / 2 - 22;
        int controlWidth = x + panelWidth - 12 - selector;
        addRenderableWidget(RotChrome.button(font, Component.literal(settings.item.title), selector, y + 33,
                controlWidth, 20, false, button -> {
                    settings.item = ITEMS[(settings.item.ordinal() + 1) % ITEMS.length];
                    button.setMessage(Component.literal(settings.item.title));
                }));
        addRenderableWidget(new IntegerSlider(selector, y + 59, controlWidth, settings.healthPercent, 100,
                "%", value -> settings.healthPercent = value));
        addRenderableWidget(new IntegerSlider(selector, y + 85, controlWidth, settings.minimumSeconds, 20,
                " s", value -> settings.minimumSeconds = value));
        addRenderableWidget(new IntegerSlider(selector, y + 111, controlWidth, settings.selectionDelayTicks, 10,
                " ticks", value -> settings.selectionDelayTicks = value));
        addRenderableWidget(new IntegerSlider(selector, y + 137, controlWidth, settings.stepDelayTicks, 10,
                " ticks", value -> settings.stepDelayTicks = value));
        addRenderableWidget(new IntegerSlider(selector, y + 163, controlWidth, settings.returnDelayTicks, 10,
                " ticks", value -> settings.returnDelayTicks = value));
        addRenderableWidget(RotChrome.button(font, Component.literal("Done"), x + panelWidth - 76, y + 225,
                64, 20, true, button -> onClose()));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        RotChrome.panel(graphics, x, y, panelWidth, PANEL_HEIGHT);
        graphics.fill(x + 12, y + 1, x + 77, y + 3, RotChrome.accent());
        RotChrome.text(graphics, font, "Auto Healing", x + 12, y + 9, panelWidth - 24, RotChrome.TEXT);
        label(graphics, "Healing item", 39);
        label(graphics, "Health below", 65);
        label(graphics, "Minimum interval", 91);
        label(graphics, "Select delay", 117);
        label(graphics, "Heal delay", 143);
        label(graphics, "Return delay", 169);
        RotChrome.text(graphics, font, "1 client tick is about 50 ms at 20 TPS.", x + 12, y + 194,
                panelWidth - 24, RotChrome.MUTED);
        super.extractRenderState(graphics, mx, my, delta);
    }

    private void label(GuiGraphicsExtractor graphics, String text, int offset) {
        RotChrome.text(graphics, font, text, x + 12, y + offset, panelWidth / 2 - 39, RotChrome.TEXT);
    }

    @Override public void removed() { config.save(); }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private final class IntegerSlider extends AbstractSliderButton {
        private final int steps;
        private final String suffix;
        private final IntConsumer setter;

        IntegerSlider(int x, int y, int width, int current, int max, String suffix, IntConsumer setter) {
            super(x, y, width, 20, Component.empty(), (current - 1.0) / (max - 1));
            this.steps = max - 1;
            this.suffix = suffix;
            this.setter = setter;
            updateMessage();
        }

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

        private int selection() { return 1 + (int) Math.round(value * steps); }
        @Override protected void updateMessage() { setMessage(Component.literal(selection() + suffix)); }
        @Override protected void applyValue() { setter.accept(selection()); }

        @Override public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            RotChrome.slider(g, font, getMessage().getString(), getX(), getY(), getWidth(), getHeight(), value, isHoveredOrFocused());
            handleCursor(g);
        }
    }
}
