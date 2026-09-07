package fi.rotclient;

/**
 * Coordinates activation of a Rot Client loadout.
 *
 * For now a loadout activation consists of:
 *
 * 1. Select/persist the target loadout.
 * 2. If it has a linked settings profile, switch that profile live.
 *
 * Gear switching will be added later and must only start after the linked
 * settings profile has switched successfully.
 */
final class RotClientLoadoutActivationCoordinator {
    private final RotClientLoadoutManager loadouts;
    private final RotClientProfileController profiles;

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
     * Activates an existing loadout.
     *
     * A loadout without a linked settings profile simply becomes active.
     *
     * A loadout with a linked settings profile only remains active when that
     * profile can also be switched successfully.
     */
    boolean activate(String loadoutId) {
        RotClientLoadout target =
                loadouts.findById(loadoutId);

        if (target == null) {
            return false;
        }

        RotClientLoadout previous =
                loadouts.activeLoadout();

        String previousLoadoutId =
                previous == null
                        ? null
                        : previous.id;

        /*
         * Persist the target loadout first.
         *
         * This lets us safely restore the previous active loadout if the
         * settings-profile switch fails.
         */
        if (!loadouts.activate(target.id)) {
            return false;
        }

        /*
         * No linked settings profile: metadata-only activation is complete.
         */
        if (!target.hasLinkedSettingsProfile()) {
            return true;
        }

        /*
         * RotClientProfileController.switchTo() owns the live settings switch,
         * including persistence and runtime reconciliation.
         */
        if (profiles.switchTo(
                target.settingsProfileId)) {

            return true;
        }

        /*
         * The linked profile was missing or failed to activate.
         * Restore the previous loadout selection when possible.
         */
        if (previousLoadoutId != null
                && !previousLoadoutId.equals(
                target.id)) {

            loadouts.activate(
                    previousLoadoutId);
        }

        return false;
    }
}