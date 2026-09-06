package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerPolicyTest {
    @Test
    void parsesQuestLifecycleWithoutDependingOnFormattingWhitespace() {
        assertEquals(
                SlayerPolicy.QuestSignal.STARTED,
                SlayerPolicy.questSignal("   SLAYER QUEST STARTED!   "));
        assertEquals(
                SlayerPolicy.QuestSignal.COMPLETED,
                SlayerPolicy.questSignal("SLAYER QUEST COMPLETE!"));
        assertEquals(
                SlayerPolicy.QuestSignal.FAILED,
                SlayerPolicy.questSignal("  SLAYER QUEST FAILED!"));
        assertEquals(
                SlayerPolicy.QuestSignal.NONE,
                SlayerPolicy.questSignal("You earned 100 Combat XP"));
        assertEquals(
                SlayerPolicy.QuestSignal.STARTED,
                SlayerPolicy.questSignal("☠ SLAYER QUEST STARTED!"));
    }

    @Test
    void classifiesEveryLiveBossFamilyAndRomanTier() {
        assertBoss("☠ Revenant Horror V 2.4M❤", SlayerPolicy.SlayerType.REVENANT, 5);
        assertBoss("☠ Tarantula Broodfather IV 2.4M❤", SlayerPolicy.SlayerType.TARANTULA, 4);
        assertBoss("☠ Sven Packmaster IV 2.4M❤", SlayerPolicy.SlayerType.SVEN, 4);
        assertBoss("☠ Voidgloom Seraph IV❤", SlayerPolicy.SlayerType.VOIDGLOOM, 4);
        assertBoss("☠ Voidgloom Seraph II 12M❤", SlayerPolicy.SlayerType.VOIDGLOOM, 2);
        assertBoss("☠ Inferno Demonlord IV 150M❤", SlayerPolicy.SlayerType.INFERNO, 4);
        assertBoss("☠ Riftstalker Bloodfiend V 10M❤", SlayerPolicy.SlayerType.VAMPIRE, 5);
    }

    @Test
    void voidgloomTierTwoIsNotReadAsTierFourFromLaterHitsText() {
        SlayerPolicy.EntityDescriptor boss = SlayerPolicy.classifyHolograms(java.util.List.of(
                "☠ Voidgloom Seraph II 12M❤",
                "43 Hits",
                "Spawned by: LocalPlayer")).orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.BOSS, boss.role());
        assertEquals(2, boss.tier());
        assertEquals("LocalPlayer", boss.owner());
    }

    @Test
    void nearbyVoidgloomTitleDoesNotTurnAVoidlingIntoTheBoss() {
        SlayerPolicy.EntityDescriptor mini = SlayerPolicy.classifyHolograms(java.util.List.of(
                "Voidling Devotee",
                "☠ Voidgloom Seraph IV 210M❤",
                "Spawned by: LocalPlayer")).orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.MINIBOSS, mini.role());
        assertEquals("Voidling Devotee", mini.displayName());
    }

    @Test
    void infersVoidgloomTierFromHealthWhenTheNametagHasNoRoman() {
        SlayerPolicy.EntityDescriptor t2 = SlayerPolicy.classifyTag(
                "☠ Voidgloom Seraph 12M❤", "Spawned by: LocalPlayer").orElseThrow();
        assertEquals(2, t2.tier());
    }

    @Test
    void infersEveryFamilyTierFromHealthWhenTheNametagHasNoRoman() {
        assertEquals(1, classifyTier("☠ Revenant Horror 500❤"));
        assertEquals(2, classifyTier("☠ Revenant Horror 20k❤"));
        assertEquals(3, classifyTier("☠ Revenant Horror 400k❤"));
        assertEquals(4, classifyTier("☠ Revenant Horror 1.5M❤"));
        assertEquals(5, classifyTier("☠ Revenant Horror 10M❤"));
        assertEquals(5, classifyTier("☠ Atoned Horror 10M❤"));
        assertEquals(1, classifyTier("☠ Tarantula Broodfather 750❤"));
        assertEquals(2, classifyTier("☠ Tarantula Broodfather 30k❤"));
        assertEquals(3, classifyTier("☠ Tarantula Broodfather 900k❤"));
        assertEquals(4, classifyTier("☠ Tarantula Broodfather 2.4M❤"));
        assertEquals(5, classifyTier("☠ Tarantula Broodfather 10M❤"));
        assertEquals(5, classifyTier("☠ Conjoined Brood 20M❤"));
        assertEquals("Conjoined Brood", SlayerPolicy.classifyTag(
                "☠ Conjoined Brood 20M❤", "Owner: ExamplePlayer").orElseThrow().displayName());
        assertEquals("Tarantula Broodfather", SlayerPolicy.classifyTag(
                "☠ Tarantula Broodfather 10M❤", "Owner: ExamplePlayer").orElseThrow().displayName());
        assertEquals(1, classifyTier("☠ Sven Packmaster 2k❤"));
        assertEquals(2, classifyTier("☠ Sven Packmaster 40k❤"));
        assertEquals(3, classifyTier("☠ Sven Packmaster 750k❤"));
        assertEquals(4, classifyTier("☠ Sven Packmaster 2M❤"));
        assertEquals(1, classifyTier("☠ Inferno Demonlord 2.5M❤"));
        assertEquals(2, classifyTier("☠ Inferno Demonlord 10M❤"));
        assertEquals(3, classifyTier("☠ Inferno Demonlord 45M❤"));
        assertEquals(4, classifyTier("☠ Inferno Demonlord 150M❤"));
        assertEquals(0, classifyTier("☠ Riftstalker Bloodfiend 2400❤"));
    }

    @Test
    void nearbyBossTitleDoesNotTurnFamilyMinibossesIntoTheBoss() {
        SlayerPolicy.EntityDescriptor mini = SlayerPolicy.classifyHolograms(java.util.List.of(
                "Revenant Sycophant",
                "☠ Revenant Horror IV 1.5M❤",
                "Spawned by: LocalPlayer")).orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.MINIBOSS, mini.role());
        assertEquals("Revenant Sycophant", mini.displayName());
    }

    @Test
    void classifiesMinibossesDemonsOwnerAndAttunement() {
        SlayerPolicy.EntityDescriptor mini = SlayerPolicy.classifyTag(
                "Voidcrazed Maniac 12M❤", "Spawned by: ExamplePlayer").orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.MINIBOSS, mini.role());
        assertEquals(SlayerPolicy.SlayerType.VOIDGLOOM, mini.type());
        assertTrue(mini.bigMiniboss());
        assertEquals("ExamplePlayer", mini.owner());

        SlayerPolicy.EntityDescriptor demon = SlayerPolicy.classifyTag(
                "ⓆⓊⒶⓏⒾⒾ 25M❤ ASHEN ♨", "Owner: ExamplePlayer").orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.DEMON, demon.role());
        assertEquals(SlayerPolicy.SlayerType.INFERNO, demon.type());
        assertEquals(SlayerPolicy.Attunement.ASHEN, demon.attunement());

        assertFalse(SlayerPolicy.classifyTag("[Lv100] Random Zombie", "").isPresent());
        assertFalse(SlayerPolicy.classifyTag("We killed a Voidgloom Seraph earlier", "").isPresent());
        assertTrue(SlayerPolicy.isCombatNametag("☠ Revenant Horror I 500❤"));
        assertTrue(SlayerPolicy.shouldTrackLiveEntity(SlayerPolicy.classifyTag(
                "☠ Revenant Horror I 500❤", "Spawned by: LocalPlayer").orElseThrow()));
        assertFalse(SlayerPolicy.shouldTrackLiveEntity(SlayerPolicy.classifyTag(
                "☠ Revenant Horror I 500❤", "").orElseThrow()));
        assertEquals(java.util.List.of("02:46", "☠ Revenant Horror I 500❤"), SlayerPolicy.nametagHudLines(java.util.List.of(
                "Spawned by: LocalPlayer",
                "02:46",
                "☠ Revenant Horror I 500❤")));
    }

    @Test
    void parsesRareDropAndCocoonSignals() {
        SlayerPolicy.DropObservation drop = SlayerPolicy.dropObservation(
                "CRAZY RARE DROP! (Judgement Core)").orElseThrow();
        assertEquals("Judgement Core", drop.displayName());
        assertTrue(drop.rare());
        assertTrue(SlayerPolicy.isCocooned("YOU COCOONED YOUR SLAYER BOSS"));
    }

    @Test
    void parsesInsaneDropMessages() {
        SlayerPolicy.DropObservation drop = SlayerPolicy.dropObservation(
                "INSANE DROP! (Warden Heart)").orElseThrow();

        assertEquals("Warden Heart", drop.displayName());
        assertTrue(drop.rare());
    }

    private static void assertBoss(
            String tag,
            SlayerPolicy.SlayerType expected,
            int tier) {
        SlayerPolicy.EntityDescriptor descriptor =
                SlayerPolicy.classifyTag(tag, "Owner: ExamplePlayer").orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.BOSS, descriptor.role());
        assertEquals(expected, descriptor.type());
        assertEquals(tier, descriptor.tier());
        assertEquals("ExamplePlayer", descriptor.owner());
    }

    private static int classifyTier(String tag) {
        return SlayerPolicy.classifyTag(tag, "Owner: ExamplePlayer").orElseThrow().tier();
    }
}
