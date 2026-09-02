package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MobLootRareDropParserTest {
    @Test
    void parsesRareDropChatAndStripsMagicFind() {
        assertEquals(
                "Poisonous Potato",
                MobLootRareDropParser.parse(
                        "§6§lRARE DROP! §r§fPoisonous Potato §r§b(+42% Magic Find)")
                        .orElse(""));
        assertEquals(
                "Spirit Leap",
                MobLootRareDropParser.parse("VERY RARE DROP! Spirit Leap")
                        .orElse(""));
        assertEquals(
                "Tarantula Silk",
                MobLootRareDropParser.parse(
                        "CRAZY RARE DROP! Tarantula Silk (+123% Magic Find)")
                        .orElse(""));
        assertEquals(42, MagicFindParser.parse(
                "§6§lRARE DROP! §r§fPoisonous Potato §r§b(+42% Magic Find)")
                .orElse(-1));
    }

    @Test
    void ignoresUnrelatedChat() {
        assertTrue(MobLootRareDropParser.parse("+2 Rotten Flesh").isEmpty());
        assertTrue(MobLootRareDropParser.parse("RARE DROP!").isEmpty());
        assertTrue(MobLootRareDropParser.parse(null).isEmpty());
    }
}
