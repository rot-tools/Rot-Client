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
        assertEquals("F7", DungeonPolicy.parseSidebar(List.of("The Catacombs (F7)")).floor());
        assertEquals("E", DungeonPolicy.parseSidebar(List.of("The Catacombs (Entrance)")).floor());
        assertEquals("E", DungeonPolicy.parseSidebar(List.of("The Catacombs", "Entrance")).floor());
        assertEquals("M5", DungeonPolicy.parseSidebar(List.of("Master Mode (M5)")).floor());
        assertEquals("Entrance", DungeonPolicy.hudFloorLabel("E"));
        assertEquals("", DungeonPolicy.parseSidebar(List.of("Dungeon Hub")).floor());
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
        List<DungeonPolicy.TerminalItem> ten = List.of(
                new DungeonPolicy.TerminalItem(10, "10", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(11, "1", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(12, "5", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(13, "2", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(14, "8", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(19, "3", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(20, "9", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(21, "4", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(22, "6", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(23, "7", "red_stained_glass_pane", false));
        assertEquals(List.of(11, 13, 19, 21, 12, 22, 23, 14, 20, 10),
                DungeonPolicy.solveTerminal(DungeonPolicy.Terminal.NUMBERS, "", ten));
        List<DungeonPolicy.TerminalItem> melody = List.of(
                new DungeonPolicy.TerminalItem(5, "Magenta", "magenta_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(23, "Lime", "lime_stained_glass_pane", false));
        DungeonPolicy.MelodyState state = DungeonPolicy.parseMelody(melody);
        assertEquals(4, state.correct());
        assertEquals(1, state.buttonRow());
        assertEquals(4, state.current());
        assertTrue(state.readyToClick());
        assertEquals(25, state.clickSlot());
        List<DungeonPolicy.TerminalItem> threeRowMelody = List.of(
                new DungeonPolicy.TerminalItem(16, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(25, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(34, "Button", "lime_terracotta", false),
                new DungeonPolicy.TerminalItem(11, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(20, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(29, "Note", "white_stained_glass_pane", false));
        assertEquals(3, DungeonPolicy.melodyPlayRows(threeRowMelody));
        List<DungeonPolicy.TerminalItem> fourRowMelody = List.of(
                new DungeonPolicy.TerminalItem(16, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(25, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(34, "Button", "red_terracotta", false),
                new DungeonPolicy.TerminalItem(43, "Button", "lime_terracotta", false),
                new DungeonPolicy.TerminalItem(11, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(20, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(29, "Note", "white_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(38, "Note", "white_stained_glass_pane", false));
        assertEquals(4, DungeonPolicy.melodyPlayRows(fourRowMelody));
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
        List<DungeonPolicy.TerminalItem> greenItems = List.of(
                new DungeonPolicy.TerminalItem(10, "Lime Dye", "lime_dye", false),
                new DungeonPolicy.TerminalItem(11, "Cactus Green", "cactus_green", false),
                new DungeonPolicy.TerminalItem(12, "Poppy", "poppy", false));
        assertEquals(List.of(10, 11), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, "Select all the green items!", greenItems));
        assertEquals(List.of(12), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.SELECT_ALL, "Select all the red items!", greenItems));
        List<DungeonPolicy.TerminalItem> borderPanes = List.of(
                new DungeonPolicy.TerminalItem(10, "Red Stained Glass", "red_stained_glass", false),
                new DungeonPolicy.TerminalItem(11, "Red Stained Glass", "red_stained_glass", false),
                new DungeonPolicy.TerminalItem(16, "Red Stained Glass", "red_stained_glass", false));
        assertEquals(List.of(11), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.PANES, "Correct all the panes!", borderPanes));
        List<DungeonPolicy.TerminalItem> extraNumber = List.of(
                new DungeonPolicy.TerminalItem(11, "1", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(12, "2", "red_stained_glass_pane", false),
                new DungeonPolicy.TerminalItem(28, "3", "red_stained_glass_pane", false));
        assertEquals(List.of(11, 12), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.NUMBERS, "Click in order!", extraNumber));
        List<DungeonPolicy.TerminalItem> extraStarts = List.of(
                new DungeonPolicy.TerminalItem(10, "Apple", "apple", false),
                new DungeonPolicy.TerminalItem(37, "Axe", "iron_axe", false));
        assertEquals(List.of(10), DungeonPolicy.solveTerminal(
                DungeonPolicy.Terminal.STARTS_WITH, "What starts with: 'A'?", extraStarts));
    }

    @Test
    void chatHelpersCoverMimicPrinceInvincibilityAndRequeue() {
        assertTrue(DungeonPolicy.isMimicChat("Mimic Dead!"));
        assertTrue(DungeonPolicy.isPrinceChat("A Prince falls. +1 Bonus Score"));
        assertEquals(DungeonPolicy.Invincibility.BONZO,
                DungeonPolicy.invincibilityFromChat("Your Bonzo's Mask saved your life!"));
        assertEquals(DungeonPolicy.Invincibility.SPIRIT,
                DungeonPolicy.invincibilityFromChat("Second Wind Activated!"));
        assertEquals(DungeonPolicy.Invincibility.BONZO,
                DungeonPolicy.maskFromSkyBlockId("STARRED_BONZO_MASK"));
        assertEquals(DungeonPolicy.Invincibility.SPIRIT,
                DungeonPolicy.maskFromSkyBlockId("SPIRIT_MASK"));
        assertEquals(DungeonPolicy.Invincibility.NONE,
                DungeonPolicy.maskFromSkyBlockId("HYPERION"));
        assertEquals(16, DungeonPolicy.maskOverlayHeight(180_000L, 180_000L));
        assertEquals(8, DungeonPolicy.maskOverlayHeight(90_000L, 180_000L));
        assertEquals(0, DungeonPolicy.maskOverlayHeight(0L, 180_000L));
        assertEquals(180_000L, DungeonPolicy.cooldownMillis(DungeonPolicy.Invincibility.BONZO));
        assertEquals(30_000L, DungeonPolicy.cooldownMillis(DungeonPolicy.Invincibility.SPIRIT));
        assertEquals(60_000L, DungeonPolicy.cooldownMillis(DungeonPolicy.Invincibility.PHOENIX));
        assertTrue(DungeonPolicy.isDungeonEnd("                     Extra Stats                     "));
        assertTrue(DungeonPolicy.isExtraStatsChat("> EXTRA STATS <"));
        assertTrue(DungeonPolicy.isDungeonEnd("Dungeon Reward"));
        assertFalse(DungeonPolicy.isDungeonEnd("Team Score: 305"));
        assertFalse(DungeonPolicy.isDungeonEnd("Team Score: 0"));
        assertFalse(DungeonPolicy.isExtraStatsChat("Team Score: 305 (S)"));
        assertFalse(DungeonPolicy.isExtraStatsChat("Party > Henri: check extra stats"));
        assertFalse(DungeonPolicy.isExtraStatsChat("Click Extra Stats for more"));
        assertFalse(DungeonPolicy.isDungeonEnd("The Catacombs - Floor 7"));
        assertFalse(DungeonPolicy.isDungeonEnd("The Catacombs (F7)"));
        assertFalse(DungeonPolicy.shouldArmRequeue(
                true, false, false, 200, "                     Extra Stats                     "));
        assertFalse(DungeonPolicy.shouldArmRequeue(
                true, true, false, 10, "                     Extra Stats                     "));
        assertFalse(DungeonPolicy.shouldArmRequeue(
                true, true, false, 200, "Team Score: 305"));
        assertFalse(DungeonPolicy.shouldArmRequeue(
                true, true, false, 200, "Party > Henri: extra stats later"));
        assertTrue(DungeonPolicy.shouldArmRequeue(
                true, true, false, 200, "                     Extra Stats                     "));
        assertEquals("Mimic Killed!", DungeonPolicy.partyAnnounce("Mimic Dead!").orElseThrow());
        assertTrue(DungeonPolicy.isBloodCampReady("[BOSS] The Watcher: You have proven yourself. That will be enough."));
        assertEquals("Terminal 3/7", DungeonPolicy.f7Title("henri activated a terminal! (3/7)").orElseThrow());
        assertTrue(DungeonPolicy.isRagnarockCancelled("Ragnarock was cancelled due to taking damage!"));
        assertTrue(DungeonPolicy.isThreeWeirdosTruth("[NPC] Relieved: The reward is not in my chest!"));
        assertTrue(DungeonPolicy.isSimonStart(110, 121, 91));
        assertEquals(DungeonPolicy.EspKind.CRYSTAL, DungeonPolicy.classifyHologram("Energy Crystal"));
        assertEquals(10_000, DungeonPolicy.blazeHealth("[Lv15] Blaze 8,500/10,000❤").orElseThrow());
        assertEquals(10_000, DungeonPolicy.blazeHealth("§c[Lv15] Blaze 10,000/10,000❤").orElseThrow());
        assertTrue(DungeonPolicy.secretCountIncreased(0, 1));
        assertFalse(DungeonPolicy.secretCountIncreased(-1, 0));
        assertFalse(DungeonPolicy.secretCountIncreased(3, 3));
    }

    @Test
    void terracottaAndLeapLoreMatchHypixelMenus() {
        assertTrue(DungeonPolicy.isTerracottaStart(
                "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!"));
        assertTrue(DungeonPolicy.isTerracottaChat(
                "[BOSS] Sadan: Those terracotta soldiers were my finest work!"));
        assertFalse(DungeonPolicy.isTerracottaStart("[BOSS] The Watcher: That will be enough."));
        assertTrue(DungeonPolicy.isTerracottaEnd("[BOSS] Sadan: ENOUGH!"));
        assertFalse(DungeonPolicy.isTerracottaEnd("[BOSS] The Watcher: That will be enough."));
        assertTrue(DungeonPolicy.leapHeadDead(List.of("Class: Mage", "This player is dead!")));
        assertTrue(DungeonPolicy.leapHeadOffline(List.of("Currently offline")));
        assertTrue(DungeonPolicy.leapHeadUnavailable(List.of("Player is dead")));
        assertFalse(DungeonPolicy.leapHeadUnavailable(List.of("Class: Archer")));
        assertEquals("DEAD", DungeonPolicy.leapHeadLabel(List.of("Currently dead")));
        assertEquals("OFFLINE", DungeonPolicy.leapHeadLabel(List.of("offline")));
        assertEquals("OFFLINE", DungeonPolicy.leapHeadLabel(List.of("Currently not online")));
        assertEquals("", DungeonPolicy.leapHeadLabel(List.of("Class: Tank")));
        assertEquals("", DungeonPolicy.leapHeadLabel(List.of("Undead sword")));
    }
}
