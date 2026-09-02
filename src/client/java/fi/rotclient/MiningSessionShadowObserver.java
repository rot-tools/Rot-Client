package fi.rotclient;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Diagnostic-only Other Mined observer with no access to live accounting. */
final class MiningSessionShadowObserver {
    static final long DELIVERY_DEDUPE_WINDOW_MILLIS = 2_500L;
    static final int MAX_RECENT_DELIVERIES = 128;

    private final MiningResourceCatalog catalog;
    private final MiningSessionDirectBreakTracker directBreakTracker;
    private final MiningSessionLedger ledger;
    private final DiagnosticSink diagnostics;
    private final boolean clearLedgerOnInvalidation;
    private final Map<DeliveryFingerprint, Long> recentDeliveries =
            new LinkedHashMap<>();
    /**
     * Correlation context/channel pairs already credited. Sack and Pristine
     * are independent products of one gemstone break, while replaying either
     * individual channel must still be rejected.
     * Bounded with {@link #MAX_RECENT_DELIVERIES}; cleared on epoch reset.
     */
    private final LinkedHashMap<String, Long> acceptedContextChannels =
            new LinkedHashMap<>();
    /**
     * Pristine/Sack component occurrence identities already accepted. Prevents
     * same-component replay from consuming a second direct-break context after
     * the short fingerprint window expires. Cleared on epoch reset.
     */
    private final LinkedHashMap<String, Long> acceptedDeliveryIdentities =
            new LinkedHashMap<>();

    private TrackerSelection selection = TrackerSelection.GOLD;
    private boolean trackerEnabled;
    private long selectionEpoch;

    MiningSessionShadowObserver() {
        this(
                new MiningResourceCatalog(),
                new MiningSessionDirectBreakTracker(),
                new MiningSessionLedger(),
                DiagnosticSink.RECORDER,
                true);
    }

    MiningSessionShadowObserver(
            MiningResourceCatalog catalog,
            MiningSessionDirectBreakTracker directBreakTracker,
            MiningSessionLedger ledger,
            DiagnosticSink diagnostics) {
        this(
                catalog,
                directBreakTracker,
                ledger,
                diagnostics,
                true);
    }

    MiningSessionShadowObserver(
            MiningResourceCatalog catalog,
            MiningSessionDirectBreakTracker directBreakTracker,
            MiningSessionLedger ledger,
            DiagnosticSink diagnostics,
            boolean clearLedgerOnInvalidation) {
        if (catalog == null
                || directBreakTracker == null
                || ledger == null
                || diagnostics == null) {
            throw new IllegalArgumentException(
                    "Shadow observer dependencies cannot be null");
        }
        this.catalog = catalog;
        this.directBreakTracker = directBreakTracker;
        this.ledger = ledger;
        this.diagnostics = diagnostics;
        this.clearLedgerOnInvalidation = clearLedgerOnInvalidation;
    }

    boolean isObservationEnabled() {
        // Observation follows engine-synchronized tracker state. DiagnosticSink
        // activity only gates marker writes, not whether shadow credits run.
        // Session Analytics can be active without DiagnosticRecorder.
        return trackerEnabled;
    }

    boolean supportsMaterialBreak(TrackedMaterial material) {
        return catalog.familyFor(material).isPresent();
    }

    void onDiagnosticStarted(
            boolean enabled,
            TrackerSelection selectedTracker) {
        transition(enabled, selectedTracker);
    }

    void onDiagnosticStopped() {
        transition(false, selection);
    }

    void onTrackerEnabled(TrackerSelection selectedTracker) {
        transition(true, selectedTracker);
    }

    void onTrackerDisabled() {
        transition(false, selection);
    }

    void onSelectedTargetChanged(
            TrackerSelection selectedTracker,
            boolean enabled) {
        transition(enabled, selectedTracker);
    }

    void onReset() {
        invalidateState();
    }

