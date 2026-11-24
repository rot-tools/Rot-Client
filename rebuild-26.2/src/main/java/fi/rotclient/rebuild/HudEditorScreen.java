package fi.rotclient.rebuild;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** The original Plus world-editor inspector for the HUD features shipped in this edition. */
final class HudEditorScreen extends Screen {
    private final Screen parent;
    private final ArrayDeque<LayoutSnapshot> undo = new ArrayDeque<>();
    private final ArrayDeque<LayoutSnapshot> redo = new ArrayDeque<>();
    private List<HudLayerSettings.HitArea> hitAreas = List.of();
    private HudLayerSettings.Layer selection;
    private boolean movingInspector;
    private boolean movingElement;
    private boolean changed;
    private double offsetX, offsetY;

    HudEditorScreen(Screen parent) {
        this(parent, null);
    }

    HudEditorScreen(Screen parent, HudLayerSettings.Layer selection) {
        super(Component.literal("HUD Elements Editor"));
        this.parent = parent;
        this.selection = selection;
    }

    private HudEditorChromePolicy.Rect inspector() {
        var settings = RebuildClient.settings();
        return HudEditorChromePolicy.resolve(HudEditorChromePolicy.Panel.TITLE,
                settings.hudInspectorX, settings.hudInspectorY, width, height);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // The world is the placement reference, as in the Plus editor.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        RotClientUiClock.beginFrame(minecraft);
        RotClientTheme.apply(RebuildClient.settings());
        hitAreas = HudLayerSettings.takeHitAreas();
        drawElementSelection(g, mouseX, mouseY);
        RotClientUiDraw.drawBackButton(g, font, mouseX, mouseY, 12, 12);
        drawEditorChromeCard(g, inspector(), selection == null ? "Select a visible HUD element" : selection.title(), mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private void drawEditorChromeCard(
            GuiGraphicsExtractor graphics,
            HudEditorChromePolicy.Rect rect,
            String selected,
            int mouseX,
            int mouseY) {

        int x = rect.x();
        int y = rect.y();
        int w = rect.w();
        int h = rect.h();
        boolean canEdit = selection != null;

        RotClientUiDraw.roundedFill(
                graphics,
                x + 2,
                y + 3,
                x + w + 2,
                y + h + 3,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.SHADOW,
                        0x58),
                6);

        RotClientUiDraw.roundedFill(
                graphics,
                x,
                y,
                x + w,
                y + h,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.SURFACE,
                        0xF4),
                6);

        RotClientUiDraw.roundedOutline(
                graphics,
                x,
                y,
                x + w,
                y + h,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.BORDER,
                        0xD0),
                6);

        RotClientUiDraw.roundedFill(
                graphics,
                x + 10,
                y,
                x + 72,
                y + 2,
                RotClientTheme.HUD_ACCENT,
                1);

        RotClientUiDraw.text(
                graphics,
                font,
                "HUD EDITOR",
                x + 10,
                y + 8,
                RotClientTheme.TEXT,
                true);

        String selectedText =
                selected == null
                        || selected.isBlank()
                        || selected.startsWith(
                                "No QoL")
                        ? "Click a HUD to edit it"
                        : selected;

        selectedText =
                RotClientUiDraw.ellipsize(
                        font,
                        selectedText,
                        w - 20);

        RotClientUiDraw.text(
                graphics,
                font,
                selectedText,
                x + 10,
                y + 22,
                RotClientTheme.TEXT_DIM,
                false);

        RotClientUiDraw.text(
                graphics,
                font,
                canEdit ? "Drag to move  ·  Scroll to resize" : "Click a visible HUD element to edit it",
                x + 10,
                y + 37,
                RotClientTheme.TEXT_MUTED,
                false);

