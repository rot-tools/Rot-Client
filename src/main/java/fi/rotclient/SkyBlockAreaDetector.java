package fi.rotclient;

import java.util.List;
import java.util.Locale;

/**
 * Best-effort SkyBlock area detection. Pure string heuristics live here so
 * they stay unit-testable without any Minecraft client dependency; the
 * client module is responsible for reading the live scoreboard sidebar
 * (Hypixel renders the current location as one of its lines, e.g.
 * "&#8961; Cliffside Veins") and reporting it via
 * {@link #updateCurrentLocation}.
 *
 * Detection is intentionally non-gating and sticky: a momentary miss (for
 * example one tick before the sidebar has finished rendering after a world
 * join) never downgrades a previously known area back to UNKNOWN. Callers
 * that need a hard reset (world change, disconnect) must call
 * {@link #updateCurrentLocation} with {@link SkyBlockLocation#UNKNOWN}
 * (or {@link #updateCurrentArea} with
 * {@link SkyBlockArea#UNKNOWN_SKYBLOCK_AREA}) explicitly.
 *
 * Live Hypixel scoreboards usually show the <em>sub-area</em> name while
 * mining (Cliffside Veins, The Forge, Magma Fields, …), not the parent
 * island title. Those sub-areas map to a parent {@link SkyBlockArea} plus
 * an optional {@link SkyBlockSubArea}.
 *
 * Ambiguous bare tokens such as a lone "glacite" or "mineshaft" are
 * intentionally NOT matched.
 */
final class SkyBlockAreaDetector {
    private static volatile SkyBlockLocation currentLocation =
            SkyBlockLocation.UNKNOWN;
    private static volatile boolean inSkyblock;

    private SkyBlockAreaDetector() {
    }

    /** Returns the most recently detected parent area, or UNKNOWN if none. */
    static SkyBlockArea detect() {
        return currentLocation.parentArea();
    }

    /**
     * Returns the most recently detected location snapshot (parent +
     * optional sub-area). Never {@code null}.
     */
    static SkyBlockLocation detectLocation() {
        return currentLocation;
    }

    /**
     * SkyBlock scoreboard gate: Hypixel scoreboard title contains
     * {@code SKYBLOCK}. Sticky across momentary sidebar misses; world-change
     * callers must {@link #clearSkyblockPresence()}.
     */
    static boolean isInSkyblock() {
        return inSkyblock;
    }

    static void clearSkyblockPresence() {
        inSkyblock = false;
    }

    static void updateSkyblockPresence(List<String> lines) {
        if (looksLikeSkyblock(lines)) {
            inSkyblock = true;
        } else if (lines != null && !lines.isEmpty()) {
            inSkyblock = false;
        }
    }

    static boolean looksLikeSkyblock(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        for (String line : lines) {
            if (normalizeLocationText(line).contains("skyblock")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Expires a previously timestamped location after a bounded sequence of
     * scoreboard misses. Untimestamped explicit/test locations remain sticky,
     * and clock rollback never expires a fresh observation.
     */
    static boolean expireCurrentLocationIfStale(
            long nowMillis,
            long maximumAgeMillis) {
        SkyBlockLocation observed = currentLocation;
        if (observed.isUnknown()
                || observed.observedAtMillis() <= 0L
                || maximumAgeMillis <= 0L
                || nowMillis < observed.observedAtMillis()) {
            return false;
        }
        if (nowMillis - observed.observedAtMillis() < maximumAgeMillis) {
            return false;
        }
        currentLocation = SkyBlockLocation.UNKNOWN;
        return true;
    }

    /**
     * Reports the latest live-detected parent area. Clears any previous
     * sub-area. Prefer {@link #updateCurrentLocation} when both parent and
     * sub-area are known. Pass UNKNOWN explicitly to force a reset.
     */
    static void updateCurrentArea(SkyBlockArea area) {
        if (area == null || area == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA) {
            currentLocation = SkyBlockLocation.UNKNOWN;
            return;
        }
        currentLocation = SkyBlockLocation.of(area);
    }

    /**
     * Reports the latest live-detected location. Callers pass
     * {@link SkyBlockLocation#UNKNOWN} explicitly to force a reset; a
     * heuristic miss should simply not call this, to preserve stickiness.
     */
    static void updateCurrentLocation(SkyBlockLocation location) {
        currentLocation = location == null || location.isUnknown()
                ? SkyBlockLocation.UNKNOWN
                : location;
    }

    /**
     * Lightweight heuristic for a single line of text (scoreboard title or
     * sidebar line, tab list header/footer, etc). Matches common Dwarven /
     * Crystal Hollows / Glacite fragments and known sub-areas.
     */
    static SkyBlockArea fromScoreboardTitle(String title) {
        return fromLine(title).parentArea();
    }

    /** Full parent + optional sub-area match for a single location line. */
    static SkyBlockLocation locationFromScoreboardTitle(String title) {
        return fromLine(title);
    }

    /**
     * Scans every sidebar scoreboard line (in render order) and returns the
     * first recognized parent area. Hypixel typically shows the location a
     * few lines below the date/time header, not in the scoreboard title
     * itself, so the whole sidebar must be scanned rather than only the
     * title.
     */
    static SkyBlockArea fromScoreboardLines(List<String> lines) {
        return locationFromScoreboardLines(lines).parentArea();
    }

    /**
     * Scans every sidebar scoreboard line and returns the first recognized
     * location (parent + optional sub-area).
     */
    static SkyBlockLocation locationFromScoreboardLines(List<String> lines) {
        if (lines == null) {
            return SkyBlockLocation.UNKNOWN;
        }
        for (String line : lines) {
            SkyBlockLocation match = fromLine(line);
            if (!match.isUnknown()) {
                return match;
            }
        }
        return SkyBlockLocation.UNKNOWN;
    }

    /**
     * Normalizes scoreboard/location text for matching: lowercases, strips
     * common Hypixel location glyphs / section formatting leftovers, and
     * collapses whitespace.
     */
    static String normalizeLocationText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '§') {
                // Skip section code + following format char when present.
                if (i + 1 < raw.length()) {
                    i++;
                }
                continue;
            }
            // Keep letters/digits/spaces/apostrophes; treat hyphens as spaces;
            // drop icons like ⏣. Map û → u so Khazad-Dûm normalizes cleanly.
            if (c == 'û' || c == 'Û') {
                out.append('u');
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == '\'' || c == ' ') {
                out.append(Character.toLowerCase(c));
            } else if (c == '-' || Character.isWhitespace(c)) {
                out.append(' ');
            }
        }
        return out.toString().trim().replaceAll("\\s+", " ");
    }

