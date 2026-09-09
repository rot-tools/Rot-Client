package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Plus Etherwarp left-click warp / auto-shift. Guess render stays shared.
 */
public final class EtherwarpPlusRuntime {
    private static int pendingWarpTicks;
    private static int pendingReleaseTicks;
    private static boolean holdingSneakForWarp;

    private EtherwarpPlusRuntime() {
    }

    static void clear() {
        Minecraft client = Minecraft.getInstance();
        if (holdingSneakForWarp) {
            ClickPulseHelper.setSneak(client, false);
        }
        pendingWarpTicks = 0;
        pendingReleaseTicks = 0;
        holdingSneakForWarp = false;
    }

    public static boolean forceSneak() {
        return EtherwarpHelperPolicy.forceSneakInput(holdingSneakForWarp);
    }

    public static boolean onMousePress(int button, int action) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || action != GLFW.GLFW_PRESS) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (client == null || client.player == null || client.options == null) {
            return false;
        }
        if (client.gui != null && client.gui.screen() != null) {
            return false;
        }
        LocalPlayer player = client.player;
        boolean etherwarpItem = AutoClickerItemIdentity.isEtherwarpItem(player.getMainHandItem());
        if (!EtherwarpHelperPolicy.shouldHandleLeftClick(
                qol.etherwarpEnabled,
                qol.etherwarpLeftClickWarp,
                true,
                true,
                etherwarpItem)) {
            return false;
        }
        if (!EtherwarpHelperPolicy.shouldAssistLookTarget(
                EtherwarpHelperRuntime.hasValidLookTarget(client, qol))) {
            return false;
        }
        boolean sneaking = player.isShiftKeyDown() || player.isCrouching();
        if (!EtherwarpHelperPolicy.canWarpNow(sneaking, qol.etherwarpShiftAutomatically)) {
            return false;
        }
        if (EtherwarpHelperPolicy.needsAutoShift(sneaking, qol.etherwarpShiftAutomatically)) {
            beginAutoShift(client);
            return true;
        }
        warpNow(client);
        return true;
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (client == null || client.gui == null || client.gui.screen() != null) {
            if (holdingSneakForWarp || pendingWarpTicks > 0 || pendingReleaseTicks > 0) {
                clear();
            }
            return;
        }
        if (!qol.etherwarpEnabled || !qol.etherwarpLeftClickWarp) {
            if (holdingSneakForWarp || pendingWarpTicks > 0 || pendingReleaseTicks > 0) {
                clear();
            }
            return;
        }
        if (holdingSneakForWarp) {
            ClickPulseHelper.setSneak(client, true);
        }
        if (pendingWarpTicks > 0) {
            pendingWarpTicks--;
            if (pendingWarpTicks == 0) {
                warpNow(client);
                pendingReleaseTicks = 2;
            }
        } else if (pendingReleaseTicks > 0) {
            pendingReleaseTicks--;
            if (pendingReleaseTicks == 0 && holdingSneakForWarp) {
                ClickPulseHelper.setSneak(client, false);
                holdingSneakForWarp = false;
            }
        }
    }

    private static void beginAutoShift(Minecraft client) {
        ClickPulseHelper.setSneak(client, true);
        holdingSneakForWarp = true;
        pendingWarpTicks = EtherwarpHelperPolicy.shiftHoldTicks(
                ThreadLocalRandom.current().nextDouble());
        pendingReleaseTicks = 0;
    }

    private static void warpNow(Minecraft client) {
        ClickPulseHelper.pulseUse(client);
        if (client != null && client.player != null) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
