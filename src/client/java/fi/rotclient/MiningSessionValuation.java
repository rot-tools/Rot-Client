package fi.rotclient;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalLong;

/** Derived diagnostic valuation for immutable shadow ledger entries. */
final class MiningSessionValuation {
    static final String PRICE_BASIS_LABEL =
            "Bazaar instant sell (gross)";

    private final MiningSessionPriceResolution.PriceSource priceBasis;
    private final OptionalLong priceBookObservedAtMillis;
    private final OptionalLong oldestQuoteAgeMillis;
    private final int resolvedEntryCount;
    private final int unresolvedEntryCount;
    private final int staleEntryCount;
    private final int unavailableEntryCount;
    private final int unsupportedEntryCount;
    private final int excludedCurrencyEntryCount;
    private final BigDecimal resolvedItemValue;
    private final Map<MiningSessionCategory, BigDecimal> valueByCategory;
    private final Map<MiningSessionCategory, Integer> unresolvedByCategory;

    private MiningSessionValuation(
            MiningSessionPriceResolution.PriceSource priceBasis,
            OptionalLong priceBookObservedAtMillis,
            OptionalLong oldestQuoteAgeMillis,
            int resolvedEntryCount,
            int unresolvedEntryCount,
            int staleEntryCount,
            int unavailableEntryCount,
            int unsupportedEntryCount,
            int excludedCurrencyEntryCount,
            BigDecimal resolvedItemValue,
            Map<MiningSessionCategory, BigDecimal> valueByCategory,
            Map<MiningSessionCategory, Integer> unresolvedByCategory) {
        if (priceBasis == null) {
            throw new IllegalArgumentException(
                    "Price basis cannot be null");
        }
        if (resolvedItemValue == null) {
            throw new IllegalArgumentException(
                    "Resolved item value cannot be null");
        }
        if (valueByCategory == null) {
            throw new IllegalArgumentException(
                    "Value by category cannot be null");
        }
        if (unresolvedByCategory == null) {
            throw new IllegalArgumentException(
                    "Unresolved by category cannot be null");
        }

        this.priceBasis = priceBasis;
        this.priceBookObservedAtMillis = priceBookObservedAtMillis;
        this.oldestQuoteAgeMillis = oldestQuoteAgeMillis;
        this.resolvedEntryCount = resolvedEntryCount;
        this.unresolvedEntryCount = unresolvedEntryCount;
        this.staleEntryCount = staleEntryCount;
        this.unavailableEntryCount = unavailableEntryCount;
        this.unsupportedEntryCount = unsupportedEntryCount;
        this.excludedCurrencyEntryCount = excludedCurrencyEntryCount;
        this.resolvedItemValue = resolvedItemValue;
        this.valueByCategory = valueByCategory;
        this.unresolvedByCategory = unresolvedByCategory;
    }

    static MiningSessionValuation unavailable() {
        return fromUnavailableCounts(0, 0, 0);
    }

    static MiningSessionValuation fromUnavailableCounts(
            int unavailableEntryCount,
            int unsupportedEntryCount,
            int excludedCurrencyEntryCount) {
        return new MiningSessionValuation(
                MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL,
                OptionalLong.empty(),
                OptionalLong.empty(),
                0,
                0,
                0,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                BigDecimal.ZERO,
                zeroCategoryValues(),
                zeroUnresolvedCounts());
    }

    static MiningSessionValuation create(
            MiningSessionPriceResolution.PriceSource priceBasis,
            long priceBookObservedAtMillis,
            long oldestQuoteAgeMillis,
            int resolvedEntryCount,
            int unresolvedEntryCount,
            int staleEntryCount,
            int unavailableEntryCount,
            int unsupportedEntryCount,
            int excludedCurrencyEntryCount,
            BigDecimal resolvedItemValue,
            Map<MiningSessionCategory, BigDecimal> valueByCategory) {
        return create(
                priceBasis,
                priceBookObservedAtMillis,
                oldestQuoteAgeMillis,
                resolvedEntryCount,
                unresolvedEntryCount,
                staleEntryCount,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                resolvedItemValue,
                valueByCategory,
                zeroUnresolvedCounts());
    }

