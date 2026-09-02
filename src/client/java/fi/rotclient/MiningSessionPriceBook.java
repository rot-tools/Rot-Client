package fi.rotclient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Immutable Bazaar instant-sell price snapshot for shadow valuation.
 *
 * <h2>Price ingress policy</h2>
 * External Hypixel Bazaar quotes are not guaranteed to have a short decimal
 * scale ({@code quick_status} values are weighted averages). Valid positive
 * quotes are normalized with {@link RoundingMode#HALF_UP} to at most
 * {@link #MAX_UNIT_PRICE_SCALE} fractional digits. Trailing zeros are stripped
 * before scale decisions. Quotes that are non-positive, non-finite, above
 * {@link #MAX_UNIT_PRICE}, or would round to zero are rejected and remain
 * unresolved; zero valuation is never fabricated.
 */
final class MiningSessionPriceBook {
    static final long STALE_THRESHOLD_MILLIS = 60_000L;
    static final int MAX_UNIT_PRICE_SCALE = 8;
    static final BigDecimal MAX_UNIT_PRICE =
            new BigDecimal("1000000000000");
    static final RoundingMode UNIT_PRICE_ROUNDING = RoundingMode.HALF_UP;

    enum RejectReason {
        NULL,
        NON_POSITIVE,
        NON_FINITE,
        EXCEEDS_MAXIMUM,
        ROUNDS_TO_NON_POSITIVE,
        BLANK_PRODUCT_ID
    }

    record NormalizeResult(
            Optional<BigDecimal> normalized,
            Optional<RejectReason> rejectReason,
            boolean roundedToMaxScale) {
        static NormalizeResult accepted(BigDecimal value, boolean rounded) {
            return new NormalizeResult(
                    Optional.of(value),
                    Optional.empty(),
                    rounded);
        }

        static NormalizeResult rejected(RejectReason reason) {
            return new NormalizeResult(
                    Optional.empty(),
                    Optional.of(reason),
                    false);
        }

        boolean accepted() {
            return normalized.isPresent();
        }
    }

    private final boolean available;
    private final boolean invalidTimestamp;
    private final MiningSessionPriceResolution.PriceSource source;
    private final long observedAtMillis;
    private final Map<String, BigDecimal> unitPricesByProductId;

    private MiningSessionPriceBook(
            boolean available,
            boolean invalidTimestamp,
            MiningSessionPriceResolution.PriceSource source,
            long observedAtMillis,
            Map<String, BigDecimal> unitPricesByProductId) {
        this.available = available;
        this.invalidTimestamp = invalidTimestamp;
        this.source = source;
        this.observedAtMillis = observedAtMillis;
        this.unitPricesByProductId = unitPricesByProductId;
    }

    static MiningSessionPriceBook available(
            long observedAtMillis,
            Map<String, BigDecimal> unitPricesByProductId) {
        if (observedAtMillis < 0L) {
            throw new IllegalArgumentException(
                    "Observed timestamp cannot be negative");
        }
        if (unitPricesByProductId == null) {
            throw new IllegalArgumentException(
                    "Unit prices cannot be null");
        }

        Map<String, BigDecimal> validated = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry
                : unitPricesByProductId.entrySet()) {
            String productId = entry.getKey();
            if (productId == null || productId.isBlank()) {
                continue;
            }
            NormalizeResult result = normalizeUnitPrice(entry.getValue());
            if (result.accepted()) {
                validated.put(productId, result.normalized().orElseThrow());
            }
        }

        if (validated.isEmpty()) {
            return unavailable();
        }

        return new MiningSessionPriceBook(
                true,
                false,
                MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL,
                observedAtMillis,
                Collections.unmodifiableMap(validated));
    }

    static MiningSessionPriceBook unavailable() {
        return new MiningSessionPriceBook(
                false,
                false,
                MiningSessionPriceResolution.PriceSource.NONE,
                -1L,
                Map.of());
    }

    static MiningSessionPriceBook unavailableInvalidTimestamp() {
        return new MiningSessionPriceBook(
                false,
                true,
                MiningSessionPriceResolution.PriceSource.NONE,
                -1L,
                Map.of());
    }

    boolean isAvailable() {
        return available;
    }

    boolean invalidTimestamp() {
        return invalidTimestamp;
    }

    MiningSessionPriceResolution.PriceSource source() {
        return source;
    }

    long observedAtMillis() {
        return observedAtMillis;
    }

    Map<String, BigDecimal> unitPricesByProductId() {
        return unitPricesByProductId;
    }

    Optional<BigDecimal> unitPrice(String productId) {
        if (!available || productId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(unitPricesByProductId.get(productId));
    }

    static boolean hasValidTimestampOrdering(
            long nowMillis,
            long observedAtMillis) {
        return nowMillis >= 0L
                && observedAtMillis >= 0L
                && nowMillis >= observedAtMillis;
    }

    boolean isStale(long nowMillis) {
        if (!available) {
            return false;
        }
        if (!hasValidTimestampOrdering(nowMillis, observedAtMillis)) {
            return true;
        }
        long ageMillis = nowMillis - observedAtMillis;
        return ageMillis > STALE_THRESHOLD_MILLIS;
    }

    OptionalLong quoteAgeMillis(long nowMillis) {
        if (!available
                || !hasValidTimestampOrdering(nowMillis, observedAtMillis)) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(nowMillis - observedAtMillis);
    }

    /**
     * Converts a finite positive double quote into a normalized unit price.
     * Uses {@link Double#toString(double)} rather than binary double bits so
     * decimal-looking Bazaar averages keep stable digit sequences.
     */
    static NormalizeResult normalizeUnitPriceFromDouble(double raw) {
        if (!Double.isFinite(raw)) {
            return NormalizeResult.rejected(RejectReason.NON_FINITE);
        }
        if (raw <= 0D) {
            return NormalizeResult.rejected(RejectReason.NON_POSITIVE);
        }
        try {
            return normalizeUnitPrice(new BigDecimal(Double.toString(raw)));
        } catch (NumberFormatException | ArithmeticException failure) {
            return NormalizeResult.rejected(RejectReason.NON_FINITE);
        }
    }

    /**
     * Canonical unit-price ingress. Never throws for high fractional scale —
     * over-scale values are rounded with {@link #UNIT_PRICE_ROUNDING}.
     */
    static NormalizeResult normalizeUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            return NormalizeResult.rejected(RejectReason.NULL);
        }
        if (unitPrice.signum() <= 0) {
            return NormalizeResult.rejected(RejectReason.NON_POSITIVE);
        }

        BigDecimal working = unitPrice.stripTrailingZeros();
        if (working.compareTo(MAX_UNIT_PRICE) > 0) {
            return NormalizeResult.rejected(RejectReason.EXCEEDS_MAXIMUM);
        }
        boolean rounded = false;
        if (working.scale() > MAX_UNIT_PRICE_SCALE) {
            BigDecimal candidate = working
                    .setScale(MAX_UNIT_PRICE_SCALE, UNIT_PRICE_ROUNDING)
                    .stripTrailingZeros();
            if (candidate.signum() <= 0) {
                return NormalizeResult.rejected(
                        RejectReason.ROUNDS_TO_NON_POSITIVE);
            }
            working = candidate;
            rounded = true;
        }
        return NormalizeResult.accepted(working, rounded);
    }

    /**
     * Strict helper for callers that already hold a normalized price or need
     * fail-fast on non-positive / over-max values. High scale is normalized,
     * not rejected.
     */
    static BigDecimal validateUnitPrice(BigDecimal unitPrice) {
        NormalizeResult result = normalizeUnitPrice(unitPrice);
        if (result.accepted()) {
            return result.normalized().orElseThrow();
        }
        RejectReason reason = result.rejectReason().orElse(RejectReason.NULL);
        throw new IllegalArgumentException(switch (reason) {
            case NULL -> "Unit price cannot be null";
            case NON_POSITIVE, ROUNDS_TO_NON_POSITIVE ->
                    "Unit price must be positive";
            case NON_FINITE -> "Unit price must be finite";
            case EXCEEDS_MAXIMUM -> "Unit price exceeds maximum";
            case BLANK_PRODUCT_ID -> "Product ID cannot be null or blank";
        });
    }

    static BigDecimal entryValue(BigDecimal unitPrice, long quantity) {
        if (quantity <= 0L) {
            throw new IllegalArgumentException(
                    "Quantity must be positive");
        }
        BigDecimal validated = validateUnitPrice(unitPrice);
        return validated.multiply(BigDecimal.valueOf(quantity));
    }

    static String formatCoinAmount(BigDecimal value) {
        if (value == null) {
            return "unavailable";
        }
        return value
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    static String sanitizeNumericForDiag(BigDecimal value) {
        if (value == null) {
            return "null";
        }
        String plain = value.stripTrailingZeros().toPlainString();
        if (plain.length() > 32) {
            return plain.substring(0, 32) + "...";
        }
        return plain;
    }

    static String sanitizeNumericForDiag(double value) {
        if (!Double.isFinite(value)) {
            return Double.isNaN(value) ? "NaN" : "Infinity";
        }
        return sanitizeNumericForDiag(new BigDecimal(Double.toString(value)));
    }

    static String rejectReasonLabel(RejectReason reason) {
        return reason == null
                ? "UNKNOWN"
                : reason.name().toLowerCase(Locale.ROOT);
    }
}
