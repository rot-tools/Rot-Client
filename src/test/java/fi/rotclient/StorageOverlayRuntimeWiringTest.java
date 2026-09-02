package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageOverlayRuntimeWiringTest {
    @Test
    void overviewPagesRemainVisibleBeforeTheirContentsAreCached() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));

        assertTrue(source.contains("Map<StorageOverlayPolicy.Page, CachedPage> visiblePages"));
        assertTrue(source.contains("SELECTOR_SLOTS.keySet()"));
        assertTrue(source.contains("new CachedPage(page, List.of(), 0)"));
        assertTrue(source.contains("Open to preview"));
    }

    @Test
    void storageCacheHasAnExplicitLocalClearPath() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));

        assertTrue(source.contains("public static void clearObservedPages()"));
        assertTrue(source.contains("SELECTOR_SLOTS.clear();"));
        assertTrue(source.contains("CACHE.clear();"));
    }

    @Test
    void cachedCardsNeverInventAStorageCommandWhenNoServerSelectorExists() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));

        assertTrue(source.contains("StorageOverlayPolicy.pageCommand"));
        assertTrue(source.contains("sendCommand"));
        assertTrue(source.contains("shouldNavigatePage"));
        assertTrue(source.contains("shouldKeepCursorAcrossScreens"));
        assertTrue(!source.contains("rememberForNextScreen"));
        assertTrue(!source.contains("CustomCursorRuntime.render"));
        assertTrue(!source.contains("sendCommand(\"storage\")"));
    }

    @Test
    void catalogAndDashboardExposeAClearSearchAction() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/fi/rotclient/QolUtilityCatalog.java"));
        String dashboard = Files.readString(Path.of("src/client/java/fi/rotclient/QolUtilityDashboard.java"));

        assertTrue(catalog.contains("qol.storage_overlay.clear_search"));
        assertTrue(dashboard.contains("qol.storage_overlay.clear_search"));
        assertTrue(dashboard.contains("storageOverlaySearchQuery = \"\""));
    }

    @Test
    void searchFilteringRemainsOptInAndLocalToCachedPages() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));
        String catalog = Files.readString(Path.of("src/main/java/fi/rotclient/QolUtilityCatalog.java"));

        assertTrue(source.contains("storageOverlayFilterSearch"));
        assertTrue(source.contains("No cached page matches this search."));
        assertTrue(catalog.contains("qol.storage_overlay.filter_search"));
    }

    @Test
    void searchAlsoMarksMatchingPageCardsWithoutChangingServerData() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));

        assertTrue(source.contains("boolean pageSearchMatch"));
        assertTrue(source.contains("pageSearchMatch ? extras.storageOverlayHighlightColor"));
    }

    @Test
    void overlayReplacesTheVanillaStorageGuiInsteadOfDrawingBesideIt() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));
        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"));

        assertTrue(source.contains("public static boolean shouldReplaceVanilla"));
        assertTrue(source.contains("contentSlotStart()"));
        assertTrue(source.contains("drawSlotWell"));
        assertTrue(source.contains("enableScissor"));
        assertTrue(source.contains("playerSlotPosition"));
        assertTrue(mixin.contains("InventoryChromeRuntime.extractInventoryBackground"));
        assertTrue(mixin.contains("method = \"extractContents\""));
        assertTrue(!mixin.contains("method = \"extractBackground\""));
        assertTrue(mixin.contains("StorageOverlayRuntime.shouldReplaceVanilla"));
        assertTrue(mixin.contains("hoveredSlot = StorageOverlayRuntime.hoveredSlot"));
        assertTrue(mixin.contains("rotclient$renderStorageReplacement"));
        assertTrue(!mixin.contains("extractTransparentBackground"));
        String containerBackground = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ContainerScreenStorageOverlayMixin.java"));
        assertTrue(containerBackground.contains("@Mixin(ContainerScreen.class)"));
        assertTrue(containerBackground.contains("method = \"extractBackground\""));
        assertTrue(containerBackground.contains("StorageOverlayRuntime.shouldReplaceVanilla"));
        assertTrue(containerBackground.contains("extractTransparentBackground"));
        String mixinsJson = Files.readString(Path.of("src/client/resources/rotclient.client.mixins.json"));
        String cursorResetMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/GuiNoCursorResetMixin.java"));
        assertTrue(mixinsJson.contains("ContainerScreenStorageOverlayMixin"));
        assertTrue(!source.contains("left - panelWidth"));
        assertTrue(mixinsJson.contains("SlotPositionAccessor"));
        assertTrue(!source.contains("rememberForNextScreen"));
        assertTrue(cursorResetMixin.contains("storageTransition"));
        assertTrue(cursorResetMixin.contains("NoCursorResetPolicy.DEFAULT_TIMEOUT_MS"));
        assertTrue(source.contains("shouldNavigatePage"));
        assertTrue(mixin.contains("hasShiftDown"));
        assertTrue(mixin.contains("StorageOverlayRuntime.click"));
    }
}
