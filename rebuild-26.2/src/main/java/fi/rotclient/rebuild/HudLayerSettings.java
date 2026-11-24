package fi.rotclient.rebuild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import net.minecraft.resources.Identifier;

final class HudLayerSettings {
    static final String STATUS_EFFECTS = "effects";
    static final List<Layer> LAYERS = List.of(
            new Layer("hotbar", "Hotbar", Anchor.BOTTOM, 0, -11, VanillaHudElements.HOTBAR),
            new Layer("health", "Health Hearts", Anchor.BOTTOM, -50, -34, VanillaHudElements.HEALTH_BAR),
            new Layer("food", "Hunger", Anchor.BOTTOM, 50, -34, VanillaHudElements.FOOD_BAR),
            new Layer("armor", "Armor Icons", Anchor.BOTTOM, -50, -44, VanillaHudElements.ARMOR_BAR),
            new Layer("xp", "XP Bar / Level", Anchor.BOTTOM, 0, -26, VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.INFO_BAR),
            new Layer("air", "Air Bubbles", Anchor.BOTTOM, 50, -44, VanillaHudElements.AIR_BAR),
            new Layer("mount", "Mount Health", Anchor.BOTTOM, 50, -35, VanillaHudElements.MOUNT_HEALTH),
            new Layer("scoreboard", "Scoreboard", Anchor.RIGHT, -6, 0, VanillaHudElements.SCOREBOARD),
            new Layer("boss", "Boss Bar", Anchor.TOP, 0, 16, VanillaHudElements.BOSS_BAR),
            new Layer("action", "Action Bar", Anchor.BOTTOM, 0, -62, VanillaHudElements.OVERLAY_MESSAGE),
            new Layer("item_name", "Held Item Name", Anchor.BOTTOM, 0, -55, VanillaHudElements.HELD_ITEM_TOOLTIP),
            new Layer(STATUS_EFFECTS, "Status Effects", Anchor.TOP_RIGHT, -12, 12, VanillaHudElements.MOB_EFFECTS),
            new Layer("titles", "Titles", Anchor.CENTER, 0, 0, VanillaHudElements.TITLE_AND_SUBTITLE),
            new Layer("tab", "Player Tab List", Anchor.TOP, 0, 10, VanillaHudElements.PLAYER_LIST));
    private static final Map<Layer, ScreenRectangle> bounds = new LinkedHashMap<>();

    private HudLayerSettings() {}

    static void register() {
        for (Layer layer : LAYERS) {
            for (Identifier element : layer.elements()) {
                HudElementRegistry.replaceElement(element, original -> (graphics, delta) ->
                        extract(layer, original, graphics, delta));
            }
        }
    }

    private static void extract(Layer layer, HudElement original, GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (hidden(layer)) return;
        HudPlacement placement = placement(layer);
        boolean editing = Minecraft.getInstance().gui.screen() instanceof HudEditorScreen;
        Set<ScreenArea> before = editing ? Collections.newSetFromMap(new IdentityHashMap<>()) : null;
        if (before != null) eachArea(graphics.guiRenderState, before::add);
        if (HudPlacement.DEFAULT.equals(placement)) {
            original.extractRenderState(graphics, delta);
        } else {
            float ax = layer.anchorX(graphics.guiWidth()), ay = layer.anchorY(graphics.guiHeight());
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate((float) (ax + placement.x()), (float) (ay + placement.y()))
                        .scale((float) placement.scale()).translate(-ax, -ay);
                original.extractRenderState(graphics, delta);
            } finally {
                graphics.pose().popMatrix();
            }
        }
        if (before == null) return;
        eachArea(graphics.guiRenderState, area -> {
            if (!before.contains(area)) {
                ScreenRectangle rect = area.bounds();
                if (rect != null && rect.width() > 0 && rect.height() > 0) bounds.merge(layer, rect, HudLayerSettings::union);
            }
        });
    }

    private static void eachArea(GuiRenderState state, java.util.function.Consumer<ScreenArea> visit) {
        state.forEachElement(visit::accept, GuiRenderState.TraverseRange.ALL);
        state.forEachItem(visit::accept);
        state.forEachText(visit::accept);
        state.forEachPictureInPicture(visit::accept);
    }

    private static ScreenRectangle union(ScreenRectangle a, ScreenRectangle b) {
        int left = Math.min(a.left(), b.left()), top = Math.min(a.top(), b.top());
        return new ScreenRectangle(left, top, Math.max(a.right(), b.right()) - left, Math.max(a.bottom(), b.bottom()) - top);
    }

    static List<HitArea> takeHitAreas() {
        List<HitArea> result = new ArrayList<>(bounds.size());
        bounds.forEach((layer, rect) -> result.add(new HitArea(layer, rect)));
        bounds.clear();
        return result;
    }

    static boolean hidden(Layer layer) {
        return RebuildClient.settings().hiddenVanillaLayers.contains(layer.id());
    }

    static void toggleHidden(Layer layer) {
        Set<String> hidden = RebuildClient.settings().hiddenVanillaLayers;
        if (!hidden.remove(layer.id())) hidden.add(layer.id());
        RebuildClient.settings().save();
    }

    static HudPlacement placement(Layer layer) {
        return RebuildClient.settings().vanillaHudPlacements.getOrDefault(layer.id(), HudPlacement.DEFAULT);
    }

    static void place(Layer layer, HudPlacement value) {
        Map<String, HudPlacement> placements = RebuildClient.settings().vanillaHudPlacements;
        if (HudPlacement.DEFAULT.equals(value)) placements.remove(layer.id());
        else placements.put(layer.id(), value);
    }

    private enum Anchor { BOTTOM, RIGHT, TOP, TOP_RIGHT, CENTER }

    record HitArea(Layer layer, ScreenRectangle bounds) {}

    record Layer(String id, String title, Anchor anchor, int dx, int dy, Identifier... elements) {
        int anchorX(int width) {
            return (anchor == Anchor.RIGHT || anchor == Anchor.TOP_RIGHT ? width : width / 2) + dx;
        }

        int anchorY(int height) {
            return switch (anchor) {
                case BOTTOM -> height + dy;
                case TOP, TOP_RIGHT -> dy;
                case RIGHT, CENTER -> height / 2 + dy;
            };
        }
    }
}
