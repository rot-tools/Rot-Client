package fi.rotclient;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Ephemeral coordinator for the diagnostic mining-session shadow. Existing
 * live accounting remains authoritative and supplies only accepted events.
 */
final class MiningSessionEngine {
    private static final long POWDER_CHEST_SELECTION_EPOCH = 0L;
    private static final int MAX_DIAGNOSTIC_IDENTIFIER_LENGTH = 96;
    private static final long VALUATION_DIAGNOSTIC_INTERVAL_MILLIS = 5_000L;

    private final MiningResourceCatalog catalog;
    private final MiningSessionLedger unifiedLedger;
    private final MiningSessionTargetMirror targetMirror;
    private final MiningSessionShadowObserver otherMinedObserver;
    private final PowderChestContextTracker chestContextTracker;
    private final MiningSessionChestObserver chestObserver;
    private final MiningSessionParity parity;
    private final MiningSessionShadowObserver.DiagnosticSink diagnostics;
    private final MiningSessionPriceProvider priceProvider;
    private final Map<MiningSessionResource, Long> epochTargetQuantities =
            new HashMap<>();
    private final Map<TrackedMaterial, Long> epochMaterialBlocks =
            new EnumMap<>(TrackedMaterial.class);
    private final Map<GemstoneTier, Long> epochGemstoneQuantities =
            new EnumMap<>(GemstoneTier.class);
    private final Map<String, Long> lastValuationDiagnosticMillis =
            new HashMap<>();

    private long sessionSequence;
    private long sessionId;
    private long sessionEpoch;
    private long selectionEpoch;
    private long sessionStartMillis;
    private long selectionStartMillis;
    private long epochGemstoneRoughEquivalent;
    private long epochGemstoneBlocks;
    private Long lastAcceptedEventTimestamp;
    private boolean diagnosticsActive;
    /**
     * Domain observation for Current Session (OTHER/CHEST/etc). Independent of
     * {@link DiagnosticRecorder} instrumentation. Kept in lockstep with the
     * analytics lifecycle start/stop APIs so "Session Analytics running" means
     * collection is active — but recording markers is optional.
     */
    private boolean collectionActive;
    private boolean trackerEnabled;
    private TrackerSelection selection = TrackerSelection.GOLD;
    private MiningSessionSnapshot retainedFinalSnapshot;

    MiningSessionEngine() {
        this(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                MiningSessionShadowObserver.DiagnosticSink.RECORDER,
                0L,
                0L,
                0L,
                MiningSessionBazaarPriceProvider.INSTANCE);
    }

    MiningSessionEngine(
            MiningSessionShadowObserver.DiagnosticSink diagnostics) {
        this(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                diagnostics,
                0L,
                0L,
                0L,
                MiningSessionBazaarPriceProvider.INSTANCE);
    }

    MiningSessionEngine(
            MiningResourceCatalog catalog,
            MiningSessionLedger unifiedLedger,
            MiningSessionShadowObserver.DiagnosticSink diagnostics,
            long initialSessionSequence,
            long initialSessionEpoch,
            long initialSelectionEpoch) {
        this(
                catalog,
                unifiedLedger,
                diagnostics,
                initialSessionSequence,
                initialSessionEpoch,
                initialSelectionEpoch,
                MiningSessionBazaarPriceProvider.INSTANCE);
    }

    MiningSessionEngine(
            MiningResourceCatalog catalog,
            MiningSessionLedger unifiedLedger,
            MiningSessionShadowObserver.DiagnosticSink diagnostics,
            long initialSessionSequence,
            long initialSessionEpoch,
            long initialSelectionEpoch,
            MiningSessionPriceProvider priceProvider) {
        if (catalog == null
                || unifiedLedger == null
                || diagnostics == null
                || priceProvider == null) {
            throw new IllegalArgumentException(
                    "Mining session engine dependencies cannot be null");
        }
        if (initialSessionSequence < 0L
                || initialSessionEpoch < 0L
                || initialSelectionEpoch < 0L) {
            throw new IllegalArgumentException(
                    "Mining session counters cannot be negative");
        }

        this.catalog = catalog;
        this.unifiedLedger = unifiedLedger;
        this.diagnostics = diagnostics;
        this.priceProvider = priceProvider;
        this.targetMirror = new MiningSessionTargetMirror(
                unifiedLedger,
                catalog);
        this.otherMinedObserver = new MiningSessionShadowObserver(
                catalog,
                new MiningSessionDirectBreakTracker(),
                unifiedLedger,
                diagnostics,
                false);
        this.chestContextTracker = new PowderChestContextTracker(diagnostics);
        this.chestObserver = new MiningSessionChestObserver(
                catalog,
                unifiedLedger,
                diagnostics);
        this.parity = new MiningSessionParity(diagnostics);
        this.sessionSequence = initialSessionSequence;
        this.sessionEpoch = initialSessionEpoch;
        this.selectionEpoch = initialSelectionEpoch;
    }

