package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned legacy Experimentation Table solver settings. */
record ExperimentSolverSettings(boolean enabled, boolean chronomatron,
                                boolean ultrasequencer, boolean superpairs,
                                boolean blockWrongClicks, boolean hideTooltip,
                                boolean hideWrongChrono, boolean hideWrongUltra,
                                boolean privateIslandOnly, int firstColor,
                                int secondColor, int matchedColor,
                                int matchColor, int powerupColor) {
    static ExperimentSolverSettings from(QolUtilityConfig config) {
        return new ExperimentSolverSettings(
                bool(config, "experimentSolverEnabled", false),
                bool(config, "experimentChronomatron", true),
                bool(config, "experimentUltrasequencer", true),
                bool(config, "experimentSuperpairs", true),
                bool(config, "experimentBlockWrongClicks", true),
                bool(config, "experimentHideTooltip", true),
                bool(config, "experimentHideWrongChrono", false),
                bool(config, "experimentHideWrongUltra", false),
                bool(config, "experimentPrivateIslandOnly", true),
                integer(config, "experimentFirstColor", 0x8022C55E),
                integer(config, "experimentSecondColor", 0x80FACC15),
                integer(config, "experimentMatchedColor", 0x8022C55E),
                integer(config, "experimentMatchColor", 0x80FACC15),
                integer(config, "experimentPowerupColor", 0x80FF00FF));
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        fields(config).addProperty("experimentSolverEnabled", value);
    }

    static Boolean readBoolean(QolUtilityConfig config, String id) {
        String key = booleanKey(id);
        return key == null ? null : switch (key) {
            case "experimentChronomatron", "experimentUltrasequencer",
                 "experimentSuperpairs", "experimentHideTooltip",
                 "experimentPrivateIslandOnly" -> bool(config, key, true);
            case "experimentHideWrongChrono", "experimentHideWrongUltra",
                 "experimentBlockWrongClicks" -> bool(config, key,
                         key.equals("experimentBlockWrongClicks"));
            default -> null;
        };
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        String key = booleanKey(id);
        if (key == null) return false;
        fields(config).addProperty(key, value);
        return true;
    }

    static Integer readColor(QolUtilityConfig config, String id) {
        String key = colorKey(id);
        if (key == null) return null;
        return switch (key) {
            case "experimentFirstColor", "experimentMatchedColor" ->
                    integer(config, key, 0x8022C55E);
            case "experimentSecondColor", "experimentMatchColor" ->
                    integer(config, key, 0x80FACC15);
            case "experimentPowerupColor" -> integer(config, key, 0x80FF00FF);
            default -> null;
        };
    }

    static boolean writeColor(QolUtilityConfig config, String id, int value) {
        String key = colorKey(id);
        if (key == null) return false;
        fields(config).addProperty(key, value);
        return true;
    }

    static void reset(QolUtilityConfig config) {
        for (String key : new String[]{"experimentSolverEnabled", "experimentChronomatron",
                "experimentUltrasequencer", "experimentSuperpairs",
                "experimentBlockWrongClicks", "experimentHideTooltip",
                "experimentHideWrongChrono", "experimentHideWrongUltra",
                "experimentPrivateIslandOnly", "experimentFirstColor",
                "experimentSecondColor", "experimentMatchedColor",
                "experimentMatchColor", "experimentPowerupColor"}) fields(config).remove(key);
    }

    private static String booleanKey(String id) {
        return switch (id == null ? "" : id) {
            case "qol.experiment_solver.chronomatron" -> "experimentChronomatron";
            case "qol.experiment_solver.ultrasequencer" -> "experimentUltrasequencer";
            case "qol.experiment_solver.superpairs" -> "experimentSuperpairs";
            case "qol.experiment_solver.block_wrong_clicks" -> "experimentBlockWrongClicks";
            case "qol.experiment_solver.hide_tooltip" -> "experimentHideTooltip";
            case "qol.experiment_solver.hide_wrong_chronomatron" -> "experimentHideWrongChrono";
            case "qol.experiment_solver.hide_wrong_ultrasequencer" -> "experimentHideWrongUltra";
            case "qol.experiment_solver.private_island_only" -> "experimentPrivateIslandOnly";
            default -> null;
        };
    }

    private static String colorKey(String id) {
        return switch (id == null ? "" : id) {
            case "qol.experiment_solver.first_color" -> "experimentFirstColor";
            case "qol.experiment_solver.second_color" -> "experimentSecondColor";
            case "qol.experiment_solver.matched_color" -> "experimentMatchedColor";
            case "qol.experiment_solver.match_color" -> "experimentMatchColor";
            case "qol.experiment_solver.powerup_color" -> "experimentPowerupColor";
            default -> null;
        };
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
