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
                                "Click to despawn!",
                                "Held Item: Crochet Tiger Plushie"));

        assertTrue(snapshot.isPresent());
        assertEquals(200, snapshot.get().level());
        assertEquals("Golden Dragon", snapshot.get().name());
        assertEquals(
                "Crochet Tiger Plushie",
                snapshot.get().heldItem());
        assertEquals(
                "[Lvl 200]",
                snapshot.get().levelLabel());
    }

    @Test
    void enrichesPetAndHeldItemWithIndependentRarityColors() {
        PetHudPolicy.Snapshot base =
                PetHudPolicy.parse(
                                "[Lvl 91] Elephant",
                                List.of(
                                        "Held Item: Farming Exp Boost",
                                        "Progress to Level 92: 33.4%"))
                        .orElseThrow();

        PetHudPolicy.Snapshot enriched =
                PetHudPolicy.withRuntimeDetails(
                        base,
                        "{\"type\":\"ELEPHANT\","
                                + "\"exp\":12345678.25,"
                                + "\"tier\":\"EPIC\"}",
                        0,
                        ItemRarityPolicy.DEFAULT_LEGENDARY,
                        List.of(
                                "Held Item: Farming Exp Boost",
                                "Progress to Level 92: 33.4%"));

        assertEquals(
                ItemRarityPolicy.DEFAULT_EPIC,
                enriched.petColor());

        assertEquals(
                ItemRarityPolicy.DEFAULT_LEGENDARY,
                enriched.heldItemColor());

        assertEquals(
                33.4D,
                enriched.progressPercent(),
                0.001D);

        assertEquals(
                92,
                enriched.nextLevel());

        assertEquals(
                "33.4% to Lv 92",
                enriched.progressLabel());

        assertFalse(
                enriched.maxLevel());
    }

    @Test
    void detectsMaxLevelInsteadOfShowingProgressBar() {
        PetHudPolicy.Snapshot snapshot =
                PetHudPolicy.parse(
                                "[Lvl 100] Bee",
                                List.of(
                                        "Held Item: None",
                                        "MAX LEVEL"))
                        .orElseThrow();

        assertTrue(snapshot.maxLevel());
        assertFalse(snapshot.hasProgress());

        assertEquals(
                "MAX LEVEL",
                snapshot.progressLabel());

        assertEquals(
                100.0D,
                snapshot.progressPercent(),
                0.001D);
    }

    @Test
    void progressParserAcceptsHypixelProgressLore() {
        PetHudPolicy.Snapshot snapshot =
                PetHudPolicy.parse(
                                "[Lvl 52] Tiger",
                                List.of(
                                        "Progress to Level 53: 12.4%"))
                        .orElseThrow();

        assertTrue(snapshot.hasProgress());

        assertEquals(
                12.4D,
                snapshot.progressPercent(),
                0.001D);

        assertEquals(
                53,
                snapshot.nextLevel());
    }

    @Test
    void petTierUsesExistingRarityPalette() {
        assertEquals(
                ItemRarityPolicy.DEFAULT_COMMON,
                PetHudPolicy.petRarityColor(
                        "{\"tier\":\"COMMON\"}",
                        0));

        assertEquals(
                ItemRarityPolicy.DEFAULT_UNCOMMON,
                PetHudPolicy.petRarityColor(
                        "{\"tier\":\"UNCOMMON\"}",
                        0));

        assertEquals(
                ItemRarityPolicy.DEFAULT_RARE,
                PetHudPolicy.petRarityColor(
                        "{\"tier\":\"RARE\"}",
                        0));

        assertEquals(
                ItemRarityPolicy.DEFAULT_EPIC,
                PetHudPolicy.petRarityColor(
                        "{\"tier\":\"EPIC\"}",
                        0));

        assertEquals(
                ItemRarityPolicy.DEFAULT_LEGENDARY,
                PetHudPolicy.petRarityColor(
                        "{\"tier\":\"LEGENDARY\"}",
                        0));

        assertEquals(
                ItemRarityPolicy.DEFAULT_MYTHIC,
                PetHudPolicy.petRarityColor(
                        "{\"tier\":\"MYTHIC\"}",
                        0));
    }

    @Test
    void petExperienceParserStillSupportsCachedMetadata() {
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
                        "[Lvl 100] Bee",
                        List.of(
                                "Held Item: None"));

        assertTrue(snapshot.isPresent());
        assertEquals("Bee", snapshot.get().name());
        assertEquals("", snapshot.get().heldItem());
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

        assertTrue(labeled.isPresent());
        assertEquals(200, labeled.get().level());
        assertEquals(
                "Golden Dragon",
                labeled.get().name());

        Optional<PetHudPolicy.Snapshot> lvlOnly =
                PetHudPolicy.parseTabText(
                        "[Lvl 100] Bee");

        assertTrue(lvlOnly.isPresent());
        assertEquals(
                "Bee",
                lvlOnly.get().name());
    }

    @Test
    void tabRefreshPreservesRichGuiFields() {
        PetHudPolicy.Snapshot existing =
                new PetHudPolicy.Snapshot(
                        91,
                        "Elephant",
                        "Farming Exp Boost",
                        1234.0D,
                        ItemRarityPolicy.DEFAULT_EPIC,
                        ItemRarityPolicy.DEFAULT_RARE,
                        33.4D,
                        92,
                        false);

        PetHudPolicy.Snapshot tab =
                new PetHudPolicy.Snapshot(
                        92,
                        "Elephant",
                        "");

        PetHudPolicy.Snapshot merged =
                PetHudPolicy.mergeTabSnapshot(
                        existing,
                        tab);

        assertEquals(
                92,
                merged.level());

        assertEquals(
                ItemRarityPolicy.DEFAULT_EPIC,
                merged.petColor());

        assertEquals(
                ItemRarityPolicy.DEFAULT_RARE,
                merged.heldItemColor());

        assertEquals(
                33.4D,
                merged.progressPercent(),
                0.001D);
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