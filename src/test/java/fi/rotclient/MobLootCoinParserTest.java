package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MobLootCoinParserTest {
    @Test
    void parsesPlainAndFormattedCoinPickups() {
        assertEquals(12L, MobLootCoinParser.parse("+12 coins").orElse(0L));
        assertEquals(12L, MobLootCoinParser.parse("+12 coin").orElse(0L));
        assertEquals(1_250L, MobLootCoinParser.parse("§6+1,250 coins").orElse(0L));
        assertEquals(8L, MobLootCoinParser.parse("  +8 Coins  ").orElse(0L));
        assertEquals(12L, MobLootCoinParser.parse(
                "❤ 123/123     +12 coins     ✎ 100/100").orElse(0L));
    }

    @Test
    void rejectsSalesAndUnrelatedChat() {
        assertTrue(MobLootCoinParser.parse(
                "[Bazaar] Sold 10x Rotten Flesh for 120 coins!").isEmpty());
        assertTrue(MobLootCoinParser.parse(
                "Sold 10x Rotten Flesh for 120 coins").isEmpty());
        assertTrue(MobLootCoinParser.parse("+2 Rotten Flesh").isEmpty());
        assertTrue(MobLootCoinParser.parse("").isEmpty());
        assertTrue(MobLootCoinParser.parse(null).isEmpty());
    }
}
