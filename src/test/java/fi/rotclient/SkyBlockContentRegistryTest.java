package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyBlockContentRegistryTest {
    @Test
    void builtinKnowsDeepCavernsVanillaLookingOresFromResearch() {
        SkyBlockContentRegistry registry = SkyBlockContentRegistry.builtin();
        assertTrue(registry.isKnown("COAL_ORE"));
        assertTrue(registry.isKnown("IRON_ORE"));
        assertTrue(registry.isKnown("LAPIS_LAZULI"));
        assertTrue(registry.isKnown("REDSTONE"));
        assertEquals(
                "COAL_ORE",
                registry.lookupAlias("Coal").orElseThrow().id());
        assertEquals(
                "IRON_ORE",
                registry.lookupAlias("Iron Ingot").orElseThrow().id());
        assertEquals(
                "REDSTONE",
                registry.lookupAlias("Redstone Dust").orElseThrow().id());
        // Bare STONE must not silently become Hard Stone.
        assertTrue(registry.lookupAlias("STONE").isEmpty()
                || !"HARD_STONE".equals(
                registry.lookupAlias("STONE").map(SkyBlockContentRegistry.KnownItem::id)
                        .orElse("")));
        assertTrue(registry.lookup("HARD_STONE").isPresent());
    }

    @Test
    void builtinKnowsCoreMiningMaterials() {
        SkyBlockContentRegistry registry = SkyBlockContentRegistry.builtin();
        assertTrue(registry.isKnown("GOLD_INGOT"));
        assertTrue(registry.isKnown("MITHRIL_ORE"));
        assertTrue(registry.lookup("DIAMOND").isPresent());
        assertEquals(
                "Gold Ingot",
                registry.lookup("GOLD_INGOT").orElseThrow().displayName());
    }

    @Test
    void builtinKnowsHardStoneAndCobblestoneFillerBlocks() {
        SkyBlockContentRegistry registry = SkyBlockContentRegistry.builtin();
        assertTrue(registry.isKnown("HARD_STONE"));
        assertTrue(registry.isKnown("COBBLESTONE"));
        assertTrue(registry.isKnown("ENCHANTED_COBBLESTONE"));
        assertTrue(registry.lookupAlias("Cobblestone").isPresent());
        assertEquals(
                "Cobblestone",
                registry.lookup("COBBLESTONE").orElseThrow().displayName());
    }

    @Test
    void builtinKnowsGlaciteFamilyResourcesFromResearchEnrichment() {
        SkyBlockContentRegistry registry = SkyBlockContentRegistry.builtin();
        assertTrue(registry.isKnown("GLACITE"));
        assertTrue(registry.isKnown("ENCHANTED_GLACITE"));
        assertTrue(registry.isKnown("UMBER"));
        assertTrue(registry.isKnown("ENCHANTED_UMBER"));
        assertTrue(registry.isKnown("ENCHANTED_TUNGSTEN"));
        assertTrue(registry.isKnown("GLACITE_POWDER"));
        assertEquals(
                SessionSourceType.CURRENCY.name(),
                registry.lookup("GLACITE_POWDER").orElseThrow().sourceHint());
        // Unknown stable IDs still return empty — catalog is enrichment, not
        // an observation allow-list.
        assertTrue(registry.lookup("TOTALLY_UNKNOWN_STABLE_ORE").isEmpty());
    }

    @Test
    void unknownLookupIsEmptyNeverThrows() {
        SkyBlockContentRegistry registry = SkyBlockContentRegistry.builtin();
        assertTrue(registry.lookup("TOTALLY_FAKE_ITEM").isEmpty());
        assertTrue(registry.lookupAlias("not a real alias").isEmpty());
        assertTrue(registry.lookup(null).isEmpty());
        assertFalse(registry.isKnown(""));
    }

    @Test
    void aliasLookupFindsDisplayNames() {
        SkyBlockContentRegistry registry = SkyBlockContentRegistry.builtin();
        assertTrue(registry.lookupAlias("Gold Ingot").isPresent());
        assertTrue(registry.lookupAlias("Rough Ruby Gemstone").isPresent());
    }

    @Test
    void duplicateIdsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new SkyBlockContentRegistry(List.of(
                        new SkyBlockContentRegistry.KnownItem(
                                "A", "A", List.of("one"), null, List.of(), null),
                        new SkyBlockContentRegistry.KnownItem(
                                "A", "A2", List.of("two"), null, List.of(), null))));
    }

    @Test
    void duplicateAliasesRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new SkyBlockContentRegistry(List.of(
                        new SkyBlockContentRegistry.KnownItem(
                                "A", "A", List.of("shared"), null, List.of(), null),
                        new SkyBlockContentRegistry.KnownItem(
                                "B", "B", List.of("shared"), null, List.of(), null))));
    }
}
