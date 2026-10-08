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
                -1.0D,
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

    @Test
    void cosmeticLevelsAndSkinsDoNotChangePetIdentity() {
        PetHudPolicy.Snapshot gui = PetHudPolicy.parse(
                "⭐ [Lvl 200] [1,234✦] Golden Dragon ✦",
                List.of("Held Item: Lucky Clover", "MAX LEVEL")).orElseThrow();
        assertEquals("Golden Dragon", gui.name());
        var tab = PetHudPolicy.parseTabText("Pet:\n[Lvl 200] [1,235✦] Golden Dragon").orElseThrow();
        assertEquals("Lucky Clover", PetHudPolicy.mergeTabSnapshot(gui, tab).heldItem());
        assertTrue(PetHudPolicy.mergeTabSnapshot(gui, tab).maxLevel());
        // Older cache entries used the decorated name; they must migrate without losing XP.
        var oldCache = new PetHudPolicy.Snapshot(200, "[1,234✦] Golden Dragon ✦", "Textbook");
        assertEquals("Textbook", PetHudPolicy.mergeTabSnapshot(oldCache, tab).heldItem());
        assertTrue(PetHudPolicy.samePetIdentity(oldCache, tab));
        assertFalse(PetHudPolicy.samePetIdentity(oldCache, new PetHudPolicy.Snapshot(200, "Rose Dragon", "")));
        assertFalse(PetHudPolicy.samePetIdentity(null, tab));
    }

    @Test
    void newNamesAndSpecialPhoenixRaritiesNeedNoPetRegistryUpdate() {
        assertEquals("Eagle", PetHudPolicy.parseTabText("Pet: [Lvl 12] Eagle").orElseThrow().name());
        assertEquals("T-Rex", PetHudPolicy.parseTabText("Pet: [Lvl 67] T-Rex ✦").orElseThrow().name());
        assertEquals(ItemRarityPolicy.DEFAULT_SPECIAL,
                PetHudPolicy.petRarityColor("{\"tier\":\"SPECIAL\"}", 0));
        assertEquals(ItemRarityPolicy.DEFAULT_SPECIAL,
                PetHudPolicy.petRarityColor("{\"tier\":\"VERY_SPECIAL\"}", 0));
        assertFalse(PetHudPolicy.parseTabText("Pet: [Lvl 12] Eagle").orElseThrow().hasProgress());
    }

    @Test
    void serverPercentageWinsOverRoundedXpAbbreviations() {
        var pet = PetHudPolicy.parseTabText(
                "Pet:\n[Lvl 70] Rabbit\n931,886.2/1.4M XP (67.2%)").orElseThrow();
        assertEquals(67.2D, pet.progressPercent(), 0.001D);
        assertEquals(71, pet.nextLevel());
    }

    @Test
    void otherWidgetsCannotSupplyPetProgressOrPetNames() {
        var pet = PetHudPolicy.parseTabText(
                "Pet:\n[Lvl 70] Rabbit\nGarden Level:\nXP: 75%").orElseThrow();
        assertFalse(pet.hasProgress());
        assertTrue(PetHudPolicy.parseTabText("Pet:\nPlayers:\n[Lvl 50] Someone").isEmpty());
        assertFalse(PetHudPolicy.parseTabText("Pet:\n[Lvl 70] Rabbit\n+123,456 XP").orElseThrow().hasProgress());
    }

    @Test
    void explicitNoPetIsDifferentFromAnAbsentOrUnpopulatedWidget() {
        assertTrue(PetHudPolicy.tabReportsNoPet("Pet: None"));
        assertTrue(PetHudPolicy.tabReportsNoPet("Pet:\nNo pet selected\nSkills:"));
        assertTrue(PetHudPolicy.tabReportsNoPet("Active Pet:\n✖"));
        assertFalse(PetHudPolicy.tabReportsNoPet("Pet:\n\nSkills:"));
        assertFalse(PetHudPolicy.tabReportsNoPet("Skills:\nNone"));
        assertFalse(PetHudPolicy.tabReportsNoPet(""));
        assertFalse(PetHudPolicy.tabReportsNoPet(null));
        assertTrue(PetHudPolicy.parseTabText("Pet:\nNo pet selected").isEmpty());
    }

    @Test
    void malformedServerLevelsCannotThrowOrOverflowNextLevel() {
        assertTrue(PetHudPolicy.parse("[Lvl 99999999999999] Bee", List.of()).isEmpty());
        assertFalse(PetHudPolicy.parseTabText(
                "Pet: [Lvl 2,147,483,647] Bee\nXP: 10%").orElseThrow().hasProgress());
    }

    @Test
    void globalPetSummaryPreservesSelectedPetAcrossOtherPages() {
        var summary = PetHudPolicy.parseMenuSummary(List.of(
                "Pet Score: 150", "Selected pet: Rabbit ✦", "Progress to Level 71: 42.5%",
                "42.5k/100k", "Click to view!")).orElseThrow();
        assertEquals("Rabbit", summary.name());
        assertEquals(70, summary.level());
        assertEquals(42.5D, summary.progressPercent(), 0.001D);
        assertEquals(-1.0D, summary.experience()); // Rounded per-level XP is not total XP.
        var cached = new PetHudPolicy.Snapshot(70, "Rabbit", "Textbook", 1234,
                ItemRarityPolicy.DEFAULT_EPIC, ItemRarityPolicy.DEFAULT_RARE, 20, 71, false);
        var merged = PetHudPolicy.mergeMenuSummary(cached, summary);
        assertEquals("Textbook", merged.heldItem());
        assertEquals(42.5D, merged.progressPercent(), 0.001D);
        assertEquals(ItemRarityPolicy.DEFAULT_EPIC, merged.petColor());
    }

    @Test
    void absentPetOnFilteredPageIsNotAnExplicitDespawn() {
        assertTrue(PetHudPolicy.parseMenuSummary(List.of("Pet Score: 150", "Click to view!")).isEmpty());
        assertFalse(PetHudPolicy.menuReportsNoPet(List.of("Pet Score: 150", "Click to view!")));
        assertFalse(PetHudPolicy.menuReportsNoPet(List.of("Selected pet:")));
        assertFalse(PetHudPolicy.menuReportsNoPet(null));
        assertTrue(PetHudPolicy.menuReportsNoPet(List.of("§7Selected pet: §cNone")));
        assertTrue(PetHudPolicy.parseMenuSummary(List.of("Selected pet: None")).isEmpty());
    }

    @Test
    void maxLevelSummaryDoesNotInventAPetSpecificMaximum() {
        var summary = PetHudPolicy.parseMenuSummary(List.of("Selected pet: Golden Dragon", "MAX LEVEL")).orElseThrow();
        assertTrue(summary.maxLevel());
        assertEquals(-1, summary.level());
        assertFalse(summary.hasProgress());
    }

    @Test
    void authoritativeRarityChangeCannotReuseAnotherPetsHeldItemOrXp() {
        var old = new PetHudPolicy.Snapshot(70, "Rabbit", "Textbook", 1234,
                ItemRarityPolicy.DEFAULT_EPIC, ItemRarityPolicy.DEFAULT_RARE, 20, 71, false);
        var summary = PetHudPolicy.withRuntimeDetails(
                PetHudPolicy.parseMenuSummary(List.of("Selected pet: Rabbit", "Progress to Level 71: 42.5%")).orElseThrow(),
                "{\"tier\":\"LEGENDARY\"}", 0, 0, List.of());
        assertFalse(PetHudPolicy.sameMenuSummaryPet(old, summary));
        var merged = PetHudPolicy.mergeMenuSummary(old, summary);
        assertEquals("", merged.heldItem());
        assertEquals(-1.0D, merged.experience());
        assertEquals(ItemRarityPolicy.DEFAULT_LEGENDARY, merged.petColor());
    }

    @Test
    void exactPetUuidConfirmsTheSelectedIndividualRatherThanJustTheName() {
        String compact = "{\"uuid\":\"0123456789abcdef0123456789abcdef\"}";
        String dashed = "{\"uuid\":\"01234567-89ab-cdef-0123-456789abcdef\"}";
        String other = "{\"uuid\":\"11234567-89ab-cdef-0123-456789abcdef\"}";
        assertTrue(PetHudPolicy.sameExactPetUuid(compact, dashed));
        assertFalse(PetHudPolicy.sameExactPetUuid(compact, other));
        assertFalse(PetHudPolicy.sameExactPetUuid("{}", "{}"));
        assertFalse(PetHudPolicy.sameExactPetUuid(compact, null));
        assertTrue(PetHudPolicy.petUuid("{\"uuid\":true}").isEmpty());
        assertTrue(PetHudPolicy.petUuid("{\"uuid\":\"1-1-1-1-1\"}").isEmpty());
        assertTrue(PetHudPolicy.petUuid("bad json").isEmpty());
        assertTrue(PetHudPolicy.petUuid("{\"nested\":{\"uuid\":\"0123456789abcdef0123456789abcdef\"}}").isEmpty());
    }

    @Test
    void negativeOrInstructionalLoreDoesNotMeanEquipped() {
        assertFalse(PetHudPolicy.loreMeansEquipped(List.of("Not currently equipped")));
        assertFalse(PetHudPolicy.loreMeansEquipped(List.of("Click to summon, then click again to despawn")));
        assertTrue(PetHudPolicy.loreMeansEquipped(List.of("This pet is spawned!")));
        assertTrue(PetHudPolicy.loreMeansEquipped(List.of("\u00a0§e▸ Click to despawn! §9✦\u200b")));
    }

    @Test
    void namedHeadsAndOtherItemsCannotMasqueradeAsMenuPets() {
        assertFalse(PetHudPolicy.isPetMenuItem("", "", "[Lvl 70] Rabbit"));
        assertFalse(PetHudPolicy.isPetMenuItem("CUSTOM_HEAD", "{\"type\":\"RABBIT\",\"tier\":\"EPIC\"}", "[Lvl 70] Rabbit"));
        assertTrue(PetHudPolicy.isPetMenuItem("PET", "", "[Lvl 70] Rabbit ✦"));
        assertTrue(PetHudPolicy.isPetMenuItem("", "{\"type\":\"RABBIT\",\"tier\":\"EPIC\"}", "[Lvl 70] Rabbit"));
        assertFalse(PetHudPolicy.isPetMenuItem("PET", "", "Pets"));
        assertFalse(PetHudPolicy.isPetMenuItem("", "{\"type\":true,\"tier\":\"EPIC\"}", "[Lvl 70] Rabbit"));
    }
}
