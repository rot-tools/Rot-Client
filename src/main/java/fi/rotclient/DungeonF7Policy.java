package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Floor 7 Click GUI helpers: Maxor crystals, I Hate Diorite
 * pillars, Simon Says buttons, Wither ESP names, Melody alerts, overlay
 * colors and tick/title filters. Minecraft-free for unit tests.
 */
public final class DungeonF7Policy {
    public enum WitherBoss {
        NONE,
        MAXOR,
        STORM,
        GOLDOR,
        NECRON
    }

    public record Pillar(EmberDungeonPolicy.IntVec origin, EmberDungeonPolicy.GlassTint tint) {
    }

    public static final long MAXOR_START_MILLIS = 8_350L;
    public static final long STORM_START_MILLIS = 6_000L;
    /** Maxor crystals respawn 34 ticks after the beam / YOU TRICKED ME line. */
    public static final int CRYSTAL_RESPAWN_TICKS = 34;
    public static final long CRYSTAL_RESPAWN_MILLIS = CRYSTAL_RESPAWN_TICKS * 50L;
    public static final long RELIC_SPAWN_MILLIS = 42_000L;
    public static final int MIN_RELIC_SPAWN_TICKS = 1;
    public static final int MAX_RELIC_SPAWN_TICKS = 1200;
    public static final int DEFAULT_RELIC_SPAWN_TICKS = 840;
    public static final List<String> DRAGON_SOLO_CLASSES = List.of("Tank", "Healer");
    public static final List<String> DRAGON_KILL_ORDER = List.of("Power", "Flame", "Apex", "Ice", "Soul");
    public static final List<String> DRAGON_KILL_ORDER_PAUL = List.of("Ice", "Soul", "Power", "Flame", "Apex");
    private static final Pattern DRAGON_NAME = Pattern.compile("(?i)\\b(power|flame|apex|ice|soul)\\b");
    private static final Pattern DRAGON_KILL = Pattern.compile("(?i)(slain|killed|destroyed|down)");

    private static final Pattern CRYSTAL_PICKUP = Pattern.compile(
            "^(\\w{3,16}) picked up an Energy Crystal!$");
    private static final Pattern CRYSTAL_SPAWN = Pattern.compile(
            "^\\[BOSS] Maxor: (?:THAT BEAM! IT HURTS! IT HURTS!!|YOU TRICKED ME!)$");
    private static final Pattern DRAGON_SPRAY = Pattern.compile("(?i)ice spray");
    private static final Pattern DRAGON_ARROWS = Pattern.compile("(?i)arrows? hit");
    private static final Pattern MELODY_PARTY = Pattern.compile(
            "(?i)^Party > (?:\\[[^\\]]+]\\s*)?(\\w{3,16})\\s*[:\\>].*(?:melody|\\d+/\\d+|\\d{1,3}%)");

    private DungeonF7Policy() {
    }

    public static boolean crystalPickup(String chat) {
        return CRYSTAL_PICKUP.matcher(DungeonPolicy.normalize(chat)).matches();
    }

    public static boolean crystalSpawnChat(String chat) {
        return CRYSTAL_SPAWN.matcher(DungeonPolicy.normalize(chat)).matches();
    }

    public static boolean holdingEnergyCrystal(String itemName) {
        String text = DungeonPolicy.normalize(itemName).toLowerCase(Locale.ROOT);
        return text.contains("energy crystal");
    }

    public static boolean melodyTerminalTitle(String title) {
        return DungeonPolicy.detectTerminal(title) == DungeonPolicy.Terminal.MELODY;
    }

    public static boolean melodyAlertChat(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        if (!text.contains("melody")) {
            return false;
        }
        return text.contains("terminal") || text.startsWith("party >");
    }

    public static WitherBoss witherBoss(String name) {
        String text = DungeonPolicy.normalize(name).toLowerCase(Locale.ROOT);
        if (text.contains("maxor")) {
            return WitherBoss.MAXOR;
        }
        if (text.contains("storm")) {
            return WitherBoss.STORM;
        }
        if (text.contains("goldor")) {
            return WitherBoss.GOLDOR;
        }
        if (text.contains("necron")) {
            return WitherBoss.NECRON;
        }
        return WitherBoss.NONE;
    }

