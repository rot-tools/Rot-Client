package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Magical Map scan of room-grid/doors/checkmarks,
 * entrance calibration and world-to-map player placement.
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

    public record PlayerIcon(int mapX, int mapZ, float yaw, boolean self, String classKey, String name) {
        public PlayerIcon(int mapX, int mapZ, float yaw, boolean self) {
            this(mapX, mapZ, yaw, self, "", "");
        }

        public PlayerIcon(int mapX, int mapZ, float yaw, boolean self, String classKey) {
            this(mapX, mapZ, yaw, self, classKey, "");
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
    private static final int GRID = 6;

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
            case COLOR_UNKNOWN -> RoomType.UNKNOWN;
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
        if (centerColor == roomColor) {
            return Checkmark.NONE;
        }
        return switch (centerColor) {
            case COLOR_BLOOD -> Checkmark.RED;
            case COLOR_ENTRANCE -> Checkmark.GREEN;
            case COLOR_WHITE_CHECK -> Checkmark.WHITE;
            case COLOR_WITHER_DOOR -> Checkmark.QUESTION;
            default -> Checkmark.NONE;
        };
    }

    public static int argb(RoomType type) {
        return switch (type) {
            case EMPTY -> 0xFF111111;
            case ENTRANCE -> 0xFF22C55E;
            case NORMAL -> 0xFFC4A574;
            case PUZZLE -> 0xFFA855F7;
            case TRAP -> 0xFFFB923C;
            case FAIRY -> 0xFFF9A8D4;
            case BLOOD -> 0xFFDC2626;
            case MINIBOSS -> 0xFFFACC15;
            case UNKNOWN -> 0xFF9CA3AF;
            case UNDISCOVERED -> 0xFF292524;
        };
    }

    public static int doorArgb(DoorType type) {
        return switch (type) {
            case NONE -> 0x00000000;
            case NORMAL -> 0xFFF8FAFC;
            case WITHER -> 0xFF7C3AED;
            case OPENED_WITHER -> 0xFF9A3412;
            case BLOOD -> 0xFFDC2626;
            case FAIRY -> 0xFFEC4899;
            case ENTRANCE -> 0xFF22C55E;
        };
    }

    public static Calibration calibrate(byte[] colors, int stride) {
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
                    return calibrationFromEntrance(start, run, width);
                }
                run = 0;
            }
        }
        if (run == 16 || run == 18) {
            return calibrationFromEntrance(start, run, width);
        }
        return Calibration.none();
    }

    public static Board scan(
            byte[] colors,
            int stride,
            List<PlayerIcon> decorations,
            Double playerX,
            Double playerZ) {
        Calibration cal = calibrate(colors, stride);
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
            players.add(new PlayerIcon(Math.round(mapX), Math.round(mapZ), 0.0F, true));
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

    private static Calibration calibrationFromEntrance(int start, int size, int width) {
        int startX = start % width % (size + 4);
        int startZ = (start / width) % (size + 4);
        if (startX == 0) {
            startX = 22;
        }
        if (startZ == 0) {
            startZ = 22;
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
