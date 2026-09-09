package fi.rotclient;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Galatea / Park / Torrhus foraging decisions. No Minecraft types. The four foraging
 * parents live in the catalog; this class stays Minecraft-free.
 */
public final class ForagingPolicy {
    public enum Island {
        NONE,
        HUB,
        PARK,
        GALATEA,
        TORRHUS
    }

    public enum TreeType {
        FIG,
        MANGROVE,
        HELIX,
        UNKNOWN
    }

    public enum BeaconPitch {
        LOW,
        NORMAL,
        HIGH
    }

    /** Horizontal facing for Forest Temple terracotta. */
    public enum Cardinal {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    public record BlockKey(int x, int y, int z) {
    }

    public record TreeProgress(TreeType type, int percent) {
    }

    public record TutorialStep(String quest, String npc, double x, double y, double z) {
    }

    public record StarlynBracket(String sister, String bracket) {
    }

    public record GiftContribution(TreeType type, double percent) {
    }

    public static final List<TutorialStep> PARK_TUTORIAL = List.of(
            new TutorialStep("Foraging Tutorial", "Lumber Jack", -123.5D, 74.0D, -30.0D),
            new TutorialStep("Into the Woods", "Charlie", -275.9D, 80.0D, -17.1D),
            new TutorialStep("A Helping Hand", "Kelly", -350.8D, 94.0D, 31.7D),
            new TutorialStep("The Campfire Cult", "Ryan", -362.7D, 102.0D, -90.5D),
            new TutorialStep("The Rebuild", "Melody", -412.3D, 109.0D, 70.2D));

