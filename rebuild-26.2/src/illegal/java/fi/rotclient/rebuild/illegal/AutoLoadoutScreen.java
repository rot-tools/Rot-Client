package fi.rotclient.rebuild.illegal;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.rebuild.RotChrome;
import fi.rotclient.rebuild.illegal.autocommands.AutoCommandsClient;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class AutoLoadoutScreen extends Screen {
    private final Screen parent;
    private final IllegalSettings config;
    private final AutoLoadoutSettings settings;
    private boolean bindingPage;
    private int startSlot;
    private int capturing = -1;
    private int x, y, panelWidth, panelHeight, slotRows;
    private String status = "";

    AutoLoadoutScreen(Screen parent, IllegalSettings config) {
        super(Component.literal("Auto Loadout"));
        this.parent = parent;
        this.config = config;
        this.settings = config.autoLoadout;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(422, width - 18);
        slotRows = Math.clamp((height - 110) / 24, 2, AutoLoadoutSettings.SLOT_COUNT);
        panelHeight = bindingPage ? slotRows * 24 + 93 : 170;
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        int right = x + panelWidth - 12;
        button("Timing", x + 12, y + 33, 86, !bindingPage, () -> changePage(false));
        button("Keybinds", x + 104, y + 33, 86, bindingPage, () -> changePage(true));
        button("Done", right - 66, y + panelHeight - 29, 66, true, this::onClose);
        if (bindingPage) {
            buildSlots(right);
        } else {
            int controls = Math.min(170, panelWidth / 2);
            addRenderableWidget(new DelaySlider(right - controls, y + 59, controls, settings.equipDelay, 8, value -> settings.equipDelay = value));
            addRenderableWidget(new DelaySlider(right - controls, y + 84, controls, settings.closeDelay, 8, value -> settings.closeDelay = value));
            addRenderableWidget(new DelaySlider(right - controls, y + 109, controls, settings.variety, 5, value -> settings.variety = value));
        }
    }

    private void buildSlots(int right) {
        startSlot = Math.clamp(startSlot, 0, AutoLoadoutSettings.SLOT_COUNT - slotRows);
        for (int slot = startSlot; slot < startSlot + slotRows; slot++) {
            int row = y + 57 + 24 * (slot - startSlot);
            int index = slot;
            String label = capturing == slot ? "Press a key…" : keyLabel(settings.keys[slot]);
            button(label, right - 146, row + 1, 120, capturing == slot, () -> {
                capturing = index;
                status = "Esc cancels. Choose an unused key.";
                rebuildWidgets();
            });
            button("×", right - 22, row + 1, 22, false, () -> {
                settings.keys[index] = GLFW.GLFW_KEY_UNKNOWN;
                finishCapture();
            }).active = settings.keys[slot] != GLFW.GLFW_KEY_UNKNOWN;
        }
        if (slotRows < AutoLoadoutSettings.SLOT_COUNT) {
            button("‹", x + 12, y + panelHeight - 29, 24, false, () -> scrollSlots(-slotRows)).active = startSlot > 0;
            button("›", x + 40, y + panelHeight - 29, 24, false, () -> scrollSlots(slotRows)).active =
                    startSlot + slotRows < AutoLoadoutSettings.SLOT_COUNT;
        }
    }

    private void changePage(boolean keybinds) {
        bindingPage = keybinds;
        capturing = -1;
        status = "";
        rebuildWidgets();
    }

    private void scrollSlots(int step) {
        int next = Math.clamp(startSlot + step, 0, AutoLoadoutSettings.SLOT_COUNT - slotRows);
        if (next == startSlot) return;
        startSlot = next;
        capturing = -1;
        status = "";
        rebuildWidgets();
    }

    private Button button(String label, int left, int top, int w, boolean primary, Runnable action) {
        return addRenderableWidget(RotChrome.button(font, Component.literal(label), left, top, w, 20,
                primary, ignored -> action.run()));
    }

    private void finishCapture() {
        capturing = -1;
        status = "";
        config.save();
        rebuildWidgets();
    }

    private static String keyLabel(int key) {
        return AutoLoadoutSettings.validKey(key)
                ? InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString() : "Not bound";
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (capturing < 0) return super.keyPressed(event);
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            capturing = -1;
            status = "";
            rebuildWidgets();
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
            settings.keys[capturing] = GLFW.GLFW_KEY_UNKNOWN;
            finishCapture();
            return true;
        }
        if (!AutoLoadoutSettings.validKey(key) || (event.modifiers() & 15) != 0) {
            status = "Choose one key. Right Shift is reserved.";
            return true;
        }
        int existing = settings.slotForKey(key);
        String keyName = InputConstants.Type.KEYSYM.getOrCreate(key).getName();
        boolean command = AutoCommandsClient.keyIsAssigned(keyName);
        if ((existing >= 0 && existing != capturing) || command || key == config.clickGuiKey || key == config.autoSprintKey) {
            status = "That key is already assigned in Rot Client.";
            return true;
        }
        settings.keys[capturing] = key;
        finishCapture();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!bindingPage || dy == 0 || mx < x || mx >= x + panelWidth || my < y + 57 || my >= y + 57 + slotRows * 24) {
            return super.mouseScrolled(mx, my, dx, dy);
        }
        scrollSlots(dy > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        RotChrome.panel(graphics, x, y, panelWidth, panelHeight);
        graphics.fill(x + 12, y + 1, x + 78, y + 3, RotChrome.accent());
        RotChrome.text(graphics, font, "Auto Loadout", x + 12, y + 9, panelWidth - 24, RotChrome.TEXT);
        RotChrome.text(graphics, font, "One key per slot · Test server", x + 12, y + 20, panelWidth - 24, RotChrome.MUTED);
        if (bindingPage) {
            for (int row = 0; row < slotRows; row++) {
                int top = y + 57 + row * 24;
                RotChrome.row(graphics, x + 12, top, panelWidth - 24, 22, my >= top && my < top + 22
                        && mx >= x + 12 && mx < x + panelWidth - 12);
                RotChrome.text(graphics, font, "Slot " + (startSlot + row + 1), x + 20, top + 6, panelWidth - 180, RotChrome.TEXT);
            }
            RotChrome.text(graphics, font, status, x + 70, y + panelHeight - 24, panelWidth - 154, RotChrome.MUTED);
        } else {
            RotChrome.text(graphics, font, "Equip delay", x + 12, y + 65, panelWidth / 2 - 12, RotChrome.TEXT);
            RotChrome.text(graphics, font, "Close delay", x + 12, y + 90, panelWidth / 2 - 12, RotChrome.TEXT);
            RotChrome.text(graphics, font, "Max delay variety", x + 12, y + 115, panelWidth / 2 - 12, RotChrome.TEXT);
            RotChrome.text(graphics, font, "Adds 0 to the selected variety to each delay.", x + 12, y + 143,
                    panelWidth - 100, RotChrome.MUTED);
        }
        super.extractRenderState(graphics, mx, my, delta);
    }

    @Override
    public void onClose() {
        config.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        config.save();
    }

    @Override public boolean isPauseScreen() { return false; }

    private final class DelaySlider extends AbstractSliderButton {
        private final int max;
        private final IntConsumer setter;

        DelaySlider(int x, int y, int w, int ticks, int max, IntConsumer setter) {
            super(x, y, w, 20, Component.empty(), (ticks - 1d) / (max - 1));
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int ticks() { return 1 + (int) Math.round(value * (max - 1)); }

        @Override protected void setValue(double fraction) {
            super.setValue(Math.round(Math.clamp(fraction, 0, 1) * (max - 1)) / (double) (max - 1));
        }

        @Override public boolean keyPressed(KeyEvent event) {
            if (event.key() == GLFW.GLFW_KEY_LEFT || event.key() == GLFW.GLFW_KEY_RIGHT) {
                setValue(value + (event.key() == GLFW.GLFW_KEY_LEFT ? -1d : 1d) / (max - 1));
                return true;
            }
            return super.keyPressed(event);
        }

        @Override protected void updateMessage() { setMessage(Component.literal(ticks() + (ticks() == 1 ? " tick" : " ticks"))); }
        @Override protected void applyValue() { setter.accept(ticks()); }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
            RotChrome.slider(g, font, getMessage().getString(), getX(), getY(), getWidth(), getHeight(), value, isHoveredOrFocused());
            handleCursor(g);
        }
    }
}
