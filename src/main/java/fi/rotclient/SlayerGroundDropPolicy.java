package fi.rotclient;

import java.math.BigDecimal;

/** Pure display gate for locally rendered Slayer ground-drop labels. */
public final class SlayerGroundDropPolicy {
    public static final long MIN_VALUE = 0L;
    public static final long MAX_VALUE = 100_000_000L;

    private SlayerGroundDropPolicy() {
    }

    public static long clampMinimum(long value) {
        return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value));
    }

    public static BigDecimal totalValue(BigDecimal unitValue, int count) {
        if (unitValue == null || unitValue.signum() <= 0 || count <= 0) {
            return BigDecimal.ZERO;
        }
        return unitValue.multiply(BigDecimal.valueOf(count));
    }

    public static boolean shouldShowLabel(
            boolean enabled,
            BigDecimal unitValue,
            int count,
            long minimumValue) {
        BigDecimal total = totalValue(unitValue, count);
        return enabled
                && total.signum() > 0
                && total.compareTo(BigDecimal.valueOf(clampMinimum(minimumValue))) >= 0;
    }
}
