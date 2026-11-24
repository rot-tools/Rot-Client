package fi.rotclient.rebuild;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

final class HudEditorScreen extends Screen {
    private final Screen parent;
    private boolean layers;
    private boolean dragging;
    private double grabX, grabY;
    private int layerOffset;
    private int layerRows;
    private int toolbarWidth;
    private int toolsX;
    private int toolsY;

    HudEditorScreen(Screen parent) {
        super(Component.literal("HUD Elements Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        toolbarWidth = Math.min(402, width - 16);
        toolsX = (width - toolbarWidth) / 2;
        toolsY = layers ? 50 : height - 56;
        button("Back", 8, 8, 52, this::onClose);
        button(layers ? "HUD Preview" : "HUD Visibility", width - 108, 8, 100, () -> {
            layers = !layers;
            rebuildWidgets();
        });
        if (layers) {
            addLayerControls();
            return;
        }
        InterfaceSettings settings = RebuildClient.settings();
        int half = (toolbarWidth - 5) / 2;
        addRenderableWidget(slider("Scale", toolsX, half, settings.hudScale, 0.5, 2,
                value -> settings.hudScale = value));
        addRenderableWidget(slider("Panel opacity", toolsX + half + 5, half, settings.hudOpacity, 0, 1,
                value -> settings.hudOpacity = value));
        int small = (toolbarWidth - 10) / 3;
        button(settings.hudShowBackground ? "Background ON" : "Background OFF", toolsX, toolsY + 24, small, () -> {
            settings.hudShowBackground = !settings.hudShowBackground;
            settings.save();
            rebuildWidgets();
        });
        button(settings.hudShadow ? "Shadow ON" : "Shadow OFF", toolsX + small + 5, toolsY + 24, small, () -> {
            settings.hudShadow = !settings.hudShadow;
            settings.save();
            rebuildWidgets();
        });
        button("Reset preview", toolsX + 2 * (small + 5), toolsY + 24, small, () -> {
            settings.resetHud();
            rebuildWidgets();
        });
    }

    private void addLayerControls() {
        layerRows = Math.max(1, (height - toolsY - 29) / 22);
        var entries = HudLayerSettings.LAYERS;
        var hidden = RebuildClient.settings().hiddenVanillaLayers;
        layerOffset = Math.clamp(layerOffset, 0, Math.max(0, entries.size() - layerRows));
        for (int row = 0; row < layerRows && row + layerOffset < entries.size(); row++) {
            var layer = entries.get(row + layerOffset);
            button(layer.title() + (hidden.contains(layer.id()) ? "   HIDDEN" : "   VISIBLE"),
                    toolsX, toolsY + 22 * row, toolbarWidth, () -> {
                        if (!hidden.remove(layer.id())) hidden.add(layer.id());
                        RebuildClient.settings().save();
                        rebuildWidgets();
                    });
        }
        if (entries.size() > layerRows) {
            button("▲", width / 2 - 36, height - 24, 34, () -> scrollLayers(-1)).active = layerOffset > 0;
            button("▼", width / 2 + 2, height - 24, 34, () -> scrollLayers(1)).active =
                    layerOffset + layerRows < entries.size();
        }
    }

    private void scrollLayers(int delta) {
        int next = Math.clamp(layerOffset + delta, 0, Math.max(0, HudLayerSettings.LAYERS.size() - layerRows));
        if (next != layerOffset) {
            layerOffset = next;
            rebuildWidgets();
        }
    }

    private AbstractSliderButton slider(String title, int x, int w, double current, double min, double max,
                                        DoubleConsumer update) {
        return new AbstractSliderButton(x, toolsY, w, 20, caption(title, current), (current - min) / (max - min)) {
            @Override
            protected void updateMessage() {
                setMessage(caption(title, actual()));
            }

            @Override
            protected void applyValue() {
                update.accept(actual());
            }

            @Override
            public boolean mouseReleased(MouseButtonEvent event) {
                RebuildClient.settings().save();
                return super.mouseReleased(event);
            }

            private double actual() {
                return min + value * (max - min);
            }
        };
    }

    private static Component caption(String title, double value) {
        return Component.literal(title + " " + String.format(Locale.ROOT, "%.2f", value));
    }

    private Button button(String title, int x, int y, int w, Runnable click) {
        return addRenderableWidget(Button.builder(Component.literal(title), ignored -> click.run())
                .bounds(x, y, w, 20).build());
    }

    private Preview preview() {
        var settings = RebuildClient.settings();
        int boxWidth = (int) Math.round(118 * settings.hudScale);
        int boxHeight = (int) Math.round(32 * settings.hudScale);
        int x = 8 + (int) Math.round(settings.hudX * Math.max(0, width - 16 - boxWidth));
        int y = 47 + (int) Math.round(settings.hudY * Math.max(0, toolsY - 55 - boxHeight));
        return new Preview(x, y, boxWidth, boxHeight);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (!layers && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            Preview preview = preview();
            if (event.x() >= preview.x && event.x() < preview.x + preview.w
                    && event.y() >= preview.y && event.y() < preview.y + preview.h) {
                dragging = true;
                grabX = event.x() - preview.x;
                grabY = event.y() - preview.y;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!dragging) return super.mouseDragged(event, deltaX, deltaY);
        var settings = RebuildClient.settings();
        Preview preview = preview();
        settings.hudX = Math.clamp((event.x() - grabX - 8) / Math.max(1, width - 16 - preview.w), 0, 1);
        settings.hudY = Math.clamp((event.y() - grabY - 47) / Math.max(1, toolsY - 55 - preview.h), 0, 1);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!dragging) return super.mouseReleased(event);
        dragging = false;
        RebuildClient.settings().save();
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (layers && vertical != 0) {
            scrollLayers(vertical > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        InterfaceSettings settings = RebuildClient.settings();
        graphics.fill(0, 0, width, 35, 0xE0000000 | settings.panel);
        graphics.fill(0, 34, width, 35, 0xFF000000 | settings.accent);
        graphics.centeredText(font, getTitle(), width / 2 - 12, 14, 0xFFF8F5FF);
        if (layers) {
            graphics.fill(toolsX - 4, 39, toolsX + toolbarWidth + 4, height - 4, 0xF0000000 | settings.panel);
        } else {
            graphics.text(font, "Drag the HUD preview to place it.", 8, 38, 0xFFE6DDF0);
            Preview p = preview();
            int alpha = (int) Math.round(settings.hudOpacity * 255);
            if (settings.hudShowBackground) {
                graphics.fill(p.x, p.y, p.x + p.w, p.y + p.h,
                        (alpha << 24) | settings.hudBackground);
            }
            int accent = 0xFF000000 | settings.accent;
            graphics.fill(p.x, p.y, p.x + p.w, p.y + 1, accent);
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(p.x, p.y).scale((float) settings.hudScale);
                graphics.text(font, "HUD preview", 8, 6, 0xFF000000 | settings.hudText, settings.hudShadow);
                graphics.text(font, "Layout sample", 8, 18, 0xFFC8BFD8, settings.hudShadow);
            } finally {
                graphics.pose().popMatrix();
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        RebuildClient.settings().save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Preview(int x, int y, int w, int h) {}
}
