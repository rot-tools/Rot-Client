package fi.rotclient;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonLeftoverPolicyTest {
    @Test
    void queueTermsEnqueuesAndDrainsWithCooldown() {
        assertTrue(DungeonLeftoverPolicy.queueTermsSupported());
        Queue<DungeonLeftoverPolicy.QueuedClick> queue = new ArrayDeque<>();
        DungeonLeftoverPolicy.enqueue(queue, 10, 0);
        DungeonLeftoverPolicy.enqueue(queue, 12, 1);
        assertTrue(DungeonLeftoverPolicy.dequeueIfReady(queue, 2).isEmpty());
        assertEquals(10, DungeonLeftoverPolicy.dequeueIfReady(queue, 0).orElseThrow().slot());
        assertEquals(12, DungeonLeftoverPolicy.dequeueIfReady(queue, 0).orElseThrow().slot());
    }

    @Test
    void ghostBlockBlacklistsSecretsAndAllowsStone() {
        assertTrue(DungeonLeftoverPolicy.isGhostBlacklisted("minecraft:chest"));
        assertTrue(DungeonLeftoverPolicy.isGhostBlacklisted("oak_door"));
        assertTrue(DungeonLeftoverPolicy.isGhostBlacklisted("minecraft:air"));
        assertFalse(DungeonLeftoverPolicy.isGhostBlacklisted("minecraft:stone"));
        assertTrue(DungeonLeftoverPolicy.canGhostBlock(true, true, true, "stone"));
        assertFalse(DungeonLeftoverPolicy.canGhostBlock(true, true, false, "stone"));
        assertTrue(DungeonLeftoverPolicy.isPickaxe("diamond_pickaxe", "Stonk"));
    }

    @Test
    void triggerBotIsSecretOrCrystalNotGeneralPvp() {
        assertEquals(
                DungeonLeftoverPolicy.TriggerKind.SECRET,
                DungeonLeftoverPolicy.triggerKind(true, true, false, "minecraft:chest", ""));
        assertEquals(
                DungeonLeftoverPolicy.TriggerKind.CRYSTAL,
                DungeonLeftoverPolicy.triggerKind(true, false, true, "air", "Energy Crystal"));
        assertEquals(
                DungeonLeftoverPolicy.TriggerKind.NONE,
                DungeonLeftoverPolicy.triggerKind(true, true, false, "minecraft:stone", "Zombie"));
        assertTrue(DungeonLeftoverPolicy.isEnergyCrystalName("Energy Crystal"));
    }

    @Test
    void autoDojoParsesTestsAndDisciplineSwords() {
        assertEquals(
                DungeonLeftoverPolicy.DojoType.CONTROL,
                DungeonLeftoverPolicy.dojoTypeFromChat("[NPC] Ginko: Test of Control"));
        assertEquals(
                DungeonLeftoverPolicy.DojoType.MASTERY,
                DungeonLeftoverPolicy.dojoTypeFromChat("Test of Mastery has begun"));
        assertEquals(
                DungeonLeftoverPolicy.DojoType.DISCIPLINE,
                DungeonLeftoverPolicy.dojoTypeFromChat("Test of Discipline"));
        assertTrue(DungeonLeftoverPolicy.dojoChatClears("You have completed the Dojo challenge"));
        assertEquals("wooden_sword", DungeonLeftoverPolicy.disciplineSwordForHelmet("leather_helmet"));
        assertEquals("diamond_sword", DungeonLeftoverPolicy.disciplineSwordForHelmet("minecraft:diamond_helmet"));
    }

    @Test
    void superboomWallsMatchHearthDefaultsPlusCryptStone() {
        assertTrue(DungeonLeftoverPolicy.isSuperboomWall("minecraft:cracked_stone_bricks"));
        assertTrue(DungeonLeftoverPolicy.isSuperboomWall("coal_block"));
        assertFalse(DungeonLeftoverPolicy.isSuperboomWall("minecraft:chest"));
        assertTrue(DungeonLeftoverPolicy.shouldAutoSuperboom(true, true, true, true));
        assertFalse(DungeonLeftoverPolicy.shouldAutoSuperboom(true, true, true, false));
    }

    @Test
    void cheaterMapRevealsDoorTargetsAndDarkens() {
        DungeonMapPolicy.Calibration cal = new DungeonMapPolicy.Calibration(true, 22, 22, 16, 20, 0.625D);
        DungeonMapPolicy.RoomTile room = new DungeonMapPolicy.RoomTile(
                0, 0, DungeonMapPolicy.RoomType.NORMAL, DungeonMapPolicy.Checkmark.NONE, 22, 22);
        DungeonMapPolicy.DoorTile door = new DungeonMapPolicy.DoorTile(
                0, 0, true, DungeonMapPolicy.DoorType.WITHER);
        DungeonMapPolicy.Board board = new DungeonMapPolicy.Board(
                cal, List.of(room), List.of(door), List.of(), "1 rooms");
        DungeonMapPolicy.Board revealed = DungeonLeftoverPolicy.revealHiddenRooms(board);
        assertEquals(2, revealed.rooms().size());
        assertEquals(DungeonMapPolicy.RoomType.UNDISCOVERED, revealed.rooms().get(1).type());
        assertTrue(DungeonLeftoverPolicy.darkenArgb(0xFFFFFFFF, 0.5D) < 0xFFFFFFFF);
        assertEquals("?", DungeonLeftoverPolicy.cheaterRoomLabel(revealed.rooms().get(1)));
    }

    @Test
    void leapOverlayIsTwoByTwoAndClickable() {
        List<DungeonLeftoverPolicy.LeapEntry> players = List.of(
                new DungeonLeftoverPolicy.LeapEntry(11, "Mage", DungeonPolicy.DungeonClass.MAGE),
                new DungeonLeftoverPolicy.LeapEntry(12, "Arch", DungeonPolicy.DungeonClass.ARCHER));
        List<DungeonLeftoverPolicy.LeapCell> cells =
                DungeonLeftoverPolicy.leapOverlay(400, 240, players);
        assertEquals(4, cells.size());
        assertEquals("Arch", cells.get(0).entry().name());
        List<DungeonLeftoverPolicy.LeapEntry> mixed = List.of(
                new DungeonLeftoverPolicy.LeapEntry(11, "DeadArch", DungeonPolicy.DungeonClass.ARCHER, true),
                new DungeonLeftoverPolicy.LeapEntry(12, "Mage", DungeonPolicy.DungeonClass.MAGE, false));
        List<DungeonLeftoverPolicy.LeapCell> sorted =
                DungeonLeftoverPolicy.leapOverlay(400, 240, mixed);
        assertEquals("Mage", sorted.get(0).entry().name());
        assertTrue(sorted.get(1).entry().dead());
        DungeonLeftoverPolicy.LeapEntry offline = DungeonLeftoverPolicy.LeapEntry.fromLore(
                13,
                "Gone",
                DungeonPolicy.DungeonClass.HEALER,
                List.of("Currently offline"));
        assertEquals("OFFLINE", offline.statusLabel());
        assertTrue(offline.dead());
        DungeonLeftoverPolicy.LeapEntry living = DungeonLeftoverPolicy.LeapEntry.fromLore(
                14,
                "Tank",
                DungeonPolicy.DungeonClass.TANK,
                List.of("Class: Tank"));
        assertEquals("", living.statusLabel());
        assertFalse(living.dead());
        assertTrue(DungeonLeftoverPolicy.cellAt(cells, cells.get(0).x() + 4, cells.get(0).y() + 4).isPresent());
        List<DungeonLeftoverPolicy.LeapEntry> chestOrder = List.of(
                new DungeonLeftoverPolicy.LeapEntry(11, "Healer", DungeonPolicy.DungeonClass.HEALER),
                new DungeonLeftoverPolicy.LeapEntry(12, "Arch", DungeonPolicy.DungeonClass.ARCHER));
        assertEquals("Arch", DungeonLeftoverPolicy.leapDigitTarget(chestOrder, 1, true).orElseThrow().name());
        assertEquals("Healer", DungeonLeftoverPolicy.leapDigitTarget(chestOrder, 1, false).orElseThrow().name());
        assertTrue(DungeonLeftoverPolicy.leapDigitTarget(chestOrder, 3, false).isEmpty());
    }

    @Test
    void dungeonSplitTimersFollowBloodThenBoss() {
        long t0 = 1_000_000L;
        var start = DungeonLeftoverPolicy.applySplit(
                null, DungeonLeftoverPolicy.SplitEvent.START, t0);
        var blood = DungeonLeftoverPolicy.applySplit(
                start, DungeonLeftoverPolicy.splitEvent("The BLOOD DOOR has been opened!"), t0 + 45_000L);
        assertTrue(blood.bloodRushDone());
        assertEquals(45_000L, blood.bloodRushMs());
        var clear = DungeonLeftoverPolicy.applySplit(
                blood, DungeonLeftoverPolicy.SplitEvent.BLOOD_CLEAR, t0 + 70_000L);
        var boss = DungeonLeftoverPolicy.applySplit(
                clear, DungeonLeftoverPolicy.splitEvent("[BOSS] Bonzo: Fun!"), t0 + 90_000L);
        assertTrue(boss.bossDone());
        assertEquals(90_000L, boss.bossEnterMs());
        assertFalse(boss.hudLines(t0 + 90_000L).isEmpty());
        Map<String, Long> pbs = new LinkedHashMap<>();
        assertTrue(DungeonLeftoverPolicy.recordSplitPersonalBest(pbs, "BLOOD_RUSH", 45_000L));
        assertFalse(DungeonLeftoverPolicy.recordSplitPersonalBest(pbs, "BLOOD_RUSH", 50_000L));
        String hud = String.join("\n", boss.hudLines(t0 + 90_000L, pbs));
        assertTrue(hud.contains("PB"));
        assertEquals(
                DungeonLeftoverPolicy.SplitEvent.RUN_END,
                DungeonLeftoverPolicy.splitEvent("                     Extra Stats                     "));
        assertEquals(
                DungeonLeftoverPolicy.SplitEvent.NONE,
                DungeonLeftoverPolicy.splitEvent("Team Score: 305"));
        assertEquals(
                DungeonLeftoverPolicy.SplitEvent.NONE,
                DungeonLeftoverPolicy.splitEvent("Party > Henri: extra stats later"));
    }

    @Test
    void termSimPersonalBestsKeepFasterTimes() {
        Map<TermSimPolicy.Kind, Integer> pbs = new EnumMap<>(TermSimPolicy.Kind.class);
        assertTrue(DungeonLeftoverPolicy.recordPersonalBest(pbs, TermSimPolicy.Kind.PANES, 3200));
        assertFalse(DungeonLeftoverPolicy.recordPersonalBest(pbs, TermSimPolicy.Kind.PANES, 4000));
        assertTrue(DungeonLeftoverPolicy.recordPersonalBest(pbs, TermSimPolicy.Kind.PANES, 2100));
        String stored = DungeonLeftoverPolicy.writePersonalBests(pbs);
        assertEquals(2100, DungeonLeftoverPolicy.parsePersonalBests(stored).get(TermSimPolicy.Kind.PANES));
        assertTrue(DungeonLeftoverPolicy.hubSlotName(TermSimPolicy.Kind.PANES, pbs).contains("2.10s"));
    }
}
