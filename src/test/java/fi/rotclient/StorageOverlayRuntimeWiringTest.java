package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageOverlayRuntimeWiringTest {
    @Test
    void serverSnapshotsGuardCacheUpdatesAndAllVanillaSlotInputs() throws Exception {
        String runtime = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));
        String packets = Files.readString(Path.of("src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"));
        String screen = Files.readString(Path.of("src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"));
        assertTrue(packets.contains("ClientBoundaryGuard.run(\"STORAGE_CONTENT_PACKET\""));
        assertTrue(packets.contains("StorageOverlayRuntime.onContainerContent(packet.containerId(), packet.items().size())"));
        assertTrue(runtime.contains("MENU_READINESS.acceptContents"));
        assertTrue(runtime.contains("if (!menuReady(screen)) return;"));
        assertTrue(runtime.contains("client.player.containerMenu != screen.getMenu()"));
        assertTrue(runtime.contains("allPagesConfirmedEmpty()"));
        assertTrue(runtime.contains("slotFullyVisible"));
        assertTrue(runtime.contains("getCarried()"));
        assertTrue(runtime.contains("overItem || hoveredPageItem"));
        assertTrue(runtime.contains("StorageOverlayPolicy.layoutColumns(layout"));
        assertFalse(runtime.contains("clampColumns(extras.storageOverlayColumns)"));
        assertFalse(runtime.contains("pageHasIdentity(CACHE.get(prefetchWaitingFor))"));
        assertTrue(screen.contains("STORAGE_SLOT_INPUT"));
        assertTrue(screen.contains("StorageOverlayRuntime.shouldBlockSlotInput"));
    }

    @Test
    void overviewPagesRemainVisibleBeforeTheirContentsAreCached() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));

        assertTrue(source.contains("Map<StorageOverlayPolicy.Page, CachedPage> visiblePages"));
        assertTrue(source.contains("SELECTOR_SLOTS.keySet()"));
        assertTrue(source.contains("new CachedPage(page, List.of(), 0)"));
        assertTrue(source.contains("slotRows"));
        assertTrue(source.contains("Search items..."));
        assertTrue(source.contains("highlightSearchMatches"));
        assertTrue(source.contains("paintSearchOutline"));
        assertTrue(source.contains("searchGlowPixel"));
        assertTrue(source.contains("sawOverview"));
        assertTrue(source.contains("drawSearchField"));
        assertTrue(source.contains("drawCloseButton"));
        assertTrue(source.contains("drawValueIcon"));
        assertTrue(source.contains("clipSearchFromEnd"));
        assertTrue(source.contains("scheduleSave"));
        assertTrue(source.contains("texture"));
        assertTrue(source.contains("closeOverlay"));
        assertTrue(!source.contains("Open to preview"));
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
        assertTrue(catalog.contains("qol.storage_overlay.reload_pages"));
        assertTrue(dashboard.contains("qol.storage_overlay.clear_search"));
        assertTrue(dashboard.contains("StorageOverlayRuntime.requestReloadAll"));
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
        assertTrue(source.contains("pageSearchMatch ? RotClientTheme.VIOLET"));
    }

    @Test
    void overlayReplacesTheVanillaStorageGuiInsteadOfDrawingBesideIt() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));
        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"));

        assertTrue(source.contains("public static boolean shouldReplaceVanilla"));
        assertTrue(source.contains("showSkyblockInventoryUi"));
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
        assertTrue(mixin.contains("ci.cancel()"));
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
        assertTrue(mixinsJson.contains("MouseHandlerStorageOverlayMixin"));
        assertTrue(mixinsJson.contains("MouseHandlerCursorAccessor"));
        assertTrue(!source.contains("left - panelWidth"));
        assertTrue(mixinsJson.contains("SlotPositionAccessor"));
        assertTrue(!source.contains("rememberForNextScreen"));
        assertTrue(cursorResetMixin.contains("storageTransition"));
        assertTrue(cursorResetMixin.contains("NoCursorResetPolicy.DEFAULT_TIMEOUT_MS"));
        assertTrue(source.contains("RotClientUiDraw.drawScrollbar"));
        assertTrue(source.contains("PAGE_SCROLL"));
        assertTrue(source.contains("beginThumbDrag"));
        assertTrue(source.contains("RotClientTheme.DASHBOARD_HEADER"));
        assertTrue(mixin.contains("StorageOverlayRuntime.drag"));
        assertTrue(mixin.contains("StorageOverlayRuntime.mouseReleased"));
    }

    @Test
    void overlayKeepsKnownPagesAndClicksLiveSlotsWithoutRecentering() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/StorageOverlayRuntime.java"));
        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"));
        String guiMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/GuiNoCursorResetMixin.java"));
        String mouseMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/MouseHandlerStorageOverlayMixin.java"));
        String keyMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenMenuKeybindMixin.java"));
        String charMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/KeyboardHandlerRingMixin.java"));
        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"));
        assertTrue(source.contains("shouldKeepExistingCache"));
        assertTrue(source.contains("PAGE_FINGERPRINTS"));
        assertTrue(source.contains("shouldSuppressOutsideClick"));
        assertTrue(source.contains("shouldPinClosedContainer"));
        assertTrue(source.contains("shouldKeepUngrabbedCursor"));
        assertTrue(source.contains("forcePreserveNext"));
        assertTrue(source.contains("StorageOverlayPolicy.CACHE_FILE"));
        String policy = Files.readString(Path.of("src/main/java/fi/rotclient/StorageOverlayPolicy.java"));
        assertTrue(policy.contains("rotclient-storage-cache.json"));
        assertTrue(source.contains("addProperty(\"nbt\""));
        assertTrue(source.contains("addProperty(\"texture\""));
        assertTrue(source.contains("ResolvableProfile.createResolved"));
        assertTrue(source.contains("applyValueTooltip"));
        assertTrue(source.contains("flushForShutdown"));
        assertTrue(source.contains("encodeStack"));
        assertTrue(source.contains("incomingIsPlaceholder"));
        assertTrue(source.contains("codecStackNeedsFallback"));
        assertTrue(source.contains("requestReloadAll"));
        assertTrue(source.contains("shouldReloadCache"));
        assertTrue(source.contains("useLiveDisplay"));
        assertTrue(source.contains("pagesToPrefetch"));
        assertTrue(source.contains("directoryScanCompleted"));
        assertTrue(source.contains("pendingRefresh"));
        assertTrue(source.contains("rememberClickedPage"));
        assertTrue(source.contains("charTyped"));
        assertTrue(source.contains("keyPressed"));
        assertTrue(source.contains("insideSearchField"));
        assertTrue(!source.contains("overlaySlotIndex"));
        assertTrue(mixin.contains("hasClickedOutside"));
        assertTrue(mixin.contains("shouldSuppressOutsideClick"));
        assertTrue(mixin.contains("extractLabels"));
        assertTrue(guiMixin.contains("shouldPinClosedContainer"));
        assertTrue(mouseMixin.contains("shouldKeepUngrabbedCursor"));
        assertTrue(keyMixin.contains("StorageOverlayRuntime.keyPressed"));
        assertTrue(charMixin.contains("StorageOverlayRuntime.charTyped"));
        assertTrue(source.contains("highlightSearchMatches"));
        assertTrue(client.contains("StorageOverlayRuntime.onJoin"));
        assertTrue(client.contains("StorageOverlayRuntime.tick"));
        assertTrue(client.contains("shouldStealOverlayWheel"));
        assertTrue(client.contains("CustomTooltipRuntime.clear()"));
        assertTrue(source.contains("SkyBlockMarketQuoteService.current()"));
        assertTrue(source.contains("marketUnitValue"));
        assertTrue(source.contains("lowestBin()"));
        assertTrue(source.contains("restoreMarketIdentity"));
        assertTrue(source.contains("addProperty(\"marketId\""));
        String quotes = Files.readString(Path.of(
                "src/client/java/fi/rotclient/SkyBlockMarketQuoteService.java"));
        assertTrue(quotes.contains("api.eliteskyblock.com/resources/auctions/neu"));
        assertTrue(quotes.contains("lb.tricked.pro/lowestbins"));
        assertTrue(!quotes.contains("shouldFetchRemoteQuotes"));
    }
}
