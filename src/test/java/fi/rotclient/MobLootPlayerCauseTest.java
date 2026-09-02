package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MobLootPlayerCauseTest {
    @Test
    void matchesPlayerCauseOrDirectIds() {
        assertTrue(MobLootPlayerCause.matchesIds(12, 12, 40));
        assertTrue(MobLootPlayerCause.matchesIds(12, 8, 12));
        assertFalse(MobLootPlayerCause.matchesIds(12, 8, 40));
        assertFalse(MobLootPlayerCause.matchesIds(12, 0, 0));
        assertFalse(MobLootPlayerCause.matchesIds(0, 0, 0));
    }
}
