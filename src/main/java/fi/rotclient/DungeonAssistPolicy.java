package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
 * Remaining Catacombs helpers:
 * quiz / three weirdos, F7 titles and tick timers, Ragnarock, blessings,
 * salvage overlay, party finder and chest-profit lore.
 */
public final class DungeonAssistPolicy {
    public enum WeirdoKind {
        NONE,
        CORRECT,
        WRONG
    }

    public enum F7Title {
        NONE,
        CRYSTAL,
        ENRAGED,
        TERMINAL,
        GATE
    }

    public enum F7Timer {
        NONE,
        MAXOR_START,
        STORM_START,
        STORM_PAD,
        STORM_LIGHTNING,
        STORM_PY,
        GOLDOR,
        CORE,
        NECRON
    }

    public record ChestCoinLine(String name, long coins) {
    }

    public record ChestLootLine(String name, long value) {
    }

    public record ChestProfitSummary(
            String chestName,
            long loot,
            long cost,
            long profit,
            List<ChestLootLine> items) {
        public ChestProfitSummary {
            items = items == null ? List.of() : List.copyOf(items);
            chestName = chestName == null ? "" : chestName;
        }

        public List<String> hudLines(boolean compact) {
            List<String> lines = new ArrayList<>();
            String profitText = (profit >= 0L ? "+" : "") + formatCoins(profit);
            lines.add((chestName.isBlank() ? "Chest" : chestName)
                    + " " + profitText + " §8[bundled prices]");
            if (!compact) {
                for (ChestLootLine item : items) {
                    lines.add("  " + item.name() + " " + formatCoins(item.value()));
                }
                if (cost > 0L) {
                    lines.add("  Cost -" + formatCoins(cost));
                }
            }
            return lines;
        }
    }

    public static final int SIMON_X = 110;
    public static final int SIMON_Y = 121;
    public static final int SIMON_Z = 91;
    public static final long STORM_PAD_MILLIS = 20_000L;
    public static final long STORM_LIGHTNING_MILLIS = 5_050L;
    public static final long STORM_PY_MILLIS = 5_050L;
    public static final long GOLDOR_MILLIS = 60_000L;
    public static final long CORE_MILLIS = 5_000L;
    public static final long NECRON_MILLIS = 8_000L;
    public static final long SKYBLOCK_EPOCH_SECONDS = 1_560_275_700L;
    public static final long SKYBLOCK_YEAR_SECONDS = 446_400L;

