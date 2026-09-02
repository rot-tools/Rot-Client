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
        assertBoss("☠ Voidgloom Seraph IV 210M❤ 43 Hits", SlayerPolicy.SlayerType.VOIDGLOOM, 4);
        assertBoss("☠ Inferno Demonlord IV 150M❤", SlayerPolicy.SlayerType.INFERNO, 4);
        assertBoss("☠ Riftstalker Bloodfiend V 10M❤", SlayerPolicy.SlayerType.VAMPIRE, 5);
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
}