    void onDiagnosticStart(
            boolean enabled,
            TrackerSelection selectedTracker,
            MiningSessionParity.LiveBaseline liveBaseline,
            long startedAtMillis) {
        requireTimestamp(startedAtMillis);
        requireSelectionAndBaseline(selectedTracker, liveBaseline);

        long nextSessionId = Math.addExact(sessionSequence, 1L);
        long nextSessionEpoch = Math.addExact(sessionEpoch, 1L);
        long nextSelectionEpoch = Math.addExact(selectionEpoch, 1L);
        MiningSessionParity.Context nextContext = new MiningSessionParity.Context(
                nextSessionId,
                nextSessionEpoch,
                nextSelectionEpoch,
                selectedTracker,
                startedAtMillis);

        unifiedLedger.clear();
        parity.reset();
        clearEpochState();
        parity.beginContext(nextContext, liveBaseline);

        sessionSequence = nextSessionId;
        sessionId = nextSessionId;
        sessionEpoch = nextSessionEpoch;
        selectionEpoch = nextSelectionEpoch;
        sessionStartMillis = startedAtMillis;
        selectionStartMillis = startedAtMillis;
        diagnosticsActive = true;
        collectionActive = true;
        trackerEnabled = enabled;
        selection = selectedTracker;
        lastAcceptedEventTimestamp = null;
        retainedFinalSnapshot = null;
        lastValuationDiagnosticMillis.clear();
        resetChestState(startedAtMillis);
        synchronizeObserver();

        emit(
                "MINING_SESSION_STARTED",
                lifecycleDetails());
    }

    Optional<MiningSessionSnapshot> onDiagnosticStop(long stoppedAtMillis) {
        requireTimestamp(stoppedAtMillis);
        if (!collectionActive || sessionId == 0L) {
            synchronizeObserver();
            return Optional.ofNullable(retainedFinalSnapshot);
        }

        diagnosticsActive = false;
        collectionActive = false;
        abandonChestState(
                "CHEST_CONTEXT_INTERRUPTED",
                stoppedAtMillis);
        chestObserver.resetState();
        synchronizeObserver();
        MiningSessionSnapshot finalSnapshot = capture(stoppedAtMillis);
        retainedFinalSnapshot = finalSnapshot;
        emitSnapshot(finalSnapshot);
        emit(
                "MINING_SESSION_STOPPED",
                lifecycleDetails()
                        + " snapshotAt=" + stoppedAtMillis
                        + " entryCount=" + finalSnapshot.entryCount()
                        + " parityStatus="
                        + finalSnapshot.targetParityStatus().name()
                        + " mismatchCount="
                        + finalSnapshot.parityMismatchCount());
        return Optional.of(finalSnapshot);
    }

    void onTrackerEnabled(
            TrackerSelection selectedTracker,
            MiningSessionParity.LiveBaseline liveBaseline,
            long transitionedAtMillis) {
        transitionSelectionContext(
                true,
                selectedTracker,
                liveBaseline,
                transitionedAtMillis,
                false);
    }

    void onTrackerDisabled(
            TrackerSelection selectedTracker,
            MiningSessionParity.LiveBaseline liveBaseline,
            long transitionedAtMillis) {
        transitionSelectionContext(
                false,
                selectedTracker,
                liveBaseline,
                transitionedAtMillis,
                false);
    }

    void onSelectionChanged(
            TrackerSelection selectedTracker,
            boolean enabled,
            MiningSessionParity.LiveBaseline liveBaseline,
            long transitionedAtMillis) {
        transitionSelectionContext(
                enabled,
                selectedTracker,
                liveBaseline,
                transitionedAtMillis,
                false);
    }

    void onWorldChanged(
            TrackerSelection selectedTracker,
            boolean enabled,
            MiningSessionParity.LiveBaseline liveBaseline,
            long transitionedAtMillis) {
        transitionSelectionContext(
                enabled,
                selectedTracker,
                liveBaseline,
                transitionedAtMillis,
                true);
    }

    void onReset(
            boolean enabled,
            TrackerSelection selectedTracker,
            MiningSessionParity.LiveBaseline liveBaseline,
            long resetAtMillis) {
        requireTimestamp(resetAtMillis);
        requireSelectionAndBaseline(selectedTracker, liveBaseline);

        long nextSessionId = Math.addExact(sessionSequence, 1L);
        long nextSessionEpoch = Math.addExact(sessionEpoch, 1L);
        long nextSelectionEpoch = Math.addExact(selectionEpoch, 1L);
        MiningSessionParity.Context nextContext = new MiningSessionParity.Context(
                nextSessionId,
                nextSessionEpoch,
                nextSelectionEpoch,
                selectedTracker,
                resetAtMillis);

        unifiedLedger.clear();
        parity.reset();
        clearEpochState();
        parity.beginContext(nextContext, liveBaseline);

        sessionSequence = nextSessionId;
        sessionId = nextSessionId;
        sessionEpoch = nextSessionEpoch;
        selectionEpoch = nextSelectionEpoch;
        sessionStartMillis = resetAtMillis;
        selectionStartMillis = resetAtMillis;
        trackerEnabled = enabled;
        selection = selectedTracker;
        lastAcceptedEventTimestamp = null;
        retainedFinalSnapshot = null;
        resetChestState(resetAtMillis);
        synchronizeObserver();

        emit(
                "MINING_SESSION_RESET",
                lifecycleDetails());
    }

