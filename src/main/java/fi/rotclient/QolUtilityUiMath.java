package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure layout/hit-test helpers for the QoL utility dashboard cards + drawer.
 */
public final class QolUtilityUiMath {
    public enum CardAction {
        NONE,
        OPEN_SETTINGS,
        TOGGLE,
        OPEN_HUD_SETTINGS
    }

    public enum PageFilter {
        ALL,
        ENABLED,
        CHEAT
    }

    public static final int CARD_HEIGHT = 112;
    public static final int CARD_GAP = 14;
    public static final int GROUP_HEADER_HEIGHT = 26;
    public static final int DRAWER_WIDTH = 440;
    public static final int DRAWER_MIN_WIDTH = 380;
    public static final int SETTINGS_BUTTON_WIDTH = 88;
    public static final int SETTINGS_BUTTON_HEIGHT = 24;
    public static final int TOGGLE_WIDTH = 44;
    public static final int TOGGLE_HEIGHT = 20;
    public static final int SQUARE_LATCH_SIZE = 20;
    public static final int CARD_PAD = 14;
    public static final int FOOTER_HEIGHT = 28;
    public static final int MODULE_BLOCK_WIDTH = 92;
    public static final int HUD_CONTROL_WIDTH = 78;
    public static final int HUD_MENU_WIDTH = 248;
    public static final int HUD_MENU_ROW_HEIGHT = 28;
    public static final int HUD_MENU_EDIT_WIDTH = 52;
    public static final int HUD_MENU_SEARCH_HEIGHT = OverflowListPolicy.SEARCH_HEIGHT;
    public static final int DRAWER_ROW_HEIGHT = 44;
    public static final int DRAWER_CONTROL_RESERVE = 64;
    public static final int CHEAT_BADGE_GAP = 10;
    public static final int STATUS_BADGE_HEIGHT = 14;
    public static final int STATUS_BADGE_PAD_X = 6;
    public static final int STATUS_BADGE_LEFT = 14;
    public static final int STATUS_BADGE_TOP = 6;
    public static final int DRAWER_SECTION_HEIGHT = 26;
    public static final int GRID_MIN_COLUMN_WIDTH = 320;
    public static final int NUMBER_CONTROL_WIDTH = 100;
    public static final int NUMBER_BUTTON_WIDTH = 22;
    public static final int SLIDER_ROW_HEIGHT = 44;
    public static final int SLIDER_TRACK_INSET = 8;
    public static final int SLIDER_TRACK_HEIGHT = 6;
    public static final int SLIDER_THUMB_SIZE = 10;
    public static final int ENUM_OPTION_HEIGHT = 26;
    public static final int PAGE_HEADER_HEIGHT = 80;
    public static final int FILTER_CHIP_Y = 50;
    public static final int FILTER_CHIP_HEIGHT = 22;
    public static final int FILTER_CHIP_WIDTH = 64;
    public static final int CHEAT_FILTER_CHIP_WIDTH = 92;
    public static final int FILTER_CHIP_GAP = 8;
    /** @deprecated use SETTINGS_BUTTON_* */
    @Deprecated
    public static final int SETTINGS_ICON_SIZE = 18;

    private QolUtilityUiMath() {
    }

    public static int statusBadgeWidth(int textWidth) {
        return Math.max(1, textWidth) + STATUS_BADGE_PAD_X * 2;
    }

    public static CardAction moduleCardAction(
            boolean leftClick,
            boolean rightClick,
            boolean hitModuleToggle,
            boolean hitHud,
            boolean hitSettings,
            boolean hasSettings,
            boolean toggleable,
            boolean hasHud,
            boolean hudOpensMenu) {
        if (rightClick) {
            return hasSettings ? CardAction.OPEN_SETTINGS : CardAction.NONE;
        }
        if (!leftClick) {
            return CardAction.NONE;
        }
        if (hitSettings && hasSettings) {
            return CardAction.OPEN_SETTINGS;
        }
        if (hitHud && hasHud) {
            return CardAction.OPEN_HUD_SETTINGS;
        }
        if (hitModuleToggle && toggleable) {
            return CardAction.TOGGLE;
        }
        return hasSettings ? CardAction.OPEN_SETTINGS : CardAction.NONE;
    }