    private static final Pattern STARLYN_BRACKET = Pattern.compile(
            "\\[NPC]\\s+(.+?):\\s+You reached the\\s+(\\w+)\\s+Bracket",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SWEEP_AMOUNT = Pattern.compile(
            "Sweep Details:.*?([\\d,.]+).*Sweep",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TOUGHNESS_LINE = Pattern.compile(
            "(\\w+)\\s+Tree Toughness:\\s*([\\d,.]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COUPON_COST = Pattern.compile(
            "(?:Cost|Price):\\s*([\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WHISPERS = Pattern.compile(
            "([\\d,]+)\\s+Forest Whispers",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SHARD_GAIN = Pattern.compile(
            "(?i)(?:you (?:found|caught|obtained)|obtained) (?:a |an )?(.+?) shards?(?:!|\\.|$)");

    public static final float PITCH_LOW = 0.0952381F;
    public static final float PITCH_NORMAL = 0.7936508F;
    public static final float PITCH_HIGH = 1.4920635F;
    public static final int BEACON_COLOR_SLOT = 46;
    public static final int BEACON_SPEED_SLOT = 48;
    public static final int BEACON_PITCH_SLOT = 50;
    public static final int BEACON_UPGRADE_SLOT_OFFSET = -9;
    public static final int MAX_SWEEP_WOOD = 35;
    public static final int COLOR_CYCLE_LENGTH = 13;
    public static final int SPEED_CYCLE_LENGTH = 5;
    public static final int PITCH_CYCLE_LENGTH = 3;

    private static final Pattern TREE_PROGRESS = Pattern.compile(
            "(?<type>FIG|MANGROVE|HELIX)\\s+TREE\\s+(?<percent>\\d+)%",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GIFT_HEADER = Pattern.compile("TREE GIFT", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIFT_CONTRIBUTION = Pattern.compile(
            "You helped cut\\s+([\\d.]+)%\\s+of the\\s+(.+?)\\s+Tree",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SWEEP_HEADER = Pattern.compile(
            "Sweep Details:\\s*.*?([\\d,.]+).*Sweep",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SWEEP_TAB = Pattern.compile("Sweep:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BEACON_READY = Pattern.compile("Cooldown:\\s*AVAILABLE", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURRENT_PITCH = Pattern.compile("Current pitch:\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CURRENT_SPEED = Pattern.compile(
            "(?:Current\\s+)?[Ss]peed:\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SWEEP_VALUE = Pattern.compile(
            "Sweep Details:\\s*.*?([\\d,.]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOTF_TITLE = Pattern.compile("^Heart of the Forest$", Pattern.CASE_INSENSITIVE);

    /** Beacon dye cycle (slot 46). */
    public static final String[] BEACON_DYE_CYCLE = {
            "magenta_dye",
            "light_blue_dye",
            "yellow_dye",
            "lime_dye",
            "pink_dye",
            "cyan_dye",
            "purple_dye",
            "lapis_lazuli",
            "cocoa_beans",
            "green_dye",
            "red_dye",
            "bone_meal",
            "orange_dye"
    };
    /** Parallel stained-glass cycle for the moving/target panes. */
    public static final String[] BEACON_GLASS_CYCLE = {
            "magenta_stained_glass_pane",
            "light_blue_stained_glass_pane",
            "yellow_stained_glass_pane",
            "lime_stained_glass_pane",
            "pink_stained_glass_pane",
            "cyan_stained_glass_pane",
            "purple_stained_glass_pane",
            "blue_stained_glass_pane",
            "brown_stained_glass_pane",
            "green_stained_glass_pane",
            "red_stained_glass_pane",
            "white_stained_glass_pane",
            "orange_stained_glass_pane"
    };
    /** Ticks between target-pane moves → speed 1..5. */
    public static final int[][] BEACON_SPEED_TICK_RANGES = {
            {50, 64},
            {40, 49},
            {30, 39},
            {20, 29},
            {10, 19}
    };
    public static final int BEACON_TARGET_SLOT_START = 10;
    public static final int BEACON_TARGET_SLOT_END = 16;
    public static final int BEACON_MOVING_SLOT_START = 28;
    public static final int BEACON_MOVING_SLOT_END = 34;
    public static final int FOREST_TEMPLE_WALL_X_MAX = -633;
    public static final int FOREST_TEMPLE_WALL_X_MIN = -640;
    public static final int FOREST_TEMPLE_WALL_Y_MAX = 66;
    public static final int FOREST_TEMPLE_WALL_Y_MIN = 65;
    public static final int FOREST_TEMPLE_WALL_Z = 85;
    public static final int FOREST_TEMPLE_FLOOR_Y = 59;
    public static final int FOREST_TEMPLE_FLOOR_Z_EVEN = 76;
    public static final int FOREST_TEMPLE_FLOOR_Z_ODD = 75;
    public static final int FOREST_TEMPLE_FLOOR_X0 = -633;

    private static final Set<String> TREE_BIT_BLOCKS = Set.of(
            "minecraft:stripped_spruce_wood",
            "stripped_spruce_wood",
            "minecraft:mangrove_wood",
            "mangrove_wood",
            "minecraft:mangrove_leaves",
            "mangrove_leaves",
            "minecraft:azalea_leaves",
            "azalea_leaves",
            "minecraft:stripped_birch_wood",
            "stripped_birch_wood",
            "minecraft:stripped_mangrove_wood",
            "stripped_mangrove_wood",
            "minecraft:oak_leaves",
            "oak_leaves");

    private static final Set<String> FORAGING_AXES = Set.of(
            "JUNGLE_AXE",
            "TREECAPITATOR_AXE",
            "FIG_AXE",
            "FIGSTONE_AXE",
            "HELIX_CHOPPER",
            "ROOKIE_AXE",
            "PROMISING_AXE",
            "SWEET_AXE",
            "EFFICIENT_AXE");

    private static final Set<String> THROWABLE_AXES = Set.of(
            "JUNGLE_AXE",
            "TREECAPITATOR_AXE",
            "FIG_AXE",
            "FIGSTONE_AXE",
            "HELIX_CHOPPER");

    private ForagingPolicy() {
    }

    public static String strip(String raw) {
        return ChatTextPolicy.stripFormatting(raw).trim();
    }

    public static final int MOONGLADE_BEACON_X = -688;
    public static final int MOONGLADE_BEACON_Y = 128;
    public static final int MOONGLADE_BEACON_Z = 65;

    public static Island islandFromTexts(String... texts) {
        if (texts == null) {
            return Island.NONE;
        }
        Island best = Island.NONE;
        for (String text : texts) {
            Island island = islandFromArea(text);
            if (islandRank(island) > islandRank(best)) {
                best = island;
            }
        }
        return best;
    }

    public static Island islandFromArea(String area) {
        if (area == null || area.isBlank()) {
            return Island.NONE;
        }
        String text = strip(area).toLowerCase(Locale.ROOT);
        if (text.contains("torrhus")
                || text.contains("foraging_3")
                || text.contains("desert temple")
                || text.contains("torrhus springs")) {
            return Island.TORRHUS;
        }
        if (text.contains("galatea")
                || text.contains("moonglade")
                || text.contains("foraging_2")
                || text.contains("forest temple")) {
            return Island.GALATEA;
        }
        if (text.contains("the park")
                || text.equals("park")
                || text.contains("foraging_1")
                || text.contains("birch park")
                || text.contains("spruce woods")
                || text.contains("dark thicket")
                || text.contains("savanna woodland")
                || text.contains("jungle island")
                || text.contains("howling cave")
                || text.contains("melody's")) {
            return Island.PARK;
        }
        if (text.contains("skyblock hub")
                || text.equals("hub")
                || text.equals("the hub")
                || text.contains("village hub")
                || text.contains("hub island")) {
            return Island.HUB;
        }
        return Island.NONE;
    }

    public static int islandRank(Island island) {
        if (island == null) {
            return 0;
        }
        return switch (island) {
            case TORRHUS -> 4;
            case GALATEA -> 3;
            case PARK -> 2;
            case HUB -> 1;
            case NONE -> 0;
        };
    }

    public static Island higherRank(Island a, Island b) {
        Island left = a == null ? Island.NONE : a;
        Island right = b == null ? Island.NONE : b;
        return islandRank(left) >= islandRank(right) ? left : right;
    }

    /**
     * When scoreboard/tab miss Galatea/Torrhus/Park/Hub, infer from the log
     * swept on that island.
     */
    public static Island inferIslandFromLog(Island current, String blockId) {
        if (chopTrees(current)) {
            return current;
        }
        if (!isForagingLog(blockId)) {
            return current == null ? Island.NONE : current;
        }
        String id = blockId.toLowerCase(Locale.ROOT);
        if (id.contains("stripped_spruce") || (id.contains("mangrove") && !id.contains("stripped_mangrove"))) {
            return Island.GALATEA;
        }
        if (id.contains("stripped_birch") || id.contains("stripped_mangrove")) {
            return Island.TORRHUS;
        }
        if (id.contains("oak_log") || id.contains("oak_wood")) {
            return Island.HUB;
        }
        return Island.PARK;
    }

    /** Fig / Mangrove / Helix gift trees (Galatea + Torrhus). */
    public static boolean customTrees(Island island) {
        return island == Island.GALATEA || island == Island.TORRHUS;
    }

    /** Park + Galatea + Torrhus skill islands. */
    public static boolean foragingSkillIsland(Island island) {
        return island == Island.PARK || customTrees(island);
    }

    /** Everywhere treated as axe-choppable: Hub oak, Park logs, Galatea, Torrhus. */
    public static boolean chopTrees(Island island) {
        return island == Island.HUB || foragingSkillIsland(island);
    }

    public static boolean galateaIsland(Island island) {
        return island == Island.GALATEA;
    }

    public static boolean torrhusIsland(Island island) {
        return island == Island.TORRHUS;
    }

    public static boolean shouldHideTreeBits(
            boolean hideEnabled,
            Island island,
            boolean blockDisplay,
            String blockId) {
        if (!hideEnabled || !blockDisplay || !customTrees(island)) {
            return false;
        }
        return isTreeBitBlock(blockId);
    }

    public static boolean isTreeBitBlock(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return false;
        }
        return TREE_BIT_BLOCKS.contains(blockId.toLowerCase(Locale.ROOT));
    }

    public static TreeProgress parseTreeProgress(String nametag) {
        Matcher matcher = TREE_PROGRESS.matcher(strip(nametag));
        if (!matcher.find()) {
            return null;
        }
        int percent = Math.max(0, Math.min(100, Integer.parseInt(matcher.group("percent"))));
        return new TreeProgress(treeType(matcher.group("type")), percent);
    }

    public static String compactProgressLine(TreeProgress progress) {
        if (progress == null) {
            return "";
        }
        return progress.type().name() + " " + progress.percent() + "%";
    }

    public static boolean showTreeProgress(
            boolean enabled,
            Island island,
            boolean onlyHoldingAxe,
            boolean holdingAxe,
            TreeProgress progress) {
        if (!enabled || progress == null) {
            return false;
        }
        Island effective = higherRank(island, islandFromTreeType(progress.type()));
        if (!customTrees(effective)) {
            return false;
        }
        return !onlyHoldingAxe || holdingAxe;
    }

    public static TreeType treeType(String raw) {
        if (raw == null) {
            return TreeType.UNKNOWN;
        }
        String text = strip(raw).toUpperCase(Locale.ROOT);
        if (text.contains("HELIX")) {
            return TreeType.HELIX;
        }
        if (text.contains("MANGROVE")) {
            return TreeType.MANGROVE;
        }
        if (text.contains("FIG")) {
            return TreeType.FIG;
        }
        return TreeType.UNKNOWN;
    }

    public static boolean isTreeGiftHeader(String line) {
        return GIFT_HEADER.matcher(strip(line)).find();
    }

    public static GiftContribution parseGiftContribution(String line) {
        Matcher matcher = GIFT_CONTRIBUTION.matcher(strip(line));
        if (!matcher.find()) {
            return null;
        }
        return new GiftContribution(treeType(matcher.group(2)), Double.parseDouble(matcher.group(1)));
    }

    public static boolean isSweepDetailsHeader(String line) {
        return SWEEP_HEADER.matcher(strip(line)).find();
    }

    public static boolean hideUnmineableChat(boolean filterEnabled, Island island, String line) {
        if (!filterEnabled || !customTrees(island)) {
            return false;
        }
        String text = strip(line).toLowerCase(Locale.ROOT);
        return text.contains("cannot damage a tree while it is regenerating")
                || text.contains("toughness of this tree is way too high");
    }

    public static boolean mutePhantom(boolean enabled, Island island, String soundId) {
        return enabled && island == Island.GALATEA && containsSound(soundId, "entity.phantom.");
    }

    public static boolean muteTreeBreak(boolean enabled, boolean alsoGalatea, Island island, String soundId) {
        if (!enabled || !containsSound(soundId, "entity.creaking.death")) {
            return false;
        }
        if (customTrees(island)) {
            return alsoGalatea;
        }
        return true;
    }

    public static boolean muteFusionMachine(boolean enabled, Island island, String soundId, float volume) {
        if (!enabled || island != Island.GALATEA) {
            return false;
        }
        if (!containsSound(soundId, "entity.firework_rocket.blast")) {
            return false;
        }
        return volume >= 19.5F;
    }

    public static boolean isHotfTitle(String title) {
        return HOTF_TITLE.matcher(strip(title)).matches();
    }

    public static boolean isBeaconTuneTitle(String title) {
        String text = strip(title);
        return text.startsWith("Tune Frequency") || text.startsWith("Upgrade Signal Strength");
    }

    public static boolean isUpgradeStrengthMenu(String title) {
        return strip(title).startsWith("Upgrade Signal Strength");
    }

    public static int beaconSlot(String title, int baseSlot) {
        return isUpgradeStrengthMenu(title) ? baseSlot + BEACON_UPGRADE_SLOT_OFFSET : baseSlot;
    }

    public static boolean isBeaconReady(String tabLine) {
        return BEACON_READY.matcher(strip(tabLine)).find();
    }

    public static BeaconPitch parsePitchName(String loreOrName) {
        Matcher matcher = CURRENT_PITCH.matcher(strip(loreOrName));
        String token = matcher.find() ? matcher.group(1) : strip(loreOrName);
        String upper = token.toUpperCase(Locale.ROOT);
        if (upper.startsWith("LOW")) {
            return BeaconPitch.LOW;
        }
        if (upper.startsWith("HIGH")) {
            return BeaconPitch.HIGH;
        }
        if (upper.startsWith("NORMAL") || upper.startsWith("MID")) {
            return BeaconPitch.NORMAL;
        }
        return null;
    }

    public static BeaconPitch classifyPitch(float pitch) {
        float dLow = Math.abs(pitch - PITCH_LOW);
        float dMid = Math.abs(pitch - PITCH_NORMAL);
        float dHigh = Math.abs(pitch - PITCH_HIGH);
        if (dLow <= dMid && dLow <= dHigh) {
            return BeaconPitch.LOW;
        }
        if (dHigh <= dMid) {
            return BeaconPitch.HIGH;
        }
        return BeaconPitch.NORMAL;
    }

    public static int shortestCycleClicks(int fromIndex, int toIndex, int cycleLength) {
        if (cycleLength <= 0) {
            return 0;
        }
        int from = Math.floorMod(fromIndex, cycleLength);
        int to = Math.floorMod(toIndex, cycleLength);
        int forward = Math.floorMod(to - from, cycleLength);
        int backward = Math.floorMod(from - to, cycleLength);
        return forward <= backward ? forward : -backward;
    }

    public static int remainingClicksAfterPress(int clicks, int cycleLength, boolean rightClick) {
        int delta = rightClick ? 1 : -1;
        int forward = clicks >= 0 ? clicks : cycleLength + clicks;
        forward = Math.floorMod(forward + delta, cycleLength);
        int backward = cycleLength - forward;
        return forward <= backward ? forward : -backward;
    }

    public static boolean preventBeaconOverClick(boolean prevent, int remainingSignedClicks) {
        return prevent && remainingSignedClicks == 0;
    }

    public static boolean shouldAutoClickBeacon(
            boolean moduleEnabled,
            boolean cheatClick,
            boolean inTuneMenu,
            int remainingSignedClicks) {
        return moduleEnabled && cheatClick && inTuneMenu && remainingSignedClicks != 0;
    }

    public static float toughness(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        if (id.contains("stripped_spruce")) {
            return 10.0F;
        }
        if (id.contains("stripped_birch") || id.contains("stripped_mangrove")) {
            return 150.0F;
        }
        if (id.contains("mangrove")) {
            return 50.0F;
        }
        return 1.0F;
    }

    public static int maxWood(float sweepStat, float toughness, boolean thrownAbility) {
        double logs;
        if (toughness <= 1.0F) {
            logs = Math.min(MAX_SWEEP_WOOD, (int) sweepStat);
        } else {
            double x = (sweepStat + Math.sqrt(sweepStat) - toughness) / Math.pow(toughness, 0.511);
            logs = Math.log10(1.0 + Math.pow(Math.max(0.0, x), 1.9)) * 4.0;
        }
        int wood = (int) Math.ceil(Math.min(MAX_SWEEP_WOOD, logs));
        if (thrownAbility) {
            wood /= 2;
        }
        return Math.max(0, wood);
    }

    public static int parseSweepDetails(String line) {
        Matcher matcher = SWEEP_VALUE.matcher(strip(line));
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1).replace(",", ""));
    }

    public static int parseTabSweep(String line) {
        Matcher matcher = SWEEP_TAB.matcher(strip(line));
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    public static boolean isForagingAxe(String skyblockId) {
        return skyblockId != null && FORAGING_AXES.contains(skyblockId.toUpperCase(Locale.ROOT));
    }

    public static boolean isThrowableAxe(String skyblockId) {
        return skyblockId != null && THROWABLE_AXES.contains(skyblockId.toUpperCase(Locale.ROOT));
    }

    public static boolean isForagingLog(String blockId) {
        if (blockId == null) {
            return false;
        }
        String id = blockId.toLowerCase(Locale.ROOT);
        return id.contains("_log") || (id.contains("_wood") && !id.contains("leaves"));
    }

    /**
     * Island log sets: Galatea fig/mangrove, Torrhus helix
     * birch/stripped mangrove, Hub oak, Park any log.
     */
    public static boolean isChopLog(Island island, String blockId) {
        if (!isForagingLog(blockId)) {
            return false;
        }
        String id = blockId.toLowerCase(Locale.ROOT);
        return switch (island == null ? Island.NONE : island) {
            case GALATEA -> id.contains("stripped_spruce")
                    || (id.contains("mangrove") && !id.contains("stripped_mangrove"));
            case TORRHUS -> id.contains("stripped_birch") || id.contains("stripped_mangrove");
            case HUB -> id.contains("oak_log") || id.contains("oak_wood");
            case PARK, NONE -> true;
        };
    }

    public static boolean sameChopFamily(Island island, String sourceId, String destId) {
        if (island != Island.TORRHUS) {
            return true;
        }
        boolean srcBirch = sourceId != null && sourceId.toLowerCase(Locale.ROOT).contains("birch");
        boolean dstBirch = destId != null && destId.toLowerCase(Locale.ROOT).contains("birch");
        boolean srcMangrove = sourceId != null && sourceId.toLowerCase(Locale.ROOT).contains("mangrove");
        boolean dstMangrove = destId != null && destId.toLowerCase(Locale.ROOT).contains("mangrove");
        return srcBirch && dstBirch || srcMangrove && dstMangrove;
    }

    public static Island islandFromTreeType(TreeType type) {
        if (type == TreeType.HELIX) {
            return Island.TORRHUS;
        }
        if (type == TreeType.FIG || type == TreeType.MANGROVE) {
            return Island.GALATEA;
        }
        return Island.NONE;
    }

    public static int countSweepCluster(Set<BlockKey> logs, BlockKey start, int maxWood) {
        if (logs == null || start == null || !logs.contains(start)) {
            return 0;
        }
        int cap = Math.max(0, Math.min(MAX_SWEEP_WOOD, maxWood));
        Set<BlockKey> visited = new HashSet<>();
        Queue<BlockKey> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        int wood = 0;
        while (wood < cap && !queue.isEmpty()) {
            BlockKey pos = queue.poll();
            wood++;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockKey next = new BlockKey(pos.x() + dx, pos.y() + dy, pos.z() + dz);
                        if (logs.contains(next) && visited.add(next)) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return wood;
    }

    public static String itemPath(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "";
        }
        String text = itemId.toLowerCase(Locale.ROOT);
        int colon = text.indexOf(':');
        return colon >= 0 ? text.substring(colon + 1) : text;
    }

    public static int cycleIndex(String itemId, String[] cycle) {
        if (cycle == null || cycle.length == 0) {
            return -1;
        }
        String path = itemPath(itemId);
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i].equals(path)) {
                return i;
            }
        }
        return -1;
    }

    public static int parseCurrentSpeed(String loreOrName) {
        Matcher matcher = CURRENT_SPEED.matcher(strip(loreOrName));
        if (!matcher.find()) {
            return 0;
        }
        int speed = Integer.parseInt(matcher.group(1));
        return speed >= 1 && speed <= SPEED_CYCLE_LENGTH ? speed : 0;
    }

    public static int speedFromMoveTicks(int ticks) {
        for (int i = 0; i < BEACON_SPEED_TICK_RANGES.length; i++) {
            int[] range = BEACON_SPEED_TICK_RANGES[i];
            if (ticks >= range[0] && ticks <= range[1]) {
                return i + 1;
            }
        }
        return 0;
    }

    public static int colorClicks(String dyeItemId, String targetGlassItemId) {
        int from = cycleIndex(dyeItemId, BEACON_DYE_CYCLE);
        int to = cycleIndex(targetGlassItemId, BEACON_GLASS_CYCLE);
        if (from < 0 || to < 0) {
            return 0;
        }
        return shortestCycleClicks(from, to, COLOR_CYCLE_LENGTH);
    }

    public static int speedClicks(int currentSpeed, int targetSpeed) {
        if (currentSpeed < 1 || targetSpeed < 1) {
            return 0;
        }
        return shortestCycleClicks(currentSpeed - 1, targetSpeed - 1, SPEED_CYCLE_LENGTH);
    }

    public static int pitchClicks(BeaconPitch current, BeaconPitch target) {
        if (current == null || target == null) {
            return 0;
        }
        return shortestCycleClicks(current.ordinal(), target.ordinal(), PITCH_CYCLE_LENGTH);
    }

    public record BeaconHint(int colorClicks, int speedClicks, int pitchClicks) {
        public boolean pending() {
            return colorClicks != 0 || speedClicks != 0 || pitchClicks != 0;
        }

        public String hudLine() {
            if (!pending()) {
                return "Beacon matched";
            }
            return "Beacon C"
                    + signed(colorClicks)
                    + " S"
                    + signed(speedClicks)
                    + " P"
                    + signed(pitchClicks);
        }

        private static String signed(int value) {
            return value > 0 ? "+" + value : String.valueOf(value);
        }
    }

    public record AutoBeaconClick(int slot, boolean rightClick) {
    }

    /**
     * Next cheat container click. Positive remaining clicks are right-click
     * (button 1 / cycle +1).
     */
    public static AutoBeaconClick nextBeaconClick(
            boolean moduleEnabled,
            boolean cheatClick,
            String title,
            BeaconHint hint) {
        if (hint == null || !isBeaconTuneTitle(title)) {
            return null;
        }
        int remaining = firstPendingClicks(hint);
        if (!shouldAutoClickBeacon(moduleEnabled, cheatClick, true, remaining)) {
            return null;
        }
        if (hint.colorClicks() != 0) {
            return new AutoBeaconClick(beaconSlot(title, BEACON_COLOR_SLOT), hint.colorClicks() > 0);
        }
        if (hint.speedClicks() != 0) {
            return new AutoBeaconClick(beaconSlot(title, BEACON_SPEED_SLOT), hint.speedClicks() > 0);
        }
        return new AutoBeaconClick(beaconSlot(title, BEACON_PITCH_SLOT), hint.pitchClicks() > 0);
    }

    public static BeaconHint applyPress(BeaconHint hint, int slot, boolean rightClick, String title) {
        if (hint == null) {
            return null;
        }
        int colorSlot = beaconSlot(title, BEACON_COLOR_SLOT);
        int speedSlot = beaconSlot(title, BEACON_SPEED_SLOT);
        int pitchSlot = beaconSlot(title, BEACON_PITCH_SLOT);
        int color = hint.colorClicks();
        int speed = hint.speedClicks();
        int pitch = hint.pitchClicks();
        if (slot == colorSlot) {
            color = remainingAfterDirectedClick(color, COLOR_CYCLE_LENGTH, rightClick);
        } else if (slot == speedSlot) {
            speed = remainingAfterDirectedClick(speed, SPEED_CYCLE_LENGTH, rightClick);
        } else if (slot == pitchSlot) {
            pitch = remainingAfterDirectedClick(pitch, PITCH_CYCLE_LENGTH, rightClick);
        }
        return new BeaconHint(color, speed, pitch);
    }

    /** After one click toward (+) or against (−) the remaining signed count. */
    public static int remainingAfterDirectedClick(int remaining, int cycleLength, boolean rightClick) {
        int delta = rightClick ? 1 : -1;
        int next = remaining - delta;
        return shortestCycleClicks(0, Math.floorMod(next, cycleLength), cycleLength);
    }

    private static int firstPendingClicks(BeaconHint hint) {
        if (hint.colorClicks() != 0) {
            return hint.colorClicks();
        }
        if (hint.speedClicks() != 0) {
            return hint.speedClicks();
        }
        return hint.pitchClicks();
    }

    public static Cardinal cardinalFromFacing(String facing) {
        if (facing == null || facing.isBlank()) {
            return null;
        }
        String text = strip(facing).toUpperCase(Locale.ROOT);
        if (text.contains("NORTH")) {
            return Cardinal.NORTH;
        }
        if (text.contains("EAST")) {
            return Cardinal.EAST;
        }
        if (text.contains("SOUTH")) {
            return Cardinal.SOUTH;
        }
        if (text.contains("WEST")) {
            return Cardinal.WEST;
        }
        return null;
    }

    public static boolean isForestTempleWall(int x, int y, int z) {
        return z == FOREST_TEMPLE_WALL_Z
                && x <= FOREST_TEMPLE_WALL_X_MAX
                && x >= FOREST_TEMPLE_WALL_X_MIN
                && y <= FOREST_TEMPLE_WALL_Y_MAX
                && y >= FOREST_TEMPLE_WALL_Y_MIN;
    }

    public static BlockKey forestTempleFloorBlock(int solutionIndex) {
        int row = Math.max(0, solutionIndex) / 2;
        int z = solutionIndex % 2 == 0 ? FOREST_TEMPLE_FLOOR_Z_EVEN : FOREST_TEMPLE_FLOOR_Z_ODD;
        return new BlockKey(FOREST_TEMPLE_FLOOR_X0 - row, FOREST_TEMPLE_FLOOR_Y, z);
    }

    public static int forestTempleTurns(Cardinal wallFacing, Cardinal floorFacing) {
        return signedQuarterTurns(floorFacing, forestTempleFloorFacing(wallFacing));
    }

    public static String templeHudLine(int pendingCells) {
        if (pendingCells <= 0) {
            return "";
        }
        return "Temple " + pendingCells + " tile" + (pendingCells == 1 ? "" : "s");
    }

    public static String sweepHudLine(int tabSweep, int clusterWood, int maxWood) {
        if (tabSweep <= 0 && maxWood <= 0) {
            return "";
        }
        if (maxWood > 0) {
            return "Sweep " + Math.max(tabSweep, 0) + " wood " + clusterWood + "/" + maxWood;
        }
        return "Sweep " + tabSweep;
    }

    public static boolean shouldMuteSound(
            boolean phantom,
            boolean treeBreak,
            boolean treeBreakAlsoGalatea,
            boolean fusion,
            boolean stereo,
            Island island,
            boolean nearbyMusicPants,
            String soundId,
            float volume) {
        return mutePhantom(phantom, island, soundId)
                || muteTreeBreak(treeBreak, treeBreakAlsoGalatea, island, soundId)
                || muteFusionMachine(fusion, island, soundId, volume)
                || muteStereoPants(stereo, island, nearbyMusicPants, soundId);
    }

    public static Cardinal forestTempleFloorFacing(Cardinal wallFacing) {
        if (wallFacing == null) {
            return null;
        }
        return switch (wallFacing) {
            case NORTH -> Cardinal.WEST;
            case EAST -> Cardinal.SOUTH;
            case SOUTH -> Cardinal.EAST;
            case WEST -> Cardinal.NORTH;
        };
    }

    public static int signedQuarterTurns(Cardinal from, Cardinal to) {
        if (from == null || to == null) {
            return 0;
        }
        int clockwise = Math.floorMod(to.ordinal() - from.ordinal(), 4);
        int counter = Math.floorMod(from.ordinal() - to.ordinal(), 4);
        if (clockwise == 0) {
            return 0;
        }
        return clockwise <= counter ? clockwise : -counter;
    }

    public static List<String> desertTempleButtonOrder(Map<String, Integer> colorCounts) {
        if (colorCounts == null || colorCounts.isEmpty()) {
            return List.of();
        }
        return colorCounts.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .toList();
    }

    public static boolean isTreeFelledChat(String line) {
        String text = strip(line);
        return text.equalsIgnoreCase("PETALFALL! You felled the entire Tree!")
                || text.equalsIgnoreCase("WOODPECKER! You felled the entire Tree!")
                || text.equalsIgnoreCase("TIMBER! You felled the entire Tree!");
    }

    public static boolean highlightLushlilac(Island island, String blockId) {
        return island == Island.GALATEA && containsBlock(blockId, "flowering_azalea");
    }

    public static boolean highlightSeaLumies(Island island, String blockId, int pickleCount, int minimum) {
        if (!customTrees(island) || !containsBlock(blockId, "sea_pickle")) {
            return false;
        }
        int min = Math.max(1, Math.min(4, minimum));
        return pickleCount >= min;
    }

    public static boolean highlightVeilshroom(Island island, String blockId) {
        return island == Island.TORRHUS && containsBlock(blockId, "crimson_fungus");
    }

    public static boolean highlightHoneyhive(
            Island island, String blockId, int honeyLevel, boolean birchFenceAbove) {
        return island == Island.TORRHUS
                && containsBlock(blockId, "bee_nest")
                && honeyLevel >= 5
                && !birchFenceAbove;
    }

    public static boolean isHoneyhiveLootChat(String line) {
        return strip(line).toLowerCase(Locale.ROOT).contains("stick your hand into the honeyhive");
    }

    public static boolean isQueenBeeChat(String line) {
        String text = strip(line).toUpperCase(Locale.ROOT);
        return text.contains("QUEEN BEE");
    }

    public static boolean muteStereoPants(
            boolean enabled, Island island, boolean nearbyMusicPants, String soundId) {
        if (!enabled || island != Island.GALATEA || !nearbyMusicPants) {
            return false;
        }
        return containsSound(soundId, "note_block") || containsSound(soundId, "block.note");
    }

    public static boolean shouldAutoChop(
            boolean moduleEnabled,
            boolean cheatEnabled,
            Island island,
            boolean holdingAxe,
            boolean lookingAtLog) {
        return moduleEnabled
                && cheatEnabled
                && chopTrees(island)
                && holdingAxe
                && lookingAtLog;
    }

    public static boolean shouldAxeToss(
            boolean moduleEnabled,
            boolean cheatEnabled,
            boolean throwableAxe,
            int clusterSize,
            int minCluster,
            boolean cooldownReady) {
        return moduleEnabled
                && cheatEnabled
                && throwableAxe
                && cooldownReady
                && clusterSize >= Math.max(1, minCluster);
    }

    public static boolean isInvisibugCrit(String particleId, int count, float speed) {
        if (particleId == null) {
            return false;
        }
        String id = particleId.toLowerCase(Locale.ROOT);
        return (id.equals("crit") || id.endsWith(":crit") || id.contains("crit"))
                && count == 1
                && speed <= 0.0001F;
    }

    public static boolean isHuntingBoxTitle(String title) {
        return strip(title).equalsIgnoreCase("Hunting Box");
    }

    public static boolean isStarlynShop(String title) {
        String text = strip(title);
        return text.equalsIgnoreCase("Agatha's Shop") || text.equalsIgnoreCase("Miria's Shop");
    }

    public static StarlynBracket parseStarlynBracket(String line) {
        Matcher matcher = STARLYN_BRACKET.matcher(strip(line));
        if (!matcher.find()) {
            return null;
        }
        return new StarlynBracket(matcher.group(1).trim(), matcher.group(2).trim().toUpperCase(Locale.ROOT));
    }

    public static String compactStarlynLine(StarlynBracket result) {
        if (result == null) {
            return "";
        }
        return result.sister() + ": " + result.bracket();
    }

    public static boolean isVerySpecialBracket(StarlynBracket result) {
        return result != null && result.bracket().contains("SPECIAL");
    }

    public static Double parseSweepAmount(String line) {
        Matcher matcher = SWEEP_AMOUNT.matcher(strip(line));
        if (!matcher.find()) {
            return null;
        }
        return Double.parseDouble(matcher.group(1).replace(",", ""));
    }

    public static String compactSweepLine(String treeType, String toughnessLogs, Double sweep) {
        String tree = treeType == null || treeType.isBlank() ? "?" : treeType;
        String tough = toughnessLogs == null || toughnessLogs.isBlank() ? "?" : toughnessLogs;
        String sw = sweep == null ? "?" : String.valueOf(sweep.intValue());
        return "Sweep " + sw + " · " + tree + " toughness " + tough;
    }

    public static String parseToughnessTree(String line) {
        Matcher matcher = TOUGHNESS_LINE.matcher(strip(line));
        return matcher.find() ? matcher.group(1) : null;
    }

    public static Integer parseCouponCost(String loreLine) {
        Matcher matcher = COUPON_COST.matcher(strip(loreLine));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1).replace(",", ""));
    }

    public static int couponProfit(int bazaarSell, int couponCost) {
        return bazaarSell - Math.max(0, couponCost);
    }

    public static Integer parseForestWhispers(String loreLine) {
        Matcher matcher = WHISPERS.matcher(strip(loreLine));
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1).replace(",", ""));
    }

