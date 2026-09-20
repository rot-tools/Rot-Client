package fi.rotclient;

final class HudLayoutMath {
    /** Compact single-card Mining HUD: every row height lives here. */
    static final int PAD_TOP = 4;
    static final int PAD_BOTTOM = 4;
    static final int HEADER_ROW = 13;
    static final int BLOCKS_ROW = 11;
    /** 16px sparkline plus 3px of air below it. */
    static final int GRAPH_HEIGHT = 16;
    static final int GRAPH_SECTION = GRAPH_HEIGHT + 3;
    static final int METRIC_ROW = 11;
    static final int FOOTER_ROW = 11;
    /** 2px auto-pause bar plus 3px of air above it. */
    static final int PAUSE_BAR_HEIGHT = 2;
    static final int PAUSE_BAR_SECTION = PAUSE_BAR_HEIGHT + 3;
    /** Divider between the tracker section and the value section. */
    static final int SECTION_GAP = 4;
    static final int HEADING_ROW = 10;
    static final int ITEM_ROW = 12;
    static final int VALUE_ROW = 11;
    static final int BAZAAR_ROW = 10;
    /** 1px rule plus air between list rows and value lines. */
    static final int SEPARATOR = 4;
    /** One ledger row per {@code GemstoneTier}. */
    static final int GEMSTONE_TIER_ROWS = 5;
    /** All Gemstones shows this many gemstone rows, then "+N more". */
    static final int MAX_ALL_GEMSTONE_ROWS = 6;

    private HudLayoutMath() {
    }

