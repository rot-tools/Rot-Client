package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Combined sea-creature table + rare list + barn-cap/timer rules.
 * Chat matching is formatting-stripped.
 */
public final class FishingCreaturesPolicy {
    public static final int SEA_CREATURE_CAP = 10;
    public static final int DEFAULT_TIMER_SECONDS = 340;
    public static final int MAX_TIMER_SECONDS = 600;
    public static final int DEFAULT_AUTO_DELAY = 4;
    public static final int MAX_AUTO_DELAY = 40;
    public static final String DEFAULT_MIN_RARITY = "LEGENDARY";
    public static final List<String> RARITIES = List.of(
            "COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC");

    private static final Pattern DOUBLE_HOOK =
            Pattern.compile("Double Hook!(?: Woot woot!)?", Pattern.CASE_INSENSITIVE);
    private static final List<Creature> CREATURES = buildCreatures();
    private static final Map<String, Creature> BY_SPAWN = indexBySpawn();

    public record Creature(String name, String spawn, String rarity, String category) {
        public int rarityRank() {
            return rankOf(rarity);
        }

        public int color() {
            return colorOf(rarity);
        }
    }

    public record ChatSignal(boolean doubleHook, Creature spawn) {
    }

    public record CapState(int count, boolean atCap, long oldestAgeMs, boolean timerDue) {
    }

    private FishingCreaturesPolicy() {
    }

    public static List<Creature> creatures() {
        return CREATURES;
    }

    public static int clampTimer(int seconds) {
        return Math.max(30, Math.min(MAX_TIMER_SECONDS, seconds));
    }

    public static int clampAutoDelay(int ticks) {
        return Math.max(1, Math.min(MAX_AUTO_DELAY, ticks));
    }

