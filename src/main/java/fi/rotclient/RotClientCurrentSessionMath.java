package fi.rotclient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure helpers for Current Session aggregates, HUD summaries, and segment
 * lifecycle. No Minecraft / disk I/O dependencies.
 */
final class RotClientCurrentSessionMath {
    private RotClientCurrentSessionMath() {
    }

    /**
     * OTHER_MINED-only quantity (MINING + OTHER). Analytics source breakdown
     * uses this; the HUD OTHERS projection is broader — see
     * {@link #sumHudOthersQuantity}.
     */
    static long sumOtherMinedQuantity(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return sumQuantityWhere(items, RotClientCurrentSessionMath::isOtherMined);
    }

    static double sumOtherMinedResolvedValue(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return sumResolvedValueWhere(
                items, RotClientCurrentSessionMath::isOtherMined);
    }

    static int countOtherUnresolved(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return countUnresolvedWhere(
                items, RotClientCurrentSessionMath::isOtherMined);
    }

    static int countOtherMinedRows(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return countRowsWhere(items, RotClientCurrentSessionMath::isOtherMined);
    }

    static double sumTargetMinedResolvedValue(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return sumResolvedValueWhere(
                items, RotClientCurrentSessionMath::isTargetMined);
    }

    static int countTargetUnresolved(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return countUnresolvedWhere(
                items, RotClientCurrentSessionMath::isTargetMined);
    }

    static int countTargetMinedRows(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return countRowsWhere(items, RotClientCurrentSessionMath::isTargetMined);
    }

    /** Item units eligible for the HUD OTHERS projection. */
    static long sumHudOthersQuantity(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return sumQuantityWhere(
                items, RotClientCurrentSessionMath::isEligibleHudOthers);
    }

    static double sumHudOthersResolvedValue(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return sumResolvedValueWhere(
                items, RotClientCurrentSessionMath::isEligibleHudOthers);
    }

    static int countHudOthersUnresolved(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return countUnresolvedWhere(
                items, RotClientCurrentSessionMath::isEligibleHudOthers);
    }

