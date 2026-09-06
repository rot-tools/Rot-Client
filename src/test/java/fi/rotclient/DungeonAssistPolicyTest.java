package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonAssistPolicyTest {
    @Test
    void quizAndWeirdosMatchKnownAnswers() {
        assertEquals("Stalker",
                DungeonAssistPolicy.quizAnswer("What is the status of The Watcher?").orElseThrow());
        assertEquals("Wool Weaver",
                DungeonAssistPolicy.quizAnswer("What is the name of the vendor in the Hub who sells stained glass?")
                        .orElseThrow());
        assertEquals(DungeonAssistPolicy.WeirdoKind.CORRECT,
                DungeonAssistPolicy.weirdoKind("The reward is not in my chest!"));
        assertEquals(DungeonAssistPolicy.WeirdoKind.WRONG,
                DungeonAssistPolicy.weirdoKind("The reward is in my chest."));
        assertEquals("Relieved", DungeonAssistPolicy.weirdoNpcName(
                "[NPC] Relieved: The reward is not in my chest!").orElseThrow());
        assertEquals("Stalker", DungeonAssistPolicy.quizAnswer(
                "[NPC] Oruo the Magician: What is the status of The Watcher?").orElseThrow());
        assertEquals("Stalker", DungeonAssistPolicy.quizAnswer(
                "[STATUE] Oruo the Omniscient: What is the status of The Watcher?").orElseThrow());
        assertEquals("289 Fairy Souls",
                DungeonAssistPolicy.quizAnswer("How many total Fairy Souls are there?").orElseThrow());
        assertEquals("61 Minions",
                DungeonAssistPolicy.quizAnswer("How many unique minions are there?").orElseThrow());
        assertEquals("5 Fairy Souls", DungeonAssistPolicy.quizAnswer(
                "How many Fairy Souls are there in Backwater Bayou?").orElseThrow());
        assertEquals(1, DungeonAssistPolicy.skyBlockYear(1_560_275_700_000L));
        assertEquals(0, DungeonAssistPolicy.quizCorrectOption(
                DungeonAssistPolicy.QUIZ_OPTION_A + " Stalker",
                List.of("Stalker")).orElseThrow());
        assertEquals(2, DungeonAssistPolicy.quizCorrectOption(
                DungeonAssistPolicy.QUIZ_OPTION_C + " Wool Weaver",
                List.of("Wool Weaver")).orElseThrow());
        assertTrue(DungeonAssistPolicy.quizCorrectOption(
                DungeonAssistPolicy.QUIZ_OPTION_B + " Wrong",
                List.of("Stalker")).isEmpty());
        assertTrue(DungeonAssistPolicy.quizAnswers(
                DungeonAssistPolicy.QUIZ_OPTION_A + " Stalker").isEmpty());
    }

    @Test
    void f7TitlesTimersAndRagnarockParseChat() {
        assertEquals(DungeonAssistPolicy.F7Title.CRYSTAL,
                DungeonAssistPolicy.f7Title("2/3 Energy Crystals are now active!"));
        assertEquals("Crystals 2/3",
                DungeonAssistPolicy.f7TitleText("2/3 Energy Crystals are now active!").orElseThrow());
        assertEquals("Crystals 2/3",
                DungeonAssistPolicy.f7TitleText(
                        "2/3 Energy Crystals are now active!",
                        "Crystals {current}/{total}",
                        "{name} Enraged",
                        "{name} {current}/{total}",
                        "{name} {current}/{total}").orElseThrow());
        assertEquals("Henri 3/7",
                DungeonAssistPolicy.f7TitleText(
                        "Henri activated a terminal! (3/7)",
                        "",
                        "",
                        "{player} {current}/{total}",
                        "").orElseThrow());
        assertEquals(DungeonAssistPolicy.F7Title.TERMINAL,
                DungeonAssistPolicy.f7Title("Henri activated a terminal! (3/7)"));
        assertTrue(DungeonAssistPolicy.otherProgressTitle("Bob completed a terminal! (3/7)", "Henri"));
        assertFalse(DungeonAssistPolicy.otherProgressTitle("Henri completed a terminal! (3/7)", "Henri"));
        assertFalse(DungeonAssistPolicy.otherProgressTitle("2/3 Energy Crystals are now active!", "Henri"));
        assertTrue(DungeonAssistPolicy.hideProgressTitleAtDevice(
                true, false, true, false, true, "Bob completed a terminal! (3/7)"));
        assertFalse(DungeonAssistPolicy.hideProgressTitleAtDevice(
                true, false, true, false, true, "Device Completed!"));
        assertFalse(DungeonAssistPolicy.hideProgressTitleAtDevice(
                true, false, true, false, false, "Bob completed a terminal! (3/7)"));
        assertEquals(DungeonAssistPolicy.F7Timer.GOLDOR,
                DungeonAssistPolicy.f7TimerFromChat("[BOSS] Goldor: Who dares trespass into my domain?"));
        assertEquals(DungeonAssistPolicy.GOLDOR_MILLIS,
                DungeonAssistPolicy.f7TimerMillis(DungeonAssistPolicy.F7Timer.GOLDOR));
        assertTrue(DungeonAssistPolicy.isRagnarockCancelled(
                "Ragnarock was cancelled due to taking damage!"));
        assertEquals(150, DungeonAssistPolicy.ragnarockStrength("Ragnarock granted you +150 strength").orElseThrow());
        assertEquals("Power X", DungeonAssistPolicy.blessingLine("Blessing of Power X").orElseThrow());
        assertTrue(DungeonAssistPolicy.isRoomAlert("PUZZLE FAIL!"));
        assertTrue(DungeonAssistPolicy.isFloor7("m7"));
    }

    @Test
    void menusSalvagePartyFinderAndChestProfit() {
        assertTrue(DungeonAssistPolicy.isPartyFinderMenu("Party Finder"));
        assertTrue(DungeonAssistPolicy.partyFinderMatches(List.of("Catacombs: 42"), 40));
        assertTrue(DungeonAssistPolicy.partyFinderMatches(List.of("Cata: 40"), 40));
        assertFalse(DungeonAssistPolicy.partyFinderMatches(List.of("Catacombs: 12"), 40));
        assertTrue(DungeonAssistPolicy.salvageable("Adaptive Helmet", 50, false));
        assertFalse(DungeonAssistPolicy.salvageable("Adaptive Helmet ✪", 50, false));
        assertEquals(0x8022C55E, DungeonAssistPolicy.salvageColor(50, 0x8022C55E, 0x80FACC15));
        assertEquals(12_000L, DungeonAssistPolicy.chestProfit("Random Dungeon Drop",
                List.of("Sell for 12,000 coins")).orElseThrow().coins());
        assertEquals(6_500_000L, DungeonAssistPolicy.itemValue("Recombobulator 3000", List.of(), true));
        assertEquals(2_000_000L, DungeonAssistPolicy.itemValue("Wither Essence x10", List.of(), true));
        assertEquals(0L, DungeonAssistPolicy.itemValue("Wither Essence x10", List.of(), false));
        assertEquals(1_000_000L, DungeonAssistPolicy.chestCost(List.of("Cost: 1,000,000 Coins")));
        var summary = DungeonAssistPolicy.summarizeChest(
                "Bedrock",
                List.of(new DungeonAssistPolicy.ChestCoinLine("Necron's Handle", 550_000_000L)),
                2_000_000L,
                true);
        assertEquals(548_000_000L, summary.profit());
        assertTrue(summary.hudLines(true).getFirst().contains("+"));
        assertEquals(List.of(100, 400), DungeonAssistPolicy.sortBlazeHealth(List.of(400, 100), true));
        assertEquals(List.of(400, 100), DungeonAssistPolicy.sortBlazeHealth(List.of(400, 100), false));
        assertEquals(10_000, DungeonAssistPolicy.blazeHealth("[Lv15] Blaze 9,000/10,000❤").orElseThrow());
        assertTrue(DungeonAssistPolicy.blazeLowestFirst("Higher Blaze", false, true, 50.0D, 60.0D));
        assertFalse(DungeonAssistPolicy.blazeLowestFirst("Lower Blaze", true, false, 80.0D, 90.0D));
        assertTrue(DungeonAssistPolicy.blazeLowestFirst("Blaze", true, false, 50.0D, 60.0D));
        assertFalse(DungeonAssistPolicy.blazeLowestFirst("Blaze", false, true, 80.0D, 90.0D));
        assertTrue(DungeonAssistPolicy.blazeLowestFirst("Blaze", false, false, 72.0D, 90.0D));
        assertFalse(DungeonAssistPolicy.blazeLowestFirst("Blaze", false, false, 50.0D, 65.0D));
        assertTrue(DungeonAssistPolicy.blazeLowestFirst(
                "Blaze", true, true, 75.0D, 50.0D, 90.0D));
        assertFalse(DungeonAssistPolicy.blazeLowestFirst(
                "Blaze", true, true, 50.0D, 50.0D, 90.0D));
        assertTrue(DungeonAssistPolicy.isBlazeIceBlock("minecraft:packed_ice"));
        assertTrue(DungeonAssistPolicy.isBlazeMagmaBlock("magma_block"));
    }

    @Test
    void noammMapFooterLeapAndScoreTitle() {
        DungeonPolicy.Sidebar sidebar = DungeonPolicy.parseSidebar(List.of(
                "Secrets: 0/33",
                "Crypts: 2",
                "Score: 142",
                "Deaths: 0"));
        List<String> footer = DungeonAssistPolicy.mapExtraInfo(sidebar, true, true, true, true);
        assertEquals(List.of("Secrets: 0/33", "Crypts: 2", "Score: 142", "Deaths: 0"), footer);
        List<DungeonAssistPolicy.MapChip> bar = DungeonAssistPolicy.mapStatusBar(
                sidebar, true, true, true, true, false, false);
        assertEquals("Secrets: 0/33", bar.getFirst().text());
        assertEquals("M: x", bar.get(4).text());
        assertEquals("P: x", bar.get(5).text());
        List<DungeonAssistPolicy.MapChip> noChips = DungeonAssistPolicy.mapStatusBar(
                sidebar, true, true, true, true, false, false, false, false);
        assertEquals(4, noChips.size());
        assertEquals("270 Score!", DungeonAssistPolicy.scoreTitleText(270, 270));
        assertEquals("ILY Henri", DungeonAssistPolicy.leapAnnounce("ILY {name}", "Henri"));
        assertEquals("Bob", DungeonAssistPolicy.leapTarget("You leaped to Bob!").orElseThrow());
        assertEquals("Henri", DungeonAssistPolicy.leapTarget("You have teleported to Henri!").orElseThrow());
        assertTrue(DungeonAssistPolicy.leapTarget("Henri leaped to you!").isEmpty());
        assertEquals(40, DungeonAssistPolicy.clampOpacity(40));
    }
}
