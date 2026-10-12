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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Modern core hash, clay-corner rotation and secret coordinates adapted from
 * Noamm9/NoammAddons ScanUtils.kt and UniqueRoom.kt, CC0-1.0, revision
 * 4164abc38500e041a805e03ca0f01b46e37bd8a5. Pinned room data: data branch
 * 3c7b32c471148899e68022cfd2afdfc843be91a1. Full notice is packaged under
 * META-INF/licenses/NoammAddons-LICENSE.txt. Rot adds complete-column checks,
 * collision rejection and immutable integration with the existing room model.
 * No network download or upstream framework runs in the client.
 */
public final class DungeonModernRoomPolicy {
    private static final int COLUMN_SIZE = DungeonRoomDataPolicy.HASH_Y_TOP - DungeonRoomDataPolicy.HASH_Y_BOTTOM + 1;
    private static final Set<String> IGNORED = Set.of("minecraft:chest", "minecraft:trapped_chest",
            "minecraft:piston_head", "minecraft:moving_piston", "minecraft:water", "minecraft:lava",
            "minecraft:fire", "minecraft:soul_fire");
    public record Room(DungeonRoomDataPolicy.RoomMeta meta, List<DungeonRoomDataPolicy.SecretWaypoint> secrets) {
        public Room { secrets = List.copyOf(secrets); }
    }
    public record Catalog(List<Room> rooms, Map<Integer, Room> byCore, Map<Integer, Room> byId) {
        public Catalog { rooms = List.copyOf(rooms); byCore = Map.copyOf(byCore); byId = Map.copyOf(byId); }
    }
    private static final class Holder {
        private static final Catalog CATALOG = load();
    }
    private DungeonModernRoomPolicy() { }

    public static Catalog catalog() { return Holder.CATALOG; }

    /** Same Java int overflow and block-name fold as Noamm's modern getCore. */
    public static Optional<Integer> hashColumn(List<String> column) {
        if (column == null || column.size() != COLUMN_SIZE) return Optional.empty();
        int hash = 1;
        for (String raw : column) {
            if (raw == null || raw.isBlank()) return Optional.empty();
            String id = raw.trim();
            int state = id.indexOf('[');
            if (state >= 0) id = id.substring(0, state);
            if (!id.contains(":")) id = "minecraft:" + id;
            if (IGNORED.contains(id) || id.endsWith("_planks")) id = "minecraft:air";
            hash = hash * 31 + id.hashCode();
        }
        return Optional.of(hash);
    }

    public static Optional<DungeonRoomDataPolicy.RoomMeta> identify(List<String> column) {
        return hashColumn(column).map(catalog().byCore()::get).map(Room::meta);
    }

    public static boolean owns(DungeonRoomDataPolicy.RoomMeta room) {
        return room != null && catalog().byId().containsKey(room.roomId());
    }

    public static List<DungeonRoomDataPolicy.PlacedWaypoint> placeSecrets(
            DungeonRoomDataPolicy.RoomMeta room, DungeonRoomDataPolicy.Rotation rotation) {
        if (!owns(room) || rotation == null) return List.of();
        List<DungeonRoomDataPolicy.PlacedWaypoint> out = new ArrayList<>();
        for (DungeonRoomDataPolicy.SecretWaypoint secret : catalog().byId().get(room.roomId()).secrets()) {
            var world = DungeonRoomDataPolicy.fromComp(secret.x(), secret.y(), secret.z(), rotation);
            out.add(new DungeonRoomDataPolicy.PlacedWaypoint(secret.kind(), world.x(), world.y(), world.z(), room.name()));
        }
        return List.copyOf(out);
    }

