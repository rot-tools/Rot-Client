package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Hidden-menu Pet activation for Rot Client loadouts.
 *
 * Opens /pets without rendering the chest GUI, searches every Pets page for
 * the configured exact pet identity, clicks it when necessary, then closes
 * the hidden container.
 */
public final class RotClientPetAutoEquipRuntime {
    private static final long TIMEOUT_MS = 5_000L;
    private static final int CLICK_DELAY_TICKS = 1;
    private static final int CLOSE_DELAY_TICKS = 1;

    private static boolean active;
    private static boolean inMenu;
    private static boolean awaitingPageTurn;
    private static boolean seekingFirstPage;

    private static ItemStack pendingEquippedPet =
            ItemStack.EMPTY;

    private static String targetPetUuid = "";

    private static int containerId = -1;
    private static int currentPage = -1;
    private static int totalPages = 1;
    private static int waitTicks;
    private static int pendingCloseTicks = -1;
    private static boolean awaitingEquipConfirmation;

    private static long startedAtMs;

    private RotClientPetAutoEquipRuntime() {
    }

    static boolean busy() {
        return active
                || pendingCloseTicks >= 0;
    }

    static boolean begin(
            String petUuid) {

        if (petUuid == null
                || petUuid.isBlank()) {

            return true;
        }

        if (busy()) {
            return false;
        }

        Minecraft client =
                Minecraft.getInstance();

        LocalPlayer player =
                client == null
                        ? null
                        : client.player;

        if (player == null
                || player.connection == null) {

            return false;
        }

        targetPetUuid =
                petUuid.trim();

        active = true;
        inMenu = false;
        awaitingPageTurn = false;
        seekingFirstPage = true;
        awaitingEquipConfirmation = false;
        pendingEquippedPet = ItemStack.EMPTY;

        containerId = -1;
        currentPage = -1;
        totalPages = 1;
        waitTicks = 0;

        startedAtMs =
                System.currentTimeMillis();

        player.connection.sendCommand(
                InventoryOverlayPolicy.OPEN_PETS_COMMAND);

        return true;
    }

    static void tick(
            Minecraft client) {

        tickPendingClose(client);

        if (!active) {
            return;
        }

        if (System.currentTimeMillis()
                - startedAtMs > TIMEOUT_MS) {

            cancelAndClose(client);
            return;
        }

        if (!inMenu
                || client == null
                || client.player == null
                || client.gameMode == null) {

            return;
        }

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        LocalPlayer player =
                client.player;

        AbstractContainerMenu menu =
                player.containerMenu;

        if (menu == null
                || menu.containerId != containerId) {

            return;
        }

        if (!menuReady(menu)) {
            return;
        }

        /*
         * /pets normally starts on page 1, but force our search to page 1
         * anyway so activation remains deterministic if Hypixel changes that.
         */
        if (seekingFirstPage
                && currentPage > 1) {

            turnPage(
                    client,
                    player,
                    MenuKeybindPolicy.PETS_PREVIOUS_SLOT);

            return;
        }

        seekingFirstPage = false;

        int targetSlot =
                findTargetPetSlot(menu);

        if (targetSlot >= 0) {
            ItemStack stack =
                    menu.slots
                            .get(targetSlot)
                            .getItem();

            boolean alreadyEquipped =
                    MenuKeybindPolicy
                            .loreMeansPetEquipped(
                                    InventoryChromeRuntime
                                            .loreLines(stack));

            /*
             * Hypixel has now confirmed that this exact pet is active.
             *
             * Only now update InventoryChromeRuntime. This prevents us from caching
             * the pre-click Pets-menu state.
             */
            if (alreadyEquipped) {
                InventoryChromeRuntime
                        .noteEquippedPet(stack);

                pendingEquippedPet =
                        ItemStack.EMPTY;

                awaitingEquipConfirmation = false;

                finishSuccessfully();
                return;
            }

            /*
             * We already sent the summon click. Wait for the server's Pets-menu
             * update instead of repeatedly clicking the same pet.
             */
            if (awaitingEquipConfirmation) {
                return;
            }

            /*
             * Keep a copy of the exact pet before Hypixel closes/refreshes the menu.
             * Some Pets-menu versions close immediately after summoning, so there may
             * never be a later lore update for us to observe.
             */
            pendingEquippedPet =
                    stack.copy();

            client.gameMode
                    .handleContainerInput(
                            containerId,
                            targetSlot,
                            0,
                            ContainerInput.PICKUP,
                            player);

            awaitingEquipConfirmation = true;

            /*
             * Give the server a fresh timeout window to confirm the equip.
             */
            startedAtMs =
                    System.currentTimeMillis();

            return;
        }

        /*
         * The target may temporarily disappear while Hypixel refreshes the menu
         * after our click. Do not interpret that as "search the next page".
         */
        if (awaitingEquipConfirmation) {
            return;
        }

        /*
         * Target is not on this page. Search pages from 1 -> total.
         */
        if (currentPage > 0
                && currentPage < totalPages) {

            turnPage(
                    client,
                    player,
                    MenuKeybindPolicy.PETS_NEXT_SLOT);

            return;
        }

        /*
         * Every page was searched and the stored pet no longer exists.
         */
        cancelAndClose(client);
    }

