package fi.rotclient;

import java.util.Collections;
import java.util.List;

/**
 * Runtime owner of user-created Rot Client settings profiles.
 *
 * Handles profile lifecycle and persistence. Applying a selected profile to
 * the live client is handled separately so profile metadata management stays
 * predictable and testable.
 */
final class RotClientProfileManager {
    private RotClientProfileConfig config =
            RotClientProfileConfig.defaults();

    void loadFromDisk() {
        config = RotClientProfileStore.load();

        if (config == null) {
            config = RotClientProfileConfig.defaults();
        }

        config.normalize();
    }

    List<RotClientProfile> profiles() {
        config.normalize();
        return Collections.unmodifiableList(config.profiles);
    }

    RotClientProfile activeProfile() {
        config.normalize();
        return config.activeProfile();
    }

    boolean hasProfiles() {
        return !profiles().isEmpty();
    }

    /**
     * Creates a new profile using Rot Client's default settings.
     */
    RotClientProfile createDefault(String name) {
        return createWithSettings(
                name,
                RotClientProfileSettings.defaults());
    }

    /**
     * Creates a new profile by capturing the current live settings.
     */
    RotClientProfile createFromCurrent(
            String name,
            TrackerConfig currentConfig) {

        RotClientProfileSettings settings =
                RotClientProfileSettingsAdapter.capture(
                        currentConfig);

        return createWithSettings(name, settings);
    }

    /**
     * Duplicates an existing profile, including all profile-specific settings.
     */
    RotClientProfile duplicate(
            String profileId,
            String newName) {

        RotClientProfile source =
                config.findById(profileId);

        if (source == null
                || !RotClientProfile.isValidName(newName)) {
            return null;
        }

        String normalizedName =
                RotClientProfile.normalizeName(newName);

        if (containsName(normalizedName, null)) {
            return null;
        }

        RotClientProfile copy =
                source.duplicate(normalizedName);

        String previousActiveProfileId =
                config.activeProfileId;

        config.profiles.add(copy);
        config.activeProfileId = copy.id;

        if (!saveNow()) {
            config.profiles.remove(copy);
            config.activeProfileId =
                    previousActiveProfileId;

            return null;
        }

        return copy;
    }

    boolean rename(
            String profileId,
            String newName) {

        RotClientProfile profile =
                config.findById(profileId);

        if (profile == null
                || !RotClientProfile.isValidName(newName)) {
            return false;
        }

        String normalizedName =
                RotClientProfile.normalizeName(newName);

        if (containsName(normalizedName, profile.id)) {
            return false;
        }

        if (profile.name.equals(normalizedName)) {
            return true;
        }

        String previousName =
                profile.name;

        profile.rename(normalizedName);

        if (!saveNow()) {
            profile.name = previousName;
            return false;
        }

        return true;
    }

    /**
     * Changes which profile is selected.
     *
     * This only changes profile metadata. The live-settings switching layer
     * will apply the profile settings separately.
     */
    boolean activate(String profileId) {
        RotClientProfile profile =
                config.findById(profileId);

        if (profile == null) {
            return false;
        }

        if (profile.id.equals(config.activeProfileId)) {
            return true;
        }

        String previousActiveProfileId =
                config.activeProfileId;

        config.activeProfileId =
                profile.id;

        if (!saveNow()) {
            config.activeProfileId =
                    previousActiveProfileId;

            return false;
        }

        return true;
    }

    /**
     * Deletes a profile.
     *
     * If the profile being deleted is active and other profiles remain, a
     * replacement profile must explicitly be supplied. This prevents an
     * arbitrary profile from silently becoming active.
     *
     * Passing null as replacementProfileId is valid when deleting an inactive
     * profile or when deleting the final remaining profile.
     */
    boolean delete(
            String profileId,
            String replacementProfileId) {

        RotClientProfile profile =
                config.findById(profileId);

        if (profile == null) {
            return false;
        }

        boolean wasActive =
                profile.id.equals(config.activeProfileId);

        RotClientProfile replacement = null;

        if (wasActive && config.profiles.size() > 1) {
            replacement =
                    config.findById(replacementProfileId);

            if (replacement == null
                    || replacement.id.equals(profile.id)) {
                return false;
            }
        }

        int originalIndex =
                config.profiles.indexOf(profile);

        String previousActiveProfileId =
                config.activeProfileId;

        config.profiles.remove(profile);

        if (config.profiles.isEmpty()) {
            config.activeProfileId = "";
        } else if (wasActive) {
            config.activeProfileId =
                    replacement.id;
        }

        if (!saveNow()) {
            int restoreIndex =
                    Math.max(
                            0,
                            Math.min(
                                    originalIndex,
                                    config.profiles.size()));

            config.profiles.add(
                    restoreIndex,
                    profile);

            config.activeProfileId =
                    previousActiveProfileId;

            return false;
        }

        return true;
    }

    RotClientProfile findById(String profileId) {
        config.normalize();
        return config.findById(profileId);
    }

    /**
     * Replaces the stored settings snapshot for one profile.
     *
     * The incoming settings are deep-copied so the persisted profile never shares
     * mutable QoL collections with the live TrackerConfig.
     */
    boolean updateSettings(
            String profileId,
            RotClientProfileSettings settings) {

        RotClientProfile profile =
                config.findById(profileId);

        if (profile == null) {
            return false;
        }

        RotClientProfileSettings previousSettings =
                profile.settings == null
                        ? RotClientProfileSettings.defaults()
                        : profile.settings.copy();

        profile.settings =
                settings == null
                        ? RotClientProfileSettings.defaults()
                        : settings.copy();

        if (!saveNow()) {
            profile.settings = previousSettings;
            return false;
        }

        return true;
    }

    /**
     * Captures the current live Rot Client preferences into a stored profile.
     */
    boolean captureCurrentSettings(
            String profileId,
            TrackerConfig currentConfig) {

        if (currentConfig == null) {
            return false;
        }

        return updateSettings(
                profileId,
                RotClientProfileSettingsAdapter.capture(
                        currentConfig));
    }

    boolean saveNow() {
        config.normalize();
        return RotClientProfileStore.save(config);
    }

    private RotClientProfile createWithSettings(
            String name,
            RotClientProfileSettings settings) {

        if (!RotClientProfile.isValidName(name)) {
            return null;
        }

        String normalizedName =
                RotClientProfile.normalizeName(name);

        if (containsName(normalizedName, null)) {
            return null;
        }

        RotClientProfile profile =
                RotClientProfile.create(normalizedName);

        profile.settings =
                settings == null
                        ? RotClientProfileSettings.defaults()
                        : settings.copy();

        String previousActiveProfileId =
                config.activeProfileId;

        config.profiles.add(profile);
        config.activeProfileId = profile.id;

        if (!saveNow()) {
            config.profiles.remove(profile);
            config.activeProfileId =
                    previousActiveProfileId;

            return null;
        }

        return profile;
    }

    private boolean containsName(
            String name,
            String excludedProfileId) {

        if (name == null) {
            return false;
        }

        for (RotClientProfile profile : config.profiles) {
            if (profile == null
                    || profile.name == null) {
                continue;
            }

            if (excludedProfileId != null
                    && profile.matchesId(excludedProfileId)) {
                continue;
            }

            if (profile.name.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }
}