package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

final class MarketWatchWatchlistDashboard {
    private enum Page {
        AUCTION_HOUSE,
        BAZAAR,
        ALERTS
    }

    private static final int WATCH_ROW_HEIGHT = 56;
    private static final int WATCH_ROW_STEP = 64;

    private static final int WATCH_EDIT_WIDTH = 40;
    private static final int WATCH_DELETE_WIDTH = 40;
    private static final int WATCH_CONTROL_GAP = 6;

    private static final int ALERT_TAB_X_OFFSET = 240;
    private static final int ALERT_TAB_WIDTH = 72;

    private static final int ALERT_ROW_HEIGHT = 44;
    private static final int ALERT_ROW_STEP = 48;


    /*
     * PREMIUM MARKET WATCH HEADER
     *
     * Gives the dashboard a clearer hierarchy:
     * status -> live market data -> enabled alerts -> watch management.
     */
/*
     * PREMIUM MARKET WATCH ROWS
     *
     * Row drawing and click geometry deliberately continue to share these
     * constants, preventing the visual controls and hitboxes from drifting.
     */
    private Page page = Page.AUCTION_HOUSE;

    private final MarketWatchCreateForm createForm =
            new MarketWatchCreateForm();

    private int alertScrollOffset;

    private boolean alertScrollbarDragging;

    private int alertScrollbarX;
    private int alertScrollbarTrackTop;
    private int alertScrollbarTrackBottom;

    private int alertScrollbarThumbTop;
    private int alertScrollbarThumbHeight;

    private int alertScrollbarMaxOffset;
    private int alertScrollbarGrabOffset;

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
                enabled ? "RUNNING" : "PAUSED",
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


        graphics.fill(
                left,
                masterY,
                left + 3,
                masterY + masterH,
                enabled
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.DIVIDER);
RotClientUiDraw.text(
                graphics,
                font,
                enabled
                        ? "Alerts running"
                        : "Alerts paused",
                left + 14,
                masterY + 12,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                enabled
                        ? "Market Watch is checking your enabled alerts."
                        : "Your alerts are saved but are not being checked.",
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

        int enabledWatchCount =
                enabledWatchCount(
                        auctionWatches,
                        bazaarWatches);

        int totalWatchCount =
                auctionWatches.size()
                        + bazaarWatches.size();

        int metricsY =
                masterY + masterH + 14;
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
                "AH MARKET",
                auctionState.available()
                        ? formatCount(
                                auctionState.snapshot()
                                        .auctions()
                                        .size())
                        : "--",
                auctionState.available()
                        ? "LIVE AUCTIONS"
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
                "ACTIVE ALERTS",
                Integer.toString(
                        enabledWatchCount),
                enabledWatchCount
                        + " enabled / "
                        + totalWatchCount
                        + " saved",
                enabled
                        && enabledWatchCount > 0);