    private static final Pattern CRYSTAL = Pattern.compile(
            "^(\\d)/(\\d) Energy Crystals are now active!$");
    private static final Pattern ENRAGED = Pattern.compile("^⚠ (\\w+) is enraged! ⚠$");
    private static final Pattern TERMINAL = Pattern.compile(
            "(.+) (?:activated|completed) a (terminal|device|lever)! \\((\\d)/(\\d)\\)");
    private static final Pattern BLESSING = Pattern.compile(
            "(?i)blessing of (power|time|life|wisdom|stone)(?:\\s+([IVX]+|\\d+))?");
    private static final Pattern RAGNAROCK_CANCEL = Pattern.compile(
            "Ragnarock was cancelled due to (?:being hit|taking damage)!");
    private static final Pattern RAGNAROCK_STRENGTH = Pattern.compile(
            "(?i)ragnarock(?: axe)? granted you \\+(\\d+)");
    private static final Pattern CATA_LEVEL = Pattern.compile(
            "(?i)(?:catacombs|cata)\\s*(?:level)?:?\\s*(\\d+)");
    private static final Pattern NPC_NAME = Pattern.compile("(?i)^\\[(?:npc|statue)]\\s+([^:]+):");
    private static final Pattern COINS = Pattern.compile("(?i)([\\d,]+)\\s+coins");
    private static final Pattern CHEST_COST = Pattern.compile("(?i)(?:cost|price)\\s*:?\\s*([\\d,]+)\\s+coins");
    private static final Pattern ESSENCE = Pattern.compile("(?i)^(wither|undead|dragon|spider|ice|gold|diamond) essence x(\\d+)$");
    private static final Pattern BOOK = Pattern.compile("(?i)^enchanted book \\(?([\\w ']+) ([ivxlcdm]+|\\d+)\\)?$");
    private static final Pattern SHARD = Pattern.compile("(?i)^([a-z ]+) shard(?: x\\d+)?$");
    private static final Set<String> ULTIMATE_BOOKS = Set.of(
            "soul eater", "combo", "legion", "one for all", "rend", "bank",
            "swarm", "last stand", "wisdom", "no pain no gain");
    private static final Map<String, String> ITEM_IDS = Map.ofEntries(
            Map.entry("shiny wither chestplate", "WITHER_CHESTPLATE"),
            Map.entry("shiny wither leggings", "WITHER_LEGGINGS"),
            Map.entry("shiny necron's handle", "NECRON_HANDLE"),
            Map.entry("necron's handle", "NECRON_HANDLE"),
            Map.entry("shiny wither helmet", "WITHER_HELMET"),
            Map.entry("shiny wither boots", "WITHER_BOOTS"),
            Map.entry("wither shield", "WITHER_SHIELD_SCROLL"),
            Map.entry("implosion", "IMPLOSION_SCROLL"),
            Map.entry("shadow warp", "SHADOW_WARP_SCROLL"),
            Map.entry("necron dye", "DYE_NECRON"),
            Map.entry("livid dye", "DYE_LIVID"),
            Map.entry("giant's sword", "GIANTS_SWORD"),
            Map.entry("necromancers_brooch", "NECROMANCER_BROOCH"),
            Map.entry("spirit stone", "SPIRIT_DECOY"),
            Map.entry("warped stone", "AOTE_STONE"));
    private static Map<String, Long> shippedPrices;
    private static final Pattern BLAZE_HP = Pattern.compile("(\\d[\\d,]*)/(\\d[\\d,]*)");
    private static final Pattern ROOM_FAIL = Pattern.compile("(?i)(puzzle fail|wrong! you didn't click)");
    private static final Pattern PARTY_FINDER_TITLE = Pattern.compile("(?i)party finder|dungeon finder");
    private static final Pattern SALVAGE_TITLE = Pattern.compile("(?i)salvage|autonpc|trades");
    private static final Pattern CROESUS_TITLE = Pattern.compile("(?i)croesus|the catacombs -|dungeon chest");
    private static final Pattern CHEST_TITLE = Pattern.compile("(?i)wood chest|gold chest|diamond chest|emerald chest|obsidian chest|bedrock chest");

    private static final Map<String, List<String>> QUIZ = quizAnswers();

    private static final String[] WEIRDO_CORRECT = {
            "The reward is not in my chest!",
            "At least one of them is lying, and the reward is not in",
            "My chest doesn't have the reward. We are all telling the truth",
            "My chest has the reward and I'm telling the truth!",
            "The reward isn't in any of our chests",
            "Both of them are telling the truth. Also,"
    };

    private static final String[] WEIRDO_WRONG = {
            "One of us is telling the truth!",
            "They are both telling the truth. The reward isn't in",
            "We are all telling the truth!",
            "is telling the truth and the reward is in his chest",
            "My chest doesn't have the reward. At least one of the others is telling the truth!",
            "One of the others is lying.",
            "They are both telling the truth, the reward is in",
            "They are both lying, the reward is in my chest!",
            "The reward is in my chest.",
            "The reward is not in my chest. They are both lying.",
            "is telling the truth.",
            "My chest has the reward."
    };

    private DungeonAssistPolicy() {
    }

    public static Optional<String> quizAnswer(String question) {
        String text = DungeonPolicy.normalize(question)
                .replaceFirst("(?i)^\\[(?:npc|statue)]\\s+oruo the magician:\\s*", "")
                .replaceFirst("(?i)^question \\d+:\\s*", "")
                .trim();
        if (text.isBlank()) {
            return Optional.empty();
        }
        if (text.equalsIgnoreCase("What SkyBlock year is it?")) {
            return Optional.of("Year " + skyBlockYear(System.currentTimeMillis()));
        }
        if (text.trim().equalsIgnoreCase("glass?")) {
            text = "What is the name of the vendor in the Hub who sells stained glass?";
        }
        List<String> answers = QUIZ.get(text);
        if (answers != null && !answers.isEmpty()) {
            return Optional.of(answers.getFirst());
        }
        return Optional.empty();
    }

    public static int skyBlockYear(long epochMillis) {
        long seconds = Math.max(0L, epochMillis / 1000L - SKYBLOCK_EPOCH_SECONDS);
        return (int) (seconds / SKYBLOCK_YEAR_SECONDS) + 1;
    }

