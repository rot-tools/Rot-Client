package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Etherwarp helper: left-click uses the held Etherwarp item. Optional
 * auto-shift writes sneak into the player input for 2–4 ticks, warps, then
 * releases sneak after the use packet has gone out.
 */
public final class EtherwarpHelperRuntime {
    private static int pendingWarpTicks;
    private static int pendingReleaseTicks;
    private static boolean holdingSneakForWarp;

    private EtherwarpHelperRuntime() {
    }

    static void clear() {
        Minecraft client = Minecraft.getInstance();
        if (holdingSneakForWarp) {
            AutoClickerRuntime.setSneak(client, false);
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
        if (!EtherwarpHelperPolicy.shouldAssistLookTarget(hasValidLookTarget(client, qol))) {
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
        // Never leave synthetic sneak active inside an inventory or a wardrobe.
        // Shift-click belongs entirely to the player while a container is open.
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
            AutoClickerRuntime.setSneak(client, true);
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
                AutoClickerRuntime.setSneak(client, false);
                holdingSneakForWarp = false;
            }
        }
    }

    private static boolean hasValidLookTarget(Minecraft client, QolUtilityConfig qol) {
        if (client == null || client.player == null || client.level == null) {
            return false;
        }
        LocalPlayer player = client.player;
        var eye = player.getEyePosition();
        var look = player.getViewVector(1.0F);
        Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(eye.x, eye.y, eye.z),
                Optional.empty(),
                qol.etherwarpUseServerPosition,
                new EtherwarpPredictor.Vec3d(look.x, look.y, look.z),
                EtherwarpPredictor.DEFAULT_RANGE,
                occupancy(client.level));
        return EtherwarpPredictor.canWarpTo(target);
    }

    private static EtherwarpPredictor.BlockOccupancy occupancy(BlockGetter level) {
        return new EtherwarpPredictor.BlockOccupancy() {
            @Override
            public boolean isSolidSurface(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return !state.getCollisionShape(level, pos).isEmpty();
            }

            @Override
            public boolean isStandSpaceClear(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = level.getBlockState(pos);
                return state.getCollisionShape(level, pos).isEmpty()
                        && level.getFluidState(pos).isEmpty();
            }
        };
    }

    private static void beginAutoShift(Minecraft client) {
        AutoClickerRuntime.setSneak(client, true);
        holdingSneakForWarp = true;
        pendingWarpTicks = EtherwarpHelperPolicy.shiftHoldTicks(
                ThreadLocalRandom.current().nextDouble());
        pendingReleaseTicks = 0;
    }

    private static void warpNow(Minecraft client) {
        AutoClickerRuntime.pulseUse(client);
        if (client != null && client.player != null) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