    private static SkyBlockLocation fromLine(String line) {
        String lower = normalizeLocationText(line);
        if (lower.isEmpty()) {
            return SkyBlockLocation.UNKNOWN;
        }

        // Most-specific Glacite / Crystal / Base Camp matches first.
        // These parent areas are already precise enough — no sub-area.
        if (containsPhrase(lower, "glacite mineshaft")) {
            return SkyBlockLocation.of(SkyBlockArea.GLACITE_MINESHAFT);
        }
        if (containsPhrase(lower, "glacite tunnels")) {
            return SkyBlockLocation.of(SkyBlockArea.GLACITE_TUNNELS);
        }
        if (containsPhrase(lower, "great glacite lake")
                || containsPhrase(lower, "glacite lake")) {
            return SkyBlockLocation.of(SkyBlockArea.GREAT_GLACITE_LAKE);
        }
        if (containsPhrase(lower, "crystal nucleus")
                || containsPhrase(lower, "nucleus")) {
            return SkyBlockLocation.of(SkyBlockArea.CRYSTAL_NUCLEUS);
        }
        if (containsPhrase(lower, "dwarven base camp")
                || containsPhrase(lower, "base camp")) {
            return SkyBlockLocation.of(SkyBlockArea.DWARVEN_BASE_CAMP);
        }

        SkyBlockLocation deepCavernsSub = matchDeepCavernsSub(lower);
        if (deepCavernsSub != null) {
            return deepCavernsSub;
        }
        if (containsPhrase(lower, "deep caverns")) {
            return SkyBlockLocation.of(SkyBlockArea.DEEP_CAVERNS);
        }

        // Crystal Hollows sub-areas (most specific first).
        SkyBlockLocation crystalSub = matchCrystalHollowsSub(lower);
        if (crystalSub != null) {
            return crystalSub;
        }
        if (containsPhrase(lower, "crystal hollow")
                || containsPhrase(lower, "crystal hollows")) {
            return SkyBlockLocation.of(SkyBlockArea.CRYSTAL_HOLLOWS);
        }

        // Dwarven Mines sub-areas (most specific first).
        SkyBlockLocation dwarvenSub = matchDwarvenSub(lower);
        if (dwarvenSub != null) {
            return dwarvenSub;
        }
        if (containsPhrase(lower, "dwarven mines")) {
            return SkyBlockLocation.of(SkyBlockArea.DWARVEN_MINES);
        }

        return SkyBlockLocation.UNKNOWN;
    }