    static long sumHudOthersUnresolvedQuantity(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null || !isEligibleHudOthers(item)) {
                continue;
            }
            if (item.price()
                    != RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR) {
                total = Math.addExact(total, Math.max(0L, item.quantity()));
            }
        }
        return total;
    }

    /**
     * Per-source slices for Analytics / reports. Keys are only sources that
     * contribute to HUD OTHERS (MINING/OTHER, MOB, CHEST). Never mutates items.
     */
    static Map<SessionSourceType, MiningHudOtherSummary.SourceSlice>
            hudOthersSourceBreakdown(
                    List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        EnumMap<SessionSourceType, MutableSlice> scratch =
                new EnumMap<>(SessionSourceType.class);
        if (items != null) {
            for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
                if (item == null || !isEligibleHudOthers(item)) {
                    continue;
                }
                SessionSourceType source = item.source();
                MutableSlice slice = scratch.computeIfAbsent(
                        source, ignored -> new MutableSlice());
                long qty = Math.max(0L, item.quantity());
                slice.quantity = Math.addExact(slice.quantity, qty);
                if (item.price()
                        == RotClientCurrentSessionConfig.PriceStatus
                        .RESOLVED_BAZAAR) {
                    double value = item.resolvedGrossValue();
                    if (Double.isFinite(value) && value > 0.0) {
                        slice.resolvedGrossValue += value;
                    }
                } else {
                    slice.unresolvedEntryCount++;
                    slice.unresolvedQuantity =
                            Math.addExact(slice.unresolvedQuantity, qty);
                }
            }
        }
        EnumMap<SessionSourceType, MiningHudOtherSummary.SourceSlice> result =
                new EnumMap<>(SessionSourceType.class);
        for (Map.Entry<SessionSourceType, MutableSlice> entry
                : scratch.entrySet()) {
            MutableSlice slice = entry.getValue();
            result.put(
                    entry.getKey(),
                    new MiningHudOtherSummary.SourceSlice(
                            slice.quantity,
                            slice.resolvedGrossValue,
                            slice.unresolvedEntryCount,
                            slice.unresolvedQuantity));
        }
        return result;
    }

    /**
     * HUD OTHERS projection from the Current Session snapshot. Read-only —
     * never mutates config / items. Always AVAILABLE when a session exists.
     */
    static MiningHudOtherSummary toHudOtherSummary(
            RotClientCurrentSessionConfig config) {
        if (config == null) {
            return MiningHudOtherSummary.available(0L, 0.0, 0);
        }
        List<RotClientCurrentSessionConfig.SessionItemRecord> items =
                config.items;
        return MiningHudOtherSummary.available(
                sumHudOthersQuantity(items),
                sumHudOthersResolvedValue(items),
                countHudOthersUnresolved(items),
                sumHudOthersUnresolvedQuantity(items),
                hudOthersSourceBreakdown(items));
    }

    /**
     * Read-only projection of Current Session item rows for one source type.
     * Used by Session Analytics detailed breakdown (not the HUD OTHERS rollup).
     */
    static Map<String, ProjectedQuantity> quantitiesForSource(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items,
            SessionSourceType source) {
        LinkedHashMap<String, ProjectedQuantity> result = new LinkedHashMap<>();
        if (items == null || source == null) {
            return result;
        }
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null || item.quantity() <= 0L || item.source() != source) {
                continue;
            }
            if (source == SessionSourceType.MINING
                    && item.miningClassification() == MiningClassification.TARGET) {
                // Caller wanting OTHER_MINED should use quantitiesForOtherMined.
                continue;
            }
            result.merge(
                    item.itemId(),
                    new ProjectedQuantity(
                            item.itemId(),
                            item.displayName(),
                            item.quantity()),
                    (left, right) -> new ProjectedQuantity(
                            left.itemId(),
                            left.displayName(),
                            Math.addExact(left.quantity(), right.quantity())));
        }
        return result;
    }

    static int countMobRows(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return countRowsWhere(items, RotClientCurrentSessionMath::isMobLoot);
    }

    static int countMobUnresolved(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return countUnresolvedWhere(
                items, RotClientCurrentSessionMath::isMobLoot);
    }

    static double sumMobResolvedValue(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        return sumResolvedValueWhere(
                items, RotClientCurrentSessionMath::isMobLoot);
    }

    static Map<String, ProjectedQuantity> quantitiesForNativeCoins(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        LinkedHashMap<String, ProjectedQuantity> result = new LinkedHashMap<>();
        if (items == null) {
            return result;
        }
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (!isNativeCoins(item) || item.quantity() <= 0L) {
                continue;
            }
            result.merge(
                    item.itemId(),
                    new ProjectedQuantity(
                            item.itemId(),
                            item.displayName().isBlank()
                                    ? "Coins"
                                    : item.displayName(),
                            item.quantity()),
                    (left, right) -> new ProjectedQuantity(
                            left.itemId(),
                            left.displayName(),
                            Math.addExact(left.quantity(), right.quantity())));
        }
        return result;
    }

    static boolean isNativeCoins(
            RotClientCurrentSessionConfig.SessionItemRecord item) {
        return item != null
                && item.quantity() > 0L
                && "COINS".equals(item.itemId());
    }

    private static boolean isMobLoot(
            RotClientCurrentSessionConfig.SessionItemRecord item) {
        return item != null
                && item.quantity() > 0L
                && item.source() == SessionSourceType.MOB;
    }

    /** OTHER_MINED rows only (MINING + OTHER). */
    static Map<String, ProjectedQuantity> quantitiesForOtherMined(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        LinkedHashMap<String, ProjectedQuantity> result = new LinkedHashMap<>();
        if (items == null) {
            return result;
        }
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null || !isOtherMined(item)) {
                continue;
            }
            result.merge(
                    item.itemId(),
                    new ProjectedQuantity(
                            item.itemId(),
                            item.displayName(),
                            item.quantity()),
                    (left, right) -> new ProjectedQuantity(
                            left.itemId(),
                            left.displayName(),
                            Math.addExact(left.quantity(), right.quantity())));
        }
        return result;
    }

    /** TARGET_MINED rows only (MINING + TARGET). */
    static Map<String, ProjectedQuantity> quantitiesForTargetMined(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items) {
        LinkedHashMap<String, ProjectedQuantity> result = new LinkedHashMap<>();
        if (items == null) {
            return result;
        }
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null
                    || item.source() != SessionSourceType.MINING
                    || item.miningClassification() != MiningClassification.TARGET
                    || item.quantity() <= 0L) {
                continue;
            }
            result.merge(
                    item.itemId(),
                    new ProjectedQuantity(
                            item.itemId(),
                            item.displayName(),
                            item.quantity()),
                    (left, right) -> new ProjectedQuantity(
                            left.itemId(),
                            left.displayName(),
                            Math.addExact(left.quantity(), right.quantity())));
        }
        return result;
    }

    private static boolean isTargetMined(
            RotClientCurrentSessionConfig.SessionItemRecord item) {
        return item != null
                && item.quantity() > 0L
                && item.source() == SessionSourceType.MINING
                && item.miningClassification() == MiningClassification.TARGET;
    }

    record ProjectedQuantity(String itemId, String displayName, long quantity) {
        ProjectedQuantity {
            itemId = itemId == null ? "" : itemId;
            displayName = displayName == null || displayName.isBlank()
                    ? itemId
                    : displayName;
            quantity = Math.max(0L, quantity);
        }
    }

    static List<RotClientCurrentSessionConfig.TargetSegment> closeSegmentAndOpen(
            List<RotClientCurrentSessionConfig.TargetSegment> segments,
            String newTargetId,
            long now) {
        long safeNow = Math.max(0L, now);
        String target = newTargetId == null ? "" : newTargetId.trim();
        List<RotClientCurrentSessionConfig.TargetSegment> next =
                new ArrayList<>();
        if (segments != null) {
            for (RotClientCurrentSessionConfig.TargetSegment segment
                    : segments) {
                if (segment == null) {
                    continue;
                }
                if (segment.isOpen()) {
                    if (safeNow > segment.startedAtMillis()) {
                        next.add(new RotClientCurrentSessionConfig.TargetSegment(
                                segment.targetId(),
                                segment.startedAtMillis(),
                                safeNow));
                    }
                } else {
                    next.add(segment.copy());
                }
            }
        }
        if (!target.isBlank()) {
            next.add(new RotClientCurrentSessionConfig.TargetSegment(
                    target, safeNow, 0L));
        }
        return next;
    }

    static List<RotClientCurrentSessionConfig.AreaSegment>
            closeAreaSegmentAndOpen(
                    List<RotClientCurrentSessionConfig.AreaSegment> segments,
                    String newAreaId,
                    long now) {
        long safeNow = Math.max(0L, now);
        String areaId = SkyBlockArea.fromId(newAreaId).id();
        List<RotClientCurrentSessionConfig.AreaSegment> next =
                new ArrayList<>();
        if (segments != null) {
            for (RotClientCurrentSessionConfig.AreaSegment segment : segments) {
                if (segment == null) {
                    continue;
                }
                if (segment.isOpen()) {
                    if (safeNow > segment.startedAtMillis()) {
                        next.add(new RotClientCurrentSessionConfig.AreaSegment(
                                segment.areaId(),
                                segment.startedAtMillis(),
                                safeNow));
                    }
                } else {
                    next.add(segment.copy());
                }
            }
        }
        if (!SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id().equals(areaId)) {
            next.add(new RotClientCurrentSessionConfig.AreaSegment(
                    areaId, safeNow, 0L));
        }
        return next;
    }

    /**
     * Wall-clock session age minus every paused interval (closed and, if
     * currently paused, in-progress). Never negative, even with clock skew
     * or corrupt persisted timestamps.
     */
    static long activeDurationMillis(
            RotClientCurrentSessionConfig config,
            long now) {
        if (config == null) {
            return 0L;
        }
        long safeNow = Math.max(0L, now);
        long elapsed = Math.max(0L, safeNow - config.startedAtMillis);
        long paused = Math.max(0L, config.totalPausedMillis);
        if (config.isPaused() && config.pausedAtMillis > 0L) {
            paused = Math.addExact(
                    paused,
                    Math.max(0L, safeNow - config.pausedAtMillis));
        }
        return Math.max(0L, elapsed - paused);
    }

    static long pausedDurationMillis(
            RotClientCurrentSessionConfig config,
            long now) {
        if (config == null) {
            return 0L;
        }
        long safeNow = Math.max(0L, now);
        long paused = Math.max(0L, config.totalPausedMillis);
        if (config.isPaused() && config.pausedAtMillis > 0L) {
            paused = Math.addExact(
                    paused,
                    Math.max(0L, safeNow - config.pausedAtMillis));
        }
        long elapsed = Math.max(0L, safeNow - config.startedAtMillis);
        return Math.min(paused, elapsed);
    }

    /** Heartbeat interval for unclean-shutdown recovery (milliseconds). */
    static final long HEARTBEAT_INTERVAL_MILLIS = 5_000L;

    /**
     * If the previous process did not cleanly pause/shutdown, close open
     * activity at the last reliable heartbeat so offline wall-clock is not
     * counted as active mining time. Mutates and returns {@code config}.
     */
    static RotClientCurrentSessionConfig recoverUncleanShutdown(
            RotClientCurrentSessionConfig config) {
        if (config == null) {
            return RotClientCurrentSessionConfig.defaults();
        }
        config.normalize();
        if (config.cleanShutdown) {
            if (config.isPaused()) {
                closeActivitySegmentsAt(
                        config,
                        Math.max(
                                config.startedAtMillis,
                                config.pausedAtMillis));
            }
            return config;
        }
        long reliable = config.lastHeartbeatMillis > 0L
                ? config.lastHeartbeatMillis
                : config.startedAtMillis;
        reliable = Math.max(reliable, config.startedAtMillis);
        if (config.isActive()) {
            config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
            config.pausedAtMillis = reliable;
            config.offlineAutoPaused = true;
        } else if (config.isPaused()) {
            if (config.pausedAtMillis <= 0L || config.pausedAtMillis > reliable) {
                config.pausedAtMillis = reliable;
            }
        }
        long pauseBoundary = Math.max(
                config.startedAtMillis,
                config.pausedAtMillis);
        closeActivitySegmentsAt(config, pauseBoundary);
        config.cleanShutdown = true;
        return config;
    }

    private static void closeActivitySegmentsAt(
            RotClientCurrentSessionConfig config,
            long boundaryMillis) {
        config.targetSegments = closeSegmentAndOpen(
                config.targetSegments, "", boundaryMillis);
        config.areaSegments = closeAreaSegmentAndOpen(
                config.areaSegments,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(),
                boundaryMillis);
    }

    static RotClientCurrentSessionConfig ensureSession1IfMissing(
            RotClientCurrentSessionConfig config) {
        if (config == null) {
            return RotClientCurrentSessionConfig.defaults();
        }
        config.normalize();
        if (config.sessionId == null || config.sessionId.isBlank()) {
            config.sessionId = UUID.randomUUID().toString();
        }
        if (config.displayNumber < 1) {
            config.displayNumber = 1;
        }
        if (config.nextDisplayNumber <= config.displayNumber) {
            config.nextDisplayNumber = config.displayNumber + 1;
        }
        return config;
    }

    /**
     * Archives the current session identity and opens a fresh empty session
     * with an incremented display number. Caller is responsible for history
     * persistence of the prior snapshot.
     */
    static RotClientCurrentSessionConfig archiveAndStartNext(
            RotClientCurrentSessionConfig current,
            long now) {
        RotClientCurrentSessionConfig prior = current == null
                ? RotClientCurrentSessionConfig.defaults()
                : current.copy();
        prior.normalize();
        int nextNumber = RotClientCurrentSessionConfig.nextDisplayNumberAfter(
                prior.displayNumber);
        if (prior.nextDisplayNumber > nextNumber) {
            nextNumber = prior.nextDisplayNumber;
        }
        RotClientCurrentSessionConfig next =
                RotClientCurrentSessionConfig.defaults();
        next.startedAtMillis = Math.max(0L, now);
        next.displayNumber = nextNumber;
        next.nextDisplayNumber = nextNumber + 1;
        next.state = RotClientCurrentSessionConfig.STATE_ACTIVE;
        next.pausedAtMillis = 0L;
        next.totalPausedMillis = 0L;
        next.offlineAutoPaused = false;
        next.lastHeartbeatMillis = Math.max(0L, now);
        next.cleanShutdown = false;
        next.currentTargetId = prior.currentTargetId;
        if (next.currentTargetId != null && !next.currentTargetId.isBlank()) {
            next.targetSegments = List.of(
                    new RotClientCurrentSessionConfig.TargetSegment(
                            next.currentTargetId,
                            next.startedAtMillis,
                            0L));
        }
        next.currentAreaId = prior.currentAreaId;
        if (next.currentAreaId != null
                && !next.currentAreaId.isBlank()
                && !SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id()
                .equals(next.currentAreaId)) {
            next.areaSegments = List.of(
                    new RotClientCurrentSessionConfig.AreaSegment(
                            next.currentAreaId,
                            next.startedAtMillis,
                            0L));
            next.areasVisited = List.of(next.currentAreaId);
        }
        next.lastPriceBookObservedAtMillis = 0L;
        next.normalize();
        return next;
    }

    /**
     * Eligible for the HUD OTHERS projection: confidently attributed
     * non-target item gains. Currency and unattributed inventory noise are
     * excluded. Classification is whatever was stored at observation time —
     * target switches never reclassify history.
     */
    static boolean isEligibleHudOthers(
            RotClientCurrentSessionConfig.SessionItemRecord item) {
        if (item == null || item.quantity() <= 0L) {
            return false;
        }
        SessionSourceType source = item.source();
        return switch (source) {
            case MINING -> item.miningClassification()
                    == MiningClassification.OTHER;
            case MOB -> !isNativeCoins(item);
            case CHEST -> true;
            case CURRENCY, UNATTRIBUTED -> false;
        };
    }

    /** Analytics OTHER_MINED-only predicate (narrower than HUD OTHERS). */
    static boolean isOtherMined(
            RotClientCurrentSessionConfig.SessionItemRecord item) {
        return item != null
                && item.source() == SessionSourceType.MINING
                && item.miningClassification() == MiningClassification.OTHER;
    }

    private static long sumQuantityWhere(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items,
            java.util.function.Predicate<
                    RotClientCurrentSessionConfig.SessionItemRecord> predicate) {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null || !predicate.test(item)) {
                continue;
            }
            total = Math.addExact(total, Math.max(0L, item.quantity()));
        }
        return total;
    }

    private static double sumResolvedValueWhere(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items,
            java.util.function.Predicate<
                    RotClientCurrentSessionConfig.SessionItemRecord> predicate) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null || !predicate.test(item)) {
                continue;
            }
            if (item.price()
                    != RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR) {
                continue;
            }
            double value = item.resolvedGrossValue();
            if (Double.isFinite(value) && value > 0.0) {
                total += value;
            }
        }
        return total;
    }

    private static int countUnresolvedWhere(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items,
            java.util.function.Predicate<
                    RotClientCurrentSessionConfig.SessionItemRecord> predicate) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item == null || !predicate.test(item)) {
                continue;
            }
            if (item.price()
                    != RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR) {
                count++;
            }
        }
        return count;
    }

    private static int countRowsWhere(
            List<RotClientCurrentSessionConfig.SessionItemRecord> items,
            java.util.function.Predicate<
                    RotClientCurrentSessionConfig.SessionItemRecord> predicate) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (RotClientCurrentSessionConfig.SessionItemRecord item : items) {
            if (item != null && predicate.test(item)) {
                count++;
            }
        }
        return count;
    }

    private static final class MutableSlice {
        long quantity;
        double resolvedGrossValue;
        int unresolvedEntryCount;
        long unresolvedQuantity;
    }
}
