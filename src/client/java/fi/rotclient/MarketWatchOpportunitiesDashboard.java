package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MarketWatchOpportunitiesDashboard {

    private enum Filter {
        ALL,
        AUCTION_HOUSE,
        BAZAAR
    }

    private enum SortMode {
        QUALITY("BEST QUALITY"),
        PROFIT("MOST PROFIT"),
        ROI("BEST ROI"),
        LIQUIDITY("BEST LIQUIDITY"),
        EDGE("BEST EDGE"),
        CAPITAL("LOWEST CAPITAL");

        private final String label;

        SortMode(
                String label) {

            this.label =
                    label;
        }

        String label() {
            return label;
        }
    }

    private enum RarityFilter {
        ANY("ANY"),
        COMMON("COMMON"),
        UNCOMMON("UNCOMMON"),
        RARE("RARE"),
        EPIC("EPIC"),
        LEGENDARY("LEGENDARY"),
        MYTHIC_PLUS("MYTHIC+"),
        SPECIAL("SPECIAL");

        private final String label;

        RarityFilter(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    /*
     * These deliberately follow the broad categories exposed by the
     * Auction House feed rather than trying to infer detailed item types.
     */
    private enum TypeFilter {
        ANY("ANY"),
        WEAPON("WEAPON"),
        ARMOR("ARMOR"),
        ACCESSORY("ACCESSORY"),
        CONSUMABLE("CONSUMABLE"),
        COSMETIC("COSMETIC"),
        MISC("MISC");

        private final String label;

        TypeFilter(
                String label) {

            this.label =
                    label;
        }

        String label() {
            return label;
        }
    }

    private enum LiquidityFilter {
        ANY("ANY"),
        MEDIUM_PLUS("MEDIUM+"),
        HIGH_ONLY("HIGH");

        private final String label;

        LiquidityFilter(
                String label) {

            this.label =
                    label;
        }

        String label() {
            return label;
        }
    }

    private enum ConfidenceFilter {
        ANY("ANY"),
        MEDIUM_PLUS("MEDIUM+"),
        HIGH_ONLY("HIGH");

        private final String label;

        ConfidenceFilter(
                String label) {

            this.label =
                    label;
        }

        String label() {
            return label;
        }
    }

    private enum RoiFilter {
        ANY("ANY", 0.0D),
        TWO("2%+", 2.0D),
        FIVE("5%+", 5.0D),
        TEN("10%+", 10.0D),
        TWENTY_FIVE("25%+", 25.0D);

        private final String label;
        private final double minimum;

        RoiFilter(
                String label,
                double minimum) {

            this.label =
                    label;

            this.minimum =
                    minimum;
        }

        String label() {
            return label;
        }

        double minimum() {
            return minimum;
        }
    }

    private enum ProfitFilter {
        ANY("ANY", 0.0D),
        HUNDRED_K("100K+", 100_000.0D),
        FIVE_HUNDRED_K("500K+", 500_000.0D),
        ONE_M("1M+", 1_000_000.0D),
        FIVE_M("5M+", 5_000_000.0D);

        private final String label;
        private final double minimum;

        ProfitFilter(
                String label,
                double minimum) {

            this.label =
                    label;

            this.minimum =
                    minimum;
        }

        String label() {
            return label;
        }

        double minimum() {
            return minimum;
        }
    }

    private enum EdgeFilter {
        ANY("ANY", 0.0D),
        TWO("2%+", 2.0D),
        FIVE("5%+", 5.0D),
        TEN("10%+", 10.0D),
        TWENTY("20%+", 20.0D);

        private final String label;
        private final double minimum;

        EdgeFilter(
                String label,
                double minimum) {

            this.label =
                    label;

            this.minimum =
                    minimum;
        }

        String label() {
            return label;
        }

        double minimum() {
            return minimum;
        }
    }

    private enum VolumeFilter {
        ANY("ANY", 0L),
        HUNDRED_K("100K+", 100_000L),
        THREE_M("3M+", 3_000_000L),
        FIFTEEN_M("15M+", 15_000_000L);

        private final String label;
        private final long minimum;

        VolumeFilter(
                String label,
                long minimum) {

            this.label =
                    label;

            this.minimum =
                    minimum;
        }

        String label() {
            return label;
        }

        long minimum() {
            return minimum;
        }
    }
/*
     * V3 GRID
     *
     * Each column flows independently. Expanding a card therefore pushes
     * only the cards below it in the same column, like a genuine dropdown.
     */
    private static final int CARD_HEIGHT =
            74;

    /*
     * Extra space at the bottom is reserved for the explicit PIN button.
     */
    private static final int EXPANDED_EXTRA_HEIGHT =
            128;

    private static final int PIN_BUTTON_WIDTH =
            104;

    private static final int PIN_BUTTON_HEIGHT =
            24;

    private static final int PIN_BUTTON_MARGIN =
            8;

    /*
     * Delay drawing the button until enough of the animated dropdown is
     * visible. It therefore arrives naturally with the card expansion.
     */
    private static final int PIN_BUTTON_VISIBLE_AT =
            116;

    /*
     * Compact manager for the session's frozen deals.
     */
    private static final int PIN_MANAGER_BUTTON_WIDTH =
            96;

    private static final int PIN_MANAGER_MENU_WIDTH =
            430;

    private static final int PIN_MANAGER_MENU_GAP =
            6;

    private static final int PIN_MANAGER_HEADER_HEIGHT =
            30;

    private static final int PIN_MANAGER_ROW_HEIGHT =
            58;

    private static final int PIN_MANAGER_FOOTER_HEIGHT =
            34;

    private static final int PIN_MANAGER_REMOVE_WIDTH =
            72;

    private static final int PIN_MANAGER_ACTION_HEIGHT =
            22;

    private static final long PIN_MANAGER_STALE_MILLIS =
            15L * 60L * 1000L;

    private static final int CARD_GAP_X =
            10;

    private static final int CARD_GAP_Y =
            9;

    private static final int FILTER_ALL_WIDTH =
            48;

    private static final int FILTER_AH_WIDTH =
            112;

    private static final int FILTER_BAZAAR_WIDTH =
            76;

    private static final int FILTER_GAP =
            8;

    private static final int SORT_WIDTH =
            146;

    private static final int SORT_MENU_GAP =
            4;

    private static final int FILTER_CONTROL_WIDTH =
            96;

    private static final int CONTROL_GAP =
            8;

    private static final int SORT_MENU_WIDTH =
            218;

    private static final int SORT_ROW_HEIGHT =
            34;

    private static final int FILTER_PANEL_WIDTH =
            222;

    private static final int FILTER_ROW_HEIGHT =
            28;

    private static final int FILTER_MENU_GAP =
            4;

    /*
     * Sort and all filter option lists intentionally share these
     * measurements so every selector feels like the same component.
     */
    private static final int PREMIUM_MENU_WIDTH =
            222;

    private static final int PREMIUM_HEADER_HEIGHT =
            30;

    private static final int PREMIUM_OPTION_HEIGHT =
            32;

    private static final int FILTER_HOME_ROW_HEIGHT =
            34;

    private static final int FILTER_FOOTER_HEIGHT =
            32;

    /*
     * Same timing as the expanding deal cards.
     */
    private static final double MENU_ANIMATION_SECONDS =
            0.18D;

    private static final int FILTER_TRAY_MAX_WIDTH =
            640;

    private static final int FILTER_TRAY_PADDING =
            10;

    private static final int FILTER_TRAY_LABEL_WIDTH =
            78;

    private static final int FILTER_TRAY_SECTION_GAP =
            7;

    private static final int FILTER_CHIP_HEIGHT =
            24;

    private static final int FILTER_CHIP_GAP =
            5;

    private static final String[] RARITY_OPTIONS = {
            "ANY",
            "COMMON",
            "UNCOMMON",
            "RARE",
            "EPIC",
            "LEGENDARY",
            "MYTHIC+",
            "SPECIAL"
    };

    private static final String[] CATEGORY_OPTIONS = {
            "ANY",
            "WEAPON",
            "ARMOR",
            "ACCESSORY",
            "CONSUMABLE",
            "COSMETIC",
            "MISC"
    };

    private static final String[] LIQUIDITY_OPTIONS = {
            "ANY",
            "MEDIUM+",
            "HIGH"
    };

    private static final String[] CONFIDENCE_OPTIONS = {
            "ANY",
            "MEDIUM+",
            "HIGH"
    };

    private static final String[] ROI_OPTIONS = {
            "ANY",
            "2%+",
            "5%+",
            "10%+",
            "25%+"
    };

    private static final String[] PROFIT_OPTIONS = {
            "ANY",
            "100K+",
            "500K+",
            "1M+",
            "5M+"
    };

    private static final String[] EDGE_OPTIONS = {
            "ANY",
            "2%+",
            "5%+",
            "10%+",
            "20%+"
    };

    /*
     * Based on the live Bazaar distribution:
     * ~100K = upper quartile
     * ~3M   = top 10%
     * ~15M  = roughly upper few percent
     */
    private static final String[] WEEKLY_VOLUME_OPTIONS = {
            "ANY",
            "100K+",
            "3M+",
            "15M+"
    };
    private static final int BUDGET_INPUT_WIDTH =
            94;

    private static final int BUDGET_INPUT_HEIGHT =
            24;

    private static final int APPLY_WIDTH =
            52;

    private static final int SCROLL_STEP =
            30;

    /*
     * Approximate open/close duration.
     */
    private static final double EXPANSION_SECONDS =
            0.18D;

    private Filter filter =
            Filter.ALL;

    private SortMode sortMode =
            SortMode.QUALITY;

    private boolean sortMenuOpen;

    private boolean filterMenuOpen;


    /*
     * These are visual animation states. The booleans above remain the
     * logical open/closed state, while progress lets the panel ease out
     * instead of disappearing on the exact click frame.
     */
    private double sortMenuProgress;

    private double filterMenuProgress;

    private long lastMenuAnimationNanos;

    /*
     * Empty set means ANY.
     *
     * Multiple selections inside the same facet use OR semantics:
     * EPIC + LEGENDARY = Epic OR Legendary.
     *
     * Different facets still combine with AND:
     * (Epic OR Legendary) AND Weapon AND Medium+ liquidity.
     */
    private final java.util.EnumSet<RarityFilter> rarityFilters =
            java.util.EnumSet.noneOf(
                    RarityFilter.class);

    private final java.util.EnumSet<TypeFilter> typeFilters =
            java.util.EnumSet.noneOf(
                    TypeFilter.class);

    private LiquidityFilter liquidityFilter =
            LiquidityFilter.ANY;

    private ConfidenceFilter confidenceFilter =
            ConfidenceFilter.ANY;

    private RoiFilter roiFilter =
            RoiFilter.ANY;

    private ProfitFilter profitFilter =
            ProfitFilter.ANY;

    private EdgeFilter edgeFilter =
            EdgeFilter.ANY;

    private VolumeFilter volumeFilter =
            VolumeFilter.ANY;

    private int scrollPixels;

    /*
     * Geometry from the currently rendered scrollbar.
     * The visual bar stays slim, while input gets a wider hit area.
     */
    private boolean scrollbarDragging;
    private int scrollbarGrabOffset;
    private int scrollbarX;
    private int scrollbarTrackTop;
    private int scrollbarTrackBottom;
    private int scrollbarThumbTop;
    private int scrollbarThumbHeight;
    private int scrollbarMaxScroll;

    private boolean budgetFocused;

    private String budgetText =
            editableBudget(
                    MarketWatchOpportunityService
                            .budgetCoins());

    private String budgetError =
            "";

    /*
     * expandedOpportunityId = logical open card.
     *
     * animationOpportunityId remains populated while the card is closing,
     * allowing the same card body to smoothly shrink back to CARD_HEIGHT.
     */
    private record PinButtonHit(
            int x,
            int y,
            int width,
            int height,
            MarketWatchOpportunity opportunity) {
    }

    private final List<PinButtonHit> pinButtonHits =
            new ArrayList<>();

    private boolean pinnedMenuOpen;

    private double pinnedMenuProgress;

    /*
     * Used by controls rendered deep inside deal cards so their
     * premium hover state matches the rest of the dashboard.
     */
    private int frameMouseX;

    private int frameMouseY;

    private String expandedOpportunityId =
            "";

    private String animationOpportunityId =
            "";

    private double expansionProgress;

    private long lastAnimationNanos;

    /*
     * Advanced filter values survive dashboard reconstruction and
     * client restarts. Menu open/closed animation remains transient.
     */
    private boolean preferencesLoaded;

    void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY) {

        ensureFilterPreferencesLoaded();

        scrollbarMaxScroll = 0;
        scrollbarThumbHeight = 0;

        /*
         * Rebuilt from the buttons actually rendered this frame.
         */
        pinButtonHits.clear();

        frameMouseX =
                mouseX;

        frameMouseY =
                mouseY;

        MarketWatchOpportunitySnapshot snapshot =
                MarketWatchOpportunityService
                        .current();

        List<MarketWatchOpportunity> opportunities =
                filtered(
                        snapshot);

        boolean updatePending =
                MarketWatchOpportunityService
                        .budgetUpdatePending();

        validateExpandedOpportunity(
                opportunities);

        advanceExpansionAnimation();
        advanceMenuAnimation();

        RotClientUiDraw.pageTitle(
                graphics,
                font,
                "PROFIT FINDER",
                left,
                top);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Deals within your budget. Open a card to inspect or pin the price target.",
                left,
                top + 14);

        String matchStatus =
                !snapshot.hasMarketData()
                        ? "WAITING"
                        : updatePending
                        ? "UPDATING"
                        : opportunities.size()
                        + (opportunities.size() == 1
                        ? " MATCH"
                        : " MATCHES");

        RotClientUiDraw.drawStatusPill(
                graphics,
                font,
                right,
                top,
                matchStatus,
                snapshot.hasMarketData()
                        && !updatePending
                        && !opportunities.isEmpty()
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_MUTED);

        int width =
                Math.max(
                        1,
                        right - left);

        drawBudgetControl(
                graphics,
                font,
                snapshot,
                opportunities.size(),
                updatePending,
                left,
                budgetY(top),
                right,
                mouseX,
                mouseY);
        int filterY =
                filterY(top);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                left,
                filterY,
                FILTER_ALL_WIDTH,
                "ALL",
                filter == Filter.ALL,
                true);

        int ahX =
                left
                        + FILTER_ALL_WIDTH
                        + FILTER_GAP;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                ahX,
                filterY,
                FILTER_AH_WIDTH,
                "AUCTION HOUSE",
                filter
                        == Filter.AUCTION_HOUSE,
                true);

        int bazaarX =
                ahX
                        + FILTER_AH_WIDTH
                        + FILTER_GAP;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                bazaarX,
                filterY,
                FILTER_BAZAAR_WIDTH,
                "BAZAAR",
                filter == Filter.BAZAAR,
                true);

        drawPinnedDealsButton(
                graphics,
                font,
                right,
                filterY,
                mouseX,
                mouseY);

        drawFilterButton(
                graphics,
                font,
                right,
                filterY,
                mouseX,
                mouseY);

        drawSortButton(
                graphics,
                font,
                right,
                filterY,
                mouseX,
                mouseY);

        int baseListTop =
                listY(top);

        /*
         * FILTERS behaves like an expanding card:
         * the panel itself grows and the deals below move with it.
         */
        drawFilterMenu(
                graphics,
                font,
                left,
                right,
                baseListTop,
                mouseX,
                mouseY);

        int listTop =
                baseListTop
                        + filterTrayLayoutOffset(
                        left,
                        right);

        if (!snapshot.hasMarketData()) {
            scrollPixels = 0;

            drawEmptyState(
                    graphics,
                    font,
                    left,
                    listTop,
                    width,
                    "Waiting for market data",
                    "Auction House and Bazaar data are still loading.");

            /*
             * Menus are overlays and must render even when the deal body
             * has nothing to draw.
             */
            drawControlOverlays(
                    graphics,
                    font,
                    left,
                    right,
                    filterY,
                    mouseX,
                    mouseY);

            return;
        }

        if (opportunities.isEmpty()) {
            scrollPixels = 0;
            closeExpandedImmediately();

            boolean filteredEmpty =
                    activeAdvancedFilterCount() > 0
                            || filter != Filter.ALL;

            drawEmptyState(
                    graphics,
                    font,
                    left,
                    listTop,
                    width,
                    filteredEmpty
                            ? "No deals match these filters"
                            : "No strong deals within your budget",
                    filteredEmpty
                            ? "Change or reset a filter to broaden the results."
                            : "Change your budget or wait for the market to update.");

            /*
             * Critical UX rule: a filter that produces zero results must
             * never remove the controls required to undo that filter.
             */
            drawControlOverlays(
                    graphics,
                    font,
                    left,
                    right,
                    filterY,
                    mouseX,
                    mouseY);

            return;
        }

        int viewportHeight =
                Math.max(
                        1,
                        bottom
                                - listTop
                                - 4);

        int contentHeight =
                gridContentHeight(
                        opportunities);

        int maxScroll =
                Math.max(
                        0,
                        contentHeight
                                - viewportHeight);

        scrollPixels =
                Math.max(
                        0,
                        Math.min(
                                scrollPixels,
                                maxScroll));

        graphics.enableScissor(
                left,
                listTop,
                right,
                bottom);

        try {
            drawGrid(
                    graphics,
                    font,
                    opportunities,
                    left,
                    listTop,
                    right,
                    bottom,
                    mouseX,
                    mouseY);
        } finally {
            graphics.disableScissor();
        }

        if (maxScroll > 0) {
            drawScrollbar(
                    graphics,
                    right,
                    listTop,
                    bottom,
                    contentHeight,
                    viewportHeight,
                    maxScroll);
        }

        drawControlOverlays(
                graphics,
                font,
                left,
                right,
                filterY,
                mouseX,
                mouseY);
    }

    private void drawGrid(
            GuiGraphicsExtractor graphics,
            Font font,
            List<MarketWatchOpportunity> opportunities,
            int left,
            int listTop,
            int right,
            int bottom,
            int mouseX,
            int mouseY) {

        GridLayout layout =
                gridLayout(
                        left,
                        right);

        int leftY =
                listTop
                        - scrollPixels;

        int rightY =
                listTop
                        - scrollPixels;

        /*
         * Scissoring prevents pixels outside the viewport from appearing,
         * but it does not prevent the CPU from preparing those cards.
         *
         * Keep a generous overscan above and below the viewport so cards
         * enter smoothly while scrolling, while avoiding the expensive
         * text/icon/card rendering work for dozens of off-screen results.
         */
        int renderOverscan =
                Math.max(
                        120,
                        CARD_HEIGHT * 2);

        int renderTop =
                listTop
                        - renderOverscan;

        int renderBottom =
                bottom
                        + renderOverscan;

        for (int i = 0;
                i < opportunities.size();
                i++) {

            MarketWatchOpportunity opportunity =
                    opportunities.get(i);

            boolean rightColumn =
                    (i & 1) == 1;

            int x =
                    rightColumn
                            ? layout.rightX()
                            : layout.leftX();

            int y =
                    rightColumn
                            ? rightY
                            : leftY;

            int extraHeight =
                    expandedHeight(
                            opportunity);

            int cardHeight =
                    CARD_HEIGHT
                            + extraHeight;

            /*
             * Layout remains exact for every opportunity, including cards
             * that are not currently rendered. This preserves scrolling,
             * expansion positions and all interaction behaviour.
             */
            boolean visible =
                    y + cardHeight
                            >= renderTop
                            && y
                            <= renderBottom;

            if (visible) {
                drawDealCard(
                        graphics,
                        font,
                        opportunity,
                        x,
                        y,
                        layout.cardWidth(),
                        extraHeight,
                        mouseX,
                        mouseY);
            }

            int consumedHeight =
                    cardHeight
                            + CARD_GAP_Y;

            if (rightColumn) {
                rightY +=
                        consumedHeight;
            } else {
                leftY +=
                        consumedHeight;
            }
        }
    }

    private void drawDealCard(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchOpportunity opportunity,
            int x,
            int y,
            int width,
            int extraHeight,
            int mouseX,
            int mouseY) {

        int totalHeight =
                CARD_HEIGHT
                        + extraHeight;

        boolean logicallyExpanded =
                opportunity.id()
                        .equals(
                                expandedOpportunityId);

        boolean hovered =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        CARD_HEIGHT);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                totalHeight);

        int accent =
                opportunity.market()
                        == MarketWatchOpportunity
                        .Market.BAZAAR
                        ? RotClientTheme.VIOLET
                        : RotClientTheme.HUD_ACCENT;

        graphics.fill(
                x,
                y,
                x + (hovered
                || logicallyExpanded
                || extraHeight > 0
                ? 4
                : 3),
                y + totalHeight,
                accent);

        drawCompactContent(
                graphics,
                font,
                opportunity,
                x,
                y,
                width,
                hovered,
                logicallyExpanded);

        if (extraHeight > 0) {
            drawDropdownContent(
                    graphics,
                    font,
                    opportunity,
                    x,
                    y,
                    width,
                    extraHeight);
        }
    }

    private void drawCompactContent(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchOpportunity opportunity,
            int x,
            int y,
            int width,
            boolean hovered,
            boolean expanded) {

        ItemStack icon =
                opportunity.market()
                        == MarketWatchOpportunity
                        .Market.BAZAAR
                        ? MarketWatchItemIconResolver
                        .bazaarIcon(
                                opportunity.itemId())
                        : MarketWatchItemIconResolver
                        .auctionIcon(
                                opportunity.category(),
                                opportunity.itemName());

        graphics.item(
                icon,
                x + 10,
                y + 9);

        int textX =
                x + 33;

        String scoreText =
                String.format(
                        Locale.ROOT,
                        "%.1f %s",
                        opportunity.score(),
                        qualityLabel(
                                opportunity.score()));

        int scoreReserve =
                Math.max(
                        96,
                        font.width(
                                scoreText)
                                + 18);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        opportunity.itemName(),
                        Math.max(
                                30,
                                width
                                        - (textX - x)
                                        - scoreReserve
                                        - 8)),
                textX,
                y + 7,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.drawStatusPill(
                graphics,
                font,
                x + width - 8,
                y + 5,
                scoreText,
                scoreColor(
                        opportunity.score()));

        String priceFlow;

        if (opportunity.market()
                == MarketWatchOpportunity
                .Market.AUCTION_HOUSE) {

            priceFlow =
                    "Buy "
                            + formatCoins(
                            opportunity.capitalCoins())
                            + " total"
                            + ahStackSuffix(
                            opportunity)
                            + "  ->  Sell "
                            + formatCoins(
                            opportunity.grossReturnCoins());

        } else {

            priceFlow =
                    "Capital "
                            + formatCoins(
                            opportunity.capitalCoins())
                            + "  ->  "
                            + "Return "
                            + formatCoins(
                            opportunity.grossReturnCoins());
        }

        RotClientUiDraw.helpText(
                graphics,
                font,
                fit(
                        font,
                        priceFlow,
                        width - 44),
                textX,
                y + 27);

        String profitLine =
                "Profit "
                        + formatSignedCoins(
                        opportunity.expectedProfitCoins())
                        + "    ROI "
                        + String.format(
                        Locale.ROOT,
                        "%.1f%%",
                        opportunity.roiPercent());

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        profitLine,
                        width - 44),
                textX,
                y + 42,
                RotClientTheme.SUCCESS,
                true);

        String qualityLine =
                confidenceLabel(
                        opportunity.confidence())
                        + " confidence"
                        + "    "
                        + liquidityLabel(
                        opportunity.liquidity())
                        + " liquidity";

        boolean pinned =
                MarketWatchPinnedDealStore
                        .contains(
                                opportunity.id());

        String toggle =
                (pinned
                        ? "PINNED  |  "
                        : "")
                        + (expanded
                        ? "^ LESS"
                        : "v MORE");

        int toggleWidth =
                font.width(
                        toggle);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        qualityLine,
                        Math.max(
                                25,
                                width
                                        - 45
                                        - toggleWidth
                                        - 18)),
                textX,
                y + 58,
                confidenceColor(
                        opportunity.confidence()),
                false);

        RotClientUiDraw.text(
                graphics,
                font,
                toggle,
                x
                        + width
                        - 9
                        - toggleWidth,
                y + 58,
                hovered
                        ? RotClientTheme.HUD_ACCENT
                        : RotClientTheme.TEXT_MUTED,
                false);
    }

    /*
     * This is not a second card. The original card's own background grows
     * downward, and this content is painted inside that added height.
     */
    private void drawDropdownContent(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchOpportunity opportunity,
            int x,
            int y,
            int width,
            int extraHeight) {

        if (opportunity == null
                || extraHeight <= 0) {
            return;
        }

        int top =
                y + CARD_HEIGHT;

        int innerX =
                x + 14;

        int innerWidth =
                Math.max(
                        1,
                        width - 28);

        boolean auctionHouse =
                opportunity.market()
                        == MarketWatchOpportunity.Market.AUCTION_HOUSE;

        long quantity =
                Math.max(
                        1L,
                        opportunity.quantity());

        String quantityText =
                "x"
                        + formatLongCount(
                        quantity);

        if (auctionHouse) {

            /*
             * Expanded AH cards are deliberately written as instructions.
             * The collapsed card already shows profit, ROI, confidence and
             * liquidity, so none of that is repeated here.
             */
            if (extraHeight >= 13) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        "1  BUY THIS LISTING",
                        innerX,
                        top + 5,
                        RotClientTheme.HUD_ACCENT,
                        true);
            }

            if (extraHeight >= 27) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                "Buy "
                                        + quantityText
                                        + " for no more than "
                                        + formatCoins(
                                        opportunity.capitalCoins())
                                        + " total",
                                innerWidth),
                        innerX,
                        top + 19,
                        RotClientTheme.TEXT,
                        false);
            }

            if (extraHeight >= 39) {
                String buyDetail =
                        formatCoins(
                                opportunity.buyPricePerUnit())
                                + " each";

                String seller =
                        sellerName(
                                opportunity);

                if (seller != null
                        && !seller.isBlank()) {

                    buyDetail +=
                            "  |  Seller "
                                    + seller;
                }

                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                buyDetail,
                                innerWidth),
                        innerX,
                        top + 31,
                        RotClientTheme.TEXT_MUTED,
                        false);
            }

            if (extraHeight >= 45) {
                graphics.fill(
                        innerX,
                        top + 43,
                        x + width - 14,
                        top + 44,
                        RotClientTheme.BORDER);
            }

            if (extraHeight >= 56) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        "2  LIST IT",
                        innerX,
                        top + 50,
                        RotClientTheme.HUD_ACCENT,
                        true);
            }

            if (extraHeight >= 70) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                "List "
                                        + quantityText
                                        + " for "
                                        + formatCoins(
                                        opportunity.grossReturnCoins())
                                        + " total",
                                innerWidth),
                        innerX,
                        top + 64,
                        RotClientTheme.TEXT,
                        false);
            }

            if (extraHeight >= 82) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                formatCoins(
                                        opportunity.sellPricePerUnit())
                                        + " each",
                                innerWidth),
                        innerX,
                        top + 76,
                        RotClientTheme.TEXT_MUTED,
                        false);
            }

        } else {

            /*
             * Bazaar wording mirrors the actions the player actually takes
             * in SkyBlock: create a buy order, then create a sell order.
             */
            if (extraHeight >= 13) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        "1  CREATE BUY ORDER",
                        innerX,
                        top + 5,
                        RotClientTheme.HUD_ACCENT,
                        true);
            }

            if (extraHeight >= 27) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                "Buy "
                                        + quantityText
                                        + " at up to "
                                        + formatCoins(
                                        opportunity.buyPricePerUnit())
                                        + " each",
                                innerWidth),
                        innerX,
                        top + 19,
                        RotClientTheme.TEXT,
                        false);
            }

            if (extraHeight >= 39) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                "Total spend: "
                                        + formatCoins(
                                        opportunity.capitalCoins()),
                                innerWidth),
                        innerX,
                        top + 31,
                        RotClientTheme.TEXT_MUTED,
                        false);
            }

            if (extraHeight >= 45) {
                graphics.fill(
                        innerX,
                        top + 43,
                        x + width - 14,
                        top + 44,
                        RotClientTheme.BORDER);
            }

            if (extraHeight >= 56) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        "2  CREATE SELL ORDER",
                        innerX,
                        top + 50,
                        RotClientTheme.HUD_ACCENT,
                        true);
            }

            if (extraHeight >= 70) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                "Sell "
                                        + quantityText
                                        + " at "
                                        + formatCoins(
                                        opportunity.sellPricePerUnit())
                                        + " each",
                                innerWidth),
                        innerX,
                        top + 64,
                        RotClientTheme.TEXT,
                        false);
            }

            if (extraHeight >= 82) {
                RotClientUiDraw.text(
                        graphics,
                        font,
                        fit(
                                font,
                                "Expected return: "
                                        + formatCoins(
                                        opportunity.grossReturnCoins()),
                                innerWidth),
                        innerX,
                        top + 76,
                        RotClientTheme.TEXT_MUTED,
                        false);
            }
        }

        /*
         * Fees/buffer are the only calculation details retained here,
         * because they explain why displayed profit is below raw spread.
         */
        if (extraHeight >= 94) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    fit(
                            font,
                            "Costs: "
                                    + formatCoins(
                                    opportunity.estimatedFeesCoins())
                                    + " fees  |  "
                                    + formatCoins(
                                    opportunity.riskBufferCoins())
                                    + " safety buffer",
                            innerWidth),
                    innerX,
                    top + 88,
                    RotClientTheme.TEXT_MUTED,
                    false);
        }

        drawPinDealButton(
                graphics,
                font,
                opportunity,
                x,
                top,
                width,
                extraHeight);
    }

    private void drawPinDealButton(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchOpportunity opportunity,
            int x,
            int dropdownTop,
            int width,
            int visibleHeight) {

        if (opportunity == null
                || visibleHeight
                < PIN_BUTTON_VISIBLE_AT) {

            return;
        }

        boolean pinned =
                isOpportunityPinned(
                        opportunity.id());

        int buttonX =
                x
                        + width
                        - PIN_BUTTON_MARGIN
                        - PIN_BUTTON_WIDTH;

        int buttonY =
                dropdownTop
                        + EXPANDED_EXTRA_HEIGHT
                        - PIN_BUTTON_MARGIN
                        - PIN_BUTTON_HEIGHT;

        String label =
                pinned
                        ? "PINNED"
                        : "PIN DEAL";

        boolean hovered =
                RotClientUiDraw.inside(
                        frameMouseX,
                        frameMouseY,
                        buttonX,
                        buttonY,
                        PIN_BUTTON_WIDTH,
                        PIN_BUTTON_HEIGHT);

        /*
         * Uses the same premium surface treatment as filter choices,
         * including a real hover state.
         */
        drawPremiumChoiceSurface(
                graphics,
                font,
                buttonX,
                buttonY,
                PIN_BUTTON_WIDTH,
                PIN_BUTTON_HEIGHT,
                label,
                "",
                pinned,
                hovered,
                false);

        String hint =
                pinned
                        ? ""
                        : "";

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        hint,
                        Math.max(
                                1,
                                buttonX
                                        - x
                                        - 24)),
                x + 11,
                buttonY + 8,
                pinned
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_MUTED,
                false);

        pinButtonHits.add(
                new PinButtonHit(
                        buttonX,
                        buttonY,
                        PIN_BUTTON_WIDTH,
                        PIN_BUTTON_HEIGHT,
                        opportunity));
    }

    private boolean isOpportunityPinned(
            String opportunityId) {

        if (opportunityId == null
                || opportunityId.isBlank()) {

            return false;
        }

        for (MarketWatchPinnedDeal pinned
                : MarketWatchPinnedDealStore
                .all()) {

            if (pinned != null
                    && opportunityId.equals(
                            pinned.id())) {

                return true;
            }
        }

        return false;
    }

    private void drawAhDropdown(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchOpportunity opportunity,
            int x,
            int top,
            int width,
            int visibleHeight) {

        if (visibleHeight >= 38) {
            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 27,
                    width,
                    "AH  >  "
                            + opportunity.itemName(),
                    RotClientTheme.TEXT);
        }

        if (visibleHeight >= 54) {
            String purchaseDetails =
                    "<= "
                            + formatCoins(
                            opportunity.capitalCoins());

            if (opportunity.quantity() > 1L) {
                purchaseDetails +=
                        " total  |  "
                                + formatCoins(
                                opportunity.buyPricePerUnit())
                                + " ea x"
                                + opportunity.quantity();
            }

            purchaseDetails +=
                    "  |  seller "
                            + sellerName(
                            opportunity);

            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 43,
                    width,
                    purchaseDetails,
                    RotClientTheme.TEXT);
        }

        if (visibleHeight >= 70) {
            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 59,
                    width,
                    "! CHECK MODIFIERS  >  BUY"
                            + (opportunity.quantity() > 1L
                            ? " x" + opportunity.quantity()
                            : "")
                            + "  >  LIST "
                            + formatCoins(
                            opportunity.grossReturnCoins()),
                    RotClientTheme.HUD_ACCENT);
        }

        if (visibleHeight >= 88) {
            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 77,
                    width,
                    "Fees "
                            + formatCoins(
                            opportunity.estimatedFeesCoins())
                            + "  |  Buffer "
                            + formatCoins(
                            opportunity.riskBufferCoins())
                            + "  |  "
                            + opportunity.comparisonCount()
                            + " comps"
                            + "  |  Edge "
                            + String.format(
                            Locale.ROOT,
                            "%.1f%%",
                            opportunity.marketEdgePercent()),
                    RotClientTheme.TEXT_MUTED);
        }
    }

    private void drawBazaarDropdown(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchOpportunity opportunity,
            int x,
            int top,
            int width,
            int visibleHeight) {

        if (visibleHeight >= 38) {
            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 27,
                    width,
                    "BZ  >  "
                            + opportunity.itemName(),
                    RotClientTheme.TEXT);
        }

        if (visibleHeight >= 54) {
            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 43,
                    width,
                    "BUY ORDER    "
                            + formatLongCount(
                            opportunity.quantity())
                            + " x "
                            + formatUnitPrice(
                            opportunity.buyPricePerUnit()),
                    RotClientTheme.HUD_ACCENT);
        }

        if (visibleHeight >= 70) {
            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 59,
                    width,
                    "WAIT FILL  >  SELL OFFER    "
                            + formatUnitPrice(
                            opportunity.sellPricePerUnit()),
                    RotClientTheme.HUD_ACCENT);
        }

        if (visibleHeight >= 88) {
            drawDropdownLine(
                    graphics,
                    font,
                    x,
                    top + 77,
                    width,
                    "Tax "
                            + formatCoins(
                            opportunity.estimatedFeesCoins())
                            + "  |  Vol "
                            + formatLongCount(
                            opportunity.weeklyVolume())
                            + "  |  Edge "
                            + String.format(
                            Locale.ROOT,
                            "%.1f%%",
                            opportunity.marketEdgePercent()),
                    RotClientTheme.TEXT_MUTED);
        }
    }

    private static void drawDropdownLine(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String text,
            int color) {

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        text,
                        width - 22),
                x + 11,
                y,
                color,
                false);
    }

    /*
     * Smoothstep gives the dropdown an ease-in/ease-out motion instead of
     * linearly snapping between heights.
     */
    private int expandedHeight(
            MarketWatchOpportunity opportunity) {

        if (opportunity == null
                || animationOpportunityId.isBlank()
                || !opportunity.id()
                .equals(
                        animationOpportunityId)) {

            return 0;
        }

        double eased =
                smoothStep(
                        expansionProgress);

        return (int) Math.round(
                EXPANDED_EXTRA_HEIGHT
                        * eased);
    }

    private void advanceExpansionAnimation() {

        if (animationOpportunityId.isBlank()) {
            expansionProgress = 0.0D;
            lastAnimationNanos = 0L;
            return;
        }

        long now =
                System.nanoTime();

        if (lastAnimationNanos == 0L) {
            lastAnimationNanos = now;
            return;
        }

        double elapsed =
                Math.max(
                        0.0D,
                        (now
                                - lastAnimationNanos)
                                / 1_000_000_000.0D);

        lastAnimationNanos =
                now;

        double target =
                animationOpportunityId.equals(
                        expandedOpportunityId)
                        ? 1.0D
                        : 0.0D;

        double step =
                elapsed
                        / EXPANSION_SECONDS;

        if (target > expansionProgress) {
            expansionProgress =
                    Math.min(
                            1.0D,
                            expansionProgress
                                    + step);

        } else if (target < expansionProgress) {
            expansionProgress =
                    Math.max(
                            0.0D,
                            expansionProgress
                                    - step);
        }

        if (target == 0.0D
                && expansionProgress <= 0.0D) {

            animationOpportunityId = "";
            lastAnimationNanos = 0L;
        }
    }

    private static double smoothStep(
            double value) {

        double t =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                value));

        return t
                * t
                * (3.0D
                - 2.0D * t);
    }

    private boolean handlePinButtonClick(
            int mouseX,
            int mouseY) {

        if (pinButtonHits.isEmpty()) {
            return false;
        }

        for (PinButtonHit hit
                : pinButtonHits) {

            if (hit == null
                    || hit.opportunity() == null) {

                continue;
            }

            if (!RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    hit.x(),
                    hit.y(),
                    hit.width(),
                    hit.height())) {

                continue;
            }

            MarketWatchPinnedDealStore
                    .toggle(
                            hit.opportunity());

            /*
             * Deliberately keep the opportunity open so the user gets
             * immediate visual confirmation from PIN DEAL -> PINNED.
             */
            return true;
        }

        return false;
    }

    private void toggleExpanded(
            MarketWatchOpportunity opportunity) {

        if (opportunity == null) {
            return;
        }

        String id =
                opportunity.id();

        if (id.equals(
                expandedOpportunityId)) {

            /*
             * Keep animationOpportunityId until shrink reaches zero.
             */
            expandedOpportunityId = "";
            lastAnimationNanos =
                    System.nanoTime();

            return;
        }

        /*
         * Switching from another card starts the new dropdown from closed.
         * This avoids one card morphing into another card's height.
         */
        expandedOpportunityId =
                id;

        animationOpportunityId =
                id;

        expansionProgress =
                0.0D;

        lastAnimationNanos =
                System.nanoTime();
    }

    private void validateExpandedOpportunity(
            List<MarketWatchOpportunity> opportunities) {

        if (expandedOpportunityId.isBlank()
                && animationOpportunityId.isBlank()) {

            return;
        }

        boolean logicalFound =
                expandedOpportunityId.isBlank();

        boolean animatedFound =
                animationOpportunityId.isBlank();

        for (MarketWatchOpportunity opportunity
                : opportunities) {

            if (opportunity == null) {
                continue;
            }

            if (!logicalFound
                    && opportunity.id()
                    .equals(
                            expandedOpportunityId)) {

                logicalFound = true;
            }

            if (!animatedFound
                    && opportunity.id()
                    .equals(
                            animationOpportunityId)) {

                animatedFound = true;
            }
        }

        if (!logicalFound
                || !animatedFound) {

            closeExpandedImmediately();
        }
    }

    private void closeExpandedImmediately() {

        expandedOpportunityId = "";
        animationOpportunityId = "";
        expansionProgress = 0.0D;
        lastAnimationNanos = 0L;
    }

    private int gridContentHeight(
            List<MarketWatchOpportunity> opportunities) {

        int leftHeight =
                0;

        int rightHeight =
                0;

        int leftCount =
                0;

        int rightCount =
                0;

        for (int i = 0;
                i < opportunities.size();
                i++) {

            MarketWatchOpportunity opportunity =
                    opportunities.get(i);

            int height =
                    CARD_HEIGHT
                            + expandedHeight(
                            opportunity);

            if ((i & 1) == 0) {

                if (leftCount > 0) {
                    leftHeight +=
                            CARD_GAP_Y;
                }

                leftHeight +=
                        height;

                leftCount++;

            } else {

                if (rightCount > 0) {
                    rightHeight +=
                            CARD_GAP_Y;
                }

                rightHeight +=
                        height;

                rightCount++;
            }
        }

        return Math.max(
                leftHeight,
                rightHeight);
    }

    private GridLayout gridLayout(
            int left,
            int right) {

        int totalWidth =
                Math.max(
                        1,
                        right - left);

        int scrollbarReserve =
                12;

        int usableWidth =
                Math.max(
                        220,
                        totalWidth
                                - scrollbarReserve);

        int cardWidth =
                Math.max(
                        105,
                        (usableWidth
                                - CARD_GAP_X)
                                / 2);

        return new GridLayout(
                left,
                left
                        + cardWidth
                        + CARD_GAP_X,
                cardWidth);
    }

    private MarketWatchOpportunity cardAt(
            List<MarketWatchOpportunity> opportunities,
            int left,
            int listTop,
            int right,
            int mouseX,
            int mouseY) {

        GridLayout layout =
                gridLayout(
                        left,
                        right);

        int leftY =
                listTop
                        - scrollPixels;

        int rightY =
                listTop
                        - scrollPixels;

        for (int i = 0;
                i < opportunities.size();
                i++) {

            MarketWatchOpportunity opportunity =
                    opportunities.get(i);

            boolean rightColumn =
                    (i & 1) == 1;

            int x =
                    rightColumn
                            ? layout.rightX()
                            : layout.leftX();

            int y =
                    rightColumn
                            ? rightY
                            : leftY;

            /*
             * Only the compact header toggles the dropdown.
             * Clicking information inside the dropdown itself does nothing.
             */
            if (RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    x,
                    y,
                    layout.cardWidth(),
                    CARD_HEIGHT)) {

                return opportunity;
            }

            int consumed =
                    CARD_HEIGHT
                            + expandedHeight(
                            opportunity)
                            + CARD_GAP_Y;

            if (rightColumn) {
                rightY +=
                        consumed;
            } else {
                leftY +=
                        consumed;
            }
        }

        return null;
    }

    private void drawPinnedDealsButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int right,
            int y,
            int mouseX,
            int mouseY) {

        int count =
                MarketWatchPinnedDealStore
                        .all()
                        .size();

        int x =
                pinnedManagerButtonX(
                        right);

        String label =
                count <= 0
                        ? "PINNED"
                        : "PINNED  "
                        + count;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x,
                y,
                PIN_MANAGER_BUTTON_WIDTH,
                label
                        + (pinnedMenuOpen
                        ? " ^"
                        : " v"),
                pinnedMenuOpen
                        || count > 0,
                true);
    }

    private void drawPinnedDealsMenu(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int right,
            int filterY,
            int mouseX,
            int mouseY) {

        if (!pinnedMenuOpen
                && pinnedMenuProgress <= 0.001D) {

            return;
        }

        List<MarketWatchPinnedDeal> pins =
                MarketWatchPinnedDealStore
                        .all();

        int width =
                pinnedManagerWidth(
                        left,
                        right);

        int x =
                pinnedManagerMenuX(
                        left,
                        right,
                        width);

        int y =
                filterY
                        + RotClientUiDraw.BUTTON_HEIGHT
                        + PIN_MANAGER_MENU_GAP;

        int fullHeight =
                pinnedManagerFullHeight(
                        pins.size());

        int visibleHeight =
                animatedMenuHeight(
                        fullHeight,
                        pinnedMenuProgress);

        if (visibleHeight <= 1) {
            return;
        }

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                visibleHeight);

        graphics.enableScissor(
                x,
                y,
                x + width,
                y + visibleHeight);

        try {
            drawPremiumHeader(
                    graphics,
                    font,
                    x,
                    y,
                    width,
                    "PINNED DEALS",
                    pins.isEmpty()
                            ? "NONE SAVED"
                            : pins.size()
                            + " SAVED");

            if (pins.isEmpty()) {

                RotClientUiDraw.text(
                        graphics,
                        font,
                        "Pin a promising deal to keep its frozen price target here.",
                        x + 10,
                        y + PIN_MANAGER_HEADER_HEIGHT + 13,
                        RotClientTheme.TEXT_MUTED,
                        false);

                return;
            }

            int rowY =
                    y
                            + PIN_MANAGER_HEADER_HEIGHT;

            long now =
                    System.currentTimeMillis();

            for (MarketWatchPinnedDeal pin
                    : pins) {

                drawPinnedDealManagerRow(
                        graphics,
                        font,
                        pin,
                        x + 6,
                        rowY,
                        width - 12,
                        mouseX,
                        mouseY,
                        now);

                rowY +=
                        PIN_MANAGER_ROW_HEIGHT;
            }

            int clearWidth =
                    88;

            int clearX =
                    x
                            + width
                            - 8
                            - clearWidth;

            int clearY =
                    rowY
                            + 5;

            boolean clearHovered =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            clearX,
                            clearY,
                            clearWidth,
                            PIN_MANAGER_ACTION_HEIGHT);

            drawPremiumChoiceSurface(
                    graphics,
                    font,
                    clearX,
                    clearY,
                    clearWidth,
                    PIN_MANAGER_ACTION_HEIGHT,
                    "CLEAR ALL",
                    "",
                    false,
                    clearHovered,
                    false);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "Frozen targets stay here until you remove them.",
                    x + 10,
                    clearY + 7,
                    RotClientTheme.TEXT_MUTED,
                    false);

        } finally {
            graphics.disableScissor();
        }
    }

    private void drawPinnedDealManagerRow(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchPinnedDeal pin,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            long now) {

        if (pin == null) {
            return;
        }

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                PIN_MANAGER_ROW_HEIGHT - 2);

        graphics.fill(
                x,
                y + 6,
                x + 3,
                y + PIN_MANAGER_ROW_HEIGHT - 8,
                pin.market()
                        == MarketWatchOpportunity.Market.BAZAAR
                        ? RotClientTheme.VIOLET
                        : RotClientTheme.HUD_ACCENT);

        /*
         * Use the same resolver as the actual opportunity cards.
         */
        ItemStack icon =
                pinnedDealIcon(
                        pin);

        graphics.item(
                icon,
                x + 10,
                y + 10);

        int removeX =
                x
                        + width
                        - 8
                        - PIN_MANAGER_REMOVE_WIDTH;

        int removeY =
                y
                        + (PIN_MANAGER_ROW_HEIGHT
                        - PIN_MANAGER_ACTION_HEIGHT)
                        / 2;

        boolean removeHovered =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        removeX,
                        removeY,
                        PIN_MANAGER_REMOVE_WIDTH,
                        PIN_MANAGER_ACTION_HEIGHT);

        drawPremiumChoiceSurface(
                graphics,
                font,
                removeX,
                removeY,
                PIN_MANAGER_REMOVE_WIDTH,
                PIN_MANAGER_ACTION_HEIGHT,
                "REMOVE",
                "",
                false,
                removeHovered,
                false);

        /*
         * 16px icon + comfortable spacing before text.
         */
        int textX =
                x + 34;

        int textWidth =
                Math.max(
                        40,
                        removeX
                                - textX
                                - 12);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        pinnedDealDisplayName(
                                pin),
                        textWidth),
                textX,
                y + 6,
                RotClientTheme.TEXT,
                true);

        /*
         * Quantity is intentionally separate from the item title.
         * This avoids strings such as "Sand x6766" looking like the name.
         */
        String quantity =
                pin.quantity() > 1L
                        ? "  |  QTY "
                        + compactPinnedQuantity(
                                pin.quantity())
                        : "";

        String target =
                pin.market()
                        == MarketWatchOpportunity.Market.AUCTION_HOUSE
                        ? "BUY "
                        + pinnedCoins(
                                pin.capitalCoins())
                        + "  >  LIST "
                        + pinnedCoins(
                                pin.grossReturnCoins())
                        + quantity
                        : "BUY "
                        + pinnedCoins(
                                pin.capitalCoins())
                        + "  >  SELL "
                        + pinnedCoins(
                                pin.grossReturnCoins())
                        + quantity;

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        target,
                        textWidth),
                textX,
                y + 21,
                RotClientTheme.TEXT_MUTED,
                false);

        long age =
                Math.max(
                        0L,
                        now
                                - pin.pinnedAtMillis());

        String stats =
                pinnedSignedCoins(
                        pin.expectedProfitCoins())
                        + "  |  "
                        + String.format(
                        Locale.ROOT,
                        "%.1f%% ROI",
                        pin.roiPercent())
                        + "  |  "
                        + (age >= PIN_MANAGER_STALE_MILLIS
                        ? "STALE "
                        : "")
                        + pinnedAgeLabel(
                        age);

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        stats,
                        textWidth),
                textX,
                y + 36,
                age >= PIN_MANAGER_STALE_MILLIS
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.SUCCESS,
                false);
    }

    private static int pinnedManagerButtonX(
            int right) {

        return right
                - SORT_WIDTH
                - CONTROL_GAP
                - FILTER_CONTROL_WIDTH
                - CONTROL_GAP
                - PIN_MANAGER_BUTTON_WIDTH;
    }

    private static int pinnedManagerButtonRight(
            int right) {

        return pinnedManagerButtonX(
                right)
                + PIN_MANAGER_BUTTON_WIDTH;
    }

    private static int pinnedManagerWidth(
            int left,
            int right) {

        return Math.min(
                PIN_MANAGER_MENU_WIDTH,
                Math.max(
                        1,
                        right - left));
    }

    private static int pinnedManagerMenuX(
            int left,
            int right,
            int width) {

        return Math.max(
                left,
                pinnedManagerButtonRight(
                        right)
                        - width);
    }

    private static int pinnedManagerFullHeight(
            int pinCount) {

        if (pinCount <= 0) {
            return PIN_MANAGER_HEADER_HEIGHT
                    + 42;
        }

        return PIN_MANAGER_HEADER_HEIGHT
                + pinCount
                * PIN_MANAGER_ROW_HEIGHT
                + PIN_MANAGER_FOOTER_HEIGHT;
    }

    private static String pinnedAgeLabel(
            long millis) {

        long seconds =
                Math.max(
                        0L,
                        millis / 1000L);

        if (seconds < 60L) {
            return seconds
                    + "s";
        }

        long minutes =
                seconds / 60L;

        if (minutes < 60L) {
            return minutes
                    + "m";
        }

        long hours =
                minutes / 60L;

        return hours
                + "h "
                + (minutes % 60L)
                + "m";
    }

    private static String pinnedSignedCoins(
            double value) {

        if (!Double.isFinite(value)) {
            return "0";
        }

        return (value >= 0.0D
                ? "+"
                : "-")
                + pinnedCoins(
                Math.abs(value));
    }

    private static String pinnedCoins(
            double value) {

        if (!Double.isFinite(value)) {
            return "0";
        }

        double absolute =
                Math.abs(value);

        if (absolute >= 1_000_000_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.2fB",
                    value
                            / 1_000_000_000.0D);
        }

        if (absolute >= 1_000_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    value
                            / 1_000_000.0D);
        }

        if (absolute >= 1_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.1fK",
                    value
                            / 1_000.0D);
        }

        return String.format(
                Locale.ROOT,
                "%.0f",
                value);
    }

    private ItemStack pinnedDealIcon(
            MarketWatchPinnedDeal pin) {

        if (pin == null) {
            return ItemStack.EMPTY;
        }

        if (pin.market()
                == MarketWatchOpportunity.Market.BAZAAR) {

            return MarketWatchItemIconResolver
                    .bazaarIcon(
                            pin.itemId());
        }

        return MarketWatchItemIconResolver
                .auctionIcon(
                        pin.category(),
                        pinnedDealDisplayName(
                                pin));
    }

    private static String pinnedDealDisplayName(
            MarketWatchPinnedDeal pin) {

        return pin == null
                ? "Unknown Item"
                : pin.displayName();
    }

    private static String compactPinnedQuantity(
            long quantity) {

        long safe =
                Math.max(
                        1L,
                        quantity);

        if (safe >= 1_000_000_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.2fB",
                    safe
                            / 1_000_000_000.0D);
        }

        if (safe >= 1_000_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    safe
                            / 1_000_000.0D);
        }

        if (safe >= 1_000L) {
            return String.format(
                    Locale.ROOT,
                    "%.1fK",
                    safe
                            / 1_000.0D);
        }

        return Long.toString(
                safe);
    }

    private void drawFilterButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int right,
            int y,
            int mouseX,
            int mouseY) {

        int x =
                right
                        - SORT_WIDTH
                        - CONTROL_GAP
                        - FILTER_CONTROL_WIDTH;

        int active =
                activeAdvancedFilterCount();

        String label =
                active <= 0
                        ? "FILTERS"
                        : "FILTERS  "
                        + active;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x,
                y,
                FILTER_CONTROL_WIDTH,
                label
                        + (filterMenuOpen
                        ? " ^"
                        : " v"),
                filterMenuOpen
                        || active > 0,
                true);
    }

    private void drawSortButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int right,
            int y,
            int mouseX,
            int mouseY) {

        int x =
                right
                        - SORT_WIDTH;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x,
                y,
                SORT_WIDTH,
                sortMode.label()
                        + (sortMenuOpen
                        ? " ^"
                        : " v"),
                sortMenuOpen,
                true);
    }

    /*
     * -----------------------------------------------------------------
     * PREMIUM SELECTOR SYSTEM
     * -----------------------------------------------------------------
     *
     * Sort, rarity, item type, liquidity and confidence all use the same
     * option-row renderer. This keeps spacing, hover behaviour, selection
     * state and typography consistent.
     */

    private void drawControlOverlays(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int right,
            int filterY,
            int mouseX,
            int mouseY) {

        /*
         * Filters expand inline. Sort and Pinned Deals are floating
         * premium overlays and are mutually exclusive.
         */
        drawPinnedDealsMenu(
                graphics,
                font,
                left,
                right,
                filterY,
                mouseX,
                mouseY);

        drawSortMenu(
                graphics,
                font,
                right,
                filterY,
                mouseX,
                mouseY);
    }
    private void drawSortMenu(
            GuiGraphicsExtractor graphics,
            Font font,
            int right,
            int filterY,
            int mouseX,
            int mouseY) {

        if (!sortMenuOpen
                && sortMenuProgress <= 0.001D) {

            return;
        }

        int x =
                right
                        - PREMIUM_MENU_WIDTH;

        int y =
                filterY
                        + RotClientUiDraw.BUTTON_HEIGHT
                        + SORT_MENU_GAP;

        int fullHeight =
                PREMIUM_HEADER_HEIGHT
                        + SortMode.values().length
                        * PREMIUM_OPTION_HEIGHT
                        + 8;

        int visibleHeight =
                animatedMenuHeight(
                        fullHeight,
                        sortMenuProgress);

        if (visibleHeight <= 1) {
            return;
        }

        /*
         * The card itself grows from 0 -> full height.
         * This is the same visual model as an expanding deal card.
         */
        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                PREMIUM_MENU_WIDTH,
                visibleHeight);

        graphics.enableScissor(
                x,
                y,
                x + PREMIUM_MENU_WIDTH,
                y + visibleHeight);

        try {
            drawPremiumHeader(
                    graphics,
                    font,
                    x,
                    y,
                    PREMIUM_MENU_WIDTH,
                    "SORT DEALS",
                    sortMode.label());

            int rowY =
                    y
                            + PREMIUM_HEADER_HEIGHT;

            for (SortMode mode
                    : SortMode.values()) {

                drawPremiumOptionRow(
                        graphics,
                        font,
                        x,
                        rowY,
                        mode.label(),
                        sortDescription(
                                mode),
                        sortMode == mode,
                        mouseX,
                        mouseY);

                rowY +=
                        PREMIUM_OPTION_HEIGHT;
            }
        } finally {
            graphics.disableScissor();
        }
    }

    private void drawFilterMenu(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int right,
            int trayY,
            int mouseX,
            int mouseY) {

        if (!filterMenuOpen
                && filterMenuProgress <= 0.001D) {

            return;
        }

        int width =
                filterTrayWidth(
                        left,
                        right);

        int x =
                right
                        - width;

        int y =
                trayY;

        int fullHeight =
                filterTrayHeight(
                        width);

        int visibleHeight =
                animatedMenuHeight(
                        fullHeight,
                        filterMenuProgress);

        if (visibleHeight <= 1) {
            return;
        }

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                visibleHeight);

        graphics.enableScissor(
                x,
                y,
                x + width,
                y + visibleHeight);

        try {
            drawFilterTrayHeader(
                    graphics,
                    font,
                    x,
                    y,
                    width,
                    mouseX,
                    mouseY);

            int labelWidth =
                    filterTrayLabelWidth(
                            width);

            int chipX =
                    x
                            + FILTER_TRAY_PADDING
                            + labelWidth;

            int chipAreaWidth =
                    Math.max(
                            1,
                            width
                                    - FILTER_TRAY_PADDING * 2
                                    - labelWidth);

            int sectionY =
                    y
                            + PREMIUM_HEADER_HEIGHT;

            /*
             * AH-only classification.
             */
            if (filter
                    == Filter.AUCTION_HOUSE) {

                sectionY =
                        drawFilterTrayGroup(
                                graphics,
                                font,
                                x,
                                chipX,
                                chipAreaWidth,
                                sectionY,
                                "RARITY",
                                RARITY_OPTIONS,
                                -1,
                                mouseX,
                                mouseY);

                sectionY +=
                        FILTER_TRAY_SECTION_GAP;

                sectionY =
                        drawFilterTrayGroup(
                                graphics,
                                font,
                                x,
                                chipX,
                                chipAreaWidth,
                                sectionY,
                                "CATEGORY",
                                CATEGORY_OPTIONS,
                                -1,
                                mouseX,
                                mouseY);

                sectionY +=
                        FILTER_TRAY_SECTION_GAP;
            }

            /*
             * Filters meaningful for both AH and Bazaar.
             */
            sectionY =
                    drawFilterTrayGroup(
                            graphics,
                            font,
                            x,
                            chipX,
                            chipAreaWidth,
                            sectionY,
                            "LIQUIDITY",
                            LIQUIDITY_OPTIONS,
                            liquidityFilter.ordinal(),
                            mouseX,
                            mouseY);

            sectionY +=
                    FILTER_TRAY_SECTION_GAP;

            sectionY =
                    drawFilterTrayGroup(
                            graphics,
                            font,
                            x,
                            chipX,
                            chipAreaWidth,
                            sectionY,
                            "CONFIDENCE",
                            CONFIDENCE_OPTIONS,
                            confidenceFilter.ordinal(),
                            mouseX,
                            mouseY);

            sectionY +=
                    FILTER_TRAY_SECTION_GAP;

            sectionY =
                    drawFilterTrayGroup(
                            graphics,
                            font,
                            x,
                            chipX,
                            chipAreaWidth,
                            sectionY,
                            "MIN ROI",
                            ROI_OPTIONS,
                            roiFilter.ordinal(),
                            mouseX,
                            mouseY);

            sectionY +=
                    FILTER_TRAY_SECTION_GAP;

            sectionY =
                    drawFilterTrayGroup(
                            graphics,
                            font,
                            x,
                            chipX,
                            chipAreaWidth,
                            sectionY,
                            "MIN PROFIT",
                            PROFIT_OPTIONS,
                            profitFilter.ordinal(),
                            mouseX,
                            mouseY);

            sectionY +=
                    FILTER_TRAY_SECTION_GAP;

            sectionY =
                    drawFilterTrayGroup(
                            graphics,
                            font,
                            x,
                            chipX,
                            chipAreaWidth,
                            sectionY,
                            "MIN EDGE",
                            EDGE_OPTIONS,
                            edgeFilter.ordinal(),
                            mouseX,
                            mouseY);

            /*
             * Bazaar-specific market activity filter.
             */
            if (filter
                    == Filter.BAZAAR) {

                sectionY +=
                        FILTER_TRAY_SECTION_GAP;

                drawFilterTrayGroup(
                        graphics,
                        font,
                        x,
                        chipX,
                        chipAreaWidth,
                        sectionY,
                        "WEEKLY VOL",
                        WEEKLY_VOLUME_OPTIONS,
                        volumeFilter.ordinal(),
                        mouseX,
                        mouseY);
            }
        } finally {
            graphics.disableScissor();
        }
    }
    private void drawFilterTrayHeader(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.text(
                graphics,
                font,
                "FILTER DEALS",
                x + FILTER_TRAY_PADDING,
                y + 10,
                RotClientTheme.TEXT,
                true);

        int active =
                activeAdvancedFilterCount();

        if (active <= 0) {

            String state =
                    "ALL ITEMS";

            RotClientUiDraw.text(
                    graphics,
                    font,
                    state,
                    x
                            + width
                            - FILTER_TRAY_PADDING
                            - font.width(
                            state),
                    y + 10,
                    RotClientTheme.TEXT_MUTED,
                    false);

            return;
        }

        String reset =
                "RESET ALL";

        int resetX =
                x
                        + width
                        - FILTER_TRAY_PADDING
                        - font.width(
                        reset);

        boolean hovered =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        resetX - 6,
                        y + 4,
                        font.width(
                                reset) + 12,
                        PREMIUM_HEADER_HEIGHT - 6);

        RotClientUiDraw.text(
                graphics,
                font,
                reset,
                resetX,
                y + 10,
                hovered
                        ? RotClientTheme.TEXT
                        : RotClientTheme.HUD_ACCENT,
                true);

        String activeText =
                active + " ACTIVE";

        int activeX =
                resetX
                        - 12
                        - font.width(
                        activeText);

        if (activeX
                > x
                + FILTER_TRAY_PADDING
                + 90) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    activeText,
                    activeX,
                    y + 10,
                    RotClientTheme.TEXT_MUTED,
                    false);
        }
    }

    private int drawFilterTrayGroup(
            GuiGraphicsExtractor graphics,
            Font font,
            int trayX,
            int chipStartX,
            int availableWidth,
            int y,
            String groupTitle,
            String[] options,
            int selectedIndex,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.text(
                graphics,
                font,
                groupTitle,
                trayX + FILTER_TRAY_PADDING,
                y + 7,
                RotClientTheme.TEXT_MUTED,
                true);

        int cursorX =
                chipStartX;

        int cursorY =
                y;

        int rightEdge =
                chipStartX
                        + availableWidth;

        for (int index = 0;
                index < options.length;
                index++) {

            String label =
                    options[index];

            int chipWidth =
                    filterChipWidth(
                            label,
                            availableWidth);

            if (cursorX > chipStartX
                    && cursorX
                    + chipWidth
                    > rightEdge) {

                cursorX =
                        chipStartX;

                cursorY +=
                        FILTER_CHIP_HEIGHT
                                + FILTER_CHIP_GAP;
            }

            boolean hovered =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            cursorX,
                            cursorY,
                            chipWidth,
                            FILTER_CHIP_HEIGHT);

            drawPremiumChoiceSurface(
                    graphics,
                    font,
                    cursorX,
                    cursorY,
                    chipWidth,
                    FILTER_CHIP_HEIGHT,
                    label,
                    "",
                    filterChipSelected(
                            groupTitle,
                            index,
                            selectedIndex),
                    hovered,
                    false);

            cursorX +=
                    chipWidth
                            + FILTER_CHIP_GAP;
        }

        return y
                + filterChipGroupHeight(
                options,
                availableWidth);
    }

    private boolean filterChipSelected(
            String groupTitle,
            int index,
            int selectedIndex) {

        if ("RARITY".equals(
                groupTitle)) {

            if (index == 0) {
                return rarityFilters.isEmpty();
            }

            RarityFilter[] values =
                    RarityFilter.values();

            return index < values.length
                    && rarityFilters.contains(
                    values[index]);
        }

        if ("CATEGORY".equals(
                groupTitle)) {

            if (index == 0) {
                return typeFilters.isEmpty();
            }

            TypeFilter[] values =
                    TypeFilter.values();

            return index < values.length
                    && typeFilters.contains(
                    values[index]);
        }

        return selectedIndex
                == index;
    }

    /*
     * Shared renderer    /*
     * Shared renderer for sort options and filter chips.
     */
    private void drawPremiumChoiceSurface(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String title,
            String description,
            boolean selected,
            boolean hovered,
            boolean showSelectedText) {

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                height);

        /*
         * A soft accent surface feels less harsh than the old outline-only
         * buttons while retaining the existing Rot Client visual language.
         */
        if (selected) {

            graphics.fill(
                    x + 1,
                    y + 1,
                    x + width - 1,
                    y + height - 1,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            hovered
                                    ? 58
                                    : 42));

            graphics.fill(
                    x,
                    y,
                    x + 3,
                    y + height,
                    RotClientTheme.HUD_ACCENT);

        } else if (hovered) {

            graphics.fill(
                    x + 1,
                    y + 1,
                    x + width - 1,
                    y + height - 1,
                    withAlpha(
                            RotClientTheme.HUD_ACCENT,
                            18));
        }

        boolean hasDescription =
                description != null
                        && !description.isBlank();

        int titleAvailable =
                width - 18;

        if (selected
                && showSelectedText) {

            titleAvailable -=
                    font.width(
                            "SELECTED")
                            + 14;
        }

        String shownTitle =
                fitChoiceLabel(
                        font,
                        title,
                        titleAvailable);

        int titleY =
                hasDescription
                        ? y + 6
                        : y
                        + Math.max(
                        4,
                        (height - 9) / 2);

        RotClientUiDraw.text(
                graphics,
                font,
                shownTitle,
                x + 9,
                titleY,
                selected
                        || hovered
                        ? RotClientTheme.HUD_ACCENT
                        : RotClientTheme.TEXT,
                true);

        if (hasDescription) {

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    description,
                    x + 9,
                    y + 18);
        }

        if (selected
                && showSelectedText) {

            String selectedText =
                    "SELECTED";

            RotClientUiDraw.text(
                    graphics,
                    font,
                    selectedText,
                    x
                            + width
                            - 9
                            - font.width(
                            selectedText),
                    y + 6,
                    RotClientTheme.SUCCESS,
                    false);
        }
    }

    private static String fitChoiceLabel(
            Font font,
            String title,
            int availableWidth) {

        if (title == null) {
            return "";
        }

        int safeWidth =
                Math.max(
                        1,
                        availableWidth);

        if (font.width(
                title) <= safeWidth) {

            return title;
        }

        String ellipsis =
                "...";

        int ellipsisWidth =
                font.width(
                        ellipsis);

        if (ellipsisWidth >= safeWidth) {
            return "";
        }

        for (int length =
                title.length() - 1;
                length > 0;
                length--) {

            String candidate =
                    title.substring(
                            0,
                            length)
                            + ellipsis;

            if (font.width(
                    candidate) <= safeWidth) {

                return candidate;
            }
        }

        return "";
    }

    private static int withAlpha(
            int color,
            int alpha) {

        int safeAlpha =
                Math.max(
                        0,
                        Math.min(
                                255,
                                alpha));

        return (safeAlpha << 24)
                | (color
                & 0x00FFFFFF);
    }
    private void drawPremiumHeader(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String title,
            String status) {

        RotClientUiDraw.text(
                graphics,
                font,
                title,
                x + 10,
                y + 10,
                RotClientTheme.TEXT,
                true);

        if (status == null
                || status.isBlank()) {

            return;
        }

        RotClientUiDraw.text(
                graphics,
                font,
                status,
                x
                        + width
                        - 10
                        - font.width(
                        status),
                y + 10,
                RotClientTheme.TEXT_MUTED,
                false);
    }

    private void drawPremiumOptionRow(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            String title,
            String description,
            boolean selected,
            int mouseX,
            int mouseY) {

        int rowX =
                x + 5;

        int rowWidth =
                PREMIUM_MENU_WIDTH - 10;

        int rowHeight =
                PREMIUM_OPTION_HEIGHT - 2;

        boolean hovered =
                RotClientUiDraw.inside(
                        mouseX,
                        mouseY,
                        rowX,
                        y,
                        rowWidth,
                        rowHeight);

        drawPremiumChoiceSurface(
                graphics,
                font,
                rowX,
                y,
                rowWidth,
                rowHeight,
                title,
                description,
                selected,
                hovered,
                true);
    }

    private boolean handlePinnedDealsClick(
            int mouseX,
            int mouseY,
            int left,
            int right,
            int filterY) {

        int buttonX =
                pinnedManagerButtonX(
                        right);

        if (RotClientUiDraw.inside(
                mouseX,
                mouseY,
                buttonX,
                filterY,
                PIN_MANAGER_BUTTON_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            pinnedMenuOpen =
                    !pinnedMenuOpen;

            if (pinnedMenuOpen) {

                sortMenuOpen =
                        false;

                filterMenuOpen =
                        false;

                sortMenuProgress =
                        0.0D;

                filterMenuProgress =
                        0.0D;
            }

            return true;
        }

        if (!pinnedMenuOpen) {

            /*
             * Consume clicks while the closing animation is still visible
             * so controls underneath cannot be clicked through it.
             */
            return pinnedMenuProgress
                    > 0.001D;
        }

        List<MarketWatchPinnedDeal> pins =
                MarketWatchPinnedDealStore
                        .all();

        int width =
                pinnedManagerWidth(
                        left,
                        right);

        int x =
                pinnedManagerMenuX(
                        left,
                        right,
                        width);

        int y =
                filterY
                        + RotClientUiDraw.BUTTON_HEIGHT
                        + PIN_MANAGER_MENU_GAP;

        int visibleHeight =
                animatedMenuHeight(
                        pinnedManagerFullHeight(
                                pins.size()),
                        pinnedMenuProgress);

        if (!RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x,
                y,
                width,
                Math.max(
                        1,
                        visibleHeight))) {

            pinnedMenuOpen =
                    false;

            return false;
        }

        if (pins.isEmpty()) {
            return true;
        }

        int rowY =
                y
                        + PIN_MANAGER_HEADER_HEIGHT;

        int rowWidth =
                width - 12;

        for (MarketWatchPinnedDeal pin
                : pins) {

            int removeX =
                    x
                            + 6
                            + rowWidth
                            - 8
                            - PIN_MANAGER_REMOVE_WIDTH;

            int removeY =
                    rowY
                            + (PIN_MANAGER_ROW_HEIGHT
                            - PIN_MANAGER_ACTION_HEIGHT)
                            / 2;

            if (mouseY
                    < y + visibleHeight
                    && RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    removeX,
                    removeY,
                    PIN_MANAGER_REMOVE_WIDTH,
                    PIN_MANAGER_ACTION_HEIGHT)) {

                MarketWatchPinnedDealStore
                        .remove(
                                pin.id());

                return true;
            }

            rowY +=
                    PIN_MANAGER_ROW_HEIGHT;
        }

        int clearWidth =
                88;

        int clearX =
                x
                        + width
                        - 8
                        - clearWidth;

        int clearY =
                rowY + 5;

        if (mouseY
                < y + visibleHeight
                && RotClientUiDraw.inside(
                mouseX,
                mouseY,
                clearX,
                clearY,
                clearWidth,
                PIN_MANAGER_ACTION_HEIGHT)) {

            MarketWatchPinnedDealStore
                    .clear();

            return true;
        }

        return true;
    }

    private boolean handleSortClick(
            int mouseX,
            int mouseY,
            int right,
            int filterY) {

        int buttonX =
                right
                        - SORT_WIDTH;

        if (RotClientUiDraw.inside(
                mouseX,
                mouseY,
                buttonX,
                filterY,
                SORT_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            sortMenuOpen =
                    !sortMenuOpen;

            if (sortMenuOpen) {

                pinnedMenuOpen =
                        false;

                pinnedMenuProgress =
                        0.0D;

                filterMenuOpen =
                        false;

                /*
                 * The other overlay disappears immediately when switching
                 * controls so the two cards never visually overlap.
                 */
                filterMenuProgress =
                        0.0D;
            }

            return true;
        }

        if (!sortMenuOpen) {
            return sortMenuProgress
                    > 0.001D;
        }

        int x =
                right
                        - PREMIUM_MENU_WIDTH;

        int y =
                filterY
                        + RotClientUiDraw.BUTTON_HEIGHT
                        + SORT_MENU_GAP;

        int fullHeight =
                PREMIUM_HEADER_HEIGHT
                        + SortMode.values().length
                        * PREMIUM_OPTION_HEIGHT
                        + 8;

        int visibleHeight =
                animatedMenuHeight(
                        fullHeight,
                        sortMenuProgress);

        int visibleBottom =
                y
                        + visibleHeight;

        if (!RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x,
                y,
                PREMIUM_MENU_WIDTH,
                Math.max(
                        1,
                        visibleHeight))) {

            sortMenuOpen =
                    false;

            return false;
        }

        int rowY =
                y
                        + PREMIUM_HEADER_HEIGHT;

        for (SortMode mode
                : SortMode.values()) {

            if (mouseY < visibleBottom
                    && RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    x + 5,
                    rowY,
                    PREMIUM_MENU_WIDTH - 10,
                    PREMIUM_OPTION_HEIGHT - 2)) {

                sortMode =
                        mode;

                sortMenuOpen =
                        false;

                scrollPixels =
                        0;

                closeExpandedImmediately();

                return true;
            }

            rowY +=
                    PREMIUM_OPTION_HEIGHT;
        }

        return true;
    }

    private boolean handleFilterClick(
            int mouseX,
            int mouseY,
            int left,
            int right,
            int filterY,
            int trayY) {

        int buttonRight =
                right
                        - SORT_WIDTH
                        - CONTROL_GAP;

        int buttonX =
                buttonRight
                        - FILTER_CONTROL_WIDTH;

        if (RotClientUiDraw.inside(
                mouseX,
                mouseY,
                buttonX,
                filterY,
                FILTER_CONTROL_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            filterMenuOpen =
                    !filterMenuOpen;

            if (filterMenuOpen) {

                pinnedMenuOpen =
                        false;

                pinnedMenuProgress =
                        0.0D;

                sortMenuOpen =
                        false;

                sortMenuProgress =
                        0.0D;
            }

            return true;
        }

        if (!filterMenuOpen) {
            return filterMenuProgress
                    > 0.001D;
        }

        int width =
                filterTrayWidth(
                        left,
                        right);

        int x =
                right
                        - width;

        int y =
                trayY;

        int fullHeight =
                filterTrayHeight(
                        width);

        int visibleHeight =
                animatedMenuHeight(
                        fullHeight,
                        filterMenuProgress);

        if (!RotClientUiDraw.inside(
                mouseX,
                mouseY,
                x,
                y,
                width,
                Math.max(
                        1,
                        visibleHeight))) {

            filterMenuOpen =
                    false;

            return false;
        }

        if (activeAdvancedFilterCount() > 0) {

            int resetWidth =
                    72;

            if (RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    x
                            + width
                            - FILTER_TRAY_PADDING
                            - resetWidth,
                    y + 3,
                    resetWidth
                            + FILTER_TRAY_PADDING,
                    PREMIUM_HEADER_HEIGHT - 4)) {

                resetAdvancedFilters();

                return true;
            }
        }

        int labelWidth =
                filterTrayLabelWidth(
                        width);

        int chipX =
                x
                        + FILTER_TRAY_PADDING
                        + labelWidth;

        int chipAreaWidth =
                Math.max(
                        1,
                        width
                                - FILTER_TRAY_PADDING * 2
                                - labelWidth);

        int sectionY =
                y
                        + PREMIUM_HEADER_HEIGHT;

        int hit;

        if (filter
                == Filter.AUCTION_HOUSE) {

            hit =
                    filterChipIndexAt(
                            RARITY_OPTIONS,
                            chipX,
                            sectionY,
                            chipAreaWidth,
                            mouseX,
                            mouseY);

            if (hit >= 0
                    && hit
                    < RarityFilter.values().length) {

                if (hit == 0) {

                    rarityFilters.clear();

                } else {

                    RarityFilter choice =
                            RarityFilter.values()[hit];

                    if (!rarityFilters.add(
                            choice)) {

                        rarityFilters.remove(
                                choice);
                    }
                }

                filtersChanged();

                return true;
            }

            sectionY +=
                    filterChipGroupHeight(
                            RARITY_OPTIONS,
                            chipAreaWidth)
                            + FILTER_TRAY_SECTION_GAP;

            hit =
                    filterChipIndexAt(
                            CATEGORY_OPTIONS,
                            chipX,
                            sectionY,
                            chipAreaWidth,
                            mouseX,
                            mouseY);

            if (hit >= 0
                    && hit
                    < TypeFilter.values().length) {

                if (hit == 0) {

                    typeFilters.clear();

                } else {

                    TypeFilter choice =
                            TypeFilter.values()[hit];

                    if (!typeFilters.add(
                            choice)) {

                        typeFilters.remove(
                                choice);
                    }
                }

                filtersChanged();

                return true;
            }

            sectionY +=
                    filterChipGroupHeight(
                            CATEGORY_OPTIONS,
                            chipAreaWidth)
                            + FILTER_TRAY_SECTION_GAP;
        }

        hit =
                filterChipIndexAt(
                        LIQUIDITY_OPTIONS,
                        chipX,
                        sectionY,
                        chipAreaWidth,
                        mouseX,
                        mouseY);

        if (hit >= 0
                && hit
                < LiquidityFilter.values().length) {

            liquidityFilter =
                    LiquidityFilter.values()[hit];

            filtersChanged();

            return true;
        }

        sectionY +=
                filterChipGroupHeight(
                        LIQUIDITY_OPTIONS,
                        chipAreaWidth)
                        + FILTER_TRAY_SECTION_GAP;

        hit =
                filterChipIndexAt(
                        CONFIDENCE_OPTIONS,
                        chipX,
                        sectionY,
                        chipAreaWidth,
                        mouseX,
                        mouseY);

        if (hit >= 0
                && hit
                < ConfidenceFilter.values().length) {

            confidenceFilter =
                    ConfidenceFilter.values()[hit];

            filtersChanged();

            return true;
        }

        sectionY +=
                filterChipGroupHeight(
                        CONFIDENCE_OPTIONS,
                        chipAreaWidth)
                        + FILTER_TRAY_SECTION_GAP;

        hit =
                filterChipIndexAt(
                        ROI_OPTIONS,
                        chipX,
                        sectionY,
                        chipAreaWidth,
                        mouseX,
                        mouseY);

        if (hit >= 0
                && hit
                < RoiFilter.values().length) {

            roiFilter =
                    RoiFilter.values()[hit];

            filtersChanged();

            return true;
        }

        sectionY +=
                filterChipGroupHeight(
                        ROI_OPTIONS,
                        chipAreaWidth)
                        + FILTER_TRAY_SECTION_GAP;

        hit =
                filterChipIndexAt(
                        PROFIT_OPTIONS,
                        chipX,
                        sectionY,
                        chipAreaWidth,
                        mouseX,
                        mouseY);

        if (hit >= 0
                && hit
                < ProfitFilter.values().length) {

            profitFilter =
                    ProfitFilter.values()[hit];

            filtersChanged();

            return true;
        }

        sectionY +=
                filterChipGroupHeight(
                        PROFIT_OPTIONS,
                        chipAreaWidth)
                        + FILTER_TRAY_SECTION_GAP;

        hit =
                filterChipIndexAt(
                        EDGE_OPTIONS,
                        chipX,
                        sectionY,
                        chipAreaWidth,
                        mouseX,
                        mouseY);

        if (hit >= 0
                && hit
                < EdgeFilter.values().length) {

            edgeFilter =
                    EdgeFilter.values()[hit];

            filtersChanged();

            return true;
        }

        sectionY +=
                filterChipGroupHeight(
                        EDGE_OPTIONS,
                        chipAreaWidth);

        if (filter
                == Filter.BAZAAR) {

            sectionY +=
                    FILTER_TRAY_SECTION_GAP;

            hit =
                    filterChipIndexAt(
                            WEEKLY_VOLUME_OPTIONS,
                            chipX,
                            sectionY,
                            chipAreaWidth,
                            mouseX,
                            mouseY);

            if (hit >= 0
                    && hit
                    < VolumeFilter.values().length) {

                volumeFilter =
                        VolumeFilter.values()[hit];

                filtersChanged();

                return true;
            }
        }

        return true;
    }
    private void resetAdvancedFilters() {

        rarityFilters.clear();

        typeFilters.clear();

        liquidityFilter =
                LiquidityFilter.ANY;

        confidenceFilter =
                ConfidenceFilter.ANY;

        roiFilter =
                RoiFilter.ANY;

        profitFilter =
                ProfitFilter.ANY;

        edgeFilter =
                EdgeFilter.ANY;

        volumeFilter =
                VolumeFilter.ANY;

        filtersChanged();
    }
    private void filtersChanged() {

        scrollPixels =
                0;

        closeExpandedImmediately();

        saveFilterPreferences();
    }

    private void ensureFilterPreferencesLoaded() {

        if (preferencesLoaded) {
            return;
        }

        /*
         * Mark first so a malformed setting can never cause repeated
         * load attempts every rendered frame.
         */
        preferencesLoaded =
                true;

        MarketWatchOpportunityPreferences.FilterPreferences saved =
                MarketWatchOpportunityPreferences
                        .loadFilterPreferences();

        if (saved == null) {
            return;
        }

        restoreEnumSet(
                rarityFilters,
                RarityFilter.class,
                saved.rarities());

        restoreEnumSet(
                typeFilters,
                TypeFilter.class,
                saved.types());

        liquidityFilter =
                parseEnum(
                        LiquidityFilter.class,
                        saved.liquidity(),
                        LiquidityFilter.ANY);

        confidenceFilter =
                parseEnum(
                        ConfidenceFilter.class,
                        saved.confidence(),
                        ConfidenceFilter.ANY);

        roiFilter =
                parseEnum(
                        RoiFilter.class,
                        saved.roi(),
                        RoiFilter.ANY);

        profitFilter =
                parseEnum(
                        ProfitFilter.class,
                        saved.profit(),
                        ProfitFilter.ANY);

        edgeFilter =
                parseEnum(
                        EdgeFilter.class,
                        saved.edge(),
                        EdgeFilter.ANY);

        volumeFilter =
                parseEnum(
                        VolumeFilter.class,
                        saved.volume(),
                        VolumeFilter.ANY);
    }

    private void saveFilterPreferences() {

        if (!preferencesLoaded) {
            return;
        }

        MarketWatchOpportunityPreferences
                .saveFilterPreferences(
                        new MarketWatchOpportunityPreferences
                                .FilterPreferences(
                                serializeEnums(
                                        rarityFilters),
                                serializeEnums(
                                        typeFilters),
                                liquidityFilter.name(),
                                confidenceFilter.name(),
                                roiFilter.name(),
                                profitFilter.name(),
                                edgeFilter.name(),
                                volumeFilter.name()));
    }

    private static <E extends Enum<E>>
    void restoreEnumSet(
            java.util.EnumSet<E> target,
            Class<E> type,
            String raw) {

        if (target == null
                || type == null) {

            return;
        }

        target.clear();

        if (raw == null
                || raw.isBlank()) {

            return;
        }

        for (String token
                : raw.split(",")) {

            String value =
                    token == null
                            ? ""
                            : token.trim();

            if (value.isBlank()) {
                continue;
            }

            try {
                target.add(
                        Enum.valueOf(
                                type,
                                value.toUpperCase(
                                        Locale.ROOT)));

            } catch (IllegalArgumentException ignored) {
                /*
                 * Old/removed filter enum values are ignored so
                 * future changes remain backwards-compatible.
                 */
            }
        }
    }

    private static <E extends Enum<E>> E parseEnum(
            Class<E> type,
            String raw,
            E fallback) {

        if (type == null
                || raw == null
                || raw.isBlank()) {

            return fallback;
        }

        try {
            return Enum.valueOf(
                    type,
                    raw.trim()
                            .toUpperCase(
                                    Locale.ROOT));

        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static String serializeEnums(
            Iterable<? extends Enum<?>> values) {

        if (values == null) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        for (Enum<?> value
                : values) {

            if (value == null) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(',');
            }

            result.append(
                    value.name());
        }

        return result.toString();
    }

    private static String sortDescription(
            SortMode mode) {

        return switch (mode) {
            case QUALITY ->
                    "Balanced overall deal strength";
            case PROFIT ->
                    "Highest expected coin profit";
            case ROI ->
                    "Best return on your capital";
            case LIQUIDITY ->
                    "Most liquid markets first";
            case EDGE ->
                    "Largest market price advantage";
            case CAPITAL ->
                    "Lowest required investment";
        };
    }

    private int activeAdvancedFilterCount() {

        int count =
                0;

        /*
         * Market-specific AH filters.
         */
        if (filter
                == Filter.AUCTION_HOUSE) {

            if (!rarityFilters.isEmpty()) {
                count++;
            }

            if (!typeFilters.isEmpty()) {
                count++;
            }
        }

        /*
         * Common filters.
         */
        if (liquidityFilter
                != LiquidityFilter.ANY) {
            count++;
        }

        if (confidenceFilter
                != ConfidenceFilter.ANY) {
            count++;
        }

        if (roiFilter
                != RoiFilter.ANY) {
            count++;
        }

        if (profitFilter
                != ProfitFilter.ANY) {
            count++;
        }

        if (edgeFilter
                != EdgeFilter.ANY) {
            count++;
        }

        /*
         * Bazaar-only filter.
         */
        if (filter
                == Filter.BAZAAR
                && volumeFilter
                != VolumeFilter.ANY) {

            count++;
        }

        return count;
    }
    private int filterTrayLayoutOffset(
            int left,
            int right) {

        int width =
                filterTrayWidth(
                        left,
                        right);

        int fullHeight =
                filterTrayHeight(
                        width);

        int animatedHeight =
                animatedMenuHeight(
                        fullHeight,
                        filterMenuProgress);

        if (animatedHeight <= 1) {
            return 0;
        }

        return animatedHeight
                + FILTER_MENU_GAP;
    }

    private static int filterTrayWidth(
            int left,
            int right) {

        int available =
                Math.max(
                        1,
                        right - left);

        return Math.min(
                FILTER_TRAY_MAX_WIDTH,
                available);
    }

    private static int filterTrayLabelWidth(
            int trayWidth) {

        if (trayWidth < 430) {
            return 62;
        }

        return FILTER_TRAY_LABEL_WIDTH;
    }

    private int filterTrayHeight(
            int trayWidth) {

        int labelWidth =
                filterTrayLabelWidth(
                        trayWidth);

        int chipAreaWidth =
                Math.max(
                        1,
                        trayWidth
                                - FILTER_TRAY_PADDING * 2
                                - labelWidth);

        int height =
                PREMIUM_HEADER_HEIGHT
                        + 8;

        int groups =
                0;

        if (filter
                == Filter.AUCTION_HOUSE) {

            height =
                    appendFilterGroupHeight(
                            height,
                            groups++,
                            RARITY_OPTIONS,
                            chipAreaWidth);

            height =
                    appendFilterGroupHeight(
                            height,
                            groups++,
                            CATEGORY_OPTIONS,
                            chipAreaWidth);
        }

        height =
                appendFilterGroupHeight(
                        height,
                        groups++,
                        LIQUIDITY_OPTIONS,
                        chipAreaWidth);

        height =
                appendFilterGroupHeight(
                        height,
                        groups++,
                        CONFIDENCE_OPTIONS,
                        chipAreaWidth);

        height =
                appendFilterGroupHeight(
                        height,
                        groups++,
                        ROI_OPTIONS,
                        chipAreaWidth);

        height =
                appendFilterGroupHeight(
                        height,
                        groups++,
                        PROFIT_OPTIONS,
                        chipAreaWidth);

        height =
                appendFilterGroupHeight(
                        height,
                        groups++,
                        EDGE_OPTIONS,
                        chipAreaWidth);

        if (filter
                == Filter.BAZAAR) {

            height =
                    appendFilterGroupHeight(
                            height,
                            groups,
                            WEEKLY_VOLUME_OPTIONS,
                            chipAreaWidth);
        }

        return height;
    }

    private static int appendFilterGroupHeight(
            int currentHeight,
            int existingGroups,
            String[] options,
            int chipAreaWidth) {

        return currentHeight
                + (existingGroups > 0
                ? FILTER_TRAY_SECTION_GAP
                : 0)
                + filterChipGroupHeight(
                options,
                chipAreaWidth);
    }
    private static int filterChipGroupHeight(
            String[] options,
            int availableWidth) {

        if (options == null
                || options.length == 0) {

            return FILTER_CHIP_HEIGHT;
        }

        int safeWidth =
                Math.max(
                        1,
                        availableWidth);

        int lines =
                1;

        int used =
                0;

        for (String option
                : options) {

            int width =
                    filterChipWidth(
                            option,
                            safeWidth);

            int required =
                    used <= 0
                            ? width
                            : used
                            + FILTER_CHIP_GAP
                            + width;

            if (used > 0
                    && required > safeWidth) {

                lines++;
                used =
                        width;
            } else {

                used =
                        required;
            }
        }

        return lines
                * FILTER_CHIP_HEIGHT
                + Math.max(
                0,
                lines - 1)
                * FILTER_CHIP_GAP;
    }

    private static int filterChipWidth(
            String label,
            int availableWidth) {

        int safeAvailable =
                Math.max(
                        1,
                        availableWidth);

        Font font =
                net.minecraft.client.Minecraft
                        .getInstance()
                        .font;

        int textWidth =
                label == null
                        ? 0
                        : font.width(
                        label);

        /*
         * Exact font metrics + comfortable horizontal padding.
         */
        int desired =
                textWidth
                        + 22;

        return Math.min(
                safeAvailable,
                Math.max(
                        46,
                        desired));
    }
    private static int filterChipIndexAt(
            String[] options,
            int startX,
            int startY,
            int availableWidth,
            int mouseX,
            int mouseY) {

        if (options == null
                || options.length == 0) {

            return -1;
        }

        int safeWidth =
                Math.max(
                        1,
                        availableWidth);

        int rightEdge =
                startX
                        + safeWidth;

        int cursorX =
                startX;

        int cursorY =
                startY;

        for (int index = 0;
                index < options.length;
                index++) {

            int width =
                    filterChipWidth(
                            options[index],
                            safeWidth);

            if (cursorX > startX
                    && cursorX
                    + width
                    > rightEdge) {

                cursorX =
                        startX;

                cursorY +=
                        FILTER_CHIP_HEIGHT
                                + FILTER_CHIP_GAP;
            }

            if (RotClientUiDraw.inside(
                    mouseX,
                    mouseY,
                    cursorX,
                    cursorY,
                    width,
                    FILTER_CHIP_HEIGHT)) {

                return index;
            }

            cursorX +=
                    width
                            + FILTER_CHIP_GAP;
        }

        return -1;
    }

    private static int animatedMenuHeight(
            int fullHeight,
            double progress) {

        return (int) Math.round(
                Math.max(
                        0,
                        fullHeight)
                        * smoothStep(
                        progress));
    }

    private void advanceMenuAnimation() {

        long now =
                System.nanoTime();

        if (lastMenuAnimationNanos == 0L) {

            lastMenuAnimationNanos =
                    now;

            return;
        }

        double elapsed =
                Math.max(
                        0.0D,
                        (now
                                - lastMenuAnimationNanos)
                                / 1_000_000_000.0D);

        lastMenuAnimationNanos =
                now;

        double step =
                MENU_ANIMATION_SECONDS <= 0.0D
                        ? 1.0D
                        : elapsed
                        / MENU_ANIMATION_SECONDS;

        sortMenuProgress =
                approach(
                        sortMenuProgress,
                        sortMenuOpen
                                ? 1.0D
                                : 0.0D,
                        step);

        filterMenuProgress =
                approach(
                        filterMenuProgress,
                        filterMenuOpen
                                ? 1.0D
                                : 0.0D,
                        step);

        pinnedMenuProgress =
                approach(
                        pinnedMenuProgress,
                        pinnedMenuOpen
                                ? 1.0D
                                : 0.0D,
                        step);
    }
    private static double approach(
            double value,
            double target,
            double step) {

        double safeStep =
                Math.max(
                        0.0D,
                        step);

        if (target > value) {
            return Math.min(
                    target,
                    value + safeStep);
        }

        if (target < value) {
            return Math.max(
                    target,
                    value - safeStep);
        }

        return value;
    }
    private void sortOpportunities(
            List<MarketWatchOpportunity> opportunities) {

        if (opportunities == null
                || opportunities.size() < 2) {

            return;
        }

        opportunities.sort(
                (left, right) -> {

                    int primary =
                            switch (sortMode) {

                                case QUALITY ->
                                        Double.compare(
                                                right.score(),
                                                left.score());

                                case PROFIT ->
                                        Double.compare(
                                                right.expectedProfitCoins(),
                                                left.expectedProfitCoins());

                                case ROI ->
                                        Double.compare(
                                                right.roiPercent(),
                                                left.roiPercent());

                                case LIQUIDITY ->
                                        Integer.compare(
                                                liquidityRank(
                                                        right.liquidity()),
                                                liquidityRank(
                                                        left.liquidity()));

                                case EDGE ->
                                        Double.compare(
                                                right.marketEdgePercent(),
                                                left.marketEdgePercent());

                                case CAPITAL ->
                                        Double.compare(
                                                left.capitalCoins(),
                                                right.capitalCoins());
                            };

                    if (primary != 0) {
                        return primary;
                    }

                    /*
                     * Universal tie-breaker: better overall quality first.
                     */
                    int quality =
                            Double.compare(
                                    right.score(),
                                    left.score());

                    if (quality != 0) {
                        return quality;
                    }

                    int profit =
                            Double.compare(
                                    right.expectedProfitCoins(),
                                    left.expectedProfitCoins());

                    if (profit != 0) {
                        return profit;
                    }

                    return left.itemName()
                            .compareToIgnoreCase(
                                    right.itemName());
                });
    }

    private static int liquidityRank(
            MarketWatchOpportunity.Liquidity liquidity) {

        return switch (liquidity) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }

    private static String ahStackSuffix(
            MarketWatchOpportunity opportunity) {

        if (opportunity == null
                || opportunity.quantity() <= 1L) {

            return "";
        }

        return " x"
                + opportunity.quantity();
    }

    private void drawBudgetControl(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchOpportunitySnapshot snapshot,
            int visibleMatches,
            boolean updatePending,
            int left,
            int y,
            int right,
            int mouseX,
            int mouseY) {

        int width =
                Math.max(
                        1,
                        right - left);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                left,
                y,
                width,
                36);

        RotClientUiDraw.text(
                graphics,
                font,
                "TRADING BUDGET",
                left + 12,
                y + 5,
                RotClientTheme.TEXT,
                true);

        String compactSummary =
                !budgetError.isBlank()
                        ? budgetError
                        : updatePending
                        ? "Updating budget scan..."
                        : "AH "
                        + formatCount(
                        snapshot.scannedAuctions())
                        + "  |  BZ "
                        + formatCount(
                        snapshot.scannedBazaarProducts())
                        + "  |  "
                        + visibleMatches
                        + " / "
                        + snapshot.opportunities()
                        .size()
                        + " matches";

        RotClientUiDraw.helpText(
                graphics,
                font,
                compactSummary,
                left + 12,
                y + 20);

        int applyX =
                right
                        - 12
                        - APPLY_WIDTH;

        int inputX =
                applyX
                        - 8
                        - BUDGET_INPUT_WIDTH;

        int inputY =
                y + 6;

        RotClientUiDraw.drawElevatedCard(
                graphics,
                inputX,
                inputY,
                BUDGET_INPUT_WIDTH,
                BUDGET_INPUT_HEIGHT);

        if (budgetFocused) {
            graphics.fill(
                    inputX,
                    inputY,
                    inputX + 2,
                    inputY + BUDGET_INPUT_HEIGHT,
                    RotClientTheme.HUD_ACCENT);
        }

        String display =
                budgetText.isBlank()
                        ? "25M"
                        : budgetText;

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        display
                                + (budgetFocused
                                ? "_"
                                : ""),
                        BUDGET_INPUT_WIDTH - 12),
                inputX + 7,
                inputY + 8,
                budgetText.isBlank()
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.TEXT,
                false);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                applyX,
                inputY,
                APPLY_WIDTH,
                "APPLY",
                false,
                true);
    }

    boolean mouseClicked(
            int button,
            double mouseX,
            double mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (button
                != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && button
                != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {

            return false;
        }

        int mx =
                (int) Math.round(
                        mouseX);

        int my =
                (int) Math.round(
                        mouseY);
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && beginScrollbarDrag(
                mx,
                my)) {

            return true;
        }

        if (button
                == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && handlePinnedDealsClick(
                        mx,
                        my,
                        left,
                        right,
                        filterY(
                                top))) {

            return true;
        }

        if (button
                == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && handlePinButtonClick(
                        mx,
                        my)) {

            return true;
        }

        if (button
                == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {

            /*
             * Never let an animated Sort / Filters popup right-click
             * through to an opportunity underneath it.
             */
            if (sortMenuOpen
                    || filterMenuOpen
                    || sortMenuProgress > 0.001D
                    || filterMenuProgress > 0.001D) {

                return false;
            }

            int pinListTop =
                    listY(
                            top);

            if (!RotClientUiDraw.inside(
                    mx,
                    my,
                    left,
                    pinListTop,
                    Math.max(
                            1,
                            right - left),
                    Math.max(
                            1,
                            bottom - pinListTop))) {

                return false;
            }

            List<MarketWatchOpportunity> pinOpportunities =
                    filtered(
                            MarketWatchOpportunityService
                                    .current());

            MarketWatchOpportunity pinCandidate =
                    cardAt(
                            pinOpportunities,
                            left,
                            pinListTop,
                            right,
                            mx,
                            my);

            if (pinCandidate == null) {
                return false;
            }

            MarketWatchPinnedDealStore
                    .toggle(
                            pinCandidate);

            return true;
        }

        int budgetY =
                budgetY(top);

        int applyX =
                right
                        - 12
                        - APPLY_WIDTH;

        int inputX =
                applyX
                        - 8
                        - BUDGET_INPUT_WIDTH;

        int inputY =
                budgetY + 6;

        if (RotClientUiDraw.inside(
                mx,
                my,
                inputX,
                inputY,
                BUDGET_INPUT_WIDTH,
                BUDGET_INPUT_HEIGHT)) {

            budgetFocused = true;
            return true;
        }

        if (RotClientUiDraw.inside(
                mx,
                my,
                applyX,
                inputY,
                APPLY_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            applyBudget();
            return true;
        }

        budgetFocused = false;

        int filterY =
                filterY(top);

        if (handleSortClick(
                mx,
                my,
                right,
                filterY)) {

            return true;
        }

        if (handleFilterClick(
                mx,
                my,
                left,
                right,
                filterY,
                listY(top))) {

            return true;
        }

        if (RotClientUiDraw.inside(
                mx,
                my,
                left,
                filterY,
                FILTER_ALL_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            filter =
                    Filter.ALL;

            scrollPixels = 0;
            closeExpandedImmediately();

            return true;
        }

        int ahX =
                left
                        + FILTER_ALL_WIDTH
                        + FILTER_GAP;

        if (RotClientUiDraw.inside(
                mx,
                my,
                ahX,
                filterY,
                FILTER_AH_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            filter =
                    Filter.AUCTION_HOUSE;

            scrollPixels = 0;
            closeExpandedImmediately();

            return true;
        }

        int bazaarX =
                ahX
                        + FILTER_AH_WIDTH
                        + FILTER_GAP;

        if (RotClientUiDraw.inside(
                mx,
                my,
                bazaarX,
                filterY,
                FILTER_BAZAAR_WIDTH,
                RotClientUiDraw.BUTTON_HEIGHT)) {

            filter =
                    Filter.BAZAAR;

            scrollPixels = 0;
            closeExpandedImmediately();

            return true;
        }

        int listTop =
                listY(top);

        if (!RotClientUiDraw.inside(
                mx,
                my,
                left,
                listTop,
                Math.max(
                        1,
                        right - left),
                Math.max(
                        1,
                        bottom - listTop))) {

            return false;
        }

        List<MarketWatchOpportunity> opportunities =
                filtered(
                        MarketWatchOpportunityService
                                .current());

        MarketWatchOpportunity clicked =
                cardAt(
                        opportunities,
                        left,
                        listTop,
                        right,
                        mx,
                        my);

        if (clicked != null) {
            toggleExpanded(
                    clicked);

            return true;
        }

        return false;
    }

    boolean mouseScrolled(
            int mouseX,
            int mouseY,
            double verticalAmount,
            int left,
            int top,
            int right,
            int bottom) {

        if (verticalAmount == 0.0D) {
            return false;
        }

        if (sortMenuOpen
                || filterMenuOpen
                || pinnedMenuOpen) {

            return true;
        }

        int listTop =
                listY(top);

        if (!RotClientUiDraw.inside(
                mouseX,
                mouseY,
                left,
                listTop,
                Math.max(
                        1,
                        right - left),
                Math.max(
                        1,
                        bottom - listTop))) {

            return false;
        }

        List<MarketWatchOpportunity> opportunities =
                filtered(
                        MarketWatchOpportunityService
                                .current());

        int viewport =
                Math.max(
                        1,
                        bottom
                                - listTop
                                - 4);

        int content =
                gridContentHeight(
                        opportunities);

        int max =
                Math.max(
                        0,
                        content - viewport);

        if (max <= 0) {
            scrollPixels = 0;
            return false;
        }

        int direction =
                verticalAmount < 0.0D
                        ? 1
                        : -1;

        scrollPixels =
                Math.max(
                        0,
                        Math.min(
                                max,
                                scrollPixels
                                        + direction
                                        * SCROLL_STEP));

        return true;
    }

    boolean mouseDragged(
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (!scrollbarDragging) {
            return false;
        }

        dragScrollbar(
                mouseY);

        return true;
    }
    boolean mouseReleased() {

        boolean wasDragging =
                scrollbarDragging;

        scrollbarDragging =
                false;

        return wasDragging;
    }
    boolean captureChar(
            String incoming,
            boolean allowed) {

        if (!budgetFocused
                || !allowed
                || incoming == null
                || incoming.isEmpty()) {

            return false;
        }

        for (int i = 0;
                i < incoming.length();
                i++) {

            char c =
                    incoming.charAt(i);

            if ("0123456789.,kKmMbB _"
                    .indexOf(c) < 0) {

                continue;
            }

            if (budgetText.length() < 18) {
                budgetText +=
                        c;
            }
        }

        budgetError = "";
        return true;
    }

    boolean captureKey(
            int key) {

if (key == GLFW.GLFW_KEY_ESCAPE
                && (sortMenuOpen
                || filterMenuOpen)) {

            sortMenuOpen =
                    false;

            filterMenuOpen =
                    false;

            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE
                && !expandedOpportunityId.isBlank()) {

            expandedOpportunityId = "";
            lastAnimationNanos =
                    System.nanoTime();

            return true;
        }

        if (!budgetFocused) {
            return false;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            budgetFocused = false;
            return true;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!budgetText.isEmpty()) {
                budgetText =
                        budgetText.substring(
                                0,
                                budgetText.length() - 1);
            }

            budgetError = "";
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_KP_ENTER) {

            applyBudget();
            return true;
        }

        return false;
    }

    private void applyBudget() {

        double parsed =
                parseBudget(
                        budgetText);

        if (!(parsed > 0.0D)
                || !Double.isFinite(
                        parsed)) {

            budgetError =
                    "Use 500k, 25m, 1.5b, etc.";

            return;
        }

        if (parsed
                > MarketWatchOpportunityPreferences
                .MAX_BUDGET) {

            budgetError =
                    "Maximum: 100B";

            return;
        }

        double budget =
                MarketWatchOpportunityPreferences
                        .normalizeBudget(
                                parsed);

        MarketWatchOpportunityService
                .setBudgetCoins(
                        budget);

        MarketWatchOpportunityPreferences
                .saveBudgetCoins(
                        budget);

        budgetText =
                editableBudget(
                        budget);

        budgetFocused = false;
        budgetError = "";
        scrollPixels = 0;

        closeExpandedImmediately();
    }

    private List<MarketWatchOpportunity> filtered(
            MarketWatchOpportunitySnapshot snapshot) {

        if (snapshot == null
                || snapshot.opportunities()
                .isEmpty()) {

            return List.of();
        }

        double budget =
                MarketWatchOpportunityService
                        .budgetCoins();

        List<MarketWatchOpportunity> result =
                new ArrayList<>();

        for (MarketWatchOpportunity opportunity
                : snapshot.opportunities()) {

            if (opportunity == null
                    || opportunity.capitalCoins()
                    > budget + 0.5D) {

                continue;
            }

            if (filter
                    == Filter.AUCTION_HOUSE
                    && opportunity.market()
                    != MarketWatchOpportunity
                    .Market.AUCTION_HOUSE) {

                continue;
            }

            if (filter
                    == Filter.BAZAAR
                    && opportunity.market()
                    != MarketWatchOpportunity
                    .Market.BAZAAR) {

                continue;
            }

            if (!matchesAdvancedFilters(
                    opportunity)) {

                continue;
            }

            result.add(
                    opportunity);
        }

        sortOpportunities(
                result);

        return List.copyOf(
                result);
    }

    private boolean matchesAdvancedFilters(
            MarketWatchOpportunity opportunity) {

        /*
         * Common economic filters apply everywhere.
         */
        if (!matchesLiquidity(
                opportunity.liquidity())) {

            return false;
        }

        if (!matchesConfidence(
                opportunity.confidence())) {

            return false;
        }

        if (!matchesRoi(
                opportunity)) {

            return false;
        }

        if (!matchesProfit(
                opportunity)) {

            return false;
        }

        if (!matchesEdge(
                opportunity)) {

            return false;
        }

        /*
         * ALL intentionally ignores AH-only and Bazaar-only filters.
         * Hidden selections therefore cannot unexpectedly remove deals.
         */
        if (filter
                == Filter.ALL) {

            return true;
        }

        if (filter
                == Filter.AUCTION_HOUSE) {

            FilterMetadata metadata =
                    filterMetadata(
                            opportunity);

            return matchesRarity(
                    metadata.tier())
                    && matchesType(
                    metadata.category());
        }

        if (filter
                == Filter.BAZAAR) {

            return matchesVolume(
                    opportunity);
        }

        return true;
    }
    private FilterMetadata filterMetadata(
            MarketWatchOpportunity opportunity) {

        if (opportunity == null) {
            return new FilterMetadata(
                    "",
                    "");
        }

        if (opportunity.market()
                == MarketWatchOpportunity
                .Market.AUCTION_HOUSE) {

            return new FilterMetadata(
                    opportunity.category(),
                    opportunity.tier());
        }

        MarketWatchSkyBlockResourceIconService
                .ItemClassification classification =
                MarketWatchSkyBlockResourceIconService
                        .classification(
                                opportunity.itemId());

        return new FilterMetadata(
                classification.category(),
                classification.tier());
    }

    private boolean matchesRarity(
            String rawTier) {

        if (rarityFilters.isEmpty()) {
            return true;
        }

        String tier =
                normalizeFacet(
                        rawTier);

        for (RarityFilter selected
                : rarityFilters) {

            if (matchesRarityOption(
                    selected,
                    tier)) {

                return true;
            }
        }

        return false;
    }

    private static boolean matchesRarityOption(
            RarityFilter selected,
            String tier) {

        return switch (selected) {

            case ANY ->
                    true;

            case COMMON ->
                    tier.equals(
                            "COMMON");

            case UNCOMMON ->
                    tier.equals(
                            "UNCOMMON");

            case RARE ->
                    tier.equals(
                            "RARE");

            case EPIC ->
                    tier.equals(
                            "EPIC");

            case LEGENDARY ->
                    tier.equals(
                            "LEGENDARY");

            case MYTHIC_PLUS ->
                    tier.equals(
                            "MYTHIC")
                            || tier.equals(
                            "DIVINE")
                            || tier.equals(
                            "SUPREME");

            case SPECIAL ->
                    tier.equals(
                            "SPECIAL")
                            || tier.equals(
                            "VERY_SPECIAL");
        };
    }
    private boolean matchesType(
            String rawCategory) {

        if (typeFilters.isEmpty()) {
            return true;
        }

        String category =
                normalizeFacet(
                        rawCategory);

        for (TypeFilter selected
                : typeFilters) {

            if (matchesAhCategory(
                    selected,
                    category)) {

                return true;
            }
        }

        return false;
    }

    private static boolean matchesAhCategory(
            TypeFilter selected,
            String category) {

        return switch (selected) {

            case ANY ->
                    true;

            case WEAPON ->
                    category.equals(
                            "WEAPON");

            case ARMOR ->
                    category.equals(
                            "ARMOR");

            case ACCESSORY ->
                    category.equals(
                            "ACCESSORY")
                            || category.equals(
                            "ACCESSORIES");

            case CONSUMABLE ->
                    category.equals(
                            "CONSUMABLE")
                            || category.equals(
                            "CONSUMABLES");

            /*
             * Hypixel splits visual/non-gameplay items into several broad
             * AH buckets. Present them to the user as one useful category.
             */
            case COSMETIC ->
                    category.equals(
                            "COSMETIC")
                            || category.equals(
                            "RUNES")
                            || category.equals(
                            "DYES")
                            || category.equals(
                            "HELMET_SKINS")
                            || category.equals(
                            "OTHER_SKINS");

            /*
             * Mining/farming tools currently arrive here as MISC.
             */
            case MISC ->
                    category.equals(
                            "MISC");
        };
    }
    private boolean matchesLiquidity(
            MarketWatchOpportunity.Liquidity liquidity) {

        return switch (liquidityFilter) {
            case ANY ->
                    true;

            case MEDIUM_PLUS ->
                    liquidity
                            == MarketWatchOpportunity
                            .Liquidity.MEDIUM
                            || liquidity
                            == MarketWatchOpportunity
                            .Liquidity.HIGH;

            case HIGH_ONLY ->
                    liquidity
                            == MarketWatchOpportunity
                            .Liquidity.HIGH;
        };
    }

    private boolean matchesConfidence(
            MarketWatchOpportunity.Confidence confidence) {

        return switch (confidenceFilter) {
            case ANY ->
                    true;

            case MEDIUM_PLUS ->
                    confidence
                            == MarketWatchOpportunity
                            .Confidence.MEDIUM
                            || confidence
                            == MarketWatchOpportunity
                            .Confidence.HIGH;

            case HIGH_ONLY ->
                    confidence
                            == MarketWatchOpportunity
                            .Confidence.HIGH;
        };
    }

    private boolean matchesRoi(
            MarketWatchOpportunity opportunity) {

        return roiFilter
                == RoiFilter.ANY
                || opportunity.roiPercent()
                >= roiFilter.minimum();
    }

    private boolean matchesProfit(
            MarketWatchOpportunity opportunity) {

        return profitFilter
                == ProfitFilter.ANY
                || opportunity.expectedProfitCoins()
                >= profitFilter.minimum();
    }

    private boolean matchesEdge(
            MarketWatchOpportunity opportunity) {

        return edgeFilter
                == EdgeFilter.ANY
                || opportunity.marketEdgePercent()
                >= edgeFilter.minimum();
    }

    private boolean matchesVolume(
            MarketWatchOpportunity opportunity) {

        return volumeFilter
                == VolumeFilter.ANY
                || opportunity.weeklyVolume()
                >= volumeFilter.minimum();
    }

    private static String normalizeFacet(
            String value) {

        return value == null
                ? ""
                : value
                .trim()
                .toUpperCase(
                        Locale.ROOT)
                .replace(
                        '-',
                        '_')
                .replace(
                        ' ',
                        '_');
    }

    private void drawScrollbar(
            GuiGraphicsExtractor graphics,
            int right,
            int top,
            int bottom,
            int contentHeight,
            int viewportHeight,
            int maxScroll) {

        int trackHeight =
                Math.max(
                        1,
                        bottom - top);

        int thumbHeight =
                Math.max(
                        18,
                        (int) Math.round(
                                trackHeight
                                        * (viewportHeight
                                        / (double) contentHeight)));

        thumbHeight =
                Math.min(
                        trackHeight,
                        thumbHeight);

        int travel =
                Math.max(
                        0,
                        trackHeight - thumbHeight);

        int thumbTop =
                top;

        if (travel > 0
                && maxScroll > 0) {

            thumbTop +=
                    (int) Math.round(
                            travel
                                    * (scrollPixels
                                    / (double) maxScroll));
        }

        int x =
                right - 7;

        scrollbarX =
                x;

        scrollbarTrackTop =
                top;

        scrollbarTrackBottom =
                bottom;

        scrollbarThumbTop =
                thumbTop;

        scrollbarThumbHeight =
                thumbHeight;

        scrollbarMaxScroll =
                maxScroll;

        graphics.fill(
                x,
                top,
                x + 3,
                bottom,
                RotClientTheme.TEXT_MUTED);

        graphics.fill(
                x - 1,
                thumbTop,
                x + 4,
                thumbTop + thumbHeight,
                RotClientTheme.HUD_ACCENT);
    }

    private boolean beginScrollbarDrag(
            int mouseX,
            int mouseY) {

        if (scrollbarMaxScroll <= 0) {
            return false;
        }

        if (scrollbarThumbHeight <= 0) {
            return false;
        }

        /*
         * Wider input area than the visible 5px thumb.
         */
        if (mouseX < scrollbarX - 7) {
            return false;
        }

        if (mouseX > scrollbarX + 9) {
            return false;
        }

        if (mouseY < scrollbarTrackTop) {
            return false;
        }

        if (mouseY > scrollbarTrackBottom) {
            return false;
        }

        boolean onThumb =
                mouseY >= scrollbarThumbTop
                        && mouseY
                        <= scrollbarThumbTop
                        + scrollbarThumbHeight;

        scrollbarDragging =
                true;

        if (onThumb) {

            scrollbarGrabOffset =
                    mouseY
                            - scrollbarThumbTop;

        } else {

            /*
             * Clicking the empty track jumps toward the mouse and
             * immediately starts dragging from the thumb centre.
             */
            scrollbarGrabOffset =
                    scrollbarThumbHeight / 2;

            dragScrollbar(
                    mouseY);
        }

        return true;
    }

    private void dragScrollbar(
            int mouseY) {

        if (!scrollbarDragging) {
            return;
        }

        if (scrollbarMaxScroll <= 0) {
            return;
        }

        int trackHeight =
                scrollbarTrackBottom
                        - scrollbarTrackTop;

        int travel =
                Math.max(
                        0,
                        trackHeight
                                - scrollbarThumbHeight);

        if (travel <= 0) {
            scrollPixels = 0;
            return;
        }

        int target =
                mouseY
                        - scrollbarGrabOffset;

        target =
                Math.max(
                        scrollbarTrackTop,
                        Math.min(
                                target,
                                scrollbarTrackTop
                                        + travel));

        double progress =
                (target
                        - scrollbarTrackTop)
                        / (double) travel;

        scrollPixels =
                (int) Math.round(
                        progress
                                * scrollbarMaxScroll);

        scrollPixels =
                Math.max(
                        0,
                        Math.min(
                                scrollPixels,
                                scrollbarMaxScroll));
    }

    private static int budgetY(
            int top) {

        return top + 28;
    }

    private static int metricsY(
            int top) {

        return budgetY(top)
                + 52;
    }

    private static int filterY(
            int top) {

        return budgetY(top)
                + 44;
    }

    private static int listY(
            int top) {

        return filterY(top)
                + RotClientUiDraw.BUTTON_HEIGHT
                + 8;
    }

    private static void drawEmptyState(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int y,
            int width,
            String title,
            String help) {

        RotClientUiDraw.drawElevatedCard(
                graphics,
                left,
                y,
                width,
                58);

        RotClientUiDraw.text(
                graphics,
                font,
                title,
                left + 14,
                y + 12,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                help,
                left + 14,
                y + 31);
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
                Math.max(
                        60,
                        width),
                title,
                value,
                hint,
                healthy
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_MUTED);
    }

    private static int scoreColor(
            double score) {

        if (score >= 85.0D) {
            return RotClientTheme.SUCCESS;
        }

        if (score >= 68.0D) {
            return RotClientTheme.HUD_ACCENT;
        }

        return RotClientTheme.TEXT_MUTED;
    }

    private static String qualityLabel(
            double score) {

        if (score >= 90.0D) {
            return "EXCELLENT";
        }

        if (score >= 80.0D) {
            return "STRONG";
        }

        if (score >= 68.0D) {
            return "GOOD";
        }

        if (score >= 55.0D) {
            return "FAIR";
        }

        return "SPECULATIVE";
    }

    private static int confidenceColor(
            MarketWatchOpportunity.Confidence confidence) {

        return switch (confidence) {
            case HIGH ->
                    RotClientTheme.SUCCESS;

            case MEDIUM ->
                    RotClientTheme.HUD_ACCENT;

            case LOW ->
                    RotClientTheme.TEXT_MUTED;
        };
    }

    private static String confidenceLabel(
            MarketWatchOpportunity.Confidence confidence) {

        return switch (confidence) {
            case HIGH -> "HIGH";
            case MEDIUM -> "MEDIUM";
            case LOW -> "LOW";
        };
    }

    private static String liquidityLabel(
            MarketWatchOpportunity.Liquidity liquidity) {

        return switch (liquidity) {
            case HIGH -> "HIGH";
            case MEDIUM -> "MEDIUM";
            case LOW -> "LOW";
        };
    }

    private static String sellerName(
            MarketWatchOpportunity opportunity) {

        String seller =
                MarketWatchSellerNameService
                        .displayName(
                                opportunity.sellerUuid());

        return seller == null
                || seller.isBlank()
                ? "?"
                : seller;
    }

    private static String fit(
            Font font,
            String text,
            int maxWidth) {

        if (text == null
                || text.isEmpty()
                || maxWidth <= 0) {

            return "";
        }

        if (font.width(
                text)
                <= maxWidth) {

            return text;
        }

        String suffix =
                "...";

        int suffixWidth =
                font.width(
                        suffix);

        if (suffixWidth
                >= maxWidth) {

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

    private static double parseBudget(
            String raw) {

        if (raw == null
                || raw.isBlank()) {

            return -1.0D;
        }

        String value =
                raw.trim()
                        .toLowerCase(
                                Locale.ROOT)
                        .replace("_", "")
                        .replace(" ", "");

        /*
         * Support both 1.5m and 1,5m.
         */
        if (value.contains(",")
                && !value.contains(".")) {

            value =
                    value.replace(
                            ',',
                            '.');

        } else {

            value =
                    value.replace(
                            ",",
                            "");
        }

        double multiplier =
                1.0D;

        if (value.endsWith("k")) {

            multiplier =
                    1_000.0D;

            value =
                    value.substring(
                            0,
                            value.length() - 1);

        } else if (value.endsWith("m")) {

            multiplier =
                    1_000_000.0D;

            value =
                    value.substring(
                            0,
                            value.length() - 1);

        } else if (value.endsWith("b")) {

            multiplier =
                    1_000_000_000.0D;

            value =
                    value.substring(
                            0,
                            value.length() - 1);
        }

        try {

            double number =
                    Double.parseDouble(
                            value);

            double result =
                    number
                            * multiplier;

            return Double.isFinite(
                    result)
                    && result > 0.0D
                    ? result
                    : -1.0D;

        } catch (NumberFormatException ignored) {

            return -1.0D;
        }
    }

    private static String editableBudget(
            double coins) {

        if (!Double.isFinite(
                coins)
                || coins <= 0.0D) {

            return "25M";
        }

        if (coins
                >= 1_000_000_000.0D) {

            return editableNumber(
                    coins
                            / 1_000_000_000.0D)
                    + "B";
        }

        if (coins
                >= 1_000_000.0D) {

            return editableNumber(
                    coins
                            / 1_000_000.0D)
                    + "M";
        }

        if (coins
                >= 1_000.0D) {

            return editableNumber(
                    coins
                            / 1_000.0D)
                    + "K";
        }

        return Long.toString(
                Math.round(
                        coins));
    }

    private static String editableNumber(
            double value) {

        String text =
                String.format(
                        Locale.ROOT,
                        "%.2f",
                        value);

        while (text.contains(".")
                && text.endsWith("0")) {

            text =
                    text.substring(
                            0,
                            text.length() - 1);
        }

        if (text.endsWith(".")) {

            text =
                    text.substring(
                            0,
                            text.length() - 1);
        }

        return text;
    }

    private static String formatSignedCoins(
            double coins) {

        if (!Double.isFinite(
                coins)) {

            return "--";
        }

        return (coins >= 0.0D
                ? "+"
                : "-")
                + formatCoins(
                Math.abs(
                        coins));
    }

    private static String formatCoins(
            double coins) {

        if (!Double.isFinite(
                coins)) {

            return "--";
        }

        double value =
                Math.abs(
                        coins);

        if (value
                >= 1_000_000_000.0D) {

            return String.format(
                    Locale.ROOT,
                    "%.2fB",
                    value
                            / 1_000_000_000.0D);
        }

        if (value
                >= 1_000_000.0D) {

            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    value
                            / 1_000_000.0D);
        }

        if (value
                >= 1_000.0D) {

            return String.format(
                    Locale.ROOT,
                    "%.1fk",
                    value
                            / 1_000.0D);
        }

        return String.format(
                Locale.ROOT,
                "%.0f",
                value);
    }

    private static String formatUnitPrice(
            double price) {

        if (!Double.isFinite(
                price)
                || price <= 0.0D) {

            return "--";
        }

        if (price
                >= 1_000_000.0D) {

            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    price
                            / 1_000_000.0D);
        }

        if (price
                >= 1_000.0D) {

            return String.format(
                    Locale.ROOT,
                    "%.2fk",
                    price
                            / 1_000.0D);
        }

        if (price
                >= 100.0D) {

            return String.format(
                    Locale.ROOT,
                    "%.1f",
                    price);
        }

        if (price
                >= 1.0D) {

            return String.format(
                    Locale.ROOT,
                    "%.2f",
                    price);
        }

        return String.format(
                Locale.ROOT,
                "%.3f",
                price);
    }

    private static String formatCount(
            int count) {

        return formatLongCount(
                Math.max(
                        0,
                        count));
    }

    private static String formatLongCount(
            long count) {

        long value =
                Math.max(
                        0L,
                        count);

        if (value
                >= 1_000_000_000L) {

            return String.format(
                    Locale.ROOT,
                    "%.1fB",
                    value
                            / 1_000_000_000.0D);
        }

        if (value
                >= 1_000_000L) {

            return String.format(
                    Locale.ROOT,
                    "%.1fM",
                    value
                            / 1_000_000.0D);
        }

        if (value
                >= 1_000L) {

            return String.format(
                    Locale.ROOT,
                    "%.1fk",
                    value
                            / 1_000.0D);
        }

        return Long.toString(
                value);
    }

    private record FilterMetadata(
            String category,
            String tier) {

        private FilterMetadata {
            category =
                    category == null
                            ? ""
                            : category;

            tier =
                    tier == null
                            ? ""
                            : tier;
        }
    }

    private record GridLayout(
            int leftX,
            int rightX,
            int cardWidth) {
    }
}