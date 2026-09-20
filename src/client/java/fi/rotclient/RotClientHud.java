package fi.rotclient;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Mining HUD: one compact, translucent card. Row heights come from
 * {@link HudLayoutMath} so drawing and drag/clamp bounds cannot drift apart.
 */
final class RotClientHud {
    static final int WIDTH = 172;
    private static final int PAD = 6;
    private static final int INNER = WIDTH - 2 * PAD;
    private static final int NAME_X = PAD + 14;
    /** Right edge of the quantity column in item / ledger rows. */
    private static final int QTY_RIGHT = WIDTH - PAD - 34;
    /**
     * Quantity column of the single-gemstone ledger. It sits well left of
     * the "ROUGH EQ" heading so the two headings cannot overlap.
     */
    private static final int LEDGER_QTY_RIGHT = WIDTH - PAD - 62;
    /** Air between columns in the All Gemstones matrix. */
    private static final int MATRIX_GAP = 6;
    /** Used instead when the columns would leave too little room for names. */
    private static final int MATRIX_TIGHT_GAP = 3;
    private static final int MATRIX_MIN_NAME = 24;
    private static final float ICON_SCALE = 0.625F;
    private static final int MAX_RATE_SAMPLES = 64;
    /** No per-element override exists, so this resolves to the HUD Layout style. */
    private static final String HUD_STYLE_ID = "mining_tracker";

    private static final DecimalFormat NUMBER = new DecimalFormat("#,##0.#");

    private final TrackerConfig config;
    private final Deque<Double> rateHistory = new ArrayDeque<>();
    private boolean editorOpen;
    private boolean dragging;
    private double dragOffsetX;
    private double dragOffsetY;
    private long lastRateSampleMillis;
    private long lastRateBlocks;
    private double smoothedBlocksPerHour;
    private double graphScaleMaximum = 1;

    /** One inline "value label" figure in the metric row. */
    private record Metric(String value, String label) {
    }

    RotClientHud(TrackerConfig config) {
        this.config = config;
    }

    void render(GuiGraphicsExtractor graphics, DeltaTracker ignored) {
        if (!config.enabled) return;

        if (config.selectedSelection().isGemstone()) {
            renderGemstone(graphics);
            return;
        }

        long now = System.currentTimeMillis();
        TrackingTarget target = config.selectedTarget();
        TrackedMaterial primary = target.primaryMaterial();
        long sessionBlocks = RotClientClient.selectedSessionBlocks();
        long sessionBaseDrops =
                RotClientClient.selectedSessionBaseDrops();
        boolean active = RotClientClient.isActive(now);
        sampleMiningRate(sessionBlocks, now, active);

        long activeMillis = RotClientClient.currentSessionActiveMillis(now);
        boolean fortuneKnown = !config.fortuneAuto
                || config.fortuneLastDetectedEpochMillis > 0;
        double effectiveFortune =
                RotClientClient.effectiveFortune(primary);
        double basePerBlock = sessionBlocks > 0 && sessionBaseDrops > 0
                ? (double) sessionBaseDrops / sessionBlocks
                : primary.baseDrop();
        double estimatedPerBlock = basePerBlock
                * (1.0 + effectiveFortune / 100.0);
        if (!fortuneKnown) estimatedPerBlock = basePerBlock;

        double displayedResource = 0;
        double sessionValue = 0;
        double unsoldValue = 0;
        for (TrackedMaterial material : target.materials()) {
            double materialResource = displayedResource(
                    material, effectiveFortune, fortuneKnown);
            displayedResource += materialResource;
            MaterialTrackerState materialState = config.state(material);
            if (materialState.sessionActualRawEquivalent > 0) {
                sessionValue +=
                        RotClientClient.currentSessionProfit(material);
                unsoldValue +=
                        RotClientClient.currentSessionUnsoldProfit(
                                material);
            } else {
                double fallbackValue = materialResource
                        * materialState.lastRawPrice
                        * Math.max(
                                0,
                                1.0 - config.bazaarTaxPercent / 100.0);
                sessionValue += fallbackValue;
                unsoldValue += fallbackValue;
            }
        }
        double averagePerBlock = sessionBlocks <= 0
                ? estimatedPerBlock
                : displayedResource / sessionBlocks;
        double observedFortune = sessionBlocks > 0 && sessionBaseDrops > 0
                ? Math.max(
                        0,
                        (displayedResource / sessionBaseDrops - 1.0)
                                * 100.0)
                : effectiveFortune;
        double liveResourcePerHour = smoothedBlocksPerHour * averagePerBlock;
        double coinsPerHour = activeMillis <= 0
                ? 0
                : sessionValue * 3_600_000.0 / activeMillis;

        List<Metric> metrics = new ArrayList<>(3);
        if (config.showMaterialPerHour) {
            metrics.add(new Metric(compact(liveResourcePerHour), "/h"));
        }
        if (config.showDropAndFortune) {
            metrics.add(new Metric(NUMBER.format(averagePerBlock), "avg"));
            metrics.add(new Metric("~" + compact(observedFortune), "fort"));
        }

        int topHeight = topCardHeight();
        int fullHeight = currentHeight();

        graphics.pose().pushMatrix();
        graphics.pose().translate(config.x, config.y);
        graphics.pose().scale(config.scale, config.scale);

        drawCard(graphics, 0, 0, WIDTH, fullHeight);
        drawTopSection(
                graphics,
                target.displayName(),
                sessionBlocks,
                now,
                activeMillis,
                active,
                RotClientClient.lastSelectedBreakEpochMillis(),
                metrics,
                RotClientVersionLabel.brandLabel());
        if (hasProfitCard()) {
            drawRule(graphics, topHeight);
            drawValueSection(
                    graphics, target, now,
                    topHeight + HudLayoutMath.SECTION_GAP,
                    sessionValue, unsoldValue, coinsPerHour);
        }

        graphics.pose().popMatrix();
    }

