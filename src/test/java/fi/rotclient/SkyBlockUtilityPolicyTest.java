package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class SkyBlockUtilityPolicyTest {
    @Test
    void detectorPingsOnceWhenHitsCollapseToOne() {
        assertTrue(SkyBlockUtilityPolicy.shouldPingDetector(4, 1));
        assertFalse(SkyBlockUtilityPolicy.shouldPingDetector(1, 1));
        assertFalse(SkyBlockUtilityPolicy.shouldPingDetector(0, 3));
    }

    @Test
    void dwarvenCarpetsExcludeRedAndMoss() {
        assertTrue(SkyBlockUtilityPolicy.isWoolCarpet("minecraft:white_carpet"));
        assertFalse(SkyBlockUtilityPolicy.isWoolCarpet("minecraft:red_carpet"));
        assertFalse(SkyBlockUtilityPolicy.isWoolCarpet("minecraft:moss_carpet"));
        assertTrue(SkyBlockUtilityPolicy.recolorDwarvenCarpet(true, true, "minecraft:gray_carpet"));
        assertFalse(SkyBlockUtilityPolicy.recolorDwarvenCarpet(true, false, "minecraft:gray_carpet"));
    }

    @Test
    void pityAndKeyLinesUseTabCounts() {
        assertEquals(
                "Mineshaft pity: 1250/2000",
                SkyBlockUtilityPolicy.pityChatLine(new MiningLeftoverPolicy.Pity(1250, 2000)));
        Map<MiningLeftoverPolicy.CorpseType, Integer> corpses = Map.of(
                MiningLeftoverPolicy.CorpseType.UMBER, 2,
                MiningLeftoverPolicy.CorpseType.TUNGSTEN, 1);
        String line = SkyBlockUtilityPolicy.keyAnnounceLine(
                corpses,
                Map.of("UMBER_KEY", 1, "TUNGSTEN_KEY", 0));
        assertTrue(line.contains("Umber 1/2"));
        assertTrue(line.contains("Tungsten 0/1"));
        assertFalse(line.contains("Lapis"));
    }

    @Test
    void scathaPetDropGainsRarityFromColorCodes() {
        Optional<String> rewritten = SkyBlockUtilityPolicy.rewriteScathaPetDrop(
                true,
                "§d§lPET DROP! §5Scatha");
        assertTrue(rewritten.isPresent());
        assertTrue(rewritten.get().contains("Legendary"));
        assertTrue(SkyBlockUtilityPolicy.rewriteScathaPetDrop(
                true,
                "PET DROP! Legendary Scatha").isEmpty());
    }

    @Test
    void previousServerAndQueueParseHypixelChat() {
        assertEquals("mini42A", SkyBlockUtilityPolicy.parseServerId("Sending you to mini42A...").orElseThrow());
        assertTrue(SkyBlockUtilityPolicy.shouldAnnouncePreviousServer(
                "mini42A", "mini42A", 1_000L, 1_000L + 30_000L, 360));
        assertFalse(SkyBlockUtilityPolicy.shouldAnnouncePreviousServer(
                "mini42A", "mini99B", 1_000L, 2_000L, 360));
        assertEquals(42, SkyBlockUtilityPolicy.parseQueuePosition(
                "You are currently in position 42 of the queue").orElseThrow());
        assertEquals(
                120,
                SkyBlockUtilityPolicy.estimateQueueSeconds(50, 1_000L, 40, 31_000L).orElseThrow());
    }

    @Test
    void ministerStarsCandyAndImplosionHelpers() {
        assertEquals("Foxy", SkyBlockUtilityPolicy.parseMinister(List.of("Minister: Foxy")).orElseThrow());
        assertEquals("§eMinister: §bFoxy", SkyBlockUtilityPolicy.ministerTooltip(true, "Foxy"));
        assertEquals(5, SkyBlockUtilityPolicy.dungeonStars(5, 0));
        assertEquals("§eDungeon Stars: §65", SkyBlockUtilityPolicy.starTooltip(true, 5));
        assertEquals("§ePet Candy: §63/10", SkyBlockUtilityPolicy.petCandyTooltip(true, 3));
        assertEquals(7, SkyBlockUtilityPolicy.parseCandyUsed("{\"candyUsed\":7}"));
        assertTrue(SkyBlockUtilityPolicy.isWitherBlade("HYPERION"));
        assertTrue(SkyBlockUtilityPolicy.hideImplosion(
                true, "minecraft:explosion_emitter", true, 1.0D));
        assertFalse(SkyBlockUtilityPolicy.hideImplosion(
                true, "minecraft:explosion_emitter", false, 1.0D));
        assertTrue(SkyBlockUtilityPolicy.isHubRat(true, true));
        assertFalse(SkyBlockUtilityPolicy.isHubRat(false, true));
        assertTrue(SkyBlockUtilityPolicy.isCorpseStand("Frozen Corpse"));
        assertTrue(SkyBlockUtilityPolicy.isMineshaftArea("Glacite Mineshaft"));
    }

    @Test
    void cloudsFogArmorIconsAndQuickJoinStayLocal() {
        assertEquals("Off", SkyBlockUtilityPolicy.normalizeCloudMode(null));
        assertEquals("Dwarven", SkyBlockUtilityPolicy.normalizeCloudMode("Dwarven Mines"));
        assertFalse(SkyBlockUtilityPolicy.shouldHideClouds("Off", "Dwarven Mines"));
        assertTrue(SkyBlockUtilityPolicy.shouldHideClouds("Dwarven", "Dwarven Mines"));
        assertFalse(SkyBlockUtilityPolicy.shouldHideClouds("Dwarven", "Hub"));
        assertTrue(SkyBlockUtilityPolicy.shouldHideClouds("Mining", "Crystal Hollows"));
        assertTrue(SkyBlockUtilityPolicy.shouldHideClouds("Always", "Hub"));
        assertEquals(0.25D, SkyBlockUtilityPolicy.clampFogScale(Double.NaN), 1.0e-9);
        assertEquals(0.05D, SkyBlockUtilityPolicy.clampFogScale(0.01D), 1.0e-9);
        assertEquals(1.0F, SkyBlockUtilityPolicy.netherFogFactor(true, true, false, 0.25D));
        assertEquals(0.25F, SkyBlockUtilityPolicy.netherFogFactor(true, true, true, 0.25D));
        assertTrue(SkyBlockUtilityPolicy.hideArmor(0));
        assertFalse(SkyBlockUtilityPolicy.hideArmor(1));
        assertTrue(SkyBlockUtilityPolicy.isAbsorbChat("Your Bonzo's Mask saved your life!"));
        assertTrue(SkyBlockUtilityPolicy.isAbsorbChat("Second Wind Activated! Your Spirit Mask saved your life!"));
        assertFalse(SkyBlockUtilityPolicy.isAbsorbChat("You died"));
        assertEquals(
                "Hyperion §c✪✪§6✪✪✪",
                SkyBlockUtilityPolicy.revertMasterStars(true, "Hyperion ✪✪✪✪✪➋", 7).orElseThrow());
        assertTrue(SkyBlockUtilityPolicy.revertMasterStars(true, "Hyperion ✪✪✪", 3).isEmpty());
        assertEquals(
                "Lv42 [Undead] Crypt Ghoul",
                SkyBlockUtilityPolicy.rewriteMobIcons(true, "Lv42\ue084 Crypt Ghoul"));
        assertEquals("Join hypixel.net", SkyBlockUtilityPolicy.quickJoinLabel("", ""));
        assertEquals("Play mc.hypixel.net", SkyBlockUtilityPolicy.quickJoinLabel("Play {ip}", "mc.hypixel.net"));
        assertEquals("hypixel.net", SkyBlockUtilityPolicy.sanitizeQuickJoinIp("  "));
    }
}
