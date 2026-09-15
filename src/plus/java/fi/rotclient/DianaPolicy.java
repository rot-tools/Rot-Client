package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mayor Diana / Mythological Ritual helpers for Serveri.
 * Burrow guess math and particle-trail handling use the reviewed Griffin
 * burrow model. Party warp and {@code /pc} share are cheat gates and stay
 * off unless explicitly enabled.
 */
public final class DianaPolicy {
    public static final int MIN_GUESS_POINTS = 4;
    public static final double TRAIL_MAX_STEP = 3.0;
    public static final long SPADE_GUESS_WINDOW_MS = 3_000L;
    public static final long BURROW_STALE_MS = 750L;
    public static final long BUGGED_SPADE_CANCEL_MS = 200L;
    public static final long GRIFFIN_WARN_COOLDOWN_MS = 30_000L;
    public static final long DUPLICATE_CHAT_MS = 400L;
    public static final long PARTY_SHARE_COOLDOWN_MS = 5_000L;

    public enum BurrowType {
        UNKNOWN,
        START,
        MOB,
        TREASURE,
        GUESS
    }

    public enum ParticleKind {
        NONE,
        ENCHANT,
        FOOTSTEP,
        START,
        MOB,
        TREASURE,
        SPADE_TRAIL
    }

    public enum RareMob {
        INQUISITOR("Minos Inquisitor"),
        SPHINX("Sphinx"),
        MANTICORE("Manticore"),
        KING_MINOS("King Minos"),
        CHAMPION("Minos Champion"),
        HUNTER("Minos Hunter"),
        MINOTAUR("Minotaur");

        private final String display;

        RareMob(String display) {
            this.display = display;
        }

        public String display() {
            return display;
        }

        public boolean rareHighlight() {
            return this == INQUISITOR || this == SPHINX || this == MANTICORE || this == KING_MINOS;
        }
    }

    public record Point(double x, double y, double z) {
        public double distanceTo(Point other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        public Point down(double amount) {
            return new Point(x, y - amount, z);
        }

        public BlockGuess roundToBlock() {
            return new BlockGuess(
                    (int) Math.floor(x),
                    (int) Math.floor(y),
                    (int) Math.floor(z));
        }
    }

    public record BlockGuess(int x, int y, int z) {
        public String hud() {
            return "Guess " + x + " " + y + " " + z;
        }

        public String partyCoords() {
            return "x: " + x + ", y: " + y + ", z: " + z;
        }
    }

    public record ParticlePacket(
            String typeId,
            int count,
            float speed,
            float offsetX,
            float offsetY,
            float offsetZ) {
    }

    public record BurrowSample(boolean hasEnchant, boolean hasFootstep, BurrowType type) {
        public boolean complete() {
            return hasEnchant && hasFootstep && type != BurrowType.UNKNOWN && type != BurrowType.GUESS;
        }

        public BurrowSample withEnchant() {
            return new BurrowSample(true, hasFootstep, type);
        }

        public BurrowSample withFootstep() {
            return new BurrowSample(hasEnchant, true, type);
        }

        public BurrowSample withType(BurrowType next) {
            return new BurrowSample(hasEnchant, hasFootstep, next);
        }
    }

    public record DugChat(int current, int max, boolean chainFinished) {
        public int remaining() {
            return Math.max(0, max - current);
        }
    }

    public record WarpPoint(String name, String command, int x, int y, int z) {
        public double distanceTo(Point target) {
            return new Point(x + 0.5, y, z + 0.5).distanceTo(target);
        }
    }

    public record Drop(String name, int amount) {
    }

    public record GuessResult(Point location, BlockGuess block, double pitchRadians) {
    }

