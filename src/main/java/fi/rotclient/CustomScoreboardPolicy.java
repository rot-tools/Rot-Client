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
import java.util.OptionalDouble;
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
    private static final Pattern TAB_LABEL = Pattern.compile(
            "(?i)^\\s*(?<key>[a-z][a-z /]+)\\s*:\\s*(?<value>.+)$");
    private static final Pattern TAB_LABEL_FIND = Pattern.compile(
            "(?i)(?<key>[a-z][a-z /]{1,32})\\s*:\\s*(?<value>[^|]+)");

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
        HEALTH("Health"),
        DEFENSE("Defense"),
        MANA("Mana"),
        SPEED("Speed"),
        OVERFLOW("Overflow"),
        VITALITY("Vitality");

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
            chunkedStats = chunkedStats == null
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
            boolean bingo,
            long nowMillis,
            SkyBlockStatBarParser.Stats combat) {
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
            combat = combat == null ? SkyBlockStatBarParser.Stats.empty() : combat;
        }

        public BoardView(
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
                boolean bingo,
                long nowMillis) {
            this(
                    skyblock,
                    hypixelAlpha,
                    title,
                    sidebar,
                    tab,
                    island,
                    locationHint,
                    profileName,
                    profileType,
                    quiverCurrent,
                    quiverMax,
                    bingo,
                    nowMillis,
                    SkyBlockStatBarParser.Stats.empty());
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
                note.put(key, " §7(" + color + sign + formatted + "§7)" + color);
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
                ChunkStat.PURSE,
                ChunkStat.MOTES,
                ChunkStat.BANK,
                ChunkStat.BITS,
                ChunkStat.COPPER,
                ChunkStat.SOWDUST,
                ChunkStat.GEMS,
                ChunkStat.HEAT,
                ChunkStat.COLD,
                ChunkStat.NORTH_STARS);
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

    public static boolean isListTextSetting(String settingId) {
        return "qol.custom_scoreboard.appearance".equals(settingId)
                || "qol.custom_scoreboard.event_priority".equals(settingId)
                || "qol.custom_scoreboard.chunked_stats".equals(settingId);
    }

    public static String listSettingTitle(String settingId) {
        return switch (settingId == null ? "" : settingId) {
            case "qol.custom_scoreboard.appearance" -> "Appearance";
            case "qol.custom_scoreboard.event_priority" -> "Event Priority";
            case "qol.custom_scoreboard.chunked_stats" -> "Chunked Stats";
            default -> "List";
        };
    }

    public static String defaultListText(String settingId) {
        return switch (settingId == null ? "" : settingId) {
            case "qol.custom_scoreboard.appearance" -> defaultAppearanceText();
            case "qol.custom_scoreboard.event_priority" -> defaultEventText();
            case "qol.custom_scoreboard.chunked_stats" -> defaultChunkedText();
            default -> "";
        };
    }

    public static List<String> listChoices(String settingId) {
        List<String> out = new ArrayList<>();
        switch (settingId == null ? "" : settingId) {
            case "qol.custom_scoreboard.appearance" -> {
                for (Slot slot : Slot.values()) {
                    out.add(slot.label());
                }
            }
            case "qol.custom_scoreboard.event_priority" -> {
                for (EventKind event : EventKind.values()) {
                    out.add(event.label());
                }
            }
            case "qol.custom_scoreboard.chunked_stats" -> {
                for (ChunkStat stat : ChunkStat.values()) {
                    out.add(stat.label());
                }
            }
            default -> {
            }
        }
        return List.copyOf(out);
    }

    public static List<String> splitListText(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    public static String joinListText(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(line.trim());
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
            if (stat != null && !out.contains(stat)) {
                out.add(stat);
            }
        }
        return List.copyOf(out);
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
        return switch (needle) {
            case "hp", "health", "hearts" -> ChunkStat.HEALTH;
            case "def", "defense", "defence" -> ChunkStat.DEFENSE;
            case "mana" -> ChunkStat.MANA;
            case "speed", "walkspeed" -> ChunkStat.SPEED;
            case "overflow", "overflowmana" -> ChunkStat.OVERFLOW;
            case "vitality" -> ChunkStat.VITALITY;
            default -> {
                for (ChunkStat stat : ChunkStat.values()) {
                    if (normalizeToken(stat.name()).equals(needle)
                            || normalizeToken(stat.label()).equals(needle)) {
                        yield stat;
                    }
                }
                yield null;
            }
        };
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
        return LegacyMcText.strip(text);
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
                    hideEmpty,
                    place.inRift());
            case MOTES -> moneyLine(
                    "Motes",
                    parsed.motes,
                    "§d",
                    options,
                    deltas,
                    "motes",
                    view.nowMillis(),
                    hideEmpty,
                    !place.inRift());
            case BANK -> bankRows(parsed, options, deltas, view.nowMillis(), hideEmpty);
            case BITS -> bitsLine(view, parsed, options, deltas, place, hideEmpty, hideWrong);
            case COPPER -> moneyLine(
                    "Copper",
                    parsed.copper,
                    "§c",
                    options,
                    deltas,
                    "copper",
                    view.nowMillis(),
                    hideEmpty,
                    !place.inGarden());
            case SOWDUST -> moneyLine(
                    "Sowdust",
                    parsed.sowdust,
                    "§2",
                    options,
                    deltas,
                    "sowdust",
                    view.nowMillis(),
                    hideEmpty,
                    !place.inGarden());
            case GEMS -> moneyLine(
                    "Gems",
                    parsed.gems,
                    "§a",
                    options,
                    deltas,
                    "gems",
                    view.nowMillis(),
                    hideEmpty,
                    false);
            case HEAT -> heatLine(parsed, options, hideEmpty, !place.inHollows());
            case COLD -> coldLine(parsed, options, hideEmpty, !place.inGlacite());
            case NORTH_STARS -> moneyLine(
                    "North Stars",
                    parsed.northStars,
                    "§d",
                    options,
                    deltas,
                    "north",
                    view.nowMillis(),
                    hideEmpty,
                    !place.inWinter());
            case CHUNKED_STATS -> chunkedRows(options, parsed, place, hideEmpty);
            case SOULFLOW -> moneyLine(
                    "Soulflow",
                    parsed.soulflow,
                    "§3",
                    options,
                    deltas,
                    "soulflow",
                    view.nowMillis(),
                    hideEmpty,
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
            case DATE -> singleton(dateLine(parsed, view.nowMillis()), options.textAlign(), hideEmpty);
            case TIME -> singleton(timeLine(parsed, options, view.nowMillis()), options.textAlign(), hideEmpty);
            case LOBBY_CODE -> singleton(
                    lobbyLine(parsed, options),
                    options.textAlign(),
                    hideEmpty && parsed.lobbyCode.isBlank());
            case POWER -> singleton(parsed.power, options.textAlign(), hideEmpty);
            case TUNING -> tuningRows(parsed, options, hideEmpty);
            case COOKIE -> singleton(parsed.cookie, options.textAlign(), hideEmpty);
            case OBJECTIVE -> copyLines(parsed.objective, options.textAlign(), hideEmpty);
            case SLAYER -> copyLines(parsed.slayer, options.textAlign(), hideEmpty);
            case QUIVER -> quiverLine(view, parsed, options, hideEmpty, hideWrong, place.inRift());
            case POWDER -> powderRows(parsed, options, hideEmpty, !place.inMining());
            case SKYBLOCK_XP -> singleton(parsed.skyblockXp, options.textAlign(), hideEmpty);
            case EVENTS -> eventRows(parsed, options, hideEmpty);
            case MAYOR -> mayorRows(parsed, options, hideEmpty, place.inRift());
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
        if (skipWrong) {
            return List.of();
        }
        long value = amount.isPresent() ? amount.getAsLong() : 0L;
        if (skipEmpty && value == 0L) {
            return List.of();
        }
        String note = deltas.suffix(key, value, options.showDiff(), options.numberStyle(), color, now);
        String number = color + formatNumber(value, options.numberStyle()) + note;
        return List.of(new Row(layout(label, number, color, options.numberLayout()), options.textAlign(), false));
    }

    private static List<Row> bitsLine(
            BoardView view,
            ParsedBoard parsed,
            Options options,
            DeltaBook deltas,
            Place place,
            boolean hideEmpty,
            boolean hideWrong) {
        if (place.inDungeon() || place.inKuudra()) {
            return List.of();
        }
        if (hideWrong && view.bingo()) {
            return List.of();
        }
        long bits = parsed.bits.orElse(0L);
        long available = parsed.bitsAvailable.orElse(0L);
        if (hideEmpty && bits == 0L && available == 0L) {
            return List.of();
        }
        String note = deltas.suffix(
                "bits", bits, options.showDiff(), options.numberStyle(), "§b", view.nowMillis());
        String number = "§b" + formatNumber(bits, options.numberStyle());
        if (options.showUnclaimedBits() && parsed.bitsAvailable.isPresent()) {
            number += "§7/§b" + formatNumber(available, options.numberStyle());
        }
        number += note;
        return List.of(new Row(layout("Bits", number, "§b", options.numberLayout()), options.textAlign(), false));
    }

    private static List<Row> bankRows(
            ParsedBoard parsed,
            Options options,
            DeltaBook deltas,
            long now,
            boolean hideEmpty) {
        if (hideEmpty && isZero(parsed.bank) && isZero(parsed.personalBank)) {
            return List.of();
        }
        long coop = parsed.bank.orElse(0L);
        String note = deltas.suffix("bank", coop, options.showDiff(), options.numberStyle(), "§6", now);
        String number = "§6" + formatNumber(coop, options.numberStyle());
        if (parsed.personalBank.isPresent()) {
            number += " §7/ §6" + formatNumber(parsed.personalBank.getAsLong(), options.numberStyle());
        }
        number += note;
        return List.of(new Row(layout("Bank", number, "§6", options.numberLayout()), options.textAlign(), false));
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
            BoardView view,
            ParsedBoard parsed,
            Options options,
            boolean hideEmpty,
            boolean hideWrong,
            boolean inRift) {
        if (inRift) {
            return List.of();
        }
        long current = view.quiverCurrent().orElse(parsed.arrows.orElse(-1L));
        if (current < 0L) {
            if (hideWrong || hideEmpty) {
                return List.of();
            }
            return List.of(new Row("No arrows selected", options.textAlign(), false));
        }
        long max = view.quiverMax().orElse(parsed.arrowMax.orElse(2_880L));
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
        if (parsed.powder.isEmpty()) {
            return hideEmpty ? List.of() : List.of();
        }
        boolean allZero = true;
        for (PowderPair pair : parsed.powder.values()) {
            if (pair.total > 0L || pair.available > 0L) {
                allZero = false;
                break;
            }
        }
        if (hideEmpty && allZero) {
            return List.of();
        }
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("§9§lPowder", options.textAlign(), false));
        parsed.powder.forEach((type, pair) -> {
            long shown = switch (options.powderMode()) {
                case TOTAL -> pair.total;
                case BOTH -> pair.available;
                case AVAILABLE -> pair.available;
            };
            String color = CustomScoreboardLines.powderColor(type);
            String number = color + formatNumber(shown, options.numberStyle());
            if (options.powderMode() == PowderMode.BOTH) {
                number += "§7/" + color + formatNumber(pair.total, options.numberStyle());
            }
            String inner = layout(type, number, color, options.numberLayout());
            rows.add(new Row(" §7- " + inner, options.textAlign(), false));
        });
        return rows;
    }

    private static List<Row> chunkedRows(
            Options options, ParsedBoard parsed, Place place, boolean hideEmpty) {
        List<String> tokens = new ArrayList<>();
        for (ChunkStat stat : options.chunkedStats()) {
            if (!chunkIsland(stat, place)) {
                continue;
            }
            String token = chunkToken(stat, parsed, options, hideEmpty);
            if (token != null && !token.isBlank()) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty()) {
            return List.of();
        }
        List<Row> rows = new ArrayList<>();
        int per = Math.max(1, options.maxStatsPerLine());
        for (int i = 0; i < tokens.size(); i += per) {
            int end = Math.min(tokens.size(), i + per);
            rows.add(new Row(String.join(" §f| ", tokens.subList(i, end)), options.textAlign(), false));
        }
        return rows;
    }

    private static boolean chunkIsland(ChunkStat stat, Place place) {
        return switch (stat) {
            case PURSE -> !place.inRift();
            case MOTES -> place.inRift();
            case COPPER, SOWDUST -> place.inGarden();
            case HEAT -> place.inHollows();
            case COLD -> place.inGlacite();
            case NORTH_STARS -> place.inWinter();
            case BITS -> !place.inDungeon() && !place.inKuudra();
            case HEALTH, DEFENSE, MANA, SPEED, OVERFLOW, VITALITY -> true;
            default -> true;
        };
    }

    private static String chunkToken(
            ChunkStat stat, ParsedBoard parsed, Options options, boolean hideEmpty) {
        return switch (stat) {
            case PURSE -> chunkAmount("§6", parsed.purse, hideEmpty, options);
            case MOTES -> chunkAmount("§d", parsed.motes, hideEmpty, options);
            case BANK -> {
                if (hideEmpty && isZero(parsed.bank) && isZero(parsed.personalBank)) {
                    yield null;
                }
                yield "§6" + (parsed.bank.isPresent()
                        ? formatNumber(parsed.bank.getAsLong(), options.numberStyle())
                        : "0");
            }
            case BITS -> {
                if (hideEmpty && isZero(parsed.bits) && isZero(parsed.bitsAvailable)) {
                    yield null;
                }
                String text = "§b" + formatNumber(parsed.bits.orElse(0L), options.numberStyle());
                if (options.showUnclaimedBits() && parsed.bitsAvailable.isPresent()) {
                    text += "§7/§b" + formatNumber(parsed.bitsAvailable.getAsLong(), options.numberStyle());
                }
                yield text;
            }
            case COPPER -> chunkAmount("§c", parsed.copper, hideEmpty, options);
            case SOWDUST -> chunkAmount("§2", parsed.sowdust, hideEmpty, options);
            case GEMS -> chunkAmount("§a", parsed.gems, hideEmpty, options);
            case HEAT -> {
                if (hideEmpty && (parsed.heat.isBlank() || strip(parsed.heat).endsWith("0"))) {
                    yield null;
                }
                yield parsed.heat.isBlank() ? "§c♨ 0" : parsed.heat;
            }
            case COLD -> {
                if (hideEmpty && parsed.cold.isBlank()) {
                    yield null;
                }
                yield parsed.cold.isBlank() ? "§b0❄" : parsed.cold;
            }
            case NORTH_STARS -> chunkAmount("§d", parsed.northStars, hideEmpty, options);
            case HEALTH -> chunkCombat(
                    "§c", parsed.combat.health(), parsed.combat.maxHealth(), hideEmpty);
            case DEFENSE -> chunkCombat("§a", parsed.combat.defense(), OptionalDouble.empty(), hideEmpty);
            case MANA -> chunkCombat(
                    "§b", parsed.combat.mana(), parsed.combat.maxMana(), hideEmpty);
            case SPEED -> chunkCombat("§f", parsed.combat.speed(), OptionalDouble.empty(), hideEmpty);
            case OVERFLOW -> chunkCombat(
                    "§3", parsed.combat.overflowMana(), OptionalDouble.empty(), hideEmpty);
            case VITALITY -> chunkCombat(
                    "§4", parsed.combat.vitality(), OptionalDouble.empty(), hideEmpty);
        };
    }

    private static String chunkCombat(
            String color,
            OptionalDouble current,
            OptionalDouble max,
            boolean hideEmpty) {
        if (current.isEmpty()) {
            return hideEmpty ? null : color + "0";
        }
        String text = color + SkyBlockStatBarParser.formatStat(current);
        if (max.isPresent()) {
            text += "§7/" + color + SkyBlockStatBarParser.formatStat(max);
        }
        return text;
    }

    private static String chunkAmount(
            String color, OptionalLong amount, boolean hideEmpty, Options options) {
        long value = amount.orElse(0L);
        if (hideEmpty && value == 0L) {
            return null;
        }
        return color + formatNumber(value, options.numberStyle());
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

    private static List<Row> mayorRows(ParsedBoard parsed, Options options, boolean hideEmpty, boolean inRift) {
        if (inRift || parsed.mayor.isBlank()) {
            return hideEmpty ? List.of() : List.of();
        }
        List<Row> rows = new ArrayList<>();
        String mayor = parsed.mayor;
        if (options.showMayorTime() && !parsed.mayorTime.isBlank()) {
            mayor += " §7(§e" + parsed.mayorTime + "§7)";
        }
        rows.add(new Row(mayor, options.textAlign(), false));
        if (options.showMayorPerks()) {
            for (String perk : parsed.mayorPerks) {
                rows.add(new Row(" §7- §e" + strip(perk), options.textAlign(), false));
            }
        }
        if (options.showExtraMayor() && !parsed.minister.isBlank()) {
            rows.add(new Row(parsed.minister, options.textAlign(), false));
            if (!parsed.ministerPerk.isBlank()) {
                rows.add(new Row(" §7- §e" + strip(parsed.ministerPerk), options.textAlign(), false));
            }
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
        if (options.showProfileName()) {
            String name = firstNonBlank(view.profileName(), parsed.profileName);
            if (!name.isBlank()) {
                return symbol + name;
            }
        }
        String type = firstNonBlank(parsed.profileType, view.profileType());
        if (type.isBlank()) {
            return "";
        }
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

    private static String dateLine(ParsedBoard parsed, long nowMillis) {
        if (!parsed.date.isBlank() && looksSkyBlockDate(parsed.date)) {
            return parsed.date;
        }
        return "§7" + SkyBlockClock.formatDate(SkyBlockClock.at(nowMillis));
    }

    private static boolean looksSkyBlockDate(String raw) {
        String plain = strip(raw).toLowerCase(Locale.ROOT);
        return plain.contains("spring")
                || plain.contains("summer")
                || plain.contains("autumn")
                || plain.contains("fall")
                || plain.contains("winter");
    }

    private static String timeLine(ParsedBoard parsed, Options options, long nowMillis) {
        if (options.timeExact() || parsed.time.isBlank()) {
            return SkyBlockClock.formatTime(
                    SkyBlockClock.at(nowMillis), options.time24h(), options.timeExact());
        }
        return formatTime(parsed.time, options);
    }

    private static String formatTime(String raw, Options options) {
        if (raw.isBlank()) {
            return raw;
        }
        String symbol = CustomScoreboardLines.timeSymbol(raw);
        Matcher matcher = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
                .matcher(strip(raw));
        if (!matcher.find() || !options.time24h() && !options.timeExact()) {
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
                : String.format(Locale.ROOT, "%d:%02d%s", hour == 0 ? 12 : hour > 12 ? hour - 12 : hour, minute,
                        ampm == null ? "" : ampm.toLowerCase(Locale.ROOT));
        String out = "§7" + clock;
        if (!symbol.isBlank()) {
            out += " " + symbol;
        }
        return out;
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

    private static List<Row> heatLine(ParsedBoard parsed, Options options, boolean hideEmpty, boolean skipWrong) {
        if (skipWrong) {
            return List.of();
        }
        String raw = parsed.heat;
        if (raw.isBlank()) {
            return hideEmpty ? List.of() : List.of(new Row(layout("Heat", "§c♨ 0", "§c", options.numberLayout()), options.textAlign(), false));
        }
        if (hideEmpty && (strip(raw).endsWith("0") || strip(raw).equals("Heat: 0"))) {
            return List.of();
        }
        return List.of(new Row(raw.contains("Heat") ? raw : layout("Heat", raw, "§c", options.numberLayout()), options.textAlign(), false));
    }

    private static List<Row> coldLine(ParsedBoard parsed, Options options, boolean hideEmpty, boolean skipWrong) {
        if (skipWrong) {
            return List.of();
        }
        String raw = parsed.cold;
        if (raw.isBlank()) {
            return hideEmpty ? List.of() : List.of(new Row(layout("Cold", "0❄", "§b", options.numberLayout()), options.textAlign(), false));
        }
        if (hideEmpty && (strip(raw).endsWith("0") || strip(raw).contains("Cold: 0"))) {
            return List.of();
        }
        return List.of(new Row(raw.contains("Cold") ? raw : layout("Cold", raw, "§b", options.numberLayout()), options.textAlign(), false));
    }

    private static boolean isZero(OptionalLong amount) {
        return amount.isEmpty() || amount.getAsLong() == 0L;
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

    private static List<Row> singleton(String text, Align align, boolean hideIfEmpty) {
        if (text == null || strip(text).isEmpty()) {
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
        parsed.profileName = view.profileName();
        parsed.combat = view.combat();
        List<String> sidebar = view.sidebar();
        boolean[] taken = new boolean[sidebar.size()];
        for (int i = 0; i < sidebar.size(); i++) {
            if (taken[i]) {
                continue;
            }
            String raw = sidebar.get(i);
            CustomScoreboardLines.Hit hit = CustomScoreboardLines.classify(raw);
            applyHit(parsed, raw, hit);
            int extra = hit.extraLines();
            for (int n = 1; n <= extra && i + n < sidebar.size(); n++) {
                String follow = sidebar.get(i + n);
                CustomScoreboardLines.Hit followHit = CustomScoreboardLines.classify(follow);
                if (followHit.kind() != CustomScoreboardLines.Kind.UNKNOWN
                        && followHit.kind() != CustomScoreboardLines.Kind.SKIP
                        && followHit.kind() != hit.kind()
                        && followHit.event() != hit.event()) {
                    break;
                }
                taken[i + n] = true;
                appendFollow(parsed, hit, follow);
            }
        }
        for (String raw : view.tab()) {
            ingestTabLine(raw, parsed);
        }
        return parsed;
    }

    private static void applyHit(ParsedBoard parsed, String raw, CustomScoreboardLines.Hit hit) {
        switch (hit.kind()) {
            case PURSE -> parsed.purse = CustomScoreboardLines.parseAmount(hit.capture());
            case MOTES -> parsed.motes = CustomScoreboardLines.parseAmount(hit.capture());
            case BANK -> parsed.bank = CustomScoreboardLines.parseAmount(hit.capture());
            case BITS -> parsed.bits = CustomScoreboardLines.parseAmount(hit.capture());
            case COPPER -> parsed.copper = CustomScoreboardLines.parseAmount(hit.capture());
            case SOWDUST -> parsed.sowdust = CustomScoreboardLines.parseAmount(hit.capture());
            case GEMS -> parsed.gems = CustomScoreboardLines.parseAmount(hit.capture());
            case NORTH_STARS -> parsed.northStars = CustomScoreboardLines.parseAmount(hit.capture());
            case SOULFLOW -> parsed.soulflow = CustomScoreboardLines.parseAmount(hit.capture());
            case HEAT -> parsed.heat = raw;
            case COLD -> parsed.cold = raw;
            case LOCATION -> parsed.location = raw;
            case DATE -> parsed.date = raw;
            case TIME -> parsed.time = raw;
            case LOBBY -> parsed.lobbyCode = hit.capture().isBlank() ? extractLobby(strip(raw)) : hit.capture();
            case VISITING -> parsed.visiting = raw;
            case PROFILE -> parsed.profileType = strip(raw);
            case POWDER -> {
                String[] parts = hit.capture().split("\\|", 2);
                String type = capitalize(parts[0]);
                PowderPair pair = parsed.powder.computeIfAbsent(type, ignored -> new PowderPair());
                long amount = parts.length > 1 ? CustomScoreboardLines.parseAmount(parts[1]).orElse(0L) : 0L;
                pair.available = amount;
                pair.total = Math.max(pair.total, amount);
            }
            case ARROWS -> {
                parsed.arrows = parseLong(strip(raw));
                Matcher max = Pattern.compile("/\\s*([\\d,]+)").matcher(strip(raw));
                if (max.find()) {
                    parsed.arrowMax = parseLong(max.group(1));
                }
            }
            case OBJECTIVE -> parsed.objective.add(raw);
            case SLAYER -> parsed.slayer.add(raw);
            case EVENT -> {
                if (hit.event() != null) {
                    parsed.events.computeIfAbsent(hit.event(), ignored -> new ArrayList<>()).add(raw);
                }
            }
            case UNKNOWN -> parsed.unknown.add(raw);
            case FOOTER, SKIP -> {
            }
        }
    }

    private static void appendFollow(ParsedBoard parsed, CustomScoreboardLines.Hit hit, String follow) {
        switch (hit.kind()) {
            case OBJECTIVE -> parsed.objective.add(follow);
            case SLAYER -> parsed.slayer.add(follow);
            case EVENT -> {
                if (hit.event() != null) {
                    parsed.events.computeIfAbsent(hit.event(), ignored -> new ArrayList<>()).add(follow);
                }
            }
            default -> {
            }
        }
    }

    private static void ingestTabLine(String raw, ParsedBoard parsed) {
        String plain = SkyBlockStatBarParser.stripFormatting(raw);
        if (plain.isBlank()) {
            parsed.tabSection = "";
            parsed.readingParty = false;
            return;
        }
        String header = tabSection(plain);
        if (header != null) {
            parsed.tabSection = header;
            parsed.readingParty = "party".equals(header);
            return;
        }
        String lowerPlain = plain.toLowerCase(Locale.ROOT);
        if (lowerPlain.startsWith("mayor ") && parsed.mayor.isBlank()) {
            parsed.tabSection = "mayor";
            parsed.mayor = stripMayorPrefix(plain);
            return;
        }
        if (lowerPlain.startsWith("minister ") && parsed.minister.isBlank()) {
            parsed.tabSection = "minister";
            parsed.minister = stripMayorPrefix(plain);
            return;
        }
        if (applyTabKeyValue(plain, parsed)) {
            return;
        }
        applyTabSectionBody(plain, raw, parsed);
    }

    private static String tabSection(String plain) {
        String needle = normalizeToken(plain);
        return switch (needle) {
            case "mayor", "mayors" -> "mayor";
            case "minister" -> "minister";
            case "election", "elections" -> "election";
            case "cookiebuff", "boostercookie", "cookie" -> "cookie";
            case "accessories", "accessorybag", "maxwell", "tunings", "tuning", "magicalpower" -> "maxwell";
            case "party" -> "party";
            case "event", "events", "activeevent", "currentevent" -> "event";
            case "communityshop", "community", "bitsshop", "bitshop", "gemshop" -> "shop";
            case "info", "profile" -> "info";
            default -> null;
        };
    }

    private static boolean applyTabKeyValue(String plain, ParsedBoard parsed) {
        Matcher matcher = TAB_LABEL.matcher(plain);
        if (!matcher.matches()) {
            matcher = TAB_LABEL_FIND.matcher(plain);
            if (!matcher.find()) {
                return false;
            }
        }
        String key = matcher.group("key").trim().toLowerCase(Locale.ROOT);
        String value = matcher.group("value").trim();
        applyTabPair(parsed, key, value);
        return true;
    }

    private static void applyTabSectionBody(String plain, String raw, ParsedBoard parsed) {
        String section = parsed.tabSection == null ? "" : parsed.tabSection;
        switch (section) {
            case "mayor" -> {
                if (parsed.mayor.isBlank()) {
                    parsed.mayor = stripMayorPrefix(plain);
                } else if (parsed.mayorPerks.size() < 6 && looksMayorPerk(plain)) {
                    parsed.mayorPerks.add(plain);
                }
            }
            case "minister" -> {
                if (parsed.minister.isBlank()) {
                    parsed.minister = stripMayorPrefix(plain);
                } else if (parsed.ministerPerk.isBlank()) {
                    parsed.ministerPerk = plain;
                }
            }
            case "election" -> parsed.mayorTime = plain;
            case "cookie" -> parsed.cookie = "§dCookie: §f" + plain;
            case "maxwell" -> applyMaxwellLine(plain, parsed);
            case "party" -> {
                parsed.party.add(plain.replace("★", "").trim());
                if (raw.contains("★") || plain.toLowerCase(Locale.ROOT).contains("leader")) {
                    parsed.partyLeader = true;
                }
            }
            case "event" -> addTabEvent(plain, raw, parsed);
            case "shop" -> {
                OptionalLong amount = parseLong(plain);
                if (amount.isPresent() && parsed.gems.isEmpty()) {
                    parsed.gems = amount;
                }
            }
            default -> {
                if (parsed.readingParty) {
                    if (plain.contains(":")) {
                        parsed.readingParty = false;
                    } else {
                        parsed.party.add(plain.replace("★", "").trim());
                        if (raw.contains("★") || plain.toLowerCase(Locale.ROOT).contains("leader")) {
                            parsed.partyLeader = true;
                        }
                    }
                } else {
                    // Ignore leftover tab names so player list rows do not become events.
                }
            }
        }
    }

    private static String stripMayorPrefix(String plain) {
        String trimmed = plain.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("mayor ")) {
            return trimmed.substring(6).trim();
        }
        if (lower.startsWith("minister ")) {
            return trimmed.substring(9).trim();
        }
        return trimmed;
    }

    private static boolean looksMayorPerk(String plain) {
        if (plain == null || plain.isBlank() || plain.length() > 48) {
            return false;
        }
        String lower = plain.toLowerCase(Locale.ROOT);
        if (lower.contains("[") || lower.contains("]") || lower.contains("●")) {
            return false;
        }
        if (lower.contains("xp")
                || lower.contains("buff")
                || lower.contains("perk")
                || lower.contains("bonus")
                || lower.contains("discount")
                || lower.contains("interest")
                || lower.contains("stat")
                || lower.contains("coin")
                || lower.contains("pet")
                || lower.contains("event")
                || lower.contains("speed")
                || lower.contains("fiesta")) {
            return true;
        }
        return plain.contains(" ") && Character.isUpperCase(plain.trim().charAt(0));
    }

    private static void applyMaxwellLine(String plain, ParsedBoard parsed) {
        OptionalLong amount = parseLong(plain);
        if (parsed.magicalPower.isEmpty() && amount.isPresent() && !looksTuning(plain)) {
            parsed.magicalPower = amount;
            return;
        }
        if (looksTuning(plain) || amount.isPresent()) {
            parsed.tunings.add(plain);
        }
    }

    private static boolean looksTuning(String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        return lower.contains("+")
                || lower.contains("health")
                || lower.contains("defense")
                || lower.contains("strength")
                || lower.contains("crit")
                || lower.contains("intelligence")
                || lower.contains("speed")
                || lower.contains("vitality")
                || lower.contains("magic find")
                || lower.contains("ferocity");
    }

    private static void addTabEvent(String plain, String raw, ParsedBoard parsed) {
        EventKind event = CustomScoreboardLines.eventKind(raw, plain);
        if (event == null) {
            event = CustomScoreboardLines.eventKind(plain, plain);
        }
        if (event == null && "event".equals(parsed.tabSection)) {
            event = EventKind.ACTIVE_TABLIST;
        }
        if (event != null) {
            String text = raw.isBlank() ? plain : raw;
            List<String> lines = parsed.events.computeIfAbsent(event, ignored -> new ArrayList<>());
            if (!lines.contains(text)) {
                lines.add(text);
            }
        }
    }

    private static void applyTabPair(ParsedBoard parsed, String key, String value) {
        OptionalLong amount = parseLong(value);
        String needle = key.toLowerCase(Locale.ROOT);
        if (bitsAvailableKey(needle) && amount.isPresent()) {
            parsed.bitsAvailable = amount;
            return;
        }
        switch (needle) {
            case "area", "island" -> parsed.island = value;
            case "profile" -> {
                String lower = value.toLowerCase(Locale.ROOT);
                if (lower.contains("ironman") || lower.contains("stranded") || lower.contains("bingo")) {
                    parsed.profileType = value;
                } else {
                    parsed.profileName = value;
                }
            }
            case "bank" -> parsed.bank = amount;
            case "purse" -> parsed.purse = amount;
            case "bits" -> parsed.bits = amount;
            case "soulflow" -> parsed.soulflow = amount;
            case "gems" -> parsed.gems = amount;
            case "copper" -> parsed.copper = amount;
            case "motes" -> parsed.motes = amount;
            case "sowdust" -> parsed.sowdust = amount;
            case "north stars", "northstars" -> parsed.northStars = amount;
            case "cookie buff", "cookie", "booster cookie" -> {
                parsed.cookie = "§dCookie: §f" + value;
                parsed.tabSection = "cookie";
            }
            case "sb level", "skyblock xp", "skyblock level" ->
                    parsed.skyblockXp = "§bSB XP: §f" + value;
            case "magical power", "mp" -> {
                parsed.magicalPower = amount;
                parsed.tabSection = "maxwell";
            }
            case "mayor" -> {
                parsed.mayor = value;
                parsed.tabSection = "mayor";
            }
            case "minister" -> {
                parsed.minister = value;
                parsed.tabSection = "minister";
            }
            case "election", "next mayor", "election over in", "over in" -> {
                parsed.mayorTime = value;
                parsed.tabSection = "election";
            }
            case "event", "active event", "current event" -> {
                addTabEvent(value, value, parsed);
                parsed.tabSection = "event";
            }
            case "ends in", "starts in" -> {
                if ("election".equals(parsed.tabSection)) {
                    parsed.mayorTime = value;
                } else {
                    addTabEvent(key + ": " + value, key + ": " + value, parsed);
                }
            }
            case "players" -> {
                parsed.players = amount;
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
                    applyChunkMoney(parsed, stat, value);
                    if ("maxwell".equals(parsed.tabSection) && looksTuning(key + " " + value)) {
                        String tuning = key + ": " + value;
                        if (!parsed.tunings.contains(tuning)) {
                            parsed.tunings.add(tuning);
                        }
                    } else {
                        parsed.combat = mergeCombat(parsed.combat, stat, value);
                    }
                } else if (needle.contains("tuning")) {
                    parsed.tunings.add(value);
                    parsed.tabSection = "maxwell";
                } else if (needle.contains("perk") && !parsed.mayor.isBlank()) {
                    parsed.mayorPerks.add(value);
                } else if (needle.contains("minister") && needle.contains("perk")) {
                    parsed.ministerPerk = value;
                } else if ((needle.contains("election") || needle.contains("mayor"))
                        && (needle.contains("time") || needle.contains("over"))) {
                    parsed.mayorTime = value;
                    parsed.tabSection = "election";
                } else if ("shop".equals(parsed.tabSection) && needle.contains("unclaimed") && amount.isPresent()) {
                    parsed.bitsAvailable = amount;
                } else if ("maxwell".equals(parsed.tabSection) && looksTuning(key + " " + value)) {
                    String tuning = key + ": " + value;
                    if (!parsed.tunings.contains(tuning)) {
                        parsed.tunings.add(tuning);
                    }
                }
            }
        }
    }

    private static boolean bitsAvailableKey(String key) {
        String needle = key.toLowerCase(Locale.ROOT);
        if (needle.contains("chest") || needle.contains("reward")) {
            return false;
        }
        return needle.contains("unclaimed bits")
                || needle.contains("bits available")
                || needle.contains("available bits")
                || (needle.contains("unclaimed") && needle.contains("bit"))
                || needle.equals("unclaimed")
                || needle.equals("uncollected");
    }

    private static void applyChunkMoney(ParsedBoard parsed, ChunkStat chunk, String raw) {
        OptionalLong amount = parseLong(raw);
        if (amount.isEmpty()) {
            return;
        }
        switch (chunk) {
            case PURSE -> parsed.purse = amount;
            case BITS -> parsed.bits = amount;
            case MOTES -> parsed.motes = amount;
            case COPPER -> parsed.copper = amount;
            case GEMS -> parsed.gems = amount;
            case NORTH_STARS -> parsed.northStars = amount;
            case BANK -> parsed.bank = amount;
            default -> {
            }
        }
    }

    private static SkyBlockStatBarParser.Stats mergeCombat(
            SkyBlockStatBarParser.Stats previous, ChunkStat stat, String raw) {
        SkyBlockStatBarParser.Stats current = previous == null
                ? SkyBlockStatBarParser.Stats.empty()
                : previous;
        OptionalDouble amount = optionalAmount(raw);
        if (amount.isEmpty()) {
            return current;
        }
        OptionalDouble max = optionalMax(raw);
        return switch (stat) {
            case HEALTH -> new SkyBlockStatBarParser.Stats(
                    amount,
                    max.isPresent() ? max : current.maxHealth(),
                    current.defense(),
                    current.mana(),
                    current.maxMana(),
                    current.overflowMana(),
                    current.speed(),
                    current.vitality());
            case DEFENSE -> new SkyBlockStatBarParser.Stats(
                    current.health(),
                    current.maxHealth(),
                    amount,
                    current.mana(),
                    current.maxMana(),
                    current.overflowMana(),
                    current.speed(),
                    current.vitality());
            case MANA -> new SkyBlockStatBarParser.Stats(
                    current.health(),
                    current.maxHealth(),
                    current.defense(),
                    amount,
                    max.isPresent() ? max : current.maxMana(),
                    current.overflowMana(),
                    current.speed(),
                    current.vitality());
            case SPEED -> new SkyBlockStatBarParser.Stats(
                    current.health(),
                    current.maxHealth(),
                    current.defense(),
                    current.mana(),
                    current.maxMana(),
                    current.overflowMana(),
                    amount,
                    current.vitality());
            case OVERFLOW -> new SkyBlockStatBarParser.Stats(
                    current.health(),
                    current.maxHealth(),
                    current.defense(),
                    current.mana(),
                    current.maxMana(),
                    amount,
                    current.speed(),
                    current.vitality());
            case VITALITY -> new SkyBlockStatBarParser.Stats(
                    current.health(),
                    current.maxHealth(),
                    current.defense(),
                    current.mana(),
                    current.maxMana(),
                    current.overflowMana(),
                    current.speed(),
                    amount);
            default -> current;
        };
    }

    private static OptionalDouble optionalAmount(String raw) {
        OptionalLong parsed = parseLong(raw);
        return parsed.isPresent() ? OptionalDouble.of(parsed.getAsLong()) : OptionalDouble.empty();
    }

    private static OptionalDouble optionalMax(String raw) {
        Matcher matcher = Pattern.compile("/\\s*([\\d,]+(?:\\.\\d+)?)").matcher(strip(raw));
        if (!matcher.find()) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(matcher.group(1).replace(",", "")));
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    private static String extractLobby(String plain) {
        Matcher matcher = Pattern.compile("(?i)\\b((?:m|mini|mega)[0-9A-Z]+)\\b").matcher(plain);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return plain;
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
        SkyBlockStatBarParser.Stats combat = SkyBlockStatBarParser.Stats.empty();
        String heat = "";
        String cold = "";
        String island = "";
        String location = "";
        String visiting = "";
        String date = "";
        String time = "";
        String lobbyCode = "";
        String profileType = "";
        String profileName = "";
        String power = "";
        String cookie = "";
        String skyblockXp = "";
        String mayor = "";
        String minister = "";
        String ministerPerk = "";
        String mayorTime = "";
        boolean readingParty;
        boolean partyLeader;
        String tabSection = "";
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
            boolean inHollows,
            boolean inWinter,
            boolean inDungeon,
            boolean inKuudra,
            boolean partyIsland) {
        static Place from(BoardView view, ParsedBoard parsed) {
            String blob = (view.island() + " " + view.locationHint() + " "
                    + parsed.island + " " + parsed.location).toLowerCase(Locale.ROOT);
            boolean rift = blob.contains("rift");
            boolean garden = blob.contains("garden");
            boolean glacite = blob.contains("glacite");
            boolean hollows = blob.contains("hollows") || blob.contains("nucleus");
            boolean mining = glacite
                    || hollows
                    || blob.contains("dwarven")
                    || blob.contains("caverns")
                    || blob.contains("mineshaft")
                    || blob.contains("base camp");
            boolean winter = blob.contains("jerry")
                    || blob.contains("winter")
                    || blob.contains("glacier");
            boolean dungeon = blob.contains("dungeon") || blob.contains("catacomb");
            boolean kuudra = blob.contains("kuudra");
            boolean party = dungeon || kuudra || blob.contains("crimson");
            return new Place(rift, garden, mining, glacite, hollows, winter, dungeon, kuudra, party);
        }
    }
}