    public static int witherColor(
            WitherBoss boss, int maxor, int storm, int goldor, int necron, int fallback) {
        return switch (boss) {
            case MAXOR -> maxor;
            case STORM -> storm;
            case GOLDOR -> goldor;
            case NECRON -> necron;
            case NONE -> fallback;
        };
    }

    public static boolean timerAllowed(
            DungeonAssistPolicy.F7Timer timer,
            boolean pad,
            boolean lightning,
            boolean py,
            boolean goldor,
            boolean necron,
            boolean maxorStart,
            boolean stormStart) {
        return switch (timer) {
            case MAXOR_START -> maxorStart;
            case STORM_START -> stormStart;
            case STORM_PAD -> pad;
            case STORM_LIGHTNING -> lightning;
            case STORM_PY -> py;
            case GOLDOR, CORE -> goldor;
            case NECRON -> necron;
            case NONE -> false;
        };
    }

    public static boolean titleAllowed(
            DungeonAssistPolicy.F7Title title,
            boolean crystal,
            boolean wither,
            boolean terminal,
            boolean gate) {
        return switch (title) {
            case CRYSTAL -> crystal;
            case ENRAGED -> wither;
            case TERMINAL -> terminal;
            case GATE -> gate;
            case NONE -> false;
        };
    }

    public static List<Pillar> dioritePillars() {
        return List.of(
                new Pillar(new EmberDungeonPolicy.IntVec(46, 169, 41), EmberDungeonPolicy.GlassTint.LIME),
                new Pillar(new EmberDungeonPolicy.IntVec(46, 169, 65), EmberDungeonPolicy.GlassTint.YELLOW),
                new Pillar(new EmberDungeonPolicy.IntVec(100, 169, 65), EmberDungeonPolicy.GlassTint.PURPLE),
                new Pillar(new EmberDungeonPolicy.IntVec(100, 169, 41), EmberDungeonPolicy.GlassTint.RED));
    }

    public static Optional<EmberDungeonPolicy.GlassTint> dioriteTint(int x, int y, int z) {
        for (Pillar pillar : dioritePillars()) {
            EmberDungeonPolicy.IntVec origin = pillar.origin();
            if (x >= origin.x() - 3 && x <= origin.x() + 3
                    && y >= origin.y() && y <= origin.y() + 37
                    && z >= origin.z() - 3 && z <= origin.z() + 3) {
                return Optional.of(pillar.tint());
            }
        }
        return Optional.empty();
    }

    public static List<EmberDungeonPolicy.IntVec> simonButtons() {
        List<EmberDungeonPolicy.IntVec> buttons = new ArrayList<>();
        for (int y = 120; y <= 123; y++) {
            for (int z = 92; z <= 95; z++) {
                buttons.add(new EmberDungeonPolicy.IntVec(110, y, z));
            }
        }
        return List.copyOf(buttons);
    }

    public record SimonState(
            List<EmberDungeonPolicy.IntVec> order,
            List<EmberDungeonPolicy.IntVec> remaining,
            Set<EmberDungeonPolicy.IntVec> prevLit,
            boolean showing) {
        public SimonState {
            order = order == null ? List.of() : List.copyOf(order);
            remaining = remaining == null ? List.of() : List.copyOf(remaining);
            prevLit = prevLit == null ? Set.of() : Set.copyOf(prevLit);
        }

        public static SimonState idle() {
            return new SimonState(List.of(), List.of(), Set.of(), false);
        }

        public EmberDungeonPolicy.IntVec nextButton() {
            return remaining.isEmpty() || showing ? null : remaining.getFirst();
        }
    }

    /**
     * Sea-lantern sequence lights the obsidian wall one block east of the
     * clickable buttons (x=111), not the stone buttons at x=110.
     */
    public static List<EmberDungeonPolicy.IntVec> simonLanterns() {
        List<EmberDungeonPolicy.IntVec> lanterns = new ArrayList<>();
        for (int y = 120; y <= 123; y++) {
            for (int z = 92; z <= 95; z++) {
                lanterns.add(new EmberDungeonPolicy.IntVec(111, y, z));
            }
        }
        return List.copyOf(lanterns);
    }

    public static boolean isSimonButton(int x, int y, int z) {
        return x == 110 && y >= 120 && y <= 123 && z >= 92 && z <= 95;
    }

    public static boolean isSimonLantern(int x, int y, int z) {
        return x == 111 && y >= 120 && y <= 123 && z >= 92 && z <= 95;
    }

