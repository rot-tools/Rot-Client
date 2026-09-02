package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

final class TargetItemGainPipelineTest {
    @Test
    void acceptedTargetItemsBecomeCanonicalDurableRows() {
        RotClientCurrentSession current = new RotClientCurrentSession();
        long started = current.snapshotConfig().startedAtMillis;
        current.onTargetChanged(TrackerSelection.GOLD.id(), started + 1L);

        assertTrue(TargetItemGainPipeline.creditMaterial(
                current,
                TrackedMaterial.GOLD,
                40L,
                SkyBlockArea.DWARVEN_MINES,
                started + 2L));
        current.onTargetChanged(TrackerSelection.RUBY.id(), started + 3L);
        assertTrue(TargetItemGainPipeline.creditGemstone(
                current,
                GemstoneType.RUBY,
                GemstoneTier.FLAWED,
                3L,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                started + 4L));

        RotClientCurrentSessionConfig snapshot = current.snapshotConfig();
        assertEquals(2, snapshot.items.size());
        assertTrue(snapshot.items.stream().allMatch(row ->
                row.source() == SessionSourceType.MINING
                        && row.miningClassification()
                        == MiningClassification.TARGET));
        assertEquals(40L, snapshot.items.stream()
                .filter(row -> row.itemId().equals("GOLD"))
                .findFirst().orElseThrow().quantity());
        assertEquals(3L, snapshot.items.stream()
                .filter(row -> row.itemId().equals("FLAWED_RUBY_GEM"))
                .findFirst().orElseThrow().quantity());

        MiningSessionAnalyticsViewModel view =
                new MiningSessionAnalyticsProjector().project(
                        Optional.empty(), snapshot);
        assertEquals(2, view.targetEntryCount());
        assertEquals(40L,
                view.targetQuantities().get("GOLD").quantity());
        assertEquals(3L,
                view.targetQuantities().get("FLAWED_RUBY_GEM").quantity());

        current.pause(started + 10L);
        current.resume(started + 20L);
        RotClientCurrentSessionConfig restarted =
                RotClientCurrentSessionStore.parseJson(
                        RotClientCurrentSessionStore.toJson(
                                current.snapshotConfig()));
        MiningSessionAnalyticsViewModel afterRestart =
                new MiningSessionAnalyticsProjector().project(
                        Optional.empty(), restarted);
        assertEquals(40L,
                afterRestart.targetQuantities().get("GOLD").quantity());
        assertEquals(3L,
                afterRestart.targetQuantities()
                        .get("FLAWED_RUBY_GEM").quantity());
    }

    @Test
    void canonicalTargetBoundaryRejectsCrossSelectionCredits() {
        RotClientCurrentSession current = new RotClientCurrentSession();
        long started = current.snapshotConfig().startedAtMillis;
        current.onTargetChanged(TrackerSelection.REDSTONE.id(), started + 1L);

        assertFalse(TargetItemGainPipeline.creditMaterial(
                current,
                TrackedMaterial.GOLD,
                40L,
                SkyBlockArea.DWARVEN_MINES,
                started + 2L));
        assertFalse(TargetItemGainPipeline.creditGemstone(
                current,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                40L,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                started + 3L));
        assertTrue(TargetItemGainPipeline.creditMaterial(
                current,
                TrackedMaterial.REDSTONE,
                40L,
                SkyBlockArea.DWARVEN_MINES,
                started + 4L));

        current.onTargetChanged(TrackerSelection.RUBY.id(), started + 5L);
        assertFalse(TargetItemGainPipeline.creditMaterial(
                current,
                TrackedMaterial.REDSTONE,
                10L,
                SkyBlockArea.DWARVEN_MINES,
                started + 6L));
        assertFalse(TargetItemGainPipeline.creditGemstone(
                current,
                GemstoneType.TOPAZ,
                GemstoneTier.ROUGH,
                10L,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                started + 7L));
        assertTrue(TargetItemGainPipeline.creditGemstone(
                current,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH,
                10L,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                started + 8L));

        RotClientCurrentSessionConfig snapshot = current.snapshotConfig();
        assertEquals(2, snapshot.items.size());
        assertEquals(40L, snapshot.items.stream()
                .filter(row -> row.itemId().equals("REDSTONE"))
                .findFirst().orElseThrow().quantity());
        assertEquals(10L, snapshot.items.stream()
                .filter(row -> row.itemId().equals("ROUGH_RUBY_GEM"))
                .findFirst().orElseThrow().quantity());
    }

