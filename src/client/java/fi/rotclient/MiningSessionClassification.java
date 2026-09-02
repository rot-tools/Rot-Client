package fi.rotclient;

record MiningSessionClassification(
        MiningSessionObservation observation,
        Outcome outcome,
        MiningSessionCategory category,
        ReasonCode reasonCode) {
    MiningSessionClassification {
        if (observation == null) {
            throw new IllegalArgumentException(
                    "Observation cannot be null");
        }
        if (outcome == null) {
            throw new IllegalArgumentException(
                    "Outcome cannot be null");
        }
        if (reasonCode == null) {
            throw new IllegalArgumentException(
                    "Reason code cannot be null");
        }

        if (outcome == Outcome.WOULD_CREDIT) {
            if (category == null) {
                throw new IllegalArgumentException(
                        "Accepted classification requires a category");
            }
            if (reasonCode != acceptedReason(category)) {
                throw new IllegalArgumentException(
                        "Accepted category and reason code do not match");
            }
        } else {
            if (category != null) {
                throw new IllegalArgumentException(
                        "Non-accepted classification cannot carry a category");
            }
            if (isAcceptedReason(reasonCode)) {
                throw new IllegalArgumentException(
                        "Non-accepted classification cannot use an accepted reason");
            }
        }
    }

    static MiningSessionClassification wouldCredit(
            MiningSessionObservation observation,
            MiningSessionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException(
                    "Category cannot be null");
        }
        return new MiningSessionClassification(
                observation,
                Outcome.WOULD_CREDIT,
                category,
                acceptedReason(category));
    }

    static MiningSessionClassification rejected(
            MiningSessionObservation observation,
            ReasonCode reasonCode) {
        return new MiningSessionClassification(
                observation,
                Outcome.REJECTED,
                null,
                reasonCode);
    }

    static MiningSessionClassification unresolved(
            MiningSessionObservation observation,
            ReasonCode reasonCode) {
        return new MiningSessionClassification(
                observation,
                Outcome.UNRESOLVED,
                null,
                reasonCode);
    }

    private static ReasonCode acceptedReason(
            MiningSessionCategory category) {
        return switch (category) {
            case TARGET_MINED -> ReasonCode.ACCEPTED_TARGET;
            case OTHER_MINED -> ReasonCode.ACCEPTED_OTHER;
            case CHEST_LOOT -> ReasonCode.ACCEPTED_CHEST_LOOT;
            case CURRENCY -> ReasonCode.ACCEPTED_CURRENCY;
        };
    }

    private static boolean isAcceptedReason(
            ReasonCode reasonCode) {
        return reasonCode == ReasonCode.ACCEPTED_TARGET
                || reasonCode == ReasonCode.ACCEPTED_OTHER
                || reasonCode == ReasonCode.ACCEPTED_CHEST_LOOT
                || reasonCode == ReasonCode.ACCEPTED_CURRENCY;
    }

    enum Outcome {
        WOULD_CREDIT,
        REJECTED,
        UNRESOLVED
    }

    enum ReasonCode {
        ACCEPTED_TARGET,
        ACCEPTED_OTHER,
        ACCEPTED_CHEST_LOOT,
        ACCEPTED_CURRENCY,
        NON_POSITIVE_QUANTITY,
        TRACKER_DISABLED,
        INACTIVE_MINING_CONTEXT,
        SELECTION_EPOCH_MISMATCH,
        DUPLICATE_DELIVERY,
        UNKNOWN_RESOURCE,
        UNSUPPORTED_EVIDENCE,
        UNSUPPORTED_SOURCE,
        EXPLICIT_NON_MINING_SOURCE,
        TARGET_EXCLUDED_FROM_OTHERS,
        MISSING_EXACT_QUANTITY_EVIDENCE,
        MISSING_MINING_CORRELATION,
        MISSING_CHEST_CONTEXT,
        INSUFFICIENT_EVIDENCE
    }
}
