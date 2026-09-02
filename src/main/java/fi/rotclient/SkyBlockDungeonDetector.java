package fi.rotclient;

import java.util.Locale;
import java.util.Optional;

/**
 * Scoreboard-line dungeon detection for Hide Players "Only in Dungeons".
 * Fail-open when uncertain: returns empty rather than inventing dungeon state.
 */
public final class SkyBlockDungeonDetector {
    private static volatile Boolean stickyConfidentInDungeon;

    private SkyBlockDungeonDetector() {
    }

    public static Optional<Boolean> detectFromScoreboardLines(
            java.util.List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Optional.empty();
        }
        boolean sawContent = false;
        for (String line : lines) {
            Optional<Boolean> hit = detectFromLine(line);
            if (hit.isPresent() && Boolean.TRUE.equals(hit.get())) {
                return hit;
            }
            if (line != null && !line.isBlank()) {
                sawContent = true;
            }
        }
        // Hub / island sidebars must clear the sticky dungeon flag so
        // Only-in-Dungeons modules do not stay armed after leaving a floor.
        return sawContent ? Optional.of(Boolean.FALSE) : Optional.empty();
    }

    public static Optional<Boolean> detectFromLine(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String text = raw.replaceAll("§.", "")
                .replace('\u00A0', ' ')
                .toLowerCase(Locale.ROOT)
                .trim();
        if (text.contains("dungeon hub")) {
            return Optional.of(Boolean.FALSE);
        }
        if (text.contains("the catacombs")
                || text.contains("master mode")
                || text.matches(".*\\bm[1-7]\\b.*")
                || text.matches(".*\\bf[1-7]\\b.*")) {
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }

    public static void updateSticky(Optional<Boolean> observed) {
        if (observed == null || observed.isEmpty()) {
            return;
        }
        stickyConfidentInDungeon = observed.get();
    }

    public static void clear() {
        stickyConfidentInDungeon = null;
    }

    /** True only when last confident observation was dungeon. */
    public static boolean confidentlyInDungeon() {
        return Boolean.TRUE.equals(stickyConfidentInDungeon);
    }
}
