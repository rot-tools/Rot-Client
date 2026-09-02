package fi.rotclient;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Backward-compatible macro parsing, sequencing, placeholder and conflict
 * policy. Minecraft I/O lives in the client runtime.
 */
public final class RingPolicy {
    public static final String MODE_SEND = "SEND";
    public static final String MODE_TYPE = "TYPE";
    public static final String MODE_EDIT = "EDIT";
    public static final String MODE_CYCLE = "CYCLE";
    public static final String MODE_RANDOM = "RANDOM";
    public static final String MODE_REPEAT = "REPEAT";
    public static final List<String> SEND_MODES =
            List.of(MODE_SEND, MODE_TYPE, MODE_EDIT, MODE_CYCLE, MODE_RANDOM, MODE_REPEAT);

    public static final String STRATEGY_SUBMIT = "SUBMIT";
    public static final String STRATEGY_ASSERT = "ASSERT";
    public static final String STRATEGY_VETO = "VETO";
    public static final String STRATEGY_AVOID = "AVOID";
    public static final List<String> CONFLICT_STRATEGIES =
            List.of(STRATEGY_SUBMIT, STRATEGY_ASSERT, STRATEGY_VETO, STRATEGY_AVOID);

    public static final String ACTIVATION_HOLD = "HOLD";
    public static final String ACTIVATION_VANILLA = "VANILLA";
    public static final String ACTIVATION_RELEASE = "RELEASE";
    public static final List<String> ACTIVATION_TYPES =
            List.of(ACTIVATION_HOLD, ACTIVATION_VANILLA, ACTIVATION_RELEASE);

    public static final String ARMOR_COMMAND = "armor";
    public static final String LOADOUT_COMMAND = "loadout";
    public static final String EQUIP_COMMAND = "equipment";
    public static final int DEFAULT_RATELIMIT_COUNT = 4;
    public static final int MIN_RATELIMIT_COUNT = 1;
    public static final int MAX_RATELIMIT_COUNT = 20;
    public static final int DEFAULT_RATELIMIT_TICKS = 20;
    public static final int MIN_RATELIMIT_TICKS = 1;
    public static final int MAX_RATELIMIT_TICKS = 200;
    public static final int DEFAULT_LENGTH_LIMIT = 256;
    public static final int MIN_LENGTH_LIMIT = 32;
    public static final int MAX_LENGTH_LIMIT = 256;
    public static final int RECENT_CHAT_SCAN = 50;
    public static final String EDIT_TOKEN = "%edit%";
    public static final String FAULT_TOKEN = "?";

    private static final Pattern DELAY_SUFFIX = Pattern.compile("^(.*)@(\\d+)$");
    private static final Pattern RECENT_CHAT = Pattern.compile("%#(.*)%");
    private static final Pattern CLIPBOARD_REGEX = Pattern.compile("%clipboard#(.*)%");
    private static final Pattern POS_OFFSET = Pattern.compile("%pos([FBLR])(\\d+)%");
    private static final Pattern X_OFFSET = Pattern.compile("%x([+-]\\d+)%");
    private static final Pattern Y_OFFSET = Pattern.compile("%y([+-]\\d+)%");
    private static final Pattern Z_OFFSET = Pattern.compile("%z([+-]\\d+)%");
    private static final Pattern LPOS_OFFSET = Pattern.compile("%lpos([FBLR])(\\d+)%");
    private static final Pattern LX_OFFSET = Pattern.compile("%lx([+-]\\d+)%");
    private static final Pattern LY_OFFSET = Pattern.compile("%ly([+-]\\d+)%");
    private static final Pattern LZ_OFFSET = Pattern.compile("%lz([+-]\\d+)%");

    private RingPolicy() {
    }

    public static String normalizeSendMode(String raw) {
        return named(raw, SEND_MODES, MODE_SEND);
    }

    public static String normalizeConflict(String raw) {
        return named(raw, CONFLICT_STRATEGIES, STRATEGY_ASSERT);
    }

    public static String normalizeActivation(String raw) {
        return named(raw, ACTIVATION_TYPES, ACTIVATION_HOLD);
    }

    public static int clampRatelimitCount(int value) {
        return clamp(value, MIN_RATELIMIT_COUNT, MAX_RATELIMIT_COUNT);
    }

