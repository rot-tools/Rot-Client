package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class PetHudPolicyTest {
    @Test
    void parsesEquippedGoldenDragonHudFields() {
        Optional<PetHudPolicy.Snapshot> snapshot =
                PetHudPolicy.parse(
                        "[Lvl 200] Golden Dragon",
                        List.of(
                                "§7Click to despawn!",
                                "Held Item: Crochet Tiger Plushie"));

        assertTrue(
                snapshot.isPresent());

        assertEquals(
                200,
                snapshot.get().level());

        assertEquals(
                "Golden Dragon",
                snapshot.get().name());

        assertEquals(
                "Crochet Tiger Plushie",
                snapshot.get().heldItem());

        assertEquals(
                "[Lvl 200]",
                snapshot.get().levelLabel());
    }

    @Test
    void enrichesSnapshotWithPetExperienceAndHeldItemRarity() {
        PetHudPolicy.Snapshot base =
                PetHudPolicy.parse(
                                "[Lvl 200] Golden Dragon",
                                List.of(
                                        "Held Item: Crochet Tiger Plushie"))
                        .orElseThrow();

        PetHudPolicy.Snapshot enriched =
                PetHudPolicy.withRuntimeDetails(
                        base,
                        "{\"type\":\"GOLDEN_DRAGON\","
                                + "\"exp\":12345678.25,"
                                + "\"tier\":\"LEGENDARY\"}",
                        ItemRarityPolicy.DEFAULT_LEGENDARY);

        assertEquals(
                12345678.25D,
                enriched.experience(),
                0.001D);

        assertEquals(
                "XP: 12,345,678",
                enriched.experienceLabel());

        assertEquals(
                ItemRarityPolicy.DEFAULT_LEGENDARY,
                enriched.heldItemColor());
    }

    @Test
    void petExperienceParserSupportsExponentNotation() {
        assertEquals(
                12500000.0D,
                PetHudPolicy.parseExperience(
                        "{\"exp\":1.25E7}"),
                0.001D);

        assertEquals(
                -1.0D,
                PetHudPolicy.parseExperience(
                        "{}"),
                0.001D);
    }

    @Test
    void heldItemColorUsesExistingRarityPaletteOnly() {
        assertEquals(
                ItemRarityPolicy.DEFAULT_UNCOMMON,
                PetHudPolicy.normalizeRarityColor(
                        ItemRarityPolicy.DEFAULT_UNCOMMON));

        assertEquals(
                ItemRarityPolicy.DEFAULT_RARE,
                PetHudPolicy.normalizeRarityColor(
                        ItemRarityPolicy.DEFAULT_RARE));

        assertEquals(
                ItemRarityPolicy.DEFAULT_EPIC,
                PetHudPolicy.normalizeRarityColor(
                        ItemRarityPolicy.DEFAULT_EPIC));

        assertEquals(
                ItemRarityPolicy.DEFAULT_LEGENDARY,
                PetHudPolicy.normalizeRarityColor(
                        ItemRarityPolicy.DEFAULT_LEGENDARY));

        assertEquals(
                ItemRarityPolicy.DEFAULT_MYTHIC,
                PetHudPolicy.normalizeRarityColor(
                        ItemRarityPolicy.DEFAULT_MYTHIC));

        assertEquals(
                ItemRarityPolicy.DEFAULT_DIVINE,
                PetHudPolicy.normalizeRarityColor(
                        ItemRarityPolicy.DEFAULT_DIVINE));

        assertEquals(
                ItemRarityPolicy.DEFAULT_SPECIAL,
                PetHudPolicy.normalizeRarityColor(
                        ItemRarityPolicy.DEFAULT_SPECIAL));

        assertEquals(
                PetHudPolicy.HELD_ITEM_FALLBACK_COLOR,
                PetHudPolicy.normalizeRarityColor(
                        0xFF123456));
    }

    @Test
    void noneHeldItemIsBlank() {
        Optional<PetHudPolicy.Snapshot> snapshot =
                PetHudPolicy.parse(
                        "§7[Lvl 100] §6Bee",
                        List.of(
                                "Held Item: None"));

        assertTrue(
                snapshot.isPresent());

        assertEquals(
                "Bee",
                snapshot.get().name());

        assertEquals(
                "",
                snapshot.get().heldItem());
    }

    @Test
    void emptyHoverIsRejected() {
        assertTrue(
                PetHudPolicy
                        .parse(
                                "",
                                List.of(
                                        "Held Item: Book"))
                        .isEmpty());
    }

    @Test
    void tabTextFindsLabeledPetAndLvlLine() {
        Optional<PetHudPolicy.Snapshot> labeled =
                PetHudPolicy.parseTabText(
                        "Skills: Combat 60\n"
                                + "Pet: [Lvl 200] Golden Dragon\n"
                                + "Speed: 400");

        assertTrue(
                labeled.isPresent());

        assertEquals(
                200,
                labeled.get().level());

        assertEquals(
                "Golden Dragon",
                labeled.get().name());

        Optional<PetHudPolicy.Snapshot> lvlOnly =
                PetHudPolicy.parseTabText(
                        "[Lvl 100] Bee");

        assertTrue(
                lvlOnly.isPresent());

        assertEquals(
                "Bee",
                lvlOnly.get().name());
    }

    @Test
    void despawnAndSpawnedLoreCountAsEquipped() {
        assertTrue(
                PetHudPolicy.loreMeansEquipped(
                        List.of(
                                "Click to despawn!")));

        assertTrue(
                PetHudPolicy.loreMeansEquipped(
                        List.of(
                                "Click to despawn")));

        assertTrue(
                PetHudPolicy.loreMeansEquipped(
                        List.of(
                                "Currently equipped")));

        assertFalse(
                PetHudPolicy.loreMeansEquipped(
                        List.of(
                                "Click to summon!")));
    }
}