    private static SkyBlockLocation matchDeepCavernsSub(String lower) {
        if (containsPhrase(lower, "gunpowder mines")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DEEP_CAVERNS,
                    SkyBlockSubArea.GUNPOWDER_MINES);
        }
        if (containsPhrase(lower, "lapis quarry")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DEEP_CAVERNS,
                    SkyBlockSubArea.LAPIS_QUARRY);
        }
        if (containsPhrase(lower, "pigmen's den")
                || containsPhrase(lower, "pigmens den")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DEEP_CAVERNS,
                    SkyBlockSubArea.PIGMENS_DEN);
        }
        if (containsPhrase(lower, "slimehill")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DEEP_CAVERNS,
                    SkyBlockSubArea.SLIMEHILL);
        }
        if (containsPhrase(lower, "diamond reserve")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DEEP_CAVERNS,
                    SkyBlockSubArea.DIAMOND_RESERVE);
        }
        if (containsPhrase(lower, "obsidian sanctuary")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DEEP_CAVERNS,
                    SkyBlockSubArea.OBSIDIAN_SANCTUARY);
        }
        return null;
    }

    private static SkyBlockLocation matchCrystalHollowsSub(String lower) {
        if (containsPhrase(lower, "precursor remnants")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.PRECURSOR_REMNANTS);
        }
        if (containsPhrase(lower, "goblin holdout")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.GOBLIN_HOLDOUT);
        }
        if (containsPhrase(lower, "magma fields")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.MAGMA_FIELDS);
        }
        if (containsPhrase(lower, "fairy grotto")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.FAIRY_GROTTO);
        }
        if (containsPhrase(lower, "khazad dum")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.KHAZAD_DUM);
        }
        if (containsPhrase(lower, "mithril deposits")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.MITHRIL_DEPOSITS);
        }
        if (containsPhrase(lower, "mines of divan")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.MINES_OF_DIVAN);
        }
        if (isCrystalHollowsJungle(lower)) {
            return SkyBlockLocation.of(
                    SkyBlockArea.CRYSTAL_HOLLOWS,
                    SkyBlockSubArea.JUNGLE);
        }
        return null;
    }

    private static SkyBlockLocation matchDwarvenSub(String lower) {
        if (containsPhrase(lower, "forge basin")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.FORGE_BASIN);
        }
        if (containsPhrase(lower, "the forge")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.THE_FORGE);
        }
        if (containsPhrase(lower, "cliffside veins")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.CLIFFSIDE_VEINS);
        }
        if (containsPhrase(lower, "lava springs")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.LAVA_SPRINGS);
        }
        if (containsPhrase(lower, "upper mines")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.UPPER_MINES);
        }
        if (containsPhrase(lower, "royal palace")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.ROYAL_PALACE);
        }
        if (containsPhrase(lower, "royal mines")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.ROYAL_MINES);
        }
        if (containsPhrase(lower, "palace bridge")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.PALACE_BRIDGE);
        }
        if (containsPhrase(lower, "rampart's quarry")
                || containsPhrase(lower, "ramparts quarry")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.RAMPARTS_QUARRY);
        }
        if (containsPhrase(lower, "goblin burrows")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.GOBLIN_BURROWS);
        }
        if (containsPhrase(lower, "the mist")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.THE_MIST);
        }
        if (containsPhrase(lower, "abandoned quarry")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.ABANDONED_QUARRY);
        }
        if (containsPhrase(lower, "divan's gateway")
                || containsPhrase(lower, "divans gateway")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.DIVANS_GATEWAY);
        }
        if (containsPhrase(lower, "great ice wall")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.GREAT_ICE_WALL);
        }
        if (containsPhrase(lower, "hanging court")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.HANGING_COURT);
        }
        if (containsPhrase(lower, "fossil research")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.FOSSIL_RESEARCH);
        }
        if (containsPhrase(lower, "far reserve")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.FAR_RESERVE);
        }
        if (containsPhrase(lower, "aristocrat passage")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.ARISTOCRAT_PASSAGE);
        }
        if (containsPhrase(lower, "gates to the mines")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.GATES_TO_THE_MINES);
        }
        if (containsPhrase(lower, "dwarven village")) {
            return SkyBlockLocation.of(
                    SkyBlockArea.DWARVEN_MINES,
                    SkyBlockSubArea.DWARVEN_VILLAGE);
        }
        return null;
    }

    /**
     * "Jungle" alone is a Crystal Hollows sub-area on the scoreboard.
     * Reject longer phrases that are not that zone (e.g. jungle temple).
     */
    private static boolean isCrystalHollowsJungle(String lower) {
        return containsPhrase(lower, "jungle");
    }

    private static boolean containsPhrase(String haystack, String needle) {
        if (haystack == null || needle == null || needle.isEmpty()) {
            return false;
        }
        // Scoreboard location rows normalize to a standalone area name.
        // Exact matching avoids turning unrelated narrative/status lines into
        // authoritative area evidence for context-gated mining resources.
        return haystack.equals(needle.toLowerCase(Locale.ROOT));
    }
}
