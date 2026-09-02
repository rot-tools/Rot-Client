package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.ChatScreenAccessor;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Hotkey-sequence runtime: presets stay SkyBlock-gated, while custom macros
 * support send/type/edit/cycle/random/repeat plus placeholders.
 */
public final class RingKeybindsRuntime {
    private static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("Macro").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    private static final RingPolicy.Session SESSION = new RingPolicy.Session();
    private static final List<String> RECENT_CHAT = new ArrayList<>();
    private static String lastCommand = "";
    private static String pmSender = "";
    private static boolean cancelCharTyped;
    private static long cancelCharTypedAt;

    private RingKeybindsRuntime() {
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.commandKeybindsEnabled || client == null || client.player == null) {
            SESSION.sync(List.of());
            return;
        }
        SESSION.sync(macros(qol));
        SESSION.tickLimiter(RingPolicy.clampRatelimitTicks(qol.commandBindRatelimitTicks));
        if (client.level == null || client.isPaused()) {
            return;
        }
        long window = client.getWindow() == null ? 0L : client.getWindow().handle();
        for (RingPolicy.DueSend send : SESSION.tick(name ->
                QolKeybindNames.isBoundDown(window, name))) {
            dispatch(client, qol, send);
        }
    }

    public static boolean onKeyPress(int glfwKey, int action) {
        if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_REPEAT) {
            return false;
        }
        return handlePress(QolKeybindNames.formatGlfwKey(glfwKey), keyOf(glfwKey));
    }

    public static boolean onMousePress(int button, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        return handlePress(
                QolKeybindNames.formatMouseButton(button),
                InputConstants.Type.MOUSE.getOrCreate(button));
    }

    public static boolean shouldCancelCharTyped() {
        if (!cancelCharTyped) {
            return false;
        }
        cancelCharTyped = false;
        return System.nanoTime() - cancelCharTypedAt < 5_000_000L;
    }

    public static void rememberOutgoingCommand(String command) {
        if (command != null && !command.isBlank()) {
            lastCommand = command.startsWith("/") ? command.substring(1) : command;
        }
    }

    public static void rememberChat(Component message) {
        if (message == null) {
            return;
        }
        String text = message.getString();
        if (text == null || text.isBlank()) {
            return;
        }
        RECENT_CHAT.addFirst(text);
        while (RECENT_CHAT.size() > RingPolicy.RECENT_CHAT_SCAN) {
            RECENT_CHAT.remove(RECENT_CHAT.size() - 1);
        }
        if (message.getContents() instanceof TranslatableContents contents
                && contents.getKey() != null
                && contents.getKey().contains("commands.message.display.incoming")
                && contents.getArgs().length > 0
                && contents.getArgs()[0] instanceof Component name) {
            pmSender = name.getString();
        }
    }

    private static boolean handlePress(String keyName, InputConstants.Key vanillaKey) {
        Minecraft client = Minecraft.getInstance();
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.commandKeybindsEnabled
                || client == null
                || client.player == null
                || client.level == null
                || client.isPaused()
                || keyName == null
                || keyName.isBlank()) {
            return false;
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (screen != null) {
            return false;
        }
        SESSION.sync(macros(qol));
        long window = client.getWindow() == null ? 0L : client.getWindow().handle();
        boolean enforceLimit = client.getSingleplayerServer() == null || qol.commandBindRatelimitSp;
        RingPolicy.KeyPressResult result = SESSION.handleKey(
                keyName,
                name -> QolKeybindNames.isBoundDown(window, name),
                hasVanillaConflict(client, vanillaKey),
                SkyBlockAreaDetector.isInSkyblock(),
                new RingPolicy.RateSettings(
                        enforceLimit,
                        RingPolicy.clampRatelimitCount(qol.commandBindRatelimitCount),
                        qol.commandBindRatelimitStrict));
        if (result.rateLimited()) {
            notifyRateLimit(client, keyName, qol);
        }
        for (RingPolicy.DueSend send : result.immediate()) {
            dispatch(client, qol, send);
        }
        if (result.cancelCharTyped()) {
            cancelCharTyped = true;
            cancelCharTypedAt = System.nanoTime();
        }
        return result.cancelVanilla();
    }

    private static List<RingPolicy.MacroDef> macros(QolUtilityConfig qol) {
        return RingPolicy.mergeMacros(
                RingPolicy.parseMacroList(
                        qol.commandBindMacros,
                        qol.commandBindSendMode,
                        qol.commandBindConflict,
                        qol.commandBindActivation,
                        qol.commandBindUseRatelimit),
                RingPolicy.skyblockPresets(
                        qol.commandPetsKey,
                        qol.commandStorageKey,
                        qol.commandArmorWardrobeKey,
                        qol.commandEquipWardrobeKey,
                        qol.commandLoadoutsKey,
                        qol.commandStatsKey,
                        qol.commandDungeonHubKey,
                        qol.commandPotionBagKey,
                        qol.commandBindSendMode,
                        qol.commandBindConflict,
                        qol.commandBindActivation,
                        qol.commandBindUseRatelimit));
    }

    private static void dispatch(Minecraft client, QolUtilityConfig qol, RingPolicy.DueSend send) {
        if (send == null || send.message().isBlank() || client.player == null) {
            return;
        }
        RingPolicy.PlaceholderResult replaced = RingPolicy.replacePlaceholders(
                send.message(),
                context(client));
        if (!replaced.ok()) {
            client.gui.hud.getChat().addClientSystemMessage(PREFIX.copy().append(
                    Component.literal("Placeholder failed: " + replaced.text())
                            .withStyle(ChatFormatting.RED)));
            return;
        }
        if (send.type() || send.edit()) {
            openChat(client, replaced.text());
            return;
        }
        int limit = RingPolicy.clampLengthLimit(qol.commandBindLengthLimit);
        if (replaced.text().length() > limit) {
            client.gui.hud.getChat().addClientSystemMessage(PREFIX.copy().append(
                    Component.literal("Blocked, length " + replaced.text().length()
                                    + " > " + limit)
                            .withStyle(ChatFormatting.RED)));
            return;
        }
        LocalPlayer player = client.player;
        if (RingPolicy.isCommandMessage(replaced.text())) {
            player.connection.sendCommand(RingPolicy.commandPayload(replaced.text()));
        } else {
            player.connection.sendChat(replaced.text());
        }
        if (qol.commandBindAddHistory) {
            client.gui.hud.getChat().addRecentChat(replaced.text());
        }
        if (qol.commandBindShowHud) {
            client.gui.hud.setOverlayMessage(
                    Component.literal(replaced.text()).withStyle(ChatFormatting.GRAY),
                    false);
        }
    }

    private static void openChat(Minecraft client, String message) {
        ChatScreen screen = new ChatScreen(message, false);
        client.gui.setScreen(screen);
        int editAt = message.indexOf(RingPolicy.EDIT_TOKEN);
        if (editAt < 0 || !(screen instanceof ChatScreenAccessor accessor)) {
            return;
        }
        EditBox input = accessor.rotclient$input();
        if (input == null) {
            return;
        }
        input.moveCursorTo(editAt + RingPolicy.EDIT_TOKEN.length(), false);
        input.moveCursorTo(editAt, true);
    }

    private static RingPolicy.PlaceholderContext context(Minecraft client) {
        LocalPlayer player = client.player;
        String lastSent = "";
        ArrayListDeque<String> recent = client.gui.hud.getChat().getRecentChat();
        if (recent != null && recent.peekLast() != null) {
            lastSent = recent.peekLast();
        }
        String clipboard = "";
        if (client.keyboardHandler != null && client.keyboardHandler.getClipboard() != null) {
            clipboard = client.keyboardHandler.getClipboard();
        }
        Integer lookX = null;
        Integer lookY = null;
        Integer lookZ = null;
        if (player != null) {
            HitResult hit = player.pick(384.0D, 0.0F, false);
            if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = block.getBlockPos();
                lookX = pos.getX();
                lookY = pos.getY();
                lookZ = pos.getZ();
            }
        }
        return new RingPolicy.PlaceholderContext(
                lastSent,
                lastCommand,
                clipboard,
                player == null ? "" : player.getName().getString(),
                pmSender,
                player == null ? null : player.blockPosition().getX(),
                player == null ? null : player.blockPosition().getY(),
                player == null ? null : player.blockPosition().getZ(),
                lookX,
                lookY,
                lookZ,
                player == null ? 0.0D : player.getLookAngle().x,
                player == null ? 0.0D : player.getLookAngle().z,
                List.copyOf(RECENT_CHAT));
    }

    private static boolean hasVanillaConflict(Minecraft client, InputConstants.Key key) {
        if (key == null || client.options == null) {
            return false;
        }
        for (KeyMapping mapping : client.options.keyMappings) {
            if (mapping == null || mapping.getCategory().equals(KeyMapping.Category.DEBUG)) {
                continue;
            }
            if (((KeyMappingAccessor) mapping).rotclient$boundKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    private static InputConstants.Key keyOf(int glfwKey) {
        return InputConstants.Type.KEYSYM.getOrCreate(glfwKey);
    }

    private static void notifyRateLimit(Minecraft client, String keyName, QolUtilityConfig qol) {
        client.gui.hud.getChat().addClientSystemMessage(PREFIX.copy().append(
                Component.literal("Rate limit: " + keyName + " ("
                                + RingPolicy.clampRatelimitCount(qol.commandBindRatelimitCount)
                                + " / "
                                + RingPolicy.clampRatelimitTicks(qol.commandBindRatelimitTicks)
                                + " ticks)")
                        .withStyle(ChatFormatting.RED)));
    }
}
