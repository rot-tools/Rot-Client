package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Plus Iota automations: toggle clicker, auto-requeue.
 */
final class IotaPlusRuntime {
    private IotaPlusRuntime() {
    }

    static void tick(Minecraft client) {
        QolSkyblockExtras extras = IotaRuntime.extras();
        if (client == null || client.player == null || !extras.iotaAddonsEnabled) {
            return;
        }
        tickToggles(client, extras);
        tickStandaloneClicker(client, extras);
    }

    static void onChat(Component message) {
        if (message == null) {
            return;
        }
        QolSkyblockExtras extras = IotaRuntime.extras();
        if (!extras.iotaAddonsEnabled || !extras.iotaAutoRequeue) {
            return;
        }
        String raw = message.getString();
        if (!IotaPolicy.isKuudraDefeatChat(raw)) {
            return;
        }
        String instance = extras.iotaLastKuudraInstance;
        if (instance != null && !instance.isBlank()) {
            IotaRuntime.sendCommand("joininstance " + instance);
        }
    }

    private static void tickToggles(Minecraft client, QolSkyblockExtras extras) {
        if (client.getWindow() == null) {
            return;
        }
        long window = client.getWindow().handle();
        boolean leftDown = QolKeybindNames.isBoundDown(window, extras.iotaToggleLeftKeybind);
        if (leftDown && !IotaRuntime.leftToggleWasDown) {
            IotaRuntime.leftLatched = !IotaRuntime.leftLatched;
            IotaRuntime.notify("Iota Toggle Left Click", IotaRuntime.leftLatched);
        }
        IotaRuntime.leftToggleWasDown = leftDown;
        boolean rightDown = QolKeybindNames.isBoundDown(window, extras.iotaToggleRightKeybind);
        if (rightDown && !IotaRuntime.rightToggleWasDown) {
            IotaRuntime.rightLatched = !IotaRuntime.rightLatched;
            IotaRuntime.notify("Iota Toggle Right Click", IotaRuntime.rightLatched);
        }
        IotaRuntime.rightToggleWasDown = rightDown;
    }

    private static void tickStandaloneClicker(Minecraft client, QolSkyblockExtras extras) {
        if (RotClientClient.qolConfigPublic().autoClickerEnabled) {
            IotaRuntime.leftAccumulator = 0.0D;
            IotaRuntime.rightAccumulator = 0.0D;
            return;
        }
        if (client.gui != null && client.gui.screen() != null && !client.gui.screen().isPauseScreen()) {
            IotaRuntime.leftAccumulator = 0.0D;
            IotaRuntime.rightAccumulator = 0.0D;
            return;
        }
        if (IotaRuntime.leftLatched) {
            IotaRuntime.leftAccumulator = AutoClickerPolicy.tickAccumulation(
                    IotaRuntime.leftAccumulator,
                    IotaPolicy.TOGGLE_CLICK_CPS,
                    ThreadLocalRandom.current().nextDouble());
            int clicks = AutoClickerPolicy.consumeClicks(IotaRuntime.leftAccumulator);
            IotaRuntime.leftAccumulator = AutoClickerPolicy.remainderAfterClicks(
                    IotaRuntime.leftAccumulator, clicks);
            for (int i = 0; i < clicks; i++) {
                ClickPulseHelper.pulseAttack(client);
            }
        } else {
            IotaRuntime.leftAccumulator = 0.0D;
        }
        if (IotaRuntime.rightLatched) {
            IotaRuntime.rightAccumulator = AutoClickerPolicy.tickAccumulation(
                    IotaRuntime.rightAccumulator,
                    IotaPolicy.TOGGLE_CLICK_CPS,
                    ThreadLocalRandom.current().nextDouble());
            int clicks = AutoClickerPolicy.consumeClicks(IotaRuntime.rightAccumulator);
            IotaRuntime.rightAccumulator = AutoClickerPolicy.remainderAfterClicks(
                    IotaRuntime.rightAccumulator, clicks);
            for (int i = 0; i < clicks; i++) {
                ClickPulseHelper.pulseUse(client);
            }
        } else {
            IotaRuntime.rightAccumulator = 0.0D;
        }
    }
}