    /** @deprecated use the HUD-aware overload */
    @Deprecated
    public static CardAction moduleCardAction(
            boolean leftClick,
            boolean rightClick,
            boolean hitToggle,
            boolean hitSettings,
            boolean hasSettings,
            boolean toggleable) {
        return moduleCardAction(
                leftClick,
                rightClick,
                hitToggle,
                false,
                hitSettings,
                hasSettings,
                toggleable,
                false,
                false);
    }

    public static int drawerWidth(int contentWidth) {
        if (contentWidth <= 0) {
            return DRAWER_MIN_WIDTH;
        }
        if (contentWidth < DRAWER_MIN_WIDTH + 220) {
            return Math.max(DRAWER_MIN_WIDTH, contentWidth - 24);
        }
        return Math.min(DRAWER_WIDTH, Math.max(DRAWER_MIN_WIDTH, contentWidth / 2));
    }

    public static boolean drawerIsOverlay(int contentWidth) {
        return contentWidth < DRAWER_MIN_WIDTH + 220;
    }

    public static int listWidth(int contentWidth, boolean drawerOpen) {
        if (!drawerOpen) {
            return contentWidth;
        }
        if (drawerIsOverlay(contentWidth)) {
            return contentWidth;
        }
        return Math.max(220, contentWidth - drawerWidth(contentWidth) - 12);
    }

    public static int gridColumns(int listWidth) {
        return listWidth >= GRID_MIN_COLUMN_WIDTH * 2 + CARD_GAP ? 2 : 1;
    }

    public static int cardWidth(int listWidth) {
        int safeWidth = Math.max(1, listWidth);
        int columns = gridColumns(safeWidth);
        return Math.max(1, (safeWidth - (columns - 1) * CARD_GAP) / columns);
    }

    public static int cardX(int moduleIndex, int listLeft, int listWidth) {
        int columns = gridColumns(listWidth);
        int column = Math.max(0, moduleIndex) % columns;
        return listLeft + column * (cardWidth(listWidth) + CARD_GAP);
    }

    public static int cardRow(int moduleIndex, int columns) {
        return Math.max(0, moduleIndex) / Math.max(1, columns);
    }

    public static int measureGridHeight(int moduleCount, int columns) {
        int safeCount = Math.max(0, moduleCount);
        if (safeCount == 0) {
            return 0;
        }
        int rows = (safeCount + Math.max(1, columns) - 1) / Math.max(1, columns);
        return rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
    }

    public record PlacedHeader(String title, int x, int y, int width) {
    }

    public record PlacedCard(
            QolUtilityCatalog.ModuleDef module, int x, int y, int width) {
    }

    public record PageLayout(
            List<PlacedHeader> headers, List<PlacedCard> cards, int height) {
    }

    public record FilterChip(
            PageFilter filter, String label, int x, int y, int width, int height) {
    }

    public static int pageListTop(int contentTop) {
        return contentTop + PAGE_HEADER_HEIGHT;
    }

    public static List<FilterChip> pageFilterChips(int headerX, int headerY) {
        int y = headerY + FILTER_CHIP_Y;
        int x = headerX + 12;
        List<FilterChip> chips = new ArrayList<>();
        for (PageFilter filter : PageFilter.values()) {
            String label = switch (filter) {
                case ALL -> "All";
                case ENABLED -> "On";
                case CHEAT -> "Cheats";
            };
            int chipWidth = filter == PageFilter.CHEAT
                    ? CHEAT_FILTER_CHIP_WIDTH
                    : FILTER_CHIP_WIDTH;
            chips.add(new FilterChip(
                    filter, label, x, y, chipWidth, FILTER_CHIP_HEIGHT));
            x += chipWidth + FILTER_CHIP_GAP;
        }
        return List.copyOf(chips);
    }

    public static String cheatFilterLabel(int cheatCount) {
        if (cheatCount <= 0) {
            return "Cheats";
        }
        return "Cheats [" + cheatCount + "]";
    }

