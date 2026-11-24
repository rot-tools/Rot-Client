// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import java.util.Objects;

final class WarpAttempt {
    record Location(String area, String island, String server) {
        static final Location UNKNOWN = new Location(null, null, null);
    }

    private static final long TIMEOUT = 12_000_000_000L;
    private static final long STABLE_LOCATION = 450_000_000L;
    private static final long AREA_SETTLE = 650_000_000L;
    private static final long SAME_AREA_SETTLE = 1_750_000_000L;
    private static final long ISLAND_FALLBACK = 2_500_000_000L;

    private final long started;
    private final Location origin;
    private final String target;
    private boolean changedWorld;
    private boolean absolutePosition;
    private boolean meaningfulTeleport;
    private long positionedAt;
    private String candidate;
    private long candidateSince;

    WarpAttempt(long started, Location origin, String target) {
        this.started = started;
        this.origin = origin;
        this.target = target;
    }

    boolean expiredWhileLoading(long now) { return now - started >= 30_000_000_000L; }

    void worldChanged() {
        changedWorld = true;
        candidate = null;
    }

    void positioned(long now, boolean absolute, boolean moved) {
        if (!absolute && !moved && !changedWorld) return;
        if (!absolutePosition && !meaningfulTeleport) positionedAt = now;
        if (changedWorld && !meaningfulTeleport) positionedAt = now;
        absolutePosition |= absolute;
        meaningfulTeleport |= moved || changedWorld;
    }

    CommandShortcut.Notice outcome(long now, Location location, boolean worldReady) {
        if (worldReady && (absolutePosition || meaningfulTeleport)) {
            long sincePosition = now - positionedAt;
            String name = arrivedLocation(location, sincePosition);
            if (!Objects.equals(name, candidate)) {
                candidate = name;
                candidateSince = now;
            }
            if (name != null && sincePosition >= AREA_SETTLE && now - candidateSince >= STABLE_LOCATION) {
                return arrived(name);
            }
        }
        if (!worldReady || now - started < TIMEOUT) return null;
        if (meaningfulTeleport) {
            return candidate != null ? arrived(candidate)
                    : new CommandShortcut.Notice("Warp complete", "Location unavailable");
        }
        return failed("Arrival could not be confirmed");
    }

    private String arrivedLocation(Location current, long sincePosition) {
        boolean freshServer = differs(current.server(), origin.server());
        boolean transferred = changedWorld || freshServer;
        boolean areaChanged = differs(current.area(), origin.area());
        boolean islandChanged = differs(current.island(), origin.island());
        boolean meaningful = meaningfulTeleport || freshServer || areaChanged || islandChanged;

        if (current.area() != null && (areaChanged || (freshServer && !islandChanged && sincePosition >= SAME_AREA_SETTLE))) {
            return current.area();
        }
        // A fresh island with the old sidebar area means the sidebar has not caught up yet.
        if (current.area() != null && !transferred && !islandChanged && sincePosition >= SAME_AREA_SETTLE
                && (meaningful || current.area().equalsIgnoreCase(target))) return current.area();
        if (current.island() != null && sincePosition >= ISLAND_FALLBACK
                && (islandChanged || freshServer || (!transferred && (meaningful || current.island().equalsIgnoreCase(target))))) {
            return current.island();
        }
        return null;
    }

    private static boolean differs(String current, String old) { return current != null && !current.equals(old); }

    static CommandShortcut.Notice arrived(String actualLocation) {
        return new CommandShortcut.Notice(actualLocation, "Warp complete");
    }

    static CommandShortcut.Notice failed(String reason) { return new CommandShortcut.Notice("Warp failed", reason); }
}
