package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrackerSelectionTest {
    @Test
    void materialIdsRemainUnchanged() {
        assertEquals("GOLD", TrackerSelection.GOLD.id());
        assertEquals("DIAMOND", TrackerSelection.DIAMOND.id());
        assertEquals("MITHRIL_TITANIUM", TrackerSelection.MITHRIL_TITANIUM.id());
        assertEquals("TUNGSTEN", TrackerSelection.TUNGSTEN.id());
        assertEquals("UMBER", TrackerSelection.UMBER.id());
    }

    @Test
    void materialSelectionsWrapExpectedTargets() {
        assertSame(TrackingTarget.GOLD, TrackerSelection.GOLD.materialTarget());
        assertSame(TrackingTarget.DIAMOND, TrackerSelection.DIAMOND.materialTarget());
        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                TrackerSelection.MITHRIL_TITANIUM.materialTarget());
    }

    @Test
    void everyGemstoneTypeHasExactlyOneSelection() {
        Set<TrackerSelection> seen = new LinkedHashSet<>();

        for (GemstoneType gemstone : GemstoneType.values()) {
            TrackerSelection selection = TrackerSelection.forGemstone(gemstone);
            assertFalse(seen.contains(selection));
            seen.add(selection);
        }

        assertEquals(GemstoneType.values().length, seen.size());
    }

    @Test
    void gemstoneSelectionIdsUseGemstonePrefix() {
        for (GemstoneType gemstone : GemstoneType.values()) {
            TrackerSelection selection = TrackerSelection.forGemstone(gemstone);
            assertTrue(selection.id().startsWith("GEMSTONE_"));
            assertEquals(
                    "GEMSTONE_" + gemstone.id(),
                    selection.id());
        }
    }

    @Test
    void idRoundTripsForEverySelection() {
        for (TrackerSelection selection : TrackerSelection.values()) {
            assertSame(selection, TrackerSelection.fromId(selection.id()));
        }
    }

    @Test
    void fromIdIsCaseInsensitive() {
        assertSame(TrackerSelection.GOLD, TrackerSelection.fromId("gold"));
        assertSame(TrackerSelection.GOLD, TrackerSelection.fromId("GOLD"));
        assertSame(
                TrackerSelection.MITHRIL_TITANIUM,
                TrackerSelection.fromId("mithril_titanium"));
        assertSame(
                TrackerSelection.forGemstone(GemstoneType.JADE),
                TrackerSelection.fromId("gemstone_jade"));
    }

@Test
void materialSelectionsSupportLiveTracking() {
    assertTrue(TrackerSelection.GOLD.supportsLiveTracking());
    assertTrue(TrackerSelection.DIAMOND.supportsLiveTracking());
    assertTrue(TrackerSelection.MITHRIL_TITANIUM.supportsLiveTracking());
}


    @Test
    void unknownOrBlankIdsFallBackToGold() {
        assertSame(TrackerSelection.GOLD, TrackerSelection.fromId(null));
        assertSame(TrackerSelection.GOLD, TrackerSelection.fromId(""));
        assertSame(TrackerSelection.GOLD, TrackerSelection.fromId("   "));
        assertSame(TrackerSelection.GOLD, TrackerSelection.fromId("unknown"));
    }

    @Test
    void legacyMithrilAliasesMapToMithrilTitanium() {
        assertSame(
                TrackerSelection.MITHRIL_TITANIUM,
                TrackerSelection.fromId("MITHRIL_TUNGSTEN"));
        assertSame(
                TrackerSelection.MITHRIL_TITANIUM,
                TrackerSelection.fromId("MITHRIL"));
        assertSame(
                TrackerSelection.MITHRIL_TITANIUM,
                TrackerSelection.fromId("TITANIUM"));
        assertSame(TrackerSelection.TUNGSTEN,
                TrackerSelection.fromId("TUNGSTEN"));
    }

    @Test
    void materialSelectionsNeverExposeGemstones() {
        assertNull(TrackerSelection.GOLD.gemstone());
        assertNull(TrackerSelection.DIAMOND.gemstone());
        assertNull(TrackerSelection.MITHRIL_TITANIUM.gemstone());
    }

    @Test
    void gemstoneSelectionsNeverExposeMaterialTargets() {
        for (GemstoneType gemstone : GemstoneType.values()) {
            TrackerSelection selection = TrackerSelection.forGemstone(gemstone);
            assertNull(selection.materialTarget());
            assertTrue(selection.isGemstone());
            assertFalse(selection.isMaterial());
        }
    }

    @Test
    void forMaterialAndForGemstoneReturnExpectedSelections() {
        assertSame(TrackerSelection.GOLD, TrackerSelection.forMaterial(TrackingTarget.GOLD));
        assertSame(TrackerSelection.DIAMOND, TrackerSelection.forMaterial(TrackingTarget.DIAMOND));
        assertSame(
                TrackerSelection.MITHRIL_TITANIUM,
                TrackerSelection.forMaterial(TrackingTarget.MITHRIL_TITANIUM));
        assertSame(
                TrackerSelection.forGemstone(GemstoneType.RUBY),
                TrackerSelection.forGemstone(GemstoneType.RUBY));
    }

    @Test
    void forMaterialFallsBackToGoldAndForGemstoneRejectsNull() {
        assertSame(TrackerSelection.GOLD, TrackerSelection.forMaterial(null));
        assertThrows(IllegalArgumentException.class, () -> TrackerSelection.forGemstone(null));
    }

    @Test
    void everySelectionIdIsUnique() {
        Set<String> ids = new LinkedHashSet<>();
        for (TrackerSelection selection : TrackerSelection.values()) {
            assertTrue(ids.add(selection.id()));
        }
        assertEquals(TrackerSelection.values().length, ids.size());
    }
}
