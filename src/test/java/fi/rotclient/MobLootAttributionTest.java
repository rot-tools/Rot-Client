package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MobLootAttributionTest {
    @Test
    void requiresARecentPlayerAttackAndMatchingDeath() {
        MobLootAttribution attribution = new MobLootAttribution();

        assertFalse(attribution.recordDeath(42, 1_000L));
        assertFalse(attribution.allowsInventoryGain(1_001L, false));

        attribution.recordAttack(42, 2_000L);
        assertFalse(attribution.recordDeath(43, 2_100L));
        assertTrue(attribution.recordDeath(42, 2_200L));
        assertTrue(attribution.allowsInventoryGain(2_300L, false));
    }

    @Test
    void acceptsMultipleDropsButExpiresQuickly() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(7, 10_000L);
        assertTrue(attribution.recordDeath(7, 10_500L));

        assertTrue(attribution.allowsInventoryGain(10_600L, false));
        assertTrue(attribution.allowsInventoryGain(12_999L, false));
        assertFalse(attribution.allowsInventoryGain(13_501L, false));
        assertTrue(attribution.allowsSackGain(12_999L));
        assertTrue(attribution.allowsSackGain(20_000L));
        assertFalse(attribution.allowsSackGain(20_501L));
    }

    @Test
    void combatSackWindowOutlivesTheInventoryWindow() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(8, 10_000L);
        assertTrue(attribution.recordDeath(8, 10_500L));

        assertFalse(attribution.allowsInventoryGain(14_000L, false));
        assertTrue(attribution.allowsSackGain(14_000L));
        assertTrue(attribution.allowsSackGain(20_499L));
        assertFalse(attribution.allowsSackGain(20_501L));
    }

    @Test
    void openInventoryScreenCannotTurnMovesIntoMobLoot() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(9, 20_000L);
        assertTrue(attribution.recordDeath(9, 20_100L));

        assertFalse(attribution.allowsInventoryGain(20_200L, true));
        assertTrue(attribution.allowsInventoryGain(20_200L, false));
    }

    @Test
    void worldResetDropsAllCombatContext() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(11, 30_000L);
        attribution.recordDeath(11, 30_100L);

        attribution.reset();

        assertFalse(attribution.allowsInventoryGain(30_200L, false));
        assertFalse(attribution.allowsSackGain(30_200L));
        assertFalse(attribution.recordDeath(11, 30_300L));
    }
}
