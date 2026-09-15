package fi.rotclient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

/** Plus-owned named-mob ESP configuration. */
record MobHighlightSettings(boolean enabled, boolean requireKey, String addKey,
                            boolean depth, boolean tracers, int color, List<String> names) {
    static MobHighlightSettings from(QolUtilityConfig config) {
        JsonElement key = PlusOpaqueSettings.fields(config).get("mobHighlightAddKey");
        String addKey = key != null && key.isJsonPrimitive()
                && key.getAsJsonPrimitive().isString() ? key.getAsString() : "";
        List<String> names = new ArrayList<>();
        JsonElement oldNames = PlusOpaqueSettings.fields(config).get("mobHighlightNames");
        if (oldNames != null && oldNames.isJsonArray()) {
            for (JsonElement name : oldNames.getAsJsonArray()) {
                if (name != null && name.isJsonPrimitive()
                        && name.getAsJsonPrimitive().isString()) names.add(name.getAsString());
            }
        }
        return new MobHighlightSettings(
                PlusOpaqueSettings.bool(config, "mobHighlightEnabled", false),
                PlusOpaqueSettings.bool(config, "mobHighlightRequireKey", false),
                addKey,
                PlusOpaqueSettings.bool(config, "mobHighlightDepth", true),
                PlusOpaqueSettings.bool(config, "mobHighlightTracers", true),
                PlusOpaqueSettings.integer(config, "mobHighlightColor", 0xFFFF55FF),
                List.copyOf(names));
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        PlusOpaqueSettings.write(config, "mobHighlightEnabled", value);
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        String key = switch (id) {
            case "qol.mob_highlight.highlight_key" -> "mobHighlightRequireKey";
            case "qol.mob_highlight.depth" -> "mobHighlightDepth";
            case "qol.mob_highlight.tracers" -> "mobHighlightTracers";
            default -> null;
        };
        if (key == null) return false;
        PlusOpaqueSettings.write(config, key, value);
        return true;
    }

    static void addKey(QolUtilityConfig config, String value) {
        PlusOpaqueSettings.fields(config).addProperty("mobHighlightAddKey",
                value == null ? "" : value.trim());
    }

    static void color(QolUtilityConfig config, int value) {
        PlusOpaqueSettings.write(config, "mobHighlightColor", value);
    }

    static void names(QolUtilityConfig config, List<String> names) {
        JsonArray array = new JsonArray();
        for (String name : names) if (name != null) array.add(name);
        PlusOpaqueSettings.fields(config).add("mobHighlightNames", array);
    }

    static void reset(QolUtilityConfig config) {
        PlusOpaqueSettings.reset(config, "mobHighlightEnabled", "mobHighlightRequireKey",
                "mobHighlightAddKey", "mobHighlightDepth", "mobHighlightTracers",
                "mobHighlightColor", "mobHighlightNames");
    }
}
