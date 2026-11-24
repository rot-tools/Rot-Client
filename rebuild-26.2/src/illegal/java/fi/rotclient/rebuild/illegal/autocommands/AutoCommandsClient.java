// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class AutoCommandsClient {
    static final CommandShortcut.Notice SUCCESS_PREVIEW = WarpAttempt.arrived("Dwarven Base Camp");
    static final CommandShortcut.Notice FAILURE_PREVIEW = WarpAttempt.failed("Destination is locked");
    static final int MODIFIERS = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER;
    private static AutoCommandsClient instance;
    private final PopupState popup = new PopupState();
    private Connection popupConnection;
    private PopupConfig config;
    private WarpAttempt warp;
    private Connection warpConnection;

    public void register() {
        if (FabricLoader.getInstance().isModLoaded("autocommands")) return;
        config = PopupConfig.load();
        instance = this;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientSendMessageEvents.COMMAND.register(raw -> {
            Minecraft client = Minecraft.getInstance();
            var listener = client.getConnection();
            if (!enabled() || listener == null || client.player == null || !client.player.isAlive()) return;
            var command = new CommandShortcut("/" + raw, "", "");
            if (!command.isTravelCommand()) return;
            var destination = WarpCatalog.find(WarpCatalog.normalized(command.command()));
            warp = new WarpAttempt(System.nanoTime(), SkyBlockLocation.read(listener), destination == null ? null : destination.name());
            warpConnection = listener.getConnection();
            popup.clear();
        });
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) onServerMessage(message);
            return true;
        });
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> { if (!overlay) onServerMessage(message); });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("rotclient_illegal", "warp_popup"), (graphics, delta) -> render(graphics));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("autocom")
                        .requires(FabricClientCommandSource::attended)
                        .executes(context -> {
                            Minecraft client = context.getSource().getClient();
                            client.schedule(() -> client.gui.setScreen(settingsScreen(null)));
                            return 1;
                        })
                        .then(literal("test")
                                .executes(context -> preview(context.getSource(), SUCCESS_PREVIEW))
                                .then(literal("failed").executes(context -> preview(context.getSource(), FAILURE_PREVIEW))))));

        LoggerFactory.getLogger("rotclient-illegal-autocommands").info("AutoCommands initialized");
    }

    public static net.minecraft.client.gui.screens.Screen settingsScreen(
            net.minecraft.client.gui.screens.Screen parent) {
        if (instance == null) return parent;
        return new CommandDashboard(instance.config, parent);
    }

    public static PopupConfig config() { return instance == null ? null : instance.config; }

    public static void showShortcut(Minecraft client, CommandShortcut shortcut) {
        if (instance != null && instance.enabled() && !shortcut.isTravelCommand()) {
            instance.show(client, shortcut.notice());
        }
    }

    private boolean enabled() {
        return config.enabled && IllegalEdition.autoCommandsEnabled();
    }

    public static void onRespawn(ClientPacketListener listener) {
        if (instance != null && instance.isPendingOn(listener)) instance.warp.worldChanged();
    }

    public static void onPosition(ClientPacketListener listener, ClientboundPlayerPositionPacket packet) {
        if (instance == null || !instance.isPendingOn(listener)) return;
        var player = Minecraft.getInstance().player;
        if (player == null || !player.isAlive()) return;
        var relatives = packet.relatives();
        boolean absolute = !relatives.contains(Relative.X) && !relatives.contains(Relative.Y) && !relatives.contains(Relative.Z);
        var before = PositionMoveRotation.of(player);
        var target = PositionMoveRotation.calculateAbsolute(before, packet.change(), relatives);
        boolean moved = absolute && before.position().distanceToSqr(target.position()) >= 4;
        instance.warp.positioned(System.nanoTime(), absolute, moved);
    }

    private boolean isPendingOn(ClientPacketListener listener) {
        return warp != null && listener != null && listener.getConnection() == warpConnection;
    }

    private void onServerMessage(Component message) {
        Minecraft client = Minecraft.getInstance();
        if (!isPendingOn(client.getConnection())) return;
        String reason = WarpFeedback.failure(message.getString());
        if (reason != null) finish(client, WarpAttempt.failed(reason));
    }

    private void tick(Minecraft client) {
        var listener = client.getConnection();
        Connection connection = listener == null ? null : listener.getConnection();
        if (popupConnection != connection || !enabled()) popup.clear();
        if (warp == null) return;
        if (!enabled() || (connection != null && connection != warpConnection)
                || client.gui.screen() instanceof DisconnectedScreen || (client.player != null && !client.player.isAlive())) {
            resetWarp();
            return;
        }
        if (warp.expiredWhileLoading(System.nanoTime())) { resetWarp(); return; }
        if (listener == null) return; // Keep the request during a server's configuration phase.
        boolean worldReady = client.level != null && client.player != null && client.gui.overlay() == null
                && !(client.gui.screen() instanceof LevelLoadingScreen);
        var location = worldReady ? SkyBlockLocation.read(listener) : WarpAttempt.Location.UNKNOWN;
        var result = warp.outcome(System.nanoTime(), location, worldReady);
        if (result != null) finish(client, result);
    }

    private int preview(FabricClientCommandSource source, CommandShortcut.Notice notice) {
        if (!enabled()) {
            source.sendFeedback(Component.literal("[AutoCommands] Enable AutoCommands in /rot and popups in Popup Style."));
            return 0;
        }
        show(source.getClient(), notice);
        return 1;
    }

    private void finish(Minecraft client, CommandShortcut.Notice notice) {
        resetWarp();
        if (enabled()) show(client, notice);
    }

    private void resetWarp() {
        warp = null;
        warpConnection = null;
    }

    private void show(Minecraft client, CommandShortcut.Notice notice) {
        var listener = client.getConnection();
        popupConnection = listener == null ? null : listener.getConnection();
        popup.show(notice, System.nanoTime());
    }

    private void render(GuiGraphicsExtractor graphics) {
        if (!enabled() || popup.notice() == null) return;
        float opacity = popup.opacity(System.nanoTime(), config.durationSeconds);
        if (opacity > 0.02f) PopupRenderer.draw(graphics, config, popup.notice(), opacity);
    }
}
