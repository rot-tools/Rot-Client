package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft-free rules for per-family Slayer fight markers: Yang Glyphs, Nukekubi
 * skulls, Revenant BOOM, Tarantula egg sacs, Sven pups, Vampire ichor/spring,
 * quest mismatch, and Maddox restart commands.
 */
public final class SlayerFightPolicy {
    public enum Marker {
        BEACON,
        NUKEKUBI,
        EGG_SAC,
        BOOM,
        PUP,
        BLOOD_ICHOR,
        KILLER_SPRING,
        TWINCLAWS,
        INVINCIBLE,
        FIRE_PILLAR
    }

    public enum VoidgloomPhase {
        UNKNOWN,
        HITS,
        LASER,
        BEACON,
        DPS
    }

    public record QuestRef(SlayerPolicy.SlayerType type, int tier) {
        public QuestRef {
            tier = Math.max(1, Math.min(5, tier));
        }
    }

    public static final long SITTING_BEACON_MILLIS = 5_000L;
    public static final long LASER_DURATION_MILLIS = 8_000L;
    public static final long LASER_REARM_MILLIS = 10_000L;
    public static final int YANG_GLYPH_BEAM_HEIGHT = 80;
    public static final int YANG_GLYPH_SIT_SCAN = 3;
    public static final int MIN_LINE_WIDTH = 1;
    public static final int MAX_LINE_WIDTH = 10;
    public static final int DEFAULT_LINE_WIDTH = 3;
    public static final int MAX_BEACON_PATH_POINTS = 80;
    /** Unique fragment of the Nukekubi skull texture payload. */
    static final String NUKEKUBI_TEXTURE_FRAGMENT = "ZWIwNzU5NGUyZGYy";
    private static final Pattern FORMAT = Pattern.compile("§[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);
    private static final Pattern SIDEBAR_QUEST = Pattern.compile(
            "(?i).*(revenant horror|tarantula broodfather|sven packmaster|voidgloom seraph|"
                    + "inferno demonlord|riftstalker bloodfiend|"
                    + "enderman slayer|zombie slayer|spider slayer|wolf slayer|"
                    + "blaze slayer|vampire slayer|"
                    + "enderman|zombie|spider|wolf|blaze|vampire)"
                    + "\\s*(?:t\\s*([1-5])|(iv|iii|ii|v|i))\\b.*");
    private static final Pattern POWER_ORB = Pattern.compile(
            "(?i).*(?:overflux|plasmaflux|mana\\s*flux|power\\s*orb|\\bradiant\\b).*");
    private static final Pattern VOIDGLOOM_HITS = Pattern.compile(
            "(?i).*(?:hits?:\\s*(\\d+)|\\b(\\d+)\\s+hits?\\b).*");
    private static final Pattern VOIDGLOOM_LASER = Pattern.compile("(?i).*(laser|immune).*");
    private static final Pattern VOIDGLOOM_BEACON = Pattern.compile("(?i).*(yang glyph|throw a beacon|destroy the beacon).*");
    private static final Pattern HEALTH_FRACTION = Pattern.compile(
            "(?i)(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?)\\s*([kmb])?\\s*/\\s*"
                    + "(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?)\\s*([kmb])?\\s*[❤♥]");
    private static final Pattern COMPACT_HEALTH = Pattern.compile(
            "(?i)(\\d{1,3}(?:,\\d{3})+|\\d+(?:\\.\\d+)?)\\s*([kmb])?\\s*[❤♥]");

    private SlayerFightPolicy() {}

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return FORMAT.matcher(raw).replaceAll("")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static Optional<Marker> markerFromStand(String hoverName, String helmetItemName) {
        String name = normalize(hoverName).toLowerCase(Locale.ROOT);
        String helmet = normalize(helmetItemName).toLowerCase(Locale.ROOT);
        if (isPowerOrbHologram(hoverName)) {
            return Optional.empty();
        }
        if (helmet.equals("beacon") || name.equals("beacon") || name.contains("yang glyph")) {
            return Optional.of(Marker.BEACON);
        }
        if (name.contains("nukekubi") || name.contains("nukekebi")) {
            return Optional.of(Marker.NUKEKUBI);
        }
        if (name.contains("egg sac")
                || helmet.contains("cobweb")
                || helmet.contains("web")
                || isEggHitsHologram(hoverName)) {
            return Optional.of(Marker.EGG_SAC);
        }
        if (isBoomHologram(hoverName) || name.contains("boom")) {
            return Optional.of(Marker.BOOM);
        }
        if (isPupName(name)) {
            return Optional.of(Marker.PUP);
        }
        if (name.contains("blood ichor")) {
            return Optional.of(Marker.BLOOD_ICHOR);
        }
        if (name.contains("killer spring")) {
            return Optional.of(Marker.KILLER_SPRING);
        }
        if (name.contains("twinclaw") || name.contains("twin claws") || name.contains("twinclaws")) {
            return Optional.of(Marker.TWINCLAWS);
        }
        if (isInvincibleHologram(hoverName)) {
            return Optional.of(Marker.INVINCIBLE);
        }
        if (isFirePillarHologram(hoverName)) {
            return Optional.of(Marker.FIRE_PILLAR);
        }
        return Optional.empty();
    }

    public static Optional<SlayerPolicy.SlayerType> familyForMarker(Marker marker) {
        if (marker == null) {
            return Optional.empty();
        }
        return switch (marker) {
            case BEACON, NUKEKUBI -> Optional.of(SlayerPolicy.SlayerType.VOIDGLOOM);
            case EGG_SAC, INVINCIBLE -> Optional.of(SlayerPolicy.SlayerType.TARANTULA);
            case BOOM -> Optional.of(SlayerPolicy.SlayerType.REVENANT);
            case PUP -> Optional.of(SlayerPolicy.SlayerType.SVEN);
            case BLOOD_ICHOR, KILLER_SPRING, TWINCLAWS -> Optional.of(SlayerPolicy.SlayerType.VAMPIRE);
            case FIRE_PILLAR -> Optional.of(SlayerPolicy.SlayerType.INFERNO);
        };
    }

    public static boolean isPupName(String raw) {
        String name = normalize(raw).toLowerCase(Locale.ROOT);
        if (name.contains("packmaster") || name.contains("enforcer")) {
            return false;
        }
        return name.contains("sven pup") || name.contains("pack pup");
    }

    public static boolean isBoomHologram(String raw) {
        String name = normalize(raw).toLowerCase(Locale.ROOT);
        return name.equals("boom") || name.equals("boom!") || name.contains("boom!");
    }

    public enum TarantulaPhase {
        UNKNOWN,
        FIRST,
        SECOND
    }

    public static final long HATCHLINGS_INVINCIBLE_MILLIS = 25_000L;
    private static final Pattern EGG_HITS = Pattern.compile("(?i)(\\d+)s\\s+(\\d+)/(\\d+)");

    public static boolean isEggHitsHologram(String raw) {
        return EGG_HITS.matcher(normalize(raw)).find();
    }

    public static Optional<String> eggHitsLabel(String raw) {
        Matcher matcher = EGG_HITS.matcher(normalize(raw));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(2) + "/" + matcher.group(3));
    }

