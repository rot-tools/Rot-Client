package fi.rotclient;

import java.util.List;

/**
 * High-level profile operations used by the Rot Client UI.
 *
 * The manager owns profile metadata/persistence. This controller coordinates
 * those operations with the live TrackerConfig so activeProfileId and the
 * settings currently applied in-game cannot intentionally drift apart.
 */
final class RotClientProfileController {
    private final RotClientProfileManager profiles;
    private final TrackerConfig liveConfig;
    private final RotClientProfileSwitchCoordinator switchCoordinator;

    RotClientProfileController(
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
        this.switchCoordinator =
                new RotClientProfileSwitchCoordinator(
                        profiles,
                        liveConfig);
    }

    List<RotClientProfile> profiles() {
        return profiles.profiles();
    }

    RotClientProfile activeProfile() {
        return profiles.activeProfile();
    }

    boolean hasProfiles() {
        return profiles.hasProfiles();
    }

    /**
     * Creates a profile from the settings currently active in-game.
     *
     * If another profile is active, save its latest live state first.
     */
    RotClientProfile createFromCurrent(String name) {
        if (!captureOutgoingProfile()) {
            return null;
        }

        RotClientProfile created =
                profiles.createFromCurrent(
                        name,
                        liveConfig);

        if (created == null) {
            return null;
        }

        /*
         * The new profile was captured from liveConfig, so no apply is
         * necessary. Persist the regular config for consistency.
         */
        TrackerStore.save(liveConfig);

        return created;
    }

    /**
     * Creates a profile using Rot Client defaults and immediately applies it.
     */
    RotClientProfile createDefault(String name) {
        RotClientProfile previousActive =
                profiles.activeProfile();

        String previousActiveId =
                previousActive == null
                        ? null
                        : previousActive.id;

        RotClientProfileSettings previousLive =
                RotClientProfileSettingsAdapter.capture(
                        liveConfig);

        if (!captureOutgoingProfile()) {
            return null;
        }

        RotClientProfile created =
                profiles.createDefault(name);

        if (created == null) {
            return null;
        }

        try {
            RotClientProfileSettingsAdapter.apply(
                    created.settings,
                    liveConfig);

            RotClientClient.reconcileSettingsProfileRuntime(
                    previousLive,
                    created.settings);

            TrackerStore.save(liveConfig);

            return created;
        } catch (RuntimeException exception) {
            rollbackCreatedProfile(
                    created,
                    previousActiveId,
                    previousLive);

            return null;
        }
    }

    /**
     * Duplicates a profile and activates the duplicate.
     *
     * The currently-active profile is captured first. This is particularly
     * important when the profile being duplicated is itself active, because
     * its stored snapshot may otherwise be older than the settings currently
     * visible in-game.
     */
    RotClientProfile duplicate(
            String sourceProfileId,
            String newName) {

        RotClientProfile previousActive =
                profiles.activeProfile();

        String previousActiveId =
                previousActive == null
                        ? null
                        : previousActive.id;

        RotClientProfileSettings previousLive =
                RotClientProfileSettingsAdapter.capture(
                        liveConfig);

        if (!captureOutgoingProfile()) {
            return null;
        }

        RotClientProfile duplicate =
                profiles.duplicate(
                        sourceProfileId,
                        newName);

        if (duplicate == null) {
            return null;
        }

        try {
            RotClientProfileSettingsAdapter.apply(
                    duplicate.settings,
                    liveConfig);

            RotClientClient.reconcileSettingsProfileRuntime(
                    previousLive,
                    duplicate.settings);

            TrackerStore.save(liveConfig);

            return duplicate;
        } catch (RuntimeException exception) {
            rollbackCreatedProfile(
                    duplicate,
                    previousActiveId,
                    previousLive);

            return null;
        }
    }

    boolean rename(
            String profileId,
            String newName) {

        return profiles.rename(
                profileId,
                newName);
    }

    boolean switchTo(String profileId) {
        return switchCoordinator.switchTo(profileId);
    }

    /**
     * Deletes a profile.
     *
     * Deleting an inactive profile does not alter live settings.
     *
     * Deleting the final profile leaves the current live settings in place and
     * simply returns Rot Client to its ordinary non-profile configuration.
     *
     * When deleting the active profile while other profiles remain, an
     * explicit replacement must be supplied and that replacement is applied
     * live as part of this operation.
     */
    boolean delete(
            String profileId,
            String replacementProfileId) {

        RotClientProfile target =
                profiles.findById(profileId);

        if (target == null) {
            return false;
        }

        RotClientProfile active =
                profiles.activeProfile();

        boolean deletingActive =
                active != null
                        && active.matchesId(target.id);

        if (!deletingActive) {
            return profiles.delete(
                    target.id,
                    null);
        }

        /*
         * The last profile can simply disappear. Keep its current settings
         * live as the ordinary non-profile configuration.
         */
        if (profiles.profiles().size() == 1) {
            boolean deleted =
                    profiles.delete(
                            target.id,
                            null);

            if (deleted) {
                TrackerStore.save(liveConfig);
            }

            return deleted;
        }

        RotClientProfile replacement =
                profiles.findById(
                        replacementProfileId);

        if (replacement == null
                || replacement.matchesId(target.id)) {
            return false;
        }

        RotClientProfileSettings previousLive =
                RotClientProfileSettingsAdapter.capture(
                        liveConfig);

        RotClientProfileSettings replacementSettings =
                replacement.settings == null
                        ? RotClientProfileSettings.defaults()
                        : replacement.settings.copy();

        /*
         * Apply first, then commit the metadata deletion. If profile
         * persistence fails, restoring the previous live settings keeps the
         * active profile and TrackerConfig aligned.
         */
        try {
            RotClientProfileSettingsAdapter.apply(
                    replacementSettings,
                    liveConfig);
        } catch (RuntimeException exception) {
            return false;
        }

        boolean deleted =
                profiles.delete(
                        target.id,
                        replacement.id);

        if (!deleted) {
            RotClientProfileSettingsAdapter.apply(
                    previousLive,
                    liveConfig);

            return false;
        }

        RotClientClient.reconcileSettingsProfileRuntime(
                previousLive,
                replacementSettings);

        TrackerStore.save(liveConfig);

        return true;
    }

    private boolean captureOutgoingProfile() {
        RotClientProfile active =
                profiles.activeProfile();

        if (active == null) {
            return true;
        }

        return profiles.captureCurrentSettings(
                active.id,
                liveConfig);
    }

    private void rollbackCreatedProfile(
            RotClientProfile created,
            String previousActiveId,
            RotClientProfileSettings previousLive) {

        if (created != null) {
            profiles.delete(
                    created.id,
                    previousActiveId);
        }

        /*
         * Capture the currently applied failed/new profile state before restoring
         * the previous settings. Runtime-sensitive modules such as Mining Tracker
         * need both the outgoing and target state for correct reconciliation.
         */
        RotClientProfileSettings failedLive =
                RotClientProfileSettingsAdapter.capture(
                        liveConfig);

        RotClientProfileSettingsAdapter.apply(
                previousLive,
                liveConfig);

        RotClientClient.reconcileSettingsProfileRuntime(
                failedLive,
                previousLive);

        TrackerStore.save(liveConfig);
    }
}