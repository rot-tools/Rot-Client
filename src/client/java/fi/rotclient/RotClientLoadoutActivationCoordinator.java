package fi.rotclient;

/**
 * Coordinates activation of a Rot Client loadout.
 *
 * Shared activation always:
 *
 * 1. Selects/persists the target loadout.
 * 2. Switches the optional linked Settings Profile.
 *
 * Automatic Wardrobe -> Pet -> Equipment execution is flavor-gated.
 * The regular/legal client leaves those gear changes manual.
 * Rot Client Plus explicitly enables the automated gear stages through
 * {@link QolClientFlavorHooks}.
 */
final class RotClientLoadoutActivationCoordinator {
    private final RotClientLoadoutManager loadouts;
    private final RotClientProfileController profiles;

    private String pendingPetUuid = "";
    private int pendingEquipmentSetNumber;
    private int gearHandoffTicks;

    RotClientLoadoutActivationCoordinator(
            RotClientLoadoutManager loadouts,
            RotClientProfileController profiles) {

        if (loadouts == null) {
            throw new IllegalArgumentException(
                    "Loadout manager cannot be null");
        }

        if (profiles == null) {
            throw new IllegalArgumentException(
                    "Profile controller cannot be null");
        }

        this.loadouts = loadouts;
        this.profiles = profiles;
    }

    boolean activate(String loadoutId) {
        RotClientLoadout target =
                loadouts.findById(loadoutId);

        if (target == null) {
            return false;
        }

        if (gearActivationBusy()) {
            return false;
        }

        RotClientLoadout previous =
                loadouts.activeLoadout();

        String previousLoadoutId =
                previous == null
                        ? null
                        : previous.id;

        if (!loadouts.activate(target.id)) {
            return false;
        }

        boolean automateGear =
                gearAutomationEnabled();

        if (!target.hasLinkedSettingsProfile()) {
            if (!automateGear
                    || startGearActivation(target)) {
                return true;
            }

            restorePrevious(
                    previousLoadoutId,
                    target.id);

            return false;
        }

        if (profiles.switchTo(
                target.settingsProfileId)) {

            if (!automateGear
                    || startGearActivation(target)) {
                return true;
            }

            restorePrevious(
                    previousLoadoutId,
                    target.id);

            return false;
        }

        restorePrevious(
                previousLoadoutId,
                target.id);

        return false;
    }

    void tick() {
        if (!gearAutomationEnabled()) {
            clearPendingGear();
            return;
        }

        if (!hasPendingGearStages()) {
            return;
        }

        QolClientFlavorHooks hooks =
                QolClientFlavorSupport.hooks();

        if (hooks.wardrobeAutoEquipBusy()) {
            gearHandoffTicks = 1;
            return;
        }

        if (hooks.loadoutPetAutoEquipBusy()) {
            gearHandoffTicks = 1;
            return;
        }

        if (hooks.loadoutEquipmentAutoEquipBusy()) {
            return;
        }

        if (gearHandoffTicks > 0) {
            gearHandoffTicks--;
            return;
        }

        if (pendingPetUuid != null
                && !pendingPetUuid.isBlank()) {

            String petUuid =
                    pendingPetUuid;

            if (hooks.beginLoadoutPetEquip(
                    petUuid)) {

                pendingPetUuid = "";
                gearHandoffTicks = 1;
            }

            return;
        }

        if (pendingEquipmentSetNumber > 0) {
            int equipmentSetNumber =
                    pendingEquipmentSetNumber;

            if (hooks.beginLoadoutEquipmentEquip(
                    equipmentSetNumber)) {

                pendingEquipmentSetNumber = 0;
            }
        }
    }

    private boolean startGearActivation(
            RotClientLoadout target) {

        if (target == null) {
            return false;
        }

        if (!gearAutomationEnabled()) {
            return true;
        }

        clearPendingGear();

        QolClientFlavorHooks hooks =
                QolClientFlavorSupport.hooks();

        String petUuid =
                target.petUuid == null
                        ? ""
                        : target.petUuid.trim();

        int equipmentSetNumber =
                Math.max(
                        0,
                        target.equipmentSetNumber);

        if (target.wardrobeSlotNumber > 0) {
            boolean started =
                    hooks.beginWardrobeLoadoutEquip(
                            target.wardrobeSlotNumber);

            if (!started) {
                return false;
            }

            if (!petUuid.isBlank()) {
                pendingPetUuid =
                        petUuid;
            }

            if (equipmentSetNumber > 0) {
                pendingEquipmentSetNumber =
                        equipmentSetNumber;
            }

            gearHandoffTicks = 1;
            return true;
        }

        if (!petUuid.isBlank()) {
            if (equipmentSetNumber > 0) {
                pendingEquipmentSetNumber =
                        equipmentSetNumber;
            }

            boolean started =
                    hooks.beginLoadoutPetEquip(
                            petUuid);

            if (!started) {
                clearPendingGear();
                return false;
            }

            gearHandoffTicks = 1;
            return true;
        }

        if (equipmentSetNumber > 0) {
            return hooks.beginLoadoutEquipmentEquip(
                    equipmentSetNumber);
        }

        return true;
    }

    private boolean hasPendingGearStages() {
        return (pendingPetUuid != null
                && !pendingPetUuid.isBlank())
                || pendingEquipmentSetNumber > 0;
    }

    private boolean gearActivationBusy() {
        if (!gearAutomationEnabled()) {
            return false;
        }

        QolClientFlavorHooks hooks =
                QolClientFlavorSupport.hooks();

        return hasPendingGearStages()
                || hooks.wardrobeAutoEquipBusy()
                || hooks.loadoutPetAutoEquipBusy()
                || hooks.loadoutEquipmentAutoEquipBusy();
    }

    private boolean gearAutomationEnabled() {
        return QolClientFlavorSupport
                .hooks()
                .loadoutsEnabled();
    }

    private void clearPendingGear() {
        pendingPetUuid = "";
        pendingEquipmentSetNumber = 0;
        gearHandoffTicks = 0;
    }

    private void restorePrevious(
            String previousLoadoutId,
            String targetLoadoutId) {

        if (previousLoadoutId == null
                || previousLoadoutId.equals(
                targetLoadoutId)) {

            return;
        }

        loadouts.activate(
                previousLoadoutId);
    }
}