package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DungeonMelodyProgressRegressionTest {
    @Test
    void threeRowProgressUsesObservedCompletedRows() {
        assertEquals("Melody 33%", DungeonBladePolicy.melodyProgressParty(2).orElseThrow());
        assertEquals("Melody 66%", DungeonBladePolicy.melodyProgressParty(3).orElseThrow());
        assertTrue(DungeonBladePolicy.melodyProgressParty(4).isEmpty());
        assertTrue(DungeonBladePolicy.melodyProgressParty(1).isEmpty());
    }

    @Test
    void explicitLegacyFourRowsRetainLegacyPercentages() {
        assertEquals("Melody 25%", DungeonBladePolicy.melodyProgressParty(2, 4).orElseThrow());
        assertEquals("Melody 50%", DungeonBladePolicy.melodyProgressParty(3, 4).orElseThrow());
        assertEquals("Melody 75%", DungeonBladePolicy.melodyProgressParty(4, 4).orElseThrow());
        assertTrue(DungeonBladePolicy.melodyProgressParty(5, 4).isEmpty());
        assertTrue(DungeonBladePolicy.melodyProgressParty(2, 0).isEmpty());
    }

    @Test
    void thirdPartyPercentagesNeverInventATerminalRowCount() {
        assertEquals("Mage has melody! 66%", DungeonBladePolicy.melodyTeammateHud("Mage", 66));
        assertEquals("Someone has melody! 25%", DungeonBladePolicy.melodyTeammateHud("", 25));
        assertEquals("Mage has melody! 100%", DungeonBladePolicy.melodyTeammateHud("Mage", 100));
    }
}
