package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

/** Plus-owned Diana options retained from the old nested QoL schema. */
final class DianaSettings {
    private static final Map<String, String> MODULES = Map.of(
            "qol.diana_burrows", "dianaBurrowsEnabled",
            "qol.diana_mobs", "dianaMobsEnabled",
            "qol.diana_profit", "dianaProfitEnabled",
            "qol.diana_share", "dianaShareEnabled");
    private static final Map<String, String> BOOLEANS = Map.ofEntries(
            Map.entry("qol.diana_burrows.guess", "dianaBurrowsGuess"),
            Map.entry("qol.diana_burrows.particles", "dianaBurrowsParticles"),
            Map.entry("qol.diana_burrows.waypoints", "dianaBurrowsWaypoints"),
            Map.entry("qol.diana_burrows.mute_spade", "dianaBurrowsMuteSpade"),
            Map.entry("qol.diana_burrows.fix_chat", "dianaBurrowsFixChat"),
            Map.entry("qol.diana_mobs.rare_esp", "dianaMobsRareEsp"),
            Map.entry("qol.diana_mobs.griffin_warn", "dianaMobsGriffinWarn"),
            Map.entry("qol.diana_profit.hud", "dianaProfitHud"),
            Map.entry("qol.diana_share.party", "dianaShareParty"),
            Map.entry("qol.diana_share.auto_warp", "dianaShareAutoWarp"));
    private static final Map<String, String> COLORS = Map.of(
            "qol.diana_burrows.guess_color", "dianaBurrowsGuessColor",
            "qol.diana_burrows.start_color", "dianaBurrowsStartColor",
            "qol.diana_burrows.mob_color", "dianaBurrowsMobColor",
            "qol.diana_burrows.treasure_color", "dianaBurrowsTreasureColor",
            "qol.diana_mobs.esp_color", "dianaMobsEspColor");
    private static final Map<String, Integer> COLOR_DEFAULTS = Map.of(
            "dianaBurrowsGuessColor", 0xFF55FF55,
            "dianaBurrowsStartColor", 0xFF55FFFF,
            "dianaBurrowsMobColor", 0xFFFF5555,
            "dianaBurrowsTreasureColor", 0xFFFFAA00,
            "dianaMobsEspColor", 0xFFFF55FF);

    private DianaSettings() {}

    static Boolean module(QolUtilityConfig config, String id) {
        String key = MODULES.get(id);
        return key == null ? null : bool(config, key, false);
    }

    static boolean writeModule(QolUtilityConfig config, String id, boolean value) {
        String key = MODULES.get(id);
        if (key == null) return false;
        fields(config).addProperty(key, value);
        return true;
    }

    static Boolean readBoolean(QolUtilityConfig config, String id) {
        String key = BOOLEANS.get(id);
        return key == null ? null : bool(config, key, !key.startsWith("dianaShare"));
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        String key = BOOLEANS.get(id);
        if (key == null) return false;
        fields(config).addProperty(key, value);
        return true;
    }

    static Integer readColor(QolUtilityConfig config, String id) {
        String key = COLORS.get(id);
        return key == null ? null : integer(config, key, COLOR_DEFAULTS.get(key));
    }

    static boolean writeColor(QolUtilityConfig config, String id, int value) {
        String key = COLORS.get(id);
        if (key == null) return false;
        fields(config).addProperty(key, value);
        return true;
    }

    static boolean reset(QolUtilityConfig config, String id) {
        String key = MODULES.get(id);
        if (key == null) return false;
        String prefix = id.equals("qol.diana_burrows") ? "dianaBurrows"
                : id.equals("qol.diana_mobs") ? "dianaMobs"
                : id.equals("qol.diana_share") ? "dianaShare" : "dianaProfit";
        fields(config).keySet().removeIf(field -> field.startsWith(prefix));
        return true;
    }

    private static JsonObject fields(QolUtilityConfig config) {
        QolSkyblockExtras extras = config.extras();
        if (extras.extensionFields == null) extras.extensionFields = new JsonObject();
        return extras.extensionFields;
    }

    private static boolean bool(QolUtilityConfig config, String key, boolean fallback) {
        JsonElement value = fields(config).get(key);
        try {
            return value != null && value.isJsonPrimitive()
                    && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int integer(QolUtilityConfig config, String key, int fallback) {
        JsonElement value = fields(config).get(key);
        try {
            return value != null && value.isJsonPrimitive()
                    && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
