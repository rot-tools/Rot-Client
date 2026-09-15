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
    void hiddenDungeonCarryAndBloodTimerPreviewsRemainFullyEditable() {
        TrackerConfig tracker =
                new TrackerConfig();

        DungeonAthenSettings athen =
                tracker.qolUtilities
                        .extras()
                        .athen();

        athen.carryEnabled = false;
        athen.carryDisplay = true;
        athen.watcherEnabled = false;
        athen.watcherBloodTimers = true;

        tracker.qolUtilities.setPose(
                "dungeon_carry",
                20.0F,
                160.0F);

        tracker.qolUtilities.setPose(
                "dungeon_watcher",
                20.0F,
                240.0F);

        QolOverlayHud overlay =
                new QolOverlayHud(
                        tracker);

        overlay.setEditorOpen(
                true);

        assertTrue(
                overlay.editorElementLabels()
                        .contains(
                                "Dungeon Carry Display"));

        assertTrue(
                overlay.editorElementLabels()
                        .contains(
                                "Blood Timers"));

        assertEquals(
                "dungeon_carry",
                overlay.elementAt(
                        25.0D,
                        165.0D));

        assertTrue(
                overlay.beginDrag(
                        25.0D,
                        165.0D));

        assertEquals(
                "Dungeon Carry Display",
                overlay.selectedElementLabel());

        assertTrue(
                overlay.centerSelectedHorizontally(
                        400));

        assertEquals(
                122.0F,
                tracker.qolUtilities
                        .pose("dungeon_carry")[0],
                0.01F);

        assertTrue(
                overlay.resetSelectedPosition());

        float[] defaultCarry =
                new QolUtilityConfig()
                        .pose(
                                "dungeon_carry");

        assertEquals(
                defaultCarry[0],
                tracker.qolUtilities
                        .pose("dungeon_carry")[0],
                0.01F);

        assertEquals(
                defaultCarry[1],
                tracker.qolUtilities
                        .pose("dungeon_carry")[1],
                0.01F);

        assertEquals(
                "dungeon_watcher",
                overlay.elementAt(
                        25.0D,
                        245.0D));

        assertTrue(
                overlay.beginDrag(
                        25.0D,
                        245.0D));

        assertEquals(
                "Blood Timers",
                overlay.selectedElementLabel());

        assertTrue(
                overlay.centerSelectedHorizontally(
                        400));

        assertEquals(
                122.0F,
                tracker.qolUtilities
                        .pose("dungeon_watcher")[0],
                0.01F);

        assertTrue(
                overlay.resetSelectedPosition());

        float[] defaultWatcher =
                new QolUtilityConfig()
                        .pose(
                                "dungeon_watcher");

        assertEquals(
                defaultWatcher[0],
                tracker.qolUtilities
                        .pose("dungeon_watcher")[0],
                0.01F);

        assertEquals(
                defaultWatcher[1],
                tracker.qolUtilities
                        .pose("dungeon_watcher")[1],
                0.01F);
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
