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
    void loadoutAutomationStaysBehindPlusFlavor() throws Exception {
        String hooks =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/QolClientFlavorHooks.java"));

        String coordinator =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientLoadoutActivationCoordinator.java"));

        String wardrobePicker =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientWardrobePickerRuntime.java"));

        String petPicker =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientPetPickerRuntime.java"));

        String equipmentPicker =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/RotClientEquipmentPickerRuntime.java"));

        String plusHooks =
                Files.readString(
                        Path.of(
                                "src/plusClient/java/fi/rotclient/RotClientPlusHooks.java"));

        /*
         * Legit has no provider and therefore inherits the false/default
         * flavor implementation. Plus explicitly opts into loadout automation.
         */
        assertTrue(
                hooks.contains(
                        "default boolean loadoutsEnabled()"));

        assertTrue(
                coordinator.contains(
                        "loadoutsEnabled()"));

        assertTrue(
                coordinator.contains(
                        ".beginWardrobeLoadoutEquip("));

        assertTrue(
                coordinator.contains(
                        ".beginLoadoutPetEquip("));

        assertTrue(
                coordinator.contains(
                        ".beginLoadoutEquipmentEquip("));

        /*
         * Shared coordination must go through the flavor boundary instead of
         * directly initiating the automatic gear runtimes.
         */
        assertTrue(
                !coordinator.contains(
                        "WardrobeAutoEquipRuntime.beginLoadoutEquip("));

        assertTrue(
                !coordinator.contains(
                        "RotClientPetAutoEquipRuntime"));

        assertTrue(
                !coordinator.contains(
                        "RotClientEquipmentAutoEquipRuntime"));

        assertTrue(
                wardrobePicker.contains(
                        "loadoutsEnabled()"));

        assertTrue(
                petPicker.contains(
                        "loadoutsEnabled()"));

        assertTrue(
                equipmentPicker.contains(
                        "loadoutsEnabled()"));

        assertTrue(
                !wardrobePicker.contains(
                        "WardrobeAutoEquipRuntime.beginLoadoutEquip("));

        assertTrue(
                !petPicker.contains(
                        "RotClientPetAutoEquipRuntime"));

        assertTrue(
                !equipmentPicker.contains(
                        "RotClientEquipmentAutoEquipRuntime"));

        assertTrue(
                plusHooks.contains(
                        "public boolean loadoutsEnabled()"));

        assertTrue(
                plusHooks.contains(
                        "beginLoadoutPetEquip("));

        assertTrue(
                plusHooks.contains(
                        "beginLoadoutEquipmentEquip("));

        /*
         * Plus standalone Wardrobe Swapper behavior remains present too.
         */
        assertTrue(
                plusHooks.contains(
                        "wardrobeAutoEquipKey("));

        assertTrue(
                plusHooks.contains(
                        "wardrobeHudText("));
    }

    @Test
    void regularUiHidesPlusOnlyLoadouts() throws Exception {
        String hooks =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/QolClientFlavorHooks.java"));

        String plusHooks =
                Files.readString(
                        Path.of(
                                "src/plusClient/java/fi/rotclient/RotClientPlusHooks.java"));

        String screen =
                Files.readString(
                        Path.of(
                                "src/client/java/fi/rotclient/MiningUiScreen.java"));

        String sidebar =
                Files.readString(
                        Path.of(
                                "src/main/java/fi/rotclient/RotClientSidebarNav.java"));

        assertTrue(
                hooks.contains(
                        "default boolean loadoutsEnabled()"));

        assertTrue(
                plusHooks.contains(
                        "public boolean loadoutsEnabled()"));

        assertTrue(
                screen.contains(
                        "pruneUnavailableLoadoutTabs(workspace)"));

        assertTrue(
                screen.contains(
                        "if (!loadoutsEnabled())"));

        assertTrue(
                screen.contains(
                        "loadoutsEnabled()"));

        assertTrue(
                screen.contains(
                        "&& layout.loadoutsVisible()"));

        assertTrue(
                sidebar.contains(
                        "loadoutsEnabled"));

        assertTrue(
                sidebar.contains(
                        "? 4"));

        assertTrue(
                sidebar.contains(
                        ": 3"));
    }
}
