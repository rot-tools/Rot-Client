package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minecraft-free Hypixel Slayer protocol classifier shared by every Slayer
 * HUD, alert, carry and automation runtime.
 */
public final class SlayerPolicy {
    public enum SlayerType {
        REVENANT("Revenant Horror", "Revenant Horror", "Atoned Horror"),
        TARANTULA("Tarantula Broodfather", "Tarantula Broodfather", "Conjoined Brood"),
        SVEN("Sven Packmaster", "Sven Packmaster"),
        VOIDGLOOM("Voidgloom Seraph", "Voidgloom Seraph"),
        INFERNO("Inferno Demonlord", "Inferno Demonlord"),
        VAMPIRE("Riftstalker Bloodfiend", "Riftstalker Bloodfiend", "Bloodfiend");

        private final String displayName;
        private final Set<String> aliases;

        SlayerType(String displayName, String... aliases) {
            this.displayName = displayName;
            this.aliases = Set.of(aliases);
        }

        public String displayName() {
            return displayName;
        }

        Set<String> aliases() {
            return aliases;
        }
    }

    public enum EntityRole {
        BOSS,
        MINIBOSS,
        DEMON
    }

    public enum QuestSignal {
        NONE,
        STARTED,
        COMPLETED,
        FAILED
    }

    public enum Attunement {
        UNKNOWN,
        ASHEN,
        AURIC,
        SPIRIT,
        CRYSTAL
    }

    public record EntityDescriptor(
            EntityRole role,
            SlayerType type,
            int tier,
            String owner,
            String displayName,
            boolean bigMiniboss,
            Attunement attunement) {
        public EntityDescriptor {
            tier = Math.max(0, Math.min(5, tier));
            owner = owner == null ? "" : owner.trim();
            displayName = displayName == null ? "" : displayName.trim();
            attunement = attunement == null ? Attunement.UNKNOWN : attunement;
        }
    }

    public record DropObservation(String displayName, boolean rare) {
        public DropObservation {
            displayName = displayName == null ? "" : displayName.trim();
        }
    }

    private record Mini(SlayerType type, boolean big) {
    }

