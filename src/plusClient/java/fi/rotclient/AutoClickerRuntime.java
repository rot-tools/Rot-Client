package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client tick bridge for Auto Clicker. Clicks pulse the attack/use
 * {@link KeyMapping} with a press-then-release, while block mining uses a held
 * attack state so consecutive blocks are not interrupted.
 */
final class AutoClickerRuntime {
    private static double leftAccumulator;
    private static double rightAccumulator;
    private static boolean syntheticAttackHeld;
    private static final AutoClickerCpsMeter CPS_METER = new AutoClickerCpsMeter();

    private AutoClickerRuntime() {
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.autoClickerEnabled || client == null) {
            releaseSyntheticAttack(client);
            resetAccumulators();
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.options == null) {
            releaseSyntheticAttack(client);
            resetAccumulators();
            return;
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (screen != null && !screen.isPauseScreen()) {
            releaseSyntheticAttack(client);
            resetAccumulators();
            return;
        }

        long window = client.getWindow().handle();
        String heldIdentity = AutoClickerItemIdentity.identify(
                player.getMainHandItem());
        String skyBlockId = AutoClickerItemIdentity.skyBlockId(
                player.getMainHandItem());
        boolean physicalLeft = isMappingHeld(client, client.options.keyAttack);
        boolean physicalRight = isMappingHeld(client, client.options.keyUse);
        boolean leftHeld = resolveActivationHeld(
                window, qol.autoClickerLeftKeybind, physicalLeft)
                || IotaRuntime.leftClickLatched();
        boolean rightHeld = resolveActivationHeld(
                window, qol.autoClickerRightKeybind, physicalRight)
                || IotaRuntime.rightClickLatched();
        boolean targetingBreakableBlock = isTargetingBreakableBlock(client);

        if (qol.autoClickerTerminatorOnly) {
            releaseSyntheticAttack(client);
            if (AutoClickerPolicy.shouldTerminatorLeftClick(
                    true,
                    true,
                    physicalRight,
                    skyBlockId)) {
                float cps = AutoClickerPolicy.clampCps(qol.autoClickerCps);
                rightAccumulator = AutoClickerPolicy.tickAccumulation(
                        rightAccumulator,
                        cps,
                        ThreadLocalRandom.current().nextDouble());
                int clicks = AutoClickerPolicy.consumeClicks(rightAccumulator);
                rightAccumulator = AutoClickerPolicy.remainderAfterClicks(
                        rightAccumulator, clicks);
                for (int i = 0; i < clicks; i++) {
                    performLeftClick(client);
                }
            } else {
                rightAccumulator = 0.0D;
            }
            leftAccumulator = 0.0D;
            return;
        }

        boolean autoLeftClickActive = AutoClickerPolicy.shouldAutoLeftClick(
                true,
                qol.autoClickerEnableLeft,
                leftHeld,
                qol.autoClickerBlockBreaker,
                qol.autoClickerWhitelistOnly,
                qol.autoClickerLeftWhitelist,
                heldIdentity,
                skyBlockId,
                targetingBreakableBlock,
                qol.autoClickerAllowBreaking);
        if (AutoClickerPolicy.shouldHoldBlockBreaking(
                autoLeftClickActive,
                targetingBreakableBlock,
                qol.autoClickerAllowBreaking)) {
            holdAttackForBlockBreaking(client);
            leftAccumulator = 0.0D;
        } else if (autoLeftClickActive) {
            releaseSyntheticAttack(client);
            float cps = AutoClickerPolicy.resolveLeftCps(
                    qol.autoClickerEnableLeft,
                    qol.autoClickerEnableRight,
                    qol.autoClickerCps,
                    qol.autoClickerLeftCps,
                    qol.autoClickerRightCps);
            leftAccumulator = AutoClickerPolicy.tickAccumulation(
                    leftAccumulator,
                    cps,
                    ThreadLocalRandom.current().nextDouble());
            int clicks = AutoClickerPolicy.consumeClicks(leftAccumulator);
            leftAccumulator = AutoClickerPolicy.remainderAfterClicks(
                    leftAccumulator, clicks);
            for (int i = 0; i < clicks; i++) {
                performLeftClick(client);
            }
        } else {
            releaseSyntheticAttack(client);
            leftAccumulator = 0.0D;
        }

        if (AutoClickerPolicy.shouldAutoRightClick(
                true,
                qol.autoClickerEnableRight,
                rightHeld,
                qol.autoClickerBlockBreaker,
                qol.autoClickerTerminatorOnly,
                physicalRight,
                qol.autoClickerWhitelistOnly,
                qol.autoClickerRightWhitelist,
                heldIdentity,
                skyBlockId)) {
            float cps = AutoClickerPolicy.resolveRightCps(
                    qol.autoClickerEnableLeft,
                    qol.autoClickerEnableRight,
                    qol.autoClickerCps,
                    qol.autoClickerLeftCps,
                    qol.autoClickerRightCps);
            rightAccumulator = AutoClickerPolicy.tickAccumulation(
                    rightAccumulator,
                    cps,
                    ThreadLocalRandom.current().nextDouble());
            int clicks = AutoClickerPolicy.consumeClicks(rightAccumulator);
            rightAccumulator = AutoClickerPolicy.remainderAfterClicks(
                    rightAccumulator, clicks);
            for (int i = 0; i < clicks; i++) {
                performRightClick(client);
            }
        } else {
            rightAccumulator = 0.0D;
        }
    }

