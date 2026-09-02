package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure decisions for the Kuudra Tools 3D overlay: supply crates, supply piles,
 * pearl waypoints, build markers, stun pods, ichor pools, the Kuudra hitbox,
 * the giant supply-hitbox alert, and the etherwarp highlights. Minecraft
 * drawing lives in the client runtime; positions come from bundled reference
 * JSON.
 */
public final class IotaKuudraPolicy {
    public static final String STUN_LEFT = "LEFT_POD";
    public static final String STUN_RIGHT = "RIGHT_POD";
    public static final String STUN_BACK = "BACK_POD";
    public static final List<String> STUN_POD_OPTIONS = List.of(STUN_LEFT, STUN_RIGHT, STUN_BACK);
    public static final double CRATE_OFFSET = 37.0D / 10.0D;
    public static final double ANGLE_OFFSET = 90.0D + 40.0D;
    public static final double CRATE_Y = 60.0D + 15.0D;
    public static final double SUPPLY_CARRIER_MAX_Y = 67.0D;
    public static final float SUPPLY_BOX_SIZE = 1.0F;
    public static final int SUPPLY_BEAM_HEIGHT = 100;
    public static final double SUPPLY_PULL_RADIUS = 5.0D;
    public static final int SUPPLY_PULL_SEGMENTS = 60;
    public static final double SUPPLY_VERTICAL_MARGIN = 4.0D;
    public static final double SUPPLY_ZOMBIE_RANGE_SQ = 3.0D * 3.0D;
    public static final double SUPPLY_ZOMBIE_GREEN_DIST = 3.0D;
    public static final int SUPPLY_COLOR = new java.awt.Color(0, 255, 255, 40).getRGB();
    public static final int PILE_NORMAL_COLOR = new java.awt.Color(255, 255, 255, 52).getRGB();
    public static final int PILE_NO_PRE_COLOR = new java.awt.Color(0, 255, 144, 50).getRGB();
    public static final double PILE_RADIUS_SQUARED = 2.25D;
    public static final double PILE_Y_SLACK = 5.0D;
    public static final int PILE_BEAM_HEIGHT = 40;
    public static final double PILE_NAME_Y = 2.5D;
    public static final int PEARL_DEFAULT_COLOR = new java.awt.Color(0, 255, 255, 110).getRGB();
    public static final int PEARL_SIZE_ADJUSTMENT = -3;
    public static final float PEARL_DEFAULT_SIZE = 0.4F;
    public static final int PEARL_SOLID_OPACITY = 80;
    public static final int SUPPLY_STEP_TIME_MS = 300;
    public static final List<Integer> SUPPLY_TICK_PERCENTAGES =
            List.of(5, 11, 17, 23, 29, 35, 41, 47, 53, 59, 65, 71, 77, 83, 89, 95, 100);
    public static final Pattern SUPPLY_PROGRESS_PATTERN = Pattern.compile("^\\[[| ]+]\\s*(\\d+)%$");
    public static final Pattern SUPPLY_PLACE_PATTERN = Pattern.compile("(.+?) recovered.*?\\((\\d)/6\\)");
    public static final Pattern BUILD_PROGRESS_PATTERN = Pattern.compile("PROGRESS:\\s*(?:§.)?(\\d+)%");
    public static final Pattern ICHOR_LOCATION_PATTERN = Pattern.compile(
            "(?i).*(?:\\bcasting(?:\\s+spell:)?\\s+ichor\\s+pool!?|\\bichor\\s+pool\\s+casted(?:!|\\b))"
                    + "\\s*(?:at|@)?\\s*\\(?\\s*(-?\\d+)\\s*[, ]+\\s*(-?\\d+)\\s*[, ]+\\s*(-?\\d+)\\s*\\)?.*");
    public static final Pattern SCOREBOARD_SUPPLIES = Pattern.compile("Rescue supplies");
    public static final Pattern SCOREBOARD_BUILD = Pattern.compile("Protect Elle\\s*\\((\\d+)%\\)");
    public static final int CHEST_BUY_SLOT = 31;
    public static final int CHEST_INFO_SLOT = 49;
    public static final int CHEST_REROLL_SLOT = 50;
    public static final int CHEST_SHARD_REROLL_SLOT = 51;
    public static final int CHEST_LOOT_FIRST = 9;
    public static final int CHEST_LOOT_LAST = 17;
    public static final long SESSION_TIMEOUT_MS = 21L * 60L * 1_000L;
    public static final long SESSION_WARNING_INTERVAL_MS = 5L * 60L * 1_000L;
    private static final Pattern CHEST_COINS = Pattern.compile("(?i)([\\d,]+)\\s+coins");
    private static final Pattern CHEST_COST = Pattern.compile("(?i)(?:cost|price)\\s*:?\\s*([\\d,]+)\\s+coins");
    public static final double STUN_ENTER_X = -161.0D;
    public static final double STUN_ENTER_Y = 49.0D;
    public static final double STUN_ENTER_Z = -186.0D;
    public static final double STUN_EATEN_Y = 50.0D;
    public static final int STUN_COLOR = new java.awt.Color(0, 245, 255, 200).getRGB();
    public static final int KUUDRA_HITBOX_COLOR = new java.awt.Color(255, 2, 2, 231).getRGB();
    public static final int KUUDRA_SIZE = 30;
    public static final float KUUDRA_MIN_MAX_HEALTH = 10000.0F;
    public static final float ICHOR_RADIUS = 8.0F;
    public static final long ICHOR_DURATION_MS = 20000L;
    public static final int ICHOR_SEGMENTS = 64;
    public static final int ICHOR_COLOR = new java.awt.Color(96, 238, 255, 210).getRGB();
    public static final int BUILD_BEAM_HEIGHT = 25;
    public static final float BUILD_OPACITY = 0.3F;
    public static final int BUILD_0_20 = rgb(168, 0, 0);
    public static final int BUILD_21_40 = rgb(255, 0, 0);
    public static final int BUILD_41_60 = rgb(255, 135, 0);
    public static final int BUILD_61_80 = rgb(46, 130, 0);
    public static final int BUILD_81_100 = rgb(125, 218, 88);
    public static final int GIANT_PRIMARY_FILL = new java.awt.Color(255, 0, 0, 120).getRGB();
    public static final int GIANT_PRIMARY_OUTLINE = new java.awt.Color(255, 80, 80, 255).getRGB();
    public static final int GIANT_SECONDARY_FILL = new java.awt.Color(255, 255, 255, 70).getRGB();
    public static final int GIANT_SECONDARY_OUTLINE = new java.awt.Color(255, 255, 255, 230).getRGB();
    public static final double GIANT_ALERT_EXPAND = 0.08D;
    public static final double GIANT_OUTLINE_STEP = 0.03D;
    public static final int GIANT_PRIMARY_LAYERS = 3;
    public static final int GIANT_SECONDARY_LAYERS = 2;
    public static final String DOUBLE_PEARL_TITLE = "§c§lDOUBLE PEARL";
    public static final int GREEN = 0xFF55FF55;
    public static final double ETHERWARP_OUTLINE_STEP = 0.0035D;
    public static final float ETHERWARP_DEFAULT_ALPHA = 0.8F;

