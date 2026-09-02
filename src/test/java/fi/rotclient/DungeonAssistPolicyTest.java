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
        assertEquals(1, DungeonAssistPolicy.skyBlockYear(1_560_275_700_000L));
    }

    @Test
    void f7TitlesTimersAndRagnarockParseChat() {
        assertEquals(DungeonAssistPolicy.F7Title.CRYSTAL,
                DungeonAssistPolicy.f7Title("2/3 Energy Crystals are now active!"));
        assertEquals("Crystals 2/3",
                DungeonAssistPolicy.f7TitleText("2/3 Energy Crystals are now active!").orElseThrow());
        assertEquals(DungeonAssistPolicy.F7Title.TERMINAL,
                DungeonAssistPolicy.f7Title("Henri activated a terminal! (3/7)"));
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
        assertEquals("270 Score!", DungeonAssistPolicy.scoreTitleText(270, 270));
        assertEquals("ILY Henri", DungeonAssistPolicy.leapAnnounce("ILY {name}", "Henri"));
        assertEquals("Bob", DungeonAssistPolicy.leapTarget("You leaped to Bob!").orElseThrow());
        assertEquals(40, DungeonAssistPolicy.clampOpacity(40));
    }
}
