package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned terminal click trail options from the old Athen schema. */
record DungeonTerminalClickSettings(boolean enabled, int radius, int thickness,
                                    int leftColor, int rightColor) {
    static DungeonTerminalClickSettings from(QolUtilityConfig config) {
        return new DungeonTerminalClickSettings(
                bool(config, "termClickEnabled", false),
                integer(config, "termClickRadius", 4),
                integer(config, "termClickThickness", 2),
                integer(config, "termClickLeftColor", 0xFFC084FC),
                integer(config, "termClickRightColor", 0xFFFDBA74));
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        fields(config).addProperty("termClickEnabled", value);
    }

    static Double readNumber(QolUtilityConfig config, String id) {
        return switch (id == null ? "" : id) {
            case "qol.dungeon_term_click.radius" -> (double) from(config).radius();
            case "qol.dungeon_term_click.thickness" -> (double) from(config).thickness();
            default -> null;
        };
    }

    static boolean writeNumber(QolUtilityConfig config, String id, double value) {
        if (!Double.isFinite(value)) return false;
        switch (id == null ? "" : id) {
            case "qol.dungeon_term_click.radius" -> fields(config).addProperty(
                    "termClickRadius", Math.max(1, Math.min(16, (int) Math.round(value))));
            case "qol.dungeon_term_click.thickness" -> fields(config).addProperty(
                    "termClickThickness", Math.max(1, Math.min(8, (int) Math.round(value))));
            default -> { return false; }
        }
        return true;
    }

    static Integer readColor(QolUtilityConfig config, String id) {
        return switch (id == null ? "" : id) {
            case "qol.dungeon_term_click.left_color" -> from(config).leftColor();
            case "qol.dungeon_term_click.right_color" -> from(config).rightColor();
            default -> null;
        };
    }

    static boolean writeColor(QolUtilityConfig config, String id, int value) {
        switch (id == null ? "" : id) {
            case "qol.dungeon_term_click.left_color" -> fields(config).addProperty("termClickLeftColor", value);
            case "qol.dungeon_term_click.right_color" -> fields(config).addProperty("termClickRightColor", value);
            default -> { return false; }
        }
        return true;
    }

    static void reset(QolUtilityConfig config) {
        for (String key : new String[]{"termClickEnabled", "termClickRadius", "termClickThickness",
                "termClickLeftColor", "termClickRightColor"}) fields(config).remove(key);
    }

    private static JsonObject fields(QolUtilityConfig config) {
        DungeonAthenSettings athen = config.extras().athen();
        if (athen.extensionFields == null) athen.extensionFields = new JsonObject();
        return athen.extensionFields;
    }

    private static boolean bool(QolUtilityConfig config, String key, boolean fallback) {
        JsonElement value = fields(config).get(key);
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }

    private static int integer(QolUtilityConfig config, String key, int fallback) {
        JsonElement value = fields(config).get(key);
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isNumber() ? value.getAsInt() : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }
}
