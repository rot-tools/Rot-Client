package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class SkyblockFlavorPolicyTest {
    @Test
    void detectorPingsOnceWhenHitsCollapseToOne() {
        assertTrue(SkyblockFlavorPolicy.shouldPingDetector(4, 1));
        assertFalse(SkyblockFlavorPolicy.shouldPingDetector(1, 1));
        assertFalse(SkyblockFlavorPolicy.shouldPingDetector(0, 3));
    }

    @Test
    void dwarvenCarpetsExcludeRedAndMoss() {
        assertTrue(SkyblockFlavorPolicy.isWoolCarpet("minecraft:white_carpet"));
        assertFalse(SkyblockFlavorPolicy.isWoolCarpet("minecraft:red_carpet"));
        assertFalse(SkyblockFlavorPolicy.isWoolCarpet("minecraft:moss_carpet"));
        assertTrue(SkyblockFlavorPolicy.recolorDwarvenCarpet(true, true, "minecraft:gray_carpet"));
        assertFalse(SkyblockFlavorPolicy.recolorDwarvenCarpet(true, false, "minecraft:gray_carpet"));
    }

    @Test
    void pityAndKeyLinesUseTabCounts() {
        assertEquals(
                "Mineshaft pity: 1250/2000",
                SkyblockFlavorPolicy.pityChatLine(new MiningLeftoverPolicy.Pity(1250, 2000)));
        Map<MiningLeftoverPolicy.CorpseType, Integer> corpses = Map.of(
                MiningLeftoverPolicy.CorpseType.UMBER, 2,
                MiningLeftoverPolicy.CorpseType.TUNGSTEN, 1);
        String line = SkyblockFlavorPolicy.keyAnnounceLine(
                corpses,
                Map.of("UMBER_KEY", 1, "TUNGSTEN_KEY", 0));
        assertTrue(line.contains("Umber 1/2"));
        assertTrue(line.contains("Tungsten 0/1"));
        assertFalse(line.contains("Lapis"));
    }

    @Test
    void scathaPetDropGainsRarityFromColorCodes() {
        Optional<String> rewritten = SkyblockFlavorPolicy.rewriteScathaPetDrop(
                true,
                "§d§lPET DROP! §5Scatha");
        assertTrue(rewritten.isPresent());
        assertTrue(rewritten.get().contains("Legendary"));
        assertTrue(SkyblockFlavorPolicy.rewriteScathaPetDrop(
                true,
                "PET DROP! Legendary Scatha").isEmpty());
    }

    @Test
    void previousServerAndQueueParseHypixelChat() {
        assertEquals("mini42A", SkyblockFlavorPolicy.parseServerId("Sending you to mini42A...").orElseThrow());
        assertTrue(SkyblockFlavorPolicy.shouldAnnouncePreviousServer(
                "mini42A", "mini42A", 1_000L, 1_000L + 30_000L, 360));
        assertFalse(SkyblockFlavorPolicy.shouldAnnouncePreviousServer(
                "mini42A", "mini99B", 1_000L, 2_000L, 360));
        assertEquals(42, SkyblockFlavorPolicy.parseQueuePosition(
                "You are currently in position 42 of the queue").orElseThrow());
        assertEquals(
                120,
                SkyblockFlavorPolicy.estimateQueueSeconds(50, 1_000L, 40, 31_000L).orElseThrow());
    }

    @Test
    void ministerStarsCandyAndImplosionHelpers() {
        assertEquals("Foxy", SkyblockFlavorPolicy.parseMinister(List.of("Minister: Foxy")).orElseThrow());
        assertEquals("§eMinister: §bFoxy", SkyblockFlavorPolicy.ministerTooltip(true, "Foxy"));
        assertEquals(5, SkyblockFlavorPolicy.dungeonStars(5, 0));
        assertEquals("§eDungeon Stars: §65", SkyblockFlavorPolicy.starTooltip(true, 5));
        assertEquals("§ePet Candy: §63/10", SkyblockFlavorPolicy.petCandyTooltip(true, 3));
        assertEquals(7, SkyblockFlavorPolicy.parseCandyUsed("{\"candyUsed\":7}"));
        assertTrue(SkyblockFlavorPolicy.isWitherBlade("HYPERION"));
        assertTrue(SkyblockFlavorPolicy.hideImplosion(
                true, "minecraft:explosion_emitter", true, 1.0D));
        assertFalse(SkyblockFlavorPolicy.hideImplosion(
                true, "minecraft:explosion_emitter", false, 1.0D));
        assertTrue(SkyblockFlavorPolicy.isHubRat(true, true));
        assertFalse(SkyblockFlavorPolicy.isHubRat(false, true));
        assertTrue(SkyblockFlavorPolicy.isCorpseStand("Frozen Corpse"));
        assertTrue(SkyblockFlavorPolicy.isMineshaftArea("Glacite Mineshaft"));
    }

    @Test
    void cloudsFogArmorIconsAndQuickJoinStayLocal() {
        assertEquals("Off", SkyblockFlavorPolicy.normalizeCloudMode(null));
        assertEquals("Dwarven", SkyblockFlavorPolicy.normalizeCloudMode("Dwarven Mines"));
        assertFalse(SkyblockFlavorPolicy.shouldHideClouds("Off", "Dwarven Mines"));
        assertTrue(SkyblockFlavorPolicy.shouldHideClouds("Dwarven", "Dwarven Mines"));
        assertFalse(SkyblockFlavorPolicy.shouldHideClouds("Dwarven", "Hub"));
        assertTrue(SkyblockFlavorPolicy.shouldHideClouds("Mining", "Crystal Hollows"));
        assertTrue(SkyblockFlavorPolicy.shouldHideClouds("Always", "Hub"));
        assertEquals(0.25D, SkyblockFlavorPolicy.clampFogScale(Double.NaN), 1.0e-9);
        assertEquals(0.05D, SkyblockFlavorPolicy.clampFogScale(0.01D), 1.0e-9);
        assertEquals(1.0F, SkyblockFlavorPolicy.netherFogFactor(true, true, false, 0.25D));
        assertEquals(0.25F, SkyblockFlavorPolicy.netherFogFactor(true, true, true, 0.25D));
        assertTrue(SkyblockFlavorPolicy.hideArmor(0));
        assertFalse(SkyblockFlavorPolicy.hideArmor(1));
        assertTrue(SkyblockFlavorPolicy.isAbsorbChat("Your Bonzo's Mask saved your life!"));
        assertTrue(SkyblockFlavorPolicy.isAbsorbChat("Second Wind Activated! Your Spirit Mask saved your life!"));
        assertFalse(SkyblockFlavorPolicy.isAbsorbChat("You died"));
        assertEquals(
                "Hyperion §c✪✪§6✪✪✪",
                SkyblockFlavorPolicy.revertMasterStars(true, "Hyperion ✪✪✪✪✪➋", 7).orElseThrow());
        assertTrue(SkyblockFlavorPolicy.revertMasterStars(true, "Hyperion ✪✪✪", 3).isEmpty());
        assertEquals(
                "Lv42 [Undead] Crypt Ghoul",
                SkyblockFlavorPolicy.rewriteMobIcons(true, "Lv42\ue084 Crypt Ghoul"));
        assertEquals("Join hypixel.net", SkyblockFlavorPolicy.quickJoinLabel("", ""));
        assertEquals("Play mc.hypixel.net", SkyblockFlavorPolicy.quickJoinLabel("Play {ip}", "mc.hypixel.net"));
        assertEquals("hypixel.net", SkyblockFlavorPolicy.sanitizeQuickJoinIp("  "));
    }
}