    private static void performLeftClick(Minecraft client) {
        pulseClick(client, client.options.keyAttack);
        CPS_METER.recordLeft(System.currentTimeMillis());
    }

    private static void performRightClick(Minecraft client) {
        pulseClick(client, client.options.keyUse);
        CPS_METER.recordRight(System.currentTimeMillis());
    }

    static AutoClickerCpsMeter.Snapshot cpsSnapshot() {
        return CPS_METER.snapshot(System.currentTimeMillis());
    }

    static boolean isHoldingBlockBreak() {
        return syntheticAttackHeld;
    }

    static void pulseUse(Minecraft client) {
        if (client != null && client.options != null) {
            pulseClick(client, client.options.keyUse);
        }
    }

    static void pulseAttack(Minecraft client) {
        if (client != null && client.options != null) {
            pulseClick(client, client.options.keyAttack);
        }
    }

    static void setSneak(Minecraft client, boolean down) {
        if (client == null || client.options == null || client.options.keyShift == null) {
            return;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) client.options.keyShift)
                .rotclient$boundKey();
        if (bound != null) {
            KeyMapping.set(bound, down);
        }
        client.options.keyShift.setDown(down);
    }

    /**
     * Press-then-release of the attack/use key: set down, {@code click}, then
     * always release. Leaving a physically held attack mapping down after
     * the pulse lets vanilla {@code continueAttack(true)} keep {@code missTime}
     * alive, which swallows extra clicks and makes combat feel like ~2 CPS.
     * Block mining uses {@link #holdAttackForBlockBreaking} instead.
     */
    private static void pulseClick(Minecraft client, KeyMapping mapping) {
        if (client == null || mapping == null) {
            return;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping)
                .rotclient$boundKey();
        if (bound == null) {
            return;
        }
        KeyMapping.set(bound, true);
        KeyMapping.click(bound);
        KeyMapping.set(bound, false);
    }

    private static void holdAttackForBlockBreaking(Minecraft client) {
        if (client == null || client.options == null || client.options.keyAttack == null) {
            return;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) client.options.keyAttack)
                .rotclient$boundKey();
        if (bound == null) {
            return;
        }
        KeyMapping.set(bound, true);
        syntheticAttackHeld = true;
    }

    private static void releaseSyntheticAttack(Minecraft client) {
        if (!syntheticAttackHeld) {
            return;
        }
        syntheticAttackHeld = false;
        if (client == null || client.options == null || client.options.keyAttack == null) {
            return;
        }
        KeyMapping mapping = client.options.keyAttack;
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping).rotclient$boundKey();
        if (bound != null) {
            KeyMapping.set(bound, isMappingHeld(client, mapping));
        }
    }

    private static boolean isMappingHeld(Minecraft client, KeyMapping mapping) {
        if (client == null || mapping == null || client.getWindow() == null) {
            return false;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping)
                .rotclient$boundKey();
        if (bound == null) {
            return false;
        }
        long handle = client.getWindow().handle();
        int value = bound.getValue();
        if (bound.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(handle, value) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, value) == GLFW.GLFW_PRESS;
    }

    private static boolean isTargetingBreakableBlock(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        if (client.level == null) {
            return false;
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockState state = client.level.getBlockState(blockHit.getBlockPos());
        return !state.isAir();
    }

    private static boolean resolveActivationHeld(
            long window,
            String configuredKeybind,
            boolean defaultMouseHeld) {
        if (configuredKeybind == null || configuredKeybind.isBlank()) {
            return defaultMouseHeld;
        }
        String token = configuredKeybind.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        switch (token) {
            case "MOUSE_LEFT", "LMB", "LEFT_MOUSE", "MOUSE_1" -> {
                return isMouseDown(window, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            }
            case "MOUSE_RIGHT", "RMB", "RIGHT_MOUSE", "MOUSE_2" -> {
                return isMouseDown(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            }
            case "MOUSE_MIDDLE", "MMB", "MIDDLE_MOUSE", "MOUSE_3" -> {
                return isMouseDown(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
            }
            default -> {
                int glfwKey = QolKeybindNames.resolveGlfwKey(token, "");
                if (glfwKey != GLFW.GLFW_KEY_UNKNOWN) {
                    return QolKeybindNames.isKeyDown(window, glfwKey);
                }
                return defaultMouseHeld;
            }
        }
    }

    private static boolean isMouseDown(long window, int button) {
        if (window == 0L) {
            return false;
        }
        return GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }

    private static void resetAccumulators() {
        leftAccumulator = 0.0D;
        rightAccumulator = 0.0D;
    }
}
