// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class WarpCatalog {
    record Destination(String name, String command, List<String> aliases) { }

    // SkyBlock Fast Travel destinations and their server aliases, checked 2026-11-22.
    static final List<Destination> DESTINATIONS = List.of(
            warp("Arachne's Sanctuary", "arachne", "sanctuary"),
            warp("Backwater Bayou", "bayou", "backwater"),
            warp("Carnival", "carnival"),
            warp("Crimson Isle", "isle", "crimson", "nether", "ci"),
            warp("Critter Safari Entrance", "safari", "safari_zone", "crittersafari", "critter_safari"),
            warp("Crystal Hollows", "crystals", "hollows", "ch"),
            warp("Crystal Nucleus", "nucleus", "cn", "nuc"),
            warp("Deep Caverns", "deep"),
            warp("Dragon's Nest", "drag", "dragons"),
            warp("Dragontail", "dragontail"),
            warp("Dungeon Hub", "dungeon_hub", "dungeons", "dhub", "dh"),
            warp("Dwarven Base Camp", "basecamp", "base", "camp", "glacite", "tunnel", "tunnels", "gt"),
            warp("Dwarven Mines", "dwarves", "dmines", "mines", "deeper"),
            warp("Elizabeth", "elizabeth"),
            warp("Forgotten Skull", "skull", "kuudra"),
            warp("Glowing Mushroom Cave", "glowing", "moby"),
            warp("Gold Mine", "gold"),
            warp("Howling Cave", "howl", "howling_cave", "wolf_cave"),
            warp("Hub Crypts", "crypts", "crypt"),
            warp("Jerry's Workshop", "jerry", "winter", "workshop", "/savethejerrys"),
            warp("Jungle Island", "jungle"),
            warp("Lotus Atoll", "lotus", "atoll"),
            warp("Moonglade Marsh", "moonglade", "galatea", "marsh"),
            warp("Murkwater Loch", "murkwater", "murk", "murkwater_loch", "loch"),
            warp("Museum", "museum"),
            warp("Mushroom Desert", "desert"),
            warp("Private Island", "home", "island", "/is", "/island", "/privateisland"),
            warp("Ruins", "castle"),
            warp("Scarleton", "scarleton"),
            warp("Sirius' Shack", "da"),
            warp("SkyBlock Hub", "hub", "village", "/hub"),
            warp("Smoldering Tomb", "smoldering", "smold", "smoldering_tomb", "tomb"),
            warp("Spider Mound", "mound", "top", "nest"),
            warp("Spider's Den", "spider", "spiders"),
            warp("Taylor's Shop", "taylor"),
            warp("The Barn", "barn", "farming"),
            warp("The End", "end"),
            warp("The Forge", "forge", "/warpforge"),
            warp("The Garden", "garden"),
            warp("The Park", "park", "trees"),
            warp("The Rift", "rift", "the_rift"),
            warp("The Wasteland", "wasteland"),
            warp("Torrhus Canyon", "torrhus", "canyon", "torrhus_canyon"),
            warp("Torrhus Springs", "springs", "spring", "torrhus_springs"),
            warp("Trade Center", "stonks"),
            warp("Trapper's Den", "trapper", "trap"),
            warp("Void Sepulture", "void", "sepulture"),
            warp("Wizard Tower", "wizard", "tower", "wiz", "wizard_tower")
    );

    private static final Map<String, Destination> BY_COMMAND = index();

    private WarpCatalog() { }

    private static Destination warp(String name, String target, String... aliases) {
        return new Destination(name, "/warp " + target, List.of(aliases));
    }

    private static Map<String, Destination> index() {
        Map<String, Destination> result = new LinkedHashMap<>();
        for (Destination destination : DESTINATIONS) {
            result.put(destination.command(), destination);
            for (String alias : destination.aliases()) {
                result.put(alias.startsWith("/") ? alias : "/warp " + alias, destination);
            }
        }
        return Map.copyOf(result);
    }

    static String normalized(String command) {
        String value = command.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        int space = value.indexOf(' ');
        String root = space < 0 ? value : value.substring(0, space);
        return switch (root) {
            case "/warps", "/travel" -> space < 0 ? "/warp" : "/warp" + value.substring(space);
            default -> value;
        };
    }

    static Destination find(String normalizedCommand) { return BY_COMMAND.get(normalizedCommand); }
}
