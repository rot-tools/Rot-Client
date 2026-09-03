package fi.rotclient;

import java.util.List;
import java.util.Locale;

/**
 * Card-style Appearance submenu shown from {@code qol.appearance}.
 * Mining HUD colors live under Colors, not a separate Appearance page.
 */
public final class AppearanceLandingPolicy {
    public static final String OPEN_DASHBOARD = "qol.appearance.open_dashboard";
    public static final String OPEN_COLORS = "qol.appearance.open_colors";
    public static final String OPEN_BACKGROUND = "qol.appearance.open_background";
    public static final String OPEN_CHARTS = "qol.appearance.open_charts";
    public static final String OPEN_RESET = "qol.appearance.open_reset";

    public record Card(String actionId, String title, String subtitle) {
    }

    private AppearanceLandingPolicy() {
    }

    public static List<Card> cards() {
        return List.of(
                new Card(OPEN_DASHBOARD, "Dashboard", "Panels, sidebar, headers, and buttons"),
                new Card(OPEN_COLORS, "Colors", "Text, HUD surfaces, and accents"),
                new Card(OPEN_BACKGROUND, "Background", "Optional dashboard background image"),
                new Card(OPEN_CHARTS, "Charts", "Rate sparkline line, fill, and grid"),
                new Card(OPEN_RESET, "Reset", "Restore appearance defaults"));
    }

    public static boolean isAppearanceAction(String settingId) {
        String id = settingId == null ? "" : settingId.trim().toLowerCase(Locale.ROOT);
        return OPEN_DASHBOARD.equals(id)
                || OPEN_COLORS.equals(id)
                || OPEN_BACKGROUND.equals(id)
                || OPEN_CHARTS.equals(id)
                || OPEN_RESET.equals(id);
    }

    public static String sectionId(String actionId) {
        String id = actionId == null ? "" : actionId.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case OPEN_DASHBOARD -> "dashboard";
            case OPEN_COLORS -> "colors";
            case OPEN_BACKGROUND -> "background";
            case OPEN_CHARTS -> "charts";
            case OPEN_RESET -> "reset";
            default -> "";
        };
    }

    public static int[] cardRect(
            int index,
            int contentLeft,
            int listTop,
            int gridWidth) {
        int count = cards().size();
        if (index < 0 || index >= count) {
            return new int[] {0, 0, 0, 0};
        }
        int cols = 2;
        int gap = QolUtilityUiMath.CARD_GAP;
        int cardW = Math.max(1, (gridWidth - gap) / cols);
        int col = index % cols;
        int row = index / cols;
        int x = contentLeft + col * (cardW + gap);
        int y = listTop + 28 + row * (QolUtilityUiMath.CARD_HEIGHT + gap);
        return new int[] {x, y, cardW, QolUtilityUiMath.CARD_HEIGHT};
    }

    public static int hitIndex(
            int mouseX,
            int mouseY,
            int contentLeft,
            int listTop,
            int gridWidth) {
        for (int i = 0; i < cards().size(); i++) {
            int[] rect = cardRect(i, contentLeft, listTop, gridWidth);
            if (mouseX >= rect[0]
                    && mouseY >= rect[1]
                    && mouseX < rect[0] + rect[2]
                    && mouseY < rect[1] + rect[3]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean hitBack(
            int mouseX,
            int mouseY,
            int contentLeft,
            int listTop) {
        return mouseX >= contentLeft
                && mouseY >= listTop
                && mouseX < contentLeft + 120
                && mouseY < listTop + 22;
    }

    public enum LandingHit {
        BACK,
        CARD,
        EMPTY,
        OUTSIDE
    }

    public static LandingHit hitKind(
            int mouseX,
            int mouseY,
            int contentLeft,
            int contentTop,
            int contentWidth,
            int contentBottom,
            int listTop,
            int gridWidth) {
        if (hitBack(mouseX, mouseY, contentLeft, listTop)) {
            return LandingHit.BACK;
        }
        if (hitIndex(mouseX, mouseY, contentLeft, listTop, gridWidth) >= 0) {
            return LandingHit.CARD;
        }
        if (contentWidth > 0
                && mouseX >= contentLeft
                && mouseX < contentLeft + contentWidth
                && mouseY >= contentTop
                && mouseY < contentBottom) {
            return LandingHit.EMPTY;
        }
        return LandingHit.OUTSIDE;
    }
}
