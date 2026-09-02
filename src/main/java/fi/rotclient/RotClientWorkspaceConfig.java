package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Persistent Client UI workspace: tabs + dashboard window position.
 * Schema version is independent of Rot Client application version.
 */
final class RotClientWorkspaceConfig {
    static final int SCHEMA_VERSION = 1;
    static final int MAX_TABS = 12;

    int schemaVersion = SCHEMA_VERSION;
    String activeTabId = "";
    List<RotClientWorkspaceTab> tabs = new ArrayList<>();

    /** Normalized dashboard origin X in [0,1] within the logical GUI viewport. */
    float clientUiNormX = 0.5F;
    /** Normalized dashboard origin Y in [0,1]. */
    float clientUiNormY = 0.5F;
    /**
     * Floating window size as a fraction of the viewport. {@code 0} keeps the
     * default 86% canvas until the user resizes.
     */
    float clientUiNormW;
    float clientUiNormH;
    /**
     * Windows-style placement: {@code floating}, {@code maximized}, {@code left},
     * or {@code right}.
     */
    String windowPlacement = RotClientWindowPlacementPolicy.FLOATING;

    /**
     * Multi-expand sidebar accordion sections that are currently open.
     * Null/missing deserializations normalize to all sections expanded.
     * An explicit empty list means every collapsible section is collapsed.
     */
    List<String> expandedSidebarSections = new ArrayList<>(
            RotClientSidebarNav.defaultExpandedSections());

    /**
     * Multi-expand Appearance color-group accordion IDs. Null/missing → all
     * known groups expanded. Explicit empty list → all collapsed. Unknown IDs
     * are dropped on normalize.
     */
    List<String> expandedAppearanceSections = new ArrayList<>(
            RotClientAppearanceNav.defaultExpandedSections());

    static RotClientWorkspaceConfig defaults() {
        RotClientWorkspaceConfig config = new RotClientWorkspaceConfig();
        RotClientWorkspaceTab overview = new RotClientWorkspaceTab(
                RotClientWorkspaceRoute.OVERVIEW);
        config.tabs.add(overview);
        config.activeTabId = overview.id;
        config.expandedSidebarSections = new ArrayList<>(
                RotClientSidebarNav.defaultExpandedSections());
        config.expandedAppearanceSections = new ArrayList<>(
                RotClientAppearanceNav.defaultExpandedSections());
        return config;
    }

    RotClientWorkspaceConfig copy() {
        RotClientWorkspaceConfig copy = new RotClientWorkspaceConfig();
        copy.schemaVersion = schemaVersion;
        copy.activeTabId = activeTabId == null ? "" : activeTabId;
        copy.clientUiNormX = clientUiNormX;
        copy.clientUiNormY = clientUiNormY;
        copy.clientUiNormW = clientUiNormW;
        copy.clientUiNormH = clientUiNormH;
        copy.windowPlacement = windowPlacement;
        copy.expandedSidebarSections = new ArrayList<>(
                RotClientSidebarNav.normalizeExpandedSections(
                        expandedSidebarSections));
        copy.expandedAppearanceSections = new ArrayList<>(
                RotClientAppearanceNav.normalizeExpandedSections(
                        expandedAppearanceSections));
        copy.tabs = new ArrayList<>();
        if (tabs != null) {
            for (RotClientWorkspaceTab tab : tabs) {
                if (tab != null) {
                    copy.tabs.add(tab.copy());
                }
            }
        }
        return copy;
    }

    void normalize() {
        schemaVersion = SCHEMA_VERSION;
        if (tabs == null) {
            tabs = new ArrayList<>();
        }
        List<RotClientWorkspaceTab> cleaned = new ArrayList<>();
        for (RotClientWorkspaceTab tab : tabs) {
            if (tab == null) {
                continue;
            }
            tab.normalize();
            cleaned.add(tab);
            if (cleaned.size() >= MAX_TABS) {
                break;
            }
        }
        if (cleaned.isEmpty()) {
            RotClientWorkspaceTab overview = new RotClientWorkspaceTab(
                    RotClientWorkspaceRoute.OVERVIEW);
            overview.normalize();
            cleaned.add(overview);
        }
        tabs = cleaned;
        clientUiNormX = clamp01(clientUiNormX);
        clientUiNormY = clamp01(clientUiNormY);
        clientUiNormW = clampUnit(clientUiNormW);
        clientUiNormH = clampUnit(clientUiNormH);
        windowPlacement = RotClientWindowPlacementPolicy.normalize(windowPlacement);
        if (expandedSidebarSections == null) {
            expandedSidebarSections = new ArrayList<>(
                    RotClientSidebarNav.defaultExpandedSections());
        } else {
            expandedSidebarSections =
                    RotClientSidebarNav.normalizeExpandedSections(
                            expandedSidebarSections);
        }
        if (expandedAppearanceSections == null) {
            expandedAppearanceSections = new ArrayList<>(
                    RotClientAppearanceNav.defaultExpandedSections());
        } else {
            expandedAppearanceSections =
                    RotClientAppearanceNav.normalizeExpandedSections(
                            expandedAppearanceSections);
        }
        if (activeTabId == null || activeTabId.isBlank() || findTab(activeTabId) == null) {
            activeTabId = tabs.get(0).id;
        }
    }

    RotClientWorkspaceTab findTab(String id) {
        if (id == null || tabs == null) {
            return null;
        }
        for (RotClientWorkspaceTab tab : tabs) {
            if (tab != null && id.equals(tab.id)) {
                return tab;
            }
        }
        return null;
    }

    RotClientWorkspaceTab activeTab() {
        normalize();
        RotClientWorkspaceTab tab = findTab(activeTabId);
        return tab != null ? tab : tabs.get(0);
    }

    int activeIndex() {
        normalize();
        for (int i = 0; i < tabs.size(); i++) {
            if (activeTabId.equals(tabs.get(i).id)) {
                return i;
            }
        }
        return 0;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.5F;
        }
        if (value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }

    /** Allows 0 (meaning "use layout default") unlike {@link #clamp01}. */
    private static float clampUnit(float value) {
        if (!Float.isFinite(value) || value < 0.0F) {
            return 0.0F;
        }
        if (value > 1.0F) {
            return 1.0F;
        }
        return value;
    }

    static String migrateNote(String ignoredAppVersion) {
        // Explicitly documents that app version must not wipe workspace.
        return "schema=" + SCHEMA_VERSION
                + ";app="
                + (ignoredAppVersion == null ? "" : ignoredAppVersion.toLowerCase(Locale.ROOT));
    }
}
