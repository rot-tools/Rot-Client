package fi.rotclient;

import java.util.LinkedHashMap;
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
    private static final Pattern ROMAN_TIER = Pattern.compile("(?:^|\\s)(V|IV|III|II|I)(?:\\s|$)");
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
        for (SlayerType type : SlayerType.values()) {
            for (String alias : type.aliases()) {
                if (containsIgnoreCase(tag, alias)) {
                    return Optional.of(new EntityDescriptor(
                            EntityRole.BOSS,
                            type,
                            romanTierAfter(tag, alias),
                            owner,
                            type.displayName(),
                            false,
                            attunement));
                }
            }
        }
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
        return Optional.empty();
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
            case "rev", "zombie" -> Optional.of(SlayerType.REVENANT);
            case "tara", "spider" -> Optional.of(SlayerType.TARANTULA);
            case "wolf" -> Optional.of(SlayerType.SVEN);
            case "void", "eman", "enderman" -> Optional.of(SlayerType.VOIDGLOOM);
            case "blaze" -> Optional.of(SlayerType.INFERNO);
            case "vamp", "rift", "bloodfiend" -> Optional.of(SlayerType.VAMPIRE);
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

    private static int romanTierAfter(String tag, String alias) {
        int start = tag.toLowerCase(Locale.ROOT).indexOf(alias.toLowerCase(Locale.ROOT));
        String tail = start < 0 ? tag : tag.substring(start + alias.length());
        Matcher matcher = ROMAN_TIER.matcher(tail);
        return matcher.find() ? romanValue(matcher.group(1)) : 0;
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
