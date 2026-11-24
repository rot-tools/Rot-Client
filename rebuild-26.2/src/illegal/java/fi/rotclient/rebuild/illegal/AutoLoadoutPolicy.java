package fi.rotclient.rebuild.illegal;

import java.util.Locale;
import java.util.List;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.regex.Pattern;

/** Compatibility with the server's 4-by-3 Loadouts menu. */
final class AutoLoadoutPolicy {
    private static final Pattern MENU = Pattern.compile("^\\(([0-9]{1,2})/([0-9]{1,2})\\) Loadouts$");

    private AutoLoadoutPolicy() {}

    static int menuCapacity(String title) {
        var match = MENU.matcher(title.strip());
        if (!match.matches()) return 0;
        int current = Integer.parseInt(match.group(1)), capacity = Integer.parseInt(match.group(2));
        return current <= capacity && capacity <= AutoLoadoutSettings.SLOT_COUNT ? capacity : 0;
    }

    static int containerSlot(int binding) {
        if (binding < 0 || binding >= AutoLoadoutSettings.SLOT_COUNT) throw new IllegalArgumentException("Invalid loadout slot");
        return 14 + binding / 3 * 9 + binding % 3;
    }

    static boolean equipped(List<String> lore) {
        for (String line : lore) {
            String state = line.strip().toLowerCase(Locale.ROOT);
            if (state.equals("equipped") || state.equals("currently equipped!") || state.equals("currently equipped")
                    || state.equals("selected") || state.equals("currently selected!")) return true;
        }
        if (lore.stream().map(line -> line.strip().toLowerCase(Locale.ROOT))
                .anyMatch(line -> line.contains("click to equip") || line.contains("click to load"))) return false;
        // In the reviewed Loadouts layout the equipped entry omits the action line before the edit hint.
        return lore.size() >= 2 && lore.get(lore.size() - 2).isBlank()
                && lore.getLast().toLowerCase(Locale.ROOT).contains("click to edit");
    }

    /** Keeps the close request alive through short server-side menu refreshes. */
    static final class CloseWindow {
        private static final int REFRESH_TICKS = 16;
        private final long closeAt;
        private final Set<Object> closed = Collections.newSetFromMap(new IdentityHashMap<>());

        CloseWindow(long closeAt) {
            this.closeAt = closeAt;
        }

        boolean expired(long tick) {
            return tick > closeAt + REFRESH_TICKS || closed.size() >= 3;
        }

        boolean shouldClose(long tick, Object menu, boolean cursorEmpty) {
            return tick >= closeAt && !expired(tick) && menu != null && cursorEmpty && closed.add(menu);
        }
    }

    static boolean unavailable(String title, List<String> lore) {
        String name = title.strip().toLowerCase(Locale.ROOT);
        if (name.equals("locked") || name.equals("empty slot") || name.equals("empty loadout")
                || name.startsWith("locked loadout") || name.startsWith("create loadout")) return true;
        return lore.stream().map(line -> line.strip().toLowerCase(Locale.ROOT))
                .anyMatch(line -> line.equals("click to create!") || line.equals("click to create a loadout!"));
    }
}
