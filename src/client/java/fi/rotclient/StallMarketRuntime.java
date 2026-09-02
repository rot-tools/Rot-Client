package fi.rotclient;

import fi.rotclient.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Minecraft bridge for {@link StallMarketPolicy}: bazaar search, sell
 * protection, angry co-op, BIN overlay, and AH highlights.
 */
public final class StallMarketRuntime {
    private static String pendingBazaarSearch;
    private static StallMarketPolicy.AuctionStatus auctionStatus =
            StallMarketPolicy.AuctionStatus.INIT;
    private static String auctionItemName = "";
    private static boolean searchKeyWasDown;
    private static boolean pendingSearchClicked;
    private static long closeSignAtMs;

    private StallMarketRuntime() {
    }

    static void clear() {
        pendingBazaarSearch = null;
        auctionStatus = StallMarketPolicy.AuctionStatus.INIT;
        auctionItemName = "";
        searchKeyWasDown = false;
        pendingSearchClicked = false;
        closeSignAtMs = 0L;
    }

    static void tick(Minecraft client) {
        QolSkyblockExtras extras = extras();
        if (client == null || !extras.stallMarketEnabled) {
            searchKeyWasDown = false;
            return;
        }
        if (closeSignAtMs > 0L
                && System.currentTimeMillis() >= closeSignAtMs
                && currentScreen() instanceof AbstractSignEditScreen) {
            closeSignAtMs = 0L;
            currentScreen().onClose();
        }
        if (client.getWindow() == null || client.player == null) {
            return;
        }
        if (!extras.stallBazaarSearch) {
            searchKeyWasDown = false;
            return;
        }
        long window = client.getWindow().handle();
        boolean down = QolKeybindNames.isBoundDown(window, extras.stallSearchKeybind);
        if (down && !searchKeyWasDown) {
            searchHoveredOrHeld(client);
        }
        searchKeyWasDown = down;
        if (currentScreen() instanceof AbstractContainerScreen<?> container) {
            maybeClickPendingSearch(container);
            syncBinStatus(container);
        }
    }

