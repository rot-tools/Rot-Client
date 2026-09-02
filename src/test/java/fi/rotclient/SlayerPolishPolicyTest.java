package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerPolishPolicyTest {
    @Test
    void spawnParticlesAndDeathWindowMatchNorth() {
        assertTrue(SlayerPolishPolicy.isSpawnParticle("minecraft:enchant"));
        assertTrue(SlayerPolishPolicy.isSpawnParticle("minecraft:witch"));
        assertTrue(SlayerPolishPolicy.isSpawnParticle("minecraft:entity_effect"));
        assertFalse(SlayerPolishPolicy.isSpawnParticle("minecraft:flame"));
        assertTrue(SlayerPolishPolicy.nearRecentDeath(
                1.0D, 64.0D, 1.0D, 0.0D, 64.0D, 0.0D, 0L, 1_000L));
        assertFalse(SlayerPolishPolicy.nearRecentDeath(
                20.0D, 64.0D, 0.0D, 0.0D, 64.0D, 0.0D, 0L, 1_000L));
        assertFalse(SlayerPolishPolicy.nearRecentDeath(
                1.0D, 64.0D, 1.0D, 0.0D, 64.0D, 0.0D, 0L, 4_000L));
    }

    @Test
    void damageSplashIgnoresHealthAndHitsHolograms() {
        assertTrue(SlayerPolishPolicy.isDamageSplash("✧123"));
        assertTrue(SlayerPolishPolicy.isDamageSplash("1,250⚔"));
        assertFalse(SlayerPolishPolicy.isDamageSplash("210M❤"));
        assertFalse(SlayerPolishPolicy.isDamageSplash("Hits: 42"));
        assertFalse(SlayerPolishPolicy.isDamageSplash("12s 3/8"));
    }

    @Test
    void spawnNametagsHideFullHpTrashAndKeepSpecials() {
        assertTrue(SlayerPolishPolicy.shouldHideSpawnMobName("[Lv15] Zombie 20,000/20,000❤"));
        assertTrue(SlayerPolishPolicy.shouldHideSpawnMobName("[Lv12] Weaver Spider 0/12,000❤"));
        assertFalse(SlayerPolishPolicy.shouldHideSpawnMobName("[Lv15] Zombie 8,000/20,000❤"));
        assertFalse(SlayerPolishPolicy.shouldHideSpawnMobName("[Lv15] Corrupted Zombie 20,000/20,000❤"));
        assertFalse(SlayerPolishPolicy.shouldHideSpawnMobName("[Lv600] Voidgloom Seraph IV 210M/210M❤"));
    }

    @Test
    void gummyAndMaddoxAndVampireHelpersParseKnownLines() {
        assertTrue(SlayerPolishPolicy.isGummyConsumeChat("You ate a Re-Heated Gummy Polar Bear!"));
        assertTrue(SlayerPolishPolicy.isSmolderingArea("Smoldering Tomb\nInferno Demonlord IV"));
        assertTrue(SlayerPolishPolicy.hasHabaneroEnchant("ultimate_habanero_tactics"));
        assertTrue(SlayerPolishPolicy.shouldWarnGummy(true, false, false));
        assertFalse(SlayerPolishPolicy.shouldWarnGummy(true, true, true));
        assertTrue(SlayerPolishPolicy.isGummyActive(5_000L, 1_000L, ""));
        assertTrue(SlayerPolishPolicy.isGummyActive(0L, 1_000L, "Gummy Polar Bear"));
        assertTrue(SlayerPolishPolicy.shouldBlockMaddoxClick(
                "Slayer", List.of("Requires Combat 22", "Doesn't exist here!")));
        assertFalse(SlayerPolishPolicy.shouldBlockMaddoxClick(
                "Slayer", List.of("Click to start!")));
        assertTrue(SlayerPolishPolicy.isChaliceHologram("12.5s"));
        assertFalse(SlayerPolishPolicy.isChaliceHologram("5s 8 hits"));
        assertTrue(SlayerPolishPolicy.isEffigyScoreboardLine("Effigies:  rik"));
        assertEquals(2, SlayerPolishPolicy.unbrokenEffigies(List.of("GRAY", "RED", "GRAY")).size());
        assertEquals(150, SlayerPolishPolicy.unbrokenEffigies(List.of("GRAY")).get(0).x());
        assertEquals("INFERNO:4", SlayerPolishPolicy.personalBestKey(SlayerPolicy.SlayerType.INFERNO, 4));
        assertEquals(4_000L, SlayerPolishPolicy.improvedPersonalBest(5_000L, 4_000L).orElseThrow());
        assertTrue(SlayerPolishPolicy.improvedPersonalBest(4_000L, 5_000L).isEmpty());
    }
}
