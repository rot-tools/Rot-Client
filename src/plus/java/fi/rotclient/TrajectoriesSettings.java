package fi.rotclient;

/** Plus-owned trajectory rendering settings with legacy JSON keys. */
record TrajectoriesSettings(boolean enabled, boolean bows, boolean pearls,
                            boolean lines, boolean boxes, boolean depth,
                            boolean plane, boolean entities, int range,
                            float width, float boxSize, float planeSize, int color) {
    static TrajectoriesSettings from(QolUtilityConfig config) {
        return new TrajectoriesSettings(
                PlusOpaqueSettings.bool(config, "trajectoriesEnabled", false),
                PlusOpaqueSettings.bool(config, "trajectoriesBows", true),
                PlusOpaqueSettings.bool(config, "trajectoriesPearls", true),
                PlusOpaqueSettings.bool(config, "trajectoriesLines", true),
                PlusOpaqueSettings.bool(config, "trajectoriesBoxes", true),
                PlusOpaqueSettings.bool(config, "trajectoriesDepth", true),
                PlusOpaqueSettings.bool(config, "trajectoriesPlane", false),
                PlusOpaqueSettings.bool(config, "trajectoriesEntities", true),
                TrajectoryPredictor.clampRange(PlusOpaqueSettings.integer(config,
                        "trajectoriesRange", TrajectoryPredictor.DEFAULT_RANGE)),
                clamp(PlusOpaqueSettings.floating(config, "trajectoriesWidth", 1.0F), 0.1F, 5.0F),
                clamp(PlusOpaqueSettings.floating(config, "trajectoriesBoxSize", 0.5F), 0.5F, 3.0F),
                clamp(PlusOpaqueSettings.floating(config, "trajectoriesPlaneSize", 2.0F), 0.5F, 8.0F),
                PlusOpaqueSettings.integer(config, "trajectoriesColor", 0xFF00AAAA));
    }

    static void enabled(QolUtilityConfig config, boolean value) {
        PlusOpaqueSettings.write(config, "trajectoriesEnabled", value);
    }

    static boolean writeBoolean(QolUtilityConfig config, String id, boolean value) {
        String key = switch (id) {
            case "qol.trajectories.bows" -> "trajectoriesBows";
            case "qol.trajectories.pearls" -> "trajectoriesPearls";
            case "qol.trajectories.lines" -> "trajectoriesLines";
            case "qol.trajectories.boxes" -> "trajectoriesBoxes";
            case "qol.trajectories.depth" -> "trajectoriesDepth";
            case "qol.trajectories.plane" -> "trajectoriesPlane";
            case "qol.trajectories.entities" -> "trajectoriesEntities";
            default -> null;
        };
        if (key == null) return false;
        PlusOpaqueSettings.write(config, key, value);
        return true;
    }

    static boolean writeNumber(QolUtilityConfig config, String id, double value) {
        switch (id) {
            case "qol.trajectories.range" -> PlusOpaqueSettings.write(config,
                    "trajectoriesRange", TrajectoryPredictor.clampRange((int) Math.round(value)));
            case "qol.trajectories.width" -> PlusOpaqueSettings.write(config,
                    "trajectoriesWidth", clamp((float) value, 0.1F, 5.0F));
            case "qol.trajectories.box_size" -> PlusOpaqueSettings.write(config,
                    "trajectoriesBoxSize", clamp((float) value, 0.5F, 3.0F));
            case "qol.trajectories.plane_size" -> PlusOpaqueSettings.write(config,
                    "trajectoriesPlaneSize", clamp((float) value, 0.5F, 8.0F));
            default -> { return false; }
        }
        return true;
    }

    static void color(QolUtilityConfig config, int value) {
        PlusOpaqueSettings.write(config, "trajectoriesColor", value);
    }

    static void reset(QolUtilityConfig config) {
        PlusOpaqueSettings.reset(config, "trajectoriesEnabled", "trajectoriesBows",
                "trajectoriesPearls", "trajectoriesLines", "trajectoriesBoxes",
                "trajectoriesDepth", "trajectoriesPlane", "trajectoriesEntities",
                "trajectoriesRange", "trajectoriesWidth", "trajectoriesBoxSize",
                "trajectoriesPlaneSize", "trajectoriesColor");
    }

    private static float clamp(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
