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
    void playersCannotSpoofOrHideChatByTypingServerPhrases() {
        // Only the server line counts, and it may end in "." or "!".
        assertNotNull(FishingTrophyPolicy.parseCatch("TROPHY FISH! You caught a Blobfish Bronze."));
        assertNotNull(FishingTrophyPolicy.parseCatch("TROPHY FISH! You caught a Blobfish Bronze!"));
        assertNull(FishingTrophyPolicy.parseCatch("[MVP+] Steve: TROPHY FISH! You caught a Blobfish Bronze!"));
        assertEquals(
                FishingTrophyPolicy.GoldenEvent.NONE,
                FishingTrophyPolicy.goldenEvent("Steve: The Golden Fish is weak!"));
        assertEquals(
                FishingTrophyPolicy.GoldenEvent.WEAK,
                FishingTrophyPolicy.goldenEvent("The Golden Fish is weak!"));
        assertTrue(FishingToolsPolicy.isBottleChargedChat("Your Bottle of Thunder has fully charged!"));
        assertFalse(FishingToolsPolicy.isBottleChargedChat(
                "Steve: your bottle of thunder has fully charged"));
    }

    @Test
    void lochEmperorChatMatchesTheSameNameAsItsNametag() {
        FishingCreaturesPolicy.Creature spawned =
                FishingCreaturesPolicy.matchSpawn("The Loch Emperor arises from the depths.");
        assertEquals("The Loch Emperor", spawned.name());
        assertEquals(
                spawned.name(),
                FishingCreaturesPolicy.matchNametag("[Lv600] The Loch Emperor 5M\u2764").name());
    }

    @Test
    void barnTimerIsDueAsALevelNotAShortWindow() {
        // 340s default. Well past the old 250 ms window, it must still be due.
        assertTrue(FishingCreaturesPolicy.timerDue(true, true, 3, 340_000L, 340));
        assertTrue(FishingCreaturesPolicy.timerDue(true, true, 3, 500_000L, 340));
        assertFalse(FishingCreaturesPolicy.timerDue(true, true, 3, 339_999L, 340));
        assertFalse(FishingCreaturesPolicy.timerDue(true, true, 0, 500_000L, 340));
        assertFalse(FishingCreaturesPolicy.timerDue(true, false, 3, 500_000L, 340));
    }

    @Test
    void totemRemainingLineIsItsOwnHologramLine() {
        assertTrue(FishingToolsPolicy.isTotemRemaining("Remaining: 4m 12s"));
        assertEquals(Integer.valueOf(252), FishingToolsPolicy.parseTotemSeconds("Remaining: 4m 12s"));
        assertFalse(FishingToolsPolicy.isTotemRemaining("Totem of Corruption"));
        assertFalse(FishingToolsPolicy.isTotemRemaining("Owner: Steve"));
        assertNull(FishingToolsPolicy.parseTotemSeconds("Remaining: 99999999999m 1s"));
    }

    @Test
    void hotspotOnlyCountsAsGoneWhenItWasNearby() {
        java.util.List<FishingHotspotPolicy.Circle> before =
                java.util.List.of(new FishingHotspotPolicy.Circle(100.0D, 70.0D, 100.0D, 8.0D));
        java.util.Set<String> none = java.util.Set.of();
        // 20 blocks away: it should have been seen, so it really is gone.
        assertTrue(FishingHotspotPolicy.vanishedNearby(before, none, 100.0D, 120.0D, 40.0D));
        // 47 blocks away: it just left scan range, which is not a despawn.
        assertFalse(FishingHotspotPolicy.vanishedNearby(before, none, 100.0D, 147.0D, 40.0D));
        assertFalse(FishingHotspotPolicy.vanishedNearby(
                before, java.util.Set.of(FishingHotspotPolicy.key(100.0D, 100.0D)), 100.0D, 120.0D, 40.0D));
    }

    @Test
    void stripFormattingKeepsBehaviourAndSkipsPlainText() {
        assertEquals("Hello", ChatTextPolicy.stripFormatting("§aHel§r§llo"));
        assertEquals("Hello", ChatTextPolicy.stripFormatting("&aHel&llo"));
        assertEquals("plain text", ChatTextPolicy.stripFormatting("plain text"));
        assertEquals("", ChatTextPolicy.stripFormatting(null));
    }

    @Test
    void hotspotAndToolsParse() {
        assertTrue(FishingHotspotPolicy.isHotspotNametag("§6Fishing Hotspot"));
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
