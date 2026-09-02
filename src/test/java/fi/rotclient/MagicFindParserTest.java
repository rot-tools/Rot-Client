package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MagicFindParserTest {
    @Test
    void parsesRareDropSuffixAndLabeledTabValues() {
        assertEquals(42, MagicFindParser.parse(
                "§6§lRARE DROP! §r§fPoisonous Potato §r§b(+42% Magic Find)")
                .orElse(-1));
        assertEquals(123, MagicFindParser.parse(
                "CRAZY RARE DROP! Tarantula Silk (+123% Magic Find)")
                .orElse(-1));
        assertEquals(1_234, MagicFindParser.parse("Magic Find: 1,234")
                .orElse(-1));
        assertEquals(50, MagicFindParser.parse("✯ Magic Find: 50")
                .orElse(-1));
    }

    @Test
    void ignoresMissingOrUnrelatedText() {
        assertTrue(MagicFindParser.parse("+2 Rotten Flesh").isEmpty());
        assertTrue(MagicFindParser.parse("Mining Fortune: 1,000").isEmpty());
        assertTrue(MagicFindParser.parse("").isEmpty());
        assertTrue(MagicFindParser.parse(null).isEmpty());
    }
}