    public static boolean consumeOpenScreen(
            ClientboundOpenScreenPacket packet) {

        if (!active
                || packet == null) {

            return false;
        }

        String title =
                packet.getTitle() == null
                        ? ""
                        : packet.getTitle()
                        .getString();

        MenuKeybindPolicy.PageTitle page =
                MenuKeybindPolicy
                        .parsePetsTitle(title);

        if (page == null) {
            return false;
        }

        Minecraft client =
                Minecraft.getInstance();

        LocalPlayer player =
                client == null
                        ? null
                        : client.player;

        if (player == null
                || packet.getType() == null) {

            return false;
        }

        player.containerMenu =
                packet.getType()
                        .create(
                                packet.getContainerId(),
                                player.getInventory());

        containerId =
                packet.getContainerId();

        currentPage =
                Math.max(
                        1,
                        page.current());

        totalPages =
                Math.max(
                        currentPage,
                        page.total());

        waitTicks =
                CLICK_DELAY_TICKS;

        inMenu = true;
        awaitingPageTurn = false;

        startedAtMs =
                System.currentTimeMillis();

        return true;
    }

    public static void onContainerClosed() {
        /*
         * Ignore a stale close packet while /pets is still waiting to open or
         * while moving between Pets pages.
         */
        if (active
                && !inMenu) {

            return;
        }

        /*
         * Hypixel commonly closes the Pets menu immediately after a pet is
         * summoned. In that case the close itself is our confirmation that the
         * click completed, because we never get a chance to observe the updated
         * "Click to despawn!" lore.
         */
        if (active
                && awaitingEquipConfirmation
                && pendingEquippedPet != null
                && !pendingEquippedPet.isEmpty()) {

            InventoryChromeRuntime
                    .noteEquippedPet(
                            pendingEquippedPet);

            reset();
            return;
        }

        reset();
    }

    private static void turnPage(
            Minecraft client,
            LocalPlayer player,
            int slot) {

        if (client == null
                || client.gameMode == null
                || player == null
                || containerId < 0) {

            cancelAndClose(client);
            return;
        }

        AbstractContainerMenu menu =
                player.containerMenu;

        if (menu == null
                || slot < 0
                || slot >= menu.slots.size()) {

            cancelAndClose(client);
            return;
        }

        client.gameMode
                .handleContainerInput(
                        containerId,
                        slot,
                        0,
                        ContainerInput.PICKUP,
                        player);

        awaitingPageTurn = true;
        inMenu = false;
        containerId = -1;

        startedAtMs =
                System.currentTimeMillis();
    }

    private static int findTargetPetSlot(
            AbstractContainerMenu menu) {

        for (int slotIndex :
                MenuKeybindPolicy.PET_SLOTS) {

            if (slotIndex < 0
                    || slotIndex >= menu.slots.size()) {

                continue;
            }

            Slot slot =
                    menu.slots.get(slotIndex);

            ItemStack stack =
                    slot.getItem();

            if (stack == null
                    || stack.isEmpty()) {

                continue;
            }

            String identity =
                    petIdentity(stack);

            if (!identity.isBlank()
                    && identity.equals(
                    targetPetUuid)) {

                return slotIndex;
            }
        }

        return -1;
    }

    /**
     * Uses exactly the same identity scheme as the loadout Pet picker.
     */
    private static String petIdentity(
            ItemStack stack) {

        String uuid =
                SkyBlockItemData
                        .uuid(stack);

        if (uuid != null
                && !uuid.isBlank()) {

            return uuid.trim();
        }

        String petName =
                MenuKeybindPolicy
                        .stripGuiText(
                                stack.getHoverName()
                                        .getString())
                        .trim();

        String petInfo =
                SkyBlockItemData
                        .petInfo(stack);

        if (petInfo == null
                || petInfo.isBlank()
                || petName.isBlank()) {

            return "";
        }

        return "menu:"
                + petName
                .toLowerCase()
                .replace(' ', '_');
    }

    private static boolean menuReady(
            AbstractContainerMenu menu) {

        if (menu == null
                || menu.slots.size()
                <= MenuKeybindPolicy.PETS_CLOSE_SLOT) {

            return false;
        }

        ItemStack close =
                menu.slots
                        .get(
                                MenuKeybindPolicy
                                        .PETS_CLOSE_SLOT)
                        .getItem();

        if (close != null
                && !close.isEmpty()) {

            return true;
        }

        /*
         * Fallback for a menu-layout change: populated Pet slots are also
         * enough evidence that the page contents arrived.
         */
        for (int slot :
                MenuKeybindPolicy.PET_SLOTS) {

            if (slot >= 0
                    && slot < menu.slots.size()
                    && !menu.slots
                    .get(slot)
                    .getItem()
                    .isEmpty()) {

                return true;
            }
        }

        return false;
    }

    private static void finishSuccessfully() {
        active = false;
        inMenu = false;
        awaitingPageTurn = false;

        containerId = -1;

        pendingCloseTicks =
                CLOSE_DELAY_TICKS;
    }

    private static void tickPendingClose(
            Minecraft client) {

        if (pendingCloseTicks < 0) {
            return;
        }

        if (client == null
                || client.player == null) {

            reset();
            return;
        }

        if (pendingCloseTicks == 0) {
            pendingCloseTicks = -1;

            client.player
                    .closeContainer();

            return;
        }

        pendingCloseTicks--;
    }

    private static void cancelAndClose(
            Minecraft client) {

        if (client != null
                && client.player != null
                && inMenu) {

            client.player
                    .closeContainer();
        }

        reset();
    }

    private static void reset() {
        active = false;
        inMenu = false;
        awaitingPageTurn = false;
        seekingFirstPage = false;
        awaitingEquipConfirmation = false;
        pendingEquippedPet = ItemStack.EMPTY;

        targetPetUuid = "";

        containerId = -1;
        currentPage = -1;
        totalPages = 1;
        waitTicks = 0;
        pendingCloseTicks = -1;

        startedAtMs = 0L;
    }
}