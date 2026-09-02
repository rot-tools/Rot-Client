package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientDashboardLayoutTest {
    @Test
    void largeViewportUsesCompactControlPanelFootprint() {
        RotClientDashboardLayout.PanelSize qhd =
                RotClientDashboardLayout.resolve(2560, 1440);
        assertEquals(Math.round(2560 * 0.72), qhd.width());
        assertEquals(Math.round(1440 * 0.72), qhd.height());

        RotClientDashboardLayout.PanelSize fhd =
                RotClientDashboardLayout.resolve(1920, 1080);
        assertEquals(Math.round(1920 * 0.72), fhd.width());
        assertEquals(Math.round(1080 * 0.72), fhd.height());
    }

    @Test
    void smallViewportClampsWithoutGoingFullscreen() {
        RotClientDashboardLayout.PanelSize tiny =
                RotClientDashboardLayout.resolve(800, 480);
        assertTrue(tiny.width() <= 800 - 8);
        assertTrue(tiny.height() <= 480 - 8);
        assertTrue(tiny.height() < 480);
    }

    @Test
    void actionRowStaysInsidePanel() {
        int panelY = 10;
        int panelHeight = RotClientDashboardLayout.BASE_HEIGHT;
        int actionY = RotClientDashboardLayout.actionRowY(panelY, panelHeight);
        int clipBottom = RotClientDashboardLayout.contentClipBottom(
                panelY, panelHeight);
        assertTrue(clipBottom < actionY);
        assertTrue(actionY
                < panelY + panelHeight
                - RotClientDashboardLayout.HOME_BUTTON_Y_OFFSET);
    }

    @Test
    void chromeHeightIsTabStripPlusOmnibox() {
        assertEquals(
                RotClientDashboardLayout.TAB_STRIP_HEIGHT
                        + RotClientDashboardLayout.OMNIBOX_HEIGHT,
                RotClientDashboardLayout.chromeHeight());
    }
}
