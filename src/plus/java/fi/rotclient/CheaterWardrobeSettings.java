package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned typed access to legacy wardrobe automation values. */
record CheaterWardrobeSettings(
        boolean enabled,
        boolean stationaryOnly,
        boolean resetOpen,
        int clickDelay,
        int closeDelay,
        int delayVariance,
        String[] slotBinds) {

    private static final String ENABLED = "cheaterWardrobeEnabled";
    private static final String MOVE_EQUIP = "cheaterWardrobeMoveEquip";
    private static final String STATIONARY = "cheaterWardrobeStationaryOnly";
    private static final String RESET_OPEN = "cheaterWardrobeResetOpen";
    private static final String CLICK_DELAY = "cheaterWardrobeClickDelay";
    private static final String CLOSE_DELAY = "cheaterWardrobeCloseDelay";
    private static final String VARIANCE = "cheaterWardrobeDelayVariance";
    private static final String SLOT_PREFIX = "cheaterWardrobeSlot";

    static CheaterWardrobeSettings from(QolUtilityConfig config) {
        JsonObject fields = fields(config);
        String[] slots = new String[9];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = text(fields, SLOT_PREFIX + (i + 1), "");
        }
        return new CheaterWardrobeSettings(
                bool(fields, ENABLED, false),
                bool(fields, STATIONARY, true),
                bool(fields, RESET_OPEN, true),
                bounded(fields, CLICK_DELAY, 1, 0, 8),
                bounded(fields, CLOSE_DELAY, 1, 0, 8),
                bounded(fields, VARIANCE, 1, 0, 5),
                slots);
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        fields(config).addProperty(ENABLED, value);
    }

    static Boolean readBoolean(QolUtilityConfig config, String settingId) {
        CheaterWardrobeSettings settings = from(config);
        return switch (settingId == null ? "" : settingId) {
            case "qol.cheater_wardrobe.stationary_only" -> settings.stationaryOnly();
            case "qol.cheater_wardrobe.reset_open" -> settings.resetOpen();
            default -> null;
        };
    }

    static boolean writeBoolean(QolUtilityConfig config, String settingId, boolean value) {
        String key = switch (settingId == null ? "" : settingId) {
            case "qol.cheater_wardrobe.stationary_only" -> STATIONARY;
            case "qol.cheater_wardrobe.reset_open" -> RESET_OPEN;
            default -> null;
        };
        if (key == null) return false;
        fields(config).addProperty(key, value);
        return true;
    }

    static Double readNumber(QolUtilityConfig config, String settingId) {
        CheaterWardrobeSettings settings = from(config);
        return switch (settingId == null ? "" : settingId) {
            case "qol.cheater_wardrobe.click_delay" -> (double) settings.clickDelay();
            case "qol.cheater_wardrobe.close_delay" -> (double) settings.closeDelay();
            case "qol.cheater_wardrobe.delay_variance" -> (double) settings.delayVariance();
            default -> null;
        };
    }

    static boolean writeNumber(QolUtilityConfig config, String settingId, double value) {
        String key;
        int maximum;
        switch (settingId == null ? "" : settingId) {
            case "qol.cheater_wardrobe.click_delay" -> { key = CLICK_DELAY; maximum = 8; }
            case "qol.cheater_wardrobe.close_delay" -> { key = CLOSE_DELAY; maximum = 8; }
            case "qol.cheater_wardrobe.delay_variance" -> { key = VARIANCE; maximum = 5; }
            default -> { return false; }
        }
        int rounded = Double.isFinite(value) ? (int) Math.round(value) : 1;
        fields(config).addProperty(key, Math.max(0, Math.min(maximum, rounded)));
        return true;
    }

    static String readKeybind(QolUtilityConfig config, String settingId) {
        int slot = slot(settingId);
        return slot < 0 ? null : from(config).slotBinds()[slot];
    }

    static boolean writeKeybind(QolUtilityConfig config, String settingId, String value) {
        int slot = slot(settingId);
        if (slot < 0) return false;
        fields(config).addProperty(SLOT_PREFIX + (slot + 1), value == null ? "" : value.trim());
        return true;
    }

    static void reset(QolUtilityConfig config) {
        JsonObject fields = fields(config);
        for (String key : new String[] {
                ENABLED, MOVE_EQUIP, STATIONARY, RESET_OPEN,
                CLICK_DELAY, CLOSE_DELAY, VARIANCE}) {
            fields.remove(key);
        }
        for (int i = 1; i <= 9; i++) fields.remove(SLOT_PREFIX + i);
    }

    private static JsonObject fields(QolUtilityConfig config) {
        QolSkyblockExtras extras = config.extras();
        if (extras.extensionFields == null) extras.extensionFields = new JsonObject();
        return extras.extensionFields;
    }

    private static boolean bool(JsonObject fields, String key, boolean fallback) {
        JsonElement value = fields.get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback;
    }

    private static int bounded(JsonObject fields, String key, int fallback, int min, int max) {
        JsonElement value = fields.get(key);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) return fallback;
        try {
            return Math.max(min, Math.min(max, value.getAsInt()));
        } catch (RuntimeException malformed) {
            return fallback;
        }
    }

    private static String text(JsonObject fields, String key, String fallback) {
        JsonElement value = fields.get(key);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) return fallback;
        return value.getAsString();
    }

    private static int slot(String settingId) {
        String prefix = "qol.cheater_wardrobe.slot_";
        if (settingId == null || !settingId.startsWith(prefix)) return -1;
        try {
            int index = Integer.parseInt(settingId.substring(prefix.length())) - 1;
            return index >= 0 && index < 9 ? index : -1;
        } catch (NumberFormatException malformed) {
            return -1;
        }
    }
}
