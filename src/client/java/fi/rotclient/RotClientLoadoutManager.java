package fi.rotclient;

import java.util.Collections;
import java.util.List;

/**
 * Runtime owner of user-created Rot Client loadouts.
 *
 * This class handles loadout metadata and persistence.
 *
 * It does not equip armor, pets, equipment or inventory items. Actual loadout
 * execution will be handled by a separate runtime/controller layer.
 */
final class RotClientLoadoutManager {
    private RotClientLoadoutConfig config =
            RotClientLoadoutConfig.defaults();

    void loadFromDisk() {
        config =
                RotClientLoadoutStore.load();

        if (config == null) {
            config =
                    RotClientLoadoutConfig.defaults();
        }

        config.normalize();
    }

    List<RotClientLoadout> loadouts() {
        config.normalize();

        return Collections.unmodifiableList(
                config.loadouts);
    }

    RotClientLoadout activeLoadout() {
        config.normalize();

        return config.activeLoadout();
    }

    boolean hasLoadouts() {
        return !loadouts().isEmpty();
    }

    RotClientLoadout findById(
            String loadoutId) {

        config.normalize();

        return config.findById(
                loadoutId);
    }

    RotClientLoadout create(String name) {
        if (!RotClientLoadout.isValidName(name)) {
            return null;
        }

        String normalizedName =
                RotClientLoadout.normalizeName(name);

        if (containsName(
                normalizedName,
                null)) {

            return null;
        }

        RotClientLoadout loadout =
                RotClientLoadout.create(
                        normalizedName);

        String previousActiveId =
                config.activeLoadoutId;

        config.loadouts.add(loadout);

        config.activeLoadoutId =
                loadout.id;

        if (!saveNow()) {
            config.loadouts.remove(loadout);

            config.activeLoadoutId =
                    previousActiveId;

            return null;
        }

        return loadout;
    }

    RotClientLoadout duplicate(
            String loadoutId,
            String newName) {

        RotClientLoadout source =
                config.findById(loadoutId);

        if (source == null
                || !RotClientLoadout.isValidName(newName)) {

            return null;
        }

        String normalizedName =
                RotClientLoadout.normalizeName(newName);

        if (containsName(
                normalizedName,
                null)) {

            return null;
        }

        RotClientLoadout copy =
                source.duplicate(
                        normalizedName);

        String previousActiveId =
                config.activeLoadoutId;

        config.loadouts.add(copy);

        config.activeLoadoutId =
                copy.id;

        if (!saveNow()) {
            config.loadouts.remove(copy);

            config.activeLoadoutId =
                    previousActiveId;

            return null;
        }

        return copy;
    }

    boolean rename(
            String loadoutId,
            String newName) {

        RotClientLoadout loadout =
                config.findById(loadoutId);

        if (loadout == null
                || !RotClientLoadout.isValidName(newName)) {

            return false;
        }

        String normalizedName =
                RotClientLoadout.normalizeName(newName);

        if (containsName(
                normalizedName,
                loadout.id)) {

            return false;
        }

        if (loadout.name.equals(
                normalizedName)) {

            return true;
        }

        String previousName =
                loadout.name;

        loadout.rename(
                normalizedName);

        if (!saveNow()) {
            loadout.name =
                    previousName;

            return false;
        }

        return true;
    }

    /**
     * Marks a loadout as the currently selected loadout.
     *
     * At this stage this only changes metadata. It does not yet equip anything.
     */
    boolean activate(String loadoutId) {
        RotClientLoadout loadout =
                config.findById(loadoutId);

        if (loadout == null) {
            return false;
        }

        if (loadout.id.equals(
                config.activeLoadoutId)) {

            return true;
        }

        String previousActiveId =
                config.activeLoadoutId;

        config.activeLoadoutId =
                loadout.id;

        if (!saveNow()) {
            config.activeLoadoutId =
                    previousActiveId;

            return false;
        }

        return true;
    }