    public static TarantulaPhase tarantulaPhase(String hologram) {
        String name = normalize(hologram).toLowerCase(Locale.ROOT);
        if (name.contains("conjoined")) {
            return TarantulaPhase.SECOND;
        }
        if (name.contains("tarantula broodfather")) {
            return TarantulaPhase.FIRST;
        }
        return TarantulaPhase.UNKNOWN;
    }

    public static String tarantulaPhaseLabel(TarantulaPhase phase) {
        return switch (phase) {
            case FIRST -> "Phase 1/2";
            case SECOND -> "Phase 2/2";
            case UNKNOWN -> "";
        };
    }

    /**
     * Tarantula T5 dies twice: Broodfather then Conjoined Brood. Phase 1 is not
     * a finished kill for time-to-kill, personal bests, or carry counts.
     */
    public static boolean isTarantulaTierFivePhaseOne(SlayerPolicy.EntityDescriptor descriptor) {
        if (descriptor == null
                || descriptor.type() != SlayerPolicy.SlayerType.TARANTULA
                || descriptor.tier() != 5) {
            return false;
        }
        return tarantulaPhase(descriptor.displayName()) == TarantulaPhase.FIRST;
    }

    public static boolean isHatchlingsChat(String raw) {
        String text = normalize(raw).toLowerCase(Locale.ROOT);
        return text.contains("kill the broodfather's hatchlings")
                || text.contains("kill the broodfathers hatchlings");
    }

