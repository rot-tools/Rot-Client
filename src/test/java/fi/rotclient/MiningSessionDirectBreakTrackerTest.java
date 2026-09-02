package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionDirectBreakTrackerTest {
    @Test
    void matchingMaterialAuthorizesMaterialChannel() {
        MiningSessionDirectBreakTracker tracker = tracker();
        tracker.recordConfirmedBreak(material(TrackedMaterial.GOLD), 1L, 100L);

        assertTrue(find(
                tracker,
                material(TrackedMaterial.GOLD),
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY,
                1L,
                105L).isPresent());
    }

    @Test
    void gemstoneAuthorizesSackChannel() {
        MiningSessionDirectBreakTracker tracker = tracker();
        tracker.recordConfirmedBreak(gemstone(GemstoneType.TOPAZ), 1L, 100L);

        assertTrue(find(
                tracker,
                gemstone(GemstoneType.TOPAZ),
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L,
                110L).isPresent());
    }

    @Test
    void gemstoneAuthorizesPristineChannel() {
        MiningSessionDirectBreakTracker tracker = tracker();
        tracker.recordConfirmedBreak(gemstone(GemstoneType.TOPAZ), 1L, 100L);

        assertTrue(find(
                tracker,
                gemstone(GemstoneType.TOPAZ),
                MiningSessionDirectBreakTracker.EvidenceChannel.PRISTINE_QUANTITY,
                1L,
                110L).isPresent());
    }

    @Test
    void pristineDoesNotConsumeSackChannel() {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family = gemstone(GemstoneType.TOPAZ);
        tracker.recordConfirmedBreak(family, 1L, 100L);
        MiningSessionDirectBreakTracker.Correlation pristine = find(
                tracker,
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.PRISTINE_QUANTITY,
                1L,
                110L).orElseThrow();

        assertTrue(tracker.consume(pristine, 110L));
        assertTrue(find(
                tracker,
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L,
                111L).isPresent());
    }

    @Test
    void sackDoesNotConsumePristineChannel() {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family = gemstone(GemstoneType.TOPAZ);
        tracker.recordConfirmedBreak(family, 1L, 100L);
        MiningSessionDirectBreakTracker.Correlation sack = find(
                tracker,
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L,
                110L).orElseThrow();

        assertTrue(tracker.consume(sack, 110L));
        assertTrue(find(
                tracker,
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.PRISTINE_QUANTITY,
                1L,
                111L).isPresent());
    }

    @Test
    void wrongMaterialDoesNotConsumeContext() {
        assertWrongFamilyPreserves(
                material(TrackedMaterial.GOLD),
                material(TrackedMaterial.DIAMOND),
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY);
    }

    @Test
    void wrongGemstoneDoesNotConsumeContext() {
        assertWrongFamilyPreserves(
                gemstone(GemstoneType.RUBY),
                gemstone(GemstoneType.TOPAZ),
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY);
    }

    @Test
    void wrongSelectionEpochDoesNotConsumeContext() {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family = gemstone(GemstoneType.TOPAZ);
        tracker.recordConfirmedBreak(family, 1L, 100L);

        assertTrue(find(tracker, family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                2L, 110L).isEmpty());
        assertTrue(find(tracker, family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L, 111L).isPresent());
    }

    @Test
    void negativeQuantityDoesNotConsumeContext() {
        assertInvalidCandidatePreserves(-1L, true, false);
    }

    @Test
    void zeroQuantityDoesNotConsumeContext() {
        assertInvalidCandidatePreserves(0L, true, false);
    }

    @Test
    void unsupportedSourceDoesNotConsumeContext() {
        assertInvalidCandidatePreserves(1L, false, false);
    }

    @Test
    void duplicateDeliveryDoesNotConsumeContext() {
        assertInvalidCandidatePreserves(1L, true, true);
    }

    @Test
    void expiredContextCannotAuthorizeCredit() {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family = gemstone(GemstoneType.TOPAZ);
        tracker.recordConfirmedBreak(family, 1L, 100L);

        assertTrue(find(tracker, family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L, 121L).isEmpty());
    }

    @Test
    void pendingContextCanCompleteAcrossInactiveBoundary() {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family = gemstone(GemstoneType.TOPAZ);
        tracker.recordConfirmedBreak(family, 1L, 100L);

        assertTrue(find(tracker, family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L, 120L).isPresent());
    }

    @Test
    void uncorrelatedQuantityCannotOpenContext() {
        MiningSessionDirectBreakTracker tracker = tracker();

        assertTrue(find(tracker, gemstone(GemstoneType.TOPAZ),
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L, 100L).isEmpty());
        assertEquals(0, tracker.pendingContextCount());
    }

    @Test
    void resetClearsContexts() {
        assertClearOperation(MiningSessionDirectBreakTracker::reset);
    }

    @Test
    void targetTransitionClearsContexts() {
        assertClearOperation(MiningSessionDirectBreakTracker::onTargetTransition);
    }

    @Test
    void trackerOffClearsContexts() {
        assertClearOperation(MiningSessionDirectBreakTracker::onTrackerDisabled);
    }

    @Test
    void worldTransitionClearsContexts() {
        assertClearOperation(MiningSessionDirectBreakTracker::onWorldTransition);
    }

    @Test
    void contextIdsAreUniqueAndMonotonic() {
        MiningSessionDirectBreakTracker tracker = tracker();

        String first = tracker.recordConfirmedBreak(
                gemstone(GemstoneType.RUBY), 1L, 100L);
        String second = tracker.recordConfirmedBreak(
                gemstone(GemstoneType.RUBY), 1L, 101L);

        assertEquals("context-1", first);
        assertEquals("context-2", second);
        assertNotEquals(first, second);
    }

    @Test
    void sequenceOverflowHasNoPartialMutation() {
        MiningSessionDirectBreakTracker tracker =
                new MiningSessionDirectBreakTracker(
                        10L, 20L, 30L, 4, Long.MAX_VALUE);

        assertThrows(
                ArithmeticException.class,
                () -> tracker.recordConfirmedBreak(
                        gemstone(GemstoneType.RUBY), 1L, 100L));
        assertEquals(0, tracker.pendingContextCount());
    }

    @Test
    void consumedChannelCannotBeConsumedTwice() {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family =
                material(TrackedMaterial.GOLD);
        tracker.recordConfirmedBreak(family, 1L, 100L);
        MiningSessionDirectBreakTracker.Correlation correlation = find(
                tracker,
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY,
                1L,
                105L).orElseThrow();

        assertTrue(tracker.consume(correlation, 105L));
        assertFalse(tracker.consume(correlation, 106L));
    }

    @Test
    void separateBreakContextsRemainDistinct() {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family =
                material(TrackedMaterial.GOLD);
        tracker.recordConfirmedBreak(family, 1L, 100L);
        tracker.recordConfirmedBreak(family, 1L, 101L);
        MiningSessionDirectBreakTracker.Correlation first = find(
                tracker,
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY,
                1L,
                105L).orElseThrow();
        assertTrue(tracker.consume(first, 105L));
        MiningSessionDirectBreakTracker.Correlation second = find(
                tracker,
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY,
                1L,
                106L).orElseThrow();

        assertNotEquals(first.contextId(), second.contextId());
    }

    @Test
    void pendingStateIsBoundedAndSnapshotsAreImmutable() {
        MiningSessionDirectBreakTracker tracker =
                new MiningSessionDirectBreakTracker(10L, 20L, 30L, 2, 0L);
        tracker.recordConfirmedBreak(gemstone(GemstoneType.RUBY), 1L, 100L);
        tracker.recordConfirmedBreak(gemstone(GemstoneType.TOPAZ), 1L, 101L);
        tracker.recordConfirmedBreak(gemstone(GemstoneType.JADE), 1L, 102L);

        assertEquals(2, tracker.pendingContextCount());
        List<MiningSessionDirectBreakTracker.ContextSnapshot> snapshots =
                tracker.snapshots(102L);
        assertThrows(UnsupportedOperationException.class, snapshots::clear);
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshots.getFirst().availableChannels().clear());
    }

    private static void assertWrongFamilyPreserves(
            MiningSessionDirectBreakTracker.FamilyKey recorded,
            MiningSessionDirectBreakTracker.FamilyKey wrong,
            MiningSessionDirectBreakTracker.EvidenceChannel channel) {
        MiningSessionDirectBreakTracker tracker = tracker();
        tracker.recordConfirmedBreak(recorded, 1L, 100L);

        assertTrue(find(tracker, wrong, channel, 1L, 105L).isEmpty());
        assertTrue(find(tracker, recorded, channel, 1L, 106L).isPresent());
    }

    private static void assertInvalidCandidatePreserves(
            long quantity,
            boolean supportedSource,
            boolean duplicate) {
        MiningSessionDirectBreakTracker tracker = tracker();
        MiningSessionDirectBreakTracker.FamilyKey family =
                gemstone(GemstoneType.TOPAZ);
        tracker.recordConfirmedBreak(family, 1L, 100L);

        assertTrue(tracker.findCorrelation(
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                quantity,
                supportedSource,
                duplicate,
                1L,
                105L).isEmpty());
        assertTrue(find(tracker, family,
                MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY,
                1L, 106L).isPresent());
    }

    private static void assertClearOperation(
            java.util.function.Consumer<MiningSessionDirectBreakTracker> operation) {
        MiningSessionDirectBreakTracker tracker = tracker();
        tracker.recordConfirmedBreak(
                gemstone(GemstoneType.TOPAZ), 1L, 100L);

        operation.accept(tracker);

        assertEquals(0, tracker.pendingContextCount());
    }

    @Test
    void coveredBatchWindowKeepsEvidenceEligibleBeyondDefaultMaterialWindow() {
        MiningSessionDirectBreakTracker tracker =
                new MiningSessionDirectBreakTracker();
        MiningSessionDirectBreakTracker.FamilyKey family =
                MiningSessionDirectBreakTracker.FamilyKey.material(
                        TrackedMaterial.COBBLESTONE);
        tracker.recordConfirmedBreak(family, 1L, 1_000L);

        // 17s later — outside default 10s material window.
        assertTrue(tracker.findCorrelation(
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY,
                1L,
                true,
                false,
                1L,
                1_000L + 17_000L,
                0L).isEmpty());

        // Same evidence remains eligible when Sack reports Last 24s.
        assertTrue(tracker.findCorrelation(
                family,
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY,
                1L,
                true,
                false,
                1L,
                1_000L + 17_000L,
                24_000L).isPresent());
    }

    private static java.util.Optional<MiningSessionDirectBreakTracker.Correlation> find(
            MiningSessionDirectBreakTracker tracker,
            MiningSessionDirectBreakTracker.FamilyKey family,
            MiningSessionDirectBreakTracker.EvidenceChannel channel,
            long epoch,
            long now) {
        return tracker.findCorrelation(
                family, channel, 1L, true, false, epoch, now);
    }

    private static MiningSessionDirectBreakTracker tracker() {
        return new MiningSessionDirectBreakTracker(10L, 20L, 30L, 128, 0L);
    }

    private static MiningSessionDirectBreakTracker.FamilyKey material(
            TrackedMaterial material) {
        return MiningSessionDirectBreakTracker.FamilyKey.material(material);
    }

    private static MiningSessionDirectBreakTracker.FamilyKey gemstone(
            GemstoneType gemstone) {
        return MiningSessionDirectBreakTracker.FamilyKey.gemstone(gemstone);
    }
}