    @Test
    void everySelectionAcceptsOnlyItsOwnTargetFamily() {
        for (TrackerSelection selection : TrackerSelection.values()) {
            RotClientCurrentSession current = new RotClientCurrentSession();
            long started = current.snapshotConfig().startedAtMillis;
            current.onTargetChanged(selection.id(), started + 1L);

            for (TrackedMaterial material : TrackedMaterial.values()) {
                boolean expected = selection.isMaterial()
                        && selection.materialTarget().includes(material);
                assertEquals(expected, TargetItemGainPipeline.creditMaterial(
                        current,
                        material,
                        1L,
                        SkyBlockArea.DWARVEN_MINES,
                        started + 2L + material.ordinal()),
                        selection.id() + " -> " + material.id());
            }
            for (GemstoneType gemstone : GemstoneType.values()) {
                boolean expected = selection.isGemstone()
                        && selection.gemstone() == gemstone;
                assertEquals(expected, TargetItemGainPipeline.creditGemstone(
                        current,
                        gemstone,
                        GemstoneTier.ROUGH,
                        1L,
                        SkyBlockArea.CRYSTAL_HOLLOWS,
                        started + 100L + gemstone.ordinal()),
                        selection.id() + " -> " + gemstone.id());
            }
            assertTrue(current.snapshotConfig().items.stream().allMatch(row ->
                    row.source() == SessionSourceType.MINING
                            && row.miningClassification()
                            == MiningClassification.TARGET), selection.id());
        }

        RotClientCurrentSession unknown = new RotClientCurrentSession();
        long started = unknown.snapshotConfig().startedAtMillis;
        unknown.onTargetChanged("UNKNOWN_TARGET", started + 1L);
        assertFalse(TargetItemGainPipeline.creditMaterial(
                unknown,
                TrackedMaterial.GOLD,
                1L,
                SkyBlockArea.DWARVEN_MINES,
                started + 2L));
    }

    @Test
    void pausedCurrentSessionRejectsTargetCreditsWithoutMutation() {
        RotClientCurrentSession current = new RotClientCurrentSession();
        long started = current.snapshotConfig().startedAtMillis;
        current.pause(started + 10L);

        assertFalse(TargetItemGainPipeline.creditMaterial(
                current,
                TrackedMaterial.DIAMOND,
                8L,
                SkyBlockArea.DEEP_CAVERNS,
                started + 11L));
        assertTrue(current.snapshotConfig().items.isEmpty());
    }

    @Test
    void analyticsAndHistoryKeepAreaAttributedRowCountsInParity() {
        RotClientCurrentSession current = new RotClientCurrentSession();
        long started = current.snapshotConfig().startedAtMillis;
        current.onTargetChanged(TrackerSelection.GOLD.id(), started + 1L);

        assertTrue(TargetItemGainPipeline.creditMaterial(
                current,
                TrackedMaterial.GOLD,
                4L,
                SkyBlockArea.DWARVEN_MINES,
                started + 2L));
        assertTrue(TargetItemGainPipeline.creditMaterial(
                current,
                TrackedMaterial.GOLD,
                6L,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                started + 3L));
        current.creditItem(
                "HARD_STONE",
                "Hard Stone",
                7L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                started + 4L);
        current.creditItem(
                "HARD_STONE",
                "Hard Stone",
                8L,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.CRYSTAL_HOLLOWS,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                started + 5L);

        RotClientCurrentSessionConfig snapshot = current.snapshotConfig();
        MiningSessionAnalyticsViewModel live =
                new MiningSessionAnalyticsProjector().project(
                        Optional.empty(), snapshot);
        RotClientSessionFreeze frozen = current.freeze(started + 10L);
        MiningSessionHistoryRecord archived =
                MiningSessionHistoryRecord.fromCurrentSessionFreeze(
                        "hareaentryparity1",
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        MiningSessionHistoryCodec.contentFingerprint(frozen),
                        frozen);

        assertEquals(10L, live.targetQuantities().get("GOLD").quantity());
        assertEquals(15L,
                live.otherMinedQuantities().get("HARD_STONE").quantity());
        assertEquals(4, live.entryCount());
        assertEquals(2, live.targetEntryCount());
        assertEquals(2, live.otherEntryCount());
        assertEquals(4, live.unresolvedEntryCount());
        assertEquals(archived.entryCount(), live.entryCount());
        assertEquals(archived.targetEntryCount(), live.targetEntryCount());
        assertEquals(archived.otherEntryCount(), live.otherEntryCount());
        assertEquals(archived.unresolvedEntryCount(),
                live.unresolvedEntryCount());
    }
}
