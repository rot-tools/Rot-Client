package fi.rotclient;

/**
 * Coordinates activation of a Rot Client loadout.
 *
 * Activation order:
 *
 * 1. Select/persist the target loadout.
 * 2. Switch the optional linked Settings Profile.
 * 3. Equip the configured Wardrobe set.
 * 4. Equip the configured Pet.
 * 5. Equip the configured Equipment Set.
 *
 * Hotbar / inventory stages can be appended later.
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

        if (!target.hasLinkedSettingsProfile()) {
            if (startGearActivation(target)) {
                return true;
            }

            restorePrevious(
                    previousLoadoutId,
                    target.id);

            return false;
        }

        if (profiles.switchTo(
                target.settingsProfileId)) {

            if (startGearActivation(target)) {
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
        if (!hasPendingGearStages()) {
            return;
        }

        if (WardrobeAutoEquipRuntime.busy()) {
            gearHandoffTicks = 1;
            return;
        }

        if (RotClientPetAutoEquipRuntime.busy()) {
            gearHandoffTicks = 1;
            return;
        }

        if (RotClientEquipmentAutoEquipRuntime.busy()) {
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

            if (RotClientPetAutoEquipRuntime
                    .begin(petUuid)) {

                pendingPetUuid = "";
                gearHandoffTicks = 1;
            }

            return;
        }

        if (pendingEquipmentSetNumber > 0) {
            int equipmentSetNumber =
                    pendingEquipmentSetNumber;

            if (RotClientEquipmentAutoEquipRuntime
                    .begin(equipmentSetNumber)) {

                pendingEquipmentSetNumber = 0;
            }
        }
    }

    private boolean startGearActivation(
            RotClientLoadout target) {

        if (target == null) {
            return false;
        }

        clearPendingGear();

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
                    WardrobeAutoEquipRuntime
                            .beginLoadoutEquip(
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
                    RotClientPetAutoEquipRuntime
                            .begin(petUuid);

            if (!started) {
                clearPendingGear();
                return false;
            }

            gearHandoffTicks = 1;
            return true;
        }

        if (equipmentSetNumber > 0) {
            return RotClientEquipmentAutoEquipRuntime
                    .begin(equipmentSetNumber);
        }

        return true;
    }

    private boolean hasPendingGearStages() {
        return (pendingPetUuid != null
                && !pendingPetUuid.isBlank())
                || pendingEquipmentSetNumber > 0;
    }

    private boolean gearActivationBusy() {
        return hasPendingGearStages()
                || WardrobeAutoEquipRuntime.busy()
                || RotClientPetAutoEquipRuntime.busy()
                || RotClientEquipmentAutoEquipRuntime.busy();
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