package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Live Ice Fill, Boulder and Water Board layouts from the shipped puzzle
 * boards. Relative coordinates use the same blue-terracotta corner rotation
 * as hashed rooms.
 */
public final class DungeonPuzzleBoardPolicy {
    public record RelPos(int x, int y, int z) {
    }

    public record IceFillFloor(int y, List<RelPos> path) {
        public IceFillFloor {
            path = path == null ? List.of() : List.copyOf(path);
        }
    }

    public record BoulderClick(RelPos render, RelPos click) {
    }

    public record WaterLever(String id, RelPos pos, List<Double> times) {
        public WaterLever {
            id = id == null ? "" : id;
            times = times == null ? List.of() : List.copyOf(times);
        }
    }

    public record CreeperPair(RelPos a, RelPos b) {
    }

    private static final List<RelPos> QUIZ_OPTIONS = List.of(
            new RelPos(20, 70, 6),
            new RelPos(15, 70, 9),
            new RelPos(10, 70, 6));

    @FunctionalInterface
    public interface BlockProbe {
        String idAt(int x, int y, int z);
    }

    private static final List<RelPos> TELEPORT_MAZE_PADS = List.of(
            new RelPos(4, 69, 12), new RelPos(4, 69, 6),
            new RelPos(10, 69, 12), new RelPos(10, 69, 6),
            new RelPos(4, 69, 20), new RelPos(4, 69, 14),
            new RelPos(10, 69, 20), new RelPos(10, 69, 14),
            new RelPos(4, 69, 28), new RelPos(4, 69, 22),
            new RelPos(10, 69, 28), new RelPos(10, 69, 22),
            new RelPos(12, 69, 28), new RelPos(12, 69, 22),
            new RelPos(18, 69, 28), new RelPos(18, 69, 22),
            new RelPos(20, 69, 28), new RelPos(20, 69, 22),
            new RelPos(26, 69, 28), new RelPos(26, 69, 22),
            new RelPos(26, 69, 20), new RelPos(26, 69, 14),
            new RelPos(20, 69, 20), new RelPos(20, 69, 14),
            new RelPos(26, 69, 12), new RelPos(26, 69, 6),
            new RelPos(20, 69, 12), new RelPos(20, 69, 6),
            new RelPos(15, 69, 14), new RelPos(15, 69, 12));
    private static final RelPos[] WATER_WOOL = {
            new RelPos(15, 56, 19),
            new RelPos(15, 56, 18),
            new RelPos(15, 56, 17),
            new RelPos(15, 56, 16),
            new RelPos(15, 56, 15)
    };
    private static final Map<String, RelPos> WATER_LEVERS = Map.of(
            "coal_block", new RelPos(20, 61, 10),
            "gold_block", new RelPos(20, 61, 15),
            "quartz_block", new RelPos(20, 61, 20),
            "diamond_block", new RelPos(10, 61, 20),
            "emerald_block", new RelPos(10, 61, 15),
            "hardened_clay", new RelPos(10, 61, 10),
            "water", new RelPos(15, 60, 5));

    private static volatile Catalog catalog;

    private DungeonPuzzleBoardPolicy() {
    }

    public static boolean isAir(String blockId) {
        String id = DungeonLeftoverPolicy.path(blockId);
        return id.isBlank() || id.equals("air") || id.equals("cave_air") || id.equals("void_air");
    }

    public static boolean isSeaLantern(String blockId) {
        return DungeonLeftoverPolicy.path(blockId).equals("sea_lantern");
    }

    public static List<RelPos> quizOptionRels() {
        return QUIZ_OPTIONS;
    }

    public static List<CreeperPair> creeperBeamPairs() {
        return catalog().creeper();
    }