    public static WeirdoKind weirdoKind(String message) {
        String text = DungeonPolicy.normalize(message);
        for (String correct : WEIRDO_CORRECT) {
            if (text.contains(correct) || text.startsWith(correct)) {
                return WeirdoKind.CORRECT;
            }
        }
        for (String wrong : WEIRDO_WRONG) {
            if (text.contains(wrong)) {
                return WeirdoKind.WRONG;
            }
        }
        return WeirdoKind.NONE;
    }

    public static Optional<String> weirdoNpcName(String message) {
        if (weirdoKind(message) == WeirdoKind.NONE) {
            return Optional.empty();
        }
        Matcher matcher = NPC_NAME.matcher(DungeonPolicy.normalize(message));
        return matcher.find() ? Optional.of(matcher.group(1).trim()) : Optional.empty();
    }

    public static Optional<String> blessingLine(String chat) {
        Matcher matcher = BLESSING.matcher(DungeonPolicy.normalize(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        String kind = matcher.group(1);
        String rank = matcher.group(2) == null ? "" : matcher.group(2);
        String pretty = kind.substring(0, 1).toUpperCase(Locale.ROOT) + kind.substring(1);
        return Optional.of(rank.isBlank() ? pretty : pretty + " " + rank.toUpperCase(Locale.ROOT));
    }

    public static boolean isRagnarockCancelled(String chat) {
        return RAGNAROCK_CANCEL.matcher(DungeonPolicy.normalize(chat)).matches();
    }

    public static Optional<Integer> ragnarockStrength(String chat) {
        Matcher matcher = RAGNAROCK_STRENGTH.matcher(DungeonPolicy.normalize(chat));
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group(1))) : Optional.empty();
    }

    public static F7Title f7Title(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (CRYSTAL.matcher(text).matches()) {
            return F7Title.CRYSTAL;
        }
        if (ENRAGED.matcher(text).matches()) {
            return F7Title.ENRAGED;
        }
        Matcher terminal = TERMINAL.matcher(text);
        if (terminal.matches()) {
            return "lever".equalsIgnoreCase(terminal.group(2)) ? F7Title.GATE : F7Title.TERMINAL;
        }
        return F7Title.NONE;
    }

    public static Optional<String> f7TitleText(String chat) {
        String text = DungeonPolicy.normalize(chat);
        Matcher crystal = CRYSTAL.matcher(text);
        if (crystal.matches()) {
            return Optional.of("Crystals " + crystal.group(1) + "/" + crystal.group(2));
        }
        Matcher enraged = ENRAGED.matcher(text);
        if (enraged.matches()) {
            return Optional.of(enraged.group(1) + " Enraged");
        }
        Matcher terminal = TERMINAL.matcher(text);
        if (terminal.matches()) {
            return Optional.of(terminal.group(2).substring(0, 1).toUpperCase(Locale.ROOT)
                    + terminal.group(2).substring(1)
                    + " " + terminal.group(3) + "/" + terminal.group(4));
        }
        return Optional.empty();
    }

    public static F7Timer f7TimerFromChat(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (text.equals("[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!")) {
            return F7Timer.MAXOR_START;
        }
        if (text.equals("[BOSS] Maxor: I'M TOO YOUNG TO DIE AGAIN!")) {
            return F7Timer.STORM_START;
        }
        if (text.equals("[BOSS] Storm: Pathetic Maxor, just like expected.")) {
            return F7Timer.STORM_PAD;
        }
        if (text.equals("[BOSS] Storm: I should have known that I stood no chance.")) {
            return F7Timer.STORM_LIGHTNING;
        }
        if (text.startsWith("[BOSS] Storm: ENERGY HEED MY CALL")
                || text.startsWith("[BOSS] Storm: THUNDER LET ME BE YOUR CATALYST")) {
            return F7Timer.STORM_PY;
        }
        if (text.equals("[BOSS] Goldor: Who dares trespass into my domain?")) {
            return F7Timer.GOLDOR;
        }
        if (text.equals("The Core entrance is opening!")) {
            return F7Timer.CORE;
        }
        if (text.equals("[BOSS] Necron: I'm afraid, your journey ends now.")) {
            return F7Timer.NECRON;
        }
        return F7Timer.NONE;
    }