    public static boolean isInvincibleHologram(String raw) {
        String name = normalize(raw).toLowerCase(Locale.ROOT);
        return name.contains("invincible") || name.contains("kill hatchling");
    }

    public static boolean isSpiderSound(String soundId) {
        String id = soundId == null ? "" : soundId.trim().toLowerCase(Locale.ROOT);
        if (id.startsWith("minecraft:")) {
            id = id.substring("minecraft:".length());
        }
        return id.contains("entity.spider.")
                || id.contains("entity.cave_spider.")
                || id.contains("entity.silverfish.")
                || id.contains("entity.skeleton.")
                || id.equals("entity.bat.hurt")
                || id.endsWith("entity.bat.hurt");
    }

    public static boolean isTarantulaSoundArea(String sidebar) {
        String text = normalize(sidebar).toLowerCase(Locale.ROOT);
        return text.contains("spider's den")
                || text.contains("spiders den")
                || text.contains("spider den")
                || text.contains("crimson isle")
                || text.contains("tarantula broodfather");
    }

    public static boolean isInfernoFightArea(String sidebar) {
        String text = normalize(sidebar).toLowerCase(Locale.ROOT);
        return text.contains("inferno demonlord");
    }

    public static boolean isBeaconHelmet(String itemName) {
        return normalize(itemName).equalsIgnoreCase("Beacon");
    }

    /**
     * The flying/sitting Yang Glyph is a beacon armor stand. Boss holograms that
     * say "Yang Glyph" or "Destroy the beacon!" are not the thrown glyph.
     */
    public static boolean isThrownYangGlyphStand(
            String hoverName,
            String helmetItemName,
            boolean helmetIsBeacon) {
        if (helmetIsBeacon || isBeaconHelmet(helmetItemName)) {
            return true;
        }
        return normalize(hoverName).equalsIgnoreCase("Beacon");
    }

    /**
     * Power orbs (Radiant / Mana Flux / Overflux / Plasmaflux) are armor stands
     * that also wear a Beacon. They are not Yang Glyphs.
     */
    public static boolean isPowerOrbHologram(String raw) {
        String text = normalize(raw);
        return !text.isBlank() && POWER_ORB.matcher(text).matches();
    }

    public static boolean isNukekubiTexture(String raw) {
        return raw != null
                && raw.toUpperCase(Locale.ROOT).contains(NUKEKUBI_TEXTURE_FRAGMENT.toUpperCase(Locale.ROOT));
    }

    public static VoidgloomPhase voidgloomPhase(String hologram) {
        if (isFirePillarHologram(hologram)) {
            return VoidgloomPhase.UNKNOWN;
        }
        String text = normalize(hologram);
        if (text.isEmpty()) {
            return VoidgloomPhase.UNKNOWN;
        }
        if (VOIDGLOOM_BEACON.matcher(text).matches()) {
            return VoidgloomPhase.BEACON;
        }
        if (VOIDGLOOM_HITS.matcher(text).matches()) {
            return VoidgloomPhase.HITS;
        }
        if (VOIDGLOOM_LASER.matcher(text).matches()) {
            return VoidgloomPhase.LASER;
        }
        if (text.toLowerCase(Locale.ROOT).contains("dps")
                || text.toLowerCase(Locale.ROOT).contains("damage")) {
            return VoidgloomPhase.DPS;
        }
        return VoidgloomPhase.UNKNOWN;
    }

