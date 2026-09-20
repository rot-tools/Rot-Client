package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Where or what the player is doing, at the granularity the automatic profile
 * switcher can act on. Pure string/enum logic so it stays unit-testable
 * without any Minecraft dependency; the client bridge feeds it the sidebar
 * lines and the already-detected location.
 *
 * {@link #OTHER} is never offered as a rule row. It exists so "everywhere
 * else" has a value to resolve against the fallback profile.
 */
enum AutoProfileContext {
    DUNGEONS("dungeons", "Dungeons"),
    KUUDRA("kuudra", "Kuudra"),
    DWARVEN_MINES("dwarven_mines", "Dwarven Mines"),
    CRYSTAL_HOLLOWS("crystal_hollows", "Crystal Hollows"),
    GLACITE("glacite", "Glacite Tunnels"),
    GLACITE_MINESHAFT("glacite_mineshaft", "Glacite Mineshaft"),
    DEEP_CAVERNS("deep_caverns", "Deep Caverns"),
    GARDEN("garden", "Garden"),
    PRIVATE_ISLAND("private_island", "Private Island"),
    SLAYER("slayer", "Slayer Quest"),
    OTHER("other", "Elsewhere");

    private final String id;
    private final String displayName;

    AutoProfileContext(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    /** Stable persisted key. Never rename an existing value. */
    String id() {
        return id;
    }

    String displayName() {
        return displayName;
    }

    /** True for contexts that get their own rule row in the UI. */
    boolean hasRuleRow() {
        return this != OTHER;
    }

    static Optional<AutoProfileContext> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (AutoProfileContext context : values()) {
            if (context.id.equals(wanted)) {
                return Optional.of(context);
            }
        }
        return Optional.empty();
    }

    /**
     * Classifies one sidebar snapshot. Returns empty when the sidebar is not
     * a SkyBlock one (lobby, other minigame, no sidebar yet) so the switcher
     * never reacts outside SkyBlock.
     *
     * Precedence is fixed: what the player is physically inside (dungeon,
     * Kuudra, a mining area, the Garden, their island) always beats the
     * Slayer quest that merely stays on the sidebar while they travel.
     *
     * @param lines        sidebar title plus rows, in render order
     * @param location     location parsed from those same lines
     * @param inDungeon    the dungeon detector's sticky verdict
     */
    static Optional<AutoProfileContext> classify(
            List<String> lines,
            SkyBlockLocation location,
            boolean inDungeon) {
        if (!SkyBlockAreaDetector.looksLikeSkyblock(lines)) {
            return Optional.empty();
        }
        if (inDungeon) {
            return Optional.of(DUNGEONS);
        }
        boolean kuudra = false;
        boolean garden = false;
        boolean island = false;
        boolean slayer = false;
        for (String line : lines) {
            if (IotaKuudraPolicy.isKuudraArea(line)) {
                kuudra = true;
            }
            String text = SkyBlockAreaDetector.normalizeLocationText(line);
            if (text.contains("the garden") || text.startsWith("plot ")) {
                garden = true;
            }
            if (text.contains("your island")) {
                island = true;
            }
            if (text.startsWith("slayer quest")) {
                slayer = true;
            }
        }
        if (kuudra) {
            return Optional.of(KUUDRA);
        }
        AutoProfileContext mining = fromArea(
                location == null ? null : location.parentArea());
        if (mining != null) {
            return Optional.of(mining);
        }
        if (garden) {
            return Optional.of(GARDEN);
        }
        if (island) {
            return Optional.of(PRIVATE_ISLAND);
        }
        return Optional.of(slayer ? SLAYER : OTHER);
    }

    /** Mining-area mapping; null when the area is not a mining context. */
    static AutoProfileContext fromArea(SkyBlockArea area) {
        if (area == null) {
            return null;
        }
        return switch (area) {
            case DEEP_CAVERNS -> DEEP_CAVERNS;
            case DWARVEN_MINES, DWARVEN_BASE_CAMP -> DWARVEN_MINES;
            case CRYSTAL_HOLLOWS, CRYSTAL_NUCLEUS -> CRYSTAL_HOLLOWS;
            case GLACITE_TUNNELS, GREAT_GLACITE_LAKE -> GLACITE;
            case GLACITE_MINESHAFT -> GLACITE_MINESHAFT;
            case UNKNOWN_SKYBLOCK_AREA -> null;
        };
    }
}