    private void renderGemstone(GuiGraphicsExtractor graphics) {
        long now = System.currentTimeMillis();
        TrackerSelection selection = config.selectedSelection();
        GemstoneTrackerState state = config.gemstoneTimeline(selection);
        GemstoneLedger ledger = config.gemstoneSessionLedger(selection);
        long sessionBlocks = state.sessionBlocks;
        boolean active = state.isActive(
                now, RotClientClient.PAUSE_AFTER_MILLIS);
        long activeMillis = state.currentSessionActiveMillis(
                now, RotClientClient.PAUSE_AFTER_MILLIS);
        sampleMiningRate(sessionBlocks, now, active);

        long totalItems = ledger.totalItemCount();
        long roughEquivalent = ledger.totalRoughEquivalent();
        double averagePerBlock = sessionBlocks <= 0L
                ? 0.0
                : (double) roughEquivalent / sessionBlocks;
        double roughEquivalentPerHour = activeMillis <= 0L
                ? 0.0
                : roughEquivalent * 3_600_000.0 / activeMillis;

        List<Metric> metrics = List.of(
                new Metric(compact(roughEquivalentPerHour), "eq/h"),
                new Metric(NUMBER.format(averagePerBlock), "avg"),
                new Metric(compact(roughEquivalent), "eq"));

        int topHeight = gemstoneTopCardHeight();
        int fullHeight = currentHeight();
        List<GemstoneRow> allRows = selection.isAllGemstones()
                ? gemstoneRows()
                : List.of();

        graphics.pose().pushMatrix();
        graphics.pose().translate(config.x, config.y);
        graphics.pose().scale(config.scale, config.scale);

        drawCard(graphics, 0, 0, WIDTH, fullHeight);
        drawTopSection(
                graphics,
                selection.displayName(),
                sessionBlocks,
                now,
                activeMillis,
                active,
                state.lastBreakEpochMillis,
                metrics,
                RotClientVersionLabel.gemstoneHudFooter());
        drawRule(graphics, topHeight);
        if (selection.isAllGemstones()) {
            drawGemstoneMatrix(
                    graphics,
                    allRows,
                    topHeight + HudLayoutMath.SECTION_GAP,
                    totalItems,
                    roughEquivalent);
        } else {
            drawGemstoneLedger(
                    graphics,
                    ledger,
                    topHeight + HudLayoutMath.SECTION_GAP,
                    totalItems,
                    roughEquivalent);
        }

        graphics.pose().popMatrix();
    }

    /**
     * Header, area, blocks, graph, metric row, footer and pause bar. Shared
     * by ore and gemstone targets; each block advances the cursor by exactly
     * the constant {@link HudLayoutMath#topCardHeight} adds for it.
     */
    private void drawTopSection(GuiGraphicsExtractor graphics,
                                String name,
                                long sessionBlocks,
                                long now,
                                long activeMillis,
                                boolean active,
                                long lastBreakMillis,
                                List<Metric> metrics,
                                String footerLabel) {
        int cursor = HudLayoutMath.PAD_TOP;
        cursor = drawHeader(graphics, name, active, sessionBlocks, cursor);

        if (config.showArea) {
            cursor = drawAreaRow(graphics, cursor);
        }
        if (config.showBlocks) {
            cursor = drawBlocksRow(graphics, sessionBlocks, cursor);
        }
        if (config.showRateGraph) {
            drawRateGraph(
                    graphics, PAD, cursor,
                    WIDTH - PAD, cursor + HudLayoutMath.GRAPH_HEIGHT);
            cursor += HudLayoutMath.GRAPH_SECTION;
        }
        if (!metrics.isEmpty()) {
            drawMetricRow(graphics, metrics, cursor);
            cursor += HudLayoutMath.METRIC_ROW;
        }
        if (config.showHudVersion || config.showSessionTime) {
            int y = cursor + 1;
            if (config.showHudVersion) {
                RotClientUiDraw.text(graphics, font(), footerLabel,
                        PAD, y, RotClientTheme.TEXT_DIM, false);
            }
            if (config.showSessionTime) {
                drawRight(graphics, "◷ " + formatDuration(activeMillis),
                        WIDTH - PAD, y, RotClientTheme.TEXT_MUTED, false);
            }
            cursor += HudLayoutMath.FOOTER_ROW;
        }
        if (config.showHudAutoPause) {
            long sinceBreak = lastBreakMillis <= 0
                    ? RotClientClient.PAUSE_AFTER_MILLIS
                    : Math.max(0, now - lastBreakMillis);
            double remaining = active
                    ? 1.0 - Math.min(
                            1.0,
                            sinceBreak
                                    / (double) RotClientClient
                                    .PAUSE_AFTER_MILLIS)
                    : 0;
            int barTop = cursor + HudLayoutMath.PAUSE_BAR_SECTION
                    - HudLayoutMath.PAUSE_BAR_HEIGHT;
            drawProgressBar(
                    graphics, PAD, barTop,
                    WIDTH - PAD, barTop + HudLayoutMath.PAUSE_BAR_HEIGHT,
                    remaining);
        }
    }

    /** One line: target, held tool, status. Returns the next cursor. */
    private int drawHeader(GuiGraphicsExtractor graphics,
                           String name,
                           boolean active,
                           long sessionBlocks,
                           int cursor) {
        boolean showTitle = config.showHudTitle;
        boolean showStatus = config.showHudStatus;
        boolean showTool = config.showActiveTool;
        if (!showTitle && !showStatus && !showTool) return cursor;

        int y = cursor + 2;
        int left = PAD;
        int right = WIDTH - PAD;
        if (showStatus) {
            String status = active
                    ? "RUNNING"
                    : (sessionBlocks > 0 ? "PAUSED" : "READY");
            int color = active
                    ? RotClientTheme.SUCCESS
                    : (sessionBlocks > 0
                            ? RotClientTheme.WARNING
                            : RotClientTheme.TEXT_MUTED);
            String label = "● " + status;
            int width = RotClientFonts.width(font(), label);
            RotClientUiDraw.text(
                    graphics, font(), label, right - width, y, color, false);
            right -= width + 8;
        }
        if (showTitle) {
            String title = fitHudText(
                    name.toUpperCase(Locale.ROOT), right - left);
            RotClientUiDraw.text(graphics, font(), title, left, y,
                    RotClientTheme.HUD_TITLE, true);
            left += RotClientFonts.width(font(), title) + 8;
        }
        if (showTool && right - left > 24) {
            String tool = fitHudText(heldToolName(), right - left);
            RotClientUiDraw.text(graphics, font(), tool, left, y,
                    RotClientTheme.HUD_TEXT_DIM, false);
        }
        return cursor + HudLayoutMath.HEADER_ROW;
    }