    /**
     * Clears only ephemeral analytics/session shadow state back to
     * not-started. Does not mutate live tracker totals, persistence, or
     * Bazaar caches.
     */
    void clearAnalyticsSession(long clearedAtMillis) {
        requireTimestamp(clearedAtMillis);
        diagnosticsActive = false;
        collectionActive = false;
        resetChestState(clearedAtMillis);

        unifiedLedger.clear();
        parity.reset();
        clearEpochState();
        lastAcceptedEventTimestamp = null;
        retainedFinalSnapshot = null;
        sessionId = 0L;
        sessionStartMillis = 0L;
        selectionStartMillis = 0L;
        synchronizeObserver();
        emit(
                "MINING_SESSION_CLEARED",
                "trackerEnabled=" + trackerEnabled
                        + " selection=" + selection.id());
    }

    boolean hasRetainedFinalSnapshot() {
        return retainedFinalSnapshot != null;
    }

    boolean hasSessionIdentity() {
        return sessionId > 0L;
    }

    EventDisposition onAcceptedTargetMaterialQuantity(
            TrackedMaterial material,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            long liveCumulativeNormalizedQuantity,
            long observedAtMillis,
            String eventId,
            String sourceName) {
        requireTimestamp(observedAtMillis);
        requireNonNegative(
                liveCumulativeNormalizedQuantity,
                "Live material quantity");
        if (material == null || evidence == null) {
            throw new IllegalArgumentException(
                    "Material target event fields cannot be null");
        }
        if (!acceptsEvents() && !collectionActive) {
            return EventDisposition.INACTIVE;
        }
        MiningSessionResource resource = catalog.fromMaterial(material)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material has no canonical mining resource"))
                .resource();

        if (!acceptsEvents()) {
            emitTargetRejected(
                    resource,
                    quantity,
                    evidence,
                    eventId,
                    observedAtMillis,
                    "ENGINE_INACTIVE");
            return EventDisposition.INACTIVE;
        }

        Long nextEpochQuantity = quantity > 0L
                ? Math.addExact(
                epochTargetQuantities.getOrDefault(resource, 0L),
                quantity)
                : null;
        MiningSessionTargetMirror.QuantityResult result =
                targetMirror.mirrorQuantity(
                        new MiningSessionTargetMirror.QuantityEvent(
                                resource,
                                quantity,
                                evidence,
                                selection,
                                selectionEpoch,
                                observedAtMillis,
                                eventId,
                                null,
                                sourceName));
        EventDisposition disposition = disposition(result);
        if (!result.appended()) {
            emitTargetRejected(
                    resource,
                    quantity,
                    evidence,
                    eventId,
                    observedAtMillis,
                    result.status().name() + ":" + result.reason().name());
            return disposition;
        }

        epochTargetQuantities.put(resource, nextEpochQuantity);
        lastAcceptedEventTimestamp = observedAtMillis;
        parity.compare(
                MiningSessionParity.Sample.materialNormalized(
                        currentParityContext(),
                        MiningSessionCategory.TARGET_MINED,
                        material,
                        nextEpochQuantity,
                        liveCumulativeNormalizedQuantity,
                        observedAtMillis));
        emitTargetMirrored(
                result.classification().observation(),
                parity.snapshot().status());
        return disposition;
    }

