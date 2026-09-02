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
 * Pure chat-convenience decisions for incoming {@code !} helpers and outgoing
 * emote replacement. Minecraft send/receive lives in the client runtime.
 */
public final class ChatCommandsPolicy {
    public static final int PROCESS_DELAY_TICKS = 4;
    public static final int MAX_CHAT_RULES = 64;
    public static final int MAX_RULE_PATTERN_CHARS = 256;
    public static final int MAX_RULE_INPUT_CHARS = 4_096;
    private static final Pattern NESTED_QUANTIFIER = Pattern.compile(
            "\\([^)]*(?:\\*|\\+|\\{\\d+(?:,\\d*)?})[^)]*\\)(?:\\*|\\+|\\{)");
    private static final Pattern BACK_REFERENCE = Pattern.compile("\\\\[1-9]");

    public enum Channel {
        PARTY,
        GUILD,
        PRIVATE
    }

    public enum ActionKind {
        REPLY,
        COMMAND
    }

    public record Incoming(Channel channel, String sender, String body) {
    }

    public record Decision(ActionKind kind, String payload) {
        public static Decision reply(String text) {
            return new Decision(ActionKind.REPLY, text);
        }

        public static Decision command(String command) {
            return new Decision(ActionKind.COMMAND, command);
        }
    }

    public record Context(
            String localName,
            int blockX,
            int blockY,
            int blockZ,
            int pingMs,
            int fps,
            String tps,
            String location,
            String holding,
            boolean partyLeader,
            List<String> partyMembers,
            double random01) {
    }

    private static final Pattern SENDER_BODY = Pattern.compile(
            "^(?:\\[[^]]*])?\\s*(\\w{1,16})(?:\\s+\\[[^]]*])?(?:\\s+\\S)?\\s*:\\s*(.+)$");
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Map<String, String> EMOTES = emotes();
    private static final String[] EIGHT_BALL = {
            "It is certain",
            "It is decidedly so",
            "Without a doubt",
            "Yes definitely",
            "You may rely on it",
            "As I see it, yes",
            "Most likely",
            "Outlook good",
            "Yes",
            "Signs point to yes",
            "Reply hazy try again",
            "Ask again later",
            "Better not tell you now",
            "Cannot predict now",
            "Concentrate and ask again",
            "Don't count on it",
            "My reply is no",
            "My sources say no",
            "Outlook not so good",
            "Very doubtful"
    };

    private ChatCommandsPolicy() {
    }

    public static String stripFormatting(String raw) {
        if (raw == null) {
            return "";
        }
        return FORMATTING.matcher(raw).replaceAll("").trim();
    }

    public static Optional<Incoming> parseIncoming(String raw) {
        String value = stripFormatting(raw);
        Channel channel;
        String rest;
        if (value.startsWith("Party > ")) {
            channel = Channel.PARTY;
            rest = value.substring("Party > ".length());
        } else if (value.startsWith("Guild > ")) {
            channel = Channel.GUILD;
            rest = value.substring("Guild > ".length());
        } else if (value.startsWith("From ")) {
            channel = Channel.PRIVATE;
            rest = value.substring("From ".length());
        } else {
            return Optional.empty();
        }
        Matcher matcher = SENDER_BODY.matcher(rest);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String body = matcher.group(2).trim();
        if (!body.startsWith("!")) {
            return Optional.empty();
        }
        return Optional.of(new Incoming(channel, matcher.group(1), body));
    }

    public static boolean channelEnabled(
            Channel channel,
            boolean party,
            boolean guild,
            boolean priv) {
        if (channel == null) {
            return false;
        }
        return switch (channel) {
            case PARTY -> party;
            case GUILD -> guild;
            case PRIVATE -> priv;
        };
    }

    public static String replyCommand(Channel channel, String sender, String message) {
        String text = message == null ? "" : message;
        return switch (channel) {
            case PARTY -> "pc " + text;
            case GUILD -> "gc " + text;
            case PRIVATE -> "msg " + sender + " " + text;
        };
    }

    public static Optional<String> applyEmotes(String message, boolean enabled) {
        if (!enabled || message == null || message.isBlank()) {
            return Optional.empty();
        }
        if (isCommandMessage(message) && !emoteCommandAllowed(message)) {
            return Optional.empty();
        }
        String[] parts = message.split(" ", -1);
        boolean changed = false;
        for (int i = 0; i < parts.length; i++) {
            String replacement = EMOTES.get(parts[i]);
            if (replacement != null) {
                parts[i] = replacement;
                changed = true;
            }
        }
        if (!changed) {
            return Optional.empty();
        }
        return Optional.of(String.join(" ", parts));
    }

