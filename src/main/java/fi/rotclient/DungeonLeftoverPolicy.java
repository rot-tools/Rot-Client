package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Leftover dungeon helpers for Serveri:
 * terminal click queue, ghost-block blacklist, secret/crystal
 * triggerbot, Auto Dojo chat, Superboom walls, Cheater Map darken, leap overlay
 * layout, and Blood Rush / Blood Open / Boss Enter timers.
 */
public final class DungeonLeftoverPolicy {
    public enum TriggerKind {
        NONE,
        CRYSTAL,
        SECRET
    }

    public enum DojoType {
        NONE,
        CONTROL,
        MASTERY,
        DISCIPLINE,
        FORCE
    }

    public enum SplitEvent {
        NONE,
        START,
        BLOOD_DOOR,
        BLOOD_CLEAR,
        BOSS_ENTER,
        RUN_END
    }

    public record QueuedClick(int slot, int button) {
    }

    public record LeapEntry(
            int slot,
            String name,
            DungeonPolicy.DungeonClass dungeonClass,
            boolean dead,
            String statusLabel) {
        public LeapEntry {
            name = name == null ? "" : name;
            dungeonClass = dungeonClass == null ? DungeonPolicy.DungeonClass.UNKNOWN : dungeonClass;
            statusLabel = statusLabel == null ? "" : statusLabel;
        }

        public LeapEntry(int slot, String name, DungeonPolicy.DungeonClass dungeonClass) {
            this(slot, name, dungeonClass, false, "");
        }

        public LeapEntry(
                int slot, String name, DungeonPolicy.DungeonClass dungeonClass, boolean dead) {
            this(slot, name, dungeonClass, dead, dead ? "DEAD" : "");
        }

        public static LeapEntry fromLore(
                int slot,
                String name,
                DungeonPolicy.DungeonClass dungeonClass,
                List<String> lore) {
            String label = DungeonPolicy.leapHeadLabel(lore);
            return new LeapEntry(slot, name, dungeonClass, !label.isBlank(), label);
        }
    }

    public record LeapCell(int index, int x, int y, int width, int height, LeapEntry entry) {
    }

    public record SplitSnapshot(
            long startMs,
            long bloodRushMs,
            long bloodOpenMs,
            long bossEnterMs,
            boolean bloodRushDone,
            boolean bloodOpenDone,
            boolean bossDone) {
        public List<String> hudLines(long now) {
            return hudLines(now, Map.of());
        }

        public List<String> hudLines(long now, Map<String, Long> pbs) {
            List<String> lines = new ArrayList<>();
            if (startMs <= 0L) {
                return lines;
            }
            lines.add("Blood Rush: " + format(bloodRushDone ? bloodRushMs : now - startMs)
                    + pbSuffix("BLOOD_RUSH", pbs, bloodRushDone));
            if (bloodRushDone) {
                lines.add("Blood Open: " + format(bloodOpenDone ? bloodOpenMs : now - startMs)
                        + pbSuffix("BLOOD_OPEN", pbs, bloodOpenDone));
            }
            if (bloodOpenDone) {
                lines.add("Boss Enter: " + format(bossDone ? bossEnterMs : now - startMs)
                        + pbSuffix("BOSS_ENTER", pbs, bossDone));
            }
            return lines;
        }
    }

    public static final int QUEUE_MAX = 16;
    public static final int DEFAULT_QUEUE_DELAY = 2;
    public static final int DEFAULT_TRIGGER_DELAY_MS = 200;
    public static final int MIN_TRIGGER_DELAY_MS = 0;
    public static final int MAX_TRIGGER_DELAY_MS = 1000;
    public static final double DEFAULT_CHEATER_DARKEN = 0.6D;
    public static final int DEFAULT_SUPERBOOM_DELAY = 2;
    public static final int LEAP_CELL_WIDTH = 140;
    public static final int LEAP_CELL_HEIGHT = 72;
    public static final int LEAP_GAP = 8;

