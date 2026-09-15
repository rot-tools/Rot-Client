package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned Secret Hitboxes settings stored in opaque edition fields. */
record SecretHitboxesSettings(
        boolean enabled,
        boolean onlyDungeons,
        boolean lever,
        boolean oldLever,
        boolean button,
        boolean flatButton,
        boolean skull,
        boolean chests,
        boolean onlyTrappedChests) {

    static SecretHitboxesSettings from(QolUtilityConfig config) {
        return new SecretHitboxesSettings(
                read(config, "secretHitboxesEnabled", false),
                read(config, "secretHitboxesOnlyDungeons", true),
                read(config, "secretHitboxesLever", true),
                read(config, "secretHitboxesOldLever", true),
                read(config, "secretHitboxesButton", true),
                read(config, "secretHitboxesFlatButton", false),
                read(config, "secretHitboxesSkull", true),
                read(config, "secretHitboxesChests", false),
                read(config, "secretHitboxesOnlyTrappedChests", false));
    }

    static boolean read(QolUtilityConfig config, String key, boolean fallback) {
        JsonObject fields = fields(config);
        JsonElement value = fields.get(key);
        return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean()
                ? value.getAsBoolean() : fallback;
    }

    static void write(QolUtilityConfig config, String key, boolean value) {
        fields(config).addProperty(key, value);
    }

    static void reset(QolUtilityConfig config) {
        JsonObject fields = fields(config);
        fields.remove("secretHitboxesEnabled");
        fields.remove("secretHitboxesOnlyDungeons");
        fields.remove("secretHitboxesLever");
        fields.remove("secretHitboxesOldLever");
        fields.remove("secretHitboxesButton");
        fields.remove("secretHitboxesFlatButton");
        fields.remove("secretHitboxesSkull");
        fields.remove("secretHitboxesChests");
        fields.remove("secretHitboxesOnlyTrappedChests");
    }

    private static JsonObject fields(QolUtilityConfig config) {
        if (config.extensionFields == null) {
            config.extensionFields = new JsonObject();
        }
        return config.extensionFields;
    }
}
