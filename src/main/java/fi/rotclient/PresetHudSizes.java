package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How much screen each movable HUD element occupies, for laying out example
 * profiles only.
 *
 * These are footprints, not the HUD editor's drag boxes: the Dungeon HUD, for
 * example, draws a 128px map beside its text panel and a score overlay below
 * it, so its footprint is much larger than the panel. Sizes are in GUI-scaled
 * pixels, like the poses they are used to compute.
 *
 * They are deliberately a little generous. A HUD that is smaller than listed
 * just leaves a small gap; one that is bigger would overlap its neighbour.
 * The Custom Scoreboard has no entry: it aligns itself to the right edge and
 * example layouts never move it.
 */
final class PresetHudSizes {
    /** Standalone Mining Tracker HUD (profile settings, not a QoL pose). */
    static final String MINING_TRACKER = "mining_tracker";

    /** Standalone Powder Chest HUD (profile settings, not a QoL pose). */
    static final String POWDER_CHEST = "powder_chest";

    private static final Map<String, PresetHudLayout.Size> SIZES = build();

    private PresetHudSizes() {
    }

    static PresetHudLayout.Size of(String id) {
        return id == null ? null : SIZES.get(id);
    }

    static boolean isKnown(String id) {
        return of(id) != null;
    }

    /** Every element id that has a footprint. */
    static java.util.Set<String> ids() {
        return SIZES.keySet();
    }

    private static Map<String, PresetHudLayout.Size> build() {
        Map<String, PresetHudLayout.Size> sizes = new LinkedHashMap<>();
        put(sizes, "pet", 180, 48);
        put(sizes, "performance", 120, 36);
        put(sizes, "commission", 180, 48);

        // Player Display: one 110x14 line per stat.
        for (String stat : new String[] {
                "health", "mana", "overflow", "defense",
                "vitality", "ehp", "speed"}) {
            put(sizes, stat, 110, 14);
        }

        put(sizes, "slayer", 156, 58);
        put(sizes, "slayer_progress", 156, 48);
        put(sizes, "slayer_rng", 156, 48);
        put(sizes, "slayer_stats", 156, 68);
        put(sizes, "slayer_carry", 156, 68);
        put(sizes, "slayer_cocoon", 156, 36);
        put(sizes, "slayer_attunement", 156, 36);
        put(sizes, "slayer_vengeance", 156, 36);

        // 156 panel + 4 gap + 128 map, and room for the score overlay below.
        put(sizes, "dungeon", 288, 150);
        put(sizes, "dungeon_watcher", 156, 68);

        put(sizes, "fishing", 156, 68);
        put(sizes, "mining", 156, 68);

        // 172 wide. 223 tall for the default Gold target, plus 34 for every
        // extra material row of other targets, so keep it alone in its column.
        put(sizes, MINING_TRACKER, 172, 232);
        // 268 wide; 80 tall plus 14 per item row, so put it last in its stack.
        put(sizes, POWDER_CHEST, 268, 108);
        return Map.copyOf(sizes);
    }

    private static void put(
            Map<String, PresetHudLayout.Size> sizes,
            String id,
            int width,
            int height) {
        sizes.put(id, new PresetHudLayout.Size(width, height));
    }
}