    public enum Phase {
        NONE,
        SUPPLIES,
        BUILD,
        EATEN,
        STUN,
        DPS,
        SKIP,
        BOSS,
        COMPLETED;

        boolean isRun() {
            return this != NONE && this != COMPLETED;
        }
    }

    public enum ChestKind {
        NONE,
        FREE,
        PAID
    }

    public enum RerollKind {
        NONE,
        ITEMS,
        SHARD
    }

    public record SessionExpire(
            Kind kind,
            long lastActivityAt,
            long lastWarningAt,
            long idleMinutes,
            long remainingMinutes) {
        public enum Kind {
            NONE,
            WARN,
            RESET
        }

        static SessionExpire none(long lastActivityAt, long lastWarningAt) {
            return new SessionExpire(Kind.NONE, lastActivityAt, lastWarningAt, 0L, 0L);
        }
    }

    public enum SpawnDirection {
        RIGHT,
        FRONT,
        LEFT,
        BACK,
        UNKNOWN
    }

    public enum HighlightShape {
        FULL,
        TOP,
        BOTTOM,
        SLAB_LOWER,
        SLAB_UPPER,
        HALF_LOWER,
        HALF_UPPER,
        CENTER_PLATE,
        EDGE_TOP,
        EDGE_BOTTOM,
        PILLAR,
        CUSTOM
    }

    public enum AlertLevel {
        NONE,
        SECONDARY,
        PRIMARY
    }

    public record Vec3d(double x, double y, double z) {
        public Vec3d add(Vec3d other) {
            return new Vec3d(x + other.x, y + other.y, z + other.z);
        }

        public Vec3d subtract(Vec3d other) {
            return new Vec3d(x - other.x, y - other.y, z - other.z);
        }

        public double distanceToSqr(Vec3d other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    public record Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Aabb inflate(double amount) {
            return new Aabb(
                    minX - amount, minY - amount, minZ - amount,
                    maxX + amount, maxY + amount, maxZ + amount);
        }

        public boolean contains(double x, double y, double z) {
            return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
        }

        public boolean intersects(
                double otherMinX, double otherMinY, double otherMinZ,
                double otherMaxX, double otherMaxY, double otherMaxZ) {
            return minX < otherMaxX && maxX > otherMinX
                    && minY < otherMaxY && maxY > otherMinY
                    && minZ < otherMaxZ && maxZ > otherMinZ;
        }
    }

