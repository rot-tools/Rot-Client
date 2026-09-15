package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned sea-creature attack control, including old profile values. */
record FishingAutoAttackSettings(boolean enabled, int delayTicks) {
    static FishingAutoAttackSettings from(QolUtilityConfig config) {
        JsonObject fields = fields(config);
        return new FishingAutoAttackSettings(bool(fields.get("fishingCreaturesAutoAttack")),
                delay(fields.get("fishingCreaturesAutoDelay")));
    }

    static Boolean readBoolean(QolUtilityConfig config, String id) {
        return "qol.fishing_creatures.auto_attack".equals(id) ? from(config).enabled() : null;
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        if (!"qol.fishing_creatures.auto_attack".equals(id)) return false;
        fields(config).addProperty("fishingCreaturesAutoAttack", value);
        return true;
    }

    static Double readNumber(QolUtilityConfig config, String id) {
        return "qol.fishing_creatures.auto_delay".equals(id)
                ? (double) from(config).delayTicks() : null;
    }

    static boolean writeNumber(QolUtilityConfig config, String id, double value) {
        if (!"qol.fishing_creatures.auto_delay".equals(id) || !Double.isFinite(value)) return false;
        fields(config).addProperty("fishingCreaturesAutoDelay",
                FishingAutomationPolicy.clampAutoDelay((int) Math.round(value)));
        return true;
    }

    static void reset(QolUtilityConfig config) {
        fields(config).remove("fishingCreaturesAutoAttack");
        fields(config).remove("fishingCreaturesAutoDelay");
    }

    private static JsonObject fields(QolUtilityConfig config) {
        QolSkyblockExtras extras = config.extras();
        if (extras.extensionFields == null) extras.extensionFields = new JsonObject();
        return extras.extensionFields;
    }

    private static boolean bool(JsonElement value) {
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean(); }
        catch (RuntimeException ignored) { return false; }
    }

    private static int delay(JsonElement value) {
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isNumber()
                ? FishingAutomationPolicy.clampAutoDelay(value.getAsInt()) : 4; }
        catch (RuntimeException ignored) { return 4; }
    }
}
