package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

final class PerformanceHudLayoutTest {
    @Test
    void disabledPerformanceHudRendersNothing() {
        assertTrue(PerformanceHudLayout.renderLines(
                snapshot(240, 19.9, 146),
                false,
                false,
                false,
                PerformanceHudLayout.Direction.HORIZONTAL).isEmpty());
    }

    @Test
    void showFpsOnly() {
        List<String> lines = PerformanceHudLayout.renderLines(
                snapshot(240, 19.9, 146),
                true,
                false,
                false,
                PerformanceHudLayout.Direction.HORIZONTAL);
        assertEquals(List.of("FPS 240"), lines);
    }

    @Test
    void showPingOnly() {
        List<String> lines = PerformanceHudLayout.renderLines(
                snapshot(240, 19.9, 146),
                false,
                false,
                true,
                PerformanceHudLayout.Direction.HORIZONTAL);
        assertEquals(List.of("Ping 146ms"), lines);
    }

    @Test
    void horizontalLayout() {
        List<String> lines = PerformanceHudLayout.renderLines(
                snapshot(240, 19.9, 146),
                true,
                true,
                true,
                PerformanceHudLayout.Direction.HORIZONTAL);
        assertEquals(1, lines.size());
        assertEquals("FPS 240   TPS 19.9   Ping 146ms", lines.get(0));
    }

    @Test
    void verticalLayout() {
        List<String> lines = PerformanceHudLayout.renderLines(
                snapshot(240, 19.9, 146),
                true,
                true,
                true,
                PerformanceHudLayout.Direction.VERTICAL);
        assertEquals(List.of("FPS 240", "TPS 19.9", "Ping 146ms"), lines);
    }

    @Test
    void unavailableTpsDoesNotFabricateValue() {
        List<PerformanceHudLayout.Metric> metrics = PerformanceHudLayout.visibleMetrics(
                new PerformanceHudLayout.Snapshot(
                        Optional.of(120),
                        Optional.empty(),
                        Optional.of(40)),
                true,
                true,
                true);
        assertEquals("TPS", metrics.get(1).name());
        assertEquals("--", metrics.get(1).value());
        assertFalse(metrics.get(1).available());
        assertFalse(metrics.get(1).value().contains("20.0"));
    }

    @Test
    void hiddenMetricReflowsLayout() {
        List<String> lines = PerformanceHudLayout.renderLines(
                snapshot(240, 19.9, 146),
                true,
                false,
                true,
                PerformanceHudLayout.Direction.HORIZONTAL);
        assertEquals(List.of("FPS 240   Ping 146ms"), lines);
        assertFalse(lines.get(0).contains("TPS"));
    }

    private static PerformanceHudLayout.Snapshot snapshot(
            int fps,
            double tps,
            int ping) {
        return new PerformanceHudLayout.Snapshot(
                Optional.of(fps),
                Optional.of(tps),
                Optional.of(ping));
    }
}
