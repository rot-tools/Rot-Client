package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Auto Conversation: click the first matching NPC {@code /command}
 * option from chat after the configured delay.
 */
final class AutoConversationRuntime {
    private record Pending(int ticks, String command) {
    }

    private static final List<Pending> QUEUE = new CopyOnWriteArrayList<>();

    private AutoConversationRuntime() {
    }

    static void clear() {
        QUEUE.clear();
    }

    static void onChat(Component message) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.autoConversationEnabled || message == null) {
            return;
        }
        String stripped = AutoConversationPolicy.stripFormatting(message.getString());
        if (!AutoConversationPolicy.isNpcPrompt(stripped)) {
            return;
        }
        List<AutoConversationPolicy.ClickOption> options = new ArrayList<>();
        collect(message, options);
        List<String> commands = AutoConversationPolicy.selectCommands(
                options, qol.autoConversationGreen, qol.autoConversationMulti);
        if (commands.isEmpty()) {
            return;
        }
        String command = commands.getFirst();
        int delay = AutoConversationPolicy.clampDelayTicks(qol.autoConversationDelayTicks);
        if (delay <= 0) {
            send(command);
            return;
        }
        int wait = AutoConversationPolicy.scheduledDelayTicks(
                delay, ThreadLocalRandom.current().nextInt(0, AutoConversationPolicy.JITTER_TICKS + 1));
        QUEUE.add(new Pending(wait, command));
    }

    static void tick(Minecraft client) {
        if (QUEUE.isEmpty()) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.autoConversationEnabled) {
            QUEUE.clear();
            return;
        }
        List<Pending> next = new ArrayList<>();
        for (Pending pending : QUEUE) {
            if (pending.ticks() <= 0) {
                send(pending.command());
            } else {
                next.add(new Pending(pending.ticks() - 1, pending.command()));
            }
        }
        QUEUE.clear();
        QUEUE.addAll(next);
    }

    private static void collect(Component component, List<AutoConversationPolicy.ClickOption> out) {
        if (component == null) {
            return;
        }
        Style style = component.getStyle();
        String command = runCommand(style);
        if (command != null && !command.isBlank()) {
            TextColor color = style.getColor();
            int rgb = color == null ? 0 : color.getValue();
            out.add(new AutoConversationPolicy.ClickOption(command, rgb, component.getString()));
        }
        for (Component sibling : component.getSiblings()) {
            collect(sibling, out);
        }
    }

    private static String runCommand(Style style) {
        if (style == null) {
            return null;
        }
        ClickEvent event = style.getClickEvent();
        if (event instanceof ClickEvent.RunCommand run) {
            return run.command();
        }
        return null;
    }

    private static void send(String command) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || player.connection == null || command == null || command.isBlank()) {
            return;
        }
        player.connection.sendCommand(command);
    }
}
