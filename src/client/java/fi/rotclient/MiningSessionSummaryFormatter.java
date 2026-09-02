package fi.rotclient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

/**
 * Deterministic privacy-safe plain-text formatter for Session Analytics.
 * Used by status, copy, and tests. Never includes raw diagnostics or private data.
 */
final class MiningSessionSummaryFormatter {
    static final int MAX_SUMMARY_LENGTH = 6_000;
    static final int MAX_RESOURCE_ROWS = 24;

    String format(MiningSessionAnalyticsViewModel model) {
        if (model == null) {
            return truncate("Rot Client Session Analytics\nState: unavailable");
        }

        StringBuilder result = new StringBuilder(512);
        result.append("Rot Client Session Analytics\n");
        result.append("State: ")
                .append(stateLabel(model.sessionState()))
                .append('\n');
        result.append("Target: ")
                .append(safeText(model.selectedTargetDisplayName()))
                .append('\n');
        result.append("Tracker: ")
                .append(model.trackerEnabled() ? "ON" : "OFF")
                .append('\n');
        result.append('\n');

        result.append("Entries\n");
        result.append("TARGET_MINED: ")
                .append(model.targetEntryCount())
                .append('\n');
        result.append("OTHER_MINED: ")
                .append(model.otherEntryCount())
                .append('\n');
        result.append("CHEST_LOOT: ")
                .append(model.chestLootEntryCount())
                .append('\n');
        result.append("CURRENCY: ")
                .append(model.currencyEntryCount())
                .append('\n');
        result.append("Total: ")
                .append(model.entryCount())
                .append('\n');
        result.append('\n');

        result.append("Values\n");
        result.append("Price basis: ")
                .append(safeText(model.priceBasisLabel()))
                .append('\n');
        result.append("Resolved item value: ");
        if (model.resolvedValueAvailable()) {
            result.append(formatCoins(model.resolvedItemValue()))
                    .append(" coins");
        } else {
            result.append("unavailable");
        }
        result.append('\n');
        result.append("Resolved entries: ")
                .append(model.resolvedEntryCount())
                .append('\n');
        result.append("Unresolved entries: ")
                .append(model.unresolvedEntryCount())
                .append('\n');
        result.append("Stale entries: ")
                .append(model.staleEntryCount())
                .append('\n');
        result.append("Unavailable entries: ")
                .append(model.unavailableEntryCount())
                .append('\n');
        result.append("Unsupported entries: ")
                .append(model.unsupportedEntryCount())
                .append('\n');
        result.append("Excluded currency entries: ")
                .append(model.excludedCurrencyEntryCount())
                .append('\n');
        if (model.resolvedValueAvailable()) {
            result.append("TARGET_MINED value: ")
                    .append(formatCoins(model.targetMinedValue()))
                    .append('\n');
            result.append("OTHER_MINED value: ")
                    .append(formatCoins(model.otherMinedValue()))
                    .append('\n');
            result.append("CHEST_LOOT value: ")
                    .append(formatCoins(model.chestLootValue()))
                    .append('\n');
        }
        result.append("Price-book age: ");
        if (model.priceBookAgeMillis().isPresent()) {
            result.append(model.priceBookAgeMillis().getAsLong() / 1_000L)
                    .append('s');
        } else {
            result.append("unavailable");
        }
        result.append('\n');
        result.append('\n');

        appendQuantitySection(
                result,
                "Target quantities",
                model.targetQuantities());
        appendQuantitySection(
                result,
                "Other mined quantities",
                model.otherMinedQuantities());
        appendQuantitySection(
                result,
                "Chest loot quantities",
                model.chestLootQuantities());
        appendQuantitySection(
                result,
                "Currency quantities",
                model.currencyQuantities());

        result.append("Parity: ")
                .append(safeText(model.parityStatusLabel()))
                .append('\n');
        result.append("Mismatches: ")
                .append(model.mismatchCount());

        return truncate(result.toString());
    }

    private static void appendQuantitySection(
            StringBuilder result,
            String title,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    quantities) {
        result.append(title).append('\n');
        if (quantities == null || quantities.isEmpty()) {
            result.append("(none)\n\n");
            return;
        }

        int shown = 0;
        int omitted = 0;
        for (MiningSessionAnalyticsViewModel.ResourceQuantity quantity
                : quantities.values()) {
            if (shown >= MAX_RESOURCE_ROWS) {
                omitted++;
                continue;
            }
            result.append(quantity.resourceId())
                    .append(": ")
                    .append(quantity.quantity())
                    .append('\n');
            shown++;
        }
        if (omitted > 0) {
            result.append('+')
                    .append(omitted)
                    .append(" more\n");
        }
        result.append('\n');
    }

    private static String stateLabel(
            MiningSessionAnalyticsViewModel.SessionState state) {
        if (state == null) {
            return "unavailable";
        }
        return switch (state) {
            case NOT_STARTED -> "Not started";
            case ACTIVE -> "Active";
            case STOPPED -> "Stopped";
        };
    }

    private static String formatCoins(BigDecimal value) {
        if (value == null) {
            return "unavailable";
        }
        DecimalFormatSymbols symbols =
                DecimalFormatSymbols.getInstance(Locale.ROOT);
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    private static String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "unavailable";
        }
        return value.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_SUMMARY_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_SUMMARY_LENGTH - 15)
                + "\n...truncated";
    }
}
