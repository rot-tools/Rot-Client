package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remaining local mining QoL policies.
 * Display-first; party/command helpers are cheat-tagged at catalog.
 */
public final class MiningLeftoverPolicy {
    public static final int WORM_COOLDOWN_TICKS = 620;
    public static final int PITY_MAX = 2000;

    public enum WormKind {
        NONE,
        WORM,
        SCATHA
    }

    public enum MiningEvent {
        NONE,
        GONE_WITH_THE_WIND,
        DOUBLE_POWDER,
        GOBLIN_RAID,
        BETTER_TOGETHER,
        RAFFLE,
        MITHRIL_GOURMAND
    }

    public enum NotifyKind {
        NONE,
        MINESHAFT_PORTAL,
        SUSPICIOUS_SCRAP,
        GOLDEN_GOBLIN,
        DIAMOND_GOBLIN,
        COMMISSION_COMPLETE,
        WORM_APPROACHING,
        SCATHA_PET
    }

    public enum CorpseType {
        UNKNOWN,
        LAPIS,
        UMBER,
        TUNGSTEN,
        VANGUARD
    }

    public record Pity(int current, int max) {
        public Pity {
            current = Math.max(0, current);
            max = Math.max(1, max);
        }

        public int remaining() {
            return Math.max(0, max - current);
        }

        public float percent() {
            return Math.min(100.0F, 100.0F * current / (float) max);
        }
    }

    public record CorpseCoords(int x, int y, int z) {
        public String partyLine() {
            return "x: " + x + ", y: " + y + ", z: " + z;
        }
    }

    public record TreasureDistance(double meters) {
    }

    public record DrillFuel(int remaining, int max) {
        public String hud() {
            return "Fuel " + remaining + "/" + max;
        }
    }

    public record PickaxeAbility(String name, String status) {
        public String hud() {
            return name + ": " + status;
        }

        public boolean ready() {
            return status != null && status.toUpperCase(Locale.ROOT).contains("READY");
        }
    }

    public record SkyMall(String perk) {
    }

    public record Cold(int value) {
    }