    private static final Pattern FORMAT_CODE = Pattern.compile("§[0-9A-FK-OR]", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP = Pattern.compile(
            "(?i).*(?:RARE|RNGESUS|PRAY TO RNGESUS|CRAZY RARE|INSANE) DROP!.*?\\(([^)]+)\\).*"                );
    /** Roman tier must sit immediately after the boss alias, not later in Hits/HP text. */
    private static final Pattern ROMAN_TIER = Pattern.compile(
            "^\\s*(IV|III|II|V|I)(?=\\s|$|[❤♥])");
    private static final Pattern COMPACT_HEALTH = Pattern.compile(
            "(?i)(\\d+(?:[.,]\\d+)?)\\s*([kmb])?\\s*[❤♥]");
    private static final Map<String, Mini> MINIBOSSES = createMinibosses();
    private static final Set<String> DEMONS = Set.of("Quazii", "ⓆⓊⒶⓏⒾⒾ", "Typhoeus", "ⓉⓎⓅⒽⓄⒺⓊⓈ");

    private SlayerPolicy() {
    }

    public static QuestSignal questSignal(String raw) {
        String line = normalize(raw).trim().toUpperCase(Locale.ROOT);
        if (line.contains("SLAYER QUEST STARTED!")) {
            return QuestSignal.STARTED;
        }
        if (line.contains("SLAYER QUEST COMPLETE!")) {
            return QuestSignal.COMPLETED;
        }
        if (line.contains("SLAYER QUEST FAILED!")) {
            return QuestSignal.FAILED;
        }
        return QuestSignal.NONE;
    }

    public static Optional<EntityDescriptor> classifyTag(String rawTag, String rawOwnerTag) {
        String tag = normalize(rawTag).trim();
        if (tag.isEmpty()) {
            return Optional.empty();
        }
        String owner = parseOwner(rawOwnerTag);
        Attunement attunement = attunement(tag);
        for (Map.Entry<String, Mini> entry : MINIBOSSES.entrySet()) {
            if (containsIgnoreCase(tag, entry.getKey())) {
                Mini mini = entry.getValue();
                return Optional.of(new EntityDescriptor(
                        EntityRole.MINIBOSS,
                        mini.type(),
                        0,
                        owner,
                        entry.getKey(),
                        mini.big(),
                        attunement));
            }
        }
        for (String demon : DEMONS) {
            if (containsIgnoreCase(tag, demon)) {
                return Optional.of(new EntityDescriptor(
                        EntityRole.DEMON,
                        SlayerType.INFERNO,
                        0,
                        owner,
                        demon,
                        false,
                        attunement));
            }
        }
        for (SlayerType type : SlayerType.values()) {
            for (String alias : type.aliases()) {
                if (containsIgnoreCase(tag, alias)) {
                    int namedTier = specialBossTier(tag);
                    int tier = namedTier > 0 ? namedTier : romanTierAfter(tag, alias);
                    if (tier <= 0) {
                        tier = inferTierFromHealth(type, tag);
                    }
                    return Optional.of(new EntityDescriptor(
                            EntityRole.BOSS,
                            type,
                            tier,
                            owner,
                            type.displayName(),
                            false,
                            attunement));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Classifies a living host from every nearby hologram. Miniboss and Inferno
     * demon names win over a leaked boss title, so a Voidling standing under a
     * Voidgloom nametag is not treated as the boss.
     */
    public static Optional<EntityDescriptor> classifyHolograms(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Optional.empty();
        }
        String owner = "";
        for (String line : lines) {
            String lower = normalize(line).toLowerCase(Locale.ROOT);
            if (lower.contains("spawned by") || lower.startsWith("owner:")) {
                owner = parseOwner(line);
                if (!owner.isBlank()) {
                    break;
                }
            }
        }
        Optional<EntityDescriptor> miniboss = Optional.empty();
        Optional<EntityDescriptor> demon = Optional.empty();
        Optional<EntityDescriptor> boss = Optional.empty();
        for (String line : lines) {
            Optional<EntityDescriptor> parsed = classifyTag(line, owner);
            if (parsed.isEmpty()) {
                continue;
            }
            EntityDescriptor descriptor = withOwner(parsed.get(), owner);
            switch (descriptor.role()) {
                case MINIBOSS -> {
                    if (miniboss.isEmpty()) {
                        miniboss = Optional.of(descriptor);
                    }
                }
                case DEMON -> {
                    if (demon.isEmpty()) {
                        demon = Optional.of(descriptor);
                    }
                }
                case BOSS -> {
                    if (boss.isEmpty() || (boss.get().tier() <= 0 && descriptor.tier() > 0)) {
                        boss = Optional.of(descriptor);
                    }
                }
            }
        }
        if (miniboss.isPresent()) {
            return miniboss;
        }
        if (demon.isPresent()) {
            return demon;
        }
        return boss;
    }

    public static boolean isSlayerHologram(String raw) {
        String tag = normalize(raw);
        if (tag.isEmpty()) {
            return false;
        }
        String lower = tag.toLowerCase(Locale.ROOT);
        if (lower.contains("spawned by") || lower.startsWith("owner:")) {
            return true;
        }
        return classifyTag(tag, "").isPresent();
    }

    public static Optional<DropObservation> dropObservation(String raw) {
        Matcher matcher = DROP.matcher(normalize(raw));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String item = matcher.group(1).trim();
        return item.isEmpty() ? Optional.empty() : Optional.of(new DropObservation(item, true));
    }

    public static Optional<SlayerType> slayerType(String raw) {
        String value = normalize(raw).toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return Optional.empty();
        }
        for (SlayerType type : SlayerType.values()) {
            if (type.name().toLowerCase(Locale.ROOT).equals(value)
                    || type.displayName().toLowerCase(Locale.ROOT).contains(value)) {
                return Optional.of(type);
            }
            for (String alias : type.aliases()) {
                if (alias.toLowerCase(Locale.ROOT).contains(value)) {
                    return Optional.of(type);
                }
            }
        }
        return switch (value) {
            case "rev", "zombie", "zombie slayer" -> Optional.of(SlayerType.REVENANT);
            case "tara", "spider", "spider slayer" -> Optional.of(SlayerType.TARANTULA);
            case "wolf", "wolf slayer" -> Optional.of(SlayerType.SVEN);
            case "void", "eman", "enderman", "enderman slayer" -> Optional.of(SlayerType.VOIDGLOOM);
            case "blaze", "blaze slayer" -> Optional.of(SlayerType.INFERNO);
            case "vamp", "rift", "bloodfiend", "vampire slayer" -> Optional.of(SlayerType.VAMPIRE);
            default -> Optional.empty();
        };
    }

    public static boolean isCocooned(String raw) {
        return normalize(raw).trim().equalsIgnoreCase("YOU COCOONED YOUR SLAYER BOSS");
    }

    public static Attunement attunement(String raw) {
        String upper = normalize(raw).toUpperCase(Locale.ROOT);
        for (Attunement value : Attunement.values()) {
            if (value != Attunement.UNKNOWN && upper.contains(value.name())) {
                return value;
            }
        }
        return Attunement.UNKNOWN;
    }

    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return FORMAT_CODE.matcher(raw).replaceAll("")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String parseOwner(String raw) {
        String owner = normalize(raw);
        if (owner.isEmpty()) {
            return "";
        }
        int colon = owner.lastIndexOf(':');
        if (colon >= 0 && colon + 1 < owner.length()) {
            owner = owner.substring(colon + 1).trim();
        }
        int space = owner.indexOf(' ');
        if (space > 0) {
            String first = owner.substring(0, space);
            if (first.matches("[A-Za-z0-9_]{1,16}")) {
                return first;
            }
        }
        return owner;
    }

    private static EntityDescriptor withOwner(EntityDescriptor descriptor, String owner) {
        if (descriptor == null || owner == null || owner.isBlank()
                || !descriptor.owner().isBlank()) {
            return descriptor;
        }
        return new EntityDescriptor(
                descriptor.role(),
                descriptor.type(),
                descriptor.tier(),
                owner,
                descriptor.displayName(),
                descriptor.bigMiniboss(),
                descriptor.attunement());
    }

    private static int specialBossTier(String tag) {
        if (containsIgnoreCase(tag, "Atoned Horror") || containsIgnoreCase(tag, "Conjoined Brood")) {
            return 5;
        }
        return 0;
    }

    private static int romanTierAfter(String tag, String alias) {
        int start = tag.toLowerCase(Locale.ROOT).indexOf(alias.toLowerCase(Locale.ROOT));
        String tail = start < 0 ? tag : tag.substring(start + alias.length());
        Matcher matcher = ROMAN_TIER.matcher(tail);
        return matcher.find() ? romanValue(matcher.group(1)) : 0;
    }

    private static int inferTierFromHealth(SlayerType type, String tag) {
        Matcher matcher = COMPACT_HEALTH.matcher(tag);
        if (!matcher.find()) {
            return 0;
        }
        double health = parseCompactHealth(matcher.group(1), matcher.group(2));
        if (!Double.isFinite(health) || health <= 0.0D) {
            return 0;
        }
        return switch (type) {
            case REVENANT -> health >= 5_000_000.0D ? 5
                    : health >= 800_000.0D ? 4
                    : health >= 150_000.0D ? 3
                    : health >= 8_000.0D ? 2
                    : health >= 200.0D ? 1
                    : 0;
            case TARANTULA -> health >= 15_000_000.0D ? 5
                    : health >= 5_000_000.0D ? 5
                    : health >= 1_500_000.0D ? 4
                    : health >= 300_000.0D ? 3
                    : health >= 10_000.0D ? 2
                    : health >= 300.0D ? 1
                    : 0;
            case SVEN -> health >= 1_200_000.0D ? 4
                    : health >= 300_000.0D ? 3
                    : health >= 15_000.0D ? 2
                    : health >= 800.0D ? 1
                    : 0;
            case VOIDGLOOM -> health >= 150_000_000.0D ? 4
                    : health >= 30_000_000.0D ? 3
                    : health >= 5_000_000.0D ? 2
                    : health >= 100_000.0D ? 1
                    : 0;
            case INFERNO -> health >= 90_000_000.0D ? 4
                    : health >= 25_000_000.0D ? 3
                    : health >= 5_000_000.0D ? 2
                    : health >= 1_000_000.0D ? 1
                    : 0;
            case VAMPIRE -> 0;
        };
    }

    private static double parseCompactHealth(String amount, String suffix) {
        double value = Double.parseDouble(amount.replace(",", "").replace(' ', '.'));
        if (suffix == null || suffix.isBlank()) {
            return value;
        }
        return switch (suffix.toLowerCase(Locale.ROOT)) {
            case "k" -> value * 1_000.0D;
            case "m" -> value * 1_000_000.0D;
            case "b" -> value * 1_000_000_000.0D;
            default -> value;
        };
    }

    private static int romanValue(String roman) {
        return switch (roman == null ? "" : roman.toUpperCase(Locale.ROOT)) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            default -> 0;
        };
    }

    private static boolean containsIgnoreCase(String source, String fragment) {
        return source.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }

    private static Map<String, Mini> createMinibosses() {
        Map<String, Mini> values = new LinkedHashMap<>();
        add(values, SlayerType.REVENANT, false, "Revenant Sycophant", "Revenant Champion", "Atoned Champion");
        add(values, SlayerType.REVENANT, true, "Deformed Revenant", "Atoned Revenant");
        add(values, SlayerType.TARANTULA, false, "Tarantula Vermin", "Tarantula Beast", "Primordial Jockey");
        add(values, SlayerType.TARANTULA, true, "Mutant Tarantula", "Primordial Viscount");
        add(values, SlayerType.SVEN, false, "Pack Enforcer", "Sven Follower");
        add(values, SlayerType.SVEN, true, "Sven Alpha");
        add(values, SlayerType.VOIDGLOOM, false, "Voidling Devotee", "Voidling Radical");
        add(values, SlayerType.VOIDGLOOM, true, "Voidcrazed Maniac");
        add(values, SlayerType.INFERNO, false, "Flare Demon", "Kindleheart Demon");
        add(values, SlayerType.INFERNO, true, "Burningsoul Demon");
        return Map.copyOf(values);
    }

    private static void add(
            Map<String, Mini> values,
            SlayerType type,
            boolean big,
            String... names) {
        for (String name : names) {
            values.put(name, new Mini(type, big));
        }
    }
}
