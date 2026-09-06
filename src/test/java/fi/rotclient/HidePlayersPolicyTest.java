package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

final class HidePlayersPolicyTest {
    @Test
    void disabledRendersPlayers() {
        assertFalse(HidePlayersPolicy.shouldHideRemotePlayer(
                false, false, true, true, 32, 5, false));
    }

    @Test
    void hideAllHidesRemotePlayer() {
        assertTrue(HidePlayersPolicy.shouldHideRemotePlayer(
                true, false, false, true, 32, 200, false));
    }

    @Test
    void localPlayerNeverHidden() {
        assertFalse(HidePlayersPolicy.shouldHideRemotePlayer(
                true, false, true, true, 32, 1, true));
    }

    @Test
    void distanceThreshold() {
        assertTrue(HidePlayersPolicy.shouldHideRemotePlayer(
                true, false, false, false, 32, 10, false));
        assertFalse(HidePlayersPolicy.shouldHideRemotePlayer(
                true, false, false, false, 32, 64, false));
        assertEquals(1.0D, HidePlayersPolicy.clampDistance(0.1D), 0.0001D);
        assertEquals(128.0D, HidePlayersPolicy.clampDistance(999), 0.0001D);
    }

    @Test
    void dungeonOnlyOutsideDungeonFailsOpen() {
        assertFalse(HidePlayersPolicy.shouldHideRemotePlayer(
                true, true, false, true, 32, 5, false));
    }

    @Test
    void dungeonOnlyInsideDungeonApplies() {
        assertTrue(HidePlayersPolicy.shouldHideRemotePlayer(
                true, true, true, true, 32, 5, false));
        assertTrue(SkyBlockDungeonDetector.detectFromLine("The Catacombs").orElse(false));
        assertTrue(SkyBlockDungeonDetector.detectFromLine("Master Mode").orElse(false));
        assertTrue(SkyBlockDungeonDetector.detectFromLine("F7").isPresent());
        assertFalse(SkyBlockDungeonDetector.detectFromLine("Dungeon Hub").orElse(true));
        assertEquals(
                Optional.of(Boolean.FALSE),
                SkyBlockDungeonDetector.detectFromScoreboardLines(
                        java.util.List.of("Crystal Hollows", "Magma Fields")));
    }

    @Test
    void leapAndSimonSaysHideUseExactPadAndDuration() {
        assertTrue(HidePlayersPolicy.hideAfterLeap(true, true, true, 1000L, 2500L));
        assertFalse(HidePlayersPolicy.hideAfterLeap(true, true, true, 1000L, 4000L));
        assertFalse(HidePlayersPolicy.hideAfterLeap(true, true, false, 1000L, 1500L));
        assertTrue(HidePlayersPolicy.hideAtSimonSays(true, true, true, false, true));
        assertFalse(HidePlayersPolicy.hideAtSimonSays(true, true, true, true, true));
        assertFalse(HidePlayersPolicy.hideAtSimonSays(true, false, false, false, true));
    }
}