    EventDisposition onAcceptedTargetGemstoneQuantity(
            GemstoneType gemstone,
            GemstoneTier tier,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            long liveCumulativeTierQuantity,
            long liveCumulativeRoughEquivalent,
            long observedAtMillis,
            String eventId,
            String sourceName) {
        requireTimestamp(observedAtMillis);
        requireNonNegative(
                liveCumulativeTierQuantity,
                "Live gemstone tier quantity");
        requireNonNegative(
                liveCumulativeRoughEquivalent,
                "Live gemstone Rough Equivalent");
        if (gemstone == null || tier == null || evidence == null) {
            throw new IllegalArgumentException(
                    "Gemstone target event fields cannot be null");
        }
        if (!acceptsEvents() && !collectionActive) {
            return EventDisposition.INACTIVE;
        }
        MiningSessionResource resource = catalog.fromGemstone(gemstone, tier)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gemstone tier has no canonical mining resource"))
                .resource();

        if (!acceptsEvents()) {
            emitTargetRejected(
                    resource,
                    quantity,
                    evidence,
                    eventId,
                    observedAtMillis,
                    "ENGINE_INACTIVE");
            return EventDisposition.INACTIVE;
        }

        Long nextResourceQuantity = quantity > 0L
                ? Math.addExact(
                epochTargetQuantities.getOrDefault(resource, 0L),
                quantity)
                : null;
        Long nextTierQuantity = quantity > 0L
                ? Math.addExact(
                epochGemstoneQuantities.getOrDefault(tier, 0L),
                quantity)
                : null;
        Long nextRoughEquivalent = quantity > 0L
                ? Math.addExact(
                epochGemstoneRoughEquivalent,
                Math.multiplyExact(quantity, tier.roughEquivalent()))
                : null;
        MiningSessionTargetMirror.QuantityResult result =
                targetMirror.mirrorQuantity(
                        new MiningSessionTargetMirror.QuantityEvent(
                                resource,
                                quantity,
                                evidence,
                                selection,
                                selectionEpoch,
                                observedAtMillis,
                                eventId,
                                null,
                                sourceName));
        EventDisposition disposition = disposition(result);
        if (!result.appended()) {
            emitTargetRejected(
                    resource,
                    quantity,
                    evidence,
                    eventId,
                    observedAtMillis,
                    result.status().name() + ":" + result.reason().name());
            return disposition;
        }

        epochTargetQuantities.put(resource, nextResourceQuantity);
        epochGemstoneQuantities.put(tier, nextTierQuantity);
        epochGemstoneRoughEquivalent = nextRoughEquivalent;
        lastAcceptedEventTimestamp = observedAtMillis;
        MiningSessionParity.Context context = currentParityContext();
        parity.compare(MiningSessionParity.Sample.gemstoneTier(
                context,
                MiningSessionCategory.TARGET_MINED,
                gemstone,
                tier,
                nextTierQuantity,
                liveCumulativeTierQuantity,
                observedAtMillis));
        parity.compare(
                MiningSessionParity.Sample.gemstoneRoughEquivalent(
                        context,
                        MiningSessionCategory.TARGET_MINED,
                        gemstone,
                        nextRoughEquivalent,
                        liveCumulativeRoughEquivalent,
                        observedAtMillis));
        emitTargetMirrored(
                result.classification().observation(),
                parity.snapshot().status());
        return disposition;
    }

    boolean onAcceptedTargetMaterialBlock(
            TrackedMaterial material,
            int count,
            long liveCumulativeBlocks,
            long observedAtMillis,
            String eventId,
            String sourceName) {
        requireTimestamp(observedAtMillis);
        requireNonNegative(liveCumulativeBlocks, "Live material blocks");
        if (material == null) {
            throw new IllegalArgumentException(
                    "Material block identity cannot be null");
        }
        if (!acceptsEvents() || count <= 0) {
            return false;
        }
        MiningSessionResource resource = catalog.fromMaterial(material)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material has no canonical mining resource"))
                .resource();

        long nextBlocks = Math.addExact(
                epochMaterialBlocks.getOrDefault(material, 0L),
                count);
        MiningSessionTargetMirror.BlockResult result = targetMirror.observeBlock(
                new MiningSessionTargetMirror.BlockObservation(
                        resource,
                        selection,
                        selectionEpoch,
                        observedAtMillis,
                        eventId,
                        null,
                        sourceName));
        if (!result.acceptedForParity()) {
            return false;
        }

        epochMaterialBlocks.put(material, nextBlocks);
        lastAcceptedEventTimestamp = observedAtMillis;
        parity.compare(MiningSessionParity.Sample.materialBlocks(
                currentParityContext(),
                MiningSessionCategory.TARGET_MINED,
                material,
                nextBlocks,
                liveCumulativeBlocks,
                observedAtMillis));
        return true;
    }

    boolean onAcceptedTargetGemstoneBlock(
            GemstoneType gemstone,
            long liveCumulativeBlocks,
            long observedAtMillis,
            String eventId,
            String sourceName) {
        requireTimestamp(observedAtMillis);
        requireNonNegative(liveCumulativeBlocks, "Live gemstone blocks");
        if (gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone block identity cannot be null");
        }
        if (!acceptsEvents()) {
            return false;
        }
        MiningSessionResource resource = catalog.fromGemstone(
                        gemstone,
                        GemstoneTier.ROUGH)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Gemstone has no canonical mining resource"))
                .resource();

        long nextBlocks = Math.addExact(epochGemstoneBlocks, 1L);
        MiningSessionTargetMirror.BlockResult result = targetMirror.observeBlock(
                new MiningSessionTargetMirror.BlockObservation(
                        resource,
                        selection,
                        selectionEpoch,
                        observedAtMillis,
                        eventId,
                        null,
                        sourceName));
        if (!result.acceptedForParity()) {
            return false;
        }

        epochGemstoneBlocks = nextBlocks;
        lastAcceptedEventTimestamp = observedAtMillis;
        parity.compare(MiningSessionParity.Sample.gemstoneBlocks(
                currentParityContext(),
                MiningSessionCategory.TARGET_MINED,
                gemstone,
                nextBlocks,
                liveCumulativeBlocks,
                observedAtMillis));
        return true;
    }

