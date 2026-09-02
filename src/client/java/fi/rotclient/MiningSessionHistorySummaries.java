package fi.rotclient;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Presentation helpers for Session History list/detail. Never mutates live
 * Current Session state and never recomputes frozen valuations.
 */
final class MiningSessionHistorySummaries {
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withZone(ZoneId.systemDefault());

    private MiningSessionHistorySummaries() {
    }

    static String listTitle(MiningSessionHistoryRecord record, int ordinal) {
        if (record == null) {
            return ordinal + ". Session";
        }
        String sessionLabel = record.currentSessionFreeze()
                .map(frozen -> "Session " + frozen.displayNumber() + "  ·  ")
                .orElse("");
        return ordinal + ". " + sessionLabel
                + record.selectedTargetDisplayName()
                + "  ·  FROZEN";
    }

    static String listSubtitle(MiningSessionHistoryRecord record) {
        if (record == null) {
            return "";
        }
        String value = record.resolvedValueAvailable()
                ? MiningSessionPriceBook.formatCoinAmount(
                record.resolvedItemValue())
                : "value unresolved";
        return whenLabel(record)
                + "  ·  "
                + durationLabel(record)
                + "  ·  entries "
                + record.entryCount()
                + "  ·  "
                + value
                + "  ·  "
                + record.parityStatusLabel()
                + (record.mismatchCount() > 0L
                ? " (" + record.mismatchCount() + ")"
                : "");
    }

    static String detailBadge() {
        return "HISTORICAL · FROZEN VALUATION";
    }

    static String whenLabel(MiningSessionHistoryRecord record) {
        long millis = record.sessionStartedMillis().orElse(record.stoppedMillis());
        if (millis <= 0L) {
            return "unknown time";
        }
        return WHEN.format(Instant.ofEpochMilli(millis));
    }

    static String durationLabel(MiningSessionHistoryRecord record) {
        return formatDuration(durationMillis(record), "duration n/a");
    }

    static String pausedDurationLabel(MiningSessionHistoryRecord record) {
        long paused = record == null
                ? 0L
                : record.currentSessionFreeze()
                .map(RotClientSessionFreeze::pausedDurationMillis)
                .orElse(0L);
        return formatDuration(paused, "0s");
    }

    private static String formatDuration(long duration, String emptyLabel) {
        if (duration <= 0L) {
            return emptyLabel;
        }
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.ROOT, "%dh %02dm", hours, minutes);
        }
        if (minutes > 0L) {
            return String.format(Locale.ROOT, "%dm %02ds", minutes, seconds);
        }
        return seconds + "s";
    }

    static long durationMillis(MiningSessionHistoryRecord record) {
        if (record != null && record.currentSessionFreeze().isPresent()) {
            return record.currentSessionFreeze().orElseThrow()
                    .activeDurationMillis();
        }
        if (record == null || record.sessionStartedMillis().isEmpty()) {
            return 0L;
        }
        long start = record.sessionStartedMillis().getAsLong();
        long stop = record.stoppedMillis();
        if (start <= 0L || stop < start) {
            return 0L;
        }
        return stop - start;
    }

    static int sourceRowCount(
            MiningSessionHistoryRecord record,
            SessionSourceType source) {
        if (record == null || source == null
                || record.currentSessionFreeze().isEmpty()) {
            return 0;
        }
        return (int) record.currentSessionFreeze().orElseThrow().itemRows()
                .stream()
                .filter(row -> row.source() == source)
                .count();
    }

    static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            chestAndMobQuantities(MiningSessionHistoryRecord record) {
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                result = new LinkedHashMap<>();
        if (record == null) {
            return result;
        }
        result.putAll(record.chestLootQuantities());
        record.currentSessionFreeze().ifPresent(frozen -> {
            for (RotClientCurrentSessionConfig.SessionItemRecord row
                    : frozen.itemRows()) {
                if (row.source() != SessionSourceType.MOB) {
                    continue;
                }
                result.merge(
                        row.itemId(),
                        new MiningSessionAnalyticsViewModel.ResourceQuantity(
                                row.itemId(), row.displayName(), row.quantity()),
                        (left, right) ->
                                new MiningSessionAnalyticsViewModel
                                        .ResourceQuantity(
                                        left.resourceId(),
                                        left.displayName(),
                                        Math.addExact(
                                                left.quantity(),
                                                right.quantity())));
            }
        });
        return Collections.unmodifiableMap(result);
    }
}
