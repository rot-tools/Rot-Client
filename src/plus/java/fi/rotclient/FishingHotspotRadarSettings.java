package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-only radar flags stored with profile compatibility fields. */
record FishingHotspotRadarSettings(boolean enabled, boolean tracer) {
    static FishingHotspotRadarSettings from(QolUtilityConfig config) {
        JsonObject fields = fields(config);
        return new FishingHotspotRadarSettings(
                bool(fields.get("fishingHotspotsRadar"), false),
                bool(fields.get("fishingHotspotsTracer"), true));
    }

    static Boolean readBoolean(QolUtilityConfig config, String id) {
        return switch (id == null ? "" : id) {
            case "qol.fishing_hotspots.radar" -> from(config).enabled();
            case "qol.fishing_hotspots.tracer" -> from(config).tracer();
            default -> null;
        };
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        String key = switch (id == null ? "" : id) {
            case "qol.fishing_hotspots.radar" -> "fishingHotspotsRadar";
            case "qol.fishing_hotspots.tracer" -> "fishingHotspotsTracer";
            default -> null;
        };
        if (key == null) return false;
        fields(config).addProperty(key, value);
        return true;
    }

    static void reset(QolUtilityConfig config) {
        fields(config).remove("fishingHotspotsRadar");
        fields(config).remove("fishingHotspotsTracer");
    }

    private static JsonObject fields(QolUtilityConfig config) {
        QolSkyblockExtras extras = config.extras();
        if (extras.extensionFields == null) extras.extensionFields = new JsonObject();
        return extras.extensionFields;
    }

    private static boolean bool(JsonElement value, boolean fallback) {
        try { return value != null && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isBoolean() ? value.getAsBoolean() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }
}
