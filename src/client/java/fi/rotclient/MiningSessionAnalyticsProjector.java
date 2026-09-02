package fi.rotclient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Projects an immutable mining-session snapshot into a privacy-safe
 * Session Analytics view model. Performs no networking and mutates nothing.
 *
 * <p>When a Current Session config is supplied, TARGET_MINED and OTHER_MINED
 * quantities and values are taken from that canonical ledger so Analytics,
 * HUD and History share one durable mutation source. Engine-only categories
 * remain projections for ingress channels that are not live yet.
 */
final class MiningSessionAnalyticsProjector {
    MiningSessionAnalyticsViewModel project(
            Optional<MiningSessionSnapshot> snapshot) {
        return project(snapshot, null);
    }

    MiningSessionAnalyticsViewModel project(
            Optional<MiningSessionSnapshot> snapshot,
            RotClientCurrentSessionConfig currentSession) {
        try {
            if (snapshot == null || snapshot.isEmpty()) {
                if (currentSession != null) {
                    return projectCanonicalOnly(currentSession);
                }
                return MiningSessionAnalyticsViewModel.notStarted();
            }
            return project(snapshot.get(), currentSession);
        } catch (RuntimeException ignored) {
            return MiningSessionAnalyticsViewModel.unavailable();
        }
    }

    MiningSessionAnalyticsViewModel project(MiningSessionSnapshot snapshot) {
        return project(snapshot, null);
    }

    MiningSessionAnalyticsViewModel project(
            MiningSessionSnapshot snapshot,
            RotClientCurrentSessionConfig currentSession) {
        try {
            if (snapshot == null) {
                if (currentSession != null) {
                    return projectCanonicalOnly(currentSession);
                }
                return MiningSessionAnalyticsViewModel.notStarted();
            }
            return projectChecked(snapshot, currentSession);
        } catch (RuntimeException ignored) {
            return MiningSessionAnalyticsViewModel.unavailable();
        }
    }

