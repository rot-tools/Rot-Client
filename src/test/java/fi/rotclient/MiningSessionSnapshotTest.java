package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class MiningSessionSnapshotTest {
    private static final MiningSessionResource RUBY =
            MiningSessionResource.gemstone(
                    "ROUGH_RUBY_GEM",
                    "Rough Ruby Gemstone",
                    GemstoneType.RUBY,
                    GemstoneTier.ROUGH);
    private static final MiningSessionResource TOPAZ =
            MiningSessionResource.gemstone(
                    "FLAWED_TOPAZ_GEM",
                    "Flawed Topaz Gemstone",
                    GemstoneType.TOPAZ,
                    GemstoneTier.FLAWED);

    @Test
    void emptySnapshotIsValid() {
        MiningSessionSnapshot snapshot =
                snapshot(new MiningSessionLedger());

        assertEquals(0, snapshot.entryCount());
        assertEquals(List.of(), snapshot.entries());
        assertEquals(OptionalLong.empty(),
                snapshot.lastAcceptedEventTimestamp());
        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.OTHER_MINED));
    }

    @Test
    void capturesSessionIdAndTimestamps() {
        MiningSessionSnapshot snapshot =
                MiningSessionSnapshot.capture(
                        91L,
                        1_000L,
                        2_000L,
                        4_000L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        3L,
                        5L,
                        new MiningSessionLedger(),
                        parity(),
                        null);

        assertEquals(91L, snapshot.sessionId());
        assertEquals(1_000L, snapshot.sessionStartMillis());
        assertEquals(2_000L, snapshot.selectionStartMillis());
        assertEquals(4_000L, snapshot.snapshotAtMillis());
    }

    @Test
    void capturesSessionAndSelectionEpochs() {
        MiningSessionSnapshot snapshot =
                MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        17L,
                        23L,
                        new MiningSessionLedger(),
                        parity());

        assertEquals(17L, snapshot.sessionEpoch());
        assertEquals(23L, snapshot.selectionEpoch());
    }

    @Test
    void capturesSelectedTrackerAtSnapshotTime() {
        MiningSessionSnapshot snapshot =
                MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        true,
                        TrackerSelection.TOPAZ,
                        1L,
                        2L,
                        new MiningSessionLedger(),
                        parity());

        assertEquals(
                TrackerSelection.TOPAZ,
                snapshot.selectedTracker());
    }

    @Test
    void capturesTrackerState() {
        MiningSessionSnapshot snapshot =
                MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        false,
                        TrackerSelection.RUBY,
                        1L,
                        2L,
                        new MiningSessionLedger(),
                        parity());

        assertFalse(snapshot.trackerEnabled());
    }

    @Test
    void capturesDiagnosticsState() {
        MiningSessionSnapshot snapshot =
                MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        false,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        2L,
                        new MiningSessionLedger(),
                        parity());

        assertFalse(snapshot.diagnosticsActive());
    }

    @Test
    void countsTargetEntries() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger, RUBY, 8L,
                MiningSessionCategory.TARGET_MINED,
                1_500L, "target-1");
        append(ledger, RUBY, 9L,
                MiningSessionCategory.TARGET_MINED,
                1_600L, "target-2");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(2, snapshot.targetMinedEntryCount());
        assertEquals(2, snapshot.entryCount(
                MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void countsOtherEntries() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger, TOPAZ, 4L,
                MiningSessionCategory.OTHER_MINED,
                1_500L, "other-1");
        append(ledger, TOPAZ, 6L,
                MiningSessionCategory.OTHER_MINED,
                1_600L, "other-2");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(2, snapshot.otherMinedEntryCount());
        assertEquals(2, snapshot.entryCount(
                MiningSessionCategory.OTHER_MINED));
    }

    @Test
    void reportsTotalEntryCount() {
        MiningSessionLedger ledger = mixedLedger();

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(3, snapshot.entryCount());
        assertEquals(3, snapshot.entries().size());
    }

    @Test
    void aggregatesTargetQuantitiesByCanonicalResource() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger, RUBY, 8L,
                MiningSessionCategory.TARGET_MINED,
                1_500L, "target-1");
        append(ledger, RUBY, 13L,
                MiningSessionCategory.TARGET_MINED,
                1_600L, "target-2");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(21L, snapshot.quantity(
                MiningSessionCategory.TARGET_MINED,
                RUBY));
        assertEquals(
                Map.of(RUBY, 21L),
                snapshot.quantities(
                        MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void aggregatesOtherQuantitiesByCanonicalResource() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger, TOPAZ, 5L,
                MiningSessionCategory.OTHER_MINED,
                1_500L, "other-1");
        append(ledger, TOPAZ, 7L,
                MiningSessionCategory.OTHER_MINED,
                1_600L, "other-2");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(12L, snapshot.quantity(
                MiningSessionCategory.OTHER_MINED,
                TOPAZ));
        assertEquals(
                Map.of(TOPAZ, 12L),
                snapshot.quantities(
                        MiningSessionCategory.OTHER_MINED));
    }

    @Test
    void totalsItemQuantityByCategory() {
        MiningSessionLedger ledger = mixedLedger();

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(18L, snapshot.totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
        assertEquals(4L, snapshot.totalItemQuantity(
                MiningSessionCategory.OTHER_MINED));
    }

    @Test
    void capturesLastAcceptedEventTimestamp() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger, RUBY, 8L,
                MiningSessionCategory.TARGET_MINED,
                1_500L, "target-1");
        append(ledger, TOPAZ, 4L,
                MiningSessionCategory.OTHER_MINED,
                2_750L, "other-1");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(
                OptionalLong.of(2_750L),
                snapshot.lastAcceptedEventTimestamp());
    }

    @Test
    void countsEveryEntryAsUnresolved() {
        MiningSessionSnapshot snapshot = snapshot(mixedLedger());

        assertEquals(3, snapshot.unresolvedPriceEntryCount());
        assertEquals(
                snapshot.entryCount(),
                snapshot.unresolvedPriceEntryCount());
    }

    @Test
    void resolvedCoinTotalsRemainZero() {
        MiningSessionSnapshot snapshot = snapshot(mixedLedger());

        assertEquals(
                BigDecimal.ZERO,
                snapshot.resolvedCoinValue(
                        MiningSessionCategory.TARGET_MINED));
        assertEquals(
                BigDecimal.ZERO,
                snapshot.resolvedCoinValue(
                        MiningSessionCategory.OTHER_MINED));
        assertEquals(
                BigDecimal.ZERO,
                snapshot.resolvedTotalCoinValue());
    }

    @Test
    void attachesDerivedValuationWithoutMutatingEntries() {
        MiningSessionLedger ledger = mixedLedger();
        MiningSessionValuation valuation = MiningSessionValuation.create(
                MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL,
                1_000L,
                500L,
                1,
                2,
                0,
                0,
                0,
                0,
                new BigDecimal("12.50"),
                Map.of(
                        MiningSessionCategory.TARGET_MINED,
                        new BigDecimal("12.50"),
                        MiningSessionCategory.OTHER_MINED,
                        BigDecimal.ZERO,
                        MiningSessionCategory.CHEST_LOOT,
                        BigDecimal.ZERO));
        MiningSessionSnapshot snapshot = MiningSessionSnapshot.capture(
                42L,
                1_000L,
                1_000L,
                3_000L,
                true,
                true,
                TrackerSelection.RUBY,
                4L,
                7L,
                ledger,
                parity(),
                null,
                valuation);

        assertEquals(1, snapshot.valuation().resolvedEntryCount());
        assertEquals(
                new BigDecimal("12.50"),
                snapshot.valuation().resolvedItemValue());
        assertEquals(
                BigDecimal.ZERO,
                snapshot.resolvedTotalCoinValue());
    }

    @Test
    void chestLootRemainsEmptyWithoutChestEntries() {
        MiningSessionSnapshot snapshot = snapshot(mixedLedger());

        assertEquals(0, snapshot.entryCount(
                MiningSessionCategory.CHEST_LOOT));
        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.CHEST_LOOT));
        assertEquals(
                Map.of(),
                snapshot.quantities(
                        MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void chestLootItemEntriesAreAccepted() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger,
                MiningSessionResource.genericItem(
                        "GOLD_ESSENCE",
                        "Gold Essence"),
                3L,
                MiningSessionCategory.CHEST_LOOT,
                1_500L,
                "chest-1");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(1, snapshot.chestLootEntryCount());
        assertEquals(3L, snapshot.totalItemQuantity(
                MiningSessionCategory.CHEST_LOOT));
        assertEquals(1, snapshot.unresolvedPriceEntryCount());
    }

    @Test
    void currencyRemainsEmptyWithoutCurrencyEntries() {
        MiningSessionSnapshot snapshot = snapshot(mixedLedger());
        MiningSessionResource powder =
                MiningSessionResource.currency(
                        "GEMSTONE_POWDER",
                        "Gemstone Powder");

        assertEquals(0, snapshot.currencyEntryCount());
        assertEquals(0, snapshot.entryCount(
                MiningSessionCategory.CURRENCY));
        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.CURRENCY));
        assertEquals(0L, snapshot.quantity(
                MiningSessionCategory.CURRENCY,
                powder));
    }

    @Test
    void acceptsValidCurrencyEntry() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionResource powder =
                MiningSessionResource.currency(
                        "GEMSTONE_POWDER",
                        "Gemstone Powder");
        append(ledger,
                powder,
                296L,
                MiningSessionCategory.CURRENCY,
                1_500L,
                "currency-1");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(1, snapshot.currencyEntryCount());
        assertEquals(296L, snapshot.quantity(
                MiningSessionCategory.CURRENCY,
                powder));
        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.CURRENCY));
    }

    @Test
    void currencyDoesNotPolluteItemTotals() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionResource powder =
                MiningSessionResource.currency(
                        "GEMSTONE_POWDER",
                        "Gemstone Powder");
        append(ledger,
                powder,
                100L,
                MiningSessionCategory.CURRENCY,
                1_500L,
                "currency-1");

        MiningSessionSnapshot snapshot = snapshot(ledger);

        assertEquals(0L, snapshot.totalItemQuantity(
                MiningSessionCategory.CURRENCY));
        assertEquals(100L, snapshot.quantity(
                MiningSessionCategory.CURRENCY,
                powder));
    }

    @Test
    void entryListIsImmutable() {
        MiningSessionSnapshot snapshot = snapshot(mixedLedger());

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.entries().clear());
        assertEquals(3, snapshot.entryCount());
    }

    @Test
    void aggregateMapsAreDeeplyImmutable() {
        MiningSessionSnapshot snapshot = snapshot(mixedLedger());

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.entryCounts().put(
                        MiningSessionCategory.TARGET_MINED,
                        99));
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.quantities().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.quantities(
                        MiningSessionCategory.TARGET_MINED)
                        .put(RUBY, 99L));
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.totalItemQuantities().clear());
    }

    @Test
    void snapshotDoesNotChangeAfterLaterAppend() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger, RUBY, 8L,
                MiningSessionCategory.TARGET_MINED,
                1_500L, "target-1");
        MiningSessionSnapshot before = snapshot(ledger);

        append(ledger, TOPAZ, 7L,
                MiningSessionCategory.OTHER_MINED,
                2_000L, "other-1");

        assertEquals(1, before.entryCount());
        assertEquals(0L, before.quantity(
                MiningSessionCategory.OTHER_MINED,
                TOPAZ));
        assertEquals(OptionalLong.of(1_500L),
                before.lastAcceptedEventTimestamp());
    }

    @Test
    void snapshotAfterResetUsesFreshSessionState() {
        MiningSessionLedger ledger = mixedLedger();
        MiningSessionSnapshot beforeReset = snapshot(ledger);
        ledger.clear();

        MiningSessionSnapshot afterReset =
                MiningSessionSnapshot.capture(
                        43L,
                        3_500L,
                        4_000L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        5L,
                        8L,
                        ledger,
                        parity());

        assertEquals(3, beforeReset.entryCount());
        assertEquals(0, afterReset.entryCount());
        assertEquals(43L, afterReset.sessionId());
        assertEquals(5L, afterReset.sessionEpoch());
        assertEquals(8L, afterReset.selectionEpoch());
    }

    @Test
    void inactiveFinalSnapshotRemainsIndependentAfterStopCleanup() {
        MiningSessionLedger ledger = mixedLedger();
        MiningSessionSnapshot finalSnapshot =
                MiningSessionSnapshot.capture(
                        42L,
                        1_000L,
                        3_000L,
                        false,
                        true,
                        TrackerSelection.RUBY,
                        4L,
                        7L,
                        ledger,
                        parity());

        ledger.clear();

        assertFalse(finalSnapshot.diagnosticsActive());
        assertEquals(3, finalSnapshot.entryCount());
        assertEquals(18L, finalSnapshot.totalItemQuantity(
                MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void negativeTimestampsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        -1L,
                        3L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        1L,
                        new MiningSessionLedger(),
                        parity()));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        -1L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        1L,
                        new MiningSessionLedger(),
                        parity()));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        -1L,
                        3L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        1L,
                        new MiningSessionLedger(),
                        parity(),
                        null));
    }

    @Test
    void negativeEpochsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        -1L,
                        1L,
                        new MiningSessionLedger(),
                        parity()));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        -1L,
                        new MiningSessionLedger(),
                        parity()));
    }

    @Test
    void capturesParityStatusAndMismatchCount() {
        MiningSessionSnapshot snapshot = snapshot(mixedLedger());

        assertEquals(
                MiningSessionParity.Status.NOT_CHECKED,
                snapshot.targetParityStatus());
        assertEquals(0L, snapshot.parityMismatchCount());
    }

    @Test
    void snapshotCreationHasNoLedgerSideEffects() {
        MiningSessionLedger ledger = mixedLedger();
        List<MiningSessionLedger.Entry> before = ledger.entries();

        MiningSessionSnapshot first = snapshot(ledger);
        MiningSessionSnapshot second = snapshot(ledger);

        assertEquals(before, ledger.entries());
        assertEquals(3, ledger.entryCount());
        assertEquals(first.entries(), second.entries());
        assertEquals(first.quantities(), second.quantities());
    }

    @Test
    void acceptsValidCurrencyLedgerEntry() {
        MiningSessionLedger currencyLedger = new MiningSessionLedger();
        MiningSessionObservation observation =
                observation(
                        MiningSessionResource.currency(
                                "GEMSTONE_POWDER",
                                "Gemstone Powder"),
                        10L,
                        1_500L,
                        "currency-1");
        currencyLedger.append(
                MiningSessionClassification.wouldCredit(
                        observation,
                        MiningSessionCategory.CURRENCY),
                MiningSessionPriceResolution.unresolved());

        MiningSessionSnapshot snapshot = snapshot(currencyLedger);
        assertEquals(1, snapshot.currencyEntryCount());
    }

    @Test
    void rejectsCurrencyCategoryWithItemResource() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionObservation observation =
                observation(
                        MiningSessionResource.genericItem(
                                "GOBLIN_EGG",
                                "Goblin Egg"),
                        1L,
                        1_500L,
                        "currency-item-1");

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.append(
                        MiningSessionClassification.wouldCredit(
                                observation,
                                MiningSessionCategory.CURRENCY),
                        MiningSessionPriceResolution.unresolved()));
    }

    @Test
    void rejectsChestLootCategoryWithCurrencyResource() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionObservation observation =
                observation(
                        MiningSessionResource.currency(
                                "GEMSTONE_POWDER",
                                "Gemstone Powder"),
                        10L,
                        1_500L,
                        "chest-currency-1");

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.append(
                        MiningSessionClassification.wouldCredit(
                                observation,
                                MiningSessionCategory.CHEST_LOOT),
                        MiningSessionPriceResolution.unresolved()));
    }

    @Test
    void rejectsAnyNonUnresolvedPrice() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionObservation observation =
                observation(RUBY, 8L, 1_500L, "target-1");
        ledger.append(
                MiningSessionClassification.wouldCredit(
                        observation,
                        MiningSessionCategory.TARGET_MINED),
                MiningSessionPriceResolution.resolved(
                        MiningSessionPriceResolution.PriceSource.NPC_SELL,
                        BigDecimal.ONE,
                        1_000L));

        assertThrows(
                IllegalArgumentException.class,
                () -> snapshot(ledger));
    }

    @Test
    void rejectsIncompleteFactoryState() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        true,
                        null,
                        1L,
                        1L,
                        new MiningSessionLedger(),
                        parity()));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        1L,
                        null,
                        parity()));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionSnapshot.capture(
                        1L,
                        2L,
                        3L,
                        true,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        1L,
                        new MiningSessionLedger(),
                        null));
    }

    private static MiningSessionSnapshot snapshot(
            MiningSessionLedger ledger) {
        return MiningSessionSnapshot.capture(
                42L,
                1_000L,
                3_000L,
                true,
                true,
                TrackerSelection.RUBY,
                4L,
                7L,
                ledger,
                parity());
    }

    private static MiningSessionParity.Snapshot parity() {
        return new MiningSessionParity.Snapshot(
                MiningSessionParity.Status.NOT_CHECKED,
                0L,
                null);
    }

    private static MiningSessionLedger mixedLedger() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        append(ledger, RUBY, 8L,
                MiningSessionCategory.TARGET_MINED,
                1_500L, "target-1");
        append(ledger, RUBY, 10L,
                MiningSessionCategory.TARGET_MINED,
                1_600L, "target-2");
        append(ledger, TOPAZ, 4L,
                MiningSessionCategory.OTHER_MINED,
                1_700L, "other-1");
        return ledger;
    }

    private static void append(
            MiningSessionLedger ledger,
            MiningSessionResource resource,
            long quantity,
            MiningSessionCategory category,
            long observedAtMillis,
            String eventId) {
        ledger.append(
                MiningSessionClassification.wouldCredit(
                        observation(
                                resource,
                                quantity,
                                observedAtMillis,
                                eventId),
                        category),
                MiningSessionPriceResolution.unresolved());
    }

    private static MiningSessionObservation observation(
            MiningSessionResource resource,
            long quantity,
            long observedAtMillis,
            String eventId) {
        return new MiningSessionObservation(
                resource,
                quantity,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                7L,
                observedAtMillis,
                eventId,
                "batch-" + eventId,
                "Synthetic Test Source",
                null);
    }
}