    void onWorldTransition() {
        invalidateState();
    }

    /**
     * Aligns this observer with the engine-owned selection epoch while clearing
     * only correlation and delivery state. The shared unified ledger remains
     * under {@link MiningSessionEngine} ownership.
     */
    void synchronizeEngineState(
            boolean enabled,
            TrackerSelection selectedTracker,
            long engineSelectionEpoch) {
        if (selectedTracker == null) {
            throw new IllegalArgumentException(
                    "Selected tracker cannot be null");
        }
        if (engineSelectionEpoch < 0L) {
            throw new IllegalArgumentException(
                    "Selection epoch cannot be negative");
        }
        trackerEnabled = enabled;
        selection = selectedTracker;
        resetPendingState();
        selectionEpoch = engineSelectionEpoch;
    }

    void onConfirmedMaterialBreak(
            TrackedMaterial material,
            int count,
            long observedAtMillis) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled() || count <= 0) {
            return;
        }
        Optional<MiningSessionDirectBreakTracker.FamilyKey> family =
                catalog.familyFor(material);
        if (family.isEmpty()) {
            return;
        }
        for (int index = 0; index < count; index++) {
            directBreakTracker.recordConfirmedBreak(
                    family.get(), selectionEpoch, observedAtMillis);
        }
    }

    void onConfirmedGemstoneBreak(
            GemstoneType gemstone,
            long observedAtMillis) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled()) {
            return;
        }
        catalog.familyFor(gemstone).ifPresent(family ->
                directBreakTracker.recordConfirmedBreak(
                        family, selectionEpoch, observedAtMillis));
    }

    void observeSackChanges(
            List<SackChangeParser.Change> changes,
            long observedAtMillis) {
        observeSackChanges(changes, observedAtMillis, 0L);
    }

    void observeSackChanges(
            List<SackChangeParser.Change> changes,
            long observedAtMillis,
            long coveredBatchMillis) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled() || changes == null) {
            return;
        }
        for (SackChangeParser.Change change : List.copyOf(changes)) {
            observeSackChange(change, observedAtMillis, coveredBatchMillis);
        }
    }

    ObservationResult observeSackChangesForTest(
            SackChangeParser.Change change,
            long observedAtMillis) {
        return observeSackChangesForTest(change, observedAtMillis, 0L);
    }

    ObservationResult observeSackChangesForTest(
            SackChangeParser.Change change,
            long observedAtMillis,
            long coveredBatchMillis) {
        return observeSackChangesForTest(
                change, observedAtMillis, coveredBatchMillis, "");
    }

    ObservationResult observeSackChangesForTest(
            SackChangeParser.Change change,
            long observedAtMillis,
            long coveredBatchMillis,
            String deliveryIdentity) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled()) {
            return ObservationResult.ignored();
        }
        return observeSackChange(
                change, observedAtMillis, coveredBatchMillis, deliveryIdentity);
    }

    ObservationResult observePristine(
            PristineMessageParser.Reward reward,
            long observedAtMillis) {
        return observePristine(reward, observedAtMillis, "");
    }

    ObservationResult observePristine(
            PristineMessageParser.Reward reward,
            long observedAtMillis,
            String deliveryIdentity) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled() || reward == null) {
            return ObservationResult.ignored();
        }
        Optional<MiningResourceCatalog.ResourceDefinition> definition =
                catalog.fromGemstone(
                        reward.gemstone(), GemstoneTier.FLAWED);
        if (definition.isEmpty()) {
            return rejectUnknown(
                    reward.flawedAmount(),
                    MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                    "PRISTINE",
                    observedAtMillis);
        }
        return process(
                definition.get(),
                reward.flawedAmount(),
                MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                MiningSessionDirectBreakTracker.EvidenceChannel.PRISTINE_QUANTITY,
                "PRISTINE",
                true,
                true,
                observedAtMillis,
                0L,
                deliveryIdentity == null ? "" : deliveryIdentity);
    }

    /**
     * Reports an exact, precisely-measured raw inventory increase for a
     * non-target {@link TrackedMaterial}. Unlike {@link #observeInventory},
     * this is exact-quantity evidence: the caller has already computed a
     * precise stack-count delta (not an ambiguous whole-inventory
     * snapshot), so it is routed through {@link #process} exactly like a
     * Mining Sack delivery and can satisfy direct-break correlation on its
     * own. This is the primary path for materials that are never deposited
     * into a Mining Sack (e.g. Cobblestone) and a fallback for materials
     * whose sack notification is delayed, batched, or missed.
     */
    ObservationResult observeMaterialInventoryGain(
            TrackedMaterial material,
            long quantity,
            long observedAtMillis) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled() || material == null) {
            return ObservationResult.ignored();
        }
        Optional<MiningResourceCatalog.ResourceDefinition> definition =
                catalog.fromMaterial(material);
        if (definition.isEmpty()) {
            return rejectUnknown(
                    quantity,
                    MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                    "inventory",
                    observedAtMillis);
        }

        MiningResourceCatalog.ResourceDefinition resource = definition.get();
        return process(
                resource,
                quantity,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY,
                "inventory",
                true,
                resource.exactQuantitySupported(),
                observedAtMillis);
    }

    ObservationResult observeInventory(
            MiningSessionResource resource,
            long quantity,
            long observedAtMillis) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled() || resource == null) {
            return ObservationResult.ignored();
        }
        MiningSessionObservation observation = observation(
                resource,
                quantity,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                "inventory",
                observedAtMillis,
                null,
                null);
        MiningSessionClassification classification =
                MiningSessionClassifier.classify(
                        observation,
                        context(true, true, false, false));
        emitObserved(observation, null, classification);
        emitRejected(observation, null, classification, false);
        return ObservationResult.from(classification, false, null);
    }

    ObservationResult observeExplicitTransfer(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            long observedAtMillis) {
        requireTimestamp(observedAtMillis);
        if (!isObservationEnabled() || resource == null) {
            return ObservationResult.ignored();
        }
        if (evidence != MiningSessionObservation.EvidenceType.MANUAL_TRANSFER
                && evidence
                != MiningSessionObservation.EvidenceType.BAZAAR_PURCHASE) {
            throw new IllegalArgumentException(
                    "Explicit transfer must be manual or Bazaar evidence");
        }
        String source = evidence
                == MiningSessionObservation.EvidenceType.BAZAAR_PURCHASE
                ? "bazaar"
                : "manual-transfer";
        MiningSessionObservation observation = observation(
                resource, quantity, evidence, source, observedAtMillis, null, null);
        MiningSessionClassification classification =
                MiningSessionClassifier.classify(
                        observation,
                        context(true, true, false, false));
        emitObserved(observation, null, classification);
        emitRejected(observation, null, classification, false);
        return ObservationResult.from(classification, false, null);
    }

    int entryCount() {
        return ledger.entryCount();
    }

    long quantity(MiningSessionResource resource) {
        return ledger.quantity(MiningSessionCategory.OTHER_MINED, resource);
    }

    List<MiningSessionLedger.Entry> entries() {
        return ledger.entries();
    }

    int pendingContextCount() {
        return directBreakTracker.pendingContextCount();
    }

    long selectionEpoch() {
        return selectionEpoch;
    }

    private ObservationResult observeSackChange(
            SackChangeParser.Change change,
            long observedAtMillis,
            long coveredBatchMillis) {
        return observeSackChange(change, observedAtMillis, coveredBatchMillis, "");
    }

    private ObservationResult observeSackChange(
            SackChangeParser.Change change,
            long observedAtMillis,
            long coveredBatchMillis,
            String deliveryIdentity) {
        if (change == null) {
            return ObservationResult.ignored();
        }
        Optional<MiningResourceCatalog.ResourceDefinition> definition =
                catalog.fromExactSackItem(change.itemName());
        String sourceName = sourceName(change.sacks());
        if (definition.isEmpty()) {
            return rejectUnknown(
                    change.delta(),
                    MiningSessionObservation.EvidenceType.SACK_CHANGE,
                    sourceName,
                    observedAtMillis);
        }

        MiningResourceCatalog.ResourceDefinition resource = definition.get();
        boolean supportedSource = supportedSackSource(resource, change.sacks());
        MiningSessionDirectBreakTracker.EvidenceChannel channel =
                resource.material() == null
                        ? MiningSessionDirectBreakTracker.EvidenceChannel.SACK_QUANTITY
                        : MiningSessionDirectBreakTracker.EvidenceChannel.MATERIAL_QUANTITY;
        return process(
                resource,
                change.delta(),
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                channel,
                sourceName,
                supportedSource,
                resource.exactQuantitySupported(),
                observedAtMillis,
                coveredBatchMillis,
                deliveryIdentity);
    }

    private ObservationResult process(
            MiningResourceCatalog.ResourceDefinition definition,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            MiningSessionDirectBreakTracker.EvidenceChannel channel,
            String sourceName,
            boolean supportedSource,
            boolean exactQuantity,
            long observedAtMillis) {
        return process(
                definition,
                quantity,
                evidence,
                channel,
                sourceName,
                supportedSource,
                exactQuantity,
                observedAtMillis,
                0L,
                "");
    }

    private ObservationResult process(
            MiningResourceCatalog.ResourceDefinition definition,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            MiningSessionDirectBreakTracker.EvidenceChannel channel,
            String sourceName,
            boolean supportedSource,
            boolean exactQuantity,
            long observedAtMillis,
            long coveredBatchMillis,
            String deliveryIdentity) {
        MiningSessionObservation previewObservation = observation(
                definition.resource(),
                quantity,
                evidence,
                sourceName,
                observedAtMillis,
                null,
                null);
        boolean knownResource = definition.known();
        MiningSessionClassification preview = MiningSessionClassifier.classify(
                previewObservation,
                context(
                        knownResource,
                        supportedSource,
                        exactQuantity,
                        true));

        if (preview.outcome()
                != MiningSessionClassification.Outcome.WOULD_CREDIT) {
            emitObserved(previewObservation, null, preview);
            emitRejected(previewObservation, null, preview, false);
            rememberDelivery(new DeliveryFingerprint(
                    evidence,
                    definition.resource(),
                    quantity,
                    sourceName,
                    selectionEpoch,
                    deliveryIdentity == null ? "" : deliveryIdentity),
                    observedAtMillis);
            return ObservationResult.from(preview, false, null);
        }

        if (!acceptsEvidence(definition, evidence)) {
            MiningSessionClassification unsupported =
                    MiningSessionClassification.unresolved(
                            previewObservation,
                            MiningSessionClassification.ReasonCode
                                    .UNSUPPORTED_EVIDENCE);
            emitObserved(previewObservation, null, unsupported);
            emitRejected(previewObservation, null, unsupported, false);
            rememberDelivery(new DeliveryFingerprint(
                    evidence,
                    definition.resource(),
                    quantity,
                    sourceName,
                    selectionEpoch,
                    deliveryIdentity == null ? "" : deliveryIdentity),
                    observedAtMillis);
            return ObservationResult.from(unsupported, false, null);
        }

        if (preview.category() == MiningSessionCategory.TARGET_MINED) {
            MiningSessionClassification excluded =
                    MiningSessionClassification.rejected(
                            previewObservation,
                            MiningSessionClassification.ReasonCode
                                    .TARGET_EXCLUDED_FROM_OTHERS);
            emitObserved(previewObservation, null, preview);
            emitRejected(previewObservation, null, excluded, false);
            rememberDelivery(new DeliveryFingerprint(
                    evidence,
                    definition.resource(),
                    quantity,
                    sourceName,
                    selectionEpoch,
                    deliveryIdentity == null ? "" : deliveryIdentity),
                    observedAtMillis);
            return ObservationResult.from(excluded, false, null);
        }

        String safeDeliveryIdentity =
                deliveryIdentity == null ? "" : deliveryIdentity;
        DeliveryFingerprint fingerprint = new DeliveryFingerprint(
                evidence,
                definition.resource(),
                quantity,
                sourceName,
                selectionEpoch,
                safeDeliveryIdentity);
        if (isAcceptedDeliveryIdentity(safeDeliveryIdentity)
                || isDuplicate(fingerprint, observedAtMillis)) {
            MiningSessionClassification rejected =
                    MiningSessionClassification.rejected(
                            previewObservation,
                            MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY);
            emitObserved(previewObservation, null, rejected);
            emitRejected(previewObservation, null, rejected, true);
            return ObservationResult.from(rejected, false, null);
        }

        Optional<MiningSessionDirectBreakTracker.Correlation> correlation =
                directBreakTracker.findCorrelation(
                        definition.family(),
                        channel,
                        quantity,
                        supportedSource,
                        false,
                        selectionEpoch,
                        observedAtMillis,
                        coveredBatchMillis);
        if (correlation.isEmpty()) {
            MiningSessionClassification unresolved =
                    MiningSessionClassifier.classify(
                            previewObservation,
                            context(
                                    knownResource,
                                    supportedSource,
                                    exactQuantity,
                                    false));
            emitObserved(previewObservation, null, unresolved);
            emitRejected(previewObservation, null, unresolved, false);
            rememberDelivery(fingerprint, observedAtMillis);
            return ObservationResult.from(unresolved, false, null);
        }

        MiningSessionDirectBreakTracker.Correlation matched = correlation.get();
        String acceptedKey = acceptedKey(matched);
        if (isAcceptedContext(acceptedKey)) {
            MiningSessionClassification rejected =
                    MiningSessionClassification.rejected(
                            previewObservation,
                            MiningSessionClassification.ReasonCode
                                    .DUPLICATE_DELIVERY);
            emitObserved(previewObservation, matched.contextId(), rejected);
            emitRejected(previewObservation, matched.contextId(), rejected, true);
            return ObservationResult.from(rejected, false, matched.contextId());
        }

        String eventId = "other-mining:"
                + matched.contextId()
                + ":"
                + matched.channel().name();
        MiningSessionObservation correlatedObservation = observation(
                definition.resource(),
                quantity,
                evidence,
                sourceName,
                observedAtMillis,
                eventId,
                matched.contextId());
        MiningSessionClassification classification =
                MiningSessionClassifier.classify(
                        correlatedObservation,
                        context(
                                knownResource,
                                supportedSource,
                                exactQuantity,
                                true));
        emitObserved(correlatedObservation, matched.contextId(), classification);

        if (classification.outcome()
                        != MiningSessionClassification.Outcome.WOULD_CREDIT
                || classification.category()
                        != MiningSessionCategory.OTHER_MINED) {
            emitRejected(correlatedObservation, matched.contextId(), classification, false);
            rememberDelivery(fingerprint, observedAtMillis);
            return ObservationResult.from(
                    classification, false, matched.contextId());
        }

        boolean appended = ledger.append(
                classification,
                MiningSessionPriceResolution.unresolved());
        if (!appended) {
            MiningSessionClassification duplicateClassification =
                    MiningSessionClassification.rejected(
                            correlatedObservation,
                            MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY);
            emitRejected(
                    correlatedObservation,
                    matched.contextId(),
                    duplicateClassification,
                    true);
            rememberAcceptedContext(acceptedKey, observedAtMillis);
            rememberAcceptedDeliveryIdentity(
                    safeDeliveryIdentity, observedAtMillis);
            return ObservationResult.from(
                    duplicateClassification, false, matched.contextId());
        }
        if (!directBreakTracker.consume(
                matched, observedAtMillis, coveredBatchMillis)) {
            throw new IllegalStateException(
                    "Accepted shadow delivery lost its direct-break context");
        }
        rememberDelivery(fingerprint, observedAtMillis);
        rememberAcceptedContext(acceptedKey, observedAtMillis);
        rememberAcceptedDeliveryIdentity(
                safeDeliveryIdentity, observedAtMillis);
        emitWouldCredit(correlatedObservation, matched.contextId());
        return ObservationResult.from(classification, true, matched.contextId());
    }

    private ObservationResult rejectUnknown(
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            String sourceName,
            long observedAtMillis) {
        recordDiag(
                "OTHER_MINING_OBSERVED",
                "resourceId=UNKNOWN resourceKind=ITEM"
                        + " quantity=" + quantity
                        + " evidence=" + evidence.name()
                        + " selection=" + selection.id()
                        + " selectionEpoch=" + selectionEpoch
                        + " contextId=none"
                        + " sourceName=" + bounded(sourceName)
                        + " observedAt=" + observedAtMillis
                        + " outcome=UNRESOLVED"
                        + " reason=UNKNOWN_RESOURCE");
        recordDiag(
                "OTHER_MINING_REJECTED",
                "resourceId=UNKNOWN"
                        + " quantity=" + quantity
                        + " evidence=" + evidence.name()
                        + " selection=" + selection.id()
                        + " selectionEpoch=" + selectionEpoch
                        + " contextId=none"
                        + " sourceName=" + bounded(sourceName)
                        + " outcome=UNRESOLVED"
                        + " reason=UNKNOWN_RESOURCE"
                        + " duplicate=false");
        return new ObservationResult(
                MiningSessionClassification.Outcome.UNRESOLVED,
                MiningSessionClassification.ReasonCode.UNKNOWN_RESOURCE,
                false,
                null);
    }

    private MiningSessionClassifier.ClassificationContext context(
            boolean knownResource,
            boolean supportedSource,
            boolean exactQuantity,
            boolean confirmedCorrelation) {
        return new MiningSessionClassifier.ClassificationContext(
                trackerEnabled,
                true,
                selection,
                selectionEpoch,
                knownResource,
                supportedSource,
                false,
                exactQuantity,
                confirmedCorrelation,
                false);
    }

    private MiningSessionObservation observation(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            String sourceName,
            long observedAtMillis,
            String eventId,
            String correlationId) {
        return new MiningSessionObservation(
                resource,
                quantity,
                evidence,
                selection,
                selectionEpoch,
                observedAtMillis,
                eventId,
                correlationId,
                bounded(sourceName),
                null);
    }

    private void recordDiag(String marker, String details) {
        if (diagnostics.isActive()) {
            diagnostics.record(marker, details);
        }
    }

    private void emitObserved(
            MiningSessionObservation observation,
            String contextId,
            MiningSessionClassification classification) {
        recordDiag(
                "OTHER_MINING_OBSERVED",
                baseDetails(observation, contextId)
                        + " observedAt=" + observation.observedAtMillis()
                        + " outcome=" + classification.outcome().name()
                        + " reason=" + classification.reasonCode().name());
    }

    private void emitRejected(
            MiningSessionObservation observation,
            String contextId,
            MiningSessionClassification classification,
            boolean duplicate) {
        recordDiag(
                "OTHER_MINING_REJECTED",
                baseDetails(observation, contextId)
                        + " outcome=" + classification.outcome().name()
                        + " reason=" + classification.reasonCode().name()
                        + " duplicate=" + duplicate);
    }

    private void emitWouldCredit(
            MiningSessionObservation observation,
            String contextId) {
        recordDiag(
                "OTHER_MINING_WOULD_CREDIT",
                "category=OTHER_MINED "
                        + baseDetails(observation, contextId)
                        + " eventId=" + observation.eventId()
                        + " correlationId=" + observation.correlationId()
                        + " ephemeralResourceTotal="
                        + ledger.quantity(
                        MiningSessionCategory.OTHER_MINED,
                        observation.resource())
                        + " ephemeralEntryCount=" + ledger.entryCount());
    }

    private String baseDetails(
            MiningSessionObservation observation,
            String contextId) {
        return "resourceId=" + observation.resource().resourceId()
                + " resourceKind=" + observation.resource().kind().name()
                + " quantity=" + observation.quantity()
                + " evidence=" + observation.evidenceType().name()
                + " selection=" + observation.selectedTracker().id()
                + " selectionEpoch=" + observation.selectionEpoch()
                + " contextId=" + (contextId == null ? "none" : contextId)
                + " sourceName=" + bounded(observation.sourceName());
    }

    private boolean isDuplicate(
            DeliveryFingerprint fingerprint,
            long observedAtMillis) {
        if (fingerprint == null
                || fingerprint.deliveryIdentity().isBlank()) {
            return false;
        }
        pruneDeliveries(observedAtMillis);
        Long previous = recentDeliveries.get(fingerprint);
        return previous != null
                && age(observedAtMillis, previous)
                <= DELIVERY_DEDUPE_WINDOW_MILLIS;
    }

    private boolean isAcceptedContext(String contextChannel) {
        return contextChannel != null
                && !contextChannel.isBlank()
                && acceptedContextChannels.containsKey(contextChannel);
    }

    private boolean isAcceptedDeliveryIdentity(String deliveryIdentity) {
        return deliveryIdentity != null
                && !deliveryIdentity.isBlank()
                && acceptedDeliveryIdentities.containsKey(deliveryIdentity);
    }

    private void rememberAcceptedContext(
            String contextChannel,
            long observedAtMillis) {
        if (contextChannel == null || contextChannel.isBlank()) {
            return;
        }
        acceptedContextChannels.put(contextChannel, observedAtMillis);
        while (acceptedContextChannels.size() > MAX_RECENT_DELIVERIES) {
            Iterator<String> iterator =
                    acceptedContextChannels.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private void rememberAcceptedDeliveryIdentity(
            String deliveryIdentity,
            long observedAtMillis) {
        if (deliveryIdentity == null || deliveryIdentity.isBlank()) {
            return;
        }
        acceptedDeliveryIdentities.put(deliveryIdentity, observedAtMillis);
        while (acceptedDeliveryIdentities.size() > MAX_RECENT_DELIVERIES) {
            Iterator<String> iterator =
                    acceptedDeliveryIdentities.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private static String acceptedKey(
            MiningSessionDirectBreakTracker.Correlation correlation) {
        return correlation.contextId() + ":" + correlation.channel().name();
    }

    private void rememberDelivery(
            DeliveryFingerprint fingerprint,
            long observedAtMillis) {
        if (fingerprint == null
                || fingerprint.deliveryIdentity().isBlank()) {
            return;
        }
        pruneDeliveries(observedAtMillis);
        recentDeliveries.put(fingerprint, observedAtMillis);
        while (recentDeliveries.size() > MAX_RECENT_DELIVERIES) {
            Iterator<DeliveryFingerprint> iterator =
                    recentDeliveries.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    private void pruneDeliveries(long observedAtMillis) {
        recentDeliveries.entrySet().removeIf(entry ->
                age(observedAtMillis, entry.getValue())
                        > DELIVERY_DEDUPE_WINDOW_MILLIS);
    }

    private void transition(
            boolean enabled,
            TrackerSelection selectedTracker) {
        TrackerSelection safeSelection = selectedTracker == null
                ? TrackerSelection.GOLD
                : selectedTracker;
        trackerEnabled = enabled;
        selection = safeSelection;
        invalidateState();
    }

    private void invalidateState() {
        long nextEpoch = Math.addExact(selectionEpoch, 1L);
        resetPendingState();
        if (clearLedgerOnInvalidation) {
            ledger.clear();
        }
        selectionEpoch = nextEpoch;
    }

    private void resetPendingState() {
        directBreakTracker.reset();
        recentDeliveries.clear();
        acceptedContextChannels.clear();
        acceptedDeliveryIdentities.clear();
    }

    private static boolean supportedSackSource(
            MiningResourceCatalog.ResourceDefinition definition,
            List<String> sacks) {
        if (sacks == null) {
            return false;
        }
        for (String sack : sacks) {
            String normalized = MiningResourceCatalog.normalizeAlias(sack);
            if (definition.gemstone() != null
                    && (normalized.equals("gemstone sack")
                    || normalized.equals("gemstones sack"))) {
                return true;
            }
            if (definition.material() != null
                    && (normalized.equals("mining sack")
                    || normalized.equals("enchanted mining sack")
                    || normalized.equals("dwarven sack"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptsEvidence(
            MiningResourceCatalog.ResourceDefinition definition,
            MiningSessionObservation.EvidenceType evidence) {
        if (!definition.shadowAcceptanceSupported()) {
            return false;
        }
        if (definition.material() != null) {
            // Materials may arrive via a Mining Sack notification (when a
            // sack is equipped and has room) or directly into the player
            // inventory (no sack, sack full/disabled, or the item is not
            // sackable at all, e.g. Cobblestone). Both are legitimate,
            // mutually exclusive physical pickup paths for the same
            // confirmed break, so either evidence type may satisfy
            // correlation against a pending direct-break context.
            return evidence == MiningSessionObservation.EvidenceType.SACK_CHANGE
                    || evidence
                    == MiningSessionObservation.EvidenceType.INVENTORY_CHANGE;
        }
        return (evidence == MiningSessionObservation.EvidenceType.SACK_CHANGE
                && definition.gemstoneTier() == GemstoneTier.ROUGH)
                || (evidence
                == MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE
                && definition.gemstoneTier() == GemstoneTier.FLAWED);
    }

    private static String sourceName(List<String> sacks) {
        if (sacks == null || sacks.isEmpty()) {
            return "unknown-sack";
        }
        return bounded(sacks.getFirst());
    }

    private static String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        String sanitized = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim()
                .replaceAll("\\s+", " ");
        return sanitized.length() <= 64
                ? sanitized
                : sanitized.substring(0, 64);
    }

    private static long age(long now, long previous) {
        return now >= previous ? now - previous : 0L;
    }

    private static void requireTimestamp(long observedAtMillis) {
        if (observedAtMillis < 0L) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }
    }

    interface DiagnosticSink {
        DiagnosticSink RECORDER = new DiagnosticSink() {
            @Override
            public boolean isActive() {
                return DiagnosticRecorder.isRecording();
            }

            @Override
            public void record(String marker, String details) {
                DiagnosticRecorder.record(marker, details);
            }
        };

        boolean isActive();

        void record(String marker, String details);
    }

    record ObservationResult(
            MiningSessionClassification.Outcome outcome,
            MiningSessionClassification.ReasonCode reason,
            boolean appended,
            String contextId) {
        private static ObservationResult ignored() {
            return new ObservationResult(null, null, false, null);
        }

        private static ObservationResult from(
                MiningSessionClassification classification,
                boolean appended,
                String contextId) {
            return new ObservationResult(
                    classification.outcome(),
                    classification.reasonCode(),
                    appended,
                    contextId);
        }
    }

    private record DeliveryFingerprint(
            MiningSessionObservation.EvidenceType evidence,
            MiningSessionResource resource,
            long quantity,
            String sourceName,
            long selectionEpoch,
            String deliveryIdentity) {
    }
}
