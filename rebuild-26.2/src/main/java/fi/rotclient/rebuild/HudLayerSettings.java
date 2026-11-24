package fi.rotclient.rebuild;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

final class HudLayerSettings {
    static final List<Layer> LAYERS = List.of(
            new Layer("hotbar", "Hotbar", VanillaHudElements.HOTBAR),
            new Layer("health", "Health Hearts", VanillaHudElements.HEALTH_BAR),
            new Layer("food", "Hunger", VanillaHudElements.FOOD_BAR),
            new Layer("armor", "Armor Icons", VanillaHudElements.ARMOR_BAR),
            new Layer("xp", "XP Bar / Level", VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.INFO_BAR),
            new Layer("air", "Air Bubbles", VanillaHudElements.AIR_BAR),
            new Layer("mount", "Mount Health", VanillaHudElements.MOUNT_HEALTH),
            new Layer("scoreboard", "Scoreboard", VanillaHudElements.SCOREBOARD),
            new Layer("boss", "Boss Bar", VanillaHudElements.BOSS_BAR),
            new Layer("action", "Action Bar", VanillaHudElements.OVERLAY_MESSAGE),
            new Layer("item_name", "Held Item Name", VanillaHudElements.HELD_ITEM_TOOLTIP),
            new Layer("effects", "Status Effects", VanillaHudElements.MOB_EFFECTS),
            new Layer("titles", "Titles", VanillaHudElements.TITLE_AND_SUBTITLE),
            new Layer("tab", "Player Tab List", VanillaHudElements.PLAYER_LIST));

    private HudLayerSettings() {}

    static void register() {
        for (Layer layer : LAYERS) {
            for (Identifier element : layer.elements()) {
                HudElementRegistry.replaceElement(element, original -> (graphics, delta) -> {
                    if (!RebuildClient.settings().hiddenVanillaLayers.contains(layer.id())) {
                        original.extractRenderState(graphics, delta);
                    }
                });
            }
        }
    }

    record Layer(String id, String title, Identifier... elements) {}
}
