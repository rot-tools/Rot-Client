package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionLedgerTest {
    @Test
    void wouldCreditEntryIsAppended() {
        MiningSessionLedger ledger = new MiningSessionLedger();

        assertTrue(ledger.append(
                accepted(item("RUBY"), 10L, MiningSessionCategory.TARGET_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved()));

        assertEquals(1, ledger.entryCount());
        assertEquals(10L, ledger.entries().getFirst().observation().quantity());
    }

    @Test
    void rejectedClassificationCannotBeAppended() {
        MiningSessionObservation observation = observation(
                item("RUBY"), 10L, "event-1", null);
        MiningSessionClassification rejected =
                MiningSessionClassification.rejected(
                        observation,
                        MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY);

        MiningSessionLedger ledger = new MiningSessionLedger();

        assertFalse(ledger.append(
                rejected,
                MiningSessionPriceResolution.unresolved()));
        assertEquals(0, ledger.entryCount());
    }

    @Test
    void unresolvedClassificationCannotBeAppended() {
        MiningSessionObservation observation = observation(
                item("RUBY"), 10L, "event-1", null);
        MiningSessionClassification unresolved =
                MiningSessionClassification.unresolved(
                        observation,
                        MiningSessionClassification.ReasonCode.INSUFFICIENT_EVIDENCE);

        MiningSessionLedger ledger = new MiningSessionLedger();

        assertFalse(ledger.append(
                unresolved,
                MiningSessionPriceResolution.unresolved()));
        assertEquals(0, ledger.entryCount());
    }

    @Test
    void sameEventIdentityIsIdempotent() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionResource resource = item("RUBY");
        MiningSessionClassification first =
                MiningSessionClassification.wouldCredit(
                        observation(resource, 10L, "event-1", "batch-1"),
                        MiningSessionCategory.TARGET_MINED);
        MiningSessionClassification redelivery =
                MiningSessionClassification.wouldCredit(
                        observation(
                                MiningSessionResource.genericItem("RUBY", "Ruby renamed"),
                                10L,
                                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                                TrackerSelection.RUBY,
                                1L,
                                9_000L,
                                "event-1",
                                "batch-1",
                                "Gemstones Sack",
                                "different diagnostic"),
                        MiningSessionCategory.TARGET_MINED);

        assertTrue(ledger.append(
                first,
                resolved("2.00")));
        assertFalse(ledger.append(
                redelivery,
                resolved("999.00")));
        assertEquals(1, ledger.entryCount());
        assertEquals(new BigDecimal("20.00"), ledger.resolvedTotalCoinValue());
    }

    @Test
    void sameResourceAndQuantityWithDifferentEventIdsIsAcceptedTwice() {
        MiningSessionResource resource = item("RUBY");
        MiningSessionLedger ledger = new MiningSessionLedger();

        assertTrue(ledger.append(
                accepted(resource, 10L, MiningSessionCategory.TARGET_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved()));
        assertTrue(ledger.append(
                accepted(resource, 10L, MiningSessionCategory.TARGET_MINED, "event-2"),
                MiningSessionPriceResolution.unresolved()));

        assertEquals(2, ledger.entryCount());
        assertEquals(
                20L,
                ledger.quantity(MiningSessionCategory.TARGET_MINED, resource));
    }

    @Test
    void missingEventIdentityDoesNotCreateUnsafePermanentDedupe() {
        MiningSessionResource resource = item("RUBY");
        MiningSessionClassification classification =
                MiningSessionClassification.wouldCredit(
                        observation(resource, 10L, null, null),
                        MiningSessionCategory.TARGET_MINED);
        MiningSessionLedger ledger = new MiningSessionLedger();

        assertTrue(ledger.append(
                classification,
                MiningSessionPriceResolution.unresolved()));
        assertTrue(ledger.append(
                classification,
                MiningSessionPriceResolution.unresolved()));
        assertEquals(2, ledger.entryCount());
        assertFalse(ledger.entries().getFirst().hasDeliveryIdentity());
    }

    @Test
    void correlationIdentityProvidesDedupeWhenEventIdIsMissing() {
        MiningSessionResource resource = item("RUBY");
        MiningSessionClassification classification =
                MiningSessionClassification.wouldCredit(
                        observation(resource, 10L, null, "batch-1"),
                        MiningSessionCategory.TARGET_MINED);
        MiningSessionLedger ledger = new MiningSessionLedger();

        assertTrue(ledger.append(
                classification,
                MiningSessionPriceResolution.unresolved()));
        assertFalse(ledger.append(
                classification,
                MiningSessionPriceResolution.unresolved()));
    }

    @Test
    void conflictingEventQuantityThrowsWithoutMutation() {
        assertEventConflict(
                observation(item("RUBY"), 11L, "event-1", "batch-1"),
                MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void conflictingEventEvidenceThrowsWithoutMutation() {
        assertEventConflict(
                observation(
                        item("RUBY"),
                        10L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.RUBY,
                        1L,
                        2_000L,
                        "event-1",
                        "batch-1",
                        "Gemstones Sack",
                        null),
                MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void conflictingEventTrackerThrowsWithoutMutation() {
        assertEventConflict(
                observation(
                        item("RUBY"),
                        10L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.TOPAZ,
                        1L,
                        2_000L,
                        "event-1",
                        "batch-1",
                        "Gemstones Sack",
                        null),
                MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void conflictingEventEpochThrowsWithoutMutation() {
        assertEventConflict(
                observation(
                        item("RUBY"),
                        10L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.RUBY,
                        2L,
                        2_000L,
                        "event-1",
                        "batch-1",
                        "Gemstones Sack",
                        null),
                MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void conflictingEventCategoryThrowsWithoutMutation() {
        assertEventConflict(
                observation(item("RUBY"), 10L, "event-1", "batch-1"),
                MiningSessionCategory.CHEST_LOOT);
    }

    @Test
    void conflictingEventResourceSemanticsThrowsWithoutMutation() {
        MiningSessionResource topazWithSameId = MiningSessionResource.gemstone(
                "RUBY",
                "Topaz with reused id",
                GemstoneType.TOPAZ,
                GemstoneTier.ROUGH);
        assertEventConflict(
                observation(topazWithSameId, 10L, "event-1", "batch-1"),
                MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void conflictingEventCorrelationThrowsWithoutMutation() {
        assertEventConflict(
                observation(item("RUBY"), 10L, "event-1", "batch-2"),
                MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void conflictingEventSourceThrowsWithoutMutation() {
        assertEventConflict(
                observation(
                        item("RUBY"),
                        10L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.RUBY,
                        1L,
                        2_000L,
                        "event-1",
                        "batch-1",
                        "Inventory",
                        null),
                MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void conflictingCorrelationQuantityThrows() {
        assertCorrelationConflict(
                observation(item("RUBY"), 11L, null, "batch-1"));
    }

    @Test
    void conflictingCorrelationEvidenceThrows() {
        assertCorrelationConflict(
                observation(
                        item("RUBY"),
                        10L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.RUBY,
                        1L,
                        2_000L,
                        null,
                        "batch-1",
                        "Gemstones Sack",
                        null));
    }

    @Test
    void eventAndCorrelationIdentitiesUseSeparateNamespaces() {
        MiningSessionLedger ledger = new MiningSessionLedger();

        assertTrue(ledger.append(
                MiningSessionClassification.wouldCredit(
                        observation(item("RUBY"), 10L, "shared", null),
                        MiningSessionCategory.TARGET_MINED),
                MiningSessionPriceResolution.unresolved()));
        assertTrue(ledger.append(
                MiningSessionClassification.wouldCredit(
                        observation(item("RUBY"), 10L, null, "shared"),
                        MiningSessionCategory.TARGET_MINED),
                MiningSessionPriceResolution.unresolved()));
        assertEquals(2, ledger.entryCount());
    }

    @Test
    void failedConflictDoesNotPoisonDedupeOrOtherDeliveries() {
        MiningSessionLedger ledger = ledgerWithOriginalEvent();
        MiningSessionClassification conflict =
                MiningSessionClassification.wouldCredit(
                        observation(item("RUBY"), 11L, "event-1", "batch-1"),
                        MiningSessionCategory.TARGET_MINED);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ledger.append(conflict, resolved("3.00")));

        assertTrue(exception.getMessage().contains("event"));
        assertFalse(exception.getMessage().contains("private diagnostic"));
        assertEquals(1, ledger.entryCount());
        assertEquals(10L, ledger.totalItemQuantity(MiningSessionCategory.TARGET_MINED));
        assertEquals(new BigDecimal("20.00"), ledger.resolvedTotalCoinValue());
        assertFalse(ledger.append(
                accepted(item("RUBY"), 10L, MiningSessionCategory.TARGET_MINED, "event-1"),
                resolved("100.00")));
        assertTrue(ledger.append(
                accepted(item("TOPAZ"), 4L, MiningSessionCategory.OTHER_MINED, "event-2"),
                resolved("1.50")));
        assertEquals(2, ledger.entryCount());
    }

    @Test
    void targetMinedQuantityAggregatesByResource() {
        assertCategoryQuantity(MiningSessionCategory.TARGET_MINED);
    }

    @Test
    void otherMinedQuantityAggregatesByResource() {
        assertCategoryQuantity(MiningSessionCategory.OTHER_MINED);
    }

    @Test
    void chestLootQuantityAggregatesByResource() {
        assertCategoryQuantity(MiningSessionCategory.CHEST_LOOT);
    }

    @Test
    void currencyQuantityAggregatesByResource() {
        MiningSessionResource powder = currency("MITHRIL_POWDER");
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(powder, 100L, MiningSessionCategory.CURRENCY, "event-1"),
                MiningSessionPriceResolution.notApplicable());
        ledger.append(
                accepted(powder, 50L, MiningSessionCategory.CURRENCY, "event-2"),
                MiningSessionPriceResolution.notApplicable());

        assertEquals(150L, ledger.totalCurrencyQuantity(powder));
        assertEquals(0L, ledger.totalItemQuantity(MiningSessionCategory.CURRENCY));
    }

    @Test
    void currencyIsExcludedFromCoinTotalsEvenWhenIncorrectlyPriced() {
        MiningSessionResource powder = currency("MITHRIL_POWDER");
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(powder, 100L, MiningSessionCategory.CURRENCY, "event-1"),
                resolved("2.50"));

        assertEquals(BigDecimal.ZERO, ledger.resolvedCoinValue(
                MiningSessionCategory.CURRENCY));
        assertEquals(BigDecimal.ZERO, ledger.resolvedTotalCoinValue());
    }

    @Test
    void resolvedItemPriceContributesToCoinTotals() {
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(item("GOLD"), 4L, MiningSessionCategory.TARGET_MINED, "event-1"),
                resolved("2.50"));

        assertEquals(
                new BigDecimal("10.00"),
                ledger.resolvedCoinValue(MiningSessionCategory.TARGET_MINED));
        assertEquals(
                new BigDecimal("10.00"),
                ledger.resolvedTotalCoinValue());
    }

    @Test
    void unresolvedPriceDoesNotContribute() {
        assertPriceDoesNotContribute(
                MiningSessionPriceResolution.unresolved());
    }

    @Test
    void unavailablePriceDoesNotContribute() {
        assertPriceDoesNotContribute(
                MiningSessionPriceResolution.unavailable(500L));
    }

    @Test
    void stalePriceDoesNotContribute() {
        assertPriceDoesNotContribute(
                MiningSessionPriceResolution.stale(
                        MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL,
                        new BigDecimal("2.50"),
                        400L,
                        500L));
    }

    @Test
    void notApplicablePriceDoesNotContribute() {
        assertPriceDoesNotContribute(
                MiningSessionPriceResolution.notApplicable());
    }

    @Test
    void bigDecimalMultiplicationRemainsExact() {
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(item("GOLD"), 3L, MiningSessionCategory.TARGET_MINED, "event-1"),
                resolved("0.1"));

        assertEquals(
                new BigDecimal("0.3"),
                ledger.resolvedTotalCoinValue());
    }

    @Test
    void bigDecimalAdditionPreservesExactScaleSemantics() {
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(item("GOLD"), 1L, MiningSessionCategory.TARGET_MINED, "event-1"),
                resolved("1.0"));
        ledger.append(
                accepted(item("DIAMOND"), 1L, MiningSessionCategory.OTHER_MINED, "event-2"),
                resolved("1.00"));

        assertEquals(new BigDecimal("2.00"), ledger.resolvedTotalCoinValue());
        assertEquals(0, ledger.resolvedTotalCoinValue().compareTo(new BigDecimal("2")));
    }

    @Test
    void quantityOverflowThrowsClearly() {
        MiningSessionResource resource = item("RUBY");
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(resource, Long.MAX_VALUE, MiningSessionCategory.TARGET_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved());

        assertThrows(
                ArithmeticException.class,
                () -> ledger.append(
                        accepted(resource, 1L, MiningSessionCategory.TARGET_MINED, "event-2"),
                        MiningSessionPriceResolution.unresolved()));
    }

    @Test
    void failedOverflowLeavesLedgerUnchanged() {
        MiningSessionResource resource = item("RUBY");
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                accepted(resource, Long.MAX_VALUE, MiningSessionCategory.TARGET_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved());
        List<MiningSessionLedger.Entry> before = ledger.entries();

        assertThrows(
                ArithmeticException.class,
                () -> ledger.append(
                        accepted(resource, 1L, MiningSessionCategory.TARGET_MINED, "event-2"),
                        MiningSessionPriceResolution.unresolved()));

        assertEquals(before, ledger.entries());
        assertEquals(1, ledger.entryCount());
        assertEquals(
                Long.MAX_VALUE,
                ledger.quantity(MiningSessionCategory.TARGET_MINED, resource));
        assertEquals(
                Long.MAX_VALUE,
                ledger.totalItemQuantity(MiningSessionCategory.TARGET_MINED));
        assertTrue(ledger.append(
                accepted(item("TOPAZ"), 1L, MiningSessionCategory.OTHER_MINED, "event-2"),
                MiningSessionPriceResolution.unresolved()));
    }

    @Test
    void aggregateOverflowAcrossDifferentResourcesIsAtomic() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionResource first = item("FIRST");
        MiningSessionResource second = item("SECOND");
        ledger.append(
                accepted(first, Long.MAX_VALUE, MiningSessionCategory.OTHER_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved());

        assertThrows(
                ArithmeticException.class,
                () -> ledger.append(
                        accepted(second, 1L, MiningSessionCategory.OTHER_MINED, "event-2"),
                        MiningSessionPriceResolution.unresolved()));

        assertEquals(1, ledger.entryCount());
        assertEquals(0L, ledger.quantity(MiningSessionCategory.OTHER_MINED, second));
        assertTrue(ledger.append(
                accepted(second, 1L, MiningSessionCategory.TARGET_MINED, "event-2"),
                MiningSessionPriceResolution.unresolved()));
    }

    @Test
    void currencyOverflowIsAtomicAndDoesNotReserveIdentity() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionResource mithril = currency("MITHRIL_POWDER");
        MiningSessionResource gemstone = currency("GEMSTONE_POWDER");
        ledger.append(
                accepted(mithril, Long.MAX_VALUE, MiningSessionCategory.CURRENCY, "event-1"),
                MiningSessionPriceResolution.notApplicable());
        List<MiningSessionLedger.Entry> before = ledger.entries();

        assertThrows(
                ArithmeticException.class,
                () -> ledger.append(
                        accepted(mithril, 1L, MiningSessionCategory.CURRENCY, "event-2"),
                        MiningSessionPriceResolution.notApplicable()));

        assertEquals(before, ledger.entries());
        assertEquals(Long.MAX_VALUE, ledger.totalCurrencyQuantity(mithril));
        assertTrue(ledger.append(
                accepted(gemstone, 1L, MiningSessionCategory.CURRENCY, "event-2"),
                MiningSessionPriceResolution.notApplicable()));
    }

    @Test
    void entrySnapshotCannotMutateInternalLedgerState() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                accepted(item("RUBY"), 10L, MiningSessionCategory.TARGET_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved());
        List<MiningSessionLedger.Entry> snapshot = ledger.entries();

        assertThrows(
                UnsupportedOperationException.class,
                snapshot::clear);
        assertEquals(1, ledger.entryCount());
    }

    @Test
    void entrySnapshotDoesNotChangeAfterLaterAppend() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                accepted(item("RUBY"), 10L, MiningSessionCategory.TARGET_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved());
        List<MiningSessionLedger.Entry> snapshot = ledger.entries();

        ledger.append(
                accepted(item("TOPAZ"), 5L, MiningSessionCategory.OTHER_MINED, "event-2"),
                MiningSessionPriceResolution.unresolved());

        assertEquals(1, snapshot.size());
        assertEquals(2, ledger.entryCount());
    }

    @Test
    void entryConstructorRejectsInvalidPublicState() {
        MiningSessionObservation observation = observation(
                item("RUBY"), 10L, null, null);

        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionLedger.Entry(
                        null,
                        MiningSessionCategory.TARGET_MINED,
                        MiningSessionPriceResolution.unresolved(),
                        null,
                        null,
                        0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionLedger.Entry(
                        observation,
                        null,
                        MiningSessionPriceResolution.unresolved(),
                        null,
                        null,
                        observation.observedAtMillis()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionLedger.Entry(
                        observation,
                        MiningSessionCategory.TARGET_MINED,
                        null,
                        null,
                        null,
                        observation.observedAtMillis()));
        MiningSessionObservation zeroQuantity = observation(
                item("RUBY"), 0L, null, null);
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionLedger.Entry(
                        zeroQuantity,
                        MiningSessionCategory.TARGET_MINED,
                        MiningSessionPriceResolution.unresolved(),
                        null,
                        null,
                        zeroQuantity.observedAtMillis()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionLedger.Entry(
                        observation,
                        MiningSessionCategory.CURRENCY,
                        MiningSessionPriceResolution.unresolved(),
                        null,
                        null,
                        observation.observedAtMillis()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionLedger.Entry(
                        observation,
                        MiningSessionCategory.TARGET_MINED,
                        MiningSessionPriceResolution.unresolved(),
                        null,
                        null,
                        observation.observedAtMillis() + 1L));
    }

    @Test
    void clearResetsOnlyDisconnectedLedger() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionResource resource = item("RUBY");
        ledger.append(
                accepted(resource, 10L, MiningSessionCategory.TARGET_MINED, "event-1"),
                resolved("2.0"));

        ledger.clear();

        assertEquals(0, ledger.entryCount());
        assertEquals(0L, ledger.quantity(MiningSessionCategory.TARGET_MINED, resource));
        assertEquals(BigDecimal.ZERO, ledger.resolvedTotalCoinValue());
    }

    @Test
    void existingMaterialAndGemstoneStatesAreNotMutated() {
        MaterialTrackerState materialState = new MaterialTrackerState();
        materialState.sessionBlocks = 14L;
        materialState.sessionActualRawEquivalent = 900L;
        GemstoneTrackerState gemstoneState = new GemstoneTrackerState();
        gemstoneState.recordBlock(100L);
        gemstoneState.recordGain(GemstoneTier.ROUGH, 800L);

        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                accepted(item("RUBY"), 10L, MiningSessionCategory.TARGET_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved());

        assertEquals(14L, materialState.sessionBlocks);
        assertEquals(900L, materialState.sessionActualRawEquivalent);
        assertEquals(1L, gemstoneState.sessionBlocks);
        assertEquals(800L, gemstoneState.sessionLedger().quantity(GemstoneTier.ROUGH));
    }

    @Test
    void priceResolutionFactoriesEnforceStatusInvariants() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionPriceResolution.resolved(
                        MiningSessionPriceResolution.PriceSource.NONE,
                        BigDecimal.ONE,
                        1L));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionPriceResolution.resolved(
                        MiningSessionPriceResolution.PriceSource.NPC_SELL,
                        new BigDecimal("-0.01"),
                        1L));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionPriceResolution.unavailable(-1L));
        assertTrue(
                MiningSessionPriceResolution.unresolved()
                        .estimatedTotal(10L)
                        .isEmpty());
        assertTrue(
                resolved("1.5")
                        .estimatedTotal(0L)
                        .isEmpty());
    }

    @Test
    void currencyAndItemCategoriesCannotBeMixed() {
        MiningSessionLedger ledger = new MiningSessionLedger();

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.append(
                        accepted(
                                currency("POWDER"),
                                1L,
                                MiningSessionCategory.CHEST_LOOT,
                                "event-1"),
                        MiningSessionPriceResolution.notApplicable()));
        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.append(
                        accepted(
                                item("GOBLIN_EGG"),
                                1L,
                                MiningSessionCategory.CURRENCY,
                                "event-2"),
                        MiningSessionPriceResolution.notApplicable()));
        assertEquals(0, ledger.entryCount());
    }

    @Test
    void resourceFactoriesKeepTypedIdentitiesSeparate() {
        MiningSessionResource material = MiningSessionResource.material(
                "TITANIUM_ORE",
                "Titanium",
                TrackedMaterial.TITANIUM);
        MiningSessionResource gemstone = MiningSessionResource.gemstone(
                "ROUGH_RUBY_GEM",
                "Rough Ruby Gemstone",
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);
        MiningSessionResource currency = currency("MITHRIL_POWDER");

        assertEquals(TrackedMaterial.TITANIUM, material.material());
        assertNull(material.gemstone());
        assertEquals(GemstoneType.RUBY, gemstone.gemstone());
        assertEquals(GemstoneTier.ROUGH, gemstone.gemstoneTier());
        assertNull(gemstone.material());
        assertNull(currency.material());
        assertNull(currency.gemstone());
    }

    @Test
    void semanticResourceIdentitiesAggregateSeparately() {
        MiningSessionResource generic = MiningSessionResource.genericItem(
                "ROUGH_RUBY_GEM",
                "Generic Ruby");
        MiningSessionResource typed = MiningSessionResource.gemstone(
                "ROUGH_RUBY_GEM",
                "Rough Ruby Gemstone",
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(generic, 3L, MiningSessionCategory.OTHER_MINED, "event-1"),
                MiningSessionPriceResolution.unresolved());
        ledger.append(
                accepted(typed, 7L, MiningSessionCategory.OTHER_MINED, "event-2"),
                MiningSessionPriceResolution.unresolved());

        assertEquals(3L, ledger.quantity(MiningSessionCategory.OTHER_MINED, generic));
        assertEquals(7L, ledger.quantity(MiningSessionCategory.OTHER_MINED, typed));
        assertEquals(10L, ledger.totalItemQuantity(MiningSessionCategory.OTHER_MINED));
    }

    @Test
    void typedAndGenericSameIdClassifyAndAggregateIndependently() {
        MiningSessionResource generic = MiningSessionResource.genericItem(
                "ROUGH_RUBY_GEM",
                "Generic Ruby");
        MiningSessionResource typed = MiningSessionResource.gemstone(
                "ROUGH_RUBY_GEM",
                "Rough Ruby Gemstone",
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);
        MiningSessionClassifier.ClassificationContext context =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        1L,
                        true,
                        true,
                        false,
                        true,
                        true,
                        false);
        MiningSessionClassification genericClassification =
                MiningSessionClassifier.classify(
                        observation(generic, 3L, "event-1", "batch-1"),
                        context);
        MiningSessionClassification typedClassification =
                MiningSessionClassifier.classify(
                        observation(typed, 7L, "event-2", "batch-2"),
                        context);
        MiningSessionLedger ledger = new MiningSessionLedger();

        assertEquals(MiningSessionCategory.OTHER_MINED, genericClassification.category());
        assertEquals(MiningSessionCategory.TARGET_MINED, typedClassification.category());
        assertTrue(ledger.append(genericClassification, MiningSessionPriceResolution.unresolved()));
        assertTrue(ledger.append(typedClassification, MiningSessionPriceResolution.unresolved()));
        assertEquals(3L, ledger.quantity(MiningSessionCategory.OTHER_MINED, generic));
        assertEquals(7L, ledger.quantity(MiningSessionCategory.TARGET_MINED, typed));
    }

    private static void assertCategoryQuantity(
            MiningSessionCategory category) {
        MiningSessionResource resource = item(category.name());
        MiningSessionLedger ledger = new MiningSessionLedger();

        ledger.append(
                accepted(resource, 7L, category, "event-1"),
                MiningSessionPriceResolution.unresolved());
        ledger.append(
                accepted(resource, 5L, category, "event-2"),
                MiningSessionPriceResolution.unresolved());

        assertEquals(12L, ledger.quantity(category, resource));
        assertEquals(12L, ledger.totalItemQuantity(category));
    }

    private static void assertPriceDoesNotContribute(
            MiningSessionPriceResolution price) {
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                accepted(item("GOLD"), 4L, MiningSessionCategory.TARGET_MINED, "event-1"),
                price);

        assertEquals(BigDecimal.ZERO, ledger.resolvedCoinValue(
                MiningSessionCategory.TARGET_MINED));
        assertEquals(BigDecimal.ZERO, ledger.resolvedTotalCoinValue());
    }

    private static void assertEventConflict(
            MiningSessionObservation conflictingObservation,
            MiningSessionCategory conflictingCategory) {
        MiningSessionLedger ledger = ledgerWithOriginalEvent();
        List<MiningSessionLedger.Entry> before = ledger.entries();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ledger.append(
                        MiningSessionClassification.wouldCredit(
                                conflictingObservation,
                                conflictingCategory),
                        resolved("999.00")));

        assertTrue(exception.getMessage().contains("event"));
        assertEquals(before, ledger.entries());
        assertEquals(
                10L,
                ledger.quantity(
                        MiningSessionCategory.TARGET_MINED,
                        item("RUBY")));
        assertEquals(10L, ledger.totalItemQuantity(MiningSessionCategory.TARGET_MINED));
        assertEquals(0L, ledger.totalItemQuantity(MiningSessionCategory.OTHER_MINED));
        assertEquals(0L, ledger.totalItemQuantity(MiningSessionCategory.CHEST_LOOT));
        assertEquals(0L, ledger.totalCurrencyQuantity(currency("MITHRIL_POWDER")));
        assertEquals(
                new BigDecimal("20.00"),
                ledger.resolvedCoinValue(MiningSessionCategory.TARGET_MINED));
        assertEquals(new BigDecimal("20.00"), ledger.resolvedTotalCoinValue());
    }

    private static void assertCorrelationConflict(
            MiningSessionObservation conflictingObservation) {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionClassification original =
                MiningSessionClassification.wouldCredit(
                        observation(item("RUBY"), 10L, null, "batch-1"),
                        MiningSessionCategory.TARGET_MINED);
        ledger.append(original, MiningSessionPriceResolution.unresolved());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> ledger.append(
                        MiningSessionClassification.wouldCredit(
                                conflictingObservation,
                                MiningSessionCategory.TARGET_MINED),
                        MiningSessionPriceResolution.unresolved()));

        assertTrue(exception.getMessage().contains("correlation"));
        assertEquals(1, ledger.entryCount());
        assertEquals(10L, ledger.totalItemQuantity(MiningSessionCategory.TARGET_MINED));
    }

    private static MiningSessionLedger ledgerWithOriginalEvent() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionObservation original = observation(
                item("RUBY"),
                10L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                1L,
                2_000L,
                "event-1",
                "batch-1",
                "Gemstones Sack",
                "private diagnostic");
        ledger.append(
                MiningSessionClassification.wouldCredit(
                        original,
                        MiningSessionCategory.TARGET_MINED),
                resolved("2.00"));
        return ledger;
    }

    private static MiningSessionClassification accepted(
            MiningSessionResource resource,
            long quantity,
            MiningSessionCategory category,
            String eventId) {
        return MiningSessionClassification.wouldCredit(
                observation(resource, quantity, eventId, "batch-1"),
                category);
    }

    private static MiningSessionObservation observation(
            MiningSessionResource resource,
            long quantity,
            String eventId,
            String correlationId) {
        return observation(
                resource,
                quantity,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                1L,
                2_000L,
                eventId,
                correlationId,
                "Gemstones Sack",
                null);
    }

    private static MiningSessionObservation observation(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidenceType,
            TrackerSelection selectedTracker,
            long selectionEpoch,
            long observedAtMillis,
            String eventId,
            String correlationId,
            String sourceName,
            String diagnosticText) {
        return new MiningSessionObservation(
                resource,
                quantity,
                evidenceType,
                selectedTracker,
                selectionEpoch,
                observedAtMillis,
                eventId,
                correlationId,
                sourceName,
                diagnosticText);
    }

    private static MiningSessionResource item(String id) {
        return MiningSessionResource.genericItem(id, id);
    }

    private static MiningSessionResource currency(String id) {
        return MiningSessionResource.currency(id, id);
    }

    private static MiningSessionPriceResolution resolved(String price) {
        return MiningSessionPriceResolution.resolved(
                MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL,
                new BigDecimal(price),
                1_500L);
    }

    @Test
    void appendAllAtomicallyAppliesCompleteBatch() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        List<MiningSessionLedger.BatchEntry> batch = List.of(
                batchEntry(item("RUBY"), 2L, "chest-1", 1_000L),
                batchEntry(item("TOPAZ"), 3L, "chest-2", 1_001L));

        MiningSessionLedger.BatchAppendResult result =
                ledger.appendAllAtomically(batch);

        assertTrue(result.applied());
        assertEquals(2, result.appliedCount());
        assertEquals(2, ledger.entryCount());
    }

    @Test
    void appendAllAtomicallyAppliesMixedChestLootAndCurrencyBatch() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionResource powder = currency("GEMSTONE_POWDER");
        List<MiningSessionLedger.BatchEntry> batch = List.of(
                batchEntry(item("RUBY"), 2L, "chest-1", 1_000L),
                new MiningSessionLedger.BatchEntry(
                        MiningSessionClassification.wouldCredit(
                                new MiningSessionObservation(
                                        powder,
                                        296L,
                                        MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                                        TrackerSelection.RUBY,
                                        1L,
                                        1_001L,
                                        "chest-2",
                                        "chest-ctx-1",
                                        "powder-chest-chat",
                                        null),
                                MiningSessionCategory.CURRENCY),
                        MiningSessionPriceResolution.unresolved()));

        MiningSessionLedger.BatchAppendResult result =
                ledger.appendAllAtomically(batch);

        assertTrue(result.applied());
        assertEquals(2, result.appliedCount());
        assertEquals(2, ledger.entryCount());
        assertEquals(2L, ledger.quantity(
                MiningSessionCategory.CHEST_LOOT,
                item("RUBY")));
        assertEquals(296L, ledger.quantity(
                MiningSessionCategory.CURRENCY,
                powder));
    }

    @Test
    void appendAllAtomicallyMixedBatchConflictLeavesLedgerUnchanged() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                accepted(item("RUBY"), 2L, MiningSessionCategory.CHEST_LOOT, "chest-1"),
                MiningSessionPriceResolution.unresolved());

        MiningSessionResource powder = currency("GEMSTONE_POWDER");
        MiningSessionLedger.BatchAppendResult result =
                ledger.appendAllAtomically(List.of(
                        batchEntry(item("TOPAZ"), 1L, "chest-1", 2_000L),
                        new MiningSessionLedger.BatchEntry(
                                MiningSessionClassification.wouldCredit(
                                        new MiningSessionObservation(
                                                powder,
                                                50L,
                                                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                                                TrackerSelection.RUBY,
                                                1L,
                                                2_001L,
                                                "chest-2",
                                                "chest-ctx-1",
                                                "powder-chest-chat",
                                                null),
                                        MiningSessionCategory.CURRENCY),
                                MiningSessionPriceResolution.unresolved())));

        assertEquals(
                MiningSessionLedger.BatchAppendOutcome.REJECTED_CONFLICT,
                result.outcome());
        assertEquals(1, ledger.entryCount());
        assertEquals(0L, ledger.quantity(
                MiningSessionCategory.CURRENCY,
                powder));
    }

    @Test
    void appendAllAtomicallyRejectsConflictingIdentityWithoutMutation() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                accepted(item("RUBY"), 2L, MiningSessionCategory.CHEST_LOOT, "chest-1"),
                MiningSessionPriceResolution.unresolved());

        MiningSessionLedger.BatchAppendResult result =
                ledger.appendAllAtomically(List.of(
                        batchEntry(item("RUBY"), 9L, "chest-1", 2_000L),
                        batchEntry(item("TOPAZ"), 1L, "chest-2", 2_001L)));

        assertEquals(
                MiningSessionLedger.BatchAppendOutcome.REJECTED_CONFLICT,
                result.outcome());
        assertEquals(1, ledger.entryCount());
        assertEquals(2L, ledger.quantity(
                MiningSessionCategory.CHEST_LOOT,
                item("RUBY")));
        assertEquals(0L, ledger.quantity(
                MiningSessionCategory.CHEST_LOOT,
                item("TOPAZ")));
    }

    @Test
    void appendAllAtomicallyRejectsDuplicateIdentityInsideBatch() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionLedger.BatchAppendResult result =
                ledger.appendAllAtomically(List.of(
                        batchEntry(item("RUBY"), 2L, "chest-1", 1_000L),
                        batchEntry(item("RUBY"), 2L, "chest-1", 1_001L)));

        assertEquals(
                MiningSessionLedger.BatchAppendOutcome.REJECTED_DUPLICATE_IN_BATCH,
                result.outcome());
        assertEquals(0, ledger.entryCount());
    }

    @Test
    void appendAllAtomicallyRejectsExactExistingDuplicateWithoutMutation() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningSessionClassification original = accepted(
                item("RUBY"),
                2L,
                MiningSessionCategory.CHEST_LOOT,
                "chest-1");
        ledger.append(
                original,
                MiningSessionPriceResolution.unresolved());

        MiningSessionLedger.BatchAppendResult result =
                ledger.appendAllAtomically(List.of(
                        new MiningSessionLedger.BatchEntry(
                                original,
                                MiningSessionPriceResolution.unresolved()),
                        batchEntry(item("TOPAZ"), 1L, "chest-2", 1_001L)));

        assertEquals(
                MiningSessionLedger.BatchAppendOutcome.REJECTED_DUPLICATE_EXISTING,
                result.outcome());
        assertEquals(1, ledger.entryCount());
    }

    private static MiningSessionLedger.BatchEntry batchEntry(
            MiningSessionResource resource,
            long quantity,
            String eventId,
            long observedAtMillis) {
        return new MiningSessionLedger.BatchEntry(
                MiningSessionClassification.wouldCredit(
                        observation(
                                resource,
                                quantity,
                                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                                TrackerSelection.RUBY,
                                1L,
                                observedAtMillis,
                                eventId,
                                "chest-context",
                                "powder-chest-chat",
                                null),
                        MiningSessionCategory.CHEST_LOOT),
                MiningSessionPriceResolution.unresolved());
    }
}