    private static final Set<String> GHOST_BLACKLIST = Set.of(
            "chest", "trapped_chest", "ender_chest", "barrel",
            "lever", "repeater", "comparator", "hopper",
            "furnace", "blast_furnace", "smoker", "brewing_stand",
            "enchanting_table", "anvil", "chipped_anvil", "damaged_anvil",
            "crafting_table", "beacon", "bedrock", "barrier", "command_block",
            "player_head", "player_wall_head", "skeleton_skull", "wither_skeleton_skull",
            "note_block", "daylight_detector", "dispenser", "dropper",
            "end_portal", "end_portal_frame", "moving_piston");
    private static final Set<String> SUPERBOOM_WALLS = Set.of(
            "cracked_stone_bricks",
            "stone_bricks",
            "mossy_stone_bricks",
            "infested_cracked_stone_bricks",
            "infested_stone_bricks",
            "cobblestone",
            "mossy_cobblestone",
            "andesite",
            "coal_block",
            "barrier");
    private static final Set<String> SECRET_BLOCKS = Set.of(
            "chest", "trapped_chest", "lever", "player_head", "player_wall_head",
            "skeleton_skull", "wither_skeleton_skull", "stone_button", "oak_button",
            "birch_button", "spruce_button", "jungle_button", "acacia_button",
            "dark_oak_button", "polished_blackstone_button");

    private DungeonLeftoverPolicy() {
    }

    public static boolean queueTermsSupported() {
        return true;
    }

    public static boolean shouldEnqueueTerminalClick(
            boolean moduleOn, boolean queueOn, boolean terminalOpen) {
        return moduleOn && queueOn && terminalOpen;
    }

    public static void enqueue(Queue<QueuedClick> queue, int slot, int button) {
        if (queue == null || slot < 0) {
            return;
        }
        if (queue.size() >= QUEUE_MAX) {
            queue.poll();
        }
        queue.add(new QueuedClick(slot, button));
    }

    public static Optional<QueuedClick> dequeueIfReady(Queue<QueuedClick> queue, int cooldown) {
        if (queue == null || queue.isEmpty() || cooldown > 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(queue.poll());
    }

    public static boolean isGhostBlacklisted(String blockId) {
        String id = path(blockId);
        if (id.isBlank() || "air".equals(id) || "cave_air".equals(id) || "void_air".equals(id)) {
            return true;
        }
        if (id.endsWith("_door") || id.endsWith("_bed") || id.endsWith("_button")
                || id.endsWith("_trapdoor") || id.endsWith("_sign") || id.endsWith("_wall_sign")
                || id.endsWith("_head") || id.endsWith("_skull") || id.contains("mushroom")) {
            return true;
        }
        return GHOST_BLACKLIST.contains(id);
    }

    public static boolean canGhostBlock(
            boolean moduleOn, boolean cheatOn, boolean uayor, String blockId) {
        return moduleOn && cheatOn && uayor && !isGhostBlacklisted(blockId);
    }

    public static boolean isPickaxe(String itemId, String name) {
        String id = path(itemId);
        String text = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return id.contains("pickaxe") || text.contains("pickaxe") || text.contains("stonk");
    }

    public static boolean isSuperboomWall(String blockId) {
        return SUPERBOOM_WALLS.contains(path(blockId));
    }

    public static boolean shouldAutoSuperboom(
            boolean moduleOn, boolean cheatOn, boolean lookingAtWall, boolean hasSuperboom) {
        return moduleOn && cheatOn && lookingAtWall && hasSuperboom;
    }

    public static TriggerKind triggerKind(
            boolean crystalOn,
            boolean secretOn,
            boolean lookingCrystal,
            String blockId,
            String hologram) {
        if (secretOn && (SECRET_BLOCKS.contains(path(blockId))
                || TempleDungeonPolicy.isSecretBlockId(blockId))) {
            return TriggerKind.SECRET;
        }
        if (crystalOn && lookingCrystal) {
            return TriggerKind.CRYSTAL;
        }
        if (crystalOn) {
            String text = hologram == null ? "" : hologram.toLowerCase(Locale.ROOT);
            if (text.contains("energy crystal") || "end_crystal".equals(path(blockId))) {
                return TriggerKind.CRYSTAL;
            }
        }
        return TriggerKind.NONE;
    }

    public static boolean isEnergyCrystalName(String name) {
        String text = name == null ? "" : name.toLowerCase(Locale.ROOT);
        return text.contains("energy crystal");
    }

    public static int clampTriggerDelay(int millis) {
        return Math.max(MIN_TRIGGER_DELAY_MS, Math.min(MAX_TRIGGER_DELAY_MS, millis));
    }

    public static DojoType dojoTypeFromChat(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        if (text.isBlank() || dojoChatClears(chat)) {
            return DojoType.NONE;
        }
        if (text.contains("test of control")) {
            return DojoType.CONTROL;
        }
        if (text.contains("test of mastery")) {
            return DojoType.MASTERY;
        }
        if (text.contains("test of discipline")) {
            return DojoType.DISCIPLINE;
        }
        if (text.contains("test of force")) {
            return DojoType.FORCE;
        }
        return DojoType.NONE;
    }

    public static boolean dojoChatClears(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        return text.contains("returned to the dojo")
                || (text.contains("completed") && text.contains("dojo"))
                || (text.contains("failed") && text.contains("test of"));
    }

    public static String disciplineSwordForHelmet(String helmetId) {
        String id = path(helmetId);
        if (id.contains("leather")) {
            return "wooden_sword";
        }
        if (id.contains("iron")) {
            return "iron_sword";
        }
        if (id.contains("golden") || id.contains("gold")) {
            return "golden_sword";
        }
        if (id.contains("diamond")) {
            return "diamond_sword";
        }
        return "";
    }

    public static SplitEvent splitEvent(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return SplitEvent.NONE;
        }
        if (DungeonPolicy.isDungeonEnd(chat)) {
            return SplitEvent.RUN_END;
        }
        if (text.contains("[boss]")
                || text.contains("the boss room")
                || text.contains("entered boss")
                || text.contains("necron's hollow")
                || text.contains("maxor") && text.contains("is enraged")) {
            return SplitEvent.BOSS_ENTER;
        }
        if (TempleDungeonPolicy.doorKeyEvent(chat) == TempleDungeonPolicy.DoorKeyEvent.BLOOD_DOOR_OPEN
                || text.contains("the blood door has been opened")) {
            return SplitEvent.BLOOD_DOOR;
        }
        if (DungeonPolicy.isBloodCampReady(chat)
                || text.contains("that will be enough")
                || text.contains("you have proven yourself")) {
            return SplitEvent.BLOOD_CLEAR;
        }
        if (text.contains("dungeon starts")
                || text.contains("starting in 1 second")
                || text.contains("the dungeon has started")
                || text.contains("has started the dungeon")) {
            return SplitEvent.START;
        }
        return SplitEvent.NONE;
    }

