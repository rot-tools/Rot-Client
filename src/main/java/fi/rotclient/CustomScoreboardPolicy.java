package fi.rotclient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rot-native SkyBlock sidebar rebuild. Reads Hypixel sidebar/tab facts and
 * paints a reorderable board. Minecraft-free so tests can drive it.
 */
public final class CustomScoreboardPolicy {
    public static final String MODULE_ID = "qol.custom_scoreboard";
    public static final String POSE_ID = "custom_scoreboard";
    static final long DELTA_MS = 5_000L;

    private static final Pattern NUMBER = Pattern.compile("([+-]?[\\d,]+(?:\\.\\d+)?)");
    private static final Pattern POWDER = Pattern.compile(
            "(?i)(mithril|gemstone|glacite)(?:\\s*powder)?\\s*:?\\s*([\\d,.kmb]+)");
    private static final Pattern TAB_LABEL = Pattern.compile(
            "(?i)^\\s*(?<key>[a-z][a-z /]+)\\s*:\\s*(?<value>.+)$");
    private static final Pattern COLOR = Pattern.compile("(?i)§[0-9a-fk-or]");

    private CustomScoreboardPolicy() {
    }

    public enum Slot {
        TITLE("Title"),
        PROFILE("Profile"),
        PURSE("Purse"),
        MOTES("Motes"),
        BANK("Bank"),
        BITS("Bits"),
        COPPER("Copper"),
        SOWDUST("Sowdust"),
        GEMS("Gems"),
        HEAT("Heat"),
        COLD("Cold"),
        NORTH_STARS("North Stars"),
        CHUNKED_STATS("Chunked Stats"),
        SOULFLOW("Soulflow"),
        ISLAND("Island"),
        LOCATION("Location"),
        PLAYER_AMOUNT("Players"),
        VISITING("Visiting"),
        DATE("Date"),
        TIME("Time"),
        LOBBY_CODE("Lobby Code"),
        POWER("Power"),
        TUNING("Tuning"),
        COOKIE("Cookie"),
        OBJECTIVE("Objective"),
        SLAYER("Slayer"),
        QUIVER("Quiver"),
        POWDER("Powder"),
        SKYBLOCK_XP("SkyBlock XP"),
        EVENTS("Events"),
        MAYOR("Mayor"),
        PARTY("Party"),
        FOOTER("Footer"),
        EXTRA("Extra"),
        EMPTY("Empty");

        private final String label;

