package fi.rotclient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

/** Immutable point-in-time view of the disconnected mining-session ledger. */
final class MiningSessionSnapshot {
    private final long sessionId;
    private final long sessionStartMillis;
    private final long selectionStartMillis;
    private final long snapshotAtMillis;
    private final boolean diagnosticsActive;
    private final boolean trackerEnabled;
    private final TrackerSelection selectedTracker;
    private final long sessionEpoch;
    private final long selectionEpoch;
    private final List<MiningSessionLedger.Entry> entries;
    private final Map<MiningSessionCategory, Integer> entryCounts;
    private final Map<MiningSessionCategory,
            Map<MiningSessionResource, Long>> quantities;
    private final Map<MiningSessionCategory, Long> itemTotals;
    private final OptionalLong lastAcceptedEventTimestamp;
    private final MiningSessionParity.Status targetParityStatus;
    private final long parityMismatchCount;
    private final int unresolvedPriceEntryCount;
    private final MiningSessionValuation valuation;

    private MiningSessionSnapshot(
            long sessionId,
            long sessionStartMillis,
            long selectionStartMillis,
            long snapshotAtMillis,
            boolean diagnosticsActive,
            boolean trackerEnabled,
            TrackerSelection selectedTracker,
            long sessionEpoch,
            long selectionEpoch,
            List<MiningSessionLedger.Entry> entries,
            Map<MiningSessionCategory, Integer> entryCounts,
            Map<MiningSessionCategory,
                    Map<MiningSessionResource, Long>> quantities,
            Map<MiningSessionCategory, Long> itemTotals,
            OptionalLong lastAcceptedEventTimestamp,
            MiningSessionParity.Status targetParityStatus,
            long parityMismatchCount,
            int unresolvedPriceEntryCount,
            MiningSessionValuation valuation) {
        this.sessionId = sessionId;
        this.sessionStartMillis = sessionStartMillis;
        this.selectionStartMillis = selectionStartMillis;
        this.snapshotAtMillis = snapshotAtMillis;
        this.diagnosticsActive = diagnosticsActive;
        this.trackerEnabled = trackerEnabled;
        this.selectedTracker = selectedTracker;
        this.sessionEpoch = sessionEpoch;
        this.selectionEpoch = selectionEpoch;
        this.entries = entries;
        this.entryCounts = entryCounts;
        this.quantities = quantities;
        this.itemTotals = itemTotals;
        this.lastAcceptedEventTimestamp = lastAcceptedEventTimestamp;
        this.targetParityStatus = targetParityStatus;
        this.parityMismatchCount = parityMismatchCount;
        this.unresolvedPriceEntryCount = unresolvedPriceEntryCount;
        this.valuation = valuation == null
                ? MiningSessionValuation.unavailable()
                : valuation;
    }

    static MiningSessionSnapshot capture(
            long sessionId,
            long sessionStartMillis,
            long snapshotAtMillis,
            boolean diagnosticsActive,
            boolean trackerEnabled,
            TrackerSelection selectedTracker,
            long sessionEpoch,
            long selectionEpoch,
            MiningSessionLedger ledger,
            MiningSessionParity.Snapshot paritySnapshot) {
        return capture(
                sessionId,
                sessionStartMillis,
                sessionStartMillis,
                snapshotAtMillis,
                diagnosticsActive,
                trackerEnabled,
                selectedTracker,
                sessionEpoch,
                selectionEpoch,
                ledger,
                paritySnapshot,
                null,
                MiningSessionValuation.unavailable());
    }

    static MiningSessionSnapshot capture(
            long sessionId,
            long sessionStartMillis,
            long snapshotAtMillis,
            boolean diagnosticsActive,
            boolean trackerEnabled,
            TrackerSelection selectedTracker,
            long sessionEpoch,
            long selectionEpoch,
            MiningSessionLedger ledger,
            MiningSessionParity.Snapshot paritySnapshot,
            Long acceptedEventTimestamp) {
        return capture(
                sessionId,
                sessionStartMillis,
                sessionStartMillis,
                snapshotAtMillis,
                diagnosticsActive,
                trackerEnabled,
                selectedTracker,
                sessionEpoch,
                selectionEpoch,
                ledger,
                paritySnapshot,
                acceptedEventTimestamp,
                MiningSessionValuation.unavailable());
    }

    static MiningSessionSnapshot capture(
            long sessionId,
            long sessionStartMillis,
            long selectionStartMillis,
            long snapshotAtMillis,
            boolean diagnosticsActive,
            boolean trackerEnabled,
            TrackerSelection selectedTracker,
            long sessionEpoch,
            long selectionEpoch,
            MiningSessionLedger ledger,
            MiningSessionParity.Snapshot paritySnapshot,
            Long acceptedEventTimestamp) {
        return capture(
                sessionId,
                sessionStartMillis,
                selectionStartMillis,
                snapshotAtMillis,
                diagnosticsActive,
                trackerEnabled,
                selectedTracker,
                sessionEpoch,
                selectionEpoch,
                ledger,
                paritySnapshot,
                acceptedEventTimestamp,
                MiningSessionValuation.unavailable());
    }