    private static final Pattern DUG = Pattern.compile(
            "You (?:finished the Griffin burrow chain!|dug out a Griffin Burrow!)\\s*\\((?<current>\\d+)/(?<max>\\d+)\\)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DUG_LOOSE = Pattern.compile(
            "You dug out a Griffin Burrow", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHAIN = Pattern.compile(
            "You finished the Griffin burrow chain", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPAWN = Pattern.compile(
            "(?:Oh|Uh oh|Yikes|Oi|Good Grief|Danger|Woah)! You dug out (?:a )?(?<creature>[\\w\\s]+)!?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COINS = Pattern.compile(
            "Wow! You dug out (?<coins>[\\d,.]+) coins!?", Pattern.CASE_INSENSITIVE);
    private static final Pattern RARE_DROP = Pattern.compile(
            "RARE DROP! (?:You dug out a )?(?<item>[-() \\w]+)(?: \\(\\+\\d+ . Magic Find\\))?!?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TREASURE_DUG = Pattern.compile(
            "(?:RARE DROP!|Wow!) You dug out(?: a)? .+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFENDERS = Pattern.compile(
            "^Defeat all the burrow defenders in order to dig it!$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARTY_COORDS = Pattern.compile(
            "(?:Party > )?.+:\\s*x:\\s*(?<x>-?\\d+(?:\\.\\d+)?)\\s*,?\\s*y:\\s*(?<y>-?\\d+(?:\\.\\d+)?)\\s*,?\\s*z:\\s*(?<z>-?\\d+(?:\\.\\d+)?)(?:\\s*\\|\\s*(?<mob>.+))?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCOREBOARD_BURROWS = Pattern.compile(
            "Burrows?:\\s*(?<current>\\d+)\\s*/\\s*(?<max>\\d+)", Pattern.CASE_INSENSITIVE);

    private static final List<WarpPoint> WARPS = List.of(
            new WarpPoint("Hub", "warp hub", 0, 77, -1),
            new WarpPoint("Stonks", "warp stonks", -37, 70, -82),
            new WarpPoint("Museum", "warp museum", 29, 72, 1),
            new WarpPoint("Castle", "warp castle", -250, 130, 45),
            new WarpPoint("Wizard", "warp wizard", 44, 119, 93),
            new WarpPoint("Dark Auction", "warp da", 91, 75, 173),
            new WarpPoint("Crypt", "warp crypt", -161, 62, -107));

    private static final String[] PROFIT_ITEMS = {
            "Griffin Feather",
            "Braided Griffin Feather",
            "Mythos Fragment",
            "Crown of Greed",
            "Antique Remedies",
            "Daedalus Stick",
            "Minos Relic",
            "Crochet Tiger Plushie",
            "Dwarf Turtle Shelmet",
            "Washed-up Souvenir",
            "Chimera",
            "Cretan Urn",
            "Hilt Of Revelations",
            "Brain Food",
            "Fateful Stinger",
            "Shimmering Wool",
            "Myth the Fish"
    };

    private DianaPolicy() {
    }

    public static String strip(String raw) {
        return ChatTextPolicy.stripFormatting(raw).trim();
    }

    public static boolean isDianaSpade(String skyblockId, String hoverName) {
        String id = skyblockId == null ? "" : skyblockId.toUpperCase(Locale.ROOT);
        if (id.contains("ANCESTRAL_SPADE")
                || id.contains("ARCHAIC_SPADE")
                || id.contains("DEIFIC_SPADE")) {
            return true;
        }
        String name = hoverName == null ? "" : hoverName;
        return name.contains("Spade") && (name.contains("Ancestral")
                || name.contains("Archaic")
                || name.contains("Deific")
                || name.contains("Griffin"));
    }

    public static boolean isGriffinPet(String petName) {
        if (petName == null || petName.isBlank()) {
            return false;
        }
        return strip(petName).toLowerCase(Locale.ROOT).contains("griffin");
    }

    public static ParticleKind classifyParticle(ParticlePacket packet) {
        if (packet == null || packet.typeId() == null) {
            return ParticleKind.NONE;
        }
        String type = normalizeType(packet.typeId());
        float ox = round2(packet.offsetX());
        float oy = round2(packet.offsetY());
        float oz = round2(packet.offsetZ());
        if (type.equals("enchant")
                && packet.count() == 5
                && packet.speed() == 0.05F
                && ox == 0.5F
                && oy == 0.4F
                && oz == 0.5F) {
            return ParticleKind.ENCHANT;
        }
        if (type.equals("enchanted_hit")
                && packet.count() == 4
                && packet.speed() == 0.01F
                && ox == 0.5F
                && oy == 0.1F
                && oz == 0.5F) {
            return ParticleKind.START;
        }
        if (type.equals("crit")
                && packet.count() == 3
                && packet.speed() == 0.01F
                && ox == 0.5F
                && oy == 0.1F
                && oz == 0.5F) {
            return ParticleKind.MOB;
        }
        if (type.equals("dripping_lava")
                && packet.count() == 2
                && packet.speed() == -0.5F) {
            return ParticleKind.SPADE_TRAIL;
        }
        if (type.equals("dripping_lava")
                && packet.count() == 2
                && packet.speed() == 0.01F
                && ox == 0.35F
                && oy == 0.1F
                && oz == 0.35F) {
            return ParticleKind.TREASURE;
        }
        if (type.equals("crit")
                && packet.count() == 1
                && packet.speed() == 0.0F
                && ox == 0.05F
                && oy == 0.0F
                && oz == 0.05F) {
            return ParticleKind.FOOTSTEP;
        }
        return ParticleKind.NONE;
    }

    public static BurrowType typeFromKind(ParticleKind kind) {
        return switch (kind) {
            case START -> BurrowType.START;
            case MOB -> BurrowType.MOB;
            case TREASURE -> BurrowType.TREASURE;
            case SPADE_TRAIL -> BurrowType.GUESS;
            default -> BurrowType.UNKNOWN;
        };
    }

    public static BurrowSample applyParticle(BurrowSample current, ParticleKind kind) {
        BurrowSample sample = current == null
                ? new BurrowSample(false, false, BurrowType.UNKNOWN)
                : current;
        return switch (kind) {
            case ENCHANT -> sample.withEnchant();
            case FOOTSTEP -> sample.withFootstep();
            case START -> sample.withType(BurrowType.START);
            case MOB -> sample.withType(BurrowType.MOB);
            case TREASURE -> sample.withType(BurrowType.TREASURE);
            default -> sample;
        };
    }

    public static BlockGuess burrowBlock(Point particleLocation) {
        return particleLocation.down(0.5).roundToBlock();
    }

    public static boolean acceptTrailPoint(List<Point> trail, Point next) {
        if (next == null) {
            return false;
        }
        if (trail == null || trail.isEmpty()) {
            return true;
        }
        Point last = trail.get(trail.size() - 1);
        double dist = last.distanceTo(next);
        return dist > 0.0 && dist <= TRAIL_MAX_STEP;
    }

    public static Optional<GuessResult> guessBurrow(List<Point> particleLocations) {
        if (particleLocations == null || particleLocations.size() < MIN_GUESS_POINTS) {
            return Optional.empty();
        }
        PolynomialFitter xFit = new PolynomialFitter(3);
        PolynomialFitter yFit = new PolynomialFitter(3);
        PolynomialFitter zFit = new PolynomialFitter(3);
        for (int i = 0; i < particleLocations.size(); i++) {
            Point p = particleLocations.get(i);
            xFit.addPoint(i, p.x());
            yFit.addPoint(i, p.y());
            zFit.addPoint(i, p.z());
        }
        double[] cx = xFit.fit();
        double[] cy = yFit.fit();
        double[] cz = zFit.fit();
        Point derivative = new Point(cx[1], cy[1], cz[1]);
        double pitch = pitchFromDerivative(derivative);
        double controlPointDistance = Math.sqrt(24.0 * Math.sin(pitch - Math.PI) + 25.0);
        double length = Math.sqrt(
                derivative.x() * derivative.x()
                        + derivative.y() * derivative.y()
                        + derivative.z() * derivative.z());
        if (length < 1.0E-9) {
            return Optional.empty();
        }
        double t = 3.0 * controlPointDistance / length;
        Point location = new Point(evalCubic(cx, t), evalCubic(cy, t), evalCubic(cz, t));
        return Optional.of(new GuessResult(location, location.down(0.5).roundToBlock(), pitch));
    }

    public static double pitchFromDerivative(Point derivative) {
        double xzLength = Math.sqrt(
                derivative.x() * derivative.x() + derivative.z() * derivative.z());
        double pitchRadians = -Math.atan2(derivative.y(), xzLength);
        double guessPitch = pitchRadians;
        double windowMin = -Math.PI / 2;
        double windowMax = Math.PI / 2;
        for (int i = 0; i < 100; i++) {
            double resultPitch = Math.atan2(Math.sin(guessPitch) - 0.75, Math.cos(guessPitch));
            if (resultPitch == pitchRadians) {
                return guessPitch;
            }
            if (resultPitch < pitchRadians) {
                windowMin = guessPitch;
            } else {
                windowMax = guessPitch;
            }
            guessPitch = (windowMin + windowMax) / 2.0;
        }
        return guessPitch;
    }

    public static Optional<DugChat> parseDug(String chat) {
        String text = strip(chat);
        Matcher matcher = DUG.matcher(text);
        if (matcher.find()) {
            return Optional.of(new DugChat(
                    Integer.parseInt(matcher.group("current")),
                    Integer.parseInt(matcher.group("max")),
                    matcher.group().toLowerCase(Locale.ROOT).contains("finished")));
        }
        if (CHAIN.matcher(text).find()) {
            return Optional.of(new DugChat(4, 4, true));
        }
        if (DUG_LOOSE.matcher(text).find()) {
            return Optional.of(new DugChat(0, 0, false));
        }
        return Optional.empty();
    }

    public static Optional<DugChat> parseScoreboardBurrows(List<String> lines) {
        if (lines == null) {
            return Optional.empty();
        }
        for (String line : lines) {
            Matcher matcher = SCOREBOARD_BURROWS.matcher(strip(line));
            if (matcher.find()) {
                return Optional.of(new DugChat(
                        Integer.parseInt(matcher.group("current")),
                        Integer.parseInt(matcher.group("max")),
                        false));
            }
        }
        return Optional.empty();
    }

    public static Optional<RareMob> parseSpawn(String chat) {
        String text = strip(chat);
        Matcher matcher = SPAWN.matcher(text);
        if (matcher.find()) {
            return matchMob(matcher.group("creature"));
        }
        return matchMob(text);
    }

    public static Optional<RareMob> matchNametag(String nametag) {
        return matchMob(strip(nametag));
    }

    public static Optional<RareMob> matchMob(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String hay = strip(text);
        for (RareMob mob : RareMob.values()) {
            if (hay.toLowerCase(Locale.ROOT).contains(mob.display().toLowerCase(Locale.ROOT))) {
                return Optional.of(mob);
            }
        }
        if (hay.toLowerCase(Locale.ROOT).contains("inquisitor")) {
            return Optional.of(RareMob.INQUISITOR);
        }
        return Optional.empty();
    }

    public static boolean isTreasureChat(String chat) {
        String text = strip(chat);
        if (text.contains("Griffin Feather")
                || text.contains(" coins!")
                || text.contains("Mythos Fragment")
                || text.contains("Braided Griffin Feather")
                || text.contains("Myth the Fish")) {
            return true;
        }
        return TREASURE_DUG.matcher(text).find() && parseSpawn(text).isEmpty();
    }

    public static Optional<Drop> parseDrop(String chat) {
        String text = strip(chat);
        Matcher coins = COINS.matcher(text);
        if (coins.find()) {
            int amount = Integer.parseInt(coins.group("coins").replace(",", ""));
            return Optional.of(new Drop("Coins", amount));
        }
        Matcher rare = RARE_DROP.matcher(text);
        if (rare.find()) {
            String item = rare.group("item").trim();
            if (isProfitItem(item)) {
                return Optional.of(new Drop(item, 1));
            }
        }
        for (String item : PROFIT_ITEMS) {
            if (text.contains(item)) {
                return Optional.of(new Drop(item, 1));
            }
        }
        return Optional.empty();
    }

    public static boolean isProfitItem(String name) {
        if (name == null) {
            return false;
        }
        String needle = name.trim();
        for (String item : PROFIT_ITEMS) {
            if (item.equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return "Coins".equalsIgnoreCase(needle);
    }

    public static boolean isDefenderChat(String chat) {
        return DEFENDERS.matcher(strip(chat)).matches();
    }

    public static boolean isBurrowRelatedChat(String chat) {
        String text = strip(chat);
        return parseDug(text).isPresent()
                || parseSpawn(text).isPresent()
                || parseDrop(text).isPresent()
                || isDefenderChat(text)
                || isTreasureChat(text);
    }

    public static boolean shouldHideDuplicate(
            boolean filterEnabled,
            String chat,
            String lastChat,
            long elapsedMs) {
        if (!filterEnabled || chat == null || lastChat == null) {
            return false;
        }
        if (elapsedMs > DUPLICATE_CHAT_MS) {
            return false;
        }
        String a = strip(chat);
        String b = strip(lastChat);
        return a.equals(b) && isBurrowRelatedChat(a);
    }

    public static boolean shouldMuteBuggedSpade(
            boolean enabled,
            boolean doingDiana,
            String soundName,
            float pitch,
            float volume,
            boolean locationZero) {
        if (!enabled || !doingDiana || soundName == null) {
            return false;
        }
        boolean isRealMusic = pitch == 1.0F && volume == 1.0F && locationZero;
        return soundName.toLowerCase(Locale.ROOT).startsWith("music") && !isRealMusic;
    }

    public static boolean isDoingDiana(
            List<String> tabOrScoreboardLines,
            boolean holdingSpade) {
        return holdingSpade
                && parseScoreboardBurrows(tabOrScoreboardLines).isPresent();
    }

    public static boolean burrowIsStale(long nowMs, long lastSeenMs) {
        return nowMs >= lastSeenMs
                && nowMs - lastSeenMs > BURROW_STALE_MS;
    }

    public static boolean shouldCancelBuggedSpadeClick(long lastTrailParticleAt, long nowMs) {
        return nowMs - lastTrailParticleAt < BUGGED_SPADE_CANCEL_MS;
    }

    public static boolean shouldWarnGriffin(
            boolean moduleEnabled,
            boolean warnEnabled,
            boolean holdingSpade,
            boolean hasGriffin,
            boolean alreadyCorrect) {
        return moduleEnabled && warnEnabled && holdingSpade && !hasGriffin && !alreadyCorrect;
    }

    public static boolean shouldPartyShare(boolean shareModule, boolean partyCheat, RareMob mob) {
        return shareModule && partyCheat && mob != null && mob.rareHighlight();
    }

    public static boolean shouldAutoWarp(boolean shareModule, boolean warpCheat) {
        return shareModule && warpCheat;
    }

    public static String partyShareLine(RareMob mob, int x, int y, int z) {
        String coords = "x: " + x + ", y: " + y + ", z: " + z;
        if (mob == null) {
            return coords;
        }
        return coords + " | " + mob.display();
    }

    public static Optional<BlockGuess> parseSharedCoords(String chat) {
        Matcher matcher = PARTY_COORDS.matcher(strip(chat));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new BlockGuess(
                (int) Math.round(Double.parseDouble(matcher.group("x"))),
                (int) Math.round(Double.parseDouble(matcher.group("y"))),
                (int) Math.round(Double.parseDouble(matcher.group("z")))));
    }

    public static Optional<WarpPoint> nearestWarp(Point target, Point player) {
        if (target == null || player == null) {
            return Optional.empty();
        }
        double bestPlayer = player.distanceTo(target);
        WarpPoint best = null;
        double bestWarp = bestPlayer;
        for (WarpPoint warp : WARPS) {
            double dist = warp.distanceTo(target);
            if (dist < bestWarp) {
                bestWarp = dist;
                best = warp;
            }
        }
        return Optional.ofNullable(best);
    }

    public static List<WarpPoint> warps() {
        return WARPS;
    }

    public static List<String> hudLines(
            boolean burrows,
            BlockGuess guess,
            WarpPoint warp,
            DugChat chain,
            boolean profit,
            Map<String, Integer> drops,
            long coins,
            int inquisitors) {
        List<String> lines = new ArrayList<>();
        if (burrows) {
            if (chain != null && chain.max() > 0) {
                lines.add("Burrows " + chain.current() + "/" + chain.max()
                        + " left " + chain.remaining());
            }
            if (guess != null) {
                lines.add(guess.hud());
            }
            if (warp != null) {
                lines.add("Warp " + warp.name());
            }
        }
        if (profit) {
            lines.add("Diana profit");
            if (inquisitors > 0) {
                lines.add("Inquisitor: " + inquisitors);
            }
            if (coins > 0) {
                lines.add("Coins: " + coins);
            }
            if (drops != null) {
                for (Map.Entry<String, Integer> entry : drops.entrySet()) {
                    if (entry.getValue() > 0) {
                        lines.add(entry.getKey() + ": " + entry.getValue());
                    }
                }
            }
        }
        return lines;
    }

    public static Map<String, Integer> applyDrop(Map<String, Integer> current, Drop drop) {
        Map<String, Integer> next = current == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(current);
        if (drop == null) {
            return next;
        }
        next.merge(drop.name(), drop.amount(), Integer::sum);
        return next;
    }

    public static String burrowLabel(BurrowType type) {
        return switch (type) {
            case START -> "Start";
            case MOB -> "Mob";
            case TREASURE -> "Treasure";
            case GUESS -> "Guess";
            default -> "Burrow";
        };
    }

    static double evalCubic(double[] c, double t) {
        return c[0] + c[1] * t + c[2] * t * t + c[3] * t * t * t;
    }

    private static String normalizeType(String typeId) {
        String id = typeId.toLowerCase(Locale.ROOT);
        int slash = id.indexOf(':');
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private static float round2(float value) {
        return Math.round(value * 100.0F) / 100.0F;
    }

    /**
     * Least-squares polynomial fit.
     */
    public static final class PolynomialFitter {
        private final int degree;
        private final List<List<Double>> xPointMatrix = new ArrayList<>();
        private final List<List<Double>> yPoints = new ArrayList<>();

        public PolynomialFitter(int degree) {
            this.degree = degree;
        }

        public void addPoint(double x, double y) {
            yPoints.add(List.of(y));
            List<Double> row = new ArrayList<>(degree + 1);
            for (int i = 0; i <= degree; i++) {
                row.add(Math.pow(x, i));
            }
            xPointMatrix.add(row);
        }

        public double[] fit() {
            Matrix xMatrix = new Matrix(xPointMatrix);
            Matrix yMatrix = new Matrix(yPoints);
            Matrix coeffs = xMatrix.transpose()
                    .multiply(xMatrix)
                    .inverse()
                    .multiply(xMatrix.transpose())
                    .multiply(yMatrix);
            List<Double> row = coeffs.transpose().data().get(0);
            double[] out = new double[row.size()];
            for (int i = 0; i < row.size(); i++) {
                out[i] = row.get(i);
            }
            return out;
        }
    }

    static final class Matrix {
        private final List<List<Double>> data;
        private final int rows;
        private final int cols;

        Matrix(List<List<Double>> data) {
            this.data = data;
            this.rows = data.size();
            this.cols = data.isEmpty() ? 0 : data.get(0).size();
        }

        List<List<Double>> data() {
            return data;
        }

        Matrix transpose() {
            List<List<Double>> result = new ArrayList<>();
            for (int j = 0; j < cols; j++) {
                List<Double> row = new ArrayList<>();
                for (int i = 0; i < rows; i++) {
                    row.add(data.get(i).get(j));
                }
                result.add(row);
            }
            return new Matrix(result);
        }

        Matrix multiply(Matrix other) {
            if (cols != other.rows) {
                throw new IllegalArgumentException("Matrix dimensions do not match");
            }
            List<List<Double>> result = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < other.cols; j++) {
                    double sum = 0.0;
                    for (int k = 0; k < cols; k++) {
                        sum += data.get(i).get(k) * other.data.get(k).get(j);
                    }
                    row.add(sum);
                }
                result.add(row);
            }
            return new Matrix(result);
        }

        Matrix inverse() {
            if (rows != cols) {
                throw new IllegalArgumentException("Only square matrices can be inverted");
            }
            int n = rows;
            double[][] augmented = new double[n][2 * n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    augmented[i][j] = data.get(i).get(j);
                }
                for (int j = 0; j < n; j++) {
                    augmented[i][j + n] = i == j ? 1.0 : 0.0;
                }
            }
            for (int i = 0; i < n; i++) {
                int maxRow = i;
                for (int k = i + 1; k < n; k++) {
                    if (Math.abs(augmented[k][i]) > Math.abs(augmented[maxRow][i])) {
                        maxRow = k;
                    }
                }
                double[] temp = augmented[i];
                augmented[i] = augmented[maxRow];
                augmented[maxRow] = temp;
                if (Math.abs(augmented[i][i]) < 1.0E-12) {
                    throw new IllegalArgumentException("Matrix is singular");
                }
                double pivot = augmented[i][i];
                for (int j = 0; j < 2 * n; j++) {
                    augmented[i][j] /= pivot;
                }
                for (int k = 0; k < n; k++) {
                    if (k == i) {
                        continue;
                    }
                    double factor = augmented[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        augmented[k][j] -= factor * augmented[i][j];
                    }
                }
            }
            List<List<Double>> inv = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < n; j++) {
                    row.add(augmented[i][j + n]);
                }
                inv.add(row);
            }
            return new Matrix(inv);
        }
    }
}