    public static int whispersForTenLevels(int perLevelCost) {
        return Math.max(0, perLevelCost) * 10;
    }

    public static TutorialStep tutorialByQuestName(String quest) {
        if (quest == null) {
            return null;
        }
        String needle = strip(quest).toLowerCase(Locale.ROOT);
        for (TutorialStep step : PARK_TUTORIAL) {
            if (step.quest().toLowerCase(Locale.ROOT).equals(needle)) {
                return step;
            }
        }
        return null;
    }

    public static boolean hideonleafName(String nametag) {
        String text = strip(nametag).toLowerCase(Locale.ROOT);
        return text.contains("hideonleaf");
    }

    public static boolean hideonsunName(String nametag) {
        return strip(nametag).toLowerCase(Locale.ROOT).contains("hideonsun");
    }

    public static boolean shellwiseName(String nametag) {
        return strip(nametag).toLowerCase(Locale.ROOT).contains("shellwise");
    }

    public static boolean coralotName(String nametag) {
        return strip(nametag).toLowerCase(Locale.ROOT).contains("coralot");
    }

    public static boolean blueJayName(String nametag) {
        String text = strip(nametag).toLowerCase(Locale.ROOT);
        return text.contains("blue jay") || text.contains("bluejay");
    }

    public static boolean birriesName(String nametag) {
        return strip(nametag).equalsIgnoreCase("Birries");
    }

