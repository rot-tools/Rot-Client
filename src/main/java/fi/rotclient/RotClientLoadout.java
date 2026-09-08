package fi.rotclient;

import java.util.UUID;

/**
 * One user-created SkyBlock loadout.
 *
 * A loadout has its own stable identity and may optionally be linked to a
 * Rot Client settings profile.
 *
 * The relationship intentionally points from:
 *
 *     loadout -> settings profile
 *
 * This allows multiple loadouts to share the same settings profile.
 */
final class RotClientLoadout {
    static final int MAX_NAME_LENGTH = 48;

    String id;
    String name;

    /**
     * Optional RotClientProfile.id.
     *
     * Empty means this loadout is not linked to a settings profile.
     */
    String settingsProfileId = "";

    /**
     * Global 1-based SkyBlock wardrobe set number.
     *
     * 0 means this loadout has no wardrobe set selected.
     *
     * This is deliberately stored as the logical set number instead of the
     * current container slot because Wardrobe pages reuse container slots 36-44.
     */
    int wardrobeSlotNumber;
    int equipmentSetNumber;
    /**
     * Required for Gson.
     */

    /**
     * UUID of the exact SkyBlock pet selected for this loadout.
     *
     * Empty means no pet selected.
     */
    String petUuid = "";

    /**
     * Last observed display name for the selected pet.
     *
     * This is only for UI display. petUuid is the actual identity.
     */
    String petName = "";
    RotClientLoadout() {
    }

    private RotClientLoadout(
            String id,
            String name) {

        this.id = id;
        this.name = name;

        normalize();
    }

    static RotClientLoadout create(String name) {
        return new RotClientLoadout(
                UUID.randomUUID().toString(),
                normalizeName(name));
    }

    RotClientLoadout duplicate(String newName) {
        RotClientLoadout duplicate =
                create(newName);

        duplicate.settingsProfileId =
                settingsProfileId == null
                        ? ""
                        : settingsProfileId;

        duplicate.wardrobeSlotNumber =
                wardrobeSlotNumber;

        duplicate.equipmentSetNumber =
                equipmentSetNumber;

        duplicate.petUuid =
                petUuid == null
                        ? ""
                        : petUuid;

        duplicate.petName =
                petName == null
                        ? ""
                        : petName;

        return duplicate;
    }

    void rename(String newName) {
        name = normalizeName(newName);
    }

    void normalize() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        if (wardrobeSlotNumber < 0) {
            wardrobeSlotNumber = 0;
        }

        if (equipmentSetNumber < 0) {
            equipmentSetNumber = 0;
        }

        name = normalizeName(name);

        if (settingsProfileId == null) {
            settingsProfileId = "";
        } else {
            settingsProfileId =
                    settingsProfileId.trim();
        }

        if (petUuid == null) {
            petUuid = "";
        }

        if (petName == null) {
            petName = "";
        }
    }

    boolean matchesId(String otherId) {
        return otherId != null
                && otherId.equals(id);
    }

    boolean hasLinkedSettingsProfile() {
        return settingsProfileId != null
                && !settingsProfileId.isBlank();
    }

    static boolean isValidName(String raw) {
        if (raw == null) {
            return false;
        }

        String trimmed =
                raw.trim();

        return !trimmed.isEmpty()
                && trimmed.length() <= MAX_NAME_LENGTH;
    }

    static String normalizeName(String raw) {
        if (raw == null) {
            return "Loadout";
        }

        String trimmed =
                raw.trim();

        if (trimmed.isEmpty()) {
            return "Loadout";
        }

        if (trimmed.length() > MAX_NAME_LENGTH) {
            return trimmed.substring(
                    0,
                    MAX_NAME_LENGTH);
        }

        return trimmed;
    }
}