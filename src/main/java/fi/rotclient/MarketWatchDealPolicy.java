package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

final class MarketWatchDealPolicy {
    private static final double EPSILON =
            0.000_001D;

    private static final double IQR_MULTIPLIER =
            1.5D;

    enum Confidence {
        VERY_LOW,
        LOW,
        MEDIUM,
        HIGH
    }

    record Analysis(
            boolean usable,
            boolean trigger,
            long candidatePrice,
            long referencePrice,
            long thresholdPrice,
            double requestedDiscountPercent,
            double requiredDiscountPercent,
            double actualDiscountPercent,
            long potentialSpreadCoins,
            Confidence confidence,
            int rawComparisonCount,
            int comparisonCount,
            int highOutliersRemoved,
            boolean percentRoute) {
    }

    private MarketWatchDealPolicy() {
    }

    /*
     * V4:
     *
     * The user's percentage is literal.
     *
     * Dynamic 20% means the CURRENT LOWEST BIN must be at least
     * 20% below the robust reference price.
     *
     * This policy does not decide which auction is the current lowest;
     * MarketWatchAlertEngine supplies only that candidate.
     */
    static Analysis analyzeAuction(
            long candidatePrice,
            List<Long> rawComparisonPrices,
            double requestedDiscountPercent) {

        double requested =
                cleanPercent(
                        requestedDiscountPercent);

        List<Long> prices =
                sanitize(
                        rawComparisonPrices);

        int rawCount =
                prices.size();

        if (candidatePrice <= 0L
                || prices.isEmpty()
                || requested <= 0.0D
                || requested >= 100.0D) {

            return unavailable(
                    candidatePrice,
                    requested,
                    rawCount);
        }

        /*
         * Only HIGH-side statistical outliers are removed.
         *
         * Cheap observations may indicate a genuinely falling market and
         * therefore stay in the data.
         */
        List<Long> filtered =
                removeHighOutliers(
                        prices);

        if (filtered.isEmpty()) {
            return unavailable(
                    candidatePrice,
                    requested,
                    rawCount);
        }

        long reference =
                robustReference(
                        filtered);

        if (reference <= 0L) {
            return unavailable(
                    candidatePrice,
                    requested,
                    rawCount);
        }

        long threshold =
                Math.max(
                        1L,
                        (long) Math.floor(
                                reference
                                        * (1.0D
                                        - requested
                                        / 100.0D)));

        double actualDiscount =
                ((reference
                        - candidatePrice)
                        / (double) reference)
                        * 100.0D;

        long potentialSpread =
                Math.max(
                        0L,
                        reference
                                - candidatePrice);

        boolean percentRoute =
                candidatePrice < reference
                        && candidatePrice <= threshold
                        && actualDiscount
                        + EPSILON
                        >= requested;

        return new Analysis(
                true,
                percentRoute,
                candidatePrice,
                reference,
                threshold,
                requested,
                requested,
                actualDiscount,
                potentialSpread,
                confidence(
                        filtered.size()),
                rawCount,
                filtered.size(),
                Math.max(
                        0,
                        rawCount
                                - filtered.size()),
                percentRoute);
    }

    private static Analysis unavailable(
            long candidatePrice,
            double requested,
            int rawCount) {

        return new Analysis(
                false,
                false,
                Math.max(
                        0L,
                        candidatePrice),
                0L,
                0L,
                requested,
                requested,
                0.0D,
                0L,
                Confidence.VERY_LOW,
                Math.max(
                        0,
                        rawCount),
                0,
                0,
                false);
    }

    /*
     * Sparse markets use lower median.
     *
     * Example:
     *
     * 125k, 500k
     *
     * reference = 125k
     *
     * rather than allowing the 500k seller to inflate fair value.
     *
     * Five or more observations use conventional midpoint median.
     */
    private static long robustReference(
            List<Long> sortedPrices) {

        if (sortedPrices == null
                || sortedPrices.isEmpty()) {

            return 0L;
        }

        int size =
                sortedPrices.size();

        if (size <= 4) {
            return sortedPrices.get(
                    (size - 1) / 2);
        }

        int middle =
                size / 2;

        if ((size & 1) == 1) {
            return sortedPrices.get(
                    middle);
        }

        long left =
                sortedPrices.get(
                        middle - 1);

        long right =
                sortedPrices.get(
                        middle);

        return left
                + (right - left)
                / 2L;
    }

    private static List<Long> removeHighOutliers(
            List<Long> sortedPrices) {

        if (sortedPrices == null
                || sortedPrices.size() < 5) {

            return sortedPrices == null
                    ? List.of()
                    : List.copyOf(
                            sortedPrices);
        }

        double q1 =
                percentile(
                        sortedPrices,
                        0.25D);

        double q3 =
                percentile(
                        sortedPrices,
                        0.75D);

        double iqr =
                Math.max(
                        0.0D,
                        q3 - q1);

        double upperFence =
                q3
                        + IQR_MULTIPLIER
                        * iqr;

        List<Long> filtered =
                new ArrayList<>();

        for (Long value : sortedPrices) {
            if (value != null
                    && value > 0L
                    && value <= upperFence) {

                filtered.add(
                        value);
            }
        }

        if (filtered.isEmpty()) {
            return List.copyOf(
                    sortedPrices);
        }

        return List.copyOf(
                filtered);
    }

    private static double percentile(
            List<Long> sortedPrices,
            double percentile) {

        if (sortedPrices == null
                || sortedPrices.isEmpty()) {

            return 0.0D;
        }

        if (sortedPrices.size() == 1) {
            return sortedPrices.getFirst();
        }

        double clamped =
                Math.max(
                        0.0D,
                        Math.min(
                                1.0D,
                                percentile));

        double position =
                (sortedPrices.size() - 1)
                        * clamped;

        int lowerIndex =
                (int) Math.floor(
                        position);

        int upperIndex =
                (int) Math.ceil(
                        position);

        long lower =
                sortedPrices.get(
                        lowerIndex);

        long upper =
                sortedPrices.get(
                        upperIndex);

        if (lowerIndex == upperIndex) {
            return lower;
        }

        double fraction =
                position
                        - lowerIndex;

        return lower
                + (upper - lower)
                * fraction;
    }

    private static List<Long> sanitize(
            List<Long> rawPrices) {

        if (rawPrices == null
                || rawPrices.isEmpty()) {

            return List.of();
        }

        List<Long> result =
                new ArrayList<>();

        for (Long value : rawPrices) {
            if (value != null
                    && value > 0L) {

                result.add(
                        value);
            }
        }

        result.sort(
                Long::compareTo);

        return List.copyOf(
                result);
    }

    private static Confidence confidence(
            int comparisons) {

        if (comparisons >= 8) {
            return Confidence.HIGH;
        }

        if (comparisons >= 5) {
            return Confidence.MEDIUM;
        }

        if (comparisons >= 2) {
            return Confidence.LOW;
        }

        return Confidence.VERY_LOW;
    }

    private static double cleanPercent(
            double value) {

        if (!Double.isFinite(value)
                || value <= 0.0D) {

            return 0.0D;
        }

        return Math.min(
                99.999D,
                value);
    }
}