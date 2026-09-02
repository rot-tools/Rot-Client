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
import java.util.Deque;
import java.util.Locale;

final class RotClientHud {
    static final int WIDTH = 268;
    private static final int CARD_GAP = 6;
    private static final int MAX_RATE_SAMPLES = 64;

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

    RotClientHud(TrackerConfig config) {
        this.config = config;
    }

    void render(GuiGraphicsExtractor graphics, DeltaTracker ignored) {
        if (!config.enabled) return;

        if (config.selectedSelection().isGemstone()) {
            renderGemstone(
                    graphics);
            return;
        }

        long now = System.currentTimeMillis();
        TrackingTarget target = config.selectedTarget();
        TrackedMaterial primary = target.primaryMaterial();
        long sessionBlocks = RotClientClient.selectedSessionBlocks();
        long sessionBaseDrops =
                RotClientClient.selectedSessionBaseDrops();
        sampleMiningRate(
                sessionBlocks,
                now,
                RotClientClient.isActive(
                        now));

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

        int topHeight = topCardHeight();
        boolean profitCardVisible = hasProfitCard();
        int fullHeight = currentHeight();

        graphics.pose().pushMatrix();
        graphics.pose().translate(config.x, config.y);
        graphics.pose().scale(config.scale, config.scale);

        drawTopCard(
                graphics,
                target,
                sessionBlocks,
                now,
                topHeight,
                activeMillis,
                averagePerBlock,
                observedFortune,
                liveResourcePerHour);
        if (profitCardVisible) {
            drawProfitCard(graphics, target, now,
                    topHeight + CARD_GAP,
                    fullHeight - topHeight - CARD_GAP,
                    sessionValue, unsoldValue, coinsPerHour);
        }

        graphics.pose().popMatrix();
    }

    private void renderGemstone(
            GuiGraphicsExtractor graphics) {
        long now =
                System.currentTimeMillis();

        GemstoneType gemstone =
                config.selectedSelection()
                        .gemstone();

        GemstoneTrackerState state =
                config.gemstoneState(
                        gemstone);

        GemstoneLedger ledger =
                state.sessionLedger();

        long sessionBlocks =
                state.sessionBlocks;

        boolean active =
                state.isActive(
                        now,
                        RotClientClient.PAUSE_AFTER_MILLIS);

        long activeMillis =
                state.currentSessionActiveMillis(
                        now,
                        RotClientClient.PAUSE_AFTER_MILLIS);

        sampleMiningRate(
                sessionBlocks,
                now,
                active);

        long totalItems =
                ledger.totalItemCount();

        long roughEquivalent =
                ledger.totalRoughEquivalent();

        double averagePerBlock =
                sessionBlocks <= 0L
                        ? 0.0
                        : (double) roughEquivalent
                        / sessionBlocks;

        double roughEquivalentPerHour =
                activeMillis <= 0L
                        ? 0.0
                        : roughEquivalent
                        * 3_600_000.0
                        / activeMillis;

        int topHeight =
                gemstoneTopCardHeight();

        int ledgerTop =
                topHeight
                        + CARD_GAP;

        int fullHeight =
                ledgerTop
                        + gemstoneLedgerCardHeight();

        graphics.pose()
                .pushMatrix();

        graphics.pose()
                .translate(
                        config.x,
                        config.y);

        graphics.pose()
                .scale(
                        config.scale,
                        config.scale);

        drawGemstoneTopCard(
                graphics,
                gemstone,
                state,
                sessionBlocks,
                now,
                topHeight,
                activeMillis,
                averagePerBlock,
                roughEquivalent,
                roughEquivalentPerHour,
                active);

        drawGemstoneLedgerCard(
                graphics,
                gemstone,
                ledger,
                ledgerTop,
                fullHeight - ledgerTop,
                totalItems,
                roughEquivalent);

        graphics.pose()
                .popMatrix();
    }

