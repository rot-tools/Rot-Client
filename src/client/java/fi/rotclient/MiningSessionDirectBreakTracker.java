package fi.rotclient;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/** Explicit-time, bounded direct-break correlation state for the shadow ledger. */
final class MiningSessionDirectBreakTracker {
    static final long MATERIAL_WINDOW_MILLIS = 10_000L;
    static final long GEMSTONE_SACK_WINDOW_MILLIS =
            GemstoneMiningBatchTracker.DEFAULT_BREAK_WINDOW_MILLIS;
    static final long GEMSTONE_PRISTINE_WINDOW_MILLIS =
            GemstoneMiningCorrelationGate.DEFAULT_WINDOW_MILLIS;
    /**
     * Upper bound for retaining break evidence so Hypixel Sack batches that
     * report {@code Last Ns} (often 20–30s) can still correlate. Correlation
     * itself still requires the per-request effective window.
     */
    static final long MAX_EVIDENCE_RETENTION_MILLIS =
            SackBatchInterval.MAX_COVERED_MILLIS;
    static final int MAX_PENDING_CONTEXTS = 128;

    private final long materialWindowMillis;
    private final long gemstoneSackWindowMillis;
    private final long gemstonePristineWindowMillis;
    private final long evidenceRetentionMillis;
    private final int maximumContexts;
    private final Deque<PendingContext> contexts = new ArrayDeque<>();
    private long contextSequence;

    MiningSessionDirectBreakTracker() {
        this(
                MATERIAL_WINDOW_MILLIS,
                GEMSTONE_SACK_WINDOW_MILLIS,
                GEMSTONE_PRISTINE_WINDOW_MILLIS,
                MAX_PENDING_CONTEXTS,
                0L);
    }

    MiningSessionDirectBreakTracker(
            long materialWindowMillis,
            long gemstoneSackWindowMillis,
            long gemstonePristineWindowMillis,
            int maximumContexts,
            long initialContextSequence) {
        if (materialWindowMillis <= 0L
                || gemstoneSackWindowMillis <= 0L
                || gemstonePristineWindowMillis <= 0L) {
            throw new IllegalArgumentException(
                    "Correlation windows must be positive");
        }
        if (maximumContexts <= 0 || initialContextSequence < 0L) {
            throw new IllegalArgumentException(
                    "Tracker bounds and sequence must be non-negative");
        }
        this.materialWindowMillis = materialWindowMillis;
        this.gemstoneSackWindowMillis = gemstoneSackWindowMillis;
        this.gemstonePristineWindowMillis = gemstonePristineWindowMillis;
        this.evidenceRetentionMillis = Math.max(
                MAX_EVIDENCE_RETENTION_MILLIS,
                Math.max(
                        materialWindowMillis,
                        Math.max(
                                gemstoneSackWindowMillis,
                                gemstonePristineWindowMillis)));
        this.maximumContexts = maximumContexts;
        this.contextSequence = initialContextSequence;
    }

    String recordConfirmedBreak(
            FamilyKey family,
            long selectionEpoch,
            long observedAtMillis) {
        requireFamily(family);
        requireEpoch(selectionEpoch);
        requireTimestamp(observedAtMillis);

        long nextSequence = Math.addExact(contextSequence, 1L);
        PendingContext next = new PendingContext(
                "context-" + nextSequence,
                family,
                selectionEpoch,
                observedAtMillis,
                channelsFor(family));

        expire(observedAtMillis);

        if (contexts.size() == maximumContexts) {
            contexts.removeFirst();
        }
        contexts.addLast(next);
        contextSequence = nextSequence;
        return next.contextId;
    }

    Optional<Correlation> findCorrelation(
            FamilyKey family,
            EvidenceChannel channel,
            long quantity,
            boolean supportedSource,
            boolean duplicateDelivery,
            long selectionEpoch,
            long observedAtMillis) {
        return findCorrelation(
                family,
                channel,
                quantity,
                supportedSource,
                duplicateDelivery,
                selectionEpoch,
                observedAtMillis,
                0L);
    }

    /**
     * @param coveredBatchMillis Sack batch covered duration (e.g. Last 24s).
     *        When &gt; 0, evidence within that interval remains eligible even if
     *        older than the default channel window.
     */
    Optional<Correlation> findCorrelation(
            FamilyKey family,
            EvidenceChannel channel,
            long quantity,
            boolean supportedSource,
            boolean duplicateDelivery,
            long selectionEpoch,
            long observedAtMillis,
            long coveredBatchMillis) {
        requireFamily(family);
        requireChannel(channel);
        requireEpoch(selectionEpoch);
        requireTimestamp(observedAtMillis);

        expire(observedAtMillis);
        if (quantity <= 0L || !supportedSource || duplicateDelivery) {
            return Optional.empty();
        }

        long correlationWindow = effectiveCorrelationWindow(
                channel, coveredBatchMillis);

        Iterator<PendingContext> iterator = contexts.descendingIterator();
        while (iterator.hasNext()) {
            PendingContext context = iterator.next();
            if (context.family.equals(family)
                    && context.selectionEpoch == selectionEpoch
                    && context.availableChannels.contains(channel)
                    && age(observedAtMillis, context.observedAtMillis)
                    <= correlationWindow) {
                return Optional.of(new Correlation(
                        context.contextId,
                        family,
                        channel,
                        selectionEpoch,
                        context.observedAtMillis));
            }
        }
        return Optional.empty();
    }

    boolean consume(Correlation correlation, long observedAtMillis) {
        return consume(correlation, observedAtMillis, 0L);
    }