    public static SplitSnapshot applySplit(SplitSnapshot current, SplitEvent event, long now) {
        SplitSnapshot state = current == null
                ? new SplitSnapshot(0L, 0L, 0L, 0L, false, false, false)
                : current;
        return switch (event) {
            case START -> new SplitSnapshot(now, 0L, 0L, 0L, false, false, false);
            case RUN_END -> state;
            case BLOOD_DOOR -> {
                if (state.startMs() <= 0L) {
                    yield new SplitSnapshot(now, 0L, 0L, 0L, true, false, false);
                }
                yield new SplitSnapshot(
                        state.startMs(), now - state.startMs(), state.bloodOpenMs(),
                        state.bossEnterMs(), true, state.bloodOpenDone(), state.bossDone());
            }
            case BLOOD_CLEAR -> {
                if (state.startMs() <= 0L) {
                    yield state;
                }
                yield new SplitSnapshot(
                        state.startMs(),
                        state.bloodRushDone() ? state.bloodRushMs() : now - state.startMs(),
                        now - state.startMs(),
                        state.bossEnterMs(),
                        true,
                        true,
                        state.bossDone());
            }
            case BOSS_ENTER -> {
                if (state.startMs() <= 0L) {
                    yield state;
                }
                yield new SplitSnapshot(
                        state.startMs(),
                        state.bloodRushDone() ? state.bloodRushMs() : now - state.startMs(),
                        state.bloodOpenDone() ? state.bloodOpenMs() : now - state.startMs(),
                        now - state.startMs(),
                        true,
                        true,
                        true);
            }
            case NONE -> state;
        };
    }