    public record SupplyCrate(Vec3d position, float carrierYaw, int entityId) {
    }

    public record Pile(String name, Vec3d position, int noPreValue) {
        public boolean isNoPrePile(int missingPre) {
            return noPreValue == missingPre;
        }

        public boolean isNearby(Vec3d pos) {
            double dx = position.x - pos.x;
            double dz = position.z - pos.z;
            return dx * dx + dz * dz <= PILE_RADIUS_SQUARED
                    && Math.abs(position.y - pos.y) <= PILE_Y_SLACK;
        }
    }

    public record PearlWaypoint(
            Vec3d target,
            int colorRgb,
            Vec3d standBlock,
            Integer preSupply,
            Integer hideForPre,
            float size,
            String label,
            boolean alert) {
        public boolean shouldShow(int missingPre) {
            if (hideForPre != null && hideForPre == missingPre) {
                return false;
            }
            return preSupply == null || missingPre <= 0 || preSupply == missingPre;
        }

        public boolean hasStandBlock() {
            return standBlock != null
                    && (standBlock.x != 0.0D || standBlock.y != 0.0D || standBlock.z != 0.0D);
        }
    }

    public record PearlArea(
            String name,
            double minX,
            double minZ,
            double maxX,
            double maxZ,
            Boolean invertForwardBackward,
            Boolean invertLeftRight,
            List<PearlWaypoint> waypoints) {
        public boolean containsPlayer(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    public record EtherWaypoint(
            String name,
            List<Vec3d> positions,
            int colorRgb,
            List<Integer> colorsRgb,
            float alpha,
            String renderStyle,
            float lineWidth,
            Set<Phase> showInPhases,
            Set<Phase> hideInPhases,
            float maxRenderDistance,
            HighlightShape shape,
            Vec3d boxMin,
            Vec3d boxMax) {
        public boolean shouldShowInPhase(Phase phase) {
            return !hideInPhases.contains(phase) && showInPhases.contains(phase);
        }

        public boolean valid() {
            return name != null && !name.isBlank() && !positions.isEmpty() && !showInPhases.isEmpty();
        }

        public int colorForIndex(int index) {
            if (colorsRgb == null || colorsRgb.isEmpty()) {
                return colorRgb;
            }
            return index < colorsRgb.size() ? colorsRgb.get(index) : colorsRgb.get(colorsRgb.size() - 1);
        }
    }

    public record EtherCategory(String name, boolean enabled, List<EtherWaypoint> waypoints) {
    }

    public record IchorParse(int x, int y, int z) {
    }

    public record NoPreCall(int missingPreValue, String canonicalPileName) {
    }

    public record PreSpot(
            String displayName,
            Vec3d location,
            String secondaryName,
            Vec3d secondaryLocation,
            double detectionRadius,
            int missingPreValue,
            int secondaryMissingValue) {
        public boolean isPlayerNearby(Vec3d playerPos) {
            return playerPos.distanceToSqr(location) < detectionRadius * detectionRadius;
        }
    }

    private static final Pattern CONTROL = Pattern.compile("§.");
    private static final Map<String, String> NO_PRE_ALIASES = Map.ofEntries(
            Map.entry("triangle", "TRIANGLE"),
            Map.entry("tri", "TRIANGLE"),
            Map.entry("x", "X"),
            Map.entry("xc", "X CANNON"),
            Map.entry("xcannon", "X CANNON"),
            Map.entry("x cannon", "X CANNON"),
            Map.entry("equals", "EQUALS"),
            Map.entry("eq", "EQUALS"),
            Map.entry("slash", "SLASH"),
            Map.entry("shop", "SHOP"),
            Map.entry("square", "SQUARE"));
    private static final List<AliasRule> NO_PRE_RULES;
    private static final List<PreSpot> PRE_SPOTS = List.of(
            new PreSpot("Triangle", new Vec3d(-67.5D, 77.0D, -122.5D), "Shop",
                    new Vec3d(-81.0D, 76.0D, -143.0D), 15.0D, 6, 7),
            new PreSpot("X", new Vec3d(-142.5D, 77.0D, -151.0D), "X Cannon",
                    new Vec3d(-143.0D, 76.0D, -125.0D), 30.0D, 1, 2),
            new PreSpot("Equals", new Vec3d(-65.5D, 76.0D, -87.5D), null, null, 15.0D, 5, -1),
            new PreSpot("Slash", new Vec3d(-113.5D, 77.0D, -68.5D), "Square",
                    new Vec3d(-143.0D, 76.0D, -80.0D), 15.0D, 4, 3));

    private record AliasRule(String alias, String canonical, Pattern pattern) {
    }

    static {
        List<AliasRule> rules = new ArrayList<>();
        for (Map.Entry<String, String> entry : NO_PRE_ALIASES.entrySet()) {
            rules.add(new AliasRule(
                    entry.getKey(),
                    entry.getValue(),
                    Pattern.compile("\\b(?:no|missing)\\s+" + Pattern.quote(entry.getKey()) + "\\b",
                            Pattern.CASE_INSENSITIVE)));
        }
        rules.sort(Comparator.comparingInt((AliasRule rule) -> rule.alias().length()).reversed());
        NO_PRE_RULES = List.copyOf(rules);
    }

    private IotaKuudraPolicy() {
    }

    public static String normalizeStunPod(String value) {
        if (value == null) {
            return STUN_LEFT;
        }
        String key = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (key) {
            case "RIGHT", "RIGHT_POD" -> STUN_RIGHT;
            case "BACK", "BACK_POD" -> STUN_BACK;
            default -> STUN_LEFT;
        };
    }

    public static Vec3d stunPod(String value) {
        return switch (normalizeStunPod(value)) {
            case STUN_RIGHT -> new Vec3d(-167.5D, 28.0D, -167.5D);
            case STUN_BACK -> new Vec3d(-154.5D, 28.0D, -156.5D);
            default -> new Vec3d(-152.5D, 27.0D, -172.5D);
        };
    }

    public static Phase phaseFromChat(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        if (message.contains("Sending to server") || message.contains("Starting in 4 seconds...")) {
            return Phase.NONE;
        }
        if (message.contains("[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!")) {
            return Phase.SUPPLIES;
        }
        if (message.contains("[NPC] Elle: OMG! Great work collecting my supplies!")) {
            return Phase.BUILD;
        }
        if (message.contains("[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!")) {
            return Phase.EATEN;
        }
        if (message.contains("has been eaten by Kuudra!") && !message.contains("Elle")) {
            return Phase.STUN;
        }
        if (message.contains("destroyed one of Kuudra's pods!")) {
            return Phase.DPS;
        }
        if (message.contains("[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!")) {
            return Phase.SKIP;
        }
        if (message.contains("KUUDRA DOWN!")) {
            return Phase.COMPLETED;
        }
        return null;
    }

    public static Optional<Phase> phaseFromScoreboard(String sidebar) {
        if (sidebar == null || sidebar.isBlank()) {
            return Optional.empty();
        }
        for (String line : sidebar.split("\\R")) {
            String plain = CONTROL.matcher(line).replaceAll("");
            if (SCOREBOARD_SUPPLIES.matcher(plain).find()) {
                return Optional.of(Phase.SUPPLIES);
            }
            if (SCOREBOARD_BUILD.matcher(plain).find()) {
                return Optional.of(Phase.BUILD);
            }
        }
        return Optional.empty();
    }

    public static boolean isDefeat(String message) {
        if (message == null) {
            return false;
        }
        return "DEFEAT".equals(CONTROL.matcher(message).replaceAll("").trim());
    }

    public static ChestKind chestKind(String title) {
        if (title == null) {
            return ChestKind.NONE;
        }
        String plain = CONTROL.matcher(title).replaceAll("").toLowerCase(Locale.ROOT);
        if (plain.contains("free chest")) {
            return ChestKind.FREE;
        }
        if (plain.contains("paid chest")) {
            return ChestKind.PAID;
        }
        return ChestKind.NONE;
    }

    public static Optional<ChestKind> chestReward(String message) {
        if (message == null) {
            return Optional.empty();
        }
        String plain = CONTROL.matcher(message).replaceAll("").toUpperCase(Locale.ROOT);
        if (plain.contains("PAID CHEST REWARDS")) {
            return Optional.of(ChestKind.PAID);
        }
        if (plain.contains("FREE CHEST REWARDS")) {
            return Optional.of(ChestKind.FREE);
        }
        return Optional.empty();
    }

    public static boolean isBuySlot(int index) {
        return index == CHEST_BUY_SLOT;
    }

    public static boolean isItemRerollSlot(int index) {
        return index == CHEST_REROLL_SLOT;
    }

    public static boolean isShardRerollSlot(int index) {
        return index == CHEST_SHARD_REROLL_SLOT;
    }

    public static boolean canUseReroll(String hover, String lore, String blockedPhrase) {
        String name = CONTROL.matcher(hover == null ? "" : hover).replaceAll("").toLowerCase(Locale.ROOT);
        if (!name.contains("reroll")) {
            return false;
        }
        String blocked = blockedPhrase == null ? "" : blockedPhrase.toLowerCase(Locale.ROOT);
        if (blocked.isBlank()) {
            return true;
        }
        String text = CONTROL.matcher(lore == null ? "" : lore).replaceAll("").toLowerCase(Locale.ROOT);
        return !text.contains(blocked);
    }

    public static RerollKind rerollKind(int slotIndex, String hover, String lore) {
        if (isItemRerollSlot(slotIndex) && canUseReroll(hover, lore, "rerolled this chest")) {
            return RerollKind.ITEMS;
        }
        if (isShardRerollSlot(slotIndex) && canUseReroll(hover, lore, "rerolled this shard")) {
            return RerollKind.SHARD;
        }
        return RerollKind.NONE;
    }

    public static long rerollCost(String lore) {
        long cost = keyCost(lore);
        return cost > 0L ? cost : loreCoins(lore);
    }

    public static boolean alreadyOpened(String lore) {
        if (lore == null) {
            return false;
        }
        String plain = CONTROL.matcher(lore).replaceAll("");
        return plain.contains("Already opened!")
                || plain.contains("Chest already opened!")
                || plain.contains("You have already opened a chest!");
    }

    public static boolean isBuyAction(String lore) {
        if (lore == null || lore.isBlank()) {
            return true;
        }
        return CONTROL.matcher(lore).replaceAll("").contains("Click to open!");
    }

    public static long loreCoins(String lore) {
        if (lore == null || lore.isBlank()) {
            return 0L;
        }
        long coins = 0L;
        String plain = CONTROL.matcher(lore).replaceAll("");
        Matcher matcher = CHEST_COINS.matcher(plain);
        while (matcher.find()) {
            coins = Math.max(coins, parseCoins(matcher.group(1)));
        }
        return coins;
    }

    public static long keyCost(String lore) {
        if (lore == null || lore.isBlank()) {
            return 0L;
        }
        String plain = CONTROL.matcher(lore).replaceAll("");
        Matcher matcher = CHEST_COST.matcher(plain);
        long cost = 0L;
        while (matcher.find()) {
            cost = Math.max(cost, parseCoins(matcher.group(1)));
        }
        return cost;
    }

    public static long netProfit(ChestKind kind, long lootCoins, long costCoins) {
        long loot = Math.max(0L, lootCoins);
        long cost = kind == ChestKind.PAID ? Math.max(0L, costCoins) : 0L;
        return loot - cost;
    }

    public static int decrementChests(int current) {
        return Math.max(0, current - 1);
    }

    public static double nextAverageSeconds(double previousAvg, int previousCompleted, long durationMs) {
        if (durationMs <= 0L) {
            return Math.max(0.0D, previousAvg);
        }
        double seconds = durationMs / 1000.0D;
        if (previousCompleted <= 0) {
            return seconds;
        }
        return ((Math.max(0.0D, previousAvg) * previousCompleted) + seconds)
                / (previousCompleted + 1.0D);
    }

    public static long hourlyRate(long profitCoins, long durationMs) {
        if (durationMs <= 0L) {
            return 0L;
        }
        return Math.round(profitCoins * (3_600_000.0D / durationMs));
    }

    /**
     * Iota profit-session idle rules: warn every 5 minutes after 5 idle minutes,
     * then reset session profit/runs after 21 minutes. Chest-limit count stays.
     */
    public static SessionExpire expireSession(long now, long lastActivityAt, long lastWarningAt) {
        if (lastActivityAt <= 0L) {
            return SessionExpire.none(lastActivityAt, lastWarningAt);
        }
        long idle = Math.max(0L, now - lastActivityAt);
        if (idle > SESSION_TIMEOUT_MS) {
            return new SessionExpire(SessionExpire.Kind.RESET, 0L, 0L, idle / 60_000L, 0L);
        }
        if (idle > SESSION_WARNING_INTERVAL_MS) {
            long sinceWarn = lastWarningAt <= 0L ? Long.MAX_VALUE : now - lastWarningAt;
            if (lastWarningAt <= 0L || sinceWarn >= SESSION_WARNING_INTERVAL_MS) {
                long remaining = (SESSION_TIMEOUT_MS - idle) / 60_000L;
                return new SessionExpire(
                        SessionExpire.Kind.WARN, lastActivityAt, now, idle / 60_000L, remaining);
            }
        }
        return SessionExpire.none(lastActivityAt, lastWarningAt);
    }

    public static String sessionResetMessage() {
        return "§8[§ePROFIT TRACKER§8] §fSession data has been reset after 20 minutes of inactivity.";
    }

    public static String sessionWarningMessage(long idleMinutes, long remainingMinutes) {
        return "§8[§ePROFIT TRACKER§8] §fNo runs in " + idleMinutes
                + " minutes. Session resets in " + remainingMinutes + " minutes.";
    }

    private static long parseCoins(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.replace(",", "").trim());
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public static boolean isKuudraArea(String sidebar) {
        if (sidebar == null || sidebar.isBlank()) {
            return false;
        }
        String plain = CONTROL.matcher(sidebar).replaceAll("").replace('’', '\'');
        return plain.contains("Kuudra's Hollow");
    }

    public static boolean isInstanceTransfer(String message) {
        return message != null
                && (message.contains("Sending to server")
                || (message.contains("Starting in ") && message.contains(" seconds")));
    }

    public static boolean isSupplyPickup(String message) {
        return message != null && message.contains("You retrieved some of Elle's supplies from the Lava!");
    }

    public static boolean isLocalSupplyDrop(String message) {
        return message != null && message.contains("You moved and the Chest slipped out of your hands!");
    }

    public static boolean isHumanCannonball(String message) {
        return message != null && message.contains("You purchased Human Cannonball!");
    }

    public static boolean isPodDestroyed(String message) {
        return message != null && message.contains("destroyed one of Kuudra's pods!");
    }

    public static Optional<Integer> supplyProgress(String stripped) {
        if (stripped == null) {
            return Optional.empty();
        }
        Matcher matcher = SUPPLY_PROGRESS_PATTERN.matcher(CONTROL.matcher(stripped).replaceAll("").trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(matcher.group(1)));
    }

    public static SupplyCrate crateFromGiant(double giantX, double giantZ, float yaw, int entityId) {
        double heading = Math.toRadians(yaw) + Math.toRadians(ANGLE_OFFSET);
        double reach = CRATE_OFFSET;
        return new SupplyCrate(
                new Vec3d(
                        giantX + reach * Math.cos(heading),
                        CRATE_Y,
                        giantZ + reach * Math.sin(heading)),
                yaw,
                entityId);
    }

    public static Aabb supplyBox(Vec3d position, float boxSize) {
        double half = boxSize / 2.0D;
        return new Aabb(
                position.x + 0.5D - half,
                position.y - 1.0D,
                position.z + 1.5D - half,
                position.x + 0.5D + half,
                position.y,
                position.z + 1.5D + half);
    }

    public static Vec3d supplyPullCenter(Vec3d position) {
        return new Vec3d(position.x + 0.5D, position.y, position.z + 1.5D);
    }

    public static boolean bobberInsidePull(Vec3d crate, double bobberX, double bobberY, double bobberZ) {
        Vec3d center = supplyPullCenter(crate);
        double dx = bobberX - center.x;
        double dz = bobberZ - center.z;
        double radius = SUPPLY_PULL_RADIUS;
        if (dx * dx + dz * dz > radius * radius) {
            return false;
        }
        return bobberY >= (crate.y - 1.0D) - SUPPLY_VERTICAL_MARGIN
                && bobberY <= crate.y + SUPPLY_VERTICAL_MARGIN;
    }

    public static Aabb unitCubeFromLowerCorner(Vec3d position) {
        return new Aabb(position.x, position.y, position.z,
                position.x + 1.0D, position.y + 1.0D, position.z + 1.0D);
    }

    public static Aabb pearlBox(Vec3d target, float waypointSize, int sizeAdjustment) {
        int clamped = Math.clamp(sizeAdjustment, -5, 5);
        double scale = 1.0D + clamped * 0.1D;
        float size = (float) Math.max(0.05D, waypointSize * scale);
        float half = size / 2.0F;
        return new Aabb(
                target.x - half - 0.5D,
                target.y,
                target.z - half - 0.5D,
                target.x + half - 0.5D,
                target.y + size,
                target.z + half - 0.5D);
    }

    public static Vec3d dynamicPearlTarget(
            Vec3d jsonTarget,
            Vec3d player,
            Vec3d standCenter,
            Boolean invertForwardBackward,
            Boolean invertLeftRight) {
        if (standCenter == null || player == null) {
            return jsonTarget;
        }
        Vec3d offset = player.subtract(standCenter);
        double heightAdjustment = 0.0D;
        if (invertForwardBackward != null) {
            double sign = invertForwardBackward ? -1.0D : 1.0D;
            heightAdjustment = offset.z * 0.31D * sign;
        }
        double leftRight = 0.0D;
        if (invertLeftRight != null) {
            double sign = invertLeftRight ? -1.0D : 1.0D;
            leftRight = -offset.x * 0.63D * sign;
        }
        return jsonTarget.subtract(new Vec3d(0.0D, offset.y * 0.81D + heightAdjustment + leftRight, 0.0D));
    }

    public static boolean skipPearlTarget(Vec3d target) {
        return target.x == 0.0D && target.y == 0.0D && target.z == 0.0D;
    }

    public static int progressIndex(int progress) {
        if (progress <= 0) {
            return -1;
        }
        for (int i = 0; i < SUPPLY_TICK_PERCENTAGES.size(); i++) {
            if (SUPPLY_TICK_PERCENTAGES.get(i) >= progress) {
                return i;
            }
        }
        return SUPPLY_TICK_PERCENTAGES.size() - 1;
    }

    public static long targetTimeMs(int targetIndex) {
        return targetIndex * (long) SUPPLY_STEP_TIME_MS;
    }

    public static Optional<Integer> parseBuildProgress(String name) {
        if (name == null || !name.contains("PROGRESS:") || !name.contains("%")) {
            return Optional.empty();
        }
        String plain = CONTROL.matcher(name).replaceAll("");
        Matcher matcher = BUILD_PROGRESS_PATTERN.matcher(plain);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        String digits = plain.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(digits));
    }

    public static int buildColor(int progress) {
        if (progress <= 20) {
            return BUILD_0_20;
        }
        if (progress <= 40) {
            return BUILD_21_40;
        }
        if (progress <= 60) {
            return BUILD_41_60;
        }
        if (progress <= 80) {
            return BUILD_61_80;
        }
        return BUILD_81_100;
    }

    public static int withOpacity(int rgb, float opacityPercent) {
        float fraction = opacityPercent > 1.0F ? opacityPercent / 100.0F : opacityPercent;
        int alpha = Math.clamp(Math.round(fraction * 255.0F), 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    public static Vec3d stunWaypoint(Vec3d pod, Vec3d interpolatedPlayer, boolean stunPhase, boolean eaten) {
        if (!eaten && !stunPhase) {
            return null;
        }
        if (stunPhase) {
            return pod.add(interpolatedPlayer.subtract(new Vec3d(STUN_ENTER_X, STUN_ENTER_Y, STUN_ENTER_Z)));
        }
        return pod;
    }

    public static Aabb stunBox(Vec3d pos) {
        return new Aabb(pos.x - 0.5D, pos.y, pos.z - 0.5D, pos.x + 0.5D, pos.y + 1.0D, pos.z + 0.5D);
    }

    public static Optional<IchorParse> parseIchor(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = ICHOR_LOCATION_PATTERN.matcher(CONTROL.matcher(message).replaceAll(""));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new IchorParse(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))));
    }

