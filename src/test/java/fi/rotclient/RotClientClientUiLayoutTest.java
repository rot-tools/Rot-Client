package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientClientUiLayoutTest {
    @Test
    void defaultCenterNormProducesCenteredPanel() {
        int viewportW = 1920;
        int viewportH = 1080;
        RotClientDashboardLayout.PanelSize size =
                RotClientDashboardLayout.resolve(viewportW, viewportH);
        int panelW = size.width();
        int panelH = size.height();
        int x = RotClientClientUiLayout.panelX(viewportW, panelW, 0.5F);
        int y = RotClientClientUiLayout.panelY(viewportH, panelH, 0.5F);
        int expectedX = Math.round(0.5F * (viewportW - panelW));
        int expectedY = Math.round(0.5F * (viewportH - panelH));
        assertEquals(
                RotClientClientUiLayout.clampPanelX(viewportW, panelW, expectedX),
                x);
        assertEquals(
                RotClientClientUiLayout.clampPanelY(viewportH, panelH, expectedY),
                y);
    }

    @Test
    void moveAndSerializeRoundTrip() {
        int viewportW = 1280;
        int viewportH = 720;
        RotClientDashboardLayout.PanelSize size =
                RotClientDashboardLayout.resolve(viewportW, viewportH);
        int panelW = size.width();
        int panelH = size.height();
        int panelX = 100;
        int panelY = 80;
        float normX = RotClientClientUiLayout.normXFromPanelX(
                viewportW, panelW, panelX);
        float normY = RotClientClientUiLayout.normYFromPanelY(
                viewportH, panelH, panelY);
        assertEquals(
                RotClientClientUiLayout.clampPanelX(viewportW, panelW, panelX),
                RotClientClientUiLayout.panelX(viewportW, panelW, normX));
        assertEquals(
                RotClientClientUiLayout.clampPanelY(viewportH, panelH, panelY),
                RotClientClientUiLayout.panelY(viewportH, panelH, normY));
    }

    @Test
    void clampsLeftRightTopBottom() {
        int viewportW = 1000;
        int viewportH = 600;
        int panelW = 700;
        int panelH = 400;
        assertTrue(
                RotClientClientUiLayout.clampPanelX(viewportW, panelW, -500) >= 0);
        assertTrue(
                RotClientClientUiLayout.clampPanelX(viewportW, panelW, 9999)
                        <= viewportW - panelW);
        assertTrue(
                RotClientClientUiLayout.clampPanelY(viewportH, panelH, -500) >= 0);
        assertTrue(
                RotClientClientUiLayout.clampPanelY(viewportH, panelH, 9999)
                        <= viewportH - panelH + 4);
    }

    @Test
    void smallViewportKeepsHeaderReachable() {
        int viewportW = 400;
        int viewportH = 200;
        int panelW = RotClientDashboardLayout.BASE_WIDTH;
        int panelH = RotClientDashboardLayout.BASE_HEIGHT;
        int x = RotClientClientUiLayout.panelX(viewportW, panelW, 0.5F);
        int y = RotClientClientUiLayout.panelY(viewportH, panelH, 0.5F);
        assertTrue(x <= 4);
        assertTrue(y >= 0);
        assertTrue(y < viewportH);
    }

    @Test
    void largeViewportAllowsFullPanelInside() {
        int viewportW = 1920;
        int viewportH = 1080;
        RotClientDashboardLayout.PanelSize size =
                RotClientDashboardLayout.resolve(viewportW, viewportH);
        int panelW = size.width();
        int panelH = size.height();
        int x = RotClientClientUiLayout.panelX(viewportW, panelW, 0.0F);
        int y = RotClientClientUiLayout.panelY(viewportH, panelH, 0.0F);
        assertTrue(x >= 0);
        assertTrue(y >= 0);
        assertTrue(x + panelW <= viewportW);
        assertTrue(y + panelH <= viewportH);
    }

    @Test
    void guiResizeReclampsSavedNorm() {
        float normX = 0.9F;
        float normY = 0.9F;
        RotClientDashboardLayout.PanelSize small =
                RotClientDashboardLayout.resolve(900, 500);
        RotClientDashboardLayout.PanelSize large =
                RotClientDashboardLayout.resolve(1600, 900);
        int smallX = RotClientClientUiLayout.panelX(900, small.width(), normX);
        int largeX = RotClientClientUiLayout.panelX(1600, large.width(), normX);
        assertTrue(smallX + small.width() <= 900);
        assertTrue(largeX + large.width() <= 1600);
        int smallY = RotClientClientUiLayout.panelY(500, small.height(), normY);
        int largeY = RotClientClientUiLayout.panelY(900, large.height(), normY);
        assertTrue(smallY >= 0);
        assertTrue(largeY >= 0);
    }

    @Test
    void layoutResetDefaultsNormsToCenter() {
        RotClientWorkspaceConfig config = RotClientWorkspaceConfig.defaults();
        config.clientUiNormX = 0.1F;
        config.clientUiNormY = 0.9F;
        config.clientUiNormX = 0.5F;
        config.clientUiNormY = 0.5F;
        assertEquals(0.5F, config.clientUiNormX, 0.0001F);
        assertEquals(0.5F, config.clientUiNormY, 0.0001F);
    }

    @Test
    void tabStripSitsAtTheTopOfTheWindow() {
        RotClientTabStrip.Layout layout = RotClientTabStrip.compute(10, 40, 820, 3);
        assertEquals(40, layout.y());
        assertTrue(layout.maximizeX() > layout.plusX());
        assertTrue(layout.closeX() > layout.maximizeX());
    }

    @Test
    void tabStripOverflowUsesScrollWhenCrowded() {
        RotClientTabStrip.Layout sparse = RotClientTabStrip.compute(0, 0, 820, 3);
        RotClientTabStrip.Layout crowded = RotClientTabStrip.compute(0, 0, 820, 12);
        assertTrue(crowded.maxScroll() >= sparse.maxScroll());
        assertTrue(crowded.tabWidth() >= RotClientTabStrip.MIN_TAB_WIDTH);
        assertTrue(crowded.tabWidth() <= RotClientTabStrip.MAX_TAB_WIDTH);
    }

    @Test
    void dropIndexSupportsReorderEndpoints() {
        RotClientTabStrip.Layout layout = RotClientTabStrip.compute(10, 20, 820, 4);
        assertEquals(0, RotClientTabStrip.dropIndex(layout, 4, 0, layout.x() - 50));
        assertEquals(
                3,
                RotClientTabStrip.dropIndex(
                        layout, 4, 0, layout.x() + layout.width() + 100));
    }
}
