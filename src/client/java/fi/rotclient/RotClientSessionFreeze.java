package fi.rotclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;

/**
 * Immutable Session History 2.0 freeze of the canonical Current Session.
 *
 * <p>This is an archive boundary, not a second live ledger. Item quantities
 * are copied exactly once from {@link RotClientCurrentSessionConfig}; HUD and
 * Analytics remain read-only projections of the live Current Session.</p>
 */
final class RotClientSessionFreeze {
    enum SourceReadiness {
        /** Complete canonical live ingestion and archive projection. */
        CANONICAL,
        /** Archive schema is ready; live ingestion remains source-gated. */
        SCHEMA_READY,
        /** Deliberately excluded from trusted accounting. */
        EXCLUDED
    }

    private final String sourceSessionIdForDedupe;
    private final int displayNumber;
    private final long startedAtMillis;
    private final long stoppedAtMillis;
    private final long activeDurationMillis;
    private final long pausedDurationMillis;
    private final String selectedTargetId;
    private final List<RotClientCurrentSessionConfig.TargetSegment>
            targetSegments;
    private final List<RotClientCurrentSessionConfig.AreaSegment> areaSegments;
    private final List<RotClientCurrentSessionConfig.SessionItemRecord> itemRows;
    private final long powderChestsOpened;
    private final OptionalLong priceBookObservedAtMillis;

    private RotClientSessionFreeze(
            String sourceSessionIdForDedupe,
            int displayNumber,
            long startedAtMillis,
            long stoppedAtMillis,
            long activeDurationMillis,
            long pausedDurationMillis,
            String selectedTargetId,
            List<RotClientCurrentSessionConfig.TargetSegment> targetSegments,
            List<RotClientCurrentSessionConfig.AreaSegment> areaSegments,
            List<RotClientCurrentSessionConfig.SessionItemRecord> itemRows,
            long powderChestsOpened,
            OptionalLong priceBookObservedAtMillis) {
        this.sourceSessionIdForDedupe = sourceSessionIdForDedupe == null
                ? ""
                : sourceSessionIdForDedupe;
        this.displayNumber = Math.max(1, displayNumber);
        this.startedAtMillis = Math.max(0L, startedAtMillis);
        this.stoppedAtMillis = Math.max(this.startedAtMillis, stoppedAtMillis);
        long elapsed = Math.max(0L, this.stoppedAtMillis - this.startedAtMillis);
        this.pausedDurationMillis = Math.min(
                Math.max(0L, pausedDurationMillis), elapsed);
        this.activeDurationMillis = Math.min(
                Math.max(0L, activeDurationMillis), elapsed);
        this.selectedTargetId = selectedTargetId == null
                ? ""
                : selectedTargetId.trim();
        this.targetSegments = immutableTargetSegments(targetSegments);
        this.areaSegments = immutableAreaSegments(areaSegments);
        this.itemRows = immutableItemRows(itemRows);
        this.powderChestsOpened = Math.max(0L, powderChestsOpened);
        this.priceBookObservedAtMillis = priceBookObservedAtMillis == null
                || priceBookObservedAtMillis.isEmpty()
                || priceBookObservedAtMillis.getAsLong() < 0L
                ? OptionalLong.empty()
                : OptionalLong.of(priceBookObservedAtMillis.getAsLong());
    }

    static RotClientSessionFreeze fromCurrentSession(
            RotClientCurrentSessionConfig source,
            long stoppedAtMillis) {
        RotClientCurrentSessionConfig config = source == null
                ? RotClientCurrentSessionConfig.defaults()
                : source.copy();
        config.normalize();
        long stopped = Math.max(config.startedAtMillis,
                Math.max(0L, stoppedAtMillis));
        OptionalLong priceObserved = config.lastPriceBookObservedAtMillis > 0L
                ? OptionalLong.of(config.lastPriceBookObservedAtMillis)
                : OptionalLong.empty();
        return new RotClientSessionFreeze(
                config.sessionId,
                config.displayNumber,
                config.startedAtMillis,
                stopped,
                RotClientCurrentSessionMath.activeDurationMillis(config, stopped),
                RotClientCurrentSessionMath.pausedDurationMillis(config, stopped),
                config.currentTargetId,
                closeTargetSegments(
                        config.targetSegments,
                        config.startedAtMillis,
                        stopped),
                closeAreaSegments(
                        config.areaSegments,
                        config.startedAtMillis,
                        stopped),
                freezeItemRows(config.items, stopped, priceObserved),
                config.powderChestsOpened,
                priceObserved);
    }

