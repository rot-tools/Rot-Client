package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StorageOverlayReliabilityTest {
    @Test
    void columnsAdaptToGuiScaleWithoutChangingTheConfiguredMaximum() {
        for (int width : new int[] {320, 427, 640, 854, 1280, 1920}) {
            for (int requested = 1; requested <= 5; requested++) {
                for (int spacing : new int[] {0, 5, 40}) {
                    var layout = StorageOverlayPolicy.layout(width, 240, requested, spacing, 324, spacing);
                    int columns = StorageOverlayPolicy.layoutColumns(layout, spacing);
                    assertTrue(columns >= 1 && columns <= requested);
                    assertTrue(layout.panelX() >= 4, "left edge: " + width);
                    assertTrue(layout.panelX() + layout.panelWidth() <= width - 4);
                    assertTrue(layout.playerY() + StorageOverlayPolicy.PLAYER_HEIGHT <= 236);
                    assertTrue(layout.searchX() >= layout.innerX() + 52);
                }
            }
        }
        assertEquals(1, StorageOverlayPolicy.fittedColumns(320, 5, 5, 20));
        assertEquals(5, StorageOverlayPolicy.fittedColumns(1920, 5, 5, 20));
    }

    @Test
    void partiallyClippedSlotsNeverRemainInteractive() {
        var layout = StorageOverlayPolicy.layout(640, 480, 3, 5, 324);
        assertTrue(StorageOverlayPolicy.slotFullyVisible(layout, layout.innerX(), layout.innerY()));
        assertFalse(StorageOverlayPolicy.slotFullyVisible(layout, layout.innerX(), layout.innerY() - 1));
        assertTrue(StorageOverlayPolicy.slotFullyVisible(layout, layout.innerX(),
                layout.innerY() + layout.innerHeight() - 16));
        assertFalse(StorageOverlayPolicy.slotFullyVisible(layout, layout.innerX(),
                layout.innerY() + layout.innerHeight() - 15));
    }

    @Test
    void confirmedEmptyPagesCanReplaceAndPersistButLoadingEmptiesCannot() {
        assertTrue(StorageOverlayPolicy.shouldKeepExistingCache(true, true, false));
        assertFalse(StorageOverlayPolicy.shouldKeepExistingCache(true, true, true));
        assertTrue(StorageOverlayPolicy.shouldSkipEmptyStorageSave(false, true, false, false));
        assertFalse(StorageOverlayPolicy.shouldSkipEmptyStorageSave(false, true, false, true));
    }

    @Test
    void itemScrollSettingOnlyBlocksWhenHoveringAnItem() {
        assertFalse(StorageOverlayPolicy.allowPageScroll(true, true));
        assertTrue(StorageOverlayPolicy.allowPageScroll(true, false));
        assertTrue(StorageOverlayPolicy.allowPageScroll(false, true));
    }
}
