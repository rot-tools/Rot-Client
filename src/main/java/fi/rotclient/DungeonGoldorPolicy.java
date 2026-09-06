package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Floor 7 Goldor helpers copied from licensed Temple Arrows Device /
 * Terminal Times and Edge position / ledge numbers. Minecraft-free so
 * unit tests can pin the 3x3 Sharp Shooter grid, greedy aims, section
 * timers and callout boxes without a client.
 */
public final class DungeonGoldorPolicy {
    public static final EmberDungeonPolicy.Aabb SHARP_ROOM =
            new EmberDungeonPolicy.Aabb(20.0D, 100.0D, 30.0D, 89.0D, 151.0D, 51.0D);
    public static final EmberDungeonPolicy.Aabb LEDGE_YELLOW =
            new EmberDungeonPolicy.Aabb(20.0D, 163.0D, 0.0D, 58.0D, 213.0D, 107.0D);
    public static final double LEDGE_X = 33.704D;
    public static final int MARKED_COLOR = 0x8000FFFF;
    public static final int TARGET_COLOR = 0x80FF55FF;
    public static final int FIRST_AIM_COLOR = 0x8022C55E;
    public static final int SECOND_AIM_COLOR = 0x80FACC15;
    public static final int THIRD_AIM_COLOR = 0x80EF4444;
    public static final String[] SECTION_LABELS = {"1st", "2nd", "3rd", "4th"};

    private static final Pattern DEVICE_COMPLETE = Pattern.compile(
            "^(.{1,16}) completed a device! \\((\\d)/(\\d)\\)$");
    private static final Pattern TERMINAL_COMPLETE = Pattern.compile(
            "^(.{1,16}) (activated|completed) a (terminal|lever|device)! \\((\\d)/(\\d)\\)$");
    private static final Pattern GOLDOR_START = Pattern.compile(
            "^\\[BOSS] Goldor: Who dares trespass into my domain\\?$");
    private static final Pattern CORE_OPENING = Pattern.compile(
            "^The Core entrance is opening!$");
    public static final List<EmberDungeonPolicy.IntVec> DEVICE_BLOCKS = EmberDungeonPolicy.i4Blocks();
    private static final List<IntPair> ADJACENT_PAIRS = buildAdjacentPairs();

    public record IntPair(EmberDungeonPolicy.IntVec first, EmberDungeonPolicy.IntVec second) {
    }

    public record Vec3d(double x, double y, double z) {
        public double distanceTo(Vec3d other) {
            if (other == null) {
                return Double.POSITIVE_INFINITY;
            }
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }

    public record Aim(Vec3d position, Set<EmberDungeonPolicy.IntVec> covered, double distance) {
        public Aim {
            covered = covered == null ? Set.of() : Set.copyOf(covered);
        }
    }

    public record ShooterState(
            Set<EmberDungeonPolicy.IntVec> marked,
            EmberDungeonPolicy.IntVec target,
            List<Aim> aims,
            boolean complete) {
        public ShooterState {
            marked = marked == null ? Set.of() : Set.copyOf(marked);
            aims = aims == null ? List.of() : List.copyOf(aims);
        }

        public static ShooterState idle() {
            return new ShooterState(Set.of(), null, List.of(), false);
        }
    }

    public record TermTimesState(
            int completed,
            int total,
            boolean gateBlown,
            long sectionStartMs,
            long phaseStartMs,
            List<Float> sectionTimes,
            int goldorSection,
            boolean stormPhase,
            boolean stormDead,
            boolean goldorPhase,
            String lastLine,
            String totalLine) {
        public TermTimesState {
            sectionTimes = sectionTimes == null ? List.of() : List.copyOf(sectionTimes);
            lastLine = lastLine == null ? "" : lastLine;
            totalLine = totalLine == null ? "" : totalLine;
        }

        public static TermTimesState idle() {
            return new TermTimesState(0, 7, false, 0L, 0L, List.of(), -1, false, false, false, "", "");
        }

        public boolean inP2() {
            return stormPhase;
        }

        public boolean inP3() {
            return goldorPhase;
        }
    }

