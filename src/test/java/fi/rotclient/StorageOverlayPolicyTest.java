package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StorageOverlayPolicyTest {
    @Test void parsesHypixelStorageTitles() {
        assertTrue(StorageOverlayPolicy.isOverviewTitle("Storage"));
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 1),
                StorageOverlayPolicy.pageFromTitle("Ender Chest").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 3),
                StorageOverlayPolicy.pageFromTitle("Ender Chest #3").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 12),
                StorageOverlayPolicy.pageFromTitle("Large Backpack Page 12").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 1),
                StorageOverlayPolicy.pageFromTitle("Ender Chest (1/9)").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 4),
                StorageOverlayPolicy.pageFromTitle("Backpack [4/18]").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 3),
                StorageOverlayPolicy.pageFromTitle("Ender Chest ✦ (3/9)").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 4),
                StorageOverlayPolicy.pageFromTitle("Greater Backpack ✦ (Slot #4)").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 1),
                StorageOverlayPolicy.pageFromTitle("Greater Backpack").orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 1),
                StorageOverlayPolicy.pageFromTitle("Backpack").orElseThrow());
        assertTrue(StorageOverlayPolicy.pageFromTitle("Chest").isEmpty());
    }

    @Test void mapsHypixelOverviewSlotIndices() {
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 1),
                StorageOverlayPolicy.pageFromOverviewSlotIndex(9).orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 9),
                StorageOverlayPolicy.pageFromOverviewSlotIndex(17).orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 1),
                StorageOverlayPolicy.pageFromOverviewSlotIndex(27).orElseThrow());
        assertEquals(new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 18),
                StorageOverlayPolicy.pageFromOverviewSlotIndex(44).orElseThrow());
        assertTrue(StorageOverlayPolicy.pageFromOverviewSlotIndex(0).isEmpty());
        assertTrue(StorageOverlayPolicy.pageFromOverviewSlotIndex(18).isEmpty());
    }

    @Test void skipsTheHypixelControlRowAndLockedSelectors() {
        assertEquals(9, StorageOverlayPolicy.contentSlotStart());
        assertEquals(45, StorageOverlayPolicy.contentSlotCount(54));
        assertEquals(36, StorageOverlayPolicy.contentSlotCount(45));
        assertEquals(0, StorageOverlayPolicy.contentSlotCount(9));
        assertEquals("enderchest 3", StorageOverlayPolicy.pageCommand(
                new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 3)));
        assertEquals("backpack 12", StorageOverlayPolicy.pageCommand(
                new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 12)));
        assertTrue(StorageOverlayPolicy.isLockedSelectorItem("minecraft:red_stained_glass_pane"));
        assertTrue(StorageOverlayPolicy.isLockedSelectorItem("brown_stained_glass_pane"));
        assertFalse(StorageOverlayPolicy.isLockedSelectorItem("minecraft:ender_chest"));
    }

    @Test void centersTheReplacementOverlayAndKeepsPlayerInventoryOnAGrid() {
        var layout = StorageOverlayPolicy.layout(960, 540, 3, 5, 324);
        assertTrue(layout.panelX() > 0);
        assertEquals(960 / 2 - layout.panelWidth() / 2, layout.panelX());
        assertEquals(960 / 2 - StorageOverlayPolicy.PLAYER_WIDTH / 2, layout.playerX());
        assertArrayEquals(new int[] {layout.playerX() + 12, layout.playerY() + 74},
                StorageOverlayPolicy.playerSlotPosition(layout.playerX(), layout.playerY(), 0));
        assertArrayEquals(new int[] {13, 42},
                StorageOverlayPolicy.contentSlotPosition(10, 20, 0));
        assertArrayEquals(new int[] {13, 60},
                StorageOverlayPolicy.contentSlotPosition(10, 20, 9));
        assertEquals(9, StorageOverlayPolicy.playerInventoryIndex(0));
        assertEquals(0, StorageOverlayPolicy.playerInventoryIndex(27));
        assertEquals(8, StorageOverlayPolicy.playerInventoryIndex(35));
    }

    @Test void clampsLayoutInputs() {
        assertEquals(1, StorageOverlayPolicy.clampColumns(-1));
        assertEquals(5, StorageOverlayPolicy.clampColumns(99));
        assertEquals(180, StorageOverlayPolicy.clampHeight(10));
        assertEquals(40, StorageOverlayPolicy.clampScrollSpeed(99));
        assertTrue(StorageOverlayPolicy.matchesSearch("Warden Heart", "warden"));
        assertTrue(StorageOverlayPolicy.matchesSearch("Warden Heart", "heart warden"));
        assertFalse(StorageOverlayPolicy.matchesSearch("Warden Heart", "necron"));
        assertFalse(StorageOverlayPolicy.matchesSearch("Warden Heart", "warden necron"));
        assertTrue(StorageOverlayPolicy.matchesSearch("Warden Heart", ""));
        assertEquals(2, StorageOverlayPolicy.matchingNameCount(
                java.util.List.of("Warden Heart", "Necron Handle", "Warden Helmet"), "warden"));
        assertEquals(0, StorageOverlayPolicy.matchingNameCount(
                java.util.List.of("Warden Heart"), ""));
        assertEquals(0, StorageOverlayPolicy.clampScrollOffset(-20, 500, 200));
        assertEquals(300, StorageOverlayPolicy.clampScrollOffset(999, 500, 200));
        assertEquals(0, StorageOverlayPolicy.clampScrollOffset(50, 100, 200));
    }

    @Test void searchSummaryMakesLocalCacheScopeExplicit() {
        assertEquals("Storage", StorageOverlayPolicy.searchSummary("", 0, 0));
        assertEquals("Storage · 1 matches · 1 page cached",
                StorageOverlayPolicy.searchSummary("aspect", 1, 1));
        assertEquals("Storage · 0 matches · 2 pages cached",
                StorageOverlayPolicy.searchSummary("term", 2, 0));
    }

    @Test void pageClicksDoNotStealShiftClicksOrPlayerInventory() {
        var layout = StorageOverlayPolicy.layout(960, 540, 3, 5, 324);
        int cardX = layout.innerX();
        int cardY = layout.innerY() + 18;
        assertTrue(StorageOverlayPolicy.shouldNavigatePage(
                false, false, false, cardX + 8, cardY + 8, cardX, cardY, 166, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                true, false, false, cardX + 8, cardY + 8, cardX, cardY, 166, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                false, true, false, cardX + 8, cardY + 8, cardX, cardY, 166, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                false, false, true, cardX + 8, cardY + 8, cardX, cardY, 166, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                false, false, false,
                layout.playerX() + 20, layout.playerY() + 20,
                cardX, cardY, 166, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldOpenPage(true, false, false, false));
        assertTrue(StorageOverlayPolicy.shouldOpenPage(false, false, false, false));
    }

    @Test void pageClicksStayInsideTheVisibleViewportAndNeverStealShiftOrInventory() {
        var layout = StorageOverlayPolicy.layout(960, 540, 3, 5, 324);
        int cardX = layout.innerX() + 8;
        int cardY = layout.innerY() + 8;
        assertTrue(StorageOverlayPolicy.shouldNavigatePage(
                false, false, false, cardX + 4, cardY + 4,
                cardX, cardY, 80, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                true, false, false, cardX + 4, cardY + 4,
                cardX, cardY, 80, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                false, true, false, cardX + 4, cardY + 4,
                cardX, cardY, 80, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                false, false, true, cardX + 4, cardY + 4,
                cardX, cardY, 80, 80, layout));
        assertFalse(StorageOverlayPolicy.shouldNavigatePage(
                false, false, false, layout.playerX() + 10, layout.playerY() + 10,
                cardX, cardY, 80, 80, layout));
        assertFalse(StorageOverlayPolicy.clippedHit(
                layout.innerX() + 4,
                layout.innerY() + layout.innerHeight() + 20,
                layout.innerX(),
                layout.innerY() + layout.innerHeight() - 10,
                80,
                80,
                layout.innerX(),
                layout.innerY(),
                layout.innerWidth(),
                layout.innerHeight()));
    }

    @Test
    void emptyPageTicksDoNotWipeAKnownPreview() {
        assertTrue(StorageOverlayPolicy.shouldKeepExistingCache(true, true));
        assertFalse(StorageOverlayPolicy.shouldKeepExistingCache(true, false));
        assertFalse(StorageOverlayPolicy.shouldKeepExistingCache(false, true));
        assertTrue(StorageOverlayPolicy.incomingIsPlaceholder(true, false, false));
        assertFalse(StorageOverlayPolicy.incomingIsPlaceholder(true, true, false));
        assertTrue(StorageOverlayPolicy.incomingIsPlaceholder(true, false, true));
        assertFalse(StorageOverlayPolicy.incomingIsPlaceholder(true, false, true, true));
        assertFalse(StorageOverlayPolicy.useLiveDisplay(true, false));
        assertTrue(StorageOverlayPolicy.useLiveDisplay(true, true));
        assertFalse(StorageOverlayPolicy.useLiveDisplay(false, true));
    }

    @Test
    void storageCacheReloadsWhenCodecBecomesReadyAndSkipsEmptyShutdownWrites() {
        assertEquals("rotclient-storage-cache.json", StorageOverlayPolicy.CACHE_FILE);
        assertEquals("storage", StorageOverlayPolicy.OVERVIEW_COMMAND);
        assertFalse(StorageOverlayPolicy.shouldReloadCache(true, true, true, true, true));
        assertTrue(StorageOverlayPolicy.shouldReloadCache(false, true, true, false, false));
        assertTrue(StorageOverlayPolicy.shouldReloadCache(true, false, true, false, true));
        assertFalse(StorageOverlayPolicy.shouldReloadCache(true, false, true, true, true));
        assertFalse(StorageOverlayPolicy.shouldReloadCache(true, true, false, false, false));
        assertTrue(StorageOverlayPolicy.shouldSkipEmptyStorageSave(false, true, false));
        assertFalse(StorageOverlayPolicy.shouldSkipEmptyStorageSave(false, true, true));
        assertFalse(StorageOverlayPolicy.shouldSkipEmptyStorageSave(true, true, false));
        assertTrue(StorageOverlayPolicy.shouldSkipUnreadyShutdownSave(false, true));
        assertFalse(StorageOverlayPolicy.shouldSkipUnreadyShutdownSave(true, true));
        assertTrue(StorageOverlayPolicy.codecStackNeedsFallback(false, false, true, false, false));
        assertFalse(StorageOverlayPolicy.codecStackNeedsFallback(false, true, true, true, true));
        assertTrue(StorageOverlayPolicy.codecStackNeedsFallback(true, false, false, true, false));
    }

    @Test
    void storagePrefetchFillsMissingPagesThenSettles() {
        assertTrue(StorageOverlayPolicy.pageNeedsRefresh(true, false, false));
        assertFalse(StorageOverlayPolicy.pageNeedsRefresh(true, true, false));
        assertTrue(StorageOverlayPolicy.pageNeedsRefresh(true, true, true));
        assertFalse(StorageOverlayPolicy.pageNeedsRefresh(false, false, false));
        assertTrue(StorageOverlayPolicy.shouldStartPrefetch(true, true, false, false, 3, 2));
        assertFalse(StorageOverlayPolicy.shouldStartPrefetch(true, true, true, false, 3, 2));
        assertTrue(StorageOverlayPolicy.shouldStartPrefetch(true, true, true, true, 3, 0));
        assertFalse(StorageOverlayPolicy.shouldStartPrefetch(true, false, false, false, 3, 2));
        assertFalse(StorageOverlayPolicy.prefetchPageSettled(true, true, 0));
        assertTrue(StorageOverlayPolicy.prefetchPageSettled(
                true, true, StorageOverlayPolicy.PREFETCH_SETTLE_TICKS));
        assertTrue(StorageOverlayPolicy.prefetchPageSettled(
                false, false, StorageOverlayPolicy.PREFETCH_TIMEOUT_TICKS));
        var first = new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 2);
        var second = new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 1);
        assertEquals(java.util.List.of(first, second), StorageOverlayPolicy.prefetchOrder(java.util.List.of(second, first)));
    }

    @Test
    void laterStorageOpensReloadOnlyClickedPages() {
        var ender1 = new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 1);
        var ender2 = new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.ENDER_CHEST, 2);
        var pack3 = new StorageOverlayPolicy.Page(StorageOverlayPolicy.Kind.BACKPACK, 3);
        var unlocked = java.util.List.of(ender1, ender2, pack3);
        assertEquals(
                java.util.List.of(ender1, ender2, pack3),
                StorageOverlayPolicy.pagesToPrefetch(unlocked, java.util.List.of(), java.util.List.of(), false, false));
        assertEquals(
                java.util.List.of(ender2, pack3),
                StorageOverlayPolicy.pagesToPrefetch(unlocked, java.util.List.of(ender1), java.util.List.of(), false, false));
        assertEquals(
                java.util.List.of(),
                StorageOverlayPolicy.pagesToPrefetch(unlocked, unlocked, java.util.List.of(), true, false));
        var clicked = StorageOverlayPolicy.withClickedPage(
                StorageOverlayPolicy.withClickedPage(java.util.List.of(), ender1), pack3);
        assertEquals(java.util.List.of(ender1, pack3), clicked);
        assertEquals(
                java.util.List.of(pack3, ender1),
                StorageOverlayPolicy.pagesToPrefetch(unlocked, unlocked, clicked, true, false));
        assertEquals(
                java.util.List.of(ender1, ender2, pack3),
                StorageOverlayPolicy.pagesToPrefetch(unlocked, unlocked, java.util.List.of(pack3), true, true));
        assertTrue(StorageOverlayPolicy.inferDirectoryScanCompleted(false, false, unlocked, unlocked));
        assertFalse(StorageOverlayPolicy.inferDirectoryScanCompleted(false, false, java.util.List.of(ender1), unlocked));
        assertFalse(StorageOverlayPolicy.inferDirectoryScanCompleted(false, false, java.util.List.of(), java.util.List.of()));
        assertFalse(StorageOverlayPolicy.inferDirectoryScanCompleted(true, false, unlocked, unlocked));
        assertTrue(StorageOverlayPolicy.directoryScanComplete(false, unlocked, unlocked));
        assertFalse(StorageOverlayPolicy.directoryScanComplete(false, unlocked, java.util.List.of(ender1)));
        assertFalse(StorageOverlayPolicy.shouldStartPrefetch(true, true, false, false, 3, 0));
    }

    @Test
    void overlayClicksStayInsideTheReplacementGui() {
        var layout = StorageOverlayPolicy.layout(960, 540, 3, 5, 324);
        assertTrue(StorageOverlayPolicy.isClickInsideOverlay(
                layout, layout.panelX() + 8, layout.panelY() + 8));
        assertTrue(StorageOverlayPolicy.isClickInsideOverlay(
                layout, layout.playerX() + 10, layout.playerY() + 10));
        assertFalse(StorageOverlayPolicy.isClickInsideOverlay(
                layout, 2, 2));
        assertTrue(StorageOverlayPolicy.shouldSuppressOutsideClick(
                true, layout, layout.innerX() + 4, layout.innerY() + 4));
        assertFalse(StorageOverlayPolicy.shouldSuppressOutsideClick(
                false, layout, layout.innerX() + 4, layout.innerY() + 4));
        assertTrue(StorageOverlayPolicy.shouldPinScreenOnClose(true, false, true));
        assertFalse(StorageOverlayPolicy.shouldPinScreenOnClose(true, true, true));
        assertFalse(StorageOverlayPolicy.shouldPinScreenOnClose(true, false, false));
        assertTrue(StorageOverlayPolicy.cacheFingerprintUnchanged("a", "a"));
        assertFalse(StorageOverlayPolicy.cacheFingerprintUnchanged("a", "b"));
    }

    @Test
    void inventorySitsBelowTheStoragePanelWithAVisibleSlotGridAndSearchBox() {
        var layout = StorageOverlayPolicy.layout(960, 540, 3, 5, 324);
        assertTrue(layout.playerY() >= layout.panelY() + layout.panelHeight() + StorageOverlayPolicy.PLAYER_GAP);
        assertTrue(StorageOverlayPolicy.insideSearchField(
                layout, layout.searchX() + 4, layout.searchY() + 4));
        assertEquals(StorageOverlayPolicy.pageHeight(5), StorageOverlayPolicy.emptyPageHeight());
        assertEquals(45, StorageOverlayPolicy.defaultEmptySlotCount());
        assertEquals("Warden", StorageOverlayPolicy.appendSearchChar("Warde", "n"));
        assertEquals("Warde", StorageOverlayPolicy.deleteSearchChar("Warden"));
        assertTrue(StorageOverlayPolicy.searching("pickaxe"));
        assertFalse(StorageOverlayPolicy.searching("  "));
        assertTrue(StorageOverlayPolicy.isClickInsideOverlay(
                layout, layout.searchX() + 2, layout.searchY() + 2));
        assertTrue(StorageOverlayPolicy.overScrollBar(
                layout, layout.scrollBarX() + 1, layout.scrollBarY() + 8));
        assertFalse(StorageOverlayPolicy.overScrollBar(
                layout, layout.panelX() + 4, layout.innerY() + 8));
        assertTrue(StorageOverlayPolicy.scrollBarTrackBottom(layout)
                > layout.scrollBarY());
        int rows = 3;
        int cardHeight = StorageOverlayPolicy.pageHeight(rows);
        int[] lastSlot = StorageOverlayPolicy.contentSlotPosition(0, 0, rows * 9 - 1);
        assertTrue(lastSlot[1] + StorageOverlayPolicy.SLOT_SIZE
                <= cardHeight - StorageOverlayPolicy.CARD_BOTTOM_PAD + 1);
        assertEquals(5, StorageOverlayPolicy.slotRows(0, true));
        assertEquals(3, StorageOverlayPolicy.slotRows(3, false));
        assertEquals(27, StorageOverlayPolicy.defaultDirectory().size());
        assertFalse(StorageOverlayPolicy.isPhysicalSelectorSlot(
                StorageOverlayPolicy.COMMAND_SELECTOR_SLOT));
    }

    @Test
    void searchGlowTravelsAroundTheSlotClockwise() {
        int perimeter = StorageOverlayPolicy.searchGlowPerimeter(18);
        assertEquals(68, perimeter);
        assertEquals(0, StorageOverlayPolicy.searchGlowHead(0L, perimeter));
        int later = StorageOverlayPolicy.searchGlowHead(
                StorageOverlayPolicy.SEARCH_GLOW_PERIOD_MS / 2, perimeter);
        assertTrue(later > 0);
        assertTrue(later < perimeter);
        int[] start = StorageOverlayPolicy.searchGlowPixel(10, 20, 18, 0);
        assertArrayEquals(new int[] {10, 20}, start);
        for (int i = 0; i < perimeter; i++) {
            int[] pixel = StorageOverlayPolicy.searchGlowPixel(10, 20, 18, i);
            assertTrue(pixel[0] >= 10 && pixel[0] < 28);
            assertTrue(pixel[1] >= 20 && pixel[1] < 38);
        }
        int[] inward = StorageOverlayPolicy.searchGlowInward(10, 20, 18, 10, 20);
        assertArrayEquals(new int[] {11, 21}, inward);
        int headColor = StorageOverlayPolicy.searchGlowColor(0, 18);
        int tailColor = StorageOverlayPolicy.searchGlowColor(17, 18);
        assertEquals(0xFF, (headColor >> 24) & 0xFF);
        assertTrue(((headColor >> 16) & 0xFF) > 0x80);
        assertTrue((tailColor & 0xFF) > 0x40);
    }

    @Test
    void searchCaretStaysInsideTheFieldAndClipsFromTheStart() {
        assertEquals("SSSS", StorageOverlayPolicy.clipSearchFromEnd(
                "XXXXSSSS", 4, String::length));
        assertEquals("abc", StorageOverlayPolicy.clipSearchFromEnd(
                "abc", 10, String::length));
        assertEquals("", StorageOverlayPolicy.clipSearchFromEnd("abc", 0, String::length));
        int caret = StorageOverlayPolicy.searchCaretX(10, 4, 40, 200);
        assertTrue(caret >= 14);
        assertTrue(caret <= 10 + 40 - 4 - 1);
        assertEquals(14, StorageOverlayPolicy.searchCaretX(10, 4, 40, 0));
    }

    @Test
    void closeButtonAndOutsideClicksDismissTheOverlay() {
        var layout = StorageOverlayPolicy.layout(960, 540, 3, 5, 324);
        assertTrue(StorageOverlayPolicy.overCloseButton(
                layout, layout.closeX() + 2, layout.closeY() + 2));
        assertFalse(StorageOverlayPolicy.overCloseButton(
                layout, layout.innerX() + 8, layout.innerY() + 8));
        assertTrue(StorageOverlayPolicy.shouldCloseOnOutsideClick(true, layout, 2, 2));
        assertFalse(StorageOverlayPolicy.shouldCloseOnOutsideClick(
                true, layout, layout.panelX() + 8, layout.panelY() + 8));
        assertFalse(StorageOverlayPolicy.shouldCloseOnOutsideClick(
                true, layout, layout.playerX() + 10, layout.playerY() + 10));
        assertFalse(StorageOverlayPolicy.shouldCloseOnOutsideClick(false, layout, 2, 2));
        assertTrue(layout.closeX() + layout.closeSize() <= layout.panelX() + layout.panelWidth());
        assertTrue(layout.searchX() + layout.searchWidth() <= layout.closeX());
    }

    @Test
    void pageValueUsesBazaarUnitPricesAndSitsOnTheCardHeader() {
        var lines = java.util.List.of(
                new StorageOverlayPolicy.MarketLine("ENCHANTED_DIAMOND", 2),
                new StorageOverlayPolicy.MarketLine("MISSING", 8),
                new StorageOverlayPolicy.MarketLine("", 4));
        assertEquals(2500.0D, StorageOverlayPolicy.instantSellTotal(
                lines, java.util.Map.of("ENCHANTED_DIAMOND", 1250.0D)));
        assertEquals(0.0D, StorageOverlayPolicy.instantSellTotal(lines, java.util.Map.of()));
        assertEquals("Total value: 2.5k", StorageOverlayPolicy.pageValueLabel(2500.0D));
        assertEquals("No AH/BZ prices yet", StorageOverlayPolicy.pageValueLabel(0.0D));
        assertEquals(40.0D, StorageOverlayPolicy.marketUnitValue(0, 0, 40));
        assertEquals(25.0D, StorageOverlayPolicy.marketUnitValue(90, 25, 0));
        assertEquals(12.0D, StorageOverlayPolicy.marketUnitValue(12, 0, 0));
        assertEquals(0.0D, StorageOverlayPolicy.marketUnitValue(0, 0, 0));
        int[] icon = StorageOverlayPolicy.valueIconPosition(10, 20, 166);
        assertEquals(10 + 166 - StorageOverlayPolicy.VALUE_ICON_SIZE - 3, icon[0]);
        assertTrue(StorageOverlayPolicy.overValueIcon(10, 20, 166, icon[0] + 2, icon[1] + 2));
        assertFalse(StorageOverlayPolicy.overValueIcon(10, 20, 166, 12, 22));
        assertTrue(StorageOverlayPolicy.headerLabelMaxWidth(166) < 166);
    }

    @Test
    void leftoverOverlayStateDoesNotStealTheDashboardCursorOrWheel() {
        assertTrue(StorageOverlayPolicy.shouldKeepCursorOnScreenChange(
                true, false, true, true, false));
        assertFalse(StorageOverlayPolicy.shouldKeepCursorOnScreenChange(
                true, false, true, false, false));
        assertFalse(StorageOverlayPolicy.shouldKeepUngrabbedCursor(true, false, false));
        assertTrue(StorageOverlayPolicy.shouldKeepUngrabbedCursor(true, true, false));
        assertFalse(StorageOverlayPolicy.shouldStealOverlayWheel(false, true));
        assertTrue(StorageOverlayPolicy.shouldStealOverlayWheel(true, true));
        assertTrue(StorageOverlayPolicy.shouldStealOverlayWheel(true, false, true, true));
        assertFalse(StorageOverlayPolicy.shouldStealOverlayWheel(true, true, false, true));
        assertTrue(StorageOverlayPolicy.shouldStealOverlayWheel(true, true, true, true));
        assertTrue(CustomTooltipPolicy.panTooltipVertically(false, false));
        assertFalse(CustomTooltipPolicy.panTooltipVertically(true, false));
        assertTrue(CustomTooltipPolicy.panTooltipVertically(true, true));
        assertTrue(CustomTooltipPolicy.panTooltipHorizontally(false, true, true));
        assertFalse(CustomTooltipPolicy.panTooltipHorizontally(true, true, true));
        assertTrue(CustomTooltipPolicy.storageOverlayTakesWheel(true, false));
        assertFalse(CustomTooltipPolicy.storageOverlayTakesWheel(true, true));
    }
}
