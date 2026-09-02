package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyBlockMiningMechanicsTest {
    private static final Set<String> OFFICIAL_MINING_COLLECTIONS = Set.of(
            "COAL", "COBBLESTONE", "DIAMOND", "EMERALD", "ENDER_STONE",
            "GEMSTONE_COLLECTION", "GLACITE", "GLOWSTONE_DUST", "GOLD_INGOT",
            "GRAVEL", "HARD_STONE", "ICE", "INK_SACK:4", "IRON_INGOT",
            "MITHRIL_ORE", "MYCEL", "NETHERRACK", "OBSIDIAN", "QUARTZ",
            "REDSTONE", "SAND", "SAND:1", "SULPHUR_ORE", "TUNGSTEN", "UMBER");

    @Test
    void bundledRegistryCoversAllCurrentOfficialMiningCollections() {
        SkyBlockMiningResourceRegistry registry =
                SkyBlockMiningResourceRegistry.bundled();
        Set<String> actual = registry.resources().stream()
                .filter(SkyBlockMiningResource::isCollection)
                .map(SkyBlockMiningResource::collectionId)
                .collect(Collectors.toSet());

        assertEquals("mining-resources.v2", registry.schemaVersion());
        assertEquals("2026-08-12", registry.reviewedAt());
        assertEquals(25, actual.size());
        assertEquals(OFFICIAL_MINING_COLLECTIONS, actual);
        assertEquals(27, registry.resources().size());
        assertTrue(registry.lookup("TITANIUM").isPresent());
        assertTrue(registry.lookup("STARFALL").isPresent());
    }

    @Test
    void identityBoundariesPreserveOfficialAndRotIds() {
        SkyBlockMiningResourceRegistry registry =
                SkyBlockMiningResourceRegistry.bundled();

        SkyBlockMiningResource iron = registry.lookup("IRON_INGOT").orElseThrow();
        assertEquals("IRON_ORE", iron.physicalBlockTokens().getFirst());
        assertEquals("IRON_INGOT", iron.hypixelItemId());
        assertEquals("IRON_INGOT", iron.bazaarProductId());

        SkyBlockMiningResource titanium = registry.lookup("TITANIUM").orElseThrow();
        assertEquals("TITANIUM", titanium.canonicalResourceId());
        assertEquals("TITANIUM_ORE", titanium.hypixelItemId());
        assertEquals("TITANIUM_ORE", titanium.bazaarProductId());

        SkyBlockMiningResource lapis = registry.lookup("LAPIS_LAZULI").orElseThrow();
        assertEquals("INK_SACK:4", lapis.collectionId());
        assertEquals("INK_SACK:4", lapis.hypixelItemId());
    }

    @Test
    void unknownMechanicsRemainNullableInsteadOfInvented() {
        SkyBlockMiningResource gravel = SkyBlockMiningResourceRegistry.bundled()
                .lookup("GRAVEL").orElseThrow();

        assertNull(gravel.droppedItemId());
        assertNull(gravel.bazaarProductId());
        assertNull(gravel.breakingPower());
        assertEquals(MiningFortuneCategory.UNKNOWN, gravel.fortuneCategory());
    }

    @Test
    void reviewedDwarvenMetalMechanicsPreserveOfficialValues() {
        SkyBlockMiningResourceRegistry registry =
                SkyBlockMiningResourceRegistry.bundled();

        assertMechanics(registry, "MITHRIL", 4, 800);
        assertMechanics(registry, "TITANIUM", 5, 2_000);
        assertMechanics(registry, "TUNGSTEN", 9, 5_600);
        assertMechanics(registry, "UMBER", 9, 5_600);
        assertMechanics(registry, "GLACITE", 9, 6_000);
    }

    @Test
    void glaciteMetalRegistryContainsEveryRuntimeBlockVariant() {
        SkyBlockMiningResourceRegistry registry =
                SkyBlockMiningResourceRegistry.bundled();

        assertEquals(
                Set.of(
                        "INFESTED_COBBLESTONE",
                        "COBBLESTONE",
                        "COBBLESTONE_SLAB",
                        "COBBLESTONE_STAIRS",
                        "CLAY"),
                Set.copyOf(registry.lookup("TUNGSTEN").orElseThrow()
                        .physicalBlockTokens()));
        assertEquals(
                Set.of(
                        "TERRACOTTA",
                        "BROWN_TERRACOTTA",
                        "SMOOTH_RED_SANDSTONE"),
                Set.copyOf(registry.lookup("UMBER").orElseThrow()
                        .physicalBlockTokens()));
    }

    @Test
    void hardStoneRequiresAReviewedMiningContext() {
        assertTrue(MiningBlockEvidencePolicy.allows(
                "HARD_STONE", SkyBlockLocation.of(SkyBlockArea.CRYSTAL_HOLLOWS)));
        assertTrue(MiningBlockEvidencePolicy.allows(
                "HARD_STONE", SkyBlockLocation.of(SkyBlockArea.GLACITE_TUNNELS)));
        assertTrue(MiningBlockEvidencePolicy.allows(
                "HARD_STONE", SkyBlockLocation.of(SkyBlockArea.GLACITE_MINESHAFT)));

        assertFalse(MiningBlockEvidencePolicy.allows(
                "HARD_STONE", SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES)));
        assertFalse(MiningBlockEvidencePolicy.allows(
                "HARD_STONE", SkyBlockLocation.UNKNOWN));

        // The mechanics registry remains metadata, never a global allow-list.
        assertTrue(MiningBlockEvidencePolicy.allows(
                "UNSEEN_FUTURE_RESOURCE", SkyBlockLocation.UNKNOWN));
    }

    @Test
    void malformedRangesAndUnknownProvenanceAreRejected() {
        String invalidRange = """
                {
                  "sources": {"official": "https://api.hypixel.net/"},
                  "resources": [{
                    "canonicalResourceId": "TEST",
                    "baseYieldMin": 2,
                    "baseYieldMax": 1,
                    "provenanceRefs": ["official"]
                  }]
                }
                """;
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SkyBlockMiningResourceRegistry.parse(
                        new StringReader(invalidRange)));

        String unknownRef = """
                {
                  "sources": {},
                  "resources": [{
                    "canonicalResourceId": "TEST",
                    "provenanceRefs": ["missing"]
                  }]
                }
                """;
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SkyBlockMiningResourceRegistry.parse(
                        new StringReader(unknownRef)));
    }

    @Test
    void sharedScanPlanRetainsCoverageWithOneSixthWorldReads() {
        assertEquals(2_475, MiningBreakScanPlan.POSITIONS_PER_FRAME);
        assertEquals(14_850, MiningBreakScanPlan.legacyWorldReads(6));
        assertEquals(2_475, MiningBreakScanPlan.sharedWorldReads(6));
        assertEquals(0, MiningBreakScanPlan.sharedWorldReads(0));
    }

    private static void assertMechanics(
            SkyBlockMiningResourceRegistry registry,
            String resourceId,
            int breakingPower,
            int blockStrength) {
        SkyBlockMiningResource resource = registry.lookup(resourceId).orElseThrow();
        assertEquals(breakingPower, resource.breakingPower());
        assertEquals(blockStrength, resource.blockStrength());
        assertEquals(MiningFortuneCategory.DWARVEN_METAL, resource.fortuneCategory());
    }
}
