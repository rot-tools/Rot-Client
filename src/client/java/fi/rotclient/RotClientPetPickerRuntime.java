package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiConsumer;

/**
 * User-driven SkyBlock pet selector.
 *
 * Opens the real Hypixel Pets menu and intercepts the selected pet click so
 * configuring a loadout does not actually spawn/equip the pet.
 */
public final class RotClientPetPickerRuntime {
    private static String pendingLoadoutId = "";

    private static BiConsumer<String, String> pendingSelection;

    private static Screen returnScreen;

    private RotClientPetPickerRuntime() {
    }

    static boolean active() {
        return (pendingLoadoutId != null
                && !pendingLoadoutId.isBlank())
                || pendingSelection != null;
    }

    /**
     * Existing-loadout mode.
     */
    static boolean begin(
            String loadoutId,
            Screen screenToReturnTo) {

        if (loadoutId == null
                || loadoutId.isBlank()
                || RotClientClient.loadouts()
                .findById(loadoutId) == null) {

            return false;
        }

        pendingLoadoutId =
                loadoutId;

        pendingSelection = null;

        return openPets(
                screenToReturnTo);
    }

    /**
     * Wizard/draft mode.
     *
     * Nothing is persisted here.
     */
    static boolean beginDraft(
            Screen screenToReturnTo,
            BiConsumer<String, String> selection) {

        if (selection == null) {
            return false;
        }

        pendingLoadoutId = "";

        pendingSelection =
                selection;

        return openPets(
                screenToReturnTo);
    }

    private static boolean openPets(
            Screen screenToReturnTo) {

        Minecraft client =
                Minecraft.getInstance();

        LocalPlayer player =
                client == null
                        ? null
                        : client.player;

        if (player == null
                || player.connection == null) {

            clearPending();
            return false;
        }

        returnScreen =
                screenToReturnTo;

        player.connection.sendCommand(
                InventoryOverlayPolicy.OPEN_PETS_COMMAND);

        return true;
    }

    /**
     * Escape cancels selection and returns to Rot Client.
     */
    public static boolean handleKeyPressed(
            AbstractContainerScreen<?> screen,
            int glfwKey) {

        if (!active()
                || screen == null
                || glfwKey != GLFW.GLFW_KEY_ESCAPE) {

            return false;
        }

        String title =
                screen.getTitle() == null
                        ? ""
                        : screen.getTitle()
                        .getString();

        if (MenuKeybindPolicy
                .parsePetsTitle(title) == null) {

            return false;
        }

        finishAndReturn();

        return true;
    }

    /**
     * true  = consume/block click
     * false = let Minecraft/Hypixel process click
     */
    public static boolean handleMousePressed(
            AbstractContainerScreen<?> screen,
            Slot hovered,
            int mouseButton) {

        if (!active()
                || screen == null) {

            return false;
        }

        String title =
                screen.getTitle() == null
                        ? ""
                        : screen.getTitle()
                        .getString();

        if (MenuKeybindPolicy
                .parsePetsTitle(title) == null) {

            return false;
        }

        /*
         * While picking a pet, only left click has meaning.
         */
        if (mouseButton != 0) {
            return true;
        }

        if (hovered == null) {
            return true;
        }

        int slot =
                hovered.index;

        /*
         * Let Hypixel change Pets pages normally.
         */
        if (slot
                == MenuKeybindPolicy.PETS_PREVIOUS_SLOT
                || slot
                == MenuKeybindPolicy.PETS_NEXT_SLOT) {

            return false;
        }

        /*
         * Treat the normal Pets close button as cancelling the picker.
         */
        if (slot
                == MenuKeybindPolicy.PETS_CLOSE_SLOT) {

            finishAndReturn();
            return true;
        }

        if (!isPetSlot(slot)) {
            return true;
        }

        ItemStack stack =
                hovered.getItem();

        if (stack == null
                || stack.isEmpty()) {

            return true;
        }

        /*
         * UUID identifies the exact pet, even if the player owns two pets of
         * the same type and rarity.
         */
        String petName =
                MenuKeybindPolicy
                        .stripGuiText(
                                stack.getHoverName()
                                        .getString())
                        .trim();

        String petUuid =
                SkyBlockItemData
                        .uuid(stack);

        /*
         * Some special Pets-menu entries do not expose a normal item UUID.
         * Keep UUID identity for normal pets, but provide a deterministic
         * menu identity for UUID-less special pets.
         */
        if (petUuid == null
                || petUuid.isBlank()) {

            String petInfo =
                    SkyBlockItemData
                            .petInfo(stack);

            if (petInfo == null
                    || petInfo.isBlank()
                    || petName.isBlank()) {

                return true;
            }

            petUuid =
                    "menu:"
                            + petName
                            .toLowerCase()
                            .replace(' ', '_');
        }

        /*
         * Wizard/draft mode.
         */
        if (pendingSelection != null) {
            BiConsumer<String, String> selection =
                    pendingSelection;

            selection.accept(
                    petUuid,
                    petName);

            finishAndReturn();

            return true;
        }

        /*
         * Existing-loadout mode.
         */
        if (pendingLoadoutId == null
                || pendingLoadoutId.isBlank()) {

            finishAndReturn();
            return true;
        }

        boolean saved =
                RotClientClient
                        .loadouts()
                        .setPet(
                                pendingLoadoutId,
                                petUuid,
                                petName);

        if (!saved) {
            return true;
        }

        finishAndReturn();

        return true;
    }

    private static boolean isPetSlot(
            int slot) {

        for (int petSlot :
                MenuKeybindPolicy.PET_SLOTS) {

            if (petSlot == slot) {
                return true;
            }
        }

        return false;
    }

    private static void finishAndReturn() {
        Minecraft client =
                Minecraft.getInstance();

        Screen target =
                returnScreen;

        clearPending();

        if (client == null) {
            return;
        }

        LocalPlayer player =
                client.player;

        if (player != null) {
            player.closeContainer();
        }

        if (client.gui != null
                && target != null) {

            client.gui.setScreen(
                    target);
        }
    }

    static void cancel() {
        clearPending();
    }

    private static void clearPending() {
        pendingLoadoutId = "";
        pendingSelection = null;
        returnScreen = null;
    }
}