package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Shipped catacombs room cores and secret waypoints, loaded from
 * {@code /rotclient/dungeons/}. Core hashing reads legacy numeric ids from
 * Y=140 down to Y=12; iron bars and chests become {@code 0}, and unknown
 * blocks are omitted.
 */
public final class DungeonRoomDataPolicy {
    public static final int HASH_Y_TOP = 140;
    public static final int HASH_Y_BOTTOM = 12;
    public static final int ROOM_SPAN = 32;
    public static final int ROOM_HALF = 15;

    public enum SecretKind {
        CHEST,
        ITEM,
        ESSENCE,
        BAT,
        REDSTONE,
        LEVER
    }

    public record IntVec(int x, int y, int z) {
    }

    public record RoomMeta(
            String name,
            String type,
            int secrets,
            List<Integer> cores,
            int roomId,
            String shape) {
        public RoomMeta {
            name = name == null ? "" : name;
            type = type == null ? "" : type;
            cores = cores == null ? List.of() : List.copyOf(cores);
            shape = shape == null ? "" : shape;
        }
    }

    public record SecretWaypoint(SecretKind kind, int x, int y, int z) {
    }

    public record PlacedWaypoint(SecretKind kind, int x, int y, int z, String roomName) {
    }

    public record Rotation(int degrees, int cornerX, int cornerZ) {
    }

    private static final Map<String, Integer> LEGACY_IDS = legacyIds();
    private static volatile Catalog catalog;

    private DungeonRoomDataPolicy() {
    }

    public static Catalog catalog() {
        Catalog loaded = catalog;
        if (loaded == null) {
            synchronized (DungeonRoomDataPolicy.class) {
                loaded = catalog;
                if (loaded == null) {
                    loaded = loadClasspath();
                    catalog = loaded;
                }
            }
        }
        return loaded;
    }

    static Catalog loadFromJson(String roomsJson, String waypointsJson) {
        List<RoomMeta> rooms = parseRooms(roomsJson);
        Map<Integer, List<SecretWaypoint>> byId = parseWaypoints(waypointsJson);
        Map<Integer, RoomMeta> byCore = new HashMap<>();
        for (RoomMeta room : rooms) {
            for (int core : room.cores()) {
                byCore.put(core, room);
            }
        }
        return new Catalog(List.copyOf(rooms), Map.copyOf(byCore), Map.copyOf(byId));
    }

    public static int hashColumn(List<String> blockIdsY140toY12) {
        StringBuilder str = new StringBuilder();
        int expected = HASH_Y_TOP - HASH_Y_BOTTOM + 1;
        for (int i = 0; i < expected; i++) {
            String id = i < blockIdsY140toY12.size() ? blockIdsY140toY12.get(i) : "";
            Integer legacy = legacyId(id);
            if (legacy == null) {
                continue;
            }
            String lower = id == null ? "" : id.toLowerCase(Locale.ROOT);
            if (lower.contains("iron_bars") || lower.contains("chest")) {
                str.append('0');
            } else {
                str.append(legacy.intValue());
            }
        }
        return str.toString().hashCode();
    }