    void onConfirmedMaterialBreak(
            TrackedMaterial material,
            int count,
            long observedAtMillis) {
        otherMinedObserver.onConfirmedMaterialBreak(
                material,
                count,
                observedAtMillis);
    }

    void onConfirmedGemstoneBreak(
            GemstoneType gemstone,
            long observedAtMillis) {
        otherMinedObserver.onConfirmedGemstoneBreak(
                gemstone,
                observedAtMillis);
    }

    void observeSackChanges(
            List<SackChangeParser.Change> changes,
            long observedAtMillis) {
        noteSackChangesForResearch(changes, observedAtMillis);

        int previousEntryCount = unifiedLedger.entryCount();
        otherMinedObserver.observeSackChanges(changes, observedAtMillis);
        if (unifiedLedger.entryCount() > previousEntryCount) {
            lastAcceptedEventTimestamp = observedAtMillis;
        }
    }

    /**
     * Powder-chest research bookkeeping for a parsed Sack batch. Does not
     * classify or credit OTHER_MINED — callers must hand each change to
     * {@link #observeSackChange}.
     */
    void noteSackChangesForResearch(
            List<SackChangeParser.Change> changes,
            long observedAtMillis) {
        if (!collectionActive) {
            return;
        }
        chestContextTracker.expireIfNeeded(observedAtMillis);
        chestObserver.noteSackChangesForResearch(
                changes,
                observedAtMillis);
    }

    /**
     * Canonical per-item Sack handoff into the OTHER_MINED classifier /
     * ledger. Returns the observation result so runtime diagnostics can
     * subscribe without becoming the terminal consumer.
     */
    MiningSessionShadowObserver.ObservationResult observeSackChange(
            SackChangeParser.Change change,
            long observedAtMillis) {
        return observeSackChange(change, observedAtMillis, 0L);
    }

    MiningSessionShadowObserver.ObservationResult observeSackChange(
            SackChangeParser.Change change,
            long observedAtMillis,
            long coveredBatchMillis) {
        return observeSackChange(
                change, observedAtMillis, coveredBatchMillis, "");
    }

    MiningSessionShadowObserver.ObservationResult observeSackChange(
            SackChangeParser.Change change,
            long observedAtMillis,
            long coveredBatchMillis,
            String deliveryIdentity) {
        int previousEntryCount = unifiedLedger.entryCount();
        MiningSessionShadowObserver.ObservationResult result =
                otherMinedObserver.observeSackChangesForTest(
                        change,
                        observedAtMillis,
                        coveredBatchMillis,
                        deliveryIdentity);
        if (unifiedLedger.entryCount() > previousEntryCount) {
            lastAcceptedEventTimestamp = observedAtMillis;
        }
        return result;
    }

    /**
     * Reports a non-target material's exact raw inventory increase (no
     * Mining Sack notification observed) so it can be correlated with a
     * confirmed break and credited as OTHER_MINED. See
     * {@link MiningSessionShadowObserver#observeMaterialInventoryGain}.
     */
    MiningSessionShadowObserver.ObservationResult onMaterialInventoryGain(
            TrackedMaterial material,
            long quantity,
            long observedAtMillis) {
        int previousEntryCount = unifiedLedger.entryCount();
        MiningSessionShadowObserver.ObservationResult result =
                otherMinedObserver.observeMaterialInventoryGain(
                        material,
                        quantity,
                        observedAtMillis);
        if (unifiedLedger.entryCount() > previousEntryCount) {
            lastAcceptedEventTimestamp = observedAtMillis;
        }
        return result;
    }

    MiningSessionChestObserver.FinalizationResult observePowderChestChatLine(
            String rawLine,
            long observedAtMillis) {
        if (!collectionActive) {
            return MiningSessionChestObserver.FinalizationResult.none();
        }
        requireTimestamp(observedAtMillis);

        chestContextTracker.expireIfNeeded(observedAtMillis);
        chestContextTracker.synchronizeEpochs(
                sessionEpoch,
                POWDER_CHEST_SELECTION_EPOCH,
                observedAtMillis);

        PowderChestChatParser.ParsedLine parsedLine =
                PowderChestChatParser.parse(rawLine);
        Optional<PowderChestContextTracker.FinalizedChestContext>
                finalized = chestContextTracker.observeLine(
                parsedLine,
                sessionEpoch,
                POWDER_CHEST_SELECTION_EPOCH,
                observedAtMillis);
        if (finalized.isEmpty()) {
            return MiningSessionChestObserver.FinalizationResult.none();
        }

        MiningSessionChestObserver.FinalizationResult result =
                chestObserver.finalizeContextDetailed(
                finalized.get(),
                observedAtMillis);
        if (result.accepted()) {
            lastAcceptedEventTimestamp = observedAtMillis;
        }
        return result;
    }

