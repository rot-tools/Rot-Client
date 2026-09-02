package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the bundled Kuudra reference JSON (supply piles, pearl areas, etherwarp
 * highlights) from Rot Client resources. Positions are factual arena facts.
 */
public final class IotaKuudraData {
    static final String PILE_RESOURCE = "/assets/rotclient/iota/pile_locations.json";
    static final String PEARL_RESOURCE = "/assets/rotclient/iota/pearl_waypoints.json";
    static final String ETHERWARP_RESOURCE = "/assets/rotclient/iota/etherwarp_config.json";

    private static volatile List<IotaKuudraPolicy.Pile> piles;
    private static volatile List<IotaKuudraPolicy.PearlArea> pearls;
    private static volatile List<IotaKuudraPolicy.EtherCategory> etherwarp;

    private IotaKuudraData() {
    }

    public static List<IotaKuudraPolicy.Pile> piles() {
        if (piles == null) {
            piles = loadPiles();
        }
        return piles;
    }

    public static List<IotaKuudraPolicy.PearlArea> pearls() {
        if (pearls == null) {
            pearls = loadPearls();
        }
        return pearls;
    }

    public static List<IotaKuudraPolicy.EtherCategory> etherwarp() {
        if (etherwarp == null) {
            etherwarp = loadEtherwarp();
        }
        return etherwarp;
    }

