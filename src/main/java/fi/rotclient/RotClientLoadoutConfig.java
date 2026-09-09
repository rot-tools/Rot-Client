package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Root persisted configuration for user-created loadouts.
 *
 * An empty loadout list is valid. Rot Client does not create predefined
 * loadouts automatically.
 */
final class RotClientLoadoutConfig {
    static final int SCHEMA_VERSION = 1;

    int schemaVersion = SCHEMA_VERSION;

    String activeLoadoutId = "";

    List<RotClientLoadout> loadouts =
            new ArrayList<>();

    static RotClientLoadoutConfig defaults() {
        return new RotClientLoadoutConfig();
    }

    void normalize() {
        if (schemaVersion <= SCHEMA_VERSION) {
            schemaVersion = SCHEMA_VERSION;
        }

        if (loadouts == null) {
            loadouts =
                    new ArrayList<>();
        }

        List<RotClientLoadout> normalized =
                new ArrayList<>();

        Set<String> usedIds =
                new HashSet<>();

        for (RotClientLoadout loadout : loadouts) {
            if (loadout == null) {
                continue;
            }

            loadout.normalize();

            /*
             * Repair duplicate IDs in damaged or manually edited configs.
             */
            while (!usedIds.add(loadout.id)) {
                loadout.id =
                        UUID.randomUUID().toString();
            }

            normalized.add(loadout);
        }

        loadouts = normalized;

        if (activeLoadoutId == null) {
            activeLoadoutId = "";
        } else {
            activeLoadoutId =
                    activeLoadoutId.trim();
        }

        /*
         * If the saved active ID no longer exists, fall back to the first
         * existing loadout.
         */
        if (!loadouts.isEmpty()
                && !activeLoadoutId.isBlank()
                && findById(activeLoadoutId) == null) {

            activeLoadoutId =
                    loadouts.get(0).id;
        }

        if (loadouts.isEmpty()) {
            activeLoadoutId = "";
        }
    }

    RotClientLoadout activeLoadout() {
        if (activeLoadoutId == null
                || activeLoadoutId.isBlank()) {

            return null;
        }

        return findById(
                activeLoadoutId);
    }

    RotClientLoadout findById(
            String loadoutId) {

        if (loadoutId == null
                || loadoutId.isBlank()) {

            return null;
        }

        for (RotClientLoadout loadout : loadouts) {
            if (loadout != null
                    && loadout.matchesId(loadoutId)) {

                return loadout;
            }
        }

        return null;
    }

    boolean containsName(String name) {
        if (name == null) {
            return false;
        }

        String wanted =
                name.trim();

        for (RotClientLoadout loadout : loadouts) {
            if (loadout != null
                    && loadout.name != null
                    && loadout.name.equalsIgnoreCase(wanted)) {

                return true;
            }
        }

        return false;
    }
}