    public static Optional<DungeonRoomDataPolicy.Rotation> resolveRotation(
            DungeonRoomDataPolicy.RoomMeta room, List<DungeonRoomDataPolicy.MapTile> tiles,
            int highestY, DungeonRoomDataPolicy.BlockAt world) {
        if (room == null || world == null || highestY <= 0 || tiles == null) return Optional.empty();
        List<DungeonRoomDataPolicy.MapTile> unique = tiles.stream().filter(java.util.Objects::nonNull).distinct().toList();
        int count = tileCount(room.shape());
        if (count == 0 || unique.size() != count || !shapeMatches(room.shape(), unique) || unique.stream().anyMatch(tile ->
                tile.tileX() < 0 || tile.tileX() >= 6 || tile.tileZ() < 0 || tile.tileZ() >= 6)) return Optional.empty();
        if (DungeonRoomDataPolicy.isFairy(room)) {
            var tile = unique.getFirst();
            return Optional.of(DungeonRoomDataPolicy.fairyRotation(
                    DungeonMapPolicy.roomWorldCenter(tile.tileX()), DungeonMapPolicy.roomWorldCenter(tile.tileZ())));
        }
        int minX = unique.stream().mapToInt(tile -> DungeonMapPolicy.roomWorldCenter(tile.tileX())).min().orElseThrow();
        int maxX = unique.stream().mapToInt(tile -> DungeonMapPolicy.roomWorldCenter(tile.tileX())).max().orElseThrow();
        int minZ = unique.stream().mapToInt(tile -> DungeonMapPolicy.roomWorldCenter(tile.tileZ())).min().orElseThrow();
        int maxZ = unique.stream().mapToInt(tile -> DungeonMapPolicy.roomWorldCenter(tile.tileZ())).max().orElseThrow();
        List<DungeonRoomDataPolicy.Rotation> candidates = new ArrayList<>();
        if ("L".equalsIgnoreCase(room.shape())) {
            for (var tile : unique) addCorners(candidates, DungeonMapPolicy.roomWorldCenter(tile.tileX()),
                    DungeonMapPolicy.roomWorldCenter(tile.tileX()), DungeonMapPolicy.roomWorldCenter(tile.tileZ()),
                    DungeonMapPolicy.roomWorldCenter(tile.tileZ()));
        } else addCorners(candidates, minX, maxX, minZ, maxZ);
        DungeonRoomDataPolicy.Rotation found = null;
        for (var candidate : candidates.stream().distinct().toList()) {
            String id = world.idAt(candidate.cornerX(), highestY, candidate.cornerZ());
            if (id == null || id.isBlank()) return Optional.empty(); // Not all corners are loaded yet.
            if (!DungeonRoomDataPolicy.isBlueTerracotta(id)) continue;
            if (found != null && !found.equals(candidate)) return Optional.empty();
            found = candidate;
        }
        return Optional.ofNullable(found);
    }

