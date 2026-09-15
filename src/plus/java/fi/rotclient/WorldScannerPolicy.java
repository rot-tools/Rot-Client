package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Crystal Hollows structure matching used by World Scanner, with the reviewed
 * quarter coordinate gates.
 */
public final class WorldScannerPolicy {
    public static final int MIN_ESP_RANGE = 8;
    public static final int MAX_ESP_RANGE = 128;
    public static final int DEFAULT_ESP_RANGE = 32;
    public static final int SCAN_CHUNKS_PER_TICK = 1;
    public static final int SCAN_RADIUS_CHUNKS = 8;
    public static final int STRUCTURE_Y_MIN = 0;
    public static final int STRUCTURE_Y_MAX = 188;
    public static final int MAX_FLUID_MARKERS = 96;
    public static final int FLUID_REFRESH_TICKS = 10;
    public static final int ESP_LABEL_ROW = 4;
    public static final float ESP_LABEL_SCALE_MIN = 2.4F;
    public static final float ESP_BEAM_WIDTH = 3.0F;
    public static final float ESP_TRACER_WIDTH = 2.5F;
    public static final float ESP_BOX_STROKE = 3.5F;

    public enum Quarter {
        NUCLEUS,
        JUNGLE,
        PRECURSOR,
        GOBLIN,
        MITHRIL,
        MAGMA,
        ANY
    }

    public record StructureDef(
            String name,
            Quarter quarter,
            int offsetX,
            int offsetY,
            int offsetZ,
            int colorArgb,
            List<String> sequence) {
        public StructureDef {
            name = name == null ? "" : name;
            quarter = quarter == null ? Quarter.ANY : quarter;
            sequence = sequence == null ? List.of() : List.copyOf(sequence);
        }

        public String triggerId() {
            for (String id : sequence) {
                if (id != null && !id.isBlank()) {
                    return normalize(id);
                }
            }
            return "";
        }
    }

    public record Hit(
            String name,
            int x,
            int y,
            int z,
            int colorArgb) {
    }

    private WorldScannerPolicy() {
    }

    public static int clampEspRange(int range) {
        return Math.max(MIN_ESP_RANGE, Math.min(MAX_ESP_RANGE, range));
    }

    public static String formatEspLabel(String displayName, double distanceBlocks) {
        String name = displayName == null || displayName.isBlank()
                ? "Waypoint"
                : displayName.trim();
        int meters = Math.max(0, (int) Math.round(distanceBlocks));
        return name + "  " + meters + "m";
    }

    public static float espLabelScale(float configured) {
        return Math.max(ESP_LABEL_SCALE_MIN, configured);
    }

    public static boolean shouldScan(
            boolean moduleEnabled,
            boolean onlyCrystalHollows,
            boolean inCrystalHollows) {
        if (!moduleEnabled) {
            return false;
        }
        if (onlyCrystalHollows && !inCrystalHollows) {
            return false;
        }
        return true;
    }

    /**
     * Queue already-loaded chunks when the module turns on or the player
     * enters Crystal Hollows. Scan them across ticks instead of hitching
     * the chunk-load thread.
     */
    public static boolean shouldRescanLoadedChunks(
            boolean moduleEnabled,
            boolean wasEnabled,
            boolean onlyCrystalHollows,
            boolean inCrystalHollows,
            boolean wasInCrystalHollows) {
        if (!moduleEnabled) {
            return false;
        }
        boolean canScan = !onlyCrystalHollows || inCrystalHollows;
        if (!canScan) {
            return false;
        }
        if (!wasEnabled) {
            return true;
        }
        return onlyCrystalHollows && inCrystalHollows && !wasInCrystalHollows;
    }

    public static boolean inQuarter(Quarter quarter, int x, int y, int z) {
        if (quarter == null || quarter == Quarter.ANY) {
            return true;
        }
        return switch (quarter) {
            case NUCLEUS -> x >= 449 && x < 577 && z >= 449 && z < 577;
            case JUNGLE -> x <= 576 && z <= 576;
            case PRECURSOR -> x > 448 && z > 448;
            case GOBLIN -> x <= 576 && z > 448;
            case MITHRIL -> x > 448 && z <= 576;
            case MAGMA -> y < 80;
            case ANY -> true;
        };
    }

