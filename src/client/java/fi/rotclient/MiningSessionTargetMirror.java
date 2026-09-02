package fi.rotclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Strict adapter from already-accepted live target events into the ephemeral
 * mining-session ledger. Detection and parsing remain outside this class.
 */
final class MiningSessionTargetMirror {
    static final int MAX_SOURCE_NAME_LENGTH = 64;

    private final MiningSessionLedger ledger;
    private final Map<MiningSessionResource,
            MiningResourceCatalog.ResourceDefinition> definitions;

    MiningSessionTargetMirror(MiningSessionLedger ledger) {
        this(ledger, new MiningResourceCatalog());
    }

    MiningSessionTargetMirror(
            MiningSessionLedger ledger,
            MiningResourceCatalog catalog) {
        if (ledger == null || catalog == null) {
            throw new IllegalArgumentException(
                    "Target mirror dependencies cannot be null");
        }

        Map<MiningSessionResource,
                MiningResourceCatalog.ResourceDefinition> indexed =
                new HashMap<>();
        for (MiningResourceCatalog.ResourceDefinition definition :
                catalog.definitions()) {
            MiningResourceCatalog.ResourceDefinition previous = indexed.put(
                    definition.resource(),
                    definition);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate canonical target resource");
            }
        }

        this.ledger = ledger;
        this.definitions = Map.copyOf(indexed);
    }

    QuantityResult mirrorQuantity(QuantityEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "Target quantity event cannot be null");
        }

        MiningSessionObservation observation = event.observation();
        MiningResourceCatalog.ResourceDefinition definition =
                definitions.get(observation.resource());
        MiningSessionClassification classification =
                MiningSessionClassifier.classify(
                        observation,
                        quantityContext(
                                observation,
                                definition != null));

        if (classification.outcome()
                != MiningSessionClassification.Outcome.WOULD_CREDIT) {
            QuantityReason reason = classification.outcome()
                    == MiningSessionClassification.Outcome.REJECTED
                    ? QuantityReason.CLASSIFIER_REJECTED
                    : QuantityReason.CLASSIFIER_UNRESOLVED;
            return new QuantityResult(
                    QuantityStatus.REJECTED,
                    reason,
                    classification);
        }

        if (classification.category()
                != MiningSessionCategory.TARGET_MINED) {
            return new QuantityResult(
                    QuantityStatus.INVARIANT_REJECTED,
                    QuantityReason.UNEXPECTED_CATEGORY,
                    classification);
        }

        if (definition == null
                || !acceptsTargetQuantityEvidence(
                definition,
                observation.evidenceType())) {
            return new QuantityResult(
                    QuantityStatus.INVARIANT_REJECTED,
                    QuantityReason.UNSUPPORTED_TARGET_EVIDENCE,
                    classification);
        }

        if (observation.eventId() == null
                && observation.correlationId() == null) {
            return new QuantityResult(
                    QuantityStatus.INVARIANT_REJECTED,
                    QuantityReason.MISSING_DELIVERY_IDENTITY,
                    classification);
        }

        final boolean appended;
        try {
            appended = ledger.append(
                    classification,
                    MiningSessionPriceResolution.unresolved());
        } catch (IllegalStateException identityConflict) {
            return new QuantityResult(
                    QuantityStatus.IDENTITY_CONFLICT,
                    QuantityReason.IDENTITY_CONFLICT,
                    classification);
        }

        return appended
                ? new QuantityResult(
                QuantityStatus.APPENDED,
                QuantityReason.APPENDED,
                classification)
                : new QuantityResult(
                QuantityStatus.DUPLICATE,
                QuantityReason.EXACT_DUPLICATE,
                classification);
    }

    BlockResult observeBlock(BlockObservation event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "Target block observation cannot be null");
        }

        MiningSessionObservation observation = event.observation();
        MiningResourceCatalog.ResourceDefinition definition =
                definitions.get(observation.resource());
        MiningSessionClassification classification =
                MiningSessionClassifier.classify(
                        observation,
                        blockContext(
                                observation,
                                definition != null));

        if (definition == null) {
            return new BlockResult(
                    BlockStatus.UNKNOWN_RESOURCE,
                    classification);
        }
        if (!definition.directBreakFamilySupported()) {
            return new BlockResult(
                    BlockStatus.UNSUPPORTED_RESOURCE,
                    classification);
        }
        if (!isSelectedTarget(
                definition.resource(),
                observation.selectedTracker())) {
            return new BlockResult(
                    BlockStatus.NON_TARGET,
                    classification);
        }

        return new BlockResult(
                BlockStatus.ACCEPTED_FOR_PARITY,
                classification);
    }

    private static MiningSessionClassifier.ClassificationContext
    quantityContext(
            MiningSessionObservation observation,
            boolean knownResource) {
        boolean exactQuantityEvidence = isQuantityEvidence(
                observation.evidenceType());
        return new MiningSessionClassifier.ClassificationContext(
                true,
                true,
                observation.selectedTracker(),
                observation.selectionEpoch(),
                knownResource,
                true,
                false,
                exactQuantityEvidence,
                exactQuantityEvidence,
                false);
    }

    private static MiningSessionClassifier.ClassificationContext blockContext(
            MiningSessionObservation observation,
            boolean knownResource) {
        return new MiningSessionClassifier.ClassificationContext(
                true,
                true,
                observation.selectedTracker(),
                observation.selectionEpoch(),
                knownResource,
                true,
                false,
                false,
                true,
                false);
    }

    private static boolean acceptsTargetQuantityEvidence(
            MiningResourceCatalog.ResourceDefinition definition,
            MiningSessionObservation.EvidenceType evidence) {
        if (!definition.exactQuantitySupported()
                || !definition.shadowAcceptanceSupported()) {
            return false;
        }
        if (definition.material() != null) {
            return evidence
                    == MiningSessionObservation.EvidenceType.INVENTORY_CHANGE
                    || evidence
                    == MiningSessionObservation.EvidenceType.SACK_CHANGE;
        }
        if (definition.gemstoneTier() == GemstoneTier.ROUGH) {
            return evidence
                    == MiningSessionObservation.EvidenceType.SACK_CHANGE;
        }
        return definition.gemstoneTier() == GemstoneTier.FLAWED
                && evidence
                == MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE;
    }

    private static boolean isQuantityEvidence(
            MiningSessionObservation.EvidenceType evidence) {
        return evidence
                == MiningSessionObservation.EvidenceType.SACK_CHANGE
                || evidence
                == MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE
                || evidence
                == MiningSessionObservation.EvidenceType.INVENTORY_CHANGE;
    }

    private static boolean isSelectedTarget(
            MiningSessionResource resource,
            TrackerSelection selection) {
        if (resource.material() != null) {
            return selection.isMaterial()
                    && selection.materialTarget()
                    .includes(resource.material());
        }
        return resource.gemstone() != null
                && selection.isGemstone()
                && selection.gemstone() == resource.gemstone();
    }

    private static String boundedSource(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sanitized = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        return sanitized.length() <= MAX_SOURCE_NAME_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_SOURCE_NAME_LENGTH);
    }

    record QuantityEvent(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            TrackerSelection selectedTracker,
            long selectionEpoch,
            long observedAtMillis,
            String eventId,
            String correlationId,
            String sourceName) {
        QuantityEvent {
            validateCommon(
                    resource,
                    selectedTracker,
                    selectionEpoch,
                    observedAtMillis);
            if (evidence == null) {
                throw new IllegalArgumentException(
                        "Target quantity evidence cannot be null");
            }
            sourceName = boundedSource(sourceName);
        }

        private MiningSessionObservation observation() {
            return new MiningSessionObservation(
                    resource,
                    quantity,
                    evidence,
                    selectedTracker,
                    selectionEpoch,
                    observedAtMillis,
                    eventId,
                    correlationId,
                    sourceName,
                    null);
        }
    }

    record BlockObservation(
            MiningSessionResource resource,
            TrackerSelection selectedTracker,
            long selectionEpoch,
            long observedAtMillis,
            String eventId,
            String correlationId,
            String sourceName) {
        BlockObservation {
            validateCommon(
                    resource,
                    selectedTracker,
                    selectionEpoch,
                    observedAtMillis);
            sourceName = boundedSource(sourceName);
        }

        private MiningSessionObservation observation() {
            return new MiningSessionObservation(
                    resource,
                    1L,
                    MiningSessionObservation.EvidenceType.DIRECT_BREAK,
                    selectedTracker,
                    selectionEpoch,
                    observedAtMillis,
                    eventId,
                    correlationId,
                    sourceName,
                    null);
        }
    }

    private static void validateCommon(
            MiningSessionResource resource,
            TrackerSelection selectedTracker,
            long selectionEpoch,
            long observedAtMillis) {
        if (resource == null || selectedTracker == null) {
            throw new IllegalArgumentException(
                    "Target event identity cannot be null");
        }
        if (selectionEpoch < 0L) {
            throw new IllegalArgumentException(
                    "Selection epoch cannot be negative");
        }
        if (observedAtMillis < 0L) {
            throw new IllegalArgumentException(
                    "Observation timestamp cannot be negative");
        }
    }

    enum QuantityStatus {
        APPENDED,
        DUPLICATE,
        REJECTED,
        INVARIANT_REJECTED,
        IDENTITY_CONFLICT
    }

    enum QuantityReason {
        APPENDED,
        EXACT_DUPLICATE,
        CLASSIFIER_REJECTED,
        CLASSIFIER_UNRESOLVED,
        MISSING_DELIVERY_IDENTITY,
        UNSUPPORTED_TARGET_EVIDENCE,
        UNEXPECTED_CATEGORY,
        IDENTITY_CONFLICT
    }

    record QuantityResult(
            QuantityStatus status,
            QuantityReason reason,
            MiningSessionClassification classification) {
        QuantityResult {
            if (status == null || reason == null || classification == null) {
                throw new IllegalArgumentException(
                        "Target quantity result cannot be incomplete");
            }
        }

        boolean appended() {
            return status == QuantityStatus.APPENDED;
        }
    }

    enum BlockStatus {
        ACCEPTED_FOR_PARITY,
        NON_TARGET,
        UNKNOWN_RESOURCE,
        UNSUPPORTED_RESOURCE
    }

    record BlockResult(
            BlockStatus status,
            MiningSessionClassification classification) {
        BlockResult {
            if (status == null || classification == null) {
                throw new IllegalArgumentException(
                        "Target block result cannot be incomplete");
            }
        }

        boolean acceptedForParity() {
            return status == BlockStatus.ACCEPTED_FOR_PARITY;
        }
    }
}
