package fi.rotclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wave-2 mining assists from shelved leftovers.
 * Policy only while Diana and Foraging own catalog/config/extras.
 */
public final class MiningAssistPolicy {
    public static final double DETECTOR_TOLERANCE = 0.25D;
    public static final int DETECTOR_RENDER_MAX = 8;
    public static final int FOSSIL_COLUMNS = 9;
    public static final int FOSSIL_ROWS = 6;
    public static final int FOSSIL_SLOTS = FOSSIL_COLUMNS * FOSSIL_ROWS;
    public static final String HOTM_SHARE_PREFIX = "FIRMHOTM/";
    public static final String COMPASS_FAIL =
            "The Wishing Compass can't seem to locate anything!";
    public static final double COMPASS_LINE_GAP = 5.0D;
    public static final int PARTICLES_PER_LINE = 25;
    public static final double INSTANT_SOFTCAP = 6.6666665D;

    public record BlockPos(int x, int y, int z) {
        public double distance(double px, double py, double pz) {
            double dx = x + 0.5D - px;
            double dy = y + 0.5D - py;
            double dz = z + 0.5D - pz;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public record Vec3(double x, double y, double z) {
        public Vec3 add(Vec3 other) {
            return new Vec3(x + other.x, y + other.y, z + other.z);
        }

        public Vec3 scale(double t) {
            return new Vec3(x * t, y * t, z * t);
        }

        public double distance(Vec3 other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public record Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }

        public Aabb inflate(double xz, double y) {
            return new Aabb(minX - xz, minY - y, minZ - xz, maxX + xz, maxY + y, maxZ + xz);
        }
    }

    public enum HollowsZone {
        CRYSTAL_NUCLEUS(new Aabb(462, 63, 461, 564, 181, 565)),
        JUNGLE(new Aabb(201, 63, 201, 513, 189, 513)),
        MITHRIL_DEPOSITS(new Aabb(512, 63, 201, 824, 189, 513)),
        GOBLIN_HOLDOUT(new Aabb(201, 63, 512, 513, 189, 824)),
        PRECURSOR_REMNANTS(new Aabb(512, 63, 512, 824, 189, 824)),
        MAGMA_FIELDS(new Aabb(201, 30, 201, 824, 64, 824));

        private final Aabb aabb;

        HollowsZone(Aabb aabb) {
            this.aabb = aabb;
        }

        public Aabb aabb() {
            return aabb;
        }
    }

    public enum FossilTile {
        EMPTY,
        FOSSIL,
        UNKNOWN
    }

    public enum TunnelKind {
        CAMPFIRE,
        FAIRY,
        OLD_GEM,
        NEW_GEM,
        COMMISSION
    }

    public record GraphNode(String id, int x, int y, int z, String name) {
    }

    public record GraphEdge(String from, String to, double weight) {
    }

    private static final Pattern TREASURE = Pattern.compile(
            "TREASURE:\\s*(?<meters>\\d+(?:\\.\\d+)?)m", Pattern.CASE_INSENSITIVE);
    private static final Pattern KEEPER = Pattern.compile(
            "Keeper of\\s+(?<name>\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FOSSIL_PROGRESS = Pattern.compile(
            "Fossil Excavation Progress:\\s*(?<pct>\\d{1,2}.\\d)%");
    private static final Pattern FOSSIL_CHARGES = Pattern.compile(
            "Chisel Charges Remaining:\\s*(?<n>\\d{1,2})");
    private static final Pattern MINING_SPEED = Pattern.compile(
            "Mining Speed:\\s*.*?(\\d+)");
    private static final Pattern BREAKING_POWER = Pattern.compile(
            "Breaking Power\\s+(\\d+)");
    private static final Pattern HOTM_TIER = Pattern.compile("Tier\\s+(?<tier>[0-9]+)");
    private static final Pattern COLLECTOR = Pattern.compile(
            "(?<what>.+?)\\s+Collector", Pattern.CASE_INSENSITIVE);
    private static final Pattern OLD_GEM = Pattern.compile(
            "(Ruby|Amethyst|Jade|Sapphire|Amber|Topaz)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NEW_GEM = Pattern.compile(
            "(Aquamarine|Onyx|Citrine|Peridot)", Pattern.CASE_INSENSITIVE);

    private static final int[][] CHEST_OFFSETS = {
            {-38, -22, 26}, {38, -22, -26}, {-40, -22, 18}, {-41, -20, 22}, {-5, -21, 16},
            {40, -22, -30}, {-42, -20, -28}, {-43, -22, -40}, {42, -19, -41}, {43, -21, -16},
            {-1, -22, -20}, {6, -21, 28}, {7, -21, 11}, {7, -21, 22}, {-12, -21, -44},
            {12, -22, 31}, {12, -22, -22}, {12, -21, 7}, {12, -21, -43}, {-14, -21, 43},
            {-14, -21, 22}, {-17, -21, 20}, {-20, -22, 0}, {1, -21, 20}, {19, -22, 29},
            {20, -22, 0}, {20, -21, -26}, {-23, -22, 40}, {22, -21, -14}, {-24, -22, 12},
            {23, -22, 26}, {23, -22, -39}, {24, -22, 27}, {25, -22, 17}, {29, -21, -44},
            {-31, -21, -12}, {-31, -21, -40}, {30, -21, -25}, {-32, -21, -40}, {-36, -20, 42},
            {-37, -21, -14}, {-37, -21, -22}
    };

    private static final Map<String, Set<String>> COMMISSION_BLOCKS = commissionBlocks();

    private MiningAssistPolicy() {
    }

    public static Optional<Double> parseTreasureMeters(String actionBar) {
        Matcher matcher = TREASURE.matcher(strip(actionBar));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Double.parseDouble(matcher.group("meters")));
    }