    public static Vec3d ichorCenter(IchorParse parsed) {
        return new Vec3d(parsed.x() + 0.5D, parsed.y() + 0.05D, parsed.z() + 0.5D);
    }

    public static boolean isKuudraBoss(int size, float maxHealth, float health) {
        return size == KUUDRA_SIZE && maxHealth >= KUUDRA_MIN_MAX_HEALTH && health > 0.0F;
    }

    public static SpawnDirection spawnDirection(double x, double z) {
        if (x < -128.0D) {
            return SpawnDirection.RIGHT;
        }
        if (z > -84.0D) {
            return SpawnDirection.FRONT;
        }
        if (x > -72.0D) {
            return SpawnDirection.LEFT;
        }
        return z < -132.0D ? SpawnDirection.BACK : SpawnDirection.UNKNOWN;
    }

    public static AlertLevel giantAlert(boolean eyeInside, boolean playerIntersects) {
        if (eyeInside) {
            return AlertLevel.PRIMARY;
        }
        return playerIntersects ? AlertLevel.SECONDARY : AlertLevel.NONE;
    }

    public static Aabb interpolatedEntityBox(
            double x, double y, double z,
            double prevX, double prevY, double prevZ,
            float width, float height, float partialTick) {
        double ix = prevX + (x - prevX) * partialTick;
        double iy = prevY + (y - prevY) * partialTick;
        double iz = prevZ + (z - prevZ) * partialTick;
        float half = width / 2.0F;
        return new Aabb(ix - half, iy, iz - half, ix + half, iy + height, iz + half);
    }

