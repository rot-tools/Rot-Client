package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class InventoryWalkPolicyTest {
    @Test
    void appliesOnlyWithOpenNonPauseGuiAndNoTextFocus() {
        assertTrue(InventoryWalkPolicy.shouldApplyMovement(
                true, true, false, false, false, 1_000L, 900L, 0L, 200));
        assertFalse(InventoryWalkPolicy.shouldApplyMovement(
                true, false, false, false, false, 1_000L, 900L, 0L, 200));
        assertFalse(InventoryWalkPolicy.shouldApplyMovement(
                true, true, true, false, false, 1_000L, 900L, 0L, 200));
        assertFalse(InventoryWalkPolicy.shouldApplyMovement(
                true, true, false, true, false, 1_000L, 900L, 0L, 200));
    }

    @Test
    void clickFreezeUsesKeepaliveAnd500MsResume() {
        assertFalse(InventoryWalkPolicy.shouldApplyMovement(
                true, true, false, false, true, 1_100L, 1_000L, 1_000L, 200));
        assertTrue(InventoryWalkPolicy.shouldApplyMovement(
                true, true, false, false, true, 1_600L, 1_560L, 1_000L, 200));
        assertTrue(InventoryWalkPolicy.shouldApplyMovement(
                true, true, false, false, false, 1_050L, 1_000L, 1_000L, 200));
        assertFalse(InventoryWalkPolicy.shouldApplyMovement(
                true, true, false, false, false, 1_300L, 1_000L, 1_000L, 200));
    }

    @Test
    void pingJitterPreservesAverageAndVaries() {
        assertEquals(200, InventoryWalkPolicy.jitteredPingMs(200, 0.5D));
        assertTrue(InventoryWalkPolicy.jitteredPingMs(200, 0.0D) < 200);
        assertTrue(InventoryWalkPolicy.jitteredPingMs(200, 1.0D) > 200);
        assertEquals(160, InventoryWalkPolicy.jitteredPingMs(200, 0.0D));
        assertEquals(240, InventoryWalkPolicy.jitteredPingMs(200, 1.0D));
    }

    @Test
    void everyClickFreezesWalkAndSlotAckDoesNotResume() {
        assertTrue(InventoryWalkPolicy.shouldReleaseKeysOnClick(false));
        assertFalse(InventoryWalkPolicy.shouldReleaseKeysOnClick(true));
        assertFalse(InventoryWalkPolicy.shouldAcknowledgeClickFromSlotUpdate());
        assertTrue(InventoryWalkPolicy.shouldAllowContainerClick(false));
        assertFalse(InventoryWalkPolicy.shouldAllowContainerClick(true));
        assertFalse(InventoryWalkPolicy.shouldFreezeWalkOnClick(false));
        assertTrue(InventoryWalkPolicy.shouldFreezeWalkOnClick(true));
        assertTrue(InventoryWalkPolicy.shouldApplyMouseLook(
                true, true, false, false, true));
        assertFalse(InventoryWalkPolicy.shouldApplyMouseLook(
                true, true, false, false, false));
    }
}

final class TrajectoryPredictorTest {
    @Test
    void pearlFallsAndStopsOnSolid() {
        TrajectoryPredictor.Occupancy solidAbove = (x, y, z) -> y <= 0;
        TrajectoryPredictor.Result result = TrajectoryPredictor.simulate(
                TrajectoryPredictor.ProjectileKind.PEARL,
                new TrajectoryPredictor.Vec3d(0.5D, 5.0D, 0.5D),
                0.0F,
                90.0F,
                1.0F,
                40,
                solidAbove);
        assertFalse(result.points().isEmpty());
        assertTrue(result.hit().isPresent());
        assertTrue(result.hit().get().blockY() <= 1);
    }

    @Test
    void detectHeldRecognizesBowAndPearl() {
        assertEquals(
                TrajectoryPredictor.ProjectileKind.PEARL,
                TrajectoryPredictor.detectHeld(true, true, "Ender Pearl").orElseThrow());
        assertEquals(
                TrajectoryPredictor.ProjectileKind.BOW,
                TrajectoryPredictor.detectHeld(true, true, "Terminator").orElseThrow());
        assertTrue(TrajectoryPredictor.detectHeld(false, true, "Bow").isEmpty());
    }

    @Test
    void terminatorUsesTripleYawOffsets() {
        assertTrue(TrajectoryPredictor.isTerminatorId("TERMINATOR"));
        assertFalse(TrajectoryPredictor.isTerminatorId("terminator"));
        assertEquals(-5.0F, TrajectoryPredictor.terminatorYawOffsets()[0]);
        assertEquals(0.0F, TrajectoryPredictor.terminatorYawOffsets()[1]);
        assertEquals(5.0F, TrajectoryPredictor.terminatorYawOffsets()[2]);
    }

    @Test
    void lerpTracksRenderPartialTick() {
        assertEquals(0.5F, TrajectoryPredictor.lerp(0.0F, 1.0F, 0.5F), 0.0001F);
        TrajectoryPredictor.Vec3d mid = TrajectoryPredictor.lerp(
                new TrajectoryPredictor.Vec3d(0.0D, 0.0D, 0.0D),
                new TrajectoryPredictor.Vec3d(10.0D, 20.0D, 30.0D),
                0.5F);
        assertEquals(5.0D, mid.x(), 0.0001D);
        assertEquals(10.0D, mid.y(), 0.0001D);
        assertEquals(15.0D, mid.z(), 0.0001D);
        assertEquals(1.0F, TrajectoryPredictor.bowPull(20.0F), 0.0001F);
    }
}

