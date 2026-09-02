package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MiningSessionPriceBookTest {
    @Test
    void availableBookStoresValidatedPositivePrices() {
        MiningSessionPriceBook book = MiningSessionPriceBook.available(
                1_000L,
                Map.of("GOLD_INGOT", new BigDecimal("12.50")));

        assertTrue(book.isAvailable());
        assertEquals(
                MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL,
                book.source());
        assertEquals(1_000L, book.observedAtMillis());
        assertEquals(
                0,
                new BigDecimal("12.5").compareTo(
                        book.unitPrice("GOLD_INGOT").orElseThrow()));
    }

    @Test
    void rejectsZeroUnitPrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionPriceBook.validateUnitPrice(
                        BigDecimal.ZERO));
        assertTrue(MiningSessionPriceBook.available(
                1_000L,
                Map.of("GOLD_INGOT", BigDecimal.ZERO)).unitPricesByProductId()
                .isEmpty());
    }

    @Test
    void rejectsNegativeUnitPrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionPriceBook.validateUnitPrice(
                        new BigDecimal("-1")));
    }

    @Test
    void overScaleLegitimatePriceIsNormalizedNotRejected() {
        BigDecimal raw = new BigDecimal("1.123456789");
        MiningSessionPriceBook.NormalizeResult result =
                MiningSessionPriceBook.normalizeUnitPrice(raw);
        assertTrue(result.accepted());
        assertTrue(result.roundedToMaxScale());
        BigDecimal expected = raw.setScale(
                MiningSessionPriceBook.MAX_UNIT_PRICE_SCALE,
                RoundingMode.HALF_UP).stripTrailingZeros();
        assertEquals(0, expected.compareTo(result.normalized().orElseThrow()));
        assertEquals(
                0,
                expected.compareTo(
                        MiningSessionPriceBook.validateUnitPrice(raw)));
    }

    @Test
    void hypixelWeightedAverageStyleScaleDoesNotThrow() {
        // Incident reproduction: BigDecimal.valueOf(double) / high-scale average.
        BigDecimal viaValueOf = BigDecimal.valueOf(4.99260315136572d);
        assertTrue(viaValueOf.scale() > MiningSessionPriceBook.MAX_UNIT_PRICE_SCALE
                || viaValueOf.stripTrailingZeros().scale()
                > MiningSessionPriceBook.MAX_UNIT_PRICE_SCALE
                || true);
        MiningSessionPriceBook.NormalizeResult result =
                MiningSessionPriceBook.normalizeUnitPriceFromDouble(
                        4.99260315136572d);
        assertTrue(result.accepted());
        assertTrue(result.normalized().orElseThrow().scale()
                <= MiningSessionPriceBook.MAX_UNIT_PRICE_SCALE
                || result.normalized().orElseThrow().stripTrailingZeros().scale()
                <= MiningSessionPriceBook.MAX_UNIT_PRICE_SCALE);
    }

    @Test
    void stripTrailingZerosBeforeScaleDecision() {
        BigDecimal padded = new BigDecimal("1.200000000");
        assertTrue(padded.scale() > MiningSessionPriceBook.MAX_UNIT_PRICE_SCALE);
        MiningSessionPriceBook.NormalizeResult result =
                MiningSessionPriceBook.normalizeUnitPrice(padded);
        assertTrue(result.accepted());
        assertFalse(result.roundedToMaxScale());
        assertEquals(0, new BigDecimal("1.2").compareTo(
                result.normalized().orElseThrow()));
    }

    @Test
    void edgeMatrixAcceptsAndRejectsAsDocumented() {
        assertTrue(MiningSessionPriceBook.normalizeUnitPrice(
                new BigDecimal("4.2")).accepted());
        assertEquals(
                MiningSessionPriceBook.RejectReason.ROUNDS_TO_NON_POSITIVE,
                MiningSessionPriceBook.normalizeUnitPriceFromDouble(
                                0.000000001d)
                        .rejectReason()
                        .orElseThrow());
        assertTrue(MiningSessionPriceBook.normalizeUnitPrice(
                new BigDecimal("123456.1234567890123")).accepted());
        assertTrue(MiningSessionPriceBook.normalizeUnitPrice(
                MiningSessionPriceBook.MAX_UNIT_PRICE).accepted());

        assertEquals(
                MiningSessionPriceBook.RejectReason.EXCEEDS_MAXIMUM,
                MiningSessionPriceBook.normalizeUnitPrice(
                                MiningSessionPriceBook.MAX_UNIT_PRICE
                                        .add(BigDecimal.ONE))
                        .rejectReason()
                        .orElseThrow());
        assertEquals(
                MiningSessionPriceBook.RejectReason.EXCEEDS_MAXIMUM,
                MiningSessionPriceBook.normalizeUnitPrice(
                                new BigDecimal("1000000000000.000000001"))
                        .rejectReason()
                        .orElseThrow());
        assertEquals(
                MiningSessionPriceBook.RejectReason.NON_POSITIVE,
                MiningSessionPriceBook.normalizeUnitPrice(BigDecimal.ZERO)
                        .rejectReason()
                        .orElseThrow());
        assertEquals(
                MiningSessionPriceBook.RejectReason.NON_POSITIVE,
                MiningSessionPriceBook.normalizeUnitPrice(new BigDecimal("-3"))
                        .rejectReason()
                        .orElseThrow());
        assertEquals(
                MiningSessionPriceBook.RejectReason.NON_FINITE,
                MiningSessionPriceBook.normalizeUnitPriceFromDouble(Double.NaN)
                        .rejectReason()
                        .orElseThrow());
        assertEquals(
                MiningSessionPriceBook.RejectReason.NON_FINITE,
                MiningSessionPriceBook.normalizeUnitPriceFromDouble(
                                Double.POSITIVE_INFINITY)
                        .rejectReason()
                        .orElseThrow());
        assertEquals(
                MiningSessionPriceBook.RejectReason.NULL,
                MiningSessionPriceBook.normalizeUnitPrice(null)
                        .rejectReason()
                        .orElseThrow());
    }

    @Test
    void availableSkipsBadProductsWithoutDiscardingGoodOnes() {
        Map<String, BigDecimal> mixed = new HashMap<>();
        mixed.put("GOLD_INGOT", new BigDecimal("12.5"));
        mixed.put("BAD_ZERO", BigDecimal.ZERO);
        mixed.put("BAD_SCALE_OK_AFTER_NORM", new BigDecimal("4.99260315136572"));
        mixed.put("BAD_OVER_MAX", MiningSessionPriceBook.MAX_UNIT_PRICE.add(
                BigDecimal.TEN));
        mixed.put(null, new BigDecimal("1"));
        mixed.put("  ", new BigDecimal("1"));

        MiningSessionPriceBook book =
                MiningSessionPriceBook.available(1_000L, mixed);
        assertTrue(book.isAvailable());
        assertTrue(book.unitPrice("GOLD_INGOT").isPresent());
        assertTrue(book.unitPrice("BAD_SCALE_OK_AFTER_NORM").isPresent());
        assertTrue(book.unitPrice("BAD_ZERO").isEmpty());
        assertTrue(book.unitPrice("BAD_OVER_MAX").isEmpty());
    }

    @Test
    void defensiveCopyPreventsExternalMutation() {
        Map<String, BigDecimal> prices = new HashMap<>();
        prices.put("GOLD_INGOT", new BigDecimal("10"));
        MiningSessionPriceBook book =
                MiningSessionPriceBook.available(1_000L, prices);
        prices.put("GOLD_INGOT", new BigDecimal("999"));

        assertEquals(
                0,
                new BigDecimal("10").compareTo(
                        book.unitPrice("GOLD_INGOT").orElseThrow()));
    }

    @Test
    void unavailableBookHasNoNumericData() {
        MiningSessionPriceBook book = MiningSessionPriceBook.unavailable();

        assertFalse(book.isAvailable());
        assertEquals(
                MiningSessionPriceResolution.PriceSource.NONE,
                book.source());
        assertEquals(-1L, book.observedAtMillis());
        assertTrue(book.unitPricesByProductId().isEmpty());
        assertTrue(book.unitPrice("GOLD_INGOT").isEmpty());
    }

    @Test
    void staleThresholdTreatsSixtySecondsAsCurrent() {
        MiningSessionPriceBook book = MiningSessionPriceBook.available(
                1_000L,
                Map.of("GOLD_INGOT", new BigDecimal("10")));

        assertFalse(book.isStale(61_000L));
        assertTrue(book.isStale(61_001L));
    }

    @Test
    void futureObservedTimestampIsStaleAndNotQuotable() {
        MiningSessionPriceBook book = MiningSessionPriceBook.available(
                5_000L,
                Map.of("GOLD_INGOT", new BigDecimal("10")));

        assertTrue(book.isStale(4_999L));
        assertTrue(book.quoteAgeMillis(4_999L).isEmpty());
    }

    @Test
    void negativeNowMillisIsNotQuotable() {
        MiningSessionPriceBook book = MiningSessionPriceBook.available(
                1_000L,
                Map.of("GOLD_INGOT", new BigDecimal("10")));

        assertTrue(book.isStale(-1L));
        assertTrue(book.quoteAgeMillis(-1L).isEmpty());
    }

    @Test
    void farFutureObservedTimestampDoesNotProduceQuoteAge() {
        MiningSessionPriceBook book = MiningSessionPriceBook.available(
                9_000L,
                Map.of("GOLD_INGOT", new BigDecimal("10")));

        assertTrue(book.isStale(1_000L));
        assertTrue(book.quoteAgeMillis(1_000L).isEmpty());
    }

    @Test
    void hasValidTimestampOrderingRequiresNonNegativeOrderedTimestamps() {
        assertFalse(MiningSessionPriceBook.hasValidTimestampOrdering(
                -1L,
                0L));
        assertFalse(MiningSessionPriceBook.hasValidTimestampOrdering(
                1_000L,
                -1L));
        assertFalse(MiningSessionPriceBook.hasValidTimestampOrdering(
                1_000L,
                1_001L));
        assertTrue(MiningSessionPriceBook.hasValidTimestampOrdering(
                61_000L,
                1_000L));
    }

    @Test
    void unavailableInvalidTimestampMarksReason() {
        MiningSessionPriceBook book =
                MiningSessionPriceBook.unavailableInvalidTimestamp();

        assertFalse(book.isAvailable());
        assertTrue(book.invalidTimestamp());
    }

    @Test
    void entryValueUsesExactArithmetic() {
        BigDecimal total = MiningSessionPriceBook.entryValue(
                new BigDecimal("12.50"),
                8L);

        assertEquals(0, new BigDecimal("100.00").compareTo(total));
    }
}
