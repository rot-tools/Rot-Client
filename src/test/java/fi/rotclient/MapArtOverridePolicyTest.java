package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    }

    @Test
    void packedLightFallsBackToFullBright() {
        assertEquals(12, MapArtOverridePolicy.packedLight(12, 4));
        assertEquals(4, MapArtOverridePolicy.packedLight(0, 4));
        assertEquals(MapArtOverridePolicy.PACKED_FULL_BRIGHT, MapArtOverridePolicy.packedLight(0, 0));
    }

    @Test
    void tileUvCoversAFilledRectangle() {
        assertTrue(MapArtOverridePolicy.isFilledRectangle(6, 3, 2));
        assertFalse(MapArtOverridePolicy.isFilledRectangle(5, 3, 2));
        assertArrayEquals(new float[] {0.0F, 0.5F, 0.5F, 1.0F}, MapArtOverridePolicy.tileUv(0, 1, 2, 2), 0.0001F);
        assertArrayEquals(new float[] {0.0F, 0.0F, 1.0F, 1.0F}, MapArtOverridePolicy.tileUv(-1, 0, 2, 2), 0.0001F);
    }
}