    static int topCardHeight(boolean showBlocks,
                             boolean showRateGraph,
                             boolean showMaterialPerHour,
                             boolean showDropAndFortune) {
        return topCardHeight(
                showBlocks,
                showRateGraph,
                showMaterialPerHour,
                showDropAndFortune,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    /**
     * Top-card height with HUD chrome visibility. Defaults for the 4-arg
     * overload keep prior layout numbers when all chrome flags are on.
     */
    static int topCardHeight(boolean showBlocks,
                             boolean showRateGraph,
                             boolean showMaterialPerHour,
                             boolean showDropAndFortune,
                             boolean showHudTitle,
                             boolean showHudAutoPause,
                             boolean showHudVersion,
                             boolean showSessionTime,
                             boolean showHudStatus) {
        return topCardHeight(
                showBlocks,
                showRateGraph,
                showMaterialPerHour,
                showDropAndFortune,
                showHudTitle,
                showHudAutoPause,
                showHudVersion,
                showSessionTime,
                showHudStatus,
                true);
    }

    static int topCardHeight(boolean showBlocks,
                             boolean showRateGraph,
                             boolean showMaterialPerHour,
                             boolean showDropAndFortune,
                             boolean showHudTitle,
                             boolean showHudAutoPause,
                             boolean showHudVersion,
                             boolean showSessionTime,
                             boolean showHudStatus,
                             boolean showActiveTool) {
        return topCardHeight(
                showBlocks,
                showRateGraph,
                showMaterialPerHour,
                showDropAndFortune,
                showHudTitle,
                showHudAutoPause,
                showHudVersion,
                showSessionTime,
                showHudStatus,
                showActiveTool,
                false);
    }

    /** Compact AREA row height when location visibility is enabled. */
    static final int AREA_ROW_HEIGHT = 11;

    static int topCardHeight(boolean showBlocks,
                             boolean showRateGraph,
                             boolean showMaterialPerHour,
                             boolean showDropAndFortune,
                             boolean showHudTitle,
                             boolean showHudAutoPause,
                             boolean showHudVersion,
                             boolean showSessionTime,
                             boolean showHudStatus,
                             boolean showActiveTool,
                             boolean showArea) {
        int height = PAD_TOP;
        if (showHudTitle || showHudStatus || showActiveTool) {
            height += HEADER_ROW;
        }
        if (showArea) height += AREA_ROW_HEIGHT;
        if (showBlocks) height += BLOCKS_ROW;
        if (showRateGraph) height += GRAPH_SECTION;
        if (showMaterialPerHour || showDropAndFortune) height += METRIC_ROW;
        if (showHudVersion || showSessionTime) height += FOOTER_ROW;
        if (showHudAutoPause) height += PAUSE_BAR_SECTION;
        return height + PAD_BOTTOM;
    }

    static boolean hasProfitCard(boolean showRawMaterial,
                                 boolean showEnchantedMaterial,
                                 boolean showSessionProfit,
                                 boolean showCoinsPerHour,
                                 boolean showUnsoldValue,
                                 boolean showBazaarPrices) {
        return showRawMaterial
                || showEnchantedMaterial
                || showSessionProfit
                || showCoinsPerHour
                || showUnsoldValue
                || showBazaarPrices;
    }

    static boolean hasProfitCard(boolean showRawMaterial,
                                 boolean showEnchantedMaterial,
                                 boolean showSessionProfit,
                                 boolean showCoinsPerHour,
                                 boolean showUnsoldValue,
                                 boolean showBazaarPrices,
                                 boolean showValuePanel,
                                 boolean showOtherSection,
                                 boolean showTargetValue,
                                 boolean showOtherValue,
                                 boolean showTotalMinedValue) {
        if (!showValuePanel) {
            return false;
        }
        return hasProfitCard(
                showRawMaterial,
                showEnchantedMaterial,
                showSessionProfit,
                showCoinsPerHour,
                showUnsoldValue,
                showBazaarPrices)
                || showOtherSection
                || showTargetValue
                || showOtherValue
                || showTotalMinedValue;
    }

    static int profitCardHeight(boolean showRawMaterial,
                                boolean showEnchantedMaterial,
                                boolean showSessionProfit,
                                boolean showCoinsPerHour,
                                boolean showUnsoldValue,
                                boolean showBazaarPrices) {
        return profitCardHeight(
                showRawMaterial,
                showEnchantedMaterial,
                showSessionProfit,
                showCoinsPerHour,
                showUnsoldValue,
                showBazaarPrices,
                1);
    }

    static int profitCardHeight(boolean showRawMaterial,
                                boolean showEnchantedMaterial,
                                boolean showSessionProfit,
                                boolean showCoinsPerHour,
                                boolean showUnsoldValue,
                                boolean showBazaarPrices,
                                int materialCount) {
        return profitCardHeight(
                showRawMaterial,
                showEnchantedMaterial,
                showSessionProfit,
                showCoinsPerHour,
                showUnsoldValue,
                showBazaarPrices,
                materialCount,
                true);
    }

    /**
     * @param includeOtherSummary compact OTHER block plus Target/Other/Total
     *                            value lines (legacy all-or-nothing flag).
     */
    static int profitCardHeight(boolean showRawMaterial,
                                boolean showEnchantedMaterial,
                                boolean showSessionProfit,
                                boolean showCoinsPerHour,
                                boolean showUnsoldValue,
                                boolean showBazaarPrices,
                                int materialCount,
                                boolean includeOtherSummary) {
        return profitCardHeight(
                showRawMaterial,
                showEnchantedMaterial,
                showSessionProfit,
                showCoinsPerHour,
                showUnsoldValue,
                showBazaarPrices,
                materialCount,
                includeOtherSummary,
                includeOtherSummary,
                includeOtherSummary,
                includeOtherSummary,
                true);
    }

    /**
     * Granular OTHER / value-line visibility. When {@code showOtherSection} is
     * false the OTHER block is omitted and height shrinks accordingly.
     */
    static int profitCardHeight(boolean showRawMaterial,
                                boolean showEnchantedMaterial,
                                boolean showSessionProfit,
                                boolean showCoinsPerHour,
                                boolean showUnsoldValue,
                                boolean showBazaarPrices,
                                int materialCount,
                                boolean showOtherSection,
                                boolean showTargetValue,
                                boolean showOtherValue,
                                boolean showTotalMinedValue) {
        return profitCardHeight(
                showRawMaterial,
                showEnchantedMaterial,
                showSessionProfit,
                showCoinsPerHour,
                showUnsoldValue,
                showBazaarPrices,
                materialCount,
                showOtherSection,
                showTargetValue,
                showOtherValue,
                showTotalMinedValue,
                true);
    }

    static int profitCardHeight(boolean showRawMaterial,
                                boolean showEnchantedMaterial,
                                boolean showSessionProfit,
                                boolean showCoinsPerHour,
                                boolean showUnsoldValue,
                                boolean showBazaarPrices,
                                int materialCount,
                                boolean showOtherSection,
                                boolean showTargetValue,
                                boolean showOtherValue,
                                boolean showTotalMinedValue,
                                boolean showTargetHeading) {
        int safeMaterialCount = Math.max(1, materialCount);
        boolean itemRows = showRawMaterial || showEnchantedMaterial;
        int height = 0;
        if (itemRows) {
            if (showTargetHeading) height += HEADING_ROW;
            if (showEnchantedMaterial) height += ITEM_ROW * safeMaterialCount;
            if (showRawMaterial) height += ITEM_ROW * safeMaterialCount;
        }
        if (showOtherSection) height += ITEM_ROW;
        int valueLines = (showTargetValue ? 1 : 0)
                + (showOtherValue ? 1 : 0)
                + (showTotalMinedValue ? 1 : 0)
                + (showSessionProfit ? 1 : 0)
                + (showCoinsPerHour ? 1 : 0)
                + (showUnsoldValue ? 1 : 0);
        if ((itemRows || showOtherSection) && valueLines > 0) {
            height += SEPARATOR;
        }
        height += VALUE_ROW * valueLines;
        if (showBazaarPrices) height += BAZAAR_ROW * safeMaterialCount;
        return height + PAD_BOTTOM;
    }

    static int gemstoneTopCardHeight(
            boolean showBlocks,
            boolean showRateGraph) {
        return gemstoneTopCardHeight(showBlocks, showRateGraph, false);
    }

    static int gemstoneTopCardHeight(
            boolean showBlocks,
            boolean showRateGraph,
            boolean showArea) {
        return gemstoneTopCardHeight(
                showBlocks, showRateGraph, showArea,
                true, true, true, true, true, true);
    }

    /** Gemstone header/footer chrome follows the same toggles as ores. */
    static int gemstoneTopCardHeight(
            boolean showBlocks,
            boolean showRateGraph,
            boolean showArea,
            boolean showHudTitle,
            boolean showHudAutoPause,
            boolean showHudVersion,
            boolean showSessionTime,
            boolean showHudStatus,
            boolean showActiveTool) {
        return topCardHeight(
                showBlocks,
                showRateGraph,
                true,
                false,
                showHudTitle,
                showHudAutoPause,
                showHudVersion,
                showSessionTime,
                showHudStatus,
                showActiveTool,
                showArea);
    }

    static int gemstoneLedgerCardHeight() {
        // Column heading, one row per tier, rule, total items, total rough EQ.
        return HEADING_ROW
                + GEMSTONE_TIER_ROWS * VALUE_ROW
                + SEPARATOR
                + 2 * VALUE_ROW
                + PAD_BOTTOM;
    }

    /**
     * All Gemstones ledger: a tier heading, one row per gemstone that has
     * items (best first, capped), an optional "+N more" row, then totals. With
     * nothing gained yet a single placeholder row keeps the layout stable.
     */
    static int gemstoneAllLedgerHeight(int gemstonesWithItems) {
        int withItems = Math.max(0, gemstonesWithItems);
        int shown = Math.min(withItems, MAX_ALL_GEMSTONE_ROWS);
        int overflow = withItems > MAX_ALL_GEMSTONE_ROWS ? VALUE_ROW : 0;
        return HEADING_ROW
                + Math.max(1, shown) * VALUE_ROW
                + overflow
                + SEPARATOR
                + 2 * VALUE_ROW
                + PAD_BOTTOM;
    }

    static int gemstoneAllHudHeight(
            boolean showBlocks,
            boolean showRateGraph,
            boolean showArea,
            boolean showHudTitle,
            boolean showHudAutoPause,
            boolean showHudVersion,
            boolean showSessionTime,
            boolean showHudStatus,
            boolean showActiveTool,
            int gemstonesWithItems) {
        return gemstoneTopCardHeight(
                showBlocks,
                showRateGraph,
                showArea,
                showHudTitle,
                showHudAutoPause,
                showHudVersion,
                showSessionTime,
                showHudStatus,
                showActiveTool)
                + SECTION_GAP
                + gemstoneAllLedgerHeight(gemstonesWithItems);
    }

    static int gemstoneHudHeight(
            boolean showBlocks,
            boolean showRateGraph) {
        return gemstoneHudHeight(showBlocks, showRateGraph, false);
    }

    static int gemstoneHudHeight(
            boolean showBlocks,
            boolean showRateGraph,
            boolean showArea) {
        return gemstoneTopCardHeight(
                showBlocks,
                showRateGraph,
                showArea)
                + SECTION_GAP
                + gemstoneLedgerCardHeight();
    }

    static int gemstoneHudHeight(
            boolean showBlocks,
            boolean showRateGraph,
            boolean showArea,
            boolean showHudTitle,
            boolean showHudAutoPause,
            boolean showHudVersion,
            boolean showSessionTime,
            boolean showHudStatus,
            boolean showActiveTool) {
        return gemstoneTopCardHeight(
                showBlocks,
                showRateGraph,
                showArea,
                showHudTitle,
                showHudAutoPause,
                showHudVersion,
                showSessionTime,
                showHudStatus,
                showActiveTool)
                + SECTION_GAP
                + gemstoneLedgerCardHeight();
    }

    static double clampOrigin(
            double requested,
            int screenSize,
            int logicalContentSize,
            float scale) {
        if (!Double.isFinite(requested)) {
            throw new IllegalArgumentException(
                    "Requested HUD position must be finite");
        }
        if (screenSize < 0 || logicalContentSize < 0) {
            throw new IllegalArgumentException(
                    "HUD dimensions cannot be negative");
        }
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            throw new IllegalArgumentException(
                    "HUD scale must be positive and finite");
        }
        double maximum = Math.max(
                0.0,
                screenSize - logicalContentSize * (double) scale);
        return Math.max(
                0.0,
                Math.min(requested, maximum));
    }
}
