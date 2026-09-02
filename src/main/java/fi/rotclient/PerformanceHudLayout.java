package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure layout for Performance HUD lines. Unavailable TPS must not fabricate 20.0.
 */
public final class PerformanceHudLayout {
    public enum Direction {
        HORIZONTAL,
        VERTICAL;

        public static Direction fromConfig(String raw) {
            if (raw != null && raw.toLowerCase(Locale.ROOT).contains("vert")) {
                return VERTICAL;
            }
            return HORIZONTAL;
        }
    }

    public record Metric(String name, String value, boolean available) {
    }

    public record Snapshot(
            Optional<Integer> fps,
            Optional<Double> tps,
            Optional<Integer> pingMs) {
        public Snapshot {
            fps = fps == null ? Optional.empty() : fps;
            tps = tps == null ? Optional.empty() : tps;
            pingMs = pingMs == null ? Optional.empty() : pingMs;
        }
    }

    private PerformanceHudLayout() {
    }

    public static List<Metric> visibleMetrics(
            Snapshot snapshot,
            boolean showFps,
            boolean showTps,
            boolean showPing) {
        List<Metric> metrics = new ArrayList<>();
        if (showFps) {
            metrics.add(new Metric(
                    "FPS",
                    snapshot.fps().map(String::valueOf).orElse("--"),
                    snapshot.fps().isPresent()));
        }
        if (showTps) {
            String value = snapshot.tps()
                    .map(v -> String.format(Locale.ROOT, "%.1f", v))
                    .orElse("--");
            metrics.add(new Metric("TPS", value, snapshot.tps().isPresent()));
        }
        if (showPing) {
            String value = snapshot.pingMs()
                    .map(v -> v + "ms")
                    .orElse("--");
            metrics.add(new Metric("Ping", value, snapshot.pingMs().isPresent()));
        }
        return List.copyOf(metrics);
    }

    public static List<String> renderLines(
            Snapshot snapshot,
            boolean showFps,
            boolean showTps,
            boolean showPing,
            Direction direction) {
        List<Metric> metrics = visibleMetrics(
                snapshot, showFps, showTps, showPing);
        if (metrics.isEmpty()) {
            return List.of();
        }
        if (direction == Direction.HORIZONTAL) {
            StringBuilder line = new StringBuilder();
            for (int i = 0; i < metrics.size(); i++) {
                Metric metric = metrics.get(i);
                if (i > 0) {
                    line.append("   ");
                }
                line.append(metric.name()).append(' ').append(metric.value());
            }
            return List.of(line.toString());
        }
        List<String> lines = new ArrayList<>();
        for (Metric metric : metrics) {
            lines.add(metric.name() + " " + metric.value());
        }
        return List.copyOf(lines);
    }

    public static int estimateWidth(List<String> lines, int charWidth) {
        int max = 0;
        for (String line : lines) {
            max = Math.max(max, line.length() * Math.max(1, charWidth));
        }
        return max + 16;
    }

    public static int estimateHeight(List<String> lines, int lineHeight) {
        if (lines.isEmpty()) {
            return 0;
        }
        return 8 + lines.size() * Math.max(1, lineHeight);
    }
}
