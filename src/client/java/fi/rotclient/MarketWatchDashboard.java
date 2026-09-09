package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

final class MarketWatchDashboard {
    private enum Page {
        AUCTION_HOUSE,
        BAZAAR
    }

    private Page page = Page.AUCTION_HOUSE;

    void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY) {

        int width = Math.max(1, right - left);

        boolean enabled =
                MarketWatchRuntime.enabled();

        List<MarketWatchAuctionWatch> auctionWatches =
                MarketWatchRuntime.auctionWatches();

        List<MarketWatchBazaarWatch> bazaarWatches =
                MarketWatchRuntime.bazaarWatches();

        MarketWatchDataService.AuctionState auctionState =
                MarketWatchDataService.currentAuctions();

        MarketWatchDataService.BazaarState bazaarState =
                MarketWatchDataService.currentBazaar();

        RotClientUiDraw.pageTitle(
                graphics,
                font,
                "MARKET WATCH",
                left,
                top);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Live Auction House and Bazaar monitoring. Alerts only - trading stays manual.",
                left,
                top + 14);

        RotClientUiDraw.drawStatusPill(
                graphics,
                font,
                right,
                top,
                enabled ? "ACTIVE" : "DISABLED",
                enabled
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_MUTED);

        int masterY = top + 34;
        int masterH = 58;

        RotClientUiDraw.drawElevatedCard(
                graphics,
                left,
                masterY,
                width,
                masterH);

        RotClientUiDraw.text(
                graphics,
                font,
                "Market Watch",
                left + 14,
                masterY + 12,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                enabled
                        ? "Monitoring configured watches and applying alert cooldowns."
                        : "Turn on to evaluate saved watches against fresh market snapshots.",
                left + 14,
                masterY + 27);

        int toggleX =
                right
                        - QolUtilityUiMath.TOGGLE_WIDTH
                        - 14;

        int toggleY =
                masterY
                        + (masterH
                        - QolUtilityUiMath.TOGGLE_HEIGHT)
                        / 2;

        boolean toggleHover =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        toggleX - 6,
                        toggleY - 6,
                        QolUtilityUiMath.TOGGLE_WIDTH + 12,
                        QolUtilityUiMath.TOGGLE_HEIGHT + 12);

        RotClientUiDraw.drawToggle(
                graphics,
                toggleX,
                toggleY,
                enabled,
                toggleHover);

        int metricsY = masterY + masterH + 14;
        int gap = 10;
        int metricWidth =
                Math.max(
                        90,
                        (width - gap * 2) / 3);

        drawMetric(
                graphics,
                font,
                left,
                metricsY,
                metricWidth,
                "AUCTION HOUSE",
                auctionState.available()
                        ? formatCount(
                                auctionState.snapshot()
                                        .auctions()
                                        .size())
                        : "--",
                auctionState.available()
                        ? "LIVE SNAPSHOT"
                        : "WAITING FOR DATA",
                auctionState.available());

        drawMetric(
                graphics,
                font,
                left + metricWidth + gap,
                metricsY,
                metricWidth,
                "BAZAAR",
                bazaarState.available()
                        ? formatCount(
                                bazaarState.snapshot()
                                        .products()
                                        .size())
                        : "--",
                bazaarState.available()
                        ? "LIVE PRODUCTS"
                        : "WAITING FOR DATA",
                bazaarState.available());

        drawMetric(
                graphics,
                font,
                left + (metricWidth + gap) * 2,
                metricsY,
                right
                        - (left
                        + (metricWidth + gap) * 2),
                "SAVED WATCHES",
                Integer.toString(
                        auctionWatches.size()
                                + bazaarWatches.size()),
                auctionWatches.size()
                        + " AH / "
                        + bazaarWatches.size()
                        + " BZ",
                enabled);

        int tabsY = metricsY + 70;

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                left,
                tabsY,
                128,
                "AUCTION HOUSE",
                page == Page.AUCTION_HOUSE,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                left + 138,
                tabsY,
                92,
                "BAZAAR",
                page == Page.BAZAAR,
                true);

        List<?> watches =
                page == Page.AUCTION_HOUSE
                        ? auctionWatches
                        : bazaarWatches;

        String section =
                page == Page.AUCTION_HOUSE
                        ? "AUCTION HOUSE WATCHES"
                        : "BAZAAR WATCHES";

        int sectionY = tabsY + 40;

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                section,
                left,
                sectionY);

        RotClientUiDraw.drawStatusPill(
                graphics,
                font,
                right,
                sectionY - 4,
                watches.size() == 1
                        ? "1 WATCH"
                        : watches.size() + " WATCHES",
                watches.isEmpty()
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.HUD_ACCENT);

        int listY = sectionY + 20;

        if (watches.isEmpty()) {
            RotClientUiDraw.drawElevatedCard(
                    graphics,
                    left,
                    listY,
                    width,
                    62);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "No watches configured",
                    left + 14,
                    listY + 13,
                    RotClientTheme.TEXT,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    page == Page.AUCTION_HOUSE
                            ? "Add an AH watch to monitor a specific item and price."
                            : "Add a Bazaar watch to monitor price, spread, and liquidity.",
                    left + 14,
                    listY + 29);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Watch creation and editing controls are the next UI step.",
                    left + 14,
                    listY + 43);

            return;
        }

        int availableHeight =
                Math.max(
                        0,
                        bottom - listY - 8);

        int rowStep = 50;
        int maxRows =
                Math.max(
                        1,
                        availableHeight / rowStep);

        int shown =
                Math.min(
                        watches.size(),
                        maxRows);

        for (int i = 0; i < shown; i++) {
            int rowY =
                    listY + i * rowStep;

            RotClientUiDraw.drawElevatedCard(
                    graphics,
                    left,
                    rowY,
                    width,
                    42);

            if (page == Page.AUCTION_HOUSE) {
                MarketWatchAuctionWatch watch =
                        auctionWatches.get(i);

                String title =
                        !watch.itemName.isBlank()
                                ? watch.itemName
                                : watch.itemId;

                RotClientUiDraw.text(
                        graphics,
                        font,
                        title.isBlank()
                                ? "Unnamed Auction Watch"
                                : title,
                        left + 12,
                        rowY + 8,
                        watch.enabled
                                ? RotClientTheme.TEXT
                                : RotClientTheme.TEXT_MUTED,
                        true);

                RotClientUiDraw.helpText(
                        graphics,
                        font,
                        auctionSummary(watch),
                        left + 12,
                        rowY + 23);

                RotClientUiDraw.drawStatusPill(
                        graphics,
                        font,
                        right - 10,
                        rowY + 13,
                        watch.enabled ? "ON" : "OFF",
                        watch.enabled
                                ? RotClientTheme.SUCCESS
                                : RotClientTheme.TEXT_MUTED);
            } else {
                MarketWatchBazaarWatch watch =
                        bazaarWatches.get(i);

                RotClientUiDraw.text(
                        graphics,
                        font,
                        watch.productId.isBlank()
                                ? "Unnamed Bazaar Watch"
                                : watch.productId,
                        left + 12,
                        rowY + 8,
                        watch.enabled
                                ? RotClientTheme.TEXT
                                : RotClientTheme.TEXT_MUTED,
                        true);

                RotClientUiDraw.helpText(
                        graphics,
                        font,
                        bazaarSummary(watch),
                        left + 12,
                        rowY + 23);

                RotClientUiDraw.drawStatusPill(
                        graphics,
                        font,
                        right - 10,
                        rowY + 13,
                        watch.enabled ? "ON" : "OFF",
                        watch.enabled
                                ? RotClientTheme.SUCCESS
                                : RotClientTheme.TEXT_MUTED);
            }
        }

        if (watches.size() > shown) {
            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "+"
                            + (watches.size() - shown)
                            + " more watches",
                    left,
                    listY + shown * rowStep + 2);
        }
    }

    boolean mouseClicked(
            int button,
            double mouseX,
            double mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        int mx =
                (int) Math.round(mouseX);

        int my =
                (int) Math.round(mouseY);

        int masterY = top + 34;

        int toggleX =
                right
                        - QolUtilityUiMath.TOGGLE_WIDTH
                        - 14;

        int toggleY =
                masterY
                        + (58
                        - QolUtilityUiMath.TOGGLE_HEIGHT)
                        / 2;

        if (RotClientUiDraw.inside(
                mx,
                my,
                toggleX - 8,
                toggleY - 8,
                QolUtilityUiMath.TOGGLE_WIDTH + 16,
                QolUtilityUiMath.TOGGLE_HEIGHT + 16)) {

            MarketWatchRuntime.setEnabled(
                    !MarketWatchRuntime.enabled());

            return true;
        }

        int tabsY =
                masterY
                        + 58
                        + 14
                        + 70;

        if (RotClientUiDraw.inside(
                mx,
                my,
                left,
                tabsY,
                128,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            page = Page.AUCTION_HOUSE;
            return true;
        }

        if (RotClientUiDraw.inside(
                mx,
                my,
                left + 138,
                tabsY,
                92,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            page = Page.BAZAAR;
            return true;
        }

        return false;
    }

    private static void drawMetric(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String title,
            String value,
            String hint,
            boolean healthy) {

        RotClientUiDraw.drawMetricCard(
                graphics,
                font,
                x,
                y,
                Math.max(60, width),
                title,
                value,
                hint,
                healthy
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_MUTED);
    }

    private static String auctionSummary(
            MarketWatchAuctionWatch watch) {

        StringBuilder summary =
                new StringBuilder();

        if (!watch.tier.isBlank()) {
            summary.append(watch.tier);
        }

        if (watch.binOnly) {
            appendPart(
                    summary,
                    "BIN");
        }

        if (watch.maxPriceCoins > 0L) {
            appendPart(
                    summary,
                    "max "
                            + formatCoins(
                            watch.maxPriceCoins));
        }

        if (summary.isEmpty()) {
            return "No active price threshold";
        }

        return summary.toString();
    }

    private static String bazaarSummary(
            MarketWatchBazaarWatch watch) {

        StringBuilder summary =
                new StringBuilder();

        if (watch.maxInstantBuyPrice > 0.0D) {
            appendPart(
                    summary,
                    "buy <= "
                            + formatPrice(
                            watch.maxInstantBuyPrice));
        }

        if (watch.minInstantSellPrice > 0.0D) {
            appendPart(
                    summary,
                    "sell >= "
                            + formatPrice(
                            watch.minInstantSellPrice));
        }

        if (watch.minSpreadPercent > 0.0D) {
            appendPart(
                    summary,
                    "spread >= "
                            + String.format(
                            Locale.ROOT,
                            "%.1f%%",
                            watch.minSpreadPercent));
        }

        if (summary.isEmpty()) {
            return "No active price threshold";
        }

        return summary.toString();
    }

    private static void appendPart(
            StringBuilder builder,
            String value) {

        if (!builder.isEmpty()) {
            builder.append("  /  ");
        }

        builder.append(value);
    }

    private static String formatCoins(long coins) {
        if (coins >= 1_000_000_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.2fB",
                    coins / 1_000_000_000.0D);
        }

        if (coins >= 1_000_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    coins / 1_000_000.0D);
        }

        if (coins >= 1_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.1fk",
                    coins / 1_000.0D);
        }

        return Long.toString(coins);
    }

    private static String formatPrice(double price) {
        if (!Double.isFinite(price)) {
            return "--";
        }

        if (price >= 1_000_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    price / 1_000_000.0D);
        }

        if (price >= 1_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.1fk",
                    price / 1_000.0D);
        }

        return String.format(
                Locale.ROOT,
                "%.0f",
                price);
    }

    private static String formatCount(int count) {
        if (count >= 1_000_000) {
            return String.format(
                    Locale.ROOT,
                    "%.1fM",
                    count / 1_000_000.0D);
        }

        if (count >= 1_000) {
            return String.format(
                    Locale.ROOT,
                    "%.1fk",
                    count / 1_000.0D);
        }

        return Integer.toString(count);
    }
}