    public static int clampRatelimitTicks(int value) {
        return clamp(value, MIN_RATELIMIT_TICKS, MAX_RATELIMIT_TICKS);
    }

    public static int clampLengthLimit(int value) {
        return clamp(value, MIN_LENGTH_LIMIT, MAX_LENGTH_LIMIT);
    }

    public static boolean sameBind(String left, String right) {
        if (blank(left) || blank(right)) {
            return false;
        }
        Integer mouseLeft = QolKeybindNames.resolveMouseButton(left);
        Integer mouseRight = QolKeybindNames.resolveMouseButton(right);
        if (mouseLeft != null || mouseRight != null) {
            return mouseLeft != null && mouseLeft.equals(mouseRight);
        }
        int glfwLeft = QolKeybindNames.resolveGlfwKey(left, "");
        int glfwRight = QolKeybindNames.resolveGlfwKey(right, "");
        return glfwLeft != GLFW.GLFW_KEY_UNKNOWN && glfwLeft == glfwRight;
    }

    public static List<MacroDef> parseMacroList(
            String raw,
            String defaultMode,
            String defaultConflict,
            String defaultActivation,
            boolean useRatelimit) {
        List<MacroDef> macros = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return macros;
        }
        for (String line : raw.split("\\R")) {
            MacroDef parsed = parseMacroLine(
                    line, defaultMode, defaultConflict, defaultActivation, useRatelimit);
            if (parsed != null) {
                macros.add(parsed);
            }
        }
        return macros;
    }

    public static String serializeMacroList(List<MacroDef> macros) {
        if (macros == null || macros.isEmpty()) {
            return "";
        }
        return macros.stream()
                .filter(macro -> macro != null && validationError(macro).isEmpty())
                .map(RingPolicy::serializeMacroLine)
                .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
    }

    public static String serializeMacroLine(MacroDef macro) {
        if (macro == null) {
            return "";
        }
        String key = macro.limitKey().isBlank()
                ? macro.key()
                : macro.limitKey() + "+" + macro.key();
        return key + "=" + formatMessageSequence(macro.messages())
                + " | " + macro.sendMode()
                + " | " + macro.conflictStrategy()
                + " | " + macro.activationType()
                + " | " + macro.spaceTicks()
                + " | " + macro.altKey()
                + " | " + macro.maxRepeats();
    }

    public static List<MacroMessage> parseMessageSequence(String raw) {
        return parseMessages(raw == null ? "" : raw);
    }

    public static String formatMessageSequence(List<MacroMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.stream()
                .filter(message -> message != null && !message.text().isBlank())
                .map(message -> message.text()
                        + (message.delayTicks() > 0 ? "@" + message.delayTicks() : ""))
                .collect(java.util.stream.Collectors.joining(",, "));
    }

    public static String validationError(MacroDef macro) {
        if (macro == null) {
            return "Macro is missing";
        }
        if (macro.key().isBlank()) {
            return "Choose a key";
        }
        if (macro.key().contains("|") || macro.key().contains("=")) {
            return "Key contains a reserved character";
        }
        if (macro.messages().isEmpty()
                || macro.messages().stream().allMatch(message -> message.text().isBlank())) {
            return "Add at least one command or action";
        }
        if (macro.messages().stream().anyMatch(message ->
                message.text().contains("|") || message.text().contains(",,"))) {
            return "Actions cannot contain | or ,,";
        }
        return "";
    }

    public static MacroDef parseMacroLine(
            String raw,
            String defaultMode,
            String defaultConflict,
            String defaultActivation) {
        return parseMacroLine(raw, defaultMode, defaultConflict, defaultActivation, true);
    }

    public static MacroDef parseMacroLine(
            String raw,
            String defaultMode,
            String defaultConflict,
            String defaultActivation,
            boolean useRatelimit) {
        if (raw == null) {
            return null;
        }
        String line = raw.strip();
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }
        List<String> parts = splitFields(line);
        if (parts.isEmpty()) {
            return null;
        }
        String keyField = parts.get(0);
        String messagesField = "";
        int next = 1;
        int equals = indexOfUnquotedEquals(keyField);
        if (equals >= 0) {
            messagesField = keyField.substring(equals + 1).strip();
            keyField = keyField.substring(0, equals).strip();
        } else if (parts.size() > 1) {
            messagesField = parts.get(1).strip();
            next = 2;
        }
        if (blank(keyField) || messagesField.isBlank()) {
            return null;
        }
        String limit = "";
        String key = keyField;
        int plus = keyField.lastIndexOf('+');
        if (plus > 0 && plus < keyField.length() - 1) {
            limit = keyField.substring(0, plus).strip();
            key = keyField.substring(plus + 1).strip();
        }
        if (blank(key)) {
            return null;
        }
        String mode = defaultMode;
        String conflict = defaultConflict;
        String activation = defaultActivation;
        int spaceTicks = 0;
        String altKey = "";
        int maxRepeats = 0;
        if (parts.size() > next) {
            mode = parts.get(next++).strip();
        }
        if (parts.size() > next) {
            conflict = parts.get(next++).strip();
        }
        if (parts.size() > next) {
            activation = parts.get(next++).strip();
        }
        if (parts.size() > next) {
            spaceTicks = parseNonNegative(parts.get(next++));
        }
        if (parts.size() > next) {
            altKey = parts.get(next++).strip();
        }
        if (parts.size() > next) {
            maxRepeats = parseNonNegative(parts.get(next));
        }
        return new MacroDef(
                key,
                limit,
                altKey,
                parseMessages(messagesField),
                normalizeSendMode(mode),
                normalizeConflict(conflict),
                normalizeActivation(activation),
                Math.max(0, spaceTicks),
                Math.max(0, maxRepeats),
                false,
                useRatelimit);
    }

    public static List<MacroDef> skyblockPresets(
            String petsKey,
            String storageKey,
            String armorKey,
            String equipKey,
            String loadoutKey,
            String statsKey,
            String hubKey,
            String potionKey,
            String defaultMode,
            String defaultConflict,
            String defaultActivation,
            boolean useRatelimit) {
        List<MacroDef> presets = new ArrayList<>();
        addPreset(presets, petsKey, "/pets", defaultMode, defaultConflict, defaultActivation, useRatelimit);
        addPreset(presets, storageKey, "/storage", defaultMode, defaultConflict, defaultActivation, useRatelimit);
        addPreset(presets, armorKey, "/" + ARMOR_COMMAND, defaultMode, defaultConflict, defaultActivation, useRatelimit);
        addPreset(presets, equipKey, "/" + EQUIP_COMMAND, defaultMode, defaultConflict, defaultActivation, useRatelimit);
        addPreset(presets, loadoutKey, "/" + LOADOUT_COMMAND, defaultMode, defaultConflict, defaultActivation, useRatelimit);
        addPreset(presets, statsKey, "/stats", defaultMode, defaultConflict, defaultActivation, useRatelimit);
        addPreset(presets, hubKey, "/warp dungeon_hub", defaultMode, defaultConflict, defaultActivation, useRatelimit);
        addPreset(presets, potionKey, "/potionbag", defaultMode, defaultConflict, defaultActivation, useRatelimit);
        return presets;
    }

    public static List<MacroDef> mergeMacros(List<MacroDef> custom, List<MacroDef> presets) {
        List<MacroDef> merged = new ArrayList<>(custom == null ? List.of() : custom);
        if (presets == null) {
            return merged;
        }
        for (MacroDef preset : presets) {
            boolean taken = false;
            for (MacroDef existing : merged) {
                if (sameBind(existing.key, preset.key) && sameBindOrBlank(existing.limitKey, preset.limitKey)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                merged.add(preset);
            }
        }
        return merged;
    }

    public static PlaceholderResult replacePlaceholders(String message, PlaceholderContext context) {
        if (message == null) {
            return new PlaceholderResult("", 1);
        }
        if (!message.contains("%")) {
            return new PlaceholderResult(message, 0);
        }
        PlaceholderState state = new PlaceholderState(context);
        String replaced = message;
        replaced = replaceSimple(replaced, "%lastsent%", state, () -> textOrFault(state, context.lastSent));
        replaced = replaceSimple(replaced, "%lastcmd%", state, () -> textOrFault(state, context.lastCmd));
        replaced = replaceSimple(replaced, "%clipboard%", state, () -> clipboard(state, null));
        replaced = replaceSimple(replaced, "%myname%", state, () -> textOrFault(state, context.myName));
        replaced = replaceSimple(replaced, "%pmsender%", state, () -> textOrFault(state, context.pmSender));
        replaced = replaceSimple(replaced, "%pos%", state, () -> playerPos(state, "0", "0"));
        replaced = replaceSimple(replaced, "%x%", state, () -> playerAxis(state, context.x, 0));
        replaced = replaceSimple(replaced, "%y%", state, () -> playerAxis(state, context.y, 0));
        replaced = replaceSimple(replaced, "%z%", state, () -> playerAxis(state, context.z, 0));
        replaced = replaceSimple(replaced, "%lpos%", state, () -> lookPos(state, "0", "0"));
        replaced = replaceSimple(replaced, "%lx%", state, () -> playerAxis(state, context.lookXBlock, 0));
        replaced = replaceSimple(replaced, "%ly%", state, () -> playerAxis(state, context.lookYBlock, 0));
        replaced = replaceSimple(replaced, "%lz%", state, () -> playerAxis(state, context.lookZBlock, 0));
        replaced = replaceRegex(replaced, RECENT_CHAT, 1, state, groups -> recentChat(state, groups[0]));
        replaced = replaceRegex(replaced, CLIPBOARD_REGEX, 1, state, groups -> clipboard(state, groups[0]));
        replaced = replaceRegex(replaced, POS_OFFSET, 2, state, groups -> playerPos(state, groups[0], groups[1]));
        replaced = replaceRegex(replaced, X_OFFSET, 1, state, groups -> playerAxis(state, context.x, parseSigned(groups[0])));
        replaced = replaceRegex(replaced, Y_OFFSET, 1, state, groups -> playerAxis(state, context.y, parseSigned(groups[0])));
        replaced = replaceRegex(replaced, Z_OFFSET, 1, state, groups -> playerAxis(state, context.z, parseSigned(groups[0])));
        replaced = replaceRegex(replaced, LPOS_OFFSET, 2, state, groups -> lookPos(state, groups[0], groups[1]));
        replaced = replaceRegex(replaced, LX_OFFSET, 1, state, groups -> playerAxis(state, context.lookXBlock, parseSigned(groups[0])));
        replaced = replaceRegex(replaced, LY_OFFSET, 1, state, groups -> playerAxis(state, context.lookYBlock, parseSigned(groups[0])));
        replaced = replaceRegex(replaced, LZ_OFFSET, 1, state, groups -> playerAxis(state, context.lookZBlock, parseSigned(groups[0])));
        return new PlaceholderResult(replaced, state.faults);
    }

    public static String commandPayload(String message) {
        if (message == null) {
            return "";
        }
        return message.startsWith("/") ? message.substring(1) : "";
    }

    public static boolean isCommandMessage(String message) {
        return message != null && message.startsWith("/");
    }

    public record MacroMessage(String text, int delayTicks) {
        public MacroMessage {
            text = text == null ? "" : text;
            delayTicks = Math.max(0, delayTicks);
        }
    }

    public record MacroDef(
            String key,
            String limitKey,
            String altKey,
            List<MacroMessage> messages,
            String sendMode,
            String conflictStrategy,
            String activationType,
            int spaceTicks,
            int maxRepeats,
            boolean skyblockOnly,
            boolean useRatelimit) {
        public MacroDef {
            key = key == null ? "" : key.strip();
            limitKey = limitKey == null ? "" : limitKey.strip();
            altKey = altKey == null ? "" : altKey.strip();
            messages = messages == null ? List.of() : List.copyOf(messages);
            sendMode = normalizeSendMode(sendMode);
            conflictStrategy = normalizeConflict(conflictStrategy);
            activationType = normalizeActivation(activationType);
            spaceTicks = Math.max(0, spaceTicks);
            maxRepeats = Math.max(0, maxRepeats);
        }
    }

    public record PlaceholderContext(
            String lastSent,
            String lastCmd,
            String clipboard,
            String myName,
            String pmSender,
            Integer x,
            Integer y,
            Integer z,
            Integer lookXBlock,
            Integer lookYBlock,
            Integer lookZBlock,
            double lookAngleX,
            double lookAngleZ,
            List<String> recentChat) {
        public PlaceholderContext {
            lastSent = lastSent == null ? "" : lastSent;
            lastCmd = lastCmd == null ? "" : lastCmd;
            clipboard = clipboard == null ? "" : clipboard;
            myName = myName == null ? "" : myName;
            pmSender = pmSender == null ? "" : pmSender;
            recentChat = recentChat == null ? List.of() : List.copyOf(recentChat);
        }
    }

    public record PlaceholderResult(String text, int faults) {
        public PlaceholderResult {
            text = text == null ? "" : text;
            faults = Math.max(0, faults);
        }

        public boolean ok() {
            return faults == 0;
        }
    }

    public record DueSend(String message, boolean type, boolean edit, boolean addToHistory, boolean showHud) {
        public DueSend {
            message = message == null ? "" : message;
        }
    }

    public record KeyPressResult(int code, List<DueSend> immediate, boolean rateLimited) {
        public KeyPressResult {
            immediate = immediate == null ? List.of() : List.copyOf(immediate);
        }

        public boolean cancelCharTyped() {
            return code != 0;
        }

        public boolean cancelVanilla() {
            return code == 2;
        }
    }

    public static final class RateLimiter {
        private final List<Integer> ages = new ArrayList<>();

        public void tick(int maxTicks) {
            int limit = Math.max(1, maxTicks);
            for (int i = ages.size() - 1; i >= 0; i--) {
                int next = ages.get(i) + 1;
                if (next > limit) {
                    ages.remove(i);
                } else {
                    ages.set(i, next);
                }
            }
        }

        public boolean canTrigger(boolean enforce, int maxCount, boolean strict) {
            int count = Math.max(1, maxCount);
            if (enforce && ages.size() >= count) {
                if (strict) {
                    ages.add(0);
                }
                return false;
            }
            ages.add(0);
            return true;
        }

        int size() {
            return ages.size();
        }
    }

    public static final class Engine {
        private final MacroDef def;
        private final Random random;
        private final List<Scheduled> scheduled = new ArrayList<>();
        private boolean active;
        private boolean waiting;
        private boolean reverseCycle;
        private int activeTicks;
        private int repetitions;
        private int cycleIndex;

        Engine(MacroDef def, Random random) {
            this.def = def;
            this.random = random == null ? new Random() : random;
        }

        MacroDef def() {
            return def;
        }

        boolean keyDown(Predicate<String> down) {
            return down != null && down.test(def.key);
        }

        TriggerResult trigger(boolean reverse, RateLimiter limiter, RateSettings rates) {
            List<DueSend> out = new ArrayList<>();
            if (active) {
                if (!ACTIVATION_RELEASE.equals(def.activationType) || !waiting) {
                    singleActionComplete();
                }
                return new TriggerResult(false, out);
            }
            if (def.useRatelimit
                    && limiter != null
                    && rates != null
                    && !limiter.canTrigger(rates.enforce(), rates.count(), rates.strict())) {
                return new TriggerResult(true, out);
            }
            activate(reverse, out);
            return new TriggerResult(false, out);
        }

        List<DueSend> tick(boolean keyDown) {
            List<DueSend> out = new ArrayList<>();
            scheduled.removeIf(item -> item.tick(out));
            if (!active) {
                return out;
            }
            if (ACTIVATION_HOLD.equals(def.activationType) && !keyDown) {
                deactivate();
                return out;
            }
            if (ACTIVATION_RELEASE.equals(def.activationType) && waiting) {
                if (!keyDown) {
                    waiting = false;
                    doAction(out);
                }
                return out;
            }
            if (MODE_REPEAT.equals(def.sendMode)
                    && (def.spaceTicks == 0 || (activeTicks > 0 && activeTicks % def.spaceTicks == 0))
                    && (def.maxRepeats == 0 || ++repetitions <= def.maxRepeats)) {
                scheduleAll(false, out);
            }
            activeTicks++;
            return out;
        }

        private void activate(boolean reverse, List<DueSend> out) {
            active = true;
            activeTicks = -1;
            repetitions = 1;
            reverseCycle = reverse;
            if (ACTIVATION_RELEASE.equals(def.activationType)) {
                waiting = true;
            } else {
                doAction(out);
            }
        }

        private void deactivate() {
            active = false;
            waiting = false;
        }

        private void singleActionComplete() {
            if (!ACTIVATION_HOLD.equals(def.activationType)) {
                deactivate();
            }
        }

        private void doAction(List<DueSend> out) {
            switch (def.sendMode) {
                case MODE_TYPE -> {
                    if (!def.messages.isEmpty()) {
                        out.add(new DueSend(def.messages.getFirst().text(), true, false, false, false));
                    }
                    singleActionComplete();
                }
                case MODE_EDIT -> {
                    if (!def.messages.isEmpty()) {
                        out.add(new DueSend(def.messages.getFirst().text(), false, true, false, false));
                    }
                    singleActionComplete();
                }
                case MODE_CYCLE -> {
                    if (def.messages.isEmpty()) {
                        singleActionComplete();
                        break;
                    }
                    if (reverseCycle) {
                        if (--cycleIndex < 0) {
                            cycleIndex = def.messages.size() - 1;
                        }
                    } else if (++cycleIndex >= def.messages.size()) {
                        cycleIndex = 0;
                    }
                    MacroMessage current = def.messages.get(cycleIndex);
                    for (String piece : current.text().split(",,")) {
                        if (!piece.isBlank()) {
                            schedule(current.delayTicks(), piece, out);
                        }
                    }
                    singleActionComplete();
                }
                case MODE_RANDOM -> {
                    if (!def.messages.isEmpty()) {
                        MacroMessage chosen = def.messages.get(random.nextInt(def.messages.size()));
                        if (!chosen.text().isBlank()) {
                            schedule(chosen.delayTicks(), chosen.text(), out);
                        }
                    }
                    singleActionComplete();
                }
                case MODE_REPEAT -> scheduleAll(false, out);
                default -> {
                    scheduleAll(def.spaceTicks != 0, out);
                    singleActionComplete();
                }
            }
        }

        private void scheduleAll(boolean standardDelay, List<DueSend> out) {
            int delay = standardDelay ? -def.spaceTicks : 0;
            for (MacroMessage message : def.messages) {
                delay += standardDelay ? def.spaceTicks : message.delayTicks();
                if (!message.text().isBlank()) {
                    schedule(delay, message.text(), out);
                }
            }
        }

        private void schedule(int delay, String message, List<DueSend> out) {
            if (delay > 0) {
                scheduled.add(new Scheduled(delay, message));
            } else {
                out.add(new DueSend(message, false, false, false, false));
            }
        }
    }

    public record RateSettings(boolean enforce, int count, boolean strict) {
    }

    public static final class Session {
        private final List<Engine> engines = new ArrayList<>();
        private final RateLimiter limiter = new RateLimiter();
        private final Random random;
        private String fingerprint = "";

        public Session() {
            this(new Random());
        }

        Session(Random random) {
            this.random = random == null ? new Random() : random;
        }

        public void sync(List<MacroDef> macros) {
            String next = String.valueOf(macros);
            if (next.equals(fingerprint)) {
                return;
            }
            fingerprint = next;
            engines.clear();
            if (macros == null) {
                return;
            }
            for (MacroDef macro : macros) {
                engines.add(new Engine(macro, random));
            }
        }

        public void tickLimiter(int maxTicks) {
            limiter.tick(maxTicks);
        }

        public List<DueSend> tick(Predicate<String> keyDown) {
            List<DueSend> out = new ArrayList<>();
            for (Engine engine : engines) {
                out.addAll(engine.tick(keyDown != null && engine.keyDown(keyDown)));
            }
            return out;
        }

        public KeyPressResult handleKey(
                String pressedKey,
                Predicate<String> keyDown,
                boolean vanillaConflict,
                boolean inSkyblock,
                RateSettings rates) {
            if (blank(pressedKey)) {
                return new KeyPressResult(0, List.of(), false);
            }
            List<Engine> matches = selectEngines(pressedKey, keyDown);
            int code = 0;
            boolean rateLimited = false;
            List<DueSend> immediate = new ArrayList<>();
            for (Engine engine : matches) {
                MacroDef def = engine.def();
                if (def.skyblockOnly && !inSkyblock) {
                    continue;
                }
                if (STRATEGY_AVOID.equals(def.conflictStrategy)) {
                    continue;
                }
                boolean fire = true;
                if (STRATEGY_SUBMIT.equals(def.conflictStrategy) && vanillaConflict) {
                    fire = false;
                }
                if (STRATEGY_VETO.equals(def.conflictStrategy)) {
                    code = 2;
                }
                if (!fire) {
                    continue;
                }
                TriggerResult produced = engine.trigger(
                        sameBind(pressedKey, def.altKey),
                        limiter,
                        rates);
                if (produced.rateLimited()) {
                    rateLimited = true;
                    continue;
                }
                immediate.addAll(produced.sends());
                if (code == 0
                        && (MODE_TYPE.equals(def.sendMode) || MODE_EDIT.equals(def.sendMode))) {
                    code = 1;
                }
            }
            return new KeyPressResult(code, immediate, rateLimited);
        }

        private List<Engine> selectEngines(String pressedKey, Predicate<String> keyDown) {
            List<Engine> limitMatches = new ArrayList<>();
            List<Engine> unbound = new ArrayList<>();
            for (Engine engine : engines) {
                MacroDef def = engine.def();
                boolean primary = sameBind(pressedKey, def.key);
                boolean alt = sameBind(pressedKey, def.altKey);
                if (!primary && !alt) {
                    continue;
                }
                if (primary && !blank(def.limitKey) && keyDown != null && keyDown.test(def.limitKey)) {
                    limitMatches.add(engine);
                } else if (blank(def.limitKey) || alt) {
                    unbound.add(engine);
                }
            }
            return limitMatches.isEmpty() ? unbound : limitMatches;
        }
    }

    private record TriggerResult(boolean rateLimited, List<DueSend> sends) {
        private TriggerResult {
            sends = sends == null ? List.of() : List.copyOf(sends);
        }
    }

    private static void addPreset(
            List<MacroDef> presets,
            String key,
            String command,
            String defaultMode,
            String defaultConflict,
            String defaultActivation,
            boolean useRatelimit) {
        if (blank(key) || blank(command)) {
            return;
        }
        presets.add(new MacroDef(
                key,
                "",
                "",
                List.of(new MacroMessage(command, 0)),
                normalizeSendMode(defaultMode),
                normalizeConflict(defaultConflict),
                normalizeActivation(defaultActivation),
                0,
                0,
                true,
                useRatelimit));
    }

    private static List<String> splitFields(String line) {
        String[] raw = line.split("\\|");
        List<String> parts = new ArrayList<>();
        for (String part : raw) {
            parts.add(part.strip());
        }
        return parts;
    }

    private static int indexOfUnquotedEquals(String text) {
        return text.indexOf('=');
    }

    private static List<MacroMessage> parseMessages(String raw) {
        List<MacroMessage> messages = new ArrayList<>();
        for (String piece : raw.split(",,")) {
            String text = piece.strip();
            if (text.isEmpty()) {
                continue;
            }
            Matcher matcher = DELAY_SUFFIX.matcher(text);
            if (matcher.matches()) {
                messages.add(new MacroMessage(matcher.group(1).strip(), Integer.parseInt(matcher.group(2))));
            } else {
                messages.add(new MacroMessage(text, 0));
            }
        }
        return messages;
    }

    private static boolean sameBindOrBlank(String left, String right) {
        if (blank(left) && blank(right)) {
            return true;
        }
        return sameBind(left, right);
    }

    private static String named(String raw, List<String> options, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String token = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (String option : options) {
            if (option.equals(token)) {
                return option;
            }
        }
        return fallback;
    }

    private static int parseNonNegative(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int parseSigned(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String replaceSimple(
            String message,
            String token,
            PlaceholderState state,
            java.util.function.Supplier<String> supplier) {
        if (!message.contains(token)) {
            return message;
        }
        return message.replace(token, supplier.get());
    }

    private static String replaceRegex(
            String message,
            Pattern pattern,
            int groups,
            PlaceholderState state,
            java.util.function.Function<String[], String> operator) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        boolean found = false;
        while (matcher.find()) {
            found = true;
            String[] args = new String[groups];
            for (int i = 0; i < groups; i++) {
                args[i] = matcher.group(i + 1);
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(operator.apply(args)));
        }
        if (!found) {
            return message;
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String textOrFault(PlaceholderState state, String value) {
        if (blank(value)) {
            return state.fault();
        }
        return value;
    }

    private static String clipboard(PlaceholderState state, String regex) {
        String value = state.context.clipboard;
        if (blank(value)) {
            return state.fault();
        }
        if (regex != null) {
            try {
                if (!Pattern.compile(regex).matcher(value).find()) {
                    return state.fault();
                }
            } catch (PatternSyntaxException ignored) {
                return state.fault();
            }
        }
        return value;
    }

    private static String recentChat(PlaceholderState state, String regex) {
        try {
            Pattern pattern = Pattern.compile(regex);
            int checked = 0;
            for (String line : state.context.recentChat) {
                if (++checked > RECENT_CHAT_SCAN) {
                    break;
                }
                Matcher matcher = pattern.matcher(line == null ? "" : line);
                if (matcher.find()) {
                    try {
                        return matcher.group(1);
                    } catch (IndexOutOfBoundsException ignored) {
                        return state.fault();
                    }
                }
            }
        } catch (PatternSyntaxException ignored) {
            return state.fault();
        }
        return state.fault();
    }

    private static String playerPos(PlaceholderState state, String dir, String amount) {
        PlaceholderContext context = state.context;
        if (context.x == null || context.y == null || context.z == null) {
            return state.fault();
        }
        int offset = parseNonNegative(amount);
        double x = context.x + 0.5D;
        double y = context.y;
        double z = context.z + 0.5D;
        if (offset != 0) {
            double[] shifted = offsetCardinal(x, y, z, context.lookAngleX, context.lookAngleZ, dir, offset);
            x = shifted[0];
            y = shifted[1];
            z = shifted[2];
        }
        return floor(x) + " " + floor(y) + " " + floor(z);
    }

    private static String lookPos(PlaceholderState state, String dir, String amount) {
        PlaceholderContext context = state.context;
        if (context.lookXBlock == null || context.lookYBlock == null || context.lookZBlock == null) {
            return state.fault();
        }
        int offset = parseNonNegative(amount);
        double x = context.lookXBlock + 0.5D;
        double y = context.lookYBlock;
        double z = context.lookZBlock + 0.5D;
        if (offset != 0) {
            double[] shifted = offsetCardinal(x, y, z, context.lookAngleX, context.lookAngleZ, dir, offset);
            x = shifted[0];
            y = shifted[1];
            z = shifted[2];
        }
        return floor(x) + " " + floor(y) + " " + floor(z);
    }

    private static String playerAxis(PlaceholderState state, Integer value, int offset) {
        if (value == null) {
            return state.fault();
        }
        return String.valueOf(value + offset);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static double[] offsetCardinal(
            double x,
            double y,
            double z,
            double lookX,
            double lookZ,
            String dir,
            int offset) {
        String token = dir == null ? "" : dir;
        if (Math.abs(lookX) >= Math.abs(lookZ)) {
            if (lookX >= 0.0D) {
                return switch (token) {
                    case "F" -> new double[]{x + offset, y, z};
                    case "B" -> new double[]{x - offset, y, z};
                    case "L" -> new double[]{x, y, z - offset};
                    case "R" -> new double[]{x, y, z + offset};
                    default -> new double[]{x, y, z};
                };
            }
            return switch (token) {
                case "F" -> new double[]{x - offset, y, z};
                case "B" -> new double[]{x + offset, y, z};
                case "L" -> new double[]{x, y, z + offset};
                case "R" -> new double[]{x, y, z - offset};
                default -> new double[]{x, y, z};
            };
        }
        if (lookZ >= 0.0D) {
            return switch (token) {
                case "F" -> new double[]{x, y, z + offset};
                case "B" -> new double[]{x, y, z - offset};
                case "L" -> new double[]{x + offset, y, z};
                case "R" -> new double[]{x - offset, y, z};
                default -> new double[]{x, y, z};
            };
        }
        return switch (token) {
            case "F" -> new double[]{x, y, z - offset};
            case "B" -> new double[]{x, y, z + offset};
            case "L" -> new double[]{x - offset, y, z};
            case "R" -> new double[]{x + offset, y, z};
            default -> new double[]{x, y, z};
        };
    }

    private static final class PlaceholderState {
        private final PlaceholderContext context;
        private int faults;

        private PlaceholderState(PlaceholderContext context) {
            this.context = context == null
                    ? new PlaceholderContext("", "", "", "", "", null, null, null, null, null, null, 0.0D, 0.0D, List.of())
                    : context;
        }

        private String fault() {
            faults++;
            return FAULT_TOKEN;
        }
    }

    private static final class Scheduled {
        private int delay;
        private final String message;

        private Scheduled(int delay, String message) {
            this.delay = delay;
            this.message = message;
        }

        private boolean tick(List<DueSend> out) {
            if (--delay <= 0) {
                out.add(new DueSend(message, false, false, false, false));
                return true;
            }
            return false;
        }
    }
}
