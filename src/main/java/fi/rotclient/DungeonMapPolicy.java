package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Magical Map scan of room-grid/doors/checkmarks,
 * unique-room grouping for the named HUD, and world-to-map player placement.
 */
public final class DungeonMapPolicy {
    public enum RoomType {
        EMPTY,
        ENTRANCE,
        NORMAL,
        PUZZLE,
        TRAP,
        FAIRY,
        BLOOD,
        MINIBOSS,
        UNKNOWN,
        UNDISCOVERED
    }

    public enum DoorType {
        NONE,
        NORMAL,
        WITHER,
        OPENED_WITHER,
        BLOOD,
        FAIRY,
        ENTRANCE
    }

    public enum Checkmark {
        NONE,
        WHITE,
        GREEN,
        RED,
        QUESTION
    }

    public record Calibration(boolean ok, int startX, int startZ, int roomSize, int roomGap, double coordMul) {
        public static Calibration none() {
            return new Calibration(false, 0, 0, 16, 20, 0.625D);
        }
    }

    public record RoomTile(
            int tileX,
            int tileZ,
            RoomType type,
            Checkmark checkmark,
            int pixelX,
            int pixelZ) {
    }

    public record DoorTile(int fromX, int fromZ, boolean horizontal, DoorType type) {
    }

    /**
     * Magical Map door projected into Catacombs world space (grid origin -200).
     * {@code max*} are exclusive, like an AABB.
     */
    public record WorldDoor(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, DoorType type) {
    }

    public record PlayerIcon(int mapX, int mapZ, float yaw, boolean self, String classKey, String name) {
        public PlayerIcon(int mapX, int mapZ, float yaw, boolean self) {
            this(mapX, mapZ, yaw, self, "", "");
        }

        public PlayerIcon(int mapX, int mapZ, float yaw, boolean self, String classKey) {
            this(mapX, mapZ, yaw, self, classKey, "");
        }
    }

    /**
     * Magical Map decorations: FRAME is the local player, other unnamed markers
     * are teammates in tab/sidebar order.
     */
    public record MapDecorationHint(int mapX, int mapZ, float yaw, boolean selfMarker, String name) {
        public MapDecorationHint {
            name = name == null ? "" : name;
        }
    }

    public record Board(
            Calibration calibration,
            List<RoomTile> rooms,
            List<DoorTile> doors,
            List<PlayerIcon> players,
            String summary) {
        public Board {
            rooms = rooms == null ? List.of() : List.copyOf(rooms);
            doors = doors == null ? List.of() : List.copyOf(doors);
            players = players == null ? List.of() : List.copyOf(players);
            summary = summary == null ? "" : summary;
        }
    }

    public record RoomIdentity(String name, int secrets, RoomType type) {
        public RoomIdentity(String name, int secrets) {
            this(name, secrets, RoomType.EMPTY);
        }

        public RoomIdentity {
            name = name == null ? "" : name;
            secrets = Math.max(0, secrets);
            type = type == null ? RoomType.EMPTY : type;
        }

        public boolean typed() {
            return type != RoomType.EMPTY
                    && type != RoomType.UNKNOWN
                    && type != RoomType.UNDISCOVERED;
        }
    }

    public record UniqueRoom(
            int minTileX,
            int minTileZ,
            int maxTileX,
            int maxTileZ,
            List<RoomTile> tiles,
            RoomType type,
            Checkmark checkmark) {
        public UniqueRoom {
            tiles = tiles == null ? List.of() : List.copyOf(tiles);
            type = type == null ? RoomType.EMPTY : type;
            checkmark = checkmark == null ? Checkmark.NONE : checkmark;
        }

        public boolean contains(int tileX, int tileZ) {
            for (RoomTile tile : tiles) {
                if (tile.tileX() == tileX && tile.tileZ() == tileZ) {
                    return true;
                }
            }
            return false;
        }
    }

    public record HudRect(int x, int y, int w, int h) {
    }

    public record HudRoom(
            UniqueRoom room,
            List<HudRect> rects,
            int labelX,
            int labelY,
            String name,
            int secretsFound,
            int secretsTotal,
            int color) {
        public HudRoom {
            rects = rects == null ? List.of() : List.copyOf(rects);
            name = name == null ? "" : name;
        }

        public boolean secretsComplete() {
            return secretsTotal > 0 && secretsFound >= secretsTotal;
        }
    }

    public record HudDoor(int x, int y, int w, int h, int color) {
    }

    public record HudMarker(int x, int y, float yaw, boolean self, String classKey, String name) {
        public HudMarker {
            classKey = classKey == null ? "" : classKey;
            name = name == null ? "" : name;
        }
    }

    public record Schematic(
            int width,
            int height,
            List<HudRoom> rooms,
            List<HudDoor> doors,
            List<HudMarker> players) {
        public Schematic {
            rooms = rooms == null ? List.of() : List.copyOf(rooms);
            doors = doors == null ? List.of() : List.copyOf(doors);
            players = players == null ? List.of() : List.copyOf(players);
        }

        public static Schematic empty() {
            return new Schematic(0, 0, List.of(), List.of(), List.of());
        }

        public boolean present() {
            return width > 0 && height > 0 && !rooms.isEmpty();
        }
    }

    public static final int HUD_MAP = 128;
    public static final int HUD_EXTRA = 12;
    public static final int HUD_CELL = 24;
    public static final int HUD_CELL_MIN = 12;
    public static final int HUD_CELL_MAX = 40;
    public static final int HUD_GAP = 6;
    public static final int HUD_PAD = 8;
    public static final int HUD_BG = 0xE00B0B0B;
    public static final int HUD_BORDER = 0xFF2A2A2A;
    public static final byte COLOR_ENTRANCE = 30;
    public static final byte COLOR_BLOOD = 18;
    public static final byte COLOR_PUZZLE = 66;
    public static final byte COLOR_TRAP = 62;
    public static final byte COLOR_FAIRY = 82;
    public static final byte COLOR_MINIBOSS = 74;
    public static final byte COLOR_NORMAL = 63;
    public static final byte COLOR_UNKNOWN = 85;
    public static final byte COLOR_WITHER_DOOR = 119;
    public static final byte COLOR_OPENED_WITHER = 51;
    public static final byte COLOR_WHITE_CHECK = 34;