    public record Callout(
            String id,
            String message,
            List<String> checks,
            EmberDungeonPolicy.Aabb box,
            int[] sections) {
        public Callout {
            checks = checks == null ? List.of() : List.copyOf(checks);
            sections = sections == null ? new int[0] : sections.clone();
        }
    }

    public static final class PositionTracker {
        private final Map<String, Flags> flags = new LinkedHashMap<>();

        public PositionTracker() {
            reset();
        }

        public void reset() {
            flags.clear();
            for (Callout callout : CALLOUTS) {
                flags.put(callout.id(), new Flags(true, true));
            }
        }

        public void enterP2() {
            unlock("SS");
        }

        public void enterTerminals() {
            Flags ss = flags.get("SS");
            if (ss != null && ss.sent && ss.personal) {
                unlock("SS");
            }
            unlock("EE2");
            unlock("HEE2");
            unlock("EE3");
            unlock("CORE");
            unlock("TUNNEL");
            unlock("SAFE_2");
            unlock("SAFE_3");
            unlock("SPLIT_2");
            unlock("SAFE_2_HIGH");
        }

        public void enterTunnel() {
            lockAllExceptSs();
        }

        public void noteChat(String chat) {
            String lowered = DungeonPolicy.normalize(chat).toLowerCase(Locale.ROOT);
            for (Callout callout : CALLOUTS) {
                if (!matchesAlias(callout, lowered)) {
                    continue;
                }
                Flags current = flags.get(callout.id());
                if (current != null && !current.sent) {
                    current.sent = true;
                }
            }
        }

        public Optional<String> tick(
                double x,
                double y,
                double z,
                int goldorSection,
                boolean p2,
                boolean p3) {
            for (Callout callout : CALLOUTS) {
                Flags current = flags.get(callout.id());
                if (current == null || current.sent || current.personal) {
                    continue;
                }
                if (!inSection(callout, goldorSection, p2, p3)) {
                    continue;
                }
                if (!callout.box().contains(x, y, z)) {
                    continue;
                }
                current.personal = true;
                return Optional.of(callout.message());
            }
            return Optional.empty();
        }

        private void unlock(String id) {
            flags.put(id, new Flags(false, false));
        }

        private void lockAllExceptSs() {
            for (Callout callout : CALLOUTS) {
                if ("SS".equals(callout.id())) {
                    continue;
                }
                flags.put(callout.id(), new Flags(true, true));
            }
        }
    }

    private static final class Flags {
        private boolean sent;
        private boolean personal;

        private Flags(boolean sent, boolean personal) {
            this.sent = sent;
            this.personal = personal;
        }
    }

    public static final List<Callout> CALLOUTS = List.of(
            new Callout("SS", "At SS!", List.of("ss"),
                    new EmberDungeonPolicy.Aabb(107.0D, 120.0D, 93.0D, 110.0D, 121.0D, 95.0D),
                    new int[]{0, 1}),
            new Callout("EE2", "At EE2!", List.of("ee2", "early enter 2"),
                    new EmberDungeonPolicy.Aabb(57.0D, 109.0D, 130.0D, 59.0D, 110.0D, 132.0D),
                    new int[]{1, 2}),
            new Callout("HEE2", "At High EE2!", List.of("high ee2", "highee2", "hee2"),
                    new EmberDungeonPolicy.Aabb(59.0D, 132.0D, 138.0D, 62.0D, 133.0D, 140.0D),
                    new int[]{1, 2}),
            new Callout("EE3", "At EE3!", List.of("ee3", "early enter 3", "early entry 3"),
                    new EmberDungeonPolicy.Aabb(1.0D, 109.0D, 103.0D, 3.0D, 110.0D, 106.0D),
                    new int[]{2, 3}),
            new Callout("CORE", "At Core!", List.of("core"),
                    new EmberDungeonPolicy.Aabb(53.0D, 115.0D, 51.0D, 56.0D, 116.0D, 54.0D),
                    new int[]{2, 3, 4}),
            new Callout("TUNNEL", "Inside Goldor Tunnel!", List.of("tunnel"),
                    new EmberDungeonPolicy.Aabb(52.0D, 114.0D, 55.0D, 57.0D, 116.0D, 58.0D),
                    new int[]{4, 5}),
            new Callout("SAFE_2", "At 2 Safespot!",
                    List.of("2 safespot", "ee2 safespot", "safespot ee2", "s2 safespot", "safespot s2"),
                    new EmberDungeonPolicy.Aabb(46.0D, 109.0D, 121.987D, 49.0D, 110.0D, 121.988D),
                    new int[]{1, 2}),
            new Callout("SAFE_3", "At 3 Safespot!",
                    List.of("3 safespot", "ee3 safespot", "freaky ee3", "freak ee", "safespot s3", "safespot 3"),
                    new EmberDungeonPolicy.Aabb(18.0D, 121.0D, 91.0D, 19.0D, 126.0D, 99.0D),
                    new int[]{2, 3}),
            new Callout("SPLIT_2", "At Split ee2!", List.of("split ee2", "splitee2", "mage term"),
                    new EmberDungeonPolicy.Aabb(58.0D, 119.0D, 124.0D, 60.0D, 123.0D, 126.0D),
                    new int[]{1, 2}),
            new Callout("SAFE_2_HIGH", "At High 2 Safespot!", List.of("2 safespot"),
                    new EmberDungeonPolicy.Aabb(70.0D, 127.0D, 143.7D, 57.0D, 133.0D, 146.7D),
                    new int[]{1, 2}));