    void onDiagnosticTick(long nowMillis) {
        if (!collectionActive) {
            return;
        }
        requireTimestamp(nowMillis);
        chestContextTracker.expireIfNeeded(nowMillis);
    }

    /**
     * Drops an unfinished Powder Chest chat block when the standalone tracker
     * is turned off. The context sequence must keep advancing: recycling
     * {@code chest-<epoch>-1} identities collides with the still-live shadow
     * ledger and rejects later real chests as {@code REJECTED_CONFLICT}.
     */
    void onPowderChestTrackerDisabled(long nowMillis) {
        requireTimestamp(nowMillis);
        abandonChestState("CHEST_CONTEXT_INTERRUPTED", nowMillis);
        chestObserver.resetState();
    }

    /**
     * Analytics lifecycle / UI "session running" flag. Historically named
     * diagnostics; domain collection uses {@link #isCollectionActive()}.
     */
    boolean isDiagnosticsActive() {
        return diagnosticsActive;
    }

    /** True when Current Session observation (OTHER/CHEST/…) is accepting. */
    boolean isCollectionActive() {
        return collectionActive;
    }

    MiningSessionShadowObserver.ObservationResult observePristine(
            PristineMessageParser.Reward reward,
            long observedAtMillis) {
        return observePristine(reward, observedAtMillis, "");
    }

    MiningSessionShadowObserver.ObservationResult observePristine(
            PristineMessageParser.Reward reward,
            long observedAtMillis,
            String deliveryIdentity) {
        MiningSessionShadowObserver.ObservationResult result =
                otherMinedObserver.observePristine(
                        reward,
                        observedAtMillis,
                        deliveryIdentity);
        if (result.appended()) {
            lastAcceptedEventTimestamp = observedAtMillis;
        }
        return result;
    }

    boolean isObservationEnabled() {
        return acceptsEvents();
    }

    boolean supportsMaterialBreak(TrackedMaterial material) {
        return otherMinedObserver.supportsMaterialBreak(material);
    }

    Optional<MiningSessionSnapshot> snapshot(long snapshotAtMillis) {
        requireTimestamp(snapshotAtMillis);
        if (collectionActive && sessionId > 0L) {
            return Optional.of(capture(snapshotAtMillis));
        }
        if (retainedFinalSnapshot != null) {
            return Optional.of(retainedFinalSnapshot);
        }
        return sessionId == 0L
                ? Optional.empty()
                : Optional.of(capture(snapshotAtMillis));
    }

    /**
     * Captures the current Bazaar instant-sell price book for Current Session
     * per-row valuation refresh. Does not mutate ledgers. External-data
     * failures degrade to an unavailable book — never throw into the tick.
     */
    MiningSessionPriceBook capturePriceBook(long observedAtMillis) {
        try {
            requireTimestamp(observedAtMillis);
            return priceProvider.capture(observedAtMillis);
        } catch (RuntimeException failure) {
            emit(
                    "SHADOW_PRICE_BOOK_CAPTURE_FAILED",
                    lifecycleDetails()
                            + " reason="
                            + (failure.getClass().getSimpleName() == null
                            ? "RuntimeException"
                            : failure.getClass().getSimpleName()));
            return MiningSessionPriceBook.unavailable();
        }
    }

    OptionalLong lastAcceptedEventTimestamp() {
        return lastAcceptedEventTimestamp == null
                ? OptionalLong.empty()
                : OptionalLong.of(lastAcceptedEventTimestamp);
    }

    int pendingOtherContextCount() {
        return otherMinedObserver.pendingContextCount();
    }

    private void transitionSelectionContext(
            boolean enabled,
            TrackerSelection selectedTracker,
            MiningSessionParity.LiveBaseline liveBaseline,
            long transitionedAtMillis,
            boolean interruptPowderChest) {
        requireTimestamp(transitionedAtMillis);
        requireSelectionAndBaseline(selectedTracker, liveBaseline);
        long nextSelectionEpoch = Math.addExact(selectionEpoch, 1L);
        MiningSessionParity.Context nextContext = sessionId == 0L
                ? null
                : new MiningSessionParity.Context(
                sessionId,
                sessionEpoch,
                nextSelectionEpoch,
                selectedTracker,
                transitionedAtMillis);

        clearEpochState();
        if (collectionActive) {
            if (interruptPowderChest) {
                abandonChestState(
                        "CHEST_CONTEXT_INTERRUPTED",
                        transitionedAtMillis);
                chestObserver.resetState();
            }
            parity.beginContext(nextContext, liveBaseline);
        }
        trackerEnabled = enabled;
        selection = selectedTracker;
        selectionEpoch = nextSelectionEpoch;
        selectionStartMillis = transitionedAtMillis;
        synchronizeObserver();
    }