    public static boolean cinderbatName(String nametag) {
        return strip(nametag).toLowerCase(Locale.ROOT).contains("cinderbat");
    }

    public static boolean isHuntaxe(String skyblockId, String displayName) {
        String id = skyblockId == null ? "" : skyblockId.toUpperCase(Locale.ROOT);
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        return id.contains("HUNT") && (id.contains("AXE") || id.contains("CLEAVER") || id.contains("HATCHET"))
                || name.contains("huntaxe")
                || name.contains("hunting axe")
                || name.contains("hunter axe");
    }

    public static String parseShardGain(String chat) {
        String text = strip(chat);
        Matcher matcher = SHARD_GAIN.matcher(text);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1).trim();
    }

    public static String shardHudLine(int total) {
        if (total <= 0) {
            return "";
        }
        return "Shards " + total;
    }

    public static boolean lassoAlert(boolean enabled, boolean holdingLasso, boolean huntableNearby) {
        return enabled && holdingLasso && huntableNearby;
    }

    public enum HuntGlow {
        NONE,
        HIDEONLEAF,
        SHELLWISE,
        CORALOT,
        BIRRIES,
        HIDEONSUN,
        BLUE_JAY,
        CINDERBAT
    }

    /**
     * Galatea glow (green shulker / turtle / axolotl) and Torrhus
     * Hideonsun / Blue Jay, plus named armor stands.
     */
    public static HuntGlow huntGlow(Island island, String nametag, String entityType) {
        String type = entityType == null ? "" : entityType.toLowerCase(Locale.ROOT);
        if (hideonleafName(nametag) || (island == Island.GALATEA && type.contains("shulker"))) {
            return HuntGlow.HIDEONLEAF;
        }
        if (shellwiseName(nametag) || (island == Island.GALATEA && type.contains("turtle"))) {
            return HuntGlow.SHELLWISE;
        }
        if (coralotName(nametag) || (island == Island.GALATEA && type.contains("axolotl"))) {
            return HuntGlow.CORALOT;
        }
        if (island == Island.GALATEA && birriesName(nametag)) {
            return HuntGlow.BIRRIES;
        }
        if (hideonsunName(nametag) || (island == Island.TORRHUS && type.contains("shulker"))) {
            return HuntGlow.HIDEONSUN;
        }
        if (blueJayName(nametag) || (island == Island.TORRHUS && type.contains("parrot"))) {
            return HuntGlow.BLUE_JAY;
        }
        if (cinderbatName(nametag)) {
            return HuntGlow.CINDERBAT;
        }
        return HuntGlow.NONE;
    }