    public static long f7TimerMillis(F7Timer timer) {
        return switch (timer) {
            case MAXOR_START -> DungeonF7Policy.MAXOR_START_MILLIS;
            case STORM_START -> DungeonF7Policy.STORM_START_MILLIS;
            case STORM_PAD -> STORM_PAD_MILLIS;
            case STORM_LIGHTNING -> STORM_LIGHTNING_MILLIS;
            case STORM_PY -> STORM_PY_MILLIS;
            case GOLDOR -> GOLDOR_MILLIS;
            case CORE -> CORE_MILLIS;
            case NECRON -> NECRON_MILLIS;
            case NONE -> 0L;
        };
    }

    public static String f7TimerLabel(F7Timer timer) {
        return switch (timer) {
            case MAXOR_START -> "Maxor";
            case STORM_START -> "Storm";
            case STORM_PAD -> "Pad";
            case STORM_LIGHTNING -> "Lightning";
            case STORM_PY -> "P3";
            case GOLDOR -> "Goldor";
            case CORE -> "Core";
            case NECRON -> "Necron";
            case NONE -> "";
        };
    }

    public static boolean isRoomAlert(String chat) {
        return ROOM_FAIL.matcher(DungeonPolicy.normalize(chat)).find();
    }

    public static boolean isPartyFinderMenu(String title) {
        return PARTY_FINDER_TITLE.matcher(DungeonPolicy.normalize(title)).find();
    }

    public static boolean isSalvageMenu(String title) {
        return SALVAGE_TITLE.matcher(DungeonPolicy.normalize(title)).find();
    }

    public static boolean isDungeonChestMenu(String title) {
        String text = DungeonPolicy.normalize(title);
        return CROESUS_TITLE.matcher(text).find() || CHEST_TITLE.matcher(text).find();
    }

