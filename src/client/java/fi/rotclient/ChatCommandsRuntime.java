package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Incoming {@code !} chat helpers and outgoing emote replacement for the
 * Serveri. Decisions live in {@link ChatCommandsPolicy}.
 */
public final class ChatCommandsRuntime {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT);
    private static final List<Pending> QUEUE = new ArrayList<>();
    private static String cachedRulesSource = "";
    private static List<ChatCommandsPolicy.ChatRule> cachedRules = List.of();
    private static boolean toggleWasDown;

    private record Pending(int ticksLeft, ChatCommandsPolicy.Incoming incoming) {
    }

    private ChatCommandsRuntime() {
    }

    static void clear() {
        QUEUE.clear();
        cachedRulesSource = "";
        cachedRules = List.of();
        toggleWasDown = false;
    }

    public static String modifyOutgoing(String message) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled) {
            return message;
        }
        return applyEmotes(ChatCommandsPolicy.chatFromShortcut(
                true, qol.chatCommandsShortcuts, message));
    }

    public static String applyEmotes(String message) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled) {
            return message;
        }
        return ChatCommandsPolicy.applyEmotes(message, qol.chatEmotes).orElse(message);
    }

    public static java.util.Optional<String> outgoingCommand(String message) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled) {
            return java.util.Optional.empty();
        }
        return ChatCommandsPolicy.commandFromShortcut(true, qol.chatCommandsShortcuts, message);
    }

    public static boolean shouldHideIncoming(Component message) {
        if (message == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled) {
            return false;
        }
        return ChatCommandsPolicy.shouldHideIncoming(
                true,
                rules(qol.chatCommandsRules),
                message.getString());
    }

    public static Component applyIncoming(Component message) {
        if (message == null) {
            return null;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled) {
            return message;
        }
        String replaced = ChatCommandsPolicy.applyIncomingReplacements(
                true,
                rules(qol.chatCommandsRules),
                message.getString());
        if (replaced.equals(message.getString())) {
            return message;
        }
        return Component.literal(replaced);
    }

    private static List<ChatCommandsPolicy.ChatRule> rules(String source) {
        String normalized = source == null ? "" : source;
        if (!normalized.equals(cachedRulesSource)) {
            cachedRulesSource = normalized;
            cachedRules = List.copyOf(ChatCommandsPolicy.parseRules(normalized));
        }
        return cachedRules;
    }

    static void onGameMessage(Component message) {
        if (message == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.chatCommandsEnabled) {
            return;
        }
        ChatCommandsPolicy.parseIncoming(message.getString()).ifPresent(incoming -> {
            if (!ChatCommandsPolicy.channelEnabled(
                    incoming.channel(),
                    qol.chatPartyCommands,
                    qol.chatGuildCommands,
                    qol.chatPrivateCommands)) {
                return;
            }
            QUEUE.add(new Pending(ChatCommandsPolicy.PROCESS_DELAY_TICKS, incoming));
        });
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (client == null || client.player == null) {
            clear();
            return;
        }
        tickToggle(client, qol);
        if (!qol.chatCommandsEnabled) {
            QUEUE.clear();
            return;
        }
        List<Pending> next = new ArrayList<>();
        for (Pending pending : QUEUE) {
            int left = pending.ticksLeft() - 1;
            if (left > 0) {
                next.add(new Pending(left, pending.incoming()));
            } else {
                dispatch(client, qol, pending.incoming());
            }
        }
        QUEUE.clear();
        QUEUE.addAll(next);
    }

    private static void tickToggle(Minecraft client, QolUtilityConfig qol) {
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (screen instanceof ChatScreen) {
            toggleWasDown = false;
            return;
        }
        boolean down = QolKeybindNames.isBoundDown(
                client.getWindow().handle(), qol.chatCommandsKeybind);
        if (down && !toggleWasDown) {
            qol.chatCommandsEnabled = !qol.chatCommandsEnabled;
            TrackerStore.save(RotClientClient.trackerConfig());
            client.player.sendSystemMessage(RotClientChat.message(
                    "Chat Commands " + (qol.chatCommandsEnabled ? "enabled." : "disabled.")));
        }
        toggleWasDown = down;
    }

    private static void dispatch(
            Minecraft client,
            QolUtilityConfig qol,
            ChatCommandsPolicy.Incoming incoming) {
        LocalPlayer player = client.player;
        if (player == null || player.connection == null) {
            return;
        }
        ChatCommandsPolicy.Context context = context(client, player);
        var decision = "time".equalsIgnoreCase(incoming.body().substring(1).trim().split("\\s+")[0])
                ? ChatCommandsPolicy.decideTime(
                        incoming,
                        true,
                        qol.chatPartyCommands,
                        qol.chatGuildCommands,
                        qol.chatPrivateCommands,
                        ZonedDateTime.now(ZoneId.systemDefault()).format(TIME_FORMAT))
                : ChatCommandsPolicy.decide(
                        incoming,
                        true,
                        qol.chatPartyCommands,
                        qol.chatGuildCommands,
                        qol.chatPrivateCommands,
                        context);
        if (decision.isEmpty()) {
            return;
        }
        ChatCommandsPolicy.Decision action = decision.get();
        if (action.kind() == ChatCommandsPolicy.ActionKind.REPLY) {
            player.connection.sendCommand(ChatCommandsPolicy.replyCommand(
                    incoming.channel(), incoming.sender(), action.payload()));
            return;
        }
        player.connection.sendCommand(action.payload());
    }

    private static ChatCommandsPolicy.Context context(Minecraft client, LocalPlayer player) {
        int ping = 0;
        if (client.getConnection() != null) {
            PlayerInfo info = client.getConnection().getPlayerInfo(player.getUUID());
            if (info != null) {
                ping = Math.max(0, info.getLatency());
            }
        }
        String holding = player.getMainHandItem().isEmpty()
                ? ""
                : ChatCommandsPolicy.stripFormatting(
                        player.getMainHandItem().getHoverName().getString());
        SkyBlockArea area = SkyBlockAreaDetector.detect();
        String location = area == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA
                ? "Unknown"
                : area.displayName();
        return new ChatCommandsPolicy.Context(
                player.getScoreboardName(),
                player.getBlockX(),
                player.getBlockY(),
                player.getBlockZ(),
                ping,
                client.getFps(),
                "20.0",
                location,
                holding,
                true,
                List.of(),
                ThreadLocalRandom.current().nextDouble());
    }
}
