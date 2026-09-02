package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class MiningSessionPriceResolverTest {
    private static final MiningSessionResource ENCHANTED_HARD_STONE =
            MiningSessionResource.material(
                    TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                    TrackedMaterial.HARD_STONE.enchantedItemName(),
                    TrackedMaterial.HARD_STONE);
    private static final MiningSessionResource RAW_GOLD =
            MiningSessionResource.material(
                    TrackedMaterial.GOLD.rawBazaarId(),
                    TrackedMaterial.GOLD.rawItemName(),
                    TrackedMaterial.GOLD);
    private static final MiningSessionResource ENCHANTED_GOLD =
            MiningSessionResource.material(
                    TrackedMaterial.GOLD.enchantedBazaarId(),
                    TrackedMaterial.GOLD.enchantedItemName(),
                    TrackedMaterial.GOLD);
    private static final MiningSessionResource ROUGH_TOPAZ =
            MiningSessionResource.gemstone(
                    "ROUGH_TOPAZ_GEM",
                    "Rough Topaz Gemstone",
                    GemstoneType.TOPAZ,
                    GemstoneTier.ROUGH);
    private static final MiningSessionResource FLAWED_RUBY =
            MiningSessionResource.gemstone(
                    "FLAWED_RUBY_GEM",
                    "Flawed Ruby Gemstone",
                    GemstoneType.RUBY,
                    GemstoneTier.FLAWED);
    private static final MiningSessionResource FINE_RUBY =
            MiningSessionResource.gemstone(
                    "FINE_RUBY_GEM",
                    "Fine Ruby Gemstone",
                    GemstoneType.RUBY,
                    GemstoneTier.FINE);
    private static final MiningSessionResource GEMSTONE_POWDER =
            MiningSessionResource.currency(
                    "GEMSTONE_POWDER",
                    "Gemstone Powder");

    @Test
    void resolvesSingleSupportedEntry() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("4.25")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ENCHANTED_HARD_STONE,
                        10L,
                        MiningSessionCategory.OTHER_MINED)),
                book,
                2_000L);

        assertEquals(1, valuation.resolvedEntryCount());
        assertEquals(new BigDecimal("42.50"), valuation.resolvedItemValue());
        assertEquals(
                new BigDecimal("42.50"),
                valuation.valueByCategory(
                        MiningSessionCategory.OTHER_MINED));
    }

    @Test
    void mixedResolvedAndUnsupportedEntries() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("2"),
                        "ROUGH_TOPAZ_GEM",
                        new BigDecimal("1.5")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(
                        entry(
                                ENCHANTED_HARD_STONE,
                                5L,
                                MiningSessionCategory.OTHER_MINED),
                        entry(
                                ROUGH_TOPAZ,
                                24L,
                                MiningSessionCategory.CHEST_LOOT)),
                book,
                2_000L);

        assertEquals(2, valuation.resolvedEntryCount());
        assertEquals(0, valuation.unsupportedEntryCount());
        assertEquals(
                0,
                new BigDecimal("46").compareTo(valuation.resolvedItemValue()));
    }

    @Test
    void roughGemstoneResolves() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of("ROUGH_TOPAZ_GEM", new BigDecimal("2")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ROUGH_TOPAZ,
                        10L,
                        MiningSessionCategory.CHEST_LOOT)),
                book,
                2_000L);

        assertEquals(1, valuation.resolvedEntryCount());
        assertEquals(new BigDecimal("20"), valuation.resolvedItemValue());
        assertEquals(
                new BigDecimal("20"),
                valuation.valueByCategory(
                        MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void flawedGemstoneResolves() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of("FLAWED_RUBY_GEM", new BigDecimal("3")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        FLAWED_RUBY,
                        4L,
                        MiningSessionCategory.TARGET_MINED)),
                book,
                2_000L);

        assertEquals(1, valuation.resolvedEntryCount());
        assertEquals(
                new BigDecimal("12"),
                valuation.valueByCategory(
                        MiningSessionCategory.TARGET_MINED));
    }

    @Test
    void higherTierGemstoneRemainsUnsupported() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of("FINE_RUBY_GEM", new BigDecimal("99")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        FINE_RUBY,
                        1L,
                        MiningSessionCategory.OTHER_MINED)),
                book,
                2_000L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.unsupportedEntryCount());
    }

    @Test
    void staleGemstoneQuoteContributesNoValue() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of("ROUGH_TOPAZ_GEM", new BigDecimal("2")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ROUGH_TOPAZ,
                        10L,
                        MiningSessionCategory.CHEST_LOOT)),
                book,
                61_001L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.staleEntryCount());
        assertEquals(BigDecimal.ZERO, valuation.resolvedItemValue());
    }

    @Test
    void unavailableGemstoneQuoteContributesNoValue() {
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ROUGH_TOPAZ,
                        10L,
                        MiningSessionCategory.CHEST_LOOT)),
                MiningSessionPriceBook.unavailable(),
                2_000L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.unavailableEntryCount());
    }

    @Test
    void mixedMaterialAndGemstoneValuesSumExactly() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("1"),
                        "ROUGH_TOPAZ_GEM",
                        new BigDecimal("2")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(
                        entry(
                                RAW_GOLD,
                                3L,
                                MiningSessionCategory.TARGET_MINED),
                        entry(
                                ROUGH_TOPAZ,
                                5L,
                                MiningSessionCategory.CHEST_LOOT)),
                book,
                2_000L);

        assertEquals(new BigDecimal("13"), valuation.resolvedItemValue());
        assertEquals(
                new BigDecimal("3"),
                valuation.valueByCategory(
                        MiningSessionCategory.TARGET_MINED));
        assertEquals(
                new BigDecimal("10"),
                valuation.valueByCategory(
                        MiningSessionCategory.CHEST_LOOT));
    }

    @Test
    void supportedProductMissingFromBookCountsAsUnresolved() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of("GOLD_INGOT", new BigDecimal("10")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ENCHANTED_HARD_STONE,
                        5L,
                        MiningSessionCategory.OTHER_MINED)),
                book,
                2_000L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.unresolvedEntryCount());
        assertEquals(
                1,
                valuation.unresolvedEntryCount(
                        MiningSessionCategory.OTHER_MINED));
        assertEquals(
                0,
                valuation.unresolvedEntryCount(
                        MiningSessionCategory.TARGET_MINED));
        assertEquals(BigDecimal.ZERO, valuation.resolvedItemValue());
    }

    @Test
    void staleQuoteIsExcludedFromResolvedTotals() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("3")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ENCHANTED_HARD_STONE,
                        5L,
                        MiningSessionCategory.OTHER_MINED)),
                book,
                61_001L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.staleEntryCount());
        assertEquals(BigDecimal.ZERO, valuation.resolvedItemValue());
    }

    @Test
    void exactSixtySecondBoundaryRemainsCurrent() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("3")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ENCHANTED_HARD_STONE,
                        2L,
                        MiningSessionCategory.OTHER_MINED)),
                book,
                61_000L);

        assertEquals(1, valuation.resolvedEntryCount());
        assertEquals(0, valuation.staleEntryCount());
        assertEquals(new BigDecimal("6"), valuation.resolvedItemValue());
    }

    @Test
    void negativeNowMillisProducesUnavailableValuation() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("3")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ENCHANTED_HARD_STONE,
                        2L,
                        MiningSessionCategory.OTHER_MINED)),
                book,
                -1L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.unavailableEntryCount());
        assertEquals(BigDecimal.ZERO, valuation.resolvedItemValue());
    }

    @Test
    void invalidTimestampBookProducesUnavailableValuation() {
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ENCHANTED_HARD_STONE,
                        2L,
                        MiningSessionCategory.OTHER_MINED)),
                MiningSessionPriceBook.unavailableInvalidTimestamp(),
                2_000L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.unavailableEntryCount());
        assertEquals(BigDecimal.ZERO, valuation.resolvedItemValue());
    }

    @Test
    void unavailableBookMarksSupportedEntriesUnavailable() {
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(entry(
                        ENCHANTED_HARD_STONE,
                        5L,
                        MiningSessionCategory.OTHER_MINED)),
                MiningSessionPriceBook.unavailable(),
                2_000L);

        assertEquals(0, valuation.resolvedEntryCount());
        assertEquals(1, valuation.unavailableEntryCount());
        assertTrue(valuation.priceBookObservedAtMillis().isEmpty());
    }

    @Test
    void currencyIsExcludedFromValueCounts() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("2")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(
                        entry(
                                ENCHANTED_HARD_STONE,
                                5L,
                                MiningSessionCategory.OTHER_MINED),
                        currencyEntry(
                                GEMSTONE_POWDER,
                                291L,
                                MiningSessionCategory.CURRENCY)),
                book,
                2_000L);

        assertEquals(1, valuation.resolvedEntryCount());
        assertEquals(1, valuation.excludedCurrencyEntryCount());
        assertEquals(new BigDecimal("10"), valuation.resolvedItemValue());
    }

    @Test
    void categoryTotalsAggregateResolvedEntries() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("1"),
                        TrackedMaterial.GOLD.enchantedBazaarId(),
                        new BigDecimal("100")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(
                        entry(
                                RAW_GOLD,
                                3L,
                                MiningSessionCategory.TARGET_MINED),
                        entry(
                                ENCHANTED_GOLD,
                                2L,
                                MiningSessionCategory.OTHER_MINED)),
                book,
                2_000L);

        assertEquals(
                new BigDecimal("3"),
                valuation.valueByCategory(
                        MiningSessionCategory.TARGET_MINED));
        assertEquals(
                new BigDecimal("200"),
                valuation.valueByCategory(
                        MiningSessionCategory.OTHER_MINED));
        assertEquals(new BigDecimal("203"), valuation.resolvedItemValue());
    }

    @Test
    void multipleEntriesOfSameResourceAggregate() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("2")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(
                        entry(
                                ENCHANTED_HARD_STONE,
                                3L,
                                MiningSessionCategory.OTHER_MINED),
                        entry(
                                ENCHANTED_HARD_STONE,
                                4L,
                                MiningSessionCategory.OTHER_MINED)),
                book,
                2_000L);

        assertEquals(2, valuation.resolvedEntryCount());
        assertEquals(new BigDecimal("14"), valuation.resolvedItemValue());
    }

    @Test
    void rawAndEnchantedPricesRemainSeparate() {
        MiningSessionPriceBook book = book(
                1_000L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("5"),
                        TrackedMaterial.GOLD.enchantedBazaarId(),
                        new BigDecimal("500")));
        MiningSessionValuation valuation = MiningSessionPriceResolver.resolve(
                List.of(
                        entry(
                                RAW_GOLD,
                                1L,
                                MiningSessionCategory.TARGET_MINED),
                        entry(
                                ENCHANTED_GOLD,
                                1L,
                                MiningSessionCategory.TARGET_MINED)),
                book,
                2_000L);

        assertEquals(new BigDecimal("505"), valuation.resolvedItemValue());
    }

    private static MiningSessionPriceBook book(
            long observedAtMillis,
            Map<String, BigDecimal> prices) {
        return MiningSessionPriceBook.available(observedAtMillis, prices);
    }

    private static MiningSessionLedger.Entry entry(
            MiningSessionResource resource,
            long quantity,
            MiningSessionCategory category) {
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                classification(resource, quantity, category),
                MiningSessionPriceResolution.unresolved());
        return ledger.entries().getFirst();
    }

    private static MiningSessionLedger.Entry currencyEntry(
            MiningSessionResource resource,
            long quantity,
            MiningSessionCategory category) {
        MiningSessionLedger ledger = new MiningSessionLedger();
        ledger.append(
                currencyClassification(resource, quantity, category),
                MiningSessionPriceResolution.unresolved());
        return ledger.entries().getFirst();
    }

    private static MiningSessionClassification classification(
            MiningSessionResource resource,
            long quantity,
            MiningSessionCategory category) {
        return MiningSessionClassification.wouldCredit(
                observation(resource, quantity),
                category);
    }

    private static MiningSessionClassification currencyClassification(
            MiningSessionResource resource,
            long quantity,
            MiningSessionCategory category) {
        return MiningSessionClassification.wouldCredit(
                observation(resource, quantity),
                category);
    }

    private static MiningSessionObservation observation(
            MiningSessionResource resource,
            long quantity) {
        return new MiningSessionObservation(
                resource,
                quantity,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.GOLD,
                1L,
                1_000L,
                "event-" + resource.resourceId(),
                "corr-" + resource.resourceId(),
                "test",
                null);
    }
}