    private static final int MAP = 128;
    public static final int GRID = 6;

    private DungeonMapPolicy() {
    }

    public static RoomType roomType(byte color) {
        return switch (color) {
            case 0 -> RoomType.EMPTY;
            case COLOR_ENTRANCE -> RoomType.ENTRANCE;
            case COLOR_BLOOD -> RoomType.BLOOD;
            case COLOR_PUZZLE -> RoomType.PUZZLE;
            case COLOR_TRAP -> RoomType.TRAP;
            case COLOR_FAIRY -> RoomType.FAIRY;
            case COLOR_MINIBOSS -> RoomType.MINIBOSS;
            case COLOR_NORMAL -> RoomType.NORMAL;
            case COLOR_UNKNOWN -> RoomType.UNDISCOVERED;
            case COLOR_WHITE_CHECK -> RoomType.UNKNOWN;
            default -> RoomType.EMPTY;
        };
    }

    public static DoorType doorType(byte color) {
        int value = color & 0xFF;
        if (value == (COLOR_WITHER_DOOR & 0xFF)) {
            return DoorType.WITHER;
        }
        if (value == (COLOR_OPENED_WITHER & 0xFF) || value == 52 || value == 53) {
            return DoorType.OPENED_WITHER;
        }
        if (value == (COLOR_BLOOD & 0xFF)) {
            return DoorType.BLOOD;
        }
        if (value == (COLOR_FAIRY & 0xFF)) {
            return DoorType.FAIRY;
        }
        if (value == (COLOR_ENTRANCE & 0xFF)) {
            return DoorType.ENTRANCE;
        }
        if (value == 0) {
            return DoorType.NONE;
        }
        return DoorType.NORMAL;
    }

    public static Checkmark checkmark(byte roomColor, byte centerColor) {
        int center = centerColor & 0xFF;
        if (center == (COLOR_UNKNOWN & 0xFF) || center == (COLOR_WITHER_DOOR & 0xFF)) {
            return Checkmark.QUESTION;
        }
        if (centerColor == roomColor) {
            return Checkmark.NONE;
        }
        return switch (centerColor) {
            case COLOR_BLOOD -> Checkmark.RED;
            case COLOR_ENTRANCE -> Checkmark.GREEN;
            case COLOR_WHITE_CHECK -> Checkmark.WHITE;
            default -> Checkmark.NONE;
        };
    }

    public static int argb(RoomType type) {
        return argb(type, Checkmark.NONE);
    }

    public static int argb(RoomType type, Checkmark mark) {
        if (type == RoomType.UNDISCOVERED || type == RoomType.UNKNOWN) {
            return 0xFF414141;
        }
        return switch (type) {
            case EMPTY -> 0xFF111111;
            case ENTRANCE -> 0xFF00FF00;
            case NORMAL -> 0xFF794600;
            case PUZZLE -> 0xFF7B007B;
            case TRAP -> 0xFFFF8200;
            case FAIRY -> 0xFFE39BE2;
            case BLOOD -> 0xFFB20000;
            case MINIBOSS -> 0xFFFFC800;
            case UNKNOWN -> 0xFFB2B2B2;
            case UNDISCOVERED -> 0xFF414141;
        };
    }

    public static int doorArgb(DoorType type) {
        return switch (type) {
            case NONE -> 0x00000000;
            case NORMAL -> 0xFF794600;
            case WITHER -> 0xFF101010;
            case OPENED_WITHER -> 0xFF794600;
            case BLOOD -> 0xFFB20000;
            case FAIRY -> 0xFFE39BE2;
            case ENTRANCE -> 0xFF00FF00;
        };
    }

    public static int labelArgb(Checkmark mark) {
        return switch (mark) {
            case GREEN -> 0xFF55FF55;
            case RED -> 0xFFFF0000;
            case WHITE -> 0xFFFFFFFF;
            case QUESTION, NONE -> 0xFFAAAAAA;
        };
    }

