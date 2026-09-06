package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class DungeonAthenPortPolicyTest {
    @Test
    void extraSuperboomWallsAndDelayRange() {
        assertTrue(DungeonAthenPortPolicy.isSuperboomWall("minecraft:cracked_stone_bricks", ""));
        assertFalse(DungeonAthenPortPolicy.isSuperboomWall("minecraft:obsidian", ""));
        String csv = DungeonAthenPortPolicy.addExtraBlock("", "minecraft:obsidian");
        assertTrue(DungeonAthenPortPolicy.isSuperboomWall("obsidian", csv));
        csv = DungeonAthenPortPolicy.removeExtraBlock(csv, "obsidian");
        assertFalse(DungeonAthenPortPolicy.isSuperboomWall("obsidian", csv));
        assertEquals(1, DungeonAthenPortPolicy.clampSuperboomDelay(0));
        assertEquals(5, DungeonAthenPortPolicy.clampSuperboomDelay(99));
        int delay = DungeonAthenPortPolicy.randomBetween(2, 2);
        assertEquals(2, delay);
    }

    @Test
    void termOrderEnumAndHumanOrderCompat() {
        List<DungeonPolicy.TerminalClick> clicks = List.of(
                new DungeonPolicy.TerminalClick(0, 0),
                new DungeonPolicy.TerminalClick(22, 0),
                new DungeonPolicy.TerminalClick(53, 0));
        List<DungeonPolicy.TerminalClick> first =
                DungeonAthenPortPolicy.orderClicks(clicks, 22, "First", false);
        assertEquals(0, first.getFirst().slot());
        List<DungeonPolicy.TerminalClick> closestCompat =
                DungeonAthenPortPolicy.orderClicks(clicks, 22, "First", true);
        assertEquals(22, closestCompat.getFirst().slot());
        List<DungeonPolicy.TerminalClick> furthest =
                DungeonAthenPortPolicy.orderClicks(clicks, 22, "Furthest", false);
        assertTrue(furthest.getFirst().slot() == 0 || furthest.getFirst().slot() == 53);
        assertEquals("Closest", DungeonAthenPortPolicy.normalizeTermOrder("closest"));
    }

    @Test
    void queueResyncAndChestCloseDelay() {
        assertFalse(DungeonAthenPortPolicy.queueNeedsResync(1000L, 1400L, 800));
        assertTrue(DungeonAthenPortPolicy.queueNeedsResync(1000L, 1900L, 800));
        assertEquals(800, DungeonAthenPortPolicy.clampResyncMs(800));
        assertEquals(400, DungeonAthenPortPolicy.clampResyncMs(10));
        int ticks = DungeonAthenPortPolicy.chestCloseDelayTicks(0, 1);
        assertTrue(ticks >= 0 && ticks <= 1);
        assertEquals(5, DungeonAthenPortPolicy.clampChestDelay(9));
    }

    @Test
    void qualityStyleTokensAndLegacyBlank() {
        assertTrue(DungeonAthenPortPolicy.formatQuality("", 50, 50, 7).contains("Quality:"));
        String styled = DungeonAthenPortPolicy.formatQuality("&7Item Quality: &c#cur&8/&c#max #floor", 12, 50, 7);
        assertTrue(styled.contains("12"));
        assertTrue(styled.contains("50"));
        assertTrue(styled.contains("F7"));
        assertTrue(styled.contains("§7"));
        assertEquals("§cVroom!", DungeonAthenPortPolicy.applyMcCodes("<red>Vroom!"));
    }

    @Test
    void partyFinderLoreStatusesAndJoinKick() {
        assertEquals(
                DungeonPartyFinderPolicy.SlotStatus.CARRY,
                DungeonPartyFinderPolicy.loreStatus(List.of("Need a carry")));
        assertEquals(
                DungeonPartyFinderPolicy.SlotStatus.VC,
                DungeonPartyFinderPolicy.loreStatus(List.of("Must VC")));
        assertEquals(
                DungeonPartyFinderPolicy.SlotStatus.JOINABLE,
                DungeonPartyFinderPolicy.loreStatus(List.of("Cata 50", "Note: S+")));
        DungeonPartyFinderPolicy.KickThresholds thresholds =
                new DungeonPartyFinderPolicy.KickThresholds("5:30", "50k", "8.4", "800");
        DungeonPartyFinderPolicy.Stats low =
                new DungeonPartyFinderPolicy.Stats(10_000L, 4.0D, 200, 400);
        DungeonPartyFinderPolicy.Stats ok =
                new DungeonPartyFinderPolicy.Stats(80_000L, 9.0D, 1200, 300);
        assertTrue(DungeonPartyFinderPolicy.shouldKick(low, thresholds));
        assertFalse(DungeonPartyFinderPolicy.shouldKick(ok, thresholds));
        assertEquals("/p kick Steve", DungeonPartyFinderPolicy.partyKickCommand("Steve"));
        assertEquals("Steve", DungeonPartyFinderPolicy.joinPlayer(
                "Party Finder > Steve joined the dungeon group!").orElse(""));
    }

    @Test
    void watcherSpeedBuckets() {
        assertEquals(DungeonWatcherPolicy.Speed.FAST, DungeonWatcherPolicy.speedForSpeak(3_000L));
        assertEquals(DungeonWatcherPolicy.Speed.NORMAL, DungeonWatcherPolicy.speedForSpeak(5_000L));
        assertEquals(DungeonWatcherPolicy.Speed.SLOW, DungeonWatcherPolicy.speedForSpeak(8_000L));
        assertEquals(DungeonWatcherPolicy.Speed.VERY_SLOW, DungeonWatcherPolicy.speedForSpeak(12_000L));
        assertTrue(DungeonWatcherPolicy.speakChat(
                "[BOSS] The Watcher: Let's see how you can handle this."));
        assertEquals("§cVroom!", DungeonWatcherPolicy.alertText(
                DungeonWatcherPolicy.Speed.FAST, "<red>Vroom!", "n", "s", "vs"));
    }

    @Test
    void waypointClassFilterAndSoulsandRule() {
        assertTrue(DungeonAthenPortPolicy.waypointVisible(
                false, DungeonPolicy.DungeonClass.TANK, DungeonPolicy.DungeonClass.MAGE));
        assertTrue(DungeonAthenPortPolicy.waypointVisible(
                true, DungeonPolicy.DungeonClass.UNKNOWN, DungeonPolicy.DungeonClass.MAGE));
        assertFalse(DungeonAthenPortPolicy.waypointVisible(
                true, DungeonPolicy.DungeonClass.TANK, DungeonPolicy.DungeonClass.MAGE));
        assertTrue(DungeonAthenPortPolicy.waypointVisible(
                true, DungeonPolicy.DungeonClass.TANK, DungeonPolicy.DungeonClass.TANK));
        assertEquals(25, EmberDungeonPolicy.waypointNodes().size());
        assertTrue(DungeonAthenPortPolicy.soulsandPlaceTarget(
                true, true, 105, "minecraft:stone_bricks", "soul_sand"));
        assertFalse(DungeonAthenPortPolicy.soulsandPlaceTarget(
                true, true, 104, "minecraft:stone_bricks", "soul_sand"));
    }

    @Test
    void hoverTermSlotPickSkipsMelody() {
        List<DungeonPolicy.TerminalClick> clicks = List.of(
                new DungeonPolicy.TerminalClick(10, 0),
                new DungeonPolicy.TerminalClick(16, 1));
        assertEquals(10, DungeonAthenPortPolicy.hoverClick(
                clicks, 10, DungeonPolicy.Terminal.PANES).orElseThrow().slot());
        assertTrue(DungeonAthenPortPolicy.hoverClick(
                clicks, 10, DungeonPolicy.Terminal.MELODY).isEmpty());
        assertTrue(DungeonAthenPortPolicy.hoverClick(
                clicks, 3, DungeonPolicy.Terminal.PANES).isEmpty());
    }

    @Test
    void pinglessPredictDropsConfirmedSlotsAndSkipsMelody() {
        List<DungeonPolicy.TerminalClick> live = List.of(
                new DungeonPolicy.TerminalClick(11, 0),
                new DungeonPolicy.TerminalClick(12, 0),
                new DungeonPolicy.TerminalClick(13, 0));
        assertTrue(DungeonAthenPortPolicy.usesPinglessPredict(DungeonPolicy.Terminal.PANES));
        assertTrue(DungeonAthenPortPolicy.usesPinglessPredict(DungeonPolicy.Terminal.NUMBERS));
        assertFalse(DungeonAthenPortPolicy.usesPinglessPredict(DungeonPolicy.Terminal.MELODY));
        assertFalse(DungeonAthenPortPolicy.usesPinglessPredict(DungeonPolicy.Terminal.RUBIX));
        List<Integer> predicted = DungeonAthenPortPolicy.withPredictedSlot(List.of(), 11);
        List<DungeonPolicy.TerminalClick> remaining =
                DungeonAthenPortPolicy.withoutPredictedSlots(live, predicted);
        assertEquals(List.of(12, 13), remaining.stream().map(DungeonPolicy.TerminalClick::slot).toList());
        List<DungeonPolicy.TerminalClick> afterServer = List.of(
                new DungeonPolicy.TerminalClick(12, 0),
                new DungeonPolicy.TerminalClick(13, 0));
        assertTrue(DungeonAthenPortPolicy.keepPredictedSlots(afterServer, predicted).isEmpty());
        List<Integer> two = DungeonAthenPortPolicy.withPredictedSlot(predicted, 12);
        assertEquals(List.of(12), DungeonAthenPortPolicy.keepPredictedSlots(afterServer, two));
    }

    @Test
    void skyCryptUrlNeverUsesStarredFoo() {
        assertEquals(
                "https://sky.shiiyu.moe/api/v2/profile/Steve",
                DungeonProfileStatsService.urlFor("Steve"));
        assertFalse(DungeonProfileStatsService.PROFILE_URL.contains("starred.foo"));
        OptionalStats parsed = OptionalStats.of(DungeonProfileStatsService.parse(
                "{\"profiles\":{\"p\":{\"current\":true,\"secrets_found\":50000,"
                        + "\"secret_average\":8.4,\"magical_power\":900,\"fastest_time\":300}}}",
                "F7"));
        assertTrue(parsed.present);
        assertEquals(50_000L, parsed.secrets);
        assertEquals(8.4D, parsed.average, 0.0001);
        assertEquals(900, parsed.mp);
        assertEquals(300, parsed.pb);
    }

    @Test
    void breakerInstamineSkipsSecrets() {
        assertTrue(DungeonAthenPortPolicy.shouldInstamineBreaker(
                true, true, true, true, 3, "minecraft:stone"));
        assertFalse(DungeonAthenPortPolicy.shouldInstamineBreaker(
                true, true, true, true, 3, "minecraft:chest"));
        assertFalse(DungeonAthenPortPolicy.shouldInstamineBreaker(
                true, false, true, true, 3, "minecraft:stone"));
    }

    private record OptionalStats(
            boolean present, long secrets, double average, int mp, int pb) {
        static OptionalStats of(java.util.Optional<DungeonPartyFinderPolicy.Stats> stats) {
            if (stats.isEmpty()) {
                return new OptionalStats(false, 0L, 0.0D, 0, 0);
            }
            DungeonPartyFinderPolicy.Stats value = stats.get();
            return new OptionalStats(true, value.secrets(), value.secretAverage(),
                    value.magicalPower(), value.pbSeconds());
        }
    }
}