    /** "12.3k blocks/h  ↑4%" on the left, "4.5k total" on the right. */
    private int drawBlocksRow(GuiGraphicsExtractor graphics,
                              long sessionBlocks,
                              int cursor) {
        int y = cursor + 1;
        String total = compact(sessionBlocks);
        int totalWidth = pairWidth(total, "total");
        drawPair(graphics, WIDTH - PAD - totalWidth, y, total,
                RotClientTheme.HUD_ACCENT, "total");

        int x = drawPair(graphics, PAD, y, compact(smoothedBlocksPerHour),
                RotClientTheme.HUD_TEXT, "blocks/h");
        String trend = compactTrend();
        int trendWidth = RotClientFonts.width(font(), trend);
        if (!trend.isEmpty()
                && x + 5 + trendWidth < WIDTH - PAD - totalWidth - 6) {
            int color = trend.startsWith("↓")
                    ? RotClientTheme.WARNING
                    : RotClientTheme.SUCCESS;
            RotClientUiDraw.text(graphics, font(), trend, x + 5, y,
                    color, false);
        }
        return cursor + HudLayoutMath.BLOCKS_ROW;
    }

    /**
     * Inline figures spread across the row. If labels would not fit, the
     * figures fall back to bare values rather than overlapping.
     */
    private void drawMetricRow(GuiGraphicsExtractor graphics,
                               List<Metric> metrics,
                               int cursor) {
        int y = cursor + 1;
        int minGap = 6;
        int gaps = Math.max(0, metrics.size() - 1);
        int labelled = 0;
        for (Metric metric : metrics) {
            labelled += pairWidth(metric.value(), metric.label());
        }
        boolean showLabels = labelled + minGap * gaps <= INNER;
        int used = 0;
        for (Metric metric : metrics) {
            used += showLabels
                    ? pairWidth(metric.value(), metric.label())
                    : RotClientFonts.width(font(), metric.value());
        }
        int gap = gaps == 0 ? 0 : Math.max(minGap, (INNER - used) / gaps);
        int x = PAD;
        for (Metric metric : metrics) {
            x = showLabels
                    ? drawPair(graphics, x, y, metric.value(),
                            RotClientTheme.HUD_ACCENT, metric.label())
                    : drawPair(graphics, x, y, metric.value(),
                            RotClientTheme.HUD_ACCENT, "");
            x += gap;
        }
    }

    /** Bright value followed by a dim label. Returns the end x. */
    private int drawPair(GuiGraphicsExtractor graphics,
                         int x, int y,
                         String value, int valueColor, String label) {
        RotClientUiDraw.text(graphics, font(), value, x, y, valueColor, true);
        int end = x + RotClientFonts.width(font(), value);
        if (label.isEmpty()) return end;
        RotClientUiDraw.text(graphics, font(), label, end + 3, y,
                RotClientTheme.HUD_TEXT_DIM, false);
        return end + 3 + RotClientFonts.width(font(), label);
    }

    private int pairWidth(String value, String label) {
        int width = RotClientFonts.width(font(), value);
        return label.isEmpty()
                ? width
                : width + 3 + RotClientFonts.width(font(), label);
    }