    private DungeonGoldorPolicy() {
    }

    public static List<IntPair> adjacentPairs() {
        return ADJACENT_PAIRS;
    }

    public static boolean isDeviceBlock(int x, int y, int z) {
        return EmberDungeonPolicy.isI4Block(x, y, z);
    }

    public static boolean inSharpRoom(double x, double y, double z) {
        return SHARP_ROOM.contains(x, y, z);
    }

    public static boolean isEmerald(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("emerald_block") || id.endsWith(":emerald_block") || id.equals("emerald_block");
    }

    public static boolean isBlueTerracotta(String blockId) {
        String id = blockId == null ? "" : blockId.toLowerCase(Locale.ROOT);
        return id.contains("blue_terracotta") && !id.contains("light_blue");
    }

    public static boolean isActiveArmorStand(String name) {
        return "Active".equals(DungeonPolicy.normalize(name));
    }

    public static boolean localCompletedDevice(String chat, String localName) {
        Matcher match = DEVICE_COMPLETE.matcher(DungeonPolicy.normalize(chat));
        if (!match.matches() || localName == null || localName.isBlank()) {
            return false;
        }
        return namesMatch(match.group(1), localName);
    }

    public static ShooterState resetShooter() {
        return ShooterState.idle();
    }

    public static ShooterState completeShooter(ShooterState current) {
        return new ShooterState(Set.of(), null, List.of(), true);
    }

    public static ShooterState observeBlock(
            ShooterState current,
            int x,
            int y,
            int z,
            String oldId,
            String newId) {
        if (current == null || current.complete() || !isDeviceBlock(x, y, z)) {
            return current == null ? ShooterState.idle() : current;
        }
        EmberDungeonPolicy.IntVec pos = new EmberDungeonPolicy.IntVec(x, y, z);
        if (isEmerald(oldId) && isBlueTerracotta(newId)) {
            return mark(current, pos);
        }
        if (isBlueTerracotta(oldId) && isEmerald(newId)) {
            return retarget(current, pos);
        }
        return current;
    }

    public static ShooterState observeWorld(
            ShooterState current,
            Function<EmberDungeonPolicy.IntVec, String> idAt) {
        if (current == null || current.complete() || idAt == null) {
            return current == null ? ShooterState.idle() : current;
        }
        EmberDungeonPolicy.IntVec emerald = null;
        for (EmberDungeonPolicy.IntVec pos : DEVICE_BLOCKS) {
            if (isEmerald(idAt.apply(pos))) {
                emerald = pos;
                break;
            }
        }
        ShooterState next = current;
        if (current.target() != null
                && !current.target().equals(emerald)
                && isBlueTerracotta(idAt.apply(current.target()))) {
            next = mark(next, current.target());
        }
        if (emerald != null && (next.target() == null || !emerald.equals(next.target()))) {
            next = retarget(next, emerald);
        }
        return next;
    }

