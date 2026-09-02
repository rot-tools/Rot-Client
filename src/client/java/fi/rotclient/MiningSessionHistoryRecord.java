package fi.rotclient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Immutable privacy-bounded stored Session Analytics snapshot.
 * Contains only aggregated display fields; never ledger events or private data.
 */
final class MiningSessionHistoryRecord {
    private final String recordId;
    private final int schemaVersion;
    private final String contentFingerprint;
    private final long stoppedMillis;
    private final OptionalLong sessionStartedMillis;
    private final String selectedTargetDisplayName;
    private final boolean trackerEnabled;
    private final int entryCount;
    private final int targetEntryCount;
    private final int otherEntryCount;
    private final int chestLootEntryCount;
    private final int currencyEntryCount;
    private final String parityStatusLabel;
    private final long mismatchCount;
    private final String priceBasisLabel;
    private final boolean resolvedValueAvailable;
    private final BigDecimal resolvedItemValue;
    private final int resolvedEntryCount;
    private final int unresolvedEntryCount;
    private final int staleEntryCount;
    private final int unavailableEntryCount;
    private final int unsupportedEntryCount;
    private final int excludedCurrencyEntryCount;
    private final BigDecimal targetMinedValue;
    private final BigDecimal otherMinedValue;
    private final BigDecimal chestLootValue;
    private final OptionalLong priceBookObservedAtMillis;
    private final Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            targetQuantities;
    private final Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            otherMinedQuantities;
    private final Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            chestLootQuantities;
    private final Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            currencyQuantities;
    private final Optional<RotClientSessionFreeze> currentSessionFreeze;

    private MiningSessionHistoryRecord(
            String recordId,
            int schemaVersion,
            String contentFingerprint,
            long stoppedMillis,
            OptionalLong sessionStartedMillis,
            String selectedTargetDisplayName,
            boolean trackerEnabled,
            int entryCount,
            int targetEntryCount,
            int otherEntryCount,
            int chestLootEntryCount,
            int currencyEntryCount,
            String parityStatusLabel,
            long mismatchCount,
            String priceBasisLabel,
            boolean resolvedValueAvailable,
            BigDecimal resolvedItemValue,
            int resolvedEntryCount,
            int unresolvedEntryCount,
            int staleEntryCount,
            int unavailableEntryCount,
            int unsupportedEntryCount,
            int excludedCurrencyEntryCount,
            BigDecimal targetMinedValue,
            BigDecimal otherMinedValue,
            BigDecimal chestLootValue,
            OptionalLong priceBookObservedAtMillis,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    targetQuantities,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    otherMinedQuantities,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    chestLootQuantities,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    currencyQuantities,
            Optional<RotClientSessionFreeze> currentSessionFreeze) {
        this.recordId = recordId;
        this.schemaVersion = schemaVersion;
        this.contentFingerprint = contentFingerprint;
        this.stoppedMillis = stoppedMillis;
        this.sessionStartedMillis = sessionStartedMillis;
        this.selectedTargetDisplayName = selectedTargetDisplayName;
        this.trackerEnabled = trackerEnabled;
        this.entryCount = entryCount;
        this.targetEntryCount = targetEntryCount;
        this.otherEntryCount = otherEntryCount;
        this.chestLootEntryCount = chestLootEntryCount;
        this.currencyEntryCount = currencyEntryCount;
        this.parityStatusLabel = parityStatusLabel;
        this.mismatchCount = mismatchCount;
        this.priceBasisLabel = priceBasisLabel;
        this.resolvedValueAvailable = resolvedValueAvailable;
        this.resolvedItemValue = resolvedItemValue;
        this.resolvedEntryCount = resolvedEntryCount;
        this.unresolvedEntryCount = unresolvedEntryCount;
        this.staleEntryCount = staleEntryCount;
        this.unavailableEntryCount = unavailableEntryCount;
        this.unsupportedEntryCount = unsupportedEntryCount;
        this.excludedCurrencyEntryCount = excludedCurrencyEntryCount;
        this.targetMinedValue = targetMinedValue;
        this.otherMinedValue = otherMinedValue;
        this.chestLootValue = chestLootValue;
        this.priceBookObservedAtMillis = priceBookObservedAtMillis;
        this.targetQuantities = targetQuantities;
        this.otherMinedQuantities = otherMinedQuantities;
        this.chestLootQuantities = chestLootQuantities;
        this.currencyQuantities = currencyQuantities;
        this.currentSessionFreeze = currentSessionFreeze == null
                ? Optional.empty()
                : currentSessionFreeze;
    }

