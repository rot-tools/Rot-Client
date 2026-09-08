package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

/**
 * User-driven Equipment Set selector.
 *
 * Opens the real Hypixel Equipment Wardrobe and intercepts the chosen set
 * click so configuring a loadout does not actually equip that set.
 */
public final class RotClientEquipmentPickerRuntime {
    private static final int FIRST_SET_SLOT = 36;
    private static final int LAST_SET_SLOT = 44;

    private static String pendingLoadoutId = "";

    private static IntConsumer pendingSelection;

    private static Screen returnScreen;

    private RotClientEquipmentPickerRuntime() {
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

        return openEquipmentWardrobe(
                screenToReturnTo);
    }

    /**
     * Wizard/draft mode.
     *
     * Nothing is persisted until the wizard is completed.
     */
    static boolean beginDraft(
            Screen screenToReturnTo,
            IntConsumer selection) {

        if (selection == null) {
            return false;
        }

        pendingLoadoutId = "";

        pendingSelection =
                selection;

        return openEquipmentWardrobe(
                screenToReturnTo);
    }

    private static boolean openEquipmentWardrobe(
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
                InventoryOverlayPolicy.OPEN_WARDROBE_COMMAND);

        return true;
    }

    /**
     * Escape cancels the picker and returns to Rot Client.
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

        if (!InventoryOverlayPolicy
                .isEquipmentSetsMenu(title)) {

            return false;
        }

        finishAndReturn();

        return true;
    }

    /**
     * true  = consume/block click
     * false = let Minecraft/Hypixel process it
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

        if (!InventoryOverlayPolicy
                .isEquipmentSetsMenu(title)) {

            return false;
        }

        /*
         * Only left click is meaningful while selecting a set.
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
         * Only the nine Equipment Set selector slots are valid choices.
         * Everything else in the menu is blocked while the picker is active.
         */
        if (slot < FIRST_SET_SLOT
                || slot > LAST_SET_SLOT) {

            return true;
        }

        ItemStack stack =
                hovered.getItem();

        if (stack == null
                || stack.isEmpty()) {

            return true;
        }

        int equipmentSetNumber =
                slot - FIRST_SET_SLOT + 1;

        /*
         * Wizard/draft mode.
         */
        if (pendingSelection != null) {
            IntConsumer selection =
                    pendingSelection;

            selection.accept(
                    equipmentSetNumber);

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
                        .setEquipmentSet(
                                pendingLoadoutId,
                                equipmentSetNumber);

        if (!saved) {
            return true;
        }

        finishAndReturn();

        return true;
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