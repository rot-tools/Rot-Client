package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Minecraft bridge for {@link IotaPolicy}: toggle left/right click,
 * party/limbo, mutes, and arrow tracker.
 */
public final class IotaRuntime {
    private static boolean leftLatched;
    private static boolean rightLatched;
    private static boolean leftToggleWasDown;
    private static boolean rightToggleWasDown;
    private static boolean consumedPartyCommand;
    private static double leftAccumulator;
    private static double rightAccumulator;
    private static IotaPolicy.ArrowSnapshot arrows = IotaPolicy.ArrowSnapshot.empty();
    private static boolean waitingForQuiverRefresh;
    private static String lockedArrowType = "Unknown";
    private static int lockedArrowCount;
    private static String overlayTitle = "";
    private static long overlayUntilMs;

    private IotaRuntime() {
    }

    static void clear() {
        leftLatched = false;
        rightLatched = false;
        leftToggleWasDown = false;
        rightToggleWasDown = false;
        consumedPartyCommand = false;
        leftAccumulator = 0.0D;
        rightAccumulator = 0.0D;
        arrows = IotaPolicy.ArrowSnapshot.empty();
        waitingForQuiverRefresh = false;
        overlayTitle = "";
        overlayUntilMs = 0L;
        IotaKuudraRuntime.clear();
    }

    static boolean leftClickLatched() {
        return extras().iotaAddonsEnabled && leftLatched;
    }

    static boolean rightClickLatched() {
        return extras().iotaAddonsEnabled && rightLatched;
    }

    static boolean consumedPartyCommand() {
        return consumedPartyCommand;
    }

    public static boolean shouldMuteTerminatorChat(String raw) {
        QolSkyblockExtras extras = extras();
        return IotaPolicy.shouldMuteTerminator(
                extras.iotaAddonsEnabled,
                extras.iotaMuteTerminator,
                inSkyblock(),
                holdingTerminator())
                && IotaPolicy.isTerminatorCooldownChat(raw);
    }

    public static boolean shouldMuteTerminatorSound() {
        QolSkyblockExtras extras = extras();
        return IotaPolicy.shouldMuteTerminator(
                extras.iotaAddonsEnabled,
                extras.iotaMuteTerminator,
                inSkyblock(),
                holdingTerminator());
    }

    public static boolean shouldMuteFishingCast() {
        QolSkyblockExtras extras = extras();
        return IotaPolicy.shouldMuteFishingCast(extras.iotaAddonsEnabled, extras.iotaMuteFishingCast);
    }

    public static boolean shouldFixFishingHook() {
        QolSkyblockExtras extras = extras();
        return IotaPolicy.shouldFixFishingHook(extras.iotaAddonsEnabled, extras.iotaFixFishingHook);
    }