    public static List<Aim> calculateOptimalAimPositions(
            EmberDungeonPolicy.IntVec target,
            Set<EmberDungeonPolicy.IntVec> marked) {
        if (target == null) {
            return List.of();
        }
        Set<EmberDungeonPolicy.IntVec> markedSet = marked == null ? Set.of() : marked;
        List<EmberDungeonPolicy.IntVec> unmarked = new ArrayList<>();
        for (EmberDungeonPolicy.IntVec pos : DEVICE_BLOCKS) {
            if (!markedSet.contains(pos)) {
                unmarked.add(pos);
            }
        }
        Set<EmberDungeonPolicy.IntVec> unmarkedSet = new LinkedHashSet<>(unmarked);
        Aim greenAim = null;
        for (IntPair pair : ADJACENT_PAIRS) {
            if (!pairContains(pair, target)) {
                continue;
            }
            Aim aim = createAim(pair, unmarkedSet);
            if (aim == null) {
                continue;
            }
            if (greenAim == null || aim.covered().size() > greenAim.covered().size()) {
                greenAim = aim;
            }
        }
        if (greenAim == null) {
            return List.of();
        }
        List<Aim> remaining = new ArrayList<>();
        for (IntPair pair : ADJACENT_PAIRS) {
            if (pairContains(pair, target)) {
                continue;
            }
            Aim aim = createAim(pair, unmarkedSet);
            if (aim != null) {
                remaining.add(aim);
            }
        }
        return findBestCombination(greenAim, remaining);
    }

    public static int aimColor(int index) {
        return switch (index) {
            case 0 -> FIRST_AIM_COLOR;
            case 1 -> SECOND_AIM_COLOR;
            default -> THIRD_AIM_COLOR;
        };
    }

    public static EmberDungeonPolicy.Aabb aimBox(Aim aim) {
        Vec3d pos = aim.position();
        return new EmberDungeonPolicy.Aabb(
                pos.x() - 0.5D,
                pos.y() - 0.5D,
                pos.z() - 0.1D,
                pos.x() + 0.5D,
                pos.y() + 0.5D,
                pos.z() + 0.9D);
    }

    public static TermTimesState applyChat(TermTimesState current, String chat, long now) {
        TermTimesState state = current == null ? TermTimesState.idle() : current;
        String text = DungeonPolicy.normalize(chat);
        if (DungeonF7Policy.stormDeath(text)) {
            return new TermTimesState(
                    state.completed(),
                    state.total(),
                    state.gateBlown(),
                    state.sectionStartMs(),
                    state.phaseStartMs(),
                    state.sectionTimes(),
                    state.goldorSection() < 0 ? 0 : state.goldorSection(),
                    true,
                    true,
                    false,
                    "",
                    "");
        }
        DungeonAssistPolicy.F7Timer timer = DungeonAssistPolicy.f7TimerFromChat(text);
        if (timer == DungeonAssistPolicy.F7Timer.STORM_START
                || timer == DungeonAssistPolicy.F7Timer.STORM_PAD
                || timer == DungeonAssistPolicy.F7Timer.STORM_PY) {
            return new TermTimesState(
                    state.completed(),
                    state.total(),
                    state.gateBlown(),
                    state.sectionStartMs(),
                    state.phaseStartMs(),
                    state.sectionTimes(),
                    0,
                    true,
                    false,
                    false,
                    "",
                    "");
        }
        if (GOLDOR_START.matcher(text).matches()
                || timer == DungeonAssistPolicy.F7Timer.GOLDOR) {
            return resetSection(state, now, true, 1, true);
        }
        if (timer == DungeonAssistPolicy.F7Timer.NECRON) {
            return TermTimesState.idle();
        }
        Matcher progress = TERMINAL_COMPLETE.matcher(text);
        if (progress.matches()) {
            int currentCount = Integer.parseInt(progress.group(4));
            int total = Integer.parseInt(progress.group(5));
            String line = progressLine(
                    progress.group(1),
                    progress.group(2),
                    progress.group(3),
                    currentCount,
                    total,
                    secondsSince(state.sectionStartMs(), now),
                    secondsSince(state.phaseStartMs(), now));
            if ((currentCount == total && state.gateBlown())
                    || currentCount < state.completed()) {
                TermTimesState reset = resetSection(state, now, false, state.goldorSection() + 1, true);
                return new TermTimesState(
                        reset.completed(),
                        reset.total(),
                        reset.gateBlown(),
                        reset.sectionStartMs(),
                        reset.phaseStartMs(),
                        reset.sectionTimes(),
                        reset.goldorSection(),
                        reset.stormPhase(),
                        reset.stormDead(),
                        reset.goldorPhase(),
                        line,
                        "");
            }
            return new TermTimesState(
                    currentCount,
                    total,
                    state.gateBlown(),
                    state.sectionStartMs(),
                    state.phaseStartMs(),
                    state.sectionTimes(),
                    Math.max(1, state.goldorSection()),
                    false,
                    true,
                    true,
                    line,
                    "");
        }
        if (DungeonF7Policy.p3GateDestroyed(text)) {
            if (state.completed() == state.total()) {
                return resetSection(state, now, false, state.goldorSection() + 1, true);
            }
            return new TermTimesState(
                    state.completed(),
                    state.total(),
                    true,
                    state.sectionStartMs(),
                    state.phaseStartMs(),
                    state.sectionTimes(),
                    Math.max(1, state.goldorSection()),
                    false,
                    true,
                    true,
                    "",
                    "");
        }
        if (CORE_OPENING.matcher(text).matches()) {
            TermTimesState reset = resetSection(state, now, false, 5, true);
            return new TermTimesState(
                    reset.completed(),
                    reset.total(),
                    reset.gateBlown(),
                    reset.sectionStartMs(),
                    reset.phaseStartMs(),
                    reset.sectionTimes(),
                    5,
                    false,
                    true,
                    true,
                    "",
                    totalLine(reset.sectionTimes(), secondsSince(state.phaseStartMs(), now)));
        }
        return state;
    }

