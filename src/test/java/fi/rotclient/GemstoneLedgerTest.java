package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GemstoneLedgerTest {
    @Test
    void startsWithEveryTierAtZero() {
        GemstoneLedger ledger = new GemstoneLedger();

        for (GemstoneTier tier : GemstoneTier.values()) {
            assertEquals(0L, ledger.quantity(tier));
        }

        assertEquals(0L, ledger.totalItemCount());
        assertEquals(0L, ledger.totalRoughEquivalent());
        assertTrue(ledger.isEmpty());
    }

    @Test
    void addsIndependentTierQuantities() {
        GemstoneLedger ledger = new GemstoneLedger();

        ledger.add(GemstoneTier.ROUGH, 125L);
        ledger.add(GemstoneTier.FLAWED, 4L);
        ledger.add(GemstoneTier.FINE, 2L);

        assertEquals(
                125L,
                ledger.quantity(GemstoneTier.ROUGH));

        assertEquals(
                4L,
                ledger.quantity(GemstoneTier.FLAWED));

        assertEquals(
                2L,
                ledger.quantity(GemstoneTier.FINE));

        assertEquals(
                0L,
                ledger.quantity(GemstoneTier.FLAWLESS));

        assertFalse(ledger.isEmpty());
    }

    @Test
    void calculatesTotalItemCount() {
        GemstoneLedger ledger = new GemstoneLedger();

        ledger.add(GemstoneTier.ROUGH, 100L);
        ledger.add(GemstoneTier.FLAWED, 5L);
        ledger.add(GemstoneTier.FINE, 2L);

        assertEquals(107L, ledger.totalItemCount());
    }

    @Test
    void calculatesRoughEquivalentAcrossAllTiers() {
        GemstoneLedger ledger = new GemstoneLedger();

        ledger.add(GemstoneTier.ROUGH, 100L);
        ledger.add(GemstoneTier.FLAWED, 2L);
        ledger.add(GemstoneTier.FINE, 1L);
        ledger.add(GemstoneTier.FLAWLESS, 1L);
        ledger.add(GemstoneTier.PERFECT, 1L);

        long expected =
                100L
                        + 2L * 80L
                        + 6_400L
                        + 512_000L
                        + 2_560_000L;

        assertEquals(
                expected,
                ledger.totalRoughEquivalent());
    }

    @Test
    void removesOnlyAvailableQuantity() {
        GemstoneLedger ledger = new GemstoneLedger();

        ledger.add(GemstoneTier.FLAWED, 5L);

        assertEquals(
                3L,
                ledger.removeUpTo(
                        GemstoneTier.FLAWED,
                        3L));

        assertEquals(
                2L,
                ledger.quantity(
                        GemstoneTier.FLAWED));

        assertEquals(
                2L,
                ledger.removeUpTo(
                        GemstoneTier.FLAWED,
                        10L));

        assertEquals(
                0L,
                ledger.quantity(
                        GemstoneTier.FLAWED));
    }

    @Test
    void clearResetsEveryTier() {
        GemstoneLedger ledger = new GemstoneLedger();

        for (GemstoneTier tier : GemstoneTier.values()) {
            ledger.add(tier, 3L);
        }

        ledger.clear();

        for (GemstoneTier tier : GemstoneTier.values()) {
            assertEquals(0L, ledger.quantity(tier));
        }

        assertTrue(ledger.isEmpty());
    }

    @Test
    void rejectsNegativeAmountsAndMissingTier() {
        GemstoneLedger ledger = new GemstoneLedger();

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.add(
                        GemstoneTier.ROUGH,
                        -1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.removeUpTo(
                        GemstoneTier.ROUGH,
                        -1L));

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.quantity(null));

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.add(null, 1L));
    }

    @Test
    void repairsNullQuantityMapAndNormalizesValues() throws Exception {
        GemstoneLedger ledger = new GemstoneLedger();

        Field quantitiesField =
                GemstoneLedger.class.getDeclaredField("quantities");
        quantitiesField.setAccessible(true);

        EnumMap<GemstoneTier, Long> values =
                new EnumMap<>(GemstoneTier.class);
        values.put(GemstoneTier.ROUGH, 5L);
        values.put(GemstoneTier.FLAWED, null);
        values.put(GemstoneTier.FINE, -2L);
        quantitiesField.set(ledger, values);

        ledger.normalize();

        assertEquals(5L, ledger.quantity(GemstoneTier.ROUGH));
        assertEquals(0L, ledger.quantity(GemstoneTier.FLAWED));
        assertEquals(0L, ledger.quantity(GemstoneTier.FINE));
        assertEquals(0L, ledger.quantity(GemstoneTier.FLAWLESS));
        assertEquals(0L, ledger.quantity(GemstoneTier.PERFECT));
    }

    @Test
    void detectsArithmeticOverflow() {
        GemstoneLedger ledger = new GemstoneLedger();

        ledger.add(
                GemstoneTier.PERFECT,
                Long.MAX_VALUE);

        assertThrows(
                ArithmeticException.class,
                ledger::totalRoughEquivalent);
    }
}
