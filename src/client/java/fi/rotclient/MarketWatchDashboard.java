package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;

/**
 * Top-level Market Watch workspace.
 *
 * Market Watch and Profit Finder behave like two browser-level tabs.
 * Each child dashboard owns and preserves its own navigation state.
 */
final class MarketWatchDashboard {

    private enum Workspace {
        MARKET_WATCH,
        OPPORTUNITIES
    }

    private static final int PRIMARY_TAB_WIDTH =
            126;

    private static final int OPPORTUNITY_TAB_WIDTH =
            138;

    private static final int PRIMARY_TAB_GAP =
            8;

    private static final int BODY_GAP =
            10;

    private static final int INFO_BUTTON_WIDTH =
            28;

    private static final int INFO_POPUP_WIDTH =
            700;

    private static final int INFO_POPUP_HEIGHT =
            300;

    private boolean infoPopupOpen;
    private Workspace workspace =
            Workspace.MARKET_WATCH;

    private final MarketWatchWatchlistDashboard watchlist =
            new MarketWatchWatchlistDashboard();

    private final MarketWatchOpportunitiesDashboard opportunities =
            new MarketWatchOpportunitiesDashboard();

    void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY) {

        drawWorkspaceTabs(
                graphics,
                font,
                left,
                top,
                right,
                mouseX,
                mouseY);

        drawInfoButton(
                graphics,
                font,
                right,
                top,
                mouseX,
                mouseY);

        int bodyTop =
                bodyTop(
                        top);

        if (workspace
                == Workspace.OPPORTUNITIES) {

            opportunities.draw(
                    graphics,
                    font,
                    left,
                    bodyTop,
                    right,
                    bottom,
                    mouseX,
                    mouseY);

        } else {

            watchlist.draw(
                    graphics,
                    font,
                    left,
                    bodyTop,
                    right,
                    bottom,
                    mouseX,
                    mouseY);
        }

        if (infoPopupOpen) {
            drawInfoPopup(
                    graphics,
                    font,
                    left,
                    top,
                    right,
                    bottom,
                    mouseX,
                    mouseY);
        }
    }
    private void drawInfoButton(
            GuiGraphicsExtractor graphics,
            Font font,
            int right,
            int top,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                infoButtonX(
                        right),
                top,
                INFO_BUTTON_WIDTH,
                "i",
                infoPopupOpen,
                true);
    }

    private void drawInfoPopup(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY) {

        int popupWidth =
                Math.min(
                        INFO_POPUP_WIDTH,
                        Math.max(
                                360,
                                right - left - 40));

        int popupHeight =
                INFO_POPUP_HEIGHT;

        int x =
                left
                        + Math.max(
                        0,
                        (right
                                - left
                                - popupWidth)
                                / 2);

        int y =
                bodyTop(
                        top)
                        + 8;

        graphics.fill(
                left,
                bodyTop(
                        top),
                right,
                bottom,
                0x99000000);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                popupWidth,
                popupHeight);

        graphics.fill(
                x,
                y,
                x + 4,
                y + popupHeight,
                RotClientTheme.HUD_ACCENT);

        String title =
                workspace
                        == Workspace.OPPORTUNITIES
                        ? "PROFIT FINDER GUIDE"
                        : "MARKET WATCH GUIDE";

        String subtitle =
                workspace
                        == Workspace.OPPORTUNITIES
                        ? "See a deal, open it, then follow the buy and sell instructions."
                        : "Watch the market for conditions you care about and get an alert when they happen.";

        RotClientUiDraw.text(
                graphics,
                font,
                title,
                x + 18,
                y + 13,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                fitInfoText(
                        font,
                        subtitle,
                        popupWidth - 76),
                x + 18,
                y + 29);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + popupWidth - 38,
                y + 9,
                28,
                "X",
                false,
                true);

        int innerX =
                x + 16;

        int gap =
                10;

        int innerWidth =
                popupWidth - 32;

        int cardWidth =
                (innerWidth - gap) / 2;

        int rightX =
                innerX
                        + cardWidth
                        + gap;

        int rowOne =
                y + 54;

        int rowTwo =
                rowOne + 64;

        int rowThree =
                rowTwo + 64;

        if (workspace
                == Workspace.OPPORTUNITIES) {

            drawInfoFeature(
                    graphics,
                    font,
                    innerX,
                    rowOne,
                    cardWidth,
                    "1",
                    "FIND A DEAL",
                    "Set your trading budget and choose the market.",
                    "Use filters or sorting if you want fewer results.");

            drawInfoFeature(
                    graphics,
                    font,
                    rightX,
                    rowOne,
                    cardWidth,
                    "2",
                    "OPEN A CARD",
                    "Click a deal to see the exact steps.",
                    "The expanded card tells you what to buy.");

            drawInfoFeature(
                    graphics,
                    font,
                    innerX,
                    rowTwo,
                    cardWidth,
                    "3",
                    "BUY",
                    "Follow the shown quantity and maximum cost.",
                    "For AH, use the seller to identify the listing.");

            drawInfoFeature(
                    graphics,
                    font,
                    rightX,
                    rowTwo,
                    cardWidth,
                    "4",
                    "SELL",
                    "Use the shown target price after buying.",
                    "Profit already includes estimated trading costs.");

            drawInfoFeature(
                    graphics,
                    font,
                    innerX,
                    rowThree,
                    cardWidth,
                    "AH",
                    "AUCTION HOUSE",
                    "Buy the exact BIN listing shown by the deal.",
                    "Then list the bought items at the target price.");

            drawInfoFeature(
                    graphics,
                    font,
                    rightX,
                    rowThree,
                    cardWidth,
                    "BZ",
                    "BAZAAR",
                    "Create the shown buy order first.",
                    "After it fills, create the shown sell order.");

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Tip: PIN DEAL keeps up to 3 price targets available while you trade.",
                    x + 16,
                    y + popupHeight - 18);

        } else {

            drawInfoFeature(
                    graphics,
                    font,
                    innerX,
                    rowOne,
                    cardWidth,
                    "1",
                    "CREATE A WATCH",
                    "Choose the item you want Market Watch to follow.",
                    "Select Auction House or Bazaar when creating it.");

            drawInfoFeature(
                    graphics,
                    font,
                    rightX,
                    rowOne,
                    cardWidth,
                    "2",
                    "SET A CONDITION",
                    "Choose the price or market condition you want.",
                    "The watch waits until that condition is met.");

            drawInfoFeature(
                    graphics,
                    font,
                    innerX,
                    rowTwo,
                    cardWidth,
                    "3",
                    "LEAVE IT RUNNING",
                    "Enabled watches continue checking market updates.",
                    "You do not need to keep this screen open.");

            drawInfoFeature(
                    graphics,
                    font,
                    rightX,
                    rowTwo,
                    cardWidth,
                    "4",
                    "GET AN ALERT",
                    "When the condition matches, an alert appears.",
                    "Recent matches can also be reviewed in Alerts.");

            drawInfoFeature(
                    graphics,
                    font,
                    innerX,
                    rowThree,
                    cardWidth,
                    "AH",
                    "AUCTION HOUSE",
                    "Watches live BIN listings for your chosen item.",
                    "Useful when waiting for a particular price.");

            drawInfoFeature(
                    graphics,
                    font,
                    rightX,
                    rowThree,
                    cardWidth,
                    "BZ",
                    "BAZAAR",
                    "Watches live Bazaar prices and spreads.",
                    "Useful for price or margin based conditions.");

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Market Watch only informs you. Buying and selling always stays manual.",
                    x + 16,
                    y + popupHeight - 18);
        }
    }

    private void drawInfoFeature(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            String badge,
            String title,
            String lineOne,
            String lineTwo) {

        int height =
                58;

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x,
                y,
                width,
                height);

        graphics.fill(
                x,
                y + 7,
                x + 3,
                y + height - 7,
                RotClientTheme.HUD_ACCENT);

        int badgeSize =
                24;

        int badgeX =
                x + 9;

        int badgeY =
                y + 8;

        RotClientUiDraw.drawElevatedCard(
                graphics,
                badgeX,
                badgeY,
                badgeSize,
                badgeSize);

        int badgeTextX =
                badgeX
                        + Math.max(
                        0,
                        (badgeSize
                                - font.width(
                                badge))
                                / 2);

        RotClientUiDraw.text(
                graphics,
                font,
                badge,
                badgeTextX,
                badgeY + 8,
                RotClientTheme.HUD_ACCENT,
                true);

        int textX =
                x + 42;

        int textWidth =
                Math.max(
                        1,
                        width - 51);

        RotClientUiDraw.text(
                graphics,
                font,
                fitInfoText(
                        font,
                        title,
                        textWidth),
                textX,
                y + 8,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                fitInfoText(
                        font,
                        lineOne,
                        textWidth),
                textX,
                y + 25);

        RotClientUiDraw.helpText(
                graphics,
                font,
                fitInfoText(
                        font,
                        lineTwo,
                        textWidth),
                textX,
                y + 39);
    }

    private static String fitInfoText(
            Font font,
            String text,
            int width) {

        if (text == null) {
            return "";
        }

        if (text.isBlank()) {
            return "";
        }

        if (font.width(
                text) <= width) {

            return text;
        }

        String suffix =
                "...";

        int end =
                text.length();

        while (end > 0
                && font.width(
                text.substring(
                        0,
                        end)
                        + suffix) > width) {

            end--;
        }

        return text.substring(
                0,
                end)
                + suffix;
    }

    private static int infoButtonX(
            int right) {

        return right
                - INFO_BUTTON_WIDTH;
    }

    private static int infoPopupX(
            int left,
            int right) {

        int width =
                Math.min(
                        INFO_POPUP_WIDTH,
                        Math.max(
                                360,
                                right - left - 40));

        return left
                + Math.max(
                0,
                (right
                        - left
                        - width)
                        / 2);
    }

    private static int infoPopupY(
            int top) {

        return bodyTop(
                top)
                + 8;
    }

    private static int infoPopupWidth(
            int left,
            int right) {

        return Math.min(
                INFO_POPUP_WIDTH,
                Math.max(
                        360,
                        right - left - 40));
    }

    private void drawWorkspaceTabs(
            GuiGraphicsExtractor graphics,
            Font font,
            int left,
            int top,
            int right,
            int mouseX,
            int mouseY) {

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                left,
                top,
                PRIMARY_TAB_WIDTH,
                "MARKET WATCH",
                workspace
                        == Workspace.MARKET_WATCH,
                true);

        int opportunityX =
                left
                        + PRIMARY_TAB_WIDTH
                        + PRIMARY_TAB_GAP;

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                opportunityX,
                top,
                OPPORTUNITY_TAB_WIDTH,
                "PROFIT FINDER",
                workspace
                        == Workspace.OPPORTUNITIES,
                true);

        int dividerY =
                top
                        + RotClientUiDraw.BUTTON_HEIGHT
                        + 4;

        graphics.fill(
                left,
                dividerY,
                right,
                dividerY + 1,
                RotClientTheme.DIVIDER);
    }

    boolean mouseClicked(
            int button,
            double mouseX,
            double mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        int mx =
                (int) Math.round(
                        mouseX);

        int my =
                (int) Math.round(
                        mouseY);

        if (infoPopupOpen
                && button
                != GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            return true;
        }

        if (button
                == GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            if (RotClientUiDraw.inside(
                    mx,
                    my,
                    infoButtonX(
                            right),
                    top,
                    INFO_BUTTON_WIDTH,
                    RotClientUiDraw.BUTTON_HEIGHT)) {

                infoPopupOpen =
                        !infoPopupOpen;

                return true;
            }

            if (infoPopupOpen) {

                int popupX =
                        infoPopupX(
                                left,
                                right);

                int popupY =
                        infoPopupY(
                                top);

                int popupWidth =
                        infoPopupWidth(
                                left,
                                right);

                int closeX =
                        popupX
                                + popupWidth
                                - 38;

                int closeY =
                        popupY + 9;

                if (RotClientUiDraw.inside(
                        mx,
                        my,
                        closeX,
                        closeY,
                        28,
                        RotClientUiDraw.BUTTON_HEIGHT)) {

                    infoPopupOpen =
                            false;

                    return true;
                }

                if (!RotClientUiDraw.inside(
                        mx,
                        my,
                        popupX,
                        popupY,
                        popupWidth,
                        INFO_POPUP_HEIGHT)) {

                    infoPopupOpen =
                            false;
                }

                return true;
            }

            if (RotClientUiDraw.inside(
                    mx,
                    my,
                    left,
                    top,
                    PRIMARY_TAB_WIDTH,
                    RotClientUiDraw.BUTTON_HEIGHT)) {

                workspace =
                        Workspace.MARKET_WATCH;

                return true;
            }

            int opportunityX =
                    left
                            + PRIMARY_TAB_WIDTH
                            + PRIMARY_TAB_GAP;

            if (RotClientUiDraw.inside(
                    mx,
                    my,
                    opportunityX,
                    top,
                    OPPORTUNITY_TAB_WIDTH,
                    RotClientUiDraw.BUTTON_HEIGHT)) {

                workspace =
                        Workspace.OPPORTUNITIES;

                return true;
            }
        }

        int bodyTop =
                bodyTop(
                        top);

        if (workspace
                == Workspace.OPPORTUNITIES) {

            return opportunities.mouseClicked(
                    button,
                    mouseX,
                    mouseY,
                    left,
                    bodyTop,
                    right,
                    bottom);
        }

        return watchlist.mouseClicked(
                button,
                mouseX,
                mouseY,
                left,
                bodyTop,
                right,
                bottom);
    }
    boolean mouseDragged(
            int mouseX,
            int mouseY,
            int left,
            int top,
            int right,
            int bottom) {

        if (infoPopupOpen) {
            return true;
        }

        int bodyTop =
                bodyTop(
                        top);

        if (workspace
                == Workspace.OPPORTUNITIES) {

            return opportunities.mouseDragged(
                    mouseX,
                    mouseY,
                    left,
                    bodyTop,
                    right,
                    bottom);
        }

        return watchlist.mouseDragged(
                mouseX,
                mouseY,
                left,
                bodyTop,
                right,
                bottom);
    }
    boolean mouseReleased() {

        if (infoPopupOpen) {
            return true;
        }

        if (workspace
                == Workspace.OPPORTUNITIES) {

            return opportunities.mouseReleased();
        }

        return watchlist.mouseReleased();
    }
    boolean mouseScrolled(
            int mouseX,
            int mouseY,
            double verticalAmount,
            int left,
            int top,
            int right,
            int bottom) {

        if (infoPopupOpen) {
            return true;
        }

        int bodyTop =
                bodyTop(
                        top);

        if (workspace
                == Workspace.OPPORTUNITIES) {

            return opportunities.mouseScrolled(
                    mouseX,
                    mouseY,
                    verticalAmount,
                    left,
                    bodyTop,
                    right,
                    bottom);
        }

        return watchlist.mouseScrolled(
                mouseX,
                mouseY,
                verticalAmount,
                left,
                bodyTop,
                right,
                bottom);
    }
    boolean captureChar(
            String incoming,
            boolean allowed) {

        if (infoPopupOpen) {
            return true;
        }

        if (workspace
                == Workspace.OPPORTUNITIES) {

            return opportunities.captureChar(
                    incoming,
                    allowed);
        }

        return watchlist.captureChar(
                incoming,
                allowed);
    }
    boolean captureKey(
            int key) {

        if (infoPopupOpen) {

            if (key == GLFW.GLFW_KEY_ESCAPE) {
                infoPopupOpen =
                        false;
            }

            return true;
        }

        if (workspace
                == Workspace.OPPORTUNITIES) {

            return opportunities.captureKey(
                    key);
        }

        return watchlist.captureKey(
                key);
    }
    private static int bodyTop(
            int top) {

        return top
                + RotClientUiDraw.BUTTON_HEIGHT
                + BODY_GAP;
    }
}