    boolean consume(
            Correlation correlation,
            long observedAtMillis,
            long coveredBatchMillis) {
        if (correlation == null) {
            return false;
        }
        requireTimestamp(observedAtMillis);
        expire(observedAtMillis);

        long correlationWindow = effectiveCorrelationWindow(
                correlation.channel(), coveredBatchMillis);

        for (PendingContext context : contexts) {
            if (context.contextId.equals(correlation.contextId())
                    && context.family.equals(correlation.family())
                    && context.selectionEpoch == correlation.selectionEpoch()
                    && context.availableChannels.contains(correlation.channel())
                    && age(observedAtMillis, context.observedAtMillis)
                    <= correlationWindow) {
                context.availableChannels.remove(correlation.channel());
                removeCompleted();
                return true;
            }
        }
        return false;
    }

    int pendingContextCount() {
        return contexts.size();
    }

    List<ContextSnapshot> snapshots(long observedAtMillis) {
        requireTimestamp(observedAtMillis);
        expire(observedAtMillis);
        List<ContextSnapshot> result = new ArrayList<>(contexts.size());
        for (PendingContext context : contexts) {
            result.add(new ContextSnapshot(
                    context.contextId,
                    context.family,
                    context.selectionEpoch,
                    context.observedAtMillis,
                    context.availableChannels));
        }
        return List.copyOf(result);
    }

    void reset() {
        contexts.clear();
    }

    void onTargetTransition() {
        reset();
    }

    void onTrackerDisabled() {
        reset();
    }

    void onWorldTransition() {
        reset();
    }

    private void expire(long observedAtMillis) {
        contexts.removeIf(context ->
                age(observedAtMillis, context.observedAtMillis)
                        > evidenceRetentionMillis);
    }

    private void removeCompleted() {
        contexts.removeIf(context -> context.availableChannels.isEmpty());
    }

    private long effectiveCorrelationWindow(
            EvidenceChannel channel,
            long coveredBatchMillis) {
        long base = windowFor(channel);
        if (coveredBatchMillis <= 0L) {
            return base;
        }
        return Math.min(
                evidenceRetentionMillis,
                Math.max(base, SackBatchInterval.effectiveWindowMillis(
                        coveredBatchMillis)));
    }

    private long windowFor(EvidenceChannel channel) {
        return switch (channel) {
            case MATERIAL_QUANTITY -> materialWindowMillis;
            case SACK_QUANTITY -> gemstoneSackWindowMillis;
            case PRISTINE_QUANTITY -> gemstonePristineWindowMillis;
        };
    }

    private static EnumSet<EvidenceChannel> channelsFor(FamilyKey family) {
        if (family.material() != null) {
            return EnumSet.of(EvidenceChannel.MATERIAL_QUANTITY);
        }
        return EnumSet.of(
                EvidenceChannel.SACK_QUANTITY,
                EvidenceChannel.PRISTINE_QUANTITY);
    }

    private static long age(long now, long then) {
        return now >= then ? now - then : 0L;
    }

    private static void requireFamily(FamilyKey family) {
        if (family == null) {
            throw new IllegalArgumentException("Mining family cannot be null");
        }
    }

    private static void requireChannel(EvidenceChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Evidence channel cannot be null");
        }
    }

    private static void requireEpoch(long selectionEpoch) {
        if (selectionEpoch < 0L) {
            throw new IllegalArgumentException("Selection epoch cannot be negative");
        }
    }

    private static void requireTimestamp(long observedAtMillis) {
        if (observedAtMillis < 0L) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }
    }

    enum EvidenceChannel {
        SACK_QUANTITY,
        PRISTINE_QUANTITY,
        MATERIAL_QUANTITY
    }

    record FamilyKey(
            TrackedMaterial material,
            GemstoneType gemstone) {
        FamilyKey {
            if ((material == null) == (gemstone == null)) {
                throw new IllegalArgumentException(
                        "Mining family must identify one material or gemstone");
            }
        }

        static FamilyKey material(TrackedMaterial material) {
            if (material == null) {
                throw new IllegalArgumentException("Material cannot be null");
            }
            return new FamilyKey(material, null);
        }

        static FamilyKey gemstone(GemstoneType gemstone) {
            if (gemstone == null) {
                throw new IllegalArgumentException("Gemstone cannot be null");
            }
            return new FamilyKey(null, gemstone);
        }

        String id() {
            return material != null
                    ? "MATERIAL:" + material.id()
                    : "GEMSTONE:" + gemstone.id();
        }
    }

    record Correlation(
            String contextId,
            FamilyKey family,
            EvidenceChannel channel,
            long selectionEpoch,
            long directBreakAtMillis) {
    }

    record ContextSnapshot(
            String contextId,
            FamilyKey family,
            long selectionEpoch,
            long observedAtMillis,
            java.util.Set<EvidenceChannel> availableChannels) {
        ContextSnapshot {
            availableChannels = java.util.Set.copyOf(availableChannels);
        }
    }

    private static final class PendingContext {
        private final String contextId;
        private final FamilyKey family;
        private final long selectionEpoch;
        private final long observedAtMillis;
        private final EnumSet<EvidenceChannel> availableChannels;

        private PendingContext(
                String contextId,
                FamilyKey family,
                long selectionEpoch,
                long observedAtMillis,
                EnumSet<EvidenceChannel> availableChannels) {
            this.contextId = contextId;
            this.family = family;
            this.selectionEpoch = selectionEpoch;
            this.observedAtMillis = observedAtMillis;
            this.availableChannels = EnumSet.copyOf(availableChannels);
        }
    }
}
