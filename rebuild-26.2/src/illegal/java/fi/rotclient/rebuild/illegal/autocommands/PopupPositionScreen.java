// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import fi.rotclient.rebuild.RotChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class PopupPositionScreen extends Screen {
    private final Screen parent;
    private final PopupConfig config;
    private boolean dragging;
    private boolean previewFailure;
    private double offsetX;
    private double offsetY;

    public PopupPositionScreen(Screen parent, PopupConfig config) {
        super(Component.literal("Popup preview"));
        this.parent = parent;
        this.config = config;
    }

    @Override protected void init() {
        addRenderableWidget(RotChrome.button(font, Component.literal(previewFailure ? "Show success" : "Show failure"),
                width / 2 - 112, height - 29, 103, 20, false, button -> {
            previewFailure = !previewFailure;
            button.setMessage(Component.literal(previewFailure ? "Show success" : "Show failure"));
        }));
        addRenderableWidget(RotChrome.button(font, Component.literal("Back to settings"),
                width / 2 - 5, height - 29, 117, 20, true, button -> onClose()));
    }

    private CommandShortcut.Notice sample() {
        return previewFailure ? AutoCommandsClient.FAILURE_PREVIEW : AutoCommandsClient.SUCCESS_PREVIEW;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0x30000000);
        var box = PopupRenderer.draw(graphics, config, sample(), 1);
        if (dragging || box.contains(mouseX, mouseY)) {
            graphics.outline(Math.round(box.left()) - 1, Math.round(box.top()) - 1,
                    Math.round(box.right() - box.left()) + 2, Math.round(box.bottom() - box.top()) + 2,
                    RotChrome.accent());
        }
        RotChrome.centered(graphics, font, "Drag the popup. Scroll to resize.", width / 2, 10, RotChrome.TEXT);
        RotChrome.centered(graphics, font, config.enabled ? "Preview" : "Preview (popup is disabled)",
                width / 2, 23, RotChrome.MUTED);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        var box = PopupRenderer.bounds(width, height, config, sample());
        if (event.button() == 0 && box.contains(event.x(), event.y())) {
            dragging = true;
            offsetX = event.x() - box.centerX();
            offsetY = event.y() - box.anchorY();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            config.horizontalPosition = Math.clamp((event.x() - offsetX) / width, 0, 1);
            config.verticalPosition = Math.clamp((event.y() - offsetY) / height, 0, 1);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        config.scale = Math.clamp(config.scale + scrollY * 0.1, 0.5, 3);
        return true;
    }

    @Override public void onClose() { config.save(); minecraft.gui.setScreen(parent); }
    @Override public void removed() { config.save(); }
    @Override public boolean isPauseScreen() { return false; }
}
