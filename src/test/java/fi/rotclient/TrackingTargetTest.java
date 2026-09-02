package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrackingTargetTest {
    @Test
    void combinedTargetContainsMithrilAndTitaniumOnly() {
        TrackingTarget target = TrackingTarget.MITHRIL_TITANIUM;

        assertTrue(target.isCombined());
        assertTrue(target.includes(TrackedMaterial.MITHRIL));
        assertTrue(target.includes(TrackedMaterial.TITANIUM));
        assertFalse(target.includes(TrackedMaterial.TUNGSTEN));
        assertFalse(target.includes(TrackedMaterial.GOLD));
        assertEquals(2, target.materials().size());
    }

    @Test
    void legacyTargetIdsMapToCurrentCombinedTarget() {
        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                TrackingTarget.fromId("MITHRIL_TUNGSTEN"));
        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                TrackingTarget.fromId("MITHRIL"));
        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                TrackingTarget.fromId("TITANIUM"));
        assertSame(TrackingTarget.TUNGSTEN,
                TrackingTarget.fromId("TUNGSTEN"));
    }

    @Test
    void activeMaterialMappingIncludesStandaloneTungstenAndUmber() {
        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                TrackingTarget.forMaterial(TrackedMaterial.MITHRIL));
        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                TrackingTarget.forMaterial(TrackedMaterial.TITANIUM));
        assertSame(TrackingTarget.TUNGSTEN,
                TrackingTarget.forMaterial(TrackedMaterial.TUNGSTEN));
        assertSame(TrackingTarget.UMBER,
                TrackingTarget.forMaterial(TrackedMaterial.UMBER));
    }

    @Test
    void hardStoneAndCobblestoneAreNeverSelectableTargets() {
        assertNull(
                TrackingTarget.forMaterial(TrackedMaterial.HARD_STONE));
        assertNull(
                TrackingTarget.forMaterial(TrackedMaterial.COBBLESTONE));
    }

    @Test
    void singleMaterialTargetsRemainUnchanged() {
        assertSame(
                TrackingTarget.GOLD,
                TrackingTarget.fromId("GOLD"));
        assertSame(
                TrackingTarget.DIAMOND,
                TrackingTarget.fromId("DIAMOND"));
        assertFalse(TrackingTarget.GOLD.isCombined());
        assertFalse(TrackingTarget.DIAMOND.isCombined());
    }
}