    public static boolean sequenceMatches(List<String> column, List<String> expected) {
        if (expected == null || expected.isEmpty() || column == null) {
            return false;
        }
        if (column.size() < expected.size()) {
            return false;
        }
        for (int i = 0; i < expected.size(); i++) {
            String want = expected.get(i);
            if (want == null || want.isBlank()) {
                continue;
            }
            if (!normalize(want).equals(normalize(column.get(i)))) {
                return false;
            }
        }
        return true;
    }

    public static Optional<Hit> match(
            StructureDef structure,
            int x,
            int y,
            int z,
            List<String> columnUp) {
        if (structure == null || !inQuarter(structure.quarter(), x, y, z)) {
            return Optional.empty();
        }
        if (!sequenceMatches(columnUp, structure.sequence())) {
            return Optional.empty();
        }
        return Optional.of(new Hit(
                structure.name(),
                x + structure.offsetX(),
                y + structure.offsetY(),
                z + structure.offsetZ(),
                structure.colorArgb()));
    }

    public static boolean isFairyGrottoBlock(String blockId) {
        String id = normalize(blockId);
        return id.equals("amethyst_block") || id.equals("budding_amethyst");
    }

    public static boolean isWormFishingLava(int x, int y, int z, String blockId, String aboveId) {
        if (y <= 63 || !"lava".equals(normalize(blockId))) {
            return false;
        }
        if (!isAir(aboveId)) {
            return false;
        }
        return (x >= 564 && z >= 513) || (x >= 513 && z >= 564);
    }

    public static boolean isEspSurface(String blockId, String aboveId, String fluid) {
        if (!fluid.equals(normalize(blockId)) || !isAir(aboveId)) {
            return false;
        }
        return true;
    }

    static boolean isAir(String blockId) {
        String id = normalize(blockId);
        return id.isBlank() || id.equals("air") || id.equals("cave_air") || id.equals("void_air");
    }

    static String normalize(String id) {
        if (id == null) {
            return "";
        }
        String value = id.trim().toLowerCase(Locale.ROOT);
        int colon = value.indexOf(':');
        return colon >= 0 ? value.substring(colon + 1) : value;
    }

    public static List<StructureDef> crystalStructures() {
        return List.of(
                new StructureDef(
                        "Goblin King",
                        Quarter.GOBLIN,
                        1, -1, 2,
                        0xFFFFAA00,
                        List.of("gold_block", "polished_diorite", "polished_diorite", "polished_diorite")),
                new StructureDef(
                        "Goblin Queen",
                        Quarter.ANY,
                        0, 5, 0,
                        0xFFFFAA00,
                        List.of("diamond_block", "glass", "glass", "glass", "glass", "beacon")),
                new StructureDef(
                        "Mines of Divan",
                        Quarter.MITHRIL,
                        0, 5, 0,
                        0xFF55FF55,
                        List.of("quartz_block", "quartz_stairs", "chiseled_quartz_block", "quartz_pillar")),
                new StructureDef(
                        "Precursor City",
                        Quarter.PRECURSOR,
                        24, 0, -17,
                        0xFF55FFFF,
                        List.of(
                                "prismarine", "dark_prismarine", "dark_prismarine",
                                "dark_prismarine", "dark_prismarine", "sea_lantern",
                                "prismarine_bricks", "prismarine_bricks", "polished_diorite")),
                new StructureDef(
                        "Jungle Temple",
                        Quarter.ANY,
                        -45, 47, -18,
                        0xFFAA00AA,
                        List.of(
                                "clay", "clay", "clay", "clay", "diamond_block",
                                "sandstone", "sandstone", "sandstone",
                                "smooth_sandstone", "smooth_sandstone",
                                "red_sandstone", "red_sandstone", "gold_block")),
                new StructureDef(
                        "Khazad-dûm",
                        Quarter.MAGMA,
                        0, 1, 0,
                        0xFFFFAA00,
                        List.of(
                                "lava", "netherrack", "netherrack", "netherrack",
                                "netherrack", "netherrack", "netherrack",
                                "netherrack", "netherrack", "netherrack", "netherrack")));
    }

