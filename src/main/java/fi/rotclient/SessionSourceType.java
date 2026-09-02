package fi.rotclient;

import java.util.Locale;

/** How an item entered the Current Session ledger. */
enum SessionSourceType {
    MINING,
    CHEST,
    MOB,
    CURRENCY,
    UNATTRIBUTED;

    static SessionSourceType fromName(String name) {
        if (name == null || name.isBlank()) {
            return UNATTRIBUTED;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        for (SessionSourceType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return UNATTRIBUTED;
    }
}