    private void drawGemstoneTopCard(
            GuiGraphicsExtractor graphics,
            GemstoneType gemstone,
            GemstoneTrackerState state,
            long sessionBlocks,
            long now,
            int height,
            long activeMillis,
            double averagePerBlock,
            long roughEquivalent,
            double roughEquivalentPerHour,
            boolean active) {
        drawCard(
                graphics,
                0,
                0,
                WIDTH,
                height);

        boolean showTitle = config.showHudTitle;
        boolean showStatus = config.showHudStatus;
        int headerBottom = showTitle || showStatus ? 31 : 22;
        int cursor = showTitle || showStatus ? 37 : 28;

        if (config.hudShowBackground) {
            roundedFill(
                    graphics,
                    1,
                    1,
                    WIDTH - 1,
                    headerBottom,
                    RotClientTheme.HUD_HEADER);
        }

        if (showTitle) {
            RotClientUiDraw.text(
                    graphics,
                    font(),
                    gemstone.displayName()
                            .toUpperCase(
                                    Locale.ROOT)
                            + " TRACKER",
                    10,
                    7,
                    RotClientTheme.TEXT,
                    true);
        }

        String subtitle =
                config.showActiveTool
                        ? activeGemstoneTool(
                                gemstone)
                        : gemstone.displayName()
                        .toUpperCase(
                                Locale.ROOT)
                        + " | GEMSTONE MINING";

        RotClientUiDraw.text(
                graphics,
                font(),
                subtitle,
                10,
                showTitle ? 19 : 8,
                RotClientTheme.TEXT_DIM,
                false);

        if (showStatus) {
            drawGemstoneStatusPill(
                    graphics,
                    state,
                    now);
        }

        if (config.showArea) {
            cursor = drawAreaRow(graphics, cursor);
        }

        if (config.showBlocks) {
            RotClientUiDraw.text(
                    graphics,
                    font(),
                    "BLOCKS / HOUR",
                    10,
                    cursor + 1,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.text(
                    graphics,
                    font(),
                    compact(
                            smoothedBlocksPerHour),
                    10,
                    cursor + 13,
                    RotClientTheme.TEXT,
                    true);

            String trend =
                    rateTrend();

            int trendColor =
                    trend.startsWith("\u2193")
                            ? RotClientTheme.WARNING
                            : RotClientTheme.SUCCESS;

            RotClientUiDraw.text(
                    graphics,
                    font(),
                    trend,
                    57,
                    cursor + 14,
                    trendColor,
                    false);

            RotClientUiDraw.text(
                    graphics,
                    font(),
                    "SESSION BLOCKS",
                    WIDTH - 92,
                    cursor + 1,
                    RotClientTheme.TEXT_DIM,
                    true);

            drawRight(
                    graphics,
                    compact(
                            sessionBlocks),
                    WIDTH - 10,
                    cursor + 13,
                    RotClientTheme.HUD_ACCENT,
                    true);

            cursor +=
                    28;
        }

        if (config.showRateGraph) {
            drawRateGraph(
                    graphics,
                    10,
                    cursor,
                    WIDTH - 10,
                    cursor + 34);

            cursor +=
                    39;
        }

        graphics.fill(
                1,
                cursor - 3,
                WIDTH - 1,
                cursor + 22,
                RotClientTheme.HUD_PANEL);

        drawStripMetric(
                graphics,
                0,
                3,
                cursor,
                "ROUGH EQ / HOUR",
                compact(
                        roughEquivalentPerHour));

        drawStripMetric(
                graphics,
                1,
                3,
                cursor,
                "AVG / BLOCK",
                NUMBER.format(
                        averagePerBlock));

        drawStripMetric(
                graphics,
                2,
                3,
                cursor,
                "ROUGH EQ",
                compact(
                        roughEquivalent));

        cursor +=
                29;

        long lastBreak =
                state.lastBreakEpochMillis;

        long sinceBreak =
                lastBreak <= 0L
                        ? RotClientClient
                        .PAUSE_AFTER_MILLIS
                        : Math.max(
                                0L,
                                now - lastBreak);

        double remaining =
                active
                        ? 1.0
                        - Math.min(
                                1.0,
                                sinceBreak
                                        / (double) RotClientClient
                                        .PAUSE_AFTER_MILLIS)
                        : 0.0;

        int footerY = cursor;
        if (config.showHudAutoPause) {
            RotClientUiDraw.text(
                    graphics,
                    font(),
                    "AUTO-PAUSE",
                    10,
                    cursor,
                    RotClientTheme.TEXT_DIM,
                    false);

            String pauseValue =
                    active
                            ? Math.max(
                                    0L,
                                    (RotClientClient
                                    .PAUSE_AFTER_MILLIS
                                            - sinceBreak
                                            + 999L)
                                            / 1_000L)
                            + "s"
                            : "PAUSED";

            drawRight(
                    graphics,
                    pauseValue,
                    WIDTH - 10,
                    cursor,
                    RotClientTheme.TEXT_MUTED,
                    false);

            drawProgressBar(
                    graphics,
                    10,
                    cursor + 11,
                    WIDTH - 10,
                    cursor + 16,
                    remaining);

            footerY = cursor + 21;
        }

        if (config.showHudVersion) {
            RotClientUiDraw.text(
                    graphics,
                    font(),
                    RotClientVersionLabel.gemstoneHudFooter(),
                    10,
                    footerY,
                    RotClientTheme.TEXT_DIM,
                    false);
        }

        if (config.showSessionTime) {
            drawRight(
                    graphics,
                    formatDuration(
                            activeMillis),
                    WIDTH - 10,
                    footerY,
                    RotClientTheme.TEXT_MUTED,
                    false);
        }
    }

    private void drawGemstoneLedgerCard(
            GuiGraphicsExtractor graphics,
            GemstoneType gemstone,
            GemstoneLedger ledger,
            int top,
            int height,
            long totalItems,
            long roughEquivalent) {
        drawCard(
                graphics,
                0,
                top,
                WIDTH,
                top + height);

        RotClientUiDraw.text(
                graphics,
                font(),
                "GEMSTONE SESSION",
                10,
                top + 8,
                RotClientTheme.HUD_ACCENT,
                true);

        drawRight(
                graphics,
                gemstone.displayName()
                        .toUpperCase(
                                Locale.ROOT),
                WIDTH - 10,
                top + 8,
                RotClientTheme.TEXT_DIM,
                true);

        RotClientUiDraw.text(
                graphics,
                font(),
                "TIER",
                12,
                top + 22,
                RotClientTheme.TEXT_DIM,
                true);

        drawRight(
                graphics,
                "QUANTITY",
                WIDTH - 92,
                top + 22,
                RotClientTheme.TEXT_DIM,
                true);

        drawRight(
                graphics,
                "ROUGH EQ",
                WIDTH - 10,
                top + 22,
                RotClientTheme.TEXT_DIM,
                true);

        int cursor =
                top + 38;

        for (GemstoneTier tier :
                GemstoneTier.values()) {
            drawGemstoneTierRow(
                    graphics,
                    ledger,
                    tier,
                    cursor);

            cursor +=
                    15;
        }

        graphics.fill(
                10,
                cursor + 1,
                WIDTH - 10,
                cursor + 2,
                RotClientTheme.DIVIDER);

        cursor +=
                10;

        RotClientUiDraw.text(
                graphics,
                font(),
                "TOTAL ITEMS",
                10,
                cursor,
                RotClientTheme.TEXT_MUTED,
                false);

        drawRight(
                graphics,
                compact(
                        totalItems),
                WIDTH - 10,
                cursor,
                RotClientTheme.TEXT,
                true);

        cursor +=
                15;

        RotClientUiDraw.text(
                graphics,
                font(),
                "TOTAL ROUGH EQUIVALENT",
                10,
                cursor,
                RotClientTheme.TEXT,
                false);

        drawRight(
                graphics,
                compact(
                        roughEquivalent),
                WIDTH - 10,
                cursor,
                RotClientTheme.HUD_ACCENT,
                true);
    }

    private void drawGemstoneTierRow(
            GuiGraphicsExtractor graphics,
            GemstoneLedger ledger,
            GemstoneTier tier,
            int y) {
        long quantity =
                ledger.quantity(
                        tier);

        long roughEquivalent =
                Math.multiplyExact(
                        quantity,
                        tier.roughEquivalent());

        RotClientUiDraw.text(
                graphics,
                font(),
                tier.displayName(),
                12,
                y,
                gemstoneTierColor(
                        tier),
                false);

        drawRight(
                graphics,
                compact(
                        quantity),
                WIDTH - 92,
                y,
                RotClientTheme.TEXT,
                false);

        drawRight(
                graphics,
                compact(
                        roughEquivalent),
                WIDTH - 10,
                y,
                RotClientTheme.TEXT_DIM,
                false);
    }

    private void drawGemstoneStatusPill(
            GuiGraphicsExtractor graphics,
            GemstoneTrackerState state,
            long now) {
        boolean active =
                state.isActive(
                        now,
                        RotClientClient.PAUSE_AFTER_MILLIS);

        String status =
                active
                        ? "RUNNING"
                        : (state.sessionBlocks > 0L
                        ? "PAUSED"
                        : "READY");

        int color =
                active
                        ? RotClientTheme.SUCCESS
                        : (state.sessionBlocks > 0L
                        ? RotClientTheme.WARNING
                        : RotClientTheme.TEXT_MUTED);

        int textWidth =
                RotClientFonts.width(
                        font(),
                        status);

        int left =
                WIDTH
                        - textWidth
                        - 18;

        roundedFill(
                graphics,
                left,
                7,
                WIDTH - 8,
                25,
                RotClientTheme.HUD_PANEL_ALT);

        RotClientUiDraw.text(
                graphics,
                font(),
                status,
                left + 5,
                12,
                color,
                true);
    }

    private String activeGemstoneTool(
            GemstoneType gemstone) {
        Minecraft client =
                Minecraft.getInstance();

        String fallback =
                gemstone.displayName()
                        .toUpperCase(
                                Locale.ROOT)
                        + " | GEMSTONE MINING";

        if (
            client.player == null
            || client.player
            .getMainHandItem()
            .isEmpty()
        ) {
            return fallback;
        }

        String name =
                client.player
                        .getMainHandItem()
                        .getHoverName()
                        .getString()
                        .trim();

        String suffix =
                "...";

        int maxWidth =
                145;

        if (client.font.width(
                name) <= maxWidth) {
            return name;
        }

        while (
            !name.isEmpty()
            && client.font.width(
                    name + suffix) > maxWidth
        ) {
            name =
                    name.substring(
                            0,
                            name.length() - 1);
        }

        return name + suffix;
    }

    private int gemstoneTopCardHeight() {
        return HudLayoutMath.gemstoneTopCardHeight(
                config.showBlocks,
                config.showRateGraph,
                config.showArea);
    }

    private static int gemstoneLedgerCardHeight() {
        return HudLayoutMath.gemstoneLedgerCardHeight();
    }

    private static int gemstoneTierColor(
            GemstoneTier tier) {
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

    private void drawTopCard(GuiGraphicsExtractor graphics,
                             TrackingTarget target,
                             long sessionBlocks,
                             long now, int height,
                             long activeMillis, double averagePerBlock,
                             double observedFortune,
                             double liveResourcePerHour) {
        drawCard(graphics, 0, 0, WIDTH, height);
        boolean showTitle = config.showHudTitle;
        boolean showStatus = config.showHudStatus;
        boolean showTool = config.showActiveTool;
        int headerBottom = showTitle || showStatus || showTool ? 31 : 22;
        int cursor = showTitle || showStatus || showTool ? 37 : 28;
        if (config.hudShowBackground) {
            roundedFill(graphics, 1, 1, WIDTH - 1, headerBottom, RotClientTheme.HUD_HEADER);
        }

        if (showTitle) {
            RotClientUiDraw.text(graphics, font(),
                    target.displayName().toUpperCase(Locale.ROOT) + " TRACKER",
                    10, 7, RotClientTheme.HUD_TITLE, true);
        }
        if (showTool) {
            int subtitleY = showTitle ? 19 : 8;
            RotClientUiDraw.text(graphics, font(), activeTool(target), 10, subtitleY,
                    RotClientTheme.HUD_TEXT_DIM, false);
        }
        if (showStatus) {
            drawStatusPill(graphics, sessionBlocks, now);
        }

        if (config.showArea) {
            cursor = drawAreaRow(graphics, cursor);
        }

        if (config.showBlocks) {
            RotClientUiDraw.text(graphics, font(), "Blocks / Hour", 10, cursor + 1, RotClientTheme.HUD_TEXT_DIM, true);
            RotClientUiDraw.text(graphics, font(), compact(smoothedBlocksPerHour), 10, cursor + 13, RotClientTheme.HUD_TEXT, true);
            String trend = rateTrend();
            int trendColor = trend.startsWith("↓") ? RotClientTheme.WARNING : RotClientTheme.SUCCESS;
            RotClientUiDraw.text(graphics, font(), trend, 57, cursor + 14, trendColor, false);
            RotClientUiDraw.text(graphics, font(), "Session total", WIDTH - 83, cursor + 1, RotClientTheme.HUD_TEXT_DIM, true);
            drawRight(graphics, compact(sessionBlocks),
                    WIDTH - 10, cursor + 13, RotClientTheme.HUD_ACCENT, true);
            cursor += 28;
        }

        if (config.showRateGraph) {
            drawRateGraph(graphics, 10, cursor, WIDTH - 10, cursor + 34);
            cursor += 39;
        }

        cursor = drawMetricStrip(
                graphics, target, cursor, liveResourcePerHour,
                averagePerBlock, observedFortune);

        int activityTop = cursor;
        long lastBreak = RotClientClient.lastSelectedBreakEpochMillis();
        long sinceBreak = lastBreak <= 0
                ? RotClientClient.PAUSE_AFTER_MILLIS
                : Math.max(0, now - lastBreak);
        double remaining = RotClientClient.isActive(now)
                ? 1.0 - Math.min(1.0, sinceBreak / (double) RotClientClient.PAUSE_AFTER_MILLIS)
                : 0;
        int footerY = activityTop;
        if (config.showHudAutoPause) {
            RotClientUiDraw.text(graphics, font(), "AUTO-PAUSE", 10, activityTop, RotClientTheme.TEXT_DIM, false);
            String pauseValue = RotClientClient.isActive(now)
                    ? Math.max(0, (RotClientClient.PAUSE_AFTER_MILLIS - sinceBreak + 999) / 1000) + "s"
                    : "PAUSED";
            drawRight(graphics, pauseValue, WIDTH - 10, activityTop, RotClientTheme.TEXT_MUTED, false);
            drawProgressBar(graphics, 10, activityTop + 11,
                    WIDTH - 10, activityTop + 16, remaining);
            footerY = activityTop + 21;
        }
        if (config.showHudVersion) {
            RotClientUiDraw.text(graphics, font(), RotClientVersionLabel.brandLabel(),
                    10, footerY, RotClientTheme.TEXT_DIM, false);
        }
        if (config.showSessionTime) {
            drawRight(graphics, "◷ " + formatDuration(activeMillis),
                    WIDTH - 10, footerY, RotClientTheme.TEXT_MUTED, false);
        }
    }

    private void drawProfitCard(GuiGraphicsExtractor graphics,
                                TrackingTarget target,
                                long now, int top, int height,
                                double sessionValue, double unsoldValue, double coinsPerHour) {
        drawCard(graphics, 0, top, WIDTH, top + height);
        RotClientUiDraw.text(graphics, font(), "SESSION VALUE  ·  ESTIMATE",
                10, top + 8, RotClientTheme.HUD_ACCENT, true);

        MiningHudOtherSummary otherSummary =
                RotClientClient.hudOtherMinedSummary();
        boolean itemRows =
                config.showRawMaterial || config.showEnchantedMaterial;
        double targetValue = 0.0;
        int cursor;
        if (itemRows) {
            int headerY = top + 22;
            if (config.showTargetHeading) {
                RotClientUiDraw.text(graphics, font(), "TARGET", 10, headerY, RotClientTheme.TEXT_MUTED, true);
                headerY += 14;
            }
            RotClientUiDraw.text(graphics, font(), "TYPE", 12, headerY, RotClientTheme.TEXT_DIM, true);
            RotClientUiDraw.text(graphics, font(), "ITEM", 39, headerY, RotClientTheme.TEXT_DIM, true);
            RotClientUiDraw.text(graphics, font(), "QTY", WIDTH - 90, headerY, RotClientTheme.TEXT_DIM, true);
            drawRight(graphics, "EST. VALUE",
                    WIDTH - 10, headerY, RotClientTheme.TEXT_DIM, true);
            cursor = headerY + 16;

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
                            material.enchantedItemName(), amount, value, RotClientTheme.HUD_ACCENT);
                    cursor += 18;
                }
                if (config.showRawMaterial) {
                    long amount = RotClientClient.currentSessionRawItems(
                            material);
                    double value = RotClientClient.estimateNetValue(
                            material, amount, 0);
                    targetValue += value;
                    drawMaterialItemRow(
                            graphics, material, cursor,
                            material.rawItemName(), amount, value, RotClientTheme.CHART_LINE);
                    cursor += 18;
                }
            }
        } else {
            cursor = top + 26;
            targetValue = sessionValue;
        }

