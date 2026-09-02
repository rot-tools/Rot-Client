package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Powder Chest reward validation and transient CHEST_LOOT / CURRENCY shadow
 * finalization. Accepted batches are also returned as immutable canonical
 * credit candidates; this observer never mutates Current Session itself.
 *
 * <p>Resource resolution intentionally reuses {@link MiningResourceCatalog}
 * canonical IDs for resources already represented by Rot Client. No external
 * reward table was imported. Unknown display names remain diagnostic-only.
 */
final class MiningSessionChestObserver {
    static final long SACK_RESEARCH_WINDOW_MILLIS = 30_000L;
    static final long REPLAY_WINDOW_MILLIS = 500L;

    private final PowderChestResourceResolver resolver;
    private final MiningSessionLedger ledger;
    private final MiningSessionShadowObserver.DiagnosticSink diagnostics;
    private boolean trackerEnabled;
    private TrackerSelection selection = TrackerSelection.GOLD;
    private long selectionEpoch;
    private String lastFinalizedContextId;
    private long lastFinalizedAtMillis = -1L;
    private String lastReplayFingerprint;
    private long lastReplayFinalizedAtMillis = -1L;

    MiningSessionChestObserver(
            MiningResourceCatalog catalog,
            MiningSessionLedger ledger,
            MiningSessionShadowObserver.DiagnosticSink diagnostics) {
        if (catalog == null || ledger == null || diagnostics == null) {
            throw new IllegalArgumentException(
                    "Chest observer dependencies cannot be null");
        }
        this.resolver = new PowderChestResourceResolver(catalog);
        this.ledger = ledger;
        this.diagnostics = diagnostics;
    }

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
        selectionEpoch = engineSelectionEpoch;
    }

    void resetState() {
        lastFinalizedContextId = null;
        lastFinalizedAtMillis = -1L;
        lastReplayFingerprint = null;
        lastReplayFinalizedAtMillis = -1L;
    }

    int finalizeContext(
            PowderChestContextTracker.FinalizedChestContext context,
            long observedAtMillis) {
        return finalizeContextDetailed(context, observedAtMillis)
                .shadowAppliedCount();
    }

    FinalizationResult finalizeContextDetailed(
            PowderChestContextTracker.FinalizedChestContext context,
            long observedAtMillis) {
        if (context == null) {
            return FinalizationResult.none();
        }

        String replayFingerprint = replayFingerprint(context);
        if (isReplayDuplicate(replayFingerprint, observedAtMillis)) {
            emitContextDuplicate(context, replayFingerprint, observedAtMillis);
            return FinalizationResult.none();
        }

        for (PowderChestContextTracker.BufferedCurrencyReward currency
                : context.currencies()) {
            emitCurrencyCandidate(context, currency);
        }

        List<PreparedCredit> preparedCredits = new ArrayList<>();
        boolean rejectedKnownCandidate = false;
        for (PowderChestContextTracker.BufferedItemReward item
                : context.items()) {
            PreparedCredit prepared = prepareItemCredit(
                    context,
                    item,
                    observedAtMillis);
            if (prepared == PreparedCredit.UNKNOWN) {
                continue;
            }
            if (prepared == PreparedCredit.REJECTED) {
                rejectedKnownCandidate = true;
                continue;
            }
            preparedCredits.add(prepared);
        }

        for (PowderChestContextTracker.BufferedCurrencyReward currency
                : context.currencies()) {
            PreparedCredit prepared = prepareCurrencyCredit(
                    context,
                    currency,
                    observedAtMillis);
            if (prepared == PreparedCredit.UNKNOWN) {
                continue;
            }
            if (prepared == PreparedCredit.REJECTED) {
                rejectedKnownCandidate = true;
                continue;
            }
            preparedCredits.add(prepared);
        }

        if (rejectedKnownCandidate) {
            emitBatchRejected(
                    context,
                    MiningSessionLedger.BatchAppendOutcome.REJECTED_INVALID,
                    preparedCredits.size());
            return FinalizationResult.none();
        }

        if (preparedCredits.isEmpty()) {
            rememberReplayFingerprint(replayFingerprint, observedAtMillis);
            lastFinalizedContextId = context.contextId();
            lastFinalizedAtMillis = observedAtMillis;
            return FinalizationResult.accepted(
                    context.contextId(), List.of(), 0);
        }

        List<MiningSessionLedger.BatchEntry> batchEntries =
                new ArrayList<>(preparedCredits.size());
        for (PreparedCredit prepared : preparedCredits) {
            batchEntries.add(new MiningSessionLedger.BatchEntry(
                    prepared.classification(),
                    MiningSessionPriceResolution.unresolved()));
        }

        MiningSessionLedger.BatchAppendResult batchResult =
                ledger.appendAllAtomically(batchEntries);
        if (!batchResult.applied()) {
            emitBatchRejected(
                    context,
                    batchResult.outcome(),
                    preparedCredits.size());
            return FinalizationResult.none();
        }

        lastFinalizedContextId = context.contextId();
        lastFinalizedAtMillis = observedAtMillis;
        rememberReplayFingerprint(replayFingerprint, observedAtMillis);

        for (PreparedCredit prepared : preparedCredits) {
            emitCredited(context, prepared);
        }
        List<CanonicalCredit> canonicalCredits = new ArrayList<>(
                preparedCredits.size());
        for (PreparedCredit prepared : preparedCredits) {
            canonicalCredits.add(new CanonicalCredit(
                    prepared.resource().resourceId(),
                    prepared.resource().displayName(),
                    prepared.quantity(),
                    prepared.classification().category()
                            == MiningSessionCategory.CURRENCY
                            ? SessionSourceType.CURRENCY
                            : SessionSourceType.CHEST));
        }
        return FinalizationResult.accepted(
                context.contextId(),
                canonicalCredits,
                batchResult.appliedCount());
    }

    void noteSackChangesForResearch(
            List<SackChangeParser.Change> changes,
            long observedAtMillis) {
        if (!diagnostics.isActive()
                || changes == null
                || changes.isEmpty()
                || lastFinalizedAtMillis < 0L
                || lastFinalizedContextId == null) {
            return;
        }
        if (observedAtMillis - lastFinalizedAtMillis
                > SACK_RESEARCH_WINDOW_MILLIS) {
            return;
        }

        for (SackChangeParser.Change change : changes) {
            if (change == null) {
                continue;
            }
            diagnostics.record(
                    "CHEST_SACK_RESEARCH_ONLY",
                    "contextId=" + lastFinalizedContextId
                            + " itemName=" + bounded(change.itemName())
                            + " delta=" + change.delta()
                            + " sacks=" + bounded(String.join(
                            ",",
                            change.sacks())));
        }
    }

    private PreparedCredit prepareItemCredit(
            PowderChestContextTracker.FinalizedChestContext context,
            PowderChestContextTracker.BufferedItemReward item,
            long observedAtMillis) {
        PowderChestResourceResolver.ResolvedReward resolved =
                resolver.resolveItem(item.displayName());
        String eventId = eventId(
                context.sessionEpoch(),
                context.contextSequence(),
                item.lineIndex());
        String diagnosticText = bounded(item.normalizedLine());

        if (!resolved.known()) {
            diagnostics.record(
                    "CHEST_REWARD_UNKNOWN",
                    "contextId=" + context.contextId()
                            + " eventId=" + eventId
                            + " displayName=" + bounded(item.displayName())
                            + " normalizedLine=" + diagnosticText
                            + " quantity=" + item.quantity());
            return PreparedCredit.UNKNOWN;
        }

        MiningSessionObservation observation = new MiningSessionObservation(
                resolved.resource(),
                item.quantity(),
                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                selection,
                context.selectionEpoch(),
                observedAtMillis,
                eventId,
                context.contextId(),
                "powder-chest-chat",
                diagnosticText);

        MiningSessionClassification classification = classifyChestReward(
                observation);

        if (classification.outcome()
                != MiningSessionClassification.Outcome.WOULD_CREDIT
                || classification.category()
                != MiningSessionCategory.CHEST_LOOT) {
            diagnostics.record(
                    "CHEST_REWARD_REJECTED",
                    "contextId=" + context.contextId()
                            + " eventId=" + eventId
                            + " resourceId="
                            + resolved.resource().resourceId()
                            + " reason="
                            + classification.reasonCode().name());
            return PreparedCredit.REJECTED;
        }

        return new PreparedCredit(
                eventId,
                item.lineIndex(),
                resolved.resource(),
                item.quantity(),
                classification);
    }

    private PreparedCredit prepareCurrencyCredit(
            PowderChestContextTracker.FinalizedChestContext context,
            PowderChestContextTracker.BufferedCurrencyReward currency,
            long observedAtMillis) {
        PowderChestResourceResolver.ResolvedReward resolved =
                resolver.resolveCurrency(currency.displayName());
        String eventId = eventId(
                context.sessionEpoch(),
                context.contextSequence(),
                currency.lineIndex());
        String diagnosticText = bounded(currency.normalizedLine());

        if (!resolved.known()) {
            diagnostics.record(
                    "CHEST_CURRENCY_UNKNOWN",
                    "contextId=" + context.contextId()
                            + " lineIndex=" + currency.lineIndex()
                            + " eventId=" + eventId
                            + " displayName=" + bounded(currency.displayName())
                            + " normalizedLine=" + diagnosticText
                            + " quantity=" + currency.quantity());
            return PreparedCredit.UNKNOWN;
        }

        MiningSessionObservation observation = new MiningSessionObservation(
                resolved.resource(),
                currency.quantity(),
                MiningSessionObservation.EvidenceType.CHEST_REWARD_MESSAGE,
                selection,
                context.selectionEpoch(),
                observedAtMillis,
                eventId,
                context.contextId(),
                "powder-chest-chat",
                diagnosticText);

        MiningSessionClassification classification = classifyChestReward(
                observation);

        if (classification.outcome()
                != MiningSessionClassification.Outcome.WOULD_CREDIT
                || classification.category()
                != MiningSessionCategory.CURRENCY) {
            diagnostics.record(
                    "CHEST_CURRENCY_REJECTED",
                    "contextId=" + context.contextId()
                            + " lineIndex=" + currency.lineIndex()
                            + " eventId=" + eventId
                            + " resourceId="
                            + resolved.resource().resourceId()
                            + " reason="
                            + classification.reasonCode().name());
            return PreparedCredit.REJECTED;
        }

        return new PreparedCredit(
                eventId,
                currency.lineIndex(),
                resolved.resource(),
                currency.quantity(),
                classification);
    }

    private MiningSessionClassification classifyChestReward(
            MiningSessionObservation observation) {
        MiningSessionClassifier.ClassificationContext classificationContext =
                new MiningSessionClassifier.ClassificationContext(
                        trackerEnabled,
                        true,
                        selection,
                        selectionEpoch,
                        true,
                        true,
                        false,
                        true,
                        false,
                        true);
        return MiningSessionClassifier.classify(
                observation,
                classificationContext);
    }

    private void emitCredited(
            PowderChestContextTracker.FinalizedChestContext context,
            PreparedCredit prepared) {
        if (!diagnostics.isActive()) {
            return;
        }
        MiningSessionCategory category =
                prepared.classification().category();
        if (category == MiningSessionCategory.CURRENCY) {
            diagnostics.record(
                    "CHEST_CURRENCY_CREDITED",
                    "contextId=" + context.contextId()
                            + " lineIndex=" + prepared.lineIndex()
                            + " eventId=" + prepared.eventId()
                            + " resourceId="
                            + prepared.resource().resourceId()
                            + " quantity=" + prepared.quantity()
                            + " category=CURRENCY");
            return;
        }
        diagnostics.record(
                "CHEST_REWARD_CREDITED",
                "contextId=" + context.contextId()
                        + " lineIndex=" + prepared.lineIndex()
                        + " eventId=" + prepared.eventId()
                        + " resourceId="
                        + prepared.resource().resourceId()
                        + " quantity=" + prepared.quantity()
                        + " category=CHEST_LOOT");
    }

    private void emitBatchRejected(
            PowderChestContextTracker.FinalizedChestContext context,
            MiningSessionLedger.BatchAppendOutcome outcome,
            int candidateCount) {
        if (!diagnostics.isActive()) {
            return;
        }
        diagnostics.record(
                "CHEST_REWARD_REJECTED",
                "contextId=" + context.contextId()
                        + " reason=BATCH_" + outcome.name()
                        + " candidateCount=" + candidateCount);
    }

    private void emitContextDuplicate(
            PowderChestContextTracker.FinalizedChestContext context,
            String fingerprint,
            long observedAtMillis) {
        if (!diagnostics.isActive()) {
            return;
        }
        diagnostics.record(
                "CHEST_CONTEXT_DUPLICATE",
                "contextId=" + context.contextId()
                        + " sessionEpoch=" + context.sessionEpoch()
                        + " selectionEpoch=" + context.selectionEpoch()
                        + " fingerprint=" + bounded(fingerprint)
                        + " observedAt=" + observedAtMillis);
    }

    private boolean isReplayDuplicate(
            String fingerprint,
            long observedAtMillis) {
        if (lastReplayFingerprint == null
                || lastReplayFinalizedAtMillis < 0L) {
            return false;
        }
        if (!fingerprint.equals(lastReplayFingerprint)) {
            return false;
        }
        return observedAtMillis - lastReplayFinalizedAtMillis
                <= REPLAY_WINDOW_MILLIS;
    }

    private void rememberReplayFingerprint(
            String fingerprint,
            long observedAtMillis) {
        lastReplayFingerprint = fingerprint;
        lastReplayFinalizedAtMillis = observedAtMillis;
    }

    String replayFingerprint(
            PowderChestContextTracker.FinalizedChestContext context) {
        StringBuilder builder = new StringBuilder();
        builder.append(context.sessionEpoch())
                .append('|')
                .append(context.selectionEpoch())
                .append('|');
        for (PowderChestContextTracker.BufferedItemReward item
                : context.items()) {
            PowderChestResourceResolver.ResolvedReward resolved =
                    resolver.resolveItem(item.displayName());
            String stableName = resolved.known()
                    ? resolved.resource().resourceId()
                    : "UNKNOWN:"
                    + PowderChestResourceResolver.normalizeDisplayKey(
                            item.displayName());
            builder.append("I:")
                    .append(stableName)
                    .append(':')
                    .append(item.quantity())
                    .append(';');
        }
        for (PowderChestContextTracker.BufferedCurrencyReward currency
                : context.currencies()) {
            PowderChestResourceResolver.ResolvedReward resolved =
                    resolver.resolveCurrency(currency.displayName());
            String stableName = resolved.known()
                    ? resolved.resource().resourceId()
                    : "UNKNOWN:"
                    + PowderChestResourceResolver.normalizeDisplayKey(
                            currency.displayName());
            builder.append("C:")
                    .append(stableName)
                    .append(':')
                    .append(currency.quantity())
                    .append(';');
        }
        return builder.toString();
    }

    private void emitCurrencyCandidate(
            PowderChestContextTracker.FinalizedChestContext context,
            PowderChestContextTracker.BufferedCurrencyReward currency) {
        if (!diagnostics.isActive()) {
            return;
        }
        diagnostics.record(
                "CHEST_CURRENCY_CANDIDATE",
                "contextId=" + context.contextId()
                        + " lineIndex=" + currency.lineIndex()
                        + " displayName=" + bounded(currency.displayName())
                        + " quantity=" + currency.quantity()
                        + " normalizedLine="
                        + bounded(currency.normalizedLine()));
    }

    static String eventId(
            long sessionEpoch,
            long contextSequence,
            int lineIndex) {
        return "chest:"
                + sessionEpoch
                + ":"
                + contextSequence
                + ":line:"
                + lineIndex;
    }

    private static String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "none";
        }
        String sanitized = value.trim().replaceAll("\\s+", " ");
        return sanitized.length() <= 64
                ? sanitized
                : sanitized.substring(0, 64);
    }

    private record PreparedCredit(
            String eventId,
            int lineIndex,
            MiningSessionResource resource,
            long quantity,
            MiningSessionClassification classification) {
        private static final PreparedCredit UNKNOWN = new PreparedCredit(
                null, -1, null, 0L, null);
        private static final PreparedCredit REJECTED = new PreparedCredit(
                null, -1, null, 0L, null);
    }

    record CanonicalCredit(
            String itemId,
            String displayName,
            long quantity,
            SessionSourceType sourceType) {
    }

    record FinalizationResult(
            boolean accepted,
            String contextId,
            List<CanonicalCredit> canonicalCredits,
            int shadowAppliedCount) {
        FinalizationResult {
            contextId = contextId == null ? "" : contextId;
            canonicalCredits = canonicalCredits == null
                    ? List.of()
                    : List.copyOf(canonicalCredits);
            shadowAppliedCount = Math.max(0, shadowAppliedCount);
        }

        static FinalizationResult none() {
            return new FinalizationResult(false, "", List.of(), 0);
        }

        static FinalizationResult accepted(
                String contextId,
                List<CanonicalCredit> credits,
                int shadowAppliedCount) {
            return new FinalizationResult(
                    true, contextId, credits, shadowAppliedCount);
        }
    }
}