    private static final Pattern WORM_NAME = Pattern.compile(
            "^\\[Lv5] Worm\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCATHA_NAME = Pattern.compile(
            "^\\[Lv10] Scatha\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern APPROACHING = Pattern.compile(
            "^You hear the sound of something approaching\\.\\.\\.$");
    private static final Pattern SCATHA_PET = Pattern.compile(
            "PET DROP!\\s*(?:§.)*(?<pet>Scatha)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PITY = Pattern.compile(
            "Glacite Mineshafts:\\s*(?<pity>[\\d,]+)\\s*/\\s*(?<max>[\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TREASURE = Pattern.compile(
            "TREASURE:\\s*(?<meters>\\d+(?:\\.\\d+)?)m", Pattern.CASE_INSENSITIVE);
    private static final Pattern DRILL_FUEL = Pattern.compile(
            "(?:Drill\\s+)?Fuel:\\s*(?<cur>[\\d,]+)\\s*/\\s*(?<max>[\\d,]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ABILITY = Pattern.compile(
            "(?<name>Pickobulus|Mining Speed Boost|Maniac Miner|Vein Seeker|Gemstone Infusion):\\s*(?<status>.+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SKY_MALL = Pattern.compile(
            "Sky Mall:?\\s*(?<perk>.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLD = Pattern.compile(
            "Cold:\\s*-?(?<cold>\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FETCHUR = Pattern.compile(
            "^\\[NPC] Fetchur:\\s*(?:its|theyre)\\s+(?<riddle>.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FOSSIL_MUNCHER = Pattern.compile(
            "^\\[NPC] Fossil Muncher:\\s*the fossil i want\\s+(?<riddle>.+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CORPSE_LOOT = Pattern.compile(
            "(?<type>LAPIS|UMBER|TUNGSTEN|VANGUARD)\\s+CORPSE LOOT",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CORPSE_COORDS = Pattern.compile(
            "x:\\s*(?<x>-?\\d+)\\s*,\\s*y:\\s*(?<y>-?\\d+)\\s*,\\s*z:\\s*(?<z>-?\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CORPSE_TAB = Pattern.compile(
            "(?<corpse>Lapis|Umber|Tungsten|Vanguard):\\s*(?<state>NOT LOOTED|LOOTED)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_STARTED = Pattern.compile(
            "(?<event>.+?)\\s+STARTED!?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_ENDED = Pattern.compile(
            "(?<event>.+?)\\s+ENDED!?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PASSIVE_EVENT = Pattern.compile(
            "PASSIVE EVENT\\s+(?<event>.+?)\\s+RUNNING", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMISSION_COMPLETE = Pattern.compile(
            "(.+?)\\s+Commission Complete!\\s*Visit the King",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MINESHAFT_PORTAL = Pattern.compile(
            "You found a\\s+Glacite Mineshaft\\s+portal", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRAP = Pattern.compile(
            "You found a\\s+Suspicious Scrap", Pattern.CASE_INSENSITIVE);
    private static final Pattern GOLDEN_GOBLIN = Pattern.compile(
            "A Golden Goblin has spawned", Pattern.CASE_INSENSITIVE);
    private static final Pattern DIAMOND_GOBLIN = Pattern.compile(
            "A\\s+Diamond Goblin\\s+has spawned", Pattern.CASE_INSENSITIVE);
    private static final Pattern HOTM_TITLE = Pattern.compile(
            "^Heart of the Mountain$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMISSIONS_TITLE = Pattern.compile(
            "^Commissions$", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCAVATOR_TITLE = Pattern.compile(
            "^Fossil Excavator$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> FETCHUR_ANSWERS = Map.ofEntries(
            Map.entry("yellow and see through", "Yellow Stained Glass"),
            Map.entry("circular and sometimes moves", "Compass"),
            Map.entry("expensive minerals", "Mithril"),
            Map.entry("useful during celebrations", "Firework Rocket"),
            Map.entry("hot and gives energy", "Cheap / Decent / Black Coffee"),
            Map.entry("tall and can be opened", "Any Wooden Door / Iron Door"),
            Map.entry("brown and fluffy", "Rabbit Foot"),
            Map.entry("explosive but more than usual", "Superboom TNT"),
            Map.entry("wearable and grows", "Pumpkin"),
            Map.entry("shiny and makes sparks", "Flint and Steel"),
            Map.entry("green and some dudes trade stuff for it", "Emerald"),
            Map.entry("red and soft", "Red Wool"));

    private static final Map<String, String> FOSSIL_ANSWERS = Map.of(
            "lived underground and dug tunnels", "Claw Fossil",
            "had a really fancy tail", "Clubbed Fossil",
            "was the king of his kind", "Footprint Fossil",
            "lived underwater", "Helix Fossil",
            "was kinda spiny u know", "Spine Fossil",
            "lived in herds and was quite woolly", "Tusk Fossil",
            "is pretty rough to look at", "Ugly Fossil",
            "had a really pointy beak", "Webbed Fossil");

    private static final Set<String> COMMISSION_MOBS = Set.of(
            "goblin",
            "golden goblin",
            "diamond goblin",
            "star sentry",
            "glacite walker",
            "treasure hoarder",
            "automaton",
            "team treasurite",
            "yog",
            "thyst",
            "corleone",
            "sludge");

    private static final Set<String> MINING_AREA_HINTS = Set.of(
            "dwarven mines",
            "crystal hollows",
            "crystal nucleus",
            "glacite",
            "mineshaft",
            "base camp",
            "great glacite lake");

    private MiningLeftoverPolicy() {
    }

    public static boolean isMiningArea(String scoreboardOrArea) {
        String text = strip(scoreboardOrArea).toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            return false;
        }
        for (String hint : MINING_AREA_HINTS) {
            if (text.contains(hint)) {
                return true;
            }
        }
        return text.contains("hollows") || text.contains("dwarven");
    }

    public static WormKind wormKind(String nametag) {
        String name = strip(nametag);
        if (SCATHA_NAME.matcher(name).find()) {
            return WormKind.SCATHA;
        }
        if (WORM_NAME.matcher(name).find()) {
            return WormKind.WORM;
        }
        return WormKind.NONE;
    }

    public static boolean withinWormAlertRadius(int dx, int dy, int dz) {
        return Math.abs(dy) <= 4 && (Math.abs(dx) <= 2 || Math.abs(dz) <= 2);
    }

    public static int tickCooldown(int remaining) {
        return Math.max(0, remaining - 1);
    }

    public static boolean cooldownJustEnded(int previous, int next) {
        return previous > 0 && next == 0;
    }

    public static Optional<Pity> parsePity(List<String> tabLines) {
        if (tabLines == null) {
            return Optional.empty();
        }
        for (String line : tabLines) {
            Matcher matcher = PITY.matcher(strip(line));
            if (matcher.find()) {
                int current = parseInt(matcher.group("pity"));
                int max = parseInt(matcher.group("max"));
                return Optional.of(new Pity(current, max <= 0 ? PITY_MAX : max));
            }
        }
        return Optional.empty();
    }

    public static Optional<PickaxeAbility> parseAbility(List<String> tabLines) {
        if (tabLines == null) {
            return Optional.empty();
        }
        for (String line : tabLines) {
            Matcher matcher = ABILITY.matcher(strip(line));
            if (matcher.find()) {
                return Optional.of(new PickaxeAbility(
                        matcher.group("name").trim(),
                        matcher.group("status").trim()));
            }
        }
        return Optional.empty();
    }

    public static Optional<SkyMall> parseSkyMall(List<String> tabLines) {
        if (tabLines == null) {
            return Optional.empty();
        }
        for (String line : tabLines) {
            Matcher matcher = SKY_MALL.matcher(strip(line));
            if (matcher.find()) {
                return Optional.of(new SkyMall(matcher.group("perk").trim()));
            }
        }
        return Optional.empty();
    }

    public static Optional<Cold> parseCold(List<String> scoreboardLines) {
        if (scoreboardLines == null) {
            return Optional.empty();
        }
        for (String line : scoreboardLines) {
            Matcher matcher = COLD.matcher(strip(line));
            if (matcher.find()) {
                return Optional.of(new Cold(parseInt(matcher.group("cold"))));
            }
        }
        return Optional.empty();
    }

    public static Optional<TreasureDistance> parseTreasure(String actionBar) {
        Matcher matcher = TREASURE.matcher(strip(actionBar));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new TreasureDistance(Double.parseDouble(matcher.group("meters"))));
    }

    public static Optional<DrillFuel> parseDrillFuel(List<String> loreLines) {
        if (loreLines == null) {
            return Optional.empty();
        }
        for (String line : loreLines) {
            Matcher matcher = DRILL_FUEL.matcher(strip(line));
            if (matcher.find()) {
                return Optional.of(new DrillFuel(
                        parseInt(matcher.group("cur")),
                        parseInt(matcher.group("max"))));
            }
        }
        return Optional.empty();
    }

    public static Optional<String> solveFetchur(String chat) {
        Matcher matcher = FETCHUR.matcher(strip(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        String riddle = matcher.group("riddle").trim().toLowerCase(Locale.ROOT);
        String answer = FETCHUR_ANSWERS.get(riddle);
        return Optional.of(answer == null ? riddle : answer);
    }

    public static Optional<String> solveFossilMuncher(String chat) {
        Matcher matcher = FOSSIL_MUNCHER.matcher(strip(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        String riddle = matcher.group("riddle").trim().toLowerCase(Locale.ROOT);
        String answer = FOSSIL_ANSWERS.get(riddle);
        return Optional.of(answer == null ? riddle : answer);
    }

    public static Optional<CorpseType> parseCorpseLoot(String chat) {
        Matcher matcher = CORPSE_LOOT.matcher(strip(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(corpseType(matcher.group("type")));
    }

    public static Optional<CorpseCoords> parseCorpseCoords(String chat) {
        Matcher matcher = CORPSE_COORDS.matcher(strip(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new CorpseCoords(
                parseInt(matcher.group("x")),
                parseInt(matcher.group("y")),
                parseInt(matcher.group("z"))));
    }

    public static Map<CorpseType, Boolean> parseCorpseTab(List<String> tabLines) {
        Map<CorpseType, Boolean> out = new LinkedHashMap<>();
        if (tabLines == null) {
            return out;
        }
        for (String line : tabLines) {
            Matcher matcher = CORPSE_TAB.matcher(strip(line));
            if (matcher.find()) {
                out.put(
                        corpseType(matcher.group("corpse")),
                        "LOOTED".equalsIgnoreCase(matcher.group("state")));
            }
        }
        return out;
    }

    public static MiningEvent parseEvent(String text) {
        String stripped = strip(text);
        Matcher started = EVENT_STARTED.matcher(stripped);
        if (started.find()) {
            return eventFromName(started.group("event"));
        }
        Matcher ended = EVENT_ENDED.matcher(stripped);
        if (ended.find()) {
            return eventFromName(ended.group("event"));
        }
        Matcher passive = PASSIVE_EVENT.matcher(stripped);
        if (passive.find()) {
            return eventFromName(passive.group("event"));
        }
        return eventFromName(stripped);
    }

    public static NotifyKind classifyChat(String chat) {
        String text = strip(chat);
        if (APPROACHING.matcher(text).matches()) {
            return NotifyKind.WORM_APPROACHING;
        }
        if (SCATHA_PET.matcher(text).find()) {
            return NotifyKind.SCATHA_PET;
        }
        if (MINESHAFT_PORTAL.matcher(text).find()) {
            return NotifyKind.MINESHAFT_PORTAL;
        }
        if (SCRAP.matcher(text).find()) {
            return NotifyKind.SUSPICIOUS_SCRAP;
        }
        if (DIAMOND_GOBLIN.matcher(text).find()) {
            return NotifyKind.DIAMOND_GOBLIN;
        }
        if (GOLDEN_GOBLIN.matcher(text).find()) {
            return NotifyKind.GOLDEN_GOBLIN;
        }
        if (COMMISSION_COMPLETE.matcher(text).find()) {
            return NotifyKind.COMMISSION_COMPLETE;
        }
        return NotifyKind.NONE;
    }

    public static String titleFor(NotifyKind kind) {
        return switch (kind) {
            case MINESHAFT_PORTAL -> "Mineshaft portal";
            case SUSPICIOUS_SCRAP -> "Suspicious Scrap";
            case GOLDEN_GOBLIN -> "Golden Goblin";
            case DIAMOND_GOBLIN -> "Diamond Goblin";
            case COMMISSION_COMPLETE -> "Commission complete";
            case WORM_APPROACHING -> "Worm approaching";
            case SCATHA_PET -> "Scatha pet";
            case NONE -> "";
        };
    }

    public static String scathaPartyLine(WormKind kind) {
        if (kind == WormKind.SCATHA) {
            return "Scatha";
        }
        if (kind == WormKind.WORM) {
            return "Worm";
        }
        return "";
    }

    public static String kingCallCommand() {
        return "/call mismyla";
    }

    public static boolean isHotmScreen(String title) {
        return HOTM_TITLE.matcher(strip(title)).matches();
    }

    public static boolean isCommissionsScreen(String title) {
        return COMMISSIONS_TITLE.matcher(strip(title)).matches();
    }

    public static boolean isFossilExcavatorScreen(String title) {
        return EXCAVATOR_TITLE.matcher(strip(title)).matches();
    }

    public static boolean isCompletedCommissionBook(List<String> lore) {
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            if (strip(line).toUpperCase(Locale.ROOT).contains("COMPLETED")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCommissionMob(String nametag) {
        String name = strip(nametag).toLowerCase(Locale.ROOT);
        if (name.isBlank()) {
            return false;
        }
        for (String mob : COMMISSION_MOBS) {
            if (name.contains(mob)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> hudLines(
            Pity pity,
            PickaxeAbility ability,
            DrillFuel fuel,
            TreasureDistance treasure,
            MiningEvent event,
            int wormCooldown,
            WormKind lastWorm,
            SkyMall skyMall,
            Map<CorpseType, Boolean> corpses) {
        List<String> lines = new ArrayList<>();
        if (event != null && event != MiningEvent.NONE) {
            lines.add("Event: " + eventLabel(event));
        }
        if (pity != null) {
            lines.add("Pity " + pity.current() + "/" + pity.max()
                    + " (" + Math.round(pity.percent()) + "%)");
        }
        if (ability != null) {
            lines.add(ability.hud());
        }
        if (fuel != null) {
            lines.add(fuel.hud());
        }
        if (treasure != null) {
            lines.add("Treasure " + treasure.meters() + "m");
        }
        if (wormCooldown > 0) {
            lines.add("Worm CD " + secondsLabel(wormCooldown));
        } else if (lastWorm != null && lastWorm != WormKind.NONE) {
            lines.add("Last " + (lastWorm == WormKind.SCATHA ? "Scatha" : "Worm"));
        }
        if (skyMall != null && skyMall.perk() != null && !skyMall.perk().isBlank()) {
            lines.add("Sky Mall: " + skyMall.perk());
        }
        if (corpses != null && !corpses.isEmpty()) {
            StringBuilder corpse = new StringBuilder("Corpses");
            for (Map.Entry<CorpseType, Boolean> entry : corpses.entrySet()) {
                corpse.append(" ").append(entry.getKey().name().charAt(0));
                corpse.append(entry.getValue() ? "+" : "-");
            }
            lines.add(corpse.toString());
        }
        return lines;
    }

    public static String eventLabel(MiningEvent event) {
        return switch (event) {
            case GONE_WITH_THE_WIND -> "Gone with the Wind";
            case DOUBLE_POWDER -> "2x Powder";
            case GOBLIN_RAID -> "Goblin Raid";
            case BETTER_TOGETHER -> "Better Together";
            case RAFFLE -> "Raffle";
            case MITHRIL_GOURMAND -> "Mithril Gourmand";
            case NONE -> "";
        };
    }

    public static CorpseType corpseType(String raw) {
        if (raw == null) {
            return CorpseType.UNKNOWN;
        }
        return switch (raw.trim().toUpperCase(Locale.ROOT)) {
            case "LAPIS" -> CorpseType.LAPIS;
            case "UMBER" -> CorpseType.UMBER;
            case "TUNGSTEN" -> CorpseType.TUNGSTEN;
            case "VANGUARD" -> CorpseType.VANGUARD;
            default -> CorpseType.UNKNOWN;
        };
    }

    private static MiningEvent eventFromName(String raw) {
        String name = strip(raw).toLowerCase(Locale.ROOT);
        if (name.contains("gone with the wind") || (name.contains("gone") && name.contains("wind"))) {
            return MiningEvent.GONE_WITH_THE_WIND;
        }
        if (name.contains("2x powder") || name.contains("double powder")) {
            return MiningEvent.DOUBLE_POWDER;
        }
        if (name.contains("goblin raid")) {
            return MiningEvent.GOBLIN_RAID;
        }
        if (name.contains("better together")) {
            return MiningEvent.BETTER_TOGETHER;
        }
        if (name.contains("raffle")) {
            return MiningEvent.RAFFLE;
        }
        if (name.contains("mithril gourmand")) {
            return MiningEvent.MITHRIL_GOURMAND;
        }
        return MiningEvent.NONE;
    }

    private static String secondsLabel(int ticks) {
        int seconds = Math.max(0, (ticks + 19) / 20);
        return seconds + "s";
    }

    private static int parseInt(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(raw.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return AutoConversationPolicy.stripFormatting(raw).trim();
    }
}