    public static Optional<Integer> hitsRemaining(String hologram) {
        if (isFirePillarHologram(hologram)) {
            return Optional.empty();
        }
        Matcher matcher = VOIDGLOOM_HITS.matcher(normalize(hologram));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        try {
            String group = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            return Optional.of(Integer.parseInt(group));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static Optional<String> compactHealth(String hologram) {
        Matcher matcher = COMPACT_HEALTH.matcher(normalize(hologram));
        if (!matcher.find()) {
            return Optional.empty();
        }
        String amount = healthAmountToken(matcher.group(1), matcher.group(2));
        String suffix = matcher.group(2) == null ? "" : matcher.group(2).toUpperCase(Locale.ROOT);
        return Optional.of(amount + suffix + "❤");
    }

    public static int clampLineWidth(int width) {
        return Math.max(MIN_LINE_WIDTH, Math.min(MAX_LINE_WIDTH, width));
    }

    public static String phaseLabel(VoidgloomPhase phase) {
        return switch (phase) {
            case HITS -> "Hits phase";
            case LASER -> "Laser";
            case BEACON -> "Yang Glyph";
            case DPS -> "Damage phase";
            case UNKNOWN -> "";
        };
    }

    public static String phaseLabel(VoidgloomPhase phase, int hitsRemaining) {
        if (phase == VoidgloomPhase.HITS && hitsRemaining >= 0) {
            return "Hits " + hitsRemaining;
        }
        return phaseLabel(phase);
    }

    public static double remainingSeconds(long startedAtMillis, long nowMillis, long durationMillis) {
        long remaining = startedAtMillis + durationMillis - Math.max(0L, nowMillis);
        return remaining <= 0L ? 0.0D : remaining / 1000.0D;
    }

    public static String countdownLabel(double remainingSeconds) {
        if (!Double.isFinite(remainingSeconds) || remainingSeconds <= 0.0D) {
            return "";
        }
        return String.format(Locale.ROOT, "%.1fs", remainingSeconds);
    }

    /**
     * A thrown Yang Glyph armor stand that has barely moved after traveling
     * has landed, even if the beacon block packet is late.
     */
    public static boolean flyingBeaconLanded(double pathLengthSqr, double lastStepSqr) {
        return Double.isFinite(pathLengthSqr)
                && Double.isFinite(lastStepSqr)
                && pathLengthSqr >= 4.0D
                && lastStepSqr >= 0.0D
                && lastStepSqr <= 0.09D;
    }

    public static boolean shouldHideVoidgloomParticle(String particleId) {
        String id = particleId == null ? "" : particleId.trim().toLowerCase(Locale.ROOT);
        return id.contains("large_smoke")
                || id.endsWith(":smoke")
                || id.contains("flame")
                || id.contains("witch");
    }

    public static Optional<QuestRef> questFromSidebar(List<String> lines) {
        if (lines == null) {
            return Optional.empty();
        }
        for (int i = 0; i < lines.size(); i++) {
            Optional<QuestRef> quest = questFromLine(normalize(lines.get(i)));
            if (quest.isPresent()) {
                return quest;
            }
            if (i + 1 < lines.size()) {
                quest = questFromLine(normalize(lines.get(i)) + " " + normalize(lines.get(i + 1)));
                if (quest.isPresent()) {
                    return quest;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<QuestRef> questFromLine(String line) {
        Matcher matcher = SIDEBAR_QUEST.matcher(line);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        Optional<SlayerPolicy.SlayerType> type = SlayerPolicy.slayerType(matcher.group(1));
        if (type.isEmpty()) {
            return Optional.empty();
        }
        int tier;
        if (matcher.group(2) != null) {
            tier = Integer.parseInt(matcher.group(2));
        } else {
            tier = roman(matcher.group(3));
        }
        return Optional.of(new QuestRef(type.get(), tier));
    }

    public static String romanLabel(int tier) {
        return switch (Math.max(0, tier)) {
            case 5 -> " V";
            case 4 -> " IV";
            case 3 -> " III";
            case 2 -> " II";
            case 1 -> " I";
            default -> "";
        };
    }

    public static boolean sidebarHasSlayerQuest(List<String> lines) {
        if (lines == null) {
            return false;
        }
        for (String line : lines) {
            String text = normalize(line).toLowerCase(Locale.ROOT);
            if (text.contains("slayer quest")
                    || text.contains("spawn the boss")
                    || text.contains("slay the boss")
                    || text.contains("kill the boss")) {
                return true;
            }
        }
        return questFromSidebar(lines).isPresent();
    }

    public static boolean wrongQuest(
            SlayerPolicy.SlayerType quest,
            SlayerPolicy.SlayerType damagedFamily) {
        return quest != null && damagedFamily != null && quest != damagedFamily;
    }

    public static Optional<SlayerPolicy.SlayerType> familyFromMobName(String raw) {
        String name = normalize(raw).toLowerCase(Locale.ROOT);
        if (name.contains("revenant") || name.contains("atoned")) {
            return Optional.of(SlayerPolicy.SlayerType.REVENANT);
        }
        if (name.contains("tarantula") || name.contains("broodfather") || name.contains("conjoined")) {
            return Optional.of(SlayerPolicy.SlayerType.TARANTULA);
        }
        if (name.contains("sven") || name.contains("packmaster") || name.contains("pack enforcer")
                || name.contains("sven follower") || name.contains("sven alpha")) {
            return Optional.of(SlayerPolicy.SlayerType.SVEN);
        }
        if (name.contains("voidgloom") || name.contains("seraph") || name.contains("voidling")
                || name.contains("voidcrazed")) {
            return Optional.of(SlayerPolicy.SlayerType.VOIDGLOOM);
        }
        if (name.contains("inferno") || name.contains("demonlord") || name.contains("quazii")
                || name.contains("typhoeus") || name.contains("flare demon")
                || name.contains("kindleheart") || name.contains("burningsoul")) {
            return Optional.of(SlayerPolicy.SlayerType.INFERNO);
        }
        if (name.contains("bloodfiend") || name.contains("vampire") || name.contains("riftstalker")) {
            return Optional.of(SlayerPolicy.SlayerType.VAMPIRE);
        }
        return Optional.empty();
    }

    public static String autoStartCommand(SlayerPolicy.SlayerType type, int tier) {
        int safeTier = Math.max(1, Math.min(5, tier));
        if (type == null) {
            return "";
        }
        String command = switch (type) {
            case REVENANT -> "revenant";
            case TARANTULA -> "tarantula";
            case SVEN -> "sven";
            case VOIDGLOOM -> "enderman";
            case INFERNO -> "blaze";
            case VAMPIRE -> "vampire";
        };
        return command + " " + safeTier;
    }

    public static boolean isHowlSound(String soundId) {
        String id = soundId == null ? "" : soundId.trim().toLowerCase(Locale.ROOT);
        return id.equals("minecraft:entity.wolf.howl")
                || id.endsWith("entity.wolf.howl")
                || id.equals("entity.wolf.howl");
    }

    public static boolean isWolfSound(String soundId) {
        String id = soundId == null ? "" : soundId.trim().toLowerCase(Locale.ROOT);
        if (id.startsWith("minecraft:")) {
            id = id.substring("minecraft:".length());
        }
        return id.contains("entity.wolf.");
    }

    public static boolean isHowlHologram(String raw) {
        String name = normalize(raw).toLowerCase(Locale.ROOT);
        if (name.contains("howling cave") || name.contains("packmaster") || isPupName(raw)) {
            return false;
        }
        return name.equals("howl") || name.contains("howl!");
    }

    public static boolean isSvenSoundArea(String sidebar) {
        String text = normalize(sidebar).toLowerCase(Locale.ROOT);
        return text.contains("the park")
                || text.contains("howling cave")
                || text.contains("sven packmaster")
                || text.contains("hub island")
                || text.contains("skyblock hub")
                || (text.contains("village") && text.contains("hub"));
    }

    public static boolean isVampireNoise(String soundId) {
        String id = soundId == null ? "" : soundId.trim().toLowerCase(Locale.ROOT);
        return id.contains("entity.bat")
                || id.contains("entity.phantom")
                || id.contains("particle.soul");
    }

    public static boolean isKillerSpringSound(String soundId) {
        String id = soundId == null ? "" : soundId.trim().toLowerCase(Locale.ROOT);
        if (id.startsWith("minecraft:")) {
            id = id.substring("minecraft:".length());
        }
        return id.contains("entity.wither.spawn")
                || id.contains("entity.elder_guardian.curse")
                || id.equals("entity.wither.spawn")
                || id.equals("entity.elder_guardian.curse");
    }

    public static boolean isVampireSoundArea(String sidebar) {
        String text = normalize(sidebar).toLowerCase(Locale.ROOT);
        return text.contains("stillgore")
                || text.contains("chateau")
                || text.contains("the rift")
                || text.contains("riftstalker")
                || text.contains("bloodfiend");
    }

    public static boolean isManiaHologram(String raw) {
        String name = normalize(raw).toLowerCase(Locale.ROOT);
        return name.contains("mania") && !name.contains("maniac");
    }

    public static boolean isSteakReady(String raw) {
        String name = normalize(raw);
        return name.contains("\u0489") || name.toLowerCase(Locale.ROOT).contains("steak!");
    }

    public static final int MANIA_START_TICKS = 40;
    public static final int MANIA_END_TICKS = 520;
    public static final int MIN_TWINCLAWS_DELAY_MS = 0;
    public static final int MAX_TWINCLAWS_DELAY_MS = 1_000;

    public static double maniaRemainingSeconds(int vehicleTicks) {
        if (vehicleTicks <= MANIA_START_TICKS) {
            return 0.0D;
        }
        int remainingTicks = MANIA_END_TICKS - vehicleTicks;
        return remainingTicks <= 0 ? 0.0D : remainingTicks / 20.0D;
    }

    public static int clampTwinclawsDelay(int millis) {
        return Math.max(MIN_TWINCLAWS_DELAY_MS, Math.min(MAX_TWINCLAWS_DELAY_MS, millis));
    }

    public enum InfernoPhase {
        UNKNOWN,
        FIRST,
        SECOND,
        THIRD
    }

    private static final Pattern FIRE_PILLAR_HITS = Pattern.compile(
            "(?i)(\\d+)s\\s+(\\d+)\\s+hits?");
    private static final Pattern FIRE_PILLAR_NAMED_SECONDS = Pattern.compile(
            "(?i)fire\\s*pillar.*?(\\d+)s");

    public static boolean isFirePillarHologram(String raw) {
        String text = normalize(raw);
        String lower = text.toLowerCase(Locale.ROOT);
        return FIRE_PILLAR_HITS.matcher(text).find()
                || lower.contains("fire pillar");
    }

    public static Optional<Integer> firePillarSeconds(String raw) {
        String text = normalize(raw);
        Matcher hits = FIRE_PILLAR_HITS.matcher(text);
        if (hits.find()) {
            return parsePositiveInt(hits.group(1));
        }
        Matcher named = FIRE_PILLAR_NAMED_SECONDS.matcher(text);
        if (named.find()) {
            return parsePositiveInt(named.group(1));
        }
        return Optional.empty();
    }

    public static Optional<Integer> firePillarHits(String raw) {
        Matcher matcher = FIRE_PILLAR_HITS.matcher(normalize(raw));
        if (matcher.find()) {
            return parsePositiveInt(matcher.group(2));
        }
        if (isFirePillarHologram(raw)) {
            return Optional.of(8);
        }
        return Optional.empty();
    }

    private static Optional<Integer> parsePositiveInt(String raw) {
        try {
            return Optional.of(Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isWrongAttunementChat(String raw) {
        String text = normalize(raw).toLowerCase(Locale.ROOT);
        return text.equals("your hit was reduced by hellion shield!")
                || (text.startsWith("strike using the") && text.endsWith("attunement on your dagger!"));
    }

    public record HealthReading(double current, double max) {
        public HealthReading {
            current = Math.max(0.0D, current);
            max = Math.max(current, max);
        }
    }

    public static Optional<Double> healthValue(String hologram) {
        return healthReading(hologram).map(HealthReading::current);
    }

    public static Optional<HealthReading> healthReading(String hologram) {
        String text = normalize(hologram);
        Matcher fraction = HEALTH_FRACTION.matcher(text);
        if (fraction.find()) {
            try {
                double current = parseHealthAmount(fraction.group(1), fraction.group(2));
                double max = parseHealthAmount(fraction.group(3), fraction.group(4));
                if (current > 0.0D) {
                    return Optional.of(new HealthReading(current, max));
                }
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        Matcher matcher = COMPACT_HEALTH.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            double amount = parseHealthAmount(matcher.group(1), matcher.group(2));
            if (amount <= 0.0D) {
                return Optional.empty();
            }
            return Optional.of(new HealthReading(amount, amount));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static double parseHealthAmount(String rawAmount, String suffix) {
        double amount = Double.parseDouble(healthAmountToken(rawAmount, suffix));
        String unit = suffix == null ? "" : suffix.toUpperCase(Locale.ROOT);
        return switch (unit) {
            case "K" -> amount * 1_000.0D;
            case "M" -> amount * 1_000_000.0D;
            case "B" -> amount * 1_000_000_000.0D;
            default -> amount;
        };
    }

    private static String healthAmountToken(String rawAmount, String suffix) {
        String amount = rawAmount == null ? "" : rawAmount.trim();
        if (suffix == null || suffix.isBlank()) {
            return amount.replace(",", "");
        }
        return amount.replace(',', '.');
    }

    public static InfernoPhase infernoPhase(int tier, double current, double max) {
        if (max <= 0.0D || current < 0.0D) {
            return InfernoPhase.UNKNOWN;
        }
        if (tier <= 2) {
            return current > max / 2.0D ? InfernoPhase.FIRST : InfernoPhase.SECOND;
        }
        if (current > max * 2.0D / 3.0D) {
            return InfernoPhase.FIRST;
        }
        if (current > max / 3.0D) {
            return InfernoPhase.SECOND;
        }
        return InfernoPhase.THIRD;
    }

    public static String infernoPhaseLabel(InfernoPhase phase, int tier) {
        boolean thirds = tier >= 3;
        return switch (phase) {
            case FIRST -> thirds ? "Phase 1/3" : "Phase 1/2";
            case SECOND -> thirds ? "Phase 2/3" : "Phase 2/2";
            case THIRD -> "Phase 3/3";
            case UNKNOWN -> "";
        };
    }

    public static boolean crossedFirePits(int tier, double previous, double current, double max) {
        if (tier < 3 || max <= 0.0D) {
            return false;
        }
        double threshold = max / 3.0D;
        return previous > threshold && current <= threshold;
    }

    public static int attunementColor(SlayerPolicy.Attunement attunement) {
        return switch (attunement) {
            case ASHEN -> 0xFF555555;
            case AURIC -> 0xFFFFAA00;
            case SPIRIT -> 0xFFFF55FF;
            case CRYSTAL -> 0xFF55FFFF;
            case UNKNOWN -> 0xFFFF5500;
        };
    }

    public static boolean shouldHideBlazeParticle(String particleId) {
        String id = particleId == null ? "" : particleId.trim().toLowerCase(Locale.ROOT);
        return id.contains("flame")
                || id.contains("lava")
                || id.contains("smoke")
                || id.contains("large_smoke")
                || id.contains("soul_fire")
                || id.contains("firefly");
    }

    public static boolean isVoidgloomParticle(String particleId) {
        return shouldHideVoidgloomParticle(particleId);
    }

    public static int clampAutoStartDelayTicks(int ticks) {
        return Math.max(0, Math.min(40, ticks));
    }

    private static int roman(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "V" -> 5;
            case "IV" -> 4;
            case "III" -> 3;
            case "II" -> 2;
            default -> 1;
        };
    }
}
