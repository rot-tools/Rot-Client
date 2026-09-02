package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningContextEvidencePolicyTest {
    @Test
    void reviewedDwarvenResourcesRejectUnknownAndUnrelatedAreas() {
        for (String resource : new String[]{"MITHRIL", "TITANIUM"}) {
            assertTrue(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES)));
            assertTrue(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.of(SkyBlockArea.CRYSTAL_HOLLOWS)));
            assertFalse(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.UNKNOWN));
            assertFalse(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.of(SkyBlockArea.GLACITE_TUNNELS)));
        }
    }

    @Test
    void explicitlySelectedTargetIsNeverLocationGated() {
        for (String resource : new String[]{"MITHRIL", "TITANIUM"}) {
            for (SkyBlockArea area : SkyBlockArea.values()) {
                assertTrue(MiningBlockEvidencePolicy.allows(
                                resource,
                                SkyBlockLocation.of(area),
                                true),
                        resource + " selected in " + area.id());
            }
            assertTrue(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.UNKNOWN, true));
        }

        // The same ambiguous vanilla block states remain area-gated when they
        // are merely candidates for Current Session OTHERS.
        assertFalse(MiningBlockEvidencePolicy.allows(
                "MITHRIL", SkyBlockLocation.UNKNOWN, false));
        assertFalse(MiningBlockEvidencePolicy.allows(
                "TITANIUM",
                SkyBlockLocation.of(SkyBlockArea.GLACITE_TUNNELS),
                false));
    }

    @Test
    void reviewedGlaciteResourcesRequireGlaciteContext() {
        for (String resource : new String[]{"TUNGSTEN", "UMBER"}) {
            assertTrue(MiningBlockEvidencePolicy.allows(
                    resource,
                    SkyBlockLocation.of(SkyBlockArea.DWARVEN_BASE_CAMP)));
            assertTrue(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.of(SkyBlockArea.GLACITE_TUNNELS)));
            assertTrue(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.of(SkyBlockArea.GLACITE_MINESHAFT)));
            assertFalse(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES)));
            assertFalse(MiningBlockEvidencePolicy.allows(
                    resource, SkyBlockLocation.UNKNOWN));
        }
        assertTrue(MiningBlockEvidencePolicy.allows(
                "GLACITE", SkyBlockLocation.of(SkyBlockArea.GREAT_GLACITE_LAKE)));
    }

    @Test
    void ordinaryCobblestoneAndUnknownResourcesAreNotCatalogGated() {
        assertTrue(MiningBlockEvidencePolicy.allows(
                "COBBLESTONE", SkyBlockLocation.UNKNOWN));
        assertTrue(MiningBlockEvidencePolicy.allows(
                "FUTURE_UNKNOWN_RESOURCE", SkyBlockLocation.UNKNOWN));
        assertFalse(MiningBlockEvidencePolicy.allows(
                "HARD_STONE", SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES)));
    }
}
