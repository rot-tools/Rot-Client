package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Opt-in runtime timing for named client boundaries.
 *
 * <p>The profiler is disabled by default. When disabled, callers pay only the
 * enabled check. Samples are aggregate timings only; no player, server, item,
 * chat, or session data is retained.
 */
public final class ClientPerformanceProfiler {
    private static final Map<String, MutableStats> STATS = new HashMap<>();

    private static volatile boolean enabled;
    private static long startedAtNanos;

    private ClientPerformanceProfiler() {
    }

    public static synchronized void enable() {
        STATS.clear();
        startedAtNanos = System.nanoTime();
        enabled = true;
    }

    public static synchronized void disable() {
        enabled = false;
    }

    public static synchronized void reset() {
        STATS.clear();
        startedAtNanos = System.nanoTime();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Starts one sample.
     *
     * @return nanoTime start value, or 0 when profiling is disabled
     */
    static long beginSample() {
        return enabled ? System.nanoTime() : 0L;
    }

    static void endSample(String boundary, long startedNanos) {
        if (startedNanos == 0L) {
            return;
        }

        long elapsed = System.nanoTime() - startedNanos;
        recordSample(boundary, Math.max(0L, elapsed));
    }

    static synchronized void recordSample(
            String boundary,
            long elapsedNanos) {

        if (!enabled) {
            return;
        }

        String safeBoundary = sanitizeBoundary(boundary);
        MutableStats stats = STATS.computeIfAbsent(
                safeBoundary,
                ignored -> new MutableStats());

        long safeElapsed = Math.max(0L, elapsedNanos);

        stats.calls++;
        stats.totalNanos = saturatingAdd(
                stats.totalNanos,
                safeElapsed);
        stats.maxNanos = Math.max(
                stats.maxNanos,
                safeElapsed);
    }

    public static synchronized Snapshot snapshot() {
        long now = System.nanoTime();
        long elapsed = startedAtNanos == 0L
                ? 0L
                : Math.max(0L, now - startedAtNanos);

        List<Entry> entries = new ArrayList<>();

        for (Map.Entry<String, MutableStats> item : STATS.entrySet()) {
            MutableStats value = item.getValue();

            entries.add(new Entry(
                    item.getKey(),
                    value.calls,
                    value.totalNanos,
                    value.maxNanos));
        }

        entries.sort(
                Comparator.comparingLong(Entry::totalNanos)
                        .reversed()
                        .thenComparing(Entry::boundary));

        return new Snapshot(
                enabled,
                elapsed,
                List.copyOf(entries));
    }

    static synchronized void resetForTests() {
        enabled = false;
        STATS.clear();
        startedAtNanos = 0L;
    }

    private static String sanitizeBoundary(String boundary) {
        if (boundary == null || boundary.isBlank()) {
            return "UNKNOWN";
        }

        String trimmed = boundary.trim();

        return trimmed.length() > 64
                ? trimmed.substring(0, 64)
                : trimmed;
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    public record Entry(
            String boundary,
            long calls,
            long totalNanos,
            long maxNanos) {

        public double averageMillis() {
            return calls <= 0L
                    ? 0.0D
                    : (totalNanos / (double) calls) / 1_000_000.0D;
        }

        public double maxMillis() {
            return maxNanos / 1_000_000.0D;
        }

        public double totalMillis() {
            return totalNanos / 1_000_000.0D;
        }
    }

    public record Snapshot(
            boolean enabled,
            long elapsedNanos,
            List<Entry> entries) {

        public Snapshot {
            entries = entries == null
                    ? List.of()
                    : List.copyOf(entries);
        }

        public double elapsedSeconds() {
            return elapsedNanos / 1_000_000_000.0D;
        }

        public long totalMeasuredNanos() {
            long total = 0L;

            for (Entry entry : entries) {
                total = saturatingAdd(
                        total,
                        entry.totalNanos());
            }

            return total;
        }

        public double totalMeasuredMillis() {
            return totalMeasuredNanos() / 1_000_000.0D;
        }
    }

    private static final class MutableStats {
        long calls;
        long totalNanos;
        long maxNanos;
    }
}