    public static void onScreenOpened(Screen screen) {
        QolSkyblockExtras extras = extras();
        if (!extras.stallMarketEnabled || !(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        maybeClickPendingSearch(container);
        if (extras.stallBinOverlay) {
            syncBinStatus(container);
        }
    }

    public static boolean fillPendingBazaarSign(SignBlockEntity sign, boolean front) {
        QolSkyblockExtras extras = extras();
        if (!extras.stallMarketEnabled
                || !extras.stallBazaarSearch
                || pendingBazaarSearch == null
                || pendingBazaarSearch.isBlank()
                || sign == null) {
            return false;
        }
        String query = pendingBazaarSearch;
        pendingBazaarSearch = null;
        pendingSearchClicked = false;
        try {
            Component[] messages = sign.getFrontText()
                    .getMessages(Minecraft.getInstance().isTextFilteringEnabled());
            if (messages.length < 4) {
                pendingBazaarSearch = query;
                return false;
            }
            messages[0] = Component.literal(query);
            messages[1] = Component.literal("");
            messages[2] = Component.literal("");
            messages[3] = Component.literal("");
            sign.updateText(
                    current -> new SignText(
                            messages,
                            messages,
                            current.getColor(),
                            current.hasGlowingText()),
                    front);
        } catch (Exception ignored) {
            pendingBazaarSearch = query;
            return false;
        }
        closeSignAtMs = System.currentTimeMillis() + 200L;
        return true;
    }

    public static void search(String rawName) {
        QolSkyblockExtras extras = extras();
        if (!extras.stallMarketEnabled || !extras.stallBazaarSearch) {
            return;
        }
        String cleaned = StallMarketPolicy.cleanItemNameForSearch(rawName);
        if (cleaned.isBlank()) {
            chat("§cUsage: /rot bazaarsearch <item>");
            return;
        }
        pendingBazaarSearch = cleaned;
        pendingSearchClicked = false;
        if (!(currentScreen() instanceof AbstractContainerScreen<?> container)
                || !StallMarketPolicy.isBazaar(title(container), containerSize(container))) {
            sendCommand("bz");
            chat("§eSearching bazaar for §f" + cleaned);
            return;
        }
        maybeClickPendingSearch(container);
    }

    public static boolean shouldBlockClick(
            AbstractContainerScreen<?> screen,
            Slot hovered,
            int button,
            boolean ctrl) {
        QolSkyblockExtras extras = extras();
        if (!extras.stallMarketEnabled || screen == null || hovered == null || !hovered.hasItem()) {
            return false;
        }
        ItemStack stack = hovered.getItem();
        String name = stack.getHoverName().getString();
        List<String> lore = InventoryChromeRuntime.loreLines(stack);
        boolean override = ctrl || ctrlHeld();
        if (extras.stallSellProtection) {
            StallMarketPolicy.SellBlock sell = StallMarketPolicy.sellProtection(
                    true,
                    true,
                    title(screen),
                    name,
                    lore,
                    button,
                    override,
                    extras.stallSellThreshold);
            if (sell.block()) {
                chat(sell.message());
                return true;
            }
        }
        if (extras.stallAngryCoop) {
            Optional<StallMarketPolicy.CoopScreen> mode =
                    StallMarketPolicy.coopScreen(title(screen));
            if (mode.isPresent()) {
                String player = playerName();
                boolean anyForeign = hasForeignEntry(screen, player, mode.get());
                Optional<String> actor = StallMarketPolicy.foreignActor(
                        lore, player, mode.get());
                StallMarketPolicy.CoopBlock coop = StallMarketPolicy.coopProtection(
                        true,
                        true,
                        title(screen),
                        name,
                        lore,
                        anyForeign,
                        actor.orElse(""),
                        override);
                if (coop.block()) {
                    chat(coop.message());
                    return true;
                }
            }
        }
        return false;
    }

    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        QolSkyblockExtras extras = extras();
        if (!extras.stallMarketEnabled || !extras.stallSellProtection || stack == null) {
            return;
        }
        if (!(currentScreen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        for (String line : StallMarketPolicy.sellProtectionTooltip(
                true,
                true,
                title(screen),
                stack.getHoverName().getString(),
                InventoryChromeRuntime.loreLines(stack),
                extras.stallSellThreshold)) {
            lines.add(Component.literal(line));
        }
    }

    public static Integer slotHighlightArgb(AbstractContainerScreen<?> screen, Slot slot) {
        QolSkyblockExtras extras = extras();
        if (!extras.stallMarketEnabled || screen == null || slot == null || !slot.hasItem()) {
            return null;
        }
        String title = title(screen);
        int size = containerSize(screen);
        if (extras.stallBinOverlay
                && StallMarketPolicy.isBinAuction(title, size)
                && StallMarketPolicy.shouldHighlightBinSlot(auctionStatus, slot.index)) {
            return 0x6600FF55;
        }
        if (extras.stallAhHighlight && title.toLowerCase(Locale.ROOT).contains("auction")) {
            double listing = StallMarketPolicy.listingPrice(
                    InventoryChromeRuntime.loreLines(slot.getItem()));
            String itemId = SkyBlockItemData.marketId(slot.getItem());
            double lowest = SkyBlockMarketQuoteService.current().quote(itemId).lowestBin();
            return switch (StallMarketPolicy.listingHighlight(listing, lowest)) {
                case UNDER -> 0x6600FF55;
                case OVER -> 0x66FF3333;
                case NONE -> null;
            };
        }
        return null;
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        QolSkyblockExtras extras = qol.extras();
        if (!extras.stallMarketEnabled || !extras.stallBinOverlay) {
            return List.of();
        }
        if (!(currentScreen() instanceof AbstractContainerScreen<?> screen)
                || !StallMarketPolicy.isBinAuction(title(screen), containerSize(screen))) {
            return List.of();
        }
        return StallMarketPolicy.binOverlayLines(auctionStatus, auctionItemName);
    }

    static boolean hudVisible(QolUtilityConfig qol) {
        return qol.extras().stallMarketEnabled && qol.extras().stallBinOverlay;
    }

    private static void searchHoveredOrHeld(Minecraft client) {
        String name = "";
        if (currentScreen() instanceof AbstractContainerScreen<?> screen) {
            Slot hovered = readHoveredSlot(screen);
            if (hovered != null && hovered.hasItem()) {
                name = hovered.getItem().getHoverName().getString();
            }
        }
        if (name.isBlank() && client.player != null) {
            name = client.player.getMainHandItem().getHoverName().getString();
        }
        search(name);
    }

    private static void syncBinStatus(AbstractContainerScreen<?> screen) {
        if (!StallMarketPolicy.isBinAuction(title(screen), containerSize(screen))) {
            return;
        }
        if (StallMarketPolicy.isBinAuctionConfirm(title(screen), containerSize(screen))) {
            auctionStatus = StallMarketPolicy.AuctionStatus.AUCTION_CONFIRMING;
        }
        ItemStack item = stackAt(screen, StallMarketPolicy.AUCTION_ITEM_SLOT);
        if (!item.isEmpty()) {
            auctionItemName = item.getHoverName().getString();
        }
        int action = StallMarketPolicy.actionSlot(auctionStatus);
        ItemStack actionStack = stackAt(screen, action);
        if (!actionStack.isEmpty()) {
            auctionStatus = StallMarketPolicy.updateAuctionStatus(
                    auctionStatus,
                    itemPath(actionStack),
                    actionStack.getHoverName().getString(),
                    loreText(actionStack));
        }
    }

    private static boolean hasForeignEntry(
            AbstractContainerScreen<?> screen,
            String player,
            StallMarketPolicy.CoopScreen mode) {
        LocalPlayer local = Minecraft.getInstance().player;
        for (Slot slot : screen.getMenu().slots) {
            if (local != null && slot.container == local.getInventory()) {
                continue;
            }
            if (!slot.hasItem()) {
                continue;
            }
            if (StallMarketPolicy.foreignActor(
                    InventoryChromeRuntime.loreLines(slot.getItem()),
                    player,
                    mode).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static List<StallMarketPolicy.SlotView> slots(AbstractContainerScreen<?> screen) {
        List<StallMarketPolicy.SlotView> out = new ArrayList<>();
        int size = Math.min(containerSize(screen), screen.getMenu().slots.size());
        for (int i = 0; i < size; i++) {
            ItemStack stack = screen.getMenu().slots.get(i).getItem();
            out.add(new StallMarketPolicy.SlotView(
                    i,
                    itemPath(stack),
                    stack.isEmpty() ? "" : stack.getHoverName().getString(),
                    InventoryChromeRuntime.loreLines(stack)));
        }
        return out;
    }

    private static void maybeClickPendingSearch(AbstractContainerScreen<?> container) {
        QolSkyblockExtras extras = extras();
        if (!extras.stallBazaarSearch
                || pendingBazaarSearch == null
                || pendingSearchClicked
                || !StallMarketPolicy.isBazaar(title(container), containerSize(container))) {
            return;
        }
        int slot = StallMarketPolicy.findSearchSlot(slots(container));
        if (slot < 0) {
            return;
        }
        pendingSearchClicked = true;
        clickSlot(container, slot);
    }

    private static void clickSlot(AbstractContainerScreen<?> screen, int slotIndex) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= screen.getMenu().slots.size()) {
            return;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slotIndex,
                0,
                ContainerInput.PICKUP,
                player);
    }

    private static Slot readHoveredSlot(AbstractContainerScreen<?> screen) {
        if (screen instanceof AbstractContainerScreenAccessor accessor) {
            return accessor.rotclient$hoveredSlot();
        }
        return null;
    }

    private static ItemStack stackAt(AbstractContainerScreen<?> screen, int index) {
        if (index < 0 || index >= screen.getMenu().slots.size()) {
            return ItemStack.EMPTY;
        }
        return screen.getMenu().slots.get(index).getItem();
    }

    private static int containerSize(AbstractContainerScreen<?> screen) {
        int slots = screen.getMenu().slots.size();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return slots;
        }
        return Math.max(0, slots - player.getInventory().getContainerSize());
    }

    private static String title(AbstractContainerScreen<?> screen) {
        return screen.getTitle() == null ? "" : screen.getTitle().getString();
    }

    private static String itemPath(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
    }

    private static String loreText(ItemStack stack) {
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Component line : lore.lines()) {
            out.append(line.getString()).append('\n');
        }
        return out.toString();
    }

    public static boolean controlHeld() {
        return ctrlHeld();
    }

    private static boolean ctrlHeld() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) {
            return false;
        }
        long window = client.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private static String playerName() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? "" : player.getGameProfile().name().toLowerCase(Locale.ROOT);
    }

    private static void sendCommand(String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) {
            return;
        }
        player.connection.sendCommand(command);
    }

    private static void chat(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendSystemMessage(Component.literal(message));
        }
    }

    private static Screen currentScreen() {
        Minecraft client = Minecraft.getInstance();
        return client == null || client.gui == null ? null : client.gui.screen();
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
