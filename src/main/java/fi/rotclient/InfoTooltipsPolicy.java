package fi.rotclient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Extra tooltip lines from SkyBlock ExtraAttributes.
 */
public final class InfoTooltipsPolicy {
    private InfoTooltipsPolicy() {
    }

    public record Snapshot(
            String itemId,
            int qualityBoost,
            int itemTier,
            long createdEpochMs,
            boolean donatedMuseum,
            boolean museumKnown,
            String hexColor) {
        public Snapshot {
            itemId = itemId == null ? "" : itemId.trim();
            hexColor = hexColor == null ? "" : hexColor.trim();
        }
    }

    public static List<String> lines(
            boolean enabled,
            boolean dungeonQuality,
            boolean createdDate,
            boolean hexColor,
            boolean museum,
            boolean itemId,
            Snapshot snapshot) {
        List<String> out = new ArrayList<>();
        if (!enabled || snapshot == null) {
            return out;
        }
        if (dungeonQuality && snapshot.qualityBoost() > 0) {
            String color = snapshot.qualityBoost() >= 50 ? "§c§l" : "§6";
            out.add("§bQuality: " + color + snapshot.qualityBoost() + "/50§r§b, Tier "
                    + snapshot.itemTier());
        }
        if (createdDate && snapshot.createdEpochMs() > 0L) {
            out.add("§bCreated: §6" + formatDate(snapshot.createdEpochMs()));
        }
        if (hexColor && !snapshot.hexColor().isBlank()) {
            out.add("§bDye Color: §6" + snapshot.hexColor());
        }
        if (museum && snapshot.museumKnown()) {
            out.add("§bMuseum: §6" + (snapshot.donatedMuseum() ? "Donated" : "Not donated"));
        }
        if (itemId && !snapshot.itemId().isBlank()) {
            out.add("§bItem ID: §6" + snapshot.itemId());
        }
        return List.copyOf(out);
    }

    static String formatDate(long epochMs) {
        SimpleDateFormat format = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        return format.format(new Date(epochMs));
    }

    public static String normalizeHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF).toLowerCase(Locale.ROOT);
    }
}
