package fi.rotclient;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;

/**
 * Polished line/area chart drawing for Rot Client dashboards and HUD.
 *
 * <p>Uses Catmull-Rom sampling plus soft glow, translucent area fill, and
 * multi-row grid lines. All drawing is fill-based (no GPU AA API available).
 */
final class RotClientChartDraw {
    private RotClientChartDraw() {
    }

    static void drawLineAreaChart(
            GuiGraphicsExtractor graphics,
            int left,
            int top,
            int right,
            int bottom,
            double[] rawValues,
            double scaleMaximum) {
        int width = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);
        RotClientUiDraw.roundedFill(
                graphics,
                left,
                top,
                right,
                bottom,
                RotClientTheme.CHART_WELL,
                RotClientUiDraw.RADIUS_SM);
        RotClientUiDraw.roundedOutline(
                graphics,
                left,
                top,
                right,
                bottom,
                RotClientTheme.DIVIDER,
                RotClientUiDraw.RADIUS_SM);

        // Horizontal grid bands (dashboard-style, not a single midline).
        for (int band = 1; band <= 3; band++) {
            int gy = top + (height * band) / 4;
            graphics.fill(left + 1, gy, right - 1, gy + 1, RotClientTheme.CHART_GRID);
        }

        if (rawValues == null || rawValues.length == 0) {
            return;
        }

        double[] values = RateGraphMath.smooth(rawValues);
        double maximum = Math.max(1.0, RateGraphMath.sanitizeSample(scaleMaximum));
        if (!Double.isFinite(maximum) || maximum <= 0.0) {
            maximum = 1.0;
        }
        int chartTop = top + 2;
        int chartBottom = bottom - 2;
        int chartHeight = Math.max(1, chartBottom - chartTop);
        int[] ys = new int[width];

        for (int x = 0; x < width; x++) {
            double position = values.length <= 1
                    ? 0
                    : x * (values.length - 1.0) / Math.max(1, width - 1);
            double value = RateGraphMath.sampleCatmullRom(values, position);
            if (!Double.isFinite(value)) {
                value = 0;
            }
            double ratio = Math.max(0.0, Math.min(1.0, value / maximum));
            ys[x] = chartBottom - (int) Math.round(ratio * chartHeight);
            ys[x] = Math.max(chartTop, Math.min(chartBottom, ys[x]));
        }

        // Soft area fill under the curve.
        int fillNear = RotClientTheme.CHART_FILL;
        int fillFar = RotClientUiDraw.withAlpha(RotClientTheme.CHART_FILL, 0x14);
        for (int x = 0; x < width; x++) {
            int ax = left + x;
            int mid = (ys[x] + chartBottom) / 2;
            if (mid > ys[x]) {
                graphics.fill(ax, ys[x] + 1, ax + 1, mid, fillNear);
            }
            if (chartBottom > mid) {
                graphics.fill(ax, mid, ax + 1, chartBottom, fillFar);
            }
        }

        // Stroke with glow + faux anti-alias neighbor pixels on steep moves.
        for (int x = 1; x < width; x++) {
            drawSmoothSegment(graphics, left + x, ys[x - 1], ys[x]);
        }

        // Endpoint marker.
        int ex = left + width - 1;
        int ey = ys[width - 1];
        RotClientUiDraw.roundedFill(
                graphics,
                ex - 2,
                ey - 2,
                ex + 3,
                ey + 3,
                RotClientUiDraw.withAlpha(RotClientTheme.CHART_LINE, 0x55),
                RotClientUiDraw.RADIUS_XS);
        RotClientUiDraw.roundedFill(
                graphics,
                ex - 1,
                ey - 1,
                ex + 2,
                ey + 2,
                RotClientTheme.CHART_LINE,
                RotClientUiDraw.RADIUS_XS);
    }

    /**
     * Chart framed as a dashboard card with optional title/subtitle labels.
     */
    static void drawChartCard(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int height,
            String title,
            String subtitle,
            double[] values,
            double scaleMaximum) {
        RotClientUiDraw.drawElevatedCard(graphics, x, y, width, height);
        int labelY = y + 8;
        if (title != null && !title.isBlank()) {
            RotClientUiDraw.cardTitle(graphics, font, title, x + 10, labelY);
        }
        if (subtitle != null && !subtitle.isBlank()) {
            int tw = font.width(subtitle);
            RotClientUiDraw.helpText(
                    graphics, font, subtitle, x + width - 10 - tw, labelY);
        }
        int plotTop = y + (title == null || title.isBlank() ? 8 : 22);
        drawLineAreaChart(
                graphics,
                x + 8,
                plotTop,
                x + width - 8,
                y + height - 8,
                values,
                scaleMaximum);
    }

    private static void drawSmoothSegment(
            GuiGraphicsExtractor graphics,
            int x,
            int previousY,
            int y) {
        int from = Math.min(previousY, y);
        int to = Math.max(previousY, y);
        // Soft glow halo.
        graphics.fill(
                x - 1,
                from - 1,
                x + 2,
                to + 2,
                RotClientTheme.CHART_GLOW);
        // Core stroke.
        graphics.fill(x, from, x + 1, to + 1, RotClientTheme.CHART_LINE);
        // Faux AA: dim neighbors on steep vertical runs.
        if (to - from >= 2) {
            int soft = RotClientUiDraw.withAlpha(RotClientTheme.CHART_LINE, 0x40);
            graphics.fill(x - 1, from + 1, x, to, soft);
            graphics.fill(x + 1, from + 1, x + 2, to, soft);
        }
    }
}