    private MiningSessionSnapshot capture(long snapshotAtMillis) {
        MiningSessionValuation valuation = computeValuation(snapshotAtMillis);
        return MiningSessionSnapshot.capture(
                sessionId,
                sessionStartMillis,
                selectionStartMillis,
                snapshotAtMillis,
                collectionActive,
                trackerEnabled,
                selection,
                sessionEpoch,
                selectionEpoch,
                unifiedLedger,
                parity.snapshot(),
                lastAcceptedEventTimestamp,
                valuation);
    }

    private MiningSessionValuation computeValuation(long snapshotAtMillis) {
        try {
            MiningSessionPriceBook priceBook =
                    priceProvider.capture(snapshotAtMillis);
            if (!priceBook.isAvailable()) {
                String reason = priceBook.invalidTimestamp()
                        ? " reason=INVALID_TIMESTAMP"
                        : "";
                if (shouldEmitValuationDiagnostic(
                        "SHADOW_PRICE_BOOK_UNAVAILABLE",
                        snapshotAtMillis)) {
                    emit(
                            "SHADOW_PRICE_BOOK_UNAVAILABLE",
                            lifecycleDetails() + reason);
                }
            } else {
                emitValuationDiagnostics(
                        "SHADOW_PRICE_BOOK_CAPTURED",
                        priceBook,
                        null,
                        snapshotAtMillis);
            }

            MiningSessionValuation valuation =
                    MiningSessionPriceResolver.resolve(
                            unifiedLedger.entries(),
                            priceBook,
                            snapshotAtMillis);
            emitValuationDiagnostics(
                    "SHADOW_VALUATION_COMPUTED",
                    priceBook,
                    valuation,
                    snapshotAtMillis);
            return valuation;
        } catch (RuntimeException failure) {
            if (shouldEmitValuationDiagnostic(
                    "SHADOW_VALUATION_FAILED",
                    snapshotAtMillis)) {
                emit(
                        "SHADOW_VALUATION_FAILED",
                        lifecycleDetails()
                                + " reason="
                                + bounded(failure.getClass().getSimpleName()));
            }
            return MiningSessionValuation.unavailable();
        }
    }

    private void emitValuationDiagnostics(
            String marker,
            MiningSessionPriceBook priceBook,
            MiningSessionValuation valuation,
            long snapshotAtMillis) {
        if (!diagnostics.isActive()) {
            return;
        }
        if (!shouldEmitValuationDiagnostic(marker, snapshotAtMillis)) {
            return;
        }

        String details = lifecycleDetails();
        if (priceBook != null && priceBook.isAvailable()) {
            details += " basis="
                    + MiningSessionValuation.PRICE_BASIS_LABEL
                    + " observedAt=" + priceBook.observedAtMillis()
                    + " ageMillis="
                    + priceBook.quoteAgeMillis(snapshotAtMillis)
                            .orElse(-1L);
            if (valuation == null) {
                int gemstoneProductCount = 0;
                for (String productId
                        : MiningSessionResourcePriceMapping
                        .pricedGemstoneProductIds()) {
                    if (priceBook.unitPrice(productId).isPresent()) {
                        gemstoneProductCount++;
                    }
                }
                details += " gemstoneProductCount=" + gemstoneProductCount;
            }
        }
        if (valuation != null) {
            details += " resolvedEntries="
                    + valuation.resolvedEntryCount()
                    + " staleEntries=" + valuation.staleEntryCount()
                    + " unavailableEntries="
                    + valuation.unavailableEntryCount()
                    + " unsupportedEntries="
                    + valuation.unsupportedEntryCount()
                    + " resolvedTotal="
                    + MiningSessionPriceBook.formatCoinAmount(
                    valuation.resolvedItemValue());
        }
        emit(marker, details);
    }

    private boolean shouldEmitValuationDiagnostic(
            String marker,
            long snapshotAtMillis) {
        Long previous = lastValuationDiagnosticMillis.get(marker);
        if (previous != null
                && snapshotAtMillis >= previous
                && snapshotAtMillis - previous
                        < VALUATION_DIAGNOSTIC_INTERVAL_MILLIS) {
            return false;
        }
        lastValuationDiagnosticMillis.put(marker, snapshotAtMillis);
        return true;
    }

    private MiningSessionParity.Context currentParityContext() {
        return new MiningSessionParity.Context(
                sessionId,
                sessionEpoch,
                selectionEpoch,
                selection,
                selectionStartMillis);
    }

    private void synchronizeObserver() {
        // OTHER / chest collection follows Current Session collectionActive.
        // TARGET_MINED parity still requires tracker-enabled acceptsEvents().
        // DiagnosticRecorder instrumentation is independent of this flag.
        otherMinedObserver.synchronizeEngineState(
                collectionActive,
                selection,
                selectionEpoch);
        chestObserver.synchronizeEngineState(
                collectionActive,
                selection,
                POWDER_CHEST_SELECTION_EPOCH);
    }

    private void abandonChestState(String reason, long observedAtMillis) {
        chestContextTracker.abandon(reason, observedAtMillis);
    }

