package fi.rotclient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ForagingPolicyTest {
    @Test
    void islandsMatchSkyblockModeIdsAndDisplayNames() {
        assertEquals(ForagingPolicy.Island.GALATEA, ForagingPolicy.islandFromArea("§aGalatea"));
        assertEquals(ForagingPolicy.Island.GALATEA, ForagingPolicy.islandFromArea("Moonglade Marsh"));
        assertEquals(ForagingPolicy.Island.GALATEA, ForagingPolicy.islandFromArea("foraging_2"));
        assertEquals(ForagingPolicy.Island.TORRHUS, ForagingPolicy.islandFromArea("Torrhus Canyon"));
        assertEquals(ForagingPolicy.Island.PARK, ForagingPolicy.islandFromArea("The Park"));
        assertEquals(ForagingPolicy.Island.PARK, ForagingPolicy.islandFromArea("Birch Park"));
        assertEquals(ForagingPolicy.Island.HUB, ForagingPolicy.islandFromArea("Hub"));
        assertEquals(ForagingPolicy.Island.HUB, ForagingPolicy.islandFromArea("The Hub"));
        assertEquals(ForagingPolicy.Island.GALATEA, ForagingPolicy.islandFromArea("Forest Temple"));
        assertEquals(ForagingPolicy.Island.TORRHUS, ForagingPolicy.islandFromArea("Desert Temple"));
        assertTrue(ForagingPolicy.customTrees(ForagingPolicy.Island.GALATEA));
        assertFalse(ForagingPolicy.customTrees(ForagingPolicy.Island.PARK));
        assertTrue(ForagingPolicy.chopTrees(ForagingPolicy.Island.HUB));
        assertTrue(ForagingPolicy.chopTrees(ForagingPolicy.Island.PARK));
        assertFalse(ForagingPolicy.chopTrees(ForagingPolicy.Island.NONE));
    }

    @Test
    void treeBitsHideOnlyCustomTreeBlockDisplays() {
        assertTrue(ForagingPolicy.shouldHideTreeBits(
                true, ForagingPolicy.Island.GALATEA, true, "minecraft:mangrove_wood"));
        assertFalse(ForagingPolicy.shouldHideTreeBits(
                true, ForagingPolicy.Island.PARK, true, "minecraft:mangrove_wood"));
        assertFalse(ForagingPolicy.shouldHideTreeBits(
                true, ForagingPolicy.Island.GALATEA, true, "minecraft:stone"));
        assertFalse(ForagingPolicy.shouldHideTreeBits(
                true, ForagingPolicy.Island.GALATEA, false, "minecraft:mangrove_wood"));
    }

    @Test
    void treeProgressParsesNorthNametag() {
        ForagingPolicy.TreeProgress progress = ForagingPolicy.parseTreeProgress("§aFIG TREE 42%");
        assertNotNull(progress);
        assertEquals(ForagingPolicy.TreeType.FIG, progress.type());
        assertEquals(42, progress.percent());
        assertEquals("FIG 42%", ForagingPolicy.compactProgressLine(progress));
        assertTrue(ForagingPolicy.showTreeProgress(
                true, ForagingPolicy.Island.GALATEA, true, true, progress));
        assertFalse(ForagingPolicy.showTreeProgress(
                true, ForagingPolicy.Island.GALATEA, true, false, progress));
        assertNull(ForagingPolicy.parseTreeProgress("Zombie"));
    }

    @Test
    void treeGiftAndSweepChatMatchNorth() {
        assertTrue(ForagingPolicy.isTreeGiftHeader("§2§lTREE GIFT"));
        ForagingPolicy.GiftContribution gift =
                ForagingPolicy.parseGiftContribution("§aYou helped cut 12.5% of the §2Mangrove Tree§a.");
        assertNotNull(gift);
        assertEquals(ForagingPolicy.TreeType.MANGROVE, gift.type());
        assertEquals(12.5D, gift.percent(), 0.0001D);
        assertTrue(ForagingPolicy.isSweepDetailsHeader("§eSweep Details: §a120 Sweep"));
        assertTrue(ForagingPolicy.hideUnmineableChat(
                true,
                ForagingPolicy.Island.GALATEA,
                "§cYou cannot damage a tree while it is regenerating!"));
        assertFalse(ForagingPolicy.hideUnmineableChat(true, ForagingPolicy.Island.PARK, "hello"));
    }

    @Test
    void audioGatesMatchExpectedAreas() {
        assertTrue(ForagingPolicy.mutePhantom(true, ForagingPolicy.Island.GALATEA, "minecraft:entity.phantom.ambient"));
        assertFalse(ForagingPolicy.mutePhantom(true, ForagingPolicy.Island.PARK, "minecraft:entity.phantom.ambient"));
        assertTrue(ForagingPolicy.muteTreeBreak(
                true, true, ForagingPolicy.Island.GALATEA, "entity.creaking.death"));
        assertFalse(ForagingPolicy.muteTreeBreak(
                true, false, ForagingPolicy.Island.GALATEA, "entity.creaking.death"));
        assertTrue(ForagingPolicy.muteFusionMachine(
                true, ForagingPolicy.Island.GALATEA, "entity.firework_rocket.blast", 20.0F));
        assertFalse(ForagingPolicy.muteFusionMachine(
                true, ForagingPolicy.Island.GALATEA, "entity.firework_rocket.blast", 1.0F));
    }

    @Test
    void beaconSolverUsesPlainPitchesAndGateCycleMath() {
        assertTrue(ForagingPolicy.isBeaconTuneTitle("Tune Frequency"));
        assertTrue(ForagingPolicy.isBeaconTuneTitle("Upgrade Signal Strength"));
        assertEquals(37, ForagingPolicy.beaconSlot("Upgrade Signal Strength", ForagingPolicy.BEACON_COLOR_SLOT));
        assertEquals(ForagingPolicy.BeaconPitch.LOW, ForagingPolicy.classifyPitch(0.0952381F));
        assertEquals(ForagingPolicy.BeaconPitch.NORMAL, ForagingPolicy.classifyPitch(0.7936508F));
        assertEquals(ForagingPolicy.BeaconPitch.HIGH, ForagingPolicy.parsePitchName("Current pitch: High"));
        assertTrue(ForagingPolicy.isBeaconReady(" Cooldown: AVAILABLE"));
        assertEquals(1, ForagingPolicy.shortestCycleClicks(0, 1, 13));
        assertEquals(-1, ForagingPolicy.shortestCycleClicks(0, 12, 13));
        assertTrue(ForagingPolicy.preventBeaconOverClick(true, 0));
        assertTrue(ForagingPolicy.shouldAutoClickBeacon(true, true, true, 3));
        assertFalse(ForagingPolicy.shouldAutoClickBeacon(true, false, true, 3));
        assertEquals(0, ForagingPolicy.remainingClicksAfterPress(1, 13, false));
    }

    @Test
    void sweepOverlayMatchesGateToughnessAndCap() {
        assertEquals(10.0F, ForagingPolicy.toughness("minecraft:stripped_spruce_wood"), 0.01F);
        assertEquals(50.0F, ForagingPolicy.toughness("mangrove_wood"), 0.01F);
        assertEquals(150.0F, ForagingPolicy.toughness("stripped_birch_log"), 0.01F);
        assertEquals(120, ForagingPolicy.parseTabSweep("Sweep: 120"));
        int wood = ForagingPolicy.maxWood(200.0F, 10.0F, false);
        assertTrue(wood > 0 && wood <= ForagingPolicy.MAX_SWEEP_WOOD);
        assertTrue(ForagingPolicy.maxWood(200.0F, 10.0F, true) <= wood);
        assertTrue(ForagingPolicy.isForagingAxe("FIG_AXE"));
        assertTrue(ForagingPolicy.isThrowableAxe("HELIX_CHOPPER"));
        assertFalse(ForagingPolicy.isThrowableAxe("ROOKIE_AXE"));
    }

    @Test
    void cheatChopAndTossStayOffUnlessExplicit() {
        assertFalse(ForagingPolicy.shouldAutoChop(
                true, false, ForagingPolicy.Island.GALATEA, true, true));
        assertTrue(ForagingPolicy.shouldAutoChop(
                true, true, ForagingPolicy.Island.GALATEA, true, true));
        assertTrue(ForagingPolicy.shouldAutoChop(
                true, true, ForagingPolicy.Island.PARK, true, true));
        assertTrue(ForagingPolicy.shouldAutoChop(
                true, true, ForagingPolicy.Island.HUB, true, true));
        assertTrue(ForagingPolicy.shouldAutoChop(
                true, true, ForagingPolicy.Island.TORRHUS, true, true));
        assertFalse(ForagingPolicy.shouldAutoChop(
                true, true, ForagingPolicy.Island.NONE, true, true));
        assertTrue(ForagingPolicy.shouldAxeToss(true, true, true, 8, 5, true));
        assertFalse(ForagingPolicy.shouldAxeToss(true, true, true, 3, 5, true));
    }

    @Test
    void huntingAndHotfTitles() {
        assertTrue(ForagingPolicy.isHotfTitle("Heart of the Forest"));
        assertTrue(ForagingPolicy.isHuntingBoxTitle("Hunting Box"));
        assertTrue(ForagingPolicy.isStarlynShop("Agatha's Shop"));
        assertTrue(ForagingPolicy.isInvisibugCrit("crit", 1, 0.0F));
        assertFalse(ForagingPolicy.isInvisibugCrit("crit", 8, 0.2F));
    }

    @Test
    void highlightsTemplesAndFelledChatMatchGate() {
        assertTrue(ForagingPolicy.highlightLushlilac(
                ForagingPolicy.Island.GALATEA, "minecraft:flowering_azalea"));
        assertTrue(ForagingPolicy.highlightSeaLumies(
                ForagingPolicy.Island.GALATEA, "sea_pickle", 3, 3));
        assertFalse(ForagingPolicy.highlightSeaLumies(
                ForagingPolicy.Island.GALATEA, "sea_pickle", 2, 3));
        assertTrue(ForagingPolicy.highlightVeilshroom(
                ForagingPolicy.Island.TORRHUS, "crimson_fungus"));
        assertTrue(ForagingPolicy.highlightHoneyhive(
                ForagingPolicy.Island.TORRHUS, "bee_nest", 5, false));
        assertFalse(ForagingPolicy.highlightHoneyhive(
                ForagingPolicy.Island.TORRHUS, "bee_nest", 5, true));
        assertTrue(ForagingPolicy.isQueenBeeChat("QUEEN BEE! The Honeyhive instantly refilled with loot!"));
        assertTrue(ForagingPolicy.isTreeFelledChat("TIMBER! You felled the entire Tree!"));
        assertEquals(
                ForagingPolicy.Cardinal.WEST,
                ForagingPolicy.forestTempleFloorFacing(ForagingPolicy.Cardinal.NORTH));
        assertEquals(0, ForagingPolicy.signedQuarterTurns(
                ForagingPolicy.Cardinal.WEST, ForagingPolicy.Cardinal.WEST));
        assertEquals(-1, ForagingPolicy.signedQuarterTurns(
                ForagingPolicy.Cardinal.WEST, ForagingPolicy.Cardinal.SOUTH));
        assertEquals(List.of("BLUE", "RED"), ForagingPolicy.desertTempleButtonOrder(Map.of("RED", 3, "BLUE", 1)));
        assertTrue(ForagingPolicy.muteStereoPants(
                true, ForagingPolicy.Island.GALATEA, true, "block.note_block.harp"));
        assertEquals(
                ForagingPolicy.Island.GALATEA,
                ForagingPolicy.islandFromTexts("Hub", "§bGalatea"));
        assertEquals(2, ForagingPolicy.forestTempleTurns(
                ForagingPolicy.Cardinal.NORTH, ForagingPolicy.Cardinal.EAST));
        assertEquals(
                new ForagingPolicy.BlockKey(-634, 59, 75),
                ForagingPolicy.forestTempleFloorBlock(3));
        assertTrue(ForagingPolicy.isForestTempleWall(-640, 65, 85));
    }

    @Test
    void beaconColorSpeedPitchClicksMatchGateTuner() {
        assertEquals(0, ForagingPolicy.colorClicks("magenta_dye", "magenta_stained_glass_pane"));
        assertEquals(1, ForagingPolicy.colorClicks("minecraft:magenta_dye", "light_blue_stained_glass_pane"));
        assertEquals(-1, ForagingPolicy.colorClicks("magenta_dye", "orange_stained_glass_pane"));
        assertEquals(3, ForagingPolicy.parseCurrentSpeed("Current speed: 3"));
        assertEquals(2, ForagingPolicy.speedFromMoveTicks(45));
        assertEquals(0, ForagingPolicy.speedFromMoveTicks(8));
        assertEquals(1, ForagingPolicy.speedClicks(1, 2));
        assertEquals(
                -1,
                ForagingPolicy.pitchClicks(ForagingPolicy.BeaconPitch.LOW, ForagingPolicy.BeaconPitch.HIGH));
        ForagingPolicy.BeaconHint hint = new ForagingPolicy.BeaconHint(2, 0, 0);
        ForagingPolicy.AutoBeaconClick click = ForagingPolicy.nextBeaconClick(
                true, true, "Tune Frequency", hint);
        assertNotNull(click);
        assertEquals(ForagingPolicy.BEACON_COLOR_SLOT, click.slot());
        assertTrue(click.rightClick());
        assertNull(ForagingPolicy.nextBeaconClick(true, false, "Tune Frequency", hint));
        assertEquals(
                1,
                ForagingPolicy.applyPress(hint, ForagingPolicy.BEACON_COLOR_SLOT, true, "Tune Frequency")
                        .colorClicks());
        assertEquals(0, ForagingPolicy.remainingAfterDirectedClick(1, 13, true));
        assertEquals(120, ForagingPolicy.parseSweepDetails("§eSweep Details: §a120 Sweep"));
        assertTrue(ForagingPolicy.shouldMuteSound(
                true, false, false, false, false,
                ForagingPolicy.Island.GALATEA, false,
                "minecraft:entity.phantom.ambient", 1.0F));
        assertEquals("Sweep 120 wood 8/12", ForagingPolicy.sweepHudLine(120, 8, 12));
    }

    @Test
    void sweepBfsCapsAtMaxWood() {
        Set<ForagingPolicy.BlockKey> logs = Set.of(
                new ForagingPolicy.BlockKey(0, 0, 0),
                new ForagingPolicy.BlockKey(1, 0, 0),
                new ForagingPolicy.BlockKey(2, 0, 0),
                new ForagingPolicy.BlockKey(10, 0, 0));
        assertEquals(2, ForagingPolicy.countSweepCluster(logs, new ForagingPolicy.BlockKey(0, 0, 0), 2));
        assertEquals(3, ForagingPolicy.countSweepCluster(logs, new ForagingPolicy.BlockKey(0, 0, 0), 35));
        assertEquals(1, ForagingPolicy.countSweepCluster(logs, new ForagingPolicy.BlockKey(10, 0, 0), 35));
    }

    @Test
    void giftTrackerIsHudOnlyNotALedger() {
        ForagingGiftTracker tracker = new ForagingGiftTracker();
        tracker.ingest("§2TREE GIFT");
        tracker.ingest("You helped cut 12.5% of the Fig Tree.");
        tracker.ingest("BONUS GIFT");
        tracker.ingest("+8 rewards gained!");
        assertEquals(1, tracker.giftsThisSession());
        assertEquals(ForagingPolicy.TreeType.FIG, tracker.lastType());
        assertEquals(8, tracker.lastRewards());
        assertTrue(tracker.hudLine().contains("FIG"));
        assertTrue(tracker.hudLine().contains("bonus"));
        tracker.resetSession();
        assertEquals("", tracker.hudLine());
    }

    @Test
    void leftoverJarParsersStillWork() {
        ForagingPolicy.StarlynBracket starlyn = ForagingPolicy.parseStarlynBracket(
                "§e[NPC] Agatha§f: §rYou reached the Gold Bracket in my contest!");
        assertNotNull(starlyn);
        assertEquals("Agatha", starlyn.sister());
        assertEquals("GOLD", starlyn.bracket());
        assertEquals(120.0D, ForagingPolicy.parseSweepAmount("§eSweep Details: §a120 Sweep"), 0.01D);
        assertEquals("Fig", ForagingPolicy.parseToughnessTree("Fig Tree Toughness: 10 Logs"));
        assertEquals(Integer.valueOf(40), ForagingPolicy.parseCouponCost("Cost: 40"));
        assertEquals(10, ForagingPolicy.couponProfit(50, 40));
        assertEquals(Integer.valueOf(1200), ForagingPolicy.parseForestWhispers("1,200 Forest Whispers"));
        assertEquals(100, ForagingPolicy.whispersForTenLevels(10));
        assertEquals("Lumber Jack", ForagingPolicy.tutorialByQuestName("Foraging Tutorial").npc());
        assertTrue(ForagingPolicy.hideonleafName("Hideonleaf"));
        assertTrue(ForagingPolicy.hideonsunName("Hideonsun"));
        assertTrue(ForagingPolicy.shellwiseName("Shellwise"));
        assertTrue(ForagingPolicy.coralotName("Coralot"));
        assertTrue(ForagingPolicy.blueJayName("Blue Jay"));
        assertTrue(ForagingPolicy.birriesName("Birries"));
        assertTrue(ForagingPolicy.cinderbatName("Cinderbat"));
        assertEquals(
                ForagingPolicy.HuntGlow.CINDERBAT,
                ForagingPolicy.huntGlow(ForagingPolicy.Island.NONE, "Cinderbat", "blaze"));
        assertTrue(ForagingPolicy.isHuntaxe("HUNTING_AXE", "Huntaxe"));
        assertEquals("Shiny Fish", ForagingPolicy.parseShardGain("You obtained Shiny Fish shards!"));
        assertEquals("Shards 3", ForagingPolicy.shardHudLine(3));
        assertTrue(ForagingPolicy.lassoAlert(true, true, true));
        assertFalse(ForagingPolicy.lassoAlert(true, false, true));
        assertEquals(
                ForagingPolicy.HuntGlow.HIDEONLEAF,
                ForagingPolicy.huntGlow(ForagingPolicy.Island.GALATEA, "", "entity.minecraft.shulker"));
        assertEquals(
                ForagingPolicy.HuntGlow.HIDEONSUN,
                ForagingPolicy.huntGlow(ForagingPolicy.Island.TORRHUS, "", "entity.minecraft.shulker"));
        assertEquals(
                ForagingPolicy.HuntGlow.BLUE_JAY,
                ForagingPolicy.huntGlow(ForagingPolicy.Island.TORRHUS, "", "entity.minecraft.parrot"));
        assertTrue(ForagingPolicy.isChopLog(ForagingPolicy.Island.GALATEA, "minecraft:stripped_spruce_wood"));
        assertFalse(ForagingPolicy.isChopLog(ForagingPolicy.Island.GALATEA, "minecraft:oak_log"));
        assertTrue(ForagingPolicy.isChopLog(ForagingPolicy.Island.HUB, "minecraft:oak_log"));
        assertTrue(ForagingPolicy.isChopLog(ForagingPolicy.Island.PARK, "minecraft:jungle_log"));
        assertTrue(ForagingPolicy.sameChopFamily(
                ForagingPolicy.Island.TORRHUS, "stripped_birch_log", "stripped_birch_wood"));
        assertFalse(ForagingPolicy.sameChopFamily(
                ForagingPolicy.Island.TORRHUS, "stripped_birch_log", "stripped_mangrove_wood"));
        assertEquals(
                ForagingPolicy.Island.GALATEA,
                ForagingPolicy.inferIslandFromLog(ForagingPolicy.Island.NONE, "minecraft:stripped_spruce_wood"));
        assertEquals(
                ForagingPolicy.Island.TORRHUS,
                ForagingPolicy.inferIslandFromLog(ForagingPolicy.Island.NONE, "minecraft:stripped_birch_wood"));
        assertEquals(
                ForagingPolicy.Island.HUB,
                ForagingPolicy.inferIslandFromLog(ForagingPolicy.Island.NONE, "minecraft:oak_log"));
        assertTrue(ForagingPolicy.isFrogMask("FROG_MASK", "Frog Mask"));
        assertTrue(ForagingPolicy.isLasso("HUNTING_LASSO", "Lasso"));
        assertTrue(ForagingPolicy.showFrogMaskHud(true, ForagingPolicy.Island.PARK, true));
        assertFalse(ForagingPolicy.showFrogMaskHud(true, ForagingPolicy.Island.GALATEA, true));
        assertTrue(ForagingPolicy.showLassoHud(true, ForagingPolicy.Island.TORRHUS, true));
        assertTrue(ForagingPolicy.isMoongladeBeacon(-688, 128, 65));
        assertTrue(ForagingPolicy.showTreeProgress(
                true, ForagingPolicy.Island.NONE, false, false,
                new ForagingPolicy.TreeProgress(ForagingPolicy.TreeType.HELIX, 10)));
        ForagingSettings off = ForagingSettings.disabled();
        assertFalse(off.any());
        assertFalse(off.autoChop);
    }

    @Test
    void foragingSettingsUseCatalogIds() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/fi/rotclient/ForagingSettings.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("qol.foraging_trees.progress_hud"));
        assertTrue(source.contains("qol.foraging_trees.gift_hud"));
        assertTrue(source.contains("qol.foraging_audio.mute_phantom"));
        assertTrue(source.contains("qol.foraging_helpers.beacon_hints"));
        assertFalse(source.contains("qol.foraging_trees.progress\""));
        assertFalse(source.contains("qol.foraging_helpers.beacon_alert"));
    }
}