    public static EmberDungeonPolicy.IntVec simonButtonForLantern(int x, int y, int z) {
        if (!isSimonLantern(x, y, z)) {
            return null;
        }
        return new EmberDungeonPolicy.IntVec(110, y, z);
    }

    public static boolean isSimonSequenceLit(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("sea_lantern") || id.contains("lamp");
    }

    public static EmberDungeonPolicy.IntVec simonStart() {
        return new EmberDungeonPolicy.IntVec(110, 121, 91);
    }

    /**
     * Record the ordered sea-lantern pattern. Each round replays the full
     * sequence; rising edges append, and lights going out copies the round
     * into remaining for playback.
     */
    public static SimonState observeSimon(
            SimonState current, List<EmberDungeonPolicy.IntVec> litNow) {
        SimonState state = current == null ? SimonState.idle() : current;
        List<EmberDungeonPolicy.IntVec> uniqueLit = new ArrayList<>();
        Set<EmberDungeonPolicy.IntVec> nowLit = new HashSet<>();
        if (litNow != null) {
            for (EmberDungeonPolicy.IntVec button : litNow) {
                if (button != null && nowLit.add(button)) {
                    uniqueLit.add(button);
                }
            }
        }
        boolean showing = !uniqueLit.isEmpty();
        List<EmberDungeonPolicy.IntVec> order = new ArrayList<>();
        if (showing && !state.showing()) {
            order.clear();
        } else {
            order.addAll(state.order());
        }
        if (showing) {
            for (EmberDungeonPolicy.IntVec button : uniqueLit) {
                if (!state.prevLit().contains(button)) {
                    order.add(button);
                }
            }
        }
        List<EmberDungeonPolicy.IntVec> remaining;
        if (showing || state.showing()) {
            remaining = List.copyOf(order);
        } else {
            remaining = List.copyOf(state.remaining());
        }
        return new SimonState(order, remaining, nowLit, showing);
    }

    public static SimonState consumeNext(SimonState current) {
        SimonState state = current == null ? SimonState.idle() : current;
        EmberDungeonPolicy.IntVec next = state.nextButton();
        if (next == null) {
            return state;
        }
        List<EmberDungeonPolicy.IntVec> remaining = new ArrayList<>(state.remaining());
        remaining.removeFirst();
        return new SimonState(state.order(), remaining, state.prevLit(), state.showing());
    }

    public static int simonColor(int remainingIndex, int first, int second, int other) {
        if (remainingIndex <= 0) {
            return first;
        }
        if (remainingIndex == 1) {
            return second;
        }
        return other;
    }

    public static int numbersOverlayColor(int order, int first, int second, int third, int generic) {
        if (order == 0) {
            return first;
        }
        if (order == 1) {
            return second;
        }
        if (order == 2) {
            return third;
        }
        return generic;
    }

    public static int rubixOverlayColor(int button, int positive, int negative, int generic) {
        if (button == 0) {
            return positive;
        }
        if (button == 1) {
            return negative;
        }
        return generic;
    }

    public static int melodyOverlayColor(
            DungeonPolicy.MelodyState melody,
            int slot,
            int column,
            int indicator,
            int wrong,
            int generic) {
        if (melody == null || slot < 0) {
            return 0;
        }
        if (melody.readyToClick() && slot == melody.clickSlot()) {
            return indicator;
        }
        if (melody.correct() != null) {
            int col = melody.correct() + 1;
            int row = slot / 9;
            int slotCol = slot % 9;
            if (slotCol == col && row >= 1 && row <= DungeonPolicy.LEGACY_MELODY_PLAY_ROWS) {
                return column;
            }
        }
        if (melody.current() != null && slot % 9 == melody.current() + 1) {
            return wrong;
        }
        return generic == 0 ? 0 : 0;
    }

    public static boolean overlayTypeEnabled(
            DungeonPolicy.Terminal terminal,
            boolean melody,
            boolean numbers,
            boolean colors,
            boolean rubix,
            boolean panes,
            boolean starts) {
        return DungeonPolicy.shouldAutoSolve(terminal, melody, numbers, colors, rubix, panes, starts);
    }

    public static boolean dragonSprayChat(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        return text.contains("ice sprayed") || DRAGON_SPRAY.matcher(text).find() && text.contains("dragon");
    }

    public static boolean dragonArrowChat(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        return DRAGON_ARROWS.matcher(text).find() && (text.contains("dragon") || text.contains("hit"));
    }