    private void drawValueSection(GuiGraphicsExtractor graphics,
                                  TrackingTarget target,
                                  long now, int top,
                                  double sessionValue,
                                  double unsoldValue,
                                  double coinsPerHour) {
        MiningHudOtherSummary otherSummary =
                RotClientClient.hudOtherMinedSummary();
        boolean itemRows =
                config.showRawMaterial || config.showEnchantedMaterial;
        double targetValue = 0.0;
        int cursor = top;
        if (itemRows) {
            if (config.showTargetHeading) {
                RotClientUiDraw.text(graphics, font(), "TARGET",
                        PAD, cursor + 1, RotClientTheme.TEXT_MUTED, true);
                cursor += HudLayoutMath.HEADING_ROW;
            }
            for (TrackedMaterial material : target.materials()) {
                if (config.showEnchantedMaterial) {
                    long amount =
                            RotClientClient.currentSessionEnchantedItems(
                                    material);
                    double value = RotClientClient.estimateNetValue(
                            material, 0, amount);
                    targetValue += value;
                    drawMaterialItemRow(
                            graphics, material, cursor,
                            material.enchantedItemName(), amount, value,
                            RotClientTheme.HUD_ACCENT);
                    cursor += HudLayoutMath.ITEM_ROW;
                }
                if (config.showRawMaterial) {
                    long amount = RotClientClient.currentSessionRawItems(
                            material);
                    double value = RotClientClient.estimateNetValue(
                            material, amount, 0);
                    targetValue += value;
                    drawMaterialItemRow(
                            graphics, material, cursor,
                            material.rawItemName(), amount, value,
                            RotClientTheme.CHART_LINE);
                    cursor += HudLayoutMath.ITEM_ROW;
                }
            }
        } else {
            targetValue = sessionValue;
        }

        if (config.showOtherSection) {
            cursor = drawOthersSummaryRow(graphics, cursor, otherSummary);
        }

        boolean anyValueLine = config.showTargetValue
                || config.showOtherValue
                || config.showTotalMinedValue
                || config.showSessionProfit
                || config.showCoinsPerHour
                || config.showUnsoldValue;
        if ((itemRows || config.showOtherSection) && anyValueLine) {
            drawRule(graphics, cursor);
            cursor += HudLayoutMath.SEPARATOR;
        }

        double otherNet = otherSummary.analyticsActive()
                ? otherSummary.resolvedNetValue(config.bazaarTaxPercent)
                : 0.0;
        if (config.showTargetValue) {
            cursor = drawValueLine(graphics, cursor, "Target value",
                    compactCoins(targetValue),
                    RotClientTheme.TEXT, RotClientTheme.HUD_ACCENT);
        }
        if (config.showOtherValue) {
            String value = otherSummary.analyticsActive()
                    ? otherSummary.valueDisplayCompact(compact(otherNet))
                            + " coins"
                    : "—";
            int color = !otherSummary.analyticsActive()
                    ? RotClientTheme.TEXT_MUTED
                    : otherSummary.hasUnresolved()
                            ? RotClientTheme.WARNING
                            : RotClientTheme.TEXT;
            cursor = drawValueLine(graphics, cursor, "Others value",
                    value, RotClientTheme.TEXT, color);
        }
        if (config.showTotalMinedValue) {
            double total = otherSummary.analyticsActive()
                    ? targetValue + otherNet
                    : targetValue;
            cursor = drawValueLine(graphics, cursor, "Total mined",
                    compactCoins(total),
                    RotClientTheme.TEXT, RotClientTheme.WARNING);
        }
        if (config.showSessionProfit) {
            cursor = drawValueLine(graphics, cursor, "Session value (net)",
                    compactCoins(sessionValue),
                    RotClientTheme.TEXT, RotClientTheme.WARNING);
        }
        if (config.showCoinsPerHour) {
            cursor = drawValueLine(graphics, cursor, "Coins / hour",
                    compactCoins(coinsPerHour),
                    RotClientTheme.TEXT, RotClientTheme.HUD_ACCENT);
        }
        if (config.showUnsoldValue) {
            cursor = drawValueLine(graphics, cursor, "Unsold (net)",
                    compactCoins(unsoldValue),
                    RotClientTheme.TEXT_MUTED, RotClientTheme.TEXT);
        }
        if (config.showBazaarPrices) {
            int firstRow = cursor;
            String taxLabel =
                    "TAX " + NUMBER.format(config.bazaarTaxPercent) + "%";
            int priceMax =
                    INNER - RotClientFonts.width(font(), taxLabel) - 6;
            for (TrackedMaterial material : target.materials()) {
                MaterialTrackerState state = config.state(material);
                String prices = state.lastRawPrice > 0
                        && state.lastEnchantedPrice > 0
                        ? material.displayName().toUpperCase(Locale.ROOT)
                                + " BZ " + NUMBER.format(state.lastRawPrice)
                                + " / "
                                + NUMBER.format(state.lastEnchantedPrice)
                                + " · " + priceAge(state, now)
                        : material.displayName().toUpperCase(Locale.ROOT)
                                + " BAZAAR: CONNECTING...";
                RotClientUiDraw.text(
                        graphics, font(),
                        fitHudText(prices, priceMax),
                        PAD, cursor + 1, RotClientTheme.TEXT_DIM, false);
                cursor += HudLayoutMath.BAZAAR_ROW;
            }
            drawRight(
                    graphics,
                    taxLabel,
                    WIDTH - PAD,
                    firstRow + 1,
                    RotClientTheme.TEXT_DIM,
                    false);
        }
    }

    /** Quantity column right edge; moves left when the value is wide. */
    private int qtyRightFor(String valueText) {
        return Math.min(QTY_RIGHT,
                WIDTH - PAD - RotClientFonts.width(font(), valueText) - 6);
    }

    private int drawValueLine(GuiGraphicsExtractor graphics,
                              int y,
                              String label,
                              String value,
                              int labelColor,
                              int valueColor) {
        int labelMax = INNER - RotClientFonts.width(font(), value) - 6;
        RotClientUiDraw.text(graphics, font(), fitHudText(label, labelMax),
                PAD, y + 1, labelColor, false);
        drawRight(graphics, value, WIDTH - PAD, y + 1, valueColor, true);
        return y + HudLayoutMath.VALUE_ROW;
    }

    /**
     * Single aggregate OTHERS row: non-target mining + mob + chest/reward
     * item units from Current Session. Not a per-item breakdown.
     */
    private int drawOthersSummaryRow(
            GuiGraphicsExtractor graphics,
            int y,
            MiningHudOtherSummary otherSummary) {
        String label = MiningHudOtherSummary.hudRowLabel();
        int textY = y + 2;
        int next = y + HudLayoutMath.ITEM_ROW;
        if (!otherSummary.analyticsActive()) {
            RotClientUiDraw.text(graphics, font(), "No others yet",
                    NAME_X, textY, RotClientTheme.TEXT_MUTED, false);
            drawRight(graphics, "—", WIDTH - PAD, textY,
                    RotClientTheme.TEXT_MUTED, false);
            return next;
        }

        if (otherSummary.isEmpty()) {
            RotClientUiDraw.text(graphics, font(), label, NAME_X, textY,
                    RotClientTheme.TEXT_MUTED, false);
            drawRight(graphics, "×0", qtyRightFor("0"), textY,
                    RotClientTheme.TEXT_DIM, false);
            drawRight(graphics, "0", WIDTH - PAD, textY,
                    RotClientTheme.TEXT_DIM, false);
            return next;
        }

        double otherNet = otherSummary.resolvedNetValue(config.bazaarTaxPercent);
        RotClientUiDraw.text(graphics, font(), label, NAME_X, textY,
                RotClientTheme.TEXT, false);
        String otherValue =
                otherSummary.valueDisplayCompact(compact(otherNet));
        drawRight(graphics, "×" + compact(otherSummary.totalQuantity()),
                qtyRightFor(otherValue), textY, RotClientTheme.TEXT_DIM, false);
        drawRight(
                graphics,
                otherValue,
                WIDTH - PAD,
                textY,
                otherSummary.hasUnresolved()
                        ? RotClientTheme.WARNING
                        : RotClientTheme.CHART_LINE,
                false);
        return next;
    }

