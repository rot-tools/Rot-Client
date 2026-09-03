package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MiningTrackerConfigBridgeTest {
    @Test
    void readWriteUsesTrackerConfigNotASecondStore() {
        TrackerConfig config = new TrackerConfig();
        config.showBlocks = true;
        assertTrue(MiningTrackerConfigBridge.readBoolean(
                config, "qol.mining_tracker.show_blocks"));
        assertTrue(MiningTrackerConfigBridge.writeBoolean(
                config, "qol.mining_tracker.show_blocks", false));
        assertFalse(config.showBlocks);
        assertTrue(MiningTrackerConfigBridge.writeBoolean(
                config, "qol.powder_chest.hud", false));
        assertFalse(config.powderChestHudEnabled);
    }

    @Test
    void trackerResetDoesNotClearPowderFlags() {
        TrackerConfig config = new TrackerConfig();
        config.enabled = true;
        config.showBlocks = false;
        config.powderChestHudEnabled = false;
        config.powderChestTrackerEnabled = false;
        assertTrue(MiningTrackerConfigBridge.resetModule(
                config, MiningTrackerCatalogPolicy.TRACKER));
        assertFalse(config.enabled);
        assertTrue(config.showBlocks);
        assertFalse(config.powderChestHudEnabled);
        assertFalse(config.powderChestTrackerEnabled);
        assertEquals(null, MiningTrackerConfigBridge.readBoolean(
                config, "qol.slayer_display.kill_time"));
    }
}
