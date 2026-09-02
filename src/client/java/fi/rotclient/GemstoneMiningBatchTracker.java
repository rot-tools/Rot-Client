package fi.rotclient;

import java.util.EnumMap;

/**
 * Tracks consumable gemstone mining batches.
 *
 * Direct block-break signals can authorize one Rough Sack credit only.
 * Pristine messages are credited immediately as Flawed and are later matched
 * against Sack entries only as confirmation.
 *
 * This class has no Minecraft runtime dependencies and does not update any
 * persistent tracker state.
 */
final class GemstoneMiningBatchTracker {
    static final long DEFAULT_BREAK_WINDOW_MILLIS =
            60_000L;

    static final long DEFAULT_PRISTINE_WINDOW_MILLIS =
            120_000L;

    private final long breakWindowMillis;
    private final long pristineWindowMillis;

    private final EnumMap<GemstoneType, Batch> batches =
            new EnumMap<>(
                    GemstoneType.class);

    GemstoneMiningBatchTracker() {
        this(
                DEFAULT_BREAK_WINDOW_MILLIS,
                DEFAULT_PRISTINE_WINDOW_MILLIS);
    }

    GemstoneMiningBatchTracker(
            long breakWindowMillis,
            long pristineWindowMillis) {
        if (breakWindowMillis <= 0L) {
            throw new IllegalArgumentException(
                    "Break window must be positive");
        }

        if (pristineWindowMillis <= 0L) {
            throw new IllegalArgumentException(
                    "Pristine window must be positive");
        }

        this.breakWindowMillis =
                breakWindowMillis;

        this.pristineWindowMillis =
                pristineWindowMillis;
    }

    void recordDirectBreak(
            GemstoneType gemstone,
            long epochMillis) {
        requireGemstone(
                gemstone);

        requireTimestamp(
                epochMillis);

        Batch batch =
                batchFor(
                        gemstone);

        expire(
                batch,
                epochMillis);

        batch.directBreaks =
                Math.addExact(
                        batch.directBreaks,
                        1);

        batch.latestDirectBreakEpochMillis =
                epochMillis;
    }

    PristineCredit recordPristine(
            GemstoneType gemstone,
            long amount,
            long epochMillis) {
        requireGemstone(
                gemstone);

        requirePositiveAmount(
                amount);

        requireTimestamp(
                epochMillis);

        Batch batch =
                batchFor(
                        gemstone);

        expire(
                batch,
                epochMillis);

        batch.pendingPristineAmount =
                Math.addExact(
                        batch.pendingPristineAmount,
                        amount);

        batch.latestPristineEpochMillis =
                epochMillis;

        return new PristineCredit(
                gemstone,
                GemstoneTier.FLAWED,
                amount);
    }

    SackDecision evaluateSack(
            GemstoneType gemstone,
            GemstoneTier tier,
            long amount,
            long epochMillis) {
        requireGemstone(
                gemstone);

        if (tier == null) {
            throw new IllegalArgumentException(
                    "Gemstone tier cannot be null");
        }

        requirePositiveAmount(
                amount);

        requireTimestamp(
                epochMillis);

        Batch batch =
                batchFor(
                        gemstone);

        expire(
                batch,
                epochMillis);

        SackDecision decision;

        if (tier == GemstoneTier.ROUGH) {
            decision =
                    evaluateRough(
                            amount,
                            batch);
        }
        else if (tier == GemstoneTier.FLAWED) {
            decision =
                    evaluateFlawed(
                            amount,
                            batch);
        }
        else {
            decision =
                    new SackDecision(
                            Outcome.UNSUPPORTED_TIER,
                            false,
                            0L,
                            0,
                            0L,
                            batch.pendingPristineAmount);
        }

        removeEmptyBatch(
                gemstone,
                batch);

        return decision;
    }

    Snapshot snapshot(
            GemstoneType gemstone,
            long epochMillis) {
        requireGemstone(
                gemstone);

        requireTimestamp(
                epochMillis);

        Batch batch =
                batches.get(
                        gemstone);

        if (batch == null) {
            return Snapshot.EMPTY;
        }

        expire(
                batch,
                epochMillis);

        removeEmptyBatch(
                gemstone,
                batch);

        if (isEmpty(
                batch)) {
            return Snapshot.EMPTY;
        }

        return new Snapshot(
                batch.directBreaks,
                age(
                        batch.latestDirectBreakEpochMillis,
                        epochMillis),
                batch.pendingPristineAmount,
                age(
                        batch.latestPristineEpochMillis,
                        epochMillis));
    }

    void clearExpired(
            long epochMillis) {
        requireTimestamp(
                epochMillis);

        for (GemstoneType gemstone :
                GemstoneType.values()) {
            Batch batch =
                    batches.get(
                            gemstone);

            if (batch == null) {
                continue;
            }

            expire(
                    batch,
                    epochMillis);

            removeEmptyBatch(
                    gemstone,
                    batch);
        }
    }

    void reset() {
        batches.clear();
    }

