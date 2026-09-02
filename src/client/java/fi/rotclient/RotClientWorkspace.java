package fi.rotclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory Client UI workspace controller. Domain state stays global;
 * this only tracks view routes and dashboard placement.
 */
final class RotClientWorkspace {
    private RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
    private boolean dirty;
    private long lastSaveMillis;

    void loadFromDisk() {
        config = RotClientWorkspaceStore.load();
        config.normalize();
        dirty = false;
    }

    RotClientWorkspaceConfig config() {
        return config;
    }

    List<RotClientWorkspaceTab> tabsView() {
        config.normalize();
        return Collections.unmodifiableList(config.tabs);
    }

    RotClientWorkspaceTab activeTab() {
        return config.activeTab();
    }

    RotClientWorkspaceRoute activeRoute() {
        return activeTab().resolvedRoute();
    }

    boolean canAddTab() {
        config.normalize();
        return config.tabs.size() < RotClientWorkspaceConfig.MAX_TABS;
    }

    RotClientWorkspaceTab addOverviewTab() {
        return addTab(RotClientWorkspaceRoute.OVERVIEW);
    }

    RotClientWorkspaceTab addTab(RotClientWorkspaceRoute route) {
        config.normalize();
        RotClientWorkspaceRoute safe = route == null
                ? RotClientWorkspaceRoute.OVERVIEW
                : route;
        if (!canAddTab()) {
            navigateActive(safe);
            return activeTab();
        }
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab(safe);
        tab.normalize();
        config.tabs.add(tab);
        config.activeTabId = tab.id;
        markDirtyAndSave();
        return tab;
    }

    void activate(String tabId) {
        config.normalize();
        if (config.findTab(tabId) == null) {
            return;
        }
        if (tabId.equals(config.activeTabId)) {
            return;
        }
        config.activeTabId = tabId;
        markDirtyAndSave();
    }

    void navigateActive(RotClientWorkspaceRoute route) {
        config.normalize();
        RotClientWorkspaceTab tab = activeTab();
        RotClientWorkspaceRoute safe = route == null
                ? RotClientWorkspaceRoute.OVERVIEW
                : route;
        if (tab.route.equals(safe.id())) {
            return;
        }
        tab.route = safe.id();
        tab.scrollPixels = 0;
        markDirtyAndSave();
    }

    void setQolView(String groupId, String moduleId) {
        config.normalize();
        RotClientWorkspaceTab tab = activeTab();
        String group = groupId == null ? "" : groupId.trim();
        String module = moduleId == null ? "" : moduleId.trim();
        if (group.equals(tab.qolGroup == null ? "" : tab.qolGroup)
                && module.equals(tab.qolModuleId == null ? "" : tab.qolModuleId)) {
            return;
        }
        tab.qolGroup = group;
        tab.qolModuleId = module;
        markDirtyAndSave();
    }