    private MiningSessionAnalyticsViewModel projectChecked(
            MiningSessionSnapshot snapshot,
            RotClientCurrentSessionConfig currentSession) {
        MiningSessionAnalyticsViewModel.SessionState state =
                currentSession == null
                        ? resolveState(snapshot)
                        : (currentSession.isPaused()
                        ? MiningSessionAnalyticsViewModel.SessionState.STOPPED
                        : MiningSessionAnalyticsViewModel.SessionState.ACTIVE);
        MiningSessionValuation valuation = snapshot.valuation();
        if (valuation == null) {
            valuation = MiningSessionValuation.unavailable();
        }

        long canonicalStarted = currentSession == null
                ? snapshot.sessionStartMillis()
                : currentSession.startedAtMillis;
        OptionalLong started = canonicalStarted > 0L
                ? OptionalLong.of(canonicalStarted)
                : OptionalLong.empty();
        OptionalLong snapshotAt = snapshot.snapshotAtMillis() > 0L
                ? OptionalLong.of(snapshot.snapshotAtMillis())
                : OptionalLong.empty();
        long canonicalStopped = currentSession == null
                ? snapshot.snapshotAtMillis()
                : Math.max(
                currentSession.pausedAtMillis,
                currentSession.startedAtMillis);
        OptionalLong stopped = state
                == MiningSessionAnalyticsViewModel.SessionState.STOPPED
                && canonicalStopped > 0L
                ? OptionalLong.of(canonicalStopped)
                : OptionalLong.empty();

        boolean priceBookAvailable = valuation.priceBookAvailable();
        OptionalLong priceBookAge = valuation.oldestQuoteAgeMillis();
        boolean priceBookStale = valuation.staleEntryCount() > 0
                || (priceBookAge.isPresent()
                && priceBookAge.getAsLong()
                >= MiningSessionPriceBook.STALE_THRESHOLD_MILLIS);

        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> target =
                currentSession == null
                        ? projectQuantities(snapshot.quantities(
                        MiningSessionCategory.TARGET_MINED))
                        : projectTargetFromCurrentSession(currentSession);
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> other;
        BigDecimal otherValue;
        int otherUnresolved;
        if (currentSession != null) {
            other = projectOtherFromCurrentSession(currentSession);
            otherValue = BigDecimal.valueOf(
                    RotClientCurrentSessionMath.sumOtherMinedResolvedValue(
                            currentSession.items));
            otherUnresolved = RotClientCurrentSessionMath.countOtherUnresolved(
                    currentSession.items);
        } else {
            other = projectQuantities(
                    snapshot.quantities(
                            MiningSessionCategory.OTHER_MINED));
            otherValue = categoryValue(
                    valuation,
                    MiningSessionCategory.OTHER_MINED);
            otherUnresolved = valuation.unresolvedEntryCount(
                    MiningSessionCategory.OTHER_MINED);
        }

        int otherEntryCount = currentSession == null
                ? other.size()
                : RotClientCurrentSessionMath.countOtherMinedRows(
                currentSession.items);
        int targetEntryCount = currentSession == null
                ? target.size()
                : RotClientCurrentSessionMath.countTargetMinedRows(
                currentSession.items);
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> chest =
                projectQuantities(
                        snapshot.quantities(
                                MiningSessionCategory.CHEST_LOOT));
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> currency =
                mergeNativeCoins(
                        projectQuantities(
                                snapshot.quantities(
                                        MiningSessionCategory.CURRENCY)),
                        currentSession);
        int mobRows = currentSession == null
                ? 0
                : RotClientCurrentSessionMath.countMobRows(
                currentSession.items);
        int nativeCoinRows = currentSession == null
                ? 0
                : RotClientCurrentSessionMath.quantitiesForNativeCoins(
                currentSession.items).size();
        int entryCount = targetEntryCount
                + otherEntryCount
                + chest.size()
                + currency.size()
                + Math.max(0, mobRows - nativeCoinRows);

        BigDecimal targetValue = currentSession == null
                ? categoryValue(valuation, MiningSessionCategory.TARGET_MINED)
                : BigDecimal.valueOf(
                RotClientCurrentSessionMath.sumTargetMinedResolvedValue(
                        currentSession.items));
        BigDecimal chestValue = categoryValue(
                valuation,
                MiningSessionCategory.CHEST_LOOT);
        BigDecimal mobValue = currentSession == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(
                RotClientCurrentSessionMath.sumMobResolvedValue(
                        currentSession.items));
        BigDecimal resolvedTotal = targetValue.add(otherValue)
                .add(chestValue)
                .add(mobValue);
        boolean resolvedAvailable = resolvedTotal.signum() > 0;
        int unresolvedEntries = currentSession == null
                ? valuation.unresolvedEntryCount()
                : RotClientCurrentSessionMath.countTargetUnresolved(
                currentSession.items)
                + otherUnresolved
                + valuation.unresolvedEntryCount(
                MiningSessionCategory.CHEST_LOOT)
                + RotClientCurrentSessionMath.countMobUnresolved(
                currentSession.items);
        int resolvedEntries = currentSession == null
                ? valuation.resolvedEntryCount()
                : Math.max(0, targetEntryCount
                - RotClientCurrentSessionMath.countTargetUnresolved(
                currentSession.items))
                + Math.max(0, otherEntryCount - otherUnresolved)
                + Math.max(0, chest.size()
                - valuation.unresolvedEntryCount(
                MiningSessionCategory.CHEST_LOOT))
                + Math.max(0, mobRows
                - RotClientCurrentSessionMath.countMobUnresolved(
                currentSession.items));

        return MiningSessionAnalyticsViewModel.create(
                state,
                currentSession == null
                        ? safeTargetName(snapshot.selectedTracker())
                        : safeCurrentTargetName(currentSession),
                snapshot.trackerEnabled(),
                started,
                snapshotAt,
                stopped,
                entryCount,
                targetEntryCount,
                otherEntryCount,
                chest.size(),
                currency.size(),
                parityLabel(snapshot.targetParityStatus()),
                snapshot.parityMismatchCount(),
                valuation.priceBasisLabel(),
                resolvedAvailable,
                nullToZero(resolvedTotal),
                Math.max(0, resolvedEntries),
                unresolvedEntries,
                valuation.staleEntryCount(),
                valuation.unavailableEntryCount(),
                valuation.unsupportedEntryCount(),
                valuation.excludedCurrencyEntryCount(),
                targetValue,
                nullToZero(otherValue),
                chestValue,
                priceBookAge,
                priceBookAvailable,
                priceBookStale,
                target,
                other,
                chest,
                currency);
    }

