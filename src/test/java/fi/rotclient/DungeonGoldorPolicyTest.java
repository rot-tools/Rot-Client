package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonGoldorPolicyTest {
    @Test
    void sharpShooterGridAndAdjacentPairsMatchTempleDevice() {
        assertEquals(9, DungeonGoldorPolicy.DEVICE_BLOCKS.size());
        assertEquals(new EmberDungeonPolicy.IntVec(68, 130, 50), DungeonGoldorPolicy.DEVICE_BLOCKS.getFirst());
        assertEquals(new EmberDungeonPolicy.IntVec(64, 126, 50), DungeonGoldorPolicy.DEVICE_BLOCKS.getLast());
        assertEquals(6, DungeonGoldorPolicy.adjacentPairs().size());
        assertTrue(DungeonGoldorPolicy.isDeviceBlock(66, 128, 50));
        assertFalse(DungeonGoldorPolicy.isDeviceBlock(65, 128, 50));
        assertTrue(DungeonGoldorPolicy.inSharpRoom(68.5D, 128.0D, 50.5D));
        assertFalse(DungeonGoldorPolicy.inSharpRoom(0.0D, 64.0D, 0.0D));
        assertTrue(DungeonGoldorPolicy.isEmerald("minecraft:emerald_block"));
        assertTrue(DungeonGoldorPolicy.isBlueTerracotta("blue_terracotta"));
        assertFalse(DungeonGoldorPolicy.isBlueTerracotta("light_blue_terracotta"));
        assertTrue(DungeonGoldorPolicy.isActiveArmorStand("Active"));
        assertTrue(DungeonGoldorPolicy.localCompletedDevice("Henri completed a device! (1/7)", "Henri"));
        assertFalse(DungeonGoldorPolicy.localCompletedDevice("Henri completed a terminal! (1/7)", "Henri"));
        assertFalse(DungeonGoldorPolicy.localCompletedDevice("Other completed a device! (1/7)", "Henri"));
    }

    @Test
    void emeraldToTerracottaMarksAndTerracottaToEmeraldRetargets() {
        EmberDungeonPolicy.IntVec a = new EmberDungeonPolicy.IntVec(66, 130, 50);
        EmberDungeonPolicy.IntVec b = new EmberDungeonPolicy.IntVec(68, 130, 50);
        DungeonGoldorPolicy.ShooterState targeted = DungeonGoldorPolicy.observeBlock(
                DungeonGoldorPolicy.ShooterState.idle(),
                a.x(), a.y(), a.z(),
                "blue_terracotta",
                "emerald_block");
        assertEquals(a, targeted.target());
        assertTrue(targeted.marked().isEmpty());
        assertFalse(targeted.aims().isEmpty());
        DungeonGoldorPolicy.Aim green = targeted.aims().getFirst();
        assertEquals(67.5D, green.position().x(), 0.001D);
        assertEquals(130.5D, green.position().y(), 0.001D);
        assertEquals(50.5D, green.position().z(), 0.001D);
        assertTrue(green.covered().contains(a));
        assertTrue(green.covered().contains(b));

        DungeonGoldorPolicy.ShooterState marked = DungeonGoldorPolicy.observeBlock(
                targeted,
                a.x(), a.y(), a.z(),
                "emerald_block",
                "blue_terracotta");
        assertTrue(marked.marked().contains(a));
        assertEquals(null, marked.target());

        DungeonGoldorPolicy.ShooterState next = DungeonGoldorPolicy.observeBlock(
                marked,
                64, 130, 50,
                "blue_terracotta",
                "emerald_block");
        assertEquals(new EmberDungeonPolicy.IntVec(64, 130, 50), next.target());
        assertTrue(next.marked().contains(a));
        assertFalse(next.marked().contains(next.target()));
        assertEquals(3, next.aims().size());
        EmberDungeonPolicy.Aabb box = DungeonGoldorPolicy.aimBox(green);
        assertEquals(67.0D, box.minX(), 0.001D);
        assertEquals(50.4D, box.minZ(), 0.001D);
        assertEquals(51.4D, box.maxZ(), 0.001D);
    }

    @Test
    void worldPollMarksThePreviousEmeraldWhenItTurnsTerracotta() {
        EmberDungeonPolicy.IntVec target = new EmberDungeonPolicy.IntVec(66, 128, 50);
        DungeonGoldorPolicy.ShooterState current = DungeonGoldorPolicy.observeBlock(
                DungeonGoldorPolicy.ShooterState.idle(),
                target.x(), target.y(), target.z(),
                "blue_terracotta",
                "emerald_block");
        DungeonGoldorPolicy.ShooterState polled = DungeonGoldorPolicy.observeWorld(current, pos -> {
            if (pos.equals(new EmberDungeonPolicy.IntVec(64, 128, 50))) {
                return "emerald_block";
            }
            return "blue_terracotta";
        });
        assertTrue(polled.marked().contains(target));
        assertEquals(new EmberDungeonPolicy.IntVec(64, 128, 50), polled.target());
    }

    @Test
    void terminalSplitsFollowGoldorChatAndGateThenCore() {
        DungeonGoldorPolicy.TermTimesState idle = DungeonGoldorPolicy.TermTimesState.idle();
        DungeonGoldorPolicy.TermTimesState storm = DungeonGoldorPolicy.applyChat(
                idle, "[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!", 1_000L);
        assertTrue(storm.stormPhase());
        assertFalse(storm.stormDead());
        assertEquals(0, storm.goldorSection());
        DungeonGoldorPolicy.TermTimesState dead = DungeonGoldorPolicy.applyChat(
                storm, "[BOSS] Storm: I should have known that I stood no chance.", 1_500L);
        assertTrue(dead.stormDead());
        assertTrue(dead.stormPhase());

        DungeonGoldorPolicy.TermTimesState goldor = DungeonGoldorPolicy.applyChat(
                dead, "[BOSS] Goldor: Who dares trespass into my domain?", 2_000L);
        assertTrue(goldor.goldorPhase());
        assertEquals(1, goldor.goldorSection());
        assertEquals(2_000L, goldor.sectionStartMs());
        assertEquals(2_000L, goldor.phaseStartMs());

        DungeonGoldorPolicy.TermTimesState mid = DungeonGoldorPolicy.applyChat(
                goldor, "Henri completed a terminal! (3/7)", 5_000L);
        assertEquals(3, mid.completed());
        assertTrue(mid.lastLine().contains("3/7"));
        assertTrue(mid.lastLine().contains("3.0s"));

        DungeonGoldorPolicy.TermTimesState full = DungeonGoldorPolicy.applyChat(
                mid, "Henri completed a device! (7/7)", 8_000L);
        assertEquals(7, full.completed());
        assertTrue(full.sectionTimes().isEmpty());

        DungeonGoldorPolicy.TermTimesState gated = DungeonGoldorPolicy.applyChat(
                full, "The gate has been destroyed!", 9_000L);
        assertEquals(1, gated.sectionTimes().size());
        assertEquals(2, gated.goldorSection());
        assertEquals(0, gated.completed());

        DungeonGoldorPolicy.TermTimesState core = DungeonGoldorPolicy.applyChat(
                gated, "The Core entrance is opening!", 12_000L);
        assertEquals(5, core.goldorSection());
        assertEquals(2, core.sectionTimes().size());
        assertTrue(core.totalLine().startsWith("Times:"));
        assertFalse(DungeonGoldorPolicy.termHudLines(core, Map.of(), 12_000L).isEmpty());

        Map<String, Long> pbs = DungeonGoldorPolicy.recordSectionPbs(full, gated, new LinkedHashMap<>());
        assertTrue(pbs.containsKey("1ST"));
        assertTrue(DungeonGoldorPolicy.recordTerminalTypePb(
                pbs, DungeonPolicy.Terminal.NUMBERS, 1_000L, 2_250L));
        assertEquals(1_250L, pbs.get("NUMBERS"));
    }

    @Test
    void positionCalloutsFireOnceInsideCopiedBoxes() {
        DungeonGoldorPolicy.PositionTracker tracker = new DungeonGoldorPolicy.PositionTracker();
        assertTrue(tracker.tick(108.5D, 120.5D, 94.0D, 0, true, false).isEmpty());
        tracker.enterP2();
        assertEquals("At SS!", tracker.tick(108.5D, 120.5D, 94.0D, 0, true, false).orElseThrow());
        assertTrue(tracker.tick(108.5D, 120.5D, 94.0D, 0, true, false).isEmpty());

        tracker.enterTerminals();
        assertEquals("At EE2!", tracker.tick(58.0D, 109.5D, 131.0D, 1, false, true).orElseThrow());
        assertTrue(tracker.tick(108.5D, 120.5D, 94.0D, 1, false, true).isEmpty());
        tracker.noteChat("Party > Other: At EE3!");
        assertTrue(tracker.tick(2.0D, 109.5D, 104.0D, 2, false, true).isEmpty());
    }

    @Test
    void ledgeUsesCopiedYellowPadAndClassFilter() {
        assertEquals(10.296D, DungeonGoldorPolicy.ledgeDistance(44.0D), 0.001D);
        assertTrue(DungeonGoldorPolicy.inYellowPad(40.0D, 170.0D, 50.0D));
        assertTrue(DungeonGoldorPolicy.showLedge(
                true, true, true, true, false, false, false, DungeonPolicy.DungeonClass.ARCHER));
        assertFalse(DungeonGoldorPolicy.showLedge(
                true, true, true, true, false, false, false, DungeonPolicy.DungeonClass.HEALER));
        assertTrue(DungeonGoldorPolicy.showLedge(
                true, true, true, true, false, false, true, DungeonPolicy.DungeonClass.MAGE));
        assertFalse(DungeonGoldorPolicy.showLedge(
                true, true, true, true, true, true, false, DungeonPolicy.DungeonClass.ARCHER));
        assertEquals("Ledge 10.3", DungeonGoldorPolicy.ledgeHudLine(10.296D));
    }

    @Test
    void greedyAimsPreferNewUnmarkedThenCloser() {
        EmberDungeonPolicy.IntVec target = new EmberDungeonPolicy.IntVec(66, 130, 50);
        List<DungeonGoldorPolicy.Aim> aims = DungeonGoldorPolicy.calculateOptimalAimPositions(target, Set.of());
        assertEquals(3, aims.size());
        assertEquals(0.0D, aims.getFirst().distance(), 0.001D);
        assertTrue(aims.get(1).distance() > 0.0D);
        assertEquals(2, aims.getFirst().covered().size());
    }
}
