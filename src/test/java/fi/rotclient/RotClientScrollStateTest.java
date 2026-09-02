package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientScrollStateTest {
    @Test
    void thumbHasAccessibleMinimumHeight() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(2_000, 200);

        RotClientScrollState.Thumb thumb = scroll.thumb(10, 210, 40);

        assertEquals(10, thumb.top());
        assertEquals(40, thumb.height());
    }

    @Test
    void thumbDragPreservesGrabOffsetAndReachesBottom() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(1_000, 200);
        RotClientScrollState.Thumb initial = scroll.thumb(20, 220, 40);

        assertTrue(scroll.beginThumbDrag(initial.top() + 12, 20, 220, 40));
        assertTrue(scroll.isThumbDragging());
        assertTrue(scroll.dragThumbTo(220, 20, 220, 40));
        assertEquals(scroll.maxScroll(), scroll.scrollPixels());

        assertTrue(scroll.endThumbDrag());
        assertFalse(scroll.isThumbDragging());
    }

    @Test
    void trackClickPagesTowardPointer() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(1_000, 200);

        assertTrue(scroll.clickTrack(219, 20, 220, 40));
        assertEquals(200, scroll.scrollPixels());
        assertTrue(scroll.clickTrack(20, 20, 220, 40));
        assertEquals(0, scroll.scrollPixels());
    }

    @Test
    void wheelDeltaMovesTargetProportionallyAndLerpCatchesUp() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(1_000, 200);
        scroll.scrollBySteps(-0.5D, 40);
        assertEquals(0, scroll.scrollPixels());
        scroll.snap();
        assertEquals(20, scroll.scrollPixels());
        scroll.scrollBySteps(-1.0D, 40);
        scroll.advance(System.nanoTime());
        assertTrue(scroll.scrollPixels() > 20);
        assertTrue(scroll.scrollPixels() <= 60);
    }

    @Test
    void displayedScrollKeepsSubpixelAndHighHzStepsLessThan60Hz() {
        RotClientScrollState sixty = new RotClientScrollState();
        sixty.setBounds(2_000, 200);
        sixty.scrollBySteps(-1.0D, 40);
        sixty.advanceSeconds(1.0D / 60.0D);

        RotClientScrollState high = new RotClientScrollState();
        high.setBounds(2_000, 200);
        high.scrollBySteps(-1.0D, 40);
        high.advanceSeconds(1.0D / 144.0D);

        assertTrue(high.displayedScroll() > 0.0D);
        assertTrue(high.displayedScroll() < sixty.displayedScroll());
        assertTrue(Math.abs(high.displayedScroll() - high.scrollPixels()) < 1.0D);
    }

    @Test
    void independentScrollStatesDoNotShareDragOwnership() {
        RotClientScrollState sidebar = new RotClientScrollState();
        RotClientScrollState drawer = new RotClientScrollState();
        sidebar.setBounds(1_000, 200);
        drawer.setBounds(1_000, 200);

        assertTrue(sidebar.beginThumbDrag(25, 20, 220, 40));
        assertTrue(sidebar.isThumbDragging());
        assertFalse(drawer.isThumbDragging());
    }
}