    public static Optional<Decision> decide(
            Incoming incoming,
            boolean moduleEnabled,
            boolean partyEnabled,
            boolean guildEnabled,
            boolean privateEnabled,
            Context context) {
        if (!moduleEnabled || incoming == null || context == null) {
            return Optional.empty();
        }
        if (!channelEnabled(
                incoming.channel(), partyEnabled, guildEnabled, privateEnabled)) {
            return Optional.empty();
        }
        String[] tokens = incoming.body().substring(1).trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isBlank()) {
            return Optional.empty();
        }
        String command = tokens[0].toLowerCase(Locale.ROOT);
        String arg = tokens.length > 1 && tokens[1].length() <= 16 ? tokens[1] : null;
        Channel channel = incoming.channel();
        return switch (command) {
            case "help", "h" -> Optional.of(Decision.reply(
                    "Commands: " + String.join(", ", helpKeys(channel))));
            case "coords", "co" -> Optional.of(Decision.reply(
                    "x: " + context.blockX()
                            + ", y: " + context.blockY()
                            + ", z: " + context.blockZ()));
            case "cf" -> Optional.of(Decision.reply(
                    context.random01() < 0.5D ? "heads" : "tails"));
            case "8ball" -> Optional.of(Decision.reply(eightBall(context.random01())));
            case "dice" -> Optional.of(Decision.reply(Integer.toString(dice(context.random01()))));
            case "ping" -> channel == Channel.PARTY
                    ? Optional.empty()
                    : Optional.of(Decision.reply("Current Ping: " + context.pingMs() + "ms"));
            case "tps" -> Optional.of(Decision.reply(context.tps()));
            case "fps" -> Optional.of(Decision.reply("Current FPS: " + context.fps()));
            case "time" -> Optional.empty();
            case "location" -> Optional.of(Decision.reply(
                    "Current Location: " + context.location()));
            case "holding" -> Optional.of(Decision.reply(
                    context.holding() == null || context.holding().isBlank()
                            ? "Holding: Nothing :("
                            : "Holding: " + context.holding()));
            case "boop" -> arg == null
                    ? Optional.empty()
                    : Optional.of(Decision.command("boop " + arg));
            case "warp", "w" -> partyLeaderCommand(channel, context, "party warp");
            case "allinvite", "allinv" ->
                    partyLeaderCommand(channel, context, "party settings allinvite");
            case "pt", "ptme", "transfer" -> partyLeaderCommand(
                    channel,
                    context,
                    "party transfer " + resolveMember(context, arg, incoming.sender()));
            case "promote" -> partyLeaderCommand(
                    channel,
                    context,
                    "party promote " + resolveMember(context, arg, incoming.sender()));
            case "demote" -> partyLeaderCommand(
                    channel,
                    context,
                    "party demote " + resolveMember(context, arg, incoming.sender()));
            case "kick", "k" -> partyLeaderCommand(
                    channel,
                    context,
                    "p kick " + resolveMember(context, arg, incoming.sender()));
            case "kickoffline", "ko" ->
                    partyLeaderCommand(channel, context, "p kickoffline");
            case "invite", "inv" -> channel == Channel.PRIVATE
                    ? Optional.of(Decision.command("p invite " + incoming.sender()))
                    : Optional.empty();
            case "reinvite", "reinv" -> channel == Channel.PARTY
                    ? Optional.of(Decision.command("p invite " + incoming.sender()))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    public static Optional<Decision> decideTime(
            Incoming incoming,
            boolean moduleEnabled,
            boolean partyEnabled,
            boolean guildEnabled,
            boolean privateEnabled,
            String formattedTime) {
        if (!moduleEnabled || incoming == null) {
            return Optional.empty();
        }
        if (!channelEnabled(
                incoming.channel(), partyEnabled, guildEnabled, privateEnabled)) {
            return Optional.empty();
        }
        String command = incoming.body().substring(1).trim().split("\\s+")[0]
                .toLowerCase(Locale.ROOT);
        if (!command.equals("time")) {
            return Optional.empty();
        }
        return Optional.of(Decision.reply("Current Time: " + formattedTime));
    }

    static String resolveMember(Context context, String partial, String fallback) {
        if (partial == null || partial.isBlank()) {
            return fallback;
        }
        List<String> members = context.partyMembers() == null
                ? List.of()
                : context.partyMembers();
        String needle = partial.toLowerCase(Locale.ROOT);
        for (String member : members) {
            if (member != null && member.toLowerCase(Locale.ROOT).contains(needle)) {
                return member;
            }
        }
        return partial;
    }

    static int dice(double random01) {
        double clamped = Math.max(0.0D, Math.min(0.999999D, random01));
        return 1 + (int) Math.floor(clamped * 6.0D);
    }

    static String eightBall(double random01) {
        double clamped = Math.max(0.0D, Math.min(0.999999D, random01));
        return EIGHT_BALL[(int) Math.floor(clamped * EIGHT_BALL.length)];
    }

    private static Optional<Decision> partyLeaderCommand(
            Channel channel,
            Context context,
            String command) {
        if (channel != Channel.PARTY || !context.partyLeader()) {
            return Optional.empty();
        }
        return Optional.of(Decision.command(command));
    }

    private static List<String> helpKeys(Channel channel) {
        List<String> keys = new ArrayList<>();
        keys.add("coords");
        keys.add("cf");
        keys.add("8ball");
        keys.add("dice");
        keys.add("tps");
        keys.add("fps");
        keys.add("location");
        keys.add("holding");
        keys.add("boop");
        if (channel != Channel.PARTY) {
            keys.add("ping");
        }
        if (channel == Channel.PARTY) {
            keys.add("warp");
            keys.add("allinvite");
            keys.add("pt");
            keys.add("promote");
            keys.add("demote");
            keys.add("kick");
            keys.add("kickoffline");
            keys.add("reinvite");
        }
        if (channel == Channel.PRIVATE) {
            keys.add("invite");
        }
        return keys;
    }

    private static boolean isCommandMessage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.startsWith("/")
                || lower.startsWith("pc ")
                || lower.startsWith("ac ")
                || lower.startsWith("gc ")
                || lower.startsWith("msg ")
                || lower.startsWith("w ")
                || lower.startsWith("r ")
                || lower.equals("pc")
                || lower.equals("ac")
                || lower.equals("gc")
                || lower.equals("msg")
                || lower.equals("w")
                || lower.equals("r");
    }

