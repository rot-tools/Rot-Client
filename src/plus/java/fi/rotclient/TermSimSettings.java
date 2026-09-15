package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned settings for the local Terminal Simulator. */
record TermSimSettings(boolean enabled, String keybind, int ping, boolean showPbs,
                       String pbs, String remoteIp) {
    static TermSimSettings from(QolUtilityConfig config) {
        return new TermSimSettings(
                bool(extras(config), "dungeonTermSimEnabled", false),
                string(extras(config), "dungeonTermSimKeybind", ""),
                integer(extras(config), "dungeonTermSimPing", 0),
                bool(extras(config), "dungeonTermSimShowPbs", true),
                string(extras(config), "dungeonTermSimPbs", ""),
                string(athen(config), "termSimIp", ""));
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        extras(config).addProperty("dungeonTermSimEnabled", value);
    }

    static Boolean readBoolean(QolUtilityConfig config, String id) {
        return "qol.dungeon_termsim.show_pbs".equals(id) ? from(config).showPbs() : null;
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        if (!"qol.dungeon_termsim.show_pbs".equals(id)) return false;
        extras(config).addProperty("dungeonTermSimShowPbs", value);
        return true;
    }

    static Double readNumber(QolUtilityConfig config, String id) {
        return "qol.dungeon_termsim.ping".equals(id) ? (double) from(config).ping() : null;
    }

    static boolean writeNumber(QolUtilityConfig config, String id, double value) {
        if (!"qol.dungeon_termsim.ping".equals(id) || !Double.isFinite(value)) return false;
        ping(config, (int) Math.round(value));
        return true;
    }

    static void ping(QolUtilityConfig config, int value) {
        extras(config).addProperty("dungeonTermSimPing", Math.max(0, Math.min(500, value)));
    }

    static String readKeybind(QolUtilityConfig config, String id) {
        return "qol.dungeon_termsim.keybind".equals(id) ? from(config).keybind() : null;
    }

    static boolean writeKeybind(QolUtilityConfig config, String id, String value) {
        if (!"qol.dungeon_termsim.keybind".equals(id)) return false;
        extras(config).addProperty("dungeonTermSimKeybind", value == null ? "" : value.trim());
        return true;
    }

    static String readText(QolUtilityConfig config, String id) {
        return "qol.dungeon_termsim.ip".equals(id) ? from(config).remoteIp() : null;
    }

    static boolean writeText(QolUtilityConfig config, String id, String value) {
        if (!"qol.dungeon_termsim.ip".equals(id)) return false;
        athen(config).addProperty("termSimIp", value == null ? "" : value.trim());
        return true;
    }

    static void pbs(QolUtilityConfig config, String value) {
        extras(config).addProperty("dungeonTermSimPbs", value == null ? "" : value);
    }

    static void reset(QolUtilityConfig config) {
        for (String key : new String[]{"dungeonTermSimEnabled", "dungeonTermSimKeybind",
                "dungeonTermSimPing", "dungeonTermSimShowPbs", "dungeonTermSimPbs"}) {
            extras(config).remove(key);
        }
        athen(config).remove("termSimIp");
    }

    private static JsonObject extras(QolUtilityConfig config) {
        QolSkyblockExtras extras = config.extras();
        if (extras.extensionFields == null) extras.extensionFields = new JsonObject();
        return extras.extensionFields;
    }

    private static JsonObject athen(QolUtilityConfig config) {
        DungeonAthenSettings athen = config.extras().athen();
        if (athen.extensionFields == null) athen.extensionFields = new JsonObject();
        return athen.extensionFields;
    }

    private static boolean bool(JsonObject fields, String key, boolean fallback) {
        JsonElement value = fields.get(key);
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }

    private static int integer(JsonObject fields, String key, int fallback) {
        JsonElement value = fields.get(key);
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }

    private static String string(JsonObject fields, String key, String fallback) {
        JsonElement value = fields.get(key);
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString() ? value.getAsString() : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }
}
