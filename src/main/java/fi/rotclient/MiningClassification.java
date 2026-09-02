package fi.rotclient;

import java.util.Locale;

/** Mining-only classification relative to the selected tracker target. */
enum MiningClassification {
    TARGET,
    OTHER;

    static MiningClassification fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (MiningClassification value : values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }
}