    public static int darkenArgb(int argb, double factor) {
        double scale = Double.isNaN(factor) ? DEFAULT_CHEATER_DARKEN : Math.max(0.0D, Math.min(1.0D, factor));
        int a = (argb >>> 24) & 0xFF;
        int r = (int) (((argb >>> 16) & 0xFF) * scale);
        int g = (int) (((argb >>> 8) & 0xFF) * scale);
        int b = (int) ((argb & 0xFF) * scale);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static DungeonMapPolicy.Board revealHiddenRooms(DungeonMapPolicy.Board board) {
        if (board == null || !board.calibration().ok()) {
            return board;
        }
        Map<String, DungeonMapPolicy.RoomTile> byTile = new LinkedHashMap<>();
        for (DungeonMapPolicy.RoomTile room : board.rooms()) {
            byTile.put(room.tileX() + "," + room.tileZ(), room);
        }
        List<DungeonMapPolicy.RoomTile> extra = new ArrayList<>();
        DungeonMapPolicy.Calibration cal = board.calibration();
        for (DungeonMapPolicy.DoorTile door : board.doors()) {
            int tx = door.fromX() + (door.horizontal() ? 1 : 0);
            int tz = door.fromZ() + (door.horizontal() ? 0 : 1);
            String key = tx + "," + tz;
            if (byTile.containsKey(key) || tx < 0 || tz < 0 || tx > 5 || tz > 5) {
                continue;
            }
            int originX = cal.startX() + tx * cal.roomGap();
            int originZ = cal.startZ() + tz * cal.roomGap();
            DungeonMapPolicy.RoomTile hidden = new DungeonMapPolicy.RoomTile(
                    tx, tz, DungeonMapPolicy.RoomType.UNDISCOVERED,
                    DungeonMapPolicy.Checkmark.QUESTION, originX, originZ);
            extra.add(hidden);
            byTile.put(key, hidden);
        }
        if (extra.isEmpty()) {
            return board;
        }
        List<DungeonMapPolicy.RoomTile> rooms = new ArrayList<>(board.rooms());
        rooms.addAll(extra);
        return new DungeonMapPolicy.Board(
                board.calibration(), rooms, board.doors(), board.players(),
                board.summary() + " · hidden " + extra.size());
    }

    public static String cheaterRoomLabel(DungeonMapPolicy.RoomTile room) {
        if (room == null) {
            return "";
        }
        return switch (room.type()) {
            case PUZZLE -> "Puzzle";
            case TRAP -> "Trap";
            case FAIRY -> "Fairy";
            case BLOOD -> "Blood";
            case MINIBOSS -> "Mini";
            case ENTRANCE -> "Entry";
            case UNDISCOVERED -> "?";
            case UNKNOWN -> "?";
            case NORMAL -> room.checkmark() == DungeonMapPolicy.Checkmark.NONE ? "Room" : "";
            case EMPTY -> "";
        };
    }

    public static List<LeapCell> leapOverlay(
            int screenWidth, int screenHeight, List<LeapEntry> players) {
        List<LeapEntry> source = players == null ? List.of() : players;
        List<LeapEntry> sorted = new ArrayList<>(source);
        sorted.sort((a, b) -> {
            if (a.dead() != b.dead()) {
                return a.dead() ? 1 : -1;
            }
            return Integer.compare(
                    DungeonPolicy.leapSortKey(a.dungeonClass()),
                    DungeonPolicy.leapSortKey(b.dungeonClass()));
        });
        int cols = 2;
        int rows = 2;
        int totalW = cols * LEAP_CELL_WIDTH + LEAP_GAP;
        int totalH = rows * LEAP_CELL_HEIGHT + LEAP_GAP;
        int originX = Math.max(0, (screenWidth - totalW) / 2);
        int originY = Math.max(0, (screenHeight - totalH) / 2);
        List<LeapCell> cells = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int col = i % cols;
            int row = i / cols;
            LeapEntry entry = i < sorted.size() ? sorted.get(i) : null;
            cells.add(new LeapCell(
                    i,
                    originX + col * (LEAP_CELL_WIDTH + LEAP_GAP),
                    originY + row * (LEAP_CELL_HEIGHT + LEAP_GAP),
                    LEAP_CELL_WIDTH,
                    LEAP_CELL_HEIGHT,
                    entry));
        }
        return cells;
    }

    /**
     * Digit 1-4: custom GUI uses class-sorted overlay cells. Vanilla Leap
     * uses chest order of living heads so keys match the Hypixel menu.
     */
    public static Optional<LeapEntry> leapDigitTarget(
            List<LeapEntry> players, int digit, boolean customGui) {
        if (digit < 1 || digit > 4) {
            return Optional.empty();
        }
        if (customGui) {
            List<LeapCell> cells = leapOverlay(400, 240, players);
            LeapEntry entry = cells.get(digit - 1).entry();
            return entry == null ? Optional.empty() : Optional.of(entry);
        }
        List<LeapEntry> living = new ArrayList<>();
        if (players != null) {
            for (LeapEntry entry : players) {
                if (entry != null && !entry.dead()) {
                    living.add(entry);
                }
            }
        }
        int index = digit - 1;
        if (index >= living.size()) {
            return Optional.empty();
        }
        return Optional.of(living.get(index));
    }

