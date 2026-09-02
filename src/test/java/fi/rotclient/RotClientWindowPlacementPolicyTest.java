package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientWindowPlacementPolicyTest {
    @Test
    void topEdgeSnapsToMaximized() {
        assertEquals(
                RotClientWindowPlacementPolicy.MAXIMIZED,
                RotClientWindowPlacementPolicy.snapPreview(1920, 1080, 400, 4));
    }

    @Test
    void sideEdgesSnapToHalves() {
        assertEquals(
                RotClientWindowPlacementPolicy.SNAP_LEFT,
                RotClientWindowPlacementPolicy.snapPreview(1920, 1080, 6, 200));
        assertEquals(
                RotClientWindowPlacementPolicy.SNAP_RIGHT,
                RotClientWindowPlacementPolicy.snapPreview(1920, 1080, 1915, 200));
        assertEquals(
                RotClientWindowPlacementPolicy.FLOATING,
                RotClientWindowPlacementPolicy.snapPreview(1920, 1080, 400, 200));
    }

    @Test
    void applyFillsViewportAndHalves() {
        RotClientWindowPlacementPolicy.Rect floating =
                new RotClientWindowPlacementPolicy.Rect(100, 80, 800, 500);
        RotClientWindowPlacementPolicy.Rect max =
                RotClientWindowPlacementPolicy.apply(
                        RotClientWindowPlacementPolicy.MAXIMIZED, 1920, 1080, floating);
        assertEquals(0, max.x());
        assertEquals(1920, max.width());
        assertEquals(1080, max.height());

        RotClientWindowPlacementPolicy.Rect left =
                RotClientWindowPlacementPolicy.apply(
                        RotClientWindowPlacementPolicy.SNAP_LEFT, 1920, 1080, floating);
        assertEquals(0, left.x());
        assertEquals(960, left.width());
        assertEquals(1080, left.height());

        RotClientWindowPlacementPolicy.Rect right =
                RotClientWindowPlacementPolicy.apply(
                        RotClientWindowPlacementPolicy.SNAP_RIGHT, 1920, 1080, floating);
        assertEquals(960, right.x());
        assertEquals(960, right.width());
    }

    @Test
    void restoreUnderCursorKeepsFloatingSize() {
        RotClientWindowPlacementPolicy.Rect floating =
                new RotClientWindowPlacementPolicy.Rect(40, 40, 800, 500);
        RotClientWindowPlacementPolicy.Rect restored =
                RotClientWindowPlacementPolicy.restoreUnderCursor(
                        floating, 600, 1920, 1080);
        assertEquals(800, restored.width());
        assertEquals(500, restored.height());
        assertTrue(restored.contains(600, 8) || restored.x() <= 600);
    }

    @Test
    void resizeSouthEastGrows() {
        RotClientWindowPlacementPolicy.Rect start =
                new RotClientWindowPlacementPolicy.Rect(100, 100, 600, 400);
        RotClientWindowPlacementPolicy.Rect next =
                RotClientWindowPlacementPolicy.resize(
                        start,
                        RotClientWindowPlacementPolicy.ResizeEdge.SE,
                        900,
                        700,
                        1920,
                        1080);
        assertEquals(100, next.x());
        assertEquals(100, next.y());
        assertEquals(800, next.width());
        assertEquals(600, next.height());
    }

    @Test
    void resizeWestMovesOrigin() {
        RotClientWindowPlacementPolicy.Rect start =
                new RotClientWindowPlacementPolicy.Rect(200, 100, 600, 400);
        RotClientWindowPlacementPolicy.Rect next =
                RotClientWindowPlacementPolicy.resize(
                        start,
                        RotClientWindowPlacementPolicy.ResizeEdge.W,
                        150,
                        300,
                        1920,
                        1080);
        assertEquals(150, next.x());
        assertEquals(650, next.width());
    }

    @Test
    void toggleMaximizeRoundTrips() {
        assertEquals(
                RotClientWindowPlacementPolicy.MAXIMIZED,
                RotClientWindowPlacementPolicy.toggleMaximize(
                        RotClientWindowPlacementPolicy.FLOATING));
        assertEquals(
                RotClientWindowPlacementPolicy.FLOATING,
                RotClientWindowPlacementPolicy.toggleMaximize(
                        RotClientWindowPlacementPolicy.MAXIMIZED));
    }
}