    static MiningSessionSnapshot capture(
            long sessionId,
            long sessionStartMillis,
            long selectionStartMillis,
            long snapshotAtMillis,
            boolean diagnosticsActive,
            boolean trackerEnabled,
            TrackerSelection selectedTracker,
            long sessionEpoch,
            long selectionEpoch,
            MiningSessionLedger ledger,
            MiningSessionParity.Snapshot paritySnapshot,
            Long acceptedEventTimestamp,
            MiningSessionValuation valuation) {
        requireNonNegative(sessionId, "Session ID");
        requireNonNegative(sessionStartMillis, "Session start timestamp");
        requireNonNegative(
                selectionStartMillis,
                "Selection start timestamp");
        requireNonNegative(snapshotAtMillis, "Snapshot timestamp");
        requireNonNegative(sessionEpoch, "Session epoch");
        requireNonNegative(selectionEpoch, "Selection epoch");
        if (selectedTracker == null) {
            throw new IllegalArgumentException(
                    "Selected tracker cannot be null");
        }
        if (ledger == null) {
            throw new IllegalArgumentException(
                    "Ledger cannot be null");
        }
        if (paritySnapshot == null
                || paritySnapshot.status() == null) {
            throw new IllegalArgumentException(
                    "Parity snapshot cannot be null or incomplete");
        }
        requireNonNegative(
                paritySnapshot.mismatchCount(),
                "Parity mismatch count");
        if (acceptedEventTimestamp != null) {
            requireNonNegative(
                    acceptedEventTimestamp,
                    "Last accepted event timestamp");
        }

        List<MiningSessionLedger.Entry> ledgerEntries =
                ledger.entries();
        EnumMap<MiningSessionCategory, Integer> mutableEntryCounts =
                zeroEntryCounts();
        EnumMap<MiningSessionCategory,
                Map<MiningSessionResource, Long>> mutableQuantities =
                emptyQuantities();
        EnumMap<MiningSessionCategory, Long> mutableItemTotals =
                zeroItemTotals();
        int unresolvedCount = 0;
        Long lastLedgerEntryTimestamp = null;

        for (MiningSessionLedger.Entry entry : ledgerEntries) {
            validateEntry(entry);
            MiningSessionCategory category = entry.category();
            MiningSessionResource resource =
                    entry.observation().resource();
            long quantity = entry.observation().quantity();

            mutableEntryCounts.put(
                    category,
                    Math.addExact(
                            mutableEntryCounts.get(category),
                            1));
            Map<MiningSessionResource, Long> categoryQuantities =
                    mutableQuantities.get(category);
            categoryQuantities.put(
                    resource,
                    Math.addExact(
                            categoryQuantities.getOrDefault(
                                    resource,
                                    0L),
                            quantity));
            if (resource.kind()
                    == MiningSessionResource.ResourceKind.ITEM) {
                mutableItemTotals.put(
                        category,
                        Math.addExact(
                                mutableItemTotals.get(category),
                                quantity));
            }
            unresolvedCount = Math.addExact(
                    unresolvedCount,
                    1);
            lastLedgerEntryTimestamp = entry.acceptedAtMillis();
        }

        Long lastAcceptedTimestamp = acceptedEventTimestamp == null
                ? lastLedgerEntryTimestamp
                : acceptedEventTimestamp;

        return new MiningSessionSnapshot(
                sessionId,
                sessionStartMillis,
                selectionStartMillis,
                snapshotAtMillis,
                diagnosticsActive,
                trackerEnabled,
                selectedTracker,
                sessionEpoch,
                selectionEpoch,
                List.copyOf(ledgerEntries),
                Collections.unmodifiableMap(mutableEntryCounts),
                freezeQuantities(mutableQuantities),
                Collections.unmodifiableMap(mutableItemTotals),
                lastAcceptedTimestamp == null
                        ? OptionalLong.empty()
                        : OptionalLong.of(lastAcceptedTimestamp),
                paritySnapshot.status(),
                paritySnapshot.mismatchCount(),
                unresolvedCount,
                valuation);
    }

    long sessionId() {
        return sessionId;
    }

    long sessionStartMillis() {
        return sessionStartMillis;
    }

    long selectionStartMillis() {
        return selectionStartMillis;
    }

    long snapshotAtMillis() {
        return snapshotAtMillis;
    }

    boolean diagnosticsActive() {
        return diagnosticsActive;
    }

    boolean trackerEnabled() {
        return trackerEnabled;
    }

    TrackerSelection selectedTracker() {
        return selectedTracker;
    }

    long sessionEpoch() {
        return sessionEpoch;
    }

    long selectionEpoch() {
        return selectionEpoch;
    }

    List<MiningSessionLedger.Entry> entries() {
        return entries;
    }

    Map<MiningSessionCategory, Integer> entryCounts() {
        return entryCounts;
    }

    int entryCount() {
        return entries.size();
    }

    int entryCount(MiningSessionCategory category) {
        requireCategory(category);
        return entryCounts.get(category);
    }

