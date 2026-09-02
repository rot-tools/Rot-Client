package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ActionBarGainParserTest {
    @Test
    void parsesHardStoneQuantityLine() {
        ActionBarGainParser.Gain gain = ActionBarGainParser.parse(
                "+128 Hard Stone").orElseThrow();
        assertEquals("Hard Stone", gain.itemName());
        assertEquals(128L, gain.quantity());
        assertEquals(TrackedMaterial.HARD_STONE, gain.material());
    }

    @Test
    void stripsMagicFindSuffixFromBowOverlayDrop() {
        ActionBarGainParser.Gain starred = ActionBarGainParser.parse(
                "+2 Rotten Flesh +3% ★ Magic Find").orElseThrow();
        assertEquals("Rotten Flesh", starred.itemName());
        assertEquals(2L, starred.quantity());
        ActionBarGainParser.Gain hypixelStar = ActionBarGainParser.parse(
                "+2 Rotten Flesh (+3 ✯ Magic Find)").orElseThrow();
        assertEquals("Rotten Flesh", hypixelStar.itemName());
        assertEquals(2L, hypixelStar.quantity());
    }

    @Test
    void parsesCommaQuantityAndStripsParenthetical() {
        ActionBarGainParser.Gain gain = ActionBarGainParser.parse(
                "+1,024 Diamond (Mining Sack)").orElseThrow();
        assertEquals("Diamond", gain.itemName());
        assertEquals(1024L, gain.quantity());
        assertEquals(TrackedMaterial.DIAMOND, gain.material());
    }

    @Test
    void ignoresSackChatEnvelope() {
        assertTrue(ActionBarGainParser.parse(
                "[Sacks] +128 Hard Stone").isEmpty());
    }

    @Test
    void rejectsNonGainText() {
        assertTrue(ActionBarGainParser.parse("Mining Speed Bonus").isEmpty());
        assertTrue(ActionBarGainParser.parse(
                "+40 Kill Combo +3% ★ Magic Find").isEmpty());
        assertTrue(ActionBarGainParser.parse(
                "+5 Crypt Ghoul ★ Magic Find").isEmpty());
        assertTrue(ActionBarGainParser.parse("").isEmpty());
    }
}
