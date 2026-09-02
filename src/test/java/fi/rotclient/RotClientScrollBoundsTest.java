package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientScrollBoundsTest {
    @Test
    void maxScrollIsZeroWhenContentFits() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(80, 100);
        assertEquals(0, scroll.maxScroll());
        assertFalse(scroll.canScroll());
        scroll.scrollBy(50);
        assertEquals(0, scroll.scrollPixels());
        scroll.scrollBySteps(-1.0D, 28);
        assertEquals(0, scroll.scrollPixels());
    }

    @Test
    void maxScrollIsZeroWhenContentEqualsViewport() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(120, 120);
        assertEquals(0, scroll.maxScroll());
    }

    @Test
    void maxScrollMatchesContentMinusViewport() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(250, 100);
        assertEquals(150, scroll.maxScroll());
        scroll.scrollBy(999);
        assertEquals(150, scroll.scrollPixels());
        scroll.scrollBy(-999);
        assertEquals(0, scroll.scrollPixels());
    }

    @Test
    void shrinkingContentReclampsOffset() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(400, 100);
        scroll.setScrollPixels(250);
        assertEquals(250, scroll.scrollPixels());
        scroll.setBounds(120, 100);
        assertEquals(20, scroll.maxScroll());
        assertEquals(20, scroll.scrollPixels());
    }

    @Test
    void growingViewportReclampsOffset() {
        RotClientScrollState scroll = new RotClientScrollState();
        scroll.setBounds(300, 100);
        scroll.setScrollPixels(180);
        scroll.setBounds(300, 280);
        assertEquals(20, scroll.maxScroll());
        assertEquals(20, scroll.scrollPixels());
    }

    @Test
    void measureContentHeightDoesNotDoubleCountScroll() {
        int viewportTop = 100;
        int scrollPixels = 40;
        int drawStart = viewportTop - scrollPixels;
        int drawEnd = drawStart + 220;
        assertEquals(220, RotClientScrollState.measureContentHeight(drawStart, drawEnd));
        // The old buggy formula: (end + scroll) - start == end - start + 2*scroll
        int buggy = (drawEnd + scrollPixels) - drawStart;
        assertEquals(260, buggy);
        assertTrue(buggy > RotClientScrollState.measureContentHeight(drawStart, drawEnd));
    }
}
