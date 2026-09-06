package fi.rotclient;

/**
 * Coordinates a live settings-profile switch.
 *
 * Switching performs these operations in order:
 *
 * 1. Capture the outgoing profile's current live settings.
 * 2. Persist those settings.
 * 3. Select/persist the target profile.
 * 4. Apply the target profile to the live TrackerConfig.
 * 5. Persist the updated TrackerConfig.
 *
 * Persistent tracker statistics and session/domain data are not touched by
 * RotClientProfileSettingsAdapter.
 */
final class RotClientProfileSwitchCoordinator {
    private final RotClientProfileManager profiles;
    private final TrackerConfig liveConfig;

    RotClientProfileSwitchCoordinator(
            RotClientProfileManager profiles,
            TrackerConfig liveConfig) {

        if (profiles == null) {
            throw new IllegalArgumentException(
                    "Profile manager cannot be null");
        }

        if (liveConfig == null) {
            throw new IllegalArgumentException(
                    "Tracker config cannot be null");
        }

        this.profiles = profiles;
        this.liveConfig = liveConfig;
    }

    /**
     * Switches to an existing profile.
     *
     * Returns false when the target does not exist or when profile persistence
     * fails before the target can safely be activated.
     */
    boolean switchTo(String targetProfileId) {
        RotClientProfile target =
                profiles.findById(targetProfileId);

        if (target == null) {
            return false;
        }

        RotClientProfile active =
                profiles.activeProfile();

        if (active != null
                && active.matchesId(target.id)) {
            return true;
        }

        /*
         * Keep a live snapshot so an unexpected application failure can
         * restore the settings that were active before the switch.
         */
        RotClientProfileSettings previousLiveSettings =
                RotClientProfileSettingsAdapter.capture(
                        liveConfig);

        String previousProfileId =
                active == null
                        ? null
                        : active.id;

        /*
         * The outgoing profile owns all changes made while it was active.
         * Persist them before changing activeProfileId.
         */
        if (active != null
                && !profiles.captureCurrentSettings(
                active.id,
                liveConfig)) {
            return false;
        }

        if (!profiles.activate(target.id)) {
            return false;
        }

        try {
            RotClientProfile activated =
                    profiles.findById(target.id);

            if (activated == null) {
                rollback(
                        previousProfileId,
                        previousLiveSettings);

                return false;
            }

            RotClientProfileSettingsAdapter.apply(
                    activated.settings,
                    liveConfig);

            RotClientClient.reconcileSettingsProfileRuntime(
                    previousLiveSettings,
                    activated.settings);

            TrackerStore.save(liveConfig);

            return true;

        } catch (RuntimeException exception) {
            rollback(
                    previousProfileId,
                    previousLiveSettings);

            return false;
        }
    }

    private void rollback(
            String previousProfileId,
            RotClientProfileSettings previousLiveSettings) {

        if (previousProfileId != null) {
            profiles.activate(previousProfileId);
        }

        RotClientProfileSettings failedLiveSettings =
                RotClientProfileSettingsAdapter.capture(
                        liveConfig);

        RotClientProfileSettingsAdapter.apply(
                previousLiveSettings,
                liveConfig);

        RotClientClient.reconcileSettingsProfileRuntime(
                failedLiveSettings,
                previousLiveSettings);

        TrackerStore.save(liveConfig);
    }
}