    boolean delete(String loadoutId) {
        RotClientLoadout loadout =
                config.findById(loadoutId);

        if (loadout == null) {
            return false;
        }

        int originalIndex =
                config.loadouts.indexOf(loadout);

        String previousActiveId =
                config.activeLoadoutId;

        boolean wasActive =
                loadout.id.equals(
                        config.activeLoadoutId);

        config.loadouts.remove(loadout);

        if (config.loadouts.isEmpty()) {
            config.activeLoadoutId = "";

        } else if (wasActive) {
            /*
             * Loadouts do not currently affect live player state, so selecting
             * the nearest remaining loadout as metadata is safe.
             */
            int replacementIndex =
                    Math.min(
                            originalIndex,
                            config.loadouts.size() - 1);

            config.activeLoadoutId =
                    config.loadouts
                            .get(replacementIndex)
                            .id;
        }

        if (!saveNow()) {
            int restoreIndex =
                    Math.max(
                            0,
                            Math.min(
                                    originalIndex,
                                    config.loadouts.size()));

            config.loadouts.add(
                    restoreIndex,
                    loadout);

            config.activeLoadoutId =
                    previousActiveId;

            return false;
        }

        return true;
    }

    /**
     * Links a loadout to a settings profile.
     *
     * Multiple loadouts may reference the same settings profile.
     *
     * Passing null or an empty string unlinks the loadout.
     */

    boolean setPet(
            String loadoutId,
            String petUuid,
            String petName) {

        RotClientLoadout loadout =
                findById(loadoutId);

        if (loadout == null) {
            return false;
        }

        String safeUuid =
                petUuid == null
                        ? ""
                        : petUuid.trim();

        String safeName =
                petName == null
                        ? ""
                        : petName.trim();

        String previousUuid =
                loadout.petUuid;

        String previousName =
                loadout.petName;

        loadout.petUuid =
                safeUuid;

        loadout.petName =
                safeName;

        if (saveNow()) {
            return true;
        }

        loadout.petUuid =
                previousUuid;

        loadout.petName =
                previousName;

        return false;
    }


    boolean setWardrobeSlot(
            String loadoutId,
            int wardrobeSlotNumber) {

        RotClientLoadout loadout =
                findById(loadoutId);

        if (loadout == null
                || wardrobeSlotNumber < 0) {
            return false;
        }

        int previous =
                loadout.wardrobeSlotNumber;

        loadout.wardrobeSlotNumber =
                wardrobeSlotNumber;

        if (saveNow()) {
            return true;
        }

        /*
         * Persistence failed. Keep the in-memory state aligned with disk.
         */
        loadout.wardrobeSlotNumber =
                previous;

        return false;
    }

    boolean linkSettingsProfile(
            String loadoutId,
            String settingsProfileId,
            RotClientProfileManager profiles) {

        RotClientLoadout loadout =
                config.findById(loadoutId);

        if (loadout == null
                || profiles == null) {

            return false;
        }

        String normalizedProfileId =
                settingsProfileId == null
                        ? ""
                        : settingsProfileId.trim();

        /*
         * Empty means explicitly unlink the settings profile.
         */
        if (!normalizedProfileId.isEmpty()
                && profiles.findById(
                normalizedProfileId) == null) {

            return false;
        }

        if (normalizedProfileId.equals(
                loadout.settingsProfileId)) {

            return true;
        }

        String previous =
                loadout.settingsProfileId;

        loadout.settingsProfileId =
                normalizedProfileId;

        if (!saveNow()) {
            loadout.settingsProfileId =
                    previous;

            return false;
        }

        return true;
    }

    boolean saveNow() {
        config.normalize();

        return RotClientLoadoutStore.save(
                config);
    }

    private boolean containsName(
            String name,
            String excludedLoadoutId) {

        if (name == null) {
            return false;
        }

        for (RotClientLoadout loadout :
                config.loadouts) {

            if (loadout == null
                    || loadout.name == null) {

                continue;
            }

            if (excludedLoadoutId != null
                    && loadout.matchesId(
                    excludedLoadoutId)) {

                continue;
            }

            if (loadout.name.equalsIgnoreCase(
                    name)) {

                return true;
            }
        }

        return false;
    }
}