    private SackDecision evaluateRough(
            long amount,
            Batch batch) {
        if (batch.directBreaks <= 0) {
            return new SackDecision(
                    Outcome.NO_DIRECT_BREAK,
                    false,
                    0L,
                    0,
                    0L,
                    batch.pendingPristineAmount);
        }

        int consumedDirectBreaks =
                batch.directBreaks;

        batch.directBreaks =
                0;

        batch.latestDirectBreakEpochMillis =
                -1L;

        return new SackDecision(
                Outcome.ROUGH_CREDIT,
                true,
                amount,
                consumedDirectBreaks,
                0L,
                batch.pendingPristineAmount);
    }

    private SackDecision evaluateFlawed(
            long amount,
            Batch batch) {
        if (batch.pendingPristineAmount <= 0L) {
            return new SackDecision(
                    Outcome.FLAWED_UNMATCHED,
                    false,
                    0L,
                    0,
                    0L,
                    0L);
        }

        long matchedAmount =
                Math.min(
                        amount,
                        batch.pendingPristineAmount);

        batch.pendingPristineAmount -=
                matchedAmount;

        if (batch.pendingPristineAmount == 0L) {
            batch.latestPristineEpochMillis =
                    -1L;
        }

        Outcome outcome =
                matchedAmount == amount
                        ? Outcome.FLAWED_CONFIRMED
                        : Outcome.FLAWED_PARTIAL_CONFIRMATION;

        return new SackDecision(
                outcome,
                false,
                0L,
                0,
                matchedAmount,
                batch.pendingPristineAmount);
    }

    private Batch batchFor(
            GemstoneType gemstone) {
        return batches.computeIfAbsent(
                gemstone,
                ignored -> new Batch());
    }

    private void expire(
            Batch batch,
            long epochMillis) {
        if (isExpired(
                batch.latestDirectBreakEpochMillis,
                epochMillis,
                breakWindowMillis)) {
            batch.directBreaks =
                    0;

            batch.latestDirectBreakEpochMillis =
                    -1L;
        }

        if (isExpired(
                batch.latestPristineEpochMillis,
                epochMillis,
                pristineWindowMillis)) {
            batch.pendingPristineAmount =
                    0L;

            batch.latestPristineEpochMillis =
                    -1L;
        }
    }

    private void removeEmptyBatch(
            GemstoneType gemstone,
            Batch batch) {
        if (isEmpty(
                batch)) {
            batches.remove(
                    gemstone);
        }
    }

    private static boolean isEmpty(
            Batch batch) {
        return batch.directBreaks <= 0
                && batch.pendingPristineAmount <= 0L;
    }

    private static boolean isExpired(
            long signalEpochMillis,
            long epochMillis,
            long windowMillis) {
        return signalEpochMillis >= 0L
                && epochMillis >= signalEpochMillis
                && epochMillis - signalEpochMillis
                > windowMillis;
    }

    private static long age(
            long signalEpochMillis,
            long epochMillis) {
        if (signalEpochMillis < 0L) {
            return -1L;
        }

        if (epochMillis < signalEpochMillis) {
            return 0L;
        }

        return epochMillis - signalEpochMillis;
    }

    private static void requireGemstone(
            GemstoneType gemstone) {
        if (gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone type cannot be null");
        }
    }

    private static void requirePositiveAmount(
            long amount) {
        if (amount <= 0L) {
            throw new IllegalArgumentException(
                    "Amount must be positive");
        }
    }

    private static void requireTimestamp(
            long epochMillis) {
        if (epochMillis < 0L) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be negative");
        }
    }

    enum Outcome {
        ROUGH_CREDIT(
                "rough-credit"),

        FLAWED_CONFIRMED(
                "flawed-confirmed"),

        FLAWED_PARTIAL_CONFIRMATION(
                "flawed-partial-confirmation"),

        FLAWED_UNMATCHED(
                "flawed-unmatched"),

        NO_DIRECT_BREAK(
                "no-direct-break"),

        UNSUPPORTED_TIER(
                "unsupported-tier");

        private final String id;

        Outcome(
                String id) {
            this.id =
                    id;
        }

        String id() {
            return id;
        }
    }

    record PristineCredit(
            GemstoneType gemstone,
            GemstoneTier tier,
            long amount) {
    }

    record SackDecision(
            Outcome outcome,
            boolean shouldCredit,
            long creditAmount,
            int consumedDirectBreaks,
            long matchedPristineAmount,
            long remainingPristineAmount) {
    }

    record Snapshot(
            int directBreaks,
            long latestDirectBreakAgeMillis,
            long pendingPristineAmount,
            long latestPristineAgeMillis) {
        private static final Snapshot EMPTY =
                new Snapshot(
                        0,
                        -1L,
                        0L,
                        -1L);
    }

    private static final class Batch {
        private int directBreaks;
        private long latestDirectBreakEpochMillis =
                -1L;

        private long pendingPristineAmount;
        private long latestPristineEpochMillis =
                -1L;
    }
}
