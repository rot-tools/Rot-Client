package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Typed access to Plus-owned values inside the shared opaque data slot. */
final class PlusOpaqueSettings {
    private PlusOpaqueSettings() {
    }

    static boolean bool(QolUtilityConfig config, String key, boolean fallback) {
        JsonElement value = fields(config).get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : fallback;
    }

    static int integer(QolUtilityConfig config, String key, int fallback) {
        JsonElement value = fields(config).get(key);
        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException malformed) {
            return fallback;
        }
    }

    static void write(QolUtilityConfig config, String key, boolean value) {
        fields(config).addProperty(key, value);
    }

    static void write(QolUtilityConfig config, String key, int value) {
        fields(config).addProperty(key, value);
    }

    static void reset(QolUtilityConfig config, String... keys) {
        JsonObject fields = fields(config);
        for (String key : keys) fields.remove(key);
    }

    private static JsonObject fields(QolUtilityConfig config) {
        if (config.extensionFields == null) {
            config.extensionFields = new JsonObject();
        }
        return config.extensionFields;
    }
}
