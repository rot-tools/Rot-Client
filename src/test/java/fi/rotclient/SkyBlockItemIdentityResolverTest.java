package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SkyBlockItemIdentityResolverTest {
    @Test
    void knownHardStoneResolvesWithCatalogMatch() {
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromTextToken("Hard Stone");
        assertEquals("HARD_STONE", resolved.stableId());
        assertEquals("HARD_STONE", resolved.hypixelId());
        assertTrue(resolved.catalogMatch());
        assertEquals("HIGH", resolved.confidence());
        assertEquals("HARD_STONE", resolved.bazaarId());
    }

    @Test
    void unknownDisplayIsPreservedNotDropped() {
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromTextToken("Something New Ore");
        assertFalse(resolved.catalogMatch());
        assertEquals("SOMETHING_NEW_ORE", resolved.stableId());
        assertEquals("Something New Ore", resolved.displayName());
        assertEquals("UNKNOWN", resolved.hypixelId());
        assertEquals("LOW", resolved.confidence());
    }

    @Test
    void materialHintEnrichesVanillaAndBazaar() {
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromMaterialHint(
                        "MITHRIL",
                        "Mithril",
                        "minecraft:prismarine_crystals",
                        "MITHRIL_ORE");
        assertTrue(resolved.catalogMatch()
                || "MITHRIL".equals(resolved.stableId())
                || "MITHRIL_ORE".equals(resolved.stableId()));
        assertFalse(resolved.stableId().isBlank());
    }

    @Test
    void reviewedMiningRegistryCompletesEmeraldAndQuartzDiagnostics() {
        SkyBlockItemIdentityResolver.Resolved emerald =
                SkyBlockItemIdentityResolver.fromTextToken("Emerald");
        SkyBlockItemIdentityResolver.Resolved quartz =
                SkyBlockItemIdentityResolver.fromTextToken("Nether Quartz");

        assertTrue(emerald.catalogMatch());
        assertEquals("EMERALD", emerald.stableId());
        assertEquals("EMERALD", emerald.hypixelId());
        assertEquals("EMERALD", emerald.bazaarId());

        assertTrue(quartz.catalogMatch());
        assertEquals("NETHER_QUARTZ", quartz.stableId());
        assertEquals("QUARTZ", quartz.hypixelId());
        assertEquals("QUARTZ", quartz.bazaarId());
    }
}