    public static Optional<LeapCell> cellAt(List<LeapCell> cells, int mouseX, int mouseY) {
        if (cells == null) {
            return Optional.empty();
        }
        for (LeapCell cell : cells) {
            if (mouseX >= cell.x() && mouseX < cell.x() + cell.width()
                    && mouseY >= cell.y() && mouseY < cell.y() + cell.height()
                    && cell.entry() != null) {
                return Optional.of(cell);
            }
        }
        return Optional.empty();
    }

    public static int leapPanelColor(DungeonPolicy.DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case ARCHER -> 0xCCFFAA00;
            case MAGE -> 0xCC55FFFF;
            case BERSERK -> 0xCCFF5555;
            case TANK -> 0xCC55FF55;
            case HEALER -> 0xCCFF55FF;
            case UNKNOWN -> 0xCC334155;
        };
    }

    public static Map<TermSimPolicy.Kind, Integer> parsePersonalBests(String stored) {
        EnumMap<TermSimPolicy.Kind, Integer> map = new EnumMap<>(TermSimPolicy.Kind.class);
        if (stored == null || stored.isBlank()) {
            return map;
        }
        for (String part : stored.split(",")) {
            String[] bits = part.split("=");
            if (bits.length != 2) {
                continue;
            }
            try {
                TermSimPolicy.Kind kind = TermSimPolicy.Kind.valueOf(bits[0].trim().toUpperCase(Locale.ROOT));
                int millis = Integer.parseInt(bits[1].trim());
                if (kind != TermSimPolicy.Kind.HUB && millis > 0) {
                    map.put(kind, millis);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return map;
    }

    public static String writePersonalBests(Map<TermSimPolicy.Kind, Integer> pbs) {
        if (pbs == null || pbs.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<TermSimPolicy.Kind, Integer> entry : pbs.entrySet()) {
            if (entry.getKey() == TermSimPolicy.Kind.HUB || entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(',');
            }
            out.append(entry.getKey().name()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    public static boolean recordPersonalBest(
            Map<TermSimPolicy.Kind, Integer> pbs, TermSimPolicy.Kind kind, int millis) {
        if (pbs == null || kind == null || kind == TermSimPolicy.Kind.HUB || millis <= 0) {
            return false;
        }
        Integer previous = pbs.get(kind);
        if (previous != null && previous > 0 && millis >= previous) {
            return false;
        }
        pbs.put(kind, millis);
        return true;
    }

    public static String hubSlotName(TermSimPolicy.Kind kind, Map<TermSimPolicy.Kind, Integer> pbs) {
        String base = TermSimPolicy.titleFor(kind == null ? TermSimPolicy.Kind.HUB : kind);
        if (kind == null || kind == TermSimPolicy.Kind.HUB || pbs == null) {
            return base;
        }
        Integer pb = pbs.get(kind);
        if (pb == null || pb <= 0) {
            return base;
        }
        return base + " §e" + format(pb);
    }

    public static Queue<QueuedClick> newQueue() {
        return new ArrayDeque<>();
    }

    public static Map<String, Long> parseSplitTimes(String stored) {
        return KuudraSplitPolicy.parseTimes(stored);
    }

    public static String writeSplitTimes(Map<String, Long> times) {
        return KuudraSplitPolicy.writeTimes(times);
    }

    public static boolean recordSplitPersonalBest(Map<String, Long> pbs, String key, long millis) {
        if (pbs == null || key == null || key.isBlank() || millis <= 0L) {
            return false;
        }
        String id = key.trim().toUpperCase(Locale.ROOT);
        Long previous = pbs.get(id);
        if (previous != null && previous > 0L && millis >= previous) {
            return false;
        }
        pbs.put(id, millis);
        return true;
    }

    public static String splitPbKey(String floor, String kind) {
        String split = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        String fl = floor == null ? "" : floor.trim().toUpperCase(Locale.ROOT);
        if (fl.isBlank() || "UNKNOWN".equals(fl)) {
            return split;
        }
        return fl + "." + split;
    }

    private static String pbSuffix(String key, Map<String, Long> pbs, boolean finished) {
        if (!finished || pbs == null || key == null) {
            return "";
        }
        Long pb = pbs.get(key);
        if (pb == null || pb <= 0L) {
            return "";
        }
        return " (PB " + format(pb) + ")";
    }

    private static String format(long millis) {
        if (millis < 0L) {
            millis = 0L;
        }
        return String.format(Locale.ROOT, "%.2fs", millis / 1000.0D);
    }

    public static String path(String blockId) {
        if (blockId == null) {
            return "";
        }
        String id = blockId.toLowerCase(Locale.ROOT).trim();
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