    public static Optional<String> parseKeeper(String nametag) {
        Matcher matcher = KEEPER.matcher(strip(nametag));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group("name"));
    }

    public static Optional<BlockPos> minesCenter(String keeper, int standX, int standY, int standZ) {
        if (keeper == null) {
            return Optional.empty();
        }
        return switch (keeper.trim().toLowerCase(Locale.ROOT)) {
            case "diamond" -> Optional.of(new BlockPos(standX + 33, standY, standZ + 3));
            case "lapis" -> Optional.of(new BlockPos(standX - 33, standY, standZ - 3));
            case "emerald" -> Optional.of(new BlockPos(standX - 3, standY, standZ + 33));
            case "gold" -> Optional.of(new BlockPos(standX + 3, standY, standZ - 33));
            default -> Optional.empty();
        };
    }

    public static List<BlockPos> knownChests(BlockPos center) {
        List<BlockPos> out = new ArrayList<>();
        if (center == null) {
            return out;
        }
        for (int[] offset : CHEST_OFFSETS) {
            out.add(new BlockPos(
                    center.x() + offset[0],
                    center.y() + offset[1] + 1,
                    center.z() + offset[2]));
        }
        return out;
    }

    public static List<BlockPos> filterByDistance(
            List<BlockPos> candidates,
            double playerX,
            double playerY,
            double playerZ,
            double meters) {
        List<BlockPos> out = new ArrayList<>();
        if (candidates == null) {
            return out;
        }
        for (BlockPos pos : candidates) {
            if (Math.abs(pos.distance(playerX, playerY, playerZ) - meters) < DETECTOR_TOLERANCE) {
                out.add(pos);
            }
        }
        return out;
    }

    public static boolean shouldRenderDetector(List<BlockPos> candidates) {
        return candidates != null
                && !candidates.isEmpty()
                && candidates.size() <= DETECTOR_RENDER_MAX;
    }

    public static boolean isDetectorFoundChat(String chat) {
        String text = strip(chat).toLowerCase(Locale.ROOT);
        return text.contains("you found") && text.contains("metal detector");
    }

    public static HollowsZone zoneAt(double x, double y, double z) {
        for (HollowsZone zone : HollowsZone.values()) {
            if (zone.aabb().contains(x, y, z)) {
                return zone;
            }
        }
        return null;
    }

    public static boolean isCompassFail(String chat) {
        return COMPASS_FAIL.equalsIgnoreCase(strip(chat));
    }