    public static Optional<Integer> arrowClicksAt(int x, int y, int z, List<EmberDungeonPolicy.ArrowClicks> remaining) {
        int index = EmberDungeonPolicy.arrowIndex(x, y, z);
        if (index < 0 || remaining == null) {
            return Optional.empty();
        }
        for (EmberDungeonPolicy.ArrowClicks click : remaining) {
            if (click.index() == index) {
                return Optional.of(click.clicks());
            }
        }
        return Optional.empty();
    }

    public static boolean blockWrongArrow(
            int x,
            int y,
            int z,
            boolean enabled,
            boolean sneaking,
            boolean invertSneak,
            List<EmberDungeonPolicy.ArrowClicks> remaining) {
        if (!enabled) {
            return false;
        }
        if (sneaking != invertSneak) {
            return false;
        }
        if (EmberDungeonPolicy.arrowIndex(x, y, z) < 0) {
            return false;
        }
        return arrowClicksAt(x, y, z, remaining).isEmpty();
    }

    public static boolean blockWrongSimon(
            int x,
            int y,
            int z,
            boolean enabled,
            boolean sneaking,
            EmberDungeonPolicy.IntVec next) {
        if (!enabled || sneaking || next == null) {
            return false;
        }
        if (!isSimonButton(x, y, z) && !(x == 110 && y == 121 && z == 91)) {
            return false;
        }
        return next.x() != x || next.y() != y || next.z() != z;
    }

    public static final String STORM_DEATH =
            "[BOSS] Storm: I should have known that I stood no chance.";
    public static final int I4_ROD_TICK = 174;
    public static final int I4_MASK_TICK = 244;
    public static final int I4_LEAP_TICK = 307;
    public static final int MIN_RELIC_LOOK_MS = 10;
    public static final int MAX_RELIC_LOOK_MS = 300;
    public static final int MIN_I4_ROTATION_MS = 0;
    public static final int MAX_I4_ROTATION_MS = 250;

    public record LookAim(float yaw, float pitch) {
    }

    public record DragonPad(String name, EmberDungeonPolicy.Aabb box, int color) {
    }

    public static boolean stormDeath(String chat) {
        return DungeonPolicy.normalize(chat).equals(STORM_DEATH);
    }

    public static Optional<String> melodyPlayer(String chat) {
        String text = DungeonPolicy.normalize(chat);
        if (!text.regionMatches(true, 0, "Party > ", 0, 8)) {
            return Optional.empty();
        }
        Matcher match = MELODY_PARTY.matcher(text);
        if (!match.find()) {
            return Optional.empty();
        }
        return Optional.of(match.group(1));
    }

    public static boolean holdingRelic(String itemName, String color) {
        String text = DungeonPolicy.normalize(itemName).toLowerCase(Locale.ROOT);
        String key = color == null ? "" : color.trim().toLowerCase(Locale.ROOT);
        return text.contains("relic") && (key.isEmpty() || text.contains(key));
    }

    public static boolean holdingRelicOrMenu(String itemName) {
        String text = DungeonPolicy.normalize(itemName).toLowerCase(Locale.ROOT);
        return text.contains("relic") || text.contains("skyblock menu");
    }

    public static int clampRelicLookMs(int ms) {
        return Math.max(MIN_RELIC_LOOK_MS, Math.min(MAX_RELIC_LOOK_MS, ms));
    }

    public static int clampI4RotationMs(int ms) {
        return Math.max(MIN_I4_ROTATION_MS, Math.min(MAX_I4_ROTATION_MS, ms));
    }