        if (config.showOtherSection) {
            RotClientUiDraw.text(graphics, font(), "OTHERS", 10, cursor, RotClientTheme.TEXT_MUTED, true);
            cursor += 14;
            cursor = drawOthersSummaryRow(graphics, cursor, otherSummary);
        }

        boolean anyValueLine = config.showTargetValue
                || config.showOtherValue
                || config.showTotalMinedValue;
        if (config.showOtherSection || anyValueLine) {
            graphics.fill(10, cursor, WIDTH - 10, cursor + 1, RotClientTheme.DIVIDER);
            cursor += 8;
        }

        double otherNet = otherSummary.analyticsActive()
                ? otherSummary.resolvedNetValue(config.bazaarTaxPercent)
                : 0.0;
        if (config.showTargetValue) {
            RotClientUiDraw.text(graphics, font(), "Target Value", 10, cursor, RotClientTheme.TEXT, false);
            drawRight(graphics, compactCoins(targetValue),
                    WIDTH - 10, cursor - 1, RotClientTheme.HUD_ACCENT, true);
            cursor += 16;
        }

        if (config.showOtherValue) {
            RotClientUiDraw.text(graphics, font(), "Others Value", 10, cursor, RotClientTheme.TEXT, false);
            if (otherSummary.analyticsActive()) {
                drawRight(
                        graphics,
                        otherSummary.valueDisplayCompact(compact(otherNet))
                                + " coins",
                        WIDTH - 10,
                        cursor - 1,
                        otherSummary.hasUnresolved()
                                ? RotClientTheme.WARNING
                                : RotClientTheme.TEXT,
                        true);
            } else {
                drawRight(graphics, "—", WIDTH - 10, cursor - 1, RotClientTheme.TEXT_MUTED, false);
            }
            cursor += 16;
        }