    public static String boulderSignature(Function<RelPos, Boolean> airAt) {
        StringBuilder out = new StringBuilder(42);
        for (int z = 24; z >= 9; z -= 3) {
            for (int x = 24; x >= 6; x -= 3) {
                boolean air = airAt != null && Boolean.TRUE.equals(airAt.apply(new RelPos(x, 66, z)));
                out.append(air ? '0' : '1');
            }
        }
        return out.toString();
    }

    public static List<BoulderClick> boulderClicks(String signature) {
        List<List<Integer>> rows = catalog().boulder.get(signature == null ? "" : signature);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<BoulderClick> out = new ArrayList<>();
        for (List<Integer> row : rows) {
            if (row == null || row.size() < 4) {
                continue;
            }
            out.add(new BoulderClick(
                    new RelPos(row.get(0), 65, row.get(1)),
                    new RelPos(row.get(2), 65, row.get(3))));
        }
        return List.copyOf(out);
    }

    public static List<IceFillFloor> iceFillFloors(BlockProbe probe, boolean hard) {
        if (probe == null) {
            return List.of();
        }
        Catalog loaded = catalog();
        List<List<List<RelPos>>> patterns = hard ? loaded.iceHard() : loaded.iceEasy();
        List<IceFillFloor> out = new ArrayList<>();
        int floors = Math.min(3, Math.min(loaded.iceIdentifier().size(), patterns.size()));
        for (int floor = 0; floor < floors; floor++) {
            List<List<RelPos>> identifiers = loaded.iceIdentifier().get(floor);
            List<List<RelPos>> paths = patterns.get(floor);
            int variants = Math.min(identifiers.size(), paths.size());
            for (int index = 0; index < variants; index++) {
                List<RelPos> pair = identifiers.get(index);
                if (pair.size() < 2) {
                    continue;
                }
                RelPos first = pair.get(0);
                RelPos second = pair.get(1);
                if (!isAir(probe.idAt(first.x(), first.y(), first.z()))
                        || isAir(probe.idAt(second.x(), second.y(), second.z()))) {
                    continue;
                }
                List<RelPos> path = paths.get(index);
                int y = path.isEmpty() ? first.y() : path.getFirst().y();
                out.add(new IceFillFloor(y, path));
                break;
            }
        }
        return List.copyOf(out);
    }

    public static List<RelPos> teleportMazePads() {
        return TELEPORT_MAZE_PADS;
    }

    public static Optional<Integer> waterPatternId(BlockProbe probe) {
        if (probe == null) {
            return Optional.empty();
        }
        String at1477 = blockPath(probe, 14, 77, 27);
        if (at1477.equals("terracotta") || at1477.equals("hardened_clay")) {
            return Optional.of(0);
        }
        if (blockPath(probe, 16, 78, 27).equals("emerald_block")) {
            return Optional.of(1);
        }
        String at1478 = blockPath(probe, 14, 78, 27);
        if (at1478.equals("diamond_block")) {
            return Optional.of(2);
        }
        if (at1478.equals("quartz_block")) {
            return Optional.of(3);
        }
        return Optional.empty();
    }

    public static String waterExtendedSlots(BlockProbe probe) {
        if (probe == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < WATER_WOOL.length; i++) {
            RelPos pos = WATER_WOOL[i];
            if (!isAir(probe.idAt(pos.x(), pos.y(), pos.z()))) {
                out.append(i);
            }
        }
        return out.toString();
    }

    public static List<WaterLever> waterLevers(int patternId, String extendedSlots, boolean optimized) {
        if (extendedSlots == null || extendedSlots.length() != 3) {
            return List.of();
        }
        Map<String, Map<String, Map<String, List<Double>>>> byPattern =
                catalog().water.get(optimized ? "true" : "false");
        if (byPattern == null) {
            return List.of();
        }
        Map<String, Map<String, List<Double>>> bySlots = byPattern.get(String.valueOf(patternId));
        if (bySlots == null) {
            return List.of();
        }
        Map<String, List<Double>> levers = bySlots.get(extendedSlots);
        if (levers == null) {
            return List.of();
        }
        List<WaterLever> out = new ArrayList<>();
        levers.forEach((id, times) -> {
            RelPos pos = WATER_LEVERS.get(id);
            if (pos != null) {
                out.add(new WaterLever(id, pos, times));
            }
        });
        return List.copyOf(out);
    }

