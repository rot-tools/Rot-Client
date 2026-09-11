package fi.rotclient;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.List;

final class MarketWatchAlertHud {
    private static final long POPUP_LIFETIME_MILLIS =
            6_500L;

    private static final int MAX_VISIBLE =
            3;

    private static final int MAX_WAITING =
            100;

    private static final int CARD_WIDTH =
            268;

    private static final int CARD_HEIGHT =
            68;

    private static final int CARD_GAP =
            6;

    private static final int MARGIN =
            10;

    private static final ArrayDeque<MarketWatchLiveAlert> WAITING =
            new ArrayDeque<>();

    private static final ArrayDeque<VisibleAlert> VISIBLE =
            new ArrayDeque<>();

    private MarketWatchAlertHud() {
    }

    static void render(
            GuiGraphicsExtractor graphics) {

        if (graphics == null) {
            return;
        }

        Minecraft client =
                Minecraft.getInstance();

        if (client == null
                || client.font == null
                || client.getWindow() == null) {

            return;
        }


        List<MarketWatchLiveAlert> incoming =
                MarketWatchRuntime
                        .drainPendingAlerts();

        for (MarketWatchLiveAlert alert
                : incoming) {

            while (WAITING.size()
                    >= MAX_WAITING) {

                WAITING.removeFirst();
            }

            WAITING.addLast(alert);
        }

        long now =
                System.currentTimeMillis();

        while (!VISIBLE.isEmpty()) {
            VisibleAlert first =
                    VISIBLE.peekFirst();

            if (first == null
                    || now - first.shownAtMillis()
                    < POPUP_LIFETIME_MILLIS) {

                break;
            }

            VISIBLE.removeFirst();
        }

        while (VISIBLE.size()
                < MAX_VISIBLE
                && !WAITING.isEmpty()) {

            VISIBLE.addLast(
                    new VisibleAlert(
                            WAITING.removeFirst(),
                            now));
        }

        if (VISIBLE.isEmpty()) {
            return;
        }

        int screenWidth =
                client
                        .getWindow()
                        .getGuiScaledWidth();

        int width =
                Math.min(
                        CARD_WIDTH,
                        Math.max(
                                150,
                                screenWidth
                                        - MARGIN * 2));

        int x =
                Math.max(
                        MARGIN,
                        screenWidth
                                - width
                                - MARGIN);

        int y =
                MARGIN;

        for (VisibleAlert visible
                : VISIBLE) {

            drawCard(
                    graphics,
                    client.font,
                    visible.alert(),
                    x,
                    y,
                    width);

            y +=
                    CARD_HEIGHT
                            + CARD_GAP;
        }
    }

    static void clearVisuals() {
        WAITING.clear();
        VISIBLE.clear();
    }

    static String displayName(
            MarketWatchLiveAlert alert) {

        if (alert == null) {
            return "Unknown item";
        }

        if (alert.market()
                == MarketWatchLiveAlert.Market.BAZAAR) {

            String name =
                    MarketWatchItemCatalog
                            .displayName(
                                    alert.targetId());

            if (!name.isBlank()) {
                return name;
            }
        }

        if (!alert.displayName().isBlank()) {
            return alert.displayName();
        }

        if (!alert.targetId().isBlank()) {
            return alert.targetId();
        }

        return "Unknown item";
    }

    static ItemStack icon(
            MarketWatchLiveAlert alert) {

        if (alert == null) {
            return MarketWatchItemIconResolver
                    .auctionIcon(
                            "",
                            "");
        }

        if (alert.market()
                == MarketWatchLiveAlert.Market.BAZAAR) {

            return MarketWatchItemIconResolver
                    .bazaarIcon(
                            alert.targetId());
        }

        String title =
                displayName(alert);

        String category =
                "";

        for (MarketWatchAuctionWatch watch
                : MarketWatchRuntime
                .auctionWatches()) {

            if (watch == null
                    || !watch.id.equals(
                            alert.watchId())) {

                continue;
            }

            String name =
                    !watch.itemName.isBlank()
                            ? watch.itemName
                            : title;

            category =
                    MarketWatchItemCatalog
                            .auctionStats(
                                    name,
                                    watch.tier)
                            .category();

            break;
        }

        return MarketWatchItemIconResolver
                .auctionIcon(
                        category,
                        title);
    }

    static String summary(
            MarketWatchLiveAlert alert) {

        if (alert == null) {
            return "";
        }

        if (alert.market()
                == MarketWatchLiveAlert.Market.AUCTION_HOUSE) {

            StringBuilder result =
                    new StringBuilder(
                            "Price ")
                            .append(
                                    compact(
                                            alert.auctionPriceCoins()))
                            .append(
                                    " coins");

            if (alert.spreadCoins() > 0.0D) {
                result.append(
                                "  /  Possible Margin ")
                        .append(
                                compact(
                                        alert.spreadCoins()))
                        .append(
                                " coins");
            }

            if (alert.spreadPercent() > 0.0D) {
                result.append(
                                " (")
                        .append(
                                String.format(
                                        Locale.ROOT,
                                        "%.1f%%",
                                        alert.spreadPercent()))
                        .append(
                                ")");
            }

            String seller =
                    sellerSummary(
                            alert);

            if (!seller.isBlank()) {
                result.append(
                                "  /  ")
                        .append(
                                seller);
            }

            return result.toString();
        }

        StringBuilder result =
                new StringBuilder();

        if (alert.bazaarBuyPrice()
                > 0.0D) {

            result.append(
                            "Buy ")
                    .append(
                            compact(
                                    alert.bazaarBuyPrice()));
        }

        if (alert.bazaarSellPrice()
                > 0.0D) {

            if (!result.isEmpty()) {
                result.append(
                        "  /  ");
            }

            result.append(
                            "Sell ")
                    .append(
                            compact(
                                    alert.bazaarSellPrice()));
        }

        if (result.isEmpty()
                && alert.spreadCoins()
                > 0.0D) {

            result.append(
                            "Spread ")
                    .append(
                            compact(
                                    alert.spreadCoins()));
        }

        return result.isEmpty()
                ? "Bazaar deal matched"
                : result.toString();
    }

