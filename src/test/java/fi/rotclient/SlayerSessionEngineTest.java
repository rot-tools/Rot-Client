package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerSessionEngineTest {
    @Test
    void viewSnapshotDoesNotStartAnIdleSession() {
        SlayerSessionEngine engine = new SlayerSessionEngine();

        assertEquals(0L, engine.viewSnapshot().sessionStartedAtMillis());
        assertTrue(engine.viewSnapshot().dropCounts().isEmpty());
    }

    @Test
    void ordinarySnapshotAlsoDoesNotStartAnIdleSession() {
        SlayerSessionEngine engine = new SlayerSessionEngine();

        assertEquals(0L, engine.snapshot(9_999L).sessionStartedAtMillis());
        assertTrue(engine.snapshot(9_999L).dropCounts().isEmpty());
    }

    @Test
    void oneEntityProducesOneSpawnAndOneOwnedKill() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 210M❤", "Owner: LocalPlayer").orElseThrow();

        assertTrue(engine.observeEntity(42, boss, "LocalPlayer", 2_000L).spawned());
        assertFalse(engine.observeEntity(42, boss, "LocalPlayer", 2_100L).spawned());
        SlayerSessionEngine.DeathResult result = engine.onEntityDeath(42, 8_000L);

        assertTrue(result.bossKilled());
        assertTrue(result.owned());
        assertEquals(1, engine.snapshot(8_000L).bossesKilled());
        assertEquals(6_000L, engine.snapshot(8_000L).lastKillDurationMillis());
        assertFalse(engine.onEntityDeath(42, 8_100L).bossKilled());
    }

    @Test
    void tarantulaTierFivePhaseOneDoesNotCountAsAKill() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        SlayerPolicy.EntityDescriptor phaseOne = SlayerPolicy.classifyTag(
                "☠ Tarantula Broodfather 10M❤", "Owner: LocalPlayer").orElseThrow();
        SlayerPolicy.EntityDescriptor phaseTwo = SlayerPolicy.classifyTag(
                "☠ Conjoined Brood 20M❤", "Owner: LocalPlayer").orElseThrow();

        assertTrue(engine.observeEntity(10, phaseOne, "LocalPlayer", 2_000L).spawned());
        SlayerSessionEngine.DeathResult first = engine.onEntityDeath(10, 8_000L);
        assertFalse(first.bossKilled());
        assertEquals(0, engine.snapshot(8_000L).bossesKilled());

        assertTrue(engine.observeEntity(11, phaseTwo, "LocalPlayer", 8_500L).spawned());
        SlayerSessionEngine.DeathResult second = engine.onEntityDeath(11, 16_000L);
        assertTrue(second.bossKilled());
        assertEquals(1, engine.snapshot(16_000L).bossesKilled());
    }

    @Test
    void otherPlayersBossDoesNotMutatePersonalStatsButCompletesMatchingCarry() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        assertTrue(engine.addCarry(
                "CarryPlayer", SlayerPolicy.SlayerType.INFERNO, 4, 2, 1_000L));
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Inferno Demonlord IV 150M❤", "Owner: CarryPlayer").orElseThrow();

        assertTrue(engine.observeEntity(70, boss, "LocalPlayer", 2_000L).spawned());
        SlayerSessionEngine.DeathResult first = engine.onEntityDeath(70, 10_000L);
        assertTrue(first.bossKilled());
        assertFalse(first.owned());
        assertEquals(0, engine.snapshot(10_000L).bossesKilled());
        assertEquals(1, engine.carries().getFirst().completed());
        assertTrue(first.carryAdvanced());
        assertEquals("CarryPlayer", first.completedCarryPlayer());

        engine.observeEntity(71, boss, "LocalPlayer", 12_000L);
        engine.onEntityDeath(71, 20_000L);
        assertEquals(2, engine.carries().getFirst().completed());
        assertTrue(engine.carries().getFirst().complete());
    }

    @Test
    void questCompletionCountsOwnedBossWhenTheEntityDeathEventArrivesLater() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Sven Packmaster IV 2.4M❤", "Owner: LocalPlayer").orElseThrow();
        engine.observeEntity(90, boss, "LocalPlayer", 2_000L);

        engine.onChat("SLAYER QUEST COMPLETE!", 7_000L);

        assertEquals(1, engine.snapshot(7_000L).bossesKilled());
        assertEquals(5_000L, engine.snapshot(7_000L).lastKillDurationMillis());
        assertFalse(engine.onEntityDeath(90, 7_100L).bossKilled());
    }

    @Test
    void dropsResetBossesSinceSelectedDropAndQuestFailureClearsActiveBossOnly() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Revenant Horror V 10M❤", "Owner: LocalPlayer").orElseThrow();
        engine.observeEntity(1, boss, "LocalPlayer", 1_000L);
        engine.onEntityDeath(1, 2_000L);
        assertEquals(1, engine.snapshot(2_000L).bossesSinceLastDrop());

        engine.onChat("CRAZY RARE DROP! (Warden Heart)", 2_100L);
        assertEquals(0, engine.snapshot(2_100L).bossesSinceLastDrop());
        assertEquals(1, engine.snapshot(2_100L).dropCounts().get("Warden Heart"));

        engine.observeEntity(2, boss, "LocalPlayer", 3_000L);
        engine.onChat("SLAYER QUEST FAILED!", 3_500L);
        assertTrue(engine.snapshot(3_500L).activeBosses().isEmpty());
        assertEquals(1, engine.snapshot(3_500L).bossesKilled());
    }

    @Test
    void automaticDropDetectionCanBeDisabledWithoutBreakingQuestSignals() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        assertEquals(SlayerPolicy.QuestSignal.STARTED,
                engine.onChat("SLAYER QUEST STARTED!", 1_000L, false));
        engine.onChat("CRAZY RARE DROP! (Warden Heart)", 2_000L, false);

        assertTrue(engine.snapshot(2_000L).dropCounts().isEmpty());
        assertEquals(SlayerSessionEngine.QuestState.ACTIVE,
                engine.snapshot(2_000L).questState());
    }

    @Test
    void unselectedDropStillBelongsToTheSessionButDoesNotResetTheCounter() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Revenant Horror V 10M❤", "Owner: LocalPlayer").orElseThrow();
        engine.observeEntity(1, boss, "LocalPlayer", 1_000L);
        engine.onEntityDeath(1, 2_000L);

        assertTrue(engine.observeDrop("Warden Heart", false));
        SlayerSessionEngine.Snapshot snapshot = engine.snapshot(2_100L);
        assertEquals(1, snapshot.dropCounts().get("Warden Heart"));
        assertEquals("Warden Heart", snapshot.lastDropName());
        assertEquals(1, snapshot.bossesSinceLastDrop());
    }

    @Test
    void verifiedDropCanStartTheSessionWithoutAHudRead() {
        SlayerSessionEngine engine = new SlayerSessionEngine();

        assertTrue(engine.observeDrop("Warden Heart", true, 2_500L));
        SlayerSessionEngine.Snapshot snapshot = engine.viewSnapshot();
        assertEquals(2_500L, snapshot.sessionStartedAtMillis());
        assertEquals(1, snapshot.dropCounts().get("Warden Heart"));
    }

    @Test
    void managerCompleteFinishesTheWholeCarryInsteadOfOneKill() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        assertTrue(engine.addCarry(
                "CarryPlayer", SlayerPolicy.SlayerType.VOIDGLOOM, 4, 25, 1_000L));

        assertTrue(engine.completeCarry("carryplayer", 9_000L));

        SlayerSessionEngine.Carry carry = engine.carries().getFirst();
        assertEquals(25, carry.completed());
        assertTrue(carry.complete());
        assertEquals(9_000L, carry.lastCompletedAtMillis());
        assertFalse(engine.completeCarry("CarryPlayer", 10_000L));
    }

    @Test
    void progressLatchLifecycleResetDoesNotResetBossDropOrCarryLedger() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 210M❤", "Owner: LocalPlayer").orElseThrow();
        engine.observeEntity(7, boss, "LocalPlayer", 2_000L);
        engine.onEntityDeath(7, 5_000L);
        engine.observeDrop("Warden Heart");
        assertEquals("Warden Heart", engine.viewSnapshot().lastDropName());
        assertTrue(engine.addCarry(
                "CarryPlayer", SlayerPolicy.SlayerType.VOIDGLOOM, 4, 2, 6_000L));
        SlayerSessionEngine.Snapshot before = engine.snapshot(7_000L);

        SlayerProgressPolicy.ThresholdState progress = new SlayerProgressPolicy.ThresholdState();
        assertTrue(progress.observe(
                true,
                new SlayerProgressPolicy.Progress(2_403L, 3_000L),
                80,
                false));
        progress.reset();
        engine.resetWorld();

        SlayerSessionEngine.Snapshot after = engine.snapshot(8_000L);
        assertEquals(SlayerSessionEngine.QuestState.IDLE, engine.questState());
        assertEquals(before.bossesKilled(), after.bossesKilled());
        assertEquals(before.lastKillDurationMillis(), after.lastKillDurationMillis());
        assertEquals(before.dropCounts(), after.dropCounts());
        assertEquals(before.carries(), after.carries());
    }

    @Test
    void bossObservationWaitsForOwnerThenRefreshesTheLiveDescriptor() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        SlayerPolicy.EntityDescriptor unnamed = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 210M❤", "").orElseThrow();
        SlayerPolicy.EntityDescriptor owned = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 210M❤", "Owner: LocalPlayer").orElseThrow();

        assertFalse(engine.observeEntity(42, unnamed, "LocalPlayer", 1_000L).spawned());
        assertTrue(engine.viewSnapshot().activeBosses().isEmpty());

        assertTrue(engine.observeEntity(42, owned, "LocalPlayer", 1_100L).spawned());
        assertEquals("LocalPlayer", engine.viewSnapshot().activeBosses().getFirst().descriptor().owner());
        long spawnedAt = engine.viewSnapshot().activeBosses().getFirst().spawnedAtMillis();

        SlayerPolicy.EntityDescriptor refreshed = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 180M❤", "Owner: LocalPlayer").orElseThrow();
        assertFalse(engine.observeEntity(42, refreshed, "LocalPlayer", 1_200L).spawned());
        assertEquals(spawnedAt, engine.viewSnapshot().activeBosses().getFirst().spawnedAtMillis());
        assertEquals("LocalPlayer", engine.viewSnapshot().activeBosses().getFirst().descriptor().owner());
    }

    @Test
    void questStartedClearsOnlyOwnedBosses() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        SlayerPolicy.EntityDescriptor owned = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 210M❤", "Owner: LocalPlayer").orElseThrow();
        SlayerPolicy.EntityDescriptor carry = SlayerPolicy.classifyTag(
                "☠ Inferno Demonlord IV 150M❤", "Owner: CarryPlayer").orElseThrow();
        engine.observeEntity(1, owned, "LocalPlayer", 1_000L);
        engine.observeEntity(2, carry, "LocalPlayer", 1_000L);

        engine.onChat("SLAYER QUEST STARTED!", 2_000L);

        assertFalse(engine.viewSnapshot().activeBosses().stream()
                .anyMatch(boss -> boss.entityId() == 1));
        assertTrue(engine.viewSnapshot().activeBosses().stream()
                .anyMatch(boss -> boss.entityId() == 2));
        assertEquals(SlayerSessionEngine.QuestState.ACTIVE, engine.questState());
    }

    @Test
    void staleOwnedEntityCannotRegisterAgainAfterNextQuestStarts() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        SlayerPolicy.EntityDescriptor owned = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 210M❤", "Owner: LocalPlayer").orElseThrow();
        engine.observeEntity(91, owned, "LocalPlayer", 1_000L);

        engine.onChat("SLAYER QUEST STARTED!", 2_000L);

        assertFalse(engine.observeEntity(91, owned, "LocalPlayer", 2_100L).spawned());
        assertFalse(engine.onEntityDeath(91, 2_200L).bossKilled());
        assertEquals(0, engine.viewSnapshot().bossesKilled());
    }

    @Test
    void secondOwnedBossOfTheSameFamilyIsIgnoredAndMinibossDeathIsNotAKill() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph II 12M❤", "Owner: LocalPlayer").orElseThrow();
        SlayerPolicy.EntityDescriptor extra = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph IV 210M❤", "Owner: LocalPlayer").orElseThrow();
        SlayerPolicy.EntityDescriptor mini = SlayerPolicy.classifyTag(
                "Voidcrazed Maniac 12M❤", "Spawned by: LocalPlayer").orElseThrow();

        assertTrue(engine.observeEntity(10, boss, "LocalPlayer", 2_000L).spawned());
        assertFalse(engine.observeEntity(11, extra, "LocalPlayer", 2_100L).spawned());
        assertTrue(engine.observeEntity(12, mini, "LocalPlayer", 2_200L).spawned());

        SlayerSessionEngine.DeathResult miniDeath = engine.onEntityDeath(12, 3_000L);
        assertFalse(miniDeath.bossKilled());
        assertEquals(0, engine.snapshot(3_000L).bossesKilled());
        assertEquals(SlayerSessionEngine.QuestState.ACTIVE, engine.questState());
    }

    @Test
    void duplicateZeroSecondOwnedDeathDoesNotCountAsAnotherKill() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph II 12M❤", "Owner: LocalPlayer").orElseThrow();
        engine.observeEntity(20, boss, "LocalPlayer", 2_000L);
        assertTrue(engine.onEntityDeath(20, 8_000L).bossKilled());

        engine.observeEntity(21, boss, "LocalPlayer", 8_050L);
        SlayerSessionEngine.DeathResult flicker = engine.onEntityDeath(21, 8_100L);
        assertFalse(flicker.bossKilled());
        assertEquals(1, engine.snapshot(8_100L).bossesKilled());
        assertEquals(SlayerSessionEngine.QuestState.ACTIVE, engine.questState());
    }

    @Test
    void oneOwnedBossIsKeptForEverySlayerFamily() {
        SlayerSessionEngine engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        String[] tags = {
                "☠ Revenant Horror III 400k❤",
                "☠ Tarantula Broodfather III 900k❤",
                "☠ Sven Packmaster III 750k❤",
                "☠ Voidgloom Seraph III 50M❤",
                "☠ Inferno Demonlord III 45M❤",
                "☠ Riftstalker Bloodfiend III"
        };
        int entityId = 100;
        for (String tag : tags) {
            SlayerPolicy.EntityDescriptor first = SlayerPolicy.classifyTag(
                    tag, "Owner: LocalPlayer").orElseThrow();
            SlayerPolicy.EntityDescriptor extra = SlayerPolicy.classifyTag(
                    tag, "Owner: LocalPlayer").orElseThrow();
            assertTrue(engine.observeEntity(entityId, first, "LocalPlayer", 2_000L).spawned(), tag);
            assertFalse(engine.observeEntity(entityId + 1, extra, "LocalPlayer", 2_100L).spawned(), tag);
            entityId += 2;
        }
        long ownedBosses = engine.snapshot(3_000L).activeBosses().stream()
                .filter(SlayerSessionEngine.ActiveBoss::owned)
                .filter(active -> active.descriptor().role() == SlayerPolicy.EntityRole.BOSS)
                .count();
        assertEquals(6, ownedBosses);
    }
}
