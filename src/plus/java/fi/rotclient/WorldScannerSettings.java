package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Plus-owned scanner switches and target ESP settings, retaining legacy JSON keys. */
record WorldScannerSettings(QolUtilityConfig config, boolean enabled, boolean onlyHollows,
                            boolean crystals, boolean mobSpots, boolean fairyGrottos,
                            boolean dragonNest, boolean wormFishing, boolean lavaEsp,
                            boolean waterEsp, boolean ratHitboxes, boolean chatCoords,
                            int espRange) {
    private static final Gson GSON = new Gson();

    static WorldScannerSettings from(QolUtilityConfig config) {
        return new WorldScannerSettings(config,
                PlusOpaqueSettings.bool(config, "worldScannerEnabled", false),
                PlusOpaqueSettings.bool(config, "worldScannerOnlyHollows", true),
                PlusOpaqueSettings.bool(config, "worldScannerCrystals", true),
                PlusOpaqueSettings.bool(config, "worldScannerMobSpots", true),
                PlusOpaqueSettings.bool(config, "worldScannerFairyGrottos", true),
                PlusOpaqueSettings.bool(config, "worldScannerDragonNest", false),
                PlusOpaqueSettings.bool(config, "worldScannerWormFishing", false),
                PlusOpaqueSettings.bool(config, "worldScannerLavaEsp", false),
                PlusOpaqueSettings.bool(config, "worldScannerWaterEsp", false),
                PlusOpaqueSettings.bool(config, "worldScannerRatHitboxes", true),
                PlusOpaqueSettings.bool(config, "worldScannerChatCoords", false),
                WorldScannerPolicy.clampEspRange(PlusOpaqueSettings.integer(config,
                        "worldScannerEspRange", WorldScannerPolicy.DEFAULT_ESP_RANGE)));
    }

    WorldScannerEspSettings.Target target(String id) {
        JsonObject old = rawTarget(config, id, false);
        JsonObject merged = GSON.toJsonTree(new WorldScannerEspSettings.Target(
                WorldScannerEspSettings.defaultColor(id))).getAsJsonObject();
        if (old != null) for (var field : old.entrySet()) {
            merged.add(field.getKey(), field.getValue().deepCopy());
        }
        try {
            WorldScannerEspSettings.Target target = GSON.fromJson(merged,
                    WorldScannerEspSettings.Target.class);
            return target == null ? new WorldScannerEspSettings.Target(
                    WorldScannerEspSettings.defaultColor(id)) : target;
        } catch (RuntimeException malformed) {
            return new WorldScannerEspSettings.Target(WorldScannerEspSettings.defaultColor(id));
        }
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        PlusOpaqueSettings.write(config, "worldScannerEnabled", value);
    }

    static Boolean readBoolean(QolUtilityConfig config, String id) {
        WorldScannerSettings s = from(config);
        Boolean scalar = switch (id) {
            case "qol.world_scanner.only_hollows" -> s.onlyHollows();
            case "qol.world_scanner.crystals" -> s.crystals();
            case "qol.world_scanner.mob_spots" -> s.mobSpots();
            case "qol.world_scanner.fairy" -> s.fairyGrottos();
            case "qol.world_scanner.dragon" -> s.dragonNest();
            case "qol.world_scanner.worm" -> s.wormFishing();
            case "qol.world_scanner.lava_esp" -> s.lavaEsp();
            case "qol.world_scanner.water_esp" -> s.waterEsp();
            case "qol.world_scanner.rat_hitboxes" -> s.ratHitboxes();
            case "qol.world_scanner.chat" -> s.chatCoords();
            default -> null;
        };
        if (scalar != null) return scalar;
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        if (parts == null) return null;
        WorldScannerEspSettings.Target t = s.target(parts[0]);
        return switch (parts[1]) {
            case "enabled" -> t.enabled;
            case "tracer" -> t.tracer;
            case "name" -> t.displayName;
            case "chat" -> t.chatCoords;
            case "notify" -> t.notify;
            default -> null;
        };
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        String key = switch (id) {
            case "qol.world_scanner.only_hollows" -> "worldScannerOnlyHollows";
            case "qol.world_scanner.crystals" -> "worldScannerCrystals";
            case "qol.world_scanner.mob_spots" -> "worldScannerMobSpots";
            case "qol.world_scanner.fairy" -> "worldScannerFairyGrottos";
            case "qol.world_scanner.dragon" -> "worldScannerDragonNest";
            case "qol.world_scanner.worm" -> "worldScannerWormFishing";
            case "qol.world_scanner.lava_esp" -> "worldScannerLavaEsp";
            case "qol.world_scanner.water_esp" -> "worldScannerWaterEsp";
            case "qol.world_scanner.rat_hitboxes" -> "worldScannerRatHitboxes";
            case "qol.world_scanner.chat" -> "worldScannerChatCoords";
            default -> null;
        };
        if (key != null) {
            PlusOpaqueSettings.write(config, key, value);
            return true;
        }
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        if (parts == null) return false;
        String field = switch (parts[1]) {
            case "enabled" -> "enabled";
            case "tracer" -> "tracer";
            case "name" -> "displayName";
            case "chat" -> "chatCoords";
            case "notify" -> "notify";
            default -> null;
        };
        if (field == null) return false;
        rawTarget(config, parts[0], true).addProperty(field, value);
        if ("enabled".equals(field)) {
            String master = switch (parts[0]) {
                case "fairy" -> "worldScannerFairyGrottos";
                case "dragon" -> "worldScannerDragonNest";
                case "worm" -> "worldScannerWormFishing";
                default -> null;
            };
            if (master != null) PlusOpaqueSettings.write(config, master, value);
        }
        return true;
    }

    static Double readNumber(QolUtilityConfig config, String id) {
        if ("qol.world_scanner.esp_range".equals(id)) return (double) from(config).espRange();
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        if (parts == null) return null;
        WorldScannerEspSettings.Target t = from(config).target(parts[0]);
        return switch (parts[1]) {
            case "name_scale" -> (double) t.nameScale;
            case "opacity" -> (double) t.backgroundOpacity;
            default -> null;
        };
    }

    static boolean writeNumber(QolUtilityConfig config, String id, double value) {
        if ("qol.world_scanner.esp_range".equals(id)) {
            PlusOpaqueSettings.write(config, "worldScannerEspRange",
                    WorldScannerPolicy.clampEspRange((int) Math.round(value)));
            return true;
        }
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        if (parts == null) return false;
        String field = switch (parts[1]) {
            case "name_scale" -> "nameScale";
            case "opacity" -> "backgroundOpacity";
            default -> null;
        };
        if (field == null) return false;
        float clamped = "nameScale".equals(field)
                ? clamp(value, 0.5D, 2.0D) : clamp(value, 0.0D, 1.0D);
        rawTarget(config, parts[0], true).addProperty(field, clamped);
        return true;
    }

    static String readEnum(QolUtilityConfig config, String id) {
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        return parts != null && "style".equals(parts[1])
                ? WorldScannerEspSettings.normalizeStyle(from(config).target(parts[0]).highlightStyle)
                : null;
    }

    static boolean writeEnum(QolUtilityConfig config, String id, String value) {
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        if (parts == null || !"style".equals(parts[1])) return false;
        rawTarget(config, parts[0], true).addProperty("highlightStyle",
                WorldScannerEspSettings.normalizeStyle(value));
        return true;
    }

    static Integer readColor(QolUtilityConfig config, String id) {
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        return parts != null && "color".equals(parts[1])
                ? from(config).target(parts[0]).colorArgb : null;
    }

    static boolean writeColor(QolUtilityConfig config, String id, int value) {
        String[] parts = WorldScannerEspSettings.splitSetting(id);
        if (parts == null || !"color".equals(parts[1])) return false;
        rawTarget(config, parts[0], true).addProperty("colorArgb", value);
        return true;
    }

    static void reset(QolUtilityConfig config) {
        PlusOpaqueSettings.reset(config, "worldScannerEnabled", "worldScannerOnlyHollows",
                "worldScannerCrystals", "worldScannerMobSpots", "worldScannerFairyGrottos",
                "worldScannerDragonNest", "worldScannerWormFishing", "worldScannerLavaEsp",
                "worldScannerWaterEsp", "worldScannerRatHitboxes", "worldScannerChatCoords",
                "worldScannerEspRange", "worldScannerTargets");
    }

    private static JsonObject rawTarget(QolUtilityConfig config, String id, boolean create) {
        JsonObject fields = PlusOpaqueSettings.fields(config);
        JsonElement all = fields.get("worldScannerTargets");
        JsonObject targets = all != null && all.isJsonObject() ? all.getAsJsonObject() : null;
        if (targets == null) {
            if (!create) return null;
            targets = new JsonObject();
            fields.add("worldScannerTargets", targets);
        }
        JsonElement old = targets.get(id);
        JsonObject target = old != null && old.isJsonObject() ? old.getAsJsonObject() : null;
        if (target == null && create) {
            target = new JsonObject();
            targets.add(id, target);
        }
        return target;
    }

    private static float clamp(double value, double min, double max) {
        return (float) (Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min);
    }
}
