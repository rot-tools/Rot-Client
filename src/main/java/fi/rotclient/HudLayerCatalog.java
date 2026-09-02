package fi.rotclient;

import java.util.List;

/**
 * Canonical HUD pieces for the layout editor: vanilla/Hypixel layers (hide)
 * plus Rot overlays (show). Other SkyBlock clients keep one registry instead
 * of scattering hides across Player Display and Render Optimizer.
 */
public final class HudLayerCatalog {
    public enum Kind {
        VANILLA,
        ROT
    }

    public record Layer(String id, String settingId, String label, Kind kind, boolean moduleToggle) {
    }

    private HudLayerCatalog() {
    }

    public static List<Layer> vanillaLayers() {
        return List.of(
                layer("hotbar", "Hide Hotbar", Kind.VANILLA),
                layer("health", "Hide Health Hearts", Kind.VANILLA),
                layer("food", "Hide Hunger", Kind.VANILLA),
                layer("armor", "Hide Armor Icons", Kind.VANILLA),
                layer("xp", "Hide XP Bar / Level", Kind.VANILLA),
                layer("air", "Hide Air Bubbles", Kind.VANILLA),
                layer("mount", "Hide Mount Health", Kind.VANILLA),
                layer("scoreboard", "Hide Scoreboard", Kind.VANILLA),
                layer("boss", "Hide Boss Bar", Kind.VANILLA),
                layer("action", "Hide Action Bar", Kind.VANILLA),
                layer("item_name", "Hide Held Item Name", Kind.VANILLA),
                layer("effects", "Hide Status Effects", Kind.VANILLA),
                layer("titles", "Hide Titles", Kind.VANILLA),
                layer("tab", "Hide Player Tab List", Kind.VANILLA));
    }

    public static List<Layer> rotOverlays() {
        return List.of(
                rotModule("qol.performance_hud", "Performance HUD"),
                rotSetting("qol.player_display.health_hud", "Health HUD"),
                rotSetting("qol.player_display.mana_hud", "Mana HUD"),
                rotSetting("qol.player_display.overflow_mana_hud", "Overflow HUD"),
                rotSetting("qol.player_display.defense_hud", "Defense HUD"),
                rotSetting("qol.player_display.vitality_hud", "Vitality HUD"),
                rotSetting("qol.player_display.ehp_hud", "EHP HUD"),
                rotSetting("qol.player_display.speed_hud", "Speed HUD"),
                rotModule("qol.pet_hud", "Pet HUD"),
                rotModule("qol.commission_display", "Commission Display"),
                rotSetting("qol.auto_clicker.cps_hud", "Auto Clicker CPS"),
                rotSetting("qol.fishing_helper.hook_timer_hud", "Fishing HUD"),
                rotModule("qol.dungeon_hud", "Dungeon HUD"),
                rotModule("qol.slayer_display", "Slayer Display"),
                rotModule("qol.slayer_progress", "Slayer Progress"),
                rotSetting("qol.slayer_drops.rng_hud", "RNG Meter"),
                rotSetting("qol.slayer_drops.profit_hud", "Slayer Item Profit"),
                rotModule("qol.slayer_stats", "Slayer Stats"),
                rotSetting("qol.mining_helpers.ability_hud", "Mining HUD"),
                rotModule("qol.diana_burrows", "Diana HUD"),
                rotSetting("qol.foraging_trees.progress_hud", "Foraging HUD"));
    }

    public static String settingId(String layerId) {
        return "qol.hud_layout.hide_" + layerId;
    }

    public static List<HudElementCatalog.InspectorToggle> inspectorToggles() {
        java.util.ArrayList<HudElementCatalog.InspectorToggle> out = new java.util.ArrayList<>();
        for (Layer layer : vanillaLayers()) {
            out.add(new HudElementCatalog.InspectorToggle(layer.settingId(), layer.label()));
        }
        for (Layer layer : rotOverlays()) {
            out.add(new HudElementCatalog.InspectorToggle(layer.settingId(), layer.label()));
        }
        return List.copyOf(out);
    }

    private static Layer layer(String id, String label, Kind kind) {
        return new Layer(id, settingId(id), label, kind, false);
    }

    private static Layer rotModule(String settingId, String label) {
        return new Layer(settingId, settingId, label, Kind.ROT, true);
    }

    private static Layer rotSetting(String settingId, String label) {
        return new Layer(settingId, settingId, label, Kind.ROT, false);
    }
}