    public static Optional<Vec3> intersectCompassLines(
            Vec3 startA,
            Vec3 dirA,
            Vec3 startB,
            Vec3 dirB) {
        if (startA == null || dirA == null || startB == null || dirB == null) {
            return Optional.empty();
        }
        Vec3 w = new Vec3(startA.x - startB.x, startA.y - startB.y, startA.z - startB.z);
        double a = dot(dirA, dirA);
        double b = dot(dirA, dirB);
        double c = dot(dirB, dirB);
        double d = dot(dirA, w);
        double e = dot(dirB, w);
        double denom = a * c - b * b;
        if (Math.abs(denom) < 1.0E-8D) {
            return Optional.empty();
        }
        double t = (b * e - c * d) / denom;
        double s = (a * e - b * d) / denom;
        Vec3 closeA = startA.add(dirA.scale(t));
        Vec3 closeB = startB.add(dirB.scale(s));
        if (closeA.distance(closeB) >= COMPASS_LINE_GAP) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(
                (closeA.x + closeB.x) / 2.0D,
                (closeA.y + closeB.y) / 2.0D,
                (closeA.z + closeB.z) / 2.0D));
    }

    public static Vec3 jungleTempleFromJungleGuess(Vec3 guess) {
        if (guess == null) {
            return null;
        }
        return new Vec3(guess.x - 57.0D, guess.y + 36.0D, guess.z - 21.0D);
    }

    public static boolean compassTargetInZone(HollowsZone zone, Vec3 point) {
        return zone != null && point != null && zone.aabb().inflate(100.0D, 0.0D).contains(point.x, point.y, point.z);
    }

    public static Optional<Double> parseFossilProgress(String lore) {
        Matcher matcher = FOSSIL_PROGRESS.matcher(strip(lore));
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(matcher.group("pct")));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<Integer> parseChiselCharges(String lore) {
        Matcher matcher = FOSSIL_CHARGES.matcher(strip(lore));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group("n")));
    }

    public static FossilTile tileFromItem(String itemId) {
        String id = itemId == null ? "" : itemId.toLowerCase(Locale.ROOT);
        if (id.contains("white_stained_glass_pane")) {
            return FossilTile.FOSSIL;
        }
        if (id.contains("brown_stained_glass_pane")) {
            return FossilTile.UNKNOWN;
        }
        return FossilTile.EMPTY;
    }

    public static double[] fossilHeatmap(FossilTile[] board, String percentText) {
        double[] heat = new double[FOSSIL_SLOTS];
        if (board == null || board.length != FOSSIL_SLOTS) {
            return heat;
        }
        List<boolean[]> valid = validFossilPlacements(board, percentText);
        if (valid.isEmpty()) {
            return heat;
        }
        int[] hits = new int[FOSSIL_SLOTS];
        for (boolean[] mask : valid) {
            for (int i = 0; i < FOSSIL_SLOTS; i++) {
                if (mask[i] && board[i] == FossilTile.UNKNOWN) {
                    hits[i]++;
                }
            }
        }
        for (int i = 0; i < FOSSIL_SLOTS; i++) {
            heat[i] = hits[i] / (double) valid.size();
        }
        return heat;
    }

    public static int bestFossilSlot(double[] heatmap) {
        int best = -1;
        double peak = 0.0D;
        if (heatmap == null) {
            return -1;
        }
        for (int i = 0; i < heatmap.length; i++) {
            if (heatmap[i] > peak) {
                peak = heatmap[i];
                best = i;
            }
        }
        return peak > 0.0D ? best : -1;
    }

    public static Set<String> blocksForCommission(String name) {
        String key = normalizeCommission(name);
        Set<String> blocks = COMMISSION_BLOCKS.get(key);
        return blocks == null ? Set.of() : blocks;
    }

    public static boolean isCommissionOre(List<String> activeCommissions, String blockId) {
        if (activeCommissions == null || blockId == null) {
            return false;
        }
        String id = normalizeBlock(blockId);
        for (String commission : activeCommissions) {
            if (blocksForCommission(commission).contains(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean greyscaleCommissionOre(List<String> activeIncomplete, String blockId) {
        if (blockId == null || !isMiningOreBlock(blockId)) {
            return false;
        }
        return !isCommissionOre(activeIncomplete, blockId);
    }

    public static Optional<Integer> parseMiningSpeed(String tabOrLore) {
        Matcher matcher = MINING_SPEED.matcher(strip(tabOrLore));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group(1)));
    }

    public static Optional<Integer> parseBreakingPower(String lore) {
        Matcher matcher = BREAKING_POWER.matcher(strip(lore));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group(1)));
    }

    public static int oreStrength(String blockId) {
        String id = normalizeBlock(blockId);
        return switch (id) {
            case "gray_wool", "cyan_terracotta" -> 500;
            case "prismarine", "prismarine_bricks", "dark_prismarine" -> 800;
            case "light_blue_wool" -> 1500;
            case "polished_diorite" -> 2000;
            case "red_stained_glass", "red_stained_glass_pane" -> 2300;
            case "orange_stained_glass", "orange_stained_glass_pane",
                 "purple_stained_glass", "purple_stained_glass_pane",
                 "lime_stained_glass", "lime_stained_glass_pane",
                 "light_blue_stained_glass", "light_blue_stained_glass_pane",
                 "white_stained_glass", "white_stained_glass_pane" -> 3000;
            case "yellow_stained_glass", "yellow_stained_glass_pane" -> 3800;
            case "pink_stained_glass", "pink_stained_glass_pane" -> 4800;
            case "blue_stained_glass", "blue_stained_glass_pane",
                 "brown_stained_glass", "brown_stained_glass_pane",
                 "black_stained_glass", "black_stained_glass_pane",
                 "green_stained_glass", "green_stained_glass_pane" -> 5200;
            case "terracotta", "brown_terracotta", "smooth_red_sandstone" -> 5600;
            case "infested_cobblestone", "cobblestone", "cobblestone_slab",
                 "cobblestone_stairs", "clay" -> 5600;
            case "packed_ice" -> 6000;
            default -> -1;
        };
    }

    public static int requiredBreakingPower(String blockId) {
        int strength = oreStrength(blockId);
        if (strength >= 5200) {
            return 9;
        }
        if (strength >= 3000) {
            return 8;
        }
        if (strength >= 2000) {
            return 7;
        }
        if (strength >= 1500) {
            return 6;
        }
        return strength > 0 ? 5 : 0;
    }

    public static double breakTimeMs(int strength, int miningSpeed) {
        if (strength <= 0 || miningSpeed <= 0) {
            return -1.0D;
        }
        double capped = Math.min(strength * 30.0D / miningSpeed, INSTANT_SOFTCAP * strength);
        return 50.0D * capped;
    }

    public static int ticksToBreak(int strength, int miningSpeed) {
        double ms = breakTimeMs(strength, miningSpeed);
        if (ms < 0.0D) {
            return -1;
        }
        if (miningSpeed >= strength * 60) {
            return 1;
        }
        if (miningSpeed >= Math.ceil(INSTANT_SOFTCAP * strength)) {
            return 4;
        }
        return Math.max(1, (int) Math.round(strength * 30.0D / miningSpeed));
    }

    public static boolean canBreakPredicted(String blockId, int toolBreakingPower) {
        return toolBreakingPower >= requiredBreakingPower(blockId) && oreStrength(blockId) > 0;
    }

    public static Optional<Integer> parseHotmTier(String name) {
        Matcher matcher = HOTM_TIER.matcher(strip(name));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group("tier")));
    }

    public static boolean isHotmPreset(String raw) {
        return raw != null && strip(raw).startsWith(HOTM_SHARE_PREFIX);
    }

    public static String encodeHotmPreset(List<String> perkNames) {
        StringBuilder json = new StringBuilder(HOTM_SHARE_PREFIX);
        json.append('[');
        if (perkNames != null) {
            for (int i = 0; i < perkNames.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append('"').append(perkNames.get(i).replace("\"", "")).append('"');
            }
        }
        json.append(']');
        return json.toString();
    }

    public static List<String> parseHotmPreset(String raw) {
        List<String> out = new ArrayList<>();
        if (!isHotmPreset(raw)) {
            return out;
        }
        String body = strip(raw).substring(HOTM_SHARE_PREFIX.length()).trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        if (body.isBlank()) {
            return out;
        }
        for (String part : body.split(",")) {
            String name = part.trim().replace("\"", "");
            if (!name.isBlank()) {
                out.add(name);
            }
        }
        return out;
    }

    public static boolean highlightHotmPerk(Set<String> preset, String slotName) {
        if (preset == null || slotName == null) {
            return false;
        }
        String name = strip(slotName);
        for (String perk : preset) {
            if (name.equalsIgnoreCase(perk) || name.toLowerCase(Locale.ROOT).contains(perk.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static TunnelKind tunnelKind(String nodeName) {
        String name = strip(nodeName);
        if (name.toLowerCase(Locale.ROOT).contains("campfire")) {
            return TunnelKind.CAMPFIRE;
        }
        if (name.toLowerCase(Locale.ROOT).contains("fairy")) {
            return TunnelKind.FAIRY;
        }
        if (NEW_GEM.matcher(name).find()) {
            return TunnelKind.NEW_GEM;
        }
        if (OLD_GEM.matcher(name).find()) {
            return TunnelKind.OLD_GEM;
        }
        return TunnelKind.COMMISSION;
    }

    public static Optional<String> collectorGoal(String commissionName) {
        Matcher matcher = COLLECTOR.matcher(strip(commissionName));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group("what").trim());
    }

    public static List<String> shortestPath(
            List<GraphNode> nodes,
            List<GraphEdge> edges,
            String startId,
            String goalId) {
        if (nodes == null || edges == null || startId == null || goalId == null) {
            return List.of();
        }
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> remaining = new HashSet<>();
        for (GraphNode node : nodes) {
            dist.put(node.id(), Double.POSITIVE_INFINITY);
            remaining.add(node.id());
        }
        dist.put(startId, 0.0D);
        Map<String, List<GraphEdge>> adj = new HashMap<>();
        for (GraphEdge edge : edges) {
            adj.computeIfAbsent(edge.from(), key -> new ArrayList<>()).add(edge);
            adj.computeIfAbsent(edge.to(), key -> new ArrayList<>())
                    .add(new GraphEdge(edge.to(), edge.from(), edge.weight()));
        }
        while (!remaining.isEmpty()) {
            String current = null;
            double best = Double.POSITIVE_INFINITY;
            for (String id : remaining) {
                double value = dist.getOrDefault(id, Double.POSITIVE_INFINITY);
                if (value < best) {
                    best = value;
                    current = id;
                }
            }
            if (current == null || best == Double.POSITIVE_INFINITY) {
                break;
            }
            remaining.remove(current);
            if (current.equals(goalId)) {
                break;
            }
            for (GraphEdge edge : adj.getOrDefault(current, List.of())) {
                if (!remaining.contains(edge.to())) {
                    continue;
                }
                double next = best + edge.weight();
                if (next < dist.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    dist.put(edge.to(), next);
                    prev.put(edge.to(), current);
                }
            }
        }
        if (!prev.containsKey(goalId) && !startId.equals(goalId)) {
            return List.of();
        }
        List<String> path = new ArrayList<>();
        String walk = goalId;
        path.add(walk);
        while (!walk.equals(startId)) {
            walk = prev.get(walk);
            if (walk == null) {
                return List.of();
            }
            path.add(0, walk);
        }
        return path;
    }

    public static String nearestNode(List<GraphNode> nodes, int x, int y, int z) {
        String best = "";
        double bestDist = Double.POSITIVE_INFINITY;
        if (nodes == null) {
            return best;
        }
        for (GraphNode node : nodes) {
            double dx = node.x() - x;
            double dy = node.y() - y;
            double dz = node.z() - z;
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                best = node.id();
            }
        }
        return best;
    }

    private static List<boolean[]> validFossilPlacements(FossilTile[] board, String percentText) {
        List<boolean[]> valid = new ArrayList<>();
        String pct = percentText == null ? "" : percentText.trim();
        for (FossilDef fossil : fossils()) {
            if (!pct.isBlank() && !pct.equals(fossil.percent)) {
                continue;
            }
            for (boolean[][] shape : fossil.shapes()) {
                int height = shape.length;
                int width = shape[0].length;
                for (int row = 0; row <= FOSSIL_ROWS - height; row++) {
                    for (int col = 0; col <= FOSSIL_COLUMNS - width; col++) {
                        boolean[] mask = new boolean[FOSSIL_SLOTS];
                        boolean ok = true;
                        for (int r = 0; r < height && ok; r++) {
                            for (int c = 0; c < width; c++) {
                                int index = (row + r) * FOSSIL_COLUMNS + (col + c);
                                if (shape[r][c]) {
                                    mask[index] = true;
                                    if (board[index] == FossilTile.EMPTY) {
                                        ok = false;
                                        break;
                                    }
                                } else if (board[index] == FossilTile.FOSSIL) {
                                    ok = false;
                                    break;
                                }
                            }
                        }
                        if (!ok) {
                            continue;
                        }
                        for (int i = 0; i < FOSSIL_SLOTS; i++) {
                            if (board[i] == FossilTile.FOSSIL && !mask[i]) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) {
                            valid.add(mask);
                        }
                    }
                }
            }
        }
        return valid;
    }

    private static FossilDef[] fossils() {
        return new FossilDef[] {
                new FossilDef("7.7", new String[] {
                        ".#....", "####..", ".##.#.", ".#.#.#", "..#.#."
                }, true, true),
                new FossilDef("12.5", new String[] {
                        "....#", ".#..#", "#...#", ".#.#.", "..#.."
                }, true, false),
                new FossilDef("6.2", new String[] {
                        "..##..", ".####.", "######", ".####."
                }, true, false),
                new FossilDef("7.1", new String[] {
                        "#####", "#...#", "#.#.#", "#.###"
                }, true, false),
                new FossilDef("10", new String[] {
                        "...#...", "#..#..#", ".#.#.#.", "..###.."
                }, false, true),
                new FossilDef("7.7", new String[] {
                        "#.#.#", "#.#.#", ".###.", ".###.", "..#.."
                }, true, false),
                new FossilDef("9.1", new String[] {
                        "......##", ".#....##", "#....#..", ".####..."
                }, false, true),
                new FossilDef("8.3", new String[] {
                        "..##..", ".####.", "######"
                }, true, false)
        };
    }

    private static Map<String, Set<String>> commissionBlocks() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("mithril everywhere", Set.of(
                "gray_wool", "cyan_terracotta", "prismarine", "prismarine_bricks",
                "dark_prismarine", "light_blue_wool"));
        map.put("amber gemstone collector", Set.of("orange_stained_glass", "orange_stained_glass_pane"));
        map.put("topaz gemstone collector", Set.of("yellow_stained_glass", "yellow_stained_glass_pane"));
        map.put("amethyst gemstone collector", Set.of("purple_stained_glass", "purple_stained_glass_pane"));
        map.put("ruby gemstone collector", Set.of("red_stained_glass", "red_stained_glass_pane"));
        map.put("jade gemstone collector", Set.of("lime_stained_glass", "lime_stained_glass_pane"));
        map.put("sapphire gemstone collector", Set.of("light_blue_stained_glass", "light_blue_stained_glass_pane"));
        map.put("peridot gemstone collector", Set.of("green_stained_glass", "green_stained_glass_pane"));
        map.put("aquamarine gemstone collector", Set.of("blue_stained_glass", "blue_stained_glass_pane"));
        map.put("citrine gemstone collector", Set.of("brown_stained_glass", "brown_stained_glass_pane"));
        map.put("onyx gemstone collector", Set.of("black_stained_glass", "black_stained_glass_pane"));
        map.put("glacite collector", Set.of("packed_ice"));
        map.put("umber collector", Set.of("terracotta", "brown_terracotta", "smooth_red_sandstone"));
        map.put("tungsten collector", Set.of(
                "infested_cobblestone", "cobblestone", "cobblestone_slab", "cobblestone_stairs", "clay"));
        return map;
    }

    private static boolean isMiningOreBlock(String blockId) {
        String id = normalizeBlock(blockId);
        for (Set<String> blocks : COMMISSION_BLOCKS.values()) {
            if (blocks.contains(id)) {
                return true;
            }
        }
        return oreStrength(id) > 0;
    }

    private static String normalizeCommission(String name) {
        return strip(name).toLowerCase(Locale.ROOT);
    }

    private static String normalizeBlock(String id) {
        String value = id == null ? "" : id.toLowerCase(Locale.ROOT);
        int colon = value.indexOf(':');
        return colon >= 0 ? value.substring(colon + 1) : value;
    }

    private static double dot(Vec3 a, Vec3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    private static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return ChatTextPolicy.stripFormatting(raw).trim();
    }

    private record FossilDef(String percent, String[] rows, boolean rotate, boolean flip) {
        List<boolean[][]> shapes() {
            boolean[][] base = parse(rows);
            List<boolean[][]> out = new ArrayList<>();
            addUnique(out, base);
            if (rotate) {
                boolean[][] r90 = rotate90(base);
                boolean[][] r180 = rotate90(r90);
                boolean[][] r270 = rotate90(r180);
                addUnique(out, r90);
                addUnique(out, r180);
                addUnique(out, r270);
            }
            if (flip) {
                boolean[][] flipped = flipVert(base);
                addUnique(out, flipped);
                if (rotate) {
                    boolean[][] f90 = rotate90(flipped);
                    addUnique(out, f90);
                    addUnique(out, rotate90(f90));
                    addUnique(out, rotate90(rotate90(f90)));
                }
            }
            return out;
        }

        private static boolean[][] parse(String[] rows) {
            boolean[][] grid = new boolean[rows.length][rows[0].length()];
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length(); c++) {
                    grid[r][c] = rows[r].charAt(c) == '#';
                }
            }
            return grid;
        }

        private static boolean[][] rotate90(boolean[][] grid) {
            int height = grid.length;
            int width = grid[0].length;
            boolean[][] out = new boolean[width][height];
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    out[c][height - 1 - r] = grid[r][c];
                }
            }
            return out;
        }

        private static boolean[][] flipVert(boolean[][] grid) {
            boolean[][] out = new boolean[grid.length][grid[0].length];
            for (int r = 0; r < grid.length; r++) {
                out[grid.length - 1 - r] = Arrays.copyOf(grid[r], grid[r].length);
            }
            return out;
        }

        private static void addUnique(List<boolean[][]> out, boolean[][] shape) {
            for (boolean[][] existing : out) {
                if (same(existing, shape)) {
                    return;
                }
            }
            out.add(shape);
        }

        private static boolean same(boolean[][] a, boolean[][] b) {
            if (a.length != b.length || a[0].length != b[0].length) {
                return false;
            }
            for (int r = 0; r < a.length; r++) {
                if (!Arrays.equals(a[r], b[r])) {
                    return false;
                }
            }
            return true;
        }
    }

    public static boolean isGemstoneBlock(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("stained_glass");
    }

    public static boolean sameMiningFamily(String currentId, String incomingId) {
        String current = currentId == null ? "" : currentId.toLowerCase(Locale.ROOT);
        String incoming = incomingId == null ? "" : incomingId.toLowerCase(Locale.ROOT);
        if (current.isEmpty() || incoming.isEmpty()) {
            return false;
        }
        if (current.equals(incoming)) {
            return true;
        }
        return isGemstoneBlock(current) && isGemstoneBlock(incoming);
    }

    /**
     * Ignore a server block update that would flicker the block you are
     * already mining back into place (gemstone desync / break-progress reset).
     * Air always applies so the break can complete.
     */
    public static boolean shouldIgnoreMiningUpdate(
            boolean breakReset,
            boolean gemstoneDesync,
            boolean samePos,
            String currentBlockId,
            String incomingBlockId) {
        if (!samePos || incomingBlockId == null) {
            return false;
        }
        String incoming = incomingBlockId.toLowerCase(Locale.ROOT);
        if (incoming.contains("air") || incoming.contains("void_air") || incoming.contains("cave_air")) {
            return false;
        }
        if (gemstoneDesync && isGemstoneBlock(currentBlockId) && isGemstoneBlock(incomingBlockId)) {
            return true;
        }
        return breakReset && sameMiningFamily(currentBlockId, incomingBlockId);
    }
}
