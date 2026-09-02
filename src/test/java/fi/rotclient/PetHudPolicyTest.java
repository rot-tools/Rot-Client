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
        Optional<PetHudPolicy.Snapshot> snapshot = PetHudPolicy.parse(
                "[Lvl 200] Golden Dragon",
                List.of(
                        "§7Click to despawn!",
                        "Held Item: Crochet Tiger Plushie"));
        assertTrue(snapshot.isPresent());
        assertEquals(200, snapshot.get().level());
        assertEquals("Golden Dragon", snapshot.get().name());
        assertEquals("Crochet Tiger Plushie", snapshot.get().heldItem());
        assertEquals("[Lvl 200]", snapshot.get().levelLabel());
    }

    @Test
    void noneHeldItemIsBlank() {
        Optional<PetHudPolicy.Snapshot> snapshot = PetHudPolicy.parse(
                "§7[Lvl 100] §6Bee",
                List.of("Held Item: None"));
        assertTrue(snapshot.isPresent());
        assertEquals("Bee", snapshot.get().name());
        assertEquals("", snapshot.get().heldItem());
    }

    @Test
    void emptyHoverIsRejected() {
        assertTrue(PetHudPolicy.parse("", List.of("Held Item: Book")).isEmpty());
    }

    @Test
    void tabTextFindsLabeledPetAndLvlLine() {
        Optional<PetHudPolicy.Snapshot> labeled = PetHudPolicy.parseTabText(
                "Skills: Combat 60\nPet: [Lvl 200] Golden Dragon\nSpeed: 400");
        assertTrue(labeled.isPresent());
        assertEquals(200, labeled.get().level());
        assertEquals("Golden Dragon", labeled.get().name());

        Optional<PetHudPolicy.Snapshot> lvlOnly = PetHudPolicy.parseTabText(
                "[Lvl 100] Bee");
        assertTrue(lvlOnly.isPresent());
        assertEquals("Bee", lvlOnly.get().name());
    }

    @Test
    void despawnAndSpawnedLoreCountAsEquipped() {
        assertTrue(PetHudPolicy.loreMeansEquipped(List.of("Click to despawn!")));
        assertTrue(PetHudPolicy.loreMeansEquipped(List.of("Click to despawn")));
        assertTrue(PetHudPolicy.loreMeansEquipped(List.of("Currently equipped")));
        assertFalse(PetHudPolicy.loreMeansEquipped(List.of("Click to summon!")));
    }
}