    public static Integer legacyId(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return null;
        }
        String key = blockId.trim().toLowerCase(Locale.ROOT);
        if (!key.contains(":")) {
            key = "minecraft:" + key;
        }
        Integer direct = LEGACY_IDS.get(key);
        if (direct != null) {
            return direct;
        }
        int bracket = key.indexOf('[');
        if (bracket > 0) {
            return LEGACY_IDS.get(key.substring(0, bracket));
        }
        return null;
    }

    public static Optional<RoomMeta> roomForCore(int core) {
        return Optional.ofNullable(catalog().byCore().get(core));
    }

    public static int roomOrigin(int world) {
        return Math.floorDiv(world, ROOM_SPAN) * ROOM_SPAN;
    }

    public static int roomCenter(int world) {
        return roomOrigin(world) + ROOM_HALF;
    }

    /**
     * Room-core geometry: rotate relative XZ then add the
     * blue-terracotta corner.
     */
    public static IntVec fromComp(int relX, int relY, int relZ, Rotation rotation) {
        if (rotation == null) {
            return new IntVec(relX, relY, relZ);
        }
        int[] xz = rotate(relX, relZ, 360 - rotation.degrees());
        return new IntVec(xz[0] + rotation.cornerX(), relY, xz[1] + rotation.cornerZ());
    }

    /**
     * Corner check for blue terracotta: NW/NE/SE/SW of a 1x1 room,
     * rotations 0/90/180/270.
     */
    public static List<Rotation> candidateRotations(int worldX, int worldZ) {
        int originX = roomOrigin(worldX);
        int originZ = roomOrigin(worldZ);
        int cx = originX + ROOM_HALF;
        int cz = originZ + ROOM_HALF;
        return List.of(
                new Rotation(0, cx - ROOM_HALF, cz - ROOM_HALF),
                new Rotation(90, cx + ROOM_HALF, cz - ROOM_HALF),
                new Rotation(180, cx + ROOM_HALF, cz + ROOM_HALF),
                new Rotation(270, cx - ROOM_HALF, cz + ROOM_HALF));
    }

    public static boolean isBlueTerracotta(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("blue_terracotta");
    }

    public static List<PlacedWaypoint> placeSecrets(RoomMeta room, Rotation rotation) {
        if (room == null || rotation == null) {
            return List.of();
        }
        List<SecretWaypoint> rel = catalog().waypointsByRoomId().getOrDefault(room.roomId(), List.of());
        List<PlacedWaypoint> out = new ArrayList<>();
        for (SecretWaypoint waypoint : rel) {
            IntVec world = fromComp(waypoint.x(), waypoint.y(), waypoint.z(), rotation);
            out.add(new PlacedWaypoint(waypoint.kind(), world.x(), world.y(), world.z(), room.name()));
        }
        return List.copyOf(out);
    }

    public static int secretColor(SecretKind kind) {
        return switch (kind) {
            case CHEST -> 0xFF22C55E;
            case ITEM -> 0xFF38BDF8;
            case ESSENCE -> 0xFFA855F7;
            case BAT -> 0xFFFACC15;
            case REDSTONE -> 0xFFEF4444;
            case LEVER -> 0xFFFB923C;
        };
    }

    public record Catalog(
            List<RoomMeta> rooms,
            Map<Integer, RoomMeta> byCore,
            Map<Integer, List<SecretWaypoint>> waypointsByRoomId) {
        public Catalog {
            rooms = rooms == null ? List.of() : List.copyOf(rooms);
            byCore = byCore == null ? Map.of() : Map.copyOf(byCore);
            Map<Integer, List<SecretWaypoint>> copy = new LinkedHashMap<>();
            if (waypointsByRoomId != null) {
                waypointsByRoomId.forEach((id, list) -> copy.put(id, List.copyOf(list)));
            }
            waypointsByRoomId = Map.copyOf(copy);
        }
    }

    private static Catalog loadClasspath() {
        return loadFromJson(
                readResource("/rotclient/dungeons/rooms.json"),
                readResource("/rotclient/dungeons/dungeon_waypoints.json"));
    }

    private static String readResource(String path) {
        try (InputStream in = DungeonRoomDataPolicy.class.getResourceAsStream(path)) {
            if (in == null) {
                return "[]";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private static List<RoomMeta> parseRooms(String json) {
        List<RoomMeta> rooms = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return rooms;
        }
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            List<Integer> cores = new ArrayList<>();
            if (obj.has("cores") && obj.get("cores").isJsonArray()) {
                for (JsonElement core : obj.getAsJsonArray("cores")) {
                    cores.add(core.getAsInt());
                }
            }
            rooms.add(new RoomMeta(
                    text(obj, "name"),
                    text(obj, "type"),
                    obj.has("secrets") ? obj.get("secrets").getAsInt() : 0,
                    cores,
                    obj.has("roomID") ? obj.get("roomID").getAsInt() : -1,
                    text(obj, "shape")));
        }
        return rooms;
    }

    private static Map<Integer, List<SecretWaypoint>> parseWaypoints(String json) {
        Map<Integer, List<SecretWaypoint>> byId = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return byId;
        }
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject obj = element.getAsJsonObject();
            int roomId = obj.has("roomID") ? obj.get("roomID").getAsInt() : -1;
            JsonObject waypoints = obj.has("waypoints") && obj.get("waypoints").isJsonObject()
                    ? obj.getAsJsonObject("waypoints")
                    : new JsonObject();
            List<SecretWaypoint> list = new ArrayList<>();
            for (SecretKind kind : SecretKind.values()) {
                String key = kind.name().toLowerCase(Locale.ROOT);
                if (!waypoints.has(key) || !waypoints.get(key).isJsonArray()) {
                    continue;
                }
                for (JsonElement posEl : waypoints.getAsJsonArray(key)) {
                    if (!posEl.isJsonArray()) {
                        continue;
                    }
                    JsonArray pos = posEl.getAsJsonArray();
                    if (pos.size() < 3) {
                        continue;
                    }
                    list.add(new SecretWaypoint(kind, pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt()));
                }
            }
            byId.put(roomId, list);
        }
        return byId;
    }

    private static String text(JsonObject obj, String key) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : "";
    }

    private static int[] rotate(int x, int z, int degree) {
        int normalized = Math.floorMod(degree, 360);
        return switch (normalized) {
            case 90 -> new int[]{z, -x};
            case 180 -> new int[]{-x, -z};
            case 270 -> new int[]{-z, x};
            default -> new int[]{x, z};
        };
    }

    private static Map<String, Integer> legacyIds() {
        Map<String, Integer> map = new HashMap<>();
        put(map, 0, "air");
        put(map, 1, "stone", "polished_andesite", "polished_diorite", "polished_granite",
                "andesite", "diorite", "granite");
        put(map, 2, "grass_block");
        put(map, 3, "coarse_dirt", "dirt");
        put(map, 4, "cobblestone");
        put(map, 5, "dark_oak_planks", "spruce_planks", "jungle_planks", "birch_planks");
        put(map, 7, "bedrock");
        put(map, 11, "lava");
        put(map, 13, "gravel");
        put(map, 14, "gold_ore");
        put(map, 17, "oak_wood", "oak_log");
        put(map, 18, "oak_leaves");
        put(map, 22, "lapis_block");
        put(map, 24, "sandstone");
        put(map, 29, "sticky_piston");
        put(map, 30, "cobweb");
        put(map, 33, "piston");
        put(map, 34, "piston_head");
        put(map, 35, "gray_wool", "red_wool", "black_wool", "light_gray_wool", "green_wool", "orange_wool");
        put(map, 39, "brown_mushroom");
        put(map, 41, "gold_block");
        put(map, 42, "iron_block");
        put(map, 43, "smooth_stone", "smooth_sandstone");
        put(map, 47, "bookshelf");
        put(map, 48, "mossy_cobblestone");
        put(map, 49, "obsidian");
        put(map, 50, "wall_torch", "torch");
        put(map, 51, "fire");
        put(map, 54, "chest");
        put(map, 57, "diamond_block");
        put(map, 65, "ladder");
        put(map, 66, "rail");
        put(map, 67, "cobblestone_stairs");
        put(map, 69, "lever");
        put(map, 77, "stone_button");
        put(map, 79, "ice");
        put(map, 85, "oak_fence");
        put(map, 89, "glowstone");
        put(map, 95, "black_stained_glass", "light_gray_stained_glass");
        put(map, 97, "infested_cobblestone");
        put(map, 98, "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks");
        put(map, 99, "brown_mushroom_block");
        put(map, 101, "iron_bars");
        put(map, 106, "vine");
        put(map, 109, "stone_brick_stairs");
        put(map, 112, "nether_bricks");
        put(map, 118, "cauldron");
        put(map, 120, "end_portal_frame");
        put(map, 121, "end_stone");
        put(map, 133, "emerald_block");
        put(map, 134, "spruce_stairs");
        put(map, 139, "cobblestone_wall");
        put(map, 152, "redstone_block");
        put(map, 154, "hopper");
        put(map, 155, "quartz_block");
        put(map, 156, "quartz_stairs");
        put(map, 159, "cyan_terracotta", "blue_terracotta", "light_blue_terracotta", "gray_terracotta",
                "light_gray_terracotta", "lime_terracotta", "green_terracotta", "black_terracotta",
                "magenta_terracotta", "purple_terracotta", "red_terracotta", "white_terracotta",
                "orange_terracotta", "yellow_terracotta", "pink_terracotta", "brown_terracotta");
        put(map, 160, "magenta_stained_glass_pane");
        put(map, 164, "dark_oak_stairs");
        put(map, 165, "slime_block");
        put(map, 166, "barrier");
        put(map, 168, "dark_prismarine", "prismarine", "prismarine_bricks");
        put(map, 169, "sea_lantern");
        put(map, 171, "green_carpet", "gray_carpet", "light_gray_carpet", "red_carpet", "brown_carpet",
                "magenta_carpet", "blue_carpet", "light_blue_carpet", "white_carpet", "orange_carpet",
                "yellow_carpet", "lime_carpet", "pink_carpet", "cyan_carpet", "purple_carpet", "black_carpet");
        put(map, 173, "coal_block");
        put(map, 174, "packed_ice");
        put(map, 175, "sunflower");
        put(map, 188, "spruce_fence");
        map.put("minecraft:cobblestone_slab[type=double]", 43);
        map.put("minecraft:stone_brick_slab[type=double]", 43);
        map.put("minecraft:smooth_stone_slab[type=double]", 43);
        map.put("minecraft:sandstone_slab[type=double]", 43);
        map.put("minecraft:stone_brick_slab[type=top]", 44);
        map.put("minecraft:stone_brick_slab[type=bottom]", 44);
        map.put("minecraft:cobblestone_slab[type=top]", 44);
        map.put("minecraft:cobblestone_slab[type=bottom]", 44);
        map.put("minecraft:smooth_stone_slab[type=top]", 44);
        map.put("minecraft:smooth_stone_slab[type=bottom]", 44);
        map.put("minecraft:sandstone_slab[type=top]", 44);
        map.put("minecraft:sandstone_slab[type=bottom]", 44);
        map.put("minecraft:spruce_slab[type=double]", 125);
        map.put("minecraft:oak_slab[type=double]", 125);
        map.put("minecraft:dark_oak_slab[type=double]", 125);
        map.put("minecraft:spruce_slab[type=top]", 126);
        map.put("minecraft:spruce_slab[type=bottom]", 126);
        map.put("minecraft:oak_slab[type=top]", 126);
        map.put("minecraft:oak_slab[type=bottom]", 126);
        map.put("minecraft:dark_oak_slab[type=top]", 126);
        map.put("minecraft:dark_oak_slab[type=bottom]", 126);
        map.put("minecraft:red_sandstone_slab[type=double]", 181);
        map.put("minecraft:red_sandstone_slab[type=top]", 182);
        map.put("minecraft:red_sandstone_slab[type=bottom]", 182);
        return Map.copyOf(map);
    }

    private static void put(Map<String, Integer> map, int id, String... names) {
        for (String name : names) {
            map.put("minecraft:" + name, id);
        }
    }
}