    public static Optional<NoPreCall> parseNoPre(String message) {
        if (message == null || message.isBlank()) {
            return Optional.empty();
        }
        String normalized = CONTROL.matcher(message.toLowerCase(Locale.ROOT)).replaceAll("")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        for (AliasRule rule : NO_PRE_RULES) {
            if (rule.pattern().matcher(normalized).find()) {
                int value = missingPreFromPileName(rule.canonical());
                if (value > 0) {
                    return Optional.of(new NoPreCall(value, rule.canonical()));
                }
            }
        }
        return Optional.empty();
    }

    public static int missingPreFromPileName(String pileName) {
        if (pileName == null) {
            return 0;
        }
        return switch (pileName.toLowerCase(Locale.ROOT).trim()) {
            case "triangle", "tri" -> 6;
            case "x" -> 1;
            case "x cannon", "xc", "xcannon" -> 2;
            case "equals", "eq" -> 5;
            case "slash" -> 4;
            case "shop" -> 7;
            case "square" -> 3;
            default -> 0;
        };
    }

    public static Optional<PreSpot> preSpotFromPlayer(Vec3d playerPos) {
        for (PreSpot spot : PRE_SPOTS) {
            if (spot.isPlayerNearby(playerPos)) {
                return Optional.of(spot);
            }
        }
        return Optional.empty();
    }