    public static int huntGlowColor(HuntGlow glow) {
        return switch (glow == null ? HuntGlow.NONE : glow) {
            case HIDEONLEAF -> 0xFF55FF55;
            case SHELLWISE -> 0xFF55FFFF;
            case CORALOT -> 0xFFFF88AA;
            case BIRRIES -> 0xFFAAFF55;
            case HIDEONSUN -> 0xFFFFAA00;
            case BLUE_JAY -> 0xFF5555FF;
            case CINDERBAT -> 0xFFFF5500;
            case NONE -> 0;
        };
    }

    public static boolean isFrogMask(String skyblockId, String displayName) {
        String id = skyblockId == null ? "" : skyblockId.toUpperCase(Locale.ROOT);
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        return id.contains("FROG_MASK") || id.contains("FROG MASK") || name.contains("frog mask");
    }

    public static boolean isLasso(String skyblockId, String displayName) {
        String id = skyblockId == null ? "" : skyblockId.toUpperCase(Locale.ROOT);
        String name = displayName == null ? "" : displayName.toLowerCase(Locale.ROOT);
        return id.contains("LASSO") || name.contains("lasso");
    }

    public static boolean showFrogMaskHud(boolean enabled, Island island, boolean wearingMask) {
        return enabled && island == Island.PARK && wearingMask;
    }

    public static boolean showLassoHud(boolean enabled, Island island, boolean holdingLasso) {
        return enabled && customTrees(island) && holdingLasso;
    }

    public static boolean isMoongladeBeacon(int x, int y, int z) {
        return x == MOONGLADE_BEACON_X && y == MOONGLADE_BEACON_Y && z == MOONGLADE_BEACON_Z;
    }

    public static boolean showSweepHud(boolean enabled, Island island) {
        return enabled && chopTrees(island);
    }

    private static boolean containsSound(String soundId, String needle) {
        return soundId != null && soundId.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean containsBlock(String blockId, String needle) {
        return blockId != null && blockId.toLowerCase(Locale.ROOT).contains(needle);
    }
}