    private void drawMaterialItemRow(GuiGraphicsExtractor graphics,
                                     TrackedMaterial material,
                                     int y, String item,
                                     long amount, double value,
                                     int valueColor) {
        ItemStack icon = new ItemStack(material.iconItem());
        graphics.pose().pushMatrix();
        graphics.pose().translate(PAD, y + 1);
        graphics.pose().scale(ICON_SCALE, ICON_SCALE);
        graphics.item(icon, 0, 0);
        graphics.pose().popMatrix();

        int textY = y + 2;
        String quantity = "×" + compact(amount);
        String valueText = compact(value);
        int qtyRight = qtyRightFor(valueText);
        int nameMax = qtyRight
                - RotClientFonts.width(font(), quantity) - 4 - NAME_X;
        RotClientUiDraw.text(graphics, font(), fitHudText(item, nameMax),
                NAME_X, textY, RotClientTheme.TEXT, false);
        drawRight(graphics, quantity, qtyRight, textY,
                RotClientTheme.TEXT_DIM, false);
        drawRight(graphics, valueText, WIDTH - PAD, textY,
                valueColor, false);
    }

    private void drawGemstoneLedger(GuiGraphicsExtractor graphics,
                                    GemstoneLedger ledger,
                                    int top,
                                    long totalItems,
                                    long roughEquivalent) {
        int cursor = top;
        RotClientUiDraw.text(graphics, font(), "TIER", PAD, cursor + 1,
                RotClientTheme.TEXT_DIM, true);
        drawRight(graphics, "QTY", LEDGER_QTY_RIGHT, cursor + 1,
                RotClientTheme.TEXT_DIM, true);
        drawRight(graphics, "ROUGH EQ", WIDTH - PAD, cursor + 1,
                RotClientTheme.TEXT_DIM, true);
        cursor += HudLayoutMath.HEADING_ROW;

        for (GemstoneTier tier : GemstoneTier.values()) {
            long quantity = ledger.quantity(tier);
            long equivalent =
                    Math.multiplyExact(quantity, tier.roughEquivalent());
            int y = cursor + 1;
            RotClientUiDraw.text(graphics, font(), tier.displayName(),
                    PAD, y, gemstoneTierColor(tier), false);
            drawRight(graphics, compact(quantity), LEDGER_QTY_RIGHT, y,
                    RotClientTheme.TEXT, false);
            drawRight(graphics, compact(equivalent), WIDTH - PAD, y,
                    RotClientTheme.TEXT_DIM, false);
            cursor += HudLayoutMath.VALUE_ROW;
        }

        drawRule(graphics, cursor);
        cursor += HudLayoutMath.SEPARATOR;
        cursor = drawValueLine(graphics, cursor, "Total items",
                compact(totalItems),
                RotClientTheme.TEXT_MUTED, RotClientTheme.TEXT);
        drawValueLine(graphics, cursor, "Total rough equivalent",
                compact(roughEquivalent),
                RotClientTheme.TEXT, RotClientTheme.HUD_ACCENT);
    }

    /** One gemstone that has been gained this session. */
    private record GemstoneRow(GemstoneType gemstone,
                               GemstoneLedger ledger,
                               long roughEquivalent) {
    }

    /** Gemstones with items this session, best rough equivalent first. */
    private List<GemstoneRow> gemstoneRows() {
        List<GemstoneRow> rows = new ArrayList<>();
        for (GemstoneType gemstone : GemstoneType.values()) {
            GemstoneLedger ledger =
                    config.gemstoneState(gemstone).sessionLedger();
            if (ledger.totalItemCount() > 0L) {
                rows.add(new GemstoneRow(
                        gemstone, ledger, ledger.totalRoughEquivalent()));
            }
        }
        rows.sort((left, right) ->
                Long.compare(right.roughEquivalent(), left.roughEquivalent()));
        return rows;
    }

    /**
     * All Gemstones: one row per gemstone you actually gained, one column
     * per tier. Empty gemstones take no space, so the table only ever grows
     * with what is being mined.
     */
    private void drawGemstoneMatrix(GuiGraphicsExtractor graphics,
                                    List<GemstoneRow> rows,
                                    int top,
                                    long totalItems,
                                    long roughEquivalent) {
        int shown = Math.min(rows.size(), HudLayoutMath.MAX_ALL_GEMSTONE_ROWS);
        List<GemstoneRow> visible = rows.subList(0, shown);

        // Only tiers that have data get a column (Rough and Flawed always
        // do), each sized to its widest text and packed from the right, so
        // headings, values and names are measured rather than assumed to fit.
        List<GemstoneTier> tiers = new ArrayList<>();
        for (GemstoneTier tier : GemstoneTier.values()) {
            boolean used = tier.ordinal() < 2;
            for (GemstoneRow row : visible) {
                if (row.ledger().quantity(tier) > 0L) used = true;
            }
            if (used) tiers.add(tier);
        }
        int[] widths = new int[tiers.size()];
        for (int k = 0; k < widths.length; k++) {
            GemstoneTier tier = tiers.get(k);
            widths[k] = RotClientFonts.width(font(), tierShortLabel(tier)) + 1;
            for (GemstoneRow row : visible) {
                widths[k] = Math.max(widths[k], RotClientFonts.width(
                        font(), matrixCell(row, tier)));
            }
        }
        HudColumnLayout.Result columns = HudColumnLayout.packFromRight(
                widths, WIDTH - PAD, PAD,
                MATRIX_GAP, MATRIX_TIGHT_GAP, MATRIX_MIN_NAME);
        int[] rights = columns.rights();
        int nameMax = columns.nameMax();

        int cursor = top;
        for (int k = 0; k < tiers.size(); k++) {
            drawRight(graphics, tierShortLabel(tiers.get(k)), rights[k],
                    cursor + 1, gemstoneTierColor(tiers.get(k)), true);
        }
        cursor += HudLayoutMath.HEADING_ROW;

        if (rows.isEmpty()) {
            RotClientUiDraw.text(graphics, font(), "No gemstones yet", PAD,
                    cursor + 1, RotClientTheme.TEXT_MUTED, false);
            cursor += HudLayoutMath.VALUE_ROW;
        }
        for (GemstoneRow row : visible) {
            int y = cursor + 1;
            String name = row.gemstone().displayName();
            if (RotClientFonts.width(font(), name) > nameMax) {
                // Three letters are enough with the gemstone's own colour.
                name = name.substring(0, Math.min(3, name.length()));
            }
            RotClientUiDraw.text(graphics, font(), fitHudText(name, nameMax),
                    PAD, y, gemstoneColor(row.gemstone()), false);
            for (int k = 0; k < tiers.size(); k++) {
                boolean has = row.ledger().quantity(tiers.get(k)) > 0L;
                drawRight(graphics, matrixCell(row, tiers.get(k)),
                        rights[k], y,
                        has ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED,
                        false);
            }
            cursor += HudLayoutMath.VALUE_ROW;
        }
        if (rows.size() > shown) {
            RotClientUiDraw.text(graphics, font(),
                    "+" + (rows.size() - shown) + " more", PAD, cursor + 1,
                    RotClientTheme.TEXT_DIM, false);
            cursor += HudLayoutMath.VALUE_ROW;
        }

        drawRule(graphics, cursor);
        cursor += HudLayoutMath.SEPARATOR;
        cursor = drawValueLine(graphics, cursor, "Total items",
                compact(totalItems),
                RotClientTheme.TEXT_MUTED, RotClientTheme.TEXT);
        drawValueLine(graphics, cursor, "Total rough equivalent",
                compact(roughEquivalent),
                RotClientTheme.TEXT, RotClientTheme.HUD_ACCENT);
    }