    public static DungeonRoomDataPolicy.IntVec world(
            RelPos rel,
            DungeonRoomDataPolicy.Rotation rotation) {
        if (rel == null) {
            return new DungeonRoomDataPolicy.IntVec(0, 0, 0);
        }
        return DungeonRoomDataPolicy.fromComp(rel.x(), rel.y(), rel.z(), rotation);
    }

    static Catalog catalog() {
        Catalog loaded = catalog;
        if (loaded == null) {
            synchronized (DungeonPuzzleBoardPolicy.class) {
                loaded = catalog;
                if (loaded == null) {
                    loaded = load();
                    catalog = loaded;
                }
            }
        }
        return loaded;
    }

    private static String blockPath(BlockProbe probe, int x, int y, int z) {
        return DungeonLeftoverPolicy.path(probe.idAt(x, y, z));
    }

    private static Catalog load() {
        return new Catalog(
                loadIce("/rotclient/dungeons/iceFillFloors.json"),
                loadBoulder("/rotclient/dungeons/boulderSolutions.json"),
                loadWater("/rotclient/dungeons/waterSolutions.json"),
                loadCreeper("/rotclient/dungeons/creeperBeamsSolutions.json"));
    }

    private static IceFillData loadIce(String path) {
        JsonObject root = parseObject(path);
        return new IceFillData(
                readIceTriple(root.get("identifier")),
                readIceTriple(root.get("easy")),
                readIceTriple(root.get("hard")));
    }

    private static List<List<List<RelPos>>> readIceTriple(JsonElement element) {
        List<List<List<RelPos>>> floors = new ArrayList<>();
        if (element == null || !element.isJsonArray()) {
            return floors;
        }
        for (JsonElement floorEl : element.getAsJsonArray()) {
            List<List<RelPos>> variants = new ArrayList<>();
            if (!floorEl.isJsonArray()) {
                floors.add(variants);
                continue;
            }
            for (JsonElement variantEl : floorEl.getAsJsonArray()) {
                List<RelPos> path = new ArrayList<>();
                if (variantEl.isJsonArray()) {
                    for (JsonElement posEl : variantEl.getAsJsonArray()) {
                        relPos(posEl).ifPresent(path::add);
                    }
                }
                variants.add(List.copyOf(path));
            }
            floors.add(List.copyOf(variants));
        }
        return List.copyOf(floors);
    }

    private static Optional<RelPos> relPos(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject obj = element.getAsJsonObject();
        if (!obj.has("x") || !obj.has("y") || !obj.has("z")) {
            return Optional.empty();
        }
        return Optional.of(new RelPos(obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt()));
    }

