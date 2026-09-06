package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonExtraStatsPolicyTest {
    @Test
    void compactReprintCollectsScoreTimeBitsAndSecrets() {
        DungeonExtraStatsPolicy.Snapshot stats = DungeonExtraStatsPolicy.Snapshot.idle();
        stats = DungeonExtraStatsPolicy.apply(stats, "                     Extra Stats                     ");
        stats = DungeonExtraStatsPolicy.apply(stats, "  ☠ Defeated Storm in 0m 12s");
        stats = DungeonExtraStatsPolicy.apply(stats, "  Team Score: 305 (S) (NEW RECORD!)");
        stats = DungeonExtraStatsPolicy.apply(stats, "  +1,234 Catacombs Experience");
        stats = DungeonExtraStatsPolicy.apply(stats, "  +56 Bits");
        stats = DungeonExtraStatsPolicy.apply(stats, "  Secrets Found: 55");
        stats = DungeonExtraStatsPolicy.apply(stats, "  Deaths: 1");
        assertTrue(stats.ready());
        assertEquals("Storm", stats.defeated());
        assertEquals("0m 12s", stats.time());
        assertEquals(305, stats.score());
        assertEquals("S", stats.letter());
        assertTrue(stats.scorePb());
        assertEquals("+56 Bits", stats.bits());
        assertEquals("55", stats.secrets());
        assertEquals("1", stats.deaths());
        assertTrue(stats.compactLines().getFirst().contains("Storm"));
        assertTrue(stats.compactLines().get(1).contains("305"));
    }
}
