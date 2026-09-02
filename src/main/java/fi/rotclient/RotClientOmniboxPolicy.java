package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Chrome-style omnibox: search only Rot Client pages/settings, then either
 * navigate the current tab, switch to an already-open tab, or open a new one.
 */
public final class RotClientOmniboxPolicy {
    public static final int SUGGESTION_LIMIT = 8;
    public static final int QUERY_MAX_CODEPOINTS = 48;

    public enum Action {
        NAVIGATE_CURRENT,
        OPEN_NEW_TAB,
        FOCUS_EXISTING_TAB
    }

    public record Decision(
            Action action,
            RotClientWorkspaceRoute route,
            String entryId,
            int existingTabIndex) {
    }

    private RotClientOmniboxPolicy() {
    }

    public static List<RotClientSettingsIndex.Entry> suggest(String query) {
        return RotClientSettingsIndex.search(query, SUGGESTION_LIMIT);
    }

    public static String addressLabel(
            RotClientWorkspaceRoute route,
            String query,
            boolean focused) {
        if (focused) {
            return query == null ? "" : query;
        }
        if (query != null && !query.isBlank()) {
            return query;
        }
        String id = route == null
                ? RotClientWorkspaceRoute.OVERVIEW.id()
                : route.id();
        return "rot://" + id.replace('_', '-');
    }

    public static RotClientWorkspaceRoute routeFor(RotClientSettingsIndex.Entry entry) {
        if (entry == null || entry.destination() == null) {
            return RotClientWorkspaceRoute.OVERVIEW;
        }
        return switch (entry.destination()) {
            case MINING_TRACKER -> RotClientWorkspaceRoute.MINING_TRACKER;
            case QOL_SETTINGS -> RotClientWorkspaceRoute.QOL_SETTINGS;
            case APPEARANCE -> appearanceRoute(entry.id());
            case HUD_EDITOR -> RotClientWorkspaceRoute.MINING_TRACKER;
            case SESSION_ANALYTICS -> RotClientWorkspaceRoute.SESSION_ANALYTICS;
            case SESSION_HISTORY -> RotClientWorkspaceRoute.SESSION_HISTORY;
            case OVERVIEW -> RotClientWorkspaceRoute.OVERVIEW;
        };
    }

    public static boolean opensHudEditor(RotClientSettingsIndex.Entry entry) {
        return entry != null
                && entry.destination() == RotClientSettingsIndex.Destination.HUD_EDITOR;
    }

    public static Decision decide(
            RotClientSettingsIndex.Entry entry,
            List<String> openRouteIds,
            int activeIndex,
            boolean openInNewTab,
            int tabCount,
            int maxTabs) {
        if (entry == null) {
            return new Decision(
                    Action.NAVIGATE_CURRENT,
                    RotClientWorkspaceRoute.OVERVIEW,
                    "",
                    -1);
        }
        RotClientWorkspaceRoute route = routeFor(entry);
        int existing = indexOfRoute(openRouteIds, route.id(), activeIndex);
        if (!openInNewTab && existing >= 0) {
            return new Decision(Action.FOCUS_EXISTING_TAB, route, entry.id(), existing);
        }
        boolean canOpen = openInNewTab && tabCount < Math.max(1, maxTabs);
        if (canOpen) {
            return new Decision(Action.OPEN_NEW_TAB, route, entry.id(), -1);
        }
        return new Decision(Action.NAVIGATE_CURRENT, route, entry.id(), -1);
    }

    private static int indexOfRoute(List<String> openRouteIds, String routeId, int activeIndex) {
        if (openRouteIds == null || routeId == null) {
            return -1;
        }
        for (int i = 0; i < openRouteIds.size(); i++) {
            if (i == activeIndex) {
                continue;
            }
            if (routeId.equals(openRouteIds.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static RotClientWorkspaceRoute appearanceRoute(String entryId) {
        if ("appearance.reset".equals(entryId)) {
            return RotClientWorkspaceRoute.APPEARANCE_RESET;
        }
        return RotClientWorkspaceRoute.APPEARANCE_OVERVIEW;
    }

    static List<String> routeIds(List<RotClientWorkspaceTab> tabs) {
        List<String> ids = new ArrayList<>();
        if (tabs == null) {
            return ids;
        }
        for (RotClientWorkspaceTab tab : tabs) {
            ids.add(tab == null ? RotClientWorkspaceRoute.OVERVIEW.id() : tab.route);
        }
        return ids;
    }
}
