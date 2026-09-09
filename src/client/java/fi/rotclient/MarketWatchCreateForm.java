package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Locale;

final class MarketWatchCreateForm {
    private enum Kind {
        NONE,
        AUCTION_HOUSE,
        BAZAAR
    }

    private static final int SEARCH_BOX_HEIGHT = 24;
    private static final int SUGGESTION_HEIGHT = 36;
    private static final int MAX_SUGGESTIONS = 5;

    /*
     * Reserved horizontal space between the suggestion scrollbar
     * and the actual item cards.
     */
    private static final int SUGGESTION_LIST_GUTTER = 18;

    private Kind kind = Kind.NONE;

    private int focusedField = -1;

    private String search = "";
    private boolean suggestionsOpen;

    /*
     * SCROLLABLE COMPLETE SUGGESTION RESULTS
     */
    private int suggestionOffset;
private MarketWatchItemCatalog.AuctionSuggestion
            selectedAuction;

    private MarketWatchItemCatalog.BazaarSuggestion
            selectedBazaar;

    /*
     * EXISTING WATCH EDITING
     *
     * These are copies returned by the runtime. The original persisted watch
     * is only replaced after SAVE CHANGES succeeds.
     */
    private MarketWatchAuctionWatch editingAuctionWatch;
    private MarketWatchBazaarWatch editingBazaarWatch;
    private String editingOriginalSearch = "";

    private boolean auctionBinOnly = true;
    private String auctionMaxPrice = "";
    private String auctionCooldown = "60";

    private String bazaarMaxBuy = "";
    private String bazaarMinSell = "";
    private String bazaarMinSpreadCoins = "";
    private String bazaarMinSpreadPercent = "";
    private String bazaarMinWeeklyVolume = "";
    private String bazaarCooldown = "60";

    private String error = "";

    private final RotClientScrollState bodyScroll =
            new RotClientScrollState();

    /*
     * INTERACTIVE SUGGESTION SCROLLBAR
     */
    private final RotClientScrollState suggestionScroll =
            new RotClientScrollState();

    boolean isOpen() {
        return kind != Kind.NONE;
    }

    void openAuction() {
        kind = Kind.AUCTION_HOUSE;

        editingAuctionWatch = null;
        editingBazaarWatch = null;
        editingOriginalSearch = "";

        search = "";
        suggestionsOpen = false;
        suggestionOffset = 0;
        suggestionScroll.reset();
        selectedAuction = null;
        selectedBazaar = null;

        auctionBinOnly = true;
        auctionMaxPrice = "";
        auctionCooldown = "60";

        bodyScroll.reset();

        focusedField = 0;
        error = "";
    }

    void openBazaar() {
        kind = Kind.BAZAAR;

        editingAuctionWatch = null;
        editingBazaarWatch = null;
        editingOriginalSearch = "";

        search = "";
        suggestionsOpen = false;
        suggestionOffset = 0;
        suggestionScroll.reset();
        selectedAuction = null;
        selectedBazaar = null;

        bazaarMaxBuy = "";
        bazaarMinSell = "";
        bazaarMinSpreadCoins = "";
        bazaarMinSpreadPercent = "";
        bazaarMinWeeklyVolume = "";
        bazaarCooldown = "60";

        bodyScroll.reset();

        focusedField = 0;
        error = "";
    }

    void openAuctionEdit(
            MarketWatchAuctionWatch watch) {

        if (watch == null) {
            return;
        }

        openAuction();

        editingAuctionWatch =
                watch.copy();

        search =
                !watch.itemName.isBlank()
                        ? watch.itemName
                        : watch.itemId;

        auctionBinOnly =
                watch.binOnly;

        auctionMaxPrice =
                watch.maxPriceCoins > 0L
                        ? editableAmount(
                                watch.maxPriceCoins)
                        : "";

        auctionCooldown =
                Long.toString(
                        watch.cooldownSeconds);

        /*
         * Restore the live suggestion when possible. If the item is not
         * currently present in the live snapshot, editing still works by
         * retaining the persisted watch item.
         */
        if (!watch.itemName.isBlank()) {
            List<MarketWatchItemCatalog.AuctionSuggestion>
                    candidates =
                    MarketWatchItemCatalog.auctionSuggestions(
                            watch.itemName,
                            Integer.MAX_VALUE);

            for (MarketWatchItemCatalog.AuctionSuggestion candidate
                    : candidates) {

                boolean sameName =
                        candidate.itemName()
                                .equalsIgnoreCase(
                                        watch.itemName);

                boolean sameTier =
                        watch.tier.isBlank()
                                || candidate.tier()
                                .equalsIgnoreCase(
                                        watch.tier);

                if (sameName
                        && sameTier) {

                    selectedAuction =
                            candidate;

                    search =
                            candidate.itemName();

                    break;
                }
            }
        }

        editingOriginalSearch =
                search;

        suggestionsOpen = false;
        suggestionOffset = 0;
        suggestionScroll.reset();
        bodyScroll.reset();

        focusedField = -1;
        error = "";
    }

    void openBazaarEdit(
            MarketWatchBazaarWatch watch) {

        if (watch == null) {
            return;
        }

        openBazaar();

        editingBazaarWatch =
                watch.copy();

        search =
                watch.productId;

        bazaarMaxBuy =
                watch.maxInstantBuyPrice > 0.0D
                        ? editableAmount(
                                watch.maxInstantBuyPrice)
                        : "";

        bazaarMinSell =
                watch.minInstantSellPrice > 0.0D
                        ? editableAmount(
                                watch.minInstantSellPrice)
                        : "";

        bazaarMinSpreadCoins =
                watch.minSpreadCoins > 0.0D
                        ? editableAmount(
                                watch.minSpreadCoins)
                        : "";

        bazaarMinSpreadPercent =
                watch.minSpreadPercent > 0.0D
                        ? editablePlain(
                                watch.minSpreadPercent)
                        : "";

        bazaarMinWeeklyVolume =
                watch.minWeeklyVolume > 0L
                        ? editableAmount(
                                watch.minWeeklyVolume)
                        : "";

        bazaarCooldown =
                Long.toString(
                        watch.cooldownSeconds);

        List<MarketWatchItemCatalog.BazaarSuggestion>
                candidates =
                MarketWatchItemCatalog.bazaarSuggestions(
                        watch.productId,
                        Integer.MAX_VALUE);

        for (MarketWatchItemCatalog.BazaarSuggestion candidate
                : candidates) {

            if (candidate.productId()
                    .equalsIgnoreCase(
                            watch.productId)) {

                selectedBazaar =
                        candidate;

                search =
                        candidate.displayName();

                break;
            }
        }

        editingOriginalSearch =
                search;

        suggestionsOpen = false;
        suggestionOffset = 0;
        suggestionScroll.reset();
        bodyScroll.reset();

        focusedField = -1;
        error = "";
    }