        graphics.fill(
                x + 10,
                y + 52,
                x + w - 10,
                y + 53,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.DIVIDER,
                        0xC0));


        RotClientUiDraw.text(
                graphics,
                font,
                "SCALE",
                x + 10,
                y + 59,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 70,
                30,
                20,
                "-",
                false,
                canEdit);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 44,
                y + 70,
                66,
                20,
                Math.round(
                        (canEdit ? HudLayerSettings.placement(selection).scale() : 1)
                                * 100.0F)
                        + "%",
                false,
                false);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 114,
                y + 70,
                30,
                20,
                "+",
                false,
                canEdit);

        RotClientUiDraw.text(
                graphics,
                font,
                "POSITION",
                x + 10,
                y + 96,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 107,
                96,
                20,
                "Center",
                false,
                canEdit);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 112,
                y + 107,
                96,
                20,
                "Default",
                false,
                canEdit);

        RotClientUiDraw.text(
                graphics,
                font,
                "STYLE",
                x + 10,
                y + 133,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 144,
                48,
                20,
                "BG",
                false,
                false);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 62,
                y + 144,
                48,
                20,
                "Title",
                false,
                false);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 114,
                y + 144,
                48,
                20,
                "Text",
                false,
                false);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 166,
                y + 144,
                48,
                20,
                "Panel",
                false,
                false);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 171,
                64,
                20,
                canEdit && HudLayerSettings.hidden(selection) ? "Show" : "Hide",
                false,
                canEdit);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 78,
                y + 171,
                64,
                20,
                "Undo",
                false,
                !undo.isEmpty());

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 146,
                y + 171,
                64,
                20,
                "Redo",
                false,
                !redo.isEmpty());

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 10,
                y + 197,
                100,
                20,
                "Reset all",
                false,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 114,
                y + 197,
                100,
                20,
                "Done",
                true,
                true);
    }

    private void drawElementSelection(GuiGraphicsExtractor graphics, int mx, int my) {
        var hovered = targetAt(mx, my);
        if (hovered != null && hovered != selection) {
            outline(graphics, area(hovered), RotClientUiDraw.withAlpha(RotClientTheme.VIOLET, 0x9C));
        }
        if (selection == null) return;
        ScreenRectangle rect = area(selection);
        if (rect != null) {
            outline(graphics, rect, RotClientTheme.HUD_ACCENT);
            return;
        }
        rect = marker(selection);
        RotClientUiDraw.roundedFill(graphics, rect.left(), rect.top(), rect.right(), rect.bottom(), RotClientTheme.SURFACE);
        outline(graphics, rect, RotClientTheme.HUD_ACCENT);
        String suffix = HudLayerSettings.hidden(selection) ? " (hidden)" : " (not visible)";
        RotClientUiDraw.text(graphics, font,
                RotClientUiDraw.ellipsize(font, selection.title() + suffix, rect.width() - 10),
                rect.left() + 5, rect.top() + 5, RotClientTheme.TEXT_DIM, false);
    }

    private static void outline(GuiGraphicsExtractor graphics, ScreenRectangle rect, int color) {
        RotClientUiDraw.roundedOutline(graphics, rect.left() - 2, rect.top() - 2,
                rect.right() + 2, rect.bottom() + 2, color, 2);
    }

    private ScreenRectangle area(HudLayerSettings.Layer layer) {
        return hitAreas.stream().filter(hit -> hit.layer() == layer).map(HudLayerSettings.HitArea::bounds)
                .findFirst().orElse(null);
    }

    private ScreenRectangle marker(HudLayerSettings.Layer layer) {
        HudPlacement pos = HudLayerSettings.placement(layer);
        int w = Math.min(134, width - 12);
        int x = (int) (layer.anchorX(width) + pos.x()) - w / 2;
        int y = (int) (layer.anchorY(height) + pos.y()) - 9;
        return new ScreenRectangle(Math.clamp(x, 3, width - w - 3), Math.clamp(y, 3, height - 21), w, 18);
    }

    private HudLayerSettings.Layer targetAt(double x, double y) {
        if (inspector().contains((int) x, (int) y)) return null;
        if (selection != null && area(selection) == null && marker(selection).containsPoint((int) x, (int) y)) return selection;
        return hitAreas.stream().filter(hit -> hit.bounds().containsPoint((int) x, (int) y))
                .min(Comparator.comparingLong(hit -> (long) hit.bounds().width() * hit.bounds().height()))
                .map(HudLayerSettings.HitArea::layer).orElse(null);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        int mx = (int) event.x(), my = (int) event.y();
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && RotClientUiDraw.hitBackButton(mx, my, 12, 12)) {
            onClose();
            return true;
        }
        if (inspector().contains(mx, my) && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            handleInspector(mx, my);
            return true;
        }
        var target = targetAt(event.x(), event.y());
        if (target == null) return false;
        selection = target;
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            remember();
            HudLayerSettings.toggleHidden(selection);
            return true;
        }
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        movingElement = true;
        changed = false;
        return true;
    }

    private void handleInspector(int mx, int my) {
        var rect = inspector();
        int x = rect.x(), y = rect.y();
        if (hit(mx, my, x + 10, y + 197, 100)) {
            var settings = RebuildClient.settings();
            if (!settings.hiddenVanillaLayers.isEmpty() || !settings.vanillaHudPlacements.isEmpty()) {
                remember();
                settings.hiddenVanillaLayers.clear();
                settings.vanillaHudPlacements.clear();
                settings.save();
            }
        } else if (hit(mx, my, x + 114, y + 197, 100)) {
            onClose();
        } else if (hit(mx, my, x + 78, y + 171, 64)) {
            restore(undo, redo);
        } else if (hit(mx, my, x + 146, y + 171, 64)) {
            restore(redo, undo);
        } else if (selection != null && hit(mx, my, x + 10, y + 171, 64)) {
            remember();
            HudLayerSettings.toggleHidden(selection);
        } else if (selection != null && hit(mx, my, x + 112, y + 107, 96)) {
            changePlacement(HudPlacement.DEFAULT);
        } else if (selection != null && hit(mx, my, x + 10, y + 107, 96)) {
            HudPlacement pos = HudLayerSettings.placement(selection);
            ScreenRectangle bounds = area(selection);
            double centerX = bounds != null ? bounds.left() + bounds.width() / 2d : selection.anchorX(width) + pos.x();
            double centerY = bounds != null ? bounds.top() + bounds.height() / 2d : selection.anchorY(height) + pos.y();
            changePlacement(pos.move(width / 2d - centerX, height / 2d - centerY));
        } else if (selection != null && hit(mx, my, x + 10, y + 70, 30)) {
            resize(-0.05);
        } else if (selection != null && hit(mx, my, x + 114, y + 70, 30)) {
            resize(0.05);
        } else if (rect.containsHeader(mx, my, 52)) {
            movingInspector = true;
            offsetX = mx - x;
            offsetY = my - y;
        }
    }

    private static boolean hit(int mx, int my, int x, int y, int width) {
        return RotClientUiDraw.inside(mx, my, x, y, width, 20);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (movingInspector) {
            var settings = RebuildClient.settings();
            var rect = inspector();
            settings.hudInspectorX = HudEditorChromePolicy.clampDragX((int) (event.x() - offsetX), rect.w(), width);
            settings.hudInspectorY = HudEditorChromePolicy.clampDragY((int) (event.y() - offsetY), rect.h(), height);
            return true;
        }
        if (!movingElement || selection == null) return super.mouseDragged(event, dx, dy);
        if (dx == 0 && dy == 0) return true;
        if (!changed) remember();
        changed = true;
        var placement = HudLayerSettings.placement(selection);
        HudLayerSettings.place(selection, placement.move(dx, dy));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!movingElement && !movingInspector) return super.mouseReleased(event);
        if (changed || movingInspector) RebuildClient.settings().save();
        movingElement = false;
        movingInspector = false;
        changed = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        var target = targetAt(x, y);
        if (target == null || vertical == 0) return super.mouseScrolled(x, y, horizontal, vertical);
        selection = target;
        resize(vertical > 0 ? 0.05 : -0.05);
        return true;
    }

    private void resize(double delta) {
        changePlacement(HudLayerSettings.placement(selection).resize(delta));
    }

    private void changePlacement(HudPlacement placement) {
        if (placement.equals(HudLayerSettings.placement(selection))) return;
        remember();
        HudLayerSettings.place(selection, placement);
        RebuildClient.settings().save();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if ((event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            if (event.key() == GLFW.GLFW_KEY_Z) {
                if ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) restore(redo, undo);
                else restore(undo, redo);
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_Y) {
                restore(redo, undo);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    private void remember() {
        undo.push(snapshot());
        if (undo.size() > 32) undo.removeLast();
        redo.clear();
    }

    private static LayoutSnapshot snapshot() {
        var settings = RebuildClient.settings();
        return new LayoutSnapshot(Set.copyOf(settings.hiddenVanillaLayers), Map.copyOf(settings.vanillaHudPlacements));
    }

    private static void restore(ArrayDeque<LayoutSnapshot> from, ArrayDeque<LayoutSnapshot> to) {
        if (from.isEmpty()) return;
        LayoutSnapshot next = from.pop();
        to.push(snapshot());
        var settings = RebuildClient.settings();
        settings.hiddenVanillaLayers.clear();
        settings.hiddenVanillaLayers.addAll(next.hidden);
        settings.vanillaHudPlacements.clear();
        settings.vanillaHudPlacements.putAll(next.placements);
        settings.save();
    }

    @Override
    public void onClose() {
        if (changed || movingInspector) RebuildClient.settings().save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record LayoutSnapshot(Set<String> hidden, Map<String, HudPlacement> placements) {}
}
