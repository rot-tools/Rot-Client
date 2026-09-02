package fi.rotclient;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Compact Mining Tracker HUD read-model for non-target world/gameplay item
 * gains ("OTHERS").
 *
 * <p>This is a <em>projection</em> over Current Session rows — it does not
 * mutate source taxonomy. Eligible underlying sources:
 * <ul>
 *   <li>{@link SessionSourceType#MINING} + {@link MiningClassification#OTHER}
 *   <li>{@link SessionSourceType#MOB}
 *   <li>{@link SessionSourceType#CHEST} (chest / reward loot)
 * </ul>
 *
 * Excluded from quantity and value: current TARGET mining rows, currency,
 * and {@link SessionSourceType#UNATTRIBUTED} (uncertain inventory deltas).
 *
 * One aggregate HUD row only — no per-item breakdown. Analytics keeps the
 * detailed source separation.
 */
final class MiningHudOtherSummary {
    enum Availability {
        ANALYTICS_INACTIVE,
        AVAILABLE
    }

    /** Per-source slice retained so Analytics / reports can still break value down. */
    record SourceSlice(
            long quantity,
            double resolvedGrossValue,
            int unresolvedEntryCount,
            long unresolvedQuantity) {
        SourceSlice {
            quantity = Math.max(0L, quantity);
            resolvedGrossValue = Double.isFinite(resolvedGrossValue)
                    ? Math.max(0.0, resolvedGrossValue)
                    : 0.0;
            unresolvedEntryCount = Math.max(0, unresolvedEntryCount);
            unresolvedQuantity = Math.max(0L, unresolvedQuantity);
        }

        static SourceSlice empty() {
            return new SourceSlice(0L, 0.0, 0, 0L);
        }
    }

    private final Availability availability;
    private final long totalQuantity;
    private final double resolvedGrossValue;
    private final int unresolvedEntryCount;
    private final long unresolvedQuantity;
    private final Map<SessionSourceType, SourceSlice> sourceBreakdown;

    private MiningHudOtherSummary(
            Availability availability,
            long totalQuantity,
            double resolvedGrossValue,
            int unresolvedEntryCount,
            long unresolvedQuantity,
            Map<SessionSourceType, SourceSlice> sourceBreakdown) {
        this.availability = availability;
        this.totalQuantity = Math.max(0L, totalQuantity);
        this.resolvedGrossValue = Double.isFinite(resolvedGrossValue)
                ? Math.max(0.0, resolvedGrossValue)
                : 0.0;
        this.unresolvedEntryCount = Math.max(0, unresolvedEntryCount);
        this.unresolvedQuantity = Math.max(0L, unresolvedQuantity);
        if (sourceBreakdown == null || sourceBreakdown.isEmpty()) {
            this.sourceBreakdown = Map.of();
        } else {
            EnumMap<SessionSourceType, SourceSlice> copy =
                    new EnumMap<>(SessionSourceType.class);
            copy.putAll(sourceBreakdown);
            this.sourceBreakdown = Collections.unmodifiableMap(copy);
        }
    }

    static MiningHudOtherSummary analyticsInactive() {
        return new MiningHudOtherSummary(
                Availability.ANALYTICS_INACTIVE, 0L, 0.0, 0, 0L, Map.of());
    }

    static MiningHudOtherSummary available(
            long totalQuantity,
            double resolvedGrossValue,
            int unresolvedEntryCount) {
        return available(
                totalQuantity,
                resolvedGrossValue,
                unresolvedEntryCount,
                0L,
                Map.of());
    }

    static MiningHudOtherSummary available(
            long totalQuantity,
            double resolvedGrossValue,
            int unresolvedEntryCount,
            long unresolvedQuantity,
            Map<SessionSourceType, SourceSlice> sourceBreakdown) {
        return new MiningHudOtherSummary(
                Availability.AVAILABLE,
                totalQuantity,
                resolvedGrossValue,
                unresolvedEntryCount,
                unresolvedQuantity,
                sourceBreakdown);
    }

    /**
     * Sum item quantities. Callers must already have filtered to eligible
     * HUD OTHERS rows — never pass TARGET, CURRENCY, or UNATTRIBUTED maps
     * unfiltered.
     */
    static long sumQuantities(Collection<Long> quantities) {
        if (quantities == null || quantities.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (Long quantity : quantities) {
            if (quantity == null || quantity <= 0L) {
                continue;
            }
            total = Math.addExact(total, quantity);
        }
        return total;
    }

    static long sumQuantityMap(Map<String, Long> quantities) {
        if (quantities == null || quantities.isEmpty()) {
            return 0L;
        }
        return sumQuantities(quantities.values());
    }

    Availability availability() {
        return availability;
    }

    boolean analyticsActive() {
        return availability == Availability.AVAILABLE;
    }

    long totalQuantity() {
        return totalQuantity;
    }

    double resolvedGrossValue() {
        return resolvedGrossValue;
    }

    int unresolvedEntryCount() {
        return unresolvedEntryCount;
    }

    long unresolvedQuantity() {
        return unresolvedQuantity;
    }

    Map<SessionSourceType, SourceSlice> sourceBreakdown() {
        return sourceBreakdown;
    }

    SourceSlice slice(SessionSourceType source) {
        if (source == null) {
            return SourceSlice.empty();
        }
        return sourceBreakdown.getOrDefault(source, SourceSlice.empty());
    }

    boolean hasUnresolved() {
        return unresolvedEntryCount > 0;
    }

    boolean isEmpty() {
        return availability == Availability.AVAILABLE && totalQuantity == 0L;
    }

    /** True when HUD should render exactly one OTHERS summary row (not per-item). */
    boolean singleSummaryRow() {
        return true;
    }

    /**
     * Compact HUD row label. "Others" — not "Other mined" — because the
     * projection may include mob / chest / reward item gains.
     */
    static String hudRowLabel() {
        return "Others";
    }

    /**
     * Apply bazaar tax to resolved OTHER gross for HUD totals that sit beside
     * target NET estimates. Gross source remains analytics valuation.
     */
    double resolvedNetValue(double taxPercent) {
        return SessionAccounting.afterTax(resolvedGrossValue, taxPercent);
    }

    String valueDisplayCompact(String compactResolved) {
        if (availability != Availability.AVAILABLE) {
            return "—";
        }
        if (hasUnresolved()) {
            return compactResolved + "+";
        }
        return compactResolved;
    }
}
