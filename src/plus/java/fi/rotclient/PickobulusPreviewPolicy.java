package fi.rotclient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Bounded candidate footprint; server shape and projectile mechanics require playtest calibration. */
final class PickobulusPreviewPolicy {
    static final long SCAN_INTERVAL_MILLIS = 250L;
    static final long SNAPSHOT_FRESH_MILLIS = 500L;
    static final int MAX_CANDIDATES = 11 * 11 * 11;

    record Settings(int radius, int range, boolean sphere, boolean throughWalls) {
        Settings {
            radius = Math.max(1, Math.min(5, radius));
            range = Math.max(4, Math.min(64, range));
        }
    }

    static boolean inside(int dx, int dy, int dz, int radius, boolean sphere) {
        radius = Math.max(1, Math.min(5, radius));
        // Check signed bounds before squaring: abs(MIN_VALUE) and arbitrary int squares overflow.
        if (dx < -radius || dx > radius || dy < -radius || dy > radius || dz < -radius || dz > radius) return false;
        return !sphere || dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    static boolean candidateAllowed(String resourceId, SkyBlockLocation location) {
        // A prediction has no accepted mining event to justify the live tracker's target override.
        return MiningBlockEvidencePolicy.allows(resourceId, location);
    }

    static boolean materialTarget(boolean materialSelection, List<String> selectedMaterialIds, String candidateId) {
        return materialSelection && selectedMaterialIds != null && candidateId != null
                && selectedMaterialIds.contains(candidateId);
    }

    record Context(Object world, Object player, Object config, SkyBlockLocation location,
                   String profileId, String selectionId, String toolId, List<String> lore, Settings settings) {
        Context { lore = List.copyOf(lore); }
        boolean matches(Context other) {
            return other != null && world == other.world && player == other.player && config == other.config
                    && Objects.equals(location, other.location) && Objects.equals(profileId, other.profileId)
                    && Objects.equals(selectionId, other.selectionId) && Objects.equals(toolId, other.toolId)
                    && lore.equals(other.lore) && settings.equals(other.settings);
        }
    }

    record Snapshot<T>(List<T> candidates, int targetCount) {}

    /** Invalidates before rendering; context/aim changes do not bypass the scan budget. */
    static final class Cache<T> {
        private Context context;
        private Object aim;
        private Snapshot<T> snapshot;
        private long lastClock = -1, lastScan = -1, publishedAt = -1;

        void clear() {
            context = null; aim = null; snapshot = null;
            lastClock = lastScan = publishedAt = -1;
        }

        boolean sync(Context current, long now) {
            if (now < 0 || lastClock >= 0 && now < lastClock) {
                clear();
                return false;
            }
            lastClock = now;
            if (current == null || context == null || !context.matches(current)) {
                context = current; aim = null; snapshot = null; publishedAt = -1;
            }
            if (snapshot != null && now - publishedAt >= SNAPSHOT_FRESH_MILLIS) snapshot = null;
            return context != null;
        }

        void aim(Object currentAim) {
            if (!Objects.equals(aim, currentAim)) { aim = currentAim; snapshot = null; publishedAt = -1; }
        }

        boolean beginScan(Context current, long now) {
            if (!sync(current, now) || aim == null
                    || lastScan >= 0 && now - lastScan < SCAN_INTERVAL_MILLIS) return false;
            lastScan = now;
            return true;
        }

        void publish(Context scanned, List<T> candidates, int targetCount, long now) {
            if (scanned == null || context == null || !context.matches(scanned) || now != lastScan || aim == null) return;
            if (candidates == null || candidates.size() > MAX_CANDIDATES) {
                snapshot = null;
                return;
            }
            List<T> bounded = List.copyOf(candidates);
            snapshot = new Snapshot<>(bounded, Math.max(0, Math.min(bounded.size(), targetCount)));
            publishedAt = now;
        }

        Optional<Snapshot<T>> snapshot(Context current, long now) {
            return sync(current, now) ? Optional.ofNullable(snapshot) : Optional.empty();
        }
    }
    private PickobulusPreviewPolicy() {}
}
