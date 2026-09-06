package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonBladePolicyTest {
    @Test
    void warpCooldownFiresOnEnteredCatacombsChat() {
        assertTrue(DungeonBladePolicy.dungeonEnterChat(
                "§9§m-\n§r§b[MVP§c+§b] Henri §eentered The Catacombs, Floor VII!\n§9§m-"));
        assertTrue(DungeonBladePolicy.dungeonEnterChat(
                "[MVP+] Henri entered MM The Catacombs, Floor VII!"));
        assertFalse(DungeonBladePolicy.dungeonEnterChat("Dungeon starts in 5 seconds."));
        assertEquals("Warp 12.3s", DungeonBladePolicy.warpHudLine(12_300L));
        assertEquals("", DungeonBladePolicy.warpHudLine(0L));
    }

    @Test
    void quizTimerStartsOnOruoIntroThenQuestionLines() {
        assertEquals(DungeonBladePolicy.QUIZ_INTRO_TICKS, DungeonBladePolicy.quizTimerTicks(
                "[STATUE] Oruo the Omniscient: I am Oruo the Omniscient. I have lived many lives. I have learned all there is to know.")
                .orElseThrow());
        assertEquals(1, DungeonBladePolicy.quizStage(
                "[STATUE] Oruo the Omniscient: I am Oruo the Omniscient. I have lived many lives. I have learned all there is to know."));
        assertEquals(DungeonBladePolicy.QUIZ_QUESTION_TICKS, DungeonBladePolicy.quizTimerTicks(
                "[STATUE] Oruo the Omniscient: 2 questions left... Then you will have proven your worth to me!")
                .orElseThrow());
        assertEquals(2, DungeonBladePolicy.quizStage(
                "[STATUE] Oruo the Omniscient: 2 questions left... Then you will have proven your worth to me!"));
        assertEquals(3, DungeonBladePolicy.quizStage(
                "[STATUE] Oruo the Omniscient: One more question!"));
        assertEquals("Quiz 2/3 5.0s", DungeonBladePolicy.quizHudLine(2, 100));
    }

    @Test
    void maxorStunAndStormCrushMatchBossChat() {
        assertTrue(DungeonBladePolicy.maxorStunStart(
                "[BOSS] Maxor: THAT BEAM! IT HURTS! IT HURTS!!"));
        assertTrue(DungeonBladePolicy.maxorEnraged("⚠ Maxor is enraged! ⚠"));
        assertTrue(DungeonBladePolicy.stormCrushChat("[BOSS] Storm: Oof"));
        assertTrue(DungeonBladePolicy.stormCrushChat("[BOSS] Storm: Ouch, that hurt!"));
        assertFalse(DungeonBladePolicy.stormCrushChat("[BOSS] Storm: Pathetic."));
        assertEquals(12_345.0D, DungeonBladePolicy.explosiveShotPerEnemy(
                "Your Explosive Shot hit 2 enemies for 24,690 damage.").orElseThrow(), 0.01D);
        assertEquals("Explosive 12345 / enemy", DungeonBladePolicy.explosiveShotHudLine(12_345.0D));
    }

    @Test
    void deathPartySecretSpawnStormLbAndMelodyProgressParse() {
        assertEquals("Henri", DungeonBladePolicy.deathPlayer(
                " ☠ Henri was killed by Storm and became a ghost.").orElseThrow());
        assertEquals("You", DungeonBladePolicy.deathPlayer(
                "☠ You were killed by a Super Tank Zombie and became a ghost.").orElseThrow());
        assertEquals("Henri died", DungeonBladePolicy.deathPartyMessage("{player} died", "Henri"));
        assertTrue(DungeonBladePolicy.timeElapsedTab("Time Elapsed: 1m 12s"));
        assertEquals("Secrets 20", DungeonBladePolicy.secretSpawnHudLine(20));
        assertEquals(80, DungeonBladePolicy.stormLbRemaining(600));
        assertEquals("Last Breath 4.0s", DungeonBladePolicy.stormLbHudLine(80));
        assertEquals("", DungeonBladePolicy.stormLbHudLine(200));
        assertEquals("Terms 5.0s", DungeonBladePolicy.termStartHudLine(100));
        assertEquals(2, DungeonBladePolicy.unclaimedChests("Unclaimed chests: 2").orElseThrow());
        assertEquals(3, DungeonBladePolicy.unclaimedChests(" Unclaimed chests: 3").orElseThrow());
        assertEquals("Melody 50%", DungeonBladePolicy.melodyProgressParty(3).orElseThrow());
        assertEquals("Terminal (3/7)", DungeonBladePolicy.sectionObjectiveHud("Terminal", 3, 7));
        assertEquals(2, DungeonBladePolicy.melodyClayRow(List.of(
                new DungeonPolicy.TerminalItem(20, "Clay", "lime_terracotta", false, 1)))
                .orElseThrow());
    }

    @Test
    void predevSsLocationPlayerCountAndChestWarningMatchEdgeNumbers() {
        assertTrue(DungeonBladePolicy.atThirdDevice(1.0D, 77.0D));
        assertTrue(DungeonBladePolicy.atThirdDevice(3.5D, 77.0D));
        assertFalse(DungeonBladePolicy.atThirdDevice(10.0D, 77.0D));
        assertTrue(DungeonBladePolicy.atSimonSays(108.0D, 119.0D, 94.0D));
        assertTrue(DungeonBladePolicy.atSimonSays(110.0D, 119.0D, 94.0D));
        assertFalse(DungeonBladePolicy.atSimonSays(0.0D, 64.0D, 0.0D));
        assertTrue(DungeonBladePolicy.ownLeapChat("You have teleported to Henri!"));
        assertTrue(DungeonBladePolicy.ownLeapChat("You leaped to Henri!"));
        assertFalse(DungeonBladePolicy.ownLeapChat("Henri leaped to Bob!"));
        assertTrue(DungeonBladePolicy.startingCountdown("Starting in 5 seconds"));
        assertTrue(DungeonBladePolicy.startingCountdown("Starting in 1 second"));
        assertFalse(DungeonBladePolicy.startingCountdown("Dungeon starts in 5 seconds."));
        assertEquals(2, DungeonBladePolicy.scoreboardClassCount(List.of("[A] Henri", "[M] Bob", "Secrets: 1/2")));
        assertTrue(DungeonBladePolicy.notEnoughPlayers(4));
        assertFalse(DungeonBladePolicy.notEnoughPlayers(5));
        assertFalse(DungeonBladePolicy.notEnoughPlayers(0));
        assertTrue(DungeonBladePolicy.shouldTrackPredev(true, false, true));
        assertFalse(DungeonBladePolicy.shouldTrackPredev(true, false, false));
        assertTrue(DungeonBladePolicy.shouldTrackPredev(true, true, false));
        assertEquals("Predev 12.34s (PB 11.02s)", DungeonBladePolicy.predevHudLine(12_340L, 11_020L, true));
        assertTrue(DungeonBladePolicy.chestWarningReached(50, 5, 55));
        assertFalse(DungeonBladePolicy.chestWarningReached(50, 4, 55));
        assertEquals(55, DungeonBladePolicy.clampChestWarning(55));
        assertEquals(60, DungeonBladePolicy.clampChestWarning(99));
        DungeonBladePolicy.PartyLocation notice = DungeonBladePolicy.partyLocation(
                "Party > [MVP+] Bob: At EE2!").orElseThrow();
        assertEquals("Bob", notice.username());
        assertEquals("Bob is At EE2!!", notice.hud());
        assertTrue(DungeonBladePolicy.partyLocation(
                "Party > Henri: Inside Goldor Tunnel!").isPresent());
        assertTrue(DungeonBladePolicy.partyLocation("Party > Henri: hi").isEmpty());
        assertTrue(DungeonBladePolicy.atFourthDevice(63.5D, 127.0D, 35.5D));
        assertFalse(DungeonBladePolicy.atFourthDevice(108.0D, 119.0D, 94.0D));
        assertTrue(DungeonBladePolicy.dungeonDropName("§aArchitect's First Draft"));
        assertTrue(DungeonBladePolicy.highlightDungeonDrop(true, true, false, "Revive Stone"));
        assertFalse(DungeonBladePolicy.highlightDungeonDrop(true, true, true, "Revive Stone"));
        assertEquals(DungeonBladePolicy.ITEM_HIGHLIGHT_DELAY_COLOR,
                DungeonBladePolicy.dungeonDropColor(2.0D, 5));
        assertEquals(DungeonBladePolicy.ITEM_HIGHLIGHT_READY_COLOR,
                DungeonBladePolicy.dungeonDropColor(2.0D, 12));
        assertEquals(DungeonBladePolicy.ITEM_HIGHLIGHT_FAR_COLOR,
                DungeonBladePolicy.dungeonDropColor(8.0D, 20));
        assertEquals(0, DungeonBladePolicy.dungeonDropColor(21.0D, 20));
        DungeonBladePolicy.MelodyParty melody = DungeonBladePolicy.melodyPartyPercent(
                "Party > [MVP+] Bob: Melody 50%").orElseThrow();
        assertEquals("Bob", melody.username());
        assertEquals(50, melody.percent());
        assertEquals(2, DungeonBladePolicy.melodyQuarters(50));
        assertEquals("Mage has melody! 2/4", DungeonBladePolicy.melodyTeammateHud("Mage", 50));
        assertTrue(DungeonBladePolicy.melodyPartyPercent("Party > Henri: hi").isEmpty());
        assertTrue(DungeonF7Policy.holdingRelicOrMenu("Corrupted Red Relic"));
        assertTrue(DungeonF7Policy.holdingRelicOrMenu("SkyBlock Menu"));
        assertFalse(DungeonF7Policy.holdingRelicOrMenu("Hyperion"));
    }
}
