package fi.rotclient;

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

final class MiningSessionClassifier {
    private MiningSessionClassifier() {
    }

    static MiningSessionClassification classify(
            MiningSessionObservation observation,
            ClassificationContext context) {
        if (observation == null) {
            throw new IllegalArgumentException(
                    "Observation cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException(
                    "Classification context cannot be null");
        }

        if (observation.quantity() <= 0L) {
            return MiningSessionClassification.rejected(
                    observation,
                    NON_POSITIVE_QUANTITY);
        }
        if (context.duplicateDelivery()) {
            return MiningSessionClassification.rejected(
                    observation,
                    DUPLICATE_DELIVERY);
        }
        if (!context.trackerEnabled()) {
            return MiningSessionClassification.rejected(
                    observation,
                    TRACKER_DISABLED);
        }
        if (observation.selectionEpoch()
                != context.currentSelectionEpoch()
                || observation.selectedTracker()
                != context.currentTracker()) {
            return MiningSessionClassification.rejected(
                    observation,
                    SELECTION_EPOCH_MISMATCH);
        }
        if (observation.evidenceType()
                == MiningSessionObservation.EvidenceType.BAZAAR_PURCHASE
                || observation.evidenceType()
                == MiningSessionObservation.EvidenceType.MANUAL_TRANSFER) {
            return MiningSessionClassification.rejected(
                    observation,
                    EXPLICIT_NON_MINING_SOURCE);
        }
        if (!context.supportedSource()) {
            return MiningSessionClassification.rejected(
                    observation,
                    UNSUPPORTED_SOURCE);
        }
        if (!context.knownResource()) {
            return MiningSessionClassification.unresolved(
                    observation,
                    UNKNOWN_RESOURCE);
        }

        boolean exactChestReward =
                isChestRewardEvidence(
                        observation.evidenceType())
                        && context.exactQuantityEvidence();

        if (context.confirmedChestContext()
                && exactChestReward) {
            MiningSessionCategory category =
                    observation.resource().kind()
                            == MiningSessionResource.ResourceKind.CURRENCY
                            ? MiningSessionCategory.CURRENCY
                            : MiningSessionCategory.CHEST_LOOT;
            return MiningSessionClassification.wouldCredit(
                    observation,
                    category);
        }

        if (isChestRewardEvidence(
                observation.evidenceType())) {
            if (!context.confirmedChestContext()) {
                return MiningSessionClassification.unresolved(
                        observation,
                        MISSING_CHEST_CONTEXT);
            }
            return MiningSessionClassification.unresolved(
                    observation,
                    MISSING_EXACT_QUANTITY_EVIDENCE);
        }

        if (observation.evidenceType()
                == MiningSessionObservation.EvidenceType.DIRECT_BREAK) {
            return MiningSessionClassification.unresolved(
                    observation,
                    MISSING_EXACT_QUANTITY_EVIDENCE);
        }

        if (!isMiningQuantityEvidence(
                observation.evidenceType())) {
            return MiningSessionClassification.unresolved(
                    observation,
                    observation.evidenceType()
                            == MiningSessionObservation.EvidenceType.UNKNOWN
                            || observation.evidenceType()
                            == MiningSessionObservation.EvidenceType.CHEST_OPEN
                            ? UNSUPPORTED_EVIDENCE
                            : INSUFFICIENT_EVIDENCE);
        }

        if (!context.exactQuantityEvidence()) {
            return MiningSessionClassification.unresolved(
                    observation,
                    MISSING_EXACT_QUANTITY_EVIDENCE);
        }

        if (!context.activeMiningContext()
                && !context.confirmedMiningCorrelation()) {
            return MiningSessionClassification.unresolved(
                    observation,
                    INACTIVE_MINING_CONTEXT);
        }

        if (!context.confirmedMiningCorrelation()) {
            return MiningSessionClassification.unresolved(
                    observation,
                    MISSING_MINING_CORRELATION);
        }

        if (observation.resource().kind()
                == MiningSessionResource.ResourceKind.CURRENCY) {
            return MiningSessionClassification.unresolved(
                    observation,
                    INSUFFICIENT_EVIDENCE);
        }

        MiningSessionCategory category =
                isSelectedTarget(
                        observation.resource(),
                        context.currentTracker())
                        ? MiningSessionCategory.TARGET_MINED
                        : MiningSessionCategory.OTHER_MINED;

        return MiningSessionClassification.wouldCredit(
                observation,
                category);
    }

    private static boolean isSelectedTarget(
            MiningSessionResource resource,
            TrackerSelection selection) {
        if (resource.material() != null) {
            return selection.isMaterial()
                    && selection.materialTarget()
                    .includes(resource.material());
        }
        if (resource.gemstone() != null) {
            return selection.isGemstone()
                    && selection.gemstone()
                    == resource.gemstone();
        }
        return false;
    }

    private static boolean isChestRewardEvidence(
            MiningSessionObservation.EvidenceType evidenceType) {
        return evidenceType
                == MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE
                || evidenceType
                == MiningSessionObservation.EvidenceType.CHEST_SACK_CONFIRMATION;
    }

    private static boolean isMiningQuantityEvidence(
            MiningSessionObservation.EvidenceType evidenceType) {
        return evidenceType
                == MiningSessionObservation.EvidenceType.SACK_CHANGE
                || evidenceType
                == MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE
                || evidenceType
                == MiningSessionObservation.EvidenceType.INVENTORY_CHANGE;
    }

    record ClassificationContext(
            boolean trackerEnabled,
            boolean activeMiningContext,
            TrackerSelection currentTracker,
            long currentSelectionEpoch,
            boolean knownResource,
            boolean supportedSource,
            boolean duplicateDelivery,
            boolean exactQuantityEvidence,
            boolean confirmedMiningCorrelation,
            boolean confirmedChestContext) {
        ClassificationContext {
            if (currentTracker == null) {
                throw new IllegalArgumentException(
                        "Current tracker cannot be null");
            }
            if (currentSelectionEpoch < 0L) {
                throw new IllegalArgumentException(
                        "Current selection epoch cannot be negative");
            }
        }
    }
}