        if (config.showTotalMinedValue) {
            RotClientUiDraw.text(graphics, font(), "Total Mined Value", 10, cursor, RotClientTheme.TEXT, false);
            if (otherSummary.analyticsActive()) {
                drawRight(graphics, compactCoins(targetValue + otherNet),
                        WIDTH - 10, cursor - 1, RotClientTheme.WARNING, true);
            } else {
                drawRight(graphics, compactCoins(targetValue),
                        WIDTH - 10, cursor - 1, RotClientTheme.WARNING, true);
            }
            cursor += 16;
        }

        if (config.showSessionProfit) {
            RotClientUiDraw.text(graphics, font(), "Session Value (Net)", 10, cursor, RotClientTheme.TEXT, false);
            drawRight(graphics, compactCoins(sessionValue),
                    WIDTH - 10, cursor - 1, RotClientTheme.WARNING, true);
            cursor += 16;
        }
        if (config.showCoinsPerHour) {
            RotClientUiDraw.text(graphics, font(), "Coins / Hour (Est.)", 10, cursor, RotClientTheme.TEXT, false);
            drawRight(graphics, compactCoins(coinsPerHour),
                    WIDTH - 10, cursor, RotClientTheme.HUD_ACCENT, false);
            cursor += 15;
        }
        if (config.showUnsoldValue) {
            RotClientUiDraw.text(graphics, font(), "Unsold (Est. Net)", 10, cursor, RotClientTheme.TEXT_MUTED, false);
            drawRight(graphics, compactCoins(unsoldValue),
                    WIDTH - 10, cursor, RotClientTheme.TEXT, false);
            cursor += 15;
        }
        if (config.showBazaarPrices) {
            for (TrackedMaterial material : target.materials()) {
                MaterialTrackerState state = config.state(material);
                String prices = state.lastRawPrice > 0
                        && state.lastEnchantedPrice > 0
                        ? material.displayName().toUpperCase(Locale.ROOT)
                                + " BZ " + NUMBER.format(state.lastRawPrice)
                                + " / "
                                + NUMBER.format(state.lastEnchantedPrice)
                                + "  · " + priceAge(state, now)
                        : material.displayName().toUpperCase(Locale.ROOT)
                                + " BAZAAR: CONNECTING...";
                RotClientUiDraw.text(
                        graphics,
                        font(), prices, 10, cursor, RotClientTheme.TEXT_DIM, false);
                cursor += 13;
            }
            drawRight(
                    graphics,
                    "TAX " + NUMBER.format(config.bazaarTaxPercent) + "%",
                    WIDTH - 10,
                    cursor - 13,
                    RotClientTheme.TEXT_DIM,
                    false);
        }
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
        if (!otherSummary.analyticsActive()) {
            RotClientUiDraw.text(
                    graphics,
                    font(),
                    "No others yet",
                    39,
                    y,
                    RotClientTheme.TEXT_MUTED,
                    false);
            drawRight(graphics, "—", WIDTH - 10, y, RotClientTheme.TEXT_MUTED, false);
            return y + 18;
        }