        int tabsY = metricsY + 70;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                left,
                tabsY,
                128,
                "AH WATCHES",
                page == Page.AUCTION_HOUSE,
                true);

        RotClientUiDraw.drawPremiumButton(
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

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                left + ALERT_TAB_X_OFFSET,
                tabsY,
                ALERT_TAB_WIDTH,
                "ALERTS",
                page == Page.ALERTS,
                true);

        int addWidth = 104;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                right - addWidth,
                tabsY,
                addWidth,
                page == Page.AUCTION_HOUSE
                        ? "+ ADD AH WATCH"
                        : page == Page.BAZAAR
                        ? "+ ADD BAZAAR"
                        : "CLEAR HISTORY",
                !createForm.isOpen()
                        && (page != Page.ALERTS
                        || !MarketWatchRuntime
                                .alertHistory()
                                .isEmpty()),
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

        if (page == Page.ALERTS) {
            drawAlertHistory(
                    graphics,
                    font,
                    left,
                    sectionY,
                    right,
                    bottom);

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
                    page == Page.AUCTION_HOUSE
                            ? "No Auction House alerts yet"
                            : "No Bazaar alerts yet",
                    left + 14,
                    listY + 13,
                    RotClientTheme.TEXT,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    page == Page.AUCTION_HOUSE
                            ? "Get notified when an Auction House item reaches your target."
                            : "Get notified when a Bazaar item reaches your buy or sell target.",
                    left + 14,
                    listY + 29);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    page == Page.AUCTION_HOUSE
                            ? "Use + ADD AH WATCH above to create one."
                            : "Use + ADD BAZAAR above to create one.",
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
                                MarketWatchItemCatalog
                                        .auctionStats(
                                                title,
                                                watch.tier)
                                        .category(),
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

    private void resetAlertScrollbarGeometry() {
        alertScrollbarDragging = false;

        alertScrollbarX = 0;
        alertScrollbarTrackTop = 0;
        alertScrollbarTrackBottom = 0;

        alertScrollbarThumbTop = 0;
        alertScrollbarThumbHeight = 0;

        alertScrollbarMaxOffset = 0;
        alertScrollbarGrabOffset = 0;
    }

    private boolean alertScrollbarClicked(
            int mouseX,
            int mouseY) {

        if (alertScrollbarMaxOffset <= 0
                || alertScrollbarTrackBottom
                <= alertScrollbarTrackTop) {

            return false;
        }

        boolean insideTrack =
                mouseX
                        >= alertScrollbarX - 5
                        && mouseX
                        <= alertScrollbarX + 8
                        && mouseY
                        >= alertScrollbarTrackTop
                        && mouseY
                        <= alertScrollbarTrackBottom;

        if (!insideTrack) {
            return false;
        }

        boolean insideThumb =
                mouseY
                        >= alertScrollbarThumbTop
                        && mouseY
                        <= alertScrollbarThumbTop
                        + alertScrollbarThumbHeight;

        alertScrollbarDragging =
                true;

        if (insideThumb) {
            alertScrollbarGrabOffset =
                    mouseY
                            - alertScrollbarThumbTop;

        } else {
            alertScrollbarGrabOffset =
                    alertScrollbarThumbHeight
                            / 2;

            dragAlertScrollbar(
                    mouseY);
        }

        return true;
    }

    private void dragAlertScrollbar(
            int mouseY) {

        if (!alertScrollbarDragging
                || alertScrollbarMaxOffset <= 0) {

            return;
        }

        int trackHeight =
                alertScrollbarTrackBottom
                        - alertScrollbarTrackTop;

        int travel =
                Math.max(
                        0,
                        trackHeight
                                - alertScrollbarThumbHeight);

        if (travel <= 0) {
            alertScrollOffset = 0;
            return;
        }

        int desiredThumbTop =
                mouseY
                        - alertScrollbarGrabOffset;

        desiredThumbTop =
                Math.max(
                        alertScrollbarTrackTop,
                        Math.min(
                                desiredThumbTop,
                                alertScrollbarTrackTop
                                        + travel));

        double progress =
                (desiredThumbTop
                        - alertScrollbarTrackTop)
                        / (double) travel;

        alertScrollOffset =
                (int) Math.round(
                        progress
                                * alertScrollbarMaxOffset);

        alertScrollOffset =
                Math.max(
                        0,
                        Math.min(
                                alertScrollOffset,
                                alertScrollbarMaxOffset));
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

        if (RotClientUiDraw.inside(
                mx,
                my,
                left + ALERT_TAB_X_OFFSET,
                tabsY,
                ALERT_TAB_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            page = Page.ALERTS;
            alertScrollOffset = 0;
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

            } else if (page == Page.BAZAAR) {
                createForm.openBazaar();

            } else {
                MarketWatchRuntime
                        .clearAlertHistory();

                MarketWatchAlertHud
                        .clearVisuals();

                alertScrollOffset = 0;
            }

            return true;
        }

        if (page == Page.ALERTS) {
            return alertScrollbarClicked(
                    mx,
                    my);
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

                if (alertScrollbarDragging) {
            dragAlertScrollbar(
                    mouseY);

            return true;
        }
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
                if (alertScrollbarDragging) {
            alertScrollbarDragging = false;
            return true;
        }
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

        if (createForm.isOpen()) {
            return createForm.mouseScrolled(
                    verticalAmount,
                    mouseX,
                    mouseY,
                    left,
                    top + 34,
                    right,
                    bottom);
        }

        if (page != Page.ALERTS
                || verticalAmount == 0.0D) {

            return false;
        }

        List<MarketWatchLiveAlert> alerts =
                MarketWatchRuntime
                        .alertHistory();

        if (alerts.size() <= 1) {
            alertScrollOffset = 0;
            return false;
        }

        int direction =
                verticalAmount < 0.0D
                        ? 1
                        : -1;

        alertScrollOffset =
                Math.max(
                        0,
                        Math.min(
                                alerts.size() - 1,
                                alertScrollOffset
                                        + direction));

        return true;
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

        RotClientUiDraw.drawPremiumButton(
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

        RotClientUiDraw.drawPremiumButton(
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
        private void drawAlertHistory(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int sectionY,
            int right,
            int bottom) {

        List<MarketWatchLiveAlert> alerts =
                MarketWatchRuntime
                        .alertHistory();

        int width =
                Math.max(
                        0,
                        right - left);

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                "RECENT DEAL ALERTS",
                left,
                sectionY);

        RotClientUiDraw.drawStatusPill(
                graphics,
                font,
                right,
                sectionY,
                alerts.size() == 1
                        ? "1 HIT"
                        : alerts.size()
                        + " HITS",
                alerts.isEmpty()
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.HUD_ACCENT);

        RotClientUiDraw.helpText(
                graphics,
                font,
                alerts.size() > 1
                        ? "Mouse wheel or drag the scrollbar on the right."
                        : "Newest alerts appear first.",
                left,
                sectionY + 10);

        int listY =
                sectionY + 24;

        if (alerts.isEmpty()) {
            alertScrollOffset = 0;
            resetAlertScrollbarGeometry();

            RotClientUiDraw.drawElevatedCard(
                    graphics,
                    left,
                    listY,
                    width,
                    50);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "No deal alerts yet",
                    left + 14,
                    listY + 10,
                    RotClientTheme.TEXT,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Fixed-price and dynamic-percentage deals appear here.",
                    left + 14,
                    listY + 28);

            return;
        }

        int availableHeight =
                Math.max(
                        0,
                        bottom
                                - listY
                                - 6);

        int maxRows =
                availableHeight <= ALERT_ROW_HEIGHT
                        ? 1
                        : 1
                        + (availableHeight
                        - ALERT_ROW_HEIGHT)
                        / ALERT_ROW_STEP;

        int shown =
                Math.min(
                        alerts.size(),
                        Math.max(
                                1,
                                maxRows));

        int maxOffset =
                Math.max(
                        0,
                        alerts.size()
                                - shown);

        alertScrollOffset =
                Math.max(
                        0,
                        Math.min(
                                alertScrollOffset,
                                maxOffset));

        boolean scrollable =
                maxOffset > 0;

        int gutter =
                scrollable
                        ? 14
                        : 0;

        int rowRight =
                right
                        - gutter;

        int rowWidth =
                Math.max(
                        20,
                        rowRight
                                - left);

        int textX =
                left + 35;

        int textWidth =
                Math.max(
                        20,
                        rowRight
                                - 10
                                - textX);

        for (int visible = 0;
                visible < shown;
                visible++) {

            MarketWatchLiveAlert alert =
                    alerts.get(
                            alertScrollOffset
                                    + visible);

            int rowY =
                    listY
                            + visible
                            * ALERT_ROW_STEP;

            RotClientUiDraw.drawElevatedCard(
                    graphics,
                    left,
                    rowY,
                    rowWidth,
                    ALERT_ROW_HEIGHT);

            int accent =
                    alert.market()
                            == MarketWatchLiveAlert.Market.BAZAAR
                            ? RotClientTheme.VIOLET
                            : RotClientTheme.HUD_ACCENT;

            graphics.fill(
                    left,
                    rowY,
                    left + 3,
                    rowY + ALERT_ROW_HEIGHT,
                    accent);

            graphics.item(
                    MarketWatchAlertHud
                            .icon(alert),
                    left + 9,
                    rowY + 14);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    fitWatchText(
                            font,
                            MarketWatchAlertHud
                                    .displayName(
                                            alert),
                            textWidth),
                    textX,
                    rowY + 4,
                    RotClientTheme.TEXT,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    fitWatchText(
                            font,
                            MarketWatchAlertHud
                                    .summary(
                                            alert),
                            textWidth),
                    textX,
                    rowY + 18);

            String market =
                    alert.market()
                            == MarketWatchLiveAlert.Market.BAZAAR
                            ? "BAZAAR"
                            : "AUCTION HOUSE";

            RotClientUiDraw.text(
                    graphics,
                    font,
                    fitWatchText(
                            font,
                            market
                                    + "  /  "
                                    + alertAge(
                                    alert.observedAtMillis()),
                            textWidth),
                    textX,
                    rowY + 31,
                    RotClientTheme.TEXT_MUTED,
                    false);
        }

        if (!scrollable) {
            resetAlertScrollbarGeometry();
            return;
        }

        int trackTop =
                listY;

        int trackBottom =
                Math.min(
                        bottom - 6,
                        listY
                                + (shown - 1)
                                * ALERT_ROW_STEP
                                + ALERT_ROW_HEIGHT);

        int trackHeight =
                Math.max(
                        1,
                        trackBottom
                                - trackTop);

        int thumbHeight =
                Math.max(
                        18,
                        (int) Math.round(
                                trackHeight
                                        * (shown
                                        / (double) alerts.size())));

        thumbHeight =
                Math.min(
                        trackHeight,
                        thumbHeight);

        int travel =
                Math.max(
                        0,
                        trackHeight
                                - thumbHeight);

        int thumbTop =
                trackTop;

        if (travel > 0
                && maxOffset > 0) {

            thumbTop +=
                    (int) Math.round(
                            travel
                                    * (alertScrollOffset
                                    / (double) maxOffset));
        }

        int trackX =
                right - 7;

        graphics.fill(
                trackX,
                trackTop,
                trackX + 3,
                trackBottom,
                RotClientTheme.TEXT_MUTED);

        graphics.fill(
                trackX - 1,
                thumbTop,
                trackX + 4,
                thumbTop + thumbHeight,
                RotClientTheme.HUD_ACCENT);

        alertScrollbarX =
                trackX;

        alertScrollbarTrackTop =
                trackTop;

        alertScrollbarTrackBottom =
                trackBottom;

        alertScrollbarThumbTop =
                thumbTop;

        alertScrollbarThumbHeight =
                thumbHeight;

        alertScrollbarMaxOffset =
                maxOffset;
    }

    private static String alertAge(
            long observedAtMillis) {

        long seconds =
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - Math.max(
                                        0L,
                                        observedAtMillis))
                        / 1_000L;

        if (seconds < 60L) {
            return seconds
                    + "s ago";
        }

        long minutes =
                seconds / 60L;

        if (minutes < 60L) {
            return minutes
                    + "m ago";
        }

        long hours =
                minutes / 60L;

        if (hours < 24L) {
            return hours
                    + "h ago";
        }

        return hours / 24L
                + "d ago";
    }
    private static int enabledWatchCount(
            List<MarketWatchAuctionWatch> auctionWatches,
            List<MarketWatchBazaarWatch> bazaarWatches) {

        int count = 0;

        if (auctionWatches != null) {
            for (MarketWatchAuctionWatch watch : auctionWatches) {
                if (watch != null
                        && watch.enabled) {

                    count++;
                }
            }
        }

        if (bazaarWatches != null) {
            for (MarketWatchBazaarWatch watch : bazaarWatches) {
                if (watch != null
                        && watch.enabled) {

                    count++;
                }
            }
        }

        return count;
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

        if (watch.minDiscountPercent > 0.0D) {
            appendPart(
                    summary,
                    "\u2193 Dynamic "
                            + String.format(
                            Locale.ROOT,
                            "%.1f%%",
                            watch.minDiscountPercent)
                            + " below market");

        } else if (watch.maxPriceCoins > 0L) {
            appendPart(
                    summary,
                    "\u2193 Fixed "
                            + formatCoins(
                            watch.maxPriceCoins)
                            + " max");
        }

        if (watch.binOnly
                || watch.minDiscountPercent > 0.0D) {

            appendPart(
                    summary,
                    "BIN");
        }

        if (summary.isEmpty()) {
            return "No deal trigger configured";
        }

        return summary.toString();
    }

    private static String bazaarSummary(
            MarketWatchBazaarWatch watch) {

        StringBuilder summary =
                new StringBuilder();

        if (watch.buyDealPercent > 0.0D) {
            appendPart(
                    summary,
                    "\u2193 Dynamic "
                            + String.format(
                            Locale.ROOT,
                            "%.1f%%",
                            watch.buyDealPercent)
                            + " below market");

        } else if (watch.maxInstantBuyPrice > 0.0D) {
            appendPart(
                    summary,
                    "\u2193 Fixed "
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

        if (watch.minSpreadCoins > 0.0D) {
            appendPart(
                    summary,
                    "spread "
                            + formatPrice(
                            watch.minSpreadCoins)
                            + "+");
        }

        if (watch.minSpreadPercent > 0.0D) {
            appendPart(
                    summary,
                    "spread "
                            + String.format(
                            Locale.ROOT,
                            "%.1f%%+",
                            watch.minSpreadPercent));
        }

        if (watch.minWeeklyVolume > 0L) {
            appendPart(
                    summary,
                    "volume "
                            + watch.minWeeklyVolume
                            + "+");
        }

        if (summary.isEmpty()) {
            return "No deal trigger configured";
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