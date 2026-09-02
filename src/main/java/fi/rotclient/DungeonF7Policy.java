package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    public static final long CRYSTAL_RESPAWN_MILLIS = 4_000L;
    public static final long RELIC_SPAWN_MILLIS = 42_000L;

    private static final Pattern CRYSTAL_PICKUP = Pattern.compile(
            "^(\\w{3,16}) picked up an Energy Crystal!$");
    private static final Pattern CRYSTAL_SPAWN = Pattern.compile(
            "^\\[BOSS] Maxor: (?:THAT BEAM! IT HURTS! IT HURTS!!|YOU TRICKED ME!)$");
    private static final Pattern MELODY_CHAT = Pattern.compile("(?i)melody");
    private static final Pattern DRAGON_SPRAY = Pattern.compile("(?i)ice spray");
    private static final Pattern DRAGON_ARROWS = Pattern.compile("(?i)arrows? hit");
    private static final Pattern MELODY_PARTY = Pattern.compile(
            "(?i)^Party > (?:\\[[^\\]]+]\\s*)?(\\w{3,16})\\s*[:\\>].*(?:melody|\\d/4|\\d{1,3}%)");

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
        return text.contains("melody") && (text.contains("terminal") || MELODY_CHAT.matcher(text).find());
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

    public static boolean isSimonButton(int x, int y, int z) {
        return x == 110 && y >= 120 && y <= 123 && z >= 92 && z <= 95;
    }

    public static EmberDungeonPolicy.IntVec simonStart() {
        return new EmberDungeonPolicy.IntVec(110, 121, 91);
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
            if (slotCol == col && row >= 1 && row <= 4) {
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
            "^(\\w{3,16}) (?:activated|completed) a (terminal|lever|device)! \\((\\d+)/(\\d+)\\)$");

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
        if (digit < 1 || digit > 4) {
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
        long left = Math.max(0L, deadlineMillis - nowMillis);
        return "Dragon spawn " + String.format(Locale.ROOT, "%.1fs", left / 1000.0D);
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
