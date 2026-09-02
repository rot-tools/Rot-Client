package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class FishingSuitePolicyTest {
    @Test
    void seaCreatureChatMatchesGateAndPlainVariants() {
        assertEquals(
                "Thunder",
                FishingCreaturesPolicy.matchSpawn("You hear a massive rumble as Thunder emerges.").name());
        assertEquals(
                "Titanoboa",
                FishingCreaturesPolicy.matchSpawn(
                        "A massive Titanoboa surfaces. It's body stretches as far as the eye can see.")
                        .name());
        assertTrue(FishingCreaturesPolicy.isDoubleHook("§eIt's a §r§aDouble Hook§r§e! Woot woot!"));
        assertEquals(
                "Wiki Tiki",
                FishingCreaturesPolicy.matchNametag("§c❤ Wiki Tiki §b☂").name());
        assertTrue(FishingCreaturesPolicy.looksLikeCreatureHologram("§c❤ Wiki Tiki §b☂"));
        assertFalse(FishingCreaturesPolicy.looksLikeCreatureHologram("Catfish"));
        assertFalse(FishingCreaturesPolicy.looksLikeCreatureHologram("12.5"));
        assertTrue(FishingCreaturesPolicy.shouldCapNotify(true, true, 10));
        assertFalse(FishingCreaturesPolicy.shouldCapNotify(true, true, 9));
        assertTrue(FishingCreaturesPolicy.shouldTimerNotify(true, true, 2, 340_000L, 340));
        assertTrue(FishingCreaturesPolicy.shouldTitle(true, true, "MYTHIC", "LEGENDARY"));
        assertEquals("SC Lord Jawbus x2", FishingCreaturesPolicy.compactLine(
                FishingCreaturesPolicy.matchSpawn(
                        "You have angered a legendary creature... Lord Jawbus has arrived."),
                true));
        assertTrue(FishingCreaturesPolicy.shouldAutoAttack(true, true, true, false));
        assertFalse(FishingCreaturesPolicy.shouldAutoAttack(true, true, true, true));
        assertTrue(FishingCreaturesPolicy.creatures().size() >= 80);
    }

    @Test
    void trophyAndGoldenFishParseNorthChat() {
        FishingTrophyPolicy.Catch catchInfo = FishingTrophyPolicy.parseCatch(
                "TROPHY FISH! You caught a Sulphur Skitter Diamond!");
        assertNotNull(catchInfo);
        assertEquals("SULPHUR SKITTER", catchInfo.name().toUpperCase());
        assertEquals("DIAMOND", catchInfo.rarity());
        assertTrue(FishingTrophyPolicy.shouldHideCatch(
                true,
                FishingTrophyPolicy.parseCatch("TROPHY FISH! You caught a Blobfish Bronze!"),
                "GOLD"));
        assertEquals(
                FishingTrophyPolicy.GoldenEvent.SPAWN,
                FishingTrophyPolicy.goldenEvent(
                        "You spot a Golden Fish surface from beneath the lava!"));
        assertEquals(1, FishingTrophyPolicy.nextGoldenHits(FishingTrophyPolicy.GoldenEvent.INTERACT, 0));
        assertTrue(FishingTrophyPolicy.goldenHud(true, true, 1, 0L).contains("1/3"));
    }

    @Test
    void hotspotRadarAndToolsParse() {
        assertTrue(FishingHotspotPolicy.isHotspotNametag("§6Fishing Hotspot"));
        assertTrue(FishingHotspotPolicy.isRadarFlame("minecraft:flame", 0, 0, 0));
        assertFalse(FishingHotspotPolicy.isRadarFlame("minecraft:flame", 1, 0, 0));
        var guess = FishingHotspotPolicy.guess(List.of(
                new FishingHotspotPolicy.Point(0, 64, 0),
                new FishingHotspotPolicy.Point(1, 64, 0),
                new FishingHotspotPolicy.Point(4, 64, 0)));
        assertNotNull(guess);
        assertTrue(FishingToolsPolicy.isThunderBottleId("THUNDER_IN_A_BOTTLE_EMPTY"));
        assertTrue(FishingToolsPolicy.isEmptyThunderBottle("THUNDER_IN_A_BOTTLE_EMPTY"));
        assertTrue(FishingToolsPolicy.isBottleChargedChat(
                "Your bottle of thunder has fully charged!"));
        assertEquals(Integer.valueOf(12), FishingToolsPolicy.parseBaitRemaining(
                List.of("Bait Remaining: 12")));
        assertTrue(FishingToolsPolicy.isTotemNametag("Totem of Corruption"));
        assertEquals(Integer.valueOf(75), FishingToolsPolicy.parseTotemSeconds("Remaining: 1m 15s"));
        assertTrue(FishingHelperPolicy.shouldShowBiteTitle(true, true, true, true, true));
        assertEquals(0.0F, FishingHelperPolicy.parseHookSeconds("!!!"));
        assertEquals(2.5F, FishingHelperPolicy.parseHookSeconds("§e§l2.5"));
        assertNull(FishingHelperPolicy.parseHookSeconds("125.7"));
        assertNull(FishingHelperPolicy.parseHookSeconds("12.5M"));
        assertFalse(FishingHelperPolicy.isBiteHologram("Catch!!!"));
        assertEquals(Integer.valueOf(10), FishingTrophyPolicy.filletMagmafish("SULPHUR_SKITTER_DIAMOND", 1));
        assertTrue(FishingTrophyPolicy.isGeyserCloud("minecraft:cloud", 118.0D));
        assertFalse(FishingTrophyPolicy.isGeyserCloud("minecraft:cloud", 64.0D));
        assertTrue(FishingHotspotPolicy.vanished(java.util.Set.of("1/2"), java.util.Set.of()));
        assertTrue(FishingToolsPolicy.baitChanged("Minnow Bait", "Whale Bait"));
        assertTrue(FishingToolsPolicy.shouldMuteBanshee(true, "minecraft:entity.ghast.ambient", 0.33F));
        assertTrue(FishingToolsPolicy.shouldMuteDrake(true, "minecraft:item.totem.use"));
    }
}
