package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Plus-only automatic beacon, chopping, and axe-toss actions. */
final class ForagingAutomationRuntime {
    private static int delay;

    private ForagingAutomationRuntime() {
    }

    static void reset() {
        delay = 0;
    }

    static void tick(Minecraft client) {
        if (delay > 0) {
            delay--;
        }
        if (delay > 0 || client == null || client.player == null || client.level == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!ForagingRuntime.cheats(extras)) {
            return;
        }
        if (maybeBeacon(client, extras)) {
            return;
        }
        maybeChopOrToss(client, extras);
    }

    private static boolean maybeBeacon(Minecraft client, QolSkyblockExtras extras) {
        if (!ForagingRuntime.toggle(
                extras, "qol.foraging_cheats", "qol.foraging_cheats.auto_beacon", false)) {
            return false;
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?> container) || client.gameMode == null) {
            return false;
        }
        String title = screen.getTitle().getString();
        ForagingAutomationPolicy.AutoBeaconClick click = ForagingAutomationPolicy.nextBeaconClick(
                true, true, title, ForagingRuntime.beaconHint);
        if (click == null || click.slot() < 0 || click.slot() >= container.getMenu().slots.size()) {
            return false;
        }
        client.gameMode.handleContainerInput(
                container.getMenu().containerId,
                click.slot(),
                click.rightClick() ? 1 : 0,
                ContainerInput.PICKUP,
                client.player);
        ForagingRuntime.beaconHint = ForagingAutomationPolicy.applyPress(
                ForagingRuntime.beaconHint, click.slot(), click.rightClick(), title);
        delay = configuredDelay(extras);
        return true;
    }

    private static void maybeChopOrToss(Minecraft client, QolSkyblockExtras extras) {
        if (client.gui != null && client.gui.screen() != null) {
            return;
        }
        LocalPlayer player = client.player;
        String axeId = SkyBlockItemIdentity.skyBlockId(player.getMainHandItem());
        boolean lookingLog = false;
        HitResult hit = client.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            String lookId = ForagingRuntime.blockId(
                    client.level.getBlockState(((BlockHitResult) hit).getBlockPos()));
            ForagingRuntime.island = ForagingPolicy.inferIslandFromLog(
                    ForagingRuntime.island, lookId);
            lookingLog = ForagingPolicy.isChopLog(ForagingRuntime.island, lookId);
        }
        int minCluster = ForagingRuntime.number(
                extras, "qol.foraging_cheats.min_cluster", 5);
        if (ForagingAutomationPolicy.shouldAxeToss(
                true,
                ForagingRuntime.toggle(
                        extras, "qol.foraging_cheats", "qol.foraging_cheats.axe_toss", false),
                ForagingPolicy.isThrowableAxe(axeId),
                ForagingRuntime.clusterWood,
                minCluster,
                true)) {
            ClickPulseHelper.pulseUse(client);
            delay = configuredDelay(extras);
            return;
        }
        if (ForagingAutomationPolicy.shouldAutoChop(
                true,
                ForagingRuntime.toggle(
                        extras, "qol.foraging_cheats", "qol.foraging_cheats.auto_chop", false),
                ForagingRuntime.island,
                ForagingPolicy.isForagingAxe(axeId),
                lookingLog)) {
            ClickPulseHelper.pulseAttack(client);
            delay = configuredDelay(extras);
        }
    }

    private static int configuredDelay(QolSkyblockExtras extras) {
        return Math.max(1, ForagingRuntime.number(
                extras, "qol.foraging_cheats.click_delay", 3));
    }
}
