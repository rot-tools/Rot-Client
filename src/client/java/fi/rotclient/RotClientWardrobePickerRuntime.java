package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;

/**
 * User-driven wardrobe selector.
 *
 * The real SkyBlock Wardrobe GUI is opened and the user's selected logical
 * wardrobe number is returned without sending the actual armor-set click.
 */
public final class RotClientWardrobePickerRuntime {
    private static String pendingLoadoutId = "";

    private static IntConsumer pendingSelection;

    private static Screen returnScreen;

    private RotClientWardrobePickerRuntime() {
    }

    static boolean active() {
        return (pendingLoadoutId != null
                && !pendingLoadoutId.isBlank())
                || pendingSelection != null;
    }

    /**
     * Existing-loadout mode.
     *
     * The selected wardrobe number is persisted directly on the loadout.
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

        return openWardrobe(
                screenToReturnTo);
    }

    /**
     * Draft/wizard mode.
     *
     * Nothing is persisted. The selected wardrobe number is returned to the
     * caller so the Loadout creation wizard can keep it in temporary state.
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

        return openWardrobe(
                screenToReturnTo);
    }

    private static boolean openWardrobe(
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
                WardrobeKeybindPolicy.OPEN_COMMAND);

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

        if (!WardrobeKeybindPolicy
                .isWardrobeTitle(title)) {

            return false;
        }

        finishAndReturn();

        return true;
    }

    /**
     * Called before vanilla handles a click in a container screen.
     *
     * true  = consume/block the click
     * false = allow vanilla/server handling
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

        if (!WardrobeKeybindPolicy
                .isWardrobeTitle(title)) {

            return false;
        }

        if (mouseButton != 0) {
            return true;
        }

        if (hovered == null) {
            return true;
        }

        int slot =
                hovered.index;

        /*
         * Let Hypixel handle page navigation normally.
         */
        if (slot
                == MenuKeybindPolicy.WARDROBE_PREVIOUS_SLOT
                || slot
                == MenuKeybindPolicy.WARDROBE_NEXT_SLOT) {

            return false;
        }

        int first =
                MenuKeybindPolicy.WARDROBE_SLOT_BASE;

        int last =
                first + 8;

        if (slot < first
                || slot > last) {

            return true;
        }

        ItemStack stack =
                hovered.getItem();

        if (stack == null
                || stack.isEmpty()
                || isLoadingPane(stack)
                || WardrobeKeybindPolicy.isEmptyMarker(
                isItemPath(
                        stack,
                        "gray_dye"),
                stack.getHoverName()
                        .getString())) {

            return true;
        }

        MenuKeybindPolicy.PageTitle page =
                MenuKeybindPolicy
                        .parseWardrobeTitle(title);

        int pageNumber =
                page == null
                        ? 1
                        : Math.max(
                        1,
                        page.current());

        int column =
                slot
                        - MenuKeybindPolicy
                        .WARDROBE_SLOT_BASE;

        int globalWardrobeNumber =
                (pageNumber - 1) * 9
                        + column
                        + 1;

        /*
         * Wizard/draft selection.
         */
        if (pendingSelection != null) {
            IntConsumer selection =
                    pendingSelection;

            selection.accept(
                    globalWardrobeNumber);

            finishAndReturn();

            return true;
        }

        /*
         * Existing-loadout selection.
         */
        if (pendingLoadoutId == null
                || pendingLoadoutId.isBlank()) {

            finishAndReturn();
            return true;
        }

        boolean saved =
                RotClientClient
                        .loadouts()
                        .setWardrobeSlot(
                                pendingLoadoutId,
                                globalWardrobeNumber);

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

    private static boolean isLoadingPane(
            ItemStack stack) {

        return isItemPath(
                stack,
                "pink_stained_glass_pane")
                || isItemPath(
                stack,
                "magenta_stained_glass_pane");
    }

    private static boolean isItemPath(
            ItemStack stack,
            String path) {

        if (stack == null
                || stack.isEmpty()
                || path == null) {

            return false;
        }

        var id =
                BuiltInRegistries.ITEM
                        .getKey(
                                stack.getItem());

        return id != null
                && path.equals(
                id.getPath());
    }
}