    public static Aabb etherwarpBox(HighlightShape shape, Vec3d center, Vec3d customMin, Vec3d customMax) {
        return switch (shape) {
            case FULL -> new Aabb(center.x - 0.5D, center.y, center.z - 0.5D,
                    center.x + 0.5D, center.y + 1.0D, center.z + 0.5D);
            case TOP -> new Aabb(center.x - 0.5D, center.y + 0.875D, center.z - 0.5D,
                    center.x + 0.5D, center.y + 1.0D, center.z + 0.5D);
            case BOTTOM -> new Aabb(center.x - 0.5D, center.y, center.z - 0.5D,
                    center.x + 0.5D, center.y + 0.125D, center.z + 0.5D);
            case SLAB_LOWER, HALF_LOWER -> new Aabb(center.x - 0.5D, center.y, center.z - 0.5D,
                    center.x + 0.5D, center.y + 0.5D, center.z + 0.5D);
            case SLAB_UPPER, HALF_UPPER -> new Aabb(center.x - 0.5D, center.y + 0.5D, center.z - 0.5D,
                    center.x + 0.5D, center.y + 1.0D, center.z + 0.5D);
            case CENTER_PLATE -> new Aabb(center.x - 0.25D, center.y + 0.45D, center.z - 0.25D,
                    center.x + 0.25D, center.y + 0.55D, center.z + 0.25D);
            case EDGE_TOP -> new Aabb(center.x - 0.5D, center.y + 0.95D, center.z - 0.5D,
                    center.x + 0.5D, center.y + 1.0D, center.z + 0.5D);
            case EDGE_BOTTOM -> new Aabb(center.x - 0.5D, center.y, center.z - 0.5D,
                    center.x + 0.5D, center.y + 0.05D, center.z + 0.5D);
            case PILLAR -> new Aabb(center.x - 0.2D, center.y, center.z - 0.2D,
                    center.x + 0.2D, center.y + 1.0D, center.z + 0.2D);
            case CUSTOM -> {
                Vec3d min = customMin == null ? new Vec3d(-0.5D, 0.0D, -0.5D) : customMin;
                Vec3d max = customMax == null ? new Vec3d(0.5D, 1.0D, 0.5D) : customMax;
                yield new Aabb(
                        center.x + min.x, center.y + min.y, center.z + min.z,
                        center.x + max.x, center.y + max.y, center.z + max.z);
            }
        };
    }

