package fi.rotclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Pure sidebar accordion layout for the Client UI. Top-level category text
 * always remains; children expand downward. Multi-expand: several sections may
 * stay open. Collapsing a parent never forces a route change.
 */
final class RotClientSidebarNav {
    static final String SECTION_MINING = "mining";
    static final String SECTION_SESSIONS = "sessions";
    static final String SECTION_SETTINGS = "settings";
    static final String SECTION_QOL = "qol";

    static final int SECTION_LABEL_HEIGHT = 16;
    static final int SECTION_GAP = 10;
    static final int ITEM_HEIGHT = 38;
    static final int ITEM_GAP = 8;
    static final int HEADER_HIT_HEIGHT = SECTION_LABEL_HEIGHT + 2;

    private static final Set<String> KNOWN_SECTIONS = Set.of(
            SECTION_MINING, SECTION_SESSIONS, SECTION_SETTINGS, SECTION_QOL);

    enum HitTarget {
        NONE,
        OVERVIEW,
        SECTION_MINING,
        TRACKER,
        MINING_HUD,
        POWDER_CHEST_TRACKER,
        SECTION_SESSIONS,
        ANALYTICS,
        HISTORY,
        SECTION_SETTINGS,
        APPEARANCE,
        HUD_LAYOUT,
        SECTION_QOL,
        QOL_COMBAT,
        QOL_SLAYER,
        QOL_FISHING,
        QOL_FORAGING,
        QOL_DUNGEONS,
        QOL_MINING,
        QOL_UTILITIES,
        QOL_HUD_DISPLAY,
        QOL_RENDER,
        QOL_INTERFACE
    }

    record Layout(
            int overviewY,
            int miningHeaderY,
            int trackerY,
            int miningHudY,
            int powderChestTrackerY,
            int sessionsHeaderY,
            int analyticsY,
            int historyY,
            int settingsHeaderY,
            int appearanceY,
            int hudLayoutY,
            int qolHeaderY,
            int[] qolPageYs,
            boolean miningExpanded,
            boolean sessionsExpanded,
            boolean settingsExpanded,
            boolean qolExpanded,
            double miningOpen,
            double sessionsOpen,
            double settingsOpen,
            double qolOpen) {

        boolean trackerVisible() {
            return miningClipHeight() > 0 && trackerY >= 0;
        }

        boolean miningHudVisible() {
            return miningClipHeight() > 0 && miningHudY >= 0;
        }

        boolean powderChestTrackerVisible() {
            return miningClipHeight() > 0 && powderChestTrackerY >= 0;
        }

        boolean analyticsVisible() {
            return sessionsClipHeight() > 0 && analyticsY >= 0;
        }

        boolean historyVisible() {
            return sessionsClipHeight() > 0 && historyY >= 0;
        }

        boolean appearanceVisible() {
            return settingsClipHeight() > 0 && appearanceY >= 0;
        }

        boolean hudLayoutVisible() {
            return settingsClipHeight() > 0 && hudLayoutY >= 0;
        }

        boolean qolChildrenVisible() {
            return qolClipHeight() > 0;
        }

        int miningClipHeight() {
            return RotClientEase.shownPixels(childStackHeight(3), miningOpen);
        }

        int sessionsClipHeight() {
            return RotClientEase.shownPixels(childStackHeight(2), sessionsOpen);
        }

        int settingsClipHeight() {
            return RotClientEase.shownPixels(childStackHeight(2), settingsOpen);
        }

        int qolClipHeight() {
            return RotClientEase.shownPixels(
                    childStackHeight(QolUtilityCatalog.sidebarPages().size()),
                    qolOpen);
        }

        int qolPageY(QolUtilityCatalog.Group group) {
            if (!qolChildrenVisible() || group == null || qolPageYs == null) {
                return -1;
            }
            List<QolUtilityCatalog.Group> pages = QolUtilityCatalog.sidebarPages();
            int index = pages.indexOf(group);
            if (index < 0 || index >= qolPageYs.length) {
                return -1;
            }
            return qolPageYs[index];
        }

        int qolUtilitiesY() {
            return qolPageY(QolUtilityCatalog.Group.UTILITIES);
        }

        int contentHeight(int originY) {
            int bottom = overviewY + ITEM_HEIGHT;
            bottom = Math.max(bottom, miningHeaderY + HEADER_HIT_HEIGHT);
            if (miningClipHeight() > 0 && trackerY >= 0) {
                bottom = Math.max(bottom, trackerY + miningClipHeight());
            }
            bottom = Math.max(bottom, sessionsHeaderY + HEADER_HIT_HEIGHT);
            if (sessionsClipHeight() > 0 && analyticsY >= 0) {
                bottom = Math.max(bottom, analyticsY + sessionsClipHeight());
            }
            bottom = Math.max(bottom, settingsHeaderY + HEADER_HIT_HEIGHT);
            if (settingsClipHeight() > 0 && appearanceY >= 0) {
                bottom = Math.max(bottom, appearanceY + settingsClipHeight());
            }
            bottom = Math.max(bottom, qolHeaderY + HEADER_HIT_HEIGHT);
            if (qolClipHeight() > 0 && qolPageYs != null && qolPageYs.length > 0
                    && qolPageYs[0] >= 0) {
                bottom = Math.max(bottom, qolPageYs[0] + qolClipHeight());
            }
            return Math.max(0, bottom - originY);
        }
    }

