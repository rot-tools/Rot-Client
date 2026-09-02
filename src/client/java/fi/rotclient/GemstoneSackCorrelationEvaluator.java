package fi.rotclient;

/**
 * Determines whether a parsed gemstone Sack entry can represent mining
 * output based on recent correlation signals.
 *
 * This class does not update tracker state.
 */
final class GemstoneSackCorrelationEvaluator {
    private GemstoneSackCorrelationEvaluator() {
    }

    static Decision evaluate(
            GemstoneTier tier,
            GemstoneMiningCorrelationGate.Correlation correlation) {
        if (tier == null) {
            throw new IllegalArgumentException(
                    "Gemstone tier cannot be null");
        }

        if (correlation == null) {
            throw new IllegalArgumentException(
                    "Correlation cannot be null");
        }

        if (tier == GemstoneTier.FLAWED
                && correlation.pristineObserved()) {
            return Decision.FLAWED_FROM_PRISTINE;
        }

        if (tier != GemstoneTier.ROUGH) {
            return Decision.UNSUPPORTED_TIER;
        }

        if (correlation.directBreaks() <= 0) {
            return Decision.NO_DIRECT_BREAK;
        }

        return Decision.ROUGH_AFTER_DIRECT_BREAK;
    }

    enum Decision {
        ROUGH_AFTER_DIRECT_BREAK(
                "rough-after-direct-break",
                true),

        FLAWED_FROM_PRISTINE(
                "flawed-from-pristine",
                false),

        NO_DIRECT_BREAK(
                "no-direct-break",
                false),

        UNSUPPORTED_TIER(
                "unsupported-tier",
                false);

        private final String id;
        private final boolean acceptedCandidate;

        Decision(
                String id,
                boolean acceptedCandidate) {
            this.id = id;
            this.acceptedCandidate =
                    acceptedCandidate;
        }

        String id() {
            return id;
        }

        boolean acceptedCandidate() {
            return acceptedCandidate;
        }
    }
}
