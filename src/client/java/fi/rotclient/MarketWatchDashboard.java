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

    private static final int WATCH_ROW_HEIGHT = 56;
    private static final int WATCH_ROW_STEP = 64;

    private static final int WATCH_EDIT_WIDTH = 40;
    private static final int WATCH_DELETE_WIDTH = 40;
    private static final int WATCH_CONTROL_GAP = 6;

    /*
     * PREMIUM MARKET WATCH ROWS
     *
     * Row drawing and click geometry deliberately continue to share these
     * constants, preventing the visual controls and hitboxes from drifting.
     */
    private Page page = Page.AUCTION_HOUSE;

    private final MarketWatchCreateForm createForm =
            new MarketWatchCreateForm();

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

        /*
         * DEDICATED MARKET WATCH EDITOR
         *
         * Watch creation gets the full dashboard body instead of being
         * squeezed underneath metrics and tabs. Scrolling remains available
         * only as a fallback for unusually small windows.
         */
        if (createForm.isOpen()) {
            int editorTop =
                    top + 34;

            createForm.draw(
                    graphics,
                    font,
                    left,
                    editorTop,
                    right,
                    bottom,
                    mouseX,
                    mouseY);

            return;
        }

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
                        ? "Watching your enabled alerts in the background."
                        : "Paused. Your saved alerts stay configured.",
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

        int addWidth = 104;

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                right - addWidth,
                tabsY,
                addWidth,
                page == Page.AUCTION_HOUSE
                        ? "+ ADD AH WATCH"
                        : "+ ADD BAZAAR",
                !createForm.isOpen(),
                true);

        int sectionY = tabsY + 40;

        if (createForm.isOpen()) {
            createForm.draw(
                    graphics,
                    font,
                    left,
                    sectionY,
                    right,
                    bottom,
                    mouseX,
                    mouseY);
            return;
        }

        List<?> watches =
                page == Page.AUCTION_HOUSE
                        ? auctionWatches
                        : bazaarWatches;

        String section =
                page == Page.AUCTION_HOUSE
                        ? "AUCTION HOUSE WATCHES"
                        : "BAZAAR WATCHES";

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
                    "Use the Add Watch button above to create your first alert.",
                    left + 14,
                    listY + 43);

            return;
        }

        int availableHeight =
                Math.max(
                        0,
                        bottom - listY - 8);

        int rowStep = WATCH_ROW_STEP;
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
                    WATCH_ROW_HEIGHT);

            boolean rowHover =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            left,
                            rowY,
                            width,
                            WATCH_ROW_HEIGHT);

            int textX =
                    left + 36;

            int textWidth =
                    Math.max(
                            20,
                            watchToggleX(right)
                                    - WATCH_CONTROL_GAP
                                    - textX);

            if (page == Page.AUCTION_HOUSE) {
                MarketWatchAuctionWatch watch =
                        auctionWatches.get(i);

                String title =
                        !watch.itemName.isBlank()
                                ? watch.itemName
                                : watch.itemId;

                if (title.isBlank()) {
                    title =
                            "Unnamed Auction Watch";
                }

                /*
                 * Thin state rail provides a much stronger card hierarchy
                 * without adding another bulky control.
                 */
                graphics.fill(
                        left,
                        rowY,
                        left + (rowHover ? 3 : 2),
                        rowY + WATCH_ROW_HEIGHT,
                        watch.enabled
                                ? RotClientTheme.HUD_ACCENT
                                : RotClientTheme.DIVIDER);

                /*
                 * AH item_bytes are not decoded yet, so the existing icon
                 * resolver supplies the best representative Minecraft icon.
                 */
                graphics.item(
                        MarketWatchItemIconResolver.auctionIcon(
                                "",
                                title),
                        left + 10,
                        rowY + 19);

                RotClientUiDraw.text(
                        graphics,
                        font,
                        fitWatchText(
                                font,
                                title,
                                textWidth),
                        textX,
                        rowY + 7,
                        watch.enabled
                                ? RotClientTheme.TEXT
                                : RotClientTheme.TEXT_MUTED,
                        true);

                RotClientUiDraw.helpText(
                        graphics,
                        font,
                        fitWatchText(
                                font,
                                auctionSummary(
                                        watch),
                                textWidth),
                        textX,
                        rowY + 23);

                RotClientUiDraw.text(
                        graphics,
                        font,
                        watch.enabled
                                ? "AH ALERT  /  ACTIVE"
                                : "AH ALERT  /  PAUSED",
                        textX,
                        rowY + 40,
                        watch.enabled
                                ? RotClientTheme.SUCCESS
                                : RotClientTheme.TEXT_MUTED,
                        false);

                drawWatchControls(
                        graphics,
                        font,
                        mouseX,
                        mouseY,
                        right,
                        rowY,
                        watch.enabled);

            } else {
                MarketWatchBazaarWatch watch =
                        bazaarWatches.get(i);

                String title =
                        bazaarWatchTitle(
                                watch);

                graphics.fill(
                        left,
                        rowY,
                        left + (rowHover ? 3 : 2),
                        rowY + WATCH_ROW_HEIGHT,
                        watch.enabled
                                ? RotClientTheme.VIOLET
                                : RotClientTheme.DIVIDER);

                graphics.item(
                        MarketWatchItemIconResolver.bazaarIcon(
                                watch.productId),
                        left + 10,
                        rowY + 19);

                RotClientUiDraw.text(
                        graphics,
                        font,
                        fitWatchText(
                                font,
                                title,
                                textWidth),
                        textX,
                        rowY + 7,
                        watch.enabled
                                ? RotClientTheme.TEXT
                                : RotClientTheme.TEXT_MUTED,
                        true);

                RotClientUiDraw.helpText(
                        graphics,
                        font,
                        fitWatchText(
                                font,
                                bazaarSummary(
                                        watch),
                                textWidth),
                        textX,
                        rowY + 23);

                RotClientUiDraw.text(
                        graphics,
                        font,
                        watch.enabled
                                ? "BAZAAR ALERT  /  ACTIVE"
                                : "BAZAAR ALERT  /  PAUSED",
                        textX,
                        rowY + 40,
                        watch.enabled
                                ? RotClientTheme.SUCCESS
                                : RotClientTheme.TEXT_MUTED,
                        false);

                drawWatchControls(
                        graphics,
                        font,
                        mouseX,
                        mouseY,
                        right,
                        rowY,
                        watch.enabled);
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

        if (createForm.isOpen()) {
            return createForm.mouseClicked(
                    button,
                    mx,
                    my,
                    left,
                    top + 34,
                    right,
                    bottom);
        }

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

        if (createForm.isOpen()) {
            return createForm.mouseClicked(
                    button,
                    mx,
                    my,
                    left,
                    tabsY + 40,
                    right,
                    bottom);
        }

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

        int addWidth = 104;

        if (RotClientUiDraw.inside(
                mx,
                my,
                right - addWidth,
                tabsY,
                addWidth,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            if (page == Page.AUCTION_HOUSE) {
                createForm.openAuction();
            } else {
                createForm.openBazaar();
            }

            return true;
        }

        /*
         * EXISTING WATCH MANAGEMENT
         *
         * Uses exactly the same geometry helpers as rendering, so the visual
         * controls and hitboxes cannot drift apart.
         */
        int sectionY =
                tabsY + 40;

        int listY =
                sectionY + 20;

        List<MarketWatchAuctionWatch> auctionRows =
                MarketWatchRuntime.auctionWatches();

        List<MarketWatchBazaarWatch> bazaarRows =
                MarketWatchRuntime.bazaarWatches();

        int watchCount =
                page == Page.AUCTION_HOUSE
                        ? auctionRows.size()
                        : bazaarRows.size();

        if (watchCount > 0) {
            int availableHeight =
                    Math.max(
                            0,
                            bottom - listY - 8);

            int maxRows =
                    Math.max(
                            1,
                            availableHeight
                                    / WATCH_ROW_STEP);

            int shown =
                    Math.min(
                            watchCount,
                            maxRows);

            for (int i = 0;
                    i < shown;
                    i++) {

                int rowY =
                        listY
                                + i
                                * WATCH_ROW_STEP;

                int rowToggleX =
                        watchToggleX(
                                right);

                int rowToggleY =
                        watchToggleY(
                                rowY);

                /*
                 * Per-watch ON/OFF.
                 */
                if (RotClientUiDraw.inside(
                        mx,
                        my,
                        rowToggleX - 4,
                        rowToggleY - 6,
                        QolUtilityUiMath.TOGGLE_WIDTH + 8,
                        QolUtilityUiMath.TOGGLE_HEIGHT + 12)) {

                    if (page == Page.AUCTION_HOUSE) {
                        MarketWatchAuctionWatch watch =
                                auctionRows.get(i);

                        watch.enabled =
                                !watch.enabled;

                        MarketWatchRuntime.updateAuctionWatch(
                                watch);

                    } else {
                        MarketWatchBazaarWatch watch =
                                bazaarRows.get(i);

                        watch.enabled =
                                !watch.enabled;

                        MarketWatchRuntime.updateBazaarWatch(
                                watch);
                    }

                    return true;
                }

                /*
                 * EDIT.
                 */
                if (RotClientUiDraw.inside(
                        mx,
                        my,
                        watchEditX(right),
                        watchButtonY(rowY),
                        WATCH_EDIT_WIDTH,
                        RotClientUiDraw.BUTTON_HEIGHT)) {

                    if (page == Page.AUCTION_HOUSE) {
                        createForm.openAuctionEdit(
                                auctionRows.get(i));

                    } else {
                        createForm.openBazaarEdit(
                                bazaarRows.get(i));
                    }

                    return true;
                }

                /*
                 * DELETE.
                 *
                 * This is currently immediate. We can add an explicit
                 * confirmation state later if desired.
                 */
                if (RotClientUiDraw.inside(
                        mx,
                        my,
                        watchDeleteX(right),
                        watchButtonY(rowY),
                        WATCH_DELETE_WIDTH,
                        RotClientUiDraw.BUTTON_HEIGHT)) {

                    String watchId =
                            page == Page.AUCTION_HOUSE
                                    ? auctionRows.get(i).id
                                    : bazaarRows.get(i).id;

                    MarketWatchRuntime.deleteWatch(
                            watchId);

                    return true;
                }
            }
        }

        return false;
    }

    boolean mouseDragged(
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (!createForm.isOpen()) {
            return false;
        }

        return createForm.mouseDragged(
                mouseX,
                mouseY,
                left,
                top + 34,
                right,
                bottom);
    }

    boolean mouseReleased() {
        if (!createForm.isOpen()) {
            return false;
        }

        return createForm.mouseReleased();
    }
    boolean mouseScrolled(
            int mouseX,
            int mouseY,
            double verticalAmount,
            int left,
            int top,
            int right,
            int bottom) {

        if (!createForm.isOpen()) {
            return false;
        }

        return createForm.mouseScrolled(
                verticalAmount,
                mouseX,
                mouseY,
                left,
                top + 34,
                right,
                bottom);
    }
    boolean captureChar(
            String incoming,
            boolean allowed) {

        return createForm.captureChar(
                incoming,
                allowed);
    }

    boolean captureKey(int key) {
        return createForm.captureKey(key);
    }

    private static int watchDeleteX(
            int right) {

        return right
                - 10
                - WATCH_DELETE_WIDTH;
    }

    private static int watchEditX(
            int right) {

        return watchDeleteX(right)
                - WATCH_CONTROL_GAP
                - WATCH_EDIT_WIDTH;
    }

    private static int watchToggleX(
            int right) {

        return watchEditX(right)
                - WATCH_CONTROL_GAP
                - QolUtilityUiMath.TOGGLE_WIDTH;
    }

    private static int watchButtonY(
            int rowY) {

        return rowY
                + (WATCH_ROW_HEIGHT
                - RotClientUiDraw.BUTTON_HEIGHT)
                / 2;
    }

    private static int watchToggleY(
            int rowY) {

        return rowY
                + (WATCH_ROW_HEIGHT
                - QolUtilityUiMath.TOGGLE_HEIGHT)
                / 2;
    }

    private static void drawWatchControls(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int right,
            int rowY,
            boolean enabled) {

        int toggleX =
                watchToggleX(right);

        int toggleY =
                watchToggleY(rowY);

        boolean toggleHover =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        toggleX - 4,
                        toggleY - 6,
                        QolUtilityUiMath.TOGGLE_WIDTH + 8,
                        QolUtilityUiMath.TOGGLE_HEIGHT + 12);

        RotClientUiDraw.drawToggle(
                graphics,
                toggleX,
                toggleY,
                enabled,
                toggleHover);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                watchEditX(right),
                watchButtonY(rowY),
                WATCH_EDIT_WIDTH,
                "EDIT",
                false,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                watchDeleteX(right),
                watchButtonY(rowY),
                WATCH_DELETE_WIDTH,
                "DEL",
                false,
                true);
    }

    private static String bazaarWatchTitle(
            MarketWatchBazaarWatch watch) {

        if (watch == null
                || watch.productId == null
                || watch.productId.isBlank()) {

            return "Unnamed Bazaar Watch";
        }

        MarketWatchItemCatalog.BazaarStats stats =
                MarketWatchItemCatalog.bazaarStats(
                        watch.productId);

        if (stats.available()
                && stats.displayName() != null
                && !stats.displayName().isBlank()) {

            return stats.displayName();
        }

        return prettyProductId(
                watch.productId);
    }

    private static String prettyProductId(
            String productId) {

        if (productId == null
                || productId.isBlank()) {

            return "Unnamed Bazaar Watch";
        }

        String source =
                productId
                        .replace('_', ' ')
                        .trim()
                        .toLowerCase(
                                Locale.ROOT);

        StringBuilder result =
                new StringBuilder(
                        source.length());

        boolean capitalize =
                true;

        for (int i = 0;
                i < source.length();
                i++) {

            char c =
                    source.charAt(i);

            if (Character.isWhitespace(c)) {
                result.append(c);
                capitalize = true;
                continue;
            }

            result.append(
                    capitalize
                            ? Character.toUpperCase(c)
                            : c);

            capitalize =
                    false;
        }

        return result.toString();
    }
    private static String fitWatchText(
            Font font,
            String text,
            int maxWidth) {

        if (text == null
                || text.isEmpty()
                || maxWidth <= 0) {

            return "";
        }

        if (font.width(text)
                <= maxWidth) {

            return text;
        }

        String suffix = "...";

        int suffixWidth =
                font.width(suffix);

        if (suffixWidth >= maxWidth) {
            return "";
        }

        int usable =
                maxWidth
                        - suffixWidth;

        int end =
                text.length();

        while (end > 0
                && font.width(
                        text.substring(
                                0,
                                end))
                > usable) {

            end--;
        }

        return text.substring(
                0,
                end)
                + suffix;
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
            appendPart(
                    summary,
                    watch.tier);
        }

        if (watch.binOnly) {
            appendPart(
                    summary,
                    "BIN");
        }

        if (watch.maxPriceCoins > 0L) {
            appendPart(
                    summary,
                    "\u2193 Buy "
                            + formatCoins(
                            watch.maxPriceCoins)
                            + " max");
        }

        if (summary.isEmpty()) {
            return "No price condition configured";
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
                    "\u2193 Buy "
                            + formatPrice(
                            watch.maxInstantBuyPrice)
                            + " max");
        }

        if (watch.minInstantSellPrice > 0.0D) {
            appendPart(
                    summary,
                    "\u2191 Sell "
                            + formatPrice(
                            watch.minInstantSellPrice)
                            + " min");
        }

        if (watch.minSpreadPercent > 0.0D) {
            appendPart(
                    summary,
                    "Spread "
                            + String.format(
                            Locale.ROOT,
                            "%.1f%%+",
                            watch.minSpreadPercent));
        }

        if (summary.isEmpty()) {
            return "No price condition configured";
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