    static MiningSessionValuation create(
            MiningSessionPriceResolution.PriceSource priceBasis,
            long priceBookObservedAtMillis,
            long oldestQuoteAgeMillis,
            int resolvedEntryCount,
            int unresolvedEntryCount,
            int staleEntryCount,
            int unavailableEntryCount,
            int unsupportedEntryCount,
            int excludedCurrencyEntryCount,
            BigDecimal resolvedItemValue,
            Map<MiningSessionCategory, BigDecimal> valueByCategory,
            Map<MiningSessionCategory, Integer> unresolvedByCategory) {
        if (priceBookObservedAtMillis < 0L) {
            throw new IllegalArgumentException(
                    "Price-book timestamp cannot be negative");
        }
        if (oldestQuoteAgeMillis < 0L) {
            throw new IllegalArgumentException(
                    "Oldest quote age cannot be negative");
        }

        EnumMap<MiningSessionCategory, BigDecimal> normalized =
                zeroCategoryValues();
        valueByCategory.forEach((category, value) -> {
            if (!MiningSessionResourcePriceMapping.isPricedCategory(
                    category)) {
                throw new IllegalArgumentException(
                        "Valuation category is not priced: " + category);
            }
            if (value == null) {
                throw new IllegalArgumentException(
                        "Category value cannot be null");
            }
            normalized.put(category, value);
        });

        EnumMap<MiningSessionCategory, Integer> unresolved =
                zeroUnresolvedCounts();
        if (unresolvedByCategory != null) {
            unresolvedByCategory.forEach((category, count) -> {
                if (!MiningSessionResourcePriceMapping.isPricedCategory(
                        category)) {
                    throw new IllegalArgumentException(
                            "Unresolved category is not priced: " + category);
                }
                if (count == null || count < 0) {
                    throw new IllegalArgumentException(
                            "Unresolved count cannot be null or negative");
                }
                unresolved.put(category, count);
            });
        }

        return new MiningSessionValuation(
                priceBasis,
                OptionalLong.of(priceBookObservedAtMillis),
                resolvedEntryCount > 0
                        ? OptionalLong.of(oldestQuoteAgeMillis)
                        : OptionalLong.empty(),
                resolvedEntryCount,
                unresolvedEntryCount,
                staleEntryCount,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                resolvedItemValue,
                Map.copyOf(normalized),
                Map.copyOf(unresolved));
    }

    MiningSessionPriceResolution.PriceSource priceBasis() {
        return priceBasis;
    }

    String priceBasisLabel() {
        return priceBasis
                == MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL
                ? PRICE_BASIS_LABEL
                : "unknown";
    }

    boolean priceBookAvailable() {
        return priceBookObservedAtMillis.isPresent();
    }

    OptionalLong priceBookObservedAtMillis() {
        return priceBookObservedAtMillis;
    }

    OptionalLong oldestQuoteAgeMillis() {
        return oldestQuoteAgeMillis;
    }

    int resolvedEntryCount() {
        return resolvedEntryCount;
    }

    int unresolvedEntryCount() {
        return unresolvedEntryCount;
    }

    int unresolvedEntryCount(MiningSessionCategory category) {
        if (!MiningSessionResourcePriceMapping.isPricedCategory(category)) {
            return 0;
        }
        return unresolvedByCategory.getOrDefault(category, 0);
    }

    int staleEntryCount() {
        return staleEntryCount;
    }

    int unavailableEntryCount() {
        return unavailableEntryCount;
    }

    int unsupportedEntryCount() {
        return unsupportedEntryCount;
    }

    int excludedCurrencyEntryCount() {
        return excludedCurrencyEntryCount;
    }

    BigDecimal resolvedItemValue() {
        return resolvedItemValue;
    }

    BigDecimal valueByCategory(MiningSessionCategory category) {
        if (!MiningSessionResourcePriceMapping.isPricedCategory(category)) {
            throw new IllegalArgumentException(
                    "Category is not priced: " + category);
        }
        return valueByCategory.getOrDefault(
                category,
                BigDecimal.ZERO);
    }

    Map<MiningSessionCategory, BigDecimal> valueByCategory() {
        return valueByCategory;
    }

    boolean hasResolvedValue() {
        return resolvedEntryCount > 0
                && priceBookAvailable()
                && priceBasis
                == MiningSessionPriceResolution.PriceSource.BAZAAR_INSTANT_SELL;
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
