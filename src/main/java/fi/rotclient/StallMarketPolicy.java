package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bazaar and auction-house decision policy. It has no network transport and
 * only evaluates the local Serveri GUI state.
 */
public final class StallMarketPolicy {
    public static final int BAZAAR_SIZE = 54;
    public static final int BIN_VIEW_SIZE = 54;
    public static final int BIN_CONFIRM_SIZE = 27;
    public static final int AUCTION_ITEM_SLOT = 13;
    public static final int AUCTION_BUY_SLOT = 31;
    public static final int AUCTION_CANCEL_SLOT = 49;
    public static final int AUCTION_CONFIRM_SLOT = 11;
    public static final int AUCTION_CONFIRM_CANCEL_SLOT = 15;
    public static final long DEFAULT_SELL_THRESHOLD = 1_000_000L;
    public static final long UNPARSED_AMOUNT = Long.MAX_VALUE;

    public enum AuctionStatus {
        INIT,
        AUCTION_SOLD,
        AUCTION_WAITING,
        AUCTION_BUYING,
        AUCTION_CONFIRMING,
        OWN_AUCTION_CLAIMING,
        OWN_AUCTION_CANCELING
    }

    public enum CoopScreen {
        SELLER,
        BIDDER
    }

    public enum HighlightKind {
        NONE,
        UNDER,
        OVER
    }

    public record SlotView(int index, String itemPath, String name, List<String> lore) {
        public SlotView {
            name = name == null ? "" : name;
            itemPath = itemPath == null ? "" : itemPath;
            lore = lore == null ? List.of() : List.copyOf(lore);
        }
    }

    public record SellBlock(boolean block, String buttonName, long coins, String message) {
        public static SellBlock allow() {
            return new SellBlock(false, "", 0L, "");
        }
    }

    public record CoopBlock(boolean block, String message) {
        public static CoopBlock allow() {
            return new CoopBlock(false, "");
        }
    }

    private static final Pattern CONTROL = Pattern.compile("§.");
    private static final Pattern SELL_INSTANTLY =
            Pattern.compile("Total: ([0-9,]+(?:\\.[0-9]+)?) coins");
    private static final Pattern INVENTORY_SACK =
            Pattern.compile("You earn: ([0-9,]+(?:\\.[0-9]+)?) coins");
    private static final Pattern BIN_PRICE = Pattern.compile(
            "(?i)(?:buy it now|bin|price)[:\\s]+([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*coins?");
    private static final Pattern AMOUNT =
            Pattern.compile("^([0-9]+(?:\\.[0-9]+)?)([kmb])?$");

    private StallMarketPolicy() {
    }

