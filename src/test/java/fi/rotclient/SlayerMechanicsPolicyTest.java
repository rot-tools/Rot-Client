package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerMechanicsPolicyTest {
    @Test
    void cocoonTimerStartsOnlyFromTheExactHypixelSignalAndExpiresAfterSixSeconds() {
        SlayerMechanicsPolicy.CocoonTimer timer = new SlayerMechanicsPolicy.CocoonTimer();

        assertFalse(timer.observe("You cocooned a random mob", 1_000L));
        assertEquals(0L, timer.remainingMillis(1_000L));

        assertTrue(timer.observe("  YOU COCOONED YOUR SLAYER BOSS  ", 2_000L));
        assertEquals(6_000L, timer.remainingMillis(2_000L));
        assertEquals(1_400L, timer.remainingMillis(6_600L));
        assertEquals(0L, timer.remainingMillis(8_000L));
        assertFalse(timer.active(8_000L));
    }

    @Test
    void daggerAttunementsUseTheCorrectFireAndMawFamiliesAndNbtModes() {
        assertEquals(
                SlayerMechanicsPolicy.DaggerAttunement.ASHEN,
                SlayerMechanicsPolicy.daggerAttunement("ASHEN ♨: 12M❤").orElseThrow());
        assertEquals(
                SlayerMechanicsPolicy.DaggerAttunement.CRYSTAL,
                SlayerMechanicsPolicy.daggerAttunement("CRYSTAL ♨: 12M❤").orElseThrow());

        assertTrue(SlayerMechanicsPolicy.supportsDagger(
                "FIREDUST_DAGGER", SlayerMechanicsPolicy.DaggerAttunement.ASHEN));
        assertTrue(SlayerMechanicsPolicy.supportsDagger(
                "PYROCHAOS_DAGGER", SlayerMechanicsPolicy.DaggerAttunement.AURIC));
        assertFalse(SlayerMechanicsPolicy.supportsDagger(
                "FIREDUST_DAGGER", SlayerMechanicsPolicy.DaggerAttunement.SPIRIT));
        assertTrue(SlayerMechanicsPolicy.supportsDagger(
                "MAWDUST_DAGGER", SlayerMechanicsPolicy.DaggerAttunement.SPIRIT));
        assertTrue(SlayerMechanicsPolicy.supportsDagger(
                "DEATHRIPPER_DAGGER", SlayerMechanicsPolicy.DaggerAttunement.CRYSTAL));

        assertEquals(0, SlayerMechanicsPolicy.DaggerAttunement.ASHEN.mode());
        assertEquals(1, SlayerMechanicsPolicy.DaggerAttunement.AURIC.mode());
        assertEquals(2, SlayerMechanicsPolicy.DaggerAttunement.SPIRIT.mode());
        assertEquals(3, SlayerMechanicsPolicy.DaggerAttunement.CRYSTAL.mode());
    }

    @Test
    void daggerSwapStateSchedulesOnlyRealAttunementChangesAndRespectsDelay() {
        SlayerMechanicsPolicy.DaggerSwapState state =
                new SlayerMechanicsPolicy.DaggerSwapState();

        assertTrue(state.observe("ASHEN ♨: 12M❤", 1, 2));
        assertFalse(state.ready().isPresent());
        state.tick();
        assertFalse(state.ready().isPresent());
        state.tick();
        assertFalse(state.ready().isPresent());
        state.tick();
        assertEquals(
                SlayerMechanicsPolicy.DaggerAttunement.ASHEN,
                state.ready().orElseThrow());
        state.complete();

        assertFalse(state.observe("ASHEN ♨: 8M❤", 0, 0));
        assertTrue(state.observe("AURIC ♨: 8M❤", 0, 0));
        state.tick();
        assertEquals(
                SlayerMechanicsPolicy.DaggerAttunement.AURIC,
                state.ready().orElseThrow());
    }

    @Test
    void laserHiderTargetsOnlyGuardianBeamsAnchoredToOtherVoidglooms() {
        List<SlayerMechanicsPolicy.LaserAnchor> anchors = List.of(
                new SlayerMechanicsPolicy.LaserAnchor(10.0D, 70.0D, 20.0D, true, false),
                new SlayerMechanicsPolicy.LaserAnchor(30.0D, 80.0D, 40.0D, false, false),
                new SlayerMechanicsPolicy.LaserAnchor(50.0D, 90.0D, 60.0D, false, true));

        assertFalse(SlayerMechanicsPolicy.shouldHideLaser(
                10.2D, 72.0D, 20.2D, anchors, true));
        assertTrue(SlayerMechanicsPolicy.shouldHideLaser(
                30.4D, 84.9D, 39.6D, anchors, false));
        assertFalse(SlayerMechanicsPolicy.shouldHideLaser(
                30.6D, 84.9D, 40.0D, anchors, true));
        assertFalse(SlayerMechanicsPolicy.shouldHideLaser(
                50.0D, 90.0D, 60.0D, anchors, false));
        assertTrue(SlayerMechanicsPolicy.shouldHideLaser(
                50.0D, 90.0D, 60.0D, anchors, true));
    }

    @Test
    void soulcrySupportsOnlyVoidgloomKatanasAndUsesTheRealManaCosts() {
        assertTrue(SlayerMechanicsPolicy.isSoulcryKatana("VOIDEDGE_KATANA"));
        assertTrue(SlayerMechanicsPolicy.isSoulcryKatana("VORPAL_KATANA"));
        assertTrue(SlayerMechanicsPolicy.isSoulcryKatana("ATOMSPLIT_KATANA"));
        assertFalse(SlayerMechanicsPolicy.isSoulcryKatana("GIANTS_SWORD"));

        assertEquals(200, SlayerMechanicsPolicy.soulcryManaCost(false));
        assertEquals(100, SlayerMechanicsPolicy.soulcryManaCost(true));
        assertTrue(SlayerMechanicsPolicy.hasSoulcryMana(200.0D, 0.0D, false));
        assertTrue(SlayerMechanicsPolicy.hasSoulcryMana(60.0D, 40.0D, true));
        assertFalse(SlayerMechanicsPolicy.hasSoulcryMana(99.0D, 0.0D, true));
    }

    @Test
    void soulcryTickStateArmsOnceAndWaitsForTheConfiguredDelay() {
        SlayerMechanicsPolicy.SoulcryState state = new SlayerMechanicsPolicy.SoulcryState();
        assertTrue(state.arm(1, 3, 2));
        assertFalse(state.ready());
        state.tick();
        assertFalse(state.ready());
        state.tick();
        assertFalse(state.ready());
        state.tick();
        assertTrue(state.ready());
        state.complete();
        assertFalse(state.ready());

        assertTrue(state.arm(4, 2, 99));
        assertFalse(state.ready());
        state.reset();
        assertFalse(state.ready());
    }

    @Test
    void soulcryAbilityGateWaitsTheRealFourSecondCooldownAndHonorsItemCooldown() {
        SlayerMechanicsPolicy.SoulcryAbilityGate gate =
                new SlayerMechanicsPolicy.SoulcryAbilityGate();
        assertTrue(gate.ready(false));
        assertFalse(gate.ready(true));
        gate.markUsed();
        assertFalse(gate.ready(false));
        assertEquals(80, SlayerMechanicsPolicy.SOULCRY_ABILITY_COOLDOWN_TICKS);
        assertEquals(80, gate.remainingTicks());
        for (int i = 0; i < 79; i++) {
            gate.tick();
        }
        assertFalse(gate.ready(false));
        gate.tick();
        assertTrue(gate.ready(false));
        assertFalse(gate.ready(true));

        gate.markUsed();
        gate.observeRemaining(40);
        assertEquals(80, gate.remainingTicks());
        gate.observeRemaining(90);
        assertEquals(90, gate.remainingTicks());
        gate.reset();
        assertTrue(gate.ready(false));

        assertEquals(80, SlayerMechanicsPolicy.abilityCooldownTicks(
                "This ability is on cooldown for 4s").orElse(-1));
        assertEquals(70, SlayerMechanicsPolicy.abilityCooldownTicks(
                "§cThis ability is on cooldown for 3.5s").orElse(-1));
        assertTrue(SlayerMechanicsPolicy.abilityCooldownTicks("Boss spawned").isEmpty());
    }

    @Test
    void vengeanceTimerRunsForExactlySixSecondsAndCanShowTicksOrSeconds() {
        SlayerMechanicsPolicy.VengeanceTimer timer = new SlayerMechanicsPolicy.VengeanceTimer();
        timer.start();
        assertEquals(120, timer.remainingTicks());
        assertEquals("120", timer.display(true));
        assertEquals("6.0s", timer.display(false));
        for (int i = 0; i < 119; i++) {
            timer.tick();
        }
        assertTrue(timer.active());
        assertEquals(1, timer.remainingTicks());
        timer.tick();
        assertFalse(timer.active());
        assertEquals("", timer.display(false));
    }

    @Test
    void attunementDisplayParsesBossCountAndUsesStableColors() {
        var display = SlayerMechanicsPolicy.attunementDisplay("AURIC ♨5 00:12", true).orElseThrow();
        assertEquals(SlayerMechanicsPolicy.DaggerAttunement.AURIC, display.attunement());
        assertEquals(5, display.count());
        assertEquals("§l§eAURIC ♨5", display.formatted());

        assertTrue(SlayerMechanicsPolicy.attunementDisplay("SPIRIT ♨2 00:03", false)
                .orElseThrow().formatted().endsWith("SPIRIT"));
        assertTrue(SlayerMechanicsPolicy.attunementDisplay("ASHEN ♨7", true).isEmpty());
        assertTrue(SlayerMechanicsPolicy.isVengeanceStartTag("ASHEN ♨7"));
        assertTrue(SlayerMechanicsPolicy.isVengeanceStartTag("  ASHEN ♨7 00:12"));
        assertFalse(SlayerMechanicsPolicy.isVengeanceStartTag("ASHEN ♨6 00:12"));
    }

    @Test
    void voidgloomSoundFilterCancelsOnlyTheTwoConfiguredEndermanSounds() {
        assertTrue(SlayerMechanicsPolicy.isVoidgloomNoise("minecraft:entity.enderman.stare"));
        assertTrue(SlayerMechanicsPolicy.isVoidgloomNoise("entity.enderman.scream"));
        assertFalse(SlayerMechanicsPolicy.isVoidgloomNoise("minecraft:block.note_block.pling"));
    }

    @Test
    void vengeanceDamageMatchesMarkerAndThreshold() {
        assertEquals(500_000L, SlayerMechanicsPolicy.vengeanceDamage("500,000ﬗ").orElseThrow());
        assertTrue(SlayerMechanicsPolicy.vengeanceDamage("499,999ﬗ").isEmpty());
        assertTrue(SlayerMechanicsPolicy.vengeanceDamage("500,000").isEmpty());
        assertEquals("1.25M", SlayerMechanicsPolicy.abbreviateDamage(1_250_000L));
    }
}