    public static List<StructureDef> mobSpotStructures() {
        return List.of(
                new StructureDef(
                        "Corleone Dock",
                        Quarter.MITHRIL,
                        23, 11, 17,
                        0xFF55FF55,
                        List.of(
                                "prismarine", "prismarine", "prismarine", "prismarine",
                                "", "", "", "", "", "", "", "", "", "", "", "",
                                "", "", "", "", "", "", "", "",
                                "prismarine", "prismarine", "cobblestone", "prismarine")),
                new StructureDef(
                        "Corleone Hole",
                        Quarter.MITHRIL,
                        -18, -1, 29,
                        0xFF55FF55,
                        List.of("cobblestone", "prismarine_bricks", "prismarine", "oak_planks")),
                new StructureDef(
                        "Key Guardian Tower",
                        Quarter.JUNGLE,
                        0, 0, 0,
                        0xFFAA00AA,
                        List.of("diamond_block", "oak_planks", "jungle_log")),
                new StructureDef(
                        "Xalx",
                        Quarter.GOBLIN,
                        -2, 1, -2,
                        0xFF55FF55,
                        List.of(
                                "diamond_block", "anvil", "cobblestone", "iron_block",
                                "air", "air", "air", "air", "air", "air", "air")),
                new StructureDef(
                        "Pete",
                        Quarter.GOBLIN,
                        0, 0, 0,
                        0xFFFFAA00,
                        List.of(
                                "barrel", "cobblestone", "oak_fence",
                                "air", "air", "air", "air", "air", "air", "air")),
                new StructureDef(
                        "Odawa",
                        Quarter.JUNGLE,
                        0, 0, 0,
                        0xFF55FF55,
                        List.of("jungle_log", "vine", "vine", "jungle_log")),
                new StructureDef(
                        "Golden Dragon",
                        Quarter.ANY,
                        0, -3, 5,
                        0xFFFFFFFF,
                        List.of(
                                "diamond_block", "stone", "stone", "stone",
                                "gold_ore", "gold_block")));
    }

    public static Set<String> triggerBlocks(
            List<StructureDef> structures,
            boolean fairy,
            boolean worm,
            boolean fluids) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (structures != null) {
            for (StructureDef structure : structures) {
                String trigger = structure.triggerId();
                if (!trigger.isEmpty()) {
                    ids.add(trigger);
                }
            }
        }
        if (fairy) {
            ids.add("amethyst_block");
            ids.add("budding_amethyst");
        }
        if (worm) {
            ids.add("lava");
        }
        if (fluids) {
            ids.add("lava");
            ids.add("water");
        }
        return Set.copyOf(ids);
    }

    public static boolean isTriggerBlock(String blockId, Set<String> triggers) {
        return triggers != null && triggers.contains(normalize(blockId));
    }

    public static boolean secondBlockMatches(String aboveId, List<String> expected) {
        if (expected == null || expected.size() < 2) {
            return true;
        }
        String want = expected.get(1);
        if (want == null || want.isBlank()) {
            return true;
        }
        return normalize(want).equals(normalize(aboveId));
    }

    public static List<StructureDef> matchingStructures(
            List<StructureDef> structures,
            String triggerId) {
        if (structures == null || structures.isEmpty()) {
            return List.of();
        }
        String trigger = normalize(triggerId);
        if (trigger.isEmpty()) {
            return List.of();
        }
        List<StructureDef> matches = new ArrayList<>();
        for (StructureDef structure : structures) {
            if (trigger.equals(structure.triggerId())) {
                matches.add(structure);
            }
        }
        return matches;
    }

    public static int clampNameScale(float scale) {
        if (scale < 0.4F) {
            return 1;
        }
        return Math.max(1, Math.min(4, Math.round(scale)));
    }
}