    static MiningSessionHistoryRecord create(
            String recordId,
            int schemaVersion,
            String contentFingerprint,
            long stoppedMillis,
            OptionalLong sessionStartedMillis,
            String selectedTargetDisplayName,
            boolean trackerEnabled,
            int entryCount,
            int targetEntryCount,
            int otherEntryCount,
            int chestLootEntryCount,
            int currencyEntryCount,
            String parityStatusLabel,
            long mismatchCount,
            String priceBasisLabel,
            boolean resolvedValueAvailable,
            BigDecimal resolvedItemValue,
            int resolvedEntryCount,
            int unresolvedEntryCount,
            int staleEntryCount,
            int unavailableEntryCount,
            int unsupportedEntryCount,
            int excludedCurrencyEntryCount,
            BigDecimal targetMinedValue,
            BigDecimal otherMinedValue,
            BigDecimal chestLootValue,
            OptionalLong priceBookObservedAtMillis,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    targetQuantities,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    otherMinedQuantities,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    chestLootQuantities,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    currencyQuantities) {
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException(
                    "Record ID cannot be null or blank");
        }
        if (contentFingerprint == null || contentFingerprint.isBlank()) {
            throw new IllegalArgumentException(
                    "Content fingerprint cannot be null or blank");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "Schema version must be at least 1");
        }
        if (stoppedMillis <= 0L) {
            throw new IllegalArgumentException(
                    "Stopped timestamp must be positive");
        }
        if (sessionStartedMillis == null
                || priceBookObservedAtMillis == null) {
            throw new IllegalArgumentException(
                    "Optional timestamp fields cannot be null");
        }
        if (sessionStartedMillis.isPresent()
                && sessionStartedMillis.getAsLong() <= 0L) {
            throw new IllegalArgumentException(
                    "Session start timestamp must be positive when present");
        }
        if (priceBookObservedAtMillis.isPresent()
                && priceBookObservedAtMillis.getAsLong() < 0L) {
            throw new IllegalArgumentException(
                    "Price-book timestamp cannot be negative");
        }
        if (selectedTargetDisplayName == null
                || selectedTargetDisplayName.isBlank()) {
            throw new IllegalArgumentException(
                    "Target display name cannot be null or blank");
        }
        if (parityStatusLabel == null || parityStatusLabel.isBlank()) {
            throw new IllegalArgumentException(
                    "Parity status cannot be null or blank");
        }
        if (priceBasisLabel == null || priceBasisLabel.isBlank()) {
            throw new IllegalArgumentException(
                    "Price basis label cannot be null or blank");
        }
        if (resolvedItemValue == null
                || targetMinedValue == null
                || otherMinedValue == null
                || chestLootValue == null) {
            throw new IllegalArgumentException(
                    "Value fields cannot be null");
        }
        if (mismatchCount < 0L
                || entryCount < 0
                || targetEntryCount < 0
                || otherEntryCount < 0
                || chestLootEntryCount < 0
                || currencyEntryCount < 0
                || resolvedEntryCount < 0
                || unresolvedEntryCount < 0
                || staleEntryCount < 0
                || unavailableEntryCount < 0
                || unsupportedEntryCount < 0
                || excludedCurrencyEntryCount < 0) {
            throw new IllegalArgumentException(
                    "Counts cannot be negative");
        }

        return new MiningSessionHistoryRecord(
                recordId,
                schemaVersion,
                contentFingerprint,
                stoppedMillis,
                sessionStartedMillis,
                selectedTargetDisplayName,
                trackerEnabled,
                entryCount,
                targetEntryCount,
                otherEntryCount,
                chestLootEntryCount,
                currencyEntryCount,
                parityStatusLabel,
                mismatchCount,
                priceBasisLabel,
                resolvedValueAvailable,
                resolvedItemValue,
                resolvedEntryCount,
                unresolvedEntryCount,
                staleEntryCount,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                targetMinedValue,
                otherMinedValue,
                chestLootValue,
                priceBookObservedAtMillis,
                copyQuantities(targetQuantities),
                copyQuantities(otherMinedQuantities),
                copyQuantities(chestLootQuantities),
                copyQuantities(currencyQuantities),
                Optional.empty());
    }

    static MiningSessionHistoryRecord fromStoppedViewModel(
            String recordId,
            int schemaVersion,
            String contentFingerprint,
            MiningSessionAnalyticsViewModel model,
            OptionalLong priceBookObservedAtMillis) {
        if (model == null) {
            throw new IllegalArgumentException(
                    "View model cannot be null");
        }
        if (model.sessionState()
                != MiningSessionAnalyticsViewModel.SessionState.STOPPED) {
            throw new IllegalArgumentException(
                    "Only STOPPED analytics sessions may be stored");
        }
        if (model.stoppedMillis().isEmpty()
                || model.stoppedMillis().getAsLong() <= 0L) {
            throw new IllegalArgumentException(
                    "Stopped timestamp is required");
        }
        return create(
                recordId,
                schemaVersion,
                contentFingerprint,
                model.stoppedMillis().getAsLong(),
                model.sessionStartedMillis(),
                model.selectedTargetDisplayName(),
                model.trackerEnabled(),
                model.entryCount(),
                model.targetEntryCount(),
                model.otherEntryCount(),
                model.chestLootEntryCount(),
                model.currencyEntryCount(),
                model.parityStatusLabel(),
                model.mismatchCount(),
                model.priceBasisLabel(),
                model.resolvedValueAvailable(),
                model.resolvedItemValue(),
                model.resolvedEntryCount(),
                model.unresolvedEntryCount(),
                model.staleEntryCount(),
                model.unavailableEntryCount(),
                model.unsupportedEntryCount(),
                model.excludedCurrencyEntryCount(),
                model.targetMinedValue(),
                model.otherMinedValue(),
                model.chestLootValue(),
                priceBookObservedAtMillis == null
                        ? OptionalLong.empty()
                        : priceBookObservedAtMillis,
                model.targetQuantities(),
                model.otherMinedQuantities(),
                model.chestLootQuantities(),
                model.currencyQuantities());
    }

    static MiningSessionHistoryRecord fromCurrentSessionFreeze(
            String recordId,
            int schemaVersion,
            String contentFingerprint,
            RotClientSessionFreeze frozen) {
        if (frozen == null) {
            throw new IllegalArgumentException(
                    "Current Session freeze cannot be null");
        }
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                target = new LinkedHashMap<>();
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                other = new LinkedHashMap<>();
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                chest = new LinkedHashMap<>();
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                currency = new LinkedHashMap<>();
        int targetEntries = 0;
        int otherEntries = 0;
        int chestEntries = 0;
        int currencyEntries = 0;
        int resolvedEntries = 0;
        int staleEntries = 0;
        int unavailableEntries = 0;
        int unsupportedEntries = 0;
        int excludedCurrencyEntries = 0;
        BigDecimal resolvedValue = BigDecimal.ZERO;
        BigDecimal targetValue = BigDecimal.ZERO;
        BigDecimal otherValue = BigDecimal.ZERO;
        BigDecimal chestValue = BigDecimal.ZERO;

        for (RotClientCurrentSessionConfig.SessionItemRecord row
                : frozen.itemRows()) {
            SessionSourceType source = row.source();
            if (source == SessionSourceType.CURRENCY) {
                currencyEntries++;
                excludedCurrencyEntries++;
                mergeQuantity(currency, row);
                continue;
            }
            boolean targetRow = source == SessionSourceType.MINING
                    && row.miningClassification()
                    == MiningClassification.TARGET;
            boolean otherRow = source == SessionSourceType.MINING
                    && row.miningClassification()
                    == MiningClassification.OTHER;
            boolean chestRow = source == SessionSourceType.CHEST;
            if (targetRow) {
                targetEntries++;
                mergeQuantity(target, row);
            } else if (otherRow) {
                otherEntries++;
                mergeQuantity(other, row);
            } else if (chestRow) {
                chestEntries++;
                mergeQuantity(chest, row);
            }

            BigDecimal rowValue = BigDecimal.valueOf(
                    Math.max(0.0, row.resolvedGrossValue()));
            switch (row.price()) {
                case RESOLVED_BAZAAR -> {
                    resolvedEntries++;
                    resolvedValue = resolvedValue.add(rowValue);
                    if (targetRow) {
                        targetValue = targetValue.add(rowValue);
                    } else if (otherRow) {
                        otherValue = otherValue.add(rowValue);
                    } else if (chestRow) {
                        chestValue = chestValue.add(rowValue);
                    }
                }
                case STALE -> staleEntries++;
                case UNAVAILABLE -> unavailableEntries++;
                case UNSUPPORTED -> unsupportedEntries++;
            }
        }
        int unresolvedEntries = staleEntries
                + unavailableEntries
                + unsupportedEntries;
        String targetDisplay = frozen.selectedTargetId().isBlank()
                ? "Unknown"
                : TrackerSelection.fromId(
                frozen.selectedTargetId()).displayName();
        MiningSessionHistoryRecord base = create(
                recordId,
                schemaVersion,
                contentFingerprint,
                frozen.stoppedAtMillis(),
                OptionalLong.of(frozen.startedAtMillis()),
                targetDisplay,
                true,
                frozen.itemRows().size(),
                targetEntries,
                otherEntries,
                chestEntries,
                currencyEntries,
                "NOT_CHECKED",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                resolvedEntries > 0 && unresolvedEntries == 0,
                resolvedValue,
                resolvedEntries,
                unresolvedEntries,
                staleEntries,
                unavailableEntries,
                unsupportedEntries,
                excludedCurrencyEntries,
                targetValue,
                otherValue,
                chestValue,
                frozen.priceBookObservedAtMillis(),
                target,
                other,
                chest,
                currency);
        return base.withCurrentSessionFreeze(frozen);
    }

    MiningSessionHistoryRecord withCurrentSessionFreeze(
            RotClientSessionFreeze frozen) {
        return new MiningSessionHistoryRecord(
                recordId,
                schemaVersion,
                contentFingerprint,
                stoppedMillis,
                sessionStartedMillis,
                selectedTargetDisplayName,
                trackerEnabled,
                entryCount,
                targetEntryCount,
                otherEntryCount,
                chestLootEntryCount,
                currencyEntryCount,
                parityStatusLabel,
                mismatchCount,
                priceBasisLabel,
                resolvedValueAvailable,
                resolvedItemValue,
                resolvedEntryCount,
                unresolvedEntryCount,
                staleEntryCount,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                targetMinedValue,
                otherMinedValue,
                chestLootValue,
                priceBookObservedAtMillis,
                targetQuantities,
                otherMinedQuantities,
                chestLootQuantities,
                currencyQuantities,
                Optional.of(frozen));
    }

    private static void mergeQuantity(
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> map,
            RotClientCurrentSessionConfig.SessionItemRecord row) {
        map.merge(
                row.itemId(),
                new MiningSessionAnalyticsViewModel.ResourceQuantity(
                        row.itemId(), row.displayName(), row.quantity()),
                (left, right) ->
                        new MiningSessionAnalyticsViewModel.ResourceQuantity(
                                left.resourceId(),
                                left.displayName(),
                                Math.addExact(
                                        left.quantity(), right.quantity())));
    }

    MiningSessionAnalyticsViewModel toViewModel() {
        OptionalLong priceBookAge = OptionalLong.empty();
        boolean priceBookAvailable = priceBookObservedAtMillis.isPresent();
        boolean priceBookStale = staleEntryCount > 0;
        if (priceBookObservedAtMillis.isPresent()
                && stoppedMillis >= priceBookObservedAtMillis.getAsLong()) {
            priceBookAge = OptionalLong.of(
                    stoppedMillis - priceBookObservedAtMillis.getAsLong());
            if (priceBookAge.getAsLong()
                    >= MiningSessionPriceBook.STALE_THRESHOLD_MILLIS) {
                priceBookStale = true;
            }
        }

        return MiningSessionAnalyticsViewModel.create(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                selectedTargetDisplayName,
                trackerEnabled,
                sessionStartedMillis,
                OptionalLong.of(stoppedMillis),
                OptionalLong.of(stoppedMillis),
                entryCount,
                targetEntryCount,
                otherEntryCount,
                chestLootEntryCount,
                currencyEntryCount,
                parityStatusLabel,
                mismatchCount,
                priceBasisLabel,
                resolvedValueAvailable,
                resolvedItemValue,
                resolvedEntryCount,
                unresolvedEntryCount,
                staleEntryCount,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                targetMinedValue,
                otherMinedValue,
                chestLootValue,
                priceBookAge,
                priceBookAvailable,
                priceBookStale,
                targetQuantities,
                otherMinedQuantities,
                chestLootQuantities,
                currencyQuantities);
    }

    private static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            copyQuantities(
                    Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                            source) {
        if (source == null) {
            throw new IllegalArgumentException(
                    "Quantity map cannot be null");
        }
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || value == null) {
                throw new IllegalArgumentException(
                        "Quantity map entries cannot be null");
            }
            if (!key.equals(value.resourceId())) {
                throw new IllegalArgumentException(
                        "Quantity map key must match resource ID");
            }
            if (value.quantity() <= 0L) {
                throw new IllegalArgumentException(
                        "Quantity must be positive");
            }
            copy.put(key, value);
        });
        return Collections.unmodifiableMap(copy);
    }

    String recordId() {
        return recordId;
    }

    int schemaVersion() {
        return schemaVersion;
    }

    String contentFingerprint() {
        return contentFingerprint;
    }

    long stoppedMillis() {
        return stoppedMillis;
    }

    OptionalLong sessionStartedMillis() {
        return sessionStartedMillis;
    }

    String selectedTargetDisplayName() {
        return selectedTargetDisplayName;
    }

    boolean trackerEnabled() {
        return trackerEnabled;
    }

    int entryCount() {
        return entryCount;
    }

    int targetEntryCount() {
        return targetEntryCount;
    }

    int otherEntryCount() {
        return otherEntryCount;
    }

    int chestLootEntryCount() {
        return chestLootEntryCount;
    }

    int currencyEntryCount() {
        return currencyEntryCount;
    }

    String parityStatusLabel() {
        return parityStatusLabel;
    }

    long mismatchCount() {
        return mismatchCount;
    }

    String priceBasisLabel() {
        return priceBasisLabel;
    }

    boolean resolvedValueAvailable() {
        return resolvedValueAvailable;
    }

    BigDecimal resolvedItemValue() {
        return resolvedItemValue;
    }

    int resolvedEntryCount() {
        return resolvedEntryCount;
    }

    int unresolvedEntryCount() {
        return unresolvedEntryCount;
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

    BigDecimal targetMinedValue() {
        return targetMinedValue;
    }

    BigDecimal otherMinedValue() {
        return otherMinedValue;
    }

    BigDecimal chestLootValue() {
        return chestLootValue;
    }

    OptionalLong priceBookObservedAtMillis() {
        return priceBookObservedAtMillis;
    }

    Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            targetQuantities() {
        return targetQuantities;
    }

    Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            otherMinedQuantities() {
        return otherMinedQuantities;
    }

    Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            chestLootQuantities() {
        return chestLootQuantities;
    }

    Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            currencyQuantities() {
        return currencyQuantities;
    }

    Optional<RotClientSessionFreeze> currentSessionFreeze() {
        return currentSessionFreeze;
    }

    boolean summaryMatchesCurrentSessionFreeze() {
        if (currentSessionFreeze.isEmpty()) {
            return true;
        }
        MiningSessionHistoryRecord expected = fromCurrentSessionFreeze(
                recordId,
                schemaVersion,
                contentFingerprint,
                currentSessionFreeze.orElseThrow());
        return stoppedMillis == expected.stoppedMillis
                && sessionStartedMillis.equals(expected.sessionStartedMillis)
                && selectedTargetDisplayName.equals(
                expected.selectedTargetDisplayName)
                && trackerEnabled == expected.trackerEnabled
                && entryCount == expected.entryCount
                && targetEntryCount == expected.targetEntryCount
                && otherEntryCount == expected.otherEntryCount
                && chestLootEntryCount == expected.chestLootEntryCount
                && currencyEntryCount == expected.currencyEntryCount
                && parityStatusLabel.equals(expected.parityStatusLabel)
                && mismatchCount == expected.mismatchCount
                && priceBasisLabel.equals(expected.priceBasisLabel)
                && resolvedValueAvailable == expected.resolvedValueAvailable
                && resolvedItemValue.compareTo(expected.resolvedItemValue) == 0
                && resolvedEntryCount == expected.resolvedEntryCount
                && unresolvedEntryCount == expected.unresolvedEntryCount
                && staleEntryCount == expected.staleEntryCount
                && unavailableEntryCount == expected.unavailableEntryCount
                && unsupportedEntryCount == expected.unsupportedEntryCount
                && excludedCurrencyEntryCount
                == expected.excludedCurrencyEntryCount
                && targetMinedValue.compareTo(expected.targetMinedValue) == 0
                && otherMinedValue.compareTo(expected.otherMinedValue) == 0
                && chestLootValue.compareTo(expected.chestLootValue) == 0
                && priceBookObservedAtMillis.equals(
                expected.priceBookObservedAtMillis)
                && targetQuantities.equals(expected.targetQuantities)
                && otherMinedQuantities.equals(expected.otherMinedQuantities)
                && chestLootQuantities.equals(expected.chestLootQuantities)
                && currencyQuantities.equals(expected.currencyQuantities);
    }
}
