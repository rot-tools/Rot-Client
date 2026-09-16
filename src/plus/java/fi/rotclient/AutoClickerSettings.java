package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Plus-owned Auto Clicker settings stored in the shared opaque edition slot.
 *
 * Legacy root-level autoClicker* values are preserved into extensionFields by
 * QolUnknownFieldPreserver before Gson deserializes the shared config/profile.
 */
record AutoClickerSettings(
        boolean enabled,
        boolean cpsHudEnabled,
        boolean whitelistOnly,
        boolean allowBreaking,
        boolean blockBreaker,
        boolean terminatorOnly,
        float cps,
        boolean enableLeft,
        boolean enableRight,
        float leftCps,
        float rightCps,
        String leftKeybind,
        String rightKeybind,
        List<String> leftWhitelist,
        List<String> rightWhitelist,
        float hudX,
        float hudY) {

    private static final String ENABLED = "autoClickerEnabled";
    private static final String CPS_HUD = "autoClickerCpsHudEnabled";
    private static final String WHITELIST_ONLY = "autoClickerWhitelistOnly";
    private static final String ALLOW_BREAKING = "autoClickerAllowBreaking";
    private static final String BLOCK_BREAKER = "autoClickerBlockBreaker";
    private static final String TERMINATOR_ONLY = "autoClickerTerminatorOnly";
    private static final String CPS = "autoClickerCps";
    private static final String ENABLE_LEFT = "autoClickerEnableLeft";
    private static final String ENABLE_RIGHT = "autoClickerEnableRight";
    private static final String LEFT_CPS = "autoClickerLeftCps";
    private static final String RIGHT_CPS = "autoClickerRightCps";
    private static final String LEFT_KEYBIND = "autoClickerLeftKeybind";
    private static final String RIGHT_KEYBIND = "autoClickerRightKeybind";
    private static final String LEFT_WHITELIST = "autoClickerLeftWhitelist";
    private static final String RIGHT_WHITELIST = "autoClickerRightWhitelist";
    private static final String HUD_X = "autoClickerHudX";
    private static final String HUD_Y = "autoClickerHudY";

    private static final float DEFAULT_CPS = 5.0F;
    private static final float DEFAULT_HUD_X = 12.0F;
    private static final float DEFAULT_HUD_Y = 382.0F;

    static AutoClickerSettings from(QolUtilityConfig config) {
        return new AutoClickerSettings(
                PlusOpaqueSettings.bool(config, ENABLED, false),
                PlusOpaqueSettings.bool(config, CPS_HUD, false),
                PlusOpaqueSettings.bool(config, WHITELIST_ONLY, false),
                PlusOpaqueSettings.bool(config, ALLOW_BREAKING, false),
                PlusOpaqueSettings.bool(config, BLOCK_BREAKER, true),
                PlusOpaqueSettings.bool(config, TERMINATOR_ONLY, true),
                clampCps(PlusOpaqueSettings.floating(config, CPS, DEFAULT_CPS)),
                PlusOpaqueSettings.bool(config, ENABLE_LEFT, true),
                PlusOpaqueSettings.bool(config, ENABLE_RIGHT, true),
                clampCps(PlusOpaqueSettings.floating(config, LEFT_CPS, DEFAULT_CPS)),
                clampCps(PlusOpaqueSettings.floating(config, RIGHT_CPS, DEFAULT_CPS)),
                text(config, LEFT_KEYBIND, ""),
                text(config, RIGHT_KEYBIND, ""),
                list(config, LEFT_WHITELIST),
                list(config, RIGHT_WHITELIST),
                clampPos(PlusOpaqueSettings.floating(config, HUD_X, DEFAULT_HUD_X), DEFAULT_HUD_X),
                clampPos(PlusOpaqueSettings.floating(config, HUD_Y, DEFAULT_HUD_Y), DEFAULT_HUD_Y));
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        PlusOpaqueSettings.write(config, ENABLED, value);
    }

    static Boolean readBoolean(QolUtilityConfig config, String settingId) {
        AutoClickerSettings settings = from(config);
        return switch (settingId == null ? "" : settingId) {
            case "qol.auto_clicker.whitelist_only" -> settings.whitelistOnly();
            case "qol.auto_clicker.cps_hud" -> settings.cpsHudEnabled();
            case "qol.auto_clicker.allow_breaking" -> settings.allowBreaking();
            case "qol.auto_clicker.block_breaker" -> settings.blockBreaker();
            case "qol.auto_clicker.terminator_only" -> settings.terminatorOnly();
            case "qol.auto_clicker.enable_left" -> settings.enableLeft();
            case "qol.auto_clicker.enable_right" -> settings.enableRight();
            default -> null;
        };
    }

    static boolean writeBoolean(QolUtilityConfig config, String settingId, boolean value) {
        String key = switch (settingId == null ? "" : settingId) {
            case "qol.auto_clicker.whitelist_only" -> WHITELIST_ONLY;
            case "qol.auto_clicker.cps_hud" -> CPS_HUD;
            case "qol.auto_clicker.allow_breaking" -> ALLOW_BREAKING;
            case "qol.auto_clicker.block_breaker" -> BLOCK_BREAKER;
            case "qol.auto_clicker.terminator_only" -> TERMINATOR_ONLY;
            case "qol.auto_clicker.enable_left" -> ENABLE_LEFT;
            case "qol.auto_clicker.enable_right" -> ENABLE_RIGHT;
            default -> null;
        };
        if (key == null) return false;
        PlusOpaqueSettings.write(config, key, value);
        return true;
    }

    static Double readNumber(QolUtilityConfig config, String settingId) {
        AutoClickerSettings settings = from(config);
        return switch (settingId == null ? "" : settingId) {
            case "qol.auto_clicker.cps" -> (double) settings.cps();
            case "qol.auto_clicker.left_cps" -> (double) settings.leftCps();
            case "qol.auto_clicker.right_cps" -> (double) settings.rightCps();
            default -> null;
        };
    }

    static boolean writeNumber(QolUtilityConfig config, String settingId, double value) {
        String key = switch (settingId == null ? "" : settingId) {
            case "qol.auto_clicker.cps" -> CPS;
            case "qol.auto_clicker.left_cps" -> LEFT_CPS;
            case "qol.auto_clicker.right_cps" -> RIGHT_CPS;
            default -> null;
        };
        if (key == null) return false;
        PlusOpaqueSettings.write(config, key, clampCps((float) value));
        return true;
    }

    static String readKeybind(QolUtilityConfig config, String settingId) {
        AutoClickerSettings settings = from(config);
        return switch (settingId == null ? "" : settingId) {
            case "qol.auto_clicker.left_keybind" -> settings.leftKeybind();
            case "qol.auto_clicker.right_keybind" -> settings.rightKeybind();
            default -> null;
        };
    }

    static boolean writeKeybind(QolUtilityConfig config, String settingId, String value) {
        String key = switch (settingId == null ? "" : settingId) {
            case "qol.auto_clicker.left_keybind" -> LEFT_KEYBIND;
            case "qol.auto_clicker.right_keybind" -> RIGHT_KEYBIND;
            default -> null;
        };
        if (key == null) return false;
        PlusOpaqueSettings.fields(config).addProperty(key, value == null ? "" : value.trim());
        return true;
    }

    static void leftWhitelist(QolUtilityConfig config, List<String> values) {
        writeList(config, LEFT_WHITELIST, values);
    }

    static void rightWhitelist(QolUtilityConfig config, List<String> values) {
        writeList(config, RIGHT_WHITELIST, values);
    }

    static float[] pose(QolUtilityConfig config) {
        AutoClickerSettings settings = from(config);
        return new float[] {settings.hudX(), settings.hudY(), 1.0F};
    }

    static void pose(QolUtilityConfig config, float x, float y) {
        PlusOpaqueSettings.write(config, HUD_X, clampPos(x, DEFAULT_HUD_X));
        PlusOpaqueSettings.write(config, HUD_Y, clampPos(y, DEFAULT_HUD_Y));
    }

    static void reset(QolUtilityConfig config) {
        PlusOpaqueSettings.reset(config,
                ENABLED, CPS_HUD, WHITELIST_ONLY, ALLOW_BREAKING,
                BLOCK_BREAKER, TERMINATOR_ONLY, CPS,
                ENABLE_LEFT, ENABLE_RIGHT, LEFT_CPS, RIGHT_CPS,
                LEFT_KEYBIND, RIGHT_KEYBIND,
                LEFT_WHITELIST, RIGHT_WHITELIST);
    }

    private static String text(QolUtilityConfig config, String key, String fallback) {
        JsonElement value = PlusOpaqueSettings.fields(config).get(key);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
            return fallback;
        }
        try {
            return value.getAsString();
        } catch (RuntimeException malformed) {
            return fallback;
        }
    }

    private static List<String> list(QolUtilityConfig config, String key) {
        JsonElement value = PlusOpaqueSettings.fields(config).get(key);
        if (value == null || !value.isJsonArray()) {
            return new ArrayList<>();
        }
        ArrayList<String> result = new ArrayList<>();
        for (JsonElement entry : value.getAsJsonArray()) {
            if (entry != null && entry.isJsonPrimitive()
                    && entry.getAsJsonPrimitive().isString()) {
                result.add(entry.getAsString());
            }
        }
        return result;
    }

    private static void writeList(
            QolUtilityConfig config, String key, List<String> values) {
        JsonArray array = new JsonArray();
        if (values != null) {
            for (String value : values) {
                if (value != null) array.add(value);
            }
        }
        PlusOpaqueSettings.fields(config).add(key, array);
    }

    private static float clampCps(float value) {
        return AutoClickerPolicy.clampCps(Float.isFinite(value) ? value : DEFAULT_CPS);
    }

    private static float clampPos(float value, float fallback) {
        if (!Float.isFinite(value)) return fallback;
        return Math.max(0.0F, value);
    }
}
