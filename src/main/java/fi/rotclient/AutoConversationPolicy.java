package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Auto Conversation: click NPC {@code /...} options from chat.
 * Green is Minecraft {@code §a} / RGB {@code 0x55FF55}.
 */
public final class AutoConversationPolicy {
    public static final int GREEN_RGB = 0x55FF55;
    public static final int MIN_DELAY_TICKS = 0;
    public static final int MAX_DELAY_TICKS = 40;
    public static final int DEFAULT_DELAY_TICKS = 4;
    public static final int JITTER_TICKS = 3;

    public record ClickOption(String command, int rgb, String colorCoded) {
        public ClickOption {
            command = command == null ? "" : command.trim();
            colorCoded = colorCoded == null ? "" : colorCoded;
        }
    }

    private AutoConversationPolicy() {
    }

    public static int clampDelayTicks(int ticks) {
        return Math.max(MIN_DELAY_TICKS, Math.min(MAX_DELAY_TICKS, ticks));
    }

    public static boolean isNpcPrompt(String stripped) {
        if (stripped == null) {
            return false;
        }
        String line = stripped.trim();
        return line.startsWith("[NPC] ") || line.startsWith("Select an option:");
    }

    public static boolean isGreen(int rgb, String colorCoded) {
        if (rgb == GREEN_RGB) {
            return true;
        }
        if (colorCoded == null) {
            return false;
        }
        int bracket = colorCoded.lastIndexOf('[');
        String tail = bracket >= 0 ? colorCoded.substring(bracket + 1) : colorCoded;
        return tail.startsWith("§a") || tail.startsWith("&a");
    }

    public static List<String> selectCommands(
            List<ClickOption> options,
            boolean checkGreen,
            boolean allowMulti) {
        List<String> commands = new ArrayList<>();
        if (options == null) {
            return commands;
        }
        for (ClickOption option : options) {
            if (option.command.isBlank()) {
                continue;
            }
            if (checkGreen && !isGreen(option.rgb, option.colorCoded)) {
                continue;
            }
            commands.add(normalizeCommand(option.command));
        }
        if (commands.size() > 1 && !allowMulti) {
            return List.of();
        }
        return List.copyOf(commands);
    }

    public static String normalizeCommand(String raw) {
        if (raw == null) {
            return "";
        }
        String command = raw.trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        return command;
    }

    public static int scheduledDelayTicks(int delayTicks, int jitterRollInclusive) {
        int delay = clampDelayTicks(delayTicks);
        int jitter = Math.max(0, Math.min(JITTER_TICKS, jitterRollInclusive));
        return delay + jitter;
    }

    public static String stripFormatting(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("§.", "").replaceAll("&[0-9a-fk-or]", "");
    }

    public static boolean looksLikeNpcLine(String strippedLower) {
        if (strippedLower == null) {
            return false;
        }
        String line = strippedLower.trim().toLowerCase(Locale.ROOT);
        return line.startsWith("[npc] ") || line.startsWith("select an option:");
    }
}
