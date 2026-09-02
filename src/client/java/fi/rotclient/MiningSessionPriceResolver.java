package fi.rotclient;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

/** Pure projection from ledger entries and one captured price book. */
final class MiningSessionPriceResolver {
    private MiningSessionPriceResolver() {
    }

    static MiningSessionValuation resolve(
            List<MiningSessionLedger.Entry> entries,
            MiningSessionPriceBook priceBook,
            long nowMillis) {
        if (entries == null) {
            throw new IllegalArgumentException(
                    "Entries cannot be null");
        }
        if (priceBook == null) {
            throw new IllegalArgumentException(
                    "Price book cannot be null");
        }
        if (nowMillis < 0L) {
            return unavailableValuation(entries);
        }

        if (!priceBook.isAvailable()
                || priceBook.invalidTimestamp()) {
            return unavailableValuation(entries);
        }

        if (!MiningSessionPriceBook.hasValidTimestampOrdering(
                nowMillis,
                priceBook.observedAtMillis())) {
            return unavailableValuation(entries);
        }

        boolean staleBook = priceBook.isStale(nowMillis);
        long quoteAgeMillis = priceBook.quoteAgeMillis(nowMillis)
                .orElse(0L);

        int resolvedEntryCount = 0;
        int unresolvedEntryCount = 0;
        int staleEntryCount = 0;
        int unavailableEntryCount = 0;
        int unsupportedEntryCount = 0;
        int excludedCurrencyEntryCount = 0;
        BigDecimal resolvedItemValue = BigDecimal.ZERO;
        EnumMap<MiningSessionCategory, BigDecimal> valueByCategory =
                zeroCategoryValues();
        EnumMap<MiningSessionCategory, Integer> unresolvedByCategory =
                zeroUnresolvedCounts();

        for (MiningSessionLedger.Entry entry : entries) {
            MiningSessionCategory category = entry.category();
            MiningSessionResource resource =
                    entry.observation().resource();
            long quantity = entry.observation().quantity();

            if (category == MiningSessionCategory.CURRENCY
                    || resource.kind()
                    == MiningSessionResource.ResourceKind.CURRENCY) {
                excludedCurrencyEntryCount = Math.addExact(
                        excludedCurrencyEntryCount,
                        1);
                continue;
            }

            if (!MiningSessionResourcePriceMapping.isPricedCategory(
                    category)) {
                unsupportedEntryCount = Math.addExact(
                        unsupportedEntryCount,
                        1);
                continue;
            }

            Optional<String> productId =
                    MiningSessionResourcePriceMapping.bazaarProductId(
                            resource);
            if (productId.isEmpty()) {
                unsupportedEntryCount = Math.addExact(
                        unsupportedEntryCount,
                        1);
                continue;
            }

            if (staleBook) {
                staleEntryCount = Math.addExact(staleEntryCount, 1);
                continue;
            }

            Optional<BigDecimal> unitPrice =
                    priceBook.unitPrice(productId.get());
            if (unitPrice.isEmpty()) {
                unresolvedEntryCount = Math.addExact(
                        unresolvedEntryCount,
                        1);
                unresolvedByCategory.put(
                        category,
                        Math.addExact(
                                unresolvedByCategory.get(category),
                                1));
                continue;
            }

            BigDecimal entryValue = MiningSessionPriceBook.entryValue(
                    unitPrice.get(),
                    quantity);
            resolvedEntryCount = Math.addExact(resolvedEntryCount, 1);
            resolvedItemValue = resolvedItemValue.add(entryValue);
            valueByCategory.put(
                    category,
                    valueByCategory.get(category).add(entryValue));
        }

        return MiningSessionValuation.create(
                MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL,
                priceBook.observedAtMillis(),
                quoteAgeMillis,
                resolvedEntryCount,
                unresolvedEntryCount,
                staleEntryCount,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                resolvedItemValue,
                valueByCategory,
                unresolvedByCategory);
    }

    private static MiningSessionValuation unavailableValuation(
            List<MiningSessionLedger.Entry> entries) {
        int unavailableEntryCount = 0;
        int unsupportedEntryCount = 0;
        int excludedCurrencyEntryCount = 0;

        for (MiningSessionLedger.Entry entry : entries) {
            MiningSessionCategory category = entry.category();
            MiningSessionResource resource =
                    entry.observation().resource();

            if (category == MiningSessionCategory.CURRENCY
                    || resource.kind()
                    == MiningSessionResource.ResourceKind.CURRENCY) {
                excludedCurrencyEntryCount = Math.addExact(
                        excludedCurrencyEntryCount,
                        1);
                continue;
            }

            if (!MiningSessionResourcePriceMapping.isPricedCategory(
                    category)
                    || MiningSessionResourcePriceMapping
                    .bazaarProductId(resource)
                    .isEmpty()) {
                unsupportedEntryCount = Math.addExact(
                        unsupportedEntryCount,
                        1);
                continue;
            }

            unavailableEntryCount = Math.addExact(
                    unavailableEntryCount,
                    1);
        }

        return MiningSessionValuation.fromUnavailableCounts(
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount);
    }

    private static EnumMap<MiningSessionCategory, BigDecimal>
            zeroCategoryValues() {
        EnumMap<MiningSessionCategory, BigDecimal> values =
                new EnumMap<>(MiningSessionCategory.class);
        values.put(
                MiningSessionCategory.TARGET_MINED,
                BigDecimal.ZERO);
        values.put(
                MiningSessionCategory.OTHER_MINED,
                BigDecimal.ZERO);
        values.put(
                MiningSessionCategory.CHEST_LOOT,
                BigDecimal.ZERO);
        return values;
    }

    private static EnumMap<MiningSessionCategory, Integer>
            zeroUnresolvedCounts() {
        EnumMap<MiningSessionCategory, Integer> values =
                new EnumMap<>(MiningSessionCategory.class);
        values.put(MiningSessionCategory.TARGET_MINED, 0);
        values.put(MiningSessionCategory.OTHER_MINED, 0);
        values.put(MiningSessionCategory.CHEST_LOOT, 0);
        return values;
    }
}
