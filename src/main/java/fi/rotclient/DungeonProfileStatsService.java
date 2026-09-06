package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;
import java.util.Optional;

/**
 * Fail-soft SkyCrypt profile parser. Never talks to starred.foo.
 */
public final class DungeonProfileStatsService {
    public static final String PROFILE_URL = "https://sky.shiiyu.moe/api/v2/profile/";

    private DungeonProfileStatsService() {
    }

    public static String urlFor(String player) {
        String name = DungeonCarryPolicy.sanitizePlayer(player);
        if (name.isBlank()) {
            return "";
        }
        return PROFILE_URL + name;
    }

    public static Optional<DungeonPartyFinderPolicy.Stats> parse(String json, String floor) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonElement root = JsonParser.parseString(json);
            JsonObject profile = currentProfile(root);
            if (profile == null) {
                return Optional.empty();
            }
            long secrets = firstLong(profile, "secrets_found", "secretsFound", "secrets");
            double average = firstDouble(profile, "secrets_per_run", "secret_average", "secretAverage");
            int mp = (int) firstLong(profile, "magical_power", "magicalPower");
            int pb = floorPbSeconds(profile, floor);
            if (secrets <= 0L && average <= 0.0D && mp <= 0 && pb <= 0) {
                return Optional.empty();
            }
            return Optional.of(new DungeonPartyFinderPolicy.Stats(secrets, average, mp, pb));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static JsonObject currentProfile(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            return null;
        }
        JsonObject object = root.getAsJsonObject();
        JsonElement profiles = object.get("profiles");
        if (profiles != null && profiles.isJsonObject()) {
            JsonObject chosen = null;
            for (var entry : profiles.getAsJsonObject().entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject profile = entry.getValue().getAsJsonObject();
                chosen = profile;
                if (profile.has("current") && profile.get("current").isJsonPrimitive()
                        && profile.get("current").getAsBoolean()) {
                    return profile;
                }
            }
            return chosen;
        }
        return object;
    }

    private static int floorPbSeconds(JsonObject profile, String floor) {
        String needle = DungeonCarryPolicy.normalizeFloor(floor).toLowerCase(Locale.ROOT);
        int any = (int) firstLong(profile, "fastest_time", "best_time", "fastestTime");
        if (needle.isBlank() || "f7".equals(needle)) {
            int f7 = (int) firstLongNamed(profile, "7", "f7", "floor_7");
            return f7 > 0 ? f7 : any;
        }
        int specific = (int) firstLongNamed(profile, needle, needle.replace("f", ""), needle.replace("m", "master_"));
        return specific > 0 ? specific : any;
    }

    private static long firstLong(JsonObject root, String... keys) {
        Double value = firstNumber(root, keys);
        return value == null ? 0L : Math.round(value);
    }

    private static double firstDouble(JsonObject root, String... keys) {
        Double value = firstNumber(root, keys);
        return value == null ? 0.0D : value;
    }

    private static Double firstNumber(JsonObject root, String... keys) {
        if (root == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Double found = findNumber(root, key.toLowerCase(Locale.ROOT), 0);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static long firstLongNamed(JsonObject root, String... keys) {
        Double value = firstNumber(root, keys);
        return value == null ? 0L : Math.round(value);
    }

    private static Double findNumber(JsonElement element, String key, int depth) {
        if (element == null || depth > 12) {
            return null;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (var entry : object.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)
                        && entry.getValue() != null && entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    return entry.getValue().getAsDouble();
                }
            }
            for (var entry : object.entrySet()) {
                Double nested = findNumber(entry.getValue(), key, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                Double nested = findNumber(child, key, depth + 1);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