    public static Optional<Integer> partyFinderCata(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (String line : lore) {
            Matcher matcher = CATA_LEVEL.matcher(DungeonPolicy.normalize(line));
            if (matcher.find()) {
                return Optional.of(Integer.parseInt(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

    public static boolean partyFinderMatches(List<String> lore, int minimumCata) {
        Optional<Integer> cata = partyFinderCata(lore);
        return cata.isPresent() && cata.get() >= minimumCata;
    }

    public static boolean salvageable(String name, int baseStatBoost, boolean starred) {
        return !starred
                && baseStatBoost > 0
                && name != null
                && !name.contains("✪");
    }

    public static int salvageColor(int baseStatBoost, int perfect, int under) {
        if (baseStatBoost <= 0) {
            return 0;
        }
        return baseStatBoost >= 50 ? perfect : under;
    }

    public static Optional<ChestCoinLine> chestProfit(String name, List<String> lore) {
        return chestProfit(name, lore, true);
    }

    public static Optional<ChestCoinLine> chestProfit(String name, List<String> lore, boolean includeEssence) {
        long value = itemValue(name, lore, includeEssence);
        if (value <= 0L) {
            return Optional.empty();
        }
        return Optional.of(new ChestCoinLine(name == null ? "" : name, value));
    }

    public static long itemValue(String name, List<String> lore, boolean includeEssence) {
        long loreCoins = loreCoinValue(lore);
        long catalog = catalogValue(name, includeEssence);
        if (loreCoins <= 0L && catalog <= 0L) {
            return 0L;
        }
        return Math.max(loreCoins, catalog);
    }

    public static long chestCost(List<String> lore) {
        if (lore == null) {
            return 0L;
        }
        long cost = 0L;
        for (String line : lore) {
            String text = DungeonPolicy.normalize(line);
            Matcher matcher = CHEST_COST.matcher(text);
            if (matcher.find()) {
                cost = Math.max(cost, parseCoins(matcher.group(1)));
            }
        }
        return cost;
    }

    public static ChestProfitSummary summarizeChest(
            String chestName,
            List<ChestCoinLine> loot,
            long cost,
            boolean includeEssence) {
        List<ChestLootLine> items = new ArrayList<>();
        long total = 0L;
        for (ChestCoinLine line : loot == null ? List.<ChestCoinLine>of() : loot) {
            if (line == null || line.coins() <= 0L) {
                continue;
            }
            String label = line.name() == null ? "" : line.name();
            if (!includeEssence && isEssenceName(label)) {
                continue;
            }
            items.add(new ChestLootLine(label, line.coins()));
            total += line.coins();
        }
        long profit = total - Math.max(0L, cost);
        return new ChestProfitSummary(chestName, total, Math.max(0L, cost), profit, items);
    }

    public static int profitTint(long profit, int profitColor, int lossColor) {
        if (profit > 0L) {
            return profitColor;
        }
        if (profit < 0L) {
            return lossColor;
        }
        return 0;
    }

    public static String formatCoins(long coins) {
        long abs = Math.abs(coins);
        if (abs >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%.1fb", coins / 1_000_000_000.0D);
        }
        if (abs >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fm", coins / 1_000_000.0D);
        }
        if (abs >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", coins / 1_000.0D);
        }
        return Long.toString(coins);
    }

    public static List<String> mapExtraInfo(
            DungeonPolicy.Sidebar sidebar,
            boolean secrets,
            boolean crypts,
            boolean score,
            boolean deaths) {
        if (sidebar == null) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        if (secrets && !sidebar.secrets().isEmpty()) {
            lines.add("Secrets: " + sidebar.secrets());
        }
        if (crypts && sidebar.crypts() >= 0) {
            lines.add("Crypts: " + sidebar.crypts());
        }
        if (score && sidebar.score() >= 0) {
            lines.add("Score: " + sidebar.score());
        }
        if (deaths && sidebar.deaths() >= 0) {
            lines.add("Deaths: " + sidebar.deaths());
        }
        return List.copyOf(lines);
    }

    public static String scoreTitleText(int score, int threshold) {
        return Math.max(1, threshold) + " Score!";
    }

    public static Optional<String> leapTarget(String chat) {
        Matcher matcher = Pattern.compile("(?i)leaped to ([A-Za-z0-9_]{1,16})")
                .matcher(DungeonPolicy.normalize(chat));
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public static String leapAnnounce(String template, String name) {
        String text = template == null || template.isBlank() ? "ILY {name}" : template;
        String player = name == null ? "" : name.trim();
        return text.replace("{name}", player).trim();
    }

    public static int clampOpacity(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public static int clampScoreThreshold(int value) {
        return Math.max(100, Math.min(305, value));
    }

    public static Optional<Integer> blazeHealth(String hologram) {
        Matcher matcher = BLAZE_HP.matcher(DungeonPolicy.normalize(hologram).replace(",", ""));
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static List<Integer> sortBlazeHealth(List<Integer> health, boolean lowestFirst) {
        List<Integer> copy = new ArrayList<>(health == null ? List.of() : health);
        copy.sort(lowestFirst ? Integer::compareTo : (a, b) -> Integer.compare(b, a));
        return List.copyOf(copy);
    }

    public static boolean isFloor7(String floor) {
        String text = floor == null ? "" : floor.toUpperCase(Locale.ROOT);
        return "F7".equals(text) || "M7".equals(text);
    }

    private static long loreCoinValue(List<String> lore) {
        if (lore == null) {
            return 0L;
        }
        long coins = 0L;
        for (String line : lore) {
            String text = DungeonPolicy.normalize(line);
            if (CHEST_COST.matcher(text).find()) {
                continue;
            }
            Matcher matcher = COINS.matcher(text);
            if (matcher.find()) {
                coins = Math.max(coins, parseCoins(matcher.group(1)));
            }
        }
        return coins;
    }

    private static long catalogValue(String name, boolean includeEssence) {
        String clean = DungeonPolicy.normalize(name);
        if (clean.isBlank()) {
            return 0L;
        }
        Matcher essence = ESSENCE.matcher(clean);
        if (essence.matches()) {
            if (!includeEssence) {
                return 0L;
            }
            long unit = shippedPrice("ESSENCE_" + essence.group(1).toUpperCase(Locale.ROOT));
            return unit * Long.parseLong(essence.group(2));
        }
        Matcher book = BOOK.matcher(clean);
        if (book.matches()) {
            String enchant = book.group(1).trim();
            String prefix = ULTIMATE_BOOKS.contains(enchant.toLowerCase(Locale.ROOT))
                    ? "ULTIMATE_" : "";
            String id = "ENCHANTED_BOOK-" + prefix
                    + enchant.toUpperCase(Locale.ROOT).replace(' ', '_')
                    + "-" + romanOrInt(book.group(2));
            return shippedPrice(id);
        }
        Matcher shard = SHARD.matcher(clean);
        if (shard.matches()) {
            return shippedPrice("SHARD_" + shard.group(1).trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        }
        String mapped = ITEM_IDS.get(clean.toLowerCase(Locale.ROOT));
        if (mapped != null) {
            return shippedPrice(mapped);
        }
        String id = clean.toUpperCase(Locale.ROOT)
                .replace("'", "")
                .replace(" -", "")
                .replace(' ', '_');
        return shippedPrice(id);
    }

    private static boolean isEssenceName(String name) {
        return ESSENCE.matcher(DungeonPolicy.normalize(name)).matches();
    }

    private static long shippedPrice(String id) {
        if (id == null || id.isBlank()) {
            return 0L;
        }
        Long value = shippedPrices().get(id.toUpperCase(Locale.ROOT));
        return value == null ? 0L : value;
    }

    private static Map<String, Long> shippedPrices() {
        if (shippedPrices != null) {
            return shippedPrices;
        }
        Map<String, Long> loaded = new LinkedHashMap<>();
        try (InputStream in = DungeonAssistPolicy.class.getResourceAsStream(
                "/data/rotclient/croesus-prices.json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                        loaded.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue().getAsLong());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        shippedPrices = Map.copyOf(loaded);
        return shippedPrices;
    }

    private static long parseCoins(String raw) {
        try {
            return Long.parseLong(raw.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static int romanOrInt(String raw) {
        String text = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
        }
        int total = 0;
        int prev = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            int value = switch (text.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                case 'L' -> 50;
                case 'C' -> 100;
                case 'D' -> 500;
                case 'M' -> 1000;
                default -> 0;
            };
            total += value < prev ? -value : value;
            prev = value;
        }
        return total;
    }

    private static Map<String, List<String>> quizAnswers() {
        Map<String, List<String>> answers = new LinkedHashMap<>();
        answers.put("What is the status of The Watcher?", List.of("Stalker"));
        answers.put("What is the status of Bonzo?", List.of("New Necromancer"));
        answers.put("What is the status of Scarf?", List.of("Apprentice Necromancer"));
        answers.put("What is the status of The Professor?", List.of("Professor"));
        answers.put("What is the status of Thorn?", List.of("Shaman Necromancer"));
        answers.put("What is the status of Livid?", List.of("Master Necromancer"));
        answers.put("What is the status of Sadan?", List.of("Necromancer Lord"));
        answers.put("What is the status of Maxor, Storm, Goldor, and Necron?",
                List.of("The Wither Lords"));
        answers.put("How many total Fairy Souls are there?", List.of("247 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Spider's Den?", List.of("19 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Spiders Den?", List.of("19 Fairy Souls"));
        answers.put("How many Fairy Souls are there in The End?", List.of("12 Fairy Souls"));
        answers.put("How many Fairy Souls are there in The Farming Islands?", List.of("20 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Crimson Isle?", List.of("29 Fairy Souls"));
        answers.put("How many Fairy Souls are there in The Park?", List.of("12 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Jerry's Workshop?", List.of("5 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Hub?", List.of("80 Fairy Souls"));
        answers.put("How many Fairy Souls are there in The Hub?", List.of("80 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Deep Caverns?", List.of("21 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Gold Mine?", List.of("12 Fairy Souls"));
        answers.put("How many Fairy Souls are there in Dungeon Hub?", List.of("7 Fairy Souls"));
        answers.put("Which brother is on the Spider's Den?", List.of("Rick"));
        answers.put("Which brother is on the Spiders Den?", List.of("Rick"));
        answers.put("What is the name of Rick's brother?", List.of("Pat"));
        answers.put("What is the name of the vendor in the Hub who sells stained glass?",
                List.of("Wool Weaver"));
        answers.put("What is the name of the person that upgrades pets?", List.of("Kat"));
        answers.put("What is the name of the lady of the Nether?", List.of("Elle"));
        answers.put("Which villager in the Village gives you a Rogue Sword?", List.of("Jamie"));
        answers.put("How many unique minions are there?", List.of("68 Minions"));
        answers.put("Which of these enemies does not spawn in the Spider's Den?",
                List.of("Wither Skeleton"));
        answers.put("Which of these monsters only spawns at night?", List.of("Zombie Villager"));
        answers.put("Which of these is not a dragon in The End?", List.of("Elder Dragon"));
        return answers;
    }
}
