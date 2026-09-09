package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RotClientLoadoutConfigTest {

    @Test
    void emptyConfigIsValid() {
        RotClientLoadoutConfig config =
                RotClientLoadoutConfig.defaults();

        config.normalize();

        assertTrue(config.loadouts.isEmpty());
        assertEquals("", config.activeLoadoutId);
        assertNull(config.activeLoadout());
    }

    @Test
    void loadoutUsesStableIdWhenRenamed() {
        RotClientLoadout loadout =
                RotClientLoadout.create(
                        "Gemstone Mining");

        String originalId =
                loadout.id;

        loadout.rename(
                "Mithril Mining");

        assertEquals(
                originalId,
                loadout.id);

        assertEquals(
                "Mithril Mining",
                loadout.name);
    }

    @Test
    void loadoutCanReferenceSettingsProfile() {
        RotClientLoadout loadout =
                RotClientLoadout.create(
                        "Gemstone Mining");

        loadout.settingsProfileId =
                "settings-profile-123";

        loadout.normalize();

        assertTrue(
                loadout.hasLinkedSettingsProfile());

        assertEquals(
                "settings-profile-123",
                loadout.settingsProfileId);
    }

    @Test
    void loadoutCanHaveNoSettingsProfile() {
        RotClientLoadout loadout =
                RotClientLoadout.create(
                        "Standalone");

        loadout.settingsProfileId = null;

        loadout.normalize();

        assertFalse(
                loadout.hasLinkedSettingsProfile());

        assertEquals(
                "",
                loadout.settingsProfileId);
    }

    @Test
    void duplicateKeepsSettingsProfileLinkButGetsNewId() {
        RotClientLoadout source =
                RotClientLoadout.create(
                        "Gemstone Mining");

        source.settingsProfileId =
                "mining-settings";

        RotClientLoadout duplicate =
                source.duplicate(
                        "Mithril Mining");

        assertNotEquals(
                source.id,
                duplicate.id);

        assertEquals(
                "mining-settings",
                duplicate.settingsProfileId);

        assertEquals(
                "Mithril Mining",
                duplicate.name);
    }

    @Test
    void duplicateIdsAreRepaired() {
        RotClientLoadoutConfig config =
                RotClientLoadoutConfig.defaults();

        RotClientLoadout first =
                RotClientLoadout.create("One");

        RotClientLoadout second =
                RotClientLoadout.create("Two");

        second.id =
                first.id;

        config.loadouts.add(first);
        config.loadouts.add(second);

        config.normalize();

        assertNotEquals(
                first.id,
                second.id);
    }

    @Test
    void invalidActiveIdFallsBackToExistingLoadout() {
        RotClientLoadoutConfig config =
                RotClientLoadoutConfig.defaults();

        RotClientLoadout first =
                RotClientLoadout.create(
                        "First");

        RotClientLoadout second =
                RotClientLoadout.create(
                        "Second");

        config.loadouts.add(first);
        config.loadouts.add(second);
        config.activeLoadoutId =
                "missing-loadout";

        config.normalize();

        assertEquals(
                first.id,
                config.activeLoadoutId);

        assertSame(
                first,
                config.activeLoadout());
    }

    @Test
    void blankAndOverlongNamesAreInvalid() {
        assertFalse(
                RotClientLoadout.isValidName("   "));

        assertFalse(
                RotClientLoadout.isValidName(null));

        assertFalse(
                RotClientLoadout.isValidName(
                        "x".repeat(
                                RotClientLoadout.MAX_NAME_LENGTH + 1)));

        assertTrue(
                RotClientLoadout.isValidName(
                        "Mining"));
    }

    @Test
    void containsNameIsCaseInsensitive() {
        RotClientLoadoutConfig config =
                RotClientLoadoutConfig.defaults();

        config.loadouts.add(
                RotClientLoadout.create(
                        "Mining"));

        assertTrue(
                config.containsName(
                        "mining"));

        assertTrue(
                config.containsName(
                        "MINING"));

        assertFalse(
                config.containsName(
                        "Slayer"));
    }
}