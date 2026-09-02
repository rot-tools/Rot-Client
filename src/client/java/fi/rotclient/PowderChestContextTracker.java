package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bounded explicit-time state for one Powder Chest chat reward block. */
final class PowderChestContextTracker {
    static final long INACTIVITY_TIMEOUT_MILLIS = 15_000L;
    static final long ABSOLUTE_LIFETIME_MILLIS = 30_000L;

    private final MiningSessionShadowObserver.DiagnosticSink diagnostics;
    private long nextContextSequence;
    private long activeSessionEpoch = -1L;
    private long activeSelectionEpoch = -1L;
    private PendingContext pendingContext;

    PowderChestContextTracker(
            MiningSessionShadowObserver.DiagnosticSink diagnostics) {
        if (diagnostics == null) {
            throw new IllegalArgumentException(
                    "Diagnostic sink cannot be null");
        }
        this.diagnostics = diagnostics;
    }

    Optional<FinalizedChestContext> observeLine(
            PowderChestChatParser.ParsedLine parsedLine,
            long sessionEpoch,
            long selectionEpoch,
            long observedAtMillis) {
        requireTimestamp(observedAtMillis);
        if (parsedLine == null) {
            return Optional.empty();
        }

        expireIfNeeded(observedAtMillis);

        return switch (parsedLine) {
            case PowderChestChatParser.ParsedLine.StartMarker start ->
                    handleStart(
                            start,
                            sessionEpoch,
                            selectionEpoch,
                            observedAtMillis);
            case PowderChestChatParser.ParsedLine.EndMarker end ->
                    handleEnd(end, observedAtMillis);
            case PowderChestChatParser.ParsedLine.ItemReward item ->
                    handleItem(item, observedAtMillis);
            case PowderChestChatParser.ParsedLine.CurrencyReward currency ->
                    handleCurrency(currency, observedAtMillis);
            case PowderChestChatParser.ParsedLine.Ignored ignored ->
                    Optional.empty();
        };
    }

    void abandon(String reason, long observedAtMillis) {
        if (pendingContext == null) {
            return;
        }
        abandon(reason, pendingContext, observedAtMillis, null);
    }

    private void abandon(
            String reason,
            PendingContext context,
            long observedAtMillis,
            String extraDetails) {
        emitContextDiagnostic(
                reason,
                context,
                observedAtMillis,
                extraDetails);
        pendingContext = null;
    }

    void reset() {
        pendingContext = null;
        nextContextSequence = 0L;
        activeSessionEpoch = -1L;
        activeSelectionEpoch = -1L;
    }

    void synchronizeEpochs(
            long sessionEpoch,
            long selectionEpoch,
            long observedAtMillis) {
        if (pendingContext == null) {
            activeSessionEpoch = sessionEpoch;
            activeSelectionEpoch = selectionEpoch;
            return;
        }
        if (pendingContext.sessionEpoch() != sessionEpoch
                || pendingContext.selectionEpoch() != selectionEpoch) {
            abandon("CHEST_CONTEXT_INTERRUPTED", observedAtMillis);
        }
        activeSessionEpoch = sessionEpoch;
        activeSelectionEpoch = selectionEpoch;
    }

    void expireIfNeeded(long observedAtMillis) {
        if (pendingContext == null) {
            return;
        }
        long inactiveAge = observedAtMillis
                - pendingContext.lastLineAtMillis();
        if (inactiveAge > INACTIVITY_TIMEOUT_MILLIS) {
            abandon(
                    "CHEST_CONTEXT_TIMEOUT_INACTIVE",
                    pendingContext,
                    observedAtMillis,
                    "cause=inactive"
                            + " inactiveAgeMillis=" + inactiveAge
                            + " limitMillis="
                            + INACTIVITY_TIMEOUT_MILLIS);
            return;
        }

        long lifetimeAge = observedAtMillis
                - pendingContext.openedAtMillis();
        if (lifetimeAge > ABSOLUTE_LIFETIME_MILLIS) {
            abandon(
                    "CHEST_CONTEXT_TIMEOUT_ABSOLUTE",
                    pendingContext,
                    observedAtMillis,
                    "cause=absolute"
                            + " lifetimeAgeMillis=" + lifetimeAge
                            + " limitMillis="
                            + ABSOLUTE_LIFETIME_MILLIS);
        }
    }

    boolean hasOpenContext() {
        return pendingContext != null;
    }

    private Optional<FinalizedChestContext> handleStart(
            PowderChestChatParser.ParsedLine.StartMarker start,
            long sessionEpoch,
            long selectionEpoch,
            long observedAtMillis) {
        if (pendingContext != null) {
            abandon("CHEST_CONTEXT_REPLACED", observedAtMillis);
        }

        long contextSequence = Math.addExact(nextContextSequence, 1L);
        nextContextSequence = contextSequence;
        activeSessionEpoch = sessionEpoch;
        activeSelectionEpoch = selectionEpoch;
        pendingContext = new PendingContext(
                contextSequence,
                sessionEpoch,
                selectionEpoch,
                observedAtMillis,
                observedAtMillis,
                new ArrayList<>(),
                new ArrayList<>());

        emitContextDiagnostic(
                "CHEST_CONTEXT_STARTED",
                pendingContext,
                observedAtMillis);
        return Optional.empty();
    }

    private Optional<FinalizedChestContext> handleEnd(
            PowderChestChatParser.ParsedLine.EndMarker end,
            long observedAtMillis) {
        if (pendingContext == null) {
            return Optional.empty();
        }

        PendingContext context = pendingContext;
        pendingContext = null;
        FinalizedChestContext finalized = context.finalizeAt(observedAtMillis);
        emitContextDiagnostic(
                "CHEST_CONTEXT_FINALIZED",
                context,
                observedAtMillis);
        return Optional.of(finalized);
    }

