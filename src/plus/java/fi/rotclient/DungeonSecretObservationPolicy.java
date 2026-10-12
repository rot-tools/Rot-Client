package fi.rotclient;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Room count and secret pickup behavior adapted from NoammAddons ActionBarParser.kt,
 * EventDispatcher.kt and DungeonUtils.kt (CC0-1.0, revision 4164abc38500e041a805e03ca0f01b46e37bd8a5).
 * Rot requires an identified room with the same total, a typed nearby waypoint and an
 * actual pickup / bat-death / block-interaction event. Entity disappearance is not a receipt.
 */
public final class DungeonSecretObservationPolicy {
    private static final Pattern COUNT = Pattern.compile("(?<![\\d/])(\\d{1,2})/(\\d{1,2}) Secrets\\b");
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Set<String> SECRET_ITEMS = Set.of(
            "Health Potion VIII Splash Potion", "Healing Potion 8 Splash Potion",
            "Healing Potion VIII Splash Potion", "Healing VIII Splash Potion", "Healing 8 Splash Potion",
            "Decoy", "Inflatable Jerry", "Spirit Leap", "Trap", "Training Weights", "Defuse Kit",
            "Dungeon Chest Key", "Treasure Talisman", "Revive Stone", "Architect's First Draft",
            "Secret Dye", "Candycomb");

    private DungeonSecretObservationPolicy() { }
    public record RoomCount(int found, int total) { }

    public static Optional<RoomCount> roomCount(String text) {
        if (text == null || text.length() > 4096) return Optional.empty();
        var match = COUNT.matcher(FORMATTING.matcher(text).replaceAll(""));
        if (!match.find()) return Optional.empty();
        int found = Integer.parseInt(match.group(1));
        int total = Integer.parseInt(match.group(2));
        if (total <= 0 || found > total || match.find()) return Optional.empty();
        return Optional.of(new RoomCount(found, total));
    }

    public static boolean belongsToRoom(RoomCount count, DungeonMapPolicy.RoomIdentity room) {
        return count != null && room != null && !room.name().isBlank() && count.total() == room.secrets();
    }

    public static boolean isSecretItem(String name) {
        return name != null && SECRET_ITEMS.contains(FORMATTING.matcher(name).replaceAll("").trim());
    }

    public static boolean interactionBlock(DungeonRoomDataPolicy.SecretKind kind, String block) {
        if (kind == null || block == null) return false;
        return switch (kind) {
            case CHEST -> block.equals("minecraft:chest") || block.equals("minecraft:trapped_chest");
            case ESSENCE, REDSTONE -> block.contains("skull") || block.endsWith("_head");
            case LEVER -> block.equals("minecraft:lever");
            case ITEM, BAT -> false;
        };
    }

    public static Optional<DungeonRoomDataPolicy.PlacedWaypoint> nearest(
            List<DungeonRoomDataPolicy.PlacedWaypoint> waypoints, Set<String> collected,
            String roomName, DungeonRoomDataPolicy.SecretKind kind, double x, double y, double z, int radius) {
        if (waypoints == null || collected == null || roomName == null || roomName.isBlank() || kind == null
                || radius < 0 || radius > 5 || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return Optional.empty();
        }
        // Upstream compares block positions, including negative dungeon coordinates.
        double px = Math.floor(x), py = Math.floor(y), pz = Math.floor(z);
        DungeonRoomDataPolicy.PlacedWaypoint best = null;
        double bestDistance = radius * radius;
        for (var waypoint : waypoints) {
            if (waypoint.kind() != kind || !roomName.equals(waypoint.roomName())
                    || collected.contains(DungeonRoomDataPolicy.secretKey(waypoint.x(), waypoint.y(), waypoint.z()))) continue;
            double dx = waypoint.x() - px, dy = waypoint.y() - py, dz = waypoint.z() - pz;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= bestDistance && (best == null || distance < bestDistance)) {
                best = waypoint;
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }
}