    /** Full chest reset for session start/stop/reset, where the ledger is also cleared. */
    private void resetChestState(long observedAtMillis) {
        abandonChestState("CHEST_CONTEXT_INTERRUPTED", observedAtMillis);
        chestContextTracker.reset();
        chestObserver.resetState();
    }

    private void clearEpochState() {
        epochTargetQuantities.clear();
        epochMaterialBlocks.clear();
        epochGemstoneQuantities.clear();
        epochGemstoneRoughEquivalent = 0L;
        epochGemstoneBlocks = 0L;
    }

    private boolean acceptsEvents() {
        return collectionActive
                && trackerEnabled
                && sessionId > 0L;
    }

    private String lifecycleDetails() {
        return "sessionId=" + sessionId
                + " sessionEpoch=" + sessionEpoch
                + " selection=" + selection.id()
                + " selectionEpoch=" + selectionEpoch
                + " trackerEnabled=" + trackerEnabled;
    }

    private void emitTargetMirrored(
            MiningSessionObservation observation,
            MiningSessionParity.Status parityStatus) {
        if (!diagnostics.isActive()) {
            return;
        }
        MiningSessionResource resource = observation.resource();
        emit(
                "MINING_SESSION_TARGET_MIRRORED",
                lifecycleDetails()
                        + " resourceId=" + resource.resourceId()
                        + " category=TARGET_MINED"
                        + " quantity=" + observation.quantity()
                        + " evidence=" + observation.evidenceType().name()
                        + " eventId=" + bounded(observation.eventId())
                        + " correlationId="
                        + bounded(observation.correlationId())
                        + " observedAt=" + observation.observedAtMillis()
                        + " shadowResourceTotal="
                        + unifiedLedger.quantity(
                        MiningSessionCategory.TARGET_MINED,
                        resource)
                        + " shadowCategoryTotal="
                        + unifiedLedger.totalItemQuantity(
                        MiningSessionCategory.TARGET_MINED)
                        + " entryCount=" + unifiedLedger.entryCount()
                        + " parityStatus=" + parityStatus.name());
    }

    private void emitTargetRejected(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            String eventId,
            long observedAtMillis,
            String reason) {
        if (!diagnostics.isActive()) {
            return;
        }
        emit(
                "MINING_SESSION_TARGET_REJECTED",
                lifecycleDetails()
                        + " resourceId=" + resource.resourceId()
                        + " category=TARGET_MINED"
                        + " quantity=" + quantity
                        + " evidence="
                        + (evidence == null ? "none" : evidence.name())
                        + " eventId=" + bounded(eventId)
                        + " correlationId=none"
                        + " observedAt=" + observedAtMillis
                        + " reason=" + bounded(reason)
                        + " entryCount=" + unifiedLedger.entryCount());
    }

    private void emitSnapshot(MiningSessionSnapshot snapshot) {
        emit(
                "MINING_SESSION_SNAPSHOT",
                lifecycleDetails()
                        + " snapshotAt=" + snapshot.snapshotAtMillis()
                        + " entryCount=" + snapshot.entryCount()
                        + " targetEntryCount="
                        + snapshot.targetMinedEntryCount()
                        + " otherEntryCount="
                        + snapshot.otherMinedEntryCount()
                        + " unresolvedPriceEntryCount="
                        + snapshot.unresolvedPriceEntryCount()
                        + " parityStatus="
                        + snapshot.targetParityStatus().name()
                        + " mismatchCount="
                        + snapshot.parityMismatchCount());
    }

    private void emit(String marker, String details) {
        if (diagnostics.isActive()) {
            diagnostics.record(marker, details);
        }
    }

    private static EventDisposition disposition(
            MiningSessionTargetMirror.QuantityResult result) {
        return switch (result.status()) {
            case APPENDED -> EventDisposition.APPENDED;
            case DUPLICATE -> EventDisposition.DUPLICATE;
            case IDENTITY_CONFLICT -> EventDisposition.IDENTITY_CONFLICT;
            case REJECTED, INVARIANT_REJECTED -> EventDisposition.REJECTED;
        };
    }

    private static void requireSelectionAndBaseline(
            TrackerSelection selectedTracker,
            MiningSessionParity.LiveBaseline liveBaseline) {
        if (selectedTracker == null || liveBaseline == null) {
            throw new IllegalArgumentException(
                    "Selection and live baseline cannot be null");
        }
        if (selectedTracker != liveBaseline.selection()) {
            throw new IllegalArgumentException(
                    "Selection and live baseline must match");
        }
    }

    private static void requireTimestamp(long timestamp) {
        if (timestamp < 0L) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be negative");
        }
    }

    private static void requireNonNegative(long value, String label) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    label + " cannot be negative");
        }
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
        return sanitized.length() <= MAX_DIAGNOSTIC_IDENTIFIER_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_DIAGNOSTIC_IDENTIFIER_LENGTH);
    }

    enum EventDisposition {
        APPENDED,
        DUPLICATE,
        REJECTED,
        IDENTITY_CONFLICT,
        INACTIVE
    }
}