    public static int highestY(int x, int z, DungeonRoomDataPolicy.BlockAt blocks) {
        for (int y = 256; y >= 0; y--) {
            String id = blocks.idAt(x, y, z);
            if (id == null || id.isBlank()) return 0;
            if (!Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air", "minecraft:gold_block").contains(id)) return y;
        }
        return 0;
    }

    private static boolean shapeMatches(String shape, List<DungeonRoomDataPolicy.MapTile> tiles) {
        int dx = tiles.stream().mapToInt(DungeonRoomDataPolicy.MapTile::tileX).max().orElse(0)
                - tiles.stream().mapToInt(DungeonRoomDataPolicy.MapTile::tileX).min().orElse(0) + 1;
        int dz = tiles.stream().mapToInt(DungeonRoomDataPolicy.MapTile::tileZ).max().orElse(0)
                - tiles.stream().mapToInt(DungeonRoomDataPolicy.MapTile::tileZ).min().orElse(0) + 1;
        if ("L".equalsIgnoreCase(shape) || "2x2".equalsIgnoreCase(shape)) return dx == 2 && dz == 2;
        return (dx == 1 && dz == tiles.size()) || (dz == 1 && dx == tiles.size());
    }

    private static void addCorners(List<DungeonRoomDataPolicy.Rotation> out, int minX, int maxX, int minZ, int maxZ) {
        out.add(new DungeonRoomDataPolicy.Rotation(0, minX - 15, minZ - 15));
        out.add(new DungeonRoomDataPolicy.Rotation(90, maxX + 15, minZ - 15));
        out.add(new DungeonRoomDataPolicy.Rotation(180, maxX + 15, maxZ + 15));
        out.add(new DungeonRoomDataPolicy.Rotation(270, minX - 15, maxZ + 15));
    }

    public static int tileCount(String shape) {
        return switch (shape == null ? "" : shape.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "1X1" -> 1;
            case "1X2" -> 2;
            case "1X3", "L" -> 3;
            case "1X4", "2X2" -> 4;
            default -> 0;
        };
    }

    static Catalog parse(String json) {
        List<Room> rooms = new ArrayList<>();
        Map<Integer, Room> byCore = new HashMap<>();
        Map<Integer, Room> byId = new LinkedHashMap<>();
        Set<Integer> ambiguous = new java.util.HashSet<>();
        for (JsonElement element : JsonParser.parseString(json).getAsJsonArray()) {
            JsonObject obj = element.getAsJsonObject();
            String name = obj.get("name").getAsString();
            String type = obj.get("type").getAsString();
            String shape = obj.get("shape").getAsString();
            List<Integer> cores = new ArrayList<>();
            for (JsonElement core : obj.getAsJsonArray("cores")) cores.add(core.getAsInt());
            int count = obj.has("secrets") ? obj.get("secrets").getAsInt() : 0;
            int id = -1000 - rooms.size();
            List<DungeonRoomDataPolicy.SecretWaypoint> secrets = new ArrayList<>();
            JsonObject coords = obj.has("secretCoords") && obj.get("secretCoords").isJsonObject()
                    ? obj.getAsJsonObject("secretCoords") : new JsonObject();
            addSecrets(secrets, coords, "chest", DungeonRoomDataPolicy.SecretKind.CHEST);
            addSecrets(secrets, coords, "item", DungeonRoomDataPolicy.SecretKind.ITEM);
            addSecrets(secrets, coords, "wither", DungeonRoomDataPolicy.SecretKind.ESSENCE);
            addSecrets(secrets, coords, "bat", DungeonRoomDataPolicy.SecretKind.BAT);
            addSecrets(secrets, coords, "redstoneKey", DungeonRoomDataPolicy.SecretKind.REDSTONE);
            Room room = new Room(new DungeonRoomDataPolicy.RoomMeta(name, type, count, cores, id, shape), secrets);
            rooms.add(room); byId.put(id, room);
            for (int core : cores) {
                Room previous = byCore.putIfAbsent(core, room);
                if (previous != null && !previous.meta().name().equals(name)) ambiguous.add(core);
            }
        }
        ambiguous.forEach(byCore::remove);
        return new Catalog(rooms, byCore, byId);
    }

    private static void addSecrets(List<DungeonRoomDataPolicy.SecretWaypoint> out, JsonObject coords,
                                   String key, DungeonRoomDataPolicy.SecretKind kind) {
        if (!coords.has(key) || !coords.get(key).isJsonArray()) return;
        JsonArray values = coords.getAsJsonArray(key);
        for (JsonElement value : values) {
            JsonObject pos = value.getAsJsonObject();
            out.add(new DungeonRoomDataPolicy.SecretWaypoint(kind,
                    pos.get("x").getAsInt(), pos.get("y").getAsInt(), pos.get("z").getAsInt()));
        }
    }

    private static Catalog load() {
        try (InputStream in = DungeonModernRoomPolicy.class.getResourceAsStream("/rotclient/dungeons/rooms-modern.json")) {
            if (in == null) throw new IllegalStateException("Missing pinned modern dungeon room data");
            return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (java.io.IOException error) { throw new IllegalStateException("Unreadable modern dungeon room data", error); }
    }
}