    private RotClientSidebarNav() {
    }

    static List<String> defaultExpandedSections() {
        return List.of(
                SECTION_MINING, SECTION_SESSIONS, SECTION_SETTINGS, SECTION_QOL);
    }

    static List<String> normalizeExpandedSections(Collection<String> raw) {
        if (raw == null) {
            return new ArrayList<>(defaultExpandedSections());
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String id : raw) {
            String normalized = normalizeSectionId(id);
            if (normalized != null) {
                cleaned.add(normalized);
            }
        }
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        if (cleaned.isEmpty()) {
            return new ArrayList<>(defaultExpandedSections());
        }
        return new ArrayList<>(cleaned);
    }

    static String normalizeSectionId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        return KNOWN_SECTIONS.contains(id) ? id : null;
    }

    static boolean isExpanded(Collection<String> expanded, String sectionId) {
        String id = normalizeSectionId(sectionId);
        if (id == null) {
            return false;
        }
        if (expanded == null) {
            return defaultExpandedSections().contains(id);
        }
        for (String candidate : expanded) {
            if (id.equals(normalizeSectionId(candidate))) {
                return true;
            }
        }
        return false;
    }

    static List<String> toggleSection(Collection<String> expanded, String sectionId) {
        String id = normalizeSectionId(sectionId);
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (expanded == null) {
            set.addAll(defaultExpandedSections());
        } else {
            for (String candidate : expanded) {
                String normalized = normalizeSectionId(candidate);
                if (normalized != null) {
                    set.add(normalized);
                }
            }
        }
        if (id == null) {
            return new ArrayList<>(set);
        }
        if (!set.add(id)) {
            set.remove(id);
        }
        return new ArrayList<>(set);
    }

    static List<String> knownSections() {
        return List.of(SECTION_MINING, SECTION_SESSIONS, SECTION_SETTINGS, SECTION_QOL);
    }

    static int childStackHeight(int count) {
        if (count <= 0) {
            return 0;
        }
        return count * (ITEM_HEIGHT + ITEM_GAP);
    }

    static Layout layout(int originY, Collection<String> expanded) {
        return layout(originY, expanded, null);
    }

    static Layout layout(
            int originY,
            Collection<String> expanded,
            ToDoubleFunction<String> openAmounts) {
        boolean mining = isExpanded(expanded, SECTION_MINING);
        boolean sessions = isExpanded(expanded, SECTION_SESSIONS);
        boolean settings = isExpanded(expanded, SECTION_SETTINGS);
        boolean qol = isExpanded(expanded, SECTION_QOL);
        double miningOpen = openAmount(openAmounts, SECTION_MINING, mining);
        double sessionsOpen = openAmount(openAmounts, SECTION_SESSIONS, sessions);
        double settingsOpen = openAmount(openAmounts, SECTION_SETTINGS, settings);
        double qolOpen = openAmount(openAmounts, SECTION_QOL, qol);

        int y = originY;
        int overviewY = y;
        y += ITEM_HEIGHT + ITEM_GAP;

        int miningHeaderY = y;
        y += HEADER_HIT_HEIGHT + SECTION_GAP;
        int trackerY = y;
        int miningHudY = y + ITEM_HEIGHT + ITEM_GAP;
        int powderY = y + 2 * (ITEM_HEIGHT + ITEM_GAP);
        y += RotClientEase.shownPixels(childStackHeight(3), miningOpen);

        int sessionsHeaderY = y;
        y += HEADER_HIT_HEIGHT + SECTION_GAP;
        int analyticsY = y;
        int historyY = y + ITEM_HEIGHT + ITEM_GAP;
        y += RotClientEase.shownPixels(childStackHeight(2), sessionsOpen);

        int settingsHeaderY = y;
        y += HEADER_HIT_HEIGHT + SECTION_GAP;
        int appearanceY = y;
        int hudLayoutY = y + ITEM_HEIGHT + ITEM_GAP;
        y += RotClientEase.shownPixels(childStackHeight(2), settingsOpen);

        int qolHeaderY = y;
        y += HEADER_HIT_HEIGHT + SECTION_GAP;
        List<QolUtilityCatalog.Group> pages = QolUtilityCatalog.sidebarPages();
        int[] qolPageYs = new int[pages.size()];
        int qolStackTop = y;
        for (int i = 0; i < pages.size(); i++) {
            qolPageYs[i] = qolStackTop + i * (ITEM_HEIGHT + ITEM_GAP);
        }
        y += RotClientEase.shownPixels(childStackHeight(pages.size()), qolOpen);

        return new Layout(
                overviewY,
                miningHeaderY,
                trackerY,
                miningHudY,
                powderY,
                sessionsHeaderY,
                analyticsY,
                historyY,
                settingsHeaderY,
                appearanceY,
                hudLayoutY,
                qolHeaderY,
                qolPageYs,
                mining,
                sessions,
                settings,
                qol,
                miningOpen,
                sessionsOpen,
                settingsOpen,
                qolOpen);
    }

    private static double openAmount(
            ToDoubleFunction<String> openAmounts,
            String sectionId,
            boolean expanded) {
        if (openAmounts == null) {
            return expanded ? 1.0D : 0.0D;
        }
        return RotClientEase.clamp01(openAmounts.applyAsDouble(sectionId));
    }

    static HitTarget hitTest(
            Layout layout,
            int localX,
            int localY,
            int itemX,
            int itemW,
            int headerX,
            int headerW) {
        if (layout == null) {
            return HitTarget.NONE;
        }
        if (inItem(localX, localY, itemX, itemW, layout.overviewY())) {
            return HitTarget.OVERVIEW;
        }
        if (inHeader(localX, localY, headerX, headerW, layout.miningHeaderY())) {
            return HitTarget.SECTION_MINING;
        }
        if (inClippedItem(
                localX, localY, itemX, itemW, layout.trackerY(),
                layout.trackerY(), layout.miningClipHeight())) {
            return HitTarget.TRACKER;
        }
        if (inClippedItem(
                localX, localY, itemX, itemW, layout.miningHudY(),
                layout.trackerY(), layout.miningClipHeight())) {
            return HitTarget.MINING_HUD;
        }
        if (inClippedItem(
                localX, localY, itemX, itemW, layout.powderChestTrackerY(),
                layout.trackerY(), layout.miningClipHeight())) {
            return HitTarget.POWDER_CHEST_TRACKER;
        }
        if (inHeader(localX, localY, headerX, headerW, layout.sessionsHeaderY())) {
            return HitTarget.SECTION_SESSIONS;
        }
        if (inClippedItem(
                localX, localY, itemX, itemW, layout.analyticsY(),
                layout.analyticsY(), layout.sessionsClipHeight())) {
            return HitTarget.ANALYTICS;
        }
        if (inClippedItem(
                localX, localY, itemX, itemW, layout.historyY(),
                layout.analyticsY(), layout.sessionsClipHeight())) {
            return HitTarget.HISTORY;
        }
        if (inHeader(localX, localY, headerX, headerW, layout.settingsHeaderY())) {
            return HitTarget.SECTION_SETTINGS;
        }
        if (inClippedItem(
                localX, localY, itemX, itemW, layout.appearanceY(),
                layout.appearanceY(), layout.settingsClipHeight())) {
            return HitTarget.APPEARANCE;
        }
        if (inClippedItem(
                localX, localY, itemX, itemW, layout.hudLayoutY(),
                layout.appearanceY(), layout.settingsClipHeight())) {
            return HitTarget.HUD_LAYOUT;
        }
        if (inHeader(localX, localY, headerX, headerW, layout.qolHeaderY())) {
            return HitTarget.SECTION_QOL;
        }
        if (layout.qolChildrenVisible()) {
            int qolClipTop = layout.qolPageYs() != null && layout.qolPageYs().length > 0
                    ? layout.qolPageYs()[0]
                    : -1;
            for (QolUtilityCatalog.Group group : QolUtilityCatalog.sidebarPages()) {
                int pageY = layout.qolPageY(group);
                if (inClippedItem(
                        localX, localY, itemX, itemW, pageY,
                        qolClipTop, layout.qolClipHeight())) {
                    return hitTargetForQolGroup(group);
                }
            }
        }
        return HitTarget.NONE;
    }

    static QolUtilityCatalog.Group groupForHitTarget(HitTarget hit) {
        if (hit == null) {
            return null;
        }
        return switch (hit) {
            case QOL_COMBAT -> QolUtilityCatalog.Group.COMBAT;
            case QOL_SLAYER -> QolUtilityCatalog.Group.SLAYER;
            case QOL_FISHING -> QolUtilityCatalog.Group.FISHING;
            case QOL_FORAGING -> QolUtilityCatalog.Group.FORAGING;
            case QOL_DUNGEONS -> QolUtilityCatalog.Group.DUNGEONS;
            case QOL_MINING -> QolUtilityCatalog.Group.MINING;
            case QOL_UTILITIES -> QolUtilityCatalog.Group.UTILITIES;
            case QOL_HUD_DISPLAY -> QolUtilityCatalog.Group.HUD_DISPLAY;
            case QOL_RENDER -> QolUtilityCatalog.Group.RENDER;
            case QOL_INTERFACE -> QolUtilityCatalog.Group.INTERFACE;
            default -> null;
        };
    }

    static HitTarget hitTargetForQolGroup(QolUtilityCatalog.Group group) {
        if (group == null) {
            return HitTarget.NONE;
        }
        return switch (group) {
            case COMBAT -> HitTarget.QOL_COMBAT;
            case SLAYER -> HitTarget.QOL_SLAYER;
            case FISHING -> HitTarget.QOL_FISHING;
            case FORAGING -> HitTarget.QOL_FORAGING;
            case DUNGEONS -> HitTarget.QOL_DUNGEONS;
            case MINING -> HitTarget.QOL_MINING;
            case UTILITIES -> HitTarget.QOL_UTILITIES;
            case HUD_DISPLAY -> HitTarget.QOL_HUD_DISPLAY;
            case RENDER -> HitTarget.QOL_RENDER;
            case INTERFACE -> HitTarget.QOL_INTERFACE;
        };
    }

    static HitTarget hitTargetForModule(DashboardModule module) {
        if (module == null) {
            return HitTarget.OVERVIEW;
        }
        return switch (module) {
            case NONE -> HitTarget.OVERVIEW;
            case MINING_TRACKER -> HitTarget.TRACKER;
            case POWDER_CHEST_TRACKER -> HitTarget.POWDER_CHEST_TRACKER;
            case SESSION_ANALYTICS -> HitTarget.ANALYTICS;
            case SESSION_HISTORY -> HitTarget.HISTORY;
            case QOL_SETTINGS -> HitTarget.QOL_UTILITIES;
        };
    }

    static String sectionForModule(DashboardModule module) {
        if (module == null) {
            return null;
        }
        return switch (module) {
            case NONE -> null;
            case MINING_TRACKER, POWDER_CHEST_TRACKER -> SECTION_MINING;
            case SESSION_ANALYTICS, SESSION_HISTORY -> SECTION_SESSIONS;
            case QOL_SETTINGS -> SECTION_QOL;
        };
    }

    static int targetTop(Layout layout, HitTarget target) {
        if (layout == null || target == null) {
            return -1;
        }
        return switch (target) {
            case NONE -> -1;
            case OVERVIEW -> layout.overviewY();
            case SECTION_MINING -> layout.miningHeaderY();
            case TRACKER -> layout.trackerY();
            case MINING_HUD -> layout.miningHudY();
            case POWDER_CHEST_TRACKER -> layout.powderChestTrackerY();
            case SECTION_SESSIONS -> layout.sessionsHeaderY();
            case ANALYTICS -> layout.analyticsY();
            case HISTORY -> layout.historyY();
            case SECTION_SETTINGS -> layout.settingsHeaderY();
            case APPEARANCE -> layout.appearanceY();
            case HUD_LAYOUT -> layout.hudLayoutY();
            case SECTION_QOL -> layout.qolHeaderY();
            case QOL_COMBAT, QOL_SLAYER, QOL_FISHING, QOL_FORAGING, QOL_DUNGEONS, QOL_MINING,
                    QOL_UTILITIES, QOL_HUD_DISPLAY, QOL_RENDER, QOL_INTERFACE ->
                    layout.qolPageY(groupForHitTarget(target));
        };
    }

    static int targetHeight(HitTarget target) {
        if (target == null) {
            return ITEM_HEIGHT;
        }
        return switch (target) {
            case SECTION_MINING, SECTION_SESSIONS, SECTION_SETTINGS, SECTION_QOL ->
                    HEADER_HIT_HEIGHT;
            default -> ITEM_HEIGHT;
        };
    }

    static int scrollToReveal(
            int scrollPixels,
            int viewportHeight,
            int contentHeight,
            int top,
            int bottom) {
        int max = Math.max(0, contentHeight - Math.max(0, viewportHeight));
        int next = Math.max(0, scrollPixels);
        if (top < next) {
            next = Math.max(0, top);
        } else if (bottom > next + viewportHeight) {
            next = Math.max(0, bottom - viewportHeight);
        }
        return Math.min(max, next);
    }

    private static boolean inItem(int x, int y, int itemX, int itemW, int itemY) {
        return itemY >= 0 && inside(x, y, itemX, itemY, itemW, ITEM_HEIGHT);
    }

    private static boolean inClippedItem(
            int x,
            int y,
            int itemX,
            int itemW,
            int itemY,
            int clipTop,
            int clipHeight) {
        if (itemY < 0 || clipHeight <= 0 || clipTop < 0) {
            return false;
        }
        int visTop = Math.max(itemY, clipTop);
        int visBottom = Math.min(itemY + ITEM_HEIGHT, clipTop + clipHeight);
        if (visBottom - visTop < 10) {
            return false;
        }
        return inside(x, y, itemX, visTop, itemW, visBottom - visTop);
    }

    private static boolean inHeader(int x, int y, int headerX, int headerW, int headerY) {
        return headerY >= 0 && inside(x, y, headerX, headerY, headerW, HEADER_HIT_HEIGHT);
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