    static RotClientSessionFreeze fromStored(
            int displayNumber,
            long startedAtMillis,
            long stoppedAtMillis,
            long activeDurationMillis,
            long pausedDurationMillis,
            String selectedTargetId,
            List<RotClientCurrentSessionConfig.TargetSegment> targetSegments,
            List<RotClientCurrentSessionConfig.AreaSegment> areaSegments,
            List<RotClientCurrentSessionConfig.SessionItemRecord> itemRows,
            OptionalLong priceBookObservedAtMillis) {
        return fromStored(
                displayNumber,
                startedAtMillis,
                stoppedAtMillis,
                activeDurationMillis,
                pausedDurationMillis,
                selectedTargetId,
                targetSegments,
                areaSegments,
                itemRows,
                0L,
                priceBookObservedAtMillis);
    }

    static RotClientSessionFreeze fromStored(
            int displayNumber,
            long startedAtMillis,
            long stoppedAtMillis,
            long activeDurationMillis,
            long pausedDurationMillis,
            String selectedTargetId,
            List<RotClientCurrentSessionConfig.TargetSegment> targetSegments,
            List<RotClientCurrentSessionConfig.AreaSegment> areaSegments,
            List<RotClientCurrentSessionConfig.SessionItemRecord> itemRows,
            long powderChestsOpened,
            OptionalLong priceBookObservedAtMillis) {
        return new RotClientSessionFreeze(
                "",
                displayNumber,
                startedAtMillis,
                stoppedAtMillis,
                activeDurationMillis,
                pausedDurationMillis,
                selectedTargetId,
                targetSegments,
                areaSegments,
                itemRows,
                powderChestsOpened,
                priceBookObservedAtMillis);
    }

    String sourceSessionIdForDedupe() {
        return sourceSessionIdForDedupe;
    }

    int displayNumber() {
        return displayNumber;
    }

    long startedAtMillis() {
        return startedAtMillis;
    }

    long stoppedAtMillis() {
        return stoppedAtMillis;
    }

    long activeDurationMillis() {
        return activeDurationMillis;
    }

    long pausedDurationMillis() {
        return pausedDurationMillis;
    }

    String selectedTargetId() {
        return selectedTargetId;
    }

    List<RotClientCurrentSessionConfig.TargetSegment> targetSegments() {
        return targetSegments;
    }

    List<RotClientCurrentSessionConfig.AreaSegment> areaSegments() {
        return areaSegments;
    }

    List<RotClientCurrentSessionConfig.SessionItemRecord> itemRows() {
        return itemRows;
    }

    long powderChestsOpened() {
        return powderChestsOpened;
    }

    OptionalLong priceBookObservedAtMillis() {
        return priceBookObservedAtMillis;
    }

    SourceReadiness sourceReadiness(SessionSourceType source) {
        if (source == null || source == SessionSourceType.UNATTRIBUTED) {
            return SourceReadiness.EXCLUDED;
        }
        return switch (source) {
            case MINING, CHEST, CURRENCY -> SourceReadiness.CANONICAL;
            case MOB -> SourceReadiness.SCHEMA_READY;
            case UNATTRIBUTED -> SourceReadiness.EXCLUDED;
        };
    }

    private static List<RotClientCurrentSessionConfig.TargetSegment>
            closeTargetSegments(
                    List<RotClientCurrentSessionConfig.TargetSegment> source,
                    long sessionStartedAtMillis,
                    long stoppedAtMillis) {
        List<RotClientCurrentSessionConfig.TargetSegment> copy =
                new ArrayList<>();
        if (source != null) {
            for (RotClientCurrentSessionConfig.TargetSegment segment : source) {
                if (segment == null) {
                    continue;
                }
                long started = Math.max(
                        sessionStartedAtMillis,
                        Math.min(segment.startedAtMillis(), stoppedAtMillis));
                long ended = segment.isOpen()
                        ? stoppedAtMillis
                        : Math.max(
                        started,
                        Math.min(segment.endedAtMillis(), stoppedAtMillis));
                if (ended > started) {
                    copy.add(new RotClientCurrentSessionConfig.TargetSegment(
                            segment.targetId(), started, ended));
                }
            }
        }
        return copy;
    }