    public static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length());
        boolean code = false;
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '§') {
                code = true;
            } else if (code) {
                code = false;
            } else {
                out.append(ch);
            }
        }
        return out.toString().trim();
    }

    public static boolean isBazaar(String title, int containerSize) {
        return strip(title).contains("Bazaar") && containerSize == BAZAAR_SIZE;
    }

    public static boolean isBinAuctionInit(String title, int containerSize) {
        return strip(title).contains("BIN Auction View") && containerSize == BIN_VIEW_SIZE;
    }

    public static boolean isBinAuctionConfirm(String title, int containerSize) {
        return strip(title).contains("Confirm Purchase") && containerSize == BIN_CONFIRM_SIZE;
    }

    public static boolean isBinAuction(String title, int containerSize) {
        return isBinAuctionInit(title, containerSize) || isBinAuctionConfirm(title, containerSize);
    }

    public static boolean isSellMenu(String title) {
        return strip(title).contains("➜");
    }

    public static Optional<CoopScreen> coopScreen(String title) {
        String text = strip(title).toLowerCase(Locale.ROOT);
        if (text.contains("manage auctions")) {
            return Optional.of(CoopScreen.SELLER);
        }
        if (text.contains("your bids")) {
            return Optional.of(CoopScreen.BIDDER);
        }
        return Optional.empty();
    }

    public static String cleanItemNameForSearch(String rawName) {
        if (rawName == null) {
            return "";
        }
        String cleaned = CONTROL.matcher(rawName).replaceAll("");
        cleaned = cleaned.replaceAll("\\[.*?\\]", "");
        cleaned = cleaned.replaceAll("\\(.*?\\)", "");
        return cleaned.trim();
    }

    public static int findSearchSlot(List<SlotView> slots) {
        if (slots == null) {
            return -1;
        }
        for (SlotView slot : slots) {
            if (isNamedSearchTool(slot)) {
                return slot.index();
            }
        }
        for (SlotView slot : slots) {
            if (nameContains(slot, "search")) {
                return slot.index();
            }
            if (loreContains(slot, "search")
                    && (loreContains(slot, "click") || loreContains(slot, "use"))) {
                return slot.index();
            }
        }
        return -1;
    }

    public static List<String> visibleBazaarItems(List<SlotView> slots) {
        List<String> names = new ArrayList<>();
        if (slots == null) {
            return List.of();
        }
        for (SlotView slot : slots) {
            if (slot.name().isBlank() || isGlassPane(slot.itemPath())) {
                continue;
            }
            if (slot.name().contains("Search")
                    || slot.name().contains("Go Back")
                    || slot.name().contains("Close")) {
                continue;
            }
            names.add(slot.name());
        }
        return List.copyOf(names);
    }

    public static long extractSellInstantlyAmount(List<String> lines) {
        if (lines == null) {
            return UNPARSED_AMOUNT;
        }
        for (String raw : lines) {
            String text = strip(raw);
            if (text.contains("You don't have anything to sell")
                    || text.contains("None to sell in your inventory")) {
                return 0L;
            }
            Matcher matcher = SELL_INSTANTLY.matcher(text);
            if (matcher.find()) {
                return parsePlainAmount(matcher.group(1));
            }
        }
        return UNPARSED_AMOUNT;
    }

    public static long extractInventorySackAmount(List<String> lines) {
        if (lines == null) {
            return UNPARSED_AMOUNT;
        }
        for (String raw : lines) {
            String text = strip(raw);
            if (text.contains("You don't have anything to sell")) {
                return 0L;
            }
            Matcher matcher = INVENTORY_SACK.matcher(text);
            if (matcher.find()) {
                return parsePlainAmount(matcher.group(1));
            }
        }
        return UNPARSED_AMOUNT;
    }

    public static SellBlock sellProtection(
            boolean moduleEnabled,
            boolean protectionEnabled,
            String screenTitle,
            String itemName,
            List<String> lore,
            int mouseButton,
            boolean ctrlHeld,
            long threshold) {
        if (!moduleEnabled || !protectionEnabled || !isSellMenu(screenTitle)) {
            return SellBlock.allow();
        }
        if (mouseButton != 0 && mouseButton != 1) {
            return SellBlock.allow();
        }
        String name = strip(itemName);
        long max = Math.max(0L, threshold);
        if (name.contains("Sell Instantly")) {
            long amount = extractSellInstantlyAmount(lore);
            if (mouseButton == 0 && !ctrlHeld && amount > max && amount != UNPARSED_AMOUNT) {
                return new SellBlock(
                        true,
                        "Sell Instantly",
                        amount,
                        "§c[Stall Sell Protection] §fBlocked click on §eSell Instantly§f (§6"
                                + formatCoins(amount)
                                + " coins§f)! Hold §bCtrl§f to override.");
            }
            return SellBlock.allow();
        }
        if (name.contains("Sell Sacks Now") || name.contains("Sell Inventory Now")) {
            long amount = extractInventorySackAmount(lore);
            if (!ctrlHeld && amount > max && amount != UNPARSED_AMOUNT) {
                String button = name.contains("Sell Sacks Now") ? "Sell Sacks Now" : "Sell Inventory Now";
                return new SellBlock(
                        true,
                        button,
                        amount,
                        "§c[Stall Sell Protection] §fBlocked click on §e"
                                + button
                                + "§f (§6"
                                + formatCoins(amount)
                                + " coins§f)! Hold §bCtrl§f to override.");
            }
        }
        return SellBlock.allow();
    }

    public static List<String> sellProtectionTooltip(
            boolean moduleEnabled,
            boolean protectionEnabled,
            String screenTitle,
            String itemName,
            List<String> lore,
            long threshold) {
        List<String> out = new ArrayList<>();
        if (!moduleEnabled || !protectionEnabled || !isSellMenu(screenTitle)) {
            return List.of();
        }
        String name = strip(itemName);
        long max = Math.max(0L, threshold);
        long amount = UNPARSED_AMOUNT;
        boolean warn = false;
        if (name.contains("Sell Instantly")) {
            amount = extractSellInstantlyAmount(lore);
            warn = amount > max && amount != UNPARSED_AMOUNT;
            if (warn) {
                out.add("");
                out.add("§c⚠ §lSell Protection §c⚠");
                out.add("§7Left clicks blocked if > §6" + formatCoins(max) + " coins");
                out.add("§bHold Ctrl§7 to override.");
            }
        } else if (name.contains("Sell Sacks Now") || name.contains("Sell Inventory Now")) {
            amount = extractInventorySackAmount(lore);
            warn = amount > max && amount != UNPARSED_AMOUNT;
            if (warn) {
                out.add("");
                out.add("§c⚠ §lSell Protection §c⚠");
                out.add("§7All clicks blocked if > §6" + formatCoins(max) + " coins");
                out.add("§bHold Ctrl§7 to override.");
            }
        }
        return List.copyOf(out);
    }

    public static boolean isClaimAll(String name) {
        return strip(name).equalsIgnoreCase("Claim All");
    }

    public static Optional<String> foreignActor(
            List<String> lore,
            String playerNameLower,
            CoopScreen mode) {
        if (lore == null || mode == null) {
            return Optional.empty();
        }
        String self = playerNameLower == null ? "" : playerNameLower.toLowerCase(Locale.ROOT);
        for (String raw : lore) {
            String line = strip(raw);
            String lower = line.toLowerCase(Locale.ROOT);
            if (mode == CoopScreen.SELLER) {
                if (lower.contains("this is your own auction")) {
                    return Optional.empty();
                }
                if (lower.startsWith("seller:")) {
                    if (!self.isBlank() && lower.contains(self)) {
                        return Optional.empty();
                    }
                    String name = line.substring("Seller:".length()).trim();
                    return Optional.of(name.isEmpty() ? "Unknown" : name);
                }
            } else {
                if (lower.startsWith("bidder:")) {
                    if (lower.startsWith("bidder: you")) {
                        return Optional.empty();
                    }
                    if (!self.isBlank() && lower.contains(self)) {
                        return Optional.empty();
                    }
                    String name = line.substring("Bidder:".length()).trim();
                    return Optional.of(name.isEmpty() ? "Unknown" : name);
                }
                if (lower.contains("you are the highest bidder")) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public static CoopBlock coopProtection(
            boolean moduleEnabled,
            boolean protectionEnabled,
            String screenTitle,
            String hoveredName,
            List<String> hoveredLore,
            boolean anyForeignInMenu,
            String foreignName,
            boolean ctrlHeld) {
        if (!moduleEnabled || !protectionEnabled || ctrlHeld) {
            return CoopBlock.allow();
        }
        Optional<CoopScreen> screen = coopScreen(screenTitle);
        if (screen.isEmpty()) {
            return CoopBlock.allow();
        }
        CoopScreen mode = screen.get();
        if (isClaimAll(hoveredName) && anyForeignInMenu) {
            String kind = mode == CoopScreen.SELLER
                    ? "auctions listed by co-op members"
                    : "bids placed by co-op members";
            return new CoopBlock(
                    true,
                    "§c[Stall Angry Coop] §fBlocked Claim All because "
                            + kind
                            + " were detected. Hold §bCtrl§f to override.");
        }
        if (foreignName != null && !foreignName.isBlank()) {
            String listed = mode == CoopScreen.SELLER ? "listed by" : "bid on by";
            return new CoopBlock(
                    true,
                    "§c[Stall Angry Coop] §fBlocked claiming auction "
                            + listed
                            + " §e"
                            + foreignName
                            + "§f. Hold §bCtrl§f to override.");
        }
        if (hoveredLore != null) {
            Optional<String> actor = foreignActor(hoveredLore, "", mode);
            if (actor.isPresent()) {
                String listed = mode == CoopScreen.SELLER ? "listed by" : "bid on by";
                return new CoopBlock(
                        true,
                        "§c[Stall Angry Coop] §fBlocked claiming auction "
                                + listed
                                + " §e"
                                + actor.get()
                                + "§f. Hold §bCtrl§f to override.");
            }
        }
        return CoopBlock.allow();
    }

    public static AuctionStatus updateAuctionStatus(
            AuctionStatus current,
            String itemPath,
            String customName,
            String loreText) {
        AuctionStatus status = current == null ? AuctionStatus.INIT : current;
        if (itemPath != null && itemPath.contains("black_stained_glass_pane")) {
            return AuctionStatus.OWN_AUCTION_CANCELING;
        }
        if (itemPath != null && itemPath.contains("red_bed")) {
            return AuctionStatus.AUCTION_WAITING;
        }
        if (customName == null || customName.isBlank()) {
            return status;
        }
        String name = strip(customName);
        String lore = loreText == null ? "" : loreText;
        return switch (name) {
            case "Buy Item Right Now" ->
                    lore.contains("Cannot afford bid!") || lore.contains("Click to purchase!")
                            ? AuctionStatus.AUCTION_BUYING
                            : status;
            case "Collect Auction" -> {
                if (lore.contains("Click to collect coins!")
                        || lore.contains("Click to pick up item!")) {
                    yield AuctionStatus.OWN_AUCTION_CLAIMING;
                }
                if (lore.contains("Someone else purchased the item")) {
                    yield AuctionStatus.AUCTION_SOLD;
                }
                yield status;
            }
            case "Confirm" -> AuctionStatus.AUCTION_CONFIRMING;
            default -> status;
        };
    }

    public static int actionSlot(AuctionStatus status) {
        return status == AuctionStatus.AUCTION_CONFIRMING
                ? AUCTION_CONFIRM_SLOT
                : AUCTION_BUY_SLOT;
    }

    public static boolean shouldHighlightBinSlot(AuctionStatus status, int slot) {
        if (status == AuctionStatus.AUCTION_CONFIRMING) {
            return slot == AUCTION_CONFIRM_SLOT || slot == AUCTION_CONFIRM_CANCEL_SLOT;
        }
        if (status == AuctionStatus.AUCTION_BUYING || status == AuctionStatus.INIT) {
            return slot == AUCTION_BUY_SLOT
                    || slot == AUCTION_CANCEL_SLOT
                    || slot == AUCTION_ITEM_SLOT;
        }
        if (status == AuctionStatus.OWN_AUCTION_CLAIMING) {
            return slot == AUCTION_BUY_SLOT;
        }
        return slot == AUCTION_ITEM_SLOT;
    }

    public static HighlightKind listingHighlight(double listingPrice, double lowestBin) {
        if (!(listingPrice > 0.0D) || !(lowestBin > 0.0D)) {
            return HighlightKind.NONE;
        }
        if (listingPrice < lowestBin * 0.95D) {
            return HighlightKind.UNDER;
        }
        if (listingPrice > lowestBin * 1.05D) {
            return HighlightKind.OVER;
        }
        return HighlightKind.NONE;
    }

    public static double listingPrice(List<String> lore) {
        if (lore == null) {
            return 0.0D;
        }
        for (String raw : lore) {
            Matcher matcher = BIN_PRICE.matcher(strip(raw));
            if (matcher.find()) {
                return parseDouble(matcher.group(1));
            }
        }
        return 0.0D;
    }

    public static long parseAmountString(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            throw new NumberFormatException("Empty amount string");
        }
        String value = amountStr.trim().toLowerCase(Locale.ROOT);
        Matcher matcher = AMOUNT.matcher(value);
        if (!matcher.matches()) {
            throw new NumberFormatException("Invalid format: " + amountStr);
        }
        double number = Double.parseDouble(matcher.group(1));
        String suffix = matcher.group(2);
        if (suffix == null) {
            return (long) number;
        }
        return switch (suffix.charAt(0)) {
            case 'k' -> (long) (number * 1000.0D);
            case 'm' -> (long) (number * 1_000_000.0D);
            case 'b' -> (long) (number * 1_000_000_000.0D);
            default -> throw new NumberFormatException("Invalid suffix: " + suffix);
        };
    }

    public static long clampSellThreshold(long amount) {
        if (amount < 0L) {
            return 0L;
        }
        return Math.min(amount, 2_000_000_000L);
    }

    public static String formatCoins(long coins) {
        if (coins >= 1_000_000_000L) {
            return String.format(Locale.US, "%.1fB", coins / 1.0E9);
        }
        if (coins >= 1_000_000L) {
            return String.format(Locale.US, "%.1fM", coins / 1_000_000.0);
        }
        if (coins >= 1_000L) {
            return String.format(Locale.US, "%.1fK", coins / 1000.0);
        }
        return String.valueOf(coins);
    }

    public static List<String> binOverlayLines(AuctionStatus status, String itemName) {
        List<String> lines = new ArrayList<>();
        lines.add("§6BIN Auction");
        if (itemName != null && !itemName.isBlank()) {
            lines.add("§f" + strip(itemName));
        }
        lines.add(statusLabel(status));
        return List.copyOf(lines);
    }

    public static String statusLabel(AuctionStatus status) {
        if (status == null) {
            return "§7Waiting";
        }
        return switch (status) {
            case INIT -> "§eBIN View";
            case AUCTION_BUYING -> "§aClick Buy";
            case AUCTION_CONFIRMING -> "§aConfirm Purchase";
            case AUCTION_WAITING -> "§cWaiting";
            case AUCTION_SOLD -> "§cSold";
            case OWN_AUCTION_CLAIMING -> "§eCollect";
            case OWN_AUCTION_CANCELING -> "§cCancel";
        };
    }

    private static boolean isNamedSearchTool(SlotView slot) {
        String path = slot.itemPath().toLowerCase(Locale.ROOT);
        boolean tool = path.contains("name_tag")
                || path.contains("paper")
                || path.contains("writable_book")
                || path.contains("compass");
        if (!tool) {
            return false;
        }
        if (nameContains(slot, "search")) {
            return true;
        }
        return loreContains(slot, "search")
                || loreContains(slot, "find")
                || loreContains(slot, "look for");
    }

    private static boolean nameContains(SlotView slot, String needle) {
        return strip(slot.name()).toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean loreContains(SlotView slot, String needle) {
        for (String line : slot.lore()) {
            if (strip(line).toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGlassPane(String itemPath) {
        return itemPath != null && itemPath.contains("stained_glass_pane");
    }

    private static long parsePlainAmount(String raw) {
        try {
            return (long) Double.parseDouble(raw.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return UNPARSED_AMOUNT;
        }
    }

    private static double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0.0D;
        }
    }
}