    public static PageFilter hitPageFilter(int mouseX, int mouseY, int headerX, int headerY) {
        for (FilterChip chip : pageFilterChips(headerX, headerY)) {
            if (mouseX >= chip.x()
                    && mouseX < chip.x() + chip.width()
                    && mouseY >= chip.y()
                    && mouseY < chip.y() + chip.height()) {
                return chip.filter();
            }
        }
        return null;
    }

    public static List<QolUtilityCatalog.ModuleDef> filterPageModules(
            List<QolUtilityCatalog.ModuleDef> modules,
            PageFilter filter,
            java.util.function.Predicate<QolUtilityCatalog.ModuleDef> enabled) {
        if (modules == null || modules.isEmpty()) {
            return List.of();
        }
        PageFilter active = filter == null ? PageFilter.ALL : filter;
        if (active == PageFilter.ALL) {
            return List.copyOf(modules);
        }
        List<QolUtilityCatalog.ModuleDef> out = new ArrayList<>();
        for (QolUtilityCatalog.ModuleDef module : modules) {
            boolean keep = switch (active) {
                case ALL -> true;
                case ENABLED -> enabled != null && enabled.test(module);
                case CHEAT -> QolUtilityCatalog.hasCheatTag(module);
            };
            if (keep) {
                out.add(module);
            }
        }
        return List.copyOf(out);
    }

    public static PageLayout layoutPage(
            List<QolUtilityCatalog.ModuleDef> modules,
            int listLeft,
            int gridWidth) {
        LinkedHashMap<String, List<QolUtilityCatalog.ModuleDef>> groups =
                new LinkedHashMap<>();
        if (modules != null) {
            for (QolUtilityCatalog.ModuleDef module : modules) {
                String key = module == null || module.section() == null
                        ? ""
                        : module.section().trim();
                groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(module);
            }
        }
        boolean named = groups.keySet().stream().anyMatch(key -> !key.isBlank());
        int columns = gridColumns(gridWidth);
        int cardW = cardWidth(gridWidth);
        List<PlacedHeader> headers = new ArrayList<>();
        List<PlacedCard> cards = new ArrayList<>();
        int y = 0;
        for (Map.Entry<String, List<QolUtilityCatalog.ModuleDef>> entry : groups.entrySet()) {
            if (named) {
                String title = entry.getKey().isBlank() ? "General" : entry.getKey();
                headers.add(new PlacedHeader(title, listLeft, y, gridWidth));
                y += GROUP_HEADER_HEIGHT;
            }
            List<QolUtilityCatalog.ModuleDef> list = entry.getValue();
            for (int i = 0; i < list.size(); i++) {
                int x = cardX(i, listLeft, gridWidth);
                int cardY = y + cardRow(i, columns) * (CARD_HEIGHT + CARD_GAP);
                cards.add(new PlacedCard(list.get(i), x, cardY, cardW));
            }
            int rows = (list.size() + Math.max(1, columns) - 1) / Math.max(1, columns);
            y += rows * CARD_HEIGHT + Math.max(0, rows - 1) * CARD_GAP;
            y += named ? 10 : 0;
        }
        return new PageLayout(List.copyOf(headers), List.copyOf(cards), Math.max(0, y));
    }

    public static boolean hitNumberDecrease(int mouseX, int rowX, int rowWidth) {
        int controlX = rowX + rowWidth - NUMBER_CONTROL_WIDTH;
        return mouseX >= controlX && mouseX < controlX + NUMBER_BUTTON_WIDTH;
    }

    public static boolean hitNumberIncrease(int mouseX, int rowX, int rowWidth) {
        int controlX = rowX + rowWidth - NUMBER_CONTROL_WIDTH;
        int plusX = controlX + NUMBER_CONTROL_WIDTH - NUMBER_BUTTON_WIDTH;
        return mouseX >= plusX && mouseX < plusX + NUMBER_BUTTON_WIDTH;
    }