final class SecretHitboxesPolicyTest {
    @Test
    void dungeonGateDefaultsOnAndFullCubeWhenEnabled() {
        assertEquals(
                SecretHitboxesPolicy.ShapeId.NONE,
                SecretHitboxesPolicy.resolve(
                        true, true, false,
                        SecretHitboxesPolicy.BlockKind.LEVER,
                        true, false, false, false, false, false, false,
                        SecretHitboxesPolicy.AttachFace.WALL,
                        SecretHitboxesPolicy.Cardinal.NORTH,
                        false));
        assertEquals(
                SecretHitboxesPolicy.ShapeId.FULL_CUBE,
                SecretHitboxesPolicy.resolve(
                        true, true, true,
                        SecretHitboxesPolicy.BlockKind.SKULL,
                        false, false, false, false, true, false, false,
                        SecretHitboxesPolicy.AttachFace.WALL,
                        SecretHitboxesPolicy.Cardinal.NORTH,
                        false));
    }

    @Test
    void trappedChestHonorsOnlyTrapped() {
        assertEquals(
                SecretHitboxesPolicy.ShapeId.NONE,
                SecretHitboxesPolicy.resolve(
                        true, false, false,
                        SecretHitboxesPolicy.BlockKind.CHEST,
                        false, false, false, false, false, true, true,
                        SecretHitboxesPolicy.AttachFace.WALL,
                        SecretHitboxesPolicy.Cardinal.NORTH,
                        false));
        assertEquals(
                SecretHitboxesPolicy.ShapeId.FULL_CUBE,
                SecretHitboxesPolicy.resolve(
                        true, false, false,
                        SecretHitboxesPolicy.BlockKind.TRAPPED_CHEST,
                        false, false, false, false, false, true, true,
                        SecretHitboxesPolicy.AttachFace.WALL,
                        SecretHitboxesPolicy.Cardinal.NORTH,
                        false));
    }

    @Test
    void oldLeverUsesDirectionalShapes() {
        assertEquals(
                SecretHitboxesPolicy.ShapeId.LEVER_EAST,
                SecretHitboxesPolicy.oldLeverShape(
                        SecretHitboxesPolicy.AttachFace.WALL,
                        SecretHitboxesPolicy.Cardinal.EAST));
    }
}

final class WorldScannerPolicyTest {
    @Test
    void quartersMatchCrystalHollowsGates() {
        assertTrue(WorldScannerPolicy.inQuarter(
                WorldScannerPolicy.Quarter.NUCLEUS, 500, 70, 500));
        assertFalse(WorldScannerPolicy.inQuarter(
                WorldScannerPolicy.Quarter.NUCLEUS, 100, 70, 100));
        assertTrue(WorldScannerPolicy.inQuarter(
                WorldScannerPolicy.Quarter.MAGMA, 0, 10, 0));
        assertFalse(WorldScannerPolicy.inQuarter(
                WorldScannerPolicy.Quarter.MAGMA, 0, 90, 0));
    }

    @Test
    void kingSequenceMatchesGoldThenDiorite() {
        WorldScannerPolicy.StructureDef king = WorldScannerPolicy.crystalStructures().getFirst();
        assertEquals("Goblin King", king.name());
        assertTrue(WorldScannerPolicy.inQuarter(
                WorldScannerPolicy.Quarter.GOBLIN, 200, 70, 800));
        assertTrue(WorldScannerPolicy.match(
                king,
                200, 70, 800,
                List.of("gold_block", "polished_diorite", "polished_diorite", "polished_diorite"))
                .isPresent());
        assertTrue(WorldScannerPolicy.match(
                king,
                200, 70, 200,
                List.of("gold_block", "polished_diorite", "polished_diorite", "polished_diorite"))
                .isEmpty());
    }

    @Test
    void fairyGrottoSkipsNucleus() {
        assertTrue(WorldScannerPolicy.isFairyGrottoBlock("minecraft:amethyst_block"));
        assertTrue(WorldScannerPolicy.inQuarter(
                WorldScannerPolicy.Quarter.NUCLEUS, 500, 40, 500));
    }

    @Test
    void rescansLoadedChunksWhenEnabledOrEnteringHollows() {
        assertTrue(WorldScannerPolicy.shouldRescanLoadedChunks(
                true, false, true, true, true));
        assertTrue(WorldScannerPolicy.shouldRescanLoadedChunks(
                true, true, true, true, false));
        assertFalse(WorldScannerPolicy.shouldRescanLoadedChunks(
                true, true, true, true, true));
        assertFalse(WorldScannerPolicy.shouldRescanLoadedChunks(
                false, false, true, true, true));
        assertEquals("Mines of Divan", WorldScannerPolicy.crystalStructures().get(2).name());
        assertTrue(WorldScannerPolicy.triggerBlocks(
                WorldScannerPolicy.crystalStructures(), true, false, false)
                .contains("quartz_block"));
        assertTrue(WorldScannerPolicy.secondBlockMatches(
                "netherrack",
                List.of("lava", "netherrack")));
        assertFalse(WorldScannerPolicy.secondBlockMatches(
                "air",
                List.of("lava", "netherrack")));
        assertEquals(32, WorldScannerPolicy.DEFAULT_ESP_RANGE);
    }

    @Test
    void espLabelIncludesNameAndDistance() {
        assertEquals(
                "Fairy Grotto  47m",
                WorldScannerPolicy.formatEspLabel("Fairy Grotto", 47.4D));
        assertEquals("Waypoint  0m", WorldScannerPolicy.formatEspLabel("  ", -3.0D));
        assertEquals(2.4F, WorldScannerPolicy.espLabelScale(1.0F), 0.01F);
        assertEquals(3.0F, WorldScannerPolicy.espLabelScale(3.0F), 0.01F);
    }
}
