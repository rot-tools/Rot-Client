package fi.rotclient;

/**
 * Coordinates activation of a Rot Client loadout.
 *
 * Activation order:
 *
 * 1. Select/persist the target loadout.
 * 2. Switch the optional linked Settings Profile.
 * 3. Equip the configured Wardrobe set.
 * 4. After Wardrobe finishes, equip the configured Pet.
 *
 * Equipment / hotbar / inventory stages can be appended later.
 */
final class RotClientLoadoutActivationCoordinator {
    private final RotClientLoadoutManager loadouts;
    private final RotClientProfileController profiles;

    /*
     * Pet activation is queued while the asynchronous Wardrobe runtime
     * finishes.
     */
    private String pendingPetUuid = "";

    /*
     * Small handoff delay between closing Wardrobe and opening Pets.
     */
    private int petHandoffTicks;

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

    /**
     * Begins activation of an existing loadout.
     *
     * Gear stages continue asynchronously on subsequent client ticks.
     */
    boolean activate(String loadoutId) {
        RotClientLoadout target =
                loadouts.findById(loadoutId);

        if (target == null) {
            return false;
        }

        /*
         * Do not overlap two gear activation sequences.
         */
        if (gearActivationBusy()) {
            return false;
        }

        RotClientLoadout previous =
                loadouts.activeLoadout();

        String previousLoadoutId =
                previous == null
                        ? null
                        : previous.id;

        /*
         * Persist the target first.
         */
        if (!loadouts.activate(target.id)) {
            return false;
        }

        /*
         * If there is no Settings Profile, continue directly to gear.
         */
        if (!target.hasLinkedSettingsProfile()) {
            if (startGearActivation(target)) {
                return true;
            }

            restorePrevious(
                    previousLoadoutId,
                    target.id);

            return false;
        }

        /*
         * Switch linked Rot Client settings before touching gear.
         */
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

    /**
     * Advances asynchronous loadout activation.
     *
     * In particular, Pets must not start until the hidden Wardrobe operation
     * has completely finished.
     */
    void tick() {
        if (pendingPetUuid == null
                || pendingPetUuid.isBlank()) {

            return;
        }

        /*
         * Wardrobe includes its delayed hidden-container close in busy().
         */
        if (WardrobeAutoEquipRuntime.busy()) {
            petHandoffTicks = 1;
            return;
        }

        /*
         * Never start another Pets operation on top of an existing one.
         */
        if (RotClientPetAutoEquipRuntime.busy()) {
            return;
        }

        /*
         * Give Minecraft/Hypixel one client tick between the two menus.
         */
        if (petHandoffTicks > 0) {
            petHandoffTicks--;
            return;
        }

        String petUuid =
                pendingPetUuid;

        pendingPetUuid = "";
        petHandoffTicks = 0;

        RotClientPetAutoEquipRuntime
                .begin(petUuid);
    }

    /**
     * Starts the first required gear stage.
     *
     * No configured value means "leave the player's current gear unchanged".
     */
    private boolean startGearActivation(
            RotClientLoadout target) {

        if (target == null) {
            return false;
        }

        clearPendingPet();

        String petUuid =
                target.petUuid == null
                        ? ""
                        : target.petUuid.trim();

        /*
         * Wardrobe runs first.
         */
        if (target.wardrobeSlotNumber > 0) {
            boolean started =
                    WardrobeAutoEquipRuntime
                            .beginLoadoutEquip(
                                    target.wardrobeSlotNumber);

            if (!started) {
                return false;
            }

            /*
             * Queue the Pet stage. tick() starts it only after Wardrobe is
             * completely finished.
             */
            if (!petUuid.isBlank()) {
                pendingPetUuid =
                        petUuid;

                petHandoffTicks = 1;
            }

            return true;
        }

        /*
         * No Wardrobe configured. Start Pet immediately when configured.
         */
        if (!petUuid.isBlank()) {
            return RotClientPetAutoEquipRuntime
                    .begin(petUuid);
        }

        /*
         * Neither Wardrobe nor Pet is configured.
         */
        return true;
    }

    private boolean gearActivationBusy() {
        return (pendingPetUuid != null
                && !pendingPetUuid.isBlank())
                || WardrobeAutoEquipRuntime.busy()
                || RotClientPetAutoEquipRuntime.busy();
    }

    private void clearPendingPet() {
        pendingPetUuid = "";
        petHandoffTicks = 0;
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