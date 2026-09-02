package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only Powder Chest module projection of canonical Current Session data. */
record PowderChestTrackerPresentation(
        boolean enabled,
        boolean currentSessionActive,
        long chestsOpened,
        long activeDurationMillis,
        List<RewardRow> lootRows,
        List<RewardRow> currencyRows) {
    PowderChestTrackerPresentation {
        chestsOpened = Math.max(0L, chestsOpened);
        activeDurationMillis = Math.max(0L, activeDurationMillis);
        lootRows = lootRows == null ? List.of() : List.copyOf(lootRows);
        currencyRows = currencyRows == null
                ? List.of()
                : List.copyOf(currencyRows);
    }

    static PowderChestTrackerPresentation from(
            RotClientCurrentSessionConfig currentSession,
            boolean enabled) {
        return from(currentSession, enabled, 0L);
    }

    static PowderChestTrackerPresentation from(
            RotClientCurrentSessionConfig currentSession,
            boolean enabled,
            long activeDurationMillis) {
        if (currentSession == null) {
            return new PowderChestTrackerPresentation(
                    enabled, false, 0L, activeDurationMillis, List.of(), List.of());
        }
        Map<String, MutableRow> loot = new LinkedHashMap<>();
        Map<String, MutableRow> currencies = new LinkedHashMap<>();
        if (currentSession.items != null) {
            for (RotClientCurrentSessionConfig.SessionItemRecord item
                    : currentSession.items) {
                if (item == null || item.quantity() <= 0L) {
                    continue;
                }
                Map<String, MutableRow> destination = switch (item.source()) {
                    case CHEST -> loot;
                    case CURRENCY -> currencies;
                    default -> null;
                };
                if (destination == null) {
                    continue;
                }
                MutableRow row = destination.computeIfAbsent(
                        item.itemId(),
                        ignored -> new MutableRow(
                                item.itemId(), item.displayName()));
                row.quantity = Math.addExact(row.quantity, item.quantity());
            }
        }
        return new PowderChestTrackerPresentation(
                enabled,
                currentSession.isActive(),
                currentSession.powderChestsOpened,
                activeDurationMillis,
                immutableRows(loot),
                immutableRows(currencies));
    }

    String statusLabel() {
        if (!enabled) {
            return "OFF";
        }
        return currentSessionActive ? "RUNNING" : "PAUSED";
    }

    long gemstonePowder() {
        return currencyQuantity("GEMSTONE_POWDER");
    }

    long mithrilPowder() {
        return currencyQuantity("MITHRIL_POWDER");
    }

    long enchantedHardStone() {
        long total = 0L;
        for (RewardRow row : lootRows) {
            if ("ENCHANTED_HARD_STONE".equals(row.itemId())
                    || "COMPACTED_HARD_STONE".equals(row.itemId())) {
                total = Math.addExact(total, row.quantity());
            }
        }
        return total;
    }

    double chestsPerHour() {
        return perHour(chestsOpened);
    }

    double gemstonePowderPerHour() {
        return perHour(gemstonePowder());
    }

    double mithrilPowderPerHour() {
        return perHour(mithrilPowder());
    }

    private long currencyQuantity(String itemId) {
        for (RewardRow row : currencyRows) {
            if (itemId.equals(row.itemId())) {
                return row.quantity();
            }
        }
        return 0L;
    }

    private double perHour(long quantity) {
        if (activeDurationMillis <= 0L || quantity <= 0L) {
            return 0.0;
        }
        return quantity * 3_600_000.0 / activeDurationMillis;
    }

    private static List<RewardRow> immutableRows(
            Map<String, MutableRow> rows) {
        List<RewardRow> result = new ArrayList<>(rows.size());
        for (MutableRow row : rows.values()) {
            result.add(new RewardRow(
                    row.itemId, row.displayName, row.quantity));
        }
        return List.copyOf(result);
    }

    record RewardRow(String itemId, String displayName, long quantity) {
        RewardRow {
            itemId = SkyBlockItemId.normalize(itemId);
            displayName = displayName == null || displayName.isBlank()
                    ? itemId
                    : displayName.trim();
            quantity = Math.max(0L, quantity);
        }
    }

    private static final class MutableRow {
        private final String itemId;
        private final String displayName;
        private long quantity;

        private MutableRow(String itemId, String displayName) {
            this.itemId = itemId;
            this.displayName = displayName;
        }
    }
}