    private static List<IotaKuudraPolicy.Pile> loadPiles() {
        try (Reader reader = reader(PILE_RESOURCE)) {
            if (reader == null) {
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.has("spots") ? root.getAsJsonArray("spots") : root.getAsJsonArray("piles");
            if (array == null) {
                return List.of();
            }
            List<IotaKuudraPolicy.Pile> loaded = new ArrayList<>();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                String name = text(obj, "label", "name");
                parseXyz(obj, "xyz", "position", "coords").ifPresent(vec -> loaded.add(new IotaKuudraPolicy.Pile(
                        name,
                        vec,
                        obj.has("nopre") ? obj.get("nopre").getAsInt()
                                : obj.has("noPreValue") ? obj.get("noPreValue").getAsInt() : 0)));
            }
            return List.copyOf(loaded);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static List<IotaKuudraPolicy.PearlArea> loadPearls() {
        try (Reader reader = reader(PEARL_RESOURCE)) {
            if (reader == null) {
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.has("zones") ? root.getAsJsonArray("zones") : root.getAsJsonArray("areas");
            if (array == null) {
                return List.of();
            }
            List<IotaKuudraPolicy.PearlArea> loaded = new ArrayList<>();
            for (JsonElement element : array) {
                parsePearlArea(element.getAsJsonObject()).ifPresent(loaded::add);
            }
            return List.copyOf(loaded);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static Optional<IotaKuudraPolicy.PearlArea> parsePearlArea(JsonObject obj) {
        try {
            String name = text(obj, "id", "name");
            JsonArray pos1 = obj.has("corner_a") ? obj.getAsJsonArray("corner_a") : obj.getAsJsonArray("pos1");
            JsonArray pos2 = obj.has("corner_b") ? obj.getAsJsonArray("corner_b") : obj.getAsJsonArray("pos2");
            double x1 = pos1.get(0).getAsDouble();
            double z1 = pos1.get(1).getAsDouble();
            double x2 = pos2.get(0).getAsDouble();
            double z2 = pos2.get(1).getAsDouble();
            List<IotaKuudraPolicy.PearlWaypoint> waypoints = new ArrayList<>();
            JsonArray throwsArray = obj.has("throws") ? obj.getAsJsonArray("throws") : obj.getAsJsonArray("waypoints");
            if (throwsArray != null) {
                for (JsonElement element : throwsArray) {
                    parsePearlWaypoint(element.getAsJsonObject()).ifPresent(waypoints::add);
                }
            }
            return Optional.of(new IotaKuudraPolicy.PearlArea(
                    name,
                    Math.min(x1, x2),
                    Math.min(z1, z2),
                    Math.max(x1, x2),
                    Math.max(z1, z2),
                    parseNullableBoolean(obj, obj.has("flip_ns") ? "flip_ns" : "invertForwardBackward"),
                    parseNullableBoolean(obj, obj.has("flip_ew") ? "flip_ew" : "invertLeftRight"),
                    List.copyOf(waypoints)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<IotaKuudraPolicy.PearlWaypoint> parsePearlWaypoint(JsonObject obj) {
        try {
            IotaKuudraPolicy.Vec3d target = parseXyz(obj, "xyz", "coords").orElseThrow();
            int color;
            if (obj.has("hex")) {
                color = parseColor(obj.get("hex"));
            } else {
                JsonArray rgb = obj.getAsJsonArray("rgb");
                color = (rgb.get(0).getAsInt() << 16) | (rgb.get(1).getAsInt() << 8) | rgb.get(2).getAsInt();
            }
            IotaKuudraPolicy.Vec3d stand = parseXyz(obj, "stand", "block").orElse(null);
            Integer pre = obj.has("pre") ? obj.get("pre").getAsInt() : null;
            Integer hide = obj.has("hide_pre")
                    ? obj.get("hide_pre").getAsInt()
                    : obj.has("hideForPre") ? obj.get("hideForPre").getAsInt() : null;
            float size = obj.has("scale")
                    ? obj.get("scale").getAsFloat()
                    : obj.has("size") ? obj.get("size").getAsFloat() : IotaKuudraPolicy.PEARL_DEFAULT_SIZE;
            String label = obj.has("tag")
                    ? obj.get("tag").getAsString()
                    : obj.has("text") ? obj.get("text").getAsString() : "";
            boolean alert = (obj.has("ping") && obj.get("ping").getAsBoolean())
                    || (obj.has("alert") && obj.get("alert").getAsBoolean());
            return Optional.of(new IotaKuudraPolicy.PearlWaypoint(
                    target, color, stand, pre, hide, size, label, alert));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static List<IotaKuudraPolicy.EtherCategory> loadEtherwarp() {
        try (Reader reader = reader(ETHERWARP_RESOURCE)) {
            if (reader == null) {
                return List.of();
            }
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.has("groups") ? root.getAsJsonArray("groups") : root.getAsJsonArray("categories");
            if (array == null) {
                return List.of();
            }
            List<IotaKuudraPolicy.EtherCategory> loaded = new ArrayList<>();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    parseEtherCategory(element.getAsJsonObject()).ifPresent(loaded::add);
                }
            }
            return List.copyOf(loaded);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static Optional<IotaKuudraPolicy.EtherCategory> parseEtherCategory(JsonObject obj) {
        try {
            String name = text(obj, "title", "name");
            boolean enabled = obj.has("on")
                    ? obj.get("on").getAsBoolean()
                    : !obj.has("enabled") || obj.get("enabled").getAsBoolean();
            JsonArray waypoints = obj.has("marks") ? obj.getAsJsonArray("marks") : obj.getAsJsonArray("waypoints");
            List<IotaKuudraPolicy.EtherWaypoint> list = new ArrayList<>();
            if (waypoints != null) {
                for (JsonElement element : waypoints) {
                    if (element.isJsonObject()) {
                        parseEtherWaypoint(element.getAsJsonObject()).ifPresent(list::add);
                    }
                }
            }
            return Optional.of(new IotaKuudraPolicy.EtherCategory(name, enabled, List.copyOf(list)));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<IotaKuudraPolicy.EtherWaypoint> parseEtherWaypoint(JsonObject obj) {
        try {
            String name = text(obj, "label", "name");
            List<IotaKuudraPolicy.Vec3d> positions = parsePositions(obj);
            int color = parseColor(obj.has("hex") ? obj.get("hex") : obj.get("color"));
            List<Integer> colors = parseColors(obj);
            float alpha = obj.has("alpha") ? obj.get("alpha").getAsFloat() : IotaKuudraPolicy.ETHERWARP_DEFAULT_ALPHA;
            alpha = Math.clamp(alpha, 0.0F, 1.0F);
            String style = obj.has("draw")
                    ? obj.get("draw").getAsString()
                    : obj.has("renderStyle") ? obj.get("renderStyle").getAsString() : "OUTLINE";
            float lineWidth = obj.has("line")
                    ? obj.get("line").getAsFloat()
                    : obj.has("lineWidth") ? obj.get("lineWidth").getAsFloat() : 2.0F;
            lineWidth = Math.clamp(lineWidth, 0.5F, 4.0F);
            Set<IotaKuudraPolicy.Phase> show = IotaKuudraPolicy.parsePhases(
                    stringList(obj, obj.has("phases") ? "phases" : "showInPhases"));
            Set<IotaKuudraPolicy.Phase> hide = IotaKuudraPolicy.parsePhases(
                    stringList(obj, obj.has("skip_phases") ? "skip_phases" : "hideInPhases"));
            float maxDistance = obj.has("max_dist")
                    ? obj.get("max_dist").getAsFloat()
                    : obj.has("maxRenderDistance") ? obj.get("maxRenderDistance").getAsFloat() : -1.0F;
            IotaKuudraPolicy.HighlightShape shape = IotaKuudraPolicy.parseShape(
                    obj.has("shape") ? obj.get("shape").getAsString() : "FULL");
            IotaKuudraPolicy.Vec3d boxMin = new IotaKuudraPolicy.Vec3d(-0.5D, 0.0D, -0.5D);
            IotaKuudraPolicy.Vec3d boxMax = new IotaKuudraPolicy.Vec3d(0.5D, 1.0D, 0.5D);
            if (obj.has("box") && obj.get("box").isJsonObject()) {
                JsonObject box = obj.getAsJsonObject("box");
                Optional<IotaKuudraPolicy.Vec3d> min = parseVec(box.getAsJsonArray("min"));
                Optional<IotaKuudraPolicy.Vec3d> max = parseVec(box.getAsJsonArray("max"));
                if (min.isPresent() && max.isPresent()
                        && max.get().x() > min.get().x()
                        && max.get().y() > min.get().y()
                        && max.get().z() > min.get().z()) {
                    boxMin = min.get();
                    boxMax = max.get();
                }
            }
            IotaKuudraPolicy.EtherWaypoint waypoint = new IotaKuudraPolicy.EtherWaypoint(
                    name, positions, color, colors, alpha, style, lineWidth,
                    show, hide, maxDistance, shape, boxMin, boxMax);
            return waypoint.valid() ? Optional.of(waypoint) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static List<IotaKuudraPolicy.Vec3d> parsePositions(JsonObject obj) {
        List<IotaKuudraPolicy.Vec3d> positions = new ArrayList<>();
        if (obj.has("xyz_list") && obj.get("xyz_list").isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray("xyz_list")) {
                parseXyzValue(element).ifPresent(positions::add);
            }
        }
        if (obj.has("xyz")) {
            parseXyzValue(obj.get("xyz")).ifPresent(positions::add);
        }
        if (obj.has("positions") && obj.get("positions").isJsonArray()) {
            for (JsonElement element : obj.getAsJsonArray("positions")) {
                parseXyzValue(element).ifPresent(positions::add);
            }
        }
        if (obj.has("position")) {
            parseXyzValue(obj.get("position")).ifPresent(positions::add);
        }
        return List.copyOf(positions);
    }

    private static List<String> stringList(JsonObject obj, String field) {
        if (!obj.has(field) || !obj.get(field).isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : obj.getAsJsonArray(field)) {
            try {
                values.add(element.getAsString());
            } catch (Exception ignored) {
                values.add("");
            }
        }
        return values;
    }

    private static List<Integer> parseColors(JsonObject obj) {
        String field = obj.has("hex_list") ? "hex_list" : "colors";
        if (!obj.has(field) || !obj.get(field).isJsonArray()) {
            return List.of();
        }
        List<Integer> colors = new ArrayList<>();
        for (JsonElement element : obj.getAsJsonArray(field)) {
            colors.add(parseColor(element));
        }
        return List.copyOf(colors);
    }

    private static int parseColor(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return 0xFFFFFF;
        }
        try {
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                if (array.size() == 3) {
                    int r = Math.clamp(array.get(0).getAsInt(), 0, 255);
                    int g = Math.clamp(array.get(1).getAsInt(), 0, 255);
                    int b = Math.clamp(array.get(2).getAsInt(), 0, 255);
                    return (r << 16) | (g << 8) | b;
                }
            }
            if (element.isJsonPrimitive()) {
                String text = element.getAsString().trim();
                if (text.startsWith("#")) {
                    text = text.substring(1);
                }
                if (text.length() == 6) {
                    return Integer.parseInt(text, 16);
                }
            }
        } catch (Exception ignored) {
            return 0xFFFFFF;
        }
        return 0xFFFFFF;
    }

    private static Boolean parseNullableBoolean(JsonObject obj, String key) {
        if (!obj.has(key)) {
            return false;
        }
        JsonElement element = obj.get(key);
        return element != null && !element.isJsonNull() ? element.getAsBoolean() : null;
    }

    private static String text(JsonObject obj, String primary, String fallback) {
        if (obj.has(primary)) {
            return obj.get(primary).getAsString();
        }
        if (obj.has(fallback)) {
            return obj.get(fallback).getAsString();
        }
        return "";
    }

    private static Optional<IotaKuudraPolicy.Vec3d> parseXyz(JsonObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key)) {
                Optional<IotaKuudraPolicy.Vec3d> parsed = parseXyzValue(obj.get(key));
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<IotaKuudraPolicy.Vec3d> parseXyzValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }
        if (element.isJsonArray()) {
            return parseVec(element.getAsJsonArray());
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (!obj.has("x") || !obj.has("y") || !obj.has("z")) {
                return Optional.empty();
            }
            try {
                return Optional.of(new IotaKuudraPolicy.Vec3d(
                        obj.get("x").getAsDouble(),
                        obj.get("y").getAsDouble(),
                        obj.get("z").getAsDouble()));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static Optional<IotaKuudraPolicy.Vec3d> parseVec(JsonArray array) {
        if (array == null || array.size() != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new IotaKuudraPolicy.Vec3d(
                    array.get(0).getAsDouble(),
                    array.get(1).getAsDouble(),
                    array.get(2).getAsDouble()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Reader reader(String resource) {
        InputStream stream = IotaKuudraData.class.getResourceAsStream(resource);
        return stream == null ? null : new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