    int targetMinedEntryCount() {
        return entryCount(MiningSessionCategory.TARGET_MINED);
    }

    int otherMinedEntryCount() {
        return entryCount(MiningSessionCategory.OTHER_MINED);
    }

    int chestLootEntryCount() {
        return entryCount(MiningSessionCategory.CHEST_LOOT);
    }

    int currencyEntryCount() {
        return entryCount(MiningSessionCategory.CURRENCY);
    }

    Map<MiningSessionCategory,
            Map<MiningSessionResource, Long>> quantities() {
        return quantities;
    }

    Map<MiningSessionResource, Long> quantities(
            MiningSessionCategory category) {
        requireCategory(category);
        return quantities.get(category);
    }

    long quantity(
            MiningSessionCategory category,
            MiningSessionResource resource) {
        requireCategory(category);
        if (resource == null) {
            throw new IllegalArgumentException(
                    "Resource cannot be null");
        }
        return quantities.get(category)
                .getOrDefault(resource, 0L);
    }

    Map<MiningSessionCategory, Long> totalItemQuantities() {
        return itemTotals;
    }

    long totalItemQuantity(
            MiningSessionCategory category) {
        requireCategory(category);
        return itemTotals.get(category);
    }

    OptionalLong lastAcceptedEventTimestamp() {
        return lastAcceptedEventTimestamp;
    }

    MiningSessionParity.Status targetParityStatus() {
        return targetParityStatus;
    }

    long parityMismatchCount() {
        return parityMismatchCount;
    }

    int unresolvedPriceEntryCount() {
        return unresolvedPriceEntryCount;
    }

    MiningSessionValuation valuation() {
        return valuation;
    }

    BigDecimal resolvedCoinValue(
            MiningSessionCategory category) {
        requireCategory(category);
        return BigDecimal.ZERO;
    }

    BigDecimal resolvedTotalCoinValue() {
        return BigDecimal.ZERO;
    }

    private static void validateEntry(
            MiningSessionLedger.Entry entry) {
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Ledger entries cannot be null");
        }
        MiningSessionCategory category = entry.category();
        if (category != MiningSessionCategory.TARGET_MINED
                && category != MiningSessionCategory.OTHER_MINED
                && category != MiningSessionCategory.CHEST_LOOT
                && category != MiningSessionCategory.CURRENCY) {
            throw new IllegalArgumentException(
                    "Snapshot supports only shadow ledger categories");
        }
        MiningSessionResource.ResourceKind resourceKind =
                entry.observation().resource().kind();
        if (category == MiningSessionCategory.CURRENCY) {
            if (resourceKind
                    != MiningSessionResource.ResourceKind.CURRENCY) {
                throw new IllegalArgumentException(
                        "Currency category requires a currency resource");
            }
        } else if (resourceKind
                != MiningSessionResource.ResourceKind.ITEM) {
            throw new IllegalArgumentException(
                    "Snapshot entries must be item resources");
        }
        if (entry.priceResolution().status()
                != MiningSessionPriceResolution.PriceStatus.UNRESOLVED) {
            throw new IllegalArgumentException(
                    "Snapshot entry prices must remain unresolved");
        }
    }

    private static EnumMap<MiningSessionCategory, Integer>
            zeroEntryCounts() {
        EnumMap<MiningSessionCategory, Integer> counts =
                new EnumMap<>(MiningSessionCategory.class);
        for (MiningSessionCategory category
                : MiningSessionCategory.values()) {
            counts.put(category, 0);
        }
        return counts;
    }

    private static EnumMap<MiningSessionCategory,
            Map<MiningSessionResource, Long>> emptyQuantities() {
        EnumMap<MiningSessionCategory,
                Map<MiningSessionResource, Long>> result =
                new EnumMap<>(MiningSessionCategory.class);
        for (MiningSessionCategory category
                : MiningSessionCategory.values()) {
            result.put(category, new HashMap<>());
        }
        return result;
    }

    private static EnumMap<MiningSessionCategory, Long>
            zeroItemTotals() {
        EnumMap<MiningSessionCategory, Long> totals =
                new EnumMap<>(MiningSessionCategory.class);
        for (MiningSessionCategory category
                : MiningSessionCategory.values()) {
            totals.put(category, 0L);
        }
        return totals;
    }

    private static Map<MiningSessionCategory,
            Map<MiningSessionResource, Long>> freezeQuantities(
            EnumMap<MiningSessionCategory,
                    Map<MiningSessionResource, Long>> source) {
        EnumMap<MiningSessionCategory,
                Map<MiningSessionResource, Long>> copy =
                new EnumMap<>(MiningSessionCategory.class);
        source.forEach((category, categoryQuantities) ->
                copy.put(
                        category,
                        Map.copyOf(categoryQuantities)));
        return Collections.unmodifiableMap(copy);
    }

    private static void requireCategory(
            MiningSessionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException(
                    "Category cannot be null");
        }
    }

    private static void requireNonNegative(
            long value,
            String label) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    label + " cannot be negative");
        }
    }
}