        Slot(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum EventKind {
        VOTING("Voting"),
        SERVER_CLOSE("Server Close"),
        DUNGEONS("Dungeons"),
        KUUDRA("Kuudra"),
        DOJO("Dojo"),
        DARK_AUCTION("Dark Auction"),
        JACOB_CONTEST("Jacob Contest"),
        JACOB_MEDALS("Jacob Medals"),
        TRAPPER("Trapper"),
        GARDEN("Garden"),
        FLIGHT_DURATION("Flight Duration"),
        WINTER("Winter"),
        NEW_YEAR("New Year"),
        SPOOKY("Spooky"),
        BROODMOTHER("Broodmother"),
        MINING("Mining Events"),
        GALATEA("Galatea"),
        SAFARI("Safari"),
        DAMAGE("Damage"),
        MAGMA_BOSS("Magma Boss"),
        CARNIVAL("Carnival"),
        RIFT("Rift"),
        ESSENCE("Essence"),
        QUEUE("Queue"),
        ANNIVERSARY("Anniversary"),
        ACTIVE_TABLIST("Active Tab Events"),
        STARTING_SOON("Starting Soon"),
        REDSTONE("Redstone");

        private final String label;

        EventKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum Align {
        LEFT,
        CENTER,
        RIGHT,
        DONT_ALIGN
    }

    public enum VAlign {
        TOP,
        CENTER,
        BOTTOM,
        DONT_ALIGN
    }

    public enum NumberStyle {
        COMMA,
        COMPACT
    }

    public enum NumberLayout {
        LABEL_WHITE,
        LABEL_COLOR,
        NUMBER_LABEL,
        NUMBER_WHITE_LABEL
    }

    public enum PowderMode {
        AVAILABLE,
        TOTAL,
        BOTH
    }

    public enum ArrowMode {
        COUNT,
        PERCENT
    }

    public enum ChunkStat {
        HEALTH("Health"),
        DEFENSE("Defense"),
        MANA("Mana"),
        OVERFLOW("Overflow"),
        SPEED("Speed"),
        VITALITY("Vitality"),
        STRENGTH("Strength"),
        CRIT_CHANCE("Crit Chance"),
        CRIT_DAMAGE("Crit Damage"),
        INTELLIGENCE("Intelligence"),
        MINING_SPEED("Mining Speed"),
        MINING_FORTUNE("Mining Fortune"),
        FARMING_FORTUNE("Farming Fortune"),
        FORAGING_FORTUNE("Foraging Fortune"),
        MAGIC_FIND("Magic Find"),
        FEROCITY("Ferocity");

        private final String label;

        ChunkStat(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Options(
            List<Slot> appearance,
            List<EventKind> eventOrder,
            boolean showAllEvents,
            boolean hideVanilla,
            boolean showDiff,
            boolean useCustomLines,
            boolean showUnclaimedBits,
            boolean showMaxPlayers,
            PowderMode powderMode,
            NumberStyle numberStyle,
            NumberLayout numberLayout,
            boolean time24h,
            boolean timeExact,
            boolean dateInLobby,
            String dateFormat,
            int lineSpacing,
            Align textAlign,
            boolean showProfileName,
            boolean cacheOnSwitch,
            boolean showOutsideSkyblock,
            Align alignH,
            VAlign alignV,
            int margin,
            ArrowMode arrowMode,
            boolean colorArrows,
            List<ChunkStat> chunkedStats,
            int maxStatsPerLine,
            boolean showMagicalPower,
            boolean compactTuning,
            int tuningAmount,
            boolean showMayorPerks,
            boolean showMayorTime,
            boolean showExtraMayor,
            int maxParty,
            boolean partyEverywhere,
            boolean showPartyLeader,
            Align titleAlign,
            String customTitle,
            boolean useCustomTitle,
            boolean customTitleOutside,
            Align footerAlign,
            String customFooter,
            String customAlphaFooter,
            boolean hideEmpty,
            boolean hideConsecutiveEmpty,
            boolean hideEmptyEdges,
            boolean hideIrrelevant,
            boolean unknownWarning) {
        public Options {
            appearance = appearance == null || appearance.isEmpty()
                    ? defaultAppearance()
                    : List.copyOf(appearance);
            eventOrder = eventOrder == null || eventOrder.isEmpty()
                    ? defaultEvents()
                    : List.copyOf(eventOrder);
            powderMode = powderMode == null ? PowderMode.AVAILABLE : powderMode;
            numberStyle = numberStyle == null ? NumberStyle.COMMA : numberStyle;
            numberLayout = numberLayout == null ? NumberLayout.LABEL_WHITE : numberLayout;
            dateFormat = dateFormat == null || dateFormat.isBlank() ? "MM/dd/yyyy" : dateFormat;
            textAlign = textAlign == null ? Align.LEFT : textAlign;
            alignH = alignH == null ? Align.RIGHT : alignH;
            alignV = alignV == null ? VAlign.CENTER : alignV;
            arrowMode = arrowMode == null ? ArrowMode.COUNT : arrowMode;
            chunkedStats = chunkedStats == null || chunkedStats.isEmpty()
                    ? defaultChunkedStats()
                    : List.copyOf(chunkedStats);
            titleAlign = titleAlign == null ? Align.CENTER : titleAlign;
            customTitle = customTitle == null ? "&&6&&lSKYBLOCK" : customTitle;
            footerAlign = footerAlign == null ? Align.LEFT : footerAlign;
            customFooter = customFooter == null ? "&&ewww.hypixel.net" : customFooter;
            customAlphaFooter = customAlphaFooter == null ? "&&ealpha.hypixel.net" : customAlphaFooter;
            lineSpacing = clamp(lineSpacing, 0, 20);
            margin = clamp(margin, 0, 50);
            maxStatsPerLine = clamp(maxStatsPerLine, 1, 10);
            tuningAmount = clamp(tuningAmount, 1, 8);
            maxParty = clamp(maxParty, 1, 25);
        }
    }

    public record Row(String text, Align align, boolean blank) {
        public Row {
            text = text == null ? "" : text;
            align = align == null ? Align.LEFT : align;
        }

        public String plain() {
            return strip(text);
        }
    }

    public record BoardView(
            boolean skyblock,
            boolean hypixelAlpha,
            String title,
            List<String> sidebar,
            List<String> tab,
            String island,
            String locationHint,
            String profileName,
            String profileType,
            OptionalLong quiverCurrent,
            OptionalLong quiverMax,
            Map<ChunkStat, String> liveStats,
            long nowMillis) {
        public BoardView {
            title = title == null ? "" : title;
            sidebar = sidebar == null ? List.of() : List.copyOf(sidebar);
            tab = tab == null ? List.of() : List.copyOf(tab);
            island = island == null ? "" : island;
            locationHint = locationHint == null ? "" : locationHint;
            profileName = profileName == null ? "" : profileName;
            profileType = profileType == null ? "" : profileType;
            quiverCurrent = quiverCurrent == null ? OptionalLong.empty() : quiverCurrent;
            quiverMax = quiverMax == null ? OptionalLong.empty() : quiverMax;
            liveStats = liveStats == null ? Map.of() : Map.copyOf(liveStats);
        }
    }

    public record ComposeResult(List<Row> rows, List<String> unknownPlain) {
        public ComposeResult {
            rows = rows == null ? List.of() : List.copyOf(rows);
            unknownPlain = unknownPlain == null ? List.of() : List.copyOf(unknownPlain);
        }
    }

    public static final class DeltaBook {
        private final Map<String, Long> previous = new HashMap<>();
        private final Map<String, String> note = new HashMap<>();
        private final Map<String, Long> until = new HashMap<>();

        public String suffix(
                String key,
                long amount,
                boolean enabled,
                NumberStyle style,
                String color,
                long now) {
            if (!enabled) {
                return "";
            }
            Long last = previous.get(key);
            if (last != null && last != amount) {
                long delta = amount - last;
                String formatted = formatNumber(Math.abs(delta), style);
                String sign = delta > 0L ? "+" : "-";
                note.put(key, " §7(" + color + sign + formatted + "§7)");
                until.put(key, now + DELTA_MS);
            }
            previous.put(key, amount);
            Long expires = until.get(key);
            if (expires == null || now >= expires) {
                note.remove(key);
                until.remove(key);
                return "";
            }
            return note.getOrDefault(key, "");
        }
    }

    public static List<Slot> defaultAppearance() {
        return List.of(
                Slot.TITLE,
                Slot.PROFILE,
                Slot.PURSE,
                Slot.MOTES,
                Slot.BANK,
                Slot.BITS,
                Slot.COPPER,
                Slot.SOWDUST,
                Slot.GEMS,
                Slot.NORTH_STARS,
                Slot.HEAT,
                Slot.COLD,
                Slot.EMPTY,
                Slot.ISLAND,
                Slot.LOCATION,
                Slot.LOBBY_CODE,
                Slot.PLAYER_AMOUNT,
                Slot.VISITING,
                Slot.EMPTY,
                Slot.DATE,
                Slot.TIME,
                Slot.EMPTY,
                Slot.CHUNKED_STATS,
                Slot.SOULFLOW,
                Slot.POWER,
                Slot.TUNING,
                Slot.COOKIE,
                Slot.EMPTY,
                Slot.OBJECTIVE,
                Slot.SLAYER,
                Slot.QUIVER,
                Slot.POWDER,
                Slot.SKYBLOCK_XP,
                Slot.EVENTS,
                Slot.MAYOR,
                Slot.PARTY,
                Slot.FOOTER,
                Slot.EXTRA);
    }

    public static List<EventKind> defaultEvents() {
        return List.of(EventKind.values());
    }

    public static List<ChunkStat> defaultChunkedStats() {
        return List.of(
                ChunkStat.HEALTH,
                ChunkStat.DEFENSE,
                ChunkStat.MANA,
                ChunkStat.SPEED);
    }

    public static String defaultAppearanceText() {
        return joinSlots(defaultAppearance());
    }

    public static String defaultEventText() {
        return joinEvents(defaultEvents());
    }

    public static String defaultChunkedText() {
        StringBuilder out = new StringBuilder();
        for (ChunkStat stat : defaultChunkedStats()) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(stat.label());
        }
        return out.toString();
    }

    public static String joinSlots(List<Slot> slots) {
        StringBuilder out = new StringBuilder();
        for (Slot slot : slots) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(slot.label());
        }
        return out.toString();
    }

    public static String joinEvents(List<EventKind> events) {
        StringBuilder out = new StringBuilder();
        for (EventKind event : events) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(event.label());
        }
        return out.toString();
    }

    public static List<Slot> parseAppearance(String raw) {
        List<Slot> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return defaultAppearance();
        }
        for (String line : raw.split("\\R")) {
            Slot slot = parseSlot(line);
            if (slot != null) {
                out.add(slot);
            }
        }
        return out.isEmpty() ? defaultAppearance() : List.copyOf(out);
    }

    public static List<EventKind> parseEvents(String raw) {
        List<EventKind> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return defaultEvents();
        }
        for (String line : raw.split("\\R")) {
            EventKind event = parseEvent(line);
            if (event != null) {
                out.add(event);
            }
        }
        return out.isEmpty() ? defaultEvents() : List.copyOf(out);
    }

