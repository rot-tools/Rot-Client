package fi.rotclient;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PickobulusPreviewPolicyTest {
    private static PickobulusPreviewPolicy.Context context() {
        return new PickobulusPreviewPolicy.Context(new Object(), new Object(), new Object(),
                SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES, SkyBlockSubArea.THE_FORGE),
                "profile-a", "GOLD", "PICKONIMBUS:tool-a", List.of("Ability: Pickobulus RIGHT CLICK"),
                new PickobulusPreviewPolicy.Settings(3, 32, false, false));
    }
    private static PickobulusPreviewPolicy.Cache<Integer> scanned(PickobulusPreviewPolicy.Context context) {
        var cache = new PickobulusPreviewPolicy.Cache<Integer>();
        assertTrue(cache.sync(context, 1_000));
        cache.aim("block-a");
        assertTrue(cache.beginScan(context, 1_000));
        cache.publish(context, List.of(1, 2, 3), 2, 1_000);
        return cache;
    }

    @Test void boundsAndGeometryStayExplicit() {
        assertTrue(PickobulusPreviewPolicy.inside(3, 3, 3, 3, false));
        assertFalse(PickobulusPreviewPolicy.inside(3, 3, 3, 3, true));
        assertTrue(PickobulusPreviewPolicy.inside(0, 0, 3, 3, true));
        assertFalse(PickobulusPreviewPolicy.inside(6, 0, 0, 100, false));
    }

    @Test void extremeOffsetsCannotOverflowIntoTheFootprint() {
        for (int offset : new int[] {Integer.MIN_VALUE, Integer.MAX_VALUE, -65_536, 65_536}) {
            for (boolean sphere : new boolean[] {false, true}) {
                assertFalse(PickobulusPreviewPolicy.inside(offset, 0, 0, 5, sphere));
                assertFalse(PickobulusPreviewPolicy.inside(0, offset, 0, 5, sphere));
                assertFalse(PickobulusPreviewPolicy.inside(0, 0, offset, 5, sphere));
            }
        }
    }

    @Test void speculativeHardStoneCandidatesCannotUseLiveSelectedTargetOverride() {
        var forge = SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES, SkyBlockSubArea.THE_FORGE);
        assertTrue(MiningBlockEvidencePolicy.allows("HARD_STONE", forge, true));
        assertTrue(PickobulusPreviewPolicy.materialTarget(true, List.of("HARD_STONE"), "HARD_STONE"));
        assertFalse(PickobulusPreviewPolicy.candidateAllowed("HARD_STONE", forge));
        assertFalse(PickobulusPreviewPolicy.candidateAllowed("HARD_STONE", SkyBlockLocation.UNKNOWN));
        assertTrue(PickobulusPreviewPolicy.candidateAllowed("HARD_STONE", SkyBlockLocation.of(SkyBlockArea.CRYSTAL_HOLLOWS)));
    }

    @Test void gemstoneSelectionCannotInheritCompatibilityGoldMaterialTarget() {
        // The legacy material accessor can fall back to Gold for a gemstone tracker.
        assertFalse(PickobulusPreviewPolicy.materialTarget(false, List.of("GOLD"), "GOLD"));
        assertFalse(PickobulusPreviewPolicy.materialTarget(false, List.of("MITHRIL", "TITANIUM"), "MITHRIL"));
        assertTrue(PickobulusPreviewPolicy.materialTarget(true, List.of("MITHRIL", "TITANIUM"), "MITHRIL"));
        assertTrue(PickobulusPreviewPolicy.materialTarget(true, List.of("MITHRIL", "TITANIUM"), "TITANIUM"));
        assertFalse(PickobulusPreviewPolicy.materialTarget(true, List.of("MITHRIL", "TITANIUM"), "GOLD"));
        assertFalse(PickobulusPreviewPolicy.materialTarget(true, null, "GOLD"));
    }

    @Test void corruptNumericSettingsRemainBounded() {
        assertEquals(new PickobulusPreviewPolicy.Settings(1, 4, true, false),
                new PickobulusPreviewPolicy.Settings(Integer.MIN_VALUE, Integer.MIN_VALUE, true, false));
        assertEquals(new PickobulusPreviewPolicy.Settings(5, 64, false, true),
                new PickobulusPreviewPolicy.Settings(Integer.MAX_VALUE, Integer.MAX_VALUE, false, true));
        int candidates = 0;
        for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) for (int z = -5; z <= 5; z++)
            if (PickobulusPreviewPolicy.inside(x, y, z, Integer.MAX_VALUE, false)) candidates++;
        assertEquals(1_331, candidates);
    }

    @Test void renderAndHudReadsRetireWorldPlayerConfigProfileTrackerAndToolChangesImmediately() {
        var original = context();
        List<PickobulusPreviewPolicy.Context> changes = List.of(
                new PickobulusPreviewPolicy.Context(new Object(), original.player(), original.config(), original.location(), original.profileId(), original.selectionId(), original.toolId(), original.lore(), original.settings()),
                new PickobulusPreviewPolicy.Context(original.world(), new Object(), original.config(), original.location(), original.profileId(), original.selectionId(), original.toolId(), original.lore(), original.settings()),
                new PickobulusPreviewPolicy.Context(original.world(), original.player(), new Object(), original.location(), original.profileId(), original.selectionId(), original.toolId(), original.lore(), original.settings()),
                new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(), original.location(), "profile-b", original.selectionId(), original.toolId(), original.lore(), original.settings()),
                new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(), original.location(), original.profileId(), "GEMSTONE_RUBY", original.toolId(), original.lore(), original.settings()),
                new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(), original.location(), original.profileId(), original.selectionId(), "tool-b", original.lore(), original.settings()),
                new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(), original.location(), original.profileId(), original.selectionId(), original.toolId(), List.of("Ability: Pickobulus RIGHT CLICK", "Cooldown: 30s"), original.settings())
        );
        for (var changed : changes) {
            var cache = scanned(original);
            assertTrue(cache.snapshot(changed, 1_001).isEmpty());
            assertTrue(cache.snapshot(original, 1_002).isEmpty());
        }
    }

    @Test void areaAndSettingChangesInvalidateButLocationObservationTimeDoesNot() {
        var original = context();
        var refreshedLocation = new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(),
                original.location().withObservedAt(2_000), original.profileId(), original.selectionId(), original.toolId(), original.lore(), original.settings());
        assertTrue(scanned(original).snapshot(refreshedLocation, 1_001).isPresent());
        var area = new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(),
                SkyBlockLocation.of(SkyBlockArea.CRYSTAL_HOLLOWS), original.profileId(), original.selectionId(), original.toolId(), original.lore(), original.settings());
        assertTrue(scanned(original).snapshot(area, 1_001).isEmpty());
        var subArea = new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(),
                SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES, SkyBlockSubArea.THE_MIST), original.profileId(), original.selectionId(), original.toolId(), original.lore(), original.settings());
        assertTrue(scanned(original).snapshot(subArea, 1_001).isEmpty());
        for (var settings : List.of(new PickobulusPreviewPolicy.Settings(5, 32, false, false),
                new PickobulusPreviewPolicy.Settings(3, 64, false, false),
                new PickobulusPreviewPolicy.Settings(3, 32, true, false),
                new PickobulusPreviewPolicy.Settings(3, 32, false, true))) {
            var changed = new PickobulusPreviewPolicy.Context(original.world(), original.player(), original.config(),
                    original.location(), original.profileId(), original.selectionId(), original.toolId(), original.lore(), settings);
            assertTrue(scanned(original).snapshot(changed, 1_001).isEmpty());
        }
    }

    @Test void disablingOrRemovingTheAbilityClearsCountsWithoutAllowingARescanEveryToggle() {
        var context = context();
        var cache = scanned(context);
        assertTrue(cache.snapshot(null, 1_001).isEmpty());
        assertTrue(cache.snapshot(context, 1_002).isEmpty());
        cache.aim("block-a");
        assertFalse(cache.beginScan(context, 1_002));
        assertTrue(cache.beginScan(context, 1_250));
    }

    @Test void newAimClearsOldBoxesAndPreservesFourScansPerSecondBudget() {
        var context = context();
        var cache = scanned(context);
        cache.aim("block-b");
        assertTrue(cache.snapshot(context, 1_001).isEmpty());
        assertFalse(cache.beginScan(context, 1_001));
        assertFalse(cache.beginScan(context, 1_249));
        assertTrue(cache.beginScan(context, 1_250));
        cache.publish(context, List.of(4), 1, 1_250);
        assertEquals(List.of(4), cache.snapshot(context, 1_251).orElseThrow().candidates());
        cache.aim(null);
        assertTrue(cache.snapshot(context, 1_252).isEmpty());
        assertFalse(cache.beginScan(context, 1_500));
    }

    @Test void staleOrClockReversedSnapshotsCannotReappear() {
        var context = context();
        var cache = scanned(context);
        assertTrue(cache.snapshot(context, 1_499).isPresent());
        assertTrue(cache.snapshot(context, 1_500).isEmpty());
        assertTrue(cache.snapshot(context, 1_100).isEmpty());
        cache = scanned(context);
        assertTrue(cache.snapshot(context, 999).isEmpty());
        assertTrue(cache.snapshot(context, 1_001).isEmpty());
        cache.aim("block-a");
        assertTrue(cache.beginScan(context, 1_001));
    }

    @Test void publishedCandidatesAreImmutableAndTargetCountCannotExceedTotal() {
        var context = context();
        var cache = scanned(context);
        var rows = new ArrayList<>(List.of(1, 2));
        cache.publish(context, rows, Integer.MAX_VALUE, 1_000);
        rows.clear();
        var snapshot = cache.snapshot(context, 1_001).orElseThrow();
        assertEquals(List.of(1, 2), snapshot.candidates());
        assertEquals(2, snapshot.targetCount());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.candidates().clear());
        cache.publish(context, List.of(1), -3, 1_000);
        assertEquals(0, cache.snapshot(context, 1_002).orElseThrow().targetCount());
    }

    @Test void oversizedOrWrongContextPublicationIsDiscarded() {
        var context = context();
        var cache = scanned(context);
        cache.publish(context, Collections.nCopies(1_332, 1), 100, 1_000);
        assertTrue(cache.snapshot(context, 1_001).isEmpty());
        cache.publish(context(), List.of(7), 1, 1_000);
        assertTrue(cache.snapshot(context, 1_002).isEmpty());
    }
}