    public static LookAim aimAt(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dz = toZ - fromZ;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, horiz));
        return new LookAim(yaw, pitch);
    }

    public static LookAim lerpLook(LookAim from, LookAim to, double progress) {
        double t = Math.max(0.0D, Math.min(1.0D, progress));
        float eased = (float) (t * t * (3.0D - 2.0D * t));
        float yaw = from.yaw() + shortestYaw(from.yaw(), to.yaw()) * eased;
        float pitch = from.pitch() + (to.pitch() - from.pitch()) * eased;
        return new LookAim(yaw, pitch);
    }

    public static boolean i4Action(int tick, int expected) {
        return tick == expected;
    }

    public static List<DragonPad> dragonPads() {
        return List.of(
                new DragonPad("Power", new EmberDungeonPolicy.Aabb(14.5, 13, 45.5, 39.5, 28, 70.5), 0x66FF5555),
                new DragonPad("Flame", new EmberDungeonPolicy.Aabb(72, 8, 47, 102, 28, 77), 0x66FFAA00),
                new DragonPad("Apex", new EmberDungeonPolicy.Aabb(7, 8, 80, 37, 28, 110), 0x6655FF55),
                new DragonPad("Ice", new EmberDungeonPolicy.Aabb(71.5, 16, 82.5, 96.5, 26, 107.5), 0x6655FFFF),
                new DragonPad("Soul", new EmberDungeonPolicy.Aabb(45.5, 13, 113.5, 68.5, 23, 136.5), 0x66AA00AA));
    }

    public static String dragonHealthLine(String name, float health) {
        String label = name == null || name.isBlank() ? "Dragon" : name;
        if (health >= 1_000_000F) {
            return label + " " + String.format(java.util.Locale.ROOT, "%.1fM", health / 1_000_000F);
        }
        if (health >= 1_000F) {
            return label + " " + String.format(java.util.Locale.ROOT, "%.0fK", health / 1_000F);
        }
        return label + " " + String.format(java.util.Locale.ROOT, "%.0f", health);
    }

    public static DungeonPolicy.DungeonClass leapClass(String name) {
        return DungeonPolicy.dungeonClass(DungeonPolicy.normalizeI4LeapClass(name));
    }

    public static final List<String> CLOSE_CHEST_MODES = List.of("Auto", "Any Key");
    public static final int MIN_TERM_PROTECT_MS = 100;
    public static final int MAX_TERM_PROTECT_MS = 700;
    public static final int DEFAULT_TERM_PROTECT_MS = 400;
    private static final Pattern P3_PROGRESS = Pattern.compile(
            "^(.{1,16}) (?:activated|completed) a (terminal|lever|device)! \\((\\d+)/(\\d+)\\)$");

    public record P3Progress(String player, String kind, int current, int total) {
    }

    public static Optional<P3Progress> p3Progress(String chat) {
        Matcher match = P3_PROGRESS.matcher(DungeonPolicy.normalize(chat));
        if (!match.matches()) {
            return Optional.empty();
        }
        return Optional.of(new P3Progress(
                match.group(1),
                match.group(2).toLowerCase(Locale.ROOT),
                Integer.parseInt(match.group(3)),
                Integer.parseInt(match.group(4))));
    }

    public static boolean p3GateDestroyed(String chat) {
        return DungeonPolicy.normalize(chat).equals("The gate has been destroyed!");
    }

    public static final int GOLDOR_FRENZY_TICKS = 60;
    public static final int PURPLE_PAD_TICKS = 20;
    public static final long DRAGON_SPAWN_MILLIS = 5_000L;

    public static boolean dragonSpawnChat(String chat) {
        String text = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
        return text.contains("wither king")
                && (text.contains("summon") || text.contains("dragon"));
    }

    public static String p3HudLine(int terminals, int devices, int levers) {
        return "P3  T " + terminals + "/7  D " + devices + "/7  L " + levers + "/7";
    }

    public static int melodySlotForDigit(int digit) {
        return melodySlotForDigit(digit, DungeonPolicy.DEFAULT_MELODY_PLAY_ROWS);
    }

    public static int melodySlotForDigit(int digit, int playRows) {
        int rows = DungeonPolicy.clampMelodyPlayRows(playRows);
        if (digit < 1 || digit > rows) {
            return -1;
        }
        return (digit - 1) * 9 + 16;
    }

    public static boolean protectTerminal(long openedAtMillis, long nowMillis, int thresholdMs) {
        if (openedAtMillis <= 0L) {
            return false;
        }
        int threshold = clampTermProtectMs(thresholdMs);
        return nowMillis - openedAtMillis < threshold;
    }

    public static int clampTermProtectMs(int ms) {
        return Math.max(MIN_TERM_PROTECT_MS, Math.min(MAX_TERM_PROTECT_MS, ms));
    }

    public static String normalizeCloseChestMode(String value) {
        if (value != null && value.trim().equalsIgnoreCase("Any Key")) {
            return "Any Key";
        }
        return "Auto";
    }

    public static Optional<EmberDungeonPolicy.IntVec> nextLitI4(List<EmberDungeonPolicy.IntVec> remaining) {
        if (remaining == null || remaining.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(remaining.getFirst());
    }

    public static String dragonSpawnLine(long deadlineMillis, long nowMillis) {
        return formatCountdown("Dragon spawn", Math.max(0L, deadlineMillis - nowMillis), false, true, true);
    }

    public static int clampRelicSpawnTicks(int ticks) {
        return Math.max(MIN_RELIC_SPAWN_TICKS, Math.min(MAX_RELIC_SPAWN_TICKS, ticks));
    }

    public static long relicSpawnMillis(int ticks) {
        return clampRelicSpawnTicks(ticks) * 50L;
    }

    public static String normalizeSoloClass(String value) {
        if (value != null) {
            for (String option : DRAGON_SOLO_CLASSES) {
                if (option.equalsIgnoreCase(value.trim())) {
                    return option;
                }
            }
        }
        return "Tank";
    }

    public static List<String> dragonFocusOrder(boolean paul) {
        return paul ? DRAGON_KILL_ORDER_PAUL : DRAGON_KILL_ORDER;
    }

    public static List<String> remainingDragons(boolean paul, Set<String> down) {
        List<String> left = new ArrayList<>();
        for (String name : dragonFocusOrder(paul)) {
            if (down == null || !down.contains(name.toLowerCase(Locale.ROOT))) {
                left.add(name);
            }
        }
        return List.copyOf(left);
    }

    public static String dragonPriorityLine(boolean paul, Set<String> down, String soloClass) {
        List<String> left = remainingDragons(paul, down);
        String solo = normalizeSoloClass(soloClass);
        if (left.isEmpty()) {
            return "Dragons down  Solo " + solo;
        }
        return "Dragons " + String.join(" > ", left) + "  Solo " + solo;
    }

    public static Optional<String> dragonPadName(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher match = DRAGON_NAME.matcher(DungeonPolicy.normalize(text));
        if (!match.find()) {
            return Optional.empty();
        }
        String raw = match.group(1).toLowerCase(Locale.ROOT);
        for (String name : DRAGON_KILL_ORDER) {
            if (name.equalsIgnoreCase(raw)) {
                return Optional.of(name);
            }
        }
        return Optional.empty();
    }

    public static boolean dragonKillChat(String chat) {
        String text = DungeonPolicy.normalize(chat);
        return dragonPadName(text).isPresent() && DRAGON_KILL.matcher(text).find();
    }

    public static boolean slotInSolution(List<DungeonPolicy.TerminalClick> clicks, int slot) {
        if (clicks == null || slot < 0) {
            return false;
        }
        for (DungeonPolicy.TerminalClick click : clicks) {
            if (click.slot() == slot) {
                return true;
            }
        }
        return false;
    }

    public static boolean chestTerminalSlot(int slot) {
        return slot >= 0 && slot < 54;
    }

    public static boolean shouldBlockWrongTerminalSlot(
            boolean enabled,
            boolean sneaking,
            boolean inChestSlots,
            boolean inSolution) {
        if (!enabled || sneaking || !inChestSlots) {
            return false;
        }
        return !inSolution;
    }

    public static boolean shouldHideClickedSlot(
            boolean hideClicked,
            boolean inChestSlots,
            boolean empty,
            boolean inSolution) {
        if (!hideClicked || !inChestSlots || empty) {
            return false;
        }
        return !inSolution;
    }

    public static String formatCountdown(
            String label,
            long remainingMs,
            boolean ticks,
            boolean symbol,
            boolean prefix) {
        long left = Math.max(0L, remainingMs);
        String value;
        String suffix;
        if (ticks) {
            value = Long.toString(left / 50L);
            suffix = symbol ? "t" : "";
        } else {
            value = String.format(Locale.ROOT, "%.1f", left / 1000.0D);
            suffix = symbol ? "s" : "";
        }
        String body = value + suffix;
        if (!prefix || label == null || label.isBlank()) {
            return body;
        }
        return label + " " + body;
    }

    public static String goldorFrenzyLine(int ticksLeft) {
        return "Goldor frenzy " + Math.max(0, ticksLeft) + "t";
    }

    public static String purplePadLine(int ticksLeft) {
        return "Purple pad " + Math.max(0, ticksLeft) + "t";
    }

    private static float shortestYaw(float from, float to) {
        float delta = to - from;
        while (delta > 180.0F) {
            delta -= 360.0F;
        }
        while (delta < -180.0F) {
            delta += 360.0F;
        }
        return delta;
    }
}
