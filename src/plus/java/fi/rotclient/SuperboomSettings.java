package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned typed access to Auto Superboom configuration. */
record SuperboomSettings(
        boolean enabled,
        boolean swapBack,
        int legacyDelay,
        int minDelay,
        int maxDelay,
        int swapBackMin,
        int swapBackMax,
        String swapTo,
        int customSlot,
        String extraBlocks) {

    static SuperboomSettings from(QolUtilityConfig config) {
        JsonObject root = root(config);
        JsonObject athen = athen(config);
        return new SuperboomSettings(
                bool(root, "dungeonF7AutoSuperboom", false),
                bool(root, "dungeonF7SuperboomSwapBack", false),
                integer(root, "dungeonF7SuperboomDelay", 2, 1, 10),
                integer(athen, "superboomMinDelay", 1, 1, 5),
                integer(athen, "superboomMaxDelay", 3, 1, 5),
                integer(athen, "superboomSwapBackMin", 1, 1, 5),
                integer(athen, "superboomSwapBackMax", 3, 1, 5),
                DungeonAthenPortPolicy.normalizeSwapTo(text(athen, "superboomSwapTo", "Original slot")),
                integer(athen, "superboomCustomSlot", 1, 1, 9),
                text(athen, "superboomExtraBlocks", ""));
    }

    static Boolean readBoolean(QolUtilityConfig config, String id) {
        SuperboomSettings s = from(config);
        return switch (id == null ? "" : id) {
            case "qol.dungeon_f7.auto_superboom" -> s.enabled();
            case "qol.dungeon_f7.superboom_swap_back" -> s.swapBack();
            default -> null;
        };
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        String key = switch (id == null ? "" : id) {
            case "qol.dungeon_f7.auto_superboom" -> "dungeonF7AutoSuperboom";
            case "qol.dungeon_f7.superboom_swap_back" -> "dungeonF7SuperboomSwapBack";
            default -> null;
        };
        if (key == null) return false;
        root(config).addProperty(key, value);
        return true;
    }

    static Double readNumber(QolUtilityConfig config, String id) {
        SuperboomSettings s = from(config);
        return switch (id == null ? "" : id) {
            case "qol.dungeon_f7.superboom_delay" -> (double) s.legacyDelay();
            case "qol.dungeon_f7.superboom_min_delay" -> (double) s.minDelay();
            case "qol.dungeon_f7.superboom_max_delay" -> (double) s.maxDelay();
            case "qol.dungeon_f7.superboom_swap_back_min" -> (double) s.swapBackMin();
            case "qol.dungeon_f7.superboom_swap_back_max" -> (double) s.swapBackMax();
            case "qol.dungeon_f7.superboom_custom_slot" -> (double) s.customSlot();
            default -> null;
        };
    }

    static boolean writeNumber(QolUtilityConfig config, String id, double value) {
        int rounded = Double.isFinite(value) ? (int) Math.round(value) : 1;
        switch (id == null ? "" : id) {
            case "qol.dungeon_f7.superboom_delay" ->
                    root(config).addProperty("dungeonF7SuperboomDelay", clamp(rounded, 1, 10));
            case "qol.dungeon_f7.superboom_min_delay" ->
                    athen(config).addProperty("superboomMinDelay", clamp(rounded, 1, 5));
            case "qol.dungeon_f7.superboom_max_delay" ->
                    athen(config).addProperty("superboomMaxDelay", clamp(rounded, 1, 5));
            case "qol.dungeon_f7.superboom_swap_back_min" ->
                    athen(config).addProperty("superboomSwapBackMin", clamp(rounded, 1, 5));
            case "qol.dungeon_f7.superboom_swap_back_max" ->
                    athen(config).addProperty("superboomSwapBackMax", clamp(rounded, 1, 5));
            case "qol.dungeon_f7.superboom_custom_slot" ->
                    athen(config).addProperty("superboomCustomSlot", clamp(rounded, 1, 9));
            default -> { return false; }
        }
        return true;
    }

    static String readEnum(QolUtilityConfig config, String id) {
        return "qol.dungeon_f7.superboom_swap_to".equals(id) ? from(config).swapTo() : null;
    }

    static boolean writeEnum(QolUtilityConfig config, String id, String value) {
        if (!"qol.dungeon_f7.superboom_swap_to".equals(id)) return false;
        athen(config).addProperty("superboomSwapTo", DungeonAthenPortPolicy.normalizeSwapTo(value));
        return true;
    }

    static String readText(QolUtilityConfig config, String id) {
        return "qol.dungeon_f7.superboom_blocks".equals(id) ? from(config).extraBlocks() : null;
    }

    static boolean writeText(QolUtilityConfig config, String id, String value) {
        if (!"qol.dungeon_f7.superboom_blocks".equals(id)) return false;
        athen(config).addProperty("superboomExtraBlocks", value == null ? "" : value.trim());
        return true;
    }

    static void reset(QolUtilityConfig config) {
        for (String key : new String[] {
                "dungeonF7AutoSuperboom", "dungeonF7SuperboomSwapBack", "dungeonF7SuperboomDelay"}) {
            root(config).remove(key);
        }
        for (String key : new String[] {
                "superboomMinDelay", "superboomMaxDelay", "superboomSwapBackMin",
                "superboomSwapBackMax", "superboomSwapTo", "superboomCustomSlot",
                "superboomExtraBlocks"}) {
            athen(config).remove(key);
        }
    }

    private static JsonObject root(QolUtilityConfig config) {
        QolSkyblockExtras extras = config.extras();
        if (extras.extensionFields == null) extras.extensionFields = new JsonObject();
        return extras.extensionFields;
    }

    private static JsonObject athen(QolUtilityConfig config) {
        DungeonAthenSettings settings = config.extras().athen();
        if (settings.extensionFields == null) settings.extensionFields = new JsonObject();
        return settings.extensionFields;
    }

    private static boolean bool(JsonObject fields, String key, boolean fallback) {
        JsonElement value = fields.get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
    }

    private static int integer(JsonObject fields, String key, int fallback, int min, int max) {
        JsonElement value = fields.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return fallback;
        try { return clamp(value.getAsInt(), min, max); }
        catch (RuntimeException malformed) { return fallback; }
    }

    private static String text(JsonObject fields, String key, String fallback) {
        JsonElement value = fields.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