    public static List<String> termHudLines(TermTimesState state, Map<String, Long> pbs, long now) {
        if (state == null || !state.goldorPhase()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        StringBuilder splits = new StringBuilder("P3 ");
        List<Float> times = state.sectionTimes();
        for (int i = 0; i < SECTION_LABELS.length; i++) {
            if (i > 0) {
                splits.append(" | ");
            }
            splits.append(SECTION_LABELS[i]).append(' ');
            if (i < times.size()) {
                splits.append(formatSeconds(times.get(i)));
                splits.append(pbSuffix(SECTION_LABELS[i], pbs));
            } else if (i == times.size() && state.sectionStartMs() > 0L) {
                splits.append(formatSeconds(secondsSince(state.sectionStartMs(), now)));
            } else {
                splits.append("--");
            }
        }
        lines.add(splits.toString());
        if (state.phaseStartMs() > 0L) {
            lines.add("P3 total " + formatSeconds(secondsSince(state.phaseStartMs(), now)));
        }
        if (!state.lastLine().isBlank()) {
            lines.add(state.lastLine());
        }
        if (!state.totalLine().isBlank()) {
            lines.add(state.totalLine());
        }
        return lines;
    }

    public static Map<String, Long> recordSectionPbs(TermTimesState previous, TermTimesState next, Map<String, Long> stored) {
        Map<String, Long> pbs = stored == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(stored);
        if (previous == null || next == null) {
            return pbs;
        }
        if (next.sectionTimes().size() > previous.sectionTimes().size()) {
            int index = next.sectionTimes().size() - 1;
            if (index >= 0 && index < SECTION_LABELS.length) {
                float seconds = next.sectionTimes().get(index);
                DungeonLeftoverPolicy.recordSplitPersonalBest(
                        pbs,
                        SECTION_LABELS[index].toUpperCase(Locale.ROOT),
                        Math.round(seconds * 1000.0F));
            }
        }
        return pbs;
    }

    public static boolean recordTerminalTypePb(
            Map<String, Long> pbs,
            DungeonPolicy.Terminal type,
            long openedAt,
            long now) {
        if (pbs == null || type == null || type == DungeonPolicy.Terminal.NONE || openedAt <= 0L) {
            return false;
        }
        return DungeonLeftoverPolicy.recordSplitPersonalBest(pbs, type.name(), now - openedAt);
    }

    public static double ledgeDistance(double x) {
        return x - LEDGE_X;
    }

    public static boolean inYellowPad(double x, double y, double z) {
        return LEDGE_YELLOW.contains(x, y, z);
    }

    public static boolean showLedge(
            boolean enabled,
            boolean yellowOnly,
            boolean inYellow,
            boolean p2,
            boolean stormDead,
            boolean allClasses,
            boolean masterMode,
            DungeonPolicy.DungeonClass dungeonClass) {
        if (!enabled || !p2 || stormDead) {
            return false;
        }
        if (yellowOnly && !inYellow) {
            return false;
        }
        if (allClasses) {
            return true;
        }
        if (masterMode) {
            return dungeonClass == DungeonPolicy.DungeonClass.MAGE
                    || dungeonClass == DungeonPolicy.DungeonClass.ARCHER;
        }
        return dungeonClass == DungeonPolicy.DungeonClass.TANK
                || dungeonClass == DungeonPolicy.DungeonClass.ARCHER;
    }

    public static String ledgeHudLine(double distance) {
        return String.format(Locale.ROOT, "Ledge %.1f", distance);
    }

    public static boolean namesMatch(String chatName, String localName) {
        String chat = DungeonPolicy.normalize(chatName).trim();
        String local = DungeonPolicy.normalize(localName).trim();
        if (chat.isEmpty() || local.isEmpty()) {
            return false;
        }
        return chat.equalsIgnoreCase(local) || chat.toLowerCase(Locale.ROOT).endsWith(local.toLowerCase(Locale.ROOT));
    }

    private static ShooterState mark(ShooterState current, EmberDungeonPolicy.IntVec pos) {
        LinkedHashSet<EmberDungeonPolicy.IntVec> marked = new LinkedHashSet<>(current.marked());
        marked.add(pos);
        EmberDungeonPolicy.IntVec target = pos.equals(current.target()) ? null : current.target();
        return new ShooterState(marked, target, calculateOptimalAimPositions(target, marked), false);
    }

    private static ShooterState retarget(ShooterState current, EmberDungeonPolicy.IntVec pos) {
        LinkedHashSet<EmberDungeonPolicy.IntVec> marked = new LinkedHashSet<>(current.marked());
        marked.remove(pos);
        return new ShooterState(marked, pos, calculateOptimalAimPositions(pos, marked), false);
    }

    private static Aim createAim(IntPair pair, Set<EmberDungeonPolicy.IntVec> unmarked) {
        LinkedHashSet<EmberDungeonPolicy.IntVec> covered = new LinkedHashSet<>();
        if (unmarked.contains(pair.first())) {
            covered.add(pair.first());
        }
        if (unmarked.contains(pair.second())) {
            covered.add(pair.second());
        }
        if (covered.isEmpty()) {
            return null;
        }
        return new Aim(midpoint(pair.first(), pair.second()), covered, 0.0D);
    }

    private static List<Aim> findBestCombination(Aim greenAim, List<Aim> aimPositions) {
        List<Aim> result = new ArrayList<>();
        result.add(greenAim);
        Set<EmberDungeonPolicy.IntVec> covered = new LinkedHashSet<>(greenAim.covered());
        for (int n = 0; n < 2; n++) {
            Aim picked = null;
            int bestNew = Integer.MIN_VALUE;
            int bestSize = Integer.MIN_VALUE;
            double bestCloser = Double.NEGATIVE_INFINITY;
            Vec3d last = result.get(result.size() - 1).position();
            for (Aim candidate : aimPositions) {
                if (result.contains(candidate)) {
                    continue;
                }
                int newly = 0;
                for (EmberDungeonPolicy.IntVec block : candidate.covered()) {
                    if (!covered.contains(block)) {
                        newly++;
                    }
                }
                double closer = -last.distanceTo(candidate.position());
                if (newly > bestNew
                        || (newly == bestNew && candidate.covered().size() > bestSize)
                        || (newly == bestNew
                                && candidate.covered().size() == bestSize
                                && closer > bestCloser)) {
                    bestNew = newly;
                    bestSize = candidate.covered().size();
                    bestCloser = closer;
                    picked = candidate;
                }
            }
            if (picked == null) {
                continue;
            }
            result.add(picked);
            covered.addAll(picked.covered());
        }
        List<Aim> out = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            Aim aim = result.get(i);
            double distance = i == 0 ? 0.0D : aim.position().distanceTo(result.get(i - 1).position());
            out.add(new Aim(aim.position(), aim.covered(), distance));
        }
        return List.copyOf(out);
    }