    public static int settingRowHeight(QolUtilityCatalog.SettingDef setting) {
        if (setting != null
                && setting.type() == QolUtilityCatalog.SettingType.NUMBER
                && QolNumberSettings.usesSlider(setting.id())) {
            return SLIDER_ROW_HEIGHT;
        }
        return DRAWER_ROW_HEIGHT;
    }

    public static boolean hitSlider(
            int mouseX,
            int mouseY,
            int rowX,
            int rowY,
            int rowWidth) {
        return mouseX >= rowX
                && mouseX < rowX + rowWidth
                && mouseY >= rowY
                && mouseY < rowY + SLIDER_ROW_HEIGHT;
    }

    public static double sliderFraction(int mouseX, int rowX, int rowWidth) {
        int trackX = rowX + SLIDER_TRACK_INSET;
        int trackW = Math.max(1, rowWidth - SLIDER_TRACK_INSET * 2);
        return Math.max(0.0D, Math.min(1.0D, (mouseX - trackX) / (double) trackW));
    }

    public static int measureEnumOptionsHeight(int optionCount) {
        return Math.max(0, optionCount) * ENUM_OPTION_HEIGHT;
    }

    public static int cardFooterY(int cardY, int cardHeight) {
        return cardY + Math.max(cardHeight, FOOTER_HEIGHT + 12) - 12 - FOOTER_HEIGHT;
    }

    public static int moduleToggleX(int cardX) {
        return cardX + CARD_PAD;
    }

    public static int moduleToggleY(int cardY, int cardHeight) {
        return cardFooterY(cardY, cardHeight)
                + Math.max(0, (FOOTER_HEIGHT - TOGGLE_HEIGHT) / 2);
    }

    public static int hudControlX(int cardX) {
        return cardX + CARD_PAD + MODULE_BLOCK_WIDTH;
    }

    public static int hudControlY(int cardY, int cardHeight) {
        return moduleToggleY(cardY, cardHeight);
    }

    public static boolean hitSettingsButton(
            int mouseX,
            int mouseY,
            int cardX,
            int cardY,
            int cardWidth,
            int cardHeight) {
        int buttonX = cardX + cardWidth - CARD_PAD - SETTINGS_BUTTON_WIDTH;
        int buttonY = cardFooterY(cardY, cardHeight)
                + Math.max(0, (FOOTER_HEIGHT - SETTINGS_BUTTON_HEIGHT) / 2);
        return mouseX >= buttonX
                && mouseX < buttonX + SETTINGS_BUTTON_WIDTH
                && mouseY >= buttonY
                && mouseY < buttonY + SETTINGS_BUTTON_HEIGHT;
    }

    /** Backward-compatible alias used by older call sites. */
    public static boolean hitSettingsIcon(
            int mouseX,
            int mouseY,
            int cardX,
            int cardY,
            int cardWidth,
            int cardHeight) {
        return hitSettingsButton(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight);
    }

    public static boolean hitToggle(
            int mouseX,
            int mouseY,
            int cardX,
            int cardY,
            int cardWidth) {
        return hitModuleToggle(mouseX, mouseY, cardX, cardY, CARD_HEIGHT);
    }

    public static boolean hitModuleToggle(
            int mouseX,
            int mouseY,
            int cardX,
            int cardY,
            int cardHeight) {
        int toggleX = moduleToggleX(cardX);
        int toggleY = moduleToggleY(cardY, cardHeight);
        return mouseX >= toggleX
                && mouseX < toggleX + TOGGLE_WIDTH
                && mouseY >= toggleY
                && mouseY < toggleY + TOGGLE_HEIGHT;
    }

    public static boolean hitHudControl(
            int mouseX,
            int mouseY,
            int cardX,
            int cardY,
            int cardHeight) {
        int x = hudControlX(cardX);
        int y = hudControlY(cardY, cardHeight);
        return mouseX >= x
                && mouseX < x + HUD_CONTROL_WIDTH
                && mouseY >= y
                && mouseY < y + SETTINGS_BUTTON_HEIGHT;
    }

    public static int hudMenuX(int cardX) {
        return hudControlX(cardX);
    }