    void closeTab(String tabId) {
        config.normalize();
        int index = -1;
        for (int i = 0; i < config.tabs.size(); i++) {
            if (config.tabs.get(i).id.equals(tabId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return;
        }
        boolean closingActive = config.activeTabId.equals(tabId);
        config.tabs.remove(index);
        if (config.tabs.isEmpty()) {
            RotClientWorkspaceTab overview = new RotClientWorkspaceTab(
                    RotClientWorkspaceRoute.OVERVIEW);
            overview.normalize();
            config.tabs.add(overview);
            config.activeTabId = overview.id;
        } else if (closingActive) {
            int next = Math.min(index, config.tabs.size() - 1);
            config.activeTabId = config.tabs.get(next).id;
        }
        markDirtyAndSave();
    }

    void reorder(int fromIndex, int toIndex) {
        config.normalize();
        if (fromIndex < 0 || fromIndex >= config.tabs.size()
                || toIndex < 0 || toIndex >= config.tabs.size()
                || fromIndex == toIndex) {
            return;
        }
        RotClientWorkspaceTab tab = config.tabs.remove(fromIndex);
        config.tabs.add(toIndex, tab);
        markDirtyAndSave();
    }

    void setActiveScroll(int scrollPixels) {
        RotClientWorkspaceTab tab = activeTab();
        int safe = Math.max(0, scrollPixels);
        if (tab.scrollPixels == safe) {
            return;
        }
        tab.scrollPixels = safe;
        dirty = true;
    }

    void setClientUiNorm(float normX, float normY) {
        float x = clamp01(normX);
        float y = clamp01(normY);
        if (config.clientUiNormX == x && config.clientUiNormY == y) {
            return;
        }
        config.clientUiNormX = x;
        config.clientUiNormY = y;
        dirty = true;
    }

    void setFloatingWindow(float normX, float normY, float normW, float normH) {
        config.clientUiNormX = clamp01(normX);
        config.clientUiNormY = clamp01(normY);
        config.clientUiNormW = normW;
        config.clientUiNormH = normH;
        config.windowPlacement = RotClientWindowPlacementPolicy.FLOATING;
        dirty = true;
    }

    void setWindowPlacement(String placement) {
        String next = RotClientWindowPlacementPolicy.normalize(placement);
        if (next.equals(config.windowPlacement)) {
            return;
        }
        config.windowPlacement = next;
        markDirtyAndSave();
    }

    void toggleMaximize() {
        setWindowPlacement(
                RotClientWindowPlacementPolicy.toggleMaximize(config.windowPlacement));
    }

    void resetClientUiPosition() {
        config.clientUiNormX = 0.5F;
        config.clientUiNormY = 0.5F;
        markDirtyAndSave();
    }

    /**
     * Multi-expand sidebar accordion toggle. Persists immediately so reopen
     * restores which sections were open.
     */
    void toggleSidebarSection(String sectionId) {
        config.normalize();
        List<String> next = RotClientSidebarNav.toggleSection(
                config.expandedSidebarSections, sectionId);
        config.expandedSidebarSections = new ArrayList<>(next);
        markDirtyAndSave();
    }

    List<String> expandedSidebarSections() {
        config.normalize();
        return Collections.unmodifiableList(config.expandedSidebarSections);
    }

    void toggleAppearanceSection(String sectionId) {
        config.normalize();
        List<String> next = RotClientAppearanceNav.toggleSection(
                config.expandedAppearanceSections, sectionId);
        config.expandedAppearanceSections = new ArrayList<>(next);
        markDirtyAndSave();
    }

    boolean isAppearanceSectionExpanded(String sectionId) {
        config.normalize();
        return RotClientAppearanceNav.isExpanded(
                config.expandedAppearanceSections, sectionId);
    }

    List<String> expandedAppearanceSections() {
        config.normalize();
        return Collections.unmodifiableList(config.expandedAppearanceSections);
    }

    void flushIfDirty() {
        if (!dirty) {
            return;
        }
        saveNow();
    }

    void saveNow() {
        config.normalize();
        RotClientWorkspaceStore.save(config);
        dirty = false;
        lastSaveMillis = System.currentTimeMillis();
    }

    private void markDirtyAndSave() {
        dirty = true;
        long now = System.currentTimeMillis();
        if (now - lastSaveMillis > 250L) {
            saveNow();
        }
    }

    /** Pure helper for tests — mutates a config copy without disk. */
    static RotClientWorkspaceConfig closeTabOn(
            RotClientWorkspaceConfig source,
            String tabId) {
        RotClientWorkspaceConfig config = source.copy();
        config.normalize();
        int index = -1;
        for (int i = 0; i < config.tabs.size(); i++) {
            if (config.tabs.get(i).id.equals(tabId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return config;
        }
        boolean closingActive = config.activeTabId.equals(tabId);
        config.tabs.remove(index);
        if (config.tabs.isEmpty()) {
            RotClientWorkspaceTab overview = new RotClientWorkspaceTab(
                    RotClientWorkspaceRoute.OVERVIEW);
            overview.normalize();
            config.tabs.add(overview);
            config.activeTabId = overview.id;
        } else if (closingActive) {
            int next = Math.min(index, config.tabs.size() - 1);
            config.activeTabId = config.tabs.get(next).id;
        }
        config.normalize();
        return config;
    }

    static RotClientWorkspaceConfig reorderOn(
            RotClientWorkspaceConfig source,
            int from,
            int to) {
        RotClientWorkspaceConfig config = source.copy();
        config.normalize();
        if (from < 0 || from >= config.tabs.size()
                || to < 0 || to >= config.tabs.size()
                || from == to) {
            return config;
        }
        RotClientWorkspaceTab tab = config.tabs.remove(from);
        config.tabs.add(to, tab);
        config.normalize();
        return config;
    }

    static RotClientWorkspaceConfig addOverviewOn(RotClientWorkspaceConfig source) {
        return addTabOn(source, RotClientWorkspaceRoute.OVERVIEW);
    }

    static RotClientWorkspaceConfig addTabOn(
            RotClientWorkspaceConfig source,
            RotClientWorkspaceRoute route) {
        RotClientWorkspaceConfig config = source.copy();
        config.normalize();
        if (config.tabs.size() >= RotClientWorkspaceConfig.MAX_TABS) {
            RotClientWorkspaceTab tab = config.activeTab();
            tab.route = (route == null
                    ? RotClientWorkspaceRoute.OVERVIEW
                    : route).id();
            tab.scrollPixels = 0;
            config.normalize();
            return config;
        }
        RotClientWorkspaceTab tab = new RotClientWorkspaceTab(route);
        tab.normalize();
        config.tabs.add(tab);
        config.activeTabId = tab.id;
        config.normalize();
        return config;
    }

    /** Pure helper: navigate only the active tab route. */
    static RotClientWorkspaceConfig navigateActiveOn(
            RotClientWorkspaceConfig source,
            RotClientWorkspaceRoute route) {
        RotClientWorkspaceConfig config = source.copy();
        config.normalize();
        RotClientWorkspaceTab tab = config.activeTab();
        RotClientWorkspaceRoute safe = route == null
                ? RotClientWorkspaceRoute.OVERVIEW
                : route;
        tab.route = safe.id();
        tab.scrollPixels = 0;
        config.normalize();
        return config;
    }

    static RotClientWorkspaceConfig activateOn(
            RotClientWorkspaceConfig source,
            String tabId) {
        RotClientWorkspaceConfig config = source.copy();
        config.normalize();
        if (config.findTab(tabId) == null) {
            return config;
        }
        config.activeTabId = tabId;
        config.normalize();
        return config;
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
}