    private MiningSessionAnalyticsViewModel projectCanonicalOnly(
            RotClientCurrentSessionConfig currentSession) {
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> target =
                projectTargetFromCurrentSession(currentSession);
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> other =
                projectOtherFromCurrentSession(currentSession);
        BigDecimal targetValue = BigDecimal.valueOf(
                RotClientCurrentSessionMath.sumTargetMinedResolvedValue(
                        currentSession.items));
        BigDecimal otherValue = BigDecimal.valueOf(
                RotClientCurrentSessionMath.sumOtherMinedResolvedValue(
                        currentSession.items));
        int unresolved = RotClientCurrentSessionMath.countOtherUnresolved(
                currentSession.items);
        int targetEntryCount =
                RotClientCurrentSessionMath.countTargetMinedRows(
                        currentSession.items);
        int otherEntryCount =
                RotClientCurrentSessionMath.countOtherMinedRows(
                        currentSession.items);
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> currency =
                mergeNativeCoins(Map.of(), currentSession);
        int mobRows = RotClientCurrentSessionMath.countMobRows(
                currentSession.items);
        int nativeCoinRows = RotClientCurrentSessionMath
                .quantitiesForNativeCoins(currentSession.items)
                .size();
        BigDecimal mobValue = BigDecimal.valueOf(
                RotClientCurrentSessionMath.sumMobResolvedValue(
                        currentSession.items));
        int mobUnresolved = RotClientCurrentSessionMath.countMobUnresolved(
                currentSession.items);
        MiningSessionAnalyticsViewModel.SessionState state =
                currentSession.isPaused()
                        ? MiningSessionAnalyticsViewModel.SessionState.STOPPED
                        : MiningSessionAnalyticsViewModel.SessionState.ACTIVE;
        OptionalLong started = currentSession.startedAtMillis > 0L
                ? OptionalLong.of(currentSession.startedAtMillis)
                : OptionalLong.empty();
        long projectedAt = currentSession.isPaused()
                ? Math.max(
                currentSession.pausedAtMillis,
                currentSession.startedAtMillis)
                : Math.max(
                currentSession.lastHeartbeatMillis,
                currentSession.startedAtMillis);
        return MiningSessionAnalyticsViewModel.create(
                state,
                safeCurrentTargetName(currentSession),
                false,
                started,
                projectedAt > 0L
                        ? OptionalLong.of(projectedAt)
                        : OptionalLong.empty(),
                state == MiningSessionAnalyticsViewModel.SessionState.STOPPED
                        ? OptionalLong.of(Math.max(
                        currentSession.pausedAtMillis,
                        currentSession.startedAtMillis))
                        : OptionalLong.empty(),
                targetEntryCount + otherEntryCount
                        + currency.size()
                        + Math.max(0, mobRows - nativeCoinRows),
                targetEntryCount,
                otherEntryCount,
                0,
                currency.size(),
                "NOT_CHECKED",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                targetValue.add(otherValue).add(mobValue).signum() > 0,
                targetValue.add(otherValue).add(mobValue),
                Math.max(0, targetEntryCount
                        - RotClientCurrentSessionMath.countTargetUnresolved(
                        currentSession.items))
                        + Math.max(0, otherEntryCount - unresolved)
                        + Math.max(0, mobRows - mobUnresolved),
                RotClientCurrentSessionMath.countTargetUnresolved(
                        currentSession.items) + unresolved + mobUnresolved,
                0,
                0,
                0,
                0,
                targetValue,
                otherValue,
                BigDecimal.ZERO,
                OptionalLong.empty(),
                otherValue.signum() > 0,
                false,
                target,
                other,
                Map.of(),
                currency);
    }

    private static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            projectTargetFromCurrentSession(
                    RotClientCurrentSessionConfig currentSession) {
        return projectCurrentSessionQuantities(
                currentSession == null
                        ? Map.of()
                        : RotClientCurrentSessionMath.quantitiesForTargetMined(
                        currentSession.items));
    }

    private static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            projectOtherFromCurrentSession(
                    RotClientCurrentSessionConfig currentSession) {
        return projectCurrentSessionQuantities(
                currentSession == null
                        ? Map.of()
                        : RotClientCurrentSessionMath.quantitiesForOtherMined(
                        currentSession.items));
    }

    private static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            projectCurrentSessionQuantities(
                    Map<String, RotClientCurrentSessionMath.ProjectedQuantity>
                            quantities) {
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                ordered = new LinkedHashMap<>();
        if (quantities == null) {
            return ordered;
        }
        for (RotClientCurrentSessionMath.ProjectedQuantity qty
                : quantities.values()) {
            if (qty.quantity() <= 0L) {
                continue;
            }
            ordered.put(
                    qty.itemId(),
                    new MiningSessionAnalyticsViewModel.ResourceQuantity(
                            qty.itemId(),
                            qty.displayName(),
                            qty.quantity()));
        }
        return ordered;
    }

