package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class QolOverlayHudEditorTest {
    @Test
    void visibleOverlayCanBeSelectedCenteredAndResetIndividually() {
        TrackerConfig tracker = new TrackerConfig();
        tracker.qolUtilities.playerDisplayEnabled = true;
        QolOverlayHud overlay = new QolOverlayHud(tracker);
        overlay.setEditorOpen(true);

        assertEquals("Health HUD", overlay.selectedElementLabel());
        assertTrue(overlay.beginDrag(20.0D, 255.0D));
        assertTrue(overlay.centerSelectedHorizontally(400));
        assertEquals(145.0F, tracker.qolUtilities.healthHudX, 0.01F);

        assertTrue(overlay.resetSelectedPosition());
        assertEquals(12.0F, tracker.qolUtilities.healthHudX, 0.01F);
        assertEquals(250.0F, tracker.qolUtilities.healthHudY, 0.01F);
    }

    @Test
    void slayerProgressHasARealMovableHudEditorEntry() {
        TrackerConfig tracker = new TrackerConfig();
        tracker.qolUtilities.extras().slayerProgressEnabled = true;
        QolOverlayHud overlay = new QolOverlayHud(tracker);
        overlay.setEditorOpen(true);

        assertTrue(overlay.editorElementLabels().contains("Slayer Progress"));
        assertTrue(overlay.beginDrag(175.0D, 472.0D));
        assertEquals("Slayer Progress", overlay.selectedElementLabel());
        assertTrue(overlay.centerSelectedHorizontally(400));
        assertTrue(overlay.resetSelectedPosition());
        assertEquals(172.0F, tracker.qolUtilities.slayerProgressHudX, 0.01F);
        assertEquals(468.0F, tracker.qolUtilities.slayerProgressHudY, 0.01F);
    }

    @Test
    void iotaAndStallHudsAreDraggableInTheEditor() {
        TrackerConfig tracker = new TrackerConfig();
        tracker.qolUtilities.extras().iotaAddonsEnabled = true;
        tracker.qolUtilities.extras().iotaArrowTracker = true;
        QolOverlayHud overlay = new QolOverlayHud(tracker);
        overlay.setEditorOpen(true);

        assertTrue(overlay.editorElementLabels().contains("Arrow Tracker"));
        assertTrue(overlay.beginDrag(20.0D, 70.0D));
        assertEquals("Arrow Tracker", overlay.selectedElementLabel());
        assertTrue(overlay.resetSelectedPosition());
        assertEquals(12.0F, tracker.qolUtilities.extras().iotaArrowHudX, 0.01F);
        assertEquals(48.0F, tracker.qolUtilities.extras().iotaArrowHudY, 0.01F);
    }

    @Test
    void kuudraAlertsHudIsDraggableInTheEditor() {
        TrackerConfig tracker = new TrackerConfig();
        tracker.qolUtilities.extras().iotaAddonsEnabled = true;
        tracker.qolUtilities.extras().iotaFreshTools = true;
        QolOverlayHud overlay = new QolOverlayHud(tracker);
        overlay.setEditorOpen(true);

        assertTrue(overlay.editorElementLabels().contains("Kuudra Alerts"));
        assertTrue(overlay.beginDrag(20.0D, 140.0D));
        assertEquals("Kuudra Alerts", overlay.selectedElementLabel());
        assertTrue(overlay.resetSelectedPosition());
        assertEquals(12.0F, tracker.qolUtilities.extras().iotaAlertHudX, 0.01F);
        assertEquals(120.0F, tracker.qolUtilities.extras().iotaAlertHudY, 0.01F);
    }

    @Test
    void wheelOverAHudScalesThatElement() {
        TrackerConfig tracker = new TrackerConfig();
        tracker.qolUtilities.playerDisplayEnabled = true;
        QolOverlayHud overlay = new QolOverlayHud(tracker);
        overlay.setEditorOpen(true);

        assertTrue(overlay.onScroll(20.0D, 255.0D, 1.0D));
        assertEquals("Health HUD", overlay.selectedElementLabel());
        assertEquals(1.1F, tracker.qolUtilities.extras().resolvedHudStyle("health").scale, 0.01F);
    }
}
