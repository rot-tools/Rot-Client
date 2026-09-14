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
        assertEquals(0.0F, MapArtOverridePolicy.itemFrameZDegrees(0), 0.01F);
        assertEquals(90.0F, MapArtOverridePolicy.itemFrameZDegrees(2), 0.01F);
        assertEquals(315.0F, MapArtOverridePolicy.itemFrameZDegrees(7), 0.01F);
        assertEquals(64.0F, MapArtOverridePolicy.MAP_QUAD_CENTER, 0.01F);
    }

    @Test
    void wallColumnIncreasesTowardViewerRight() {
        assertEquals(5, MapArtOverridePolicy.wallColumn(5, 9, 0));
        assertEquals(-5, MapArtOverridePolicy.wallColumn(5, 9, 2));
        assertEquals(9, MapArtOverridePolicy.wallColumn(5, 9, 1));
        assertEquals(-9, MapArtOverridePolicy.wallColumn(5, 9, 3));
        assertTrue(MapArtOverridePolicy.wallColumn(10, 0, 0) > MapArtOverridePolicy.wallColumn(9, 0, 0));
        assertTrue(MapArtOverridePolicy.wallColumn(9, 0, 2) > MapArtOverridePolicy.wallColumn(10, 0, 2));
    }

    @Test
    void tileUvCoversAFilledRectangle() {
        assertTrue(MapArtOverridePolicy.isFilledRectangle(6, 3, 2));
        assertFalse(MapArtOverridePolicy.isFilledRectangle(5, 3, 2));
        assertArrayEquals(new float[] {0.0F, 0.5F, 0.5F, 1.0F}, MapArtOverridePolicy.tileUv(0, 1, 2, 2), 0.0001F);
        assertArrayEquals(new float[] {0.0F, 0.0F, 1.0F, 1.0F}, MapArtOverridePolicy.tileUv(-1, 0, 2, 2), 0.0001F);
    }

    @Test
    void hubMapWallIsFourteenBySeven() {
        assertEquals(14, MapArtOverridePolicy.HUB_MAP_COLUMNS);
        assertEquals(7, MapArtOverridePolicy.HUB_MAP_ROWS);
        assertEquals(98, MapArtOverridePolicy.HUB_MAP_TILES);
        assertEquals(18, MapArtOverridePolicy.HUB_MAP_SEARCH_RADIUS);
        assertEquals(2, MapArtOverridePolicy.HUB_MAP_NEIGHBOR_REACH);
        assertTrue(MapArtOverridePolicy.isHubMapGrid(98, 14, 7));
        assertTrue(MapArtOverridePolicy.isFilledRectangle(98, 14, 7));
        assertTrue(MapArtOverridePolicy.isHubSizedBounds(14, 7));
        assertTrue(MapArtOverridePolicy.isHubSizedBounds(13, 6));
        assertTrue(MapArtOverridePolicy.isHubSizedBounds(14, 8));
        assertTrue(MapArtOverridePolicy.isMapPanelLayout(98, 14, 7));
        assertTrue(MapArtOverridePolicy.isMapPanelLayout(91, 13, 7));
        assertTrue(MapArtOverridePolicy.isMapPanelLayout(78, 13, 6));
        assertTrue(MapArtOverridePolicy.isMapPanelLayout(80, 14, 7));
        assertFalse(MapArtOverridePolicy.isMapPanelLayout(20, 14, 7));
        assertFalse(MapArtOverridePolicy.isFilledRectangle(80, 14, 7));
        assertFalse(MapArtOverridePolicy.isHubMapGrid(128, 16, 8));
        assertArrayEquals(
                new float[] {0.0F, 0.0F, 1.0F / 14.0F, 1.0F / 7.0F},
                MapArtOverridePolicy.tileUv(0, 0, 14, 7),
                0.0001F);
        assertArrayEquals(
                new float[] {13.0F / 14.0F, 6.0F / 7.0F, 1.0F, 1.0F},
                MapArtOverridePolicy.tileUv(13, 6, 14, 7),
                0.0001F);
    }

    @Test
    void hubFourteenBySevenContainsCenteredSquareWithBlackEdges() {
        assertEquals(1.0F, MapArtOverridePolicy.FOX_IMAGE_ASPECT, 0.0001F);
        assertArrayEquals(
                new float[] {0.25F, 0.0F, 0.5F, 1.0F},
                MapArtOverridePolicy.containRect(14, 7, 1.0F),
                0.0001F);
        float[] left = MapArtOverridePolicy.tileUvContain(0, 0, 14, 7, 1.0F);
        assertTrue(MapArtOverridePolicy.isLetterboxUv(left[0], left[1], left[2], left[3]));
        assertNull(MapArtOverridePolicy.containedTileImageQuad(left[0], left[1], left[2], left[3]));
        float[] right = MapArtOverridePolicy.tileUvContain(13, 6, 14, 7, 1.0F);
        assertTrue(MapArtOverridePolicy.isLetterboxUv(right[0], right[1], right[2], right[3]));
        float[] foxTopLeft = MapArtOverridePolicy.tileUvContain(4, 0, 14, 7, 1.0F);
        assertFalse(MapArtOverridePolicy.isLetterboxUv(foxTopLeft[0], foxTopLeft[1], foxTopLeft[2], foxTopLeft[3]));
        float[] foxBottomRight = MapArtOverridePolicy.tileUvContain(9, 6, 14, 7, 1.0F);
        assertFalse(MapArtOverridePolicy.isLetterboxUv(
                foxBottomRight[0], foxBottomRight[1], foxBottomRight[2], foxBottomRight[3]));
        float[] center = MapArtOverridePolicy.tileUvContain(6, 3, 14, 7, 1.0F);
        assertFalse(MapArtOverridePolicy.isLetterboxUv(center[0], center[1], center[2], center[3]));
        assertTrue(MapArtOverridePolicy.isMapWallItem(true, false, false));
        assertTrue(MapArtOverridePolicy.isMapWallItem(false, true, false));
        assertTrue(MapArtOverridePolicy.isMapWallItem(false, false, true));
        assertFalse(MapArtOverridePolicy.isMapWallItem(false, false, false));
        assertTrue(MapArtOverridePolicy.isWallFrameItem(false, true));
        assertFalse(MapArtOverridePolicy.isWallFrameItem(false, false));
    }
}