    private static Map<String, List<List<Integer>>> loadBoulder(String path) {
        JsonObject root = parseObject(path);
        Map<String, List<List<Integer>>> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            List<List<Integer>> rows = new ArrayList<>();
            if (entry.getValue().isJsonArray()) {
                for (JsonElement rowEl : entry.getValue().getAsJsonArray()) {
                    List<Integer> row = new ArrayList<>();
                    if (rowEl.isJsonArray()) {
                        for (JsonElement value : rowEl.getAsJsonArray()) {
                            row.add(value.getAsInt());
                        }
                    }
                    rows.add(List.copyOf(row));
                }
            }
            out.put(entry.getKey(), List.copyOf(rows));
        }
        return Map.copyOf(out);
    }

    private static Map<String, Map<String, Map<String, Map<String, List<Double>>>>> loadWater(String path) {
        JsonObject root = parseObject(path);
        Map<String, Map<String, Map<String, Map<String, List<Double>>>>> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> optimized : root.entrySet()) {
            Map<String, Map<String, Map<String, List<Double>>>> byPattern = new LinkedHashMap<>();
            if (!optimized.getValue().isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> pattern : optimized.getValue().getAsJsonObject().entrySet()) {
                Map<String, Map<String, List<Double>>> bySlots = new LinkedHashMap<>();
                if (!pattern.getValue().isJsonObject()) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> slots : pattern.getValue().getAsJsonObject().entrySet()) {
                    Map<String, List<Double>> levers = new LinkedHashMap<>();
                    if (!slots.getValue().isJsonObject()) {
                        continue;
                    }
                    for (Map.Entry<String, JsonElement> lever : slots.getValue().getAsJsonObject().entrySet()) {
                        List<Double> times = new ArrayList<>();
                        if (lever.getValue().isJsonArray()) {
                            for (JsonElement value : lever.getValue().getAsJsonArray()) {
                                times.add(value.getAsDouble());
                            }
                        }
                        levers.put(lever.getKey().toLowerCase(Locale.ROOT), List.copyOf(times));
                    }
                    bySlots.put(slots.getKey(), Map.copyOf(levers));
                }
                byPattern.put(pattern.getKey(), Map.copyOf(bySlots));
            }
            out.put(optimized.getKey(), Map.copyOf(byPattern));
        }
        return Map.copyOf(out);
    }

    private static List<CreeperPair> loadCreeper(String path) {
        String json = readResource(path);
        List<CreeperPair> out = new ArrayList<>();
        if (json == null || json.isBlank() || json.trim().startsWith("{")) {
            return List.of();
        }
        JsonElement parsed = JsonParser.parseString(json);
        if (!parsed.isJsonArray()) {
            return List.of();
        }
        for (JsonElement rowEl : parsed.getAsJsonArray()) {
            if (!rowEl.isJsonArray()) {
                continue;
            }
            JsonArray row = rowEl.getAsJsonArray();
            if (row.size() < 6) {
                continue;
            }
            out.add(new CreeperPair(
                    new RelPos(row.get(0).getAsInt(), row.get(1).getAsInt(), row.get(2).getAsInt()),
                    new RelPos(row.get(3).getAsInt(), row.get(4).getAsInt(), row.get(5).getAsInt())));
        }
        return List.copyOf(out);
    }

    private static JsonObject parseObject(String path) {
        String json = readResource(path);
        if (json.isBlank()) {
            return new JsonObject();
        }
        JsonElement parsed = JsonParser.parseString(json);
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }

    private static String readResource(String path) {
        try (InputStream in = DungeonPuzzleBoardPolicy.class.getResourceAsStream(path)) {
            if (in == null) {
                return "{}";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private record IceFillData(
            List<List<List<RelPos>>> identifier,
            List<List<List<RelPos>>> easy,
            List<List<List<RelPos>>> hard) {
        private IceFillData {
            identifier = identifier == null ? List.of() : List.copyOf(identifier);
            easy = easy == null ? List.of() : List.copyOf(easy);
            hard = hard == null ? List.of() : List.copyOf(hard);
        }
    }

    private record Catalog(
            IceFillData ice,
            Map<String, List<List<Integer>>> boulder,
            Map<String, Map<String, Map<String, Map<String, List<Double>>>>> water,
            List<CreeperPair> creeper) {
        private Catalog {
            ice = ice == null ? new IceFillData(List.of(), List.of(), List.of()) : ice;
            boulder = boulder == null ? Map.of() : Map.copyOf(boulder);
            water = water == null ? Map.of() : Map.copyOf(water);
            creeper = creeper == null ? List.of() : List.copyOf(creeper);
        }

        private List<List<List<RelPos>>> iceIdentifier() {
            return ice.identifier();
        }

        private List<List<List<RelPos>>> iceEasy() {
            return ice.easy();
        }

        private List<List<List<RelPos>>> iceHard() {
            return ice.hard();
        }
    }
}