    public static boolean shouldMuteSound(String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return false;
        }
        String id = soundId.toLowerCase(Locale.ROOT);
        if (shouldMuteFishingCast() && id.contains("fishing_bobber.throw")) {
            return true;
        }
        return shouldMuteTerminatorSound() && id.contains("enderman.teleport");
    }

    static void onGameMessage(Component message) {
        consumedPartyCommand = false;
        if (message == null) {
            return;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.iotaAddonsEnabled) {
            return;
        }
        String raw = message.getString();
        if (extras.iotaLimboAlert && IotaPolicy.isLimboKick(raw)) {
            sendParty(IotaPolicy.LIMBO_PARTY);
            playDragonGrowl();
        }
        if (extras.iotaPartyJoinSound && IotaPolicy.isPartyJoin(raw)) {
            playJoinSound();
        }
        if (extras.iotaArrowTracker) {
            IotaPolicy.arrowChatNotice(raw).ifPresent(notice -> applyArrowNotice(extras, notice));
        }
        if (extras.iotaAutoRequeue && IotaPolicy.isKuudraDefeatChat(raw)) {
            String instance = extras.iotaLastKuudraInstance;
            if (instance != null && !instance.isBlank()) {
                sendCommand("joininstance " + instance);
            }
        }
        IotaKuudraRuntime.onGameMessage(message);
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        String local = player == null ? "" : player.getGameProfile().name();
        int ping = pingMs(player);
        IotaPolicy.partyCommand(
                raw,
                true,
                extras.partyCommandConfig(),
                local,
                ping,
                20.0F,
                extras.iotaChestCount,
                extras.iotaRunCount,
                extras.iotaFailedRunCount,
                extras.iotaAverageRunSeconds,
                extras.iotaProfitCoins,
                extras.iotaHourlyRateCoins).ifPresent(action -> {
            consumedPartyCommand = true;
            rememberKuudraInstance(action.payload());
            if (action.kind() == IotaPolicy.PartyActionKind.COMMAND) {
                sendCommand(action.payload());
            } else {
                sendParty(action.payload());
            }
        });
    }

    static void tick(Minecraft client) {
        QolSkyblockExtras extras =
                extras();

        if (client == null
                || client.player == null
                || !extras.iotaAddonsEnabled) {

            clear();
            return;
        }

        tickToggles(
                client,
                extras);

        tickArrowTracker(
                client,
                extras);

        tickStandaloneClicker(
                client,
                extras);

        IotaKuudraRuntime.tick(
                client);

        if (overlayUntilMs > 0L
                && System.currentTimeMillis()
                > overlayUntilMs) {

            overlayTitle = "";
        }
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        QolSkyblockExtras extras = qol.extras();
        if (!IotaPolicy.showArrowHud(
                extras.iotaAddonsEnabled,
                extras.iotaArrowTracker,
                arrows.tracked(),
                holdingBow(),
                extras.iotaArrowVisibility)) {
            return List.of();
        }
        return IotaPolicy.arrowHudLines(arrows);
    }

    static boolean hudVisible(QolUtilityConfig qol) {
        return qol.extras().iotaAddonsEnabled && qol.extras().iotaArrowTracker;
    }

    static String overlayTitle() {
        return overlayTitle == null ? "" : overlayTitle;
    }

    private static void tickToggles(Minecraft client, QolSkyblockExtras extras) {
        if (client.getWindow() == null) {
            return;
        }
        long window = client.getWindow().handle();
        boolean leftDown = QolKeybindNames.isBoundDown(window, extras.iotaToggleLeftKeybind);
        if (leftDown && !leftToggleWasDown) {
            leftLatched = !leftLatched;
            notify("Iota Toggle Left Click", leftLatched);
        }
        leftToggleWasDown = leftDown;
        boolean rightDown = QolKeybindNames.isBoundDown(window, extras.iotaToggleRightKeybind);
        if (rightDown && !rightToggleWasDown) {
            rightLatched = !rightLatched;
            notify("Iota Toggle Right Click", rightLatched);
        }
        rightToggleWasDown = rightDown;
    }

    private static void tickStandaloneClicker(Minecraft client, QolSkyblockExtras extras) {
        if (RotClientClient.qolConfigPublic().autoClickerEnabled) {
            leftAccumulator = 0.0D;
            rightAccumulator = 0.0D;
            return;
        }
        if (client.gui != null && client.gui.screen() != null && !client.gui.screen().isPauseScreen()) {
            leftAccumulator = 0.0D;
            rightAccumulator = 0.0D;
            return;
        }
        if (leftLatched) {
            leftAccumulator = AutoClickerPolicy.tickAccumulation(
                    leftAccumulator,
                    IotaPolicy.TOGGLE_CLICK_CPS,
                    ThreadLocalRandom.current().nextDouble());
            int clicks = AutoClickerPolicy.consumeClicks(leftAccumulator);
            leftAccumulator = AutoClickerPolicy.remainderAfterClicks(leftAccumulator, clicks);
            for (int i = 0; i < clicks; i++) {
                AutoClickerRuntime.pulseAttack(client);
            }
        } else {
            leftAccumulator = 0.0D;
        }
        if (rightLatched) {
            rightAccumulator = AutoClickerPolicy.tickAccumulation(
                    rightAccumulator,
                    IotaPolicy.TOGGLE_CLICK_CPS,
                    ThreadLocalRandom.current().nextDouble());
            int clicks = AutoClickerPolicy.consumeClicks(rightAccumulator);
            rightAccumulator = AutoClickerPolicy.remainderAfterClicks(rightAccumulator, clicks);
            for (int i = 0; i < clicks; i++) {
                AutoClickerRuntime.pulseUse(client);
            }
        } else {
            rightAccumulator = 0.0D;
        }
    }

    private static void tickArrowTracker(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.iotaArrowTracker || !inSkyblock() || client.player == null) {
            return;
        }
        ItemStack quiver = client.player.getInventory().getItem(8);
        if (quiver.isEmpty()) {
            return;
        }
        var key = BuiltInRegistries.ITEM.getKey(quiver.getItem());
        String desc = key == null ? "" : key.toString();
        String hover = quiver.getHoverName().getString();
        if (!IotaPolicy.isQuiverSlot(desc, hover)) {
            return;
        }
        List<String> lore = InventoryChromeRuntime.loreLines(quiver);
        IotaPolicy.parseArrowCountFromLore(lore).ifPresent(count -> {
            String type = IotaPolicy.strip(hover);
            if (waitingForQuiverRefresh
                    && type.equals(lockedArrowType)
                    && count == lockedArrowCount) {
                return;
            }
            waitingForQuiverRefresh = false;
            arrows = new IotaPolicy.ArrowSnapshot(type, count, false, true);
        });
    }

    private static void applyArrowNotice(
            QolSkyblockExtras extras,
            IotaPolicy.ArrowNotice notice) {
        if (notice.outOfArrows()) {
            lockedArrowType = arrows.type();
            lockedArrowCount = arrows.count();
            arrows = new IotaPolicy.ArrowSnapshot(arrows.type(), 0, true, true);
            waitingForQuiverRefresh = true;
            if (extras.iotaArrowNotifications) {
                overlayTitle = IotaPolicy.ARROW_EMPTY_TITLE;
                overlayUntilMs = System.currentTimeMillis() + 2000L;
                playBell();
                sendParty(IotaPolicy.ARROW_EMPTY_PARTY);
            }
            return;
        }
        arrows = new IotaPolicy.ArrowSnapshot(notice.type(), notice.count(), false, true);
        waitingForQuiverRefresh = false;
        if (extras.iotaArrowNotifications) {
            overlayTitle = IotaPolicy.ARROW_LOW_TITLE;
            overlayUntilMs = System.currentTimeMillis() + 2000L;
            playBell();
        }
    }

    private static void rememberKuudraInstance(String command) {
        if (command != null && command.startsWith("joininstance ")) {
            extras().iotaLastKuudraInstance = command.substring("joininstance ".length()).trim();
        }
    }

    private static boolean holdingTerminator() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        return IotaPolicy.isTerminatorId(
                AutoClickerItemIdentity.skyBlockId(client.player.getMainHandItem()));
    }

    private static boolean holdingBow() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = key == null ? "" : key.getPath();
        String id = AutoClickerItemIdentity.skyBlockId(stack).toLowerCase(Locale.ROOT);
        return path.contains("bow")
                || path.contains("crossbow")
                || id.contains("bow")
                || id.contains("terminator")
                || id.contains("juju")
                || id.contains("last_breath");
    }

    private static boolean inSkyblock() {
        return SkyBlockAreaDetector.isInSkyblock();
    }

    private static int pingMs(LocalPlayer player) {
        if (player == null || player.connection == null) {
            return 0;
        }
        PlayerInfo info = player.connection.getPlayerInfo(player.getUUID());
        return info == null ? 0 : Math.max(0, info.getLatency());
    }

    private static void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || player.connection == null || command == null || command.isBlank()) {
            return;
        }
        player.connection.sendCommand(command);
    }

    private static void sendParty(String text) {
        sendCommand("pc " + text);
    }

    private static void playJoinSound() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.level == null) {
            return;
        }
        client.level.playSound(
                player,
                player.blockPosition(),
                SoundEvents.NOTE_BLOCK_PLING.value(),
                SoundSource.PLAYERS,
                2.0F,
                1.0F);
    }

    private static void playDragonGrowl() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null) {
            return;
        }
        player.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.0F);
    }

    private static void playBell() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.level == null) {
            return;
        }
        client.level.playSound(
                player,
                player.blockPosition(),
                SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundSource.MASTER,
                2.0F,
                1.2F);
    }

    private static void notify(String label, boolean enabled) {
        RotClientClient.notifyQolModuleToggled(label, enabled);
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