    private static String matrixCell(GemstoneRow row, GemstoneTier tier) {
        long quantity = row.ledger().quantity(tier);
        return quantity > 0L ? HudNumbers.compactCount(quantity) : "-";
    }

    /** Short tier labels sized for the matrix columns. */
    private static String tierShortLabel(GemstoneTier tier) {
        return switch (tier) {
            case ROUGH -> "Rgh";
            case FLAWED -> "Flwd";
            case FINE -> "Fine";
            case FLAWLESS -> "Flwl";
            case PERFECT -> "Perf";
        };
    }

    /** Roughly each gemstone's in-game colour. */
    private static int gemstoneColor(GemstoneType gemstone) {
        return switch (gemstone) {
            case RUBY -> 0xFFFF5555;
            case AMBER -> 0xFFFFAA00;
            case SAPPHIRE -> 0xFF5599FF;
            case JADE -> 0xFF55FF77;
            case AMETHYST -> 0xFFC77DFF;
            case TOPAZ -> 0xFFFFE066;
            case JASPER -> 0xFFFF6EC7;
            case OPAL -> 0xFFE8F4FF;
            case ONYX -> 0xFFB4B4C4;
            case AQUAMARINE -> 0xFF55FFEE;
            case CITRINE -> 0xFFE5B84B;
            case PERIDOT -> 0xFFA8E05F;
        };
    }

    private int gemstoneTopCardHeight() {
        return HudLayoutMath.gemstoneTopCardHeight(
                config.showBlocks,
                config.showRateGraph,
                config.showArea,
                config.showHudTitle,
                config.showHudAutoPause,
                config.showHudVersion,
                config.showSessionTime,
                config.showHudStatus,
                config.showActiveTool);
    }

    private static int gemstoneTierColor(GemstoneTier tier) {
        return switch (tier) {
            case ROUGH -> RotClientTheme.TEXT;
            case FLAWED -> RotClientTheme.SUCCESS;
            case FINE -> RotClientTheme.CHART_LINE;
            case FLAWLESS -> RotClientTheme.HUD_ACCENT;
            case PERFECT -> RotClientTheme.WARNING;
        };
    }

    private double displayedResource(
            TrackedMaterial material,
            double effectiveFortune,
            boolean fortuneKnown) {
        MaterialTrackerState state = config.state(material);
        if (state.sessionActualRawEquivalent > 0) {
            return state.sessionActualRawEquivalent;
        }
        long baseDrops = state.sessionBaseDrops > 0
                ? state.sessionBaseDrops
                : state.sessionBlocks * material.baseDrop();
        double multiplier = fortuneKnown
                ? 1.0 + effectiveFortune / 100.0
                : 1.0;
        return baseDrops * multiplier;
    }

    private void drawRule(GuiGraphicsExtractor graphics, int y) {
        graphics.fill(PAD, y, WIDTH - PAD, y + 1,
                RotClientUiDraw.withAlpha(RotClientTheme.DIVIDER, 0x90));
    }

    private void drawRateGraph(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        if (rateHistory.isEmpty()) {
            RotClientChartDraw.drawLineAreaChart(
                    graphics, left, top, right, bottom, new double[0], graphScaleMaximum);
            return;
        }
        double[] rawValues = new double[rateHistory.size()];
        int rawIndex = 0;
        for (double value : rateHistory) {
            rawValues[rawIndex++] = value;
        }
        RotClientChartDraw.drawLineAreaChart(
                graphics, left, top, right, bottom, rawValues, graphScaleMaximum);
    }

    private void drawProgressBar(GuiGraphicsExtractor graphics, int left, int top,
                                 int right, int bottom, double progress) {
        roundedFill(graphics, left, top, right, bottom, HudCardStyle.BAR_TRACK);
        int filled = (int) Math.round((right - left) * Mth.clamp(progress, 0, 1));
        if (filled > 0) roundedFill(graphics, left, top, left + filled, bottom, RotClientTheme.HUD_ACCENT);
    }