    private static Vec3d midpoint(EmberDungeonPolicy.IntVec a, EmberDungeonPolicy.IntVec b) {
        return new Vec3d((a.x() + b.x()) / 2.0D + 0.5D, a.y() + 0.5D, a.z() + 0.5D);
    }

    private static boolean pairContains(IntPair pair, EmberDungeonPolicy.IntVec target) {
        return pair.first().equals(target) || pair.second().equals(target);
    }

    private static List<IntPair> buildAdjacentPairs() {
        List<IntPair> pairs = new ArrayList<>();
        List<EmberDungeonPolicy.IntVec> blocks = DEVICE_BLOCKS;
        for (int i = 0; i < blocks.size(); i++) {
            EmberDungeonPolicy.IntVec first = blocks.get(i);
            for (int j = i + 1; j < blocks.size(); j++) {
                EmberDungeonPolicy.IntVec second = blocks.get(j);
                if (Math.abs(first.x() - second.x()) == 2
                        && first.y() == second.y()
                        && first.z() == second.z()) {
                    pairs.add(new IntPair(first, second));
                }
            }
        }
        return List.copyOf(pairs);
    }

    private static TermTimesState resetSection(
            TermTimesState state,
            long now,
            boolean full,
            int section,
            boolean goldor) {
        List<Float> times = new ArrayList<>();
        long phaseStart = state.phaseStartMs();
        if (full) {
            phaseStart = now;
        } else if (state.sectionStartMs() > 0L) {
            times.addAll(state.sectionTimes());
            times.add(secondsSince(state.sectionStartMs(), now));
        } else {
            times.addAll(state.sectionTimes());
        }
        return new TermTimesState(
                0,
                7,
                false,
                now,
                phaseStart,
                times,
                section,
                false,
                true,
                goldor,
                "",
                "");
    }