    private boolean isEditing() {
        return editingAuctionWatch != null
                || editingBazaarWatch != null;
    }
    void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY) {

        Layout layout =
                layout(
                        left,
                        top,
                        right,
                        bottom);

        boolean auction =
                kind == Kind.AUCTION_HOUSE;

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                auction
                        ? (isEditing()
                                ? "EDIT AUCTION HOUSE WATCH"
                                : "ADD AUCTION HOUSE WATCH")
                        : (isEditing()
                                ? "EDIT BAZAAR WATCH"
                                : "ADD BAZAAR WATCH"),
                left,
                top);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                layout.cardX(),
                layout.cardY(),
                layout.cardWidth(),
                layout.cardHeight());

        int viewport =
                Math.max(
                        0,
                        layout.bodyBottom()
                                - layout.bodyTop());

        bodyScroll.setBounds(
                contentHeight(),
                viewport);

        bodyScroll.advance(
                System.nanoTime());

        Layout bodyLayout =
                layout.scrolled(
                        -bodyScroll.scrollPixels());

        if (viewport > 0) {
            graphics.enableScissor(
                    layout.cardX() + 1,
                    layout.bodyTop(),
                    layout.cardX()
                            + layout.cardWidth()
                            - 12,
                    layout.bodyBottom());

            try {
                drawSearch(
                        graphics,
                        font,
                        bodyLayout,
                        mouseX,
                        mouseY);

                drawSelectionAndSettings(
                        graphics,
                        font,
                        bodyLayout,
                        mouseX,
                        mouseY);
            } finally {
                graphics.disableScissor();
            }
        }

        if (bodyScroll.canScroll()) {
            int scrollbarX =
                    layout.cardX()
                            + layout.cardWidth()
                            - RotClientUiDraw.SCROLLBAR_WIDTH
                            - 2;

            boolean hovered =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            scrollbarX - 2,
                            layout.bodyTop(),
                            RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                            viewport);

            RotClientUiDraw.drawScrollbar(
                    graphics,
                    scrollbarX,
                    layout.bodyTop(),
                    layout.bodyBottom(),
                    bodyScroll.contentHeight(),
                    bodyScroll.scrollPixels(),
                    hovered,
                    bodyScroll.isThumbDragging());
        }

        drawFooter(
                graphics,
                font,
                layout,
                mouseX,
                mouseY);
    }

    private static int suggestionRowX(
            Layout layout) {

        return layout.leftX()
                + SUGGESTION_LIST_GUTTER;
    }

    private static int suggestionRowWidth(
            Layout layout) {

        return Math.max(
                1,
                layout.leftWidth()
                        - SUGGESTION_LIST_GUTTER);
    }

    private void drawSearch(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.helpText(
                graphics,
                font,
                "SEARCH ITEM",
                layout.leftX(),
                layout.contentTop());

        drawSearchBox(
                graphics,
                font,
                layout.leftX(),
                layout.searchY(),
                layout.leftWidth(),
                search,
                focusedField == 0);

        if (!suggestionsOpen
                || search.isBlank()) {

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Start typing an item name.",
                    layout.leftX(),
                    layout.suggestionsY() + 4);

            return;
        }

        if (kind == Kind.AUCTION_HOUSE) {
            List<MarketWatchItemCatalog.AuctionSuggestion>
                    suggestions =
                    MarketWatchItemCatalog.auctionSuggestions(
                            search,
                            Integer.MAX_VALUE);

                suggestions =
                        visibleSuggestions(
                                suggestions,
                                layout.suggestionRows());

            if (suggestions.isEmpty()) {
                RotClientUiDraw.helpText(
                        graphics,
                        font,
                        "No matching live BIN items.",
                        layout.leftX(),
                        layout.suggestionsY() + 4);

                return;
            }

            for (int i = 0;
                 i < suggestions.size();
                 i++) {

                drawAuctionSuggestion(
                        graphics,
                        font,
                        suggestions.get(i),
                        suggestionRowX(layout),
                        layout.suggestionsY()
                                + i * SUGGESTION_HEIGHT,
                        suggestionRowWidth(layout),
                        mouseX,
                        mouseY);
            }

            drawSuggestionScrollbar(
                    graphics,
                    layout,
                    mouseX,
                    mouseY);

            return;
        }

        List<MarketWatchItemCatalog.BazaarSuggestion>
                suggestions =
                MarketWatchItemCatalog.bazaarSuggestions(
                            search,
                            Integer.MAX_VALUE);

                suggestions =
                        visibleSuggestions(
                                suggestions,
                                layout.suggestionRows());

        if (suggestions.isEmpty()) {
            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "No matching Bazaar items.",
                    layout.leftX(),
                    layout.suggestionsY() + 4);

            return;
        }

        for (int i = 0;
             i < suggestions.size();
             i++) {

            drawBazaarSuggestion(
                    graphics,
                    font,
                    suggestions.get(i),
                    suggestionRowX(layout),
                    layout.suggestionsY()
                            + i * SUGGESTION_HEIGHT,
                    suggestionRowWidth(layout),
                    mouseX,
                    mouseY);
        }

        drawSuggestionScrollbar(
                graphics,
                layout,
                mouseX,
                mouseY);
    }

    /*
     * DYNAMIC SUGGESTION SCROLLBAR
     *
     * Uses the filtered result count rather than the full catalog size.
     * visibleSuggestions() clamps suggestionOffset whenever the search
     * changes, so the thumb always remains valid.
     */
    private void drawSuggestionScrollbar(
            GuiGraphicsExtractor graphics,
            Layout layout,
            int mouseX,
            int mouseY) {

        if (!suggestionsOpen
                || search.isBlank()) {

            return;
        }

        int resultCount =
                suggestionResultCount();

        int visibleRows =
                Math.max(
                        1,
                        layout.suggestionRows());

        if (resultCount <= visibleRows) {
            return;
        }

        int trackTop =
                layout.suggestionsY();

        int trackBottom =
                trackTop
                        + visibleRows
                        * SUGGESTION_HEIGHT
                        - 2;

        int scrollbarX =
                layout.cardX()
                        + 4;

        boolean hovered =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        scrollbarX - 2,
                        trackTop,
                        RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                        Math.max(
                                0,
                                trackBottom - trackTop));

        configureSuggestionScroll(layout);

        int contentHeight =
                suggestionScroll.contentHeight();

        int scrollPixels =
                suggestionScroll.scrollPixels();

        RotClientUiDraw.drawScrollbar(
                graphics,
                scrollbarX,
                trackTop,
                trackBottom,
                contentHeight,
                scrollPixels,
                hovered,
                suggestionScroll.isThumbDragging());
    }
    private void drawAuctionSuggestion(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchItemCatalog.AuctionSuggestion item,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {

        boolean hover =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        SUGGESTION_HEIGHT - 2);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                SUGGESTION_HEIGHT - 2);

        if (hover) {
            graphics.fill(
                    x,
                    y,
                    x + 2,
                    y + SUGGESTION_HEIGHT - 2,
                    RotClientTheme.HUD_ACCENT);
        }

        ItemStack icon =
                MarketWatchItemIconResolver
                        .auctionIcon(
                                item.category(),
                                item.itemName());

        graphics.item(
                icon,
                x + 7,
                y + 8);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        item.itemName(),
                        width - 112),
                x + 30,
                y + 7,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.text(
                graphics,
                font,
                item.tier(),
                x + 30,
                y + 20,
                RotClientTheme.TEXT_DIM,
                false);

        drawRight(
                graphics,
                font,
                formatCoins(
                        item.lowestBin()),
                x + width - 8,
                y + 7,
                RotClientTheme.TEXT);

        drawRight(
                graphics,
                font,
                item.binCount() + " BIN",
                x + width - 8,
                y + 20,
                RotClientTheme.TEXT_MUTED);
    }

    private void drawBazaarSuggestion(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchItemCatalog.BazaarSuggestion item,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {

        boolean hover =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        SUGGESTION_HEIGHT - 2);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                SUGGESTION_HEIGHT - 2);

        if (hover) {
            graphics.fill(
                    x,
                    y,
                    x + 2,
                    y + SUGGESTION_HEIGHT - 2,
                    RotClientTheme.HUD_ACCENT);
        }

        ItemStack icon =
                MarketWatchItemIconResolver
                        .bazaarIcon(
                                item.productId());

        graphics.item(
                icon,
                x + 7,
                y + 8);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        item.displayName(),
                        width - 120),
                x + 30,
                y + 7,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Spread "
                        + formatPercent(
                        item.spreadPercent()),
                x + 30,
                y + 20);

        drawRight(
                graphics,
                font,
                "Buy "
                        + formatPrice(
                        item.instantBuy()),
                x + width - 8,
                y + 7,
                RotClientTheme.TEXT);

        drawRight(
                graphics,
                font,
                "Sell "
                        + formatPrice(
                        item.instantSell()),
                x + width - 8,
                y + 20,
                RotClientTheme.TEXT_DIM);
    }

    private void drawSelectionAndSettings(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.helpText(
                graphics,
                font,
                "SELECTED ITEM",
                layout.rightX(),
                layout.contentTop());

        int selectedY =
                layout.contentTop() + 12;

        RotClientUiDraw.drawElevatedCard(
                graphics,
                layout.rightX(),
                selectedY,
                layout.rightWidth(),
                66);

        if (kind == Kind.AUCTION_HOUSE) {
            drawSelectedAuction(
                    graphics,
                    font,
                    layout,
                    selectedY,
                    mouseX,
                    mouseY);


            drawQuickBuyPanel(
                    graphics,
                    font,
                    layout,
                    mouseX,
                    mouseY);

            drawAuctionSettings(
                    graphics,
                    font,
                    layout,
                    mouseX,
                    mouseY);

            return;
        }

        drawSelectedBazaar(
                graphics,
                font,
                layout,
                selectedY,
                mouseX,
                mouseY);


        drawQuickBuyPanel(
                graphics,
                font,
                layout,
                mouseX,
                mouseY);

        drawBazaarSettings(
                graphics,
                font,
                layout);
    }

    private void drawSelectedAuction(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            int selectedY,
            int mouseX,
            int mouseY) {

        if (selectedAuction == null) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    "No item selected",
                    layout.rightX() + 10,
                    selectedY + 13,
                    RotClientTheme.TEXT_MUTED,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Choose a result from the left.",
                    layout.rightX() + 10,
                    selectedY + 30);

            return;
        }

        MarketWatchItemCatalog.AuctionStats stats =
                MarketWatchItemCatalog.auctionStats(
                        selectedAuction.itemName(),
                        selectedAuction.tier());

        ItemStack icon =
                MarketWatchItemIconResolver
                        .auctionIcon(
                                selectedAuction.category(),
                                selectedAuction.itemName());

        graphics.item(
                icon,
                layout.rightX() + 9,
                selectedY + 9);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        selectedAuction.itemName(),
                        layout.rightWidth() - 42),
                layout.rightX() + 32,
                selectedY + 8,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                selectedAuction.tier()
                        + "  /  "
                        + selectedAuction.category(),
                layout.rightX() + 32,
                selectedY + 21);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Lowest BIN "
                        + formatCoins(
                        stats.lowestBin())
                        + "  /  Median "
                        + formatCoins(
                        stats.medianBin()),
                layout.rightX() + 10,
                selectedY + 39);

        RotClientUiDraw.helpText(
                graphics,
                font,
                stats.binCount()
                        + " matching BINs  /  "
                        + formatAge(
                        stats.observedAtMillis()),
                layout.rightX() + 10,
                selectedY + 52);
    }

    private void drawSelectedBazaar(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            int selectedY,
            int mouseX,
            int mouseY) {

        if (selectedBazaar == null) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    "No item selected",
                    layout.rightX() + 10,
                    selectedY + 13,
                    RotClientTheme.TEXT_MUTED,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Choose a result from the left.",
                    layout.rightX() + 10,
                    selectedY + 30);

            return;
        }

        MarketWatchItemCatalog.BazaarStats stats =
                MarketWatchItemCatalog.bazaarStats(
                        selectedBazaar.productId());

        ItemStack icon =
                MarketWatchItemIconResolver
                        .bazaarIcon(
                                selectedBazaar.productId());

        graphics.item(
                icon,
                layout.rightX() + 9,
                selectedY + 9);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        stats.displayName(),
                        layout.rightWidth() - 42),
                layout.rightX() + 32,
                selectedY + 8,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Buy "
                        + formatPrice(
                        stats.instantBuy())
                        + "  /  Sell "
                        + formatPrice(
                        stats.instantSell()),
                layout.rightX() + 32,
                selectedY + 21);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Spread "
                        + formatPrice(
                        stats.spreadCoins())
                        + " ("
                        + formatPercent(
                        stats.spreadPercent())
                        + ")",
                layout.rightX() + 10,
                selectedY + 39);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Weekly volume "
                        + formatCount(
                        stats.weeklyVolume())
                        + "  /  "
                        + formatAge(
                        stats.observedAtMillis()),
                layout.rightX() + 10,
                selectedY + 52);
    }

    /*
     * PREMIUM QUICK BUY PANEL
     *
     * Quick Buy is deliberately separated from the selected-item card.
     *
     * The first row contains BUY ALERT PRICE.
     * Immediately below it sits this helper panel.
     *
     * Clicking a preset writes the percentage directly into BUY ALERT PRICE,
     * so the relationship is visible to the user.
     *
     * Manual custom values such as 7%, 12.5%, 11m, etc. remain supported.
     */
    private static final int[] QUICK_BUY_DISCOUNTS = {
            5,
            10,
            15,
            20
    };

    private static final int QUICK_BUY_GAP = 5;
    private static final int QUICK_BUY_HEIGHT = 17;
    private static final int QUICK_BUY_PANEL_HEIGHT = 52;
    private static final int QUICK_BUY_PANEL_INSET = 8;

    private static int quickBuyPanelY(
            Layout layout) {

        /*
         * drawInput rows occupy 39 px.
         */
        return layout.settingsY() + 39;
    }

    private static int quickBuyPanelBottom(
            Layout layout) {

        return quickBuyPanelY(layout)
                + QUICK_BUY_PANEL_HEIGHT;
    }

    private static int postQuickSettingsY(
            Layout layout) {

        return quickBuyPanelBottom(layout)
                + 24;
    }

    private static int quickBuyButtonY(
            Layout layout) {

        return quickBuyPanelY(layout)
                + 32;
    }

    private static int quickBuyButtonWidth(
            Layout layout) {

        int available =
                Math.max(
                        4,
                        layout.rightWidth()
                                - QUICK_BUY_PANEL_INSET * 2
                                - QUICK_BUY_GAP
                                * (QUICK_BUY_DISCOUNTS.length - 1));

        return Math.max(
                1,
                available
                        / QUICK_BUY_DISCOUNTS.length);
    }

    private static int quickBuyButtonX(
            Layout layout,
            int index) {

        int width =
                quickBuyButtonWidth(
                        layout);

        return layout.rightX()
                + QUICK_BUY_PANEL_INSET
                + index
                * (width + QUICK_BUY_GAP);
    }

    private double quickBuyReferencePrice() {

        if (kind == Kind.AUCTION_HOUSE) {

            String itemName = "";
            String tier = "";

            if (selectedAuction != null) {

                itemName =
                        selectedAuction.itemName();

                tier =
                        selectedAuction.tier();

            } else if (editingAuctionWatch != null) {

                itemName =
                        editingAuctionWatch.itemName;

                tier =
                        editingAuctionWatch.tier;
            }

            if (itemName.isBlank()) {
                return 0.0D;
            }

            MarketWatchItemCatalog.AuctionStats stats =
                    MarketWatchItemCatalog.auctionStats(
                            itemName,
                            tier);

            if (!stats.available()
                    || stats.medianBin() <= 0L) {

                return 0.0D;
            }

            return stats.medianBin();
        }

        if (kind == Kind.BAZAAR) {

            String productId = "";

            if (selectedBazaar != null) {

                productId =
                        selectedBazaar.productId();

            } else if (editingBazaarWatch != null) {

                productId =
                        editingBazaarWatch.productId;
            }

            if (productId.isBlank()) {
                return 0.0D;
            }

            MarketWatchItemCatalog.BazaarStats stats =
                    MarketWatchItemCatalog.bazaarStats(
                            productId);

            if (!stats.available()
                    || stats.instantBuy() <= 0.0D) {

                return 0.0D;
            }

            return stats.instantBuy();
        }

        return 0.0D;
    }

    private String quickBuyReferenceText(
            double referencePrice) {

        if (!Double.isFinite(referencePrice)
                || referencePrice <= 0.0D) {

            return "NO LIVE PRICE";
        }

        if (kind == Kind.AUCTION_HOUSE) {

            return "Median "
                    + formatCoins(
                    Math.round(
                            referencePrice));
        }

        return "Buy "
                + formatPrice(
                referencePrice);
    }

    private void drawQuickBuyPanel(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            int mouseX,
            int mouseY) {

        int x =
                layout.rightX();

        int y =
                quickBuyPanelY(
                        layout);

        int width =
                layout.rightWidth();

        double referencePrice =
                quickBuyReferencePrice();

        boolean enabled =
                Double.isFinite(referencePrice)
                        && referencePrice > 0.0D;

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                QUICK_BUY_PANEL_HEIGHT);

        /*
         * Accent strip.
         */
        graphics.fill(
                x,
                y,
                x + 2,
                y + QUICK_BUY_PANEL_HEIGHT,
                RotClientTheme.HUD_ACCENT);

        /*
         * Header.
         */
        RotClientUiDraw.text(
                graphics,
                font,
                "\u2193 QUICK BUY TARGET",
                x + 9,
                y + 6,
                RotClientTheme.TEXT,
                true);

        drawRight(
                graphics,
                font,
                quickBuyReferenceText(
                        referencePrice),
                x + width - 9,
                y + 6,
                enabled
                        ? RotClientTheme.TEXT_DIM
                        : RotClientTheme.TEXT_MUTED);

        /*
         * Explicitly tell the user what these buttons modify.
         */
        RotClientUiDraw.helpText(
                graphics,
                font,
                fit(
                        font,
                        "Quickly fill the Buy Alert Price above",
                        Math.max(
                                20,
                                width - 18)),
                x + 9,
                y + 19);

        int buttonWidth =
                quickBuyButtonWidth(
                        layout);

        for (int i = 0;
                i < QUICK_BUY_DISCOUNTS.length;
                i++) {

            int discount =
                    QUICK_BUY_DISCOUNTS[i];

            RotClientUiDraw.drawButton(
                    graphics,
                    font,
                    mouseX,
                    mouseY,
                    quickBuyButtonX(
                            layout,
                            i),
                    quickBuyButtonY(
                            layout),
                    buttonWidth,
                    QUICK_BUY_HEIGHT,
                    "\u2193 " + discount + "%",
                    false,
                    enabled);
        }
    }

    private boolean quickBuyTargetClick(
            int mouseX,
            int mouseY,
            Layout layout) {

        /*
         * Require live data because percentage alerts are resolved using
         * the current reference price when saved.
         */
        double referencePrice =
                quickBuyReferencePrice();

        if (!Double.isFinite(referencePrice)
                || referencePrice <= 0.0D) {

            return false;
        }

        int buttonWidth =
                quickBuyButtonWidth(
                        layout);

        int buttonY =
                quickBuyButtonY(
                        layout);

        for (int i = 0;
                i < QUICK_BUY_DISCOUNTS.length;
                i++) {

            if (!RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    quickBuyButtonX(
                            layout,
                            i),
                    buttonY,
                    buttonWidth,
                    QUICK_BUY_HEIGHT)) {

                continue;
            }

            int discount =
                    QUICK_BUY_DISCOUNTS[i];

            double targetPrice =
                    referencePrice
                            * (1.0D
                            - discount / 100.0D);

            if (!Double.isFinite(targetPrice)
                    || targetPrice <= 0.0D) {

                return true;
            }

            /*
             * Preset buttons are calculators:
             *
             * Clicking -10% immediately shows the resulting coin price.
             *
             * Manual input remains separate, so the user can still type
             * "10%" directly into BUY ALERT PRICE if preferred.
             */
            if (kind == Kind.AUCTION_HOUSE) {

                auctionMaxPrice =
                        editableAmount(
                                Math.max(
                                        1L,
                                        Math.round(
                                                targetPrice)));

            } else if (kind == Kind.BAZAAR) {

                bazaarMaxBuy =
                        editableAmount(
                                targetPrice);

            } else {

                return false;
            }

            focusedField = 1;
            suggestionsOpen = false;
            error = "";

            return true;
        }

        return false;
    }
    private void drawAuctionSettings(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            int mouseX,
            int mouseY) {

        int y =
                layout.settingsY();

        RotClientUiDraw.helpText(
                graphics,
                font,
                "ALERT SETTINGS",
                layout.rightX(),
                y - 12);

        /*
         * Primary action gets the complete row.
         */
        drawInput(
                graphics,
                font,
                layout.rightX(),
                y,
                layout.rightWidth(),
                "BUY ALERT PRICE",
                auctionMaxPrice,
                "optional",
                focusedField == 1);

        /*
         * Cooldown intentionally remains an internal setting.
         * New watches retain the existing 60-second default.
         */
        int toggleY =
                postQuickSettingsY(
                        layout);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "BIN ONLY",
                layout.rightX(),
                toggleY);

        RotClientUiDraw.drawToggle(
                graphics,
                layout.rightX(),
                toggleY + 13,
                auctionBinOnly,
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        layout.rightX() - 4,
                        toggleY + 9,
                        QolUtilityUiMath.TOGGLE_WIDTH + 8,
                        QolUtilityUiMath.TOGGLE_HEIGHT + 8));
    }
    private void drawBazaarSettings(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout) {

        int gap = 8;

        int fieldWidth =
                Math.max(
                        80,
                        (layout.rightWidth() - gap) / 2);

        int rightFieldX =
                layout.rightX()
                        + fieldWidth
                        + gap;

        int rightWidth =
                layout.rightWidth()
                        - fieldWidth
                        - gap;

        int y =
                layout.settingsY();

        RotClientUiDraw.helpText(
                graphics,
                font,
                "ALERT SETTINGS",
                layout.rightX(),
                y - 12);

        /*
         * Main alert conditions.
         */
        drawInput(
                graphics,
                font,
                layout.rightX(),
                y,
                fieldWidth,
                "BUY ALERT PRICE",
                bazaarMaxBuy,
                "optional",
                focusedField == 1);

        drawInput(
                graphics,
                font,
                rightFieldX,
                y,
                rightWidth,
                "SELL ALERT PRICE",
                bazaarMinSell,
                "optional",
                focusedField == 2);

        /*
         * Quick Buy card occupies the next full row.
         *
         * Advanced conditions do not begin until there is a full visual gap
         * below that card.
         */
        y =
                postQuickSettingsY(
                        layout);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "MORE CONDITIONS",
                layout.rightX(),
                y - 12);

        drawInput(
                graphics,
                font,
                layout.rightX(),
                y,
                fieldWidth,
                "MIN SPREAD COINS",
                bazaarMinSpreadCoins,
                "optional",
                focusedField == 3);

        drawInput(
                graphics,
                font,
                rightFieldX,
                y,
                rightWidth,
                "MIN SPREAD %",
                bazaarMinSpreadPercent,
                "optional",
                focusedField == 4);

        y += 43;

        /*
         * One full-width field is cleaner here than leaving an empty column
         * where Cooldown used to be.
         */
        drawInput(
                graphics,
                font,
                layout.rightX(),
                y,
                layout.rightWidth(),
                "MIN WEEKLY VOLUME",
                bazaarMinWeeklyVolume,
                "optional",
                focusedField == 5);
    }
    private void drawFooter(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            int mouseX,
            int mouseY) {

        int cancelWidth = 82;
        int saveWidth = 104;

        int footerTop =
                Math.max(
                        layout.cardY() + 1,
                        layout.footerY() - 6);

        /*
         * Footer is rendered after the scroll body and is opaque.
         * Nothing underneath can visually overlap Save / Cancel.
         */
        graphics.fill(
                layout.cardX() + 1,
                footerTop,
                layout.cardX()
                        + layout.cardWidth()
                        - 1,
                layout.cardY()
                        + layout.cardHeight()
                        - 1,
                RotClientTheme.SURFACE_ALT);

        graphics.fill(
                layout.cardX() + 1,
                footerTop,
                layout.cardX()
                        + layout.cardWidth()
                        - 1,
                footerTop + 1,
                RotClientTheme.DIVIDER);

        int saveX =
                layout.cardX()
                        + layout.cardWidth()
                        - 12
                        - saveWidth;

        int cancelX =
                saveX
                        - 8
                        - cancelWidth;

        if (!error.isBlank()) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    fit(
                            font,
                            error,
                            Math.max(
                                    80,
                                    cancelX
                                            - layout.cardX()
                                            - 22)),
                    layout.cardX() + 12,
                    layout.footerY() + 6,
                    RotClientTheme.WARNING,
                    false);
        }

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                cancelX,
                layout.footerY(),
                cancelWidth,
                "CANCEL",
                false,
                true);

        RotClientUiDraw.drawButton(
                graphics,
                font,
                mouseX,
                mouseY,
                saveX,
                layout.footerY(),
                saveWidth,
                isEditing()
                        ? "SAVE CHANGES"
                        : "SAVE WATCH",
                true,
                true);
    }

    private void configureSuggestionScroll(
            Layout layout) {

        int visibleRows =
                Math.max(
                        1,
                        layout.suggestionRows());

        int resultCount =
                suggestionResultCount();

        suggestionScroll.setBounds(
                resultCount * SUGGESTION_HEIGHT,
                visibleRows * SUGGESTION_HEIGHT);

        if (!suggestionScroll.isThumbDragging()) {
            suggestionScroll.setScrollPixels(
                    suggestionOffset * SUGGESTION_HEIGHT);
        }
    }

    private void syncSuggestionOffsetFromScroll(
            Layout layout) {

        int visibleRows =
                Math.max(
                        1,
                        layout.suggestionRows());

        int maxOffset =
                Math.max(
                        0,
                        suggestionResultCount() - visibleRows);

        suggestionOffset =
                Math.max(
                        0,
                        Math.min(
                                maxOffset,
                                (int) Math.round(
                                        suggestionScroll.scrollPixels()
                                                / (double) SUGGESTION_HEIGHT)));
    }

    private boolean handleSuggestionScrollbarClick(
            int mouseX,
            int mouseY,
            Layout layout) {

        if (!suggestionsOpen
                || search.isBlank()) {

            return false;
        }

        int visibleRows =
                Math.max(
                        1,
                        layout.suggestionRows());

        int resultCount =
                suggestionResultCount();

        if (resultCount <= visibleRows) {
            return false;
        }

        int trackTop =
                layout.suggestionsY();

        int trackBottom =
                trackTop
                        + visibleRows * SUGGESTION_HEIGHT
                        - 2;

        int scrollbarX =
                layout.cardX()
                        + 4;

        if (!RotClientUiDraw.inside(
                mouseX,
                mouseY,
                scrollbarX - 2,
                trackTop,
                RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                Math.max(
                        0,
                        trackBottom - trackTop))) {

            return false;
        }

        configureSuggestionScroll(layout);

        if (!suggestionScroll.beginThumbDrag(
                mouseY,
                trackTop,
                trackBottom,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {

            suggestionScroll.clickTrack(
                    mouseY,
                    trackTop,
                    trackBottom,
                    RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT);
        }

        syncSuggestionOffsetFromScroll(layout);

        /*
         * Consume the click here. It must never reach the normal form
         * click handling, otherwise suggestionsOpen would be cleared.
         */
        return true;
    }
    boolean mouseClicked(
            int button,
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (kind == Kind.NONE
                || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            return false;
        }

        Layout layout =
                layout(
                        left,
                        top,
                        right,
                        bottom);

        int cancelWidth = 82;
        int saveWidth = 104;

        int cancelX =
                layout.cardX()
                        + layout.cardWidth()
                        - 12
                        - saveWidth
                        - 8
                        - cancelWidth;

        int saveX =
                layout.cardX()
                        + layout.cardWidth()
                        - 12
                        - saveWidth;

        /*
         * Footer is checked FIRST.
         * This prevents lower Bazaar fields from swallowing Save/Cancel.
         */
        if (RotClientUiDraw.inside(
                mouseX,
                mouseY,
                cancelX,
                layout.footerY(),
                cancelWidth,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            cancel();
            return true;
        }

        if (RotClientUiDraw.inside(
                mouseX,
                mouseY,
                saveX,
                layout.footerY(),
                saveWidth,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            submit();
            return true;
        }

        bodyScroll.setBounds(
                contentHeight(),
                Math.max(
                        0,
                        layout.bodyBottom()
                                - layout.bodyTop()));

        int scrollbarHitX =
                layout.cardX()
                        + layout.cardWidth()
                        - RotClientUiDraw.SCROLLBAR_HIT_WIDTH
                        - 1;

        if (bodyScroll.canScroll()
                && RotClientUiDraw.inside(
                mouseX,
                mouseY,
                scrollbarHitX,
                layout.bodyTop(),
                RotClientUiDraw.SCROLLBAR_HIT_WIDTH,
                Math.max(
                        0,
                        layout.bodyBottom()
                                - layout.bodyTop()))) {

            if (!bodyScroll.beginThumbDrag(
                    mouseY,
                    layout.bodyTop(),
                    layout.bodyBottom(),
                    RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {

                bodyScroll.clickTrack(
                        mouseY,
                        layout.bodyTop(),
                        layout.bodyBottom(),
                        RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT);
            }

            return true;
        }

        layout =
                layout.scrolled(
                        -bodyScroll.scrollPixels());

        if (handleSuggestionScrollbarClick(
                mouseX,
                mouseY,
                layout)) {

            return true;
        }

        if (RotClientUiDraw.inside(
                mouseX,
                mouseY,
                layout.leftX(),
                layout.searchY(),
                layout.leftWidth(),
                SEARCH_BOX_HEIGHT)) {

            focusedField = 0;
            suggestionsOpen = true;
            error = "";

            return true;
        }

        if (suggestionsOpen
                && !search.isBlank()) {

            if (kind == Kind.AUCTION_HOUSE) {
                List<MarketWatchItemCatalog.AuctionSuggestion>
                        suggestions =
                        MarketWatchItemCatalog.auctionSuggestions(
                            search,
                            Integer.MAX_VALUE);

                suggestions =
                        visibleSuggestions(
                                suggestions,
                                layout.suggestionRows());

                for (int i = 0;
                     i < suggestions.size();
                     i++) {

                    int rowY =
                            layout.suggestionsY()
                                    + i * SUGGESTION_HEIGHT;

                    if (RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            suggestionRowX(layout),
                            rowY,
                            suggestionRowWidth(layout),
                            SUGGESTION_HEIGHT - 2)) {

                        selectAuction(
                                suggestions.get(i));

                        return true;
                    }
                }
            } else {
                List<MarketWatchItemCatalog.BazaarSuggestion>
                        suggestions =
                        MarketWatchItemCatalog.bazaarSuggestions(
                            search,
                            Integer.MAX_VALUE);

                suggestions =
                        visibleSuggestions(
                                suggestions,
                                layout.suggestionRows());

                for (int i = 0;
                     i < suggestions.size();
                     i++) {

                    int rowY =
                            layout.suggestionsY()
                                    + i * SUGGESTION_HEIGHT;

                    if (RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            suggestionRowX(layout),
                            rowY,
                            suggestionRowWidth(layout),
                            SUGGESTION_HEIGHT - 2)) {

                        selectBazaar(
                                suggestions.get(i));

                        return true;
                    }
                }
            }
        }

        if (quickBuyTargetClick(
                mouseX,
                mouseY,
                layout)) {

            return true;
        }

        if (kind == Kind.AUCTION_HOUSE) {
            return auctionSettingsClick(
                    mouseX,
                    mouseY,
                    layout);
        }

        return bazaarSettingsClick(
                mouseX,
                mouseY,
                layout);
    }

    private boolean auctionSettingsClick(
            int mouseX,
            int mouseY,
            Layout layout) {

        int y =
                layout.settingsY();

        if (insideInput(
                mouseX,
                mouseY,
                layout.rightX(),
                y,
                layout.rightWidth())) {

            focusedField = 1;
            suggestionsOpen = false;
            return true;
        }

        int toggleY =
                postQuickSettingsY(
                        layout);

        if (RotClientUiDraw.inside(
                mouseX,
                mouseY,
                layout.rightX() - 4,
                toggleY + 9,
                QolUtilityUiMath.TOGGLE_WIDTH + 8,
                QolUtilityUiMath.TOGGLE_HEIGHT + 8)) {

            auctionBinOnly =
                    !auctionBinOnly;

            focusedField = -1;
            suggestionsOpen = false;

            return true;
        }

        focusedField = -1;
        suggestionsOpen = false;

        return true;
    }
    private boolean bazaarSettingsClick(
            int mouseX,
            int mouseY,
            Layout layout) {

        int gap = 8;

        int fieldWidth =
                Math.max(
                        80,
                        (layout.rightWidth() - gap) / 2);

        int rightX =
                layout.rightX()
                        + fieldWidth
                        + gap;

        int rightWidth =
                layout.rightWidth()
                        - fieldWidth
                        - gap;

        int y =
                layout.settingsY();

        if (insideInput(
                mouseX,
                mouseY,
                layout.rightX(),
                y,
                fieldWidth)) {

            focusedField = 1;
            suggestionsOpen = false;
            return true;
        }

        if (insideInput(
                mouseX,
                mouseY,
                rightX,
                y,
                rightWidth)) {

            focusedField = 2;
            suggestionsOpen = false;
            return true;
        }

        y =
                postQuickSettingsY(
                        layout);

        if (insideInput(
                mouseX,
                mouseY,
                layout.rightX(),
                y,
                fieldWidth)) {

            focusedField = 3;
            suggestionsOpen = false;
            return true;
        }

        if (insideInput(
                mouseX,
                mouseY,
                rightX,
                y,
                rightWidth)) {

            focusedField = 4;
            suggestionsOpen = false;
            return true;
        }

        y += 43;

        if (insideInput(
                mouseX,
                mouseY,
                layout.rightX(),
                y,
                layout.rightWidth())) {

            focusedField = 5;
            suggestionsOpen = false;
            return true;
        }

        focusedField = -1;
        suggestionsOpen = false;

        return true;
    }
    boolean mouseDragged(
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (kind == Kind.NONE) {
            return false;
        }

        Layout layout =
                layout(
                        left,
                        top,
                        right,
                        bottom);

        int viewport =
                Math.max(
                        0,
                        layout.bodyBottom()
                                - layout.bodyTop());

        bodyScroll.setBounds(
                contentHeight(),
                viewport);

        Layout visibleLayout =
                layout.scrolled(
                        -bodyScroll.scrollPixels());

        /*
         * Keep the suggestion scrollbar geometry synchronized with the
         * currently visible/scrolled editor layout.
         */
        configureSuggestionScroll(
                visibleLayout);

        int visibleRows =
                Math.max(
                        1,
                        visibleLayout.suggestionRows());

        int trackTop =
                visibleLayout.suggestionsY();

        int trackBottom =
                trackTop
                        + visibleRows
                        * SUGGESTION_HEIGHT
                        - 2;

        /*
         * Suggestion scrollbar gets drag priority.
         */
        if (suggestionScroll.dragThumbTo(
                mouseY,
                trackTop,
                trackBottom,
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT)) {

            syncSuggestionOffsetFromScroll(
                    visibleLayout);

            return true;
        }

        /*
         * Otherwise drag the main form scrollbar.
         */
        return bodyScroll.dragThumbTo(
                mouseY,
                layout.bodyTop(),
                layout.bodyBottom(),
                RotClientUiDraw.SCROLLBAR_MIN_THUMB_HEIGHT);
    }

    boolean mouseReleased() {

        boolean suggestionReleased =
                suggestionScroll.endThumbDrag();

        boolean bodyReleased =
                bodyScroll.endThumbDrag();

        return suggestionReleased
                || bodyReleased;
    }
    boolean mouseScrolled(
            double verticalAmount,
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (kind == Kind.NONE) {
            return false;
        }

        Layout layout =
                layout(
                        left,
                        top,
                        right,
                        bottom);

        int viewport =
                Math.max(
                        0,
                        layout.bodyBottom()
                                - layout.bodyTop());

        bodyScroll.setBounds(
                contentHeight(),
                viewport);

        Layout visibleLayout =
                layout.scrolled(
                        -bodyScroll.scrollPixels());

        int suggestionsTop =
                visibleLayout.suggestionsY();

        int suggestionsHeight =
                visibleLayout.suggestionRows()
                        * SUGGESTION_HEIGHT;

        if (suggestionsOpen
                && !search.isBlank()
                && RotClientUiDraw.inside(
                mouseX,
                mouseY,
                visibleLayout.leftX(),
                suggestionsTop,
                visibleLayout.leftWidth(),
                suggestionsHeight)) {

            return scrollSuggestions(
                    verticalAmount,
                    visibleLayout.suggestionRows());
        }
        if (!RotClientUiDraw.inside(
                mouseX,
                mouseY,
                layout.cardX(),
                layout.bodyTop(),
                layout.cardWidth(),
                viewport)) {

            return false;
        }

        if (!bodyScroll.canScroll()) {
            return true;
        }

        bodyScroll.scrollBySteps(
                verticalAmount,
                34);

        return true;
    }
    boolean captureChar(
            String incoming,
            boolean allowed) {

        if (kind == Kind.NONE
                || focusedField < 0) {

            return false;
        }

        if (!allowed
                || incoming == null
                || incoming.isEmpty()) {

            return true;
        }

        String current =
                activeValue();

        /*
         * Buy-alert percentage shortcut.
         *
         * Handle % before the existing numeric-character validator.
         * All other input still goes through the original validation path.
         */
        if (buyPriceField()
                && current != null
                && current.endsWith("%")) {

            return true;
        }

        if ("%".equals(incoming)
                && buyPriceField()
                && canAppendPercent(current)) {

            setActiveValue(
                    current + incoming);

            error = "";
            return true;
        }

        if (current == null) {
            return true;
        }

        if (current.codePointCount(
                0,
                current.length()) >= 64) {

            return true;
        }

        if (focusedField > 0
                && !validNumericInput(incoming)) {

            return true;
        }

        setActiveValue(
                current + incoming);

        if (focusedField == 0) {
            clearSelectionForSearch();
            suggestionsOpen = true;
        }

        error = "";
        return true;
    }

    boolean captureKey(int key) {
        if (kind == Kind.NONE) {
            return false;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            cancel();
            return true;
        }

        if (key == GLFW.GLFW_KEY_TAB) {
            int fieldCount =
                    kind == Kind.AUCTION_HOUSE
                            ? 3
                            : 7;

            focusedField =
                    Math.floorMod(
                            focusedField + 1,
                            fieldCount);

            suggestionsOpen =
                    focusedField == 0;

            return true;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE
                && focusedField >= 0) {

            String current =
                    activeValue();

            if (current != null
                    && !current.isEmpty()) {

                int next =
                        current.offsetByCodePoints(
                                current.length(),
                                -1);

                setActiveValue(
                        current.substring(
                                0,
                                next));

                if (focusedField == 0) {
                    clearSelectionForSearch();
                    suggestionsOpen = true;
                }
            }

            error = "";
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_KP_ENTER) {

            /*
             * Enter on search selects the first visible result.
             * Enter elsewhere saves.
             */
            if (focusedField == 0
                    && suggestionsOpen
                    && !search.isBlank()) {

                selectFirstSuggestion();
                return true;
            }

            submit();
            return true;
        }

        return focusedField >= 0;
    }

    private <T> List<T> visibleSuggestions(
            List<T> suggestions,
            int visibleRows) {

        if (suggestions == null
                || suggestions.isEmpty()
                || visibleRows <= 0) {

            suggestionOffset = 0;
            return List.of();
        }

        int rows =
                Math.max(
                        1,
                        visibleRows);

        int maxOffset =
                Math.max(
                        0,
                        suggestions.size() - rows);

        suggestionOffset =
                Math.max(
                        0,
                        Math.min(
                                suggestionOffset,
                                maxOffset));

        int end =
                Math.min(
                        suggestions.size(),
                        suggestionOffset + rows);

        return suggestions.subList(
                suggestionOffset,
                end);
    }

    private int suggestionResultCount() {
        if (search.isBlank()) {
            return 0;
        }

        if (kind == Kind.AUCTION_HOUSE) {
            return MarketWatchItemCatalog
                    .auctionSuggestions(
                            search,
                            Integer.MAX_VALUE)
                    .size();
        }

        if (kind == Kind.BAZAAR) {
            return MarketWatchItemCatalog
                    .bazaarSuggestions(
                            search,
                            Integer.MAX_VALUE)
                    .size();
        }

        return 0;
    }

    private boolean scrollSuggestions(
            double verticalAmount,
            int visibleRows) {

        if (!suggestionsOpen
                || search.isBlank()
                || verticalAmount == 0.0D) {

            return false;
        }

        int resultCount =
                suggestionResultCount();

        int maxOffset =
                Math.max(
                        0,
                        resultCount
                                - Math.max(
                                1,
                                visibleRows));

        if (maxOffset <= 0) {
            suggestionOffset = 0;
            return true;
        }

        int direction =
                verticalAmount > 0.0D
                        ? -1
                        : 1;

        suggestionOffset =
                Math.max(
                        0,
                        Math.min(
                                maxOffset,
                                suggestionOffset + direction));

        return true;
    }
    private void selectFirstSuggestion() {
        if (kind == Kind.AUCTION_HOUSE) {
            List<MarketWatchItemCatalog.AuctionSuggestion>
                    suggestions =
                    MarketWatchItemCatalog.auctionSuggestions(
                            search,
                            1);

            if (!suggestions.isEmpty()) {
                selectAuction(
                        suggestions.getFirst());
            }

            return;
        }

        List<MarketWatchItemCatalog.BazaarSuggestion>
                suggestions =
                MarketWatchItemCatalog.bazaarSuggestions(
                        search,
                        1);

        if (!suggestions.isEmpty()) {
            selectBazaar(
                    suggestions.getFirst());
        }
    }

    private void selectAuction(
            MarketWatchItemCatalog.AuctionSuggestion item) {

        selectedAuction = item;
        selectedBazaar = null;

        search =
                item.itemName();

        suggestionsOpen = false;
        focusedField = 1;
        error = "";
    }

    private void selectBazaar(
            MarketWatchItemCatalog.BazaarSuggestion item) {

        selectedBazaar = item;
        selectedAuction = null;

        search =
                item.displayName();

        suggestionsOpen = false;
        focusedField = 1;
        error = "";
    }

    private void clearSelectionForSearch() {
        selectedAuction = null;
        selectedBazaar = null;
        suggestionOffset = 0;
        suggestionScroll.reset();
    }

    private void submit() {
        if (kind == Kind.AUCTION_HOUSE) {
            submitAuction();
            return;
        }

        if (kind == Kind.BAZAAR) {
            submitBazaar();
        }
    }

    private void submitAuction() {
        if (selectedAuction == null
                && editingAuctionWatch == null) {

            fail(
                    0,
                    "Select an item from the search results.");

            suggestionsOpen = true;
            return;
        }

        if (editingAuctionWatch != null
                && selectedAuction == null
                && !search.trim().equalsIgnoreCase(
                        editingOriginalSearch.trim())) {

            fail(
                    0,
                    "Select the changed item from the search results.");

            suggestionsOpen = true;
            return;
        }

        Long maxPrice =
                resolveAuctionBuyPrice();

        if (maxPrice == null
                || maxPrice <= 0L) {

            fail(
                    1,
                    "Enter a buy price like 11m, or a discount like 10%.");

            return;
        }

        Long cooldown =
                parseLongAmount(
                        auctionCooldown);

        if (cooldown == null) {
            fail(
                    2,
                    "Cooldown is invalid.");

            return;
        }

        if (editingAuctionWatch != null) {
            MarketWatchAuctionWatch updated =
                    editingAuctionWatch.copy();

            if (selectedAuction != null) {
                boolean sameItem =
                        updated.itemName
                                .equalsIgnoreCase(
                                        selectedAuction.itemName())
                                && updated.tier
                                .equalsIgnoreCase(
                                        selectedAuction.tier());

                /*
                 * itemId cannot safely belong to a different selected item.
                 */
                if (!sameItem) {
                    updated.itemId = "";
                }

                updated.itemName =
                        selectedAuction.itemName();

                updated.tier =
                        selectedAuction.tier();
            }

            updated.binOnly =
                    auctionBinOnly;

            updated.maxPriceCoins =
                    maxPrice;

            updated.cooldownSeconds =
                    cooldown;

            updated.normalize();

            if (!MarketWatchRuntime.updateAuctionWatch(
                    updated)) {

                error =
                        "Could not update Auction House watch.";

                return;
            }

            cancel();
            return;
        }

        MarketWatchAuctionWatch created =
                MarketWatchRuntime.createAuctionWatch(
                        selectedAuction.itemName(),
                        selectedAuction.tier(),
                        auctionBinOnly,
                        maxPrice,
                        cooldown);

        if (created == null) {
            error =
                    "Could not save Auction House watch.";

            return;
        }

        cancel();
    }

    private void submitBazaar() {
        if (selectedBazaar == null
                && editingBazaarWatch == null) {

            fail(
                    0,
                    "Select an item from the search results.");

            suggestionsOpen = true;
            return;
        }

        if (editingBazaarWatch != null
                && selectedBazaar == null
                && !search.trim().equalsIgnoreCase(
                        editingOriginalSearch.trim())) {

            fail(
                    0,
                    "Select the changed item from the search results.");

            suggestionsOpen = true;
            return;
        }

        Double maxBuy =
                resolveBazaarBuyPrice();

        Double minSell =
                parseAmount(
                        bazaarMinSell);

        Double spreadCoins =
                parseAmount(
                        bazaarMinSpreadCoins);

        Double spreadPercent =
                parseAmount(
                        bazaarMinSpreadPercent);

        Long weeklyVolume =
                parseLongAmount(
                        bazaarMinWeeklyVolume);

        Long cooldown =
                parseLongAmount(
                        bazaarCooldown);

        if (maxBuy == null) {
            fail(
                    1,
                    "Buy alert must be a price like 11m or a discount like 10%.");
            return;
        }

        if (minSell == null) {
            fail(
                    2,
                    "Min instant sell is invalid.");
            return;
        }

        if (spreadCoins == null) {
            fail(
                    3,
                    "Spread coins is invalid.");
            return;
        }

        if (spreadPercent == null) {
            fail(
                    4,
                    "Spread % is invalid.");
            return;
        }

        if (weeklyVolume == null) {
            fail(
                    5,
                    "Weekly volume is invalid.");
            return;
        }

        if (cooldown == null) {
            fail(
                    6,
                    "Cooldown is invalid.");
            return;
        }

        if (maxBuy <= 0.0D
                && minSell <= 0.0D
                && spreadCoins <= 0.0D
                && spreadPercent <= 0.0D) {

            error =
                    "Set at least one price or spread threshold.";

            return;
        }

        if (editingBazaarWatch != null) {
            MarketWatchBazaarWatch updated =
                    editingBazaarWatch.copy();

            if (selectedBazaar != null) {
                updated.productId =
                        selectedBazaar.productId();
            }

            updated.maxInstantBuyPrice =
                    maxBuy;

            updated.minInstantSellPrice =
                    minSell;

            updated.minSpreadCoins =
                    spreadCoins;

            updated.minSpreadPercent =
                    spreadPercent;

            updated.minWeeklyVolume =
                    weeklyVolume;

            updated.cooldownSeconds =
                    cooldown;

            updated.normalize();

            if (!MarketWatchRuntime.updateBazaarWatch(
                    updated)) {

                error =
                        "Could not update Bazaar watch.";

                return;
            }

            cancel();
            return;
        }

        MarketWatchBazaarWatch created =
                MarketWatchRuntime.createBazaarWatch(
                        selectedBazaar.productId(),
                        maxBuy,
                        minSell,
                        spreadCoins,
                        spreadPercent,
                        weeklyVolume,
                        cooldown);

        if (created == null) {
            error =
                    "Could not save Bazaar watch.";

            return;
        }

        cancel();
    }

    private void fail(
            int field,
            String message) {

        focusedField = field;
        error = message;
    }

    private void cancel() {
        kind = Kind.NONE;

        focusedField = -1;
        suggestionsOpen = false;
        suggestionOffset = 0;
        suggestionScroll.reset();
        selectedAuction = null;
        selectedBazaar = null;

        editingAuctionWatch = null;
        editingBazaarWatch = null;
        editingOriginalSearch = "";


        error = "";
    }

    private String activeValue() {
        if (focusedField == 0) {
            return search;
        }

        if (kind == Kind.AUCTION_HOUSE) {
            return switch (focusedField) {
                case 1 -> auctionMaxPrice;
                case 2 -> auctionCooldown;
                default -> null;
            };
        }

        if (kind == Kind.BAZAAR) {
            return switch (focusedField) {
                case 1 -> bazaarMaxBuy;
                case 2 -> bazaarMinSell;
                case 3 -> bazaarMinSpreadCoins;
                case 4 -> bazaarMinSpreadPercent;
                case 5 -> bazaarMinWeeklyVolume;
                case 6 -> bazaarCooldown;
                default -> null;
            };
        }

        return null;
    }

    private void setActiveValue(
            String value) {

        if (focusedField == 0) {
            search = value;
            return;
        }

        if (kind == Kind.AUCTION_HOUSE) {
            switch (focusedField) {
                case 1 ->
                        auctionMaxPrice = value;

                case 2 ->
                        auctionCooldown = value;

                default -> {
                }
            }

            return;
        }

        if (kind == Kind.BAZAAR) {
            switch (focusedField) {
                case 1 ->
                        bazaarMaxBuy = value;

                case 2 ->
                        bazaarMinSell = value;

                case 3 ->
                        bazaarMinSpreadCoins = value;

                case 4 ->
                        bazaarMinSpreadPercent = value;

                case 5 ->
                        bazaarMinWeeklyVolume = value;

                case 6 ->
                        bazaarCooldown = value;

                default -> {
                }
            }
        }
    }

    private static void drawSearchBox(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String value,
            boolean focused) {

        RotClientTheme.drawInset(
                graphics,
                x,
                y,
                width,
                SEARCH_BOX_HEIGHT,
                focused);

        boolean empty =
                value == null
                        || value.isBlank();

        String shown = "";

        if (!empty) {
            shown =
                    fitFromRight(
                            font,
                            value,
                            width - 18);
        } else if (!focused) {
            shown =
                    "Type an item name...";
        }

        if (!shown.isEmpty()) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    shown,
                    x + 8,
                    y + 8,
                    empty
                            ? RotClientTheme.TEXT_MUTED
                            : RotClientTheme.TEXT,
                    false);
        }

        /*
         * Permanent caret while focused. This is intentionally not blinking:
         * it should always be obvious which field owns keyboard input.
         */
        if (focused) {
            int cursorX =
                    x + 8;

            if (!empty) {
                cursorX +=
                        font.width(shown);
            }

            cursorX =
                    Math.min(
                            x + width - 6,
                            cursorX);

            graphics.fill(
                    cursorX,
                    y + 5,
                    cursorX + 1,
                    y + SEARCH_BOX_HEIGHT - 5,
                    RotClientTheme.TEXT);
        }
    }

    private static void drawInput(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String label,
            String value,
            String placeholder,
            boolean focused) {

        /*
         * Empty fields deliberately render no placeholder inside the box.
         * Example/help text lives beside the label instead, so it cannot be
         * mistaken for a saved value.
         */
        RotClientUiDraw.text(
                graphics,
                font,
                label,
                x,
                y,
                focused
                        ? RotClientTheme.HUD_ACCENT
                        : RotClientTheme.TEXT_MUTED,
                focused);

        if (placeholder != null
                && !placeholder.isBlank()) {

            String hint =
                    fit(
                            font,
                            placeholder,
                            Math.max(
                                    20,
                                    width / 2));

            RotClientUiDraw.text(
                    graphics,
                    font,
                    hint,
                    x + width
                            - font.width(hint),
                    y,
                    RotClientTheme.TEXT_MUTED,
                    false);
        }

        int boxY =
                y + 12;

        int boxH = 25;

        RotClientTheme.drawInset(
                graphics,
                x,
                boxY,
                width,
                boxH,
                focused);

        boolean empty =
                value == null
                        || value.isBlank();

        String shown =
                empty
                        ? ""
                        : fitFromRight(
                                font,
                                value,
                                width - 16);

        if (!shown.isEmpty()) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    shown,
                    x + 7,
                    boxY + 8,
                    RotClientTheme.TEXT,
                    false);
        }

        if (focused) {
            int cursorX =
                    x + 7;

            if (!shown.isEmpty()) {
                cursorX +=
                        font.width(shown);
            }

            cursorX =
                    Math.min(
                            x + width - 5,
                            cursorX);

            graphics.fill(
                    cursorX,
                    boxY + 5,
                    cursorX + 1,
                    boxY + boxH - 5,
                    RotClientTheme.TEXT);
        }
    }

    private static boolean insideInput(
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width) {

        return RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x,
                y + 10,
                width,
                27);
    }

    private static Layout layout(
            int left,
            int top,
            int right,
            int bottom) {

        int cardX =
                left;

        int cardY =
                top + 18;

        int cardWidth =
                Math.max(
                        1,
                        right - left);

        /*
         * No minimum height:
         * the editor must never physically extend below the dashboard.
         */
        int cardHeight =
                Math.max(
                        1,
                        bottom
                                - cardY
                                - 4);

        int padding = 12;

        int innerWidth =
                Math.max(
                        1,
                        cardWidth
                                - padding * 2);

        int gap = 12;

        int availableColumns =
                Math.max(
                        1,
                        innerWidth - gap);

        int leftWidth =
                Math.max(
                        1,
                        Math.min(
                                300,
                                availableColumns / 2));

        int rightWidth =
                Math.max(
                        1,
                        availableColumns
                                - leftWidth);

        int leftX =
                cardX + padding;

        int rightX =
                leftX
                        + leftWidth
                        + gap;

        int contentTop =
                cardY + 10;

        int searchY =
                contentTop + 12;

        int suggestionsY =
                searchY
                        + SEARCH_BOX_HEIGHT
                        + 7;

        int footerY =
                cardY
                        + cardHeight
                        - RotClientUiDraw.BUTTON_HEIGHT
                        - 10;

        int settingsY =
                contentTop + 100;

        return new Layout(
                cardX,
                cardY,
                cardWidth,
                cardHeight,
                leftX,
                leftWidth,
                rightX,
                rightWidth,
                contentTop,
                searchY,
                suggestionsY,
                MAX_SUGGESTIONS,
                settingsY,
                footerY);
    }

    private int contentHeight() {
        return kind == Kind.BAZAAR
                ? 365
                : 310;
    }

    /*
     * USER FRIENDLY BUY TARGETS
     *
     * Editable coin values use k/m/b notation instead of scientific
     * notation.
     *
     * A percentage in BUY ALERT PRICE is a setup shortcut:
     *
     * Auction House:
     *     percentage below the current median BIN.
     *
     * Bazaar:
     *     percentage below the current instant-buy price.
     *
     * The calculated coin value is persisted as the actual watch threshold.
     */
    private boolean buyPriceField() {
        return focusedField == 1
                && (kind == Kind.AUCTION_HOUSE
                || kind == Kind.BAZAAR);
    }

    private static boolean canAppendPercent(
            String current) {

        if (current == null
                || current.isBlank()
                || current.contains("%")) {

            return false;
        }

        String cleaned =
                current.trim()
                        .replace(
                                ',',
                                '.');

        /*
         * k/m/b values such as 11m are coin prices, not percentages.
         */
        if (cleaned.endsWith("k")
                || cleaned.endsWith("K")
                || cleaned.endsWith("m")
                || cleaned.endsWith("M")
                || cleaned.endsWith("b")
                || cleaned.endsWith("B")) {

            return false;
        }

        try {
            double percentage =
                    Double.parseDouble(
                            cleaned);

            return Double.isFinite(
                    percentage)
                    && percentage > 0.0D
                    && percentage < 100.0D;

        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private Long resolveAuctionBuyPrice() {

        if (!hasPercentShortcut(
                auctionMaxPrice)) {

            return parseLongAmount(
                    auctionMaxPrice);
        }

        Double percentage =
                parsePercentShortcut(
                        auctionMaxPrice);

        if (percentage == null) {
            return null;
        }

        String itemName = "";
        String tier = "";

        if (selectedAuction != null) {
            itemName =
                    selectedAuction.itemName();

            tier =
                    selectedAuction.tier();

        } else if (editingAuctionWatch != null) {
            itemName =
                    editingAuctionWatch.itemName;

            tier =
                    editingAuctionWatch.tier;
        }

        if (itemName.isBlank()) {
            return null;
        }

        MarketWatchItemCatalog.AuctionStats stats =
                MarketWatchItemCatalog.auctionStats(
                        itemName,
                        tier);

        if (!stats.available()
                || stats.medianBin() <= 0L) {

            return null;
        }

        double target =
                stats.medianBin()
                        * (1.0D
                        - percentage
                        / 100.0D);

        if (!Double.isFinite(target)
                || target <= 0.0D
                || target > Long.MAX_VALUE) {

            return null;
        }

        return Math.max(
                1L,
                Math.round(target));
    }

    private Double resolveBazaarBuyPrice() {

        if (!hasPercentShortcut(
                bazaarMaxBuy)) {

            return parseAmount(
                    bazaarMaxBuy);
        }

        Double percentage =
                parsePercentShortcut(
                        bazaarMaxBuy);

        if (percentage == null) {
            return null;
        }

        String productId = "";

        if (selectedBazaar != null) {
            productId =
                    selectedBazaar.productId();

        } else if (editingBazaarWatch != null) {
            productId =
                    editingBazaarWatch.productId;
        }

        if (productId.isBlank()) {
            return null;
        }

        MarketWatchItemCatalog.BazaarStats stats =
                MarketWatchItemCatalog.bazaarStats(
                        productId);

        if (!stats.available()
                || stats.instantBuy() <= 0.0D) {

            return null;
        }

        double target =
                stats.instantBuy()
                        * (1.0D
                        - percentage
                        / 100.0D);

        return Double.isFinite(target)
                && target > 0.0D
                ? target
                : null;
    }

    private static boolean hasPercentShortcut(
            String value) {

        return value != null
                && value.trim()
                .endsWith("%");
    }

    private static Double parsePercentShortcut(
            String value) {

        if (!hasPercentShortcut(value)) {
            return null;
        }

        String raw =
                value.trim();

        String cleaned =
                raw.substring(
                        0,
                        raw.length() - 1)
                        .trim()
                        .replace(
                                ',',
                                '.');

        if (cleaned.isBlank()) {
            return null;
        }

        try {
            double percentage =
                    Double.parseDouble(
                            cleaned);

            return Double.isFinite(
                    percentage)
                    && percentage > 0.0D
                    && percentage < 100.0D
                    ? percentage
                    : null;

        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String editableAmount(
            long value) {

        if (value <= 0L) {
            return "";
        }

        return compactEditable(
                java.math.BigDecimal
                        .valueOf(value));
    }

    private static String editableAmount(
            double value) {

        if (!Double.isFinite(value)
                || value <= 0.0D) {

            return "";
        }

        return compactEditable(
                java.math.BigDecimal
                        .valueOf(value));
    }

    private static String editablePlain(
            double value) {

        if (!Double.isFinite(value)
                || value <= 0.0D) {

            return "";
        }

        return java.math.BigDecimal
                .valueOf(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String compactEditable(
            java.math.BigDecimal value) {

        java.math.BigDecimal billion =
                java.math.BigDecimal
                        .valueOf(
                                1_000_000_000L);

        java.math.BigDecimal million =
                java.math.BigDecimal
                        .valueOf(
                                1_000_000L);

        java.math.BigDecimal thousand =
                java.math.BigDecimal
                        .valueOf(
                                1_000L);

        if (value.compareTo(
                billion) >= 0) {

            return value
                    .movePointLeft(9)
                    .stripTrailingZeros()
                    .toPlainString()
                    + "b";
        }

        if (value.compareTo(
                million) >= 0) {

            return value
                    .movePointLeft(6)
                    .stripTrailingZeros()
                    .toPlainString()
                    + "m";
        }

        if (value.compareTo(
                thousand) >= 0) {

            return value
                    .movePointLeft(3)
                    .stripTrailingZeros()
                    .toPlainString()
                    + "k";
        }

        return value
                .stripTrailingZeros()
                .toPlainString();
    }
    private static boolean validNumericInput(
            String input) {

        for (int i = 0;
             i < input.length();
             i++) {

            char c =
                    input.charAt(i);

            if (!Character.isDigit(c)
                    && c != '.'
                    && c != ','
                    && c != '_'
                    && c != ' '
                    && c != 'k'
                    && c != 'K'
                    && c != 'm'
                    && c != 'M'
                    && c != 'b'
                    && c != 'B') {

                return false;
            }
        }

        return true;
    }

    private static Double parseAmount(
            String value) {

        if (value == null
                || value.isBlank()) {

            return 0.0D;
        }

        String cleaned =
                normalizeNumber(value);

        double multiplier =
                1.0D;

        if (cleaned.endsWith("k")) {
            multiplier =
                    1_000.0D;

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1);
        } else if (cleaned.endsWith("m")) {
            multiplier =
                    1_000_000.0D;

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1);
        } else if (cleaned.endsWith("b")) {
            multiplier =
                    1_000_000_000.0D;

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 1);
        }

        try {
            double result =
                    Double.parseDouble(
                            cleaned)
                            * multiplier;

            return Double.isFinite(result)
                    && result >= 0.0D
                    ? result
                    : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long parseLongAmount(
            String value) {

        Double parsed =
                parseAmount(value);

        if (parsed == null
                || parsed > Long.MAX_VALUE) {

            return null;
        }

        return Math.round(parsed);
    }

    private static String normalizeNumber(
            String raw) {

        String value =
                raw.trim()
                        .toLowerCase(Locale.ROOT)
                        .replace(" ", "")
                        .replace("_", "");

        int firstComma =
                value.indexOf(',');

        int lastComma =
                value.lastIndexOf(',');

        if (firstComma >= 0
                && firstComma == lastComma
                && value.indexOf('.') < 0) {

            int decimals =
                    value.length()
                            - firstComma
                            - 1;

            if (decimals == 1
                    || decimals == 2) {

                return value.replace(
                        ',',
                        '.');
            }
        }

        return value.replace(
                ",",
                "");
    }

    private static String formatCoins(
            long coins) {

        if (coins <= 0L) {
            return "--";
        }

        return formatPrice(
                coins);
    }

    private static String formatPrice(
            double value) {

        if (!Double.isFinite(value)
                || value <= 0.0D) {

            return "--";
        }

        if (value >= 1_000_000_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.2fB",
                    value / 1_000_000_000.0D);
        }

        if (value >= 1_000_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    value / 1_000_000.0D);
        }

        if (value >= 1_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.1fk",
                    value / 1_000.0D);
        }

        if (value >= 100.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.0f",
                    value);
        }

        return String.format(
                Locale.ROOT,
                "%.1f",
                value);
    }

    private static String formatPercent(
            double value) {

        if (!Double.isFinite(value)) {
            return "--";
        }

        return String.format(
                Locale.ROOT,
                "%.2f%%",
                value);
    }

    private static String formatCount(
            long value) {

        if (value >= 1_000_000_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.1fB",
                    value / 1_000_000_000.0D);
        }

        if (value >= 1_000_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.1fM",
                    value / 1_000_000.0D);
        }

        if (value >= 1_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.1fk",
                    value / 1_000.0D);
        }

        return Long.toString(value);
    }

    private static String formatAge(
            long observedAtMillis) {

        if (observedAtMillis < 0L) {
            return "waiting for data";
        }

        long age =
                Math.max(
                        0L,
                        System.currentTimeMillis()
                                - observedAtMillis);

        long seconds =
                age / 1_000L;

        if (seconds < 60L) {
            return seconds + "s ago";
        }

        long minutes =
                seconds / 60L;

        if (minutes < 60L) {
            return minutes + "m ago";
        }

        return (minutes / 60L)
                + "h ago";
    }

    private static String fit(
            Font font,
            String value,
            int maxWidth) {

        if (value == null) {
            return "";
        }

        if (font.width(value)
                <= maxWidth) {

            return value;
        }

        String suffix =
                "...";

        String result =
                value;

        while (!result.isEmpty()
                && font.width(
                result + suffix)
                > maxWidth) {

            result =
                    result.substring(
                            0,
                            result.length() - 1);
        }

        return result
                + suffix;
    }

    private static String fitFromRight(
            Font font,
            String value,
            int maxWidth) {

        String result =
                value == null
                        ? ""
                        : value;

        while (!result.isEmpty()
                && font.width(result)
                > maxWidth) {

            int next =
                    result.offsetByCodePoints(
                            0,
                            1);

            result =
                    result.substring(next);
        }

        return result;
    }

    private static void drawRight(
            GuiGraphicsExtractor graphics,
            Font font,
            String text,
            int right,
            int y,
            int color) {

        RotClientUiDraw.text(
                graphics,
                font,
                text,
                right - font.width(text),
                y,
                color,
                false);
    }

    private record Layout(
            int cardX,
            int cardY,
            int cardWidth,
            int cardHeight,
            int leftX,
            int leftWidth,
            int rightX,
            int rightWidth,
            int contentTop,
            int searchY,
            int suggestionsY,
            int suggestionRows,
            int settingsY,
            int footerY) {

        int bodyTop() {
            return cardY + 8;
        }

        int bodyBottom() {
            return Math.max(
                    bodyTop(),
                    footerY - 8);
        }

        Layout scrolled(int dy) {
            return new Layout(
                    cardX,
                    cardY,
                    cardWidth,
                    cardHeight,
                    leftX,
                    leftWidth,
                    rightX,
                    rightWidth,
                    contentTop + dy,
                    searchY + dy,
                    suggestionsY + dy,
                    suggestionRows,
                    settingsY + dy,
                    footerY);
        }
    }
}