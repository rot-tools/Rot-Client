package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MapArtOverridePolicyTest {
    @Test
    void paintingsNeedAHorizontalDirection() {
        assertTrue(MapArtOverridePolicy.shouldReplacePainting(true, true));
        assertFalse(MapArtOverridePolicy.shouldReplacePainting(false, true));
        assertFalse(MapArtOverridePolicy.shouldReplacePainting(true, false));
    }

    @Test
    void itemDisplaysReplaceMapsPaintingsAndLargeFixedWalls() {
        assertTrue(MapArtOverridePolicy.shouldReplaceItemDisplay(
                true, true, false, 1.0F, 1.0F, 1.0F, 0.5F, 0.5F));
        assertTrue(MapArtOverridePolicy.shouldReplaceItemDisplay(
                true, false, true, 8.0F, 5.0F, 0.05F, 1.0F, 1.0F));
        assertTrue(MapArtOverridePolicy.shouldReplaceItemDisplay(
                true, false, true, 1.0F, 1.0F, 1.0F, 8.0F, 5.0F));
        assertFalse(MapArtOverridePolicy.shouldReplaceItemDisplay(
                false, true, true, 8.0F, 5.0F, 0.05F, 8.0F, 5.0F));
        assertFalse(MapArtOverridePolicy.shouldReplaceItemDisplay(
                true, false, false, 8.0F, 5.0F, 0.05F, 8.0F, 5.0F));
        assertFalse(MapArtOverridePolicy.shouldReplaceItemDisplay(
                true, false, true, 1.0F, 1.0F, 1.0F, 0.6F, 2.0F));
        assertTrue(MapArtOverridePolicy.shouldReplaceItemDisplay(
                true, false, true, 12.0F, 8.0F, 1.0F, 0.5F, 0.5F));
    }

    @Test
    void packedLightFallsBackToFullBright() {
        assertEquals(12, MapArtOverridePolicy.packedLight(12, 4));
        assertEquals(4, MapArtOverridePolicy.packedLight(0, 4));
        assertEquals(MapArtOverridePolicy.PACKED_FULL_BRIGHT, MapArtOverridePolicy.packedLight(0, 0));
    }

    @Test
    void mapFrameZDegreesMatchesVanillaItemFrameMaps() {
        assertEquals(180.0F, MapArtOverridePolicy.mapFrameZDegrees(0), 0.01F);
        assertEquals(270.0F, MapArtOverridePolicy.mapFrameZDegrees(1), 0.01F);
        assertEquals(360.0F, MapArtOverridePolicy.mapFrameZDegrees(2), 0.01F);
        assertEquals(450.0F, MapArtOverridePolicy.mapFrameZDegrees(3), 0.01F);
        assertEquals(180.0F, MapArtOverridePolicy.mapFrameZDegrees(4), 0.01F);
        assertEquals(360.0F, MapArtOverridePolicy.mapFrameZDegrees(6), 0.01F);
        assertEquals(180.0F, MapArtOverridePolicy.mapFrameZDegrees(8), 0.01F);
    }

    @Test
    void tileUvCoversAFilledRectangle() {
        assertTrue(MapArtOverridePolicy.isFilledRectangle(6, 3, 2));
        assertFalse(MapArtOverridePolicy.isFilledRectangle(5, 3, 2));
        assertArrayEquals(new float[] {0.0F, 0.5F, 0.5F, 1.0F}, MapArtOverridePolicy.tileUv(0, 1, 2, 2), 0.0001F);
        assertArrayEquals(new float[] {0.0F, 0.0F, 1.0F, 1.0F}, MapArtOverridePolicy.tileUv(-1, 0, 2, 2), 0.0001F);
    }

    @Test
    void hubMapWallIsThirteenBySeven() {
        assertEquals(13, MapArtOverridePolicy.HUB_MAP_COLUMNS);
        assertEquals(7, MapArtOverridePolicy.HUB_MAP_ROWS);
        assertEquals(91, MapArtOverridePolicy.HUB_MAP_TILES);
        assertEquals(16, MapArtOverridePolicy.HUB_MAP_SEARCH_RADIUS);
        assertTrue(MapArtOverridePolicy.isHubMapGrid(91, 13, 7));
        assertTrue(MapArtOverridePolicy.isFilledRectangle(91, 13, 7));
        assertTrue(MapArtOverridePolicy.isMapPanelLayout(91, 13, 7));
        assertTrue(MapArtOverridePolicy.isMapPanelLayout(80, 13, 7));
        assertFalse(MapArtOverridePolicy.isFilledRectangle(80, 13, 7));
        assertFalse(MapArtOverridePolicy.isHubMapGrid(128, 16, 8));
        assertArrayEquals(
                new float[] {0.0F, 0.0F, 1.0F / 13.0F, 1.0F / 7.0F},
                MapArtOverridePolicy.tileUv(0, 0, 13, 7),
                0.0001F);
        assertArrayEquals(
                new float[] {12.0F / 13.0F, 6.0F / 7.0F, 1.0F, 1.0F},
                MapArtOverridePolicy.tileUv(12, 6, 13, 7),
                0.0001F);
    }

    @Test
    void hubThirteenBySevenContainsCenteredSquareWithBlackEdges() {
        assertEquals(1.0F, MapArtOverridePolicy.FOX_IMAGE_ASPECT, 0.0001F);
        assertArrayEquals(
                new float[] {3.0F / 13.0F, 0.0F, 7.0F / 13.0F, 1.0F},
                MapArtOverridePolicy.containRect(13, 7, 1.0F),
                0.0001F);
        float[] left = MapArtOverridePolicy.tileUvContain(0, 0, 13, 7, 1.0F);
        assertTrue(MapArtOverridePolicy.isLetterboxUv(left[0], left[1], left[2], left[3]));
        assertNull(MapArtOverridePolicy.containedTileImageQuad(left[0], left[1], left[2], left[3]));
        float[] right = MapArtOverridePolicy.tileUvContain(12, 6, 13, 7, 1.0F);
        assertTrue(MapArtOverridePolicy.isLetterboxUv(right[0], right[1], right[2], right[3]));
        float[] foxTopLeft = MapArtOverridePolicy.tileUvContain(3, 0, 13, 7, 1.0F);
        assertArrayEquals(new float[] {0.0F, 0.0F, 1.0F / 7.0F, 1.0F / 7.0F}, foxTopLeft, 0.0001F);
        assertFalse(MapArtOverridePolicy.isLetterboxUv(foxTopLeft[0], foxTopLeft[1], foxTopLeft[2], foxTopLeft[3]));
        float[] foxBottomRight = MapArtOverridePolicy.tileUvContain(9, 6, 13, 7, 1.0F);
        assertArrayEquals(new float[] {6.0F / 7.0F, 6.0F / 7.0F, 1.0F, 1.0F}, foxBottomRight, 0.0001F);
        float[] center = MapArtOverridePolicy.tileUvContain(6, 3, 13, 7, 1.0F);
        assertArrayEquals(new float[] {3.0F / 7.0F, 3.0F / 7.0F, 4.0F / 7.0F, 4.0F / 7.0F}, center, 0.0001F);
        assertTrue(MapArtOverridePolicy.isMapWallItem(true, false, false));
        assertTrue(MapArtOverridePolicy.isMapWallItem(false, true, false));
        assertTrue(MapArtOverridePolicy.isMapWallItem(false, false, true));
        assertFalse(MapArtOverridePolicy.isMapWallItem(false, false, false));
    }
}
