package fi.rotclient;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SlayerCocoonRecoveryPolicyTest {
    @Test
    void exactOwnedCocoonRestartReusesExistingUniqueFreshEntityResolver() {
        var recovery = new SlayerCocoonRecoveryPolicy.State();
        assertTrue(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 40, ownedBoss(),
                "LocalPlayer", 10_000L));
        assertTrue(recovery.observeRestart(" §aSLAYER QUEST STARTED! ", 16_000L));
        var restart = recovery.resolveRestart(quest(), 16_050L).orElseThrow();
        assertEquals(16_000L, restart.announcedAtMillis());
        var spawning = new SlayerSpawnPolicy.State();
        assertTrue(spawning.observeVerifiedTransition(restart.announcement(), restart.announcedAtMillis()));
        var unowned = SlayerPolicy.classifyTag("Voidgloom Seraph IV 210M❤", "").orElseThrow();
        var candidates = List.of(new SlayerSpawnPolicy.Candidate(
                80, SlayerPolicy.SlayerType.VOIDGLOOM, 15_950L, unowned, true));
        assertEquals("LocalPlayer", spawning.resolve(candidates, "LocalPlayer", 16_100L)
                .orElseThrow().descriptor().owner());
        assertTrue(recovery.resolveRestart(quest(), 16_100L).isEmpty());
    }

    @Test
    void exactCocoonCannotArmForForeignUnknownOrNonBossSources() {
        var foreign = SlayerPolicy.classifyTag("Voidgloom Seraph IV 210M❤", "Owner: Friend").orElseThrow();
        var unowned = SlayerPolicy.classifyTag("Voidgloom Seraph IV 210M❤", "").orElseThrow();
        var mini = SlayerPolicy.classifyTag("Voidling Devotee 750k❤", "Owner: LocalPlayer").orElseThrow();
        for (var source : List.of(foreign, unowned, mini)) {
            assertFalse(new SlayerCocoonRecoveryPolicy.State().observeCocoon(
                    "YOU COCOONED YOUR SLAYER BOSS", 40, source, "LocalPlayer", 10_000L));
        }
        assertFalse(new SlayerCocoonRecoveryPolicy.State().observeCocoon(
                "YOU COCOONED YOUR SLAYER BOSS", 40, null, "LocalPlayer", 10_000L));
    }

    @Test
    void quotedCocoonAndRestartMessagesDoNotCreateARecoveryChain() {
        var recovery = new SlayerCocoonRecoveryPolicy.State();
        assertFalse(recovery.observeCocoon("Friend: YOU COCOONED YOUR SLAYER BOSS", 40,
                ownedBoss(), "LocalPlayer", 10_000L));
        assertTrue(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 40,
                ownedBoss(), "LocalPlayer", 10_000L));
        assertFalse(recovery.observeRestart("Party > Friend: SLAYER QUEST STARTED!", 16_000L));
        assertTrue(recovery.resolveRestart(quest(), 16_000L).isEmpty());
    }

    @Test
    void differentQuestFamilyOrTierCannotBorrowPreviousOwner() {
        for (var wrong : List.of(new SlayerFightPolicy.QuestRef(SlayerPolicy.SlayerType.INFERNO, 4),
                new SlayerFightPolicy.QuestRef(SlayerPolicy.SlayerType.VOIDGLOOM, 3))) {
            var recovery = armed();
            recovery.observeRestart("SLAYER QUEST STARTED!", 16_000L);
            assertTrue(recovery.resolveRestart(wrong, 16_100L).isEmpty());
            assertFalse(recovery.pending(16_100L));
        }
    }

    @Test
    void duplicateCocoonAndRestartCannotExtendOrReopenWindow() {
        var recovery = armed();
        assertFalse(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 40,
                ownedBoss(), "LocalPlayer", 16_000L));
        assertFalse(recovery.observeRestart("SLAYER QUEST STARTED!", 17_001L));
        assertTrue(recovery.resolveRestart(quest(), 17_001L).isEmpty());

        recovery = armed();
        assertTrue(recovery.observeRestart("SLAYER QUEST STARTED!", 15_000L));
        assertFalse(recovery.observeRestart("SLAYER QUEST STARTED!", 16_000L));
        assertTrue(recovery.resolveRestart(quest(), 16_501L).isEmpty());
        assertFalse(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 80,
                ownedBoss(), "LocalPlayer", 16_600L));
    }

    @Test
    void replayedCocoonCannotSuspendResumedBossWithinOriginalWindow() {
        var recovery = armed();
        recovery.observeRestart("SLAYER QUEST STARTED!", 16_000L);
        assertTrue(recovery.resolveRestart(quest(), 16_100L).isPresent());
        assertFalse(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 80,
                ownedBoss(), "LocalPlayer", 16_200L));
        assertFalse(recovery.pending(16_200L));
        recovery.clearPending();
        assertFalse(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 80,
                ownedBoss(), "LocalPlayer", 17_000L));
        assertTrue(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 80,
                ownedBoss(), "LocalPlayer", 17_001L));
    }

    @Test
    void missingQuestMetadataWaitsWithinBoundedWindow() {
        var recovery = armed();
        recovery.observeRestart("SLAYER QUEST STARTED!", 16_000L);
        assertTrue(recovery.resolveRestart(null, 16_010L).isEmpty());
        assertTrue(recovery.resolveRestart(quest(), 16_100L).isPresent());
    }

    @Test
    void worldResetAndBackwardsClockInvalidateRecoveryEvidence() {
        var recovery = armed();
        recovery.reset();
        assertFalse(recovery.observeRestart("SLAYER QUEST STARTED!", 16_000L));
        recovery = armed();
        assertFalse(recovery.pending(9_999L));
        assertTrue(recovery.resolveRestart(quest(), 16_000L).isEmpty());
    }

    @Test
    void activeOwnedBossSuspensionAwardsNoKillOrCarryAndPreservesDrops() {
        var engine = engine();
        engine.addCarry("LocalPlayer", SlayerPolicy.SlayerType.VOIDGLOOM, 4, 2, 1_000L);
        engine.observeDrop("Judgement Core", true, 1_500L);
        engine.observeEntity(40, ownedBoss(), "LocalPlayer", 2_000L);
        assertTrue(engine.suspendOwnedBossForCocoon(40));
        assertFalse(engine.suspendOwnedBossForCocoon(40));
        assertFalse(engine.onEntityDeath(40, 10_000L).bossKilled());
        assertFalse(engine.onEntityDeath(40, 10_050L).carryAdvanced());
        assertFalse(engine.observeEntity(40, ownedBoss(), "LocalPlayer", 10_100L).spawned());
        var snapshot = engine.viewSnapshot();
        assertEquals(0, snapshot.bossesKilled());
        assertEquals(0L, snapshot.totalKillDurationMillis());
        assertEquals(0, snapshot.carries().getFirst().completed());
        assertEquals(1, snapshot.dropCounts().get("Judgement Core"));
        assertTrue(snapshot.activeBosses().isEmpty());
    }

    @Test
    void resumedFreshBossCountsExactlyOnceAfterSuspension() {
        var engine = engine();
        engine.observeEntity(40, ownedBoss(), "LocalPlayer", 2_000L);
        assertTrue(engine.suspendOwnedBossForCocoon(40));
        engine.onEntityDeath(40, 10_000L);
        engine.onChat("SLAYER QUEST STARTED!", 16_000L);
        engine.observeEntity(80, ownedBoss(), "LocalPlayer", 16_100L);
        assertTrue(engine.onEntityDeath(80, 20_000L).bossKilled());
        assertFalse(engine.onEntityDeath(80, 20_100L).bossKilled());
        assertEquals(1, engine.viewSnapshot().bossesKilled());
        assertEquals(3_900L, engine.viewSnapshot().lastKillDurationMillis());
    }

    @Test
    void foreignBossCannotBeSuspendedAndItsMatchingCarryStillWorks() {
        var engine = engine();
        engine.addCarry("Friend", SlayerPolicy.SlayerType.VOIDGLOOM, 4, 2, 1_000L);
        var foreign = SlayerPolicy.classifyTag("Voidgloom Seraph IV 210M❤", "Owner: Friend").orElseThrow();
        engine.observeEntity(40, foreign, "LocalPlayer", 2_000L);
        assertFalse(engine.suspendOwnedBossForCocoon(40));
        assertTrue(engine.onEntityDeath(40, 10_000L).carryAdvanced());
        assertEquals(1, engine.carries().getFirst().completed());
    }

    @Test
    void creditedDeathIsNeverReversedByLateCocoonObservation() {
        var engine = engine();
        engine.observeEntity(40, ownedBoss(), "LocalPlayer", 2_000L);
        assertTrue(engine.onEntityDeath(40, 10_000L).bossKilled());
        assertFalse(engine.suspendOwnedBossForCocoon(40));
        assertEquals(1, engine.viewSnapshot().bossesKilled());
        assertEquals(8_000L, engine.viewSnapshot().lastKillDurationMillis());
    }

    @Test
    void wrongEntityAndNonBossCannotBeSuspendedAndWorldResetClearsSuppression() {
        var engine = engine();
        engine.observeEntity(40, ownedBoss(), "LocalPlayer", 2_000L);
        assertFalse(engine.suspendOwnedBossForCocoon(41));
        var mini = SlayerPolicy.classifyTag("Voidling Devotee 750k❤", "Owner: LocalPlayer").orElseThrow();
        engine.observeEntity(41, mini, "LocalPlayer", 2_100L);
        assertFalse(engine.suspendOwnedBossForCocoon(41));
        assertTrue(engine.suspendOwnedBossForCocoon(40));
        engine.resetWorld();
        assertFalse(engine.isCocoonSuspended(40));
        assertTrue(engine.observeEntity(40, ownedBoss(), "LocalPlayer", 20_000L).spawned());
    }

    private static SlayerCocoonRecoveryPolicy.State armed() {
        var recovery = new SlayerCocoonRecoveryPolicy.State();
        assertTrue(recovery.observeCocoon("YOU COCOONED YOUR SLAYER BOSS", 40,
                ownedBoss(), "LocalPlayer", 10_000L));
        return recovery;
    }

    private static SlayerPolicy.EntityDescriptor ownedBoss() {
        return SlayerPolicy.classifyTag("Voidgloom Seraph IV 210M❤", "Owner: LocalPlayer").orElseThrow();
    }

    private static SlayerFightPolicy.QuestRef quest() {
        return new SlayerFightPolicy.QuestRef(SlayerPolicy.SlayerType.VOIDGLOOM, 4);
    }

    private static SlayerSessionEngine engine() {
        var engine = new SlayerSessionEngine();
        engine.onChat("SLAYER QUEST STARTED!", 1_000L);
        return engine;
    }
}