    private static boolean emoteCommandAllowed(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.startsWith("/")) {
            lower = lower.substring(1);
        }
        return lower.startsWith("pc ")
                || lower.startsWith("ac ")
                || lower.startsWith("gc ")
                || lower.startsWith("msg ")
                || lower.startsWith("w ")
                || lower.startsWith("r ");
    }

    private static Map<String, String> emotes() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("<3", "❤");
        map.put("o/", "( ﾟ◡ﾟ)/");
        map.put(":star:", "✮");
        map.put(":yes:", "✔");
        map.put(":no:", "✖");
        map.put(":java:", "☕");
        map.put(":arrow:", "➜");
        map.put(":shrug:", "¯\\_(ツ)_/¯");
        map.put(":tableflip:", "(╯°□°）╯︵ ┻━┻");
        map.put(":totem:", "☉_☉");
        map.put(":typing:", "✎...");
        map.put(":maths:", "√(π+x)=L");
        map.put(":snail:", "@'-'");
        map.put("ez", "ｅｚ");
        map.put(":thinking:", "(0.o?)");
        map.put(":gimme:", "༼つ◕_◕༽つ");
        map.put(":wizard:", "('-')⊃━☆ﾟ.*･｡ﾟ");
        map.put(":pvp:", "⚔");
        map.put(":peace:", "✌");
        map.put(":puffer:", "<('O')>");
        map.put("h/", "ヽ(^◇^*)/");
        map.put(":sloth:", "(・⊝・)");
        map.put(":dog:", "(ᵔᴥᵔ)");
        map.put(":dj:", "ヽ(⌐■_■)ノ♬");
        map.put(":yey:", "ヽ (◕◡◕) ﾉ");
        map.put(":snow:", "☃");
        map.put(":dab:", "<o/");
        map.put(":cat:", "= ＾● ⋏ ●＾ =");
        map.put(":cute:", "(✿◠‿◠)");
        map.put(":skull:", "☠");
        return map;
    }

    public record ChatRule(boolean hide, Pattern pattern, String replacement) {
    }

    /**
     * Lines {@code hide regex} or {@code replace regex => text}. Invalid
     * regex is skipped so a typo cannot crash chat.
     */
    public static List<ChatRule> parseRules(String raw) {
        List<ChatRule> rules = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return rules;
        }
        for (String line : raw.split("\\R")) {
            if (rules.size() >= MAX_CHAT_RULES) {
                break;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            try {
                if (lower.startsWith("hide ")) {
                    String expression = trimmed.substring(5).trim();
                    if (safeRuleExpression(expression)) {
                        rules.add(new ChatRule(
                                true, Pattern.compile(expression, Pattern.CASE_INSENSITIVE), ""));
                    }
                } else if (lower.startsWith("replace ")) {
                    String body = trimmed.substring(8);
                    int sep = body.indexOf("=>");
                    if (sep < 0) {
                        sep = body.indexOf("->");
                    }
                    if (sep <= 0) {
                        continue;
                    }
                    String expression = body.substring(0, sep).trim();
                    if (safeRuleExpression(expression)) {
                        rules.add(new ChatRule(
                                false,
                                Pattern.compile(expression, Pattern.CASE_INSENSITIVE),
                                body.substring(sep + 2).trim()));
                    }
                }
            } catch (Exception ignored) {
                // skip broken patterns
            }
        }
        return rules;
    }

    public static boolean shouldHideIncoming(boolean enabled, List<ChatRule> rules, String message) {
        if (!enabled || message == null || rules == null || rules.isEmpty()) {
            return false;
        }
        String text = boundedRuleInput(stripFormatting(message));
        for (ChatRule rule : rules) {
            if (rule.hide() && rule.pattern().matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public static String applyIncomingReplacements(
            boolean enabled, List<ChatRule> rules, String message) {
        if (!enabled || message == null || rules == null || rules.isEmpty()) {
            return message;
        }
        String text = boundedRuleInput(message);
        for (ChatRule rule : rules) {
            if (!rule.hide()) {
                try {
                    text = rule.pattern().matcher(text).replaceAll(rule.replacement());
                } catch (RuntimeException ignored) {
                    // A bad replacement group must not break the chat callback.
                }
            }
        }
        return text;
    }

    static boolean safeRuleExpression(String expression) {
        if (expression == null
                || expression.isBlank()
                || expression.length() > MAX_RULE_PATTERN_CHARS) {
            return false;
        }
        String compact = expression.replaceAll("\\s+", "");
        return !compact.contains("(?<=")
                && !compact.contains("(?<!")
                && !BACK_REFERENCE.matcher(compact).find()
                && !NESTED_QUANTIFIER.matcher(compact).find();
    }

    private static String boundedRuleInput(String message) {
        return message.length() <= MAX_RULE_INPUT_CHARS
                ? message
                : message.substring(0, MAX_RULE_INPUT_CHARS);
    }

    /**
     * One alias per line: {@code ah=/ah} or {@code bz -> /bz}. Exact first
     * token match only, so typed chat is not rewritten mid-sentence.
     */
    public static Map<String, String> parseShortcuts(String raw) {
        Map<String, String> map = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return map;
        }
        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int sep = trimmed.indexOf("=>");
            int width = 2;
            if (sep < 0) {
                sep = trimmed.indexOf("->");
            }
            if (sep < 0) {
                sep = trimmed.indexOf('=');
                width = 1;
            }
            if (sep <= 0) {
                continue;
            }
            String alias = trimmed.substring(0, sep).trim().toLowerCase(Locale.ROOT);
            String target = trimmed.substring(sep + width).trim();
            if (!alias.isEmpty() && !target.isEmpty()) {
                map.put(alias, target);
            }
        }
        return map;
    }

    public static String expandShortcut(boolean enabled, String shortcutsRaw, String message) {
        if (!enabled || message == null || message.isBlank()) {
            return message;
        }
        Map<String, String> shortcuts = parseShortcuts(shortcutsRaw);
        if (shortcuts.isEmpty()) {
            return message;
        }
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) {
            return message;
        }
        int space = trimmed.indexOf(' ');
        String token = (space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase(Locale.ROOT);
        String target = shortcuts.get(token);
        if (target == null) {
            return message;
        }
        String rest = space < 0 ? "" : trimmed.substring(space);
        if (target.startsWith("/")) {
            return target + rest;
        }
        return target + rest;
    }

    /**
     * When a shortcut target is a {@code /command}, the chat send must be
     * turned into {@code sendCommand} instead of a public chat line.
     */
    public static Optional<String> commandFromShortcut(
            boolean enabled, String shortcutsRaw, String message) {
        String expanded = expandShortcut(enabled, shortcutsRaw, message);
        if (expanded == null || expanded.equals(message) || !expanded.startsWith("/")) {
            return Optional.empty();
        }
        return Optional.of(expanded.substring(1));
    }

    public static String chatFromShortcut(
            boolean enabled, String shortcutsRaw, String message) {
        String expanded = expandShortcut(enabled, shortcutsRaw, message);
        if (expanded != null && expanded.startsWith("/") && !message.trim().startsWith("/")) {
            return message;
        }
        return expanded;
    }
}