    static String conditionSummary(
            MarketWatchLiveAlert alert) {

        if (alert == null) {
            return "";
        }

        if (alert.market()
                == MarketWatchLiveAlert.Market.AUCTION_HOUSE) {

            for (MarketWatchAuctionWatch watch
                    : MarketWatchRuntime
                    .auctionWatches()) {

                if (watch == null
                        || !watch.id.equals(
                                alert.watchId())) {

                    continue;
                }

                if (watch.minDiscountPercent
                        > 0.0D) {

                    return "Dynamic "
                            + trimmed(
                                    watch.minDiscountPercent)
                            + "% below market";
                }

                if (watch.maxPriceCoins
                        > 0L) {

                    return "Fixed max "
                            + compact(
                                    watch.maxPriceCoins)
                            + " coins";
                }

                return "";
            }

            return "";
        }

        for (MarketWatchBazaarWatch watch
                : MarketWatchRuntime
                .bazaarWatches()) {

            if (watch == null
                    || !watch.id.equals(
                            alert.watchId())) {

                continue;
            }

            if (watch.buyDealPercent
                    > 0.0D) {

                return "Dynamic "
                        + trimmed(
                                watch.buyDealPercent)
                        + "% below market";
            }

            if (watch.maxInstantBuyPrice
                    > 0.0D) {

                return "Fixed max "
                        + compact(
                                watch.maxInstantBuyPrice);
            }

            return "";
        }

        return "";
    }

    static String sellerSummary(
            MarketWatchLiveAlert alert) {

        if (alert == null
                || alert.market()
                != MarketWatchLiveAlert.Market.AUCTION_HOUSE
                || alert.sellerUuid().isBlank()) {

            return "";
        }

        String sellerName =
                MarketWatchSellerNameService
                        .displayName(
                                alert.sellerUuid());

        if (sellerName.isBlank()) {
            return "";
        }

        return "Seller "
                + sellerName;
    }

    private static void drawCard(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchLiveAlert alert,
            int x,
            int y,
            int width) {

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                CARD_HEIGHT);

        int accent =
                alert.market()
                        == MarketWatchLiveAlert.Market.BAZAAR
                        ? RotClientTheme.VIOLET
                        : RotClientTheme.HUD_ACCENT;

        graphics.fill(
                x,
                y,
                x + 3,
                y + CARD_HEIGHT,
                accent);

        graphics.item(
                icon(alert),
                x + 10,
                y + 26);

        String market =
                alert.market()
                        == MarketWatchLiveAlert.Market.BAZAAR
                        ? "BAZAAR ALERT"
                        : "AUCTION HOUSE ALERT";

        RotClientUiDraw.text(
                graphics,
                font,
                market,
                x + 36,
                y + 7,
                accent,
                true);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        displayName(
                                alert),
                        Math.max(
                                20,
                                width - 48)),
                x + 36,
                y + 21,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                fit(
                        font,
                        summary(
                                alert),
                        Math.max(
                                20,
                                width - 48)),
                x + 36,
                y + 38);

        RotClientUiDraw.helpText(
                graphics,
                font,
                fit(
                        font,
                        conditionSummary(
                                alert),
                        Math.max(
                                20,
                                width - 48)),
                x + 36,
                y + 52);
    }

    private static String fit(
            Font font,
            String value,
            int maxWidth) {

        if (value == null
                || value.isBlank()
                || maxWidth <= 0) {

            return "";
        }

        if (font.width(value)
                <= maxWidth) {

            return value;
        }

        String suffix = "...";

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
                i < value.length();
                i++) {

            result.append(
                    value.charAt(i));

            if (font.width(
                    result.toString()
                            + suffix)
                    > maxWidth) {

                result.setLength(
                        Math.max(
                                0,
                                result.length() - 1));

                break;
            }
        }

        return result
                + suffix;
    }

    private static String compact(
            long value) {

        return compact(
                (double) value);
    }

    private static String compact(
            double value) {

        if (!Double.isFinite(value)
                || value <= 0.0D) {

            return "-";
        }

        if (value >= 1_000_000_000.0D) {
            return trimmed(
                    value / 1_000_000_000.0D)
                    + "b";
        }

        if (value >= 1_000_000.0D) {
            return trimmed(
                    value / 1_000_000.0D)
                    + "m";
        }

        if (value >= 1_000.0D) {
            return trimmed(
                    value / 1_000.0D)
                    + "k";
        }

        return trimmed(value);
    }

    private static String trimmed(
            double value) {

        return BigDecimal
                .valueOf(value)
                .setScale(
                        value >= 100.0D
                                ? 0
                                : 2,
                        RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    private record VisibleAlert(
            MarketWatchLiveAlert alert,
            long shownAtMillis) {
    }
}