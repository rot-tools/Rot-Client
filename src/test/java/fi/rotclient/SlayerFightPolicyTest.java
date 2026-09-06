package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerFightPolicyTest {
    @Test
    void classifiesVoidgloomBeaconAndNukekubiStands() {
        assertTrue(SlayerFightPolicy.isPowerOrbHologram("§5Overflux"));
        assertTrue(SlayerFightPolicy.isPowerOrbHologram("Power Orb"));
        assertFalse(SlayerFightPolicy.markerFromStand("Overflux", "Beacon").isPresent());
        assertEquals(
                SlayerFightPolicy.Marker.BEACON,
                SlayerFightPolicy.markerFromStand("", "Beacon").orElseThrow());
        assertEquals(
                SlayerFightPolicy.Marker.BEACON,
                SlayerFightPolicy.markerFromStand("§cYang Glyph", "").orElseThrow());
        assertTrue(SlayerFightPolicy.isThrownYangGlyphStand("", "Beacon", false));
        assertTrue(SlayerFightPolicy.isThrownYangGlyphStand("", "", true));
        assertTrue(SlayerFightPolicy.isThrownYangGlyphStand("Beacon", "", false));
        assertFalse(SlayerFightPolicy.isThrownYangGlyphStand("§cYang Glyph", "", false));
        assertFalse(SlayerFightPolicy.isThrownYangGlyphStand("Destroy the beacon!", "", false));
        assertEquals(
                SlayerFightPolicy.Marker.NUKEKUBI,
                SlayerFightPolicy.markerFromStand("Nukekubi", "").orElseThrow());
        assertEquals(
                SlayerFightPolicy.VoidgloomPhase.HITS,
                SlayerFightPolicy.voidgloomPhase("§cHits: 42"));
        assertEquals(42, SlayerFightPolicy.hitsRemaining("§cHits: 42").orElseThrow());
        assertEquals(43, SlayerFightPolicy.hitsRemaining("☠ Voidgloom Seraph IV 210M❤ 43 Hits").orElseThrow());
        assertEquals("Hits 43", SlayerFightPolicy.phaseLabel(SlayerFightPolicy.VoidgloomPhase.HITS, 43));
        assertEquals(
                SlayerFightPolicy.VoidgloomPhase.BEACON,
                SlayerFightPolicy.voidgloomPhase("Destroy the beacon!"));
        assertEquals("Yang Glyph", SlayerFightPolicy.phaseLabel(SlayerFightPolicy.VoidgloomPhase.BEACON));
        assertTrue(SlayerFightPolicy.isNukekubiTexture(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWIwNzU5NGUyZGYyNzM5MjFhNzdjMTAxZDBiZmRmYTExMTVhYmVkNWI5YjIwMjllYjQ5NmNlYmE5YmRiYjRiMyJ9fX0="));
        assertEquals("3.5s", SlayerFightPolicy.countdownLabel(
                SlayerFightPolicy.remainingSeconds(0L, 1_500L, SlayerFightPolicy.SITTING_BEACON_MILLIS)));
        assertTrue(SlayerFightPolicy.flyingBeaconLanded(16.0D, 0.04D));
        assertFalse(SlayerFightPolicy.flyingBeaconLanded(1.0D, 0.01D));
        assertFalse(SlayerFightPolicy.flyingBeaconLanded(16.0D, 1.0D));
        assertEquals(0.0D, SlayerFightPolicy.remainingSeconds(0L, 8_000L, SlayerFightPolicy.LASER_DURATION_MILLIS));
        assertEquals("210M❤", SlayerFightPolicy.compactHealth("☠ Voidgloom Seraph IV 210M❤ 43 Hits").orElseThrow());
        assertEquals("1.2B❤", SlayerFightPolicy.compactHealth("1.2B❤").orElseThrow());
        assertEquals(3, SlayerFightPolicy.clampLineWidth(3));
        assertEquals(1, SlayerFightPolicy.clampLineWidth(0));
        assertEquals(10, SlayerFightPolicy.clampLineWidth(99));
    }

    @Test
    void classifiesRevenantTaraSvenAndVampireMarkers() {
        assertEquals(
                SlayerFightPolicy.Marker.BOOM,
                SlayerFightPolicy.markerFromStand("§cBOOM!", "").orElseThrow());
        assertTrue(SlayerFightPolicy.isBoomHologram("Boom!"));
        assertTrue(SlayerFightPolicy.isBoomHologram("§f§lBOOM!"));
        assertFalse(SlayerFightPolicy.isBoomHologram("Revenant Horror"));
        assertEquals(
                SlayerFightPolicy.Marker.EGG_SAC,
                SlayerFightPolicy.markerFromStand("12s 3/8", "").orElseThrow());
        assertEquals("3/8", SlayerFightPolicy.eggHitsLabel("§e12s 3/8").orElseThrow());
        assertEquals(
                SlayerFightPolicy.TarantulaPhase.FIRST,
                SlayerFightPolicy.tarantulaPhase("☠ Tarantula Broodfather V"));
        assertEquals(
                SlayerFightPolicy.TarantulaPhase.SECOND,
                SlayerFightPolicy.tarantulaPhase("Conjoined Brood"));
        assertEquals("Phase 2/2", SlayerFightPolicy.tarantulaPhaseLabel(
                SlayerFightPolicy.TarantulaPhase.SECOND));
        SlayerPolicy.EntityDescriptor t5PhaseOne = SlayerPolicy.classifyTag(
                "☠ Tarantula Broodfather 10M❤", "Owner: LocalPlayer").orElseThrow();
        SlayerPolicy.EntityDescriptor t5PhaseTwo = SlayerPolicy.classifyTag(
                "☠ Conjoined Brood 20M❤", "Owner: LocalPlayer").orElseThrow();
        SlayerPolicy.EntityDescriptor t4 = SlayerPolicy.classifyTag(
                "☠ Tarantula Broodfather IV 2.4M❤", "Owner: LocalPlayer").orElseThrow();
        assertTrue(SlayerFightPolicy.isTarantulaTierFivePhaseOne(t5PhaseOne));
        assertFalse(SlayerFightPolicy.isTarantulaTierFivePhaseOne(t5PhaseTwo));
        assertFalse(SlayerFightPolicy.isTarantulaTierFivePhaseOne(t4));
        assertTrue(SlayerFightPolicy.isHatchlingsChat(
                "§cYou need to kill the Broodfather's hatchlings before it can be damaged again!"));
        assertTrue(SlayerFightPolicy.isSpiderSound("minecraft:entity.spider.hurt"));
        assertTrue(SlayerFightPolicy.isTarantulaSoundArea("Spider's Den\nTarantula Broodfather V"));
        assertFalse(SlayerFightPolicy.isTarantulaSoundArea("The End\nVoidgloom Seraph IV"));
        assertEquals(
                SlayerFightPolicy.Marker.EGG_SAC,
                SlayerFightPolicy.markerFromStand("Egg Sac", "Cobweb").orElseThrow());
        assertEquals(
                SlayerFightPolicy.Marker.INVINCIBLE,
                SlayerFightPolicy.markerFromStand("Invincible", "").orElseThrow());
        assertTrue(SlayerFightPolicy.isPupName("Sven Pup"));
        assertFalse(SlayerFightPolicy.isPupName("Sven Packmaster"));
        assertFalse(SlayerFightPolicy.isPupName("Pack Enforcer"));
        assertEquals(
                SlayerFightPolicy.Marker.TWINCLAWS,
                SlayerFightPolicy.markerFromStand("§bTWINCLAWS 1.2s", "").orElseThrow());
        assertTrue(SlayerFightPolicy.isManiaHologram("§cMANIA"));
        assertFalse(SlayerFightPolicy.isManiaHologram("Voidcrazed Maniac"));
        assertTrue(SlayerFightPolicy.isSteakReady("Bloodfiend \u0489"));
        assertEquals(4.0D, SlayerFightPolicy.maniaRemainingSeconds(440), 0.01D);
        assertEquals(0.0D, SlayerFightPolicy.maniaRemainingSeconds(20));
        assertTrue(SlayerFightPolicy.isKillerSpringSound("minecraft:entity.wither.spawn"));
        assertTrue(SlayerFightPolicy.isVampireSoundArea("Stillgore Chateau\nRiftstalker Bloodfiend IV"));
        assertFalse(SlayerFightPolicy.isVampireSoundArea("The End\nVoidgloom Seraph IV"));
        assertEquals(500, SlayerFightPolicy.clampTwinclawsDelay(500));
        assertEquals(1_000, SlayerFightPolicy.clampTwinclawsDelay(9_999));
        assertEquals(
                SlayerFightPolicy.Marker.BLOOD_ICHOR,
                SlayerFightPolicy.markerFromStand("Blood Ichor", "").orElseThrow());
        assertEquals(
                SlayerFightPolicy.Marker.KILLER_SPRING,
                SlayerFightPolicy.markerFromStand("Killer Spring", "").orElseThrow());
        assertEquals(
                SlayerFightPolicy.Marker.FIRE_PILLAR,
                SlayerFightPolicy.markerFromStand("§6§l5s §c§l8 hits", "").orElseThrow());
        assertEquals(5, SlayerFightPolicy.firePillarSeconds("5s 8 hits").orElseThrow());
        assertEquals(8, SlayerFightPolicy.firePillarHits("5s 8 hits").orElseThrow());
        assertEquals(5, SlayerFightPolicy.firePillarSeconds("Fire Pillar 5s").orElseThrow());
        assertEquals(8, SlayerFightPolicy.firePillarHits("§6Fire Pillar §c5s").orElseThrow());
        assertEquals(
                SlayerFightPolicy.Marker.FIRE_PILLAR,
                SlayerFightPolicy.markerFromStand("Fire Pillar 4s", "").orElseThrow());
        assertTrue(SlayerFightPolicy.isWrongAttunementChat("Your hit was reduced by Hellion Shield!"));
        assertTrue(SlayerFightPolicy.isWrongAttunementChat(
                "Strike using the ASHEN attunement on your dagger!"));
        assertFalse(SlayerFightPolicy.isWrongAttunementChat("You have slain the Inferno Demonlord"));
        assertEquals(50_000_000.0D, SlayerFightPolicy.healthValue("50M❤").orElseThrow(), 0.01D);
        assertEquals(30_000_000.0D, SlayerFightPolicy.healthValue("30,000,000❤").orElseThrow(), 0.01D);
        assertEquals("30000000❤", SlayerFightPolicy.compactHealth("Inferno Demonlord 30,000,000❤").orElseThrow());
        var reading = SlayerFightPolicy.healthReading("☠ Inferno Demonlord IV 15M/30M❤").orElseThrow();
        assertEquals(15_000_000.0D, reading.current(), 0.01D);
        assertEquals(30_000_000.0D, reading.max(), 0.01D);
        var full = SlayerFightPolicy.healthReading("Inferno Demonlord 9,000,000/30,000,000❤").orElseThrow();
        assertEquals(9_000_000.0D, full.current(), 0.01D);
        assertEquals(30_000_000.0D, full.max(), 0.01D);
        assertEquals(
                SlayerFightPolicy.InfernoPhase.SECOND,
                SlayerFightPolicy.infernoPhase(4, reading.current(), reading.max()));
        assertEquals(
                SlayerFightPolicy.InfernoPhase.THIRD,
                SlayerFightPolicy.infernoPhase(4, 9_000_000.0D, 30_000_000.0D));
        assertEquals("Phase 1/3", SlayerFightPolicy.infernoPhaseLabel(
                SlayerFightPolicy.InfernoPhase.FIRST, 4));
        assertTrue(SlayerFightPolicy.crossedFirePits(4, 11_000_000.0D, 9_000_000.0D, 30_000_000.0D));
        assertFalse(SlayerFightPolicy.crossedFirePits(2, 11_000_000.0D, 9_000_000.0D, 30_000_000.0D));
        assertEquals(0xFFFFAA00, SlayerFightPolicy.attunementColor(SlayerPolicy.Attunement.AURIC));
        assertTrue(SlayerFightPolicy.shouldHideBlazeParticle("minecraft:flame"));
        assertFalse(SlayerFightPolicy.shouldHideBlazeParticle("minecraft:portal"));
        assertEquals(
                SlayerPolicy.SlayerType.VOIDGLOOM,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.BEACON).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.VOIDGLOOM,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.NUKEKUBI).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.TARANTULA,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.EGG_SAC).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.TARANTULA,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.INVINCIBLE).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.REVENANT,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.BOOM).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.SVEN,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.PUP).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.VAMPIRE,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.BLOOD_ICHOR).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.VAMPIRE,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.KILLER_SPRING).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.VAMPIRE,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.TWINCLAWS).orElseThrow());
        assertEquals(
                SlayerPolicy.SlayerType.INFERNO,
                SlayerFightPolicy.familyForMarker(SlayerFightPolicy.Marker.FIRE_PILLAR).orElseThrow());
        assertEquals(
                SlayerFightPolicy.VoidgloomPhase.UNKNOWN,
                SlayerFightPolicy.voidgloomPhase("5s 8 hits"));
        assertTrue(SlayerFightPolicy.hitsRemaining("5s 8 hits").isEmpty());
        assertTrue(SlayerFightPolicy.familyFromMobName("Revenant Horror").isPresent());
        assertTrue(SlayerFightPolicy.familyFromMobName("[Lv100] Random Zombie").isEmpty());
        assertTrue(SlayerFightPolicy.familyFromMobName("Wolf").isEmpty());
    }

    @Test
    void readsSidebarQuestAndBuildsMaddoxRestartCommands() {
        Optional<SlayerFightPolicy.QuestRef> quest = SlayerFightPolicy.questFromSidebar(List.of(
                "Purse: 12M",
                "Slayer Quest",
                "Voidgloom Seraph IV",
                "Spawn the boss!"));
        assertTrue(quest.isPresent());
        assertEquals(SlayerPolicy.SlayerType.VOIDGLOOM, quest.get().type());
        assertEquals(4, quest.get().tier());
        assertEquals("enderman 4", SlayerFightPolicy.autoStartCommand(quest.get().type(), quest.get().tier()));
        assertEquals("sven 3", SlayerFightPolicy.autoStartCommand(SlayerPolicy.SlayerType.SVEN, 3));
        assertTrue(SlayerFightPolicy.sidebarHasSlayerQuest(List.of(
                "Slayer Quest",
                "Voidgloom Seraph II",
                "Spawn the boss!")));
        Optional<SlayerFightPolicy.QuestRef> tierTwo = SlayerFightPolicy.questFromSidebar(List.of(
                "Slayer Quest",
                "Voidgloom Seraph II"));
        assertTrue(tierTwo.isPresent());
        assertEquals(2, tierTwo.get().tier());
        Optional<SlayerFightPolicy.QuestRef> enderman = SlayerFightPolicy.questFromSidebar(List.of(
                "Enderman Slayer II"));
        assertTrue(enderman.isPresent());
        assertEquals(SlayerPolicy.SlayerType.VOIDGLOOM, enderman.get().type());
        assertEquals(2, enderman.get().tier());
        Optional<SlayerFightPolicy.QuestRef> glued = SlayerFightPolicy.questFromSidebar(List.of(
                "Voidgloom SeraphIV"));
        assertTrue(glued.isPresent());
        assertEquals(4, glued.get().tier());
        Optional<SlayerFightPolicy.QuestRef> split = SlayerFightPolicy.questFromSidebar(List.of(
                "Voidgloom Seraph",
                "IV"));
        assertTrue(split.isPresent());
        assertEquals(4, split.get().tier());
        Optional<SlayerFightPolicy.QuestRef> numeric = SlayerFightPolicy.questFromSidebar(List.of(
                "Slayer Quest",
                "Voidgloom Seraph T4"));
        assertTrue(numeric.isPresent());
        assertEquals(4, numeric.get().tier());
        assertEquals(" IV", SlayerFightPolicy.romanLabel(4));
        assertTrue(SlayerFightPolicy.wrongQuest(
                SlayerPolicy.SlayerType.SVEN, SlayerPolicy.SlayerType.VOIDGLOOM));
        assertFalse(SlayerFightPolicy.wrongQuest(
                SlayerPolicy.SlayerType.SVEN, SlayerPolicy.SlayerType.SVEN));
    }

    @Test
    void howlAndParticleFiltersMatchKnownIds() {
        assertTrue(SlayerFightPolicy.isHowlSound("minecraft:entity.wolf.howl"));
        assertTrue(SlayerFightPolicy.isWolfSound("minecraft:entity.wolf.growl"));
        assertTrue(SlayerFightPolicy.isHowlHologram("§cHowl!"));
        assertFalse(SlayerFightPolicy.isHowlHologram("Howling Cave"));
        assertTrue(SlayerFightPolicy.isSvenSoundArea("The Park\nSven Packmaster IV"));
        assertFalse(SlayerFightPolicy.isSvenSoundArea("The End\nVoidgloom Seraph IV"));
        assertFalse(SlayerFightPolicy.isVoidgloomParticle("minecraft:portal"));
        assertTrue(SlayerFightPolicy.shouldHideVoidgloomParticle("minecraft:witch"));
        assertTrue(SlayerFightPolicy.shouldHideVoidgloomParticle("minecraft:large_smoke"));
        assertTrue(SlayerFightPolicy.shouldHideVoidgloomParticle("minecraft:flame"));
        assertFalse(SlayerFightPolicy.shouldHideVoidgloomParticle("minecraft:portal"));
        assertTrue(SlayerFightPolicy.isVampireNoise("minecraft:entity.bat.ambient"));
        assertTrue(SlayerFightPolicy.isSpiderSound("minecraft:entity.silverfish.hurt"));
        assertTrue(SlayerFightPolicy.isInfernoFightArea("Crimson Isle\nInferno Demonlord IV"));
        assertEquals(10, SlayerFightPolicy.clampAutoStartDelayTicks(10));
        assertEquals(40, SlayerFightPolicy.clampAutoStartDelayTicks(99));
    }
}
