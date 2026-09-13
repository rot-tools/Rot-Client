package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class RotClientLoadoutWiringTest {

    @Test
    void clientOwnsAndLoadsLoadoutManager() throws Exception {
        String source =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientClient.java"));

        assertTrue(
                source.contains(
                        "RotClientLoadoutManager LOADOUTS"));

        assertTrue(
                source.contains(
                        "LOADOUTS.loadFromDisk()"));

        assertTrue(
                source.contains(
                        "RotClientLoadoutManager loadouts()"));
    }

    @Test
    void loadoutsUseSeparatePersistenceFile() throws Exception {
        String source =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientLoadoutStore.java"));

        assertTrue(
                source.contains(
                        "\"rotclient-loadouts.json\""));

        assertTrue(
                source.contains(
                        "AtomicFileWriter.writeAtomically"));
    }

    @Test
    void clientOwnsLoadoutActivationCoordinator() throws Exception {
        String client =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientClient.java"));

        String coordinator =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientLoadoutActivationCoordinator.java"));

        assertTrue(
                client.contains(
                        "RotClientLoadoutActivationCoordinator LOADOUT_ACTIVATION"));

        assertTrue(
                client.contains(
                        "RotClientLoadoutActivationCoordinator loadoutActivation()"));

        assertTrue(
                coordinator.contains(
                        "profiles.switchTo("));

        assertTrue(
                coordinator.contains(
                        "loadouts.activate("));
    }

    @Test
    void loadoutUiUsesActivationCoordinator() throws Exception {
        String screen =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/MiningUiScreen.java"));

        assertTrue(
                screen.contains(
                        ".loadoutActivation()"));

        assertTrue(
                screen.contains(
                        ".activate("));

        assertTrue(
                screen.contains(
                        "\"SWITCH\""));
    }

    @Test
    void relationshipPointsFromLoadoutToSettingsProfile() throws Exception {
        String loadoutSource =
                Files.readString(
                        Path.of(
                                "src/main/java/fi/rotclient/RotClientLoadout.java"));

        String profileSource =
                Files.readString(
                        Path.of(
                                "src/main/java/fi/rotclient/RotClientProfile.java"));

        assertTrue(
                loadoutSource.contains(
                        "String settingsProfileId"));

        assertTrue(
                !profileSource.contains(
                        "linkedLoadoutId"));
    }

    @Test
    void loadoutsAreReachableFromClientUi() throws Exception {
        String screen =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/MiningUiScreen.java"));

        assertTrue(
                screen.contains(
                        "RotClientWorkspaceRoute.LOADOUTS"));

        assertTrue(
                screen.contains(
                        "void openLoadoutsPage()"));

        assertTrue(
                screen.contains(
                        "drawLoadoutsPage("));

        assertTrue(
                screen.contains(
                        "RotClientSidebarNav.HitTarget.LOADOUTS"));
    }

    @Test
    void regularLoadoutsOwnWardrobeActivationPath() throws Exception {
        String coordinator =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientLoadoutActivationCoordinator.java"));

        String client =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientClient.java"));

        String mixin =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"));

        String picker =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientWardrobePickerRuntime.java"));

        String plusHooks =
                Files.readString(
                        Path.of(
                                "src/plusClient/java/fi/rotclient/RotClientPlusHooks.java"));

        assertTrue(
                Files.exists(
                        Path.of(
                                "src/client/java/fi/rotclient/WardrobeAutoEquipRuntime.java")));

        assertTrue(
                !Files.exists(
                        Path.of(
                                "src/plusClient/java/fi/rotclient/WardrobeAutoEquipRuntime.java")));

        assertTrue(
                coordinator.contains(
                        "WardrobeAutoEquipRuntime.beginLoadoutEquip("));

        assertTrue(
                coordinator.contains(
                        "WardrobeAutoEquipRuntime.busy()"));

        assertTrue(
                !coordinator.contains(
                        ".beginWardrobeLoadoutEquip("));

        assertTrue(
                client.contains(
                        "WardrobeAutoEquipRuntime.tick(client)"));

        assertTrue(
                mixin.contains(
                        "WardrobeAutoEquipRuntime.consumeOpenScreen(packet)"));

        assertTrue(
                mixin.contains(
                        "WardrobeAutoEquipRuntime.onContainerClosed()"));

        assertTrue(
                picker.contains(
                        "WardrobeAutoEquipRuntime"));

        assertTrue(
                picker.contains(
                        ".beginLoadoutEquip("));

        /*
         * The runtime is shared for Loadouts, but standalone Wardrobe
         * automation controls remain Plus entry points.
         */
        assertTrue(
                plusHooks.contains(
                        "wardrobeAutoEquipKey("));

        assertTrue(
                plusHooks.contains(
                        "wardrobeHudText("));

        /*
         * Shared RotClientClient owns the runtime tick now, so Plus must not
         * tick the same Wardrobe state machine a second time.
         */
        assertTrue(
                !plusHooks.contains(
                        "\"WARDROBE_AUTO_EQUIP\""));
    }
}