    public static String normalizeRarity(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_MIN_RARITY;
        }
        String upper = value.trim().toUpperCase(Locale.ROOT);
        return RARITIES.contains(upper) ? upper : DEFAULT_MIN_RARITY;
    }

    public static int rankOf(String rarity) {
        int idx = RARITIES.indexOf(normalizeRarity(rarity));
        return Math.max(0, idx);
    }

    public static int colorOf(String rarity) {
        return switch (normalizeRarity(rarity)) {
            case "COMMON" -> 0xFFAAAAAA;
            case "UNCOMMON" -> 0xFF55FF55;
            case "RARE" -> 0xFF5555FF;
            case "EPIC" -> 0xFFAA00AA;
            case "MYTHIC" -> 0xFFFF55FF;
            default -> 0xFFFFAA00;
        };
    }

    public static String strip(String text) {
        if (text == null) {
            return "";
        }
        return ChatTextPolicy.stripFormatting(text).replace('\u00A0', ' ').trim();
    }

    public static String normalizeSpawn(String stripped) {
        String text = strip(stripped);
        return text.replace("It's body", "Its body");
    }

    public static boolean isDoubleHook(String stripped) {
        return DOUBLE_HOOK.matcher(strip(stripped)).find();
    }

    public static Creature matchSpawn(String stripped) {
        return BY_SPAWN.get(normalizeSpawn(stripped));
    }

    public static ChatSignal inspectChat(String stripped) {
        String text = strip(stripped);
        if (text.isEmpty()) {
            return new ChatSignal(false, null);
        }
        return new ChatSignal(isDoubleHook(text), matchSpawn(text));
    }

    public static Creature matchNametag(String nametag) {
        String text = strip(nametag);
        if (text.isEmpty()) {
            return null;
        }
        Creature best = null;
        int bestLen = 0;
        for (Creature creature : CREATURES) {
            if (text.contains(creature.name()) && creature.name().length() > bestLen) {
                best = creature;
                bestLen = creature.name().length();
            }
        }
        return best;
    }

    public static boolean hasCreatureHealthMarker(String nametag) {
        String text = strip(nametag);
        return text.indexOf('\u2764') >= 0
                || text.indexOf('\u2665') >= 0
                || text.contains("\u2602")
                || text.contains("\u2668");
    }

    public static boolean looksLikeCreatureHologram(String nametag) {
        return matchNametag(nametag) != null && hasCreatureHealthMarker(nametag);
    }

    public static boolean meetsRarity(String creatureRarity, String minimum) {
        return rankOf(creatureRarity) >= rankOf(minimum);
    }

    public static boolean shouldTitle(
            boolean moduleEnabled,
            boolean titlesOn,
            String creatureRarity,
            String minimum) {
        return moduleEnabled && titlesOn && meetsRarity(creatureRarity, minimum);
    }

    public static boolean shouldCapNotify(boolean moduleEnabled, boolean capOn, int liveCount) {
        return moduleEnabled && capOn && liveCount >= SEA_CREATURE_CAP;
    }

    public static boolean shouldTimerNotify(
            boolean moduleEnabled,
            boolean timerOn,
            int liveCount,
            long oldestAgeMs,
            int timerSeconds) {
        if (!moduleEnabled || !timerOn || liveCount <= 0) {
            return false;
        }
        long due = clampTimer(timerSeconds) * 1000L;
        return oldestAgeMs >= due && oldestAgeMs < due + 250L;
    }

    public static CapState capState(int count, long oldestAgeMs, int timerSeconds) {
        int clamped = Math.max(0, count);
        return new CapState(
                clamped,
                clamped >= SEA_CREATURE_CAP,
                Math.max(0L, oldestAgeMs),
                shouldTimerNotify(true, true, clamped, oldestAgeMs, timerSeconds));
    }

    public static String compactLine(Creature creature, boolean doubleHook) {
        if (creature == null) {
            return "";
        }
        String suffix = doubleHook ? " x2" : "";
        return "SC " + creature.name() + suffix;
    }

    public static boolean shouldHideCommonNametag(boolean hideCommon, Creature creature) {
        return hideCommon && creature != null && creature.rarityRank() <= rankOf("UNCOMMON");
    }

    public static boolean shouldEsp(boolean moduleEnabled, boolean espOn, Creature creature, String minRarity) {
        return moduleEnabled && espOn && creature != null && meetsRarity(creature.rarity(), minRarity);
    }

    public static boolean shouldAutoAttack(
            boolean moduleEnabled,
            boolean autoAttack,
            boolean lookingAtTracked,
            boolean screenOpen) {
        return moduleEnabled && autoAttack && lookingAtTracked && !screenOpen;
    }

    public static boolean shouldPartyAnnounce(
            boolean moduleEnabled,
            boolean partyOn,
            Creature creature,
            String minRarity) {
        return moduleEnabled && partyOn && creature != null && meetsRarity(creature.rarity(), minRarity);
    }

    public static List<String> hudLines(
            boolean hudOn,
            int count,
            long oldestAgeMs,
            int timerSeconds,
            String lastName,
            boolean doubleHookPending) {
        if (!hudOn) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("Sea Creatures " + Math.max(0, count) + "/" + SEA_CREATURE_CAP);
        if (count > 0) {
            long remain = Math.max(0L, clampTimer(timerSeconds) * 1000L - Math.max(0L, oldestAgeMs));
            lines.add("Barn " + formatSeconds(remain / 1000.0D));
        }
        if (lastName != null && !lastName.isBlank()) {
            lines.add("Last " + lastName + (doubleHookPending ? " x2" : ""));
        }
        return List.copyOf(lines);
    }

    public static String formatSeconds(double seconds) {
        if (seconds < 0.0D) {
            seconds = 0.0D;
        }
        int total = (int) Math.round(seconds);
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }

    private static Map<String, Creature> indexBySpawn() {
        Map<String, Creature> map = new LinkedHashMap<>();
        for (Creature creature : CREATURES) {
            map.put(normalizeSpawn(creature.spawn()), creature);
        }
        return Map.copyOf(map);
    }

    private static List<Creature> buildCreatures() {
        String[] rows = {
                "Squid|A Squid appeared.|COMMON|BASIC",
                "Sea Walker|You caught a Sea Walker.|COMMON|BASIC",
                "Sea Witch|It looks like you've disrupted the Sea Witch's brewing session. Watch out, she's furious!|UNCOMMON|BASIC",
                "Sea Archer|You reeled in a Sea Archer.|UNCOMMON|BASIC",
                "Rider of the Deep|The Rider of the Deep has emerged.|UNCOMMON|BASIC",
                "Catfish|Huh? A Catfish!|RARE|BASIC",
                "Sea Leech|Gross! A Sea Leech!|RARE|BASIC",
                "Guardian Defender|You've discovered a Guardian Defender of the sea.|EPIC|BASIC",
                "Deep Sea Protector|You have awoken the Deep Sea Protector, prepare for a battle!|EPIC|BASIC",
                "Water Hydra|The Water Hydra has come to test your strength.|LEGENDARY|BASIC",
                "Dumpster Diver|A Dumpster Diver has emerged from the swamp!|UNCOMMON|BACKWATER_BAYOU",
                "Trash Gobbler|The Trash Gobbler is hungry for you!|COMMON|BACKWATER_BAYOU",
                "Banshee|The desolate wail of a Banshee breaks the silence.|RARE|BACKWATER_BAYOU",
                "Bayou Sludge|A swampy mass of slime emerges, the Bayou Sludge!|EPIC|BACKWATER_BAYOU",
                "Alligator|A long snout breaks the surface of the water. It's an Alligator!|LEGENDARY|BACKWATER_BAYOU",
                "Titanoboa|A massive Titanoboa surfaces. Its body stretches as far as the eye can see.|MYTHIC|BACKWATER_BAYOU",
                "Atoll Croaker|An inquisitive Atoll Croaker takes the bait!|COMMON|LOTUS_ATOLL",
                "Puddle Jumper|A Puddle Jumper is preparing for liftoff—cast your rod into it and hold on tight!|LEGENDARY|LOTUS_ATOLL",
                "Lotus Guardian|A Lotus Guardian emerges, ready to protect the Atoll.|UNCOMMON|LOTUS_ATOLL",
                "gorF|What even is that?! A... gorF?|RARE|LOTUS_ATOLL",
                "Frog Prince|Bow down before the Frog Prince... or pay the hefty price!|MYTHIC|LOTUS_ATOLL",
                "Drowned Captain|A Drowned Captain takes hold of your bobber!|EPIC|LOTUS_ATOLL",
                "Bogged|You've hooked a Bogged!|COMMON|MOONGLADE_MARSH",
                "Wetwing|Look! A Wetwing emerges!|UNCOMMON|MOONGLADE_MARSH",
                "Tadgang|A gang of Liltads!|RARE|MOONGLADE_MARSH",
                "Ent|You've hooked an Ent, as ancient as the forest itself.|EPIC|MOONGLADE_MARSH",
                "Stridersurfer|You caught a Stridersurfer.|RARE|MOONGLADE_MARSH",
                "The Loch Emperor|The Loch Emperor arises from the depths.|LEGENDARY|MOONGLADE_MARSH",
                "Nessie|You've caused a disturbance in the loch. Could it be... Nessie?|MYTHIC|MOONGLADE_MARSH",
                "Haggard|A Haggard stumbles to the shore, ready for a fight!|COMMON|TORRHUS_CANYON",
                "Brineling|A Brineling interrupts you with a stream of bubbles!|UNCOMMON|TORRHUS_CANYON",
                "Sprawl|A Sprawl emerges from the blue, and it's looking for you!|RARE|TORRHUS_CANYON",
                "Torrid|The laughter of a Torrid echoes through the air.|EPIC|TORRHUS_CANYON",
                "Silkbreeze|Something zips through the air - it's a Silkbreeze!|LEGENDARY|TORRHUS_CANYON",
                "Giant Isopod|A Giant Isopod was dredged up from the depths!|MYTHIC|TORRHUS_CANYON",
                "Fried Chicken|Smells of burning. Must be a Fried Chicken.|COMMON|CRIMSON_ISLE",
                "Volcanic Snail|You feel a burning sensation as you reel in a Volcanic Snail!|UNCOMMON|CRIMSON_ISLE",
                "Fireproof Witch|Trouble's brewing, it's a Fireproof Witch!|RARE|CRIMSON_ISLE",
                "Magma Slug|From beneath the lava appears a Magma Slug.|UNCOMMON|CRIMSON_ISLE",
                "Moogma|You hear a faint Moo from the lava... A Moogma appears.|UNCOMMON|CRIMSON_ISLE",
                "Lava Leech|A small but fearsome Lava Leech emerges.|RARE|CRIMSON_ISLE",
                "Pyroclastic Worm|You feel the heat radiating as a Pyroclastic Worm surfaces.|RARE|CRIMSON_ISLE",
                "Magma Pillar|A Magma Pillar rises from the lava.|EPIC|CRIMSON_ISLE",
                "Lava Flame|A Lava Flame flies out from beneath the lava.|RARE|CRIMSON_ISLE",
                "Fire Eel|A Fire Eel slithers out from the depths.|RARE|CRIMSON_ISLE",
                "Taurus|Taurus and his steed emerge.|EPIC|CRIMSON_ISLE",
                "Thunder|You hear a massive rumble as Thunder emerges.|MYTHIC|CRIMSON_ISLE",
                "Fiery Scuttler|A Fiery Scuttler inconspicuously waddles up to you, friends in tow.|LEGENDARY|CRIMSON_ISLE",
                "Lord Jawbus|You have angered a legendary creature... Lord Jawbus has arrived.|MYTHIC|CRIMSON_ISLE",
                "Ragnarok|The sky darkens and the air thickens. The end times are upon us: Ragnarok is here.|MYTHIC|CRIMSON_ISLE",
                "Frosty|It's a snowman! He looks harmless.|COMMON|WINTER_ISLAND",
                "Frozen Steve|Frozen Steve fell into the pond long ago, never to resurface...until now!|COMMON|WINTER_ISLAND",
                "Grinch|The Grinch stole Jerry's Gifts...get them back!|UNCOMMON|WINTER_ISLAND",
                "Yeti|What is this creature!?|LEGENDARY|WINTER_ISLAND",
                "Nutcracker|You found a forgotten Nutcracker laying beneath the ice.|LEGENDARY|WINTER_ISLAND",
                "Reindrake|A Reindrake forms from the depths.|LEGENDARY|WINTER_ISLAND",
                "Jumpin' Jack|Watch out! It's Jumpin' Jack.|COMMON|SPOOKY",
                "Scarecrow|Phew! It's only a Scarecrow.|COMMON|SPOOKY",
                "Nightmare|You hear trotting from beneath the waves, you caught a Nightmare.|RARE|SPOOKY",
                "Werewolf|It must be a full moon, a Werewolf appears.|EPIC|SPOOKY",
                "Phantom Fisher|The spirit of a long lost Phantom Fisher has come to haunt you.|LEGENDARY|SPOOKY",
                "Grim Reaper|This can't be! The manifestation of death himself!|LEGENDARY|SPOOKY",
                "Nurse Shark|A tiny fin emerges from the water, you've caught a Nurse Shark.|COMMON|SHARK",
                "Blue Shark|You spot a fin as blue as the water it came from, it's a Blue Shark.|UNCOMMON|SHARK",
                "Tiger Shark|A striped beast bounds from the depths, the wild Tiger Shark!|EPIC|SHARK",
                "Great White Shark|Hide no longer, a Great White Shark has tracked your scent and thirsts for your blood!|LEGENDARY|SHARK",
                "Frog Man|Is it a frog? Is it a man? Well, yes, sorta, IT'S FROG MAN!!!!!!|COMMON|HOTSPOT",
                "Inkling|You get an inkling that you've caught... an Inkling!|UNCOMMON|HOTSPOT",
                "Snapping Turtle|A Snapping Turtle is coming your way, and it's ANGRY!|RARE|HOTSPOT",
                "Blue Ringed Octopus|A garish set of tentacles arise. It's a Blue Ringed Octopus!|LEGENDARY|HOTSPOT",
                "Wiki Tiki|The water bubbles and froths. A massive form emerges- you have disturbed the Wiki Tiki! You shall pay the price.|MYTHIC|HOTSPOT",
                "Small Mithril Grubber|A leech of the mines surfaces... you've caught a Mithril Grubber.|UNCOMMON|SPECIAL",
                "Medium Mithril Grubber|A leech of the mines surfaces... you've caught a Medium Mithril Grubber.|UNCOMMON|SPECIAL",
                "Large Mithril Grubber|A leech of the mines surfaces... you've caught a Large Mithril Grubber.|UNCOMMON|SPECIAL",
                "Bloated Mithril Grubber|A leech of the mines surfaces... you've caught a Bloated Mithril Grubber.|UNCOMMON|SPECIAL",
                "Oasis Sheep|An Oasis Sheep appears from the water.|UNCOMMON|SPECIAL",
                "Oasis Rabbit|An Oasis Rabbit appears from the water.|UNCOMMON|SPECIAL",
                "Carrot King|Is this even a fish? It's the Carrot King!|RARE|SPECIAL",
                "Agarimoo|Your Chumcap Bucket trembles, it's an Agarimoo.|RARE|SPECIAL",
                "Water Worm|A Water Worm surfaces!|RARE|SPECIAL",
                "Poisoned Water Worm|A Poisoned Water Worm surfaces!|RARE|SPECIAL",
                "Flaming Worm|A Flaming Worm surfaces from the depths!|RARE|SPECIAL",
                "Lava Blaze|A Lava Blaze has surfaced from the depths!|EPIC|SPECIAL",
                "Lava Pigman|A Lava Pigman arose from the depths!|EPIC|SPECIAL",
                "Abyssal Miner|An Abyssal Miner breaks out of the water!|LEGENDARY|SPECIAL",
                "Plhlegblast|WOAH! A Plhlegblast appeared.|MYTHIC|SPECIAL",
                "Loch Emperor|The Loch Emperor arises from the depths.|LEGENDARY|MOONGLADE_MARSH"
        };
        List<Creature> out = new ArrayList<>();
        for (String row : rows) {
            String[] parts = row.split("\\|", 4);
            out.add(new Creature(parts[0], parts[1], parts[2], parts[3]));
        }
        // Titanoboa typo variant
        out.add(new Creature(
                "Titanoboa",
                "A massive Titanoboa surfaces. It's body stretches as far as the eye can see.",
                "MYTHIC",
                "BACKWATER_BAYOU"));
        return List.copyOf(out);
    }
}
