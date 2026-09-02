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
        assertArrayEquals(new int[] {layout.playerX() + 12, layout.playerY() + 67},
                StorageOverlayPolicy.playerSlotPosition(layout.playerX(), layout.playerY(), 0));
        assertArrayEquals(new int[] {13, 40},
                StorageOverlayPolicy.contentSlotPosition(10, 20, 0));
        assertArrayEquals(new int[] {13, 58},
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
}
