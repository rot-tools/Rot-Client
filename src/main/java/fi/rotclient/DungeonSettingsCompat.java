package fi.rotclient;

import java.util.List;

/** Saved dungeon setting values retained for old profiles in both editions. */
final class DungeonSettingsCompat {
    static final List<String> DRAGON_SOLO_CLASSES = List.of("Tank", "Healer");

    private DungeonSettingsCompat() {
    }

    static int clampTermProtectMs(int value) {
        return Math.max(100, Math.min(700, value));
    }

    static int clampRelicLookMs(int value) {
        return Math.max(10, Math.min(300, value));
    }

    static int clampRelicSpawnTicks(int value) {
        return Math.max(1, Math.min(1200, value));
    }

    static int clampI4RotationMs(int value) {
        return Math.max(0, Math.min(250, value));
    }

    static int clampTriggerDelay(int value) {
        return Math.max(0, Math.min(1000, value));
    }

    static int clampOpacity(int value) {
        return Math.max(0, Math.min(100, value));
    }

    static int clampScoreThreshold(int value) {
        return Math.max(100, Math.min(305, value));
    }

    static String normalizeCloseChestMode(String value) {
        return value != null && value.trim().equalsIgnoreCase("Any Key") ? "Any Key" : "Auto";
    }

    static String normalizeSoloClass(String value) {
        if (value != null) {
            for (String option : DRAGON_SOLO_CLASSES) {
                if (option.equalsIgnoreCase(value.trim())) {
                    return option;
                }
            }
        }
        return "Tank";
    }
}
