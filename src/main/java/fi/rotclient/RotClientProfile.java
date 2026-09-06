package fi.rotclient;

import java.util.UUID;

/**
 * One user-created Rot Client settings profile.
 *
 * The id is the stable internal identity and must not change when the user
 * renames the profile. The visible name is completely user-controlled.
 * Settings live on {@link RotClientProfileSettings}.
 */
final class RotClientProfile {
    static final int MAX_NAME_LENGTH = 48;

    String id;
    String name;

    RotClientProfileSettings settings =
            RotClientProfileSettings.defaults();
    /**
     * Required for Gson.
     */
    RotClientProfile() {
    }

    private RotClientProfile(String id, String name) {
        this.id = id;
        this.name = name;
        normalize();
    }

    static RotClientProfile create(String name) {
        return new RotClientProfile(
                UUID.randomUUID().toString(),
                normalizeName(name));
    }

    RotClientProfile duplicate(String newName) {
        RotClientProfile duplicate =
                create(newName);

        duplicate.settings =
                settings == null
                        ? RotClientProfileSettings.defaults()
                        : settings.copy();

        return duplicate;
    }

    void rename(String newName) {
        name = normalizeName(newName);
    }

    void normalize() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        name = normalizeName(name);

        if (settings == null) {
            settings = RotClientProfileSettings.defaults();
        } else {
            settings.normalize();
        }
    }

    boolean matchesId(String otherId) {
        return otherId != null && otherId.equals(id);
    }

    static boolean isValidName(String raw) {
        if (raw == null) {
            return false;
        }

        String trimmed = raw.trim();
        return !trimmed.isEmpty()
                && trimmed.length() <= MAX_NAME_LENGTH;
    }

    static String normalizeName(String raw) {
        if (raw == null) {
            return "Profile";
        }

        String trimmed = raw.trim();

        if (trimmed.isEmpty()) {
            return "Profile";
        }

        if (trimmed.length() > MAX_NAME_LENGTH) {
            return trimmed.substring(0, MAX_NAME_LENGTH);
        }

        return trimmed;
    }
}