    public static int hudMenuY(int cardY, int cardHeight) {
        return cardY + cardHeight - 4;
    }

    public static int hudMenuHeight(int pieceCount) {
        return OverflowListPolicy.menuHeight(
                pieceCount, HUD_MENU_ROW_HEIGHT, OverflowListPolicy.needsSearch(pieceCount));
    }

    public static int hudMenuBodyTop(int menuY, boolean search) {
        return menuY + 4 + (search ? HUD_MENU_SEARCH_HEIGHT : 0);
    }

    public static int settingLabelMaxWidth(int rowWidth, QolUtilityCatalog.SettingType type) {
        int reserve = switch (type == null ? QolUtilityCatalog.SettingType.TOGGLE : type) {
            case TOGGLE -> DRAWER_CONTROL_RESERVE;
            case COLOR, SQUARE -> 36;
            case ENUM -> 118;
            case NUMBER -> NUMBER_CONTROL_WIDTH + 12;
            case ACTION -> 72;
            case KEYBIND, TEXT, SECTION -> DRAWER_CONTROL_RESERVE;
        };
        return Math.max(40, rowWidth - 16 - reserve);
    }

    public static boolean hitHudMenuRow(
            int mouseX,
            int mouseY,
            int menuX,
            int menuY,
            int index) {
        return hitHudMenuRow(mouseX, mouseY, menuX, menuY, index, false, 0);
    }

    public static boolean hitHudMenuRow(
            int mouseX,
            int mouseY,
            int menuX,
            int menuY,
            int index,
            boolean search,
            int scrollPixels) {
        int rowY = hudMenuBodyTop(menuY, search)
                + index * HUD_MENU_ROW_HEIGHT
                - Math.max(0, scrollPixels);
        return mouseX >= menuX
                && mouseX < menuX + HUD_MENU_WIDTH
                && mouseY >= rowY
                && mouseY < rowY + HUD_MENU_ROW_HEIGHT;
    }

    public static boolean hitHudMenuEdit(
            int mouseX,
            int mouseY,
            int menuX,
            int menuY,
            int index) {
        return hitHudMenuEdit(mouseX, mouseY, menuX, menuY, index, false, 0);
    }

    public static boolean hitHudMenuEdit(
            int mouseX,
            int mouseY,
            int menuX,
            int menuY,
            int index,
            boolean search,
            int scrollPixels) {
        int rowY = hudMenuBodyTop(menuY, search)
                + index * HUD_MENU_ROW_HEIGHT
                - Math.max(0, scrollPixels);
        int editX = menuX + HUD_MENU_WIDTH - 8 - HUD_MENU_EDIT_WIDTH;
        return mouseX >= editX
                && mouseX < editX + HUD_MENU_EDIT_WIDTH
                && mouseY >= rowY + 3
                && mouseY < rowY + HUD_MENU_ROW_HEIGHT - 3;
    }

    public static boolean hitHudMenuToggle(
            int mouseX,
            int mouseY,
            int menuX,
            int menuY,
            int index) {
        return hitHudMenuToggle(mouseX, mouseY, menuX, menuY, index, false, 0);
    }

    public static boolean hitHudMenuToggle(
            int mouseX,
            int mouseY,
            int menuX,
            int menuY,
            int index,
            boolean search,
            int scrollPixels) {
        if (hitHudMenuEdit(mouseX, mouseY, menuX, menuY, index, search, scrollPixels)) {
            return false;
        }
        return hitHudMenuRow(mouseX, mouseY, menuX, menuY, index, search, scrollPixels);
    }

    public static boolean hitClose(
            int mouseX,
            int mouseY,
            int drawerX,
            int drawerY,
            int drawerWidth) {
        int size = 16;
        int x = drawerX + drawerWidth - 18 - size;
        int y = drawerY + 14;
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    public static int measureModuleListHeight(int moduleCount, int groupCount) {
        int cards = Math.max(0, moduleCount) * (CARD_HEIGHT + CARD_GAP);
        int headers = Math.max(0, groupCount) * (GROUP_HEADER_HEIGHT + 6);
        return cards + headers + 12;
    }
}
