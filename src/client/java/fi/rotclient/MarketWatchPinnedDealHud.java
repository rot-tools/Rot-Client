package fi.rotclient;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class MarketWatchPinnedDealHud {

    private static final int MARGIN =
            10;

    private static final int CARD_WIDTH =
            278;

    private static final int CARD_HEIGHT =
            58;

    private static final int CARD_GAP =
            6;

    private static final long STALE_AFTER_MILLIS =
            15L * 60L * 1000L;

    private MarketWatchPinnedDealHud() {
    }

    static void render(
            GuiGraphicsExtractor graphics,
            Minecraft client) {

        if (graphics == null
                || client == null
                || client.font == null) {

            return;
        }

        List<MarketWatchPinnedDeal> pins =
                MarketWatchPinnedDealStore
                        .all();

        if (pins.isEmpty()) {
            return;
        }

        Font font =
                client.font;

        int x =
                MARGIN;

        int y =
                MARGIN;

        long now =
                System.currentTimeMillis();

        for (MarketWatchPinnedDeal pin
                : pins) {

            if (pin == null) {
                continue;
            }

            drawPin(
                    graphics,
                    font,
                    pin,
                    x,
                    y,
                    now);

            y +=
                    CARD_HEIGHT
                            + CARD_GAP;
        }
    }

    private static void drawPin(
            GuiGraphicsExtractor graphics,
            Font font,
            MarketWatchPinnedDeal pin,
            int x,
            int y,
            long now) {

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                CARD_WIDTH,
                CARD_HEIGHT);

        graphics.fill(
                x,
                y,
                x + 3,
                y + CARD_HEIGHT,
                RotClientTheme.HUD_ACCENT);

        long age =
                Math.max(
                        0L,
                        now
                                - pin.pinnedAtMillis());

        boolean stale =
                age >= STALE_AFTER_MILLIS;

        String state =
                stale
                        ? "PINNED  |  STALE "
                        + ageLabel(
                                age)
                        : "PINNED  |  "
                        + ageLabel(
                                age);

        RotClientUiDraw.text(
                graphics,
                font,
                state,
                x + 10,
                y + 6,
                stale
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.HUD_ACCENT,
                true);

        /*
         * Match the pinned-deals manager: item icon on the left and
         * text aligned beside it.
         */
        var icon =
                pin.market()
                        == MarketWatchOpportunity.Market.BAZAAR
                        ? MarketWatchItemIconResolver
                        .bazaarIcon(
                                pin.itemId())
                        : MarketWatchItemIconResolver
                        .auctionIcon(
                                pin.category(),
                                pin.displayName());

        graphics.item(
                icon,
                x + 10,
                y + 19);

        int textX =
                x + 34;

        int textWidth =
                CARD_WIDTH - 44;

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        pin.displayName(),
                        textWidth),
                textX,
                y + 18,
                RotClientTheme.TEXT,
                true);

        String quantity =
                pin.quantity() > 1L
                        ? "  |  QTY "
                        + compactQuantity(
                                pin.quantity())
                        : "";

        /*
         * Keep the HUD compact enough to fit beside the icon.
         * Quantity is never appended to the item name.
         */
        String action =
                pin.market()
                        == MarketWatchOpportunity.Market.AUCTION_HOUSE
                        ? "AH  BUY "
                        + formatCoins(
                                pin.capitalCoins())
                        + "  >  LIST "
                        + formatCoins(
                                pin.grossReturnCoins())
                        + quantity
                        : "BZ  BUY "
                        + formatCoins(
                                pin.capitalCoins())
                        + "  >  SELL "
                        + formatCoins(
                                pin.grossReturnCoins())
                        + quantity;

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        action,
                        textWidth),
                textX,
                y + 31,
                RotClientTheme.TEXT_MUTED,
                false);

        String result =
                formatSignedCoins(
                        pin.expectedProfitCoins())
                        + "  |  "
                        + String.format(
                        Locale.ROOT,
                        "%.1f%% ROI",
                        pin.roiPercent());

        RotClientUiDraw.text(
                graphics,
                font,
                fit(
                        font,
                        result,
                        textWidth),
                textX,
                y + 44,
                pin.expectedProfitCoins() > 0.0D
                        ? RotClientTheme.SUCCESS
                        : RotClientTheme.TEXT_MUTED,
                true);
    }

    private static String compactQuantity(
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

    private static String ageLabel(
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

    private static String fit(
            Font font,
            String value,
            int maxWidth) {

        if (value == null
                || value.isEmpty()
                || font.width(value)
                <= maxWidth) {

            return value == null
                    ? ""
                    : value;
        }

        String suffix =
                "...";

        int allowed =
                Math.max(
                        0,
                        maxWidth
                                - font.width(
                                suffix));

        String result =
                value;

        while (!result.isEmpty()
                && font.width(result)
                > allowed) {

            result =
                    result.substring(
                            0,
                            result.length() - 1);
        }

        return result
                + suffix;
    }

    private static String formatSignedCoins(
            double value) {

        if (!Double.isFinite(value)) {
            return "0";
        }

        return (value >= 0.0D
                ? "+"
                : "-")
                + formatCoins(
                Math.abs(value));
    }

    private static String formatCoins(
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
                    value / 1_000_000_000.0D);
        }

        if (absolute >= 1_000_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.2fM",
                    value / 1_000_000.0D);
        }

        if (absolute >= 1_000.0D) {
            return String.format(
                    Locale.ROOT,
                    "%.1fK",
                    value / 1_000.0D);
        }

        return String.format(
                Locale.ROOT,
                "%.0f",
                value);
    }
}