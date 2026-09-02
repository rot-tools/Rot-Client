package fi.rotclient;

final class HudLayoutMath {
    private static final int CARD_GAP = 6;
    private static final int GEMSTONE_BASE_TOP_HEIGHT = 101;
    private static final int GEMSTONE_LEDGER_HEIGHT = 150;

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
    static final int AREA_ROW_HEIGHT = 14;

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
        int height = showHudTitle || showHudStatus || showActiveTool ? 37 : 28;
        if (showArea) height += AREA_ROW_HEIGHT;
        if (showBlocks) height += 28;
        if (showRateGraph) height += 39;
        if (showMaterialPerHour || showDropAndFortune) height += 29;
        if (showHudAutoPause) {
            height += 34;
        } else if (showHudVersion || showSessionTime) {
            height += 16;
        } else {
            height += 4;
        }
        return height;
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
        int height;
        if (itemRows) {
            // Title + optional TARGET heading + column headers.
            height = showTargetHeading ? 52 : 38;
            if (showEnchantedMaterial) {
                height += 18 * safeMaterialCount;
            }
            if (showRawMaterial) {
                height += 18 * safeMaterialCount;
            }
        } else {
            height = 26;
        }
        if (showOtherSection) {
            height += 14 + 18;
        }
        boolean anyValueLine =
                showTargetValue || showOtherValue || showTotalMinedValue;
        if (showOtherSection || anyValueLine) {
            height += 8;
        } else if (itemRows) {
            height += 8;
        }
        if (showTargetValue) {
            height += 16;
        }
        if (showOtherValue) {
            height += 16;
        }
        if (showTotalMinedValue) {
            height += 16;
        }
        if (showSessionProfit) height += 16;
        if (showCoinsPerHour) height += 15;
        if (showUnsoldValue) height += 15;
        if (showBazaarPrices) height += 13 * safeMaterialCount;
        return Math.max(46, height + 4);
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
        int height = GEMSTONE_BASE_TOP_HEIGHT;
        if (showArea) height += AREA_ROW_HEIGHT;
        if (showBlocks) height += 28;
        if (showRateGraph) height += 39;
        return height;
    }

    static int gemstoneLedgerCardHeight() {
        return GEMSTONE_LEDGER_HEIGHT;
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
                + CARD_GAP
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