    public static List<String> nameLines(String name) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        String[] parts = name.trim().split(" ");
        List<String> lines = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                lines.add(part);
            }
        }
        return lines;
    }

    public static int floorNumber(String floor) {
        if (floor == null || floor.isBlank()) {
            return -1;
        }
        String text = floor.trim();
        if ("E".equalsIgnoreCase(text) || "Entrance".equalsIgnoreCase(text)) {
            return 0;
        }
        for (int i = text.length() - 1; i >= 0; i--) {
            char ch = text.charAt(i);
            if (Character.isDigit(ch)) {
                int value = ch - '0';
                if (i > 0 && Character.isDigit(text.charAt(i - 1))) {
                    value = (text.charAt(i - 1) - '0') * 10 + value;
                }
                return value;
            }
        }
        return -1;
    }

    public static int roomWorldOrigin(int tile) {
        return tile * DungeonRoomDataPolicy.ROOM_SPAN - DungeonRoomDataPolicy.DUNGEON_WORLD_SHIFT;
    }

    public static int roomWorldCenter(int tile) {
        return roomWorldOrigin(tile) + DungeonRoomDataPolicy.ROOM_HALF;
    }

    public static List<WorldDoor> worldDoors(Board board) {
        if (board == null || board.doors().isEmpty()) {
            return List.of();
        }
        List<WorldDoor> out = new ArrayList<>();
        int floorY = 69;
        int height = 4;
        for (DoorTile door : board.doors()) {
            if (!isWorldEspDoor(door.type())) {
                continue;
            }
            int centerA = roomWorldCenter(door.fromX());
            int centerB = roomWorldCenter(door.fromZ());
            if (door.horizontal()) {
                int x = roomWorldOrigin(door.fromX() + 1) - 1;
                out.add(new WorldDoor(x, floorY, centerB - 1, x + 1, floorY + height, centerB + 2, door.type()));
            } else {
                int z = roomWorldOrigin(door.fromZ() + 1) - 1;
                out.add(new WorldDoor(centerA - 1, floorY, z, centerA + 2, floorY + height, z + 1, door.type()));
            }
        }
        return List.copyOf(out);
    }

    public static final String MAP_MODE_EXPLORED = "Explored";
    public static final String MAP_MODE_REVEAL = "Reveal Hidden";
    public static final List<String> MAP_MODES = List.of(MAP_MODE_EXPLORED, MAP_MODE_REVEAL);

    public static String normalizeMapMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MAP_MODE_EXPLORED;
        }
        String text = raw.trim();
        if (text.equalsIgnoreCase(MAP_MODE_REVEAL)
                || text.equalsIgnoreCase("Cheater")
                || text.equalsIgnoreCase("Reveal")
                || text.equalsIgnoreCase("Hidden")) {
            return MAP_MODE_REVEAL;
        }
        return MAP_MODE_EXPLORED;
    }

    public static boolean revealsHidden(String mode) {
        return MAP_MODE_REVEAL.equals(normalizeMapMode(mode));
    }

    public static int clampHudCell(int cell) {
        return Math.max(HUD_CELL_MIN, Math.min(HUD_CELL_MAX, cell));
    }

    public static int hudExtent(int tiles, int cell, int gap, int pad) {
        int count = Math.max(0, tiles);
        if (count <= 0) {
            return 0;
        }
        return pad * 2 + count * cell + Math.max(0, count - 1) * gap;
    }

    public static boolean samePlayerName(String left, String right) {
        return left != null
                && right != null
                && !left.isBlank()
                && left.trim().equalsIgnoreCase(right.trim());
    }

    /**
     * Magical Map player dots rarely carry names. Skip the FRAME/self marker,
     * then bind remaining decorations to the dungeon roster excluding self.
     */
    public static List<PlayerIcon> assignTeammateIcons(
            List<MapDecorationHint> decorations,
            List<String> teammateOrder,
            String selfName,
            Map<String, DungeonPolicy.DungeonClass> classes,
            boolean classIcons,
            boolean headMarkers) {
        List<PlayerIcon> icons = new ArrayList<>();
        if (decorations == null || decorations.isEmpty()) {
            return icons;
        }
        List<String> remaining = new ArrayList<>();
        if (teammateOrder != null) {
            for (String teammate : teammateOrder) {
                if (teammate != null && !teammate.isBlank() && !samePlayerName(teammate, selfName)) {
                    remaining.add(teammate);
                }
            }
        }
        for (MapDecorationHint hint : decorations) {
            if (hint == null || hint.selfMarker()) {
                continue;
            }
            String name = hint.name() == null ? "" : hint.name().trim();
            if (samePlayerName(name, selfName)) {
                continue;
            }
            if (name.isBlank()) {
                if (remaining.isEmpty()) {
                    continue;
                }
                name = remaining.remove(0);
            } else {
                String assigned = name;
                remaining.removeIf(teammate -> samePlayerName(teammate, assigned));
            }
            icons.add(new PlayerIcon(
                    hint.mapX(),
                    hint.mapZ(),
                    hint.yaw(),
                    false,
                    mapClassKey(name, classes, classIcons, headMarkers),
                    name));
        }
        return List.copyOf(icons);
    }

    private static String mapClassKey(
            String name,
            Map<String, DungeonPolicy.DungeonClass> classes,
            boolean classIcons,
            boolean headMarkers) {
        if (!classIcons && !headMarkers) {
            return "";
        }
        DungeonPolicy.DungeonClass found = DungeonPolicy.DungeonClass.UNKNOWN;
        if (classes != null && name != null && !name.isBlank()) {
            found = classes.getOrDefault(name, DungeonPolicy.DungeonClass.UNKNOWN);
            if (found == DungeonPolicy.DungeonClass.UNKNOWN) {
                for (Map.Entry<String, DungeonPolicy.DungeonClass> entry : classes.entrySet()) {
                    if (samePlayerName(entry.getKey(), name)) {
                        found = entry.getValue();
                        break;
                    }
                }
            }
        }
        if (found == DungeonPolicy.DungeonClass.UNKNOWN) {
            found = DungeonPolicy.dungeonClass(name);
        }
        if (found == DungeonPolicy.DungeonClass.UNKNOWN) {
            return headMarkers ? "head" : "";
        }
        return found.name().toLowerCase(Locale.ROOT);
    }

    public static String tileKey(int tileX, int tileZ) {
        return tileX + "," + tileZ;
    }

    public static int tileFromWorld(int world) {
        return Math.floorDiv(world + DungeonRoomDataPolicy.DUNGEON_WORLD_SHIFT, DungeonRoomDataPolicy.ROOM_SPAN);
    }

    public static RoomType fromRoomData(String type) {
        if (type == null || type.isBlank()) {
            return RoomType.NORMAL;
        }
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "puzzle" -> RoomType.PUZZLE;
            case "trap" -> RoomType.TRAP;
            case "fairy" -> RoomType.FAIRY;
            case "blood" -> RoomType.BLOOD;
            case "entrance" -> RoomType.ENTRANCE;
            case "yellow", "miniboss", "champion", "rare", "mini" -> RoomType.MINIBOSS;
            default -> RoomType.NORMAL;
        };
    }

    public static boolean hiddenOnMap(UniqueRoom room) {
        if (room == null) {
            return false;
        }
        return room.type() == RoomType.UNDISCOVERED
                || room.type() == RoomType.UNKNOWN
                || room.checkmark() == Checkmark.QUESTION;
    }

    public static boolean hiddenOnMap(RoomTile room) {
        if (room == null) {
            return false;
        }
        return room.type() == RoomType.UNDISCOVERED
                || room.type() == RoomType.UNKNOWN
                || room.checkmark() == Checkmark.QUESTION;
    }

    public static int[] parseTileKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        int comma = key.indexOf(',');
        if (comma <= 0 || comma >= key.length() - 1) {
            return null;
        }
        try {
            return new int[]{
                    Integer.parseInt(key.substring(0, comma).trim()),
                    Integer.parseInt(key.substring(comma + 1).trim())
            };
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Magical Map paper plus world-hashed rooms. Reveal Hidden paints
     * identified rooms that the item still leaves blank (wither/blood doors).
     */
    public static Board applyHashedRooms(
            Board board,
            Map<String, RoomIdentity> hashed,
            boolean revealHidden) {
        if (board == null || !board.calibration().ok() || hashed == null || hashed.isEmpty()) {
            return board;
        }
        if (!revealHidden) {
            return board;
        }
        Calibration cal = board.calibration();
        Map<String, RoomTile> byTile = new LinkedHashMap<>();
        for (RoomTile room : board.rooms()) {
            byTile.put(tileKey(room.tileX(), room.tileZ()), room);
        }
        int added = 0;
        int upgraded = 0;
        for (Map.Entry<String, RoomIdentity> entry : hashed.entrySet()) {
            RoomIdentity identity = entry.getValue();
            if (identity == null || (!identity.typed() && identity.name().isBlank())) {
                continue;
            }
            int[] tile = parseTileKey(entry.getKey());
            if (tile == null) {
                continue;
            }
            int tileX = tile[0];
            int tileZ = tile[1];
            if (tileX < 0 || tileZ < 0 || tileX >= GRID || tileZ >= GRID) {
                continue;
            }
            RoomType hashedType = identity.typed() ? identity.type() : RoomType.UNDISCOVERED;
            RoomTile existing = byTile.get(entry.getKey());
            int originX = cal.startX() + tileX * cal.roomGap();
            int originZ = cal.startZ() + tileZ * cal.roomGap();
            if (existing == null) {
                byTile.put(entry.getKey(), new RoomTile(
                        tileX, tileZ, hashedType, Checkmark.QUESTION, originX, originZ));
                added++;
                continue;
            }
            if (!hiddenOnMap(existing) || !identity.typed() || existing.type() == hashedType) {
                continue;
            }
            Checkmark mark = existing.checkmark() == Checkmark.NONE
                    ? Checkmark.QUESTION
                    : existing.checkmark();
            byTile.put(entry.getKey(), new RoomTile(
                    existing.tileX(),
                    existing.tileZ(),
                    hashedType,
                    mark,
                    existing.pixelX(),
                    existing.pixelZ()));
            upgraded++;
        }
        if (added == 0 && upgraded == 0) {
            return board;
        }
        String summary = board.summary();
        if (added > 0) {
            summary += " · hashed " + added;
        }
        return new Board(
                board.calibration(),
                List.copyOf(byTile.values()),
                board.doors(),
                board.players(),
                summary);
    }

    public static List<DungeonRoomDataPolicy.MapTile> tilesNamed(
            Map<String, RoomIdentity> identities,
            String name) {
        if (identities == null || identities.isEmpty() || name == null || name.isBlank()) {
            return List.of();
        }
        List<DungeonRoomDataPolicy.MapTile> out = new ArrayList<>();
        for (Map.Entry<String, RoomIdentity> entry : identities.entrySet()) {
            RoomIdentity identity = entry.getValue();
            if (identity == null || !name.equalsIgnoreCase(identity.name())) {
                continue;
            }
            int[] tile = parseTileKey(entry.getKey());
            if (tile == null) {
                continue;
            }
            out.add(new DungeonRoomDataPolicy.MapTile(tile[0], tile[1]));
        }
        return List.copyOf(out);
    }

    public static List<DungeonRoomDataPolicy.MapTile> currentRoomTiles(
            int worldX,
            int worldZ,
            Map<String, RoomIdentity> identities) {
        int tileX = tileFromWorld(worldX);
        int tileZ = tileFromWorld(worldZ);
        RoomIdentity identity = identities == null
                ? null
                : identities.get(tileKey(tileX, tileZ));
        if (identity != null && !identity.name().isBlank()) {
            List<DungeonRoomDataPolicy.MapTile> named = tilesNamed(identities, identity.name());
            if (!named.isEmpty()) {
                return named;
            }
        }
        return List.of(new DungeonRoomDataPolicy.MapTile(tileX, tileZ));
    }

    public static boolean inRoomTiles(
            int worldX,
            int worldZ,
            List<DungeonRoomDataPolicy.MapTile> tiles) {
        if (tiles == null || tiles.isEmpty()) {
            return false;
        }
        int tileX = tileFromWorld(worldX);
        int tileZ = tileFromWorld(worldZ);
        for (DungeonRoomDataPolicy.MapTile tile : tiles) {
            if (tile != null && tile.tileX() == tileX && tile.tileZ() == tileZ) {
                return true;
            }
        }
        return false;
    }

    public static String fallbackName(RoomType type) {
        return switch (type) {
            case PUZZLE -> "Puzzle";
            case TRAP -> "Trap";
            case FAIRY -> "Fairy";
            case BLOOD -> "Blood";
            case MINIBOSS -> "Mini";
            case ENTRANCE -> "Entrance";
            case UNDISCOVERED, UNKNOWN -> "?";
            case NORMAL, EMPTY -> "";
        };
    }

    public static List<UniqueRoom> uniqueRooms(Board board) {
        return uniqueRooms(board, Map.of());
    }

    public static List<UniqueRoom> uniqueRooms(Board board, Map<String, RoomIdentity> identities) {
        if (board == null || board.rooms().isEmpty()) {
            return List.of();
        }
        Map<String, RoomIdentity> names = identities == null ? Map.of() : identities;
        RoomTile[][] grid = new RoomTile[GRID][GRID];
        for (RoomTile room : board.rooms()) {
            if (room.tileX() < 0 || room.tileZ() < 0 || room.tileX() >= GRID || room.tileZ() >= GRID) {
                continue;
            }
            grid[room.tileZ()][room.tileX()] = room;
        }
        Set<String> doorsH = new HashSet<>();
        Set<String> doorsV = new HashSet<>();
        for (DoorTile door : board.doors()) {
            String key = tileKey(door.fromX(), door.fromZ());
            if (door.horizontal()) {
                doorsH.add(key);
            } else {
                doorsV.add(key);
            }
        }
        boolean[][] seen = new boolean[GRID][GRID];
        List<UniqueRoom> out = new ArrayList<>();
        int[] dx = {1, -1, 0, 0};
        int[] dz = {0, 0, 1, -1};
        for (int z = 0; z < GRID; z++) {
            for (int x = 0; x < GRID; x++) {
                if (seen[z][x] || grid[z][x] == null) {
                    continue;
                }
                RoomType type = grid[z][x].type();
                List<RoomTile> tiles = new ArrayList<>();
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, z});
                seen[z][x] = true;
                while (!queue.isEmpty()) {
                    int[] cur = queue.removeFirst();
                    tiles.add(grid[cur[1]][cur[0]]);
                    for (int i = 0; i < 4; i++) {
                        int nx = cur[0] + dx[i];
                        int nz = cur[1] + dz[i];
                        if (nx < 0 || nz < 0 || nx >= GRID || nz >= GRID || seen[nz][nx]) {
                            continue;
                        }
                        if (grid[nz][nx] == null || grid[nz][nx].type() != type) {
                            continue;
                        }
                        if (doorBetween(cur[0], cur[1], nx, nz, doorsH, doorsV)) {
                            continue;
                        }
                        if (!sameHashedRoom(grid[cur[1]][cur[0]], grid[nz][nx], names)) {
                            continue;
                        }
                        seen[nz][nx] = true;
                        queue.add(new int[]{nx, nz});
                    }
                }
                out.add(fromTiles(tiles));
            }
        }
        return List.copyOf(out);
    }

    public static Schematic schematic(
            Board board,
            Map<String, RoomIdentity> identities,
            Map<String, Integer> secretsFoundByName,
            boolean showDoors,
            boolean showPlayers,
            boolean darkenHidden,
            double darkenFactor) {
        return schematic(
                board, identities, secretsFoundByName, showDoors, showPlayers,
                darkenHidden, darkenFactor, HUD_CELL, HUD_GAP, HUD_PAD);
    }

    public static Schematic schematic(
            Board board,
            Map<String, RoomIdentity> identities,
            Map<String, Integer> secretsFoundByName,
            boolean showDoors,
            boolean showPlayers,
            boolean darkenHidden,
            double darkenFactor,
            int cell,
            int gap,
            int pad) {
        if (board == null || !board.calibration().ok() || board.rooms().isEmpty()) {
            return Schematic.empty();
        }
        Calibration cal = board.calibration();
        int size = cal.roomSize();
        int connector = Math.max(4, cal.roomGap() - size);
        int doorSpan = 8;
        int doorwayOffset = Math.max(0, (size - doorSpan) / 2);
        Set<String> doorsH = new HashSet<>();
        Set<String> doorsV = new HashSet<>();
        for (DoorTile door : board.doors()) {
            if (door.horizontal()) {
                doorsH.add(tileKey(door.fromX(), door.fromZ()));
            } else {
                doorsV.add(tileKey(door.fromX(), door.fromZ()));
            }
        }
        Map<String, RoomIdentity> names = identities == null ? Map.of() : identities;
        List<UniqueRoom> uniques = uniqueRooms(board, names);
        List<HudRoom> rooms = new ArrayList<>();
        Map<String, Integer> found = secretsFoundByName == null ? Map.of() : secretsFoundByName;
        for (UniqueRoom unique : uniques) {
            Set<String> tileSet = new HashSet<>();
            for (RoomTile tile : unique.tiles()) {
                tileSet.add(tileKey(tile.tileX(), tile.tileZ()));
            }
            List<HudRect> rects = new ArrayList<>();
            int minPx = Integer.MAX_VALUE;
            int minPz = Integer.MAX_VALUE;
            int maxPx = 0;
            int maxPz = 0;
            for (RoomTile tile : unique.tiles()) {
                int rx = tile.pixelX();
                int rz = tile.pixelZ();
                int rw = size;
                int rh = size;
                boolean east = tileSet.contains(tileKey(tile.tileX() + 1, tile.tileZ()))
                        && !doorsH.contains(tileKey(tile.tileX(), tile.tileZ()));
                boolean south = tileSet.contains(tileKey(tile.tileX(), tile.tileZ() + 1))
                        && !doorsV.contains(tileKey(tile.tileX(), tile.tileZ()));
                if (east) {
                    rw += connector;
                }
                if (south) {
                    rh += connector;
                }
                rects.add(new HudRect(rx, rz, rw, rh));
                minPx = Math.min(minPx, rx);
                minPz = Math.min(minPz, rz);
                maxPx = Math.max(maxPx, rx + rw);
                maxPz = Math.max(maxPz, rz + rh);
            }
            RoomIdentity identity = identityFor(unique, names);
            RoomType paint = unique.type();
            if (!isTyped(paint) && identity.typed()) {
                paint = identity.type();
            }
            String name = identity.name().isBlank() ? fallbackName(paint) : identity.name();
            int total = identity.secrets();
            int got = Math.max(0, found.getOrDefault(name, 0));
            if (unique.checkmark() == Checkmark.GREEN && total > 0) {
                got = total;
            }
            if (total > 0) {
                got = Math.min(got, total);
            }
            int color = argb(paint, unique.checkmark());
            if (darkenHidden && hiddenOnMap(unique)) {
                color = DungeonLeftoverPolicy.darkenArgb(color, darkenFactor);
            }
            rooms.add(new HudRoom(
                    unique,
                    rects,
                    (minPx + maxPx) / 2,
                    (minPz + maxPz) / 2,
                    name,
                    got,
                    total,
                    color));
        }
        List<HudDoor> doors = new ArrayList<>();
        if (showDoors) {
            for (DoorTile door : board.doors()) {
                int originX = cal.startX() + door.fromX() * cal.roomGap();
                int originZ = cal.startZ() + door.fromZ() * cal.roomGap();
                int color = doorArgb(door.type());
                if (door.horizontal()) {
                    doors.add(new HudDoor(
                            originX + size,
                            originZ + doorwayOffset,
                            connector,
                            doorSpan,
                            color));
                } else {
                    doors.add(new HudDoor(
                            originX + doorwayOffset,
                            originZ + size,
                            doorSpan,
                            connector,
                            color));
                }
            }
        }
        List<HudMarker> players = new ArrayList<>();
        if (showPlayers) {
            for (PlayerIcon icon : board.players()) {
                players.add(new HudMarker(
                        icon.mapX(),
                        icon.mapZ(),
                        icon.yaw(),
                        icon.self(),
                        icon.classKey(),
                        icon.name()));
            }
        }
        return new Schematic(HUD_MAP, HUD_MAP, rooms, doors, players);
    }

    public static boolean puzzlesComplete(List<UniqueRoom> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return false;
        }
        boolean any = false;
        for (UniqueRoom room : rooms) {
            if (room.type() != RoomType.PUZZLE) {
                continue;
            }
            any = true;
            if (room.checkmark() != Checkmark.GREEN) {
                return false;
            }
        }
        return any;
    }

    public static UniqueRoom roomAt(List<UniqueRoom> rooms, int tileX, int tileZ) {
        if (rooms == null) {
            return null;
        }
        for (UniqueRoom room : rooms) {
            if (room.contains(tileX, tileZ)) {
                return room;
            }
        }
        return null;
    }

    public static boolean isWorldEspDoor(DoorType type) {
        return type == DoorType.WITHER
                || type == DoorType.OPENED_WITHER
                || type == DoorType.BLOOD
                || type == DoorType.ENTRANCE;
    }

    public static Calibration calibrate(byte[] colors, int stride) {
        return calibrate(colors, stride, -1);
    }

    public static Calibration calibrate(byte[] colors, int stride, int floor) {
        int width = stride <= 0 ? MAP : stride;
        if (colors == null || colors.length < width) {
            return Calibration.none();
        }
        int run = 0;
        int start = 0;
        for (int i = 0; i < colors.length; i++) {
            if ((colors[i] & 0xFF) == (COLOR_ENTRANCE & 0xFF)) {
                if (run == 0) {
                    start = i;
                }
                run++;
            } else {
                if (run == 16 || run == 18) {
                    return calibrationFromEntrance(start, run, width, floor);
                }
                run = 0;
            }
        }
        if (run == 16 || run == 18) {
            return calibrationFromEntrance(start, run, width, floor);
        }
        return Calibration.none();
    }

    public static Board scan(
            byte[] colors,
            int stride,
            List<PlayerIcon> decorations,
            Double playerX,
            Double playerZ) {
        return scan(colors, stride, decorations, playerX, playerZ, 0.0F, "", "");
    }

    public static Board scan(
            byte[] colors,
            int stride,
            List<PlayerIcon> decorations,
            Double playerX,
            Double playerZ,
            float yaw) {
        return scan(colors, stride, decorations, playerX, playerZ, yaw, "", "");
    }

    public static Board scan(
            byte[] colors,
            int stride,
            List<PlayerIcon> decorations,
            Double playerX,
            Double playerZ,
            float yaw,
            String classKey,
            String name) {
        return scan(colors, stride, decorations, playerX, playerZ, yaw, classKey, name, -1);
    }

    public static Board scan(
            byte[] colors,
            int stride,
            List<PlayerIcon> decorations,
            Double playerX,
            Double playerZ,
            float yaw,
            String classKey,
            String name,
            int floor) {
        Calibration cal = calibrate(colors, stride, floor);
        if (!cal.ok()) {
            return new Board(cal, List.of(), List.of(), List.of(), "");
        }
        int width = stride <= 0 ? MAP : stride;
        List<RoomTile> rooms = new ArrayList<>();
        List<DoorTile> doors = new ArrayList<>();
        int puzzles = 0;
        int blood = 0;
        int fairy = 0;
        int half = cal.roomSize() / 2;
        int connection = cal.roomSize() + 2;
        for (int tileZ = 0; tileZ < GRID; tileZ++) {
            for (int tileX = 0; tileX < GRID; tileX++) {
                int originX = cal.startX() + tileX * cal.roomGap();
                int originZ = cal.startZ() + tileZ * cal.roomGap();
                byte corner = px(colors, width, originX, originZ);
                RoomType type = roomType(corner);
                if (type != RoomType.EMPTY) {
                    byte center = px(colors, width, originX + half, originZ + half);
                    Checkmark mark = checkmark(corner, center);
                    rooms.add(new RoomTile(tileX, tileZ, type, mark, originX, originZ));
                    if (type == RoomType.PUZZLE) {
                        puzzles++;
                    } else if (type == RoomType.BLOOD) {
                        blood++;
                    } else if (type == RoomType.FAIRY) {
                        fairy++;
                    }
                }
                if (tileX < GRID - 1) {
                    byte doorColor = px(colors, width, originX + connection, originZ + half);
                    byte side = px(colors, width, originX + connection, originZ + half - 4);
                    if (side == 0 && doorColor != 0) {
                        doors.add(new DoorTile(tileX, tileZ, true, doorType(doorColor)));
                    }
                }
                if (tileZ < GRID - 1) {
                    byte doorColor = px(colors, width, originX + half, originZ + connection);
                    byte side = px(colors, width, originX + half - 4, originZ + connection);
                    if (side == 0 && doorColor != 0) {
                        doors.add(new DoorTile(tileX, tileZ, false, doorType(doorColor)));
                    }
                }
            }
        }
        List<PlayerIcon> players = new ArrayList<>();
        if (decorations != null) {
            players.addAll(decorations);
        }
        if (playerX != null && playerZ != null) {
            float mapX = (float) ((playerX + 200.0D) * cal.coordMul() + cal.startX());
            float mapZ = (float) ((playerZ + 200.0D) * cal.coordMul() + cal.startZ());
            players.add(new PlayerIcon(
                    Math.round(mapX),
                    Math.round(mapZ),
                    yaw,
                    true,
                    classKey == null ? "" : classKey,
                    name == null ? "" : name));
        }
        String summary = rooms.size() + " rooms";
        if (puzzles > 0) {
            summary += " · " + puzzles + " puzzle";
        }
        if (blood > 0) {
            summary += " · blood";
        }
        if (fairy > 0) {
            summary += " · fairy";
        }
        if (!doors.isEmpty()) {
            summary += " · " + doors.size() + " doors";
        }
        return new Board(cal, rooms, doors, players, summary);
    }

    public static DungeonPuzzlePolicy.MapPreview render(
            byte[] colors,
            int stride,
            Board board,
            boolean showDoors,
            boolean showPlayers) {
        return render(colors, stride, board, showDoors, showPlayers, false, 0.6D);
    }

    public static DungeonPuzzlePolicy.MapPreview render(
            byte[] colors,
            int stride,
            Board board,
            boolean showDoors,
            boolean showPlayers,
            boolean darkenHidden,
            double darkenFactor) {
        int width = stride <= 0 ? MAP : stride;
        if (board == null || !board.calibration().ok() || colors == null) {
            return DungeonPuzzlePolicy.previewDungeonMap(colors, stride, 21);
        }
        Calibration cal = board.calibration();
        int used = cal.startX() + GRID * cal.roomGap();
        int size = Math.min(width, Math.max(48, used + 4));
        int[] argb = new int[size * size];
        for (int i = 0; i < argb.length; i++) {
            argb[i] = 0xFF0B0B0B;
        }
        for (RoomTile room : board.rooms()) {
            int color = argb(room.type());
            if (darkenHidden && (room.type() == RoomType.UNDISCOVERED
                    || room.checkmark() == Checkmark.NONE || room.checkmark() == Checkmark.QUESTION)) {
                color = DungeonLeftoverPolicy.darkenArgb(color, darkenFactor);
            }
            for (int dz = 0; dz < cal.roomSize(); dz++) {
                for (int dx = 0; dx < cal.roomSize(); dx++) {
                    plot(argb, size, room.pixelX() + dx, room.pixelZ() + dz, color);
                }
            }
            plotCheckmark(argb, size, room, cal);
        }
        if (showDoors) {
            int half = cal.roomSize() / 2;
            int connection = cal.roomSize() + 2;
            for (DoorTile door : board.doors()) {
                int color = doorArgb(door.type());
                int originX = cal.startX() + door.fromX() * cal.roomGap();
                int originZ = cal.startZ() + door.fromZ() * cal.roomGap();
                if (door.horizontal()) {
                    for (int i = 0; i < 3; i++) {
                        plot(argb, size, originX + connection, originZ + half - 1 + i, color);
                    }
                } else {
                    for (int i = 0; i < 3; i++) {
                        plot(argb, size, originX + half - 1 + i, originZ + connection, color);
                    }
                }
            }
        }
        int playerX = size / 2;
        int playerZ = size / 2;
        if (showPlayers) {
            for (PlayerIcon icon : board.players()) {
                plotPlayer(argb, size, icon);
                if (icon.self()) {
                    playerX = Math.max(0, Math.min(size - 1, icon.mapX()));
                    playerZ = Math.max(0, Math.min(size - 1, icon.mapZ()));
                }
            }
        }
        return new DungeonPuzzlePolicy.MapPreview(size, size, argb, playerX, playerZ, board.summary());
    }

    private static boolean isTyped(RoomType type) {
        return type != null
                && type != RoomType.EMPTY
                && type != RoomType.UNKNOWN
                && type != RoomType.UNDISCOVERED;
    }

    private static boolean sameHashedRoom(
            RoomTile left,
            RoomTile right,
            Map<String, RoomIdentity> identities) {
        if (identities == null || identities.isEmpty() || left == null || right == null) {
            return true;
        }
        RoomIdentity a = identities.get(tileKey(left.tileX(), left.tileZ()));
        RoomIdentity b = identities.get(tileKey(right.tileX(), right.tileZ()));
        String nameA = a == null ? "" : a.name();
        String nameB = b == null ? "" : b.name();
        if (nameA.isBlank() || nameB.isBlank()) {
            return true;
        }
        return nameA.equalsIgnoreCase(nameB);
    }

    private static UniqueRoom fromTiles(List<RoomTile> tiles) {
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = 0;
        int maxZ = 0;
        Checkmark mark = Checkmark.NONE;
        RoomType type = RoomType.EMPTY;
        for (RoomTile tile : tiles) {
            minX = Math.min(minX, tile.tileX());
            minZ = Math.min(minZ, tile.tileZ());
            maxX = Math.max(maxX, tile.tileX());
            maxZ = Math.max(maxZ, tile.tileZ());
            type = tile.type();
            if (checkmarkRank(tile.checkmark()) > checkmarkRank(mark)) {
                mark = tile.checkmark();
            }
        }
        return new UniqueRoom(minX, minZ, maxX, maxZ, tiles, type, mark);
    }

    private static int checkmarkRank(Checkmark mark) {
        return switch (mark) {
            case GREEN -> 4;
            case WHITE -> 3;
            case RED -> 2;
            case QUESTION -> 1;
            case NONE -> 0;
        };
    }

    private static boolean doorBetween(
            int x1, int z1, int x2, int z2, Set<String> doorsH, Set<String> doorsV) {
        if (z1 == z2 && Math.abs(x1 - x2) == 1) {
            int fromX = Math.min(x1, x2);
            return doorsH.contains(tileKey(fromX, z1));
        }
        if (x1 == x2 && Math.abs(z1 - z2) == 1) {
            int fromZ = Math.min(z1, z2);
            return doorsV.contains(tileKey(x1, fromZ));
        }
        return false;
    }

    private static RoomIdentity identityFor(UniqueRoom room, Map<String, RoomIdentity> identities) {
        for (RoomTile tile : room.tiles()) {
            RoomIdentity hit = identities.get(tileKey(tile.tileX(), tile.tileZ()));
            if (hit != null && !hit.name().isBlank()) {
                return hit;
            }
        }
        return new RoomIdentity("", 0);
    }

    private static int hudPixel(
            int mapPixel,
            int origin,
            int roomGap,
            int cell,
            int gap,
            int pad,
            int tileShift) {
        if (roomGap <= 0) {
            return pad;
        }
        double tile = (mapPixel - origin) / (double) roomGap;
        return pad + (int) Math.round(tile * (cell + gap)) - tileShift * (cell + gap);
    }

    private static Calibration calibrationFromEntrance(int start, int size, int width, int floor) {
        int startX;
        int startZ;
        if (floor == 0) {
            startX = 22;
            startZ = 22;
        } else if (floor == 1) {
            startX = 22;
            startZ = 11;
        } else if (floor == 2 || floor == 3) {
            startX = 11;
            startZ = 11;
        } else {
            startX = (start % width) % (size + 4);
            startZ = ((start / width) % (size + 4));
            if (floor < 0) {
                if (startX == 0) {
                    startX = 22;
                }
                if (startZ == 0) {
                    startZ = 22;
                }
            }
        }
        double mul = (size + 4.0D) / 32.0D;
        return new Calibration(true, startX, startZ, size, size + 4, mul);
    }

    private static byte px(byte[] colors, int width, int x, int z) {
        if (x < 0 || z < 0 || x >= width) {
            return 0;
        }
        int index = z * width + x;
        if (index < 0 || index >= colors.length) {
            return 0;
        }
        return colors[index];
    }

    private static void plotCheckmark(int[] argb, int size, RoomTile room, Calibration cal) {
        int cx = room.pixelX() + cal.roomSize() / 2;
        int cz = room.pixelZ() + cal.roomSize() / 2;
        int mark = switch (room.checkmark()) {
            case GREEN -> 0xFF22C55E;
            case WHITE -> 0xFFF8FAFC;
            case RED -> 0xFFEF4444;
            case QUESTION -> 0xFFFACC15;
            case NONE -> 0;
        };
        if (mark == 0) {
            return;
        }
        if (room.checkmark() == Checkmark.QUESTION) {
            plot(argb, size, cx, cz, mark);
            plot(argb, size, cx, cz - 1, mark);
            plot(argb, size, cx + 1, cz - 1, mark);
            plot(argb, size, cx + 1, cz, mark);
            plot(argb, size, cx, cz + 2, mark);
            return;
        }
        if (room.checkmark() == Checkmark.RED) {
            plot(argb, size, cx, cz, mark);
            plot(argb, size, cx - 1, cz - 1, mark);
            plot(argb, size, cx + 1, cz + 1, mark);
            plot(argb, size, cx + 1, cz - 1, mark);
            plot(argb, size, cx - 1, cz + 1, mark);
            return;
        }
        plot(argb, size, cx - 1, cz, mark);
        plot(argb, size, cx, cz, mark);
        plot(argb, size, cx + 1, cz, mark);
        plot(argb, size, cx, cz - 1, mark);
        plot(argb, size, cx, cz + 1, mark);
        if (room.checkmark() == Checkmark.GREEN) {
            plot(argb, size, cx - 1, cz + 1, mark);
            plot(argb, size, cx + 1, cz - 1, mark);
        }
    }

    private static void plotPlayer(int[] argb, int size, PlayerIcon icon) {
        DungeonPolicy.DungeonClass dungeonClass = DungeonPolicy.dungeonClass(icon.classKey());
        int color = icon.self()
                ? 0xFFFFFFFF
                : (icon.classKey() == null || icon.classKey().isBlank()
                        ? 0xFF60A5FA
                        : EmberDungeonPolicy.classColor(dungeonClass));
        int x = icon.mapX();
        int z = icon.mapZ();
        boolean head = icon.classKey() != null && !icon.classKey().isBlank();
        if (head) {
            plot(argb, size, x, z, color);
            plot(argb, size, x + 1, z, color);
            plot(argb, size, x, z + 1, color);
            plot(argb, size, x + 1, z + 1, color);
            plot(argb, size, x - 1, z, color);
            plot(argb, size, x, z - 1, color);
            plot(argb, size, x - 1, z - 1, 0xFF111111);
            return;
        }
        plot(argb, size, x, z, color);
        plot(argb, size, x + 1, z, color);
        plot(argb, size, x, z + 1, color);
        plot(argb, size, x - 1, z, color);
        plot(argb, size, x, z - 1, color);
        double rad = Math.toRadians(icon.yaw());
        int fx = (int) Math.round(Math.sin(rad));
        int fz = (int) Math.round(-Math.cos(rad));
        if (fx != 0 || fz != 0) {
            int tip = icon.self() ? 0xFF22C55E : color;
            plot(argb, size, x + fx, z + fz, tip);
            plot(argb, size, x + fx * 2, z + fz * 2, tip);
        }
    }

    private static void plot(int[] argb, int size, int x, int z, int color) {
        if (x < 0 || z < 0 || x >= size || z >= size) {
            return;
        }
        argb[z * size + x] = color;
    }
}