    private static List<RotClientCurrentSessionConfig.AreaSegment>
            closeAreaSegments(
                    List<RotClientCurrentSessionConfig.AreaSegment> source,
                    long sessionStartedAtMillis,
                    long stoppedAtMillis) {
        List<RotClientCurrentSessionConfig.AreaSegment> copy =
                new ArrayList<>();
        if (source != null) {
            for (RotClientCurrentSessionConfig.AreaSegment segment : source) {
                if (segment == null) {
                    continue;
                }
                long started = Math.max(
                        sessionStartedAtMillis,
                        Math.min(segment.startedAtMillis(), stoppedAtMillis));
                long ended = segment.isOpen()
                        ? stoppedAtMillis
                        : Math.max(
                        started,
                        Math.min(segment.endedAtMillis(), stoppedAtMillis));
                if (ended > started) {
                    copy.add(new RotClientCurrentSessionConfig.AreaSegment(
                            segment.areaId(), started, ended));
                }
            }
        }
        return copy;
    }

    private static List<RotClientCurrentSessionConfig.TargetSegment>
            immutableTargetSegments(
                    List<RotClientCurrentSessionConfig.TargetSegment> source) {
        List<RotClientCurrentSessionConfig.TargetSegment> copy =
                new ArrayList<>();
        if (source != null) {
            for (RotClientCurrentSessionConfig.TargetSegment segment : source) {
                if (segment != null) {
                    copy.add(segment.copy());
                }
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<RotClientCurrentSessionConfig.AreaSegment>
            immutableAreaSegments(
                    List<RotClientCurrentSessionConfig.AreaSegment> source) {
        List<RotClientCurrentSessionConfig.AreaSegment> copy =
                new ArrayList<>();
        if (source != null) {
            for (RotClientCurrentSessionConfig.AreaSegment segment : source) {
                if (segment != null) {
                    copy.add(segment.copy());
                }
            }
        }
        return Collections.unmodifiableList(copy);
    }

    private static List<RotClientCurrentSessionConfig.SessionItemRecord>
            freezeItemRows(
                    List<RotClientCurrentSessionConfig.SessionItemRecord> source,
                    long stoppedAtMillis,
                    OptionalLong priceObservedAtMillis) {
        List<RotClientCurrentSessionConfig.SessionItemRecord> result =
                new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (RotClientCurrentSessionConfig.SessionItemRecord row : source) {
            if (row == null || row.quantity() <= 0L) {
                continue;
            }
            RotClientCurrentSessionConfig.PriceStatus status = row.price();
            double value = Double.isFinite(row.resolvedGrossValue())
                    && row.resolvedGrossValue() > 0.0
                    ? java.math.BigDecimal.valueOf(row.resolvedGrossValue())
                    .setScale(
                            MiningSessionPriceBook.MAX_UNIT_PRICE_SCALE,
                            MiningSessionPriceBook.UNIT_PRICE_ROUNDING)
                    .stripTrailingZeros()
                    .doubleValue()
                    : 0.0;
            if (row.source() == SessionSourceType.CURRENCY) {
                status = RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED;
                value = 0.0;
            } else if (status
                    == RotClientCurrentSessionConfig.PriceStatus
                    .RESOLVED_BAZAAR) {
                if (priceObservedAtMillis.isEmpty()
                        || stoppedAtMillis
                        < priceObservedAtMillis.getAsLong()) {
                    status = RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE;
                    value = 0.0;
                } else if (stoppedAtMillis
                        - priceObservedAtMillis.getAsLong()
                        >= MiningSessionPriceBook.STALE_THRESHOLD_MILLIS) {
                    status = RotClientCurrentSessionConfig.PriceStatus.STALE;
                }
            }
            result.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                    row.itemId(),
                    row.displayName(),
                    row.quantity(),
                    row.sourceType(),
                    row.miningClass(),
                    row.areaId(),
                    row.known(),
                    status.name(),
                    value));
        }
        return result;
    }

    private static List<RotClientCurrentSessionConfig.SessionItemRecord>
            immutableItemRows(
                    List<RotClientCurrentSessionConfig.SessionItemRecord> source) {
        List<RotClientCurrentSessionConfig.SessionItemRecord> copy =
                new ArrayList<>();
        if (source != null) {
            for (RotClientCurrentSessionConfig.SessionItemRecord row : source) {
                if (row != null && row.quantity() > 0L) {
                    copy.add(row.copy());
                }
            }
        }
        return Collections.unmodifiableList(copy);
    }
}
