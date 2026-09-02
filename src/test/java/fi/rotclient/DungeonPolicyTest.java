package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonPolicyTest {
    @Test
    void sidebarParsesSecretsScoreFloorAndClass() {
        DungeonPolicy.Sidebar sidebar = DungeonPolicy.parseSidebar(List.of(
                "The Catacombs (F7)",
                "Cleared: 82%",
                "Secrets: 12/14",
                "Score: 287",
                "Crypts: 2",
                "Deaths: 0",
                "[Mage] Henri"));
        assertEquals("F7", sidebar.floor());
        assertEquals("12/14", sidebar.secrets());
        assertEquals(12, sidebar.secretsFound());
        assertEquals(14, sidebar.secretsTotal());
        assertEquals(287, sidebar.score());
        assertEquals(82, sidebar.clearedPercent());
        assertEquals(DungeonPolicy.DungeonClass.MAGE, sidebar.dungeonClass());
        assertEquals(2, sidebar.crypts());
        assertEquals(0, sidebar.deaths());
        assertTrue(DungeonPolicy.parseSidebar(List.of("[BOSS] Maxor: NOW YOU FACE MY BRICK PHASER!")).boss());
        assertTrue(DungeonPolicy.crossedScoreMilestone(269, 270, 270));
        assertFalse(DungeonPolicy.crossedScoreMilestone(270, 271, 270));
    }

    @Test
    void hologramsClassifyStarredKeysAndHiddenMobs() {
        assertEquals(DungeonPolicy.EspKind.STARRED, DungeonPolicy.classifyHologram("✯ Crypt Ghoul"));
        assertEquals(DungeonPolicy.EspKind.WITHER_KEY, DungeonPolicy.classifyHologram("Wither Key"));
        assertEquals(DungeonPolicy.EspKind.BLOOD_KEY, DungeonPolicy.classifyHologram("Blood Key"));
        assertEquals(DungeonPolicy.EspKind.SHADOW_ASSASSIN, DungeonPolicy.classifyHologram("Shadow Assassin"));
        assertEquals(DungeonPolicy.EspKind.FEL, DungeonPolicy.classifyHologram("Fels"));
        assertTrue(DungeonPolicy.isSecretBat("bat"));
        assertFalse(DungeonPolicy.isSecretBat("zombie"));
    }

    @Test
    void leapMenuAndClassOrderMatchLayout() {
        assertTrue(DungeonPolicy.isLeapMenu("Spirit Leap"));
        assertEquals(DungeonPolicy.DungeonClass.ARCHER, DungeonPolicy.classFromLore(List.of("Class: Archer")));
        assertTrue(DungeonPolicy.leapSortKey(DungeonPolicy.DungeonClass.ARCHER)
                < DungeonPolicy.leapSortKey(DungeonPolicy.DungeonClass.HEALER));
    }

    @Test
    void terminalsSolvePanesStartsWithAndNumbers() {
        assertEquals(DungeonPolicy.Terminal.PANES, DungeonPolicy.detectTerminal("Correct all the panes!"));
        assertEquals(DungeonPolicy.Terminal.STARTS_WITH, DungeonPolicy.detectTerminal("What starts with: 'A'?"));
        assertEquals(DungeonPolicy.Terminal.NUMBERS, DungeonPolicy.detectTerminal("Click in order!"));
        List<DungeonPolicy.TerminalItem> panes = List.of(
                new DungeonPolicy.TerminalItem(10, "Lime Stained Glass", "lime_stained_glass", false),
                new DungeonPolicy.TerminalItem(11, "Red Stained Glass", "red_stained_glass", false));
        assertEquals(List.of(11), DungeonPolicy.solveTerminal(DungeonPolicy.Terminal.PANES, "", panes));
        List<DungeonPolicy.TerminalItem> starts = List.of(
                new DungeonPolicy.TerminalItem(10, "Apple", "apple", false),
                new DungeonPolicy.TerminalItem(11, "Bone", "bone", false));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.STARTS_WITH, "What starts with: 'A'?", starts));
        List<DungeonPolicy.TerminalItem> numbers = List.of(
                new DungeonPolicy.TerminalItem(10, "2", "blue_stained_glass", false),
                new DungeonPolicy.TerminalItem(12, "1", "blue_stained_glass", false),
                new DungeonPolicy.TerminalItem(13, "3", "blue_stained_glass", true));
        assertEquals(List.of(12, 10), DungeonPolicy.solveTerminal(DungeonPolicy.Terminal.NUMBERS, "", numbers));
        List<DungeonPolicy.TerminalItem> melody = List.of(
                new DungeonPolicy.TerminalItem(5, "Magenta", "magenta_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(23, "Lime", "lime_stained_glass_pane", false));
        DungeonPolicy.MelodyState state = DungeonPolicy.parseMelody(melody);
        assertEquals(4, state.correct());
        assertEquals(1, state.buttonRow());
        assertEquals(4, state.current());
        assertTrue(state.readyToClick());
        assertEquals(25, state.clickSlot());
        List<DungeonPolicy.TerminalItem> colors = List.of(
                new DungeonPolicy.TerminalItem(10, "Light Gray Dye", "light_gray_dye", false),
                new DungeonPolicy.TerminalItem(11, "Ink Sac", "ink_sac", false));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, "Select all the silver items!", colors));
        assertEquals(DungeonPolicy.Terminal.COLORS, DungeonPolicy.detectTerminal("What color was red?"));
        assertEquals(DungeonPolicy.Terminal.COLORS, DungeonPolicy.detectTerminal("Select the color silver"));
        assertEquals("red", DungeonPolicy.extractSelectColor("What color was red?"));
        assertEquals("silver", DungeonPolicy.extractSelectColor("Select the color light gray"));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.COLORS, "What color was silver?", colors));
        assertTrue(DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.COLORS, "What color was the stained glass?", colors).isEmpty());
    }

    @Test
    void chatHelpersCoverMimicPrinceInvincibilityAndRequeue() {
        assertTrue(DungeonPolicy.isMimicChat("Mimic Dead!"));
        assertTrue(DungeonPolicy.isPrinceChat("A Prince falls. +1 Bonus Score"));
        assertEquals(DungeonPolicy.Invincibility.BONZO,
                DungeonPolicy.invincibilityFromChat("Your Bonzo's Mask saved your life!"));
        assertEquals(DungeonPolicy.Invincibility.SPIRIT,
                DungeonPolicy.invincibilityFromChat("Second Wind Activated!"));
        assertTrue(DungeonPolicy.isDungeonEnd("                     Extra Stats                     "));
        assertEquals("Mimic Killed!", DungeonPolicy.partyAnnounce("Mimic Dead!").orElseThrow());
        assertTrue(DungeonPolicy.isBloodCampReady("[BOSS] The Watcher: You have proven yourself. That will be enough."));
        assertEquals("Terminal 3/7", DungeonPolicy.f7Title("henri activated a terminal! (3/7)").orElseThrow());
        assertTrue(DungeonPolicy.isRagnarockCancelled("Ragnarock was cancelled due to taking damage!"));
        assertTrue(DungeonPolicy.isThreeWeirdosTruth("[NPC] Relieved: The reward is not in my chest!"));
        assertTrue(DungeonPolicy.isSimonStart(110, 121, 91));
        assertEquals(DungeonPolicy.EspKind.CRYSTAL, DungeonPolicy.classifyHologram("Energy Crystal"));
        assertTrue(DungeonPolicy.secretCountIncreased(0, 1));
        assertFalse(DungeonPolicy.secretCountIncreased(-1, 0));
        assertFalse(DungeonPolicy.secretCountIncreased(3, 3));
    }
}