    public static HighlightShape parseShape(String raw) {
        if (raw == null || raw.isBlank()) {
            return HighlightShape.FULL;
        }
        String key = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (key) {
            case "UPPER_SLAB" -> HighlightShape.SLAB_UPPER;
            case "LOWER_SLAB" -> HighlightShape.SLAB_LOWER;
            case "UPPER_HALF", "TOP_HALF" -> HighlightShape.HALF_UPPER;
            case "LOWER_HALF", "BOTTOM_HALF" -> HighlightShape.HALF_LOWER;
            case "PLATE", "CENTER" -> HighlightShape.CENTER_PLATE;
            case "TOP_EDGE" -> HighlightShape.EDGE_TOP;
            case "BOTTOM_EDGE" -> HighlightShape.EDGE_BOTTOM;
            default -> {
                try {
                    yield HighlightShape.valueOf(key);
                } catch (IllegalArgumentException ignored) {
                    yield HighlightShape.FULL;
                }
            }
        };
    }

    public static Set<Phase> parsePhases(List<String> names) {
        EnumSet<Phase> phases = EnumSet.noneOf(Phase.class);
        if (names == null) {
            return phases;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            try {
                phases.add(Phase.valueOf(name.trim()));
            } catch (IllegalArgumentException ignored) {
                // Iota skips unknown / empty hideInPhases entries.
            }
        }
        return phases;
    }

    public static boolean etherwarpVisibleInPhase(Phase phase) {
        return phase == Phase.SUPPLIES
                || phase == Phase.BUILD
                || phase == Phase.EATEN
                || phase == Phase.STUN
                || phase == Phase.DPS
                || phase == Phase.SKIP;
    }

    public static int outlineLayers(float lineWidth) {
        return Math.max(1, Math.round(lineWidth));
    }

    public static int applyAlpha(int rgb, float alpha) {
        int a = Math.clamp(Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    public static PearlArea findPearlArea(List<PearlArea> areas, PearlArea current, double x, double z) {
        if (current != null && current.containsPlayer(x, z)) {
            return current;
        }
        for (PearlArea area : areas) {
            if (area.containsPlayer(x, z)) {
                return area;
            }
        }
        return null;
    }

    private static int rgb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
