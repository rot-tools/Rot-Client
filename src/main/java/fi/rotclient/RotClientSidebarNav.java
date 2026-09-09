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
            SECTION_SETTINGS,
            SECTION_QOL);

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
        PROFILES,
        SECTION_QOL,
        MARKET_WATCH,
        QOL_COMBAT,
        QOL_SLAYER,
        QOL_FISHING,
        QOL_FORAGING,
        QOL_DUNGEONS,
        QOL_KUUDRA,
        QOL_EVENTS,
        QOL_MINING,
        QOL_GARDEN,
        QOL_GUI,
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
            int profilesY,
            int qolHeaderY,
            int marketWatchY,
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
            return miningClipHeight() > 0
                    && trackerY >= 0;
        }

        boolean miningHudVisible() {
            return miningClipHeight() > 0
                    && miningHudY >= 0;
        }

        boolean powderChestTrackerVisible() {
            return miningClipHeight() > 0
                    && powderChestTrackerY >= 0;
        }

        boolean analyticsVisible() {
            return miningClipHeight() > 0
                    && analyticsY >= 0;
        }

        boolean historyVisible() {
            return miningClipHeight() > 0
                    && historyY >= 0;
        }

        boolean appearanceVisible() {
            return settingsClipHeight() > 0
                    && appearanceY >= 0;
        }

        boolean hudLayoutVisible() {
            return settingsClipHeight() > 0
                    && hudLayoutY >= 0;
        }

        boolean profilesVisible() {
            return settingsClipHeight() > 0
                    && profilesY >= 0;
        }

        boolean qolChildrenVisible() {
            return qolClipHeight() > 0;
        }

        int miningClipHeight() {
            return RotClientEase.shownPixels(
                    childStackHeight(5),
                    miningOpen);
        }

        int sessionsClipHeight() {
            return miningClipHeight();
        }

        int settingsClipHeight() {
            return RotClientEase.shownPixels(
                    childStackHeight(3),
                    settingsOpen);
        }

        int qolClipHeight() {
            return RotClientEase.shownPixels(
                    childStackHeight(
                            QolUtilityCatalog.sidebarPages().size() + 1),
                    qolOpen);
        }

        int qolPageY(QolUtilityCatalog.Group group) {
            if (!qolChildrenVisible()
                    || group == null
                    || qolPageYs == null) {
                return -1;
            }

            List<QolUtilityCatalog.Group> pages =
                    QolUtilityCatalog.sidebarPages();

            int index = pages.indexOf(group);

            if (index < 0
                    || index >= qolPageYs.length) {
                return -1;
            }

            return qolPageYs[index];
        }

        int qolUtilitiesY() {
            return qolPageY(
                    QolUtilityCatalog.Group.UTILITIES);
        }

        int contentHeight(int originY) {
            int bottom =
                    overviewY + ITEM_HEIGHT;

            if (miningHeaderY >= 0) {
                bottom = Math.max(
                        bottom,
                        miningHeaderY
                                + HEADER_HIT_HEIGHT);
            }

            if (miningClipHeight() > 0
                    && trackerY >= 0) {
                bottom = Math.max(
                        bottom,
                        trackerY
                                + miningClipHeight());
            }

            bottom = Math.max(
                    bottom,
                    settingsHeaderY
                            + HEADER_HIT_HEIGHT);

            if (settingsClipHeight() > 0
                    && appearanceY >= 0) {
                bottom = Math.max(
                        bottom,
                        appearanceY
                                + settingsClipHeight());
            }

            bottom = Math.max(
                    bottom,
                    qolHeaderY
                            + HEADER_HIT_HEIGHT);

            if (qolClipHeight() > 0
                    && marketWatchY >= 0) {
                bottom = Math.max(
                        bottom,
                        marketWatchY
                                + qolClipHeight());
            }

            return Math.max(
                    0,
                    bottom - originY);
        }
    }

    private RotClientSidebarNav() {
    }

    static List<String> defaultExpandedSections() {
        return List.of(
                SECTION_SETTINGS,
                SECTION_QOL);
    }

    static List<String> normalizeExpandedSections(
            Collection<String> raw) {

        if (raw == null) {
            return new ArrayList<>(
                    defaultExpandedSections());
        }

        LinkedHashSet<String> cleaned =
                new LinkedHashSet<>();

        for (String id : raw) {
            String normalized =
                    normalizeSectionId(id);

            if (normalized != null) {
                cleaned.add(normalized);
            }
        }

        if (raw.isEmpty()) {
            return new ArrayList<>();
        }

        if (cleaned.isEmpty()) {
            return new ArrayList<>(
                    defaultExpandedSections());
        }

        return new ArrayList<>(cleaned);
    }

    static String normalizeSectionId(
            String raw) {

        if (raw == null
                || raw.isBlank()) {
            return null;
        }

        String id =
                raw.trim()
                        .toLowerCase(Locale.ROOT);

        if (SECTION_SESSIONS.equals(id)
                || SECTION_MINING.equals(id)) {
            return SECTION_QOL;
        }

        return KNOWN_SECTIONS.contains(id)
                ? id
                : null;
    }

    static boolean isExpanded(
            Collection<String> expanded,
            String sectionId) {

        String id =
                normalizeSectionId(sectionId);

        if (id == null) {
            return false;
        }

        if (expanded == null) {
            return defaultExpandedSections()
                    .contains(id);
        }

        for (String candidate : expanded) {
            if (id.equals(
                    normalizeSectionId(candidate))) {
                return true;
            }
        }

        return false;
    }

    static List<String> toggleSection(
            Collection<String> expanded,
            String sectionId) {

        String id =
                normalizeSectionId(sectionId);

        LinkedHashSet<String> set =
                new LinkedHashSet<>();

        if (expanded == null) {
            set.addAll(
                    defaultExpandedSections());
        } else {
            for (String candidate : expanded) {
                String normalized =
                        normalizeSectionId(
                                candidate);

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
        return List.of(
                SECTION_SETTINGS,
                SECTION_QOL);
    }

    static int childStackHeight(
            int count) {

        if (count <= 0) {
            return 0;
        }

        return count
                * (ITEM_HEIGHT + ITEM_GAP);
    }

    static Layout layout(
            int originY,
            Collection<String> expanded) {

        return layout(
                originY,
                expanded,
                null);
    }

    static Layout layout(
            int originY,
            Collection<String> expanded,
            ToDoubleFunction<String> openAmounts) {

        boolean mining = false;
        boolean sessions = false;

        boolean settings =
                isExpanded(
                        expanded,
                        SECTION_SETTINGS);

        boolean qol =
                isExpanded(
                        expanded,
                        SECTION_QOL);

        double miningOpen = 0.0D;
        double sessionsOpen = 0.0D;

        double settingsOpen =
                openAmount(
                        openAmounts,
                        SECTION_SETTINGS,
                        settings);

        double qolOpen =
                openAmount(
                        openAmounts,
                        SECTION_QOL,
                        qol);

        int y = originY;

        int overviewY = y;

        y += ITEM_HEIGHT
                + ITEM_GAP;

        /*
         * Mining and Sessions were folded into the QoL/module catalogue.
         * Their legacy layout fields remain so older policies/tests can keep
         * using the same Layout shape without creating visible sidebar rows.
         */
        int miningHeaderY = -1;
        int trackerY = -1;
        int miningHudY = -1;
        int powderY = -1;
        int analyticsY = -1;
        int historyY = -1;
        int sessionsHeaderY = -1;

        /*
         * Visuals / settings section.
         *
         * Three children:
         * 1. Appearance
         * 2. HUD Elements Editor
         * 3. Profiles
         */
        int settingsHeaderY = y;

        y += HEADER_HIT_HEIGHT
                + SECTION_GAP;

        int appearanceY = y;

        int hudLayoutY =
                y + ITEM_HEIGHT
                        + ITEM_GAP;

        int profilesY =
                y + (ITEM_HEIGHT + ITEM_GAP) * 2;

        y += RotClientEase.shownPixels(
                childStackHeight(3),
                settingsOpen);

        /*
         * QoL/module catalogue.
         */
        int qolHeaderY = y;

        y += HEADER_HIT_HEIGHT
                + SECTION_GAP;

        List<QolUtilityCatalog.Group> pages =
                QolUtilityCatalog.sidebarPages();

        int marketWatchY = y;

        int[] qolPageYs =
                new int[pages.size()];

        int qolStackTop =
                y + ITEM_HEIGHT + ITEM_GAP;

        for (int i = 0;
             i < pages.size();
             i++) {

            qolPageYs[i] =
                    qolStackTop
                            + i
                            * (ITEM_HEIGHT
                            + ITEM_GAP);
        }

        y += RotClientEase.shownPixels(
                childStackHeight(
                        pages.size() + 1),
                qolOpen);

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
                profilesY,
                qolHeaderY,
                marketWatchY,
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
            return expanded
                    ? 1.0D
                    : 0.0D;
        }

        return RotClientEase.clamp01(
                openAmounts.applyAsDouble(
                        sectionId));
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

        if (inItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.overviewY())) {
            return HitTarget.OVERVIEW;
        }

        if (inHeader(
                localX,
                localY,
                headerX,
                headerW,
                layout.miningHeaderY())) {
            return HitTarget.SECTION_MINING;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.trackerY(),
                layout.trackerY(),
                layout.miningClipHeight())) {
            return HitTarget.TRACKER;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.miningHudY(),
                layout.trackerY(),
                layout.miningClipHeight())) {
            return HitTarget.MINING_HUD;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.powderChestTrackerY(),
                layout.trackerY(),
                layout.miningClipHeight())) {
            return HitTarget.POWDER_CHEST_TRACKER;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.analyticsY(),
                layout.trackerY(),
                layout.miningClipHeight())) {
            return HitTarget.ANALYTICS;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.historyY(),
                layout.trackerY(),
                layout.miningClipHeight())) {
            return HitTarget.HISTORY;
        }

        if (inHeader(
                localX,
                localY,
                headerX,
                headerW,
                layout.settingsHeaderY())) {
            return HitTarget.SECTION_SETTINGS;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.appearanceY(),
                layout.appearanceY(),
                layout.settingsClipHeight())) {
            return HitTarget.APPEARANCE;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.hudLayoutY(),
                layout.appearanceY(),
                layout.settingsClipHeight())) {
            return HitTarget.HUD_LAYOUT;
        }

        if (inClippedItem(
                localX,
                localY,
                itemX,
                itemW,
                layout.profilesY(),
                layout.appearanceY(),
                layout.settingsClipHeight())) {
            return HitTarget.PROFILES;
        }

        if (inHeader(
                localX,
                localY,
                headerX,
                headerW,
                layout.qolHeaderY())) {
            return HitTarget.SECTION_QOL;
        }

        if (layout.qolChildrenVisible()) {
            int qolClipTop =
                    layout.marketWatchY();

            if (inClippedItem(
                    localX,
                    localY,
                    itemX,
                    itemW,
                    layout.marketWatchY(),
                    qolClipTop,
                    layout.qolClipHeight())) {
                return HitTarget.MARKET_WATCH;
            }

            for (QolUtilityCatalog.Group group
                    : QolUtilityCatalog.sidebarPages()) {

                int pageY =
                        layout.qolPageY(group);

                if (inClippedItem(
                        localX,
                        localY,
                        itemX,
                        itemW,
                        pageY,
                        qolClipTop,
                        layout.qolClipHeight())) {

                    return hitTargetForQolGroup(
                            group);
                }
            }
        }

        return HitTarget.NONE;
    }

    static QolUtilityCatalog.Group groupForHitTarget(
            HitTarget hit) {

        if (hit == null) {
            return null;
        }

        return switch (hit) {
            case QOL_COMBAT ->
                    QolUtilityCatalog.Group.COMBAT;

            case QOL_SLAYER ->
                    QolUtilityCatalog.Group.SLAYER;

            case QOL_FISHING ->
                    QolUtilityCatalog.Group.FISHING;

            case QOL_FORAGING ->
                    QolUtilityCatalog.Group.FORAGING;

            case QOL_DUNGEONS ->
                    QolUtilityCatalog.Group.DUNGEONS;

            case QOL_KUUDRA ->
                    QolUtilityCatalog.Group.KUUDRA;

            case QOL_EVENTS ->
                    QolUtilityCatalog.Group.EVENTS;

            case QOL_MINING ->
                    QolUtilityCatalog.Group.MINING;

            case QOL_GARDEN ->
                    QolUtilityCatalog.Group.GARDEN;

            case QOL_GUI ->
                    QolUtilityCatalog.Group.GUI;

            case QOL_UTILITIES ->
                    QolUtilityCatalog.Group.UTILITIES;

            case QOL_HUD_DISPLAY ->
                    QolUtilityCatalog.Group.HUD_DISPLAY;

            case QOL_RENDER ->
                    QolUtilityCatalog.Group.RENDER;

            case QOL_INTERFACE ->
                    QolUtilityCatalog.Group.INTERFACE;

            default -> null;
        };
    }

    static HitTarget hitTargetForQolGroup(
            QolUtilityCatalog.Group group) {

        if (group == null) {
            return HitTarget.NONE;
        }

        return switch (group) {
            case COMBAT ->
                    HitTarget.QOL_COMBAT;

            case SLAYER ->
                    HitTarget.QOL_SLAYER;

            case FISHING ->
                    HitTarget.QOL_FISHING;

            case FORAGING ->
                    HitTarget.QOL_FORAGING;

            case DUNGEONS ->
                    HitTarget.QOL_DUNGEONS;

            case KUUDRA ->
                    HitTarget.QOL_KUUDRA;

            case EVENTS ->
                    HitTarget.QOL_EVENTS;

            case MINING ->
                    HitTarget.QOL_MINING;

            case GARDEN ->
                    HitTarget.QOL_GARDEN;

            case GUI ->
                    HitTarget.QOL_GUI;

            case UTILITIES ->
                    HitTarget.QOL_UTILITIES;

            case HUD_DISPLAY ->
                    HitTarget.QOL_HUD_DISPLAY;

            case RENDER ->
                    HitTarget.QOL_RENDER;

            case INTERFACE ->
                    HitTarget.QOL_INTERFACE;
        };
    }

    static HitTarget hitTargetForModule(
            DashboardModule module) {

        if (module == null) {
            return HitTarget.OVERVIEW;
        }

        return switch (module) {
            case NONE ->
                    HitTarget.OVERVIEW;

            case MINING_TRACKER,
                 POWDER_CHEST_TRACKER,
                 SESSION_ANALYTICS,
                 SESSION_HISTORY ->
                    HitTarget.QOL_MINING;

            case QOL_SETTINGS ->
                    HitTarget.SECTION_QOL;
        };
    }

    static String sectionForModule(
            DashboardModule module) {

        if (module == null) {
            return null;
        }

        return switch (module) {
            case NONE ->
                    null;

            case MINING_TRACKER,
                 POWDER_CHEST_TRACKER,
                 SESSION_ANALYTICS,
                 SESSION_HISTORY,
                 QOL_SETTINGS ->
                    SECTION_QOL;
        };
    }

    static int targetTop(
            Layout layout,
            HitTarget target) {

        if (layout == null
                || target == null) {
            return -1;
        }

        return switch (target) {
            case NONE ->
                    -1;

            case OVERVIEW ->
                    layout.overviewY();

            case SECTION_MINING ->
                    layout.miningHeaderY();

            case TRACKER ->
                    layout.trackerY();

            case MINING_HUD ->
                    layout.miningHudY();

            case POWDER_CHEST_TRACKER ->
                    layout.powderChestTrackerY();

            case SECTION_SESSIONS ->
                    layout.sessionsHeaderY();

            case ANALYTICS ->
                    layout.analyticsY();

            case HISTORY ->
                    layout.historyY();

            case SECTION_SETTINGS ->
                    layout.settingsHeaderY();

            case APPEARANCE ->
                    layout.appearanceY();

            case HUD_LAYOUT ->
                    layout.hudLayoutY();

            case PROFILES ->
                    layout.profilesY();

            case SECTION_QOL ->
                    layout.qolHeaderY();


            case MARKET_WATCH ->
                    layout.marketWatchY();

            case QOL_COMBAT,
                 QOL_SLAYER,
                 QOL_FISHING,
                 QOL_FORAGING,
                 QOL_DUNGEONS,
                 QOL_KUUDRA,
                 QOL_EVENTS,
                 QOL_MINING,
                 QOL_GARDEN,
                 QOL_GUI,
                 QOL_UTILITIES,
                 QOL_HUD_DISPLAY,
                 QOL_RENDER,
                 QOL_INTERFACE ->
                    layout.qolPageY(
                            groupForHitTarget(
                                    target));
        };
    }

    static int targetHeight(
            HitTarget target) {

        if (target == null) {
            return ITEM_HEIGHT;
        }

        return switch (target) {
            case SECTION_MINING,
                 SECTION_SESSIONS,
                 SECTION_SETTINGS,
                 SECTION_QOL ->
                    HEADER_HIT_HEIGHT;

            default ->
                    ITEM_HEIGHT;
        };
    }

    static int scrollToReveal(
            int scrollPixels,
            int viewportHeight,
            int contentHeight,
            int top,
            int bottom) {

        int max = Math.max(
                0,
                contentHeight
                        - Math.max(
                        0,
                        viewportHeight));

        int next =
                Math.max(
                        0,
                        scrollPixels);

        if (top < next) {
            next =
                    Math.max(
                            0,
                            top);
        } else if (bottom
                > next + viewportHeight) {
            next =
                    Math.max(
                            0,
                            bottom - viewportHeight);
        }

        return Math.min(
                max,
                next);
    }

    private static boolean inItem(
            int x,
            int y,
            int itemX,
            int itemW,
            int itemY) {

        return itemY >= 0
                && inside(
                x,
                y,
                itemX,
                itemY,
                itemW,
                ITEM_HEIGHT);
    }

    private static boolean inClippedItem(
            int x,
            int y,
            int itemX,
            int itemW,
            int itemY,
            int clipTop,
            int clipHeight) {

        if (itemY < 0
                || clipHeight <= 0
                || clipTop < 0) {
            return false;
        }

        int visTop =
                Math.max(
                        itemY,
                        clipTop);

        int visBottom =
                Math.min(
                        itemY + ITEM_HEIGHT,
                        clipTop + clipHeight);

        if (visBottom - visTop < 10) {
            return false;
        }

        return inside(
                x,
                y,
                itemX,
                visTop,
                itemW,
                visBottom - visTop);
    }

    private static boolean inHeader(
            int x,
            int y,
            int headerX,
            int headerW,
            int headerY) {

        return headerY >= 0
                && inside(
                x,
                y,
                headerX,
                headerY,
                headerW,
                HEADER_HIT_HEIGHT);
    }

    private static boolean inside(
            int x,
            int y,
            int left,
            int top,
            int width,
            int height) {

        return x >= left
                && x < left + width
                && y >= top
                && y < top + height;
    }
}
