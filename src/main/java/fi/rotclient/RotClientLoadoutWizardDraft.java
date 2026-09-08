package fi.rotclient;

/**
 * Temporary state while creating a new Loadout.
 *
 * Nothing in this object is persisted until the user finishes the wizard.
 */

final class RotClientLoadoutWizardDraft {
    String name = "";

    /**
     * Global 1-based SkyBlock wardrobe set number.
     * 0 means no wardrobe selection.
     */
    int wardrobeSlotNumber;
    /**
     * 1-based SkyBlock Equipment Set number.
     * 0 means no equipment-set selection.
     */
    int equipmentSetNumber;

    String petUuid = "";

    /**
     * Display name shown in the wizard/review.
     */
    String petName = "";

    /**
     * Empty means no linked Rot Client settings profile.
     */
    String settingsProfileId = "";

    void reset() {
        name = "";
        wardrobeSlotNumber = 0;
        settingsProfileId = "";
        petUuid = "";
        petName = "";
        equipmentSetNumber = 0;
    }
}