    public static List<ChunkStat> parseChunked(String raw) {
        List<ChunkStat> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return defaultChunkedStats();
        }
        for (String line : raw.split("\\R")) {
            ChunkStat stat = parseChunkStat(line);
            if (stat != null) {
                out.add(stat);
            }
        }
        return out.isEmpty() ? defaultChunkedStats() : List.copyOf(out);
    }

    public static Slot parseSlot(String raw) {
        String needle = normalizeToken(raw);
        if (needle.isEmpty()) {
            return null;
        }
        if (needle.startsWith("empty")) {
            return Slot.EMPTY;
        }
        for (Slot slot : Slot.values()) {
            if (normalizeToken(slot.name()).equals(needle)
                    || normalizeToken(slot.label()).equals(needle)) {
                return slot;
            }
        }
        return null;
    }

    public static EventKind parseEvent(String raw) {
        String needle = normalizeToken(raw);
        if (needle.isEmpty()) {
            return null;
        }
        for (EventKind event : EventKind.values()) {
            if (normalizeToken(event.name()).equals(needle)
                    || normalizeToken(event.label()).equals(needle)) {
                return event;
            }
        }
        return null;
    }

    public static ChunkStat parseChunkStat(String raw) {
        String needle = normalizeToken(raw);
        if (needle.isEmpty()) {
            return null;
        }
        for (ChunkStat stat : ChunkStat.values()) {
            if (normalizeToken(stat.name()).equals(needle)
                    || normalizeToken(stat.label()).equals(needle)) {
                return stat;
            }
        }
        return null;
    }

    public static Align parseAlign(String raw) {
        String needle = normalizeToken(raw);
        return switch (needle) {
            case "center" -> Align.CENTER;
            case "right" -> Align.RIGHT;
            case "dontalign", "dont", "none", "free" -> Align.DONT_ALIGN;
            default -> Align.LEFT;
        };
    }

    public static VAlign parseVAlign(String raw) {
        String needle = normalizeToken(raw);
        return switch (needle) {
            case "top" -> VAlign.TOP;
            case "bottom" -> VAlign.BOTTOM;
            case "dontalign", "dont", "none", "free" -> VAlign.DONT_ALIGN;
            default -> VAlign.CENTER;
        };
    }

    public static NumberStyle parseNumberStyle(String raw) {
        String needle = normalizeToken(raw);
        if (needle.contains("compact") || needle.contains("1.2m") || needle.contains("short")) {
            return NumberStyle.COMPACT;
        }
        return NumberStyle.COMMA;
    }

    public static NumberLayout parseNumberLayout(String raw) {
        if (raw == null || raw.isBlank()) {
            return NumberLayout.LABEL_WHITE;
        }
        String trimmed = raw.trim();
        if (trimmed.equalsIgnoreCase("White label: color number")) {
            return NumberLayout.LABEL_WHITE;
        }
        if (trimmed.equalsIgnoreCase("Colored label: number")) {
            return NumberLayout.LABEL_COLOR;
        }
        if (trimmed.equalsIgnoreCase("Color number then label")) {
            return NumberLayout.NUMBER_LABEL;
        }
        if (trimmed.equalsIgnoreCase("Color number, white label")) {
            return NumberLayout.NUMBER_WHITE_LABEL;
        }
        String needle = normalizeToken(raw);
        if (needle.startsWith("whitelabel")) {
            return NumberLayout.LABEL_WHITE;
        }
        if (needle.contains("coloredlabel") || needle.contains("colorlabel")) {
            return NumberLayout.LABEL_COLOR;
        }
        if (needle.contains("numberthen") || needle.equals("colornumberthenlabel")) {
            return NumberLayout.NUMBER_LABEL;
        }
        if (needle.contains("whitelabel") && needle.contains("colornumber")) {
            return NumberLayout.NUMBER_WHITE_LABEL;
        }
        return NumberLayout.LABEL_WHITE;
    }

    public static PowderMode parsePowderMode(String raw) {
        String needle = normalizeToken(raw);
        if (needle.contains("total")) {
            return PowderMode.TOTAL;
        }
        if (needle.contains("both") || needle.contains("availableall") || needle.contains("/")) {
            return PowderMode.BOTH;
        }
        return PowderMode.AVAILABLE;
    }

    public static ArrowMode parseArrowMode(String raw) {
        String needle = normalizeToken(raw);
        if (needle.contains("percent")) {
            return ArrowMode.PERCENT;
        }
        return ArrowMode.COUNT;
    }

    public static String alignLabel(Align align) {
        return switch (align == null ? Align.LEFT : align) {
            case LEFT -> "Left";
            case CENTER -> "Center";
            case RIGHT -> "Right";
            case DONT_ALIGN -> "Don't Align";
        };
    }

    public static String valignLabel(VAlign align) {
        return switch (align == null ? VAlign.CENTER : align) {
            case TOP -> "Top";
            case CENTER -> "Center";
            case BOTTOM -> "Bottom";
            case DONT_ALIGN -> "Don't Align";
        };
    }

    public static List<String> alignOptions() {
        return List.of("Left", "Center", "Right", "Don't Align");
    }

    public static List<String> valignOptions() {
        return List.of("Top", "Center", "Bottom", "Don't Align");
    }

    public static List<String> numberStyleOptions() {
        return List.of("1,234,567", "1.2M");
    }

    public static List<String> numberLayoutOptions() {
        return List.of(
                "White label: color number",
                "Colored label: number",
                "Color number then label",
                "Color number, white label");
    }

    public static List<String> powderOptions() {
        return List.of("Available", "Total", "Available / All");
    }

    public static List<String> arrowOptions() {
        return List.of("Number", "Percentage");
    }

    public static List<String> dateFormatOptions() {
        return List.of(
                "MM/dd/yyyy",
                "MM-dd-yyyy",
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "yyyy-MM-dd",
                "MMM dd, yyyy",
                "MMMM dd, yyyy",
                "yyyy/MM/dd",
                "dd MMM yyyy",
                "dd MMMM yyyy");
    }

    public static QolNumberSettings.Spec numberSpec(String settingId) {
        if (settingId == null) {
            return null;
        }
        return switch (settingId) {
            case "qol.custom_scoreboard.line_spacing" -> new QolNumberSettings.Spec(0, 20, 1, true);
            case "qol.custom_scoreboard.margin" -> new QolNumberSettings.Spec(0, 50, 1, true);
            case "qol.custom_scoreboard.max_stats_per_line" -> new QolNumberSettings.Spec(1, 10, 1, true);
            case "qol.custom_scoreboard.tuning_amount" -> new QolNumberSettings.Spec(1, 8, 1, true);
            case "qol.custom_scoreboard.max_party" -> new QolNumberSettings.Spec(1, 25, 1, true);
            case "qol.custom_scoreboard.bg_border" -> new QolNumberSettings.Spec(0, 20, 1, true);
            case "qol.custom_scoreboard.bg_round" -> new QolNumberSettings.Spec(0, 30, 1, true);
            case "qol.custom_scoreboard.outline_thickness" -> new QolNumberSettings.Spec(1, 15, 1, true);
            case "qol.custom_scoreboard.outline_blur" -> new QolNumberSettings.Spec(0, 1, 0.1, true);
            case "qol.custom_scoreboard.custom_bg_opacity" -> new QolNumberSettings.Spec(0, 100, 1, true);
            default -> null;
        };
    }

    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return COLOR.matcher(text).replaceAll("").replace('\u00a0', ' ').trim();
    }

    public static String decodeMarkup(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw.replace("\\n", "\n").replace("&&", "§");
        text = text.replace("&", "§");
        return text;
    }

    public static String formatNumber(long value, NumberStyle style) {
        if (style == NumberStyle.COMPACT) {
            return compact(value);
        }
        return String.format(Locale.US, "%,d", value);
    }

    public static ComposeResult compose(BoardView view, Options options, DeltaBook deltas) {
        if (view == null || options == null) {
            return new ComposeResult(List.of(), List.of());
        }
        ParsedBoard parsed = parse(view);
        List<Row> rows;
        if (!view.skyblock()) {
            rows = vanillaBoard(view, options, true);
        } else if (!options.useCustomLines()) {
            rows = vanillaBoard(view, options, false);
        } else {
            rows = customBoard(view, options, parsed, deltas == null ? new DeltaBook() : deltas);
        }
        rows = trimEdges(rows, options.hideEmptyEdges());
        return new ComposeResult(rows, parsed.unknown);
    }

    public static int panelX(int screenW, int panelW, Align align, int margin, int poseX) {
        int m = clamp(margin, 0, 50);
        return switch (align == null ? Align.RIGHT : align) {
            case LEFT -> m;
            case CENTER -> Math.max(m, (screenW - panelW) / 2);
            case RIGHT -> Math.max(m, screenW - panelW - m);
            case DONT_ALIGN -> poseX;
        };
    }

    public static int panelY(int screenH, int panelH, VAlign align, int margin, int poseY) {
        int m = clamp(margin, 0, 50);
        return switch (align == null ? VAlign.CENTER : align) {
            case TOP -> m;
            case CENTER -> Math.max(m, (screenH - panelH) / 2);
            case BOTTOM -> Math.max(m, screenH - panelH - m);
            case DONT_ALIGN -> poseY;
        };
    }

    public static int lineAdvance(int lineSpacing) {
        return 10 + Math.max(0, lineSpacing - 10);
    }

    public static String formatLobbyDate(String pattern, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        try {
            return day.format(DateTimeFormatter.ofPattern(
                    pattern == null || pattern.isBlank() ? "MM/dd/yyyy" : pattern,
                    Locale.US));
        } catch (RuntimeException ignored) {
            return day.format(DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));
        }
    }

    private static List<Row> vanillaBoard(BoardView view, Options options, boolean outside) {
        List<Row> rows = new ArrayList<>();
        addAll(rows, titleRows(view, options, outside));
        for (String line : view.sidebar()) {
            rows.add(new Row(line, options.textAlign(), strip(line).isEmpty()));
        }
        return rows;
    }

    private static List<Row> customBoard(
            BoardView view,
            Options options,
            ParsedBoard parsed,
            DeltaBook deltas) {
        List<Row> rows = new ArrayList<>();
        for (Slot slot : options.appearance()) {
            List<Row> chunk = slotRows(slot, view, options, parsed, deltas);
            if (chunk.isEmpty()) {
                continue;
            }
            if (options.hideConsecutiveEmpty()
                    && chunk.get(0).blank()
                    && !rows.isEmpty()
                    && rows.get(rows.size() - 1).blank()) {
                continue;
            }
            rows.addAll(chunk);
        }
        return rows;
    }

    private static List<Row> slotRows(
            Slot slot,
            BoardView view,
            Options options,
            ParsedBoard parsed,
            DeltaBook deltas) {
        boolean hideEmpty = options.hideEmpty();
        boolean hideWrong = options.hideIrrelevant();
        Place place = Place.from(view, parsed);
        return switch (slot) {
            case TITLE -> titleRows(view, options, false);
            case PROFILE -> singleton(
                    profileLine(view, parsed, options),
                    options.textAlign(),
                    hideEmpty);
            case PURSE -> moneyLine(
                    "Purse",
                    parsed.purse,
                    "§6",
                    options,
                    deltas,
                    "purse",
                    view.nowMillis(),
                    hideEmpty && parsed.purse.isEmpty(),
                    hideWrong && place.inRift());
            case MOTES -> moneyLine(
                    "Motes",
                    parsed.motes,
                    "§d",
                    options,
                    deltas,
                    "motes",
                    view.nowMillis(),
                    hideEmpty && parsed.motes.isEmpty(),
                    hideWrong && !place.inRift());
            case BANK -> singleton(
                    bankLine(parsed, options),
                    options.textAlign(),
                    hideEmpty && parsed.bank.isEmpty() && parsed.personalBank.isEmpty());
            case BITS -> bitsLine(parsed, options, deltas, view.nowMillis(), hideEmpty);
            case COPPER -> moneyLine(
                    "Copper",
                    parsed.copper,
                    "§c",
                    options,
                    deltas,
                    "copper",
                    view.nowMillis(),
                    hideEmpty && parsed.copper.isEmpty(),
                    hideWrong && !place.inGarden());
            case SOWDUST -> moneyLine(
                    "Sowdust",
                    parsed.sowdust,
                    "§e",
                    options,
                    deltas,
                    "sowdust",
                    view.nowMillis(),
                    hideEmpty && parsed.sowdust.isEmpty(),
                    hideWrong && !place.inGarden());
            case GEMS -> moneyLine(
                    "Gems",
                    parsed.gems,
                    "§a",
                    options,
                    deltas,
                    "gems",
                    view.nowMillis(),
                    hideEmpty && parsed.gems.isEmpty(),
                    false);
            case HEAT -> singleton(
                    parsed.heat,
                    options.textAlign(),
                    hideEmpty && parsed.heat.isBlank() || hideWrong && !place.inCrimson());
            case COLD -> singleton(
                    parsed.cold,
                    options.textAlign(),
                    hideEmpty && parsed.cold.isBlank() || hideWrong && !place.inGlacite());
            case NORTH_STARS -> moneyLine(
                    "North Stars",
                    parsed.northStars,
                    "§d",
                    options,
                    deltas,
                    "north",
                    view.nowMillis(),
                    hideEmpty && parsed.northStars.isEmpty(),
                    hideWrong && !place.inWinter());
            case CHUNKED_STATS -> chunkedRows(view, options, parsed, hideEmpty);
            case SOULFLOW -> moneyLine(
                    "Soulflow",
                    parsed.soulflow,
                    "§3",
                    options,
                    deltas,
                    "soulflow",
                    view.nowMillis(),
                    hideEmpty && parsed.soulflow.isEmpty(),
                    false);
            case ISLAND -> singleton(
                    firstNonBlank(parsed.island, view.island()),
                    options.textAlign(),
                    hideEmpty);
            case LOCATION -> singleton(
                    firstNonBlank(parsed.location, view.locationHint()),
                    options.textAlign(),
                    hideEmpty);
            case PLAYER_AMOUNT -> playersLine(parsed, options, hideEmpty);
            case VISITING -> singleton(parsed.visiting, options.textAlign(), hideEmpty);
            case DATE -> singleton(parsed.date, options.textAlign(), hideEmpty);
            case TIME -> singleton(
                    formatTime(parsed.time, options),
                    options.textAlign(),
                    hideEmpty);
            case LOBBY_CODE -> singleton(
                    lobbyLine(parsed, options),
                    options.textAlign(),
                    hideEmpty && parsed.lobbyCode.isBlank());
            case POWER -> singleton(parsed.power, options.textAlign(), hideEmpty);
            case TUNING -> tuningRows(parsed, options, hideEmpty);
            case COOKIE -> singleton(parsed.cookie, options.textAlign(), hideEmpty);
            case OBJECTIVE -> copyLines(parsed.objective, options.textAlign(), hideEmpty);
            case SLAYER -> copyLines(parsed.slayer, options.textAlign(), hideEmpty);
            case QUIVER -> quiverLine(view, parsed, options, hideEmpty);
            case POWDER -> powderRows(parsed, options, hideEmpty, hideWrong && !place.inMining());
            case SKYBLOCK_XP -> singleton(parsed.skyblockXp, options.textAlign(), hideEmpty);
            case EVENTS -> eventRows(parsed, options, hideEmpty);
            case MAYOR -> mayorRows(parsed, options, hideEmpty);
            case PARTY -> partyRows(parsed, options, place, hideEmpty, hideWrong);
            case FOOTER -> footerRows(view, options);
            case EXTRA -> copyLines(parsed.unknown, options.textAlign(), hideEmpty);
            case EMPTY -> List.of(new Row("", options.textAlign(), true));
        };
    }

    private static List<Row> titleRows(BoardView view, Options options, boolean outside) {
        boolean custom = options.useCustomTitle()
                && (!outside || options.customTitleOutside());
        String source = custom ? decodeMarkup(options.customTitle()) : view.title();
        if (source.isBlank()) {
            source = view.title();
        }
        List<Row> rows = new ArrayList<>();
        for (String line : source.split("\\R", -1)) {
            rows.add(new Row(line, options.titleAlign(), strip(line).isEmpty()));
        }
        return rows;
    }

    private static List<Row> footerRows(BoardView view, Options options) {
        String source = view.hypixelAlpha()
                ? decodeMarkup(options.customAlphaFooter())
                : decodeMarkup(options.customFooter());
        List<Row> rows = new ArrayList<>();
        for (String line : source.split("\\R", -1)) {
            rows.add(new Row(line, options.footerAlign(), strip(line).isEmpty()));
        }
        return rows;
    }

    private static List<Row> moneyLine(
            String label,
            OptionalLong amount,
            String color,
            Options options,
            DeltaBook deltas,
            String key,
            long now,
            boolean skipEmpty,
            boolean skipWrong) {
        if (skipWrong || skipEmpty || amount.isEmpty()) {
            return List.of();
        }
        String note = deltas.suffix(key, amount.getAsLong(), options.showDiff(), options.numberStyle(), color, now);
        String number = color + formatNumber(amount.getAsLong(), options.numberStyle()) + note;
        return List.of(new Row(layout(label, number, color, options.numberLayout()), options.textAlign(), false));
    }

    private static List<Row> bitsLine(
            ParsedBoard parsed,
            Options options,
            DeltaBook deltas,
            long now,
            boolean hideEmpty) {
        if (parsed.bits.isEmpty()) {
            return hideEmpty ? List.of() : List.of();
        }
        String note = deltas.suffix(
                "bits", parsed.bits.getAsLong(), options.showDiff(), options.numberStyle(), "§b", now);
        String number = "§b" + formatNumber(parsed.bits.getAsLong(), options.numberStyle());
        if (options.showUnclaimedBits() && parsed.bitsAvailable.isPresent()) {
            number += "§7/§b" + formatNumber(parsed.bitsAvailable.getAsLong(), options.numberStyle());
        }
        number += note;
        return List.of(new Row(layout("Bits", number, "§b", options.numberLayout()), options.textAlign(), false));
    }

    private static String bankLine(ParsedBoard parsed, Options options) {
        if (parsed.bank.isEmpty() && parsed.personalBank.isEmpty()) {
            return "";
        }
        String coop = parsed.bank.isPresent()
                ? formatNumber(parsed.bank.getAsLong(), options.numberStyle())
                : "0";
        if (parsed.personalBank.isPresent()) {
            coop += " §7/ §6" + formatNumber(parsed.personalBank.getAsLong(), options.numberStyle());
        }
        return layout("Bank", "§6" + coop, "§6", options.numberLayout());
    }

    private static List<Row> playersLine(ParsedBoard parsed, Options options, boolean hideEmpty) {
        if (parsed.players.isEmpty()) {
            return hideEmpty ? List.of() : List.of();
        }
        String text = "§a" + parsed.players.getAsLong();
        if (options.showMaxPlayers() && parsed.maxPlayers.isPresent()) {
            text += "§7/§a" + parsed.maxPlayers.getAsLong();
        }
        return List.of(new Row(layout("Players", text, "§a", options.numberLayout()), options.textAlign(), false));
    }

    private static List<Row> quiverLine(
            BoardView view, ParsedBoard parsed, Options options, boolean hideEmpty) {
        long current = view.quiverCurrent().orElse(parsed.arrows.orElse(-1L));
        if (current < 0L) {
            return hideEmpty ? List.of() : List.of();
        }
        long max = view.quiverMax().orElse(parsed.arrowMax.orElse(0L));
        String color = "§f";
        if (options.colorArrows() && max > 0L) {
            double ratio = current / (double) max;
            color = ratio > 0.5D ? "§a" : ratio > 0.15D ? "§e" : "§c";
        }
        String amount;
        if (options.arrowMode() == ArrowMode.PERCENT && max > 0L) {
            amount = color + Math.round(100.0D * current / max) + "%";
        } else {
            amount = color + formatNumber(current, options.numberStyle());
        }
        return List.of(new Row(layout("Arrows", amount, color, options.numberLayout()), options.textAlign(), false));
    }

    private static List<Row> powderRows(
            ParsedBoard parsed, Options options, boolean hideEmpty, boolean skipWrong) {
        if (skipWrong) {
            return List.of();
        }
        List<Row> rows = new ArrayList<>();
        parsed.powder.forEach((type, pair) -> {
            long shown = switch (options.powderMode()) {
                case TOTAL -> pair.total;
                case BOTH -> pair.available;
                case AVAILABLE -> pair.available;
            };
            String number = "§b" + formatNumber(shown, options.numberStyle());
            if (options.powderMode() == PowderMode.BOTH) {
                number += "§7 / §b" + formatNumber(pair.total, options.numberStyle());
            }
            rows.add(new Row(layout(type + " Powder", number, "§b", options.numberLayout()), options.textAlign(), false));
        });
        if (rows.isEmpty() && hideEmpty) {
            return List.of();
        }
        return rows;
    }

    private static List<Row> chunkedRows(
            BoardView view, Options options, ParsedBoard parsed, boolean hideEmpty) {
        List<String> tokens = new ArrayList<>();
        for (ChunkStat stat : options.chunkedStats()) {
            String value = view.liveStats().get(stat);
            if (value == null || value.isBlank()) {
                value = parsed.chunked.get(stat);
            }
            if (value == null || value.isBlank()) {
                continue;
            }
            tokens.add(chunkIcon(stat) + value);
        }
        if (tokens.isEmpty()) {
            return hideEmpty ? List.of() : List.of();
        }
        List<Row> rows = new ArrayList<>();
        int per = Math.max(1, options.maxStatsPerLine());
        for (int i = 0; i < tokens.size(); i += per) {
            int end = Math.min(tokens.size(), i + per);
            rows.add(new Row(String.join("  ", tokens.subList(i, end)), options.textAlign(), false));
        }
        return rows;
    }

    private static List<Row> tuningRows(ParsedBoard parsed, Options options, boolean hideEmpty) {
        if (parsed.tunings.isEmpty() && parsed.magicalPower.isEmpty()) {
            return hideEmpty ? List.of() : List.of();
        }
        List<Row> rows = new ArrayList<>();
        if (options.showMagicalPower() && parsed.magicalPower.isPresent()) {
            rows.add(new Row(
                    layout("Magical Power", "§b" + formatNumber(parsed.magicalPower.getAsLong(), options.numberStyle()),
                            "§b", options.numberLayout()),
                    options.textAlign(),
                    false));
        }
        if (parsed.tunings.isEmpty()) {
            return rows;
        }
        if (options.compactTuning()) {
            rows.add(new Row("§e" + String.join("§7, §e", parsed.tunings), options.textAlign(), false));
            return rows;
        }
        int limit = Math.min(parsed.tunings.size(), options.tuningAmount());
        for (int i = 0; i < limit; i++) {
            rows.add(new Row("§e" + parsed.tunings.get(i), options.textAlign(), false));
        }
        return rows;
    }

    private static List<Row> mayorRows(ParsedBoard parsed, Options options, boolean hideEmpty) {
        if (parsed.mayor.isBlank()) {
            return hideEmpty ? List.of() : List.of();
        }
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("§aMayor: §f" + parsed.mayor, options.textAlign(), false));
        if (options.showMayorPerks()) {
            for (String perk : parsed.mayorPerks) {
                rows.add(new Row("§7- §f" + perk, options.textAlign(), false));
            }
        }
        if (options.showExtraMayor() && !parsed.minister.isBlank()) {
            rows.add(new Row("§aMinister: §f" + parsed.minister, options.textAlign(), false));
            if (!parsed.ministerPerk.isBlank()) {
                rows.add(new Row("§7- §f" + parsed.ministerPerk, options.textAlign(), false));
            }
        }
        if (options.showMayorTime() && !parsed.mayorTime.isBlank()) {
            rows.add(new Row("§e" + parsed.mayorTime, options.textAlign(), false));
        }
        return rows;
    }

    private static List<Row> partyRows(
            ParsedBoard parsed,
            Options options,
            Place place,
            boolean hideEmpty,
            boolean hideWrong) {
        if (hideWrong && !options.partyEverywhere() && !place.partyIsland()) {
            return List.of();
        }
        if (parsed.party.isEmpty()) {
            return hideEmpty ? List.of() : List.of();
        }
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("§9Party", options.textAlign(), false));
        int shown = 0;
        for (int i = 0; i < parsed.party.size(); i++) {
            String name = parsed.party.get(i);
            if (!options.showPartyLeader() && i == 0 && parsed.partyLeader) {
                continue;
            }
            if (shown >= options.maxParty()) {
                rows.add(new Row("§7+" + (parsed.party.size() - i), options.textAlign(), false));
                break;
            }
            String prefix = parsed.partyLeader && i == 0 ? "§6★ §f" : "§f";
            rows.add(new Row(prefix + name, options.textAlign(), false));
            shown++;
        }
        return rows;
    }

    private static List<Row> eventRows(ParsedBoard parsed, Options options, boolean hideEmpty) {
        List<Row> rows = new ArrayList<>();
        for (EventKind kind : options.eventOrder()) {
            List<String> lines = parsed.events.get(kind);
            if (lines == null || lines.isEmpty()) {
                continue;
            }
            for (String line : lines) {
                rows.add(new Row(line, options.textAlign(), strip(line).isEmpty()));
            }
            if (!options.showAllEvents()) {
                break;
            }
        }
        if (rows.isEmpty() && hideEmpty) {
            return List.of();
        }
        return rows;
    }

    private static String profileLine(BoardView view, ParsedBoard parsed, Options options) {
        String symbol = profileSymbol(firstNonBlank(parsed.profileType, view.profileType()));
        if (options.showProfileName() && !view.profileName().isBlank()) {
            return symbol + view.profileName();
        }
        String type = firstNonBlank(parsed.profileType, view.profileType(), "Profile");
        return symbol + type;
    }

    private static String lobbyLine(ParsedBoard parsed, Options options) {
        if (parsed.lobbyCode.isBlank()) {
            return "";
        }
        if (options.dateInLobby()) {
            return "§7" + formatLobbyDate(options.dateFormat(), LocalDate.now())
                    + " §8" + parsed.lobbyCode;
        }
        return "§8" + parsed.lobbyCode;
    }

    private static String formatTime(String raw, Options options) {
        if (raw.isBlank() || !options.time24h() && !options.timeExact()) {
            return raw;
        }
        Matcher matcher = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
                .matcher(strip(raw));
        if (!matcher.find()) {
            return raw;
        }
        int hour = Integer.parseInt(matcher.group(1));
        int minute = Integer.parseInt(matcher.group(2));
        String ampm = matcher.group(3);
        if (options.time24h() && ampm != null) {
            if (ampm.equalsIgnoreCase("pm") && hour < 12) {
                hour += 12;
            }
            if (ampm.equalsIgnoreCase("am") && hour == 12) {
                hour = 0;
            }
        }
        if (!options.timeExact()) {
            minute = (minute / 10) * 10;
        }
        String clock = options.time24h()
                ? String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                : matcher.group(0);
        return raw.replace(matcher.group(0), clock);
    }

    private static String layout(String label, String number, String color, NumberLayout mode) {
        return switch (mode) {
            case LABEL_WHITE -> "§f" + label + ": " + number;
            case LABEL_COLOR -> color + label + ": " + stripColorPrefix(number);
            case NUMBER_LABEL -> number + " " + color + label;
            case NUMBER_WHITE_LABEL -> number + " §f" + label;
        };
    }

    private static String stripColorPrefix(String number) {
        if (number.startsWith("§") && number.length() >= 2) {
            return number.substring(2);
        }
        return number;
    }

    private static String chunkIcon(ChunkStat stat) {
        return switch (stat) {
            case HEALTH -> "§c❤ ";
            case DEFENSE -> "§a❈ ";
            case MANA -> "§b✎ ";
            case OVERFLOW -> "§3ʬ ";
            case SPEED -> "§f✦ ";
            case VITALITY -> "§4♨ ";
            case STRENGTH -> "§c❁ ";
            case CRIT_CHANCE -> "§9☣ ";
            case CRIT_DAMAGE -> "§9☠ ";
            case INTELLIGENCE -> "§b✎ ";
            case MINING_SPEED -> "§6⸕ ";
            case MINING_FORTUNE -> "§6☘ ";
            case FARMING_FORTUNE -> "§6☘ ";
            case FORAGING_FORTUNE -> "§6☘ ";
            case MAGIC_FIND -> "§b✯ ";
            case FEROCITY -> "§c⫽ ";
        };
    }

    private static String profileSymbol(String type) {
        String needle = type.toLowerCase(Locale.ROOT);
        if (needle.contains("iron")) {
            return "§7♲ ";
        }
        if (needle.contains("strand")) {
            return "§a☀ ";
        }
        if (needle.contains("bingo")) {
            return "§e❤ ";
        }
        return "§e";
    }

    private static List<Row> singleton(String text, Align align, boolean skip) {
        if (skip || text == null || strip(text).isEmpty()) {
            return List.of();
        }
        return List.of(new Row(text, align, false));
    }

    private static List<Row> copyLines(List<String> lines, Align align, boolean hideEmpty) {
        if (lines == null || lines.isEmpty()) {
            return hideEmpty ? List.of() : List.of();
        }
        List<Row> rows = new ArrayList<>();
        for (String line : lines) {
            rows.add(new Row(line, align, strip(line).isEmpty()));
        }
        return rows;
    }

    private static List<Row> trimEdges(List<Row> rows, boolean hide) {
        if (!hide || rows.isEmpty()) {
            return rows;
        }
        int start = 0;
        int end = rows.size() - 1;
        while (start <= end && rows.get(start).blank()) {
            start++;
        }
        while (end >= start && rows.get(end).blank()) {
            end--;
        }
        if (start > end) {
            return List.of();
        }
        return List.copyOf(rows.subList(start, end + 1));
    }

    private static void addAll(List<Row> rows, List<Row> extra) {
        rows.addAll(extra);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !strip(value).isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String compact(long value) {
        long abs = Math.abs(value);
        String sign = value < 0L ? "-" : "";
        if (abs < 1_000L) {
            return sign + abs;
        }
        if (abs < 1_000_000L) {
            return sign + trimDecimal(abs / 1_000.0D) + "k";
        }
        if (abs < 1_000_000_000L) {
            return sign + trimDecimal(abs / 1_000_000.0D) + "M";
        }
        return sign + trimDecimal(abs / 1_000_000_000.0D) + "B";
    }

    private static String trimDecimal(double value) {
        String text = String.format(Locale.US, "%.1f", value);
        if (text.endsWith(".0")) {
            return text.substring(0, text.length() - 2);
        }
        return text;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/]+", "");
    }

    private static OptionalLong parseLong(String raw) {
        Matcher matcher = NUMBER.matcher(strip(raw).replace(",", ""));
        if (!matcher.find()) {
            return OptionalLong.empty();
        }
        try {
            String token = matcher.group(1).replace(",", "");
            if (token.contains(".")) {
                return OptionalLong.of((long) Double.parseDouble(token));
            }
            return OptionalLong.of(Long.parseLong(token));
        } catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    private static ParsedBoard parse(BoardView view) {
        ParsedBoard parsed = new ParsedBoard();
        parsed.island = view.island();
        parsed.location = view.locationHint();
        parsed.profileType = view.profileType();
        for (String raw : view.sidebar()) {
            consumeSidebar(raw, parsed);
        }
        for (String raw : view.tab()) {
            consumeTab(raw, parsed);
        }
        return parsed;
    }

    private static void consumeSidebar(String raw, ParsedBoard parsed) {
        String plain = strip(raw);
        String lower = plain.toLowerCase(Locale.ROOT);
        if (plain.isBlank()) {
            return;
        }
        if (lower.contains("www.hypixel.net") || lower.contains("alpha.hypixel.net")) {
            return;
        }
        if (assignMoney(lower, "purse", raw, v -> parsed.purse = v)
                || assignMoney(lower, "piggy", raw, v -> parsed.purse = v)
                || assignMoney(lower, "motes", raw, v -> parsed.motes = v)
                || assignMoney(lower, "bits", raw, v -> parsed.bits = v)
                || assignMoney(lower, "copper", raw, v -> parsed.copper = v)
                || assignMoney(lower, "sowdust", raw, v -> parsed.sowdust = v)
                || assignMoney(lower, "gems", raw, v -> parsed.gems = v)
                || assignMoney(lower, "north star", raw, v -> parsed.northStars = v)
                || assignMoney(lower, "soulflow", raw, v -> parsed.soulflow = v)
                || assignMoney(lower, "bank", raw, v -> parsed.bank = v)) {
            return;
        }
        if (lower.startsWith("heat") || lower.contains("heat:")) {
            parsed.heat = raw;
            return;
        }
        if (lower.startsWith("cold") || lower.contains("cold:")) {
            parsed.cold = raw;
            return;
        }
        if (plain.contains("⏣") || lower.startsWith(" ⏣") || lower.contains("location")) {
            parsed.location = raw;
            return;
        }
        if (looksLikeDate(plain)) {
            parsed.date = raw;
            return;
        }
        if (looksLikeTime(plain)) {
            parsed.time = raw;
            return;
        }
        if (looksLikeLobby(plain)) {
            parsed.lobbyCode = extractLobby(plain);
            return;
        }
        if (lower.contains("visiting") || plain.contains("✌")) {
            parsed.visiting = raw;
            return;
        }
        if (lower.contains("slayer quest") || (!parsed.slayer.isEmpty() && looksSlayerFollow(lower))) {
            parsed.slayer.add(raw);
            return;
        }
        if (lower.startsWith("objective") || lower.startsWith("quest")
                || (!parsed.objective.isEmpty() && (lower.startsWith(" ") || lower.startsWith("-")))) {
            parsed.objective.add(raw);
            return;
        }
        if (lower.contains("arrow")) {
            parsed.arrows = parseLong(plain);
            Matcher max = Pattern.compile("/\\s*([\\d,]+)").matcher(plain);
            if (max.find()) {
                parsed.arrowMax = parseLong(max.group(1));
            }
            return;
        }
        Matcher powder = POWDER.matcher(plain);
        if (powder.find()) {
            String type = capitalize(powder.group(1));
            PowderPair pair = parsed.powder.computeIfAbsent(type, ignored -> new PowderPair());
            pair.available = parseCompact(powder.group(2));
            pair.total = Math.max(pair.total, pair.available);
            return;
        }
        if (lower.contains("ironman") || lower.contains("stranded") || lower.contains("bingo")) {
            parsed.profileType = plain;
            return;
        }
        EventKind event = classifyEvent(lower);
        if (event != null) {
            parsed.events.computeIfAbsent(event, ignored -> new ArrayList<>()).add(raw);
            return;
        }
        parsed.unknown.add(raw);
    }

    private static void consumeTab(String raw, ParsedBoard parsed) {
        String plain = strip(raw);
        Matcher matcher = TAB_LABEL.matcher(plain);
        if (!matcher.matches()) {
            if (plain.toLowerCase(Locale.ROOT).contains("party") && parsed.party.isEmpty()) {
                parsed.readingParty = true;
            } else if (parsed.readingParty) {
                if (plain.isBlank() || plain.contains(":")) {
                    parsed.readingParty = false;
                } else {
                    parsed.party.add(plain.replace("★", "").trim());
                    if (raw.contains("★") || raw.contains("leader")) {
                        parsed.partyLeader = true;
                    }
                }
            }
            return;
        }
        String key = matcher.group("key").trim().toLowerCase(Locale.ROOT);
        String value = matcher.group("value").trim();
        switch (key) {
            case "area", "island" -> parsed.island = value;
            case "profile" -> parsed.profileType = value;
            case "bank" -> parsed.bank = parseLong(value);
            case "purse" -> parsed.purse = parseLong(value);
            case "bits" -> parsed.bits = parseLong(value);
            case "unclaimed bits", "bits available" -> parsed.bitsAvailable = parseLong(value);
            case "soulflow" -> parsed.soulflow = parseLong(value);
            case "cookie buff", "cookie" -> parsed.cookie = "§dCookie: §f" + value;
            case "sb level", "skyblock xp", "skyblock level" -> parsed.skyblockXp = "§bSB XP: §f" + value;
            case "magical power" -> parsed.magicalPower = parseLong(value);
            case "mayor" -> parsed.mayor = value;
            case "minister" -> parsed.minister = value;
            case "election", "next mayor" -> parsed.mayorTime = value;
            case "players" -> {
                parsed.players = parseLong(value);
                Matcher max = Pattern.compile("/\\s*([\\d,]+)").matcher(value);
                if (max.find()) {
                    parsed.maxPlayers = parseLong(max.group(1));
                }
            }
            case "power" -> parsed.power = "§aPower: §f" + value;
            default -> {
                ChunkStat stat = parseChunkStat(key);
                if (stat != null) {
                    parsed.chunked.put(stat, value);
                } else if (key.contains("tuning")) {
                    parsed.tunings.add(value);
                } else if (key.contains("perk") && !parsed.mayor.isBlank()) {
                    parsed.mayorPerks.add(value);
                }
            }
        }
    }

    private static boolean assignMoney(
            String lower,
            String label,
            String raw,
            java.util.function.Consumer<OptionalLong> sink) {
        if (!lower.contains(label)) {
            return false;
        }
        sink.accept(parseLong(raw));
        return true;
    }

    private static boolean looksLikeDate(String plain) {
        return Pattern.compile("(?i)(early |late )?(spring|summer|autumn|fall|winter)\\s+\\d+")
                .matcher(plain)
                .find();
    }

    private static boolean looksLikeTime(String plain) {
        return Pattern.compile("\\d{1,2}:\\d{2}\\s*(am|pm)", Pattern.CASE_INSENSITIVE).matcher(plain).find();
    }

    private static boolean looksLikeLobby(String plain) {
        return Pattern.compile("(?i)\\b(m|mini|mega)[0-9A-Z]+\\b").matcher(plain).find()
                || Pattern.compile("\\d{2}/\\d{2}/\\d{2}").matcher(plain).find();
    }

    private static String extractLobby(String plain) {
        Matcher matcher = Pattern.compile("(?i)\\b((?:m|mini|mega)[0-9A-Z]+)\\b").matcher(plain);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return plain;
    }

    private static boolean looksSlayerFollow(String lower) {
        return lower.contains("combat xp")
                || lower.contains("boss")
                || lower.contains("spawned")
                || lower.contains("slain")
                || lower.contains("/");
    }

    private static EventKind classifyEvent(String lower) {
        if (lower.contains("year") && lower.contains("vote") || lower.contains("waiting for") && lower.contains("vote")) {
            return EventKind.VOTING;
        }
        if (lower.contains("instance shutdown") || lower.contains("server clos")) {
            return EventKind.SERVER_CLOSE;
        }
        if (lower.contains("cleared:") || lower.contains("keys:") || lower.contains("alive dragon")) {
            return EventKind.DUNGEONS;
        }
        if (lower.contains("wave:") || lower.contains("tokens:") || lower.contains("submerges")) {
            return EventKind.KUUDRA;
        }
        if (lower.contains("challenge:") && lower.contains("dojo") || lower.startsWith("challenge:")) {
            return EventKind.DOJO;
        }
        if (lower.contains("dark auction")) {
            return EventKind.DARK_AUCTION;
        }
        if (lower.contains("jacob")) {
            return lower.contains("medal") ? EventKind.JACOB_MEDALS : EventKind.JACOB_CONTEST;
        }
        if (lower.contains("pelt") || lower.contains("tracker mob")) {
            return EventKind.TRAPPER;
        }
        if (lower.contains("cleanup") || lower.contains("pasting") || lower.contains("plot -")) {
            return EventKind.GARDEN;
        }
        if (lower.contains("flight duration")) {
            return EventKind.FLIGHT_DURATION;
        }
        if (lower.contains("winter") && (lower.contains("event") || lower.contains("gift"))) {
            return EventKind.WINTER;
        }
        if (lower.contains("new year")) {
            return EventKind.NEW_YEAR;
        }
        if (lower.contains("spooky")) {
            return EventKind.SPOOKY;
        }
        if (lower.contains("broodmother")) {
            return EventKind.BROODMOTHER;
        }
        if (lower.contains("event:") || lower.contains("zone:") || lower.contains("raffle")
                || lower.contains("goblin") || lower.contains("tasty mithril")) {
            return EventKind.MINING;
        }
        if (lower.contains("galatea") || lower.contains("moonglade")) {
            return EventKind.GALATEA;
        }
        if (lower.contains("safari")) {
            return EventKind.SAFARI;
        }
        if (lower.contains("your damage") || lower.contains("boss hp") || lower.contains("dragon hp")) {
            return EventKind.DAMAGE;
        }
        if (lower.contains("magma") || lower.contains("damage soaked")) {
            return EventKind.MAGMA_BOSS;
        }
        if (lower.contains("carnival")) {
            return EventKind.CARNIVAL;
        }
        if (lower.contains("rift") || lower.contains("timecharm") || lower.contains("enigma")) {
            return EventKind.RIFT;
        }
        if (lower.contains("essence:")) {
            return EventKind.ESSENCE;
        }
        if (lower.contains("queue") || lower.contains("position:")) {
            return EventKind.QUEUE;
        }
        if (lower.contains("anniversary") || lower.contains("skyblock anniversary")) {
            return EventKind.ANNIVERSARY;
        }
        if (lower.contains("starting soon") || lower.contains("starts in")) {
            return EventKind.STARTING_SOON;
        }
        if (lower.contains("redstone:")) {
            return EventKind.REDSTONE;
        }
        if (lower.contains("active event")) {
            return EventKind.ACTIVE_TABLIST;
        }
        return null;
    }

    private static long parseCompact(String raw) {
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
            return (long) (Double.parseDouble(token) * mul);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static String capitalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1).toLowerCase(Locale.ROOT);
    }

    private static final class PowderPair {
        long available;
        long total;
    }

    private static final class ParsedBoard {
        OptionalLong purse = OptionalLong.empty();
        OptionalLong motes = OptionalLong.empty();
        OptionalLong bank = OptionalLong.empty();
        OptionalLong personalBank = OptionalLong.empty();
        OptionalLong bits = OptionalLong.empty();
        OptionalLong bitsAvailable = OptionalLong.empty();
        OptionalLong copper = OptionalLong.empty();
        OptionalLong sowdust = OptionalLong.empty();
        OptionalLong gems = OptionalLong.empty();
        OptionalLong northStars = OptionalLong.empty();
        OptionalLong soulflow = OptionalLong.empty();
        OptionalLong arrows = OptionalLong.empty();
        OptionalLong arrowMax = OptionalLong.empty();
        OptionalLong players = OptionalLong.empty();
        OptionalLong maxPlayers = OptionalLong.empty();
        OptionalLong magicalPower = OptionalLong.empty();
        String heat = "";
        String cold = "";
        String island = "";
        String location = "";
        String visiting = "";
        String date = "";
        String time = "";
        String lobbyCode = "";
        String profileType = "";
        String power = "";
        String cookie = "";
        String skyblockXp = "";
        String mayor = "";
        String minister = "";
        String ministerPerk = "";
        String mayorTime = "";
        boolean readingParty;
        boolean partyLeader;
        final List<String> tunings = new ArrayList<>();
        final List<String> mayorPerks = new ArrayList<>();
        final List<String> party = new ArrayList<>();
        final List<String> objective = new ArrayList<>();
        final List<String> slayer = new ArrayList<>();
        final List<String> unknown = new ArrayList<>();
        final Map<String, PowderPair> powder = new LinkedHashMap<>();
        final Map<ChunkStat, String> chunked = new EnumMap<>(ChunkStat.class);
        final Map<EventKind, List<String>> events = new EnumMap<>(EventKind.class);
    }

    private record Place(
            boolean inRift,
            boolean inGarden,
            boolean inMining,
            boolean inGlacite,
            boolean inCrimson,
            boolean inWinter,
            boolean partyIsland) {
        static Place from(BoardView view, ParsedBoard parsed) {
            String blob = (view.island() + " " + view.locationHint() + " "
                    + parsed.island + " " + parsed.location).toLowerCase(Locale.ROOT);
            boolean rift = blob.contains("rift");
            boolean garden = blob.contains("garden");
            boolean glacite = blob.contains("glacite");
            boolean mining = glacite
                    || blob.contains("dwarven")
                    || blob.contains("hollows")
                    || blob.contains("caverns")
                    || blob.contains("mineshaft");
            boolean crimson = blob.contains("crimson")
                    || blob.contains("nether")
                    || blob.contains("magma");
            boolean winter = blob.contains("jerry")
                    || blob.contains("winter")
                    || blob.contains("glacier");
            boolean party = blob.contains("dungeon")
                    || blob.contains("kuudra")
                    || crimson;
            return new Place(rift, garden, mining, glacite, crimson, winter, party);
        }
    }
}