    private static String progressLine(
            String name,
            String action,
            String type,
            int current,
            int total,
            float sectionSeconds,
            float phaseSeconds) {
        return name + " " + action + " a " + type + "! (" + current + "/" + total + ") "
                + formatSeconds(sectionSeconds) + " | " + formatSeconds(phaseSeconds);
    }

    private static String totalLine(List<Float> times, float totalSeconds) {
        StringBuilder out = new StringBuilder("Times: ");
        for (int i = 0; i < times.size(); i++) {
            if (i > 0) {
                out.append(" | ");
            }
            out.append(formatSeconds(times.get(i)));
        }
        out.append(", Total: ").append(formatSeconds(totalSeconds));
        return out.toString();
    }

    private static float secondsSince(long startMs, long now) {
        if (startMs <= 0L) {
            return 0.0F;
        }
        return (now - startMs) / 1000.0F;
    }

    private static String formatSeconds(float seconds) {
        return String.format(Locale.ROOT, "%.1fs", seconds);
    }

    private static String pbSuffix(String label, Map<String, Long> pbs) {
        if (pbs == null || label == null) {
            return "";
        }
        Long pb = pbs.get(label.toUpperCase(Locale.ROOT));
        if (pb == null || pb <= 0L) {
            return "";
        }
        return " (PB " + formatSeconds(pb / 1000.0F) + ")";
    }

    private static boolean inSection(
            Callout callout,
            int goldorSection,
            boolean p2,
            boolean p3) {
        for (int section : callout.sections()) {
            if (section == 0) {
                if (p2 || p3) {
                    return true;
                }
                continue;
            }
            if (p3 && goldorSection == section) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAlias(Callout callout, String lowered) {
        for (String check : callout.checks()) {
            if (lowered.contains(check)) {
                return true;
            }
        }
        return false;
    }
}
