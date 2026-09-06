package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remaining dungeon helpers: Livid wool, F4 Thorn, I4,
 * Arrow Align, terminal hitboxes, leap regions, class-ability ultimates,
 * Blood Camp boxes, F7 gates/relics, I Hate Doors glass colors and Debuff
 * phase timers. Minecraft-free so unit tests can pin Hypixel strings and
 * F7 coordinates without a client.
 */
public final class EmberDungeonPolicy {
    public enum GlassTint {
        DEFAULT,
        WHITE,
        BLACK,
        CYAN,
        LIGHT_BLUE,
        RED,
        PINK,
        ORANGE,
        MAGENTA,
        YELLOW,
        LIME,
        GRAY,
        LIGHT_GRAY,
        PURPLE,
        BLUE,
        BROWN,
        GREEN
    }

    public enum DebuffPhase {
        NONE,
        P1,
        P2,
        P3,
        P4,
        PURPLE,
        GREEN,
        RED,
        ORANGE,
        BLUE
    }

    public record IntVec(int x, int y, int z) {
    }

    public record Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public boolean contains(double x, double y, double z) {
            return x >= Math.min(minX, maxX) && x <= Math.max(minX, maxX)
                    && y >= Math.min(minY, maxY) && y <= Math.max(minY, maxY)
                    && z >= Math.min(minZ, maxZ) && z <= Math.max(minZ, maxZ);
        }
    }

    public record Relic(String name, IntVec spawn, IntVec cauldron, int color) {
    }

    public record Gate(int section, IntVec coal, Aabb box) {
    }

    public record LeapRegion(String id, Aabb box, int maxCount) {
    }

    public record ArrowClicks(int index, int clicks) {
    }

    public static final IntVec LIVID_WOOL = new IntVec(5, 108, 40);
    public static final IntVec ARROW_GRID_CORNER = new IntVec(-2, 120, 75);
    public static final long LIVID_INVULN_MILLIS = 19_500L;
    public static final int DEFAULT_DEBUFF_TICKS = 8;

    private static final Pattern DEVICE_DONE = Pattern.compile(
            "^(\\w{3,16}) completed a device! \\(\\d+/\\d+\\)$");
    private static final Pattern RELIC_PICKUP = Pattern.compile(
            "^(\\w{3,16}) picked the Corrupted (\\w{3,6}) Relic!$");
    private static final Pattern INACTIVE_TERMINAL = Pattern.compile("(?i)^inactive terminal$");
    private static final Pattern ACTIVE_TERMINAL = Pattern.compile("(?i)^terminal active$");
    private static final Pattern ICE_SPRAY = Pattern.compile("(?i)ice spray wand");
    private static final List<IntVec> I4_BLOCKS = List.of(
            new IntVec(68, 130, 50),
            new IntVec(66, 130, 50),
            new IntVec(64, 130, 50),
            new IntVec(68, 128, 50),
            new IntVec(66, 128, 50),
            new IntVec(64, 128, 50),
            new IntVec(68, 126, 50),
            new IntVec(66, 126, 50),
            new IntVec(64, 126, 50));
    private static final List<List<IntVec>> TERMINAL_SECTIONS = List.of(
            List.of(new IntVec(110, 113, 73), new IntVec(110, 119, 79),
                    new IntVec(90, 112, 92), new IntVec(90, 122, 101)),
            List.of(new IntVec(68, 109, 122), new IntVec(59, 119, 123),
                    new IntVec(47, 109, 122), new IntVec(39, 108, 142),
                    new IntVec(40, 124, 123)),
            List.of(new IntVec(-2, 109, 112), new IntVec(-2, 119, 93),
                    new IntVec(18, 123, 93), new IntVec(-2, 109, 77)),
            List.of(new IntVec(41, 109, 30), new IntVec(44, 121, 30),
                    new IntVec(67, 109, 30), new IntVec(72, 114, 47)));
    public record WaypointNode(int id, int section, int x, int y, int z, boolean lever, String label) {
    }

    private static final List<WaypointNode> WAYPOINT_NODES = List.of(
            new WaypointNode(1, 1, 111, 113, 73, false, "S1 T1"),
            new WaypointNode(2, 1, 111, 119, 79, false, "S1 T2"),
            new WaypointNode(3, 1, 89, 112, 92, false, "S1 T3"),
            new WaypointNode(4, 1, 89, 122, 101, false, "S1 T4"),
            new WaypointNode(5, 1, 94, 124, 113, true, "S1 Right"),
            new WaypointNode(6, 1, 106, 124, 113, true, "S1 Left"),
            new WaypointNode(7, 2, 68, 109, 121, false, "S2 T1"),
            new WaypointNode(8, 2, 59, 120, 122, false, "S2 T2"),
            new WaypointNode(9, 2, 47, 109, 121, false, "S2 T3"),
            new WaypointNode(10, 2, 39, 108, 143, false, "S2 T4"),
            new WaypointNode(11, 2, 40, 124, 122, false, "S2 T5"),
            new WaypointNode(12, 2, 27, 124, 127, true, "S2 Right"),
            new WaypointNode(13, 2, 23, 132, 138, true, "S2 Left"),
            new WaypointNode(14, 3, -3, 109, 112, false, "S3 T1"),
            new WaypointNode(15, 3, -3, 119, 93, false, "S3 T2"),
            new WaypointNode(16, 3, 19, 123, 93, false, "S3 T3"),
            new WaypointNode(17, 3, -3, 109, 77, false, "S3 T4"),
            new WaypointNode(18, 3, 14, 122, 55, true, "S3 Right"),
            new WaypointNode(19, 3, 2, 122, 55, true, "S3 Left"),
            new WaypointNode(20, 4, 41, 109, 29, false, "S4 T1"),
            new WaypointNode(21, 4, 44, 121, 29, false, "S4 T2"),
            new WaypointNode(22, 4, 67, 109, 29, false, "S4 T3"),
            new WaypointNode(23, 4, 72, 115, 48, false, "S4 T4"),
            new WaypointNode(24, 4, 86, 128, 46, true, "S4 Right"),
            new WaypointNode(25, 4, 84, 121, 34, true, "S4 Left"));
    private static final int[][] ARROW_SOLUTIONS = {
            {7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, -1, -1, 7, 1},
            {-1, -1, 7, 7, 5, -1, 7, 1, -1, 5, -1, -1, -1, -1, -1, -1, 7, 5, -1, 1, -1, -1, 7, 7, 1},
            {7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, -1, 7, 5, -1, -1, -1, -1, 5, -1, -1, -1, 3, 3},
            {5, 3, 3, 3, -1, 5, -1, -1, -1, -1, 7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, -1},
            {5, 3, 3, 3, 3, 5, -1, -1, -1, 1, 7, 7, -1, -1, 1, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1},
            {7, 7, 7, 7, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1},
            {-1, -1, -1, -1, -1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, 7, 7, 7, 7, 1, -1, -1, -1, -1, -1},
            {-1, -1, -1, -1, -1, -1, 1, -1, 1, -1, 7, 1, 7, 1, 3, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1}
    };
    private static final Map<String, String> LIVID_BY_WOOL = Map.of(
            "green_wool", "Frog Livid",
            "purple_wool", "Purple Livid",
            "gray_wool", "Doctor Livid",
            "blue_wool", "Scream Livid",
            "lime_wool", "Smile Livid",
            "red_wool", "Hockey Livid",
            "magenta_wool", "Crossed Livid",
            "yellow_wool", "Arcade Livid",
            "white_wool", "Vendetta Livid");

    private EmberDungeonPolicy() {
    }

    public static Optional<String> lividFromWool(String blockId) {
        if (blockId == null) {
            return Optional.empty();
        }
        String id = blockId.toLowerCase(Locale.ROOT);
        int slash = id.lastIndexOf(':');
        if (slash >= 0) {
            id = id.substring(slash + 1);
        }
        return Optional.ofNullable(LIVID_BY_WOOL.get(id));
    }

    public static boolean hologramMatchesLivid(String hologram, String lividName) {
        if (lividName == null || lividName.isBlank()) {
            return false;
        }
        String text = DungeonPolicy.normalize(hologram).toLowerCase(Locale.ROOT);
        String key = lividName.toLowerCase(Locale.ROOT).replace(" livid", "");
        return text.contains(key) && text.contains("livid");
    }

    public static boolean isThornHologram(String hologram) {
        String text = DungeonPolicy.normalize(hologram).toLowerCase(Locale.ROOT);
        return text.contains("thorn") && !text.contains("status");
    }

    public static boolean isLividHologram(String hologram) {
        String text = DungeonPolicy.normalize(hologram).toLowerCase(Locale.ROOT);
        return text.contains("livid") && !text.contains("status");
    }

    public static boolean isWatcherHologram(String hologram) {
        String text = DungeonPolicy.normalize(hologram).toLowerCase(Locale.ROOT);
        return text.contains("the watcher") || text.equals("chosen");
    }

    public static boolean isBloodMobName(String hologram) {
        String text = DungeonPolicy.normalize(hologram).toLowerCase(Locale.ROOT);
        return text.contains("revoker")
                || text.contains("psycho")
                || text.contains("reaper")
                || text.contains("cannibal")
                || text.contains("mute")
                || text.contains("ooze")
                || text.contains("putrid")
                || text.contains("freak")
                || text.contains("leech")
                || text.contains("tear")
                || text.contains("parasite")
                || text.contains("flamer")
                || text.contains("skull")
                || text.contains("mr. deadly")
                || text.contains("mr deadly");
    }

    public static boolean isInactiveTerminal(String name) {
        return INACTIVE_TERMINAL.matcher(DungeonPolicy.normalize(name)).matches();
    }

    public static boolean isActiveTerminal(String name) {
        return ACTIVE_TERMINAL.matcher(DungeonPolicy.normalize(name)).matches();
    }

    public static int terminalSection(double x, double y, double z) {
        for (int section = 0; section < TERMINAL_SECTIONS.size(); section++) {
            for (IntVec pos : TERMINAL_SECTIONS.get(section)) {
                double dx = x - pos.x();
                double dy = y - pos.y();
                double dz = z - pos.z();
                if (dx * dx + dy * dy + dz * dz <= 1.5D) {
                    return section + 1;
                }
            }
        }
        return 0;
    }

    public static List<List<IntVec>> terminalSections() {
        return TERMINAL_SECTIONS;
    }

    public static List<WaypointNode> waypointNodes() {
        return WAYPOINT_NODES;
    }

    public static List<IntVec> i4Blocks() {
        return I4_BLOCKS;
    }

    public static boolean isI4Block(int x, int y, int z) {
        return I4_BLOCKS.contains(new IntVec(x, y, z));
    }

    public static boolean isOnI4Device(double x, double y, double z) {
        return x >= 62.0D && x <= 70.0D && y >= 120.0D && y <= 136.0D && z >= 47.0D && z <= 56.0D;
    }

    public static boolean isI4Lit(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("sea_lantern") || id.contains("sea lantern") || id.contains("lit")
                || id.contains("emerald");
    }

    public static boolean isI4Done(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("obsidian") || id.contains("coal") || id.contains("black");
    }

    public static boolean isDeviceDoneChat(String chat) {
        return DEVICE_DONE.matcher(DungeonPolicy.normalize(chat)).matches();
    }

    public static int arrowIndex(int x, int y, int z) {
        if (x != ARROW_GRID_CORNER.x()) {
            return -1;
        }
        int index = (y - ARROW_GRID_CORNER.y()) + (z - ARROW_GRID_CORNER.z()) * 5;
        return index >= 0 && index < 25 ? index : -1;
    }

    public static IntVec arrowPos(int index) {
        return new IntVec(
                ARROW_GRID_CORNER.x(),
                ARROW_GRID_CORNER.y() + (index % 5),
                ARROW_GRID_CORNER.z() + (index / 5));
    }

    public static int arrowClicks(int current, int target) {
        if (target < 0 || current < 0) {
            return 0;
        }
        return (8 - current + target) % 8;
    }

    public static Optional<int[]> matchArrowSolution(int[] rotations) {
        if (rotations == null || rotations.length != 25) {
            return Optional.empty();
        }
        for (int[] solution : ARROW_SOLUTIONS) {
            boolean ok = true;
            for (int i = 0; i < 25; i++) {
                int target = solution[i];
                int curr = rotations[i];
                if ((target == -1 || curr == -1) && target != curr) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                return Optional.of(solution);
            }
        }
        return Optional.empty();
    }

    public static List<ArrowClicks> remainingArrowClicks(int[] rotations, int[] solution) {
        List<ArrowClicks> remaining = new ArrayList<>();
        if (rotations == null || solution == null) {
            return List.of();
        }
        int n = Math.min(rotations.length, solution.length);
        for (int i = 0; i < n; i++) {
            if (rotations[i] < 0 || solution[i] < 0) {
                continue;
            }
            int clicks = arrowClicks(rotations[i], solution[i]);
            if (clicks > 0) {
                remaining.add(new ArrowClicks(i, clicks));
            }
        }
        return List.copyOf(remaining);
    }

    public static int arrowColor(int clicks) {
        if (clicks < 3) {
            return 0xFF22C55E;
        }
        if (clicks < 5) {
            return 0xFFF97316;
        }
        return 0xFFEF4444;
    }

    public static Optional<String> relicPickup(String chat) {
        Matcher matcher = RELIC_PICKUP.matcher(DungeonPolicy.normalize(chat));
        return matcher.matches() ? Optional.of(matcher.group(2)) : Optional.empty();
    }

    public static List<Relic> relics() {
        return List.of(
                new Relic("Red", new IntVec(20, 7, 59), new IntVec(51, 7, 42), 0x66FF0000),
                new Relic("Orange", new IntVec(26, 7, 59), new IntVec(57, 7, 42), 0x66FF7200),
                new Relic("Green", new IntVec(20, 7, 94), new IntVec(49, 7, 44), 0x6600FF00),
                new Relic("Blue", new IntVec(91, 7, 94), new IntVec(59, 7, 44), 0x66008AFF),
                new Relic("Purple", new IntVec(56, 9, 132), new IntVec(54, 7, 41), 0x6681006F));
    }

    public static Optional<Relic> relicByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String key = name.toLowerCase(Locale.ROOT);
        for (Relic relic : relics()) {
            if (key.contains(relic.name().toLowerCase(Locale.ROOT))) {
                return Optional.of(relic);
            }
        }
        return Optional.empty();
    }

    public static boolean clickedRelicCauldron(Relic relic, int x, int y, int z) {
        if (relic == null) {
            return false;
        }
        IntVec cauldron = relic.cauldron();
        return x == cauldron.x()
                && (y == cauldron.y() || y == cauldron.y() - 1)
                && z == cauldron.z();
    }

    public static boolean isRelicCauldron(int x, int y, int z) {
        for (Relic relic : relics()) {
            if (clickedRelicCauldron(relic, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public static boolean correctRelicCauldron(String heldColor, int x, int y, int z) {
        return relicByName(heldColor)
                .filter(relic -> clickedRelicCauldron(relic, x, y, z))
                .isPresent();
    }

    public static boolean blockRelicClick(
            boolean enabled,
            String heldColor,
            boolean holdingRelicOrMenu,
            int x,
            int y,
            int z) {
        if (!enabled || heldColor == null || heldColor.isBlank() || !isRelicCauldron(x, y, z)) {
            return false;
        }
        if (!holdingRelicOrMenu) {
            return true;
        }
        return !correctRelicCauldron(heldColor, x, y, z);
    }

    public static List<Gate> gates() {
        return List.of(
                new Gate(1, new IntVec(103, 134, 123), new Aabb(95, 114, 122, 106, 134, 124)),
                new Gate(2, new IntVec(17, 134, 135), new Aabb(18, 114, 127, 19, 134, 138)),
                new Gate(3, new IntVec(5, 134, 49), new Aabb(14, 114, 51, 1, 134, 49)));
    }

    public static Optional<Gate> gateForSection(int section) {
        for (Gate gate : gates()) {
            if (gate.section() == section) {
                return Optional.of(gate);
            }
        }
        return Optional.empty();
    }

    public static int p3Section(double x, double y, double z) {
        if (x >= 90) {
            return 1;
        }
        if (z >= 113) {
            return 2;
        }
        if (x < 20) {
            return 3;
        }
        if (z <= 50) {
            return 4;
        }
        return 0;
    }

    public static List<LeapRegion> leapRegions() {
        return List.of(
                new LeapRegion("SS", new Aabb(106, 119, 92, 109, 121, 96), 3),
                new LeapRegion("EE2", new Aabb(57, 108, 130, 59, 110, 132), 4),
                new LeapRegion("HEE2", new Aabb(57, 132, 138, 62, 133, 140), 4),
                new LeapRegion("EE3", new Aabb(1, 108, 101, 3, 110, 107), 3),
                new LeapRegion("CORE", new Aabb(51, 114, 49, 58, 117, 54), 4),
                new LeapRegion("INCORE", new Aabb(51, 114, 55, 58, 117, 60), 4),
                new LeapRegion("RELIC", new Aabb(51.5, 3, 73.5, 57.5, 8, 79.5), 4));
    }

    public static Optional<LeapRegion> leapRegionAt(double x, double y, double z) {
        for (LeapRegion region : leapRegions()) {
            if (region.box().contains(x, y, z)) {
                return Optional.of(region);
            }
        }
        return Optional.empty();
    }

    public static boolean isLeapMotionPacket(String packetSimpleName) {
        if (packetSimpleName == null || packetSimpleName.isBlank()) {
            return false;
        }
        String name = packetSimpleName.toLowerCase(Locale.ROOT);
        return name.contains("teleportentity")
                || name.contains("setentitymotion")
                || name.contains("moveentity")
                || name.contains("entitypositionsync")
                || name.contains("addentity");
    }

    public static boolean isLeapTeleportIntoRegion(
            LeapRegion region, double x, double y, double z) {
        return region != null && region.box().contains(x, y, z);
    }

    public static String leapCounterText(int count, int max) {
        int shownMax = Math.max(0, max);
        int shown = Math.max(0, Math.min(count, shownMax));
        String prefix = shownMax - shown <= 1 ? "§9" : "§4";
        return prefix + shown + "§9/" + shownMax + " Players Leaped";
    }

    public static boolean shouldFireUltimate(
            String chat,
            String floor,
            DungeonPolicy.DungeonClass dungeonClass) {
        String text = DungeonPolicy.normalize(chat);
        String fl = floor == null ? "" : floor.toUpperCase(Locale.ROOT);
        int num = floorNumber(fl);
        if (text.equals("⚠ Maxor is enraged! ⚠") && num == 7) {
            return dungeonClass == DungeonPolicy.DungeonClass.HEALER
                    || dungeonClass == DungeonPolicy.DungeonClass.TANK;
        }
        if (text.startsWith("[BOSS] Goldor: You have done it, you destroyed the factory") && num == 7) {
            return dungeonClass == DungeonPolicy.DungeonClass.HEALER
                    || dungeonClass == DungeonPolicy.DungeonClass.TANK;
        }
        if (text.equals("[BOSS] Sadan: My giants! Unleashed!") && num == 6) {
            return dungeonClass != DungeonPolicy.DungeonClass.UNKNOWN;
        }
        if (text.startsWith("[BOSS] Livid: I respect you for making it to here") && num == 5) {
            return dungeonClass == DungeonPolicy.DungeonClass.HEALER
                    || dungeonClass == DungeonPolicy.DungeonClass.TANK;
        }
        return false;
    }

    public static boolean isClassUltimateItem(String name, List<String> lore) {
        String hay = (name == null ? "" : name) + " " + String.join(" ", lore == null ? List.of() : lore);
        String text = DungeonPolicy.normalize(hay).toLowerCase(Locale.ROOT);
        return text.contains("ultimate")
                && (text.contains("wish")
                || text.contains("castle of stone")
                || text.contains("thunderstorm")
                || text.contains("guided sheep")
                || text.contains("ragnarok")
                || text.contains("ragnarock")
                || text.contains("explosive shot")
                || text.contains("dungeon class ability"));
    }

    /**
     * Vanilla {@code DROP_ITEM} (Q) is used for ultimates and
     * {@code DROP_ALL_ITEMS} for the regular class ability. Local test
     * servers that remap those actions the same way will fire the ability
     * without swapping a hotbar item.
     */
    public static boolean usesVanillaDropForClassAbility() {
        return true;
    }

    public static boolean isIceSprayItem(String name, List<String> lore) {
        return containsNormalized(name, lore, "ice spray");
    }

    public static boolean isGravityWandItem(String name, List<String> lore) {
        return containsNormalized(name, lore, "gravity wand", "gyrokinetic");
    }

    public static boolean isIcedMobName(String hologram) {
        String text = DungeonPolicy.normalize(hologram).toLowerCase(Locale.ROOT);
        return text.contains("iced") || text.contains("frozen");
    }

    public static boolean isPuzzleTimerStart(String chat) {
        String text = DungeonPolicy.normalize(chat);
        return text.toLowerCase(Locale.ROOT).contains("puzzle")
                && (text.contains("started") || text.contains("began") || text.contains("opened"));
    }

    public static boolean isPuzzleTimerEnd(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        return text.contains("completed") && text.contains("puzzle")
                || text.contains("puzzle fail")
                || text.contains("wasn't on time")
                || text.contains("you failed");
    }

    private static boolean containsNormalized(String name, List<String> lore, String... needles) {
        String hay = (name == null ? "" : name) + " " + String.join(" ", lore == null ? List.of() : lore);
        String text = DungeonPolicy.normalize(hay).toLowerCase(Locale.ROOT);
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static DebuffPhase debuffPhase(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (text.equals("[BOSS] Maxor: WELL DONE YOU LITTLE MEAT CAKES!")
                || text.contains("Pathetic Maxor, just like expected")) {
            return DebuffPhase.P1;
        }
        if (text.contains("[BOSS] Storm: I should have known")) {
            return DebuffPhase.P2;
        }
        if (text.equals("[BOSS] Goldor: Who dares trespass into my domain?")) {
            return DebuffPhase.P3;
        }
        if (text.equals("[BOSS] Necron: All this, for nothing...")) {
            return DebuffPhase.P4;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("purple") && lower.contains("wither")) {
            return DebuffPhase.PURPLE;
        }
        if (lower.contains("green") && lower.contains("wither") && lower.contains("dragon")) {
            return DebuffPhase.GREEN;
        }
        if (lower.contains("red") && lower.contains("wither") && lower.contains("dragon")) {
            return DebuffPhase.RED;
        }
        if (lower.contains("orange") && lower.contains("wither") && lower.contains("dragon")) {
            return DebuffPhase.ORANGE;
        }
        if (lower.contains("blue") && lower.contains("wither") && lower.contains("dragon")) {
            return DebuffPhase.BLUE;
        }
        return DebuffPhase.NONE;
    }

    public static String debuffLabel(DebuffPhase phase) {
        return switch (phase) {
            case P1 -> "Debuff P1";
            case P2 -> "Debuff P2";
            case P3 -> "Debuff P3";
            case P4 -> "Debuff P4";
            case PURPLE -> "Purple Debuff";
            case GREEN -> "Green Debuff";
            case RED -> "Red Debuff";
            case ORANGE -> "Orange Debuff";
            case BLUE -> "Blue Debuff";
            case NONE -> "";
        };
    }

    public static boolean isIceSprayLine(String line) {
        return ICE_SPRAY.matcher(DungeonPolicy.normalize(line)).find();
    }

    public static GlassTint glassTint(String name) {
        if (name == null || name.isBlank()) {
            return GlassTint.BLACK;
        }
        String key = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return GlassTint.valueOf(key);
        } catch (IllegalArgumentException ignored) {
            return GlassTint.BLACK;
        }
    }

    public static List<String> glassTintNames() {
        List<String> names = new ArrayList<>();
        for (GlassTint tint : GlassTint.values()) {
            names.add(prettyGlass(tint));
        }
        return names;
    }

    public static String prettyGlass(GlassTint tint) {
        String raw = tint.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    public static String glassBlockId(GlassTint tint) {
        return switch (tint) {
            case DEFAULT, WHITE -> "minecraft:white_stained_glass";
            case BLACK -> "minecraft:black_stained_glass";
            case CYAN -> "minecraft:cyan_stained_glass";
            case LIGHT_BLUE -> "minecraft:light_blue_stained_glass";
            case RED -> "minecraft:red_stained_glass";
            case PINK -> "minecraft:pink_stained_glass";
            case ORANGE -> "minecraft:orange_stained_glass";
            case MAGENTA -> "minecraft:magenta_stained_glass";
            case YELLOW -> "minecraft:yellow_stained_glass";
            case LIME -> "minecraft:lime_stained_glass";
            case GRAY -> "minecraft:gray_stained_glass";
            case LIGHT_GRAY -> "minecraft:light_gray_stained_glass";
            case PURPLE -> "minecraft:purple_stained_glass";
            case BLUE -> "minecraft:blue_stained_glass";
            case BROWN -> "minecraft:brown_stained_glass";
            case GREEN -> "minecraft:green_stained_glass";
        };
    }

    public static int glassArgb(GlassTint tint) {
        return switch (tint) {
            case DEFAULT, WHITE -> 0x88FFFFFF;
            case BLACK -> 0x88111111;
            case CYAN -> 0x8800AAAA;
            case LIGHT_BLUE -> 0x8855FFFF;
            case RED -> 0x88FF5555;
            case PINK -> 0x88FF55FF;
            case ORANGE -> 0x88FFAA00;
            case MAGENTA -> 0x88AA00AA;
            case YELLOW -> 0x88FFFF55;
            case LIME -> 0x8855FF55;
            case GRAY -> 0x88555555;
            case LIGHT_GRAY -> 0x88AAAAAA;
            case PURPLE -> 0x88AA00AA;
            case BLUE -> 0x885555FF;
            case BROWN -> 0x88835433;
            case GREEN -> 0x8800AA00;
        };
    }

    public static boolean isHateDoorBlock(String blockId, boolean wither, boolean blood, boolean entrance) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        if (wither && (id.contains("coal_block") || id.contains("black_stained")
                || id.contains("black_terracotta"))) {
            return true;
        }
        if (blood && (id.contains("red_stained") || id.contains("red_terracotta")
                || id.contains("redstone_block"))) {
            return true;
        }
        if (entrance && (id.contains("lime_stained") || id.contains("green_stained")
                || id.contains("oak_door") || id.contains("iron_bars"))) {
            return true;
        }
        return false;
    }

    public static Optional<GlassTint> hateDoorTint(
            String blockId,
            boolean wither,
            boolean blood,
            boolean entrance,
            String witherGlass,
            String bloodGlass,
            String entranceGlass) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        if (id.contains("air") || id.contains("barrier")) {
            return Optional.empty();
        }
        if (blood && (id.contains("red_stained") || id.contains("red_terracotta")
                || id.contains("redstone_block"))) {
            return Optional.of(glassTint(bloodGlass));
        }
        if (entrance && (id.contains("lime_stained") || id.contains("green_stained")
                || id.contains("oak_door"))) {
            return Optional.of(glassTint(entranceGlass));
        }
        if (wither && (id.contains("coal_block") || id.contains("black_stained")
                || id.contains("black_terracotta"))) {
            return Optional.of(glassTint(witherGlass));
        }
        return Optional.empty();
    }

    public static int classColor(DungeonPolicy.DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case ARCHER -> 0xFFFFAA00;
            case BERSERK -> 0xFFFF5555;
            case MAGE -> 0xFF55FFFF;
            case HEALER -> 0xFFFF55FF;
            case TANK -> 0xFF55FF55;
            case UNKNOWN -> 0xFF60A5FA;
        };
    }

    public static String classLetter(DungeonPolicy.DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case ARCHER -> "A";
            case BERSERK -> "B";
            case MAGE -> "M";
            case HEALER -> "H";
            case TANK -> "T";
            case UNKNOWN -> "";
        };
    }

    public static Map<String, DungeonPolicy.DungeonClass> teammateClasses(List<String> lines) {
        Map<String, DungeonPolicy.DungeonClass> map = new LinkedHashMap<>();
        if (lines == null) {
            return map;
        }
        Pattern named = Pattern.compile("(?i)\\b(archer|berserk|mage|healer|tank)\\b.*?([A-Za-z0-9_]{3,16})");
        Pattern namedFirst = Pattern.compile("(?i)([A-Za-z0-9_]{3,16}).*?\\b(archer|berserk|mage|healer|tank)\\b");
        for (String raw : lines) {
            String text = DungeonPolicy.normalize(raw);
            Matcher matcher = named.matcher(text);
            if (matcher.find()) {
                map.put(matcher.group(2), DungeonPolicy.dungeonClass(matcher.group(1)));
                continue;
            }
            matcher = namedFirst.matcher(text);
            if (matcher.find()) {
                map.put(matcher.group(1), DungeonPolicy.dungeonClass(matcher.group(2)));
            }
        }
        return map;
    }

    /**
     * Class names that appear more than once in the party tab/sidebar.
     * Empty when the roster is unique or still filling.
     */
    public static List<DungeonPolicy.DungeonClass> duplicateClasses(
            Map<String, DungeonPolicy.DungeonClass> roster) {
        if (roster == null || roster.size() < 2) {
            return List.of();
        }
        Map<DungeonPolicy.DungeonClass, Integer> counts = new LinkedHashMap<>();
        for (DungeonPolicy.DungeonClass dungeonClass : roster.values()) {
            if (dungeonClass == null || dungeonClass == DungeonPolicy.DungeonClass.UNKNOWN) {
                continue;
            }
            counts.merge(dungeonClass, 1, Integer::sum);
        }
        List<DungeonPolicy.DungeonClass> duplicates = new ArrayList<>();
        for (Map.Entry<DungeonPolicy.DungeonClass, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(entry.getKey());
            }
        }
        return duplicates;
    }

    public static String duplicateClassTitle(List<DungeonPolicy.DungeonClass> duplicates) {
        if (duplicates == null || duplicates.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("Duplicate ");
        for (int i = 0; i < duplicates.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            String name = duplicates.get(i).name();
            out.append(name.charAt(0)).append(name.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    public static int floorNumber(String floor) {
        String text = floor == null ? "" : floor.toUpperCase(Locale.ROOT);
        if (text.length() >= 2 && (text.charAt(0) == 'F' || text.charAt(0) == 'M')) {
            char digit = text.charAt(1);
            if (digit >= '1' && digit <= '7') {
                return digit - '0';
            }
        }
        return 0;
    }

    public static boolean isLividStart(String chat) {
        return DungeonPolicy.normalize(chat)
                .startsWith("[BOSS] Livid: Welcome, you've arrived right on time");
    }
}