    private Optional<FinalizedChestContext> handleItem(
            PowderChestChatParser.ParsedLine.ItemReward item,
            long observedAtMillis) {
        if (pendingContext == null) {
            return Optional.empty();
        }

        int lineIndex = pendingContext.items().size()
                + pendingContext.currencies().size();
        BufferedItemReward buffered = new BufferedItemReward(
                lineIndex,
                item.normalizedLine(),
                item.displayName(),
                item.quantity());
        pendingContext.items().add(buffered);
        pendingContext = pendingContext.withLastLineAt(observedAtMillis);
        emitBuffered("CHEST_REWARD_BUFFERED", buffered, pendingContext);
        return Optional.empty();
    }

    private Optional<FinalizedChestContext> handleCurrency(
            PowderChestChatParser.ParsedLine.CurrencyReward currency,
            long observedAtMillis) {
        if (pendingContext == null) {
            return Optional.empty();
        }

        int lineIndex = pendingContext.items().size()
                + pendingContext.currencies().size();
        BufferedCurrencyReward buffered = new BufferedCurrencyReward(
                lineIndex,
                currency.normalizedLine(),
                currency.displayName(),
                currency.quantity());
        pendingContext.currencies().add(buffered);
        pendingContext = pendingContext.withLastLineAt(observedAtMillis);
        return Optional.empty();
    }

    private void emitBuffered(
            String marker,
            BufferedItemReward buffered,
            PendingContext context) {
        if (!diagnostics.isActive()) {
            return;
        }
        diagnostics.record(
                marker,
                "contextId=" + context.contextId()
                        + " lineIndex=" + buffered.lineIndex()
                        + " displayName=" + bounded(buffered.displayName())
                        + " quantity=" + buffered.quantity());
    }

    private void emitContextDiagnostic(
            String marker,
            PendingContext context,
            long observedAtMillis) {
        emitContextDiagnostic(marker, context, observedAtMillis, null);
    }

    private void emitContextDiagnostic(
            String marker,
            PendingContext context,
            long observedAtMillis,
            String extraDetails) {
        if (!diagnostics.isActive()) {
            return;
        }
        String details = "contextId=" + context.contextId()
                + " sessionEpoch=" + context.sessionEpoch()
                + " selectionEpoch=" + context.selectionEpoch()
                + " itemCount=" + context.items().size()
                + " currencyCount=" + context.currencies().size()
                + " observedAt=" + observedAtMillis;
        if (extraDetails != null && !extraDetails.isBlank()) {
            details += " " + extraDetails;
        }
        diagnostics.record(marker, details);
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

    private static void requireTimestamp(long observedAtMillis) {
        if (observedAtMillis < 0L) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be negative");
        }
    }

    record BufferedItemReward(
            int lineIndex,
            String normalizedLine,
            String displayName,
            long quantity) {
    }

    record BufferedCurrencyReward(
            int lineIndex,
            String normalizedLine,
            String displayName,
            long quantity) {
    }

    record FinalizedChestContext(
            String contextId,
            long contextSequence,
            long sessionEpoch,
            long selectionEpoch,
            long openedAtMillis,
            long finalizedAtMillis,
            List<BufferedItemReward> items,
            List<BufferedCurrencyReward> currencies) {
        FinalizedChestContext {
            items = List.copyOf(items);
            currencies = List.copyOf(currencies);
        }
    }

    private static final class PendingContext {
        private final long contextSequence;
        private final long sessionEpoch;
        private final long selectionEpoch;
        private final long openedAtMillis;
        private final long lastLineAtMillis;
        private final List<BufferedItemReward> items;
        private final List<BufferedCurrencyReward> currencies;

        private PendingContext(
                long contextSequence,
                long sessionEpoch,
                long selectionEpoch,
                long openedAtMillis,
                long lastLineAtMillis,
                List<BufferedItemReward> items,
                List<BufferedCurrencyReward> currencies) {
            this.contextSequence = contextSequence;
            this.sessionEpoch = sessionEpoch;
            this.selectionEpoch = selectionEpoch;
            this.openedAtMillis = openedAtMillis;
            this.lastLineAtMillis = lastLineAtMillis;
            this.items = items;
            this.currencies = currencies;
        }

        private String contextId() {
            return "chest-" + sessionEpoch + "-" + contextSequence;
        }

        private long sessionEpoch() {
            return sessionEpoch;
        }

        private long selectionEpoch() {
            return selectionEpoch;
        }

        private long openedAtMillis() {
            return openedAtMillis;
        }

        private long lastLineAtMillis() {
            return lastLineAtMillis;
        }

        private List<BufferedItemReward> items() {
            return items;
        }

        private List<BufferedCurrencyReward> currencies() {
            return currencies;
        }

        private PendingContext withLastLineAt(long observedAtMillis) {
            return new PendingContext(
                    contextSequence,
                    sessionEpoch,
                    selectionEpoch,
                    openedAtMillis,
                    observedAtMillis,
                    items,
                    currencies);
        }

        private FinalizedChestContext finalizeAt(long observedAtMillis) {
            return new FinalizedChestContext(
                    contextId(),
                    contextSequence,
                    sessionEpoch,
                    selectionEpoch,
                    openedAtMillis,
                    observedAtMillis,
                    List.copyOf(items),
                    List.copyOf(currencies));
        }
    }
}