    private static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            mergeNativeCoins(
                    Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                            currency,
                    RotClientCurrentSessionConfig currentSession) {
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                merged = new LinkedHashMap<>();
        if (currency != null) {
            merged.putAll(currency);
        }
        if (currentSession == null) {
            return merged;
        }
        for (RotClientCurrentSessionMath.ProjectedQuantity qty
                : RotClientCurrentSessionMath.quantitiesForNativeCoins(
                currentSession.items).values()) {
            if (qty.quantity() <= 0L || qty.itemId().isBlank()) {
                continue;
            }
            merged.merge(
                    qty.itemId(),
                    new MiningSessionAnalyticsViewModel.ResourceQuantity(
                            qty.itemId(),
                            qty.displayName(),
                            qty.quantity()),
                    (left, right) ->
                            new MiningSessionAnalyticsViewModel
                                    .ResourceQuantity(
                                    left.resourceId(),
                                    left.displayName(),
                                    Math.addExact(
                                            left.quantity(),
                                            right.quantity())));
        }
        return merged;
    }

    private static String safeCurrentTargetName(
            RotClientCurrentSessionConfig currentSession) {
        if (currentSession == null
                || currentSession.currentTargetId == null
                || currentSession.currentTargetId.isBlank()) {
            return "Current Session";
        }
        return currentSession.currentTargetId;
    }

    private static MiningSessionAnalyticsViewModel.SessionState resolveState(
            MiningSessionSnapshot snapshot) {
        if (snapshot.diagnosticsActive()) {
            return MiningSessionAnalyticsViewModel.SessionState.ACTIVE;
        }
        if (snapshot.sessionId() > 0L
                || snapshot.entryCount() > 0
                || snapshot.snapshotAtMillis() > 0L) {
            return MiningSessionAnalyticsViewModel.SessionState.STOPPED;
        }
        return MiningSessionAnalyticsViewModel.SessionState.NOT_STARTED;
    }

    private static String safeTargetName(TrackerSelection selection) {
        if (selection == null) {
            return "unavailable";
        }
        String displayName = selection.displayName();
        if (displayName == null || displayName.isBlank()) {
            return selection.id();
        }
        return displayName;
    }

    private static String parityLabel(MiningSessionParity.Status status) {
        if (status == null) {
            return "unavailable";
        }
        return switch (status) {
            case MATCH -> "MATCH";
            case MISMATCH -> "MISMATCH";
            case NOT_CHECKED -> "NOT_CHECKED";
            case INCOMPARABLE -> "INCOMPARABLE";
        };
    }

    private static BigDecimal categoryValue(
            MiningSessionValuation valuation,
            MiningSessionCategory category) {
        try {
            return nullToZero(valuation.valueByCategory(category));
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            projectQuantities(
                    Map<MiningSessionResource, Long> quantities) {
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                ordered = new LinkedHashMap<>();
        if (quantities == null || quantities.isEmpty()) {
            return ordered;
        }

        List<Map.Entry<MiningSessionResource, Long>> entries =
                new ArrayList<>(quantities.entrySet());
        entries.sort(Comparator.comparing(
                entry -> CurrentSessionCanonicalIds.canonicalItemId(
                        entry.getKey()),
                String.CASE_INSENSITIVE_ORDER));

        for (Map.Entry<MiningSessionResource, Long> entry : entries) {
            MiningSessionResource resource = entry.getKey();
            Long quantity = entry.getValue();
            if (resource == null || quantity == null || quantity <= 0L) {
                continue;
            }
            String resourceId =
                    CurrentSessionCanonicalIds.canonicalItemId(resource);
            String displayName = resource.displayName();
            if (displayName == null || displayName.isBlank()) {
                displayName = resourceId;
            }
            ordered.merge(
                    resourceId,
                    new MiningSessionAnalyticsViewModel.ResourceQuantity(
                            resourceId,
                            displayName,
                            quantity),
                    (left, right) -> new MiningSessionAnalyticsViewModel
                            .ResourceQuantity(
                            left.resourceId(),
                            left.displayName(),
                            Math.addExact(left.quantity(), right.quantity())));
        }
        return ordered;
    }
}
