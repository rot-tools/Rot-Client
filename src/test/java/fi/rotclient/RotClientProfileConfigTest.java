package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientProfileConfigTest {

    @Test
    void newConfigContainsNoPredefinedProfiles() {
        RotClientProfileConfig config =
                RotClientProfileConfig.defaults();

        config.normalize();

        assertTrue(config.profiles.isEmpty());
        assertEquals("", config.activeProfileId);
        assertNull(config.activeProfile());
    }

    @Test
    void createdProfileUsesUserNameAndStableId() {
        RotClientProfile profile =
                RotClientProfile.create("  My Setup  ");

        assertNotNull(profile.id);
        assertFalse(profile.id.isBlank());
        assertEquals("My Setup", profile.name);

        String originalId = profile.id;

        profile.rename("F7 Setup");

        assertEquals("F7 Setup", profile.name);
        assertEquals(originalId, profile.id);
    }

    @Test
    void invalidNamesAreRejected() {
        assertFalse(RotClientProfile.isValidName(null));
        assertFalse(RotClientProfile.isValidName(""));
        assertFalse(RotClientProfile.isValidName("   "));

        assertTrue(RotClientProfile.isValidName("Mining"));
    }

    @Test
    void longNamesAreInvalidAndNormalizationIsBounded() {
        String longName =
                "A".repeat(RotClientProfile.MAX_NAME_LENGTH + 10);

        assertFalse(RotClientProfile.isValidName(longName));

        String normalized =
                RotClientProfile.normalizeName(longName);

        assertEquals(
                RotClientProfile.MAX_NAME_LENGTH,
                normalized.length());
    }

    @Test
    void duplicatedProfileGetsIndependentSettingsCopy() {
        RotClientProfile original =
                RotClientProfile.create("Original");

        original.settings.autoSprintEnabled = true;
        original.settings.miningHudX = 222.0F;
        original.settings.qolUtilities.performanceHudEnabled = true;
        original.settings.qolUtilities.autoClickerLeftWhitelist.add("Terminator");

        RotClientProfile duplicate =
                original.duplicate("Duplicate");

        assertNotEquals(original.id, duplicate.id);
        assertEquals("Duplicate", duplicate.name);

        assertTrue(duplicate.settings.autoSprintEnabled);
        assertEquals(222.0F, duplicate.settings.miningHudX);
        assertTrue(
                duplicate.settings
                        .qolUtilities
                        .performanceHudEnabled);

        assertEquals(
                1,
                duplicate.settings
                        .qolUtilities
                        .autoClickerLeftWhitelist
                        .size());

        duplicate.settings.autoSprintEnabled = false;
        duplicate.settings.miningHudX = 333.0F;
        duplicate.settings
                .qolUtilities
                .performanceHudEnabled = false;

        duplicate.settings
                .qolUtilities
                .autoClickerLeftWhitelist
                .add("Juju");

        assertTrue(original.settings.autoSprintEnabled);
        assertEquals(222.0F, original.settings.miningHudX);
        assertTrue(
                original.settings
                        .qolUtilities
                        .performanceHudEnabled);

        assertEquals(
                1,
                original.settings
                        .qolUtilities
                        .autoClickerLeftWhitelist
                        .size());
    }

    @Test
    void missingSettingsAreRepairedDuringNormalization() {
        RotClientProfile profile =
                RotClientProfile.create("Broken");

        profile.settings = null;

        profile.normalize();

        assertNotNull(profile.settings);
        assertNotNull(profile.settings.qolUtilities);
    }

    @Test
    void profileSettingsNormalizeInvalidHudValues() {
        RotClientProfileSettings settings =
                RotClientProfileSettings.defaults();

        settings.miningHudX = -500.0F;
        settings.miningHudY = Float.NaN;
        settings.miningHudScale = 50.0F;

        settings.powderChestHudX = -100.0F;
        settings.powderChestHudY = Float.NaN;
        settings.powderChestHudScale = -50.0F;

        settings.bazaarTaxPercent = 500.0D;
        settings.selectedTargetId = "   ";

        settings.normalize();

        assertEquals(0.0F, settings.miningHudX);
        assertEquals(12.0F, settings.miningHudY);
        assertEquals(2.5F, settings.miningHudScale);

        assertEquals(0.0F, settings.powderChestHudX);
        assertEquals(12.0F, settings.powderChestHudY);
        assertEquals(0.5F, settings.powderChestHudScale);

        assertEquals(100.0D, settings.bazaarTaxPercent);
        assertEquals("GOLD", settings.selectedTargetId);
    }

    @Test
    void settingsCopyDoesNotShareQolCollections() {
        RotClientProfileSettings original =
                RotClientProfileSettings.defaults();

        original.qolUtilities
                .autoClickerLeftWhitelist
                .add("Original Item");

        RotClientProfileSettings copy =
                original.copy();

        copy.qolUtilities
                .autoClickerLeftWhitelist
                .add("Copy Item");

        assertEquals(
                1,
                original.qolUtilities
                        .autoClickerLeftWhitelist
                        .size());

        assertEquals(
                2,
                copy.qolUtilities
                        .autoClickerLeftWhitelist
                        .size());
    }

    @Test
    void configFindsProfilesByStableId() {
        RotClientProfile first =
                RotClientProfile.create("First");

        RotClientProfile second =
                RotClientProfile.create("Second");

        RotClientProfileConfig config =
                RotClientProfileConfig.defaults();

        config.profiles.add(first);
        config.profiles.add(second);
        config.activeProfileId = second.id;

        config.normalize();

        assertEquals(second, config.activeProfile());
        assertEquals(first, config.findById(first.id));
        assertEquals(second, config.findById(second.id));
    }

    @Test
    void missingActiveProfileFallsBackToFirstProfile() {
        RotClientProfile first =
                RotClientProfile.create("First");

        RotClientProfile second =
                RotClientProfile.create("Second");

        RotClientProfileConfig config =
                RotClientProfileConfig.defaults();

        config.profiles.add(first);
        config.profiles.add(second);
        config.activeProfileId = "does-not-exist";

        config.normalize();

        assertEquals(first.id, config.activeProfileId);
        assertEquals(first, config.activeProfile());
    }

    @Test
    void defaultProfileKeepsMiningTrackerDisabled() {
        RotClientProfileSettings settings =
                RotClientProfileSettings.defaults();

        assertFalse(
                settings.miningTrackerEnabled);
    }

    @Test
    void duplicateIdsAreRepairedDuringNormalization() {
        RotClientProfile first =
                RotClientProfile.create("First");

        RotClientProfile second =
                RotClientProfile.create("Second");

        second.id = first.id;

        RotClientProfileConfig config =
                RotClientProfileConfig.defaults();

        config.profiles.add(first);
        config.profiles.add(second);

        config.normalize();

        assertNotEquals(first.id, second.id);
        assertEquals(2, config.profiles.size());
    }

    @Test
    void profileNamesAreMatchedCaseInsensitively() {
        RotClientProfileConfig config =
                RotClientProfileConfig.defaults();

        config.profiles.add(
                RotClientProfile.create("Dungeon Setup"));

        assertTrue(config.containsName("Dungeon Setup"));
        assertTrue(config.containsName("dungeon setup"));
        assertTrue(config.containsName("  DUNGEON SETUP  "));
        assertFalse(config.containsName("Mining"));
    }

    @Test
    void deletingAllProfilesCanLeaveNoActiveProfile() {
        RotClientProfile profile =
                RotClientProfile.create("Only Profile");

        RotClientProfileConfig config =
                RotClientProfileConfig.defaults();

        config.profiles.add(profile);
        config.activeProfileId = profile.id;

        config.profiles.clear();
        config.normalize();

        assertTrue(config.profiles.isEmpty());
        assertEquals("", config.activeProfileId);
        assertNull(config.activeProfile());
    }
}

