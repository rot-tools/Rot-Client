package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionShadowObserverTest {
    @Test
    void nonTargetRoughSackWouldCredit() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L);

        assertTrue(result.appended());
        assertEquals(1, fixture.observer.entryCount());
        assertTrue(fixture.sink.contains("OTHER_MINING_WOULD_CREDIT"));
    }

    @Test
    void nonTargetPristineWouldCreditFlawedResource() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observePristine(
                        new PristineMessageParser.Reward(GemstoneType.TOPAZ, 4L),
                        110L);

        assertTrue(result.appended());
        MiningSessionLedger.Entry entry = fixture.observer.entries().getFirst();
        assertEquals("FLAWED_TOPAZ_GEM",
                entry.observation().resource().resourceId());
        assertEquals(4L, entry.observation().quantity());
    }

    @Test
    void oneGemstoneBreakCanCreditItsIndependentSackAndPristineChannels() {
        Fixture sackFirst = fixture(TrackerSelection.RUBY);
        sackFirst.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        assertTrue(sackFirst.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L).appended());
        assertTrue(sackFirst.observer.observePristine(
                new PristineMessageParser.Reward(GemstoneType.TOPAZ, 4L),
                111L).appended());
        assertEquals(2, sackFirst.observer.entryCount());

        Fixture pristineFirst = fixture(TrackerSelection.RUBY);
        pristineFirst.observer.onConfirmedGemstoneBreak(
                GemstoneType.TOPAZ, 200L);
        assertTrue(pristineFirst.observer.observePristine(
                new PristineMessageParser.Reward(GemstoneType.TOPAZ, 4L),
                210L).appended());
        assertTrue(pristineFirst.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                211L).appended());
        assertEquals(2, pristineFirst.observer.entryCount());
    }

    @Test
    void flawedSackIsConfirmationOnlyAndPreservesPristineChannel() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        MiningSessionShadowObserver.ObservationResult sackResult =
                fixture.observer.observeSackChangesForTest(
                        sack("Flawed Topaz Gemstone", 4L, "Gemstone Sack"),
                        110L);
        MiningSessionShadowObserver.ObservationResult pristineResult =
                fixture.observer.observePristine(
                        new PristineMessageParser.Reward(
                                GemstoneType.TOPAZ, 4L),
                        120L);

        assertEquals(
                MiningSessionClassification.ReasonCode.UNSUPPORTED_EVIDENCE,
                sackResult.reason());
        assertFalse(sackResult.appended());
        assertTrue(pristineResult.appended());
        assertEquals(1, fixture.observer.entryCount());
    }

    @Test
    void selectedGemstoneIsExcludedFromOtherMined() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.RUBY, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Ruby Gemstone", 80L, "Gemstones Sack"),
                        110L);

        assertFalse(result.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode.TARGET_EXCLUDED_FROM_OTHERS,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
        assertTrue(fixture.sink.contains(
                "OTHER_MINING_REJECTED",
                "reason=TARGET_EXCLUDED_FROM_OTHERS"));
    }

    @Test
    void titaniumIsExcludedFromCombinedTarget() {
        Fixture fixture = fixture(TrackerSelection.MITHRIL_TITANIUM);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.TITANIUM, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Titanium", 5L, "Dwarven Sack"),
                        110L);

        assertEquals(
                MiningSessionClassification.ReasonCode.TARGET_EXCLUDED_FROM_OTHERS,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void nonTargetMaterialWithExactEvidenceWouldCredit() {
        Fixture fixture = fixture(TrackerSelection.DIAMOND);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.GOLD, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Gold Ingot", 7L, "Mining Sack"),
                        110L);

        assertTrue(result.appended());
        assertEquals(7L, fixture.observer.quantity(
                fixture.catalog.fromMaterial(TrackedMaterial.GOLD)
                        .orElseThrow().resource()));
    }

    @Test
    void enchantedMaterialUsesVerifiedEnchantedMiningSackSource() {
        Fixture fixture = fixture(TrackerSelection.DIAMOND);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.GOLD, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Enchanted Gold", 2L, "Enchanted Mining Sack"),
                        110L);

        assertTrue(result.appended());
        assertEquals("ENCHANTED_GOLD",
                fixture.observer.entries().getFirst()
                        .observation().resource().resourceId());
    }

    @Test
    void hardStoneMinedWhileRubySelectedWouldCreditOtherMined() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Hard Stone", 576L, "Mining Sack"),
                        110L);

        assertTrue(result.appended());
        assertEquals(
                MiningSessionCategory.OTHER_MINED,
                fixture.observer.entries().getFirst().category());
        assertEquals(
                "HARD_STONE",
                fixture.observer.entries().getFirst()
                        .observation().resource().resourceId());
        assertEquals(
                576L,
                fixture.observer.entries().getFirst()
                        .observation().quantity());
    }

    /**
     * Reproduces the observed runtime timing from field diagnostics: a
     * confirmed Hard Stone break while Gold is the live target, followed by
     * the matching Mining Sack delta 7,320 ms later (the upper bound of the
     * 5.456-7.320 s delay window actually observed in-game). Exercises the
     * full production path: MiningSessionShadowObserver -&gt;
     * MiningResourceCatalog alias resolution -&gt;
     * MiningSessionDirectBreakTracker correlation and consumption -&gt;
     * MiningSessionLedger append.
     */
    @Test
    void hardStoneSackDeltaAfterObservedRuntimeDelayCreditsOtherMined() {
        Fixture fixture = fixture(TrackerSelection.GOLD);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Hard Stone", 576L, "Mining Sack"),
                        7_420L,
                        0L,
                        "runtime-component");

        assertTrue(result.appended());
        assertEquals(1, fixture.observer.entryCount());
        MiningSessionLedger.Entry entry =
                fixture.observer.entries().getFirst();
        assertEquals(MiningSessionCategory.OTHER_MINED, entry.category());
        assertEquals(
                "HARD_STONE",
                entry.observation().resource().resourceId());
        assertEquals(576L, entry.observation().quantity());
        assertEquals(
                0L,
                fixture.ledger.totalItemQuantity(
                        MiningSessionCategory.TARGET_MINED));

        // The matched context's only channel (MATERIAL_QUANTITY) has been
        // consumed, so no pending context remains to credit again.
        assertEquals(0, fixture.observer.pendingContextCount());

        MiningSessionShadowObserver.ObservationResult exhaustedCorrelation =
                fixture.observer.observeSackChangesForTest(
                        sack("Hard Stone", 1L, "Mining Sack"),
                        7_421L);
        assertFalse(exhaustedCorrelation.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode
                        .MISSING_MINING_CORRELATION,
                exhaustedCorrelation.reason());
        assertEquals(1, fixture.observer.entryCount());

        MiningSessionShadowObserver.ObservationResult duplicate =
                fixture.observer.observeSackChangesForTest(
                        sack("Hard Stone", 576L, "Mining Sack"),
                        7_422L,
                        0L,
                        "runtime-component");
        assertFalse(duplicate.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY,
                duplicate.reason());
        assertEquals(1, fixture.observer.entryCount());
    }

    @Test
    void hardStoneWithoutMiningContextIsRejected() {
        Fixture fixture = fixture(TrackerSelection.RUBY);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Hard Stone", 576L, "Mining Sack"),
                        110L);

        assertFalse(result.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode
                        .MISSING_MINING_CORRELATION,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void enchantedHardStoneUsesExactCanonicalIdentity() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack(
                                "Enchanted Hard Stone",
                                2L,
                                "Enchanted Mining Sack"),
                        110L);

        assertTrue(result.appended());
        assertEquals(
                "ENCHANTED_HARD_STONE",
                fixture.observer.entries().getFirst()
                        .observation().resource().resourceId());
        assertEquals(
                TrackedMaterial.HARD_STONE,
                fixture.observer.entries().getFirst()
                        .observation().resource().material());
        assertEquals(
                2L,
                fixture.observer.entries().getFirst()
                        .observation().quantity());
    }

    @Test
    void positiveSackWithoutCorrelationIsUnresolved() {
        Fixture fixture = fixture(TrackerSelection.RUBY);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        100L);

        assertEquals(
                MiningSessionClassification.ReasonCode.MISSING_MINING_CORRELATION,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void negativeSackIsRejectedAndPreservesContext() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        MiningSessionShadowObserver.ObservationResult negative =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", -80L, "Gemstone Sack"),
                        110L);
        MiningSessionShadowObserver.ObservationResult positive =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        111L);

        assertEquals(
                MiningSessionClassification.ReasonCode.NON_POSITIVE_QUANTITY,
                negative.reason());
        assertTrue(positive.appended());
    }

    @Test
    void wrongSackSourceIsRejectedAndPreservesContext() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        MiningSessionShadowObserver.ObservationResult wrong =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Combat Sack"),
                        110L);
        MiningSessionShadowObserver.ObservationResult valid =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        111L);

        assertEquals(
                MiningSessionClassification.ReasonCode.UNSUPPORTED_SOURCE,
                wrong.reason());
        assertTrue(valid.appended());
    }

    @Test
    void unknownItemIsUnresolved() {
        Fixture fixture = fixture(TrackerSelection.RUBY);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Unknown Player Item", 3L, "Gemstone Sack"),
                        100L);

        assertEquals(
                MiningSessionClassification.ReasonCode.UNKNOWN_RESOURCE,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void manualTransferIsRejectedAndPreservesContext() {
        assertExplicitTransferRejected(
                MiningSessionObservation.EvidenceType.MANUAL_TRANSFER,
                MiningSessionClassification.ReasonCode.EXPLICIT_NON_MINING_SOURCE);
    }

    @Test
    void bazaarPurchaseIsRejectedAndPreservesContext() {
        assertExplicitTransferRejected(
                MiningSessionObservation.EvidenceType.BAZAAR_PURCHASE,
                MiningSessionClassification.ReasonCode.EXPLICIT_NON_MINING_SOURCE);
    }

    @Test
    void duplicateDeliveryCreatesAtMostOneEntry() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 101L);

        MiningSessionShadowObserver.ObservationResult first =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L,
                        0L,
                        "same-component");
        MiningSessionShadowObserver.ObservationResult duplicate =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        111L,
                        0L,
                        "same-component");

        assertTrue(first.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY,
                duplicate.reason());
        assertEquals(1, fixture.observer.entryCount());
        assertEquals(2, fixture.observer.pendingContextCount());
    }

    @Test
    void equalIndependentDeliveriesUseDistinctBreakContextsWithinWindow() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 101L);

        MiningSessionShadowObserver.ObservationResult first =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L);
        MiningSessionShadowObserver.ObservationResult second =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        111L);

        assertTrue(first.appended());
        assertTrue(second.appended());
        assertFalse(first.contextId().equals(second.contextId()));
        assertEquals(2, fixture.observer.entryCount());
    }

    @Test
    void separateContextAfterDedupeWindowIsAccepted() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 101L);
        fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L);

        MiningSessionShadowObserver.ObservationResult second =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        2_611L);

        assertTrue(second.appended());
        assertEquals(2, fixture.observer.entryCount());
    }

    @Test
    void targetTransitionInvalidatesOldContext() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        fixture.observer.onSelectedTargetChanged(TrackerSelection.AMBER, true);
        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L);

        assertEquals(
                MiningSessionClassification.ReasonCode.MISSING_MINING_CORRELATION,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void resetInvalidatesOldContext() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.onReset();

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L);

        assertEquals(
                MiningSessionClassification.ReasonCode.MISSING_MINING_CORRELATION,
                result.reason());
    }

    @Test
    void trackerOffPreventsEntries() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.onTrackerDisabled();

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L);

        assertFalse(result.appended());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void worldTransitionClearsContext() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.onWorldTransition();

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L);

        assertEquals(
                MiningSessionClassification.ReasonCode.MISSING_MINING_CORRELATION,
                result.reason());
    }

    @Test
    void pendingContextUsesExplicitWindowAcrossPauseBoundary() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        assertTrue(fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                60_100L).appended());
        assertFalse(fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                60_101L).appended());
    }

    @Test
    void liveStateFixturesRemainUnchanged() {
        MaterialTrackerState material = new MaterialTrackerState();
        material.sessionBlocks = 17L;
        GemstoneTrackerState gemstone = new GemstoneTrackerState();
        gemstone.recordBlock(100L);
        Fixture fixture = fixture(TrackerSelection.RUBY);

        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L);

        assertEquals(17L, material.sessionBlocks);
        assertEquals(1L, gemstone.sessionBlocks);
        assertEquals(0L, gemstone.sessionLedger().totalItemCount());
    }

    @Test
    void observerResetClearsOnlyObserverState() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L);
        MaterialTrackerState external = new MaterialTrackerState();
        external.sessionBlocks = 9L;

        fixture.observer.onReset();

        assertEquals(0, fixture.observer.entryCount());
        assertEquals(0, fixture.observer.pendingContextCount());
        assertEquals(9L, external.sessionBlocks);
    }

    @Test
    void appendedEntriesAreOnlyOtherMined() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L);

        assertTrue(fixture.observer.entries().stream().allMatch(entry ->
                entry.category() == MiningSessionCategory.OTHER_MINED));
        assertFalse(fixture.observer.entries().stream().anyMatch(entry ->
                entry.category() == MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void currencyCannotBeAppended() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        MiningSessionResource currency = MiningSessionResource.currency(
                "MITHRIL_POWDER", "Mithril Powder");

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeExplicitTransfer(
                        currency,
                        10L,
                        MiningSessionObservation.EvidenceType.MANUAL_TRANSFER,
                        100L);

        assertFalse(result.appended());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void acceptedEntryNeverResolvesPrice() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L);

        assertEquals(
                MiningSessionPriceResolution.PriceStatus.UNRESOLVED,
                fixture.observer.entries().getFirst().priceResolution().status());
    }

    @Test
    void diagnosticsContainRequiredFieldsWithoutRawUnknownText() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L);
        fixture.observer.observeSackChangesForTest(
                sack("Private Player Payload", 1L, "Gemstone Sack"),
                120L);

        String credit = fixture.sink.details("OTHER_MINING_WOULD_CREDIT");
        assertTrue(credit.contains("category=OTHER_MINED"));
        assertTrue(credit.contains("resourceId=ROUGH_TOPAZ_GEM"));
        assertTrue(credit.contains("selectionEpoch="));
        assertTrue(credit.contains("contextId=context-1"));
        assertTrue(credit.contains("ephemeralEntryCount=1"));
        assertFalse(fixture.sink.allDetails().contains("Private Player Payload"));
    }

    @Test
    void ledgerIdentityConflictIsAtomicAndVisible() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        MiningSessionResource resource = fixture.catalog.fromGemstone(
                GemstoneType.TOPAZ, GemstoneTier.ROUGH)
                .orElseThrow().resource();
        MiningSessionObservation conflictingBaseline = new MiningSessionObservation(
                resource,
                99L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                fixture.observer.selectionEpoch(),
                50L,
                "other-mining:context-1:SACK_QUANTITY",
                "context-1",
                "Gemstone Sack",
                null);
        fixture.ledger.append(
                MiningSessionClassification.wouldCredit(
                        conflictingBaseline,
                        MiningSessionCategory.OTHER_MINED),
                MiningSessionPriceResolution.unresolved());
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        assertThrows(
                IllegalStateException.class,
                () -> fixture.observer.observeSackChangesForTest(
                        sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                        110L));
        assertEquals(1, fixture.observer.entryCount());
        assertEquals(1, fixture.observer.pendingContextCount());
    }

    @Test
    void explicitTimestampsProduceDeterministicResults() {
        Fixture first = fixture(TrackerSelection.RUBY);
        Fixture second = fixture(TrackerSelection.RUBY);

        runDeterministicScenario(first);
        runDeterministicScenario(second);

        assertEquals(first.observer.entries(), second.observer.entries());
        assertEquals(first.sink.markers, second.sink.markers);
    }

    @Test
    void cobblestoneInventoryGainWithoutSackWouldCreditOtherMined() {
        // Cobblestone has no Mining Sack at all; INVENTORY_CHANGE evidence
        // is its only possible delivery path.
        Fixture fixture = fixture(TrackerSelection.GOLD);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.COBBLESTONE, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeMaterialInventoryGain(
                        TrackedMaterial.COBBLESTONE, 1L, 110L);

        assertTrue(result.appended());
        assertEquals(1, fixture.observer.entryCount());
        MiningSessionLedger.Entry entry =
                fixture.observer.entries().getFirst();
        assertEquals(MiningSessionCategory.OTHER_MINED, entry.category());
        assertEquals(
                "COBBLESTONE",
                entry.observation().resource().resourceId());
        assertEquals(
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                entry.observation().evidenceType());
        assertTrue(fixture.sink.contains("OTHER_MINING_WOULD_CREDIT"));
    }

    @Test
    void hardStoneInventoryGainFallsBackWhenSackNotificationIsMissed() {
        // Reproduces a missed/delayed [Sacks] chat notification: the raw
        // inventory delta is still exact evidence and should credit
        // OTHER_MINED on its own via the direct-inventory fallback path.
        Fixture fixture = fixture(TrackerSelection.GOLD);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeMaterialInventoryGain(
                        TrackedMaterial.HARD_STONE, 1L, 110L);

        assertTrue(result.appended());
        assertEquals(
                "HARD_STONE",
                fixture.observer.entries().getFirst()
                        .observation().resource().resourceId());
        assertEquals(0, fixture.observer.pendingContextCount());
    }

    @Test
    void inventoryGainAndSackDeliveryCannotBothCreditTheSameBreak() {
        // Whichever evidence type arrives first consumes the pending
        // context; the other is safely rejected as uncorrelated, so a
        // single physical pickup is never double-counted.
        Fixture fixture = fixture(TrackerSelection.GOLD);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);

        MiningSessionShadowObserver.ObservationResult inventoryResult =
                fixture.observer.observeMaterialInventoryGain(
                        TrackedMaterial.HARD_STONE, 1L, 105L);
        MiningSessionShadowObserver.ObservationResult delayedSackResult =
                fixture.observer.observeSackChangesForTest(
                        sack("Hard Stone", 1L, "Mining Sack"),
                        7_000L);

        assertTrue(inventoryResult.appended());
        assertFalse(delayedSackResult.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode
                        .MISSING_MINING_CORRELATION,
                delayedSackResult.reason());
        assertEquals(1, fixture.observer.entryCount());
    }

    @Test
    void targetMaterialInventoryGainIsExcludedFromOthers() {
        Fixture fixture = fixture(TrackerSelection.GOLD);
        fixture.observer.onConfirmedMaterialBreak(
                TrackedMaterial.GOLD, 1, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeMaterialInventoryGain(
                        TrackedMaterial.GOLD, 1L, 110L);

        assertFalse(result.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode
                        .TARGET_EXCLUDED_FROM_OTHERS,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void materialInventoryGainWithoutMiningContextIsUnresolved() {
        Fixture fixture = fixture(TrackerSelection.GOLD);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeMaterialInventoryGain(
                        TrackedMaterial.COBBLESTONE, 1L, 110L);

        assertFalse(result.appended());
        assertEquals(
                MiningSessionClassification.ReasonCode
                        .MISSING_MINING_CORRELATION,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
    }

    @Test
    void genericInventoryIncreaseRemainsUnresolved() {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        MiningSessionResource resource = fixture.catalog.fromGemstone(
                GemstoneType.TOPAZ, GemstoneTier.ROUGH)
                .orElseThrow().resource();
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeInventory(resource, 80L, 110L);

        assertEquals(
                MiningSessionClassification.ReasonCode.MISSING_EXACT_QUANTITY_EVIDENCE,
                result.reason());
        assertEquals(0, fixture.observer.entryCount());
        assertEquals(1, fixture.observer.pendingContextCount());
    }

    private static void assertExplicitTransferRejected(
            MiningSessionObservation.EvidenceType evidence,
            MiningSessionClassification.ReasonCode expectedReason) {
        Fixture fixture = fixture(TrackerSelection.RUBY);
        MiningSessionResource resource = fixture.catalog.fromGemstone(
                GemstoneType.TOPAZ, GemstoneTier.ROUGH)
                .orElseThrow().resource();
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);

        MiningSessionShadowObserver.ObservationResult result =
                fixture.observer.observeExplicitTransfer(
                        resource, 80L, evidence, 110L);

        assertEquals(expectedReason, result.reason());
        assertEquals(0, fixture.observer.entryCount());
        assertEquals(1, fixture.observer.pendingContextCount());
    }

    private static void runDeterministicScenario(Fixture fixture) {
        fixture.observer.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        fixture.observer.observeSackChangesForTest(
                sack("Rough Topaz Gemstone", 80L, "Gemstone Sack"),
                110L);
    }

    private static Fixture fixture(TrackerSelection selection) {
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MiningSessionDirectBreakTracker tracker =
                new MiningSessionDirectBreakTracker();
        MiningSessionLedger ledger = new MiningSessionLedger();
        CaptureSink sink = new CaptureSink();
        MiningSessionShadowObserver observer = new MiningSessionShadowObserver(
                catalog, tracker, ledger, sink);
        observer.onDiagnosticStarted(true, selection);
        return new Fixture(catalog, ledger, sink, observer);
    }

    private static SackChangeParser.Change sack(
            String item,
            long quantity,
            String source) {
        return new SackChangeParser.Change(
                quantity, item, List.of(source));
    }

    private record Fixture(
            MiningResourceCatalog catalog,
            MiningSessionLedger ledger,
            CaptureSink sink,
            MiningSessionShadowObserver observer) {
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        private final List<Marker> markers = new ArrayList<>();

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void record(String marker, String details) {
            markers.add(new Marker(marker, details));
        }

        private boolean contains(String marker) {
            return markers.stream().anyMatch(value -> value.name.equals(marker));
        }

        private boolean contains(String marker, String text) {
            return markers.stream().anyMatch(value ->
                    value.name.equals(marker) && value.details.contains(text));
        }

        private String details(String marker) {
            return markers.stream()
                    .filter(value -> value.name.equals(marker))
                    .map(Marker::details)
                    .findFirst()
                    .orElseThrow();
        }

        private String allDetails() {
            return markers.toString();
        }
    }

    private record Marker(String name, String details) {
    }
}
