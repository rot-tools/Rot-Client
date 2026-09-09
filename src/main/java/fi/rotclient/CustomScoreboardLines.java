package fi.rotclient;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hypixel SkyBlock sidebar line shapes used by the custom board. Minecraft-free
 * so tests can drive matching. These are Hypixel widget facts, not a port of
 * another mod's class tree.
 */
final class CustomScoreboardLines {

    private static final Pattern PURSE = Pattern.compile(
            "(?:§.)*(?:Piggy|Purse)\\s*:?\\s*(?:§.)*(?<amount>[\\d,.]+)");
    private static final Pattern MOTES = Pattern.compile(
            "(?:§.)*Motes: (?:§.)*(?<amount>[\\d,]+)");
    private static final Pattern COPPER = Pattern.compile(
            "(?:§.)*Copper: (?:§.)*(?<amount>[\\d,]+)");
    private static final Pattern SOWDUST = Pattern.compile(
            "\\s?(?:§.)*Sowdust: (?:§.)*(?<amount>[\\d,]+)");
    private static final Pattern GEMS = Pattern.compile(
            "(?:§.)*Gems: (?:§.)*(?<amount>[\\d,]+)");
    private static final Pattern BITS = Pattern.compile(
            "(?:§.)*Bits: (?:§.)*(?<amount>[\\d,.]+)");
    private static final Pattern NORTH_STARS = Pattern.compile(
            "(?:§.)*North Stars: (?:§.)*(?<amount>[\\w,]+)");
    private static final Pattern SOULFLOW = Pattern.compile(
            "(?:§.)*Soulflow: (?:§.)*(?<amount>[\\d,]+)");
    private static final Pattern BANK = Pattern.compile(
            "(?:§.)*Bank: (?:§.)*(?<amount>[\\d,.kKmMbB]+)");
    private static final Pattern POWDER = Pattern.compile(
            "(?:§.)*᠅ §.(?<type>Gemstone|Mithril|Glacite)(?: Powder)?(?:§.)*:? (?:§.)*(?<amount>[\\d,.]*)");
    private static final Pattern POWDER_PLAIN = Pattern.compile(
            "(?i)(mithril|gemstone|glacite)(?:\\s*powder)?\\s*:?\\s*(?<amount>[\\d,.kmb]+)");
    private static final Pattern HEAT = Pattern.compile("(?i)(?:§.)*Heat(?:§.)*:\\s*(?:§.)*.+");
    private static final Pattern COLD = Pattern.compile("(?i)(?:§.)*Cold(?:§.)*:\\s*(?:§.)*.+");
    private static final Pattern LOCATION = Pattern.compile(".*[⏣📍📌].*");
    private static final Pattern LOCATION_NAME = Pattern.compile(
            "(?i)(?:^|\\s)(your island|private island|guest island|the hub|dungeon hub)\\b");
    private static final Pattern PLOT = Pattern.compile("\\s*(?:§.)*Plot (?:§.)*-.*");
    private static final Pattern DATE = Pattern.compile(
            "(?i)\\s*(?:(?:Late|Early) )?(?:Spring|Summer|Autumn|Fall|Winter) \\d+(?:st|nd|rd|th)?.*");
    private static final Pattern TIME = Pattern.compile(
            "\\s*(?:§.)*\\d{1,2}:\\d{2}\\s*(?:am|pm)\\s*(?<symbol>§b☽|§e☀|§.⚡|§.☔)?.*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LOBBY = Pattern.compile(
            "\\s*(?:§.)*(?:\\d{2}/?){3} (?:§.)*(?<code>\\S+).*");
    private static final Pattern LOBBY_CODE = Pattern.compile("(?i)\\b((?:m|mini|mega)[0-9A-Z]+)\\b");
    private static final Pattern VISITING = Pattern.compile(
            "\\s*(?:§.)*✌ (?:§.)*\\((?:§.)*\\d+(?:§.)*/(?:§.)*\\d+(?:§.)*\\)");
    private static final Pattern PROFILE = Pattern.compile(
            "\\s*(?:§.)*♲ (?:§.)*Ironman|(?:§.)*☀ (?:§.)*Stranded|(?:§.)*Ⓑ (?:§.)*Bingo",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FOOTER = Pattern.compile("(?i)(?:§.)*(?:www|alpha)\\.hypixel\\.net");
    private static final Pattern OBJECTIVE = Pattern.compile("(?:§.)*(?:Objective|Quest).*");
    private static final Pattern SLAYER = Pattern.compile("(?i)(?:§.)*Slayer Quest.*");
    private static final Pattern ARROWS = Pattern.compile("(?i).*(?:arrow|quiver).*");
    private static final Pattern DARK_AUCTION_ITEM = Pattern.compile("(?i)Current Item:");
    private static final Pattern TRACKER_MOB = Pattern.compile("(?i)(?:§.)*Tracker Mob Location:");
    private static final Pattern JACOB = Pattern.compile("(?i)(?:§.)*Jacob's Contest.*");
    private static final Pattern AGATHA = Pattern.compile("(?i)(?:§.)*Agatha's Contest.*");
    private static final Pattern MIRIA = Pattern.compile("(?i)(?:§.)*Miria's Contest.*");

    private CustomScoreboardLines() {
    }

    enum Kind {
        PURSE,
        MOTES,
        BANK,
        BITS,
        COPPER,
        SOWDUST,
        GEMS,
        HEAT,
        COLD,
        NORTH_STARS,
        SOULFLOW,
        LOCATION,
        DATE,
        TIME,
        LOBBY,
        VISITING,
        PROFILE,
        POWDER,
        ARROWS,
        OBJECTIVE,
        SLAYER,
        FOOTER,
        EVENT,
        UNKNOWN,
        SKIP
    }

    record Hit(Kind kind, CustomScoreboardPolicy.EventKind event, String capture, int extraLines) {
        Hit {
            event = event;
            capture = capture == null ? "" : capture;
            extraLines = Math.max(0, extraLines);
        }

        static Hit of(Kind kind) {
            return new Hit(kind, null, "", 0);
        }

        static Hit money(Kind kind, String amount) {
            return new Hit(kind, null, amount, 0);
        }
    }

    static Hit classify(String raw) {
        if (raw == null) {
            return Hit.of(Kind.SKIP);
        }
        String trimmed = raw.replace('\u00a0', ' ');
        String plain = LegacyMcText.strip(trimmed);
        String cleaned = SkyBlockStatBarParser.stripHudIconTokens(
                SkyBlockStatBarParser.stripFormatting(plain));
        if (plain.isBlank() || plain.length() <= 1) {
            return Hit.of(Kind.SKIP);
        }
        if (looksFooter(trimmed, cleaned)) {
            return Hit.of(Kind.SKIP);
        }
        Matcher purse = PURSE.matcher(trimmed);
        if (purse.find()) {
            return Hit.money(Kind.PURSE, purse.group("amount"));
        }
        Matcher motes = MOTES.matcher(trimmed);
        if (motes.find()) {
            return Hit.money(Kind.MOTES, motes.group("amount"));
        }
        Matcher copper = COPPER.matcher(trimmed);
        if (copper.find()) {
            return Hit.money(Kind.COPPER, copper.group("amount"));
        }
        Matcher sowdust = SOWDUST.matcher(trimmed);
        if (sowdust.find()) {
            return Hit.money(Kind.SOWDUST, sowdust.group("amount"));
        }
        Matcher gems = GEMS.matcher(trimmed);
        if (gems.find()) {
            return Hit.money(Kind.GEMS, gems.group("amount"));
        }
        Matcher bits = BITS.matcher(trimmed);
        if (bits.find()) {
            return Hit.money(Kind.BITS, bits.group("amount"));
        }
        Matcher north = NORTH_STARS.matcher(trimmed);
        if (north.find()) {
            return Hit.money(Kind.NORTH_STARS, north.group("amount"));
        }
        Matcher soulflow = SOULFLOW.matcher(trimmed);
        if (soulflow.find()) {
            return Hit.money(Kind.SOULFLOW, soulflow.group("amount"));
        }
        Matcher bank = BANK.matcher(trimmed);
        if (bank.find()) {
            return Hit.money(Kind.BANK, bank.group("amount"));
        }
        Matcher powder = POWDER.matcher(trimmed);
        if (powder.find()) {
            return new Hit(Kind.POWDER, null, powder.group("type") + "|" + powder.group("amount"), 0);
        }
        Matcher powderPlain = POWDER_PLAIN.matcher(plain);
        if (powderPlain.find()) {
            return new Hit(Kind.POWDER, null, powderPlain.group(1) + "|" + powderPlain.group("amount"), 0);
        }
        if (HEAT.matcher(trimmed).find() || HEAT.matcher(plain).find()) {
            return Hit.of(Kind.HEAT);
        }
        if (COLD.matcher(trimmed).find() || COLD.matcher(plain).find()) {
            return Hit.of(Kind.COLD);
        }
        if (PROFILE.matcher(trimmed).find() || looksProfile(plain)) {
            return Hit.of(Kind.PROFILE);
        }
        if (DATE.matcher(plain).find()) {
            return Hit.of(Kind.DATE);
        }
        if (TIME.matcher(trimmed).find() || TIME.matcher(plain).find()) {
            return Hit.of(Kind.TIME);
        }
        if (LOBBY.matcher(trimmed).find() || LOBBY_CODE.matcher(plain).find()) {
            return new Hit(Kind.LOBBY, null, lobbyCode(plain), 0);
        }
        if (LOCATION.matcher(trimmed).find()
                || PLOT.matcher(trimmed).find()
                || looksLocation(cleaned)) {
            return Hit.of(Kind.LOCATION);
        }
        if (VISITING.matcher(trimmed).find() || plain.contains("Visiting") || plain.contains("✌")) {
            return Hit.of(Kind.VISITING);
        }
        if (OBJECTIVE.matcher(trimmed).find()) {
            return new Hit(Kind.OBJECTIVE, null, "", 3);
        }
        if (SLAYER.matcher(trimmed).find() || SLAYER.matcher(plain).find()) {
            return new Hit(Kind.SLAYER, null, "", 2);
        }
        if (ARROWS.matcher(plain).find()) {
            return Hit.of(Kind.ARROWS);
        }
        CustomScoreboardPolicy.EventKind event = eventKind(trimmed, plain);
        if (event != null) {
            return new Hit(Kind.EVENT, event, "", extraForEvent(event, trimmed, plain));
        }
        return Hit.of(Kind.UNKNOWN);
    }

    static OptionalLong parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalLong.empty();
        }
        String token = raw.toLowerCase(Locale.ROOT).replace(",", "").trim();
        double mul = 1.0D;
        if (token.endsWith("k")) {
            mul = 1_000.0D;
            token = token.substring(0, token.length() - 1);
        } else if (token.endsWith("m")) {
            mul = 1_000_000.0D;
            token = token.substring(0, token.length() - 1);
        } else if (token.endsWith("b")) {
            mul = 1_000_000_000.0D;
            token = token.substring(0, token.length() - 1);
        }
        try {
            if (token.contains(".")) {
                return OptionalLong.of((long) (Double.parseDouble(token) * mul));
            }
            return OptionalLong.of((long) (Long.parseLong(token) * mul));
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    static String powderColor(String type) {
        String needle = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (needle.startsWith("gem")) {
            return "§d";
        }
        if (needle.startsWith("gla")) {
            return "§b";
        }
        return "§2";
    }

    static String timeSymbol(String raw) {
        if (raw == null) {
            return "";
        }
        Matcher matcher = TIME.matcher(raw);
        if (matcher.find() && matcher.group("symbol") != null) {
            return matcher.group("symbol");
        }
        String plain = LegacyMcText.strip(raw);
        if (plain.contains("☽")) {
            return "§b☽";
        }
        if (plain.contains("☀")) {
            return "§e☀";
        }
        return "";
    }

    private static boolean looksFooter(String raw, String cleaned) {
        if (FOOTER.matcher(raw).find() || FOOTER.matcher(cleaned).find()) {
            return true;
        }
        String lower = cleaned.toLowerCase(Locale.ROOT).replace(" ", "");
        return lower.contains("hypixel.net") || lower.contains("hypixelnet");
    }

    private static boolean looksLocation(String cleaned) {
        if (cleaned == null || cleaned.isBlank()) {
            return false;
        }
        String text = cleaned.replaceFirst("^[\\p{So}\\p{Cn}?•·]+\\s*", "").trim();
        return LOCATION_NAME.matcher(text).find();
    }

    private static String lobbyCode(String plain) {
        Matcher matcher = LOBBY_CODE.matcher(plain);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return plain.trim();
    }

    private static boolean looksProfile(String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        return lower.contains("ironman") || lower.contains("stranded") || lower.contains("bingo");
    }

    private static int extraForEvent(
            CustomScoreboardPolicy.EventKind event, String raw, String plain) {
        if (event == CustomScoreboardPolicy.EventKind.JACOB_CONTEST && JACOB.matcher(raw).find()) {
            return 3;
        }
        if (event == CustomScoreboardPolicy.EventKind.GALATEA
                && (AGATHA.matcher(raw).find() || MIRIA.matcher(raw).find())) {
            return 2;
        }
        if (event == CustomScoreboardPolicy.EventKind.DARK_AUCTION && DARK_AUCTION_ITEM.matcher(plain).find()) {
            return 1;
        }
        if (event == CustomScoreboardPolicy.EventKind.TRAPPER && TRACKER_MOB.matcher(raw).find()) {
            return 1;
        }
        return 0;
    }

    static CustomScoreboardPolicy.EventKind eventKind(String raw, String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        if (contains(lower, "year") && contains(lower, "vote")
                || contains(lower, "waiting for") && contains(lower, "vote")
                || lower.contains("votes") && raw.contains("§6Year")) {
            return CustomScoreboardPolicy.EventKind.VOTING;
        }
        if (lower.contains("instance shutdown") || lower.contains("server clos")) {
            return CustomScoreboardPolicy.EventKind.SERVER_CLOSE;
        }
        if (lower.startsWith("cleared:")
                || lower.startsWith("keys:")
                || lower.contains("no alive dragons")
                || lower.contains("§3§lsolo")
                || lower.matches(".*\\[[msthb]\\] .*")
                || lower.contains("healthy") && lower.contains("dragon")
                || lower.contains("starting in:")
                || lower.contains("auto-closing")
                || lower.contains("time elapsed:")) {
            return CustomScoreboardPolicy.EventKind.DUNGEONS;
        }
        if (lower.startsWith("wave:") || lower.startsWith("tokens:") || lower.contains("submerges")) {
            return CustomScoreboardPolicy.EventKind.KUUDRA;
        }
        if (lower.startsWith("challenge:") || lower.startsWith("difficulty:")
                && lower.contains("dojo") || lower.startsWith("points:") && lower.contains("dojo")
                || lower.startsWith("time:") && lower.contains("dojo")) {
            return CustomScoreboardPolicy.EventKind.DOJO;
        }
        if (lower.contains("dark auction") || DARK_AUCTION_ITEM.matcher(plain).find()) {
            return CustomScoreboardPolicy.EventKind.DARK_AUCTION;
        }
        if (JACOB.matcher(raw).find() || lower.contains("jacob")) {
            return lower.contains("medal")
                    ? CustomScoreboardPolicy.EventKind.JACOB_MEDALS
                    : CustomScoreboardPolicy.EventKind.JACOB_CONTEST;
        }
        if (lower.contains("gold medals") || lower.contains("silver medals") || lower.contains("bronze medals")) {
            return CustomScoreboardPolicy.EventKind.JACOB_MEDALS;
        }
        if (lower.contains("pelt") || TRACKER_MOB.matcher(raw).find()) {
            return CustomScoreboardPolicy.EventKind.TRAPPER;
        }
        if (lower.contains("cleanup") || lower.contains("pasting") || lower.contains("plot -")
                || lower.contains("locked")) {
            return CustomScoreboardPolicy.EventKind.GARDEN;
        }
        if (lower.contains("flight duration")) {
            return CustomScoreboardPolicy.EventKind.FLIGHT_DURATION;
        }
        if (lower.contains("event start:") || lower.contains("next wave:") || lower.contains("magma cubes left")
                || lower.contains("your cube damage") || lower.contains("your total damage")) {
            return CustomScoreboardPolicy.EventKind.WINTER;
        }
        if (lower.contains("new year event")) {
            return CustomScoreboardPolicy.EventKind.NEW_YEAR;
        }
        if (lower.contains("spooky festival")) {
            return CustomScoreboardPolicy.EventKind.SPOOKY;
        }
        if (lower.contains("broodmother")) {
            return CustomScoreboardPolicy.EventKind.BROODMOTHER;
        }
        if (lower.startsWith("event:") || lower.startsWith("zone:")
                || lower.contains("wind compass")
                || lower.contains("tickets:")
                || lower.contains("tasty mithril")
                || lower.contains("kill goblins")
                || lower.contains("nearby players:")
                || lower.contains("fossil dust")
                || lower.contains("event bonus:")) {
            return CustomScoreboardPolicy.EventKind.MINING;
        }
        if (AGATHA.matcher(raw).find() || MIRIA.matcher(raw).find()
                || lower.contains("whispers:") || lower.contains("hotf:")) {
            return CustomScoreboardPolicy.EventKind.GALATEA;
        }
        if (lower.contains("captured mobs")) {
            return CustomScoreboardPolicy.EventKind.SAFARI;
        }
        if (lower.contains("your damage") || lower.contains("dragon hp") || lower.contains("protector hp")) {
            return CustomScoreboardPolicy.EventKind.DAMAGE;
        }
        if (lower.contains("magma chamber") || lower.contains("kill the magmas")
                || lower.contains("damage soaked") || lower.contains("boss health")
                || lower.contains("the boss is") && lower.contains("forming")) {
            return CustomScoreboardPolicy.EventKind.MAGMA_BOSS;
        }
        if (lower.contains("carnival")) {
            return CustomScoreboardPolicy.EventKind.CARNIVAL;
        }
        if (lower.contains("rift dimension") || lower.contains("hot dog contest")
                || lower.contains("timecharm") || lower.contains("enigma")
                || lower.contains("time sliced") || lower.contains("protestors handled")
                || lower.contains("hay eaten") || lower.contains("clues:")) {
            return CustomScoreboardPolicy.EventKind.RIFT;
        }
        if (lower.contains("essence:")) {
            return CustomScoreboardPolicy.EventKind.ESSENCE;
        }
        if (lower.startsWith("queued:") || lower.startsWith("tier:")
                || lower.startsWith("position:") || lower.contains("waiting on party leader")) {
            return CustomScoreboardPolicy.EventKind.QUEUE;
        }
        if (lower.contains("anniversary") || lower.contains("century raffle")) {
            return CustomScoreboardPolicy.EventKind.ANNIVERSARY;
        }
        if (lower.contains("starts in:") || lower.contains("starting soon")) {
            return CustomScoreboardPolicy.EventKind.STARTING_SOON;
        }
        if (lower.contains("redstone:")) {
            return CustomScoreboardPolicy.EventKind.REDSTONE;
        }
        if (lower.contains("active event") || lower.contains("ends in:")) {
            return CustomScoreboardPolicy.EventKind.ACTIVE_TABLIST;
        }
        return null;
    }

    private static boolean contains(String lower, String token) {
        return lower.contains(token);
    }
}