        if (otherSummary.isEmpty()) {
            RotClientUiDraw.text(graphics, font(), label, 39, y, RotClientTheme.TEXT_MUTED, false);
            drawRight(graphics, "×0", WIDTH - 84, y, RotClientTheme.TEXT_DIM, false);
            drawRight(graphics, "0", WIDTH - 10, y, RotClientTheme.TEXT_DIM, false);
            return y + 18;
        }

        double otherNet = otherSummary.resolvedNetValue(config.bazaarTaxPercent);
        RotClientUiDraw.text(graphics, font(), label, 39, y, RotClientTheme.TEXT, false);
        drawRight(graphics, "×" + compact(otherSummary.totalQuantity()),
                WIDTH - 84, y, RotClientTheme.TEXT_DIM, false);
        drawRight(
                graphics,
                otherSummary.valueDisplayCompact(compact(otherNet)),
                WIDTH - 10,
                y,
                otherSummary.hasUnresolved()
                        ? RotClientTheme.WARNING
                        : RotClientTheme.CHART_LINE,
                false);
        return y + 18;
    }

    private void drawMaterialItemRow(GuiGraphicsExtractor graphics,
                                     TrackedMaterial material,
                                     int y, String item,
                                     long amount, double value,
                                     int valueColor) {
        ItemStack icon = new ItemStack(material.iconItem());
        graphics.item(icon, 11, y - 5);
        RotClientUiDraw.text(graphics, font(), item, 39, y, RotClientTheme.TEXT, false);
        drawRight(graphics, "×" + compact(amount),
                WIDTH - 84, y, RotClientTheme.TEXT_DIM, false);
        drawRight(graphics, compact(value), WIDTH - 10, y, valueColor, false);
    }

    private int drawMetricStrip(GuiGraphicsExtractor graphics,
                                TrackingTarget target,
                                int top,
                                double liveResourcePerHour,
                                double averagePerBlock,
                                double observedFortune) {
        int metricCount = (config.showMaterialPerHour ? 1 : 0)
                + (config.showDropAndFortune ? 2 : 0);
        if (metricCount == 0) return top;

        graphics.fill(1, top - 3, WIDTH - 1, top + 22, RotClientTheme.HUD_PANEL);
        int index = 0;
        if (config.showMaterialPerHour) {
            drawStripMetric(graphics, index++, metricCount, top,
                    (target.isCombined()
                            ? "RESOURCES"
                            : target.primaryMaterial().displayName()
                                    .toUpperCase(Locale.ROOT))
                            + " / HOUR",
                    compact(liveResourcePerHour));
        }
        if (config.showDropAndFortune) {
            drawStripMetric(graphics, index++, metricCount, top,
                    "AVG DROP", NUMBER.format(averagePerBlock));
            drawStripMetric(graphics, index, metricCount, top,
                    "FORTUNE", "~" + compact(observedFortune));
        }
        return top + 29;
    }

    private void drawStripMetric(GuiGraphicsExtractor graphics, int index, int count,
                                 int top, String label, String value) {
        int left = 1 + index * (WIDTH - 2) / count;
        if (index > 0) {
            graphics.fill(left, top - 3, left + 1, top + 22, RotClientTheme.DIVIDER);
        }
        RotClientUiDraw.text(graphics, font(), label, left + 7, top, RotClientTheme.TEXT_DIM, true);
        RotClientUiDraw.text(graphics, font(), value, left + 7, top + 11, RotClientTheme.HUD_ACCENT, true);
    }

    private void drawStatusPill(GuiGraphicsExtractor graphics,
                                long sessionBlocks,
                                long now) {
        boolean active = RotClientClient.isActive(now);
        String status = active
                ? "● RUNNING"
                : (sessionBlocks > 0 ? "● PAUSED" : "● READY");
        int color = active
                ? RotClientTheme.SUCCESS
                : (sessionBlocks > 0 ? RotClientTheme.WARNING : RotClientTheme.TEXT_MUTED);
        int textWidth = RotClientFonts.width(font(), status);
        int left = WIDTH - textWidth - 18;
        roundedFill(graphics, left, 7, WIDTH - 8, 25, RotClientTheme.HUD_PANEL_ALT);
        RotClientUiDraw.text(graphics, font(), status, left + 5, 12, color, true);
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
        roundedFill(graphics, left, top, right, bottom, RotClientTheme.TOGGLE_OFF);
        int filled = (int) Math.round((right - left) * Mth.clamp(progress, 0, 1));
        if (filled > 0) roundedFill(graphics, left, top, left + filled, bottom, RotClientTheme.HUD_ACCENT);
    }

    private void drawCard(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom) {
        if (!config.hudShowBackground) {
            if (editorOpen) {
                roundedOutline(graphics, left, top, right, bottom, RotClientTheme.HUD_ACCENT);
            }
            return;
        }
        roundedFill(graphics, left + 3, top + 4, right + 3, bottom + 4, RotClientTheme.SHADOW);
        roundedFill(graphics, left, top, right, bottom, RotClientTheme.HUD_BACKGROUND);
        roundedOutline(graphics, left, top, right, bottom, RotClientTheme.HUD_BORDER);
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

    private String rateTrend() {
        if (rateHistory.size() < 4) return "• gathering live data";
        double total = 0;
        int count = 0;
        for (double value : rateHistory) {
            total += value;
            count++;
        }
        double average = count == 0 ? 0 : total / count;
        if (average < 1) return "• waiting for mining";
        double percent = (smoothedBlocksPerHour / average - 1.0) * 100.0;
        String arrow = percent >= 0 ? "↑ " : "↓ ";
        return arrow + NUMBER.format(Math.abs(percent)) + "% vs 1m avg";
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
        if (config.selectedSelection().isGemstone()) {
            return HudLayoutMath.gemstoneHudHeight(
                    config.showBlocks,
                    config.showRateGraph,
                    config.showArea);
        }

        int top =
                topCardHeight();

        return hasProfitCard()
                ? top
                + CARD_GAP
                + profitCardHeight()
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
        return mouseX >= config.x && mouseX <= config.x + WIDTH * config.scale
                && mouseY >= config.y && mouseY <= config.y + currentHeight() * config.scale;
    }

    private String activeTool(TrackingTarget target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.getMainHandItem().isEmpty()) {
            return target.displayName().toUpperCase(Locale.ROOT)
                    + " | MINING";
        }
        String name = client.player.getMainHandItem().getHoverName().getString().trim();
        String suffix = "...";
        int maxWidth = 145;
        if (client.font.width(name) <= maxWidth) return name;
        while (!name.isEmpty() && client.font.width(name + suffix) > maxWidth) {
            name = name.substring(0, name.length() - 1);
        }
        return name + suffix;
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    private int drawAreaRow(GuiGraphicsExtractor graphics, int cursor) {
        RotClientUiDraw.text(graphics, font(), "AREA", 10, cursor, RotClientTheme.HUD_TEXT_DIM, true);
        String value = fitHudText(hudLocationDisplay(), WIDTH - 52);
        drawRight(graphics, value, WIDTH - 10, cursor, RotClientTheme.HUD_TEXT, false);
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