    /**
     * One card behind the whole HUD, styled exactly like the Pet HUD panel:
     * HUD Layout background colour, rounded corners, soft shadow, thin border.
     */
    private void drawCard(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom) {

        if (!config.hudShowBackground) {
            if (editorOpen) {
                roundedOutline(
                        graphics,
                        left,
                        top,
                        right,
                        bottom,
                        RotClientUiDraw.withAlpha(
                                RotClientTheme.HUD_ACCENT,
                                0xB8));
            }

            return;
        }

        RotClientUiDraw.roundedFill(
                graphics,
                left + HudCardStyle.SHADOW_OFFSET_X,
                top + HudCardStyle.SHADOW_OFFSET_Y,
                right + HudCardStyle.SHADOW_OFFSET_X,
                bottom + HudCardStyle.SHADOW_OFFSET_Y,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.SHADOW,
                        editorOpen
                                ? HudCardStyle.EDITOR_SHADOW_ALPHA
                                : HudCardStyle.SHADOW_ALPHA),
                HudCardStyle.RADIUS);

        RotClientUiDraw.roundedFill(
                graphics,
                left,
                top,
                right,
                bottom,
                hudBackgroundColor(),
                HudCardStyle.RADIUS);

        RotClientUiDraw.roundedOutline(
                graphics,
                left,
                top,
                right,
                bottom,
                RotClientUiDraw.withAlpha(
                        RotClientTheme.BORDER,
                        HudCardStyle.BORDER_ALPHA),
                HudCardStyle.RADIUS);
    }

    /**
     * Same source the Pet HUD uses: the HUD Layout style, so changing the HUD
     * Layout background colour or opacity restyles both together.
     */
    private int hudBackgroundColor() {
        if (config.qolUtilities == null) {
            return HudStylePolicy.DEFAULT_BG;
        }
        return config.qolUtilities.extras()
                .resolvedHudStyle(HUD_STYLE_ID).backgroundColor;
    }

    private void sampleMiningRate(
            long sessionBlocks,
            long now,
            boolean active) {
        if (sessionBlocks < lastRateBlocks) {
            rateHistory.clear();
            lastRateBlocks = sessionBlocks;
            lastRateSampleMillis = now;
            smoothedBlocksPerHour = 0;
            graphScaleMaximum = 1;
        }
        if (lastRateSampleMillis == 0) {
            lastRateSampleMillis = now;
            lastRateBlocks = sessionBlocks;
            rateHistory.add(0.0);
            return;
        }

        long elapsed = now - lastRateSampleMillis;
        if (elapsed < 1_000L) return;
        long blockDelta = Math.max(0, sessionBlocks - lastRateBlocks);
        double instantRate = blockDelta * 3_600_000.0 / Math.max(1, elapsed);
        double smoothing = 1.0 - Math.exp(-elapsed / 3_500.0);
        smoothedBlocksPerHour += (instantRate - smoothedBlocksPerHour) * smoothing;
        if (!active) {
            smoothedBlocksPerHour *= Math.exp(-elapsed / 2_500.0);
        }

        rateHistory.addLast(Math.max(0, smoothedBlocksPerHour));
        while (rateHistory.size() > MAX_RATE_SAMPLES) rateHistory.removeFirst();
        double targetScale = 1;
        for (double value : rateHistory) targetScale = Math.max(targetScale, value);
        targetScale *= 1.08;
        graphScaleMaximum = targetScale >= graphScaleMaximum
                ? targetScale
                : graphScaleMaximum + (targetScale - graphScaleMaximum) * 0.08;
        lastRateBlocks = sessionBlocks;
        lastRateSampleMillis = now;
    }

    /** "↑4%" / "↓12%" against the recent average; blank until there is signal. */
    private String compactTrend() {
        if (rateHistory.size() < 4) return "";
        double total = 0;
        for (double value : rateHistory) total += value;
        double average = total / rateHistory.size();
        if (average < 1) return "";
        double percent = (smoothedBlocksPerHour / average - 1.0) * 100.0;
        return (percent >= 0 ? "↑" : "↓")
                + NUMBER.format(Math.abs(percent)) + "%";
    }

    private int topCardHeight() {
        return HudLayoutMath.topCardHeight(
                config.showBlocks,
                config.showRateGraph,
                config.showMaterialPerHour,
                config.showDropAndFortune,
                config.showHudTitle,
                config.showHudAutoPause,
                config.showHudVersion,
                config.showSessionTime,
                config.showHudStatus,
                config.showActiveTool,
                config.showArea);
    }

    private boolean hasProfitCard() {
        return HudLayoutMath.hasProfitCard(
                config.showRawMaterial,
                config.showEnchantedMaterial,
                config.showSessionProfit,
                config.showCoinsPerHour,
                config.showUnsoldValue,
                config.showBazaarPrices,
                config.showValuePanel,
                config.showOtherSection,
                config.showTargetValue,
                config.showOtherValue,
                config.showTotalMinedValue);
    }

    private int profitCardHeight() {
        return HudLayoutMath.profitCardHeight(
                config.showRawMaterial,
                config.showEnchantedMaterial,
                config.showSessionProfit,
                config.showCoinsPerHour,
                config.showUnsoldValue,
                config.showBazaarPrices,
                config.selectedTarget().materials().size(),
                config.showOtherSection,
                config.showTargetValue,
                config.showOtherValue,
                config.showTotalMinedValue,
                config.showTargetHeading);
    }

    int currentHeight() {
        if (config.selectedSelection().isAllGemstones()) {
            return HudLayoutMath.gemstoneAllHudHeight(
                    config.showBlocks,
                    config.showRateGraph,
                    config.showArea,
                    config.showHudTitle,
                    config.showHudAutoPause,
                    config.showHudVersion,
                    config.showSessionTime,
                    config.showHudStatus,
                    config.showActiveTool,
                    gemstoneRows().size());
        }
        if (config.selectedSelection().isGemstone()) {
            return HudLayoutMath.gemstoneHudHeight(
                    config.showBlocks,
                    config.showRateGraph,
                    config.showArea,
                    config.showHudTitle,
                    config.showHudAutoPause,
                    config.showHudVersion,
                    config.showSessionTime,
                    config.showHudStatus,
                    config.showActiveTool);
        }

        int top = topCardHeight();

        return hasProfitCard()
                ? top + HudLayoutMath.SECTION_GAP + profitCardHeight()
                : top;
    }

    boolean containsScreen(double mouseX, double mouseY) {
        return contains(mouseX, mouseY);
    }

    void nudgeScale(float delta) {
        if (!editorOpen) {
            return;
        }
        float oldScale = config.scale;
        config.scale = Mth.clamp(config.scale + delta, 0.5F, 2.5F);
        if (Math.abs(config.scale - oldScale) < 0.0001F) {
            return;
        }
        clampToScreen();
        RotClientClient.save();
    }

    void onMaterialChanged() {
        rateHistory.clear();
        lastRateSampleMillis = 0;
        lastRateBlocks = 0;
        smoothedBlocksPerHour = 0;
        graphScaleMaximum = 1;
    }

    void setEditorOpen(boolean value) {
        editorOpen = value;
        if (!value) {
            dragging = false;
            RotClientClient.save();
        }
    }

    boolean beginDrag(double mouseX, double mouseY) {
        if (!editorOpen || !contains(mouseX, mouseY)) return false;
        dragging = true;
        dragOffsetX = mouseX - config.x;
        dragOffsetY = mouseY - config.y;
        return true;
    }

    boolean dragTo(double mouseX, double mouseY) {
        if (!dragging) return false;
        Minecraft client = Minecraft.getInstance();
        config.x = (float) HudLayoutMath.clampOrigin(
                mouseX - dragOffsetX,
                client.getWindow().getGuiScaledWidth(),
                WIDTH,
                config.scale);
        config.y = (float) HudLayoutMath.clampOrigin(
                mouseY - dragOffsetY,
                client.getWindow().getGuiScaledHeight(),
                currentHeight(),
                config.scale);
        return true;
    }

    boolean endDrag() {
        if (!dragging) return false;
        dragging = false;
        RotClientClient.save();
        return true;
    }

    boolean onScroll(double mouseX, double mouseY, double amount) {
        if (!editorOpen || !contains(mouseX, mouseY)) return false;
        float oldScale = config.scale;
        config.scale = Mth.clamp(config.scale + (float) Math.signum(amount) * 0.1F, 0.5F, 2.5F);
        config.x += (float) ((mouseX - config.x) * (1.0F - config.scale / oldScale));
        config.y += (float) ((mouseY - config.y) * (1.0F - config.scale / oldScale));
        clampToScreen();
        RotClientClient.save();
        return true;
    }

    void clampToScreen() {
        Minecraft client = Minecraft.getInstance();
        config.x = (float) HudLayoutMath.clampOrigin(
                config.x,
                client.getWindow().getGuiScaledWidth(),
                WIDTH,
                config.scale);
        config.y = (float) HudLayoutMath.clampOrigin(
                config.y,
                client.getWindow().getGuiScaledHeight(),
                currentHeight(),
                config.scale);
    }

    private boolean contains(double mouseX, double mouseY) {
        if (!HudEditorPreviewPolicy.showMiningTracker(config.enabled)) {
            return false;
        }
        return mouseX >= config.x && mouseX <= config.x + WIDTH * config.scale
                && mouseY >= config.y && mouseY <= config.y + currentHeight() * config.scale;
    }

    /** Name of the held item, or blank when the hand is empty. */
    private String heldToolName() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.getMainHandItem().isEmpty()) {
            return "";
        }
        return client.player.getMainHandItem().getHoverName().getString().trim();
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    private int drawAreaRow(GuiGraphicsExtractor graphics, int cursor) {
        int y = cursor + 1;
        RotClientUiDraw.text(graphics, font(), "AREA", PAD, y, RotClientTheme.HUD_TEXT_DIM, true);
        String value = fitHudText(hudLocationDisplay(), INNER - 40);
        drawRight(graphics, value, WIDTH - PAD, y, RotClientTheme.HUD_TEXT, false);
        return cursor + HudLayoutMath.AREA_ROW_HEIGHT;
    }

    /**
     * Live location for the AREA row. Editor preview uses a representative
     * Dwarven Mines · The Forge sample when no live SkyBlock location is
     * known so the layout editor works offline.
     */
    private String hudLocationDisplay() {
        SkyBlockLocation live = SkyBlockAreaDetector.detectLocation();
        if (editorOpen && live.isUnknown()) {
            return SkyBlockLocation.EDITOR_PREVIEW.hudDisplay();
        }
        return live.hudDisplay();
    }

    private String fitHudText(String text, int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        if (RotClientFonts.width(font(), text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        String trimmed = text;
        while (!trimmed.isEmpty()
                && RotClientFonts.width(font(), trimmed + suffix) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? suffix : trimmed + suffix;
    }

    private void drawRight(GuiGraphicsExtractor graphics, String text, int right, int y,
                           int color, boolean shadow) {
        RotClientUiDraw.text(graphics, font(), text, right - RotClientFonts.width(font(), text), y, color, shadow);
    }

    private static void roundedFill(GuiGraphicsExtractor graphics, int left, int top,
                                    int right, int bottom, int color) {
        RotClientUiDraw.roundedFill(graphics, left, top, right, bottom, color);
    }

    private static void roundedOutline(GuiGraphicsExtractor graphics, int left, int top,
                                       int right, int bottom, int color) {
        RotClientUiDraw.roundedOutline(graphics, left, top, right, bottom, color);
    }

    private static String compact(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000) return NUMBER.format(value / 1_000_000_000) + "b";
        if (abs >= 1_000_000) return NUMBER.format(value / 1_000_000) + "m";
        if (abs >= 1_000) return NUMBER.format(value / 1_000) + "k";
        return NUMBER.format(value);
    }

    private static String compactCoins(double value) {
        return compact(value) + " coins";
    }

    private static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(Math.max(0, millis));
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return hours > 0
                ? String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private String priceAge(MaterialTrackerState state, long now) {
        if (state.lastPriceUpdateEpochMillis <= 0) return "WAITING";
        long seconds = Math.max(
                0, (now - state.lastPriceUpdateEpochMillis) / 1_000L);
        if (seconds < 60) return seconds + "s";
        return (seconds / 60) + "m";
    }
}
