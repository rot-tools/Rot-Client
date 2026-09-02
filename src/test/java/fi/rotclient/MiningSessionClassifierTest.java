package fi.rotclient;

import org.junit.jupiter.api.Test;

import static fi.rotclient.MiningSessionClassification.Outcome.REJECTED;
import static fi.rotclient.MiningSessionClassification.Outcome.UNRESOLVED;
import static fi.rotclient.MiningSessionClassification.Outcome.WOULD_CREDIT;
import static fi.rotclient.MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY;
import static fi.rotclient.MiningSessionClassification.ReasonCode.EXPLICIT_NON_MINING_SOURCE;
import static fi.rotclient.MiningSessionClassification.ReasonCode.INACTIVE_MINING_CONTEXT;
import static fi.rotclient.MiningSessionClassification.ReasonCode.INSUFFICIENT_EVIDENCE;
import static fi.rotclient.MiningSessionClassification.ReasonCode.MISSING_CHEST_CONTEXT;
import static fi.rotclient.MiningSessionClassification.ReasonCode.MISSING_EXACT_QUANTITY_EVIDENCE;
import static fi.rotclient.MiningSessionClassification.ReasonCode.MISSING_MINING_CORRELATION;
import static fi.rotclient.MiningSessionClassification.ReasonCode.NON_POSITIVE_QUANTITY;
import static fi.rotclient.MiningSessionClassification.ReasonCode.SELECTION_EPOCH_MISMATCH;
import static fi.rotclient.MiningSessionClassification.ReasonCode.TRACKER_DISABLED;
import static fi.rotclient.MiningSessionClassification.ReasonCode.UNKNOWN_RESOURCE;
import static fi.rotclient.MiningSessionClassification.ReasonCode.UNSUPPORTED_EVIDENCE;
import static fi.rotclient.MiningSessionClassification.ReasonCode.UNSUPPORTED_SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionClassifierTest {
    @Test
    void selectedGemstoneWithConfirmedExactMiningEvidenceIsTargetMined() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                1_200L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertAccepted(
                result,
                MiningSessionCategory.TARGET_MINED,
                MiningSessionClassification.ReasonCode.ACCEPTED_TARGET);
    }

    @Test
    void nonTargetGemstoneWithConfirmedExactMiningEvidenceIsOtherMined() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.TOPAZ),
                900L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertAccepted(
                result,
                MiningSessionCategory.OTHER_MINED,
                MiningSessionClassification.ReasonCode.ACCEPTED_OTHER);
    }

    @Test
    void selectedGemstoneInConfirmedChestContextIsChestLoot() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                TrackerSelection.RUBY,
                chestContext(TrackerSelection.RUBY));

        assertAccepted(
                result,
                MiningSessionCategory.CHEST_LOOT,
                MiningSessionClassification.ReasonCode.ACCEPTED_CHEST_LOOT);
    }

    @Test
    void chestCurrencyInConfirmedContextIsCurrency() {
        MiningSessionClassification result = classify(
                MiningSessionResource.currency(
                        "MITHRIL_POWDER",
                        "Mithril Powder"),
                250L,
                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                TrackerSelection.MITHRIL_TITANIUM,
                chestContext(TrackerSelection.MITHRIL_TITANIUM));

        assertAccepted(
                result,
                MiningSessionCategory.CURRENCY,
                MiningSessionClassification.ReasonCode.ACCEPTED_CURRENCY);
    }

    @Test
    void titaniumBelongsToMithrilTitaniumTarget() {
        MiningSessionClassification result = classify(
                material(TrackedMaterial.TITANIUM),
                32L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                TrackerSelection.MITHRIL_TITANIUM,
                context(TrackerSelection.MITHRIL_TITANIUM));

        assertAccepted(
                result,
                MiningSessionCategory.TARGET_MINED,
                MiningSessionClassification.ReasonCode.ACCEPTED_TARGET);
    }

    @Test
    void selectedTargetCannotBecomeOtherMined() {
        MiningSessionClassification result = classify(
                material(TrackedMaterial.MITHRIL),
                64L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.MITHRIL_TITANIUM,
                context(TrackerSelection.MITHRIL_TITANIUM));

        assertEquals(MiningSessionCategory.TARGET_MINED, result.category());
        assertFalse(result.category() == MiningSessionCategory.OTHER_MINED);
    }

    @Test
    void directBreakWithoutExactQuantityIsUnresolved() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                1L,
                MiningSessionObservation.EvidenceType.DIRECT_BREAK,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertUnresolved(result, MISSING_EXACT_QUANTITY_EVIDENCE);
    }

    @Test
    void positiveSackChangeWithoutMiningCorrelationIsUnresolved() {
        MiningSessionClassifier.ClassificationContext context =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        true,
                        false,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                500L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context);

        assertUnresolved(result, MISSING_MINING_CORRELATION);
    }

    @Test
    void exactCorrelatedSackUsesTargetMembership() {
        MiningSessionClassification target = classify(
                gemstone(GemstoneType.RUBY),
                500L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));
        MiningSessionClassification other = classify(
                gemstone(GemstoneType.TOPAZ),
                500L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertEquals(MiningSessionCategory.TARGET_MINED, target.category());
        assertEquals(MiningSessionCategory.OTHER_MINED, other.category());
    }

    @Test
    void negativeQuantityIsRejectedBeforeContextChecks() {
        MiningSessionClassifier.ClassificationContext unsafeContext =
                new MiningSessionClassifier.ClassificationContext(
                        false,
                        false,
                        TrackerSelection.TOPAZ,
                        99L,
                        false,
                        false,
                        true,
                        false,
                        false,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                -1L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                unsafeContext);

        assertRejected(result, NON_POSITIVE_QUANTITY);
    }

    @Test
    void zeroQuantityIsRejected() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                0L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertRejected(result, NON_POSITIVE_QUANTITY);
    }

    @Test
    void unsupportedSourceIsRejected() {
        MiningSessionClassifier.ClassificationContext context =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        false,
                        false,
                        true,
                        true,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                50L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context);

        assertRejected(result, UNSUPPORTED_SOURCE);
    }

    @Test
    void manualTransferIsRejected() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                50L,
                MiningSessionObservation.EvidenceType.MANUAL_TRANSFER,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertRejected(result, EXPLICIT_NON_MINING_SOURCE);
    }

    @Test
    void bazaarPurchaseIsRejected() {
        MiningSessionClassification result = classify(
                material(TrackedMaterial.GOLD),
                50L,
                MiningSessionObservation.EvidenceType.BAZAAR_PURCHASE,
                TrackerSelection.GOLD,
                context(TrackerSelection.GOLD));

        assertRejected(result, EXPLICIT_NON_MINING_SOURCE);
    }

    @Test
    void duplicateDeliveryIsRejected() {
        MiningSessionClassifier.ClassificationContext duplicateContext =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        true,
                        true,
                        true,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                50L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                duplicateContext);

        assertRejected(result, DUPLICATE_DELIVERY);
    }

    @Test
    void disabledTrackerIsRejected() {
        MiningSessionClassifier.ClassificationContext disabledContext =
                new MiningSessionClassifier.ClassificationContext(
                        false,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        true,
                        true,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                50L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                disabledContext);

        assertRejected(result, TRACKER_DISABLED);
    }

    @Test
    void selectionEpochMismatchIsRejected() {
        MiningSessionObservation observation = observation(
                gemstone(GemstoneType.RUBY),
                50L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                6L);

        MiningSessionClassification result =
                MiningSessionClassifier.classify(
                        observation,
                        context(TrackerSelection.RUBY));

        assertRejected(result, SELECTION_EPOCH_MISMATCH);
    }

    @Test
    void unknownResourceIsUnresolved() {
        MiningSessionClassifier.ClassificationContext unknownContext =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        false,
                        true,
                        false,
                        true,
                        true,
                        false);

        MiningSessionClassification result = classify(
                MiningSessionResource.genericItem("UNKNOWN", "Unknown"),
                50L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                unknownContext);

        assertUnresolved(result, UNKNOWN_RESOURCE);
    }

    @Test
    void chestRewardWithoutChestContextIsUnresolved() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                50L,
                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertUnresolved(result, MISSING_CHEST_CONTEXT);
    }

    @Test
    void currencyWithoutChestContextIsUnresolved() {
        MiningSessionClassification result = classify(
                MiningSessionResource.currency(
                        "GEMSTONE_POWDER",
                        "Gemstone Powder"),
                50L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertUnresolved(result, INSUFFICIENT_EVIDENCE);
    }

    @Test
    void gemstoneSelectionNeverUsesGoldMaterialFallback() {
        MiningSessionClassification result = classify(
                material(TrackedMaterial.GOLD),
                100L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertAccepted(
                result,
                MiningSessionCategory.OTHER_MINED,
                MiningSessionClassification.ReasonCode.ACCEPTED_OTHER);
    }

    @Test
    void classificationIsDeterministicAndDoesNotMutateInputs() {
        MiningSessionObservation observation = observation(
                gemstone(GemstoneType.RUBY),
                100L,
                MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                TrackerSelection.RUBY,
                7L);
        MiningSessionClassifier.ClassificationContext context =
                context(TrackerSelection.RUBY);

        MiningSessionClassification first =
                MiningSessionClassifier.classify(observation, context);
        MiningSessionClassification second =
                MiningSessionClassifier.classify(observation, context);

        assertEquals(first, second);
        assertEquals(100L, observation.quantity());
        assertEquals(7L, observation.selectionEpoch());
        assertTrue(context.confirmedMiningCorrelation());
    }

    @Test
    void inactiveContextWithoutCorrelationIsUnresolved() {
        MiningSessionClassifier.ClassificationContext inactive =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        false,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        true,
                        false,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                100L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                inactive);

        assertUnresolved(result, INACTIVE_MINING_CONTEXT);
    }

    @Test
    void confirmedCorrelationCanCompleteAcrossInactiveBoundary() {
        MiningSessionClassifier.ClassificationContext confirmed =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        false,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        true,
                        true,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                100L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                confirmed);

        assertEquals(WOULD_CREDIT, result.outcome());
        assertEquals(MiningSessionCategory.TARGET_MINED, result.category());
    }

    @Test
    void resourceIdentityIgnoresDisplayNameButKeepsKindDistinct() {
        MiningSessionResource first =
                MiningSessionResource.genericItem("RESOURCE", "First");
        MiningSessionResource renamed =
                MiningSessionResource.genericItem("RESOURCE", "Second");
        MiningSessionResource currency =
                MiningSessionResource.currency("RESOURCE", "Second");

        assertEquals(first, renamed);
        assertEquals(first.hashCode(), renamed.hashCode());
        assertFalse(first.equals(currency));
    }

    @Test
    void resourceAndObservationValidateIdentityAndBounds() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionResource.genericItem("  ", "Blank"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionObservation(
                        gemstone(GemstoneType.RUBY),
                        1L,
                        MiningSessionObservation.EvidenceType.UNKNOWN,
                        TrackerSelection.RUBY,
                        -1L,
                        0L,
                        null,
                        null,
                        null,
                        null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionObservation(
                        gemstone(GemstoneType.RUBY),
                        1L,
                        MiningSessionObservation.EvidenceType.UNKNOWN,
                        TrackerSelection.RUBY,
                        0L,
                        -1L,
                        null,
                        null,
                        null,
                        null));

        String tooLong = "x".repeat(
                MiningSessionObservation.MAX_DIAGNOSTIC_TEXT_LENGTH + 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionObservation(
                        gemstone(GemstoneType.RUBY),
                        1L,
                        MiningSessionObservation.EvidenceType.UNKNOWN,
                        TrackerSelection.RUBY,
                        0L,
                        0L,
                        null,
                        null,
                        null,
                        tooLong));
    }

    @Test
    void blankOptionalObservationTextBecomesAbsent() {
        MiningSessionObservation observation =
                new MiningSessionObservation(
                        gemstone(GemstoneType.RUBY),
                        -5L,
                        MiningSessionObservation.EvidenceType.UNKNOWN,
                        TrackerSelection.RUBY,
                        0L,
                        0L,
                        "  ",
                        "\t",
                        " ",
                        "  ");

        assertEquals(-5L, observation.quantity());
        assertNull(observation.eventId());
        assertNull(observation.correlationId());
        assertNull(observation.sourceName());
        assertNull(observation.diagnosticText());
    }

    @Test
    void genericAndTypedGemstoneWithSameIdAreUnequal() {
        MiningSessionResource generic =
                MiningSessionResource.genericItem("SHARED", "Generic");
        MiningSessionResource typed =
                MiningSessionResource.gemstone(
                        "SHARED",
                        "Rough Ruby Gemstone",
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH);

        assertFalse(generic.equals(typed));
    }

    @Test
    void genericAndTypedMaterialWithSameIdAreUnequal() {
        MiningSessionResource generic =
                MiningSessionResource.genericItem("SHARED", "Generic");
        MiningSessionResource typed =
                MiningSessionResource.material(
                        "SHARED",
                        "Gold Ingot",
                        TrackedMaterial.GOLD);

        assertFalse(generic.equals(typed));
    }

    @Test
    void sameIdWithDifferentGemstoneTypesIsUnequal() {
        MiningSessionResource ruby =
                MiningSessionResource.gemstone(
                        "SHARED",
                        "Rough Ruby Gemstone",
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH);
        MiningSessionResource topaz =
                MiningSessionResource.gemstone(
                        "SHARED",
                        "Rough Topaz Gemstone",
                        GemstoneType.TOPAZ,
                        GemstoneTier.ROUGH);

        assertFalse(ruby.equals(topaz));
    }

    @Test
    void sameIdAndGemstoneWithDifferentTiersIsUnequal() {
        MiningSessionResource rough =
                MiningSessionResource.gemstone(
                        "SHARED",
                        "Rough Ruby Gemstone",
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH);
        MiningSessionResource flawed =
                MiningSessionResource.gemstone(
                        "SHARED",
                        "Flawed Ruby Gemstone",
                        GemstoneType.RUBY,
                        GemstoneTier.FLAWED);

        assertFalse(rough.equals(flawed));
    }

    @Test
    void sameIdWithDifferentMaterialsIsUnequal() {
        MiningSessionResource gold =
                MiningSessionResource.material(
                        "SHARED",
                        "Gold Ingot",
                        TrackedMaterial.GOLD);
        MiningSessionResource diamond =
                MiningSessionResource.material(
                        "SHARED",
                        "Diamond",
                        TrackedMaterial.DIAMOND);

        assertFalse(gold.equals(diamond));
    }

    @Test
    void itemAndCurrencyWithSameIdAreUnequal() {
        MiningSessionResource item =
                MiningSessionResource.genericItem("SHARED", "Item");
        MiningSessionResource currency =
                MiningSessionResource.currency("SHARED", "Currency");

        assertFalse(item.equals(currency));
    }

    @Test
    void identicalSemanticResourceIgnoresDisplayNameInEqualityAndHashing() {
        MiningSessionResource first =
                MiningSessionResource.gemstone(
                        "SHARED",
                        "First display",
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH);
        MiningSessionResource renamed =
                MiningSessionResource.gemstone(
                        "SHARED",
                        "Renamed display",
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH);

        assertEquals(first, renamed);
        assertEquals(first.hashCode(), renamed.hashCode());
    }

    @Test
    void chestOpenNeverCreatesQuantityCredit() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                1L,
                MiningSessionObservation.EvidenceType.CHEST_OPEN,
                TrackerSelection.RUBY,
                chestContext(TrackerSelection.RUBY));

        assertUnresolved(result, UNSUPPORTED_EVIDENCE);
    }

    @Test
    void inventoryChangeDoesNotBecomeChestLootFromContextAlone() {
        MiningSessionClassifier.ClassificationContext context =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        true,
                        true,
                        true);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                TrackerSelection.RUBY,
                context);

        assertAccepted(
                result,
                MiningSessionCategory.TARGET_MINED,
                MiningSessionClassification.ReasonCode.ACCEPTED_TARGET);
    }

    @Test
    void knownResourceWithUnknownEvidenceCannotCredit() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.UNKNOWN,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertUnresolved(result, UNSUPPORTED_EVIDENCE);
    }

    @Test
    void pristineUsesExactTargetMembership() {
        MiningSessionClassification target = classify(
                gemstone(GemstoneType.RUBY),
                20L,
                MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));
        MiningSessionClassification other = classify(
                gemstone(GemstoneType.TOPAZ),
                20L,
                MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                TrackerSelection.RUBY,
                context(TrackerSelection.RUBY));

        assertEquals(MiningSessionCategory.TARGET_MINED, target.category());
        assertEquals(MiningSessionCategory.OTHER_MINED, other.category());
    }

    @Test
    void pristineWithoutExactQuantityIsUnresolved() {
        MiningSessionClassifier.ClassificationContext noExact =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        false,
                        true,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                20L,
                MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                TrackerSelection.RUBY,
                noExact);

        assertUnresolved(result, MISSING_EXACT_QUANTITY_EVIDENCE);
    }

    @Test
    void pristineWithoutMiningCorrelationIsUnresolved() {
        MiningSessionClassifier.ClassificationContext noCorrelation =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        true,
                        false,
                        false);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                20L,
                MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                TrackerSelection.RUBY,
                noCorrelation);

        assertUnresolved(result, MISSING_MINING_CORRELATION);
    }

    @Test
    void chestSackConfirmationCanCreditItem() {
        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.CHEST_SACK_CONFIRMATION,
                TrackerSelection.RUBY,
                chestContext(TrackerSelection.RUBY));

        assertEquals(MiningSessionCategory.CHEST_LOOT, result.category());
    }

    @Test
    void chestSackConfirmationCanCreditCurrency() {
        MiningSessionClassification result = classify(
                MiningSessionResource.currency("POWDER", "Powder"),
                80L,
                MiningSessionObservation.EvidenceType.CHEST_SACK_CONFIRMATION,
                TrackerSelection.RUBY,
                chestContext(TrackerSelection.RUBY));

        assertEquals(MiningSessionCategory.CURRENCY, result.category());
    }

    @Test
    void chestRewardWithoutExactQuantityIsUnresolved() {
        MiningSessionClassifier.ClassificationContext noExact =
                new MiningSessionClassifier.ClassificationContext(
                        true,
                        true,
                        TrackerSelection.RUBY,
                        7L,
                        true,
                        true,
                        false,
                        false,
                        false,
                        true);

        MiningSessionClassification result = classify(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                TrackerSelection.RUBY,
                noExact);

        assertUnresolved(result, MISSING_EXACT_QUANTITY_EVIDENCE);
    }

    @Test
    void matchingEpochWithDifferentTrackerIsRejected() {
        MiningSessionObservation observation = observation(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                7L);

        MiningSessionClassification result =
                MiningSessionClassifier.classify(
                        observation,
                        context(TrackerSelection.TOPAZ));

        assertRejected(result, SELECTION_EPOCH_MISMATCH);
    }

    @Test
    void classificationConstructorRejectsMissingOrUnexpectedCategories() {
        MiningSessionObservation observation = observation(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                7L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionClassification(
                        observation,
                        WOULD_CREDIT,
                        null,
                        MiningSessionClassification.ReasonCode.ACCEPTED_TARGET));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionClassification(
                        observation,
                        REJECTED,
                        MiningSessionCategory.TARGET_MINED,
                        DUPLICATE_DELIVERY));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionClassification(
                        observation,
                        UNRESOLVED,
                        MiningSessionCategory.TARGET_MINED,
                        MISSING_MINING_CORRELATION));
    }

    @Test
    void classificationConstructorRejectsMismatchedAcceptedReasons() {
        MiningSessionObservation observation = observation(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                7L);

        assertAcceptedReasonMismatch(
                observation,
                MiningSessionCategory.OTHER_MINED,
                MiningSessionClassification.ReasonCode.ACCEPTED_TARGET);
        assertAcceptedReasonMismatch(
                observation,
                MiningSessionCategory.TARGET_MINED,
                MiningSessionClassification.ReasonCode.ACCEPTED_OTHER);
        assertAcceptedReasonMismatch(
                observation,
                MiningSessionCategory.CURRENCY,
                MiningSessionClassification.ReasonCode.ACCEPTED_CHEST_LOOT);
        assertAcceptedReasonMismatch(
                observation,
                MiningSessionCategory.CHEST_LOOT,
                MiningSessionClassification.ReasonCode.ACCEPTED_CURRENCY);
    }

    @Test
    void nonAcceptedOutcomesRejectAcceptedReasons() {
        MiningSessionObservation observation = observation(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                7L);

        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionClassification(
                        observation,
                        REJECTED,
                        null,
                        MiningSessionClassification.ReasonCode.ACCEPTED_TARGET));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionClassification(
                        observation,
                        UNRESOLVED,
                        null,
                        MiningSessionClassification.ReasonCode.ACCEPTED_OTHER));
    }

    @Test
    void validClassificationFactoriesPreserveInvariants() {
        MiningSessionObservation observation = observation(
                gemstone(GemstoneType.RUBY),
                80L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                7L);

        assertEquals(
                WOULD_CREDIT,
                MiningSessionClassification.wouldCredit(
                        observation,
                        MiningSessionCategory.TARGET_MINED)
                        .outcome());
        assertEquals(
                REJECTED,
                MiningSessionClassification.rejected(
                        observation,
                        DUPLICATE_DELIVERY)
                        .outcome());
        assertEquals(
                UNRESOLVED,
                MiningSessionClassification.unresolved(
                        observation,
                        MISSING_MINING_CORRELATION)
                        .outcome());
    }

    private static MiningSessionClassification classify(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            TrackerSelection selectedTracker,
            MiningSessionClassifier.ClassificationContext context) {
        return MiningSessionClassifier.classify(
                observation(
                        resource,
                        quantity,
                        evidence,
                        selectedTracker,
                        7L),
                context);
    }

    private static MiningSessionObservation observation(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            TrackerSelection selectedTracker,
            long epoch) {
        return new MiningSessionObservation(
                resource,
                quantity,
                evidence,
                selectedTracker,
                epoch,
                12_000L,
                "event-1",
                "correlation-1",
                "Gemstones Sack",
                "bounded diagnostic text");
    }

    private static MiningSessionClassifier.ClassificationContext context(
            TrackerSelection selection) {
        return new MiningSessionClassifier.ClassificationContext(
                true,
                true,
                selection,
                7L,
                true,
                true,
                false,
                true,
                true,
                false);
    }

    private static MiningSessionClassifier.ClassificationContext chestContext(
            TrackerSelection selection) {
        return new MiningSessionClassifier.ClassificationContext(
                true,
                true,
                selection,
                7L,
                true,
                true,
                false,
                true,
                false,
                true);
    }

    private static MiningSessionResource gemstone(
            GemstoneType gemstone) {
        return MiningSessionResource.gemstone(
                "ROUGH_" + gemstone.id() + "_GEM",
                "Rough " + gemstone.displayName() + " Gemstone",
                gemstone,
                GemstoneTier.ROUGH);
    }

    private static MiningSessionResource material(
            TrackedMaterial material) {
        return MiningSessionResource.material(
                material.rawBazaarId(),
                material.rawItemName(),
                material);
    }

    private static void assertAccepted(
            MiningSessionClassification result,
            MiningSessionCategory category,
            MiningSessionClassification.ReasonCode reason) {
        assertEquals(WOULD_CREDIT, result.outcome());
        assertEquals(category, result.category());
        assertEquals(reason, result.reasonCode());
    }

    private static void assertRejected(
            MiningSessionClassification result,
            MiningSessionClassification.ReasonCode reason) {
        assertEquals(REJECTED, result.outcome());
        assertNull(result.category());
        assertEquals(reason, result.reasonCode());
    }

    private static void assertUnresolved(
            MiningSessionClassification result,
            MiningSessionClassification.ReasonCode reason) {
        assertEquals(UNRESOLVED, result.outcome());
        assertNull(result.category());
        assertEquals(reason, result.reasonCode());
    }

    private static void assertAcceptedReasonMismatch(
            MiningSessionObservation observation,
            MiningSessionCategory category,
            MiningSessionClassification.ReasonCode reason) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionClassification(
                        observation,
                        WOULD_CREDIT,
                        category,
                        reason));
    }
}
