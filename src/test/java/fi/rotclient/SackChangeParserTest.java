package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SackChangeParserTest {
    @Test
    void parsesSignedGemstoneAndMiningSackChanges() {
        List<SackChangeParser.Change> changes =
                SackChangeParser.parse(
                        "Added items:\n"
                                + " +1,464 \uE010 "
                                + "Rough Ruby Gemstone "
                                + "(Gemstones Sack)\n"
                                + " +5 Gold Ingot "
                                + "(Mining Sack, "
                                + "Enchanted Mining Sack)");

        assertEquals(
                List.of(
                        new SackChangeParser.Change(
                                1_464L,
                                "Rough Ruby Gemstone",
                                List.of(
                                        "Gemstones Sack")),
                        new SackChangeParser.Change(
                                5L,
                                "Gold Ingot",
                                List.of(
                                        "Mining Sack",
                                        "Enchanted Mining Sack"))),
                changes);

        assertTrue(
                changes.get(0)
                        .fromSack(
                                "gemstones sack"));

        assertTrue(
                changes.get(1)
                        .fromSack(
                                "Mining Sack"));

        assertTrue(
                changes.get(1)
                        .fromSack(
                                "Enchanted Mining Sack"));
    }

    @Test
    void preservesRemovedItemsAsNegativeDeltas() {
        List<SackChangeParser.Change> changes =
                SackChangeParser.parse(
                        "Removed items:\n"
                                + " -500 Rough Ruby Gemstone "
                                + "(Gemstones Sack)");

        assertEquals(
                List.of(
                        new SackChangeParser.Change(
                                -500L,
                                "Rough Ruby Gemstone",
                                List.of(
                                        "Gemstones Sack"))),
                changes);
    }

    @Test
    void normalizesPrivateUseAndNonBreakingSpaces() {
        String text =
                "Added items:\n"
                        + "+56\u00A0\uE01C\u00A0"
                        + "Rough Topaz Gemstone "
                        + "(Gemstones Sack)";

        List<SackChangeParser.Change> changes =
                SackChangeParser.parse(
                        text);

        assertEquals(
                1,
                changes.size());

        assertEquals(
                56L,
                changes.get(0)
                        .delta());

        assertEquals(
                "Rough Topaz Gemstone",
                changes.get(0)
                        .itemName());
    }

    @Test
    void rejectsBlankAndMalformedChanges() {
        assertTrue(
                SackChangeParser.parse(
                        null)
                        .isEmpty());

        assertTrue(
                SackChangeParser.parse(
                        "")
                        .isEmpty());

        assertTrue(
                SackChangeParser.parse(
                        "Added items:\n"
                                + "Rough Ruby Gemstone")
                        .isEmpty());

        SackChangeParser.Change change =
                new SackChangeParser.Change(
                        1L,
                        "Gold Ingot",
                        List.of(
                                "Mining Sack"));

        assertFalse(
                change.fromSack(
                        "Gemstones Sack"));
    }
}
