package fi.rotclient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Immutable, privacy-bounded presentation model for Session Analytics.
 * Contains only user-displayable fields derived from a mining-session snapshot.
 */
final class MiningSessionAnalyticsViewModel {
    enum SessionState {
        NOT_STARTED,
        ACTIVE,
        STOPPED
    }

    record ResourceQuantity(
            String resourceId,
            String displayName,
            long quantity) {
        ResourceQuantity {
            if (resourceId == null || resourceId.isBlank()) {
                throw new IllegalArgumentException(
                        "Resource ID cannot be null or blank");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException(
                        "Display name cannot be null or blank");
            }
            if (quantity <= 0L) {
                throw new IllegalArgumentException(
                        "Quantity must be positive");
            }
        }
    }

    private final SessionState sessionState;
    private final String selectedTargetDisplayName;
    private final boolean trackerEnabled;
    private final OptionalLong sessionStartedMillis;
    private final OptionalLong snapshotMillis;
    private final OptionalLong stoppedMillis;
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
    private final OptionalLong priceBookAgeMillis;
    private final boolean priceBookAvailable;
    private final boolean priceBookStale;
    private final Map<String, ResourceQuantity> targetQuantities;
    private final Map<String, ResourceQuantity> otherMinedQuantities;
    private final Map<String, ResourceQuantity> chestLootQuantities;
    private final Map<String, ResourceQuantity> currencyQuantities;
    private final boolean projectionAvailable;

    private MiningSessionAnalyticsViewModel(
            SessionState sessionState,
            String selectedTargetDisplayName,
            boolean trackerEnabled,
            OptionalLong sessionStartedMillis,
            OptionalLong snapshotMillis,
            OptionalLong stoppedMillis,
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
            OptionalLong priceBookAgeMillis,
            boolean priceBookAvailable,
            boolean priceBookStale,
            Map<String, ResourceQuantity> targetQuantities,
            Map<String, ResourceQuantity> otherMinedQuantities,
            Map<String, ResourceQuantity> chestLootQuantities,
            Map<String, ResourceQuantity> currencyQuantities,
            boolean projectionAvailable) {
        this.sessionState = sessionState;
        this.selectedTargetDisplayName = selectedTargetDisplayName;
        this.trackerEnabled = trackerEnabled;
        this.sessionStartedMillis = sessionStartedMillis;
        this.snapshotMillis = snapshotMillis;
        this.stoppedMillis = stoppedMillis;
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
        this.priceBookAgeMillis = priceBookAgeMillis;
        this.priceBookAvailable = priceBookAvailable;
        this.priceBookStale = priceBookStale;
        this.targetQuantities = targetQuantities;
        this.otherMinedQuantities = otherMinedQuantities;
        this.chestLootQuantities = chestLootQuantities;
        this.currencyQuantities = currencyQuantities;
        this.projectionAvailable = projectionAvailable;
    }

    static MiningSessionAnalyticsViewModel notStarted() {
        return unavailable(SessionState.NOT_STARTED);
    }

    static MiningSessionAnalyticsViewModel unavailable() {
        return unavailable(SessionState.NOT_STARTED);
    }

    private static MiningSessionAnalyticsViewModel unavailable(
            SessionState state) {
        return new MiningSessionAnalyticsViewModel(
                state,
                "unavailable",
                false,
                OptionalLong.empty(),
                OptionalLong.empty(),
                OptionalLong.empty(),
                0,
                0,
                0,
                0,
                0,
                "unavailable",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                false,
                BigDecimal.ZERO,
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OptionalLong.empty(),
                false,
                false,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                state != SessionState.NOT_STARTED);
    }

    static MiningSessionAnalyticsViewModel create(
            SessionState sessionState,
            String selectedTargetDisplayName,
            boolean trackerEnabled,
            OptionalLong sessionStartedMillis,
            OptionalLong snapshotMillis,
            OptionalLong stoppedMillis,
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
            OptionalLong priceBookAgeMillis,
            boolean priceBookAvailable,
            boolean priceBookStale,
            Map<String, ResourceQuantity> targetQuantities,
            Map<String, ResourceQuantity> otherMinedQuantities,
            Map<String, ResourceQuantity> chestLootQuantities,
            Map<String, ResourceQuantity> currencyQuantities) {
        if (sessionState == null) {
            throw new IllegalArgumentException(
                    "Session state cannot be null");
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
        if (sessionStartedMillis == null
                || snapshotMillis == null
                || stoppedMillis == null
                || priceBookAgeMillis == null) {
            throw new IllegalArgumentException(
                    "Optional timestamp fields cannot be null");
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

        return new MiningSessionAnalyticsViewModel(
                sessionState,
                selectedTargetDisplayName,
                trackerEnabled,
                sessionStartedMillis,
                snapshotMillis,
                stoppedMillis,
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
                priceBookAgeMillis,
                priceBookAvailable,
                priceBookStale,
                copyQuantities(targetQuantities),
                copyQuantities(otherMinedQuantities),
                copyQuantities(chestLootQuantities),
                copyQuantities(currencyQuantities),
                true);
    }

    private static Map<String, ResourceQuantity> copyQuantities(
            Map<String, ResourceQuantity> source) {
        if (source == null) {
            throw new IllegalArgumentException(
                    "Quantity map cannot be null");
        }
        LinkedHashMap<String, ResourceQuantity> copy =
                new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key == null || value == null) {
                throw new IllegalArgumentException(
                        "Quantity map entries cannot be null");
            }
            if (!key.equals(value.resourceId())) {
                throw new IllegalArgumentException(
                        "Quantity map key must match resource ID");
            }
            copy.put(key, value);
        });
        return Collections.unmodifiableMap(copy);
    }

    SessionState sessionState() {
        return sessionState;
    }

    String selectedTargetDisplayName() {
        return selectedTargetDisplayName;
    }

    boolean trackerEnabled() {
        return trackerEnabled;
    }

    OptionalLong sessionStartedMillis() {
        return sessionStartedMillis;
    }

    OptionalLong snapshotMillis() {
        return snapshotMillis;
    }

    OptionalLong stoppedMillis() {
        return stoppedMillis;
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

    OptionalLong priceBookAgeMillis() {
        return priceBookAgeMillis;
    }

    boolean priceBookAvailable() {
        return priceBookAvailable;
    }

    boolean priceBookStale() {
        return priceBookStale;
    }

    Map<String, ResourceQuantity> targetQuantities() {
        return targetQuantities;
    }

    Map<String, ResourceQuantity> otherMinedQuantities() {
        return otherMinedQuantities;
    }

    Map<String, ResourceQuantity> chestLootQuantities() {
        return chestLootQuantities;
    }

    Map<String, ResourceQuantity> currencyQuantities() {
        return currencyQuantities;
    }

    boolean projectionAvailable() {
        return projectionAvailable;
    }

    boolean hasViewableSession() {
        return sessionState == SessionState.ACTIVE
                || sessionState == SessionState.STOPPED;
    }
}
