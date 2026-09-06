package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Root persisted configuration for user-created Rot Client profiles.
 *
 * This class only owns profile metadata and which profile is active.
 * Actual profile-specific settings will be added later.
 *
 * An empty profile list is valid. Rot Client does not create predefined
 * profiles for the user.
 */
final class RotClientProfileConfig {
    static final int SCHEMA_VERSION = 1;

    int schemaVersion = SCHEMA_VERSION;
    String activeProfileId = "";
    List<RotClientProfile> profiles = new ArrayList<>();

    static RotClientProfileConfig defaults() {
        return new RotClientProfileConfig();
    }

    void normalize() {
        if (schemaVersion <= SCHEMA_VERSION) {
            schemaVersion = SCHEMA_VERSION;
        }

        if (profiles == null) {
            profiles = new ArrayList<>();
        }

        List<RotClientProfile> normalizedProfiles = new ArrayList<>();
        Set<String> usedIds = new HashSet<>();

        for (RotClientProfile profile : profiles) {
            if (profile == null) {
                continue;
            }

            profile.normalize();

            /*
             * IDs should always be unique. If a damaged/manually edited
             * config contains the same ID twice, keep both profiles but
             * assign the duplicate a fresh internal identity.
             */
            while (!usedIds.add(profile.id)) {
                profile.id = UUID.randomUUID().toString();
            }

            normalizedProfiles.add(profile);
        }

        profiles = normalizedProfiles;

        if (activeProfileId == null) {
            activeProfileId = "";
        } else {
            activeProfileId = activeProfileId.trim();
        }

        /*
         * If profiles exist but the stored active ID no longer points to one,
         * fall back to the first existing user-created profile.
         */
        if (!profiles.isEmpty() && findById(activeProfileId) == null) {
            activeProfileId = profiles.get(0).id;
        }

        if (profiles.isEmpty()) {
            activeProfileId = "";
        }
    }

    RotClientProfile activeProfile() {
        if (activeProfileId == null || activeProfileId.isBlank()) {
            return null;
        }

        return findById(activeProfileId);
    }

    RotClientProfile findById(String profileId) {
        if (profileId == null || profileId.isBlank()) {
            return null;
        }

        for (RotClientProfile profile : profiles) {
            if (profile != null && profile.matchesId(profileId)) {
                return profile;
            }
        }

        return null;
    }

    boolean containsName(String name) {
        if (name == null) {
            return false;
        }

        String wanted = name.trim();

        for (RotClientProfile profile : profiles) {
            if (profile != null
                    && profile.name != null
                    && profile.name.equalsIgnoreCase(wanted)) {
                return true;
            }
